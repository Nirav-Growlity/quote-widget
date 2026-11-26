"""Generates every widget colour, background and layout, plus WidgetLayouts.kt.

Each style gets three layouts (follow system / always light / always dark) and a matching
item layout for long-quote scroll mode. On Android 12+ colours come from the wallpaper
(Material You system palette); older versions use the fixed fallbacks.

Run from the repo root:  python tools/gen_widget_res.py
"""
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "app", "src", "main", "res")
KT = os.path.join(ROOT, "app", "src", "main", "java", "io", "github", "niravgrowlity", "quotewidget",
                  "WidgetLayouts.kt")

# name: {mode: (pre-12 fallback, Android 12+ system colour)}
COLORS = {
    "tonal_bg":    {"light": ("#DCE1FF", "system_accent1_100"), "dark": ("#2F3F6E", "system_accent1_700")},
    "tonal_text":  {"light": ("#121B3D", "system_accent1_900"), "dark": ("#DCE1FF", "system_accent1_100")},
    "paper_bg":    {"light": ("#FBF9FC", "system_neutral1_10"), "dark": ("#1B1B1F", "system_neutral1_900")},
    "paper_text":  {"light": ("#1B1B1F", "system_neutral1_900"), "dark": ("#E4E2E6", "system_neutral1_100")},
    "paper_mark":  {"light": ("#3F5AA6", "system_accent1_500"), "dark": ("#B6C4FF", "system_accent1_200")},
    "bold_bg":     {"light": ("#3F5AA6", "system_accent1_600"), "dark": ("#B6C4FF", "system_accent1_200")},
    "bold_text":   {"light": ("#FFFFFF", "system_accent1_0"),   "dark": ("#0F2357", "system_accent1_800")},
    "grad_start":  {"light": ("#3F5AA6", "system_accent1_500"), "dark": ("#2F3F6E", "system_accent1_700")},
    "grad_end":    {"light": ("#8A4F7D", "system_accent3_500"), "dark": ("#6A3A60", "system_accent3_700")},
    "grad_text":   {"light": ("#FFFFFF", "system_accent1_0"),   "dark": ("#FFFFFF", "system_accent1_0")},
}

# Enum name -> look. bg: ("solid", colour) | ("gradient", start, end) | None
STYLES = {
    "EDITORIAL": dict(key="editorial", bg=("solid", "paper_bg"), fg="paper_text", mark="paper_mark",
                      font="serif", bold=False, shadow=False),
    "TONAL":     dict(key="tonal", bg=("solid", "tonal_bg"), fg="tonal_text", mark=None,
                      font="sans-serif-medium", bold=False, shadow=False),
    "BOLD":      dict(key="bold", bg=("solid", "bold_bg"), fg="bold_text", mark=None,
                      font="sans-serif", bold=True, shadow=False),
    "GRADIENT":  dict(key="gradient", bg=("gradient", "grad_start", "grad_end"), fg="grad_text", mark=None,
                      font="sans-serif-medium", bold=False, shadow=False),
    "CLEAR":     dict(key="clear", bg=None, fg=None, mark=None,
                      font="sans-serif-medium", bold=False, shadow=True),
}
MODES = ["", "_light", "_dark"]  # order must match ThemeMode: SYSTEM, LIGHT, DARK


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


# ---------- colours ----------
lines = []
for n, v in COLORS.items():
    lines += [color(f"{n}_light", v["light"][0]), color(f"{n}_dark", v["dark"][0])]
lines += [color(n, f"@color/{n}_light") for n in COLORS]
res("values/colors.xml", resources(lines))
res("values-night/colors.xml", resources([color(n, f"@color/{n}_dark") for n in COLORS]))
lines = []
for n, v in COLORS.items():
    lines += [color(f"{n}_light", f"@android:color/{v['light'][1]}"),
              color(f"{n}_dark", f"@android:color/{v['dark'][1]}")]
res("values-v31/colors.xml", resources(lines))
for stale in ["values-night-v31/colors.xml"]:
    p = os.path.join(RES, stale)
    if os.path.exists(p):
        os.remove(p)

# ---------- layouts ----------
SOLID = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/{c}" />
    <corners android:radius="@dimen/widget_radius" />
</shape>
"""
GRADIENT = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="315" android:startColor="@color/{a}" android:endColor="@color/{b}" />
    <corners android:radius="@dimen/widget_radius" />
</shape>
"""
SHADOW = ('\n{i}android:shadowColor="#B3000000"\n{i}android:shadowRadius="8"'
          '\n{i}android:shadowDx="0"\n{i}android:shadowDy="1"')

MAIN = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/background"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="18dp"{bg}>

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">
{mark}
        <!-- Normal mode: quote auto-fitted to the widget -->
        <TextView
            android:id="@+id/widget_text"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:paddingStart="{top}dp"
            android:gravity="center"
            android:fontFamily="{font}"
            android:lineSpacingMultiplier="1.12"
            android:textColor="{fg}"
            android:textSize="18sp"{text_extra} />

        <!-- Long-quote mode: scrolls, used when the quote won't fit at a readable size -->
        <ListView
            android:id="@+id/widget_list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_marginStart="{top}dp"
            android:divider="@null"
            android:fadeScrollbars="false"
            android:scrollbarStyle="outsideOverlay"
            android:scrollbars="vertical"
            android:visibility="gone" />
    </FrameLayout>

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
            android:textColor="@color/{c}"
            android:textSize="44sp" />
"""
ITEM = """<?xml version="1.0" encoding="utf-8"?>
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

main_ids, item_ids = {}, {}
for enum, s in STYLES.items():
    key = s["key"]
    main_ids[enum], item_ids[enum] = [], []
    for mode in MODES:
        name = f"{key}{mode}"
        bg = ""
        if s["bg"] and s["bg"][0] == "solid":
            res(f"drawable/wbg_{name}.xml", SOLID.format(c=f"{s['bg'][1]}{mode}"))
            bg = f'\n    android:background="@drawable/wbg_{name}"'
        elif s["bg"]:
            res(f"drawable/wbg_{name}.xml", GRADIENT.format(a=f"{s['bg'][1]}{mode}", b=f"{s['bg'][2]}{mode}"))
            bg = f'\n    android:background="@drawable/wbg_{name}"'
        fg = f"@color/{s['fg']}{mode}" if s["fg"] else "#FFFFFFFF"
        bold = '\n{i}android:textStyle="bold"' if s["bold"] else ""
        shadow = SHADOW if s["shadow"] else ""
        mark = MARK.format(c=f"{s['mark']}{mode}") if s["mark"] else ""
        top = 28 if s["mark"] else 0
        res(f"layout/widget_{name}.xml", MAIN.format(
            bg=bg, fg=fg, font=s["font"], mark=mark, top=top,
            text_extra=(bold + shadow).format(i=" " * 12),
            meta_extra=shadow.format(i=" " * 8)))
        res(f"layout/widget_item_{name}.xml", ITEM.format(
            fg=fg, font=s["font"], extra=(bold + shadow).format(i=" " * 4)))
        main_ids[enum].append(f"R.layout.widget_{name}")
        item_ids[enum].append(f"R.layout.widget_item_{name}")

unique_items = len({i for ids in item_ids.values() for i in ids})


def kt_map(ids):
    return ",\n".join(f'        "{k}" to intArrayOf({", ".join(v)})' for k, v in ids.items())


write(KT, f"""package io.github.niravgrowlity.quotewidget

// Generated by tools/gen_widget_res.py. Do not edit by hand.
internal object WidgetLayouts {{
    /** Number of distinct scroll-mode item layouts; the widget list adapter needs it. */
    const val ITEM_VIEW_TYPES = {unique_items}

    /** Style enum name -> [follow system, light, dark]. */
    val main: Map<String, IntArray> = mapOf(
{kt_map(main_ids)},
    )

    val item: Map<String, IntArray> = mapOf(
{kt_map(item_ids)},
    )
}}
""")
print(f"generated {len(STYLES)} styles, {unique_items} item layouts")
