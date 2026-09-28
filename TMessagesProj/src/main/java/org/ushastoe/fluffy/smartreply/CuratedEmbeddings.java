package org.ushastoe.fluffy.smartreply;

import android.content.Context;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/** Reads assets/fluffy/smart_reply_embeddings.bin produced by scripts/build_smart_reply_embeddings.py. */
final class CuratedEmbeddings {

    private static final String ASSET_PATH = "fluffy/smart_reply_embeddings.bin";

    private CuratedEmbeddings() {
    }

    static HashMap<Long, float[]> load(Context app) throws IOException {
        try (InputStream raw = app.getAssets().open(ASSET_PATH);
             DataInputStream in = new DataInputStream(new BufferedInputStream(raw, 64 * 1024))) {
            byte[] head = new byte[16];
            in.readFully(head);
            ByteBuffer hb = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
            if (hb.get(0) != 'F' || hb.get(1) != 'S' || hb.get(2) != 'R' || hb.get(3) != 'E' || hb.getInt(4) != 1) {
                throw new IOException("bad embeddings asset");
            }
            int dim = hb.getInt(8);
            int count = hb.getInt(12);
            if (dim != TinyBertEncoder.DIM) {
                throw new IOException("embeddings dim " + dim);
            }
            HashMap<Long, float[]> out = new HashMap<>(count * 2);
            byte[] entry = new byte[12 + dim];
            for (int i = 0; i < count; i++) {
                in.readFully(entry);
                ByteBuffer eb = ByteBuffer.wrap(entry).order(ByteOrder.LITTLE_ENDIAN);
                long hash = eb.getLong(0);
                float scale = eb.getFloat(8);
                float[] v = new float[dim];
                for (int d = 0; d < dim; d++) {
                    v[d] = entry[12 + d] * scale;
                }
                out.put(hash, v);
            }
            return out;
        }
    }

    /** FNV-1a 64 over UTF-8 — must match the build script. */
    static long hash(String cleaned) {
        long h = 0xcbf29ce484222325L;
        for (byte b : cleaned.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xff);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
