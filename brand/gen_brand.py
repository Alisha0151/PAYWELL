"""PAYWELL brand generator for the supplied 'striped P' logo (owner-provided HTML). Emits editable SVG + transparent PNG.
Wordmark/tagline are converted to outlines from Poppins (SIL OFL) so the SVGs do not depend on an installed font."""
import subprocess
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

import os
HERE = os.path.dirname(os.path.abspath(__file__))
FONT_DIR = os.environ.get("POPPINS_DIR", os.path.join(HERE, "..", "mobile", "assets", "fonts")) + "/"   # Poppins-Light.ttf / Poppins-Medium.ttf
MED, LIGHT = TTFont(FONT_DIR + "Poppins-Medium.ttf"), TTFont(FONT_DIR + "Poppins-Light.ttf")

# ---- design tokens straight from the supplied HTML (dark + light)
DARK = dict(bg="#060C1B", bg2="#0D1A33", ink="#F4F7FC", mut="#8593AE", acc="#35E0B0", b1="#FFFFFF", b2="#5C86E6", ln="#FFFFFF", ln_a=.16, gl="#4678FF", gl_a=.20)
LIGHT_ = dict(bg="#FFFFFF", bg2="#EEF3FA", ink="#0B1F3A", mut="#5F6E87", acc="#0FB98A", b1="#0B1F3A", b2="#3D6FD8", ln="#0B1F3A", ln_a=.18, gl="#3D6FD8", gl_a=.10)

K = 136 / 164                      # mark scale (svg viewBox 36 30 140 164 drawn 136 px tall)
MARK_W, MARK_H = 140 * K, 136
GAP = 26
WORD_PX, TAG_PX = 36.0, 11.5
WORD_TRACK, TAG_TRACK = .30 * WORD_PX, .34 * WORD_PX * 0 + .34 * TAG_PX
P_PATH = "M52 40 H112 A48 48 0 0 1 112 136 H86 V182 H52 Z M86 64 H112 A24 24 0 0 1 112 112 H86 Z"

def text_outline(font, text, px, track, x0, baseline):
    gs, cmap, upem = font.getGlyphSet(), font.getBestCmap(), font["head"].unitsPerEm
    s = px / upem; x = x0; out = []
    for ch in text:
        g = cmap[ord(ch)]; pen = SVGPathPen(gs)
        gs[g].draw(TransformPen(pen, (s, 0, 0, -s, x, baseline)))
        out.append(pen.getCommands()); x += font["hmtx"][g][0] * s + track
    return " ".join(out), x - x0

# lockup geometry (px)
WORD_TOP_PAD = 0
_, WORD_W = text_outline(MED, "PAYWELL", WORD_PX, WORD_TRACK, 0, 0)
_, TAG_W = text_outline(LIGHT, "SIMPLE. FAST. SECURE.", TAG_PX, TAG_TRACK, 0, 0)
BLOCK_W = max(WORD_W, TAG_W)
TAG_LH = 1.5 * TAG_PX
BLOCK_H = WORD_PX + 13 + 1 + 11 + TAG_LH
BLOCK_Y = (MARK_H - BLOCK_H) / 2
BLOCK_X = MARK_W + GAP + 1 + GAP
LOCK_W = BLOCK_X + BLOCK_W
WORD_BASE = BLOCK_Y + (WORD_PX * (1 - 1.4) / 2) + 1.05 * WORD_PX
TAG_TOP = BLOCK_Y + WORD_PX + 13 + 1 + 11
TAG_BASE = TAG_TOP + (TAG_LH - 1.4 * TAG_PX) / 2 + 1.05 * TAG_PX

def mark(uid, t, mono=None, shine=False):
    fill = f"url(#bg{uid})" if not mono else mono
    bands = "".join(f'<rect x="40" y="{40+12*i}" width="140" height="10" fill="{fill}"/>' for i in range(12))
    acc = f'<rect x="40" y="{40+12*4}" width="140" height="10" fill="{mono or t["acc"]}"/>'
    return (f'<g transform="scale({K:.5f}) translate(-36 -30)"><clipPath id="pc{uid}"><path clip-rule="evenodd" d="{P_PATH}"/></clipPath>'
            f'<g clip-path="url(#pc{uid})">{bands}{acc}</g></g>')

def defs(uid, t):
    return (f'<linearGradient id="bg{uid}" gradientUnits="userSpaceOnUse" x1="0" y1="40" x2="0" y2="182"><stop offset="0" stop-color="{t["b1"]}"/><stop offset="1" stop-color="{t["b2"]}"/></linearGradient>')

def lockup_body(uid, t, tag=True, mono=None):
    ink = mono or t["ink"]; acc = mono or t["acc"]; mut = mono or t["mut"]
    w, _ = text_outline(MED, "PAYWELL", WORD_PX, WORD_TRACK, BLOCK_X, WORD_BASE)
    o = mark(uid, t, mono)
    o += f'<rect x="{MARK_W+GAP}" y="{(MARK_H-86)/2:.2f}" width="1" height="86" fill="{mono or t["ln"]}" fill-opacity="{1 if mono else t["ln_a"]}"/>'
    o += f'<path d="{w}" fill="{ink}"/>'
    o += f'<rect x="{BLOCK_X}" y="{WORD_PX+13+BLOCK_Y:.2f}" width="{BLOCK_W:.2f}" height="1" fill="{acc}"/>'
    if tag:
        tg, _ = text_outline(LIGHT, "SIMPLE. FAST. SECURE.", TAG_PX, TAG_TRACK, BLOCK_X, TAG_BASE)
        o += f'<path d="{tg}" fill="{mut}"/>'
    return o

def svg(vb, body, uid, t, bg=False, pad=0):
    b = ""
    if bg:
        b = (f'<radialGradient id="rg{uid}" cx="50%" cy="45%" r="72%"><stop offset="0" stop-color="{t["bg2"]}"/><stop offset="1" stop-color="{t["bg"]}"/></radialGradient>'
             f'<rect x="-2000" y="-2000" width="6000" height="6000" fill="url(#rg{uid})"/>')
        defs_extra = ""
    d = defs(uid, t) + (b.split("<rect")[0] if bg else "")
    rect = ("<rect" + b.split("<rect", 1)[1]) if bg else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}"><defs>{d}</defs>{rect}{body}</svg>'

PAD = 40
VB_LOCK = f"{-PAD} {-PAD} {LOCK_W+2*PAD:.1f} {MARK_H+2*PAD}"
VB_MARK = f"-8 -8 {MARK_W+16:.2f} {MARK_H+16}"
out = {}
out["paywell-logo-on-dark"] = svg(VB_LOCK, lockup_body("a", DARK), "a", DARK, bg=True)
out["paywell-logo-on-dark-transparent"] = svg(VB_LOCK, lockup_body("b", DARK), "b", DARK)
out["paywell-logo-on-light"] = svg(VB_LOCK, lockup_body("c", LIGHT_), "c", LIGHT_)
out["paywell-logo-white"] = svg(VB_LOCK, lockup_body("d", DARK, mono="#FFFFFF"), "d", DARK)
out["paywell-logo-royal"] = svg(VB_LOCK, lockup_body("e", LIGHT_, mono="#10266F"), "e", LIGHT_)
out["paywell-symbol-on-dark"] = svg(VB_MARK, mark("f", DARK), "f", DARK)
out["paywell-symbol-on-light"] = svg(VB_MARK, mark("g", LIGHT_), "g", LIGHT_)
out["paywell-symbol-white"] = svg(VB_MARK, mark("h", DARK, mono="#FFFFFF"), "h", DARK)
out["paywell-symbol-royal"] = svg(VB_MARK, mark("i", LIGHT_, mono="#10266F"), "i", LIGHT_)

def icon(fg_only=False, frac=.62):
    k = 1024 * frac / MARK_H
    tx, ty = (1024 - MARK_W * k) / 2, (1024 - MARK_H * k) / 2
    body = f'<g transform="translate({tx:.1f} {ty:.1f}) scale({k:.4f})">{mark("j", DARK)}</g>'
    bgl = "" if fg_only else (f'<radialGradient id="ih" cx="50%" cy="45%" r="72%"><stop offset="0" stop-color="{DARK["bg2"]}"/><stop offset="1" stop-color="{DARK["bg"]}"/></radialGradient>'
                              '<rect width="1024" height="1024" fill="url(#ih)"/>')
    d = defs("j", DARK)
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><defs>{d}</defs>{bgl}{body}</svg>'
out["paywell-app-icon"] = icon()
out["paywell-app-icon-foreground"] = icon(True, .44)   # inside the Android adaptive-icon safe zone

for k, v in out.items():
    open(os.path.join(HERE, f"assets/svg/{k}.svg"), "w").write(v)
    subprocess.run(["rsvg-convert", "-w", "1024" if "icon" in k else "2400", os.path.join(HERE, f"assets/svg/{k}.svg"), "-o", os.path.join(HERE, f"assets/png/{k}.png")], check=True)

# geometry numbers for the Flutter generator
import json
_gs, _cm = MED.getGlyphSet(), MED.getBestCmap()
_xs, _x = [], 0.0
for ch in "PAYWELL":
    _xs.append(round(_x, 2)); _x += MED["hmtx"][_cm[ord(ch)]][0] * WORD_PX / 1000 + WORD_TRACK
json.dump(dict(LETTER_X=_xs, TAG_TRACK=TAG_TRACK, WORD_TRACK=WORD_TRACK, TAG_LH=TAG_LH, MARK_W=MARK_W, MARK_H=MARK_H, GAP=GAP, WORD_W=WORD_W, TAG_W=TAG_W, BLOCK_W=BLOCK_W, BLOCK_Y=BLOCK_Y, BLOCK_H=BLOCK_H, LOCK_W=LOCK_W), open(os.path.join(HERE, "geometry.json"), "w"))
print("ok", round(LOCK_W, 1), round(BLOCK_W, 1))
