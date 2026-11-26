"""Generates the widget colour, background and layout resources.

Each style has three layouts:
  widget_<style>        follows the system light/dark mode (colours from values/ + values-night/)
  widget_<style>_light  always light
  widget_<style>_dark   always dark
On Android 12+ the colours come from the wallpaper (Material You system palette).

Run:  python tools/gen_widget_res.py
"""
import os

RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

# (pre-Android-12 fallback, Android 12+ system palette)
COLORS = {
    "tonal_bg":     {"light": ("#E8DEF8", "system_accent1_100"), "dark": ("#4A4458", "system_accent1_700")},
    "tonal_text":   {"light": ("#1D192B", "system_accent1_900"), "dark": ("#E8DEF8", "system_accent1_100")},
    "surface_bg":   {"light": ("#F7F2FA", "system_neutral1_50"), "dark": ("#211F26", "system_neutral1_900")},
    "surface_text": {"light": ("#1D1B20", "system_neutral1_900"), "dark": ("#E6E0E9", "system_neutral1_100")},
    "accent_bg":    {"light": ("#6750A4", "system_accent1_600"), "dark": ("#D0BCFF", "system_accent1_200")},
    "accent_text":  {"light": ("#FFFFFF", "system_accent1_0"), "dark": ("#381E72", "system_accent1_800")},
}
APP = {  # app theme accents
    "app_primary": {"light": ("#6750A4", "system_accent1_600"), "dark": ("#D0BCFF", "system_accent1_200")},
    "app_accent":  {"light": ("#7D5260", "system_accent3_500"), "dark": ("#EFB8C8", "system_accent3_200")},
}
STYLES = ["tonal", "surface", "accent"]


def write(rel, body):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        f.write(body)


def resources(lines):
    return "<resources>\n" + "".join(f"    {l}\n" for l in lines) + "</resources>\n"


def color(name, value):
    return f'<color name="{name}">{value}</color>'


# values/: fixed fallbacks + "follow system" aliases (light)
lines = []
for n, v in COLORS.items():
    lines += [color(f"{n}_light", v["light"][0]), color(f"{n}_dark", v["dark"][0])]
lines += [color(n, f"@color/{n}_light") for n in COLORS]
lines += [color(n, v["light"][0]) for n, v in APP.items()]
write("values/colors.xml", resources(lines))

# values-night/: "follow system" aliases (dark)
lines = [color(n, f"@color/{n}_dark") for n in COLORS]
lines += [color(n, v["dark"][0]) for n, v in APP.items()]
write("values-night/colors.xml", resources(lines))

# values-v31/: Material You palette
lines = []
for n, v in COLORS.items():
    lines += [color(f"{n}_light", f"@android:color/{v['light'][1]}"),
              color(f"{n}_dark", f"@android:color/{v['dark'][1]}")]
lines += [color(n, f"@android:color/{v['light'][1]}") for n, v in APP.items()]
write("values-v31/colors.xml", resources(lines))

lines = [color(n, f"@android:color/{v['dark'][1]}") for n, v in APP.items()]
write("values-night-v31/colors.xml", resources(lines))

SHAPE = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/{c}" />
    <corners android:radius="@dimen/widget_radius" />
</shape>
"""

LAYOUT = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/background"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"{bg}>

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <!-- Normal mode: quote auto-fitted to the widget -->
        <TextView
            android:id="@+id/widget_text"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:lineSpacingMultiplier="1.1"
            android:textColor="{fg}"
            android:textSize="18sp"{extra2} />

        <!-- Long-quote mode: scrollable, used when the quote won't fit at a readable size -->
        <ListView
            android:id="@+id/widget_list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
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
        android:paddingTop="6dp"
        android:alpha="0.7"
        android:textColor="{fg}"
        android:textSize="11sp"{extra} />
</LinearLayout>
"""

ITEM = """<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_item_text"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingEnd="6dp"
    android:paddingBottom="4dp"
    android:lineSpacingMultiplier="1.1"
    android:textColor="{fg}"
    android:textSize="13sp"{extra} />
"""


def indent(attrs, extra_spaces):
    return attrs.replace("\n        ", "\n        " + " " * extra_spaces)


def emit(name, bg, fg, extra):
    write(f"layout/widget_{name}.xml", LAYOUT.format(bg=bg, fg=fg, extra=extra, extra2=indent(extra, 4)))
    write(f"layout/widget_item_{name}.xml", ITEM.format(fg=fg, extra=extra.replace("\n        ", "\n    ")))


for style in STYLES:
    extra = '\n        android:textStyle="bold"' if style == "accent" else ""
    for suffix in ["", "_light", "_dark"]:
        write(f"drawable/bg_{style}{suffix}.xml", SHAPE.format(c=f"{style}_bg{suffix}"))
        emit(f"{style}{suffix}", f'\n    android:background="@drawable/bg_{style}{suffix}"',
             f"@color/{style}_text{suffix}", extra)

SHADOW = ('\n        android:shadowColor="#CC000000"\n        android:shadowRadius="6"'
          '\n        android:shadowDx="0"\n        android:shadowDy="1"')
emit("clear", "", "#FFFFFFFF", SHADOW)
print("generated")
