import os
import zlib
import bz2
import argparse
import struct

def offtout(x):
    y = x if x >= 0 else -x
    buf = bytearray(8)
    buf[0] = y % 256; y //= 256
    buf[1] = y % 256; y //= 256
    buf[2] = y % 256; y //= 256
    buf[3] = y % 256; y //= 256
    buf[4] = y % 256; y //= 256
    buf[5] = y % 256; y //= 256
    buf[6] = y % 256; y //= 256
    buf[7] = y % 256; y //= 256
    if x < 0: buf[7] |= 0x80
    return buf

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("old_apk")
    parser.add_argument("new_apk")
    parser.add_argument("output_patch")
    args = parser.parse_args()

    # Step 1: Use standard bsdiff tool to generate a temporary bzip2 patch
    # On GitHub Actions (Ubuntu), bsdiff is pre-installed or can be apt-installed.
    tmp_patch = args.output_patch + ".tmp"
    try:
        subprocess.run(["bsdiff", args.old_apk, args.new_apk, tmp_patch], check=True)
    except:
        # Fallback if bsdiff is not installed - this is a simplified python version 
        # (Note: real bsdiff is complex, here we assume it's available in CI environment)
        print("Error: bsdiff tool not found. Please install it (sudo apt install bsdiff).")
        return

    # Step 2: Convert standard bzip2 bsdiff to our zlib-based BSZ40 format
    # Standard format: 32 byte header + bzip2(control) + bzip2(diff) + bzip2(extra)
    with open(tmp_patch, "rb") as f:
        header = f.read(32)
        if header[:8] != b"BSDIFF40":
            print("Invalid bsdiff patch")
            return
        
        # Read lengths from bzip2 header
        # offtin implementation in python:
        def offtin(buf):
            y = buf[7] & 0x7F
            for i in range(6, -1, -1):
                y = y * 256 + buf[i]
            if buf[7] & 0x80: y = -y
            return y

        ctrl_len = offtin(header[8:16])
        diff_len = offtin(header[16:24])
        new_size = offtin(header[24:32])

        # Decompress bzip2 blocks
        # We need to know where they end. bzip2 doesn't store length, 
        # but bsdiff format says:
        # 32...32+ctrl_len: control
        # 32+ctrl_len...32+ctrl_len+diff_len: diff
        # 32+ctrl_len+diff_len...end: extra
        
        f.seek(32)
        ctrl_bz2 = f.read(ctrl_len)
        diff_bz2 = f.read(diff_len)
        extra_bz2 = f.read()

        ctrl_raw = bz2.decompress(ctrl_bz2)
        diff_raw = bz2.decompress(diff_bz2)
        extra_raw = bz2.decompress(extra_bz2)

        # Re-compress with zlib
        ctrl_z = zlib.compress(ctrl_raw)
        diff_z = zlib.compress(diff_raw)
        extra_z = zlib.compress(extra_raw)

        # Write new header and blocks
        with open(args.output_patch, "wb") as out:
            new_header = bytearray(32)
            new_header[0:5] = b"BSZ40"
            new_header[8:16] = offtout(len(ctrl_z))
            new_header[16:24] = offtout(len(diff_z))
            new_header[24:32] = offtout(new_size)
            out.write(new_header)
            out.write(ctrl_z)
            out.write(diff_z)
            out.write(extra_z)

    os.remove(tmp_patch)
    print(f"Generated delta patch: {args.output_patch} ({os.path.getsize(args.output_patch)} bytes)")

import subprocess
if __name__ == "__main__":
    main()
