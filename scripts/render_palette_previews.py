"""Render deterministic design studies, not Android screenshots.

Uses Pillow available in the Codex workspace runtime; adds no Android dependency.
Run from the repository root with that runtime's Python executable.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/design/previews"
PALETTES = json.loads((ROOT / "docs/design/palettes.json").read_text())
FONT = Path("C:/Windows/Fonts")
SCALE = 2


def font(size, bold=False):
    return ImageFont.truetype(str(FONT / ("segoeuib.ttf" if bold else "segoeui.ttf")), size * SCALE)


def render(key, mode="dark"):
    p = PALETTES[key]
    c = p[mode]
    im = Image.new("RGB", (560 * SCALE, 1150 * SCALE), "#F1F3F6")
    d = ImageDraw.Draw(im)

    def rect(x, y, w, h, fill, r=0, outline=None):
        box = tuple(round(v * SCALE) for v in (x, y, x+w, y+h))
        d.rounded_rectangle(box, radius=r*SCALE, fill=fill, outline=outline, width=SCALE)

    def text(x, y, value, size=14, color=None, bold=False):
        d.text((x*SCALE, y*SCALE), value, font=font(size, bold), fill=color or c["text"])

    def line(points, color, width=2):
        d.line([(x*SCALE, y*SCALE) for x,y in points], fill=color, width=width*SCALE, joint="curve")

    def logo(x, y, size=36):
        rect(x, y, size, size, c["primary"], 10)
        pts = [(x+size*.23,y+size*.27),(x+size*.42,y+size*.76),(x+size*.58,y+size*.76),(x+size*.8,y+size*.23),(x+size*.6,y+size*.23),(x+size*.5,y+size*.53),(x+size*.39,y+size*.27)]
        d.polygon([(round(a*SCALE),round(b*SCALE)) for a,b in pts], fill=c["onPrimary"])

    text(34, 24, f"Vojo / {p['name']}", 27, "#192333", True)
    text(34, 66, "DARK THEME" if mode == "dark" else "LIGHT THEME", 11, "#526174", True)
    labels = [("BG","background"),("Surface","surface"),("Raised","variant"),("Primary","primary"),("Accent","secondary")]
    for i, (label, color) in enumerate(labels):
        x = 34+i*100
        rect(x, 98, 90, 44, c[color], 10, "#CAD0D9")
        text(x, 150, label, 11, "#425066")
        text(x, 169, c[color], 10, "#526174")

    # Product surface. All dimensions here are a single consistent sample layout.
    ox, oy, sw, sh = 60, 218, 440, 852
    rect(ox-5, oy-5, sw+10, sh+10, "#CBD1DB", 33)
    rect(ox, oy, sw, sh, c["background"], 28)
    text(ox+24, oy+15, "9:41", 12, c["muted"], True)
    text(ox+344, oy+15, "LTE  100%", 11, c["muted"])
    logo(ox+24, oy+55)
    text(ox+72, oy+56, "Vojo", 24, bold=True)
    rect(ox+366, oy+55, 42, 42, c["variant"], 21)
    text(ox+380, oy+62, "V", 18, c["primary"], True)
    text(ox+24, oy+118, "Make room for play.", 24, bold=True)
    text(ox+24, oy+155, "Your worlds. Your way.", 14, c["muted"])

    rect(ox+20, oy+202, 400, 262, c["surface"], 16)
    text(ox+40, oy+222, "SELECTED PROFILE", 11, c["muted"], True)
    text(ox+40, oy+250, "Everyday adventures", 21, bold=True)
    rect(ox+40, oy+289, 106, 28, c["variant"], 8)
    text(ox+52, oy+293, "Fabric 1.21", 12, c["secondary"])
    text(ox+40, oy+332, "Renderer  Auto     /     Memory  3 GB", 13, c["muted"])
    rect(ox+40, oy+371, 360, 54, c["primary"], 12)
    # Center visible label with a quiet geometric play glyph.
    d.polygon([(int((ox+179)*SCALE),int((oy+391)*SCALE)),(int((ox+179)*SCALE),int((oy+407)*SCALE)),(int((ox+191)*SCALE),int((oy+399)*SCALE))], fill=c["onPrimary"])
    text(ox+205, oy+385, "Play", 17, c["onPrimary"], True)
    text(ox+40, oy+436, "Locked · Unlock options appear when you play", 11, c["muted"])

    text(ox+24, oy+498, "Recent profiles", 16, bold=True)
    text(ox+350, oy+501, "See all", 12, c["secondary"])
    for i, (name, detail) in enumerate([("Vanilla", "1.21 · Release"),("Creative", "Fabric · 1.21")]):
        x = ox+20+i*205
        rect(x, oy+536, 195, 110, c["surface"], 16)
        rect(x+16, oy+552, 28, 28, c["variant"], 8)
        text(x+25, oy+553, "V" if i == 0 else "F", 14, c["secondary"], True)
        text(x+16, oy+589, name, 15, bold=True)
        text(x+16, oy+615, detail, 12, c["muted"])

    rect(ox+20, oy+666, 400, 83, c["variant"], 16)
    text(ox+38, oy+680, "A smoother session", 14, bold=True)
    text(ox+38, oy+705, "Close background apps before you play.", 12, c["muted"])

    rect(ox+2, oy+774, 436, 75, c["surface"], 23)
    for i, label in enumerate(["Home", "Versions", "Mods", "Controls", "Settings"]):
        x = ox+44+i*88
        color = c["primary"] if i == 0 else c["muted"]
        if i == 0:
            rect(x-24, oy+783, 48, 28, c["variant"], 14)
            line([(x-9,oy+799),(x,oy+790),(x+9,oy+799),(x+9,oy+807),(x-9,oy+807),(x-9,oy+799)],color)
        elif i == 1:
            for j in range(3):
                line([(x-9,oy+791+j*6),(x+9,oy+791+j*6)],color)
        elif i == 2:
            rect(x-9,oy+790,18,18,c["surface"],4,color)
            line([(x,oy+793),(x,oy+805)],color)
            line([(x-6,oy+799),(x+6,oy+799)],color)
        elif i == 3:
            rect(x-12,oy+792,24,15,c["surface"],6,color)
            line([(x-7,oy+795),(x-7,oy+803)],color)
            line([(x-10,oy+799),(x-4,oy+799)],color)
            d.ellipse(((x+4)*SCALE,(oy+797)*SCALE,(x+7)*SCALE,(oy+800)*SCALE),fill=color)
        else:
            for j in range(3):
                line([(x-10,oy+791+j*6),(x+10,oy+791+j*6)],color)
                rect(x-6+j*4,oy+789+j*6,4,4,color,2)
        bbox = d.textbbox((0,0),label,font=font(10))
        d.text((int(x*SCALE-(bbox[2]-bbox[0])/2),(oy+818)*SCALE),label,font=font(10),fill=color)

    text(60, 1094, "Design preview · Example data · No device required", 11, "#526174")
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT/f"{key}-{mode}.png"
    im.save(path, optimize=True)
    return im


def luminance(value):
    channels = [int(value[i:i+2],16)/255 for i in (1,3,5)]
    channels = [v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4 for v in channels]
    return sum(a*b for a,b in zip(channels,(.2126,.7152,.0722)))


def contrast(a,b):
    light,dark = sorted((luminance(a),luminance(b)),reverse=True)
    return (light+.05)/(dark+.05)


if __name__ == "__main__":
    results = {}
    for key,palette in PALETTES.items():
        for mode,c in ((m,palette[m]) for m in ("dark","light")):
            pairs = [("text",bg) for bg in ("background","surface","variant")]+[("muted",bg) for bg in ("background","surface","variant")]+[("onPrimary","primary")]
            results[f"{key}-{mode}"] = {f"{a}/{b}":round(contrast(c[a],c[b]),2) for a,b in pairs}
            assert min(results[f"{key}-{mode}"].values()) >= 4.5, results[f"{key}-{mode}"]
            render(key,mode)
    contact = Image.new("RGB",(1680,1150),"#F1F3F6")
    for i,key in enumerate(PALETTES):
        with Image.open(OUT/f"{key}-dark.png") as im:
            contact.paste(im.resize((560,1150),Image.Resampling.LANCZOS),(i*560,0))
    contact.save(OUT/"palette-comparison.png",optimize=True)
    (OUT/"contrast-report.json").write_text(json.dumps(results,indent=2)+"\n")
    print(f"Rendered six palette/theme studies and a comparison in {OUT}")
