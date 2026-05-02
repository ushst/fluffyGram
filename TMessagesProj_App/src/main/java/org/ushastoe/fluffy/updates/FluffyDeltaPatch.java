package org.ushastoe.fluffy.updates;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
 */
public final class FluffyDeltaPatch {

    private static final String BSDIFF_MAGIC = "BSDIFF40";
    private static final int HEADER_SIZE = 32;

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

        byte[] oldData = readFile(oldFile);
        byte[] header = new byte[HEADER_SIZE];

        try (FileInputStream patchIn = new FileInputStream(patchFile)) {
            readFully(patchIn, header);

            String magic = new String(header, 0, 8, java.nio.charset.StandardCharsets.US_ASCII);
            if (!BSDIFF_MAGIC.equals(magic)) {
                throw new IOException("Invalid patch magic: " + magic);
            }

            long ctrlLen = readInt64LE(header, 8);
            long diffLen = readInt64LE(header, 16);
            int newSize = (int) readInt64LE(header, 24);

            if (ctrlLen < 0 || diffLen < 0 || newSize < 0) {
                throw new IOException("Corrupt patch header: negative field");
            }

            byte[] newData = new byte[newSize];

            LimitedInputStream ctrlRaw = new LimitedInputStream(patchIn, ctrlLen);
            LimitedInputStream diffRaw = new LimitedInputStream(patchIn, diffLen);
            // extra block follows immediately after diffLen bytes
            BZip2CompressorInputStream ctrlBz = new BZip2CompressorInputStream(ctrlRaw);
            BZip2CompressorInputStream diffBz = new BZip2CompressorInputStream(diffRaw);
            BZip2CompressorInputStream extraBz = new BZip2CompressorInputStream(
                    new BufferedInputStream(patchIn));

            int oldPos = 0;
            int newPos = 0;

            while (newPos < newSize) {
                // Read ctrl triple
                long x = readInt64BZ(ctrlBz);
                long y = readInt64BZ(ctrlBz);
                long z = readInt64BZ(ctrlBz);

                // Bounds check
                if (newPos + x > newSize) {
                    throw new IOException("Corrupt patch: diff overrun");
                }

                // Apply diff block (XOR with old data)
                for (int i = 0; i < (int) x; i++) {
                    int diffByte = diffBz.read();
                    if (diffByte == -1) {
                        throw new IOException("Corrupt patch: diff stream ended prematurely");
                    }
                    int oldByte = (oldPos + i >= 0 && oldPos + i < oldData.length)
                            ? (oldData[oldPos + i] & 0xFF) : 0;
                    newData[newPos + i] = (byte) (diffByte ^ oldByte);
                }
                newPos += (int) x;
                oldPos += (int) x;

                // Apply extra block (raw new data)
                if (newPos + y > newSize) {
                    throw new IOException("Corrupt patch: extra overrun");
                }
                readFully(extraBz, newData, newPos, (int) y);
                newPos += (int) y;

                // Adjust old position
                oldPos += (int) z;
            }

            ctrlBz.close();
            diffBz.close();
            extraBz.close();

            // Write result
            try (FileOutputStream out = new FileOutputStream(newFile)) {
                out.write(newData);
                out.getFD().sync();
            }
        }
    }

    // ---- helpers ----

    private static byte[] readFile(File file) throws IOException {
        long len = file.length();
        if (len > Integer.MAX_VALUE) {
            throw new IOException("File too large: " + file);
        }
        byte[] data = new byte[(int) len];
        try (FileInputStream in = new FileInputStream(file)) {
            readFully(in, data);
        }
        return data;
    }

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
