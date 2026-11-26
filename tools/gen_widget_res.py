"""Generates every widget theme: colours, backgrounds, layouts, and the WidgetStyle enum.

Two kinds of theme:
  * wallpaper themes  - colours from the Material You system palette (Android 12+), with
                        fixed fallbacks on older versions; each has follow-system/light/dark.
  * colour themes     - hand-picked fixed palettes that look the same in light and dark.

Decorations: "mark" (big quote mark beside the text), "stripe" (accent bar on the left),
"dot" (small accent dot in the top corner), or None.

Run from the repo root:  python tools/gen_widget_res.py
"""
import os

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
    "grad_a":     {"light": ("#3F5AA6", "system_accent1_500"), "dark": ("#2F3F6E", "system_accent1_700")},
    "grad_b":     {"light": ("#8A4F7D", "system_accent3_500"), "dark": ("#6A3A60", "system_accent3_700")},
    "grad_text":  {"light": ("#FFFFFF", "system_accent1_0"),   "dark": ("#FFFFFF", "system_accent1_0")},
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
    dict(enum="GRADIENT", label="Gradient", wallpaper=True, bg=("gradient", ["grad_a", "grad_b"]),
         fg="grad_text", acc="grad_text", font="sans-serif-medium", soft_shadow=True),
    dict(enum="CLEAR", label="No background", wallpaper=True, bg=None, fg="#FFFFFF", acc="#FFFFFF",
         font="sans-serif-medium", shadow=True),
    # --- colour themes ---
    dict(enum="SUNSET", label="Sunset", bg=("gradient", ["#FFA06B", "#F2545B", "#A93F6E"]),
         fg="#FFFFFF", acc="#FFE3D3", font="serif", soft_shadow=True),
    dict(enum="LAGOON", label="Lagoon", bg=("gradient", ["#2FC4C9", "#2F80D9", "#3D4FC4"]),
         fg="#FFFFFF", acc="#C9F6F4", font="sans-serif-medium", soft_shadow=True),
    dict(enum="PASTEL", label="Pastel", bg=("gradient", ["#C7B5F7", "#F5C6E4", "#FFE0C4"]),
         fg="#2E1F4D", acc="#6B4FA0", font="serif", deco="mark"),
    dict(enum="ULTRAVIOLET", label="Ultraviolet", bg=("gradient", ["#9B3BEA", "#5320D8"]),
         fg="#FFFFFF", acc="#FFD166", font="sans-serif", bold=True, deco="dot"),
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
]
MODES = ["", "_light", "_dark"]  # order must match ThemeMode: SYSTEM, LIGHT, DARK
DECO_START = {"mark": 30, "stripe": 18}  # dp the text is pushed right by


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
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/background"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="18dp"{bg}>

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

    <TextView
        android:id="@+id/widget_meta"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="end"
        android:paddingTop="4dp"
        android:alpha="0.65"
        android:fontFamily="sans-serif-medium"
        android:textColor="{fg}"
        android:textSize="11sp"{meta_extra} />
</LinearLayout>
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
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_item_text"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingEnd="8dp"
    android:paddingBottom="4dp"
    android:fontFamily="{font}"
    android:lineSpacingMultiplier="1.12"
    android:textColor="{fg}"
    android:textSize="13sp"{extra} />
"""

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
        res(f"layout/widget_{name}.xml", MAIN.format(
            bg=bg, fg=fg, acc=acc, font=t["font"], indent=indent,
            stripe=stripe, mark=mark, dot=dot,
            text_extra=(bold_attr + shadow).format(i=" " * 20),
            small_extra=shadow.format(i=" " * 20),
            meta_extra=shadow.format(i=" " * 8)))
        res(f"layout/widget_item_{name}.xml", ITEM.format(
            fg=fg, font=t["font"], extra=(bold_attr + shadow).format(i=" " * 4)))
        main_ids.append(f"R.layout.widget_{name}")
        item_ids.append(f"R.layout.widget_item_{name}")
        unique_items.add(name)

    if len(main_ids) == 1:
        main_ids *= 3
        item_ids *= 3
    start = DECO_START.get(deco, 0) if deco in DECO_START else 0
    enum_rows.append(
        f'    {t["enum"]}(\n'
        f'        "{t["label"]}", wallpaper = {str(bool(t.get("wallpaper"))).lower()}, '
        f'family = "{t["font"]}", bold = {str(bold).lower()}, startDp = {start}f,\n'
        f'        layouts = intArrayOf({", ".join(main_ids)}),\n'
        f'        items = intArrayOf({", ".join(item_ids)}),\n'
        f'    )'
    )

write(KT, f"""package io.github.niravgrowlity.quotewidget

import android.graphics.Typeface

// Generated by tools/gen_widget_res.py. Do not edit by hand.

/**
 * Widget themes. [wallpaper] themes take colours from the wallpaper and have light/dark
 * variants; the others are fixed colour palettes. [family], [bold] and [startDp] mirror the
 * generated layouts so the auto-fit measures text exactly as it will be drawn.
 */
enum class WidgetStyle(
    val label: String,
    val wallpaper: Boolean,
    val family: String,
    val bold: Boolean,
    val startDp: Float,
    private val layouts: IntArray,
    private val items: IntArray,
) {{
{",\n".join(enum_rows)};

    fun layout(mode: ThemeMode) = layouts[mode.ordinal]
    fun item(mode: ThemeMode) = items[mode.ordinal]
    fun typeface(): Typeface = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)

    companion object {{
        /** Number of distinct scroll-mode item layouts; the widget list adapter needs it. */
        const val ITEM_VIEW_TYPES = {len(unique_items)}
    }}
}}
""")
print(f"generated {len(THEMES)} themes, {len(unique_items)} layouts")
