package org.ushastoe.fluffy.updates;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Applies a bsdiff binary patch to an existing APK to produce the updated APK.
 *
 * Patch format: standard bsdiff 4.3 ("BSDIFF40"):
 *   Bytes  0-7:  magic "BSDIFF40"
 *   Bytes  8-15: ctrl block compressed length (bzip2)
 *   Bytes 16-23: diff block compressed length (bzip2)
 *   Bytes 24-31: new file length
 *   Then:        bzip2(ctrl block) | bzip2(diff block) | bzip2(extra block)
 *
 * Each ctrl block is a sequence of (x, y, z) int64 triples:
 *   x — number of bytes to read from diff stream (XOR with old data)
 *   y — number of bytes to read from extra stream (raw new data)
 *   z — signed seek delta in old file
 *
 * Memory usage: O(CHUNK_SIZE) — old APK is read via RandomAccessFile in chunks;
 * the new APK is written sequentially. No full-file buffers are allocated.
 */
public final class FluffyDeltaPatch {

    private static final String BSDIFF_MAGIC = "BSDIFF40";
    private static final int HEADER_SIZE = 32;
    private static final int CHUNK = 64 * 1024;

    private FluffyDeltaPatch() {}

    /**
     * Applies {@code patchFile} (bsdiff format) to {@code oldFile} and writes the result to
     * {@code newFile}.
     *
     * @throws IOException              if I/O fails or the patch is malformed
     * @throws IllegalArgumentException if the old file is missing or the patch magic is wrong
     */
    public static void apply(File oldFile, File patchFile, File newFile) throws IOException {
        if (!oldFile.exists()) {
            throw new IllegalArgumentException("Old APK not found: " + oldFile);
        }
        if (!patchFile.exists()) {
            throw new IllegalArgumentException("Patch file not found: " + patchFile);
        }

        byte[] header = new byte[HEADER_SIZE];
        long oldLen = oldFile.length();

        try (RandomAccessFile oldRaf = new RandomAccessFile(oldFile, "r");
             FileInputStream patchIn = new FileInputStream(patchFile);
             FileOutputStream newOut = new FileOutputStream(newFile)) {

            readFully(patchIn, header);

            String magic = new String(header, 0, 8, StandardCharsets.US_ASCII);
            if (!BSDIFF_MAGIC.equals(magic)) {
                throw new IOException("Invalid patch magic: " + magic);
            }

            long ctrlLen = readInt64LE(header, 8);
            long diffLen = readInt64LE(header, 16);
            int newSize = (int) readInt64LE(header, 24);

            if (ctrlLen < 0 || diffLen < 0 || newSize < 0) {
                throw new IOException("Corrupt patch header: negative field");
            }

            LimitedInputStream ctrlRaw = new LimitedInputStream(patchIn, ctrlLen);
            LimitedInputStream diffRaw = new LimitedInputStream(patchIn, diffLen);

            BZip2CompressorInputStream ctrlBz = new BZip2CompressorInputStream(ctrlRaw);
            BZip2CompressorInputStream diffBz = new BZip2CompressorInputStream(diffRaw);
            BZip2CompressorInputStream extraBz = new BZip2CompressorInputStream(
                    new BufferedInputStream(patchIn));

            // Reusable chunk buffers — avoids any full-file allocation
            byte[] diffBuf = new byte[CHUNK];
            byte[] oldBuf = new byte[CHUNK];
            byte[] extraBuf = new byte[CHUNK];

            long newPos = 0;
            long oldPos = 0;

            while (newPos < newSize) {
                long x = readInt64BZ(ctrlBz);
                long y = readInt64BZ(ctrlBz);
                long z = readInt64BZ(ctrlBz);

                if (x < 0 || y < 0 || newPos + x > newSize || newPos + x + y > newSize) {
                    throw new IOException("Corrupt patch ctrl block at newPos=" + newPos);
                }

                // Diff block: read x bytes from diffBz, XOR with old[oldPos..oldPos+x)
                long remaining = x;
                long readOldPos = oldPos;
                while (remaining > 0) {
                    int chunk = (int) Math.min(remaining, CHUNK);
                    readFully(diffBz, diffBuf, 0, chunk);

                    // Compute which part of this chunk overlaps with the valid old-file range
                    long overlapStart = Math.max(0L, readOldPos);
                    long overlapEnd = Math.min(oldLen, readOldPos + chunk);
                    int overlapLen = (int) Math.max(0L, overlapEnd - overlapStart);
                    // Offset within diffBuf where old data begins
                    int overlapBufOff = (int) Math.max(0L, -readOldPos);

                    if (overlapLen > 0) {
                        oldRaf.seek(overlapStart);
                        readFullyRaf(oldRaf, oldBuf, 0, overlapLen);
                        for (int i = 0; i < overlapLen; i++) {
                            diffBuf[overlapBufOff + i] ^= oldBuf[i];
                        }
                    }

                    newOut.write(diffBuf, 0, chunk);
                    remaining -= chunk;
                    readOldPos += chunk;
                }
                newPos += x;
                oldPos += x;

                // Extra block: copy y raw new bytes directly to output
                remaining = y;
                while (remaining > 0) {
                    int chunk = (int) Math.min(remaining, CHUNK);
                    readFully(extraBz, extraBuf, 0, chunk);
                    newOut.write(extraBuf, 0, chunk);
                    remaining -= chunk;
                }
                newPos += y;

                // Adjust old position
                oldPos += z;
            }

            ctrlBz.close();
            diffBz.close();
            extraBz.close();
            newOut.getFD().sync();
        }
    }

    // ---- helpers ----

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        readFully(in, buf, 0, buf.length);
    }

    private static void readFully(InputStream in, byte[] buf, int off, int len) throws IOException {
        int remaining = len;
        while (remaining > 0) {
            int read = in.read(buf, off + (len - remaining), remaining);
            if (read == -1) {
                throw new IOException("Unexpected end of stream (need " + remaining + " more bytes)");
            }
            remaining -= read;
        }
    }

    private static void readFullyRaf(RandomAccessFile raf, byte[] buf, int off, int len) throws IOException {
        int remaining = len;
        while (remaining > 0) {
            int read = raf.read(buf, off + (len - remaining), remaining);
            if (read == -1) {
                throw new IOException("Unexpected end of file in old APK");
            }
            remaining -= read;
        }
    }

    /** Read a signed 64-bit little-endian integer from a byte array. */
    private static long readInt64LE(byte[] buf, int offset) {
        return ByteBuffer.wrap(buf, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * Read a signed 64-bit integer from a bzip2 stream using bsdiff's off_t encoding.
     * bsdiff stores numbers as 8 bytes, little-endian, with the sign bit in byte[7] bit 7.
     */
    private static long readInt64BZ(InputStream in) throws IOException {
        byte[] buf = new byte[8];
        readFully(in, buf);
        long value = 0;
        for (int i = 7; i >= 0; i--) {
            value = (value << 8) | (buf[i] & 0xFF);
        }
        // bsdiff sign encoding: sign is stored in the high bit of the last byte
        if ((buf[7] & 0x80) != 0) {
            value &= ~(1L << 63);
            value = -value;
        }
        return value;
    }

    /**
     * Wraps an InputStream and limits it to exactly {@code limit} bytes before presenting EOF.
     * Used to feed exactly the ctrl/diff segments to their respective bzip2 streams.
     */
    private static final class LimitedInputStream extends InputStream {
        private final InputStream wrapped;
        private long remaining;

        LimitedInputStream(InputStream wrapped, long limit) {
            this.wrapped = wrapped;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = wrapped.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int read = wrapped.read(buf, off, toRead);
            if (read > 0) remaining -= read;
            return read;
        }

        @Override
        public void close() {
            // Do NOT close the underlying stream; the caller manages it.
        }
    }
}
