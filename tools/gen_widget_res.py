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
    dict(enum="EVERFOREST_LIGHT", label="Everforest Light", group="palette", bg=("solid", "#FDF6E3"),
         fg="#5C6A72", acc="#8DA101", font="serif", deco="mark"),
    dict(enum="KANAGAWA", label="Kanagawa", group="palette", bg=("solid", "#1F1F28"),
         fg="#DCD7BA", acc="#E6C384", font="serif", deco="mark"),
    dict(enum="KANAGAWA_LOTUS", label="Kanagawa Lotus", group="palette", bg=("solid", "#F2ECBC"),
         fg="#545464", acc="#C84053", font="serif", deco="mark"),
    dict(enum="ONE_DARK", label="One Dark", group="palette", bg=("solid", "#282C34"),
         fg="#ABB2BF", acc="#61AFEF", font="monospace", deco="dot"),
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

                <!-- A library font is drawn by a widget_font_* TextView added here at runtime -->
                <FrameLayout
                    android:id="@+id/widget_text_slot"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
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

FONT_VIEW = """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/gen_widget_res.py -->
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/font_text"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:fontFamily="@font/{font}"
    android:lineSpacingMultiplier="1.12"
    android:textSize="18sp"{extra} />
"""

# ---------------------------------------------------------------- generate
enum_rows = []
fg_colors = []
unique_items = set()
for t in THEMES:
    key = t["enum"].lower()
    modes = MODES if t.get("wallpaper") else [""]  # colour themes look the same in light and dark
    bold = t.get("bold", False)
    shadow = HARD_SHADOW if t.get("shadow") else SOFT_SHADOW if t.get("soft_shadow") else ""
    deco = t.get("deco")
    indent = DECO_START.get("mark", 0) if deco == "mark" else 0
    main_ids, item_ids, fg_ids = [], [], []
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
        fg_colors.append(color(f"wfg_{name}", fg))
        fg_ids.append(f"R.color.wfg_{name}")
        main_ids.append(f"R.layout.widget_{name}")
        item_ids.append(f"R.layout.widget_item_{name}")
        unique_items.add(name)

    if len(main_ids) == 1:
        main_ids *= 3
        item_ids *= 3
        fg_ids *= 3
    start = DECO_START.get(deco, 0) if deco in DECO_START else 0
    enum_rows.append(
        f'    {t["enum"]}(\n'
        f'        "{t["label"]}", StyleGroup.{"WALLPAPER" if t.get("wallpaper") else t.get("group", "original").upper()}, '
        f'family = "{t["font"]}", bold = {str(bold).lower()}, startDp = {start}f, '
        f'leftAligned = {str(deco in ("mark", "stripe")).lower()}, shadow = {str(bool(shadow)).lower()},\n'
        f'        fg = intArrayOf({", ".join(fg_ids)}),\n'
        f'        layouts = intArrayOf({", ".join(main_ids)}),\n'
        f'        items = intArrayOf({", ".join(item_ids)}),\n'
        f'    )'
    )

write(KT, f"""package io.github.niravgrowlity.quotewidget

import android.graphics.Typeface

// Generated by tools/gen_widget_res.py. Do not edit by hand.

/** How themes are grouped in the style picker, in display order. */
enum class StyleGroup(val title: String) {{
    PALETTE("Classic palettes"),
    ORIGINAL("Originals"),
    WALLPAPER("Match my wallpaper"),
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
    /** Text has a drop shadow (styles without a background). */
    val shadow: Boolean,
    /** Quote text colour resource per ThemeMode, for library fonts. */
    private val fg: IntArray,
    private val layouts: IntArray,
    private val items: IntArray,
) {{
{",\n".join(enum_rows)};

    val wallpaper: Boolean get() = group == StyleGroup.WALLPAPER

    fun layout(mode: ThemeMode) = layouts[mode.ordinal]
    fun item(mode: ThemeMode) = items[mode.ordinal]
    fun textColor(mode: ThemeMode) = fg[mode.ordinal]
    fun typeface(): Typeface = Typeface.create(family, if (bold) Typeface.BOLD else Typeface.NORMAL)

    companion object {{
        /** Number of distinct scroll-mode item layouts; the widget list adapter needs it. */
        const val ITEM_VIEW_TYPES = {len(unique_items) + 2 * len(FONTS)}
    }}
}}
""")
res("values/widget_text_colors.xml", resources(fg_colors))

# ---------------------------------------------------------------- fonts
SHADOW_ATTRS = HARD_SHADOW.format(i=" " * 4)
font_rows = []
for enum, label, family, weight, italic, cat in FONTS:
    key = enum.lower()
    res(f"layout/widget_font_{key}.xml", FONT_VIEW.format(font=f"qf_{key}", extra=""))
    res(f"layout/widget_font_{key}_shadow.xml", FONT_VIEW.format(font=f"qf_{key}", extra=SHADOW_ATTRS))
    font_rows.append(
        f'    {enum}("{label}", FontCategory.{cat}, R.font.qf_{key}, '
        f'R.layout.widget_font_{key}, R.layout.widget_font_{key}_shadow)')
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
 * Google Fonts in res/font (licences: FONT_LICENSES.txt). Widgets can only use a font that a
 * layout names in XML, so each font has its own one-TextView layout that WidgetRenderer
 * places inside the themed widget.
 */
enum class QuoteFont(
    val label: String,
    val category: FontCategory?,
    private val font: Int,
    private val plainLayout: Int,
    private val shadowLayout: Int,
) {{
    THEME("Theme font", null, 0, 0, 0),
{rows};

    fun layout(shadow: Boolean) = if (shadow) shadowLayout else plainLayout

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
