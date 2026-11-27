"""Generates every widget theme: colours, backgrounds, layouts, and the WidgetStyle enum.

Two kinds of theme:
  * wallpaper themes  - colours from the Material You system palette (Android 12+), with
                        fixed fallbacks on older versions; each has follow-system/light/dark.
  * colour themes     - hand-picked fixed palettes that look the same in light and dark.

Decorations: "mark" (big quote mark beside the text), "stripe" (accent bar on the left),
"dot" (small accent dot in the top corner), or None.

Run from the repo root:  python tools/gen_widget_res.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from fonts import CATEGORIES, FONTS  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "app", "src", "main", "res")
KT = os.path.join(ROOT, "app", "src", "main", "java", "io", "github", "niravgrowlity", "quotewidget",
                  "WidgetStyle.kt")

# ---------------------------------------------------------------- wallpaper palette
# name: {mode: (pre-12 fallback, Android 12+ system colour)}
SYS = {
    "tonal_bg":   {"light": ("#DCE1FF", "system_accent1_100"), "dark": ("#2F3F6E", "system_accent1_700")},
    "tonal_text": {"light": ("#121B3D", "system_accent1_900"), "dark": ("#DCE1FF", "system_accent1_100")},
    "tonal_acc":  {"light": ("#3F5AA6", "system_accent1_600"), "dark": ("#B6C4FF", "system_accent1_200")},
    "paper_bg":   {"light": ("#FBF9FC", "system_neutral1_10"), "dark": ("#1B1B1F", "system_neutral1_900")},
    "paper_text": {"light": ("#1B1B1F", "system_neutral1_900"), "dark": ("#E4E2E6", "system_neutral1_100")},
    "paper_acc":  {"light": ("#3F5AA6", "system_accent1_500"), "dark": ("#B6C4FF", "system_accent1_200")},
    "bold_bg":    {"light": ("#3F5AA6", "system_accent1_600"), "dark": ("#B6C4FF", "system_accent1_200")},
    "bold_text":  {"light": ("#FFFFFF", "system_accent1_0"),   "dark": ("#0F2357", "system_accent1_800")},
}

# ---------------------------------------------------------------- themes
# bg: None | ("solid", colour) | ("gradient", [colours...])
# Colours are SYS names for wallpaper themes, or "#RRGGBB" for colour themes.
THEMES = [
    # --- match the wallpaper ---
    dict(enum="EDITORIAL", label="Editorial", wallpaper=True, bg=("solid", "paper_bg"), fg="paper_text",
         acc="paper_acc", font="serif", deco="mark"),
    dict(enum="TONAL", label="Tonal", wallpaper=True, bg=("solid", "tonal_bg"), fg="tonal_text",
         acc="tonal_acc", font="sans-serif-medium", deco="stripe"),
    dict(enum="BOLD", label="Bold", wallpaper=True, bg=("solid", "bold_bg"), fg="bold_text",
         acc="bold_text", font="sans-serif", bold=True),
    dict(enum="CLEAR", label="No background", wallpaper=True, bg=None, fg="#FFFFFF", acc="#FFFFFF",
         font="sans-serif-medium", shadow=True),
    # --- palettes from popular open-source colour schemes (official hex values, MIT licensed) ---
    dict(enum="CATPPUCCIN_LATTE", label="Catppuccin Latte", group="palette", bg=("solid", "#EFF1F5"),
         fg="#4C4F69", acc="#8839EF", font="sans-serif-medium", deco="mark"),
    dict(enum="CATPPUCCIN_MOCHA", label="Catppuccin Mocha", group="palette", bg=("solid", "#1E1E2E"),
         fg="#CDD6F4", acc="#CBA6F7", font="sans-serif-medium", deco="mark"),
    dict(enum="ROSE_PINE", label="Rosé Pine", group="palette", bg=("solid", "#191724"),
         fg="#E0DEF4", acc="#EBBCBA", font="serif", deco="stripe"),
    dict(enum="ROSE_PINE_DAWN", label="Rosé Pine Dawn", group="palette", bg=("solid", "#FAF4ED"),
         fg="#575279", acc="#D7827E", font="serif", deco="stripe"),
    dict(enum="NORD", label="Nord", group="palette", bg=("solid", "#2E3440"),
         fg="#ECEFF4", acc="#88C0D0", font="sans-serif-medium", deco="stripe"),
    dict(enum="NORD_SNOW", label="Nord Snow", group="palette", bg=("solid", "#ECEFF4"),
         fg="#2E3440", acc="#5E81AC", font="sans-serif-medium", deco="stripe"),
    dict(enum="GRUVBOX", label="Gruvbox", group="palette", bg=("solid", "#282828"),
         fg="#EBDBB2", acc="#FABD2F", font="serif", deco="mark"),
    dict(enum="GRUVBOX_LIGHT", label="Gruvbox Light", group="palette", bg=("solid", "#FBF1C7"),
         fg="#3C3836", acc="#AF3A03", font="serif", deco="mark"),
    dict(enum="DRACULA", label="Dracula", group="palette", bg=("solid", "#282A36"),
         fg="#F8F8F2", acc="#FF79C6", font="monospace", deco="dot"),
    dict(enum="SOLARIZED_LIGHT", label="Solarized Light", group="palette", bg=("solid", "#FDF6E3"),
         fg="#586E75", acc="#CB4B16", font="serif", deco="stripe"),
    dict(enum="SOLARIZED_DARK", label="Solarized Dark", group="palette", bg=("solid", "#002B36"),
         fg="#93A1A1", acc="#B58900", font="serif", deco="stripe"),
    dict(enum="TOKYO_NIGHT", label="Tokyo Night", group="palette", bg=("solid", "#1A1B26"),
         fg="#C0CAF5", acc="#7AA2F7", font="monospace", deco="dot"),
    dict(enum="EVERFOREST", label="Everforest", group="palette", bg=("solid", "#2D353B"),
         fg="#D3C6AA", acc="#A7C080", font="serif", deco="mark"),
    # More open-source schemes, colours read from each project's own theme file (all MIT).
    dict(enum="SYNTHWAVE", label="Synthwave ’84", group="palette", bg=("solid", "#262335"),
         fg="#FFFFFF", acc="#FF7EDB", font="sans-serif-medium", deco="stripe"),
    dict(enum="COBALT2", label="Cobalt2", group="palette", bg=("solid", "#193549"),
         fg="#FFFFFF", acc="#FFC600", font="sans-serif-medium", deco="mark"),
    dict(enum="AYU_LIGHT", label="Ayu Light", group="palette", bg=("solid", "#FCFCFC"),
         fg="#5C6166", acc="#F29718", font="sans-serif-medium", deco="stripe"),
    dict(enum="NIGHT_OWL", label="Night Owl", group="palette", bg=("solid", "#011627"),
         fg="#D6DEEB", acc="#C792EA", font="monospace", deco="dot"),
    # --- bright: loud, happy colours, some on a two-colour gradient ---
    dict(enum="SUNRISE", label="Sunrise", group="bright", bg=("gradient", ["#FFB36B", "#FF7A93"]),
         fg="#40101F", acc="#FFFFFF", font="sans-serif-medium", deco="dot"),
    dict(enum="LEMONADE", label="Lemonade", group="bright", bg=("gradient", ["#FFF27A", "#FFC93C"]),
         fg="#3A2A00", acc="#FF6B3D", font="serif", deco="mark"),
    dict(enum="BUBBLEGUM", label="Bubblegum", group="bright", bg=("solid", "#FFC2E2"),
         fg="#4A0E33", acc="#FF3D9A", font="sans-serif-medium", deco="stripe"),
    dict(enum="MINT_SODA", label="Mint Soda", group="bright", bg=("gradient", ["#B8F5D8", "#7FE3F0"]),
         fg="#0B3B3A", acc="#00A389", font="sans-serif-medium", deco="dot"),
    dict(enum="BLUE_SKY", label="Blue Sky", group="bright", bg=("gradient", ["#9BD4FF", "#C9B8FF"]),
         fg="#1B2259", acc="#4B56F0", font="serif", deco="mark"),
    dict(enum="WATERMELON", label="Watermelon", group="bright", bg=("gradient", ["#FF8FA3", "#FFC9A6"]),
         fg="#4A0B1C", acc="#14804A", font="serif", deco="stripe"),
    dict(enum="TANGERINE", label="Tangerine", group="bright", bg=("solid", "#FF9A3D"),
         fg="#3A1700", acc="#FFFFFF", font="sans-serif", bold=True),
    dict(enum="GRAPE_SODA", label="Grape Soda", group="bright", bg=("gradient", ["#7B4DFF", "#A23DE0"]),
         fg="#FFFFFF", acc="#FFD23F", font="sans-serif-medium", deco="stripe"),
    # --- originals ---
    dict(enum="MATCHA", label="Matcha", bg=("solid", "#DCE6CF"), fg="#24361F", acc="#5F8A44",
         font="serif", deco="mark"),
    dict(enum="LILAC", label="Lilac", bg=("solid", "#ECE4FF"), fg="#2F2059", acc="#7C5CFA",
         font="sans-serif-medium", deco="stripe"),
    dict(enum="APRICOT", label="Apricot", bg=("solid", "#FFE2CF"), fg="#4F240C", acc="#F2711C",
         font="serif", deco="stripe"),
    dict(enum="CITRUS", label="Citrus", bg=("solid", "#FFF0A3"), fg="#332B00", acc="#E0A800",
         font="sans-serif-medium", deco="dot"),
    dict(enum="MIDNIGHT", label="Midnight", bg=("solid", "#111A36"), fg="#F4EDE1", acc="#E8B04B",
         font="serif", deco="mark"),
    dict(enum="MONO", label="Mono", bg=("solid", "#000000"), fg="#FFFFFF", acc="#FF3B30",
         font="monospace", deco="dot"),
    # --- the user's own photo; colours are worked out from the photo at runtime ---
    dict(enum="PHOTO", label="Your photo", group="photo", bg=None, fg="#FFFFFF", acc="#FFFFFF",
         font="sans-serif-medium", photo=True),
]
MODES = ["", "_light", "_dark"]  # order must match ThemeMode: SYSTEM, LIGHT, DARK
DECO_START = {"mark": 30, "stripe": 18}  # dp the text is pushed right by

PHOTO_VIEW = """
    <!-- The photo, sized and finished by PhotoBackground; centerCrop so a size guess that's
         slightly off crops a little instead of stretching -->
    <ImageView
        android:id="@+id/widget_bg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop"
        android:importantForAccessibility="no" />
"""


def write(path, body):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n", encoding="utf-8") as f:
        f.write(body)


def res(rel, body):
    write(os.path.join(RES, rel), body)


def resources(lines):
    return "<resources>\n" + "".join(f"    {l}\n" for l in lines) + "</resources>\n"


def color(name, value):
    return f'<color name="{name}">{value}</color>'


def ref(c, mode):
    """Colour reference for a theme colour in a given mode."""
    return c if c.startswith("#") else f"@color/{c}{mode}"


# ---------------------------------------------------------------- colours
lines = []
for n, v in SYS.items():
    lines += [color(f"{n}_light", v["light"][0]), color(f"{n}_dark", v["dark"][0])]
lines += [color(n, f"@color/{n}_light") for n in SYS]
res("values/colors.xml", resources(lines))
res("values-night/colors.xml", resources([color(n, f"@color/{n}_dark") for n in SYS]))
lines = []
for n, v in SYS.items():
    lines += [color(f"{n}_light", f"@android:color/{v['light'][1]}"),
              color(f"{n}_dark", f"@android:color/{v['dark'][1]}")]
res("values-v31/colors.xml", resources(lines))

# Remove previously generated files so renamed themes don't leave stale resources behind.
for folder, prefixes in [("layout", ("widget_",)), ("drawable", ("wbg_", "wdeco_"))]:
    d = os.path.join(RES, folder)
    for f in os.listdir(d):
        if f.startswith(prefixes):
            os.remove(os.path.join(d, f))
old_kt = os.path.join(os.path.dirname(KT), "WidgetLayouts.kt")
if os.path.exists(old_kt):
    os.remove(old_kt)

# ---------------------------------------------------------------- templates
SOLID = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{c}" />
    <corners android:radius="@dimen/widget_radius" />
</shape>
"""
GRADIENT = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="315" {stops} />
    <corners android:radius="@dimen/widget_radius" />
</shape>
"""
STRIPE = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="{c}" />
    <corners android:radius="2dp" />
</shape>
"""
DOT = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="{c}" />
</shape>
"""
HARD_SHADOW = ('\n{i}android:shadowColor="#B3000000"\n{i}android:shadowRadius="8"'
               '\n{i}android:shadowDx="0"\n{i}android:shadowDy="1"')
SOFT_SHADOW = ('\n{i}android:shadowColor="#33000000"\n{i}android:shadowRadius="4"'
               '\n{i}android:shadowDx="0"\n{i}android:shadowDy="1"')

MAIN = """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/gen_widget_res.py -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/background"
    android:layout_width="match_parent"
    android:layout_height="match_parent"{bg}>
{photo}
    <!-- Optional decoration (Widget > Layout): corner art behind the quote, in the accent colour -->
    <ImageView
        android:id="@+id/widget_deco"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitEnd"
        android:importantForAccessibility="no"{deco_tint}
        android:visibility="gone" />

<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="18dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:orientation="horizontal">
{stripe}
        <FrameLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1">
{mark}{dot}
            <!-- Normal mode: quote auto-fitted, author underneath -->
            <LinearLayout
                android:id="@+id/widget_body"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:paddingStart="{indent}dp"
                android:gravity="center_vertical"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/widget_text"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_horizontal"
                    android:fontFamily="{font}"
                    android:lineSpacingMultiplier="1.12"
                    android:textColor="{fg}"
                    android:textSize="18sp"{text_extra} />

                <!-- Library fonts: the quote drawn by WidgetRenderer as an image (launchers
                     can't load an app's fonts), tinted with the theme's text colour -->
                <ImageView
                    android:id="@+id/widget_text_img"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:adjustViewBounds="true"
                    android:scaleType="fitCenter"{tint}
                    android:visibility="gone" />

                <TextView
                    android:id="@+id/widget_author"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:gravity="center_horizontal"
                    android:ellipsize="end"
                    android:maxLines="1"
                    android:fontFamily="sans-serif-medium"
                    android:textColor="{acc}"
                    android:textSize="12sp"
                    android:visibility="gone"{small_extra} />
            </LinearLayout>

            <!-- Long-quote mode: scrolls, used when the quote won't fit at a readable size -->
            <ListView
                android:id="@+id/widget_list"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:layout_marginStart="{indent}dp"
                android:divider="@null"
                android:fadeScrollbars="false"
                android:scrollbarStyle="outsideOverlay"
                android:scrollbars="vertical"
                android:visibility="gone" />
        </FrameLayout>
    </LinearLayout>

    <!-- Foot: the reload button (shown when nothing is pinned) and the corner text -->
    <LinearLayout
        android:id="@+id/widget_foot"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingTop="4dp">

        <ImageView
            android:id="@+id/widget_reload"
            android:layout_width="30dp"
            android:layout_height="22dp"
            android:paddingEnd="8dp"
            android:alpha="0.65"
            android:contentDescription="@string/widget_reload"
            android:scaleType="fitStart"
            android:src="@drawable/ic_widget_reload"{reload_tint}
            android:visibility="gone" />

        <TextView
            android:id="@+id/widget_meta"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="end"
            android:alpha="0.65"
            android:fontFamily="sans-serif-medium"
            android:textColor="{fg}"
            android:textSize="11sp"{meta_extra} />
    </LinearLayout>
</LinearLayout>
</FrameLayout>
"""
MARK = """
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="top|start"
                android:layout_marginTop="-6dp"
                android:fontFamily="serif"
                android:includeFontPadding="false"
                android:text="“"
                android:textColor="{c}"
                android:textSize="46sp" />
"""
STRIPE_VIEW = """
        <ImageView
            android:layout_width="4dp"
            android:layout_height="match_parent"
            android:layout_marginEnd="14dp"
            android:background="@drawable/{d}"
            android:importantForAccessibility="no" />
"""
DOT_VIEW = """
            <ImageView
                android:layout_width="8dp"
                android:layout_height="8dp"
                android:layout_gravity="top|end"
                android:background="@drawable/{d}"
                android:importantForAccessibility="no" />
"""
ITEM = """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/gen_widget_res.py -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_item"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingEnd="8dp"
    android:paddingBottom="4dp">

    <TextView
        android:id="@+id/widget_item_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:fontFamily="{font}"
        android:lineSpacingMultiplier="1.12"
        android:textColor="{fg}"
        android:textSize="13sp"{extra} />

    <ImageView
        android:id="@+id/widget_item_img"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:adjustViewBounds="true"
        android:scaleType="fitCenter"{tint}
        android:visibility="gone" />
</FrameLayout>
"""

# ---------------------------------------------------------------- decorations
# Corner art for the optional widget decoration (the Decoration enum in QuoteStore.kt). White
# shapes on a 100x100 canvas, anchored bottom-end by the layout (scaleType fitEnd) and tinted
# with the theme's accent. Kept faint so the quote stays readable where they overlap.
def _n(v):
    return f"{round(v, 1):g}"


def _circle(cx, cy, r):
    return f"M{_n(cx - r)},{_n(cy)}a{_n(r)},{_n(r)} 0 1,0 {_n(2 * r)},0a{_n(r)},{_n(r)} 0 1,0 {_n(-2 * r)},0z"


def _star(cx, cy, r):
    """Four-point sparkle."""
    return (f"M{cx},{cy - r}Q{cx},{cy} {cx + r},{cy}Q{cx},{cy} {cx},{cy + r}"
            f"Q{cx},{cy} {cx - r},{cy}Q{cx},{cy} {cx},{cy - r}Z")


def _fill(d, a):
    return f'    <path android:fillColor="#FFFFFFFF" android:fillAlpha="{a}" android:pathData="{d}" />'


def _stroke(d, a, w):
    return (f'    <path android:strokeColor="#FFFFFFFF" android:strokeAlpha="{a}" android:strokeWidth="{w}"'
            f' android:strokeLineCap="round" android:pathData="{d}" />')


HEART = ("M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09"
         "C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z")


def _heart(cx, cy, scale, rot, a):
    return (f'    <group android:pivotX="12" android:pivotY="12" android:scaleX="{scale}" android:scaleY="{scale}"'
            f' android:rotation="{rot}" android:translateX="{cx - 12}" android:translateY="{cy - 12}">\n'
            f'    {_fill(HEART, a)}\n    </group>')


def _flower(cx, cy, petal, reach):
    out = [_fill(_circle(cx + reach * math.cos(math.radians(a)), cy + reach * math.sin(math.radians(a)), petal), 0.35)
           for a in range(-90, 270, 72)]
    return out + [_fill(_circle(cx, cy, petal * 0.6), 0.7)]


def _rays(cx, cy, r0, r1):
    out = []
    for a in (188, 206, 225, 244, 262):
        c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
        out.append(_stroke(f"M{_n(cx + r0 * c)},{_n(cy + r0 * s)}L{_n(cx + r1 * c)},{_n(cy + r1 * s)}", 0.4, 3.5))
    return out


DECOS = {
    "sparkles": [_fill(_star(x, y, r), a) for x, y, r, a in
                 [(78, 18, 13, 0.6), (93, 40, 5, 0.5), (60, 8, 5, 0.45), (88, 84, 9, 0.5), (72, 94, 4, 0.4)]],
    "confetti": [_fill(_circle(x, y, r), 0.55) for x, y, r in
                 [(70, 10, 2.5), (90, 22, 3), (96, 50, 2), (82, 90, 3), (60, 96, 2.5), (94, 78, 2)]]
                + [_stroke(f"M{x},{y}l{dx},{dy}", 0.5, 3) for x, y, dx, dy in
                   [(58, 20, 4, 3), (84, 36, -3, 5), (96, 64, -5, 2), (72, 84, 5, -3), (90, 97, 3, -4), (80, 6, -4, 2)]],
    "hearts": [_heart(82, 18, 1.1, 15, 0.5), _heart(93, 44, 0.5, -12, 0.4), _heart(86, 86, 0.8, -15, 0.45)],
    "flowers": _flower(84, 86, 7, 9) + _flower(86, 14, 4, 5),
    "sun": [_fill(_circle(100, 100, 30), 0.3)] + _rays(100, 100, 38, 50),
    "rainbow": [_stroke(_circle(100, 100, r), a, 7) for r, a in [(20, 0.5), (31, 0.35), (42, 0.22)]],
    "night": [_fill(_circle(82, 18, 11), 0.35)] + [_fill(_star(x, y, r), a) for x, y, r, a in
              [(58, 10, 4, 0.5), (94, 44, 3, 0.45), (66, 30, 2.5, 0.4), (90, 88, 5, 0.45)]],
    "bubbles": [_fill(_circle(x, y, r), a) for x, y, r, a in [(90, 12, 24, 0.16), (98, 94, 32, 0.13), (62, 100, 12, 0.2)]],
}
for deco_name, paths in DECOS.items():
    res(f"drawable/deco_{deco_name}.xml",
        '<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by tools/gen_widget_res.py -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="100dp"\n    android:height="100dp"\n'
        '    android:viewportWidth="100"\n    android:viewportHeight="100">\n'
        + "\n".join(paths) + "\n</vector>\n")

# ---------------------------------------------------------------- generate
enum_rows = []
unique_items = set()
for t in THEMES:
    key = t["enum"].lower()
    modes = MODES if t.get("wallpaper") else [""]  # colour themes look the same in light and dark
    bold = t.get("bold", False)
    shadow = HARD_SHADOW if t.get("shadow") else SOFT_SHADOW if t.get("soft_shadow") else ""
    deco = t.get("deco")
    indent = DECO_START.get("mark", 0) if deco == "mark" else 0
    main_ids, item_ids = [], []
    for mode in modes:
        name = f"{key}{mode}"
        fg, acc = ref(t["fg"], mode), ref(t["acc"], mode)

        bg = ""
        if t["bg"] and t["bg"][0] == "solid":
            res(f"drawable/wbg_{name}.xml", SOLID.format(c=ref(t["bg"][1], mode)))
            bg = f'\n    android:background="@drawable/wbg_{name}"'
        elif t["bg"]:
            cols = [ref(c, mode) for c in t["bg"][1]]
            stops = f'android:startColor="{cols[0]}"'
            if len(cols) == 3:
                stops += f' android:centerColor="{cols[1]}"'
            stops += f' android:endColor="{cols[-1]}"'
            res(f"drawable/wbg_{name}.xml", GRADIENT.format(stops=stops))
            bg = f'\n    android:background="@drawable/wbg_{name}"'

        stripe = mark = dot = ""
        if deco == "stripe":
            res(f"drawable/wdeco_{name}.xml", STRIPE.format(c=acc))
            stripe = STRIPE_VIEW.format(d=f"wdeco_{name}")
        elif deco == "dot":
            res(f"drawable/wdeco_{name}.xml", DOT.format(c=acc))
            dot = DOT_VIEW.format(d=f"wdeco_{name}")
        elif deco == "mark":
            mark = MARK.format(c=acc)

        bold_attr = '\n{i}android:textStyle="bold"' if bold else ""
        # Shadow themes draw the quote image in its final colour (with the shadow), so no tint.
        # The photo style sets its colours at runtime (they come from the photo), so no tint either.
        tint = "" if shadow or t.get("photo") else f'\n{{i}}android:tint="{fg}"'
        main_xml = MAIN.format(
            bg=bg, fg=fg, acc=acc, font=t["font"], indent=indent,
            stripe=stripe, mark=mark, dot=dot, tint=tint.format(i=" " * 20),
            text_extra=(bold_attr + shadow).format(i=" " * 20),
            small_extra=shadow.format(i=" " * 20),
            # The photo style colours the reload icon at runtime, like its text.
            reload_tint="" if t.get("photo") else f'\n            android:tint="{fg}"',
            photo=PHOTO_VIEW if t.get("photo") else "",
            deco_tint="" if t.get("photo") else f'\n        android:tint="{acc}"',
            meta_extra=shadow.format(i=" " * 12))
        res(f"layout/widget_{name}.xml", main_xml)
        res(f"layout/widget_item_{name}.xml", ITEM.format(
            fg=fg, font=t["font"], extra=(bold_attr + shadow).format(i=" " * 8),
            tint=tint.format(i=" " * 8)))
        main_ids.append(f"R.layout.widget_{name}")
        item_ids.append(f"R.layout.widget_item_{name}")
        unique_items.add(name)

    if len(main_ids) == 1:
        main_ids *= 3
        item_ids *= 3
    start = DECO_START.get(deco, 0) if deco in DECO_START else 0
    enum_rows.append(
        f'    {t["enum"]}(\n'
        f'        "{t["label"]}", StyleGroup.{"WALLPAPER" if t.get("wallpaper") else t.get("group", "original").upper()}, '
        f'family = "{t["font"]}", bold = {str(bold).lower()}, startDp = {start}f, '
        f'leftAligned = {str(deco in ("mark", "stripe")).lower()}, shadow = {str(bool(shadow)).lower()}, photo = {str(bool(t.get("photo"))).lower()},\n'
        f'        layouts = intArrayOf({", ".join(main_ids)}),\n'
        f'        items = intArrayOf({", ".join(item_ids)}),\n'
        f'    )'
    )

write(KT, f"""package io.github.niravgrowlity.quotewidget

import android.graphics.Typeface

// Generated by tools/gen_widget_res.py. Do not edit by hand.

/** How themes are grouped in the style picker, in display order. */
enum class StyleGroup(val title: String) {{
    BRIGHT("Bright"),
    PALETTE("Classic palettes"),
    ORIGINAL("Originals"),
    WALLPAPER("Match my wallpaper"),
    PHOTO("Your photo"),
}}

/**
 * Widget themes. WALLPAPER themes take colours from the wallpaper and have light/dark
 * variants; the others are fixed palettes. [family], [bold] and [startDp] mirror the
 * generated layouts so the auto-fit measures text exactly as it will be drawn.
 */
enum class WidgetStyle(
    val label: String,
    val group: StyleGroup,
    val family: String,
    val bold: Boolean,
    val startDp: Float,
    /** Styles designed around a left edge (quote mark, stripe) always align left. */
    val leftAligned: Boolean,
    /**
     * Text has a drop shadow (styles without a background). Library-font text is then drawn
     * white with a shadow; otherwise it's drawn as a mask that the layout tints.
     */
    val shadow: Boolean,
    /** Background is the user's photo (PhotoBackground); text colours are set at runtime. */
    val photo: Boolean,
    private val layouts: IntArray,
    private val items: IntArray,
) {{
{",\n".join(enum_rows)};

    val wallpaper: Boolean get() = group == StyleGroup.WALLPAPER

    fun layout(mode: ThemeMode) = layouts[mode.ordinal]
    fun item(mode: ThemeMode) = items[mode.ordinal]
    fun typeface(): Typeface = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)

    companion object {{
        /** Number of distinct scroll-mode item layouts; the widget list adapter needs it. */
        const val ITEM_VIEW_TYPES = {len(unique_items)}
    }}
}}
""")
old_colors = os.path.join(RES, "values", "widget_text_colors.xml")
if os.path.exists(old_colors):
    os.remove(old_colors)

# ---------------------------------------------------------------- fonts
font_rows = [f'    {enum}("{label}", FontCategory.{cat}, R.font.qf_{enum.lower()})'
             for enum, label, family, weight, italic, cat in FONTS]
cats = ",\n".join(f'    {k}("{t}")' for k, t in CATEGORIES)
rows = ",\n".join(font_rows)
write(os.path.join(os.path.dirname(KT), "QuoteFont.kt"), f"""package io.github.niravgrowlity.quotewidget

import android.content.Context
import android.graphics.Typeface

// Generated by tools/gen_widget_res.py from tools/fonts.py. Do not edit by hand.

enum class FontCategory(val title: String) {{
{cats},
}}

/**
 * Fonts for the quote text. [THEME] keeps each widget style's own system font. The others are
 * Google Fonts in res/font (licences: FONT_LICENSES.txt). Launchers can't load an app's
 * fonts, so WidgetRenderer draws quotes in these fonts as an image.
 */
enum class QuoteFont(
    val label: String,
    val category: FontCategory?,
    private val font: Int,
) {{
    THEME("Theme font", null, 0),
{rows};

    /**
     * The bundled typeface, loaded once per process. Null for [THEME], and also null if the font
     * can't be loaded, so callers fall back to the theme font instead of crashing.
     */
    fun typeface(ctx: Context): Typeface? {{
        if (font == 0) return null
        cache[this]?.let {{ return it }}
        return runCatching {{ ctx.resources.getFont(font) }}
            .onFailure {{ android.util.Log.w("QuoteWidget", "font $name failed to load", it) }}
            .getOrNull()
            ?.also {{ cache[this] = it }}
    }}

    /** Whether this font can be used. THEME always can; a broken font is treated as THEME. */
    fun usable(ctx: Context) = this == THEME || typeface(ctx) != null

    private companion object {{
        val cache = java.util.concurrent.ConcurrentHashMap<QuoteFont, Typeface>()
    }}
}}
""")
print(f"generated {len(THEMES)} themes, {len(unique_items)} layouts, {len(FONTS)} fonts")
