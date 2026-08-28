"""Resolve `main.dart.js:LINE:COL` frames through main.dart.js.map.
    python3 decode_stack.py <map-file> "<stack text>"
"""
import json, re, sys, bisect

B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
IDX = {c: i for i, c in enumerate(B64)}


def vlq(seg):
    vals, shift, val = [], 0, 0
    for c in seg:
        d = IDX[c]
        val |= (d & 31) << shift
        if d & 32:
            shift += 5
        else:
            vals.append(-(val >> 1) if val & 1 else val >> 1)
            shift = val = 0
    return vals


def load(path):
    m = json.load(open(path))
    lines = []  # per generated line: sorted list of (gencol, srcidx, srcline, srccol)
    src = srcline = srccol = 0
    for gl in m["mappings"].split(";"):
        entries, gencol = [], 0
        for seg in filter(None, gl.split(",")):
            v = vlq(seg)
            gencol += v[0]
            if len(v) >= 4:
                src += v[1]; srcline += v[2]; srccol += v[3]
                entries.append((gencol, src, srcline, srccol))
        lines.append(entries)
    return m["sources"], lines


def resolve(sources, lines, line, col):
    if line - 1 >= len(lines):
        return None
    entries = lines[line - 1]
    cols = [e[0] for e in entries]
    i = bisect.bisect_right(cols, col - 1) - 1
    if i < 0:
        return None
    _, s, sl, sc = entries[i]
    return f"{sources[s]}:{sl + 1}:{sc + 1}"


if __name__ == "__main__":
    sources, lines = load(sys.argv[1])
    for m in re.finditer(r"main\.dart\.js:(\d+):(\d+)", sys.argv[2]):
        print(f"{m.group(0)} -> {resolve(sources, lines, int(m.group(1)), int(m.group(2)))}")
