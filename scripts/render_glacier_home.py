"""Original code-drawn Glacier design study; not a device screenshot.

Only the existing workspace Pillow runtime is used. No game artwork or fonts.
"""
from pathlib import Path
from math import sin, cos, pi
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/design/previews"
S = 2
W, H = 544, 1224
C = json.loads((ROOT / "docs/design/palettes.json").read_text())["glacier"]["dark"]
im = Image.new("RGB", (W*S, H*S), C["background"])
d = ImageDraw.Draw(im)

def font(n, bold=False):
    return ImageFont.truetype("C:/Windows/Fonts/"+("segoeuib.ttf" if bold else "segoeui.ttf"),round(n*S))

def txt(x,y,s,n=14,color=None,bold=False):
    d.text((round(x*S),round(y*S)),s,font=font(n,bold),fill=color or C["text"])

def rr(x,y,w,h,fill,r=16,stroke=None):
    d.rounded_rectangle(tuple(round(v*S) for v in (x,y,x+w,y+h)),round(r*S),fill=fill,outline=stroke,width=S)

def line(pts,fill,w=2):
    d.line([(round(x*S),round(y*S)) for x,y in pts],fill=fill,width=round(w*S),joint="curve")

def poly(pts,fill):
    d.polygon([(round(x*S),round(y*S)) for x,y in pts],fill=fill)

def logo(x,y,size):
    # Open ribbon V + flight accent, independent of any block/game visual language.
    p=lambda a,b:(x+size*a,y+size*b)
    poly([p(.10,.22),p(.30,.22),p(.52,.72),p(.42,.93)],C["primary"])
    poly([p(.42,.93),p(.31,.68),p(.70,.12),p(.95,.12)],"#CDEEFF")
    poly([p(.69,.12),p(.95,.12),p(.85,.27)],"#A5B4FC")

def chip(x,y,label,w,fill="#23364A",color=None):
    rr(x,y,w,30,fill,10)
    txt(x+11,y+5,label,12,color or C["text"],True)

def icon(kind,x,y,color,size=20):
    def path(points): line([(x+a*size,y+b*size) for a,b in points],color,1.8)
    if kind=="home": path([(.08,.48),(.5,.12),(.92,.48),(.84,.48),(.84,.9),(.16,.9),(.16,.48)])
    elif kind=="versions":
        for yy in (.22,.50,.78): path([(.1,yy),(.9,yy)])
    elif kind=="mods":
        rr(x+1,y+1,size-2,size-2,C["surface"],5,color)
        path([(.5,.22),(.5,.78)]);path([(.22,.5),(.78,.5)])
    elif kind=="controls":
        rr(x,y+3,size,size*.65,C["surface"],5,color)
        path([(.28,.32),(.28,.65)]);path([(.12,.48),(.44,.48)])
        d.ellipse(((x+size*.7)*S,(y+size*.43)*S,(x+size*.8)*S,(y+size*.53)*S),fill=color)
    elif kind=="settings":
        for i,yy in enumerate((.22,.5,.78)):
            path([(.05,yy),(.95,yy)]);rr(x+size*(.2+i*.2),y+size*yy-3,5,6,color,2)
    elif kind=="lock":
        d.arc((int((x+4)*S),int(y*S),int((x+15)*S),int((y+16)*S)),180,360,fill=color,width=2*S)
        rr(x+2,y+7,15,12,color,3)
    elif kind=="arrow": path([(.15,.5),(.85,.5),(.6,.25)]);path([(.85,.5),(.6,.75)])
    elif kind=="folder": path([(.1,.25),(.4,.25),(.5,.4),(.9,.4),(.9,.85),(.1,.85),(.1,.25)])
    elif kind=="download": path([(.5,.12),(.5,.65),(.25,.43)]);path([(.5,.65),(.75,.43)]);path([(.1,.7),(.1,.9),(.9,.9),(.9,.7)])

def landscape(x,y,w,h,style=0):
    """Original smooth vector landscape; no screenshot, textures or pixel blocks."""
    layer=Image.new("RGB",(round(w*S),round(h*S)))
    ld=ImageDraw.Draw(layer)
    top,bottom=((20,47,72),(35,76,94)) if style==0 else ((33,37,72),(52,66,94))
    for py in range(layer.height):
        t=py/max(1,layer.height-1)
        ld.line((0,py,layer.width,py),fill=tuple(round(a+(b-a)*t) for a,b in zip(top,bottom)))
    ld.ellipse((w*.69*S,h*.12*S,w*.82*S,h*.12*S+w*.13*S),fill="#A8DDF4")
    for offset,col,base in [(0,"#345C75",.63),(1,"#21445D",.82),(2,"#15334A",1.04)]:
        points=[(0,h*S)]
        for i in range(25):
            xx=w*i/24
            yy=h*(base-.18*sin(i*.35+style+offset*1.4)-.08*cos(i*.8+offset))
            points.append((round(xx*S),round(yy*S)))
        points.append((w*S,h*S));ld.polygon(points,fill=col)
    mask=Image.new("L",layer.size)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,layer.width,layer.height),16*S,fill=255)
    im.paste(layer,(round(x*S),round(y*S)),mask)

txt(24,12,"DESIGN PREVIEW / GLACIER",11,C["muted"],True)
txt(24,36,"Example data · not a running Android screen",11,C["muted"])
txt(26,76,"9:41",12,C["text"],True);txt(432,76,"LTE  100%",11,C["muted"])
logo(24,114,43)
txt(79,113,"Vojo",25,bold=True)
txt(80,145,"Your next adventure",11,C["muted"])
rr(469,113,48,48,C["variant"],24)
txt(485,122,"V",19,C["primary"],True)

txt(24,190,"Make room for play.",26,bold=True)
# Large bounded hero; subtle gradient, with art occupying the upper-right area.
hero=Image.new("RGB",(496*S,316*S))
hd=ImageDraw.Draw(hero)
for xx in range(hero.width):
    t=xx/(hero.width-1)
    a,b=(23,43,61),(26,53,74)
    hd.line((xx,0,xx,hero.height),fill=tuple(round(v+(z-v)*t) for v,z in zip(a,b)))
mask=Image.new("L",hero.size);ImageDraw.Draw(mask).rounded_rectangle((0,0,hero.width,hero.height),20*S,fill=255)
im.paste(hero,(24*S,242*S),mask)
landscape(325,260,174,116)
txt(44,262,"SELECTED PROFILE",11,"#B8D2E7",True)
txt(44,289,"Everyday",26,bold=True)
txt(44,321,"adventures",26,bold=True)
chip(44,374,"1.21",61)
chip(113,374,"Fabric",75)
chip(196,374,"3 GB RAM",99)
txt(44,421,"Renderer: Auto",12,"#B8D2E7")
rr(44,451,456,64,C["primary"],12)
poly([(240,471),(240,495),(258,483)],C["onPrimary"])
txt(271,465,"Play",20,C["onPrimary"],True)
rr(44,527,107,26,"#253D51",13)
icon("lock",54,532,"#C8E6FB",13)
txt(77,530,"Locked",12,"#EAF2FA",True)
txt(163,531,"Unlock options appear when you play",11,"#B8D2E7")

txt(24,587,"Jump back in",17,bold=True)
txt(443,591,"See all",12,C["primary"])
for i,(name,ver,loader,style) in enumerate([("Vanilla escape","1.21","Vanilla",0),("Creative lab","1.21","Fabric",1)]):
    x=24+i*254
    rr(x,624,242,171,C["surface"],16)
    landscape(x+10,634,222,70,style)
    txt(x+14,715,name,16,bold=True)
    chip(x+14,748,ver,50,C["variant"],C["muted"])
    chip(x+72,748,loader,80,C["variant"],C["muted"])

txt(24,821,"Quick actions",17,bold=True)
for i,(label,kind) in enumerate([("Profiles","folder"),("Downloads","download"),("Controls","controls")]):
    x=24+i*169
    rr(x,858,158,71,C["surface"],14)
    icon(kind,x+16,873,C["primary"],21)
    txt(x+48,875,label,13,bold=True)

rr(24,950,496,78,C["variant"],16)
txt(42,963,"A smoother session",14,bold=True)
txt(42,988,"Close background apps before you play.",12,C["muted"])
icon("arrow",479,976,C["primary"],20)

rr(12,1054,520,94,C["surface"],22)
for i,(name,kind) in enumerate([("Home","home"),("Versions","versions"),("Mods","mods"),("Controls","controls"),("Settings","settings")]):
    x=64+i*104
    col=C["primary"] if i==0 else C["muted"]
    if i==0: rr(x-28,1065,56,34,"#243C51",17)
    icon(kind,x-11,1071,col,22)
    box=d.textbbox((0,0),name,font=font(11))
    txt(x-(box[2]-box[0])/(2*S),1111,name,11,col,i==0)

txt(24,1172,"Motion plan: 220 ms card transitions + press feedback.",11,C["muted"])
txt(24,1193,"Reduced motion: static states. No autoplay or looping effects.",11,C["muted"])
OUT.mkdir(parents=True,exist_ok=True)
im.save(OUT/"glacier-home-refined.png",optimize=True)
print(OUT/"glacier-home-refined.png")
