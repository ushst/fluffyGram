#!/usr/bin/env python3
"""Precompute rubert-tiny2 embeddings for smart_reply_db.json contexts.

Output: assets/fluffy/smart_reply_embeddings.bin, read by CuratedEmbeddings.java.
Mirrors TinyBertEncoder/WordPieceTokenizer exactly (numpy, no torch).
Re-run whenever smart_reply_db.json changes.

Format (little endian): "FSRE", int32 version=1, int32 dim, int32 count,
then per entry: uint64 fnv1a64(clean(context) as UTF-8), float32 scale, int8[dim].
"""

from __future__ import annotations

import hashlib
import json
import math
import re
import struct
import unicodedata
import urllib.request
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
DB = ROOT / "TMessagesProj/src/main/assets/fluffy/smart_reply_db.json"
OUT = ROOT / "TMessagesProj/src/main/assets/fluffy/smart_reply_embeddings.bin"

# Keep in sync with EmbeddingModelStore.java.
REVISION = "e8ed3b0c8bbf4fb6984c3de043bf7d2f4e5969ae"
FILES = {
    "model.safetensors": "26ebb6db2a68593c54c74902d7a74f332da66297693f965cc9f1b0af4abf3894",
    "vocab.txt": "f056a69b097422652053bf87565c35543e5d81540ca4b7dddd28de4157a969e0",
}
CACHE = Path.home() / ".cache/fluffy" / f"rubert-tiny2-{REVISION[:7]}"

DIM, HEADS, LAYERS, MAX_LEN = 312, 12, 3, 64
W: dict[str, np.ndarray] = {}
TOK2ID: dict[str, int] = {}


def fetch_model() -> None:
    CACHE.mkdir(parents=True, exist_ok=True)
    for name, sha in FILES.items():
        path = CACHE / name
        if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != sha:
            url = f"https://huggingface.co/cointegrated/rubert-tiny2/resolve/{REVISION}/{name}"
            print("downloading", url)
            urllib.request.urlretrieve(url, path)
            if hashlib.sha256(path.read_bytes()).hexdigest() != sha:
                raise SystemExit(f"sha256 mismatch for {name}")


def load_model() -> None:
    with open(CACHE / "model.safetensors", "rb") as f:
        n = struct.unpack("<Q", f.read(8))[0]
        header = json.loads(f.read(n))
        base = 8 + n
        for key, meta in header.items():
            if key == "__metadata__" or meta["dtype"] != "F32":
                continue
            a, b = meta["data_offsets"]
            f.seek(base + a)
            W[key] = np.frombuffer(f.read(b - a), dtype="<f4").reshape(meta["shape"])
    for i, line in enumerate((CACHE / "vocab.txt").read_text(encoding="utf-8").split("\n")):
        TOK2ID.setdefault(line, i)


def clean(text: str) -> str:
    """SmartReplyText.clean — Java regex \\s/\\S are ASCII-only."""
    t = text.lower().strip()
    t = re.sub(r"https?://\S+|www\.\S+", " ", t, flags=re.ASCII)
    t = re.sub(r"[\r\n]+", " ", t)
    t = re.sub(r"\s+", " ", t, flags=re.ASCII).strip()
    return t


def is_punct(c: str) -> bool:
    cp = ord(c)
    if 33 <= cp <= 47 or 58 <= cp <= 64 or 91 <= cp <= 96 or 123 <= cp <= 126:
        return True
    return unicodedata.category(c).startswith("P")


def is_cjk(cp: int) -> bool:
    return (0x4E00 <= cp <= 0x9FFF or 0x3400 <= cp <= 0x4DBF or 0x20000 <= cp <= 0x2A6DF
            or 0x2A700 <= cp <= 0x2B73F or 0x2B740 <= cp <= 0x2B81F or 0x2B820 <= cp <= 0x2CEAF
            or 0xF900 <= cp <= 0xFAFF or 0x2F800 <= cp <= 0x2FA1F)


def basic_tokenize(text: str) -> list[str]:
    out = []
    for c in unicodedata.normalize("NFC", text):
        cp = ord(c)
        if cp in (0, 0xFFFD):
            continue
        if c in " \t\n\r" or unicodedata.category(c) == "Zs":
            out.append(" ")
        elif unicodedata.category(c) in ("Cc", "Cf"):
            continue
        elif is_cjk(cp):
            out.append(f" {c} ")
        else:
            out.append(c)
    words = []
    for chunk in "".join(out).split():
        cur = ""
        for c in chunk:
            if is_punct(c):
                if cur:
                    words.append(cur)
                    cur = ""
                words.append(c)
            else:
                cur += c
        if cur:
            words.append(cur)
    return words


def word_piece(word: str) -> list[int]:
    if len(word) > 100:
        return [TOK2ID["[UNK]"]]
    ids, start = [], 0
    while start < len(word):
        end, found = len(word), None
        while start < end:
            sub = word[start:end] if start == 0 else "##" + word[start:end]
            if sub in TOK2ID:
                found = TOK2ID[sub]
                break
            end -= 1
        if found is None:
            return [TOK2ID["[UNK]"]]
        ids.append(found)
        start = end
    return ids


def tokenize(text: str) -> list[int]:
    ids = [TOK2ID["[CLS]"]]
    for w in basic_tokenize(text):
        ids += word_piece(w)
        if len(ids) >= MAX_LEN - 1:
            break
    return ids[: MAX_LEN - 1] + [TOK2ID["[SEP]"]]


def layer_norm(x, g, b):
    m = x.mean(-1, keepdims=True)
    v = ((x - m) ** 2).mean(-1, keepdims=True)
    return (x - m) / np.sqrt(v + 1e-12) * g + b


_erf = np.vectorize(math.erf)


def linear(x, name):
    return x @ W[name + ".weight"].T + W[name + ".bias"]


def embed(text: str) -> np.ndarray:
    ids = tokenize(text)
    n, dh = len(ids), DIM // HEADS
    p = "bert.embeddings."
    x = W[p + "word_embeddings.weight"][ids] + W[p + "position_embeddings.weight"][:n] + W[p + "token_type_embeddings.weight"][0]
    x = layer_norm(x, W[p + "LayerNorm.weight"], W[p + "LayerNorm.bias"])
    for i in range(LAYERS):
        p = f"bert.encoder.layer.{i}."
        q, k, v = (linear(x, p + f"attention.self.{m}").reshape(n, HEADS, dh).transpose(1, 0, 2) for m in ("query", "key", "value"))
        s = q @ k.transpose(0, 2, 1) / math.sqrt(dh)
        s = np.exp(s - s.max(-1, keepdims=True))
        s /= s.sum(-1, keepdims=True)
        ctx = (s @ v).transpose(1, 0, 2).reshape(n, DIM)
        a = layer_norm(x + linear(ctx, p + "attention.output.dense"), W[p + "attention.output.LayerNorm.weight"], W[p + "attention.output.LayerNorm.bias"])
        h = linear(a, p + "intermediate.dense")
        h = 0.5 * h * (1 + _erf(h / math.sqrt(2)))
        x = layer_norm(a + linear(h, p + "output.dense"), W[p + "output.LayerNorm.weight"], W[p + "output.LayerNorm.bias"])
    cls = x[0]
    return cls / np.linalg.norm(cls)


def fnv1a64(text: str) -> int:
    h = 0xCBF29CE484222325
    for byte in text.encode("utf-8"):
        h ^= byte
        h = (h * 0x100000001B3) & 0xFFFFFFFFFFFFFFFF
    return h


def main() -> None:
    fetch_model()
    load_model()
    db = json.loads(DB.read_text(encoding="utf-8"))
    contexts = sorted({clean(p["context"]) for p in db.get("pairs", []) if clean(p.get("context", ""))})
    with open(OUT, "wb") as out:
        out.write(b"FSRE" + struct.pack("<iii", 1, DIM, len(contexts)))
        for i, ctx in enumerate(contexts):
            v = embed(ctx)
            scale = float(np.abs(v).max()) / 127.0 or 1.0
            q = np.clip(np.round(v / scale), -127, 127).astype("<i1")
            out.write(struct.pack("<Qf", fnv1a64(ctx), scale) + q.tobytes())
            if i % 500 == 0:
                print(f"{i}/{len(contexts)}")
    print(f"wrote {len(contexts)} vectors → {OUT.relative_to(ROOT)} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
