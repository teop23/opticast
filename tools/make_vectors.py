"""Build Android VectorDrawables from the traced mark (brand/mark.svg).
Run after tracing: python tools/make_vectors.py
"""
import os
import re

HERE = os.path.dirname(__file__)
SVG = os.path.join(HERE, "..", "brand", "mark.svg")
RES = os.path.join(HERE, "..", "app", "src", "main", "res", "drawable")

svg = open(SVG, encoding="utf-8").read()
W, H = map(float, re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', svg).groups())
D = re.search(r'\bd="([^"]+)"', svg, re.S).group(1)

TPL = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size}dp" android:height="{size}dp"
    android:viewportWidth="{S:.1f}" android:viewportHeight="{S:.1f}">
    <group android:translateX="{tx:.1f}" android:translateY="{ty:.1f}">
        <path android:fillColor="{color}" android:fillType="nonZero" android:pathData="{d}" />
    </group>
</vector>
'''


def gen(name, size, color, frac):
    s = max(W, H) / frac
    os.makedirs(RES, exist_ok=True)
    with open(os.path.join(RES, name), "w", encoding="utf-8") as f:
        f.write(TPL.format(size=size, S=s, tx=(s - W) / 2, ty=(s - H) / 2, color=color, d=D))


gen("ic_launcher_foreground.xml", 108, "#FFC6F000", 0.60)  # adaptive fg, safe zone
gen("ic_launcher_monochrome.xml", 108, "#FFFFFFFF", 0.60)  # themed icon (system tints)
gen("ic_stat_opticast.xml", 24, "#FFFFFFFF", 0.85)         # notification (white)
gen("ic_splash.xml", 108, "#FFC6F000", 0.66)               # splash mark
print("vector drawables written to", os.path.normpath(RES))
