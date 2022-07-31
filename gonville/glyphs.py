#!/usr/bin/env python3

import sys
import os
import string
import math
import time
import base64
import subprocess
import multiprocessing
import argparse
import shutil
from curves import *
from font import font, scaledbrace, GlyphContext

# UTF-7 encoding, ad-hocked to do it the way Fontforge wants it done
# (encoding control characters and double quotes, in particular).
def utf7_encode(s):
    out = ""
    b64 = ""
    # Characters we encode directly: RFC 2152's Set D, plus space.
    ok = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'(),-./:? "
    for c in s + "\0":
        assert ord(c) < 128 # we support ASCII only
        if not (c in ok):
            b64 = b64 + "\0" + c
        else:
            if b64 != "":
                b64 = base64.b64encode(b64)
                b64 = string.replace(b64, "\n", "") # just in case
                b64 = string.replace(b64, "=", "")
                out = out + "+" + b64 + "-"
                b64 = ""
            if c != '\0':
                out = out + c

    return out

def update_bbox(bbox, x, y):
    x0,y0,x1,y1 = bbox
    if x0 == None:
        x0,y0,x1,y1 = x,y,x,y
    else:
        x0 = min(x0, x)
        y0 = min(y0, y)
        x1 = max(x1, x)
        y1 = max(y1, y)
    return x0,y0,x1,y1

def bezfn(x0, x1, x2, x3, t):
    return x0*(1-t)**3 + 3*x1*(1-t)**2*t + 3*x2*(1-t)*t**2 + x3*t**3

def break_curve(x0,y0, x1,y1, x2,y2, x3,y3):
    # We must differentiate the separate cubics for the curve's x and
    # y coordinates, find any stationary points in [0,1], and break
    # the curve at those points.
    #
    # A single coordinate of a Bezier curve has the equation
    #
    #  x = x0 (1-t)^3 + 3 x1 (1-t)^2 t + 3 x2 (1-t) t^2 + x3 t^3
    #    = x0 (1-3t+3t^2-t^3) + 3 x1 (t-2t^2+t^3) + 3 x2 (t^2-t^3) + x3 t^3
    #    = t^3 (x3-3x2+3x1-x0) + t^2 (3x2-6x1+3x0) + t (3x1-3x0) + x0
    #
    # and hence its derivative is at^2+bt+c where
    #  a = 3(x3-3x2+3x1-x0)
    #  b = 6(x2-2x1+x0)
    #  c = 3(x1-x0)
    breakpts = [(0,0),(1,0)]
    for (axis,c0,c1,c2,c3) in ((1,x0,x1,x2,x3),(2,y0,y1,y2,y3)):
        a = 3*(c3-3*c2+3*c1-c0)
        b = 6*(c2-2*c1+c0)
        c = 3*(c1-c0)
        #sys.stderr.write("%d: a=%g b=%g c=%g\n" % (axis, a, b, c))
        tlist = ()
        if a == 0:
            if b != 0:
                breakpts.append((-c/b,axis))
        else:
            disc = b*b-4*a*c
            if disc >= 0:
                rdisc = math.sqrt(disc)
                breakpts.append(((-b + rdisc)/(2*a),axis))
                breakpts.append(((-b - rdisc)/(2*a),axis))
    breakpts.sort()
    curves = []
    #sys.stderr.write("break %g,%g %g,%g %g,%g %g,%g:\n" % (x0,y0,x1,y1,x2,y2,x3,y3))
    #sys.stderr.write("   at %s\n" % repr(breakpts))
    for i in range(len(breakpts)-1):
        (t0, axis0) = breakpts[i]
        (t1, axis1) = breakpts[i+1]
        if 0 <= t0 and t0 < t1 and t1 <= 1:
            nx0 = bezfn(x0,x1,x2,x3,t0)
            ny0 = bezfn(y0,y1,y2,y3,t0)
            nx3 = bezfn(x0,x1,x2,x3,t1)
            ny3 = bezfn(y0,y1,y2,y3,t1)
            nx1 = nx0 + (t1-t0) * ((x3-3*x2+3*x1-x0)*t0**2 + 2*(x2-2*x1+x0)*t0 + (x1-x0))
            ny1 = ny0 + (t1-t0) * ((y3-3*y2+3*y1-y0)*t0**2 + 2*(y2-2*y1+y0)*t0 + (y1-y0))
            nx2 = nx3 - (t1-t0) * ((x3-3*x2+3*x1-x0)*t1**2 + 2*(x2-2*x1+x0)*t1 + (x1-x0))
            ny2 = ny3 - (t1-t0) * ((y3-3*y2+3*y1-y0)*t1**2 + 2*(y2-2*y1+y0)*t1 + (y1-y0))
            if axis0 == 1:
                nx1 = nx0
            elif axis0 == 2:
                ny1 = ny0
            if axis1 == 1:
                nx2 = nx3
            elif axis1 == 2:
                ny2 = ny3
            curves.append((nx0,ny0,nx1,ny1,nx2,ny2,nx3,ny3))
            #sys.stderr.write("  got %g,%g %g,%g %g,%g %g,%g\n" % curves[-1])
    return curves

def check_call_devnull(*args, **kws):
    # Wrapper on subprocess.check_call which prints the standard error
    # of the process if it fails.
    p = subprocess.Popen(*args, **kws, stdout=subprocess.DEVNULL,
                         stderr=subprocess.PIPE)
    _, err = p.communicate()
    status = p.wait()
    if status != 0:
        sys.stderr.write(err)
        raise subprocess.CalledProcessError(status, args[0])

# Use potrace to compute the PS path outline of any glyph.
def get_ps_path(char, debug=None):
    path = []
    xsize, ysize = char.canvas_size
    res = char.trace_res
    commands = []
    commands.append(["gs", "-sDEVICE=pbm", "-sOutputFile=-",
                     "-g{:d}x{:d}".format(xsize*res, ysize*res),
                     "-r{:d}".format(72*res),
                     "-dBATCH", "-dNOPAUSE", "-q", "-"])
    if debug is not None:
        commands.append(["tee", "z1."+debug])
    commands.append(["potrace", "-b", "ps", "-c", "-q",
                     "-W", "1in", "-H", "1in", "-r", "4000",
                     "-M", "1000", "-O", "1", "-o", "-", "-"])
    if debug is not None:
        commands.append(["tee", "z2."+debug])
    procs = [None] * len(commands)
    for i, command in enumerate(commands):
        procs[i] = subprocess.Popen(command,
                                    stdin=(procs[i-1].stdout if i>0
                                           else subprocess.PIPE),
                                    stdout=subprocess.PIPE, close_fds=True)
    procs[0].stdin.write(("0 %d translate 1 -1 scale\n" % ysize + char.makeps() + "showpage").encode("ASCII"))
    procs[0].stdin.close()
    # Now we read and parse potrace's PostScript output. This is easy
    # enough if we've configured potrace to output as simply as
    # possible (which we did) and are also ignoring most of the fiddly
    # bits, which we are. I happen to know that potrace (as of v1.8 at
    # least) transforms its coordinate system into one based on tenths
    # of a pixel measured up and right from the lower left corner, so
    # I'm going to ignore the scale and translate commands and just
    # skip straight to parsing the actual lines and curves on that
    # basis.
    psstack = []
    pscurrentpoint = None, None
    output = "newpath"
    scale = 4.0 / char.trace_res
    while 1:
        s = procs[-1].stdout.readline().decode("ASCII")
        if s == "": break
        if s[:1] == "%":
            continue # comment
        ss = s.split()
        for word in ss:
            if word[:1] in "-0123456789":
                psstack.append(float(word))
            elif word == "gsave":
                pass # ignore
            elif word == "grestore":
                pass # ignore
            elif word == "showpage":
                pass # ignore
            elif word == "scale":
                psstack.pop(); psstack.pop() # ignore
            elif word == "translate":
                psstack.pop(); psstack.pop() # ignore
            elif word == "setgray":
                psstack.pop() # ignore
            elif word == "newpath":
                pscurrentpoint = None, None
            elif word == "moveto" or word == "rmoveto":
                y1 = psstack.pop(); x1 = psstack.pop()

                x0, y0 = pscurrentpoint
                if word == "moveto":
                    x1, y1 = x1, y1
                else:
                    assert x0 != None
                    x1, y1 = x1 + x0, y1 + y0
                pscurrentpoint = x1, y1
                path.append(('m', x1*scale, y1*scale))
            elif word == "lineto" or word == "rlineto":
                y1 = psstack.pop(); x1 = psstack.pop()

                x0, y0 = pscurrentpoint
                if word == "moveto":
                    x1, y1 = x1, y1
                else:
                    assert x0 != None
                    x1, y1 = x1 + x0, y1 + y0
                pscurrentpoint = x1, y1
                path.append(('l', x0*scale, y0*scale, x1*scale, y1*scale))
            elif word == "curveto" or word == "rcurveto":
                y3 = psstack.pop(); x3 = psstack.pop()
                y2 = psstack.pop(); x2 = psstack.pop()
                y1 = psstack.pop(); x1 = psstack.pop()
                x0, y0 = pscurrentpoint
                assert x0 != None
                if word == "curveto":
                    x1, y1 = x1, y1
                    x2, y2 = x2, y2
                    x3, y3 = x3, y3
                else:
                    x1, y1 = x1 + x0, y1 + y0
                    x2, y2 = x2 + x0, y2 + y0
                    x3, y3 = x3 + x0, y3 + y0
                pscurrentpoint = x3, y3
                for c in break_curve(x0*scale,y0*scale,x1*scale,y1*scale,\
                x2*scale,y2*scale,x3*scale,y3*scale):
                    path.append(('c',) + c)
            elif word == "closepath":
                path.append(('cp',))
    procs[-1].stdout.close()
    for p in procs:
        p.wait()
    bbox = None, None, None, None
    for c in path:
        if c[0] != 'cp':
            bbox = update_bbox(bbox, c[-2], c[-1])
    return bbox, path

def get_ps_path_map_function(glyphname):
    # Wrapper on get_ps_path suitable for feeding to the unordered
    # imap function in multiprocessing.Pool. Returns tuples of the
    # form (name, (bbox, path)), so that you can feed a list of those
    # tuples directly to dict().
    return glyphname, get_ps_path(getattr(font, glyphname))

verstring = "version unavailable"

lilyglyphlist = [
("zerowidthspace",       "ZWSP",       0x200b, 'lx','by','rx','by', {"x0":"lx", "x1":"rx", "y0":"by", "y1":"ty", "xw":"rx"}),
("hairspace",       "hairsp",       0x200a, 'lx','by','rx','by', {"x0":"lx", "x1":"rx", "y0":"by", "y1":"ty", "xw":"rx"}),
#("accent",       "scripts.sforzato",       0, 0.5,0.5, 1,0.5),
#("espressivo",   "scripts.espr",           0, 0.5,0.5, 1,0.5),
#("accslashbigup", "flags.ugrace",          0, 'ox','oy', 1,'oy'),
#("accslashbigdn", "flags.dgrace",          0, 'ox','oy', 1,'oy'),
#("acclparen",    "accidentals.leftparen",  0, 1,0.5, 1,0.5, {"x1":"rx"}),
#("accrparen",    "accidentals.rightparen", 0, 0,0.5, 1,0.5, {"x0":"lx"}),
#("arpeggioshort", "scripts.arpeggio",      0, 0,'oy', 1,'oy', {"x0":"lx","x1":"rx","y0":"oy","y1":"ty"}),
#("arpeggioarrowdown", "scripts.arpeggio.arrow.M1", 0, 0,0, 1,0, {"x0":"lx","x1":"rx","y1":"ey"}),
#("arpeggioarrowup", "scripts.arpeggio.arrow.1", 0, 0,0, 1,0, {"x0":"lx","x1":"rx","y0":"ey"}),
#("trillwiggle",  "scripts.trill_element",  0, 'lx',0, 1,0, {"x0":"lx", "x1":"rx"}),
## Irritatingly, we have to put the digits' baselines at the
## glitch (see below) rather than at the real baseline.
#("big0",         "zero",                   0x0030, 0,'gy', 1,'gy'),
#("big1",         "one",                    0x0031, 0,'gy', 1,'gy'),
#("big2",         "two",                    0x0032, 0,'gy', 1,'gy'),
#("big3",         "three",                  0x0033, 0,'gy', 1,'gy'),
#("big4",         "four",                   0x0034, 0,'gy', 1,'gy'),
#("big5",         "five",                   0x0035, 0,'gy', 1,'gy'),
#("big6",         "six",                    0x0036, 0,'gy', 1,'gy'),
#("big7",         "seven",                  0x0037, 0,'gy', 1,'gy'),
#("big8",         "eight",                  0x0038, 0,'gy', 1,'gy'),
#("big9",         "nine",                   0x0039, 0,'gy', 1,'gy'),
#("asciiperiod",  "period",                 0x002e, 0,'gy', 1,'gy'),
#("asciicomma",   "comma",                  0x002c, 0,'gy', 1,'gy'),
#("asciiplus",    "plus",                   0x002b, 0,'gy', 1,'gy'),
#("asciiminus",   "hyphen",                 0x002d, 0,'gy', 1,'gy'),
#("bowdown",      "scripts.downbow",        0, 0.5,0, 1,0),
#("bowup",        "scripts.upbow",          0, 0.5,0, 1,0),
#("bracketlowerlily", "brackettips.down",   0, 0,'hy', 1,'hy'),
#("bracketupperlily", "brackettips.up",     0, 0,'hy', 1,'hy'),
#("breath",       "scripts.rcomma",         0, 0,0.5, 1,0.5),
#("revbreath",    "scripts.lcomma",         0, 0,0.5, 1,0.5),
#("varbreath",    "scripts.rvarcomma",      0, 0.5,0.5, 1,0.5),
#("revvarbreath", "scripts.lvarcomma",      0, 0.5,0.5, 1,0.5),
#("tickmark",     "scripts.tickmark",       0, 'ox',0, 1,0),
#("caesura",      "scripts.caesura",        0, 0,0.4, 1,0.4),
#("caesura",      "scripts.caesura.straight", 0, 0,0.4, 1,0.4),
#("caesuracurved", "scripts.caesura.curved", 0, 0,0.4, 1,0.4),
#("breve",        "noteheads.sM1",          0, 0,0.5, 1,0.5),
#("clefC",        "clefs.C",                0, 0,0.5, 1,0.5),
#("clefCstraight", "clefs.varC",            0, 0,0.5, 1,0.5),
#("clefF",        "clefs.F",                0, 0,'hy', 1,'hy'),
#("clefG",        "clefs.G",                0, 0,'hy', 1,'hy'),
#("clefGdouble",  "clefs.GG",               0, 0,'hy', 1,'hy'),
#("clefGtenorised", "clefs.tenorG",         0, 0,'hy', 1,'hy'),
#("clefTAB",      "clefs.tab",              0, 0,'hy', 1,'hy'),
#("clefperc",     "clefs.percussion",       0, 'ox',0.5, 1,0.5),
#("clefpercbox",  "clefs.varpercussion",    0, 'ox',0.5, 1,0.5),
#("clefCsmall",   "clefs.C_change",         0, 0,0.5, 1,0.5),
#("clefCstraightsmall", "clefs.varC_change", 0, 0,0.5, 1,0.5),
#("clefFsmall",   "clefs.F_change",         0, 0,'hy', 1,'hy'),
#("clefGsmall",   "clefs.G_change",         0, 0,'hy', 1,'hy'),
#("clefGdoublesmall", "clefs.GG_change",    0, 0,'hy', 1,'hy'),
#("clefGtenorisedsmall", "clefs.tenorG_change", 0, 0,'hy', 1,'hy'),
#("clefTABsmall", "clefs.tab_change",       0, 0,'hy', 1,'hy'),
#("clefpercsmall", "clefs.percussion_change", 0, 'ox',0.5, 1,0.5),
#("clefpercboxsmall", "clefs.varpercussion_change", 0, 'ox',0.5, 1,0.5),
#("coda",         "scripts.coda",           0, 0.5,0.5, 1,0.5),
#("varcoda",      "scripts.varcoda",        0, 0.5,0.5, 1,0.5),
#("dynamicf",     "f",                      0x0066, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
#("dynamicm",     "m",                      0x006d, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
#("dynamicn",     "n",                      0x006e, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
#("dynamicp",     "p",                      0x0070, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
    #("dynamicr",     "r",                      0x0072, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
#("dynamics",     "s",                      0x0073, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
#("dynamicz",     "z",                      0x007a, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "xw":"rx"}),
("blank",        "space",                  0x0020, 'lx','by', 'rx','by', {"x0":"lx", "x1":"rx", "y0":"by", "y1":"ty", "xw":"rx"}),
#("fermata",      "scripts.ufermata",       0, 0.5,0, 1,0),
#("fermata00",    "scripts.uveryshortfermata",  0, 0.5,0, 1,0),
#("fermata0",     "scripts.ushortfermata",  0, 0.5,0, 1,0),
#("fermataleft",  "scripts.uhenzeshortfermata", 0, 'ox',0, 'ax',0),
#("fermatadbldot",  "scripts.uhenzelongfermata", 0, 0.5,0, 1,0),
#("fermata2",     "scripts.ulongfermata",   0, 0.5,0, 1,0),
#("fermata3",     "scripts.uverylongfermata", 0, 0.5,0, 1,0),
#("fermataup",    "scripts.dfermata",       0, 0.5,1, 1,1),
#("fermata00up",  "scripts.dveryshortfermata",  0, 0.5,1, 1,1),
#("fermata0up",   "scripts.dshortfermata",  0, 0.5,1, 1,1),
#("fermataleftup", "scripts.dhenzeshortfermata", 0, 'ox',1, 'ax',1),
#("fermatadbldotup", "scripts.dhenzelongfermata", 0, 0.5,1, 1,1),
#("fermata2up",   "scripts.dlongfermata",   0, 0.5,1, 1,1),
#("fermata3up",   "scripts.dverylongfermata", 0, 0.5,1, 1,1),
#("semiflat",     "accidentals.M1",         0, 0,'hy', 1,'hy'),
#("semiflat",     "accidentals.mirroredflat", 0, 0,'hy', 1,'hy'),
#("semiflatslash", "accidentals.mirroredflat.backslash", 0, 0,'hy', 1,'hy'),
#("flat",         "accidentals.M2",         0, 'ox','hy', 1,'hy'),
("flat",         "accidentals.flat",       0, 'ox','hy', 1,'hy'),
("flatup",       "accidentals.flat.arrowup", 0, 'ox','hy', 1,'hy'),
("flatupup",       "accidentals.flat.arrowupup", 0, 'ox','hy', 1,'hy'),
("flatdn",       "accidentals.flat.arrowdown", 0, 'ox','hy', 1,'hy'),
("flatdndn",       "accidentals.flat.arrowdowndown", 0, 'ox','hy', 1,'hy'),
#("flatupdn",     "accidentals.flat.arrowboth", 0, 'ox','hy', 1,'hy'),
#("flatslash",    "accidentals.flat.slash", 0, 'ox','hy', 1,'hy'),
#("flatslash2",   "accidentals.flat.slashslash", 0, 'ox','hy', 1,'hy'),
#("sesquiflat",   "accidentals.M3",         0, 0,'hy', 1,'hy'),
#("sesquiflat",   "accidentals.mirroredflat.flat", 0, 0,'hy', 1,'hy'),
#("doubleflat",   "accidentals.M4",         0, 'ox','hy', 1,'hy'),
#("doubleflat",   "accidentals.flatflat",   0, 'ox','hy', 1,'hy'),
#("doubleflatslash",   "accidentals.flatflat.slash", 0, 'ox','hy', 1,'hy'),
#("harmart",      "noteheads.s0harmonic",   0, 0,0.5, 1,'ay'),
#("harmartfilled", "noteheads.s2harmonic",  0, 0,0.5, 1,'ay'),
#("harmnat",      "scripts.flageolet",      0, 0.5,0.5, 1,0.5),
#("flagopen",     "scripts.open",           0, 0.5,'cy', 1,'cy'),
#("flagthumb",    "scripts.thumb",          0, 0.5,'cy', 1,'cy'),
#("flaghalfopend", "scripts.halfopen",      0, 0.5,'cy', 1,'cy'),
#("flaghalfopenv", "scripts.halfopenvertical", 0, 0.5,'cy', 1,'cy'),
#("headcrotchet", "noteheads.s2",           0, 0,0.5, 1,'ay'),
#("headminim",    "noteheads.s1",           0, 0,0.5, 1,'ay'),
#("legato",       "scripts.tenuto",         0, 0.5,0.5, 1,0.5),
#("portatoup",    "scripts.uportato",       0, 0.5,'ly', 1,'ly'),
#("portatodn",    "scripts.dportato",       0, 0.5,'ly', 1,'ly'),
#("mordentlower", "scripts.mordent",        0, 0.5,'cy', 1,'cy'),
#("mordentupper", "scripts.prall",          0, 0.5,'cy', 1,'cy'),
#("mordentupperlong", "scripts.prallprall", 0, 0.5,'cy', 1,'cy'),
#("mordentupperlower", "scripts.prallmordent", 0, 0.5,'cy', 1,'cy'),
#("upmordentupperlong", "scripts.upprall",  0, 0.5,'cy', 1,'cy'),
#("upmordentupperlower", "scripts.upmordent", 0, 0.5,'cy', 1,'cy'),
#("mordentupperlongdown", "scripts.pralldown", 0, 0.5,'cy', 1,'cy'),
#("downmordentupperlong", "scripts.downprall", 0, 0.5,'cy', 1,'cy'),
#("downmordentupperlower", "scripts.downmordent", 0, 0.5,'cy', 1,'cy'),
#("mordentupperlongup", "scripts.prallup",  0, 0.5,'cy', 1,'cy'),
#("straightmordentupperlong", "scripts.lineprall", 0, 0.5,'cy', 1,'cy'),
#("natural",      "accidentals.0",          0, 0,'cy', 1,'cy'),
("natural",      "accidentals.natural",    0, 'ox','cy', 1,'cy'),
("naturalup",    "accidentals.natural.arrowup", 0, 'ox','cy', 1,'cy'),
("naturalupup",    "accidentals.natural.arrowupup", 0, 'ox','cy', 1,'cy'),
("naturaldn",    "accidentals.natural.arrowdown", 0, 'ox','cy', 1,'cy'),
("naturaldndn",    "accidentals.natural.arrowdowndown", 0, 'ox','cy', 1,'cy'),
#("naturalupdn",  "accidentals.natural.arrowboth", 0, 0,'cy', 1,'cy'),
#("peddot",       "pedal..",                0, 0,'by', 1,'by'),
#("pedP",         "pedal.P",                0, 0,'by', 1,'by'),
#("pedd",         "pedal.d",                0, 0,'by', 1,'by'),
#("pede",         "pedal.e",                0, 0,'by', 1,'by'),
#("pedPed",       "pedal.Ped",              0, 0,'by', 1,'by'),
#("pedstar",      "pedal.*",                0, 0,'by', 1,'by'),
#("peddash",      "pedal.M",                0, 0,'by', 1,'by'),
#("restdbllonga", "rests.M3",               0, 0,0.5, 1,0.5),
#("restlonga",    "rests.M2",               0, 0,0.5, 1,0.5),
#("restbreve",    "rests.M1",               0, 0,0, 1,0),
#("restbreveo",   "rests.M1o",              0, 0,0, 1,0),
#("restcrotchet", "rests.2",                0, 0,0.5, 1,0.5),
#("restcrotchetx", "rests.2classical",      0, 0,0.5, 1,0.5),
#("restcrotchetz", "rests.2z",              0, 0,0.5, 1,0.5),
#("restdemi",     "rests.5",                0, 0,'cy', 1,'cy'),
#("resthemi",     "rests.6",                0, 0,'cy', 1,'cy'),
#("restquasi",    "rests.7",                0, 0,'cy', 1,'cy'),
#("rest6",        "rests.8",                0, 0,'cy', 1,'cy'),
#("rest7",        "rests.9",                0, 0,'cy', 1,'cy'),
#("rest8",        "rests.10",               0, 0,'cy', 1,'cy'),
#("restminim",    "rests.1",                0, 0,0, 1,0),
#("restminimo",   "rests.1o",               0, 0,'oy', 1,'oy'),
#("restquaver",   "rests.3",                0, 0,'cy', 1,'cy'),
#("restsemi",     "rests.4",                0, 0,'cy', 1,'cy'),
#("restminim",    "rests.0",                0, 0,1, 1,1), # reuse restminim as semibreve rest
#("restsemibreveo", "rests.0o",             0, 0,'oy', 1,'oy'),
#("segno",        "scripts.segno",          0, 0.5,0.5, 1,0.5),
#("varsegno",     "scripts.varsegno",       0, 0.5,0.5, 1,0.5),
#("semibreve",    "noteheads.s0",           0, 0,0.5, 1,0.5),
#("sforzando",    "scripts.umarcato",       0, 0.5,0, 1,0),
#("sforzandodn",  "scripts.dmarcato",       0, 0.5,1, 1,1),
#("semisharp",    "accidentals.1",          0, 0,0.5, 1,0.5),
#("semisharp",    "accidentals.sharp.slashslash.stem", 0, 0,0.5, 1,0.5),
#("semisharp3",   "accidentals.sharp.slashslashslash.stem", 0, 0,0.5, 1,0.5),
#("sharp",        "accidentals.2",          0, 0,'cy', 1,'cy'),
("sharp",        "accidentals.sharp",      0, 'ox','cy', 1,'cy'),
#("sharp3",       "accidentals.sharp.slashslashslash.stemstem", 0, 0,'cy', 1,'cy'),
("sharpup",      "accidentals.sharp.arrowup", 0, 'ox','cy', 1,'cy'),
("sharpupup",      "accidentals.sharp.arrowupup", 0, 'ox','cy', 1,'cy'),
("sharpdn",      "accidentals.sharp.arrowdown", 0, 'ox','cy', 1,'cy'),
("sharpdndn",      "accidentals.sharp.arrowdowndown", 0, 'ox','cy', 1,'cy'),
#("sharpupdn",    "accidentals.sharp.arrowboth", 0, 0,'cy', 1,'cy'),
#("sesquisharp",  "accidentals.3",          0, 0,0.5, 1,0.5),
#("sesquisharp",  "accidentals.sharp.slashslash.stemstemstem", 0, 0,0.5, 1,0.5),
#("doublesharp",  "accidentals.4",          0, 0,0.5, 1,0.5),
#("doublesharp",  "accidentals.doublesharp", 0, 0,0.5, 1,0.5),
#("staccatissup", "scripts.dstaccatissimo", 0, 0.5,1, 1,1),
#("staccatissdn", "scripts.ustaccatissimo", 0, 0.5,0, 1,0),
#("staccato",     "scripts.staccato",       0, 0.5,0.5, 1,0.5),
#("staccato",     "dots.dot",               0, 0,0.5, 1,0.5),
#("snappizz",     "scripts.snappizzicato",  0, 0.5,'oy', 1,'oy'),
#("stopping",     "scripts.stopped",        0, 0.5,0.5, 1,0.5),
#("tailquaverdn", "flags.d3",               0, 'ox','oy', 1,'oy'),
#("tailquaverup", "flags.u3",               0, 'ox','oy', 1,'oy'),
#("tailsemidn",   "flags.d4",               0, 'ox','oy', 1,'oy'),
#("tailsemiup",   "flags.u4",               0, 'ox','oy', 1,'oy'),
#("taildemidn",   "flags.d5",               0, 'ox','oy', 1,'oy'),
#("taildemiup",   "flags.u5",               0, 'ox','oy', 1,'oy'),
#("tailhemidn",   "flags.d6",               0, 'ox','oy', 1,'oy'),
#("tailhemiup",   "flags.u6",               0, 'ox','oy', 1,'oy'),
#("tailquasidn",  "flags.d7",               0, 'ox','oy', 1,'oy'),
#("tailquasiup",  "flags.u7",               0, 'ox','oy', 1,'oy'),
#("tail6dn",      "flags.d8",               0, 'ox','oy', 1,'oy'),
#("tail6up",      "flags.u8",               0, 'ox','oy', 1,'oy'),
#("tail7dn",      "flags.d9",               0, 'ox','oy', 1,'oy'),
#("tail7up",      "flags.u9",               0, 'ox','oy', 1,'oy'),
#("tail8dn",      "flags.d10",              0, 'ox','oy', 1,'oy'),
#("tail8up",      "flags.u10",              0, 'ox','oy', 1,'oy'),
#("timeCbar",     "timesig.C22",            0, 0,0.5, 1,0.5),
#("timeC",        "timesig.C44",            0, 0,0.5, 1,0.5),
#("trill",        "scripts.trill",          0, 0.5,0, 1,0),
#("turn",         "scripts.turn",           0, 0.5,0.5, 1,0.5),
#("mirrorturn",   "scripts.reverseturn",    0, 0.5,0.5, 1,0.5),
#("invturn",      "scripts.slashturn",      0, 0.5,0.5, 1,0.5),
#("turnhaydn",    "scripts.haydnturn",      0, 0.5,0.5, 1,0.5),
#("openarrowup",  "arrowheads.open.11",     0, 'cx','cy', 1,'cy'),
#("openarrowdown", "arrowheads.open.1M1",   0, 'cx','cy', 1,'cy'),
#("openarrowleft", "arrowheads.open.0M1",   0, 'cx','cy', 1,'cy'),
#("openarrowright", "arrowheads.open.01",   0, 'cx','cy', 1,'cy'),
#("closearrowup",  "arrowheads.close.11",   0, 'cx','cy', 1,'cy'),
#("closearrowdown", "arrowheads.close.1M1", 0, 'cx','cy', 1,'cy'),
#("closearrowleft", "arrowheads.close.0M1", 0, 'cx','cy', 1,'cy'),
#("closearrowright", "arrowheads.close.01", 0, 'cx','cy', 1,'cy'),
#("upedalheel",   "scripts.upedalheel",     0, 0.5,'cy', 1,'cy'),
#("dpedalheel",   "scripts.dpedalheel",     0, 0.5,'cy', 1,'cy'),
#("upedaltoe",    "scripts.upedaltoe",      0, 0.5,0, 1,0),
#("dpedaltoe",    "scripts.dpedaltoe",      0, 0.5,1, 1,1),
#("acc2",         "accordion.accFreebase",  0, 0.5,0, 1,0),
#("acc2",         "accordion.freebass",     0, 0.5,0, 1,0),
#("acc3",         "accordion.accDiscant",   0, 0.5,0, 1,0),
#("acc3",         "accordion.discant",      0, 0.5,0, 1,0),
#("acc4",         "accordion.accStdbase",   0, 0.5,0, 1,0),
#("acc4",         "accordion.stdbass",      0, 0.5,0, 1,0),
#("accr",         "accordion.accBayanbase", 0, 0.5,0, 1,0),
#("accr",         "accordion.bayanbass",    0, 0.5,0, 1,0),
#("accdot",       "accordion.accDot",       0, 0.5,0.5, 1,0.5),
#("accdot",       "accordion.dot",          0, 0.5,0.5, 1,0.5),
#("accstar",      "accordion.accOldEE",     0, 0.5,0, 1,0),
#("accstar",      "accordion.oldEE",        0, 0.5,0, 1,0),
#("accpush",      "accordion.push",         0, 'ox',0, 1,0),
#("accpull",      "accordion.pull",         0, 'ox',0, 1,0),
#("diamondsemi",  "noteheads.s0diamond",    0, 0,0.5, 1,0.5),
#("diamondminim", "noteheads.s1diamond",    0, 0,0.5, 1,0.5),
#("diamondcrotchet", "noteheads.s2diamond", 0, 0,0.5, 1,0.5),
#("trianglesemi", "noteheads.s0triangle",   0, 0,0.5, 1,0.5),
#("triangleminim", "noteheads.d1triangle",  0, 0,0.5, 1,'iy'),
#("triangleminim", "noteheads.u1triangle",  0, 0,0.5, 1,'ay'),
#("trianglecrotchet", "noteheads.d2triangle", 0, 0,0.5, 1,'iy'),
#("trianglecrotchet", "noteheads.u2triangle", 0, 0,0.5, 1,'ay'),
#("crosssemi",    "noteheads.s0cross",      0, 0,0.5, 1,0.5),
#("crossminim",   "noteheads.s1cross",      0, 0,0.5, 1,'ay'),
#("crosscrotchet", "noteheads.s2cross",     0, 0,0.5, 1,'ay'),
#("crosscircle",  "noteheads.s2xcircle",    0, 0,0.5, 1,0.5),
#("slashsemi",    "noteheads.s0slash",      0, 0,0.5, 1,0.5),
#("slashminim",   "noteheads.s1slash",      0, 0,0.5, 1,'ay'),
#("slashcrotchet", "noteheads.s2slash",     0, 0,0.5, 1,'ay'),
#("lyrictie",     "ties.lyric.default",     0, 'ox','oy', 'ox','oy', {"x0":"ox", "x1":"ox", "y1":"oy"}),
#("lyrictieshort", "ties.lyric.short",      0, 'ox','oy', 'ox','oy', {"x0":"ox", "x1":"ox", "y1":"oy"}),
]

def writesfd(filepfx, fontname, encodingname, encodingsize, outlines, glyphlist):
    fname = filepfx + ".sfd"
    f = open(fname, "w")
    f.write("SplineFontDB: 3.0\n")
    f.write("FontName: %s\n" % fontname)
    f.write("FullName: %s\n" % fontname)
    f.write("FamilyName: %s\n" % fontname)
    f.write("Copyright: No copyright is claimed on this font file.\n")
    f.write("Version: %s\n" % verstring)
    f.write("ItalicAngle: 0\n")
    f.write("UnderlinePosition: -100\n")
    f.write("UnderlineWidth: 50\n")
    f.write("Ascent: 800\n")
    f.write("Descent: 200\n")
    f.write("LayerCount: 2\n")
    f.write("Layer: 0 0 \"Back\" 1\n")
    f.write("Layer: 1 0 \"Fore\" 0\n")
    f.write("UseXUID: 0\n")
    f.write("OS2Version: 0\n")
    f.write("OS2_WeightWidthSlopeOnly: 0\n")
    f.write("OS2_UseTypoMetrics: 1\n")
    f.write("CreationTime: 1252826347\n") # when I first wrote this prologue-writing code
    f.write("ModificationTime: %d\n" % time.time())
    f.write("OS2TypoAscent: 0\n")
    f.write("OS2TypoAOffset: 1\n")
    f.write("OS2TypoDescent: 0\n")
    f.write("OS2TypoDOffset: 1\n")
    f.write("OS2TypoLinegap: 0\n")
    f.write("OS2WinAscent: 0\n")
    f.write("OS2WinAOffset: 1\n")
    f.write("OS2WinDescent: 0\n")
    f.write("OS2WinDOffset: 1\n")
    f.write("HheadAscent: 0\n")
    f.write("HheadAOffset: 1\n")
    f.write("HheadDescent: 0\n")
    f.write("HheadDOffset: 1\n")
    f.write("OS2Vendor: 'PfEd'\n")
    f.write("DEI: 0\n")
    f.write("Encoding: %s\n" % encodingname)
    f.write("UnicodeInterp: none\n")
    f.write("NameList: Adobe Glyph List\n")
    f.write("DisplaySize: -96\n")
    f.write("AntiAlias: 1\n")
    f.write("FitToEm: 1\n")
    f.write("WinInfo: 64 8 2\n")
    f.write("BeginChars: %d %d\n" % (encodingsize, len(glyphlist)))

    i = 0
    for glyph in glyphlist:
        ourname, theirname, encoding, ox, oy = glyph[:5]
        bbox, path = outlines[ourname]
        char = getattr(font, ourname)
        xrt = lambda x: x * (3600.0 / (40*char.scale)) # potrace's factor of ten, ours of four
        yrt = lambda y: y * (3600.0 / (40*char.scale))
        xat = lambda x: xrt(x) - char.origin[0]
        yat = lambda y: yrt(y) - char.origin[1]
        xt = lambda x: xat(x) - xat(ox)
        yt = lambda y: yat(y) - yat(oy)
        if len(glyph) > 9 and "xw" in glyph[9]:
            width = xt(glyph[9]["xw"]) # explicitly specified width
        else:
            width = xt(bbox[2]) # mostly default to RHS of bounding box
        f.write("\nStartChar: %s\n" % theirname)
        f.write("Encoding: %d %d %d\n" % (encoding, encoding, i))
        f.write("Width: %g\n" % width)
        f.write("Flags: W\n")
        f.write("LayerCount: 2\n")
        f.write("Fore\n")
        f.write("SplineSet\n")
        for c in path:
            if c[0] == 'm':
                f.write("%g %g m 1\n" % (xt(c[1]), yt(c[2])))
            elif c[0] == 'l':
                f.write(" %g %g l 1\n" % (xt(c[3]), yt(c[4])))
            elif c[0] == 'c':
                f.write(" %g %g %g %g %g %g c 0\n" % (xt(c[3]), yt(c[4]), xt(c[5]), yt(c[6]), xt(c[7]), yt(c[8])))
            # closepath is not given explicitly
        f.write("EndSplineSet\n")
        f.write("EndChar\n")
        i = i + 1

    f.write("\nEndChars\n")

    f.write("EndSplineFont\n")
    f.close()

def test_glyph(args):
    # example usage:
    # ./glyphs.py --test braceupper | gs -sDEVICE=pngmono -sOutputFile=out.png -r72 -g1000x1000 -dBATCH -dNOPAUSE -q -
    # and then to view that in gui for correction:
    # convert out.png -fill white -colorize 75% zout.png && ./gui.py zout.png
    glyph = getattr(font, args.argument)
    glyph.testdraw()

def test_ps(args, scaled=True):
    char = getattr(font, args.argument)
    bbox, path = get_ps_path(char)
    if scaled:
        # Compensate for potrace's factor of ten, and ours of four
        xrt = lambda x: x * (3600.0 / (40*char.scale))
        yrt = lambda y: y * (3600.0 / (40*char.scale))
        xat = lambda x: xrt(x) - char.origin[0]
        yat = lambda y: yrt(y) - char.origin[1]
    else:
        xat = yat = lambda x: x
    print("%% bbox: %g %g %g %g" % (xat(bbox[0]), yat(bbox[1]), xat(bbox[2]), yat(bbox[3])))
    for c in path:
        if c[0] == 'm':
            print("%g %g moveto" % (xat(c[1]), yat(c[2])))
        elif c[0] == 'l':
            print("  %g %g lineto" % (xat(c[3]), yat(c[4])))
        elif c[0] == 'c':
            print("  %g %g %g %g %g %g curveto" % (xat(c[3]), yat(c[4]), xat(c[5]), yat(c[6]), xat(c[7]), yat(c[8])))
        elif c[0] == 'cp':
            print("closepath")

def test_ps_unscaled(args):
    return test_ps(args, scaled=False)

def mus_output(args):
    # Generate a Postscript prologue suitable for use with 'mus'
    glyphlist = [
    "accent",
    "acciaccatura",
    "appoggiatura",
    "arpeggio",
    "big0",
    "big1",
    "big2",
    "big3",
    "big4",
    "big5",
    "big6",
    "big7",
    "big8",
    "big9",
    "bowdown",
    "bowup",
    "bracelower",
    "braceupper",
    "bracketlower",
    "bracketupper",
    "breath",
    "breve",
    "clefC",
    "clefF",
    "clefG",
    "coda",
    "ditto",
    "doubleflat",
    "doublesharp",
    "dynamicf",
    "dynamicm",
    "dynamicp",
    "dynamics",
    "dynamicz",
    "fermata",
    "flat",
    "harmart",
    "harmnat",
    "headcrotchet",
    "headminim",
    "legato",
    "mordentlower",
    "mordentupper",
    "natural",
    "repeatmarks",
    "restbreve",
    "restcrotchet",
    "restdemi",
    "resthemi",
    "restminim",
    "restquaver",
    "restsemi",
    "segno",
    "semibreve",
    "sforzando",
    "sharp",
    "small0",
    "small1",
    "small2",
    "small3",
    "small4",
    "small5",
    "small6",
    "small7",
    "small8",
    "small9",
    "smallflat",
    "smallnatural",
    "smallsharp",
    "staccatissdn",
    "staccatissup",
    "staccato",
    "stopping",
    "taildemidn",
    "taildemiup",
    "tailhemidn",
    "tailhemiup",
    "tailquaverdn",
    "tailquaverup",
    "tailsemidn",
    "tailsemiup",
    "timeCbar",
    "timeC",
    "trill",
    "turn",
    ]
    encoding = [(i+33, glyphlist[i]) for i in range(len(glyphlist))]
    f = open("prologue.ps", "w")
    g = open("abspaths.txt", "w")
    f.write("save /m /rmoveto load def /l /rlineto load def\n")
    f.write("/hm {0 m} def /vm {0 exch m} def /hl {0 l} def /vl {0 exch l} def\n")
    f.write("/mm /moveto load def\n")
    f.write("/c {4 -1 roll 5 index add 4 -1 roll 4 index add 4 2 roll\n")
    f.write("    exch 3 index add exch 2 index add rcurveto} def\n")
    f.write("/vhc {0 5 1 roll 0 c} def /hvc {0 4 1 roll 0 exch c} def\n")
    f.write("/f /fill load def\n")
    f.write("/cp {currentpoint closepath moveto} def\n")
    f.write("/ip {0.02 dup scale 2 setlinecap 0 setlinejoin 0 setgray} def\n")
    f.write("/beam {newpath 50 sub moveto\n")
    f.write("  0 100 rlineto 50 add lineto 0 -100 rlineto closepath fill} def\n")
    f.write("/line {newpath moveto lineto setlinewidth stroke} def\n")
    f.write("/tdict 5 dict def\n")
    f.write("/tie {tdict begin\n")
    f.write("  /x2 exch def /yp exch def /x1 exch def /y exch def newpath\n")
    f.write("  x1 yp moveto\n")
    f.write("  x1 y abs add yp y add\n")
    f.write("  x2 y abs sub yp y add\n")
    f.write("  x2 yp curveto\n")
    f.write("  30 setlinewidth stroke\n")
    f.write("end} def\n")
    f.write("10 dict dup begin\n")
    f.write("/FontType 3 def /FontMatrix [1 0 0 1 0 0] def\n")
    f.write("/Encoding 256 array def 0 1 255 {Encoding exch /.notdef put} for\n")
    for code, name in encoding:
        f.write("Encoding %d /.%s put\n" % (code, name))
    f.write("/BBox %d dict def\n" % len(encoding))
    f.write("/CharacterDefs %d dict def\n" % len(encoding))
    fontbbox = (None,)*4

    pool = multiprocessing.Pool(args.jobs)
    char_data = dict(pool.imap_unordered(get_ps_path_map_function, encoding))

    for code, name in encoding:
        char = getattr(font, name)
        xrt = lambda x: x * (3600.0 / (40*char.scale)) # potrace's factor of ten, ours of four
        yrt = lambda y: y * (3600.0 / (40*char.scale))
        xat = lambda x: round(xrt(x) - char.origin[0])
        yat = lambda y: round(yrt(y) - char.origin[1])
        bbox, path = char_data[name]
        f.write("CharacterDefs /.%s {\n" % name)
        g.write("# %s\n" % name)
        output = "newpath"
        currentpoint = (None, None)
        for c in path:
            if c[0] == 'm':
                x1, y1 = xat(c[1]), yat(c[2])
                x0, y0 = currentpoint
                if x0 == None:
                    output = output + " %g %g mm" % (x1,y1)
                elif x0 == x1:
                    output = output + " %g vm" % (y1-y0)
                elif y0 == y1:
                    output = output + " %g hm" % (x1-x0)
                else:
                    output = output + " %g %g m" % (x1-x0, y1-y0)
                g.write("  %g %g moveto\n" % (x1,y1))
                currentpoint = x1,y1
            elif c[0] == 'l':
                x0, y0 = xat(c[1]), yat(c[2])
                x1, y1 = xat(c[3]), yat(c[4])
                if x0 == x1:
                    output = output + " %g vl" % (y1-y0)
                elif y0 == y1:
                    output = output + " %g hl" % (x1-x0)
                else:
                    output = output + " %g %g l" % (x1-x0, y1-y0)
                g.write("  %g %g lineto\n" % (x1,y1))
                currentpoint = x1,y1
            elif c[0] == 'c':
                x0, y0 = xat(c[1]), yat(c[2])
                x1, y1 = xat(c[3]), yat(c[4])
                x2, y2 = xat(c[5]), yat(c[6])
                x3, y3 = xat(c[7]), yat(c[8])
                if x0 == x1 and y2 == y3:
                    output = output + " %g %g %g %g vhc" % (y1-y0, x2-x1, y2-y1, x3-x2)
                elif y0 == y1 and x2 == x3:
                    output = output + " %g %g %g %g hvc" % (x1-x0, x2-x1, y2-y1, y3-y2)
                else:
                    output = output + " %g %g %g %g %g %g c" % (x1-x0, y1-y0, x2-x1, y2-y1, x3-x2, y3-y2)
                g.write("  %g %g %g %g %g %g curveto\n" % (x1,y1,x2,y2,x3,y3))
                currentpoint = x3,y3
            elif c[0] == 'cp':
                output = output + " cp"
                g.write("  closepath\n")
                currentpoint = None, None
        f.write("  " + output + " f\n")
        x0, y0 = xat(bbox[0]), yat(bbox[1])
        x1, y1 = xat(bbox[2]), yat(bbox[3])
        f.write("} put BBox /.%s [%g %g %g %g] put\n" % ((name, x0, y0, x1, y1)))
        g.write("  # bbox: %g %g %g %g\n" % (x0, y0, x1, y1))
        g.write("  # w,h: %g %g\n" % (x1-x0, y1-y0))
        fontbbox = update_bbox(fontbbox, x0,y0)
        fontbbox = update_bbox(fontbbox, x1,y1)
    x0,y0,x1,y1 = fontbbox
    f.write("/FontBBox [%g %g %g %g] def\n" % (x0, y0, x1, y1))
    f.write("/BuildChar {0 begin /char exch def /fontdict exch def\n")
    f.write("  /charname fontdict /Encoding get char get def\n")
    f.write("  0 0 fontdict /BBox get charname get aload pop setcachedevice\n")
    f.write("  fontdict /CharacterDefs get charname get exec\n")
    f.write("end} def\n")
    f.write("/BuildChar load 0 3 dict put\n")
    f.write("/UniqueID 1 def\n")
    f.write("end /MusicGfx exch definefont /MGfx exch def\n")
    f.write("/ss 1 string def\n")
    for code, name in encoding:
        f.write("/.%s {moveto MGfx setfont ss 0 %d put ss show} def\n" % (name, code))
    f.write("/.tdot {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  51 150 .staccato\n")
    f.write("  grestore 67 hm} def\n")
    f.write("/.tbreve {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  333 150 .breve\n")
    f.write("  grestore 351 hm} def\n")
    f.write("/.tsemibreve {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  207 150 .semibreve\n")
    f.write("  grestore 247 hm} def\n")
    f.write("/.tminim {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  160 150 .headminim\n")
    f.write("  24 setlinewidth newpath 304 186 moveto 850 vl stroke\n")
    f.write("  grestore 178 hm} def\n")
    f.write("/.tcrotchet {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  160 150 .headcrotchet\n")
    f.write("  24 setlinewidth newpath 304 186 moveto 850 vl stroke\n")
    f.write("  grestore 178 hm} def\n")
    f.write("/.tquaver {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  160 150 .headcrotchet\n")
    f.write("  24 setlinewidth newpath 304 186 moveto 850 vl stroke\n")
    f.write("  304 1050 .tailquaverup\n")
    f.write("  grestore 293 hm} def\n")
    f.write("/.tsemiquaver {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  160 150 .headcrotchet\n")
    f.write("  24 setlinewidth newpath 304 186 moveto 850 vl stroke\n")
    f.write("  304 1050 .tailquaverup\n")
    f.write("  304 900 .tailquaverup\n")
    f.write("  grestore 293 hm} def\n")
    f.write("/.tdemisemiquaver {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  160 150 .headcrotchet\n")
    f.write("  24 setlinewidth newpath 304 186 moveto 850 vl stroke\n")
    f.write("  304 1050 .tailquaverup\n")
    f.write("  304 900 .tailquaverup\n")
    f.write("  304 750 .tailquaverup\n")
    f.write("  grestore 293 hm} def\n")
    f.write("/.themidemisemiquaver {gsave\n")
    f.write("  currentpoint translate 0.5 dup scale\n")
    f.write("  160 150 .headcrotchet\n")
    f.write("  24 setlinewidth newpath 304 186 moveto 850 vl stroke\n")
    f.write("  304 1050 .tailquaverup\n")
    f.write("  304 900 .tailquaverup\n")
    f.write("  304 750 .tailquaverup\n")
    f.write("  304 600 .tailquaverup\n")
    f.write("  grestore 293 hm} def\n")
    f.write("/.df {\n")
    f.write("  currentfont currentpoint currentpoint .dynamicf moveto 216 hm setfont\n")
    f.write("} def\n")
    f.write("/.dm {\n")
    f.write("  currentfont currentpoint currentpoint .dynamicm moveto 460 hm setfont\n")
    f.write("} def\n")
    f.write("/.dp {\n")
    f.write("  currentfont currentpoint currentpoint .dynamicp moveto 365 hm setfont\n")
    f.write("} def\n")
    f.write("/.ds {\n")
    f.write("  currentfont currentpoint currentpoint .dynamics moveto 225 hm setfont\n")
    f.write("} def\n")
    f.write("/.dz {\n")
    f.write("  currentfont currentpoint currentpoint .dynamicz moveto 299 hm setfont\n")
    f.write("} def\n")
    f.close()
    g.close()

    # Now generate prologue.c.
    f = open("prologue.ps", "r")
    g = open("../prologue.c", "w")
    g.write("/* This file is automatically generated from the Mus glyph\n")
    g.write(" * descriptions. Do not expect changes made directly to this\n")
    g.write(" * file to be permanent. */\n")
    g.write("\n")
    g.write("#include <stdio.h>\n")
    g.write("\n")
    g.write("static char *prologue[] = {\n")
    wrapbuf = ""
    while 1:
        s = f.readline()
        if s == "": break
        ws = s.split()
        for w in ws:
            if len(wrapbuf) + 1 + len(w) <= 69:
                wrapbuf = wrapbuf + " " + w
            else:
                g.write("    \"%s\\n\",\n" % wrapbuf)
                wrapbuf = w
    g.write("    \"%s\\n\",\n" % wrapbuf)
    g.write("    NULL\n")
    g.write("};\n")
    g.write("\n")
    g.write("void write_prologue(FILE *fp) {\n")
    g.write("    char **p;\n")
    g.write("\n")
    g.write("    for (p=prologue; *p; p++)\n")
    g.write("        fputs(*p, fp);\n")
    g.write("}\n")

def mkdir(d):
    "Make a directory, tolerating it existing already."
    try:
        os.mkdir(d)
    except FileExistsError:
        pass

def symlink(a, b):
    "Make a symlink, tolerating it existing already."
    try:
        os.symlink(a, b)
    except FileExistsError:
        pass

def lilypond_output(args, do_main_font=True, do_brace_font=True):
    # Generate .sfd files and supporting metadata which we then
    # process with FontForge into a replacement system font set for
    # GNU LilyPond.

    def postprocess_svg_file(outfile):
        if args.svgfilter is None:
            return
        data = subprocess.check_output([args.svgfilter, outfile])
        with open(outfile, "wb") as f:
            f.write(data)

    def run_ff(infile, outfile, tableprefix=None, fontname=None):
        #ffscript = "Open($1); CorrectDirection(); Scale(2.0);"
        #ffscript = "Open($1); CorrectDirection();"
        #if tableprefix is not None:
        #    for table in ["LILC", "LILF", "LILY"]:
        #        ffscript += "LoadTableFromFile(\"{t}\", \"{p}.{t}\"); ".format(p=tableprefix, t=table)
        #if fontname is not None:
        #    ffscript += "SetFontNames(\"{n}\",\"{n}\",\"{n}\"); ".format(n=fontname)
        #ffscript += "Generate($2)"
        #print(" ".join(["fontforge", "-lang=ff", "-c",
        #                    ffscript, infile, outfile]))
        #check_call_devnull(["fontforge", "-lang=ff", "-c",
        #                    ffscript, infile, outfile])
        print(" ".join(["fontforge", "-lang=ff", "-script",
                            "script.ff", infile, outfile]))
        check_call_devnull(["fontforge", "-lang=ff", "-script",
                            "script.ff", infile, outfile])

        if outfile.endswith(".svg"):
            postprocess_svg_file(outfile)

    def writetables(filepfx, size, subfontname, outlines, glyphlist, bracesonly=0):
        #fname = filepfx + ".LILY"
        #f = open(fname, "w")
        #if not bracesonly:
        #    f.write("(staffsize . %.6f)\n" % size)
        #    f.write("(stafflinethickness . %.6f)\n" % (size/40.))
        #    f.write("(staff_space . %.6f)\n" % (size/4.))
        #    f.write("(linethickness . %.6f)\n" % (size/40.))
        #    bbbox = outlines["headcrotchet"][0]
        #    bwidth = bbbox[2] - bbbox[0]
        #    f.write("(black_notehead_width . %.6f)\n" % (bwidth * 3600.0 / (40*font.headcrotchet.scale) * (size/1000.)))
        #    f.write("(ledgerlinethickness . %.6f)\n" % (size/40.))
        #f.write("(design_size . %.6f)\n" % size)
        #if not bracesonly:
        #    f.write("(blot_diameter . 0.4)\n")
        #f.close()
        fname = filepfx + ".LILC"
        f = open(fname, "w")
        for glyph in glyphlist:
            ourname, theirname, encoding, ox, oy, ax, ay = glyph[:7]
            char = getattr(font, ourname)
            bbox, path = outlines[ourname]
            xrt = lambda x: x * (3600.0 / (40*char.scale)) # potrace's factor of ten, ours of four
            yrt = lambda y: y * (3600.0 / (40*char.scale))
            xat = lambda x: xrt(x) - char.origin[0]
            yat = lambda y: yrt(y) - char.origin[1]
            xt = lambda x: (xat(x) - xat(ox)) * (size/1000.)
            yt = lambda y: (yat(y) - yat(oy)) * (size/1000.)
            f.write("(%s .\n" % theirname)
            f.write("((bbox . (%.6f %.6f %.6f %.6f))\n" % (xt(bbox[0]), yt(bbox[1]), xt(bbox[2]), yt(bbox[3])))
            f.write("(subfont . \"%s\")\n" % subfontname)
            f.write("(subfont-index . %d)\n" % encoding)
            f.write("(attachment . (%.6f . %.6f))))\n" % (xt(ax), yt(ay)))
        f.close()
        fname = filepfx + ".LILF"
        f = open(fname, "w")
        f.write(subfontname)
        f.close()

    if do_main_font:
        # Allocate sequential Unicode code points in the private use
        # area for all the glyphs that don't already have a specific
        # ASCII code point where they need to live.
        code = 0xe100
        for i in range(len(lilyglyphlist)):
            g = lilyglyphlist[i]
            if g[2] == 0:
                lilyglyphlist[i] = g[:2] + (code,) + g[3:]
                code = code + 1

        # Construct the PS outlines via potrace, once for each glyph
        # we're actually using.
        pool = multiprocessing.Pool(args.jobs)
        outlines = dict(pool.imap_unordered(get_ps_path_map_function,
                                            set(g[0] for g in lilyglyphlist)))

        # PAINFUL HACK! Add invisible droppings above and below the
        # digits. This is because LP draws time signatures by
        # mushing the digits up against the centre line of the
        # stave, in the assumption that they'll be big enough to
        # overlap the top and bottom lines too. Personally I like
        # time signatures to _not quite_ collide with the stave
        # lines (except the 2nd and 4th, of course, which they can't
        # avoid), and that means I need LP to consider the digits'
        # bounding boxes to be just a bit wider.
        #
        # The pathlets appended here are of zero thickness, so they
        # shouldn't ever actually show up.
        #digits = ["big%d" % i for i in range(10)]
        #ymid = (outlines["big4"][0][1] + outlines["big4"][0][3]) / 2.0
        #for d in digits:
        #    char = getattr(font, d)
        #    d250 = 250.0 * (40*char.scale) / 3600.0
        #    u250 = 236.0 * (40*char.scale) / 3600.0 # empirically chosen
        #    one = 1.0 * (40*char.scale) / 3600.0
        #    yone = 0 # set to one to make the droppings visible for debugging
        #    bbox, path = outlines[d]
        #    xmid = (bbox[0] + bbox[2]) / 2.0
        #    path.append(('m', xmid, ymid-d250+yone))
        #    path.append(('l', xmid, ymid-d250+yone, xmid-one, ymid-d250))
        #    path.append(('l', xmid-one, ymid-d250, xmid+one, ymid-d250))
        #    path.append(('l', xmid+one, ymid-d250, xmid, ymid-d250+yone))
        #    path.append(('m', xmid, ymid+u250-yone))
        #    path.append(('l', xmid, ymid+u250-yone, xmid-one, ymid+u250))
        #    path.append(('l', xmid-one, ymid+u250, xmid+one, ymid+u250))
        #    path.append(('l', xmid+one, ymid+u250, xmid, ymid+u250-yone))
        #    bbox = (bbox[0], min(bbox[1], ymid-d250), \
        #            bbox[2], max(bbox[3], ymid+u250))
        #    outlines[d] = bbox, path

        # Go through the main glyph list and transform the
        # origin/attachment/width specifications into coordinates in
        # the potrace coordinate system.
        for i in range(len(lilyglyphlist)):
            g = list(lilyglyphlist[i])
            gid = g[0]
            glyph = getattr(font, gid)
            if len(g) > 7:
                prop = g[7]
                for k, v in prop.items():
                    if k[0] == "x":
                        v = getattr(glyph, v) * 40
                    elif k[0] == "y":
                        v = (glyph.canvas_size[1] - getattr(glyph, v)) * 40
                    else:
                        raise "Error!"
                    prop[k] = v
            else:
                prop = {}
            x0, y0, x1, y1 = outlines[gid][0]
            # Allow manual overriding of the glyph's logical
            # bounding box as written into the LILC table (used to
            # make arpeggio and trill elements line up right, and
            # also - for some reason - used for dynamics glyph
            # kerning in place of the perfectly good system in the
            # font format proper). If this happens, the attachment
            # points are given in terms of the overridden bounding
            # box.
            x0 = prop.get("x0", x0)
            x1 = prop.get("x1", x1)
            y0 = prop.get("y0", y0)
            y1 = prop.get("y1", y1)
            outlines[gid] = ((x0,y0,x1,y1),outlines[gid][1])
            xo = g[3]
            if type(xo) == str:
                xo = getattr(glyph, xo) * 40
            else:
                xo = x0 + (x1-x0) * xo
            g[3] = xo
            yo = g[4]
            if type(yo) == str:
                yo = (glyph.canvas_size[1] - getattr(glyph, yo)) * 40
            else:
                yo = y0 + (y1-y0) * yo
            g[4] = yo
            xa = g[5]
            if type(xa) == str:
                xa = getattr(glyph, xa) * 40
            else:
                xa = x0 + (x1-x0) * xa
            g[5] = xa
            ya = g[6]
            if type(ya) == str:
                ya = (glyph.canvas_size[1] - getattr(glyph, ya)) * 40
            else:
                ya = y0 + (y1-y0) * ya
            g[6] = ya
            lilyglyphlist[i] = tuple(g)

        mkdir("lilysrc")
        mkdir("lilyfonts")
        mkdir("lilyfonts-old")
        mkdir("lilyfonts-old/otf")
        mkdir("lilyfonts-old/svg")

        # Copy gonville.ily into the new-style output directory.
        #here = os.path.dirname(os.path.abspath(__file__))
        #shutil.copyfile(os.path.join(here, "gonville.ily"),
        #                "lilyfonts/gonville.ily")

        #for size in [11, 13, 14, 16, 18, 20, 23, 26]:
        #    prefix = "lilysrc/gonville-%d" % size
        #    sfd = prefix + ".sfd"
        #
        #    writesfd(prefix, "Gonville-%d" % size, "UnicodeBmp", 65537, outlines, lilyglyphlist)
        #    writetables(prefix, size, "gonville%d" % size, outlines, lilyglyphlist)
        #
        #    run_ff(sfd, "lilyfonts/gonville-%d.otf" % size, tableprefix=prefix)
        #    run_ff(sfd, "lilyfonts/gonville-%d.svg" % size)
        #    run_ff(sfd, "lilyfonts/gonville-%d.woff" % size)
        #
        #    run_ff(sfd, "lilyfonts-old/otf/emmentaler-%d.otf" % size, fontname="Emmentaler-%d" % size, tableprefix=prefix)
        #    run_ff(sfd, "lilyfonts-old/svg/emmentaler-%d.svg" % size, fontname="Emmentaler-%d" % size)
        #    run_ff(sfd, "lilyfonts-old/svg/emmentaler-%d.woff" % size, fontname="Emmentaler-%d" % size)
        for size in [23]:
            prefix = "lilysrc/gonville-%d" % size
            sfd = prefix + ".sfd"

            writesfd(prefix, "Gonville-%d" % size, "UnicodeBmp", 65537, outlines, lilyglyphlist)
            #writetables(prefix, size, "gonville%d" % size, outlines, lilyglyphlist)

            run_ff(sfd, "lilyfonts/gonville-%d.otf" % size, tableprefix=prefix)
            run_ff(sfd, "lilyfonts/gonville-%d.svg" % size)
            run_ff(sfd, "lilyfonts/gonville-%d.woff" % size)

            #run_ff(sfd, "lilyfonts-old/otf/emmentaler-%d.otf" % size, fontname="Emmentaler-%d" % size, tableprefix=prefix)
            #run_ff(sfd, "lilyfonts-old/svg/emmentaler-%d.svg" % size, fontname="Emmentaler-%d" % size)
            #run_ff(sfd, "lilyfonts-old/svg/emmentaler-%d.woff" % size, fontname="Emmentaler-%d" % size)

    # Now do most of that all over again for the specialist brace
    # font, if we're doing that. (The "-lilymain" option doesn't
    # regenerate the braces, because they're large and slow and it's
    # nice to be able to debug just the interesting bits.) Construct
    # the PS outlines via potrace, once for each glyph we're
    # actually using.
    if do_brace_font:
        outlines = {}
        bracelist = []
        gidlist = []
        bracerange = range(0, 576, 25) if args.fastbrace else range(576)
        for i in bracerange:
            char = GlyphContext()
            scaledbrace(char, 525 * (151./150)**i)
            gid = "brace%d" % i
            gidlist.append(gid)
            setattr(font, gid, char)

        pool = multiprocessing.Pool(args.jobs)
        outlines = dict(pool.imap_unordered(get_ps_path_map_function, gidlist))

        for i, gid in enumerate(gidlist):
            x0, y0, x1, y1 = outlines[gid][0]
            yh = (y0+y1)/2.0
            bracelist.append((gid, gid, 0xe100+i, x1, yh, x1, yh))

        prefix = "lilysrc/gonville-brace"
        sfd = prefix + ".sfd"

        writesfd(prefix, "Gonville-Brace", "UnicodeBmp", 65537, outlines, bracelist)
        writetables(prefix, 20, "gonvillebrace", outlines, bracelist, 1)

        run_ff(sfd, "lilyfonts/gonville-brace.otf", tableprefix=prefix)
        run_ff(sfd, "lilyfonts/gonville-brace.svg")
        run_ff(sfd, "lilyfonts/gonville-brace.woff")

        run_ff(sfd, "lilyfonts-old/otf/emmentaler-brace.otf", fontname="Emmentaler-Brace", tableprefix=prefix)
        run_ff(sfd, "lilyfonts-old/svg/emmentaler-brace.svg", fontname="Emmentaler-Brace")
        run_ff(sfd, "lilyfonts-old/svg/emmentaler-brace.woff", fontname="Emmentaler-Brace")

        symlink("emmentaler-brace.otf", "lilyfonts-old/otf/aybabtu.otf")
        symlink("emmentaler-brace.svg", "lilyfonts-old/svg/aybabtu.svg")
        symlink("emmentaler-brace.svg", "lilyfonts-old/svg/aybabtu.woff")

def lilypond_output_main(args):
    return lilypond_output(args, do_brace_font=False)
def lilypond_output_brace(args):
    return lilypond_output(args, do_main_font=False)

def simple_output(args):
    # Generate an .sfd file which can be compiled into a really
    # simple binary font in which all the glyphs are in the bottom
    # 256 code points.
    #
    # Future glyphs should be added to the end of this list, so that
    # the existing code point values stay the same.
    glyphlist = [
    ("big0", 0x30),
    ("big1", 0x31),
    ("big2", 0x32),
    ("big3", 0x33),
    ("big4", 0x34),
    ("big5", 0x35),
    ("big6", 0x36),
    ("big7", 0x37),
    ("big8", 0x38),
    ("big9", 0x39),
    ("dynamicf", 0x66),
    ("dynamicm", 0x6d),
    ("dynamicp", 0x70),
    ("dynamicr", 0x72),
    ("dynamics", 0x73),
    ("dynamicz", 0x7a),
    ("asciiplus", 0x2b),
    ("asciicomma", 0x2c),
    ("asciiminus", 0x2d),
    ("asciiperiod", 0x2e),
    ("accent", 0x3e),
    ("acclparen", 0x28),
    ("accrparen", 0x29),
    ("fixedbrace", 0x7b),
    "espressivo",
    "accslashbigup",
    "accslashbigdn",
    "acciaccatura",
    "appoggiatura",
    "arpeggioshort",
    "arpeggioarrowdown",
    "arpeggioarrowup",
    "trillwiggle",
    "bowdown",
    "bowup",
    "bracketlowerlily",
    "bracketupperlily",
    "breath",
    "revbreath",
    "varbreath",
    "revvarbreath",
    "caesura",
    "caesuracurved",
    "breve",
    "clefC",
    "clefF",
    "clefG",
    "clefTAB",
    "clefperc",
    "clefCsmall",
    "clefFsmall",
    "clefGsmall",
    "clefTABsmall",
    "clefpercsmall",
    "coda",
    "varcoda",
    "ditto",
    "fermata",
    "fermata0",
    "fermata2",
    "fermata3",
    "fermataup",
    "fermata0up",
    "fermata2up",
    "fermata3up",
    "semiflat",
    "semiflatslash",
    "flat",
    "flatup",
    "flatdn",
    "flatupdn",
    "flatslash",
    "flatslash2",
    "sesquiflat",
    "doubleflat",
    "doubleflatslash",
    "harmart",
    "harmartfilled",
    "harmnat",
    "flagopen",
    "flagthumb",
    "headcrotchet",
    "headminim",
    "legato",
    "portatoup",
    "portatodn",
    "mordentlower",
    "mordentupper",
    "mordentupperlong",
    "mordentupperlower",
    "upmordentupperlong",
    "upmordentupperlower",
    "mordentupperlongdown",
    "downmordentupperlong",
    "downmordentupperlower",
    "mordentupperlongup",
    "straightmordentupperlong",
    "natural",
    "naturalup",
    "naturaldn",
    "naturalupdn",
    "peddot",
    "pedP",
    "pedd",
    "pede",
    "pedPed",
    "pedPeddot",
    "pedstar",
    "peddash",
    "repeatmarks",
    "restdbllonga",
    "restlonga",
    "restbreve",
    "restcrotchet",
    "restcrotchetx",
    "restdemi",
    "resthemi",
    "restquasi",
    "restminimo",
    "restquaver",
    "restsemi",
    "restsemibreveo",
    "segno",
    "semibreve",
    "sforzando",
    "sforzandodn",
    "semisharp",
    "semisharp3",
    "sharp",
    "sharp3",
    "sharpup",
    "sharpdn",
    "sharpupdn",
    "sesquisharp",
    "doublesharp",
    "staccatissup",
    "staccatissdn",
    "staccato",
    "snappizz",
    "stopping",
    "tailquaverdn",
    "tailquaverup",
    "tailsemidn",
    "tailsemiup",
    "taildemidn",
    "taildemiup",
    "tailhemidn",
    "tailhemiup",
    "tailquasidn",
    "tailquasiup",
    "timeCbar",
    "timeC",
    "trill",
    "turn",
    "invturn",
    "openarrowup",
    "openarrowdown",
    "openarrowleft",
    "openarrowright",
    "closearrowup",
    "closearrowdown",
    "closearrowleft",
    "closearrowright",
    "upedalheel",
    "dpedalheel",
    "upedaltoe",
    "dpedaltoe",
    "acc2",
    "acc3",
    "acc4",
    "accr",
    "accdot",
    "accstar",
    "diamondsemi",
    "diamondminim",
    "diamondcrotchet",
    "trianglesemi",
    "triangleminim",
    "trianglecrotchet",
    "crosssemi",
    "crossminim",
    "crosscrotchet",
    "crosscircle",
    "slashsemi",
    "slashminim",
    "slashcrotchet",
    ]

    code = 0x21 # use sequential code points for anything not explicitly given

    codes = {}
    for i in range(0x7f, 0xa1):
        codes[i] = None # avoid these code points

    pool = multiprocessing.Pool(args.jobs)
    gidlist = [t[0] if type(t) == tuple else t
               for t in glyphlist]
    outlines = dict(pool.imap_unordered(get_ps_path_map_function, gidlist))

    for i in range(len(glyphlist)):
        gid = glyphlist[i]

        if type(gid) == tuple:
            # Allocate a specific code.
            gid, thiscode = gid
        else:
            while code in codes:
                code = code + 1
            assert code < 0x100
            thiscode = code
        codes[thiscode] = gid

        char = getattr(font, gid)

        xo, yo = char.origin
        if (xo,yo) == (1000,1000):
            # Hack: that particular origin is taken to indicate that
            # the origin was not set to anything more sensible by
            # GlyphContext.__init__, and so we instead use the
            # centre of the glyph's bounding box.
            x0, y0, x1, y1 = outlines[gid][0]
            xo = (x0+x1)/2
            yo = (y0+y1)/2
        else:
            xo = xo * char.scale / 3600. * 40
            yo = yo * char.scale / 3600. * 40
        if hasattr(char, "hy"):
            yo = (char.canvas_size[1] - char.hy) * 40
        if hasattr(char, "hx"):
            xo = char.hx * 40

        props = {}
        if hasattr(char, "width"):
            props["xw"] = char.width * 40 + xo

        glyphlist[i] = (gid, gid, thiscode, xo, yo, None, None, None, None, props)

    writesfd("gonville-simple", "Gonville-Simple", "UnicodeBmp", 65537, outlines, glyphlist)
    check_call_devnull(["fontforge", "-lang=ff", "-c",
                        "Open($1); CorrectDirection(); Generate($2)",
                        "gonville-simple.sfd", "gonville-simple.otf"])

def lilypond_list_missing_glyphs(args):
    # Run over the list of glyph names in another font file and list
    # the ones not known to this file. Expects one additional
    # argument which is the name of a font file.
    known = {}
    for g in lilyglyphlist:
        known[g[1]] = 1

    # Regexps
    import re
    ignored = [
    ".notdef",
    # I wasn't able to get LP to generate this glyph name at all; my
    # guess is that it's a legacy version of trill_element used in
    # older versions.
    "scripts.trilelement",
    # Similarly with this one, which was introduced _more_ recently
    # than trill_element.
    "scripts.trillelement",
    # Longa notes are not supported.
    re.compile(r'noteheads\.[ud]M2'),
    # Solfa note heads are not supported.
    re.compile(r'noteheads\..*(do|re|mi|fa|so|la|ti)'),
    # Ancient music is not supported.
    re.compile(r'.*vaticana.*'),
    re.compile(r'.*mensural.*'),
    re.compile(r'.*petrucci.*'),
    re.compile(r'.*medicaea.*'),
    re.compile(r'.*solesmes.*'),
    re.compile(r'.*hufnagel.*'),
    re.compile(r'.*kievan.*'),
    "scripts.ictus",
    "scripts.uaccentus",
    "scripts.daccentus",
    "scripts.usemicirculus",
    "scripts.dsemicirculus",
    "scripts.circulus",
    "scripts.augmentum",
    "scripts.usignumcongruentiae",
    "scripts.dsignumcongruentiae",
    ]

    check_call_devnull(["fontforge", "-lang=ff", "-c",
                        "Open($1); Save($2)", args.argument, "temp.sfd"])
    f = open("temp.sfd", "r")
    while 1:
        s = f.readline()
        if s == "": break
        ss = s.split()
        if len(ss) >= 2 and ss[0] == "StartChar:":
            name = ss[1]
            ok = known.get(name, 0)
            if not ok:
                for r in ignored:
                    if type(r) == str:
                        match = (r == name)
                    else:
                        match = r.match(name)
                    if match:
                        ok = 1
                        break
            if not ok:
                print(name)
    f.close()

def lilypond_list_our_glyphs(args):
    names = (g[1] for g in lilyglyphlist)
    for name in names:
        print(name)

def main():
    parser = argparse.ArgumentParser(description='')
    parser.add_argument(
        "--ver", "--version-string",
        help="Version string to put in output font files")
    parser.add_argument(
        "--svgfilter", help="Postprocessing filter for SVG font output")
    parser.add_argument(
        "-j", "--jobs", type=int,
        help="Number of potrace jobs to run concurrently")
    group = parser.add_mutually_exclusive_group()
    group.add_argument(
        "--test", action="store_const", dest="action", const=test_glyph,
        help="Generate renderable Postscript output for a single glyph")
    group.add_argument(
        "--testps", action="store_const", dest="action", const=test_ps,
        help="Generate raw Postscript path for a single glyph")
    group.add_argument(
        "--testpsunscaled", action="store_const", dest="action",
        const=test_ps_unscaled,
        help="Generate raw Postscript path for a single glyph, exactly as the "
        "coordinates came out of potrace")
    group.add_argument(
        "--lily", action="store_const", dest="action", const=lilypond_output,
        help="Generate font files for use with GNU Lilypond.")
    group.add_argument(
        "--lilymain", action="store_const", dest="action",
        const=lilypond_output_main,
        help="Generate just the main font files for GNU Lilypond.")
    group.add_argument(
        "--lilybrace", action="store_const", dest="action",
        const=lilypond_output_brace,
        help="Generate just the font file of braces for GNU Lilypond.")
    group.add_argument(
        "--lilycheck", action="store_const", dest="action",
        const=lilypond_list_missing_glyphs,
        help="Scan a Lilypond font file for any glyphs we don't have.")
    group.add_argument(
        "--lilylist", action="store_const", dest="action",
        const=lilypond_list_our_glyphs,
        help="List all the Lilypond glyph names we do have.")
    group.add_argument(
        "--mus", action="store_const", dest="action", const=mus_output,
        help="Generate a Postscript prologue suitable for SGT's legacy 'mus' "
        "score formatter.")
    group.add_argument(
        "--simple", action="store_const", dest="action", const=simple_output,
        help="Generate a simple font file you could use in running text.")
    parser.add_argument("argument", nargs="?",
                        help="glyph to use in test modes")
    parser.add_argument("--fastbrace", action="store_true",
                        help="Only build a small fraction of the brace sizes, "
                        "to speed up dev builds.")
    parser.set_defaults(verstring="version unavailable")
    args = parser.parse_args()

    global verstring
    verstring = args.verstring

    args.action(args)

if __name__ == '__main__':
    main()
