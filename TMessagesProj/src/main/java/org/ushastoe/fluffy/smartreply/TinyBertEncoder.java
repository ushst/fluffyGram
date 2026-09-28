package org.ushastoe.fluffy.smartreply;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java forward pass of cointegrated/rubert-tiny2 (3-layer BERT, hidden 312)
 * straight from its safetensors file. Output: L2-normalized CLS vector, as in the
 * model's sentence-transformers config. The word-embedding table stays memory-mapped.
 * Not thread-safe: use from a single thread.
 */
final class TinyBertEncoder {

    static final int DIM = 312;
    private static final int HEADS = 12;
    private static final int HEAD_DIM = DIM / HEADS;
    private static final int FFN = 600;
    private static final int LAYERS = 3;
    private static final int MAX_LEN = 64;
    private static final float LN_EPS = 1e-12f;

    private static final Pattern TENSOR = Pattern.compile(
            "\"([^\"]+)\":\\{\"dtype\":\"(\\w+)\",\"shape\":\\[([\\d,]*)\\],\"data_offsets\":\\[(\\d+),(\\d+)\\]\\}");

    private final WordPieceTokenizer tokenizer;
    private final FloatBuffer wordEmbeddings;
    private final int vocabSize;
    private final float[] positions;
    private final float[] tokenType0;
    private final float[] embLnG;
    private final float[] embLnB;
    private final Layer[] layers = new Layer[LAYERS];

    TinyBertEncoder(File safetensors, File vocab) throws IOException {
        tokenizer = new WordPieceTokenizer(vocab);
        try (RandomAccessFile raf = new RandomAccessFile(safetensors, "r")) {
            FileChannel channel = raf.getChannel();
            ByteBuffer lenBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            channel.read(lenBuf, 0);
            long headerLen = lenBuf.getLong(0);
            if (headerLen <= 0 || headerLen > 1_000_000) {
                throw new IOException("bad safetensors header");
            }
            ByteBuffer headerBuf = ByteBuffer.allocate((int) headerLen);
            channel.read(headerBuf, 8);
            String header = new String(headerBuf.array(), StandardCharsets.UTF_8);
            long base = 8 + headerLen;

            HashMap<String, long[]> index = new HashMap<>();
            Matcher m = TENSOR.matcher(header);
            while (m.find()) {
                if ("F32".equals(m.group(2))) {
                    index.put(m.group(1), new long[]{Long.parseLong(m.group(4)), Long.parseLong(m.group(5))});
                }
            }
            Loader loader = new Loader(channel, base, index);

            long[] word = loader.span("bert.embeddings.word_embeddings.weight");
            vocabSize = (int) ((word[1] - word[0]) / 4 / DIM);
            wordEmbeddings = channel.map(FileChannel.MapMode.READ_ONLY, base + word[0], word[1] - word[0])
                    .order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            positions = loader.floats("bert.embeddings.position_embeddings.weight", MAX_LEN * DIM);
            tokenType0 = loader.floats("bert.embeddings.token_type_embeddings.weight", DIM);
            embLnG = loader.floats("bert.embeddings.LayerNorm.weight", DIM);
            embLnB = loader.floats("bert.embeddings.LayerNorm.bias", DIM);
            for (int i = 0; i < LAYERS; i++) {
                layers[i] = new Layer(loader, "bert.encoder.layer." + i + ".");
            }
        }
    }

    float[] encode(String text) {
        int[] ids = tokenizer.encode(text, MAX_LEN);
        int len = ids.length;
        float[] x = new float[len * DIM];
        float[] row = new float[DIM];
        for (int t = 0; t < len; t++) {
            int id = ids[t] < vocabSize ? ids[t] : 0;
            wordEmbeddings.position(id * DIM);
            wordEmbeddings.get(row, 0, DIM);
            int off = t * DIM;
            for (int d = 0; d < DIM; d++) {
                x[off + d] = row[d] + positions[off + d] + tokenType0[d];
            }
        }
        layerNorm(x, len, embLnG, embLnB);
        for (Layer layer : layers) {
            x = layer.forward(x, len);
        }
        float[] cls = new float[DIM];
        System.arraycopy(x, 0, cls, 0, DIM);
        normalize(cls);
        return cls;
    }

    static float dot(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    private static void normalize(float[] v) {
        float n = (float) Math.sqrt(dot(v, v));
        if (n > 0f) {
            for (int i = 0; i < v.length; i++) {
                v[i] /= n;
            }
        }
    }

    /** y[len, out] = x[len, in] · Wᵀ + b, W stored row-major [out, in]. */
    private static float[] linear(float[] x, int len, int in, float[] w, float[] b, int out) {
        float[] y = new float[len * out];
        for (int t = 0; t < len; t++) {
            int xo = t * in;
            int yo = t * out;
            for (int o = 0; o < out; o++) {
                int wo = o * in;
                float s = b[o];
                for (int i = 0; i < in; i++) {
                    s += x[xo + i] * w[wo + i];
                }
                y[yo + o] = s;
            }
        }
        return y;
    }

    private static void layerNorm(float[] x, int len, float[] g, float[] b) {
        for (int t = 0; t < len; t++) {
            int off = t * DIM;
            float mean = 0f;
            for (int d = 0; d < DIM; d++) {
                mean += x[off + d];
            }
            mean /= DIM;
            float var = 0f;
            for (int d = 0; d < DIM; d++) {
                float c = x[off + d] - mean;
                var += c * c;
            }
            float inv = (float) (1.0 / Math.sqrt(var / DIM + LN_EPS));
            for (int d = 0; d < DIM; d++) {
                x[off + d] = (x[off + d] - mean) * inv * g[d] + b[d];
            }
        }
    }

    private static float gelu(float v) {
        return (float) (0.5 * v * (1.0 + erf(v / Math.sqrt(2.0))));
    }

    /** Abramowitz–Stegun 7.1.26, |error| < 1.5e-7. */
    private static double erf(double x) {
        double sign = x < 0 ? -1 : 1;
        x = Math.abs(x);
        double t = 1.0 / (1.0 + 0.3275911 * x);
        double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return sign * y;
    }

    private static final class Layer {
        final float[] qW, qB, kW, kB, vW, vB, oW, oB, ln1G, ln1B, iW, iB, o2W, o2B, ln2G, ln2B;

        Layer(Loader l, String p) throws IOException {
            qW = l.floats(p + "attention.self.query.weight", DIM * DIM);
            qB = l.floats(p + "attention.self.query.bias", DIM);
            kW = l.floats(p + "attention.self.key.weight", DIM * DIM);
            kB = l.floats(p + "attention.self.key.bias", DIM);
            vW = l.floats(p + "attention.self.value.weight", DIM * DIM);
            vB = l.floats(p + "attention.self.value.bias", DIM);
            oW = l.floats(p + "attention.output.dense.weight", DIM * DIM);
            oB = l.floats(p + "attention.output.dense.bias", DIM);
            ln1G = l.floats(p + "attention.output.LayerNorm.weight", DIM);
            ln1B = l.floats(p + "attention.output.LayerNorm.bias", DIM);
            iW = l.floats(p + "intermediate.dense.weight", FFN * DIM);
            iB = l.floats(p + "intermediate.dense.bias", FFN);
            o2W = l.floats(p + "output.dense.weight", DIM * FFN);
            o2B = l.floats(p + "output.dense.bias", DIM);
            ln2G = l.floats(p + "output.LayerNorm.weight", DIM);
            ln2B = l.floats(p + "output.LayerNorm.bias", DIM);
        }

        float[] forward(float[] x, int len) {
            float[] q = linear(x, len, DIM, qW, qB, DIM);
            float[] k = linear(x, len, DIM, kW, kB, DIM);
            float[] v = linear(x, len, DIM, vW, vB, DIM);
            float[] ctx = new float[len * DIM];
            float[] scores = new float[len];
            float scale = (float) (1.0 / Math.sqrt(HEAD_DIM));
            for (int h = 0; h < HEADS; h++) {
                int ho = h * HEAD_DIM;
                for (int i = 0; i < len; i++) {
                    float max = Float.NEGATIVE_INFINITY;
                    for (int j = 0; j < len; j++) {
                        float s = 0f;
                        for (int d = 0; d < HEAD_DIM; d++) {
                            s += q[i * DIM + ho + d] * k[j * DIM + ho + d];
                        }
                        s *= scale;
                        scores[j] = s;
                        if (s > max) {
                            max = s;
                        }
                    }
                    float sum = 0f;
                    for (int j = 0; j < len; j++) {
                        scores[j] = (float) Math.exp(scores[j] - max);
                        sum += scores[j];
                    }
                    for (int j = 0; j < len; j++) {
                        float p = scores[j] / sum;
                        for (int d = 0; d < HEAD_DIM; d++) {
                            ctx[i * DIM + ho + d] += p * v[j * DIM + ho + d];
                        }
                    }
                }
            }
            float[] a = linear(ctx, len, DIM, oW, oB, DIM);
            for (int i = 0; i < a.length; i++) {
                a[i] += x[i];
            }
            layerNorm(a, len, ln1G, ln1B);
            float[] hidden = linear(a, len, DIM, iW, iB, FFN);
            for (int i = 0; i < hidden.length; i++) {
                hidden[i] = gelu(hidden[i]);
            }
            float[] out = linear(hidden, len, FFN, o2W, o2B, DIM);
            for (int i = 0; i < out.length; i++) {
                out[i] += a[i];
            }
            layerNorm(out, len, ln2G, ln2B);
            return out;
        }
    }

    private static final class Loader {
        private final FileChannel channel;
        private final long base;
        private final HashMap<String, long[]> index;

        Loader(FileChannel channel, long base, HashMap<String, long[]> index) {
            this.channel = channel;
            this.base = base;
            this.index = index;
        }

        long[] span(String name) throws IOException {
            long[] span = index.get(name);
            if (span == null) {
                throw new IOException("missing tensor " + name);
            }
            return span;
        }

        /** Reads the first {@code count} floats of a tensor. */
        float[] floats(String name, int count) throws IOException {
            long[] span = span(name);
            if (span[1] - span[0] < count * 4L) {
                throw new IOException("tensor too small " + name);
            }
            ByteBuffer buf = ByteBuffer.allocate(count * 4).order(ByteOrder.LITTLE_ENDIAN);
            long pos = base + span[0];
            while (buf.hasRemaining()) {
                int n = channel.read(buf, pos + buf.position());
                if (n < 0) {
                    throw new IOException("truncated tensor " + name);
                }
            }
            buf.flip();
            float[] out = new float[count];
            buf.asFloatBuffer().get(out);
            return out;
        }
    }
}
