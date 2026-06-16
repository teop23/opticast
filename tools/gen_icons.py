"""Generate Android launcher/notification/monochrome icons from the Opticast mark.
Source: the 4-variant sheet's top-right tile (white mark on transparent) -> clean alpha mask.
Run: python tools/gen_icons.py
"""
import os
import sys
from PIL import Image, ImageChops, ImageDraw

# 4-variant sheet: top-left tile = lime mark on a black card (white outside the card).
SHEET = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(__file__), "..", "brand", "opticast_variants.png")
RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

LIME = (198, 240, 0, 255)
BLACK = (7, 9, 13, 255)
WHITE = (255, 255, 255, 255)

DENS = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def load_mask():
    # Top-left tile = lime mark on a black card (white outside). Lime is uniquely
    # low-blue + high-green, so min(green, 255-blue) isolates it: lime ~231, white ~2,
    # black-card/gaps ~13. Threshold+stretch -> crisp anti-aliased mark with negative space.
    s = Image.open(SHEET).convert("RGB")
    w, h = s.size
    tl = s.crop((0, 0, w // 2, h // 2))
    _, g, b = tl.split()
    m = ImageChops.darker(g, b.point(lambda p: 255 - p))   # per-pixel min(G, 255-B)
    m = m.point(lambda p: 0 if p < 70 else min(255, int((p - 70) * 255 / 160)))
    return m.crop(m.getbbox())


def render(mask, color, canvas, frac):
    img = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    mw, mh = mask.size
    scale = (canvas * frac) / max(mw, mh)
    nw, nh = max(1, int(mw * scale)), max(1, int(mh * scale))
    m = mask.resize((nw, nh), Image.LANCZOS)
    layer = Image.new("RGBA", (nw, nh), color)
    layer.putalpha(m)
    img.paste(layer, ((canvas - nw) // 2, (canvas - nh) // 2), layer)
    return img


def with_bg(mask, fg, bg, canvas, frac, circle=False):
    img = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if circle:
        d.ellipse([0, 0, canvas - 1, canvas - 1], fill=bg)
    else:
        d.rounded_rectangle([0, 0, canvas - 1, canvas - 1], radius=int(canvas * 0.18), fill=bg)
    img.alpha_composite(render(mask, fg, canvas, frac))
    return img


def save(img, folder, name):
    d = os.path.join(RES, folder)
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, name))


def main():
    mask = load_mask()
    for dpi, mult in DENS.items():
        fg_px = int(108 * mult)        # adaptive foreground canvas (108dp)
        notif_px = int(24 * mult)      # notification icon (24dp)
        ic_px = int(48 * mult)         # legacy launcher fallback (48dp)
        save(render(mask, LIME, fg_px, 0.60), f"drawable-{dpi}", "ic_launcher_foreground.png")
        save(render(mask, WHITE, fg_px, 0.60), f"drawable-{dpi}", "ic_launcher_monochrome.png")
        save(render(mask, WHITE, notif_px, 0.88), f"drawable-{dpi}", "ic_stat_opticast.png")
        save(with_bg(mask, LIME, BLACK, ic_px, 0.62), f"mipmap-{dpi}", "ic_launcher.png")
        save(with_bg(mask, LIME, BLACK, ic_px, 0.58, circle=True), f"mipmap-{dpi}", "ic_launcher_round.png")
    # splash icon: ONE high-res nodpi asset (Android 12 scales it to the splash window).
    # 1152px = xxxhdpi 288dp; nodpi avoids density up-scaling so it's crisp on any phone.
    save(render(mask, LIME, 1152, 0.60), "drawable-nodpi", "ic_splash.png")
    print("icons generated under", os.path.normpath(RES))


if __name__ == "__main__":
    main()
