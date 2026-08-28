"""Compare shots/orig/<page>.png vs shots/sart/<page>.png.
Prints % differing pixels per page and writes shots/diff/<page>.png
(orig | sart | diff-mask) for anything above threshold."""
import os, sys
from PIL import Image, ImageChops

base = os.path.dirname(os.path.abspath(__file__))
O, S, D = [os.path.join(base, "shots", d) for d in ("orig", "sart", "diff")]
os.makedirs(D, exist_ok=True)
pages = sys.argv[1:] or sorted(f[:-4] for f in os.listdir(O) if f.endswith(".png"))
for page in pages:
    po, ps = os.path.join(O, page + ".png"), os.path.join(S, page + ".png")
    if not (os.path.exists(po) and os.path.exists(ps)):
        print(f"{page:28s} MISSING {'orig' if not os.path.exists(po) else 'sart'}")
        continue
    a, b = Image.open(po).convert("RGB"), Image.open(ps).convert("RGB")
    if a.size != b.size:
        print(f"{page:28s} SIZE {a.size} vs {b.size}")
        continue
    diff = ImageChops.difference(a, b).convert("L").point(lambda v: 255 if v > 24 else 0)
    bbox = diff.getbbox()
    n = sum(1 for v in diff.getdata() if v)
    pct = 100.0 * n / (a.size[0] * a.size[1])
    print(f"{page:28s} {pct:6.2f}%  bbox={bbox}")
    if n:
        w, h = a.size
        out = Image.new("RGB", (w * 3 + 20, h), (255, 0, 255))
        out.paste(a, (0, 0)); out.paste(b, (w + 10, 0))
        mask = Image.merge("RGB", (diff, Image.new("L", a.size, 0), Image.new("L", a.size, 0)))
        out.paste(Image.blend(a, mask, 0.6), (2 * w + 20, 0))
        out.save(os.path.join(D, page + ".png"))
