"""Downloads the widget font library from Google Fonts into res/font.

Run from the repo root:  python tools/fetch_fonts.py   (needs: pip install fonttools)
Then run tools/gen_widget_res.py, which writes the font layouts and QuoteFont.kt.

Each font is the static TTF that the Google Fonts css2 API serves. Fonts whose licence allows
it are trimmed to Latin + common punctuation to keep the APK small. Families with an OFL
Reserved Font Name are shipped unmodified, because the OFL forbids modifying those under the
same name. Characters a font doesn't have (other scripts, emoji) fall back to the system font.
"""
import io
import os
import re
import sys
import urllib.request

from fontTools import subset
from fontTools.ttLib import TTFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from fonts import FONTS  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")
LICENSES = os.path.join(ROOT, "FONT_LICENSES.txt")

# Latin, Latin-1, Latin Extended A/B, general punctuation (curly quotes, dashes, ellipsis),
# currency and a few symbols people put in quotes.
UNICODES = "U+0020-007E,U+00A0-024F,U+2000-206F,U+20A0-20CF,U+2100-2122,U+2190-2193,U+2212,U+2665"

UA = "Mozilla/5.0 (Windows NT 6.1; rv:10.0) Gecko/20100101 Firefox/10.0"  # gets plain TTF urls


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def font_url(family, weight, italic):
    spec = f"ital,wght@{1 if italic else 0},{weight}"
    css = get(f"https://fonts.googleapis.com/css2?family={family.replace(' ', '+')}:{spec}").decode()
    urls = re.findall(r"url\((https://[^)]+)\)", css)
    if not urls:
        raise SystemExit(f"no font url for {family}: {css[:200]}")
    return urls[0]


def license_text(family):
    d = family.lower().replace(" ", "")
    for path in (f"ofl/{d}/OFL.txt", f"apache/{d}/LICENSE.txt", f"ufl/{d}/UFL.txt"):
        try:
            return path.split("/")[0].upper(), get(f"https://raw.githubusercontent.com/google/fonts/main/{path}").decode("utf-8", "replace")
        except Exception:
            continue
    raise SystemExit(f"no license found for {family}")


os.makedirs(FONT_DIR, exist_ok=True)
for f in os.listdir(FONT_DIR):
    if f.startswith("qf_"):
        os.remove(os.path.join(FONT_DIR, f))

opts = subset.Options()
opts.layout_features = ["*"]  # keep ligatures and contextual alternates (scripts need them)
opts.name_IDs = ["*"]
opts.notdef_outline = True
opts.hinting = False  # hinting barely matters on high-dpi screens and is big
unicodes = subset.parse_unicodes(UNICODES)

total = 0
licenses = {}
for enum, label, family, weight, italic, cat in FONTS:
    if family not in licenses:
        licenses[family] = license_text(family)
    kind, text = licenses[family]
    reserved = "Reserved Font Name" in text[:1500]
    data = get(font_url(family, weight, italic))
    out = os.path.join(FONT_DIR, f"qf_{enum.lower()}.ttf")
    if reserved:
        with open(out, "wb") as fh:
            fh.write(data)
    else:
        font = TTFont(io.BytesIO(data))
        s = subset.Subsetter(opts)
        s.populate(unicodes=unicodes)
        s.subset(font)
        font.save(out)
    size = os.path.getsize(out)
    total += size
    print(f"{label:22s} {size // 1024:4d} KB{'  (unmodified: reserved name)' if reserved else ''}")

with open(LICENSES, "w", encoding="utf-8", newline="\n") as fh:
    fh.write("Licences for the fonts in app/src/main/res/font (qf_*.ttf), from Google Fonts.\n\n")
    for family, (kind, text) in licenses.items():
        fh.write(f"==== {family} ({kind}) ====\n\n{text.strip()}\n\n")
print(f"total {total // 1024} KB in {len(FONTS)} fonts")
