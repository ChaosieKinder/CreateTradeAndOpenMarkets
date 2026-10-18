"""Writes an empty structure template for gametests.

A gametest's template must cover everything the test builds: the runner only force-loads the
chunks the template intersects, so content outside it can land in an unloaded chunk.

    python tools/make-empty-template.py <name> <x> <y> <z>
"""
import gzip
import struct
import sys
from pathlib import Path

DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def int_list(values):
    return bytes([TAG_INT]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def compound_list(payloads):
    return bytes([TAG_COMPOUND]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def empty_list():
    return bytes([TAG_END]) + struct.pack(">i", 0)


def entry(tag_type, key, payload):
    return bytes([tag_type]) + name(key) + payload


def main():
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    template, *size = sys.argv[1], *map(int, sys.argv[2:])
    air = entry(TAG_STRING, "Name", name("minecraft:air")) + bytes([TAG_END])
    body = (
        entry(TAG_INT, "DataVersion", struct.pack(">i", DATA_VERSION))
        + entry(TAG_LIST, "size", int_list(size))
        + entry(TAG_LIST, "palette", compound_list([air]))
        + entry(TAG_LIST, "blocks", empty_list())
        + entry(TAG_LIST, "entities", empty_list())
        + bytes([TAG_END])
    )
    root = bytes([TAG_COMPOUND]) + name("") + body
    out = Path(__file__).resolve().parent.parent / "src/gametest/resources/data/erranttrade/structure" / f"{template}.nbt"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(gzip.compress(root, mtime=0))
    print(f"wrote {out.relative_to(Path.cwd()) if out.is_relative_to(Path.cwd()) else out}")


if __name__ == "__main__":
    main()
