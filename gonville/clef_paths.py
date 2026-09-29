#!/usr/bin/env python3
"""Trace the gonville treble and bass clefs into Compose/SVG path data.

The app's gonville.otf only contains accidentals, so the clefs used by the note
staff (app/src/main/java/de/moekadu/tuner/ui/notes/NoteStaff.kt) are stored as
path data. Requires ghostscript and potrace. Usage:

    ./clef_paths.py

Output units are staff spaces, x starts at the left edge of the glyph, y = 0 is
the clef line (G line for treble, F line for bass) and y points downwards.
"""
import os
import re
import subprocess
import sys
import tempfile

RESOLUTION = 288  # dpi; the glyph canvas is 1000pt, so 1 canvas unit = 4 px
PX_PER_UNIT = RESOLUTION / 72.0
CANVAS_PX = int(1000 * PX_PER_UNIT)

# glyph name -> (staff space in canvas units, clef line y in canvas units)
# One staff space is 125 canvas units at the default glyph scale 1900 (distance of
# the bass clef dots); the treble clef is drawn at scale 1736.
GLYPHS = {
    "clefG": (125.0 * 1736 / 1900, 1000 - 822 * 1736 / 3600.0),
    "clefF": (125.0, (417 + 542) / 2.0),
}

TOKEN = re.compile(r"[MmCcLlZz]|-?\d+(?:\.\d+)?")


def trace(name, workdir):
    here = os.path.dirname(os.path.abspath(__file__))
    ps = subprocess.run([sys.executable, os.path.join(here, "glyphs.py"), "--test", name],
                        check=True, capture_output=True, cwd=here).stdout
    pbm = os.path.join(workdir, name + ".pbm")
    subprocess.run(["gs", "-sDEVICE=pbmraw", "-sOutputFile=" + pbm, "-r%d" % RESOLUTION,
                    "-g%dx%d" % (CANVAS_PX, CANVAS_PX), "-dBATCH", "-dNOPAUSE", "-q", "-"],
                   input=ps, check=True)
    svg = subprocess.run(["potrace", "-s", "--flat", "-o", "-", pbm],
                         check=True, capture_output=True, text=True).stdout
    return " ".join(re.findall(r'<path d="([^"]+)"', svg, re.S))


def parse(d):
    """Parse potrace path data into subpaths of absolute segments in potrace units."""
    tokens = TOKEN.findall(d)
    subpaths, current = [], None
    pos = start = (0.0, 0.0)
    cmd, i = None, 0
    while i < len(tokens):
        if tokens[i].isalpha():
            cmd = tokens[i]
            i += 1
            if cmd in "Zz":
                current.append(("Z",))
                pos = start
                continue
        rel = cmd.islower()
        n = {"m": 2, "l": 2, "c": 6}[cmd.lower()]
        vals = [float(v) for v in tokens[i:i + n]]
        i += n
        pts = [(vals[k] + (pos[0] if rel else 0), vals[k + 1] + (pos[1] if rel else 0))
               for k in range(0, n, 2)]
        if cmd in "Mm":
            current = [("M", pts[0])]
            subpaths.append(current)
            start = pts[0]
            cmd = "l" if rel else "L"  # implicit lineto after moveto
        else:
            current.append(("C" if n == 6 else "L",) + tuple(pts))
        pos = pts[-1]
    return subpaths


def convert(name, workdir):
    staff_space, line_y = GLYPHS[name]
    subpaths = parse(trace(name, workdir))

    # potrace coordinates: tenths of a pixel with y up (transform translate(0,H) scale(.1,-.1))
    def canvas(p):
        return p[0] * 0.1 / PX_PER_UNIT, (CANVAS_PX - p[1] * 0.1) / PX_PER_UNIT

    points = [canvas(p) for sp in subpaths for seg in sp for p in seg[1:]]
    x0 = min(p[0] for p in points)

    def fmt(v):
        s = ("%.3f" % v).rstrip("0").rstrip(".")
        return "0" if s in ("-0", "") else s

    def unit(p):
        x, y = canvas(p)
        return fmt((x - x0) / staff_space) + " " + fmt((y - line_y) / staff_space)

    out = []
    for sp in subpaths:
        for seg in sp:
            out.append(seg[0] + " ".join(unit(p) for p in seg[1:]))
    width = (max(p[0] for p in points) - x0) / staff_space
    top = (min(p[1] for p in points) - line_y) / staff_space
    bottom = (max(p[1] for p in points) - line_y) / staff_space
    return "".join(out), width, top, bottom


def main():
    with tempfile.TemporaryDirectory() as workdir:
        for name in GLYPHS:
            path, width, top, bottom = convert(name, workdir)
            print("%s width=%.3f top=%.3f bottom=%.3f" % (name, width, top, bottom))
            print(path)


if __name__ == "__main__":
    main()
