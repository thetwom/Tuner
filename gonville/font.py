from curves import *
import collections

class GlyphContext:
    def __init__(self):
        self.curves = {}
        self.curveid = 0
        self.canvas = self # simplest thing :-)
        self.extra = self.before = ""

        # Scale in units per inch. 1900 happens to be the scale at
        # which I drew most of these glyphs; unhelpfully, the real
        # scale used by Lilypond (and Mus) is 3600.
        self.scale = 1900 # default unless otherwise specified
        # Location of the glyph's origin, in output coordinates
        # (i.e. the location in the glyph's own coordinates will be
        # this, divided by 3600, multiplied by self.scale).
        self.origin = 1000, 1000
        # Default size of canvas (in glyph coordinates) on which we
        # will display the image.
        self.canvas_size = 1000, 1000
        # Default extra resolution factor over the glyph coordinates
        # used for rendering to potrace.
        self.trace_res = 4
        # Default number of points to interpolate along each curve.
        self.curve_res = 1001
    def create_line(self, *args, **kw):
        return None
    def delete(self, *args, **kw):
        pass
    def makeps(self):
        out = "gsave 1 setlinecap\n"
        out = out + self.before + "\n"
        for cid, curve in self.curves.items():
            for it in range(self.curve_res):
                t = it / float(self.curve_res-1)
                x, y = curve.compute_point(t)
                nib = curve.compute_nib(t)
                if type(nib) == tuple:
                    radius, angle, fdist, bdist = nib
                    c = cos(angle)
                    s = -sin(angle)
                    out = out + "newpath %g %g moveto %g %g lineto %g setlinewidth stroke\n" % \
                    (x+c*fdist, y+s*fdist, x-c*bdist, y-s*bdist, 2*radius)
                elif nib != 0:
                    out = out + "newpath %g %g %g 0 360 arc fill\n" % (x, y, nib)
        e = self.extra
        if not (type(e) == tuple or type(e) == list):
            e = (e,)
        for ee in e:
            if type(ee) == str:
                out = out + ee + "\n"
            else:
                out = out + ee.makeps()
        out = out + "\ngrestore\n"
        return out
    def testdraw(self):
        print("gsave clippath flattenpath pathbbox 0 exch translate")
        print("1 -1 scale pop pop pop")
        print(self.makeps())
        print("grestore showpage")

class container:
    pass
font = container()

# 2x2 matrix multiplication.
def matmul(m1, m2):
    (a,b,c,d), (e,f,g,h) = (m1, m2)
    return (a*e+b*g, a*f+b*h, c*e+d*g, c*f+d*h)
# 2x2 matrix inversion.
def matinv(m):
    (a,b,c,d) = m
    det = a*d-b*c
    return (d/det, -b/det, -c/det, a/det)

# Turn a straight line from (0,0) to (1,1) into a quadratic curve
# hitting the same two points but passing through (1/2,1/2-k)
# instead of (1/2,1/2).
def depress(t,k):
    return t - t*(1-t)*4*k

# Nib helper function which sets up a chisel nib with one end on the
# curve and the other end at a specified other point.
def ptp_nib(c,x,y,t,theta,x1,y1,nr):
    angle = atan2(y-y1, x1-x)
    dist = sqrt((y-y1)**2 + (x1-x)**2)
    return nr, angle, dist, 0

# Nib helper function which sets up a chisel nib with one end
# following the curve and the other end following a completely
# different curve or chain of curves.
def follow_curveset_nib(c,x,y,t,theta,carray,i,n,r):
    tt = (t + i) * len(carray) / n
    ti = int(tt)
    if ti == len(carray):
        ti = ti - 1
    x1, y1 = carray[ti].compute_point(tt-ti)
    return ptp_nib(c,x,y,t,theta,x1,y1,r)

# Function which draws a blob on the end of a line.
def blob(curve, end, whichside, radius, shrink, nibradius=None):
    if nibradius == None:
        nibradius = curve.compute_nib(end)
        assert type(nibradius) != tuple
    x, y = curve.compute_point(end)
    dx, dy = curve.compute_direction(end)
    if end == 0:
        dx, dy = -dx, -dy
    dlen = sqrt(dx*dx + dy*dy)
    dx, dy = dx/dlen, dy/dlen
    if whichside == 'r':
        nx, ny = -dy, dx
    elif whichside == 'l':
        nx, ny = dy, -dx
    # We want to draw a near-circle which is essentially a single
    # involute going all the way round, so that its radius shrinks
    # from 'radius' to (radius-shrink) on the way round. That means
    # it needs to unwind once round a circle of circumference
    # 'shrink'.
    r = shrink/(2*pi)
    cx = x + radius*nx - r*dx
    cy = y + radius*ny - r*dy
    for i in range(4):
        if whichside == 'r':
            newnx, newny = -ny, nx
        elif whichside == 'l':
            newnx, newny = ny, -nx
        radius = radius - shrink/4.
        newx = cx - r*nx - radius*newnx
        newy = cy - r*ny - radius*newny
        newcurve = CircleInvolute(curve.cont, x, y, dx, dy, newx, newy, nx, ny)
        x, y, dx, dy, nx, ny = newx, newy, nx, ny, newnx, newny
        newcurve.nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,cx,cy,nibradius)

# Construct a PostScript path description which follows the centre
# of some series of curve objects and visits other points in
# between. Used to arrange that one quaver tail doesn't go past
# another, and similar.
Reversed = collections.namedtuple("Reversed", "curve")
def clippath(elements):
    coords = []
    for e in elements:
        if isinstance(e, Curve):
            for it in range(e.cont.curve_res):
                t = it / float(e.cont.curve_res-1)
                coords.append(e.compute_point(t))
        elif isinstance(e, Reversed):
            for it in range(e.curve.cont.curve_res):
                t = it / float(e.curve.cont.curve_res-1)
                coords.append(e.curve.compute_point(1 - t))
        else:
            # Plain coordinate pair.
            coords.append(e)
    for i in range(len(coords)):
        if i == 0:
            coords[i] = "%g %g moveto" % coords[i]
        else:
            coords[i] = "%g %g lineto" % coords[i]
    coords.append("closepath")
    return " ".join(coords)

def make_glyph(fn, args=(), postprocess=lambda x: x):
    cont = GlyphContext()
    fn(cont, *args)
    return postprocess(cont)

# Decorator to make it convenient to define a lot of glyphs inside
# 'font' by actually writing a function that returns their
# GlyphContext.
def define_glyph(name, **kws):
    def decorator(fn):
        setattr(font, name, make_glyph(fn, **kws))
        # Pass through the function itself unchanged, so that we can
        # chain multiple decorators
        return fn
    return decorator

# Another decorator that just defines a name in this module's global
# namespace, for making subcomponents that are reused in glyphs but
# are not glyphs in their own right.
def define_component(name, **kws):
    def decorator(fn):
        globals()[name] = make_glyph(fn, **kws)
        return fn
    return decorator

# ----------------------------------------------------------------------
# Postprocessor for making small clefs.

smallclef_scale = 0.8
def makesmallclef(clef):
    cont = GlyphContext()
    cont.extra = "%g dup scale" % smallclef_scale, clef
    cont.scale = clef.scale
    cont.origin = clef.origin
    for attr in ['hy', 'ox']:
        if hasattr(clef, attr):
            setattr(cont, attr, .8 * getattr(clef, attr))
    return cont

# ----------------------------------------------------------------------
# Shared component used in variant forms of both G and C clef.

@define_component("clefCstraightright")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 437, 420, 740, 420)
    c1 = StraightLine(cont, 740, 420, 740, 186)
    c2 = StraightLine(cont, 437, 528, 740, 528)
    c3 = StraightLine(cont, 740, 528, 740, 762)
    c0.weld_to(1, c1, 0)
    c2.weld_to(1, c3, 0)
    # End saved data
    c0.nib = c2.nib = 8, pi/2, 20, 20
    cont.default_nib = 8
    blob(c1, 1, 'r', 40, 9)
    blob(c3, 1, 'l', 40, 9)

    cont.hy = 474

# ----------------------------------------------------------------------
# G clef (treble).
#
# The G clef is drawn in two parts. First, we have a connected
# sequence of curves which draws the main outline of the clef (with
# varying stroke width as detailed below). But at the very top of
# the clef, the two edges of the thick stroke diverge: the outside
# of the curve is pointy, but the inside curves round smoothly. So
# we have a secondary context containing an alternative version of
# the top two curves (c6,c7), used as the inner smooth curve. The
# actual drawing uses the main context, but with an exciting nib
# function for c6 and c7 which moves one end of the nib along the
# main curve while the other tracks the curves in the secondary
# context.

@define_glyph("clefG")
@define_glyph("clefGsmall", postprocess=makesmallclef)
def _(cont_main):
    # Secondary curves.
    cont = GlyphContext()
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 603, 161, -0.33035, -0.943858, 563, 145, -0.943858, 0.33035)
    c1 = CircleInvolute(cont, 563, 145, -0.943858, 0.33035, 504.709, 289.062, 0.208758, 0.977967)
    c0.weld_to(1, c1, 0)
    # End saved data
    tc0, tc1 = c0, c1

    # Main context.
    cont = cont_main
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 528, 654, -0.90286, -0.429934, 569, 507, 1, 0)
    c1 = CircleInvolute(cont, 569, 507, 1, 0, 666, 607, 0, 1)
    c2 = CircleInvolute(cont, 666, 607, 0, 1, 549, 715, -1, 0)
    c3 = CircleInvolute(cont, 549, 715, -1, 0, 437, 470, 0.581238, -0.813733)
    c4 = CircleInvolute(cont, 437, 470, 0.581238, -0.813733, 536, 357, 0.731055, -0.682318)
    c5 = CircleInvolute(cont, 536, 357, 0.731055, -0.682318, 603, 161, -0.33035, -0.943858)
    c6 = CircleInvolute(cont, 603, 161, -0.33035, -0.943858, 559, 90, -0.83205, -0.5547)
    c7 = CircleInvolute(cont, 559, 90, -0.77193, 0.635707, 500, 267, 0.211282, 0.977425)
    c8 = StraightLine(cont, 500, 267, 605.66, 762)
    c9 = ExponentialInvolute(cont, 606, 762, 0.211282, 0.977425, 598, 856, -0.514496, 0.857493)
    c10 = CircleInvolute(cont, 598, 856, -0.514496, 0.857493, 446, 865, -0.633238, -0.773957)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0, 1)
    c7.weld_to(1, c8, 0)
    c8.weld_to(1, c9, 0)
    c9.weld_to(1, c10, 0)
    # End saved data

    # for use in clipping
    cont.left_side_path = [c2, c3, c4, c5, c6]
    cont.right_side_path = [Reversed(c9), Reversed(c8), Reversed(c7)]

    cont.default_nib = lambda c,x,y,t,theta: 17+11*cos(theta-c.nibdir(t))
    c0.nibdir = c1.nibdir = c2.nibdir = lambda t: 0
    phi = c4.compute_theta(1)
    c3.nibdir = lambda t: phi*t
    c4.nibdir = lambda t: phi
    gamma = c5.compute_theta(1) - pi
    c5.nibdir = lambda t: phi + (gamma-phi)*t
    c5.nib = lambda c,x,y,t,theta: 18+10*cos(theta-c.nibdir(t))
    c6.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0,tc1],0,2,8)
    c7.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0,tc1],1,2,8)
    c8.nib = c9.nib = c10.nib = 8
    blob(c10, 1, 'r', 45, 9)

    # I drew this one at a silly scale for some reason
    cont.scale = 1736
    cont.origin = 800, 822

    cont.hy = 1000 - (cont.origin[1] * cont.scale / 3600.) # I should probably work this out better

@define_glyph("clefGdouble")
@define_glyph("clefGdoublesmall", postprocess=makesmallclef)
def _(cont):
    xoffset = 155
    clip = clippath(font.clefG.left_side_path +
                    [(559, 0), (0, 0), (0, 1000), (666, 1000)])
    cont.extra = ("gsave matrix currentmatrix",
                  "%g 0 translate" % xoffset,
                  font.clefG,
                  "newpath", clip, "clip",
                  "setmatrix", font.clefG, "grestore")
    cont.scale = font.clefG.scale
    cont.origin = font.clefG.origin
    cont.hy = font.clefG.hy

@define_glyph("clefGtenorised")
@define_glyph("clefGtenorisedsmall", args=(smallclef_scale,))
def _(cont, scale = 1.0):
    # This clef has to mark two specific stave lines, so we can't do
    # the usual thing of making its small version by scaling down the
    # larger one. Instead we must make the large and small versions of
    # this clef in the same way, starting from larger and smaller
    # versions of the component pieces.
    clip = clippath(font.clefG.left_side_path +
                    [(559, 0), (1000, 0), (1000, 1000), (598, 1000)] +
                    font.clefG.right_side_path +
                    [(559, 0), (1000, 0), (1000, 1000), (666, 1000)])
    cont.scale = font.clefG.scale
    cont.extra = ("%g dup scale" % scale, font.clefG,
                  "gsave newpath", clip, "clip",
                  "0 %g translate" % font.clefG.hy,
                  "%g dup scale" % (cont.scale / clefCstraightright.scale),
                  "0 %g translate" % (-clefCstraightright.hy),
                  "40 %g translate" % (-2375/18 * 2 / scale),
                  clefCstraightright, "grestore")
    cont.hy = font.clefG.hy * scale

# ----------------------------------------------------------------------
# F clef (bass).

@define_glyph("clefF")
@define_glyph("clefFsmall", postprocess=makesmallclef)
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 534, 761, 0.964764, -0.263117, 783, 479, 0, -1)
    c1 = CircleInvolute(cont, 783, 479, 0, -1, 662, 352, -0.999133, -0.0416305)
    c2 = CircleInvolute(cont, 662, 352, -0.999133, -0.0416305, 585, 510, 0.993884, 0.110432)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: ((c.nibmax+6) + (c.nibmax-6)*cos(2*(theta-c.nibdir(t))))/2

    theta0 = c0.compute_theta(0)
    theta1 = c1.compute_theta(0)
    c0.nib = lambda c,x,y,t,theta: (34 + (6-34)*abs((theta-theta1)/(theta0-theta1))**1.4)
    c1.nibdir = lambda t: theta1
    c1.nibmax = 34
    c2.nibdir = lambda t: theta1
    c2.nibmax = 12
    blob(c2, 1, 'l', 47, 3)

    # The two dots.
    cont.extra = "newpath 857 417 20 0 360 arc fill " + \
    "newpath 857 542 20 0 360 arc fill";

    # The hot-spot y coordinate is precisely half-way between the
    # two dots.
    cont.hy = (417+542)/2.0

# ----------------------------------------------------------------------
# C clef (alto, tenor).
#
# This one took considerable thinking! The sharp point between c3
# and c4 is difficult to achieve, and I eventually did it by having
# the nib narrow to 2 pixels at that point - so it isn't actually
# perfectly sharp, but I can't bring myself to care. So what happens
# is simply that the backward C shape is drawn with a nib function
# that narrows to a near-point, and then turns a corner to go down
# to the centreline via c4. Meanwhile, tc0 defines a cutoff line at
# which the plain circular nib going along c3 suddenly shifts to
# being a point-to-point nib of the same radius with its other end
# at the end of tc0 on the centreline.
#
# (Note that, due to the nontrivial nib width at the point where the
# cutoff occurs, the actual edge that ends up drawn will not run
# precisely along tc0. Again, I don't care.)

@define_glyph("clefC")
@define_glyph("clefCsmall", postprocess=makesmallclef)
def _(cont_main):
    # Secondary context.
    cont = GlyphContext()
    # Saved data from gui.py
    c0 = StraightLine(cont, 698, 474, 744, 398)
    c1 = StraightLine(cont, 698, 474, 744, 550)
    # End saved data
    tc0, tc1 = c0, c1

    cont = cont_main
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 698, 242, 0.707107, -0.707107, 762, 216, 1, 0)
    c1 = CircleInvolute(cont, 762, 216, 1, 0, 870, 324, 0, 1)
    c2 = CircleInvolute(cont, 870, 324, 0, 1, 773, 436, -1, 0)
    c3 = CircleInvolute(cont, 773, 436, -1, 0, 705, 355, -0.0434372, -0.999056)
    c4 = CircleInvolute(cont, 705, 355, -0.220261, 0.975441, 635, 474, -0.894427, 0.447214)
    c5 = CircleInvolute(cont, 698, 706, 0.707107, 0.707107, 762, 732, 1, 0)
    c6 = CircleInvolute(cont, 762, 732, 1, 0, 870, 624, 0, -1)
    c7 = CircleInvolute(cont, 870, 624, 0, -1, 773, 512, -1, -0)
    c8 = CircleInvolute(cont, 773, 512, -1, -0, 705, 593, -0.0434372, 0.999056)
    c9 = CircleInvolute(cont, 705, 593, -0.220261, -0.975441, 635, 474, -0.894427, -0.447214)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0, 1)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0)
    c7.weld_to(1, c8, 0)
    c8.weld_to(1, c9, 0, 1)
    # End saved data

    def mad_cclef_points(c,x,y,t,theta,nibfn,cutoffline):
        # Get the ordinary nib width which the normal nib function
        # would specify for this point on the curve.
        nw = nibfn(c,x,y,t,theta)
        # If we're to the left of the cutoff line, do a PTP nib
        # pointing to the start point of the cutoff line.
        cx0, cy0, cx1, cy1 = cutoffline.inparams
        cx = cx0 + (cx1-cx0) * (y-cy0) / (cy1-cy0)
        if x < cx:
            return ptp_nib(c,x,y,t,theta,cx0,cy0,nw)
        else:
            return nw
    c0.nib = lambda c,x,y,t,theta: 6
    c1.nib = c2.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, pi, k, 0))(44*((x-min(x1,x2))/abs(x2-x1))**2)))(c.compute_x(0),c.compute_x(1))
    c3.nib = lambda c,x,y,t,theta: mad_cclef_points(c,x,y,t,theta,c0.nib,tc0)
    cx0,cy0 = tc0.compute_point(0)
    r0 = c3.compute_nib(1)[0]
    c4.nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,cx0,cy0,r0)

    c5.nib = lambda c,x,y,t,theta: 6
    c6.nib = c7.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, pi, k, 0))(44*((x-min(x1,x2))/abs(x2-x1))**2)))(c.compute_x(0),c.compute_x(1))
    c8.nib = lambda c,x,y,t,theta: mad_cclef_points(c,x,y,t,theta,c5.nib,tc1)
    cx1,cy1 = tc1.compute_point(0)
    r1 = c8.compute_nib(1)[0]
    c9.nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,cx1,cy1,r1)

    blob(c0, 0, 'l', 28, 6)
    blob(c5, 0, 'r', 28, 6)

    cont.extra = \
    "/box { newpath 3 index 3 index moveto 3 index 1 index lineto 1 index 1 index lineto 1 index 3 index lineto closepath fill pop pop pop } def " + \
    "537 206 601 742 box " + \
    "625 206 641 742 box "

@define_glyph("clefCstraight")
@define_glyph("clefCstraightsmall", postprocess=makesmallclef)
def _(cont):
    cont.extra = (font.clefC.extra,
                  "gsave newpath 641 0 moveto 0 1000 rlineto 1000 1000 lineto "
                  "1000 0 lineto closepath clip",
                  clefCstraightright, "grestore")

# ----------------------------------------------------------------------
# Percussion 'clefs'.

@define_glyph("clefperc")
@define_glyph("clefpercsmall", postprocess=makesmallclef)
def _(cont):
    cont.extra = \
    "newpath 410 368 moveto 410 632 lineto " + \
    "470 632 lineto 470 368 lineto closepath fill " + \
    "newpath 530 368 moveto 530 632 lineto " + \
    "590 632 lineto 590 368 lineto closepath fill "

    cont.ox = 320

@define_glyph("clefpercbox")
@define_glyph("clefpercboxsmall", postprocess=makesmallclef)
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 450, 282, 550, 282)
    c1 = StraightLine(cont, 550, 282, 550, 718)
    c2 = StraightLine(cont, 550, 718, 450, 718)
    c3 = StraightLine(cont, 450, 718, 450, 282)
    c0.weld_to(1, c1, 0)
    c0.weld_to(0, c3, 1)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: (10, pi/2, 0, 40) if 455 < x < 545 else 10
    c2.nib = lambda c,x,y,t,theta: (10, pi/2, 40, 0) if 455 < x < 545 else 10
    c1.nib = c3.nib = 10

    cont.ox = 380

# ----------------------------------------------------------------------
# Tablature 'clef': just the letters "TAB", written vertically in a
# vaguely calligraphic style.

@define_glyph("clefTAB")
@define_glyph("clefTABsmall", postprocess=makesmallclef)
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 100, 104, 900, 104)
    c1 = StraightLine(cont, 100, 368, 900, 368)
    c2 = StraightLine(cont, 100, 632, 900, 632)
    c3 = StraightLine(cont, 100, 896, 900, 896)
    c4 = CircleInvolute(cont, 354, 153, 0.564684, -0.825307, 443, 128, 0.964764, 0.263117)
    c5 = CircleInvolute(cont, 443, 128, 0.964764, 0.263117, 555, 118, 0.83205, -0.5547)
    c6 = CircleInvolute(cont, 463, 136, 0.110432, 0.993884, 434, 313, -0.571064, 0.820905)
    c7 = CircleInvolute(cont, 434, 313, -0.571064, 0.820905, 376, 334, -0.83205, -0.5547)
    c8 = CircleInvolute(cont, 333, 603, 0.98387, 0.178885, 486, 384, 0.0416305, -0.999133)
    c9 = CircleInvolute(cont, 486, 384, 0.110432, 0.993884, 527, 572, 0.398726, 0.91707)
    c10 = CircleInvolute(cont, 527, 572, 0.398726, 0.91707, 572, 605, 0.963518, -0.267644)
    c11 = CircleInvolute(cont, 441, 541, 0.724999, 0.688749, 482, 555, 0.977176, -0.21243)
    c12 = CircleInvolute(cont, 355, 698, 0.5547, -0.83205, 464, 648, 0.998168, -0.0604951)
    c13 = CircleInvolute(cont, 464, 648, 0.998168, -0.0604951, 551, 700, 0, 1)
    c14 = CircleInvolute(cont, 551, 700, 0, 1, 475, 756, -0.995634, 0.0933407)
    c15 = CircleInvolute(cont, 475, 756, 0.98995, 0.141421, 555, 822, 0.0416305, 0.999133)
    c16 = CircleInvolute(cont, 555, 822, 0.0416305, 0.999133, 427, 856, -0.815507, -0.578747)
    c17 = CircleInvolute(cont, 446, 667, 0.119145, 0.992877, 417, 815, -0.447214, 0.894427)
    c18 = CircleInvolute(cont, 417, 815, -0.447214, 0.894427, 342, 858, -0.876812, -0.480833)
    c4.weld_to(1, c5, 0)
    c6.weld_to(1, c7, 0)
    c8.weld_to(1, c9, 0, 1)
    c9.weld_to(1, c10, 0)
    c12.weld_to(1, c13, 0)
    c13.weld_to(1, c14, 0)
    c14.weld_to(1, c15, 0, 1)
    c15.weld_to(1, c16, 0)
    c17.weld_to(1, c18, 0)
    # End saved data

    # Stave lines as guides used when I was drawing it
    c0.nib = c1.nib = c2.nib = c3.nib = 0
    cont.default_nib = lambda c,x,y,t,theta: 12+10*sin(theta)**2

    # Vertical of T needs not to overlap top of T
    c6.nib = lambda c,x,y,t,theta: (12, theta+pi/2, 10*sin(theta)**2, 10*sin(theta)**2)

    # Special nib for crossbar of A
    c11.nib = lambda c,x,y,t,theta: 12-6*t

    cont.hy = (c1.compute_y(0) + c2.compute_y(0)) / 2.0

# ----------------------------------------------------------------------
# Quaver tails.

# Vertical space between multiple tails, which after some
# experimentation I decided should be different between the up- and
# down-pointing stems.
#
# For down stems (so that the tails have to fit under the note
# head), it's about 80% of the spacing between adjacent stave lines
# (which is, in this coordinate system, 250 * 1900/3600 = 132 minus
# 1/18. For up stems, it's a bit more than that: closer to 87%.
quavertaildispdn = 105
quavertaildispup = 115

def clipup(tail):
    # Clipped version of an up-quaver-tail designed to fit above
    # another identical tail and stop where it crosses the latter.
    cont = GlyphContext()
    clip = clippath([tail.c0, tail.c1, (900,1900), (900,100), (100,100), (100,1900)])
    cont.extra = "gsave 0 %g translate newpath" % quavertaildispup, clip, \
    "clip 0 -%g translate" % quavertaildispup, tail, "grestore"
    cont.ox = tail.ox
    cont.oy = tail.oy
    return cont

def clipdn(tail):
    # Clipped version of a down-quaver-tail designed to fit below
    # another identical tail and stop where it crosses the latter.
    cont = GlyphContext()
    clip = clippath([tail.c0, tail.c1, (900,100), (900,1900), (100,1900), (100,900)])
    cont.extra = "gsave 0 -%g translate newpath" % quavertaildispdn, clip, \
    "clip 0 %g translate" % quavertaildispdn, tail, "grestore"
    cont.ox = font.tailquaverdn.ox
    cont.oy = font.tailquaverdn.oy
    return cont

def multiup(n, tail):
    cont = GlyphContext()
    # Up-pointing multitail.
    short = clipup(tail)
    yextra = quavertaildispup*(n-1)
    cont.canvas_size = 1000, 1000 + yextra
    cont.extra = ("0 %g translate" % yextra, tail,) + ("0 -%g translate" % quavertaildispup, short) * (n-1)
    cont.ox = tail.ox
    cont.oy = tail.oy - quavertaildispup*(n-1) + yextra
    cont.origin = tail.origin
    cont.origin = (cont.origin[0], (cont.origin[1] + quavertaildispup*(n-1) - yextra) * 3600. / cont.scale - 180)
    return cont

def multidn(n, tail):
    # Down-pointing multitail.
    cont = GlyphContext()
    short = clipdn(tail)
    yextra = quavertaildispdn*(n-1)
    cont.canvas_size = 1000, 1000 + yextra
    cont.extra = (tail,) + ("0 %g translate" % quavertaildispdn, short) * (n-1)
    cont.ox = tail.ox
    cont.oy = tail.oy + quavertaildispdn*(n-1)
    cont.origin = tail.origin
    cont.origin = (cont.origin[0], cont.origin[1] - quavertaildispdn*(n-1)*3600./cont.scale)
    return cont

@define_glyph("tailquaverup")
def _(cont):
    # Full-size tail for a quaver with an up-pointing stem.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 567, 0.948683, 0.316228, 611, 607, 0.7282, 0.685365)
    c1 = CircleInvolute(cont, 611, 607, 0.7282, 0.685365, 606, 840, -0.661622, 0.749838)
    c2 = CircleInvolute(cont, 535, 465, 0.233373, 0.972387, 605, 581, 0.73994, 0.672673)
    c3 = CircleInvolute(cont, 605, 581, 0.73994, 0.672673, 606, 840, -0.661622, 0.749838)
    c4 = StraightLine(cont, 660, 875, 660, 506)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) - c2.compute_nib(0)[0] - 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 9)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailsemiup", postprocess=lambda cont: multiup(2, cont))
def _(cont):
    # Single tail for an up-pointing semiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 556, 1, 0, 602, 571, 0.825307, 0.564684)
    c1 = CircleInvolute(cont, 602, 571, 0.825307, 0.564684, 617, 779, -0.661622, 0.749838)
    c2 = CircleInvolute(cont, 535, 465, 0.371391, 0.928477, 613, 566, 0.732793, 0.680451)
    c3 = CircleInvolute(cont, 613, 566, 0.732793, 0.680451, 617, 779, -0.661622, 0.749838)
    c4 = StraightLine(cont, 660, 783.16, 660, 496.816)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (840 + 54 - quavertaildispup*1)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) - c2.compute_nib(0)[0] - 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 9)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("taildemiup", postprocess=lambda cont: multiup(3, cont))
def _(cont):
    # Single tail for an up-pointing demisemiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 555, 0.998868, -0.0475651, 586, 561, 0.913812, 0.406138)
    c1 = CircleInvolute(cont, 586, 561, 0.913812, 0.406138, 621, 800, -0.536875, 0.843662)
    c2 = CircleInvolute(cont, 535, 465, 0.416655, 0.909065, 608, 555, 0.734803, 0.67828)
    c3 = CircleInvolute(cont, 608, 555, 0.734803, 0.67828, 621, 800, -0.536875, 0.843662)
    c4 = StraightLine(cont, 660, 835.64, 660, 502.064)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (840 + 58 - quavertaildispup*2 + 132)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) - c2.compute_nib(0)[0] - 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 9)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailhemiup", postprocess=lambda cont: multiup(4, cont))
def _(cont):
    # Single tail for an up-pointing hemidemisemiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 555, 0.977176, -0.21243, 578, 556, 0.948683, 0.316228)
    c1 = CircleInvolute(cont, 578, 556, 0.948683, 0.316228, 640, 753, -0.447214, 0.894427)
    c2 = CircleInvolute(cont, 535, 465, 0.28, 0.96, 592, 545, 0.77193, 0.635707)
    c3 = CircleInvolute(cont, 592, 545, 0.77193, 0.635707, 640, 753, -0.447214, 0.894427)
    c4 = StraightLine(cont, 660, 815.96, 660, 500.096)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (840 + 60 - quavertaildispup*3 + 132*1.5)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) - c2.compute_nib(0)[0] - 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 9)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailquasiup", postprocess=lambda cont: multiup(5, cont))
@define_glyph("tail6up", postprocess=lambda cont: multiup(6, cont))
@define_glyph("tail7up", postprocess=lambda cont: multiup(7, cont))
@define_glyph("tail8up", postprocess=lambda cont: multiup(8, cont))
def _(cont):
    # Single tail for an up-pointing quasihemidemisemiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 546, 0.996546, 0.0830455, 607, 575, 0.707107, 0.707107)
    c1 = CircleInvolute(cont, 607, 575, 0.707107, 0.707107, 629, 772, -0.611448, 0.791285)
    c2 = CircleInvolute(cont, 535, 465, 0.371391, 0.928477, 595, 544, 0.707107, 0.707107)
    c3 = CircleInvolute(cont, 595, 544, 0.707107, 0.707107, 629, 772, -0.611448, 0.791285)
    c4 = StraightLine(cont, 660, 868.44, 660, 505.344)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (840 + 62 - quavertaildispup*4 + 132*2.5)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) - c2.compute_nib(0)[0] - 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 9)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailquaverdn")
def _(cont):
    # Full-size tail for a quaver with a down-pointing stem.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 363, 0.999201, -0.039968, 585, 354, 0.948683, -0.316228)
    c1 = CircleInvolute(cont, 585, 354, 0.948683, -0.316228, 635, 90, -0.563337, -0.826227)
    c2 = CircleInvolute(cont, 535, 465, 0.338427, -0.940993, 627, 349, 0.742268, -0.670103)
    c3 = CircleInvolute(cont, 627, 349, 0.742268, -0.670103, 635, 90, -0.563337, -0.826227)
    c4 = StraightLine(cont, 680, 55, 680, 424)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) + c2.compute_nib(0)[0] + 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 8)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailsemidn", postprocess=lambda cont: multidn(2, cont))
def _(cont):
    # Single tail for a down-pointing semiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 378, 0.99083, -0.135113, 611, 356, 0.868243, -0.496139)
    c1 = CircleInvolute(cont, 611, 356, 0.868243, -0.496139, 663, 195, -0.447214, -0.894427)
    c2 = CircleInvolute(cont, 535, 465, 0.467531, -0.883977, 620, 363, 0.768221, -0.640184)
    c3 = CircleInvolute(cont, 620, 363, 0.768221, -0.640184, 663, 195, -0.447214, -0.894427)
    c4 = StraightLine(cont, 680, 186.2, 680, 437.12)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (90 + quavertaildispdn*1)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) + c2.compute_nib(0)[0] + 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 8)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("taildemidn", postprocess=lambda cont: multidn(3, cont))
def _(cont):
    # Single tail for a down-pointing demisemiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 380, 0.9916, -0.129339, 615, 354, 0.861934, -0.50702)
    c1 = CircleInvolute(cont, 615, 354, 0.861934, -0.50702, 653, 168, -0.536875, -0.843662)
    c2 = CircleInvolute(cont, 535, 465, 0.450869, -0.89259, 616, 376, 0.789352, -0.613941)
    c3 = CircleInvolute(cont, 616, 376, 0.789352, -0.613941, 653, 168, -0.536875, -0.843662)
    c4 = StraightLine(cont, 680, 173.08, 680, 435.808)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (90 + quavertaildispdn*2 - 132)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) + c2.compute_nib(0)[0] + 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 8)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailhemidn", postprocess=lambda cont: multidn(4, cont))
def _(cont):
    # Single tail for a down-pointing hemidemisemiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 534, 382, 0.999133, -0.0416305, 605, 369, 0.921635, -0.388057)
    c1 = CircleInvolute(cont, 605, 369, 0.921635, -0.388057, 646, 207, -0.784883, -0.619644)
    c2 = CircleInvolute(cont, 535, 465, 0.338719, -0.940888, 630, 363, 0.825307, -0.564684)
    c3 = CircleInvolute(cont, 630, 363, 0.825307, -0.564684, 646, 207, -0.784883, -0.619644)
    c4 = StraightLine(cont, 680, 232.12, 680, 441.712)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (90 + quavertaildispdn*3 - 132*1.5)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) + c2.compute_nib(0)[0] + 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 8)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

@define_glyph("tailquasidn", postprocess=lambda cont: multidn(5, cont))
@define_glyph("tail6dn", postprocess=lambda cont: multidn(6, cont))
@define_glyph("tail7dn", postprocess=lambda cont: multidn(7, cont))
@define_glyph("tail8dn", postprocess=lambda cont: multidn(8, cont))
def _(cont):
    # Single tail for a down-pointing quasihemidemisemiquaver.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 384, 0.982007, -0.188847, 608, 357, 0.885547, -0.464549)
    c1 = CircleInvolute(cont, 608, 357, 0.885547, -0.464549, 653, 180, -0.606043, -0.795432)
    c2 = CircleInvolute(cont, 535, 465, 0.338719, -0.940888, 633, 348, 0.768221, -0.640184)
    c3 = CircleInvolute(cont, 633, 348, 0.768221, -0.640184, 653, 180, -0.606043, -0.795432)
    c4 = StraightLine(cont, 680, 219, 680, 440.4)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c3, 1, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Make sure the tail length matches what it should be.
    assert round(c1.compute_y(1) - (90 + quavertaildispdn*4 - 132*2.5)) == 0

    c4.nib = 0 # guide line to get the width the same across all versions

    c0.nib = c1.nib = 0
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],0,2,8)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c0,c1],1,2,8)

    cont.c0 = c0 # for tailshortdn
    cont.c1 = c1 # for tailshortdn

    cont.oy = c2.compute_y(0) + c2.compute_nib(0)[0] + 2

    cx = c2.compute_x(0) + c2.compute_nib(0)[0]
    cont.ox = cx
    cont.extra = "gsave newpath %g 0 moveto 0 1000 rlineto -100 0 rlineto 0 -1000 rlineto closepath 1 setgray fill grestore" % (cx - 8)

    cont.origin = cx * 3600. / cont.scale - 12, (1000-cont.oy) * 3600. / cont.scale

# ----------------------------------------------------------------------
# Minim note head.
#
# A minim head has an elliptical outline, and then a long thin
# elliptical hole in the middle.

@define_glyph("headminim")
def _(cont):
    # Parameters: a unit vector giving the direction of the ellipse's
    # long axis, the squash ratio (short axis divided by long).
    angle = 37
    sq = 0.35
    # The long and short axes as unit vectors.
    lx, ly = cos(angle*(pi/180)), -sin(angle*(pi/180))
    sx, sy = -sin(angle*(pi/180)), -cos(angle*(pi/180))
    # We want to find an ellipse, centred on the origin, which is large
    # enough to be just tangent to the outline ellipse. To do this, we
    # transform the coordinate system so that the new ellipse is
    # circular, then construct the image of the outline ellipse and find
    # its closest approach to the origin. The circle of that radius,
    # transformed back again, is the ellipse we want.
    #
    # Our original ellipse, discounting translation, is the unit circle
    # fed through a 2x2 matrix transform. We have a second 2x2 matrix
    # transform here, so we multiply the two to construct the matrix
    # which transforms the coordinate system in which the note outline
    # is a circle into the one in which the hole in the middle is a
    # circle.
    mat1 = (1,-.3,0,1) # the shear matrix from the head outline
    mat2 = (76,0,0,67) # the scaling matrix from the head outline
    mat3 = (lx,ly,sx,sy) # rotate so that our desired axes become i,j
    mat4 = (1,0,0,1/sq) # unsquash in the s-axis
    imat = matmul(matmul(mat4,mat3), matmul(mat2,mat1))
    mat = matinv(imat)
    # The equation of the outline ellipse in the new coordinate system
    # is given by transforming (x,y) by the above matrix and then
    # setting the sum of the squares of the transformed coordinates
    # equal to 1. In other words, we have
    #
    #          (x y) (a c) (a b) (x) = 1
    #                (b d) (c d) (y)
    #
    # => (x y) (a^2+c^2  ab+cd ) (x) = 1
    #          ( ba+dc  b^2+d^2) (y)
    #
    # and then the matrix in the middle is symmetric, which means we can
    # decompose it into an orthogonal eigenvector matrix and a diagonal
    # eigenvalue matrix, giving us
    #
    #    (x y) (p q) (u 0) (p r) (x) = 1
    #          (r s) (0 v) (q s) (y)
    #
    # Now the eigenvector matrix rotates our coordinate system into one
    # which has the basis vectors aligned with the axes of the ellipse,
    # so in that coordinate system the equation of the ellipse is merely
    # u x^2 + v y^2 = 1. Thus u and v are the squared reciprocals of the
    # lengths of our major and minor axes, so sqrt(min(1/u,1/v)) is the
    # closest approach to the origin of the ellipse in question.
    #
    # (We don't even bother calculating the eigenvector matrix, though
    # we could if we wanted to.)
    matO = (mat[0]*mat[0]+mat[2]*mat[2], mat[1]*mat[0]+mat[3]*mat[2],
    mat[0]*mat[1]+mat[2]*mat[3], mat[1]*mat[1]+mat[3]*mat[3])
    # Characteristic equation of a 2x2 matrix is
    #    (m0-lambda)(m3-lambda) - m1*m2 = 0
    # => lambda^2 - (m0+m3)lambda + (m0*m3-m1*m2) = 0
    # So the eigenvalues are the solutions of that quadratic, i.e.
    #    (m0+m3 +- sqrt((m0-m3)^2+4*m1*m2)) / 2
    u = (matO[0] + matO[3] + sqrt((matO[0]-matO[3])**2 + 4*matO[1]*matO[2]))/2
    v = (matO[0] + matO[3] - sqrt((matO[0]-matO[3])**2 + 4*matO[1]*matO[2]))/2
    r = sqrt(min(1/u, 1/v)) * 0.999 # small hedge against rounding glitches
    # And now we can draw our ellipse: it's the circle about the origin
    # of radius r, squashed in the y-direction by sq, rotated by angle.
    cont.extra = \
    "gsave 527 472 translate newpath " + \
    "matrix currentmatrix 76 67 scale [1 0 -.3 1 0 0] concat 1 0 moveto 0 0 1 0 360 arc closepath setmatrix " + \
    "matrix currentmatrix -%g rotate 1 %g scale %g 0 moveto 0 0 %g 360 0 arcn closepath setmatrix " % (angle,sq,r,r) + \
    "gsave fill grestore 8 setlinewidth stroke grestore"

    # Incidentally, another useful datum output from all of this is
    # that we can compute the exact point on the outer ellipse at
    # which the tangent is vertical, which is where we'll have to
    # put a note stem. This is given by reconstituting the elements
    # of the outer ellipse's transformation matrix into a quadratic
    # in x,y of the form
    #
    #     ax^2 + bxy + cy^2 = 1
    #
    # From this we can differentiate with respect to y to get
    #
    #     2ax dx/dy + bx + by dx/dy + 2cy = 0
    #
    # and then solve for dx/dy to give
    #
    #     dx/dy = -(bx + 2cy) / (2ax + by)
    #
    # which is zero iff its denominator is zero, i.e. y = -bx/2c.
    # Substituting that back into the original equation gives
    #
    #       ax^2 + bx(-bx/2c) + c(bx/2c)^2 = 1
    # =>  ax^2 - (b^2/2c)x^2 + (b^2/4c)x^2 = 1
    # =>                   (a - b^2/4c)x^2 = 1
    # =>                                 x = 1/sqrt(a - b^2/4c)
    #
    # Substituting that into the expression for y and rearranging a
    # bit gives us
    #
    #   x = (2*sqrt(c)) / sqrt(4ac-b^2)
    #   y = (-b/sqrt(c)) / sqrt(4ac-b^2)
    #
    # (Of course, that's on the outer elliptical _path_, which isn't
    # the very outside of the note shape due to the stroke width; so
    # another (currentlinewidth/2) pixels are needed to get to
    # there. But the y-position is correct.)
    matK = matinv(matmul(mat2,mat1))
    matL = (matK[0]*matK[0]+matK[2]*matK[2], matK[1]*matK[0]+matK[3]*matK[2],
    matK[0]*matK[1]+matK[2]*matK[3], matK[1]*matK[1]+matK[3]*matK[3])
    a, b, c = matL[0], matL[1]+matL[2], matL[3]
    denom = sqrt(4*a*c-b*b)
    #sys.stderr.write("%.17f, %.17f\n" % (2*sqrt(c)/denom, -b/sqrt(c)/denom))
    cont.ay = 472 - b/sqrt(c)/denom

# ----------------------------------------------------------------------
# Filled note head, for crotchet/quaver/semiquaver/etc.
#
# This is identical to the minim head but without the inner hole.

@define_glyph("headcrotchet")
def _(cont):
    cont.extra = \
    "gsave 527 472 translate newpath " + \
    "matrix currentmatrix 76 67 scale [1 0 -.3 1 0 0] concat 1 0 moveto 0 0 1 0 360 arc closepath setmatrix " + \
    "gsave fill grestore 8 setlinewidth stroke grestore"
    cont.ay = font.headminim.ay

# ----------------------------------------------------------------------
# Semibreve head. This is another nested pair of ellipses. The outer
# ellipse is unskewed and half again as wide as the crotchet/minim
# head; the inner one is at a totally different angle.

@define_glyph("semibreve")
def _(cont):
    angle = 120
    sq = 0.75
    # Everything below is repaated from the minim head.
    lx, ly = cos(angle*(pi/180)), -sin(angle*(pi/180))
    sx, sy = -sin(angle*(pi/180)), -cos(angle*(pi/180))
    mat2 = (116,0,0,67) # the scaling matrix from the head outline
    mat3 = (lx,ly,sx,sy) # rotate so that our desired axes become i,j
    mat4 = (1,0,0,1/sq) # unsquash in the s-axis
    imat = matmul(matmul(mat4,mat3), mat2)
    mat = matinv(imat)
    mat2 = (mat[0]*mat[0]+mat[2]*mat[2], mat[1]*mat[0]+mat[3]*mat[2],
    mat[0]*mat[1]+mat[2]*mat[3], mat[1]*mat[1]+mat[3]*mat[3])
    u = (mat2[0] + mat2[3] + sqrt((mat2[0]-mat2[3])**2 + 4*mat2[1]*mat2[2]))/2
    v = (mat2[0] + mat2[3] - sqrt((mat2[0]-mat2[3])**2 + 4*mat2[1]*mat2[2]))/2
    r = sqrt(min(1/u, 1/v))
    cont.extra = \
    "gsave 527 472 translate newpath " + \
    "matrix currentmatrix 116 67 scale 1 0 moveto 0 0 1 0 360 arc closepath setmatrix " + \
    "matrix currentmatrix -%g rotate 1 %g scale %g 0 moveto 0 0 %g 360 0 arcn closepath setmatrix " % (angle,sq,r,r) + \
    "gsave fill grestore 8 setlinewidth stroke grestore"

# A breve is just a semibreve with bars down the sides.

@define_glyph("breve")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 398, 390, 398, 554)
    c1 = StraightLine(cont, 656, 390, 656, 554)
    c2 = StraightLine(cont, 362, 390, 362, 554)
    c3 = StraightLine(cont, 692, 390, 692, 554)
    # End saved data

    cont.default_nib = 10

    cont.extra = font.semibreve

# ----------------------------------------------------------------------
# Shaped note heads used for drum and other notation.

@define_glyph("diamondsemi")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 411, 472, 0.970536, 0.240956, 527, 539, 0.633646, 0.773623)
    c1 = CircleInvolute(cont, 527, 539, 0.633646, -0.773623, 643, 472, 0.970536, -0.240956)
    c2 = CircleInvolute(cont, 643, 472, -0.970536, -0.240956, 527, 405, -0.633646, -0.773623)
    c3 = CircleInvolute(cont, 527, 405, -0.633646, 0.773623, 411, 472, -0.970536, 0.240956)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c3, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: (6, 0, (527-x)/3, 0)

@define_glyph("diamondminim")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 448, 472, 0.939517, 0.342501, 527, 539, 0.487147, 0.87332)
    c1 = CircleInvolute(cont, 527, 539, 0.487147, -0.87332, 606, 472, 0.939517, -0.342501)
    c2 = CircleInvolute(cont, 606, 472, -0.939517, -0.342501, 527, 405, -0.487147, -0.87332)
    c3 = CircleInvolute(cont, 527, 405, -0.487147, 0.87332, 448, 472, -0.939517, 0.342501)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c3, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = 6
    c1.nib = lambda c,x,y,t,theta: (6, 127*pi/180, min(12, 300*t, 100*(1-t)), 0)
    c3.nib = lambda c,x,y,t,theta: (6, -53*pi/180, min(12, 300*t, 100*(1-t)), 0)

@define_glyph("diamondcrotchet")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 448, 472, 0.939517, 0.342501, 527, 539, 0.487147, 0.87332)
    c1 = CircleInvolute(cont, 527, 539, 0.487147, -0.87332, 606, 472, 0.939517, -0.342501)
    c2 = CircleInvolute(cont, 606, 472, -0.939517, -0.342501, 527, 405, -0.487147, -0.87332)
    c3 = CircleInvolute(cont, 527, 405, -0.487147, 0.87332, 448, 472, -0.939517, 0.342501)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c3, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    # Fill the diamond.
    cont.default_nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,527,472,6)

@define_glyph("trianglesemi")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 411, 550, 0.944497, -0.328521, 643, 550, 0.944497, 0.328521)
    c1 = CircleInvolute(cont, 643, 550, -0.784883, -0.619644, 527, 405, -0.519947, -0.854199)
    c2 = CircleInvolute(cont, 527, 405, -0.519947, 0.854199, 411, 550, -0.784883, 0.619644)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c2, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    # End saved data

    c0.nib = 6
    angle = abs(1/tan(c0.compute_theta(0) + pi/30))
    ybase = c0.compute_y(0)
    c1.nib = lambda c,x,y,t,theta: (6, 0, 0, min((x-527)/3, (ybase-y)*angle))
    c2.nib = lambda c,x,y,t,theta: (6, 0, min((527-x)/3, (ybase-y)*angle), 0)

@define_glyph("triangleminim")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 448, 550, 0.890571, -0.454844, 606, 550, 0.890571, 0.454844)
    c1 = CircleInvolute(cont, 606, 550, -0.65319, -0.757194, 527, 405, -0.382943, -0.923772)
    c2 = CircleInvolute(cont, 527, 405, -0.382943, 0.923772, 448, 550, -0.65319, 0.757194)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c2, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    # End saved data

    c1.nib = 6
    angle = 127*pi/180
    vx, vy = cos(angle), -sin(angle)
    vdist = lambda x1,y1,x2,y2: abs(vx*(x1-x2) + vy*(y1-y2))
    x0, y0 = c0.compute_point(0)
    c0.nib = lambda c,x,y,t,theta: (6, angle, vdist(x0,y0,x,y)/3, 0)
    c2.nib = lambda c,x,y,t,theta: (6, angle, 0, min(vdist(x0,y0,x,y)/3, 350*t))

    cont.ay = c0.compute_y(0)
    cont.iy = 2*472 - cont.ay

@define_glyph("trianglecrotchet")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 448, 550, 0.890571, -0.454844, 606, 550, 0.890571, 0.454844)
    c1 = CircleInvolute(cont, 606, 550, -0.65319, -0.757194, 527, 405, -0.382943, -0.923772)
    c2 = CircleInvolute(cont, 527, 405, -0.382943, 0.923772, 448, 550, -0.65319, 0.757194)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c2, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    # End saved data

    # Fill the triangle.
    cont.default_nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,527,472,6)

    cont.ay = c0.compute_y(0)
    cont.iy = 2*472 - cont.ay

@define_glyph("crosssemi")
def _(cont):
    outerw = 9
    innerr = 12
    outerr = innerr + 2*outerw
    ax, ay = 116 - outerr, 70 - outerr
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g 1 index neg 1 index neg moveto 1 index 1 index lineto 1 index neg 1 index moveto neg lineto" % (ax,ay),
    "gsave %g setlinewidth stroke grestore %g setlinewidth 1 setgray stroke" % (2*outerr, 2*innerr),
    "grestore"]
    cont.ay = ay

@define_glyph("crossminim")
def _(cont):
    outerw = 9
    innerr = 10
    outerr = innerr + 2*outerw
    ax, ay = 79 - outerr, 70 - outerr
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g 1 index neg 1 index neg moveto 1 index 1 index lineto 1 index neg 1 index moveto neg lineto" % (ax,ay),
    "gsave %g setlinewidth stroke grestore %g setlinewidth 1 setgray stroke" % (2*outerr, 2*innerr),
    "grestore"]
    cont.ay = 472 - ay

@define_glyph("crosscrotchet")
def _(cont):
    r = 12
    ax, ay = 79 - r, 70 - r
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g 1 index neg 1 index neg moveto 1 index 1 index lineto 1 index neg 1 index moveto neg lineto" % (ax,ay),
    "%g setlinewidth stroke" % (2*r),
    "grestore"]
    cont.ay = 472 - ay

@define_glyph("crosscircle")
def _(cont):
    r = 12
    ax, ay = 70 - r, 70 - r
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g 1 index neg 1 index neg moveto 1 index 1 index lineto 1 index neg 1 index moveto neg lineto" % (ax,ay),
    "%g dup 0 moveto 0 exch 0 exch 0 360 arc" % (sqrt(ax*ax+ay*ay)),
    "%g setlinewidth stroke" % (2*r),
    "grestore"]

@define_glyph("slashsemi")
def _(cont):
    r = 12
    xouter = 116 - r
    xwidth = 160
    ay = 130 - r
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g moveto %g %g lineto %g %g lineto %g %g lineto closepath" % (xouter,-ay,xouter-xwidth,-ay,-xouter,ay,-xouter+xwidth,ay),
    "%g setlinewidth 1 setlinejoin stroke" % (2*r),
    "grestore"]
    cont.ay = 472 - ay

@define_glyph("slashminim")
def _(cont):
    r = 12
    xouter = 76 - r
    xwidth = 80
    ay = 130 - r
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g moveto %g %g lineto %g %g lineto %g %g lineto closepath" % (xouter,-ay,xouter-xwidth,-ay,-xouter,ay,-xouter+xwidth,ay),
    "%g setlinewidth 1 setlinejoin stroke" % (2*r),
    "grestore"]
    cont.ay = 472 - ay

@define_glyph("slashcrotchet")
def _(cont):
    r = 12
    xouter = 56 - r
    xwidth = 40
    ay = 130 - r
    cont.extra = ["gsave 527 472 translate",
    "newpath %g %g moveto %g %g lineto %g %g lineto %g %g lineto closepath" % (xouter,-ay,xouter-xwidth,-ay,-xouter,ay,-xouter+xwidth,ay),
    "gsave %g setlinewidth 1 setlinejoin stroke grestore fill" % (2*r),
    "grestore"]
    cont.ay = 472 - ay

# ----------------------------------------------------------------------
# Trill sign. There seem to be two standard-ish designs for this:
# one flowery one in which there are loops all over the place as if
# it's been drawn in several strokes by somebody who didn't bother
# taking the pen off the paper between them (e.g. Euterpe,
# Lilypond), and one simpler one that just looks like 'tr' written
# in an italic font and squashed together. Mine follows the latter
# model, but has a more chisel-nib-calligraphy look than other
# examples I've seen. (I drew it like that as an experiment and
# found I liked it more than the one I was comparing to!)

@define_glyph("trill")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 497, 274, 452, 425)
    c1 = CircleInvolute(cont, 452, 425, -0.285601, 0.958349, 488, 456, 0.860055, -0.510202)
    c2 = StraightLine(cont, 488, 456, 547, 421)
    c3 = StraightLine(cont, 413, 344, 488, 343)
    c4 = CircleInvolute(cont, 488, 343, 0.999911, -0.0133321, 559, 335, 0.974222, -0.225592)
    c5 = CircleInvolute(cont, 559, 335, 0.974222, -0.225592, 573, 345, -0.290482, 0.956881)
    c6 = StraightLine(cont, 573, 345, 539, 457)
    c7 = CircleInvolute(cont, 561.107, 382, 0.274721, -0.961524, 621, 332, 1, 0)
    c8 = CircleInvolute(cont, 621, 332, 1, 0, 636, 356, -0.242536, 0.970142)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c7.weld_to(1, c8, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: (3, c.nibdir(t), 17, 17)

    k2 = c4.compute_theta(1)
    c3.nibdir = c4.nibdir = c5.nibdir = c6.nibdir = lambda t: k2
    c7.nibdir = c8.nibdir = c3.nibdir

    topy = c0.compute_y(0)
    c0.nib = lambda c,x,y,t,theta: 20 if t>0.5 else (3, 0, 17, min(17,-17+(y-topy)*1.2))
    theta0 = c0.compute_theta(0)
    theta2 = c2.compute_theta(0)
    c1.nib = c2.nib = lambda c,x,y,t,theta: 14+6*cos(pi*(theta-theta0)/(theta2-theta0))

# ----------------------------------------------------------------------
# Crotchet rest. The top section is done by curve-following, drawing
# the two sides of the stroke independently; the bottom section is a
# single curve on the right, with the nib width varying in such a
# way as to present a nice curve on the left.

@define_glyph("restcrotchet")
def _(cont_main):
    # Secondary curve set.
    cont = GlyphContext()
    # Saved data from gui.py
    c0 = StraightLine(cont, 502, 276, 589, 352)
    c1 = CircleInvolute(cont, 589, 352, -0.585491, 0.810679, 592, 535, 0.74783, 0.66389)
    c0.weld_to(1, c1, 0, 1)
    # End saved data
    tc0, tc1 = c0, c1

    cont = cont_main
    # Primary curve set.
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 502, 276, 0.753113, 0.657892, 494, 448, -0.613941, 0.789352)
    c1 = StraightLine(cont, 494, 448, 592, 535)
    c2 = CircleInvolute(cont, 592, 535, -0.952424, -0.304776, 524, 569, -0.378633, 0.925547)
    c3 = CircleInvolute(cont, 524, 569, -0.378633, 0.925547, 547, 649, 0.745241, 0.666795)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # Dependencies between the above: tc0 and c0 must start at the
    # same place heading in the same direction, and tc1 and c1 must
    # end at the same place heading in the same direction.

    c0.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0,tc1],0,2,6)
    c1.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0,tc1],1,2,6)
    phi0 = c2.compute_theta(0)
    phi1 = c3.compute_theta(1) + pi
    phia = (phi0 + phi1) / 2
    c2.nib = lambda c,x,y,t,theta: (6, phia, (1-(1-t)**2)*40, 0)
    c3.nib = lambda c,x,y,t,theta: (6, phia, (1-t**2)*40, 0)

# ----------------------------------------------------------------------
# Quaver rest and friends.

@define_glyph("restquaver", args=(1,))
@define_glyph("restsemi", args=(2,))
@define_glyph("restdemi", args=(3,))
@define_glyph("resthemi", args=(4,))
@define_glyph("restquasi", args=(5,))
@define_glyph("rest6", args=(6,))
@define_glyph("rest7", args=(7,))
@define_glyph("rest8", args=(8,))
def _(cont, n):
    xoff = 39 - 4*max(0, n-5)
    c0 = StraightLine(cont, 570 - xoff*n, 141 + 130*n, 588, 81)
    cs = [CircleInvolute(cont, 588 - xoff*i, 81 + 130*i, -0.347314, 0.937749,
                         480 - xoff*i, 125 + 130*i, -0.784883, -0.619644)
          for i in range(n)]
    c0.weld_to(1, cs[0], 0, 1)

    cont.default_nib = 8

    for c in cs:
        blob(c, 1, 'r', 33, 3)

    co = cs[(n - 1) // 2] # which curve is used as the origin
    cont.cy = co.compute_y(1) - 33*sin(co.compute_theta(1)-pi/2) + 76

    cont.origin = 1000-(xoff*n*1800/cont.scale), ((1000-cont.cy) * 3600 / cont.scale)

    cont.canvas_size = 1000, 1000 + 130*n

@define_glyph("restcrotchetx")
def _(cont):
    cont.before = "1000 0 translate -1 1 scale"
    cont.extra = font.restquaver

@define_glyph("restcrotchetz")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 532, 271, 412, 81)
    c1 = CircleInvolute(cont, 412, 81, 0.5339929913860784, 0.8454889030321733, 525, 125, 0.784883, -0.619644)
    c2 = CircleInvolute(cont, 532, 271, -0.5339929913860784, -0.8454889030321733, 419, 227, -0.784883, 0.619644)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    cont.default_nib = 8

    blob(c1, 1, 'l', 33, 3)
    blob(c2, 1, 'l', 33, 3)

# ----------------------------------------------------------------------
# Rectangular rests (minim/semibreve, breve, longa, double longa).

@define_glyph("restminim")
def _(cont):
    cont.extra = \
    "newpath 440 439 moveto 440 505 lineto 614 505 lineto 614 439 lineto closepath fill "

@define_glyph("restbreve")
def _(cont):
    cont.extra = \
    "newpath 452 406 moveto 452 538 lineto 602 538 lineto 602 406 lineto closepath fill "

@define_glyph("restlonga")
def _(cont):
    cont.extra = \
    "newpath 452 406 moveto 452 670 lineto 602 670 lineto 602 406 lineto closepath fill "

@define_glyph("restdbllonga")
def _(cont):
    cont.extra = (font.restlonga, "-300 0 translate",
                  font.restlonga)

@define_glyph("restminimo")
def _(cont):
    cont.extra = font.restminim, \
    "newpath 390 505 moveto 664 505 lineto 12 setlinewidth 1 setlinecap stroke"

    cont.oy = 505

@define_glyph("restbreveo")
def _(cont):
    cont.extra = font.restbreve, \
    "newpath 390 406 moveto 664 406 lineto " \
    "390 538 moveto 664 538 lineto 12 setlinewidth 1 setlinecap stroke"

@define_glyph("restsemibreveo")
def _(cont):
    cont.extra = font.restminim, \
    "newpath 390 439 moveto 664 439 lineto 12 setlinewidth 1 setlinecap stroke"

    cont.oy = 439

# ----------------------------------------------------------------------
# Digits for time signatures.

@define_glyph("big0")
def _(cont): # zero
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 528, 253, 1, 0, 572, 273, 0.60745, 0.794358)
    c1 = CircleInvolute(cont, 572, 273, 0.60745, 0.794358, 597, 362, 0, 1)
    c2 = CircleInvolute(cont, 597, 362, 0, 1, 572, 451, -0.60745, 0.794358)
    c3 = CircleInvolute(cont, 572, 451, -0.60745, 0.794358, 528, 471, -1, 0)
    c4 = CircleInvolute(cont, 528, 471, -1, 0, 484, 451, -0.60745, -0.794358)
    c5 = CircleInvolute(cont, 484, 451, -0.60745, -0.794358, 459, 362, 0, -1)
    c6 = CircleInvolute(cont, 459, 362, 0, -1, 484, 273, 0.60745, -0.794358)
    c7 = CircleInvolute(cont, 484, 273, 0.60745, -0.794358, 528, 253, 1, 0)
    c0.weld_to(1, c1, 0)
    c0.weld_to(0, c7, 1)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0)
    # End saved data

    ymid = float(c1.compute_y(1))
    yext = abs(ymid - c0.compute_y(0))
    cont.default_nib = lambda c,x,y,t,theta: (6, 0, 25*(1-abs((y-ymid)/yext)**2.5), 25*(1-abs((y-ymid)/yext)**2.5))

@define_glyph("big1")
def _(cont): # one
    # Saved data from gui.py
    c0 = StraightLine(cont, 467, 342, 513, 257)
    c1 = StraightLine(cont, 538, 257, 538, 467)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: (6, 0, 10*t, 0)

    y2 = c1.compute_y(1)
    y1 = y2-50 # this value is the same as is used for the serif on the 4
    serif = lambda y: 0 if y<y1 else 26*((y-y1)/(y2-y1))**4
    c1.nib = lambda c,x,y,t,theta: (6, 0, 25+serif(y), 25+serif(y))

@define_glyph("big2")
def _(cont_main): # two

    # At the top of the 2 I use the same hack as I did for the 3 to
    # get the inner curve. See below.

    # Secondary context.
    cont = GlyphContext()
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 615, 419, -0.26963, 0.962964, 560, 424, -0.865426, -0.501036)
    c1 = CircleInvolute(cont, 560, 424, -0.865426, -0.501036, 449, 467, -0.419058, 0.907959)
    c0.weld_to(1, c1, 0)
    # End saved data
    tc0, tc1 = c0, c1

    # Primary context.
    cont = cont_main
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 615, 419, -0.26963, 0.962964, 548, 468, -0.83205, -0.5547)
    c1 = CircleInvolute(cont, 548, 468, -0.83205, -0.5547, 449, 467, -0.419058, 0.907959)
    c2 = CircleInvolute(cont, 449, 467, 0, -1, 523, 381, 0.94299, -0.33282)
    c3 = CircleInvolute(cont, 523, 381, 0.94299, -0.33282, 583, 307, 0, -1)
    c4 = CircleInvolute(cont, 583, 307, 0, -1, 530, 253, -1, 0)
    c5 = CircleInvolute(cont, 530, 253, -1, 0, 467, 275, -0.7282, 0.685365)
    c6 = CircleInvolute(cont, 561, 307, 0, -1, 512, 261, -1, 0)
    c7 = CircleInvolute(cont, 512, 261, -1, 0, 467, 275, -0.7282, 0.685365)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c6.weld_to(1, c7, 0)
    # End saved data

    cont.default_nib = 6

    xr = c0.compute_x(0)
    xl = c1.compute_x(1)
    c0.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0,tc1],0,2,6)
    c1.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0,tc1],1,2,6)
    c4.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, 0, k, k))(22*((x-min(x1,x2))/abs(x2-x1)))))(c.compute_x(0),c.compute_x(1))
    c3.nib = c2.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, 0, k, k))(22*((x-min(x1,x2))/abs(x2-x1)))))(c2.compute_x(0),c3.compute_x(1))

    blob(c5, 1, 'l', 25, 4)

@define_glyph("big3")
def _(cont): # three
    # Bit of a hack here. The x-based formula I use for the nib
    # thickness of the right-hand curves c1-c4 leaves a nasty corner
    # at the very top and bottom, which I solve by drawing an
    # independent inner curve at each end (c6-c9). Normally I would
    # solve this using follow_curveset_nib, filling the area between
    # c6-c7 and c0-c1 and that between c8-c9 and c2-c3; however,
    # that gets the inner curve right but destroys the outer curve
    # from the x-based formula. So instead I just do the simplest
    # possible thing: draw c1-c4 with the nib thickness formula as
    # before, but then draw c6-c9 over the top at constant
    # thickness, relying on the fact that they never separate far
    # enough from what would otherwise be the inner curve to open a
    # gap between them.

    # Saved data from gui.py
    c0 = CircleInvolute(cont, 462, 446, 0.7282, 0.685365, 525, 471, 1, 0)
    c1 = CircleInvolute(cont, 525, 471, 1, 0, 580, 416, 0, -1)
    c2 = CircleInvolute(cont, 580, 416, 0, -1, 504, 352, -1, 0)
    c3 = CircleInvolute(cont, 504, 352, 1, 0, 578, 303, 0, -1)
    c4 = CircleInvolute(cont, 578, 303, 0, -1, 525, 253, -1, 0)
    c5 = CircleInvolute(cont, 525, 253, -1, 0, 462, 276, -0.7282, 0.685365)
    c6 = CircleInvolute(cont, 462, 446, 0.7282, 0.685365, 510, 464, 1, 0)
    c7 = CircleInvolute(cont, 510, 464, 1, 0, 558, 416, 0, -1)
    c8 = CircleInvolute(cont, 556, 303, 0, -1, 511, 261, -1, 0)
    c9 = CircleInvolute(cont, 511, 261, -1, 0, 462, 276, -0.7282, 0.685365)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c6.weld_to(1, c7, 0)
    c8.weld_to(1, c9, 0)
    # End saved data

    cont.default_nib = 6

    c1.nib = c2.nib = c3.nib = c4.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, 0, k, k))(22*((x-min(x1,x2))/abs(x2-x1)))))(c.compute_x(0),c.compute_x(1))

    blob(c0, 0, 'r', 25, 4)

    blob(c5, 1, 'l', 25, 4)

@define_glyph("big4")
def _(cont_main): # four
    # Secondary context
    cont = GlyphContext()
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 496, 257, 0, 1, 432, 413, -0.665255, 0.746617)
    # End saved data
    tc0 = c0

    # Primary context
    cont = cont_main
    # Saved data from gui.py
    c0 = StraightLine(cont, 571, 257, 432, 413)
    c1 = StraightLine(cont, 432, 413, 514, 413)
    c2 = StraightLine(cont, 551, 299, 551, 467)
    c3 = StraightLine(cont, 450, 411, 599, 411)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[tc0],0,1,6)
    c1.nib = 6
    gradient = tan(c0.compute_theta(0))
    y0 = c2.compute_y(0)
    y2 = c2.compute_y(1)
    y1 = y2-50 # this value is the same as is used for the serif on the 1
    serif = lambda y: 0 if y<y1 else 26*((y-y1)/(y2-y1))**4
    c2.nib = lambda c,x,y,t,theta: (6, 0, 25+serif(y), min(25+serif(y), -25+(y-y0)/gradient))
    c3.nib = 8

    # Top line and baseline of the digits are defined by the 4.
    cont.ty = c0.compute_y(0) - c0.compute_nib(0)[0]
    cont.by = c2.compute_y(1) + c2.compute_nib(1)[0]
    # Icky glitch-handling stuff (see -lily section).
    cont.gy = (cont.ty + cont.by) / 2 + (250*cont.scale/3600.0)

@define_glyph("big5")
def _(cont): # five
    # At the bottom of the 5 I use the same hack as I did for the 3
    # to get the inner curve. See below.

    # Saved data from gui.py
    c0 = CircleInvolute(cont, 461, 442, 0.7282, 0.685365, 524, 471, 1, 0)
    c1 = CircleInvolute(cont, 524, 471, 1, 0, 579, 400, 0, -1)
    c2 = CircleInvolute(cont, 579, 400, 0, -1, 520, 332, -1, 0)
    c3 = CircleInvolute(cont, 520, 332, -1, 0, 461, 351, -0.795432, 0.606043)
    c4 = StraightLine(cont, 461, 351, 469, 257)
    c5 = CircleInvolute(cont, 469, 257, 0.938343, 0.345705, 596, 257, 0.953583, -0.301131)
    c6 = CircleInvolute(cont, 461, 442, 0.7282, 0.685365, 506, 463, 1, 0)
    c7 = CircleInvolute(cont, 506, 463, 1, 0, 557, 400, 0, -1)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    c6.weld_to(1, c7, 0)
    # End saved data

    cont.default_nib = 6

    c1.nib = c2.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, 0, k, k))(22*((x-min(x1,x2))/abs(x2-x1)))))(c.compute_x(0),c.compute_x(1))

    xr = c5.compute_x(1)
    xl = c5.compute_x(0)
    taper = lambda x: x**4 if x>0 else 0
    xm = xl + 0.5*(xr-xl)
    c5.nib = lambda c,x,y,t,theta: (6,-pi/2,32*(1-taper((x-xm)/(xr-xm))),0)

    blob(c0, 0, 'r', 25, 4)

@define_glyph("big6")
def _(cont): # six
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 535, 471, -1, 0, 479, 408, 0, -1)
    c1 = CircleInvolute(cont, 479, 408, 0, -1, 535, 349, 1, 0)
    c2 = CircleInvolute(cont, 535, 349, 1, 0, 591, 408, 0, 1)
    c3 = CircleInvolute(cont, 591, 408, 0, 1, 535, 471, -1, 0)
    c4 = CircleInvolute(cont, 535, 471, -1, 0, 491, 446, -0.60745, -0.794358)
    c5 = CircleInvolute(cont, 491, 446, -0.60745, -0.794358, 466, 360, 0, -1)
    c6 = CircleInvolute(cont, 466, 360, 0, -1, 493, 277, 0.658505, -0.752577)
    c7 = CircleInvolute(cont, 493, 277, 0.658505, -0.752577, 546, 253, 1, 0)
    c8 = CircleInvolute(cont, 546, 253, 1, 0, 598, 275, 0.7282, 0.685365)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0)
    c7.weld_to(1, c8, 0)
    # End saved data

    ymid = float(c5.compute_y(1))
    yext = abs(ymid - c4.compute_y(0))
    cont.default_nib = lambda c,x,y,t,theta: (6, 0, 25*(1-abs((y-ymid)/yext)**2.5), 25*(1-abs((y-ymid)/yext)**2.5))

    ytop2 = c2.compute_y(0)
    ybot2 = c3.compute_y(1)
    ymid2 = (ytop2+ybot2)/2
    yext2 = abs(ymid2 - ytop2)
    c2.nib = c3.nib = lambda c,x,y,t,theta: (6, 0, 22*(1-abs((y-ymid2)/yext2)**2.5), 22*(1-abs((y-ymid2)/yext2)**2.5))

    ythreshold = c1.compute_y(0.5)
    c0.nib = c1.nib = lambda c,x,y,t,theta: (6, 0, 22*(1-abs((y-ymid2)/yext2)**2.5), 0 if y>ythreshold else 22*(1-abs((y-ymid2)/yext2)**2.5))

    c8.nib = 6
    blob(c8, 1, 'r', 25, 4)

    # FIXME: consider redoing this using the x-based formula I used
    # on the 3.

@define_glyph("big7")
def _(cont): # seven
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 538, 467, 0, -1, 568, 353, 0.544988, -0.838444)
    c1 = CircleInvolute(cont, 568, 353, 0.544988, -0.838444, 604, 257, 0.233373, -0.972387)
    c2 = CircleInvolute(cont, 604, 257, -0.546268, 0.837611, 491, 284, -0.7282, -0.685365)
    c3 = CircleInvolute(cont, 491, 284, -0.7282, -0.685365, 444, 283, -0.563337, 0.826227)
    c4 = CircleInvolute(cont, 479, 467, 0, -1, 545, 345, 0.759257, -0.650791)
    c5 = CircleInvolute(cont, 545, 345, 0.759257, -0.650791, 604, 257, 0.233373, -0.972387)
    c6 = CircleInvolute(cont, 604, 257, -0.563337, 0.826227, 558, 273, -0.768221, -0.640184)
    c7 = CircleInvolute(cont, 558, 273, -0.768221, -0.640184, 444, 283, -0.563337, 0.826227)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0, 1)
    c6.weld_to(1, c7, 0)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c4,c5,c6,c7],0,4,6)
    c1.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c4,c5,c6,c7],1,4,6)
    c2.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c4,c5,c6,c7],2,4,6)
    c3.nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,[c4,c5,c6,c7],3,4,6)
    c4.nib = c5.nib = c6.nib = c7.nib = 1 # essentially ignore these
    x2 = c7.compute_x(1)
    x0 = c7.compute_x(0)
    x1 = x2 + 0.4 * (x0-x2)
    serif = lambda x: 0 if x>x1 else 26*((x-x1)/(x2-x1))**4
    xc3 = eval(c3.serialise())
    xc7 = eval(c7.serialise())
    xc3.nib = xc7.nib = lambda c,x,y,t,theta: (lambda k: (6,pi/2,k,k))(serif(x))

@define_glyph("big8")
def _(cont): # eight

    # The traditional 8 just contains _too_ many ellipse-like curves
    # to draw sensibly using involutes, so I resorted to squashing
    # the x-axis down by 3/4 so that the ellipses became more
    # circular.

    # This glyph is designed so that its _exterior_ outline is
    # mirror-symmetric. To this end, constraints currently
    # unenforced by gui.py are:
    #  - c4 should be an exact mirror image of c3
    #  - c2 should be an exact mirror image of c7
    #
    # Also, of course, c0 must join up precisely to c3 just as c4
    # does, and likewise c2 to c7 just like c6.

    # Saved data from gui.py
    c0 = CircleInvolute(cont, 529, 255, -1, 0, 490, 293, 0.485643, 0.874157, mx=(0.75, 0, 0, 1))
    c1 = CircleInvolute(cont, 490, 293, 0.485643, 0.874157, 575, 353, 0.925547, 0.378633, mx=(0.75, 0, 0, 1))
    c2 = CircleInvolute(cont, 575, 353, 0.925547, 0.378633, 529, 469, -1, 0, mx=(0.75, 0, 0, 1))
    c3 = CircleInvolute(cont, 559, 365, 0.942302, -0.334765, 529, 255, -1, 0, mx=(0.75, 0, 0, 1))
    c4 = CircleInvolute(cont, 529, 255, -1, 0, 499, 365, 0.942302, 0.334765, mx=(0.75, 0, 0, 1))
    c5 = CircleInvolute(cont, 499, 365, 0.942302, 0.334765, 576, 427, 0.263117, 0.964764, mx=(0.75, 0, 0, 1))
    c6 = CircleInvolute(cont, 576, 427, 0.263117, 0.964764, 529, 469, -1, 0, mx=(0.75, 0, 0, 1))
    c7 = CircleInvolute(cont, 529, 469, -1, 0, 483, 353, 0.925547, -0.378633, mx=(0.75, 0, 0, 1))
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0)
    # End saved data
    tcurves = c0,c1,c2
    curves = c4,c5,c6

    for i in range(len(tcurves)):
        tcurves[i].nib = 0
    for i in range(len(curves)):
        curves[i].i = i
        curves[i].nib = lambda c,x,y,t,theta: follow_curveset_nib(c,x,y,t,theta,tcurves,c.i,len(curves),8)

    c3.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (8, 0, 0, k))(9*((x-min(x1,x2))/abs(x2-x1)))))(c.compute_x(0),c.compute_x(1))
    c7.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (8, 0, k, 0))(9*((max(x1,x2)-x)/abs(x2-x1)))))(c.compute_x(0),c.compute_x(1))

@define_glyph("big9")
def _(cont): # nine
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 522, 253, 1, 0, 578, 316, 0, 1)
    c1 = CircleInvolute(cont, 578, 316, 0, 1, 522, 375, -1, 0)
    c2 = CircleInvolute(cont, 522, 375, -1, 0, 466, 316, 0, -1)
    c3 = CircleInvolute(cont, 466, 316, 0, -1, 522, 253, 1, 0)
    c4 = CircleInvolute(cont, 522, 253, 1, 0, 566, 278, 0.60745, 0.794358)
    c5 = CircleInvolute(cont, 566, 278, 0.60745, 0.794358, 591, 364, 0, 1)
    c6 = CircleInvolute(cont, 591, 364, 0, 1, 564, 447, -0.658505, 0.752577)
    c7 = CircleInvolute(cont, 564, 447, -0.658505, 0.752577, 511, 471, -1, 0)
    c8 = CircleInvolute(cont, 511, 471, -1, 0, 459, 449, -0.7282, -0.685365)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0)
    c7.weld_to(1, c8, 0)
    # End saved data

    ymid = float(c5.compute_y(1))
    yext = abs(ymid - c4.compute_y(0))
    cont.default_nib = lambda c,x,y,t,theta: (6, 0, 25*(1-abs((y-ymid)/yext)**2.5), 25*(1-abs((y-ymid)/yext)**2.5))

    ytop2 = c2.compute_y(0)
    ybot2 = c3.compute_y(1)
    ymid2 = (ytop2+ybot2)/2
    yext2 = abs(ymid2 - ytop2)
    c2.nib = c3.nib = lambda c,x,y,t,theta: (6, 0, 22*(1-abs((y-ymid2)/yext2)**2.5), 22*(1-abs((y-ymid2)/yext2)**2.5))

    ythreshold = c1.compute_y(0.5)
    c0.nib = c1.nib = lambda c,x,y,t,theta: (6, 0, 0 if y<ythreshold else 22*(1-abs((y-ymid2)/yext2)**2.5), 22*(1-abs((y-ymid2)/yext2)**2.5))

    c8.nib = 6
    blob(c8, 1, 'r', 25, 4)

    # FIXME: consider redoing this using the x-based formula I used
    # on the 3. (Well, recopying from the 6 if I do.)

@define_glyph("asciiplus")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 500, 362, 600, 362)
    c1 = StraightLine(cont, 550, 312, 550, 412)
    # End saved data

    cont.default_nib = 12

@define_glyph("asciiminus")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 500, 362, 600, 362)
    # End saved data

    cont.default_nib = 12

@define_glyph("asciicomma")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 573, 435, 0.843662, 0.536875, 548, 535, -0.894427, 0.447214)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: 4+25*cos(pi/2*t)**2

    blob(c0, 0, 'l', 5, 0)

@define_glyph("asciiperiod")
def _(cont):
    cont.extra = "newpath 500 439 34 0 360 arc fill"
for x in [getattr(font, name) for name in
          ['big0','big1','big2','big3','big5','big6','big7','big8','big9',
           'asciiplus','asciiminus','asciicomma','asciiperiod']]:
    x.ty,x.by,x.gy = (font.big4.ty, font.big4.by,
                      font.big4.gy)

# ----------------------------------------------------------------------
# The small digits used for ntuplets and fingering marks. Scaled and
# sheared versions of the big time-signature digits.

@define_glyph("small0")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big0, "grestore"

@define_glyph("small1")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big1, "grestore"

@define_glyph("small2")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big2, "grestore"

@define_glyph("small3")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big3, "grestore"

@define_glyph("small4")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big4, "grestore"

@define_glyph("small5")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big5, "grestore"

@define_glyph("small6")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big6, "grestore"

@define_glyph("small7")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big7, "grestore"

@define_glyph("small8")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big8, "grestore"

@define_glyph("small9")
def _(cont):
    cont.extra = "gsave 480 480 translate 0.6 0.72 scale [1 0 -.3 1 0 0] concat -480 -480 translate", font.big9, "grestore"

# ----------------------------------------------------------------------
# The big C for common time signature.

@define_glyph("timeC")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 732, 391, -0.5547, -0.83205, 659, 353, -1, 0)
    c1 = CircleInvolute(cont, 659, 353, -1, 0, 538, 470, 0, 1)
    c2 = CircleInvolute(cont, 538, 470, 0, 1, 650, 587, 1, 0)
    c3 = CircleInvolute(cont, 650, 587, 1, 0, 742, 508, 0.135113, -0.99083)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    c0.nib = c3.nib = 6
    c1.nib = c2.nib = lambda c,x,y,t,theta: (lambda x1,x2: ((lambda k: (6, 0, k, 0))(44*((x-max(x1,x2))/abs(x2-x1))**2)))(c.compute_x(0),c.compute_x(1))

    blob(c0, 0, 'r', 32, 8)

@define_glyph("timeCbar")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 648, 272, 648, 672)
    # End saved data

    cont.default_nib = 8

    cont.extra = font.timeC

# ----------------------------------------------------------------------
# Dynamics marks (f,m,p,s,z).

@define_glyph("dynamicm")
def _(cont): # m (we do this one first to define the baseline)
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 539, 378, 0.328521, -0.944497, 585, 331, 1, 0)
    c1 = CircleInvolute(cont, 585, 331, 1, 0, 606, 360, -0.287348, 0.957826)
    c2 = StraightLine(cont, 606, 360, 576, 460)
    c3 = CircleInvolute(cont, 621, 360, 0.287348, -0.957826, 648, 331, 1, 0)
    c4 = CircleInvolute(cont, 648, 331, 1, 0, 669, 360, -0.287348, 0.957826)
    c5 = StraightLine(cont, 669, 360, 639, 460)
    c6 = CircleInvolute(cont, 684, 360, 0.287348, -0.957826, 711, 331, 1, 0)
    c7 = CircleInvolute(cont, 711, 331, 1, 0, 732, 360, -0.286206, 0.958168)
    c8 = StraightLine(cont, 732, 360, 709, 437)
    c9 = CircleInvolute(cont, 709, 437, -0.286206, 0.958168, 726, 463, 1, 0)
    c10 = CircleInvolute(cont, 726, 463, 1, 0, 773, 415, 0.328521, -0.944497)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c6.weld_to(1, c7, 0)
    c7.weld_to(1, c8, 0)
    c8.weld_to(1, c9, 0)
    c9.weld_to(1, c10, 0)
    # End saved data

    cont.default_nib = 4
    c2.nib = c5.nib = c8.nib = (4,0,15,15)
    phi = c1.compute_theta(1)
    psi = c0.compute_theta(0)
    c0.nib = c1.nib = c3.nib = c4.nib = c6.nib = c7.nib = c9.nib = c10.nib = lambda c,x,y,t,theta: (lambda k: 4+k)(15*cos(pi/2*(theta-phi)/(psi-phi))**2)

    cont.lby = c2.compute_y(1)
    cont.by = c2.compute_y(1) + c2.compute_nib(1)[0]
    cont.lx = 557 + (-41.38 - 34.62) * cont.scale / 3600.0
    cont.rx = 751 - (-49.53 - -87.53) * cont.scale / 3600.0

@define_glyph("dynamicn")
def _(cont): # n
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 539, 378, 0.328521, -0.944497, 585, 331, 1, 0)
    c1 = CircleInvolute(cont, 585, 331, 1, 0, 606, 360, -0.287348, 0.957826)
    c2 = StraightLine(cont, 606, 360, 576, 460)
    c3 = CircleInvolute(cont, 621, 360, 0.287348, -0.957826, 648, 331, 1, 0)
    c4 = CircleInvolute(cont, 648, 331, 1, 0, 669, 360, -0.286206, 0.958168)
    c5 = StraightLine(cont, 669, 360, 646, 437)
    c6 = CircleInvolute(cont, 646, 437, -0.286206, 0.958168, 663, 463, 1, 0)
    c7 = CircleInvolute(cont, 663, 463, 1, 0, 710, 415, 0.328521, -0.944497)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    c6.weld_to(1, c7, 0)
    # End saved data

    cont.default_nib = 4
    c2.nib = c5.nib = (4,0,15,15)
    phi = c1.compute_theta(1)
    psi = c0.compute_theta(0)
    c0.nib = c1.nib = c3.nib = c4.nib = c6.nib = c7.nib = lambda c,x,y,t,theta: (lambda k: 4+k)(15*cos(pi/2*(theta-phi)/(psi-phi))**2)

    cont.lby = c2.compute_y(1)
    cont.by = c2.compute_y(1) + c2.compute_nib(1)[0]
    cont.lx = 557 + (-41.38 - 34.62) * cont.scale / 3600.0
    cont.rx = 688 - (-49.53 - -87.53) * cont.scale / 3600.0

@define_glyph("dynamicf")
def _(cont): # f
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 720, 269, -0.60745, -0.794358, 690, 254, -1, 0)
    c1 = CircleInvolute(cont, 690, 254, -1, 0, 600, 359, -0.21243, 0.977176)
    c2 = CircleInvolute(cont, 600, 359, -0.21243, 0.977176, 550, 506, -0.462566, 0.886585)
    c3 = CircleInvolute(cont, 550, 506, -0.462566, 0.886585, 490, 552, -1, 0)
    c4 = CircleInvolute(cont, 490, 552, -1, 0, 463, 516, 0.301131, -0.953583)
    c5 = StraightLine(cont, 540, 349, 661, 349)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    # End saved data

    cont.default_nib = 8

    yt = c1.compute_y(0)
    yb = c3.compute_y(1)
    m = 0.6

    # Construct a quintic which is 0 with derivative 0 at both 0 and
    # 1, and 1 with derivative 0 at m. Second derivative at 0 is
    # non-negative iff m <= 0.6, so we require 0.4 <= m <= 0.6 for
    # the values on [0,1] to be contained within [0,1].
    denom = m*m*m*(-1+m*(3+m*(-3+m)))
    a = (2-4*m)/denom
    b = (-4+m*(5+m*5))/denom
    c = (2+m*(2+m*-10))/denom
    d = (m*(-3+m*5))/denom
    quintic = lambda x: x*x*(d+x*(c+x*(b+x*a)))

    c1.nib = c2.nib = c3.nib = lambda c,x,y,t,theta: (8+20*quintic((y-yb)/(yt-yb))**0.3)
    #cos(pi/2 * ((theta % (2*pi))-phi)/(psi-phi))**2)

    c5.nib = 10

    blob(c0, 0, 'r', 20, 8)
    blob(c4, 1, 'r', 20, 8)

    cont.by = font.dynamicm.by
    cont.lx = 496.7 + (-81.74 - -86.74) * cont.scale / 3600.0
    cont.rx = 657.7 - (-139.36 - -165.36) * cont.scale / 3600.0

@define_glyph("dynamicp")
def _(cont): # p
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 539, 378, 0.328521, -0.944497, 585, 331, 1, 0)
    c1 = CircleInvolute(cont, 585, 331, 1, 0, 606, 360, -0.289177, 0.957276)
    c2 = StraightLine(cont, 606, 360, 548, 552)
    c3 = CircleInvolute(cont, 607, 428, 0, -1, 669, 336, 1, 0)
    c4 = CircleInvolute(cont, 669, 336, 1, 0, 697, 370, 0, 1)
    c5 = CircleInvolute(cont, 697, 370, 0, 1, 633, 464, -1, 0)
    c6 = CircleInvolute(cont, 633, 464, -1, 0, 607, 428, 0, -1)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c3.weld_to(1, c4, 0)
    c3.weld_to(0, c6, 1)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0)
    # End saved data

    y2 = c2.compute_y(1)
    y1 = y2 - 20
    serif = lambda y: 0 if y<y1 else 26*((y-y1)/(y2-y1))**4
    c2.nib = lambda c,x,y,t,theta: (lambda k: (6,0,k,k))(18 + serif(y))
    phi = c1.compute_theta(1)
    psi = c0.compute_theta(0)
    c0.nib = c1.nib = lambda c,x,y,t,theta: (lambda k: 4+k)(20*cos(pi/2*(theta-phi)/(psi-phi))**2)

    gamma = 1/tan(c2.compute_theta(0.5))
    shear = lambda theta: (lambda dx,dy: atan2(-dy,dx+gamma*dy))(cos(theta),-sin(theta))
    cont.default_nib = lambda c,x,y,t,theta: 12-9*sin(shear(theta))

    cont.by = font.dynamicm.by
    cont.lx = 510.4 + (-23.26 - -38.26) * cont.scale / 3600.0
    cont.rx = 690.615 - (-51.72 - -28.72) * cont.scale / 3600.0

@define_glyph("dynamicr")
def _(cont): # r
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 551, 348, 0.635707, -0.77193, 585, 331, 1, 0)
    c1 = CircleInvolute(cont, 585, 331, 1, 0, 606, 360, -0.287348, 0.957826)
    c2 = StraightLine(cont, 606, 360, 576, 460)
    c3 = CircleInvolute(cont, 617, 360, 0.287348, -0.957826, 687, 344, 0.707107, 0.707107)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    # End saved data

    cont.default_nib = 4
    c2.nib = (4,0,15,15)
    phi = c1.compute_theta(1)
    psi = c0.compute_theta(0)
    c0.nib = c1.nib = lambda c,x,y,t,theta: (lambda k: 4+k)(15*cos(pi/2*(theta-phi)/(psi-phi))**2)
    c3.nib = lambda c,x,y,t,theta: (lambda k: 8+k)(15*cos(pi/2*(theta-phi)/(psi-phi))**2)

    cont.by = font.dynamicm.by
    cont.lx = 557 + (-18.93 - 58.07) * cont.scale / 3600.0
    cont.rx = 670.187 - (-66.39 - -57.39) * cont.scale / 3600.0

@define_glyph("dynamics")
def _(cont): # s
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 635, 341, -0.845489, -0.533993, 564, 361, 0, 1)
    c1 = CircleInvolute(cont, 564, 361, 0, 1, 592, 398, 0.885832, 0.464007)
    c2 = CircleInvolute(cont, 592, 398, 0.885832, 0.464007, 619, 437, -0.196116, 0.980581)
    c3 = CircleInvolute(cont, 619, 437, -0.196116, 0.980581, 541, 452, -0.776114, -0.630593)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    phi = c1.compute_theta(1)
    cont.default_nib = lambda c,x,y,t,theta: 15+6*cos(theta-phi)
    blob(c0, 0, 'r', 12, 7)
    blob(c3, 1, 'r', 12, 7)

    cont.by = font.dynamicm.by
    cont.lx = 529 + (-0.36 - 52.64) * cont.scale / 3600.0
    cont.rx = 628.788 - (-36.35 - -51.35) * cont.scale / 3600.0

@define_glyph("dynamicz")
def _(cont): # z
    # Saved data from gui.py
    c0 = StraightLine(cont, 568, 338, 678, 338)
    c1 = StraightLine(cont, 678, 338, 539, 453)
    c2 = CircleInvolute(cont, 539, 453, 0.707107, -0.707107, 602, 441, 0.784883, 0.619644)
    c3 = CircleInvolute(cont, 602, 441, 0.784883, 0.619644, 654, 427, 0.33035, -0.943858)
    c4 = CircleInvolute(cont, 654, 427, 0.33035, -0.943858, 654, 411, -0.341743, -0.939793)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    # End saved data

    x2 = c0.compute_x(0)
    x1 = x2 + 30
    x0 = c0.compute_x(1)
    serif = lambda x: 0 if x>x1 else 13*((x-x1)/(x2-x1))**4
    serifangle = 1.15 # radians of the slant at the end of the z's top stroke
    c0.nib = lambda c,x,y,t,theta: (lambda k: (6,1.15,0,k))(min(26 + serif(x), x0-x))
    c1.nib = 6

    xr = c3.compute_x(1)
    xl = c2.compute_x(0)
    m = 0.5
    # Construct a cubic which is 0 at both 0 and 1, and 1 with
    # derivative 0 at m. Second derivative at 0 is non-negative iff
    # m <= 2/3, so we require 1/3 <= m <= 2/3 for the values on
    # [0,1] to be contained within [0,1].
    a = (1-2*m)/(m**4-2*m**3+m**2)
    b = (3*m**2-1)/(m**4-2*m**3+m**2)
    c = -a-b
    #sys.stderr.write("set xrange [0:1]\nplot x*(%g+x*(%g+x*%g))\n" % (c,b,a))
    cubic = lambda x: x*(c+x*(b+x*a))
    slantangle = c1.compute_theta(1)
    c2.nib = c3.nib = lambda c,x,y,t,theta: ((lambda k: (6, slantangle, k, k))(16*cubic((x-xl)/(xr-xl))))

    c4.nib = 6
    blob(c4, 1, 'l', 12, 8)

    cont.by = font.dynamicm.by
    cont.lx = 533 + (-0.2 - 22.8) * cont.scale / 3600.0
    cont.rx = 650.1 - (-65.44 - -42.44) * cont.scale / 3600.0
for x in [getattr(font, "dynamic"+letter) for letter in "fmprsz"]:
    x.origin = (x.by * 3600. / x.scale, x.lx * 3600. / x.scale)
    x.width = x.rx - x.lx

# ----------------------------------------------------------------------
# Accent mark.

@define_glyph("accent")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 421, 415, 633, 472)
    c1 = StraightLine(cont, 633, 472, 421, 529)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    cont.default_nib = 10

@define_glyph("espressivo")
def _(cont):
    cont.extra = (font.accent, "800 0 translate -1 1 scale",
                  font.accent)

# ----------------------------------------------------------------------
# Miscellaneous articulation marks.

@define_glyph("stopping")
def _(cont): # stopping
    # Saved data from gui.py
    c0 = StraightLine(cont, 527, 316, 527, 466)
    c1 = StraightLine(cont, 453, 391, 601, 391)
    # End saved data

    cont.default_nib = 8

@define_glyph("legato")
def _(cont): # legato
    # Saved data from gui.py
    c0 = StraightLine(cont, 454, 461, 600, 461)
    # End saved data

    cont.default_nib = 8
    cont.ly = c0.compute_y(0.5)

@define_glyph("staccato")
def _(cont): # staccato
    cont.extra = "newpath 527 446 26 0 360 arc fill "

@define_glyph("portatoup")
def _(cont): # 'portato' - a staccato stacked on a legato
    cont.extra = (font.legato, "0 -54 translate",
                  font.staccato)

    cont.ly = font.legato.ly

@define_glyph("portatodn")
def _(cont): # portato, the other way up
    cont.extra = "0 1000 translate 1 -1 scale", font.portatoup

    cont.ly = 1000 - font.portatoup.ly

@define_glyph("staccatissdn")
def _(cont): # staccatissimo
    cont.extra = "newpath 498 381 moveto 526 478 lineto 554 381 lineto closepath fill "

@define_glyph("staccatissup")
def _(cont): # staccatissimo pointing the other way
    cont.extra = "newpath 498 478 moveto 526 381 lineto 554 478 lineto closepath fill "

@define_glyph("snappizz")
def _(cont): # snap-pizzicato
    cont.extra = "newpath 500 500 50 0 360 arc 500 500 moveto 500 400 lineto 16 setlinewidth 1 setlinejoin 1 setlinecap stroke"

    cont.oy = 500

# ----------------------------------------------------------------------
# The 'segno' sign (for 'D.S. al Fine' sort of stuff).

@define_glyph("segno")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 504, 162, -0.284088, -0.958798, 420, 152, -0.393919, 0.919145)
    c1 = CircleInvolute(cont, 420, 152, -0.393919, 0.919145, 514, 295, 0.923077, 0.384615)
    c2 = CircleInvolute(cont, 514, 295, 0.923077, 0.384615, 608, 438, -0.393919, 0.919145)
    c3 = CircleInvolute(cont, 608, 438, -0.393919, 0.919145, 524, 428, -0.284088, -0.958798)
    c4 = StraightLine(cont, 624, 128, 404, 462)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    c4.nib = 10

    cont.default_nib = lambda c,x,y,t,theta: 8+16*cos(theta-c.nibdir(t))**2

    phi0 = c0.compute_theta(0)
    phi1 = c1.compute_theta(0) + 3*pi/2
    phi2 = c1.compute_theta(1) + pi
    c0.nibdir = lambda t: phi0 + (phi1-phi0)*t
    c1.nibdir = lambda t: phi1 + (phi2-phi1)*t
    c2.nibdir = lambda t: phi2 + (phi1-phi2)*t
    c3.nibdir = lambda t: phi1 + (phi0-phi1)*t

    # Draw the two dots.
    cont.extra = \
    "newpath 618 251 24 0 360 arc fill " + \
    "newpath 410 339 24 0 360 arc fill "

@define_component("varsegnoend")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 412.999, 375.995, 604.5, 511.5)
    c1 = ExponentialInvolute(cont, 412.999, 375.995, -0.816309, -0.577616, 353, 278, 0.039968, -0.999201)
    c2 = CircleInvolute(cont, 353, 278, 0.039968, -0.999201, 475, 183.5, 0.999764, 0.021734)
    c3 = CircleInvolute(cont, 475, 183.5, 0.999764, 0.021734, 635, 265, 0.581238, 0.813733)
    c4 = ExponentialInvolute(cont, 604.5, 511.5, 0.816309, 0.577616, 654.5, 588, 0.00548671, 0.999985)
    c5 = CircleInvolute(cont, 654.5, 588, 0.00548671, 0.999985, 578, 661, -0.99083, 0.135113)
    c0.weld_to(0, c1, 0)
    c0.weld_to(1, c4, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c4.weld_to(1, c5, 0)
    # End saved data

    slant = c0.compute_theta(0.5)
    cross = slant + pi/2
    def nib(c, x, y, t, theta):
        thickness = cos(theta-slant)**2
        w = 12
        r = 32 - 2*w
        return 8 + r * thickness, cross, w * thickness, w * thickness
    cont.default_nib = nib

    def c3nib(c, x, y, t, theta):
        r, w, f, b = nib(c, x, y, t, theta)
        if t == 1:
            return r + f + b
        else:
            return r + t * (f + b), w, (1-t)*f, (1-t)*b
    c3.nib = c3nib

@define_component("varsegnomiddle")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 396, 530.5, 604.5, 688.5)
    c1 = ExponentialInvolute(cont, 604.5, 688.5, 0.797009, 0.603968, 654.5, 765, 0.00548671, 0.999985)
    c2 = CircleInvolute(cont, 654.5, 765, 0.00548671, 0.999985, 578, 838, -0.99083, 0.135113)
    c3 = ExponentialInvolute(cont, 396, 530.5, -0.797009, -0.603968, 346, 454, -0.00548671, -0.999985)
    c4 = CircleInvolute(cont, 346, 454, -0.00548671, -0.999985, 422.5, 381, 0.99083, -0.135113)
    c0.weld_to(1, c1, 0)
    c0.weld_to(0, c3, 0)
    c1.weld_to(1, c2, 0)
    c3.weld_to(1, c4, 0)
    # End saved data

    cont.default_nib = varsegnoend.default_nib
    cont.centre = c0.compute_x(0.5), c0.compute_y(0.5)

@define_glyph("varsegno")
def _(cont):
    cont.extra = ("0 -100 translate", varsegnoend, varsegnomiddle,
                  "0 177 translate", varsegnomiddle,
                  "%g %g translate 180 rotate %g %g translate" % (
                      varsegnomiddle.centre[0], varsegnomiddle.centre[1],
                      -varsegnomiddle.centre[0], -varsegnomiddle.centre[1]),
                  varsegnoend)
    cont.canvas_size = 1000, 1400

# ----------------------------------------------------------------------
# The coda sign.

@define_glyph("coda")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 528, 198, 528, 475)
    c1 = StraightLine(cont, 418, 337, 639, 337)
    c2 = CircleInvolute(cont, 528, 230, 1, 0, 596, 337, 0, 1)
    c3 = CircleInvolute(cont, 596, 337, 0, 1, 528, 444, -1, 0)
    c4 = CircleInvolute(cont, 528, 444, -1, 0, 460, 337, 0, -1)
    c5 = CircleInvolute(cont, 460, 337, 0, -1, 528, 230, 1, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c2, 0)
    # End saved data

    c0.nib = c1.nib = 10
    cont.default_nib = lambda c,x,y,t,theta: 8+12*abs(sin(theta))**2.5

@define_glyph("varcoda")
def _(cont): # variant square form used by Lilypond
    # Saved data from gui.py
    c0 = StraightLine(cont, 528, 198, 528, 475)
    c1 = StraightLine(cont, 418, 337, 639, 337)
    c2 = CircleInvolute(cont, 469, 241, 0.970143, -0.242536, 587, 241, 0.970143, 0.242536)
    c3 = CircleInvolute(cont, 587, 241, 0.110432, 0.993884, 587, 433, -0.110432, 0.993884)
    c4 = CircleInvolute(cont, 587, 433, -0.970143, 0.242536, 469, 433, -0.970143, -0.242536)
    c5 = CircleInvolute(cont, 469, 433, -0.110432, -0.993884, 469, 241, 0.110432, -0.993884)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    c5.weld_to(1, c2, 0, 1)
    # End saved data

    c0.nib = c1.nib = 10
    c3.nib = c5.nib = 8, 0, 12, 12
    xmid = c0.compute_x(0)
    xend = c2.compute_x(0)
    xdiff = xend - xmid
    c2.nib = c4.nib = lambda c,x,y,t,theta: (lambda k: (8, 0, k, k))(12.0*(x-xmid)/xdiff)

# ----------------------------------------------------------------------
# The turn sign.

@define_glyph("turn")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 443, 448, -0.860927, 0.508729, 370, 401, 0, -1)
    c1 = CircleInvolute(cont, 370, 401, 0, -1, 423, 347, 1, 0)
    c2 = CircleInvolute(cont, 423, 347, 1, 0, 525, 402, 0.707107, 0.707107)
    c3 = CircleInvolute(cont, 525, 402, 0.707107, 0.707107, 627, 457, 1, 0)
    c4 = CircleInvolute(cont, 627, 457, 1, 0, 681, 395, 0, -1)
    c5 = CircleInvolute(cont, 681, 395, 0, -1, 607, 356, -0.860927, 0.508729)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c3.weld_to(1, c4, 0)
    c4.weld_to(1, c5, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 8+16*cos(theta-c.nibdir(theta))**2

    shift = lambda theta: (theta+pi/2) % (2*pi) - pi/2

    theta0 = shift(c0.compute_theta(0))
    phi0 = theta0
    theta2 = shift(c2.compute_theta(1))
    phi2 = theta2 + pi
    c0.nibdir = c1.nibdir = c2.nibdir = c3.nibdir = c4.nibdir = c5.nibdir = \
    lambda theta: phi0 + (phi2-phi0)*(shift(theta)-theta0)/(theta2-theta0)

@define_glyph("invturn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 525, 304, 525, 500)
    # End saved data

    cont.default_nib = 8

    cont.extra = font.turn

@define_glyph("mirrorturn")
def _(cont):
    cont.before = "1000 0 translate -1 1 scale"
    cont.extra = font.turn

@define_glyph("turnhaydn")
def _(cont):
    # Saved data from gui.py
    c0 = ExponentialInvolute(cont, 525, 402, -0.852987, -0.521932, 407, 427, -0.533993, 0.845489)
    c1 = ExponentialInvolute(cont, 525, 402, 0.852987, 0.521932, 643, 377, 0.533993, -0.845489)
    c2 = StraightLine(cont, 525, 336, 525, 468)
    c0.weld_to(0, c1, 0)
    # End saved data

    theta_thin = c0.compute_theta(1)
    cont.default_nib = lambda c,x,y,t,theta: 10+16*sin(theta-theta_thin)**2
    c2.nib = 10

    cont.curve_res *= 10

# ----------------------------------------------------------------------
# Mordent and its relatives.

@define_glyph("mordentupper")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 397.935, 402, 426, 368)
    c1 = StraightLine(cont, 426, 368, 498, 439)
    c2 = StraightLine(cont, 498, 439, 556, 368)
    c3 = StraightLine(cont, 556, 368, 628, 439)
    c4 = StraightLine(cont, 628, 439, 656.065, 405)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    # End saved data

    alpha = c2.compute_theta(.5)
    cont.default_nib = (8, alpha, 30, 30)

    cont.cy = c2.compute_y(.5)

@define_glyph("mordentlower")
def _(cont): # and the same with a vertical line through it
    # Saved data from gui.py
    c0 = StraightLine(cont, 526, 264, 526, 466)
    # End saved data

    cont.default_nib = 8

    # These things are stacked above the note, so they each have a
    # baseline and a height rather than being vertically centred.
    # Hence we must translate the other mordent sign upwards.
    cont.extra = ("gsave 0 -43 translate", font.mordentupper,
                  "grestore")

    cont.cy = font.mordentupper.cy - 43

@define_glyph("mordentupperlong")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 397.935, 402, 426, 368)
    c1 = StraightLine(cont, 426, 368, 498, 439)
    c2 = StraightLine(cont, 498, 439, 556, 368)
    c3 = StraightLine(cont, 556, 368, 628, 439)
    c4 = StraightLine(cont, 628, 439, 686, 368)
    c5 = StraightLine(cont, 686, 368, 758, 439)
    c6 = StraightLine(cont, 758, 439, 786.065, 405)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    c5.weld_to(1, c6, 0, 1)
    # End saved data

    alpha = c2.compute_theta(.5)
    cont.default_nib = (8, alpha, 30, 30)

    cont.cy = font.mordentupper.cy

@define_glyph("mordentupperlower")
def _(cont): # and the same with a vertical line through it
    # Saved data from gui.py
    c0 = StraightLine(cont, 656, 264, 656, 466)
    # End saved data

    cont.default_nib = 8

    # These things are stacked above the note, so they each have a
    # baseline and a height rather than being vertically centred.
    # Hence we must translate the other mordent sign upwards.
    cont.extra = ("gsave 0 -43 translate", font.mordentupperlong,
                  "grestore")

    cont.cy = font.mordentupper.cy - 43

@define_glyph("upmordentupperlong")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 397.935, 402, 426, 368)
    c1 = StraightLine(cont, 426, 368, 498, 439)
    c2 = StraightLine(cont, 498, 439, 556, 368)
    c3 = StraightLine(cont, 556, 368, 628, 439)
    c4 = StraightLine(cont, 628, 439, 686, 368)
    c5 = StraightLine(cont, 686, 368, 758, 439)
    c6 = StraightLine(cont, 758, 439, 786.065, 405)
    c7 = CircleInvolute(cont, 370, 524, -0.354654, -0.934998, 397.935, 402, 0.636585, -0.771206)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c7, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    c5.weld_to(1, c6, 0, 1)
    # End saved data

    alpha = c2.compute_theta(.5)
    cont.default_nib = (8, alpha, 30, 30)
    c0.nib = c7.nib = 8

    cont.cy = font.mordentupper.cy

@define_glyph("upmordentupperlower")
def _(cont): # and the same with a vertical line through it
    # Saved data from gui.py
    c0 = StraightLine(cont, 656, 264, 656, 466)
    # End saved data

    cont.default_nib = 8

    # These things are stacked above the note, so they each have a
    # baseline and a height rather than being vertically centred.
    # Hence we must translate the other mordent sign upwards.
    cont.extra = ("gsave 0 -43 translate", font.upmordentupperlong,
                  "grestore")

    cont.cy = font.mordentupper.cy - 43

@define_glyph("downmordentupperlong")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 378.602, 425.667, 426, 368)
    c1 = StraightLine(cont, 426, 368, 498, 439)
    c2 = StraightLine(cont, 498, 439, 556, 368)
    c3 = StraightLine(cont, 556, 368, 628, 439)
    c4 = StraightLine(cont, 628, 439, 686, 368)
    c5 = StraightLine(cont, 686, 368, 758, 439)
    c6 = StraightLine(cont, 758, 439, 786.065, 405)
    c7 = CircleInvolute(cont, 378, 287, -0.481919, 0.876216, 378.602, 425.667, 0.636585, 0.771206)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c7, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    c5.weld_to(1, c6, 0, 1)
    # End saved data

    alpha = c2.compute_theta(.5)
    cont.default_nib = (8, alpha, 30, 30)
    c0.nib = c7.nib = 8

    cont.cy = font.mordentupper.cy

@define_glyph("downmordentupperlower")
def _(cont): # and the same with a vertical line through it
    # Saved data from gui.py
    c0 = StraightLine(cont, 656, 264, 656, 466)
    # End saved data

    cont.default_nib = 8

    # These things are stacked above the note, so they each have a
    # baseline and a height rather than being vertically centred.
    # Hence we must translate the other mordent sign upwards.
    cont.extra = ("gsave 0 -43 translate", font.downmordentupperlong,
                  "grestore")

    cont.cy = font.mordentupper.cy - 43

@define_glyph("straightmordentupperlong")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 378.602, 425.667, 426, 368)
    c1 = StraightLine(cont, 426, 368, 498, 439)
    c2 = StraightLine(cont, 498, 439, 556, 368)
    c3 = StraightLine(cont, 556, 368, 628, 439)
    c4 = StraightLine(cont, 628, 439, 686, 368)
    c5 = StraightLine(cont, 686, 368, 758, 439)
    c6 = StraightLine(cont, 758, 439, 786.065, 405)
    c7 = StraightLine(cont, 378.602, 277, 378.602, 425.667)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c7, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    c5.weld_to(1, c6, 0, 1)
    # End saved data

    alpha = c2.compute_theta(.5)
    cont.default_nib = (8, alpha, 30, 30)
    c0.nib = c7.nib = 8

    cont.cy = font.mordentupper.cy

@define_glyph("mordentupperlongdown")
def _(cont):
    # Lilypond renders this glyph as a reflection of
    # upmordentupperlong, but it seems obviously preferable to me to
    # render it as a rotation of downmordentupperlong, so as to get
    # the mordent zigzag itself the same way round.
    cont.extra = ("gsave 1000 1000 translate -1 -1 scale",
                  font.downmordentupperlong)
    cont.cy = 1000 - font.mordentupper.cy

@define_glyph("mordentupperlongup")
def _(cont):
    # Likewise, Lilypond uses a reflection of downmordentupperlong,
    # whereas I rotate upmordentupperlong.
    cont.extra = ("gsave 1000 1000 translate -1 -1 scale",
                  font.upmordentupperlong)
    cont.cy = 1000 - font.mordentupper.cy

# ----------------------------------------------------------------------
# Fermata signs.

@define_glyph("fermata")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 364, 465, 0, -1, 527, 313, 1, 0)
    c1 = CircleInvolute(cont, 527, 313, 1, 0, 690, 465, 0, 1)
    c0.weld_to(1, c1, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 8+18*cos(theta)**2

    # Draw the dot.
    cont.extra = "newpath 527 437 36 0 360 arc fill "

    cont.ox = c1.compute_x(0)
    cont.ax = c1.compute_x(1) + c1.compute_nib(1)

@define_glyph("fermataleft")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 364, 465, 0, -1, 527, 313, 1, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 8+18*cos(theta)**2

    # Draw the dot.
    cont.extra = "newpath 527 437 36 0 360 arc fill "

    cont.ox = font.fermata.ox
    cont.ax = font.fermata.ax

@define_glyph("fermatadbldot")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 364, 465, 0, -1, 527, 313, 1, 0)
    c1 = CircleInvolute(cont, 527, 313, 1, 0, 690, 465, 0, 1)
    c0.weld_to(1, c1, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 8+18*cos(theta)**2

    # Draw the dots.
    cont.extra = ("newpath 477 437 36 0 360 arc fill "
                  "newpath 577 437 36 0 360 arc fill ")

@define_glyph("fermata0")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 384, 465, 527, 234)
    c1 = StraightLine(cont, 527, 233, 670, 465)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    c0.nib = 8
    c1.nib = lambda c,x,y,t,theta: (8, pi, min(24, t*250), 0)

    # Draw the dot.
    cont.extra = "newpath 527 437 36 0 360 arc fill "

@define_glyph("fermata00")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 360, 465, 537, 180)
    c1 = StraightLine(cont, 537, 180, 714, 465)
    c2 = StraightLine(cont, 422, 465, 527, 295)
    c3 = StraightLine(cont, 527, 295, 632, 465)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    c0.nib = c2.nib = 8
    c1.nib = lambda c,x,y,t,theta: (8, pi, min(24, t*250), 0)
    c3.nib = lambda c,x,y,t,theta: (8, pi, min(24, t*150), 0)

    # Draw the dot.
    cont.extra = "newpath 522 437 36 0 360 arc fill "

@define_glyph("fermata2")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 384, 441, 384, 313)
    c1 = StraightLine(cont, 384, 313, 670, 313)
    c2 = StraightLine(cont, 670, 313, 670, 441)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    # End saved data

    cont.default_nib = 8, pi/2, 24, 24

    # Draw the dot.
    cont.extra = "newpath 527 437 36 0 360 arc fill "

@define_glyph("fermata3")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 424, 447, 424, 358)
    c1 = StraightLine(cont, 424, 358, 630, 358)
    c2 = StraightLine(cont, 630, 358, 630, 447)
    c3 = StraightLine(cont, 384, 441, 384, 274)
    c4 = StraightLine(cont, 384, 274, 670, 274)
    c5 = StraightLine(cont, 670, 274, 670, 441)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    c3.weld_to(1, c4, 0, 1)
    c4.weld_to(1, c5, 0, 1)
    # End saved data

    c0.nib = c1.nib = c2.nib = 8, pi/2, 18, 18
    c3.nib = c4.nib = c5.nib = 8, pi/2, 24, 24

    # Draw the dot.
    cont.extra = "newpath 527 437 36 0 360 arc fill "

@define_glyph("fermataup")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermata

@define_glyph("fermataleftup")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermataleft
    cont.ox = font.fermata.ox
    cont.ax = font.fermata.ax

@define_glyph("fermatadbldotup")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermatadbldot

@define_glyph("fermata0up")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermata0

@define_glyph("fermata00up")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermata0

@define_glyph("fermata2up")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermata2

@define_glyph("fermata3up")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.fermata3

# ----------------------------------------------------------------------
# Parentheses to go round accidentals.

@define_glyph("acclparen")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 367, 334, -0.478852, 0.877896, 367, 604, 0.478852, 0.877896)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: 6+8*sin(pi*t)

    cont.rx = c0.compute_x(0) + c0.compute_nib(0) + 10

@define_glyph("accrparen")
def _(cont):
    cont.extra = ("gsave 1000 0 translate -1 1 scale",
                  font.acclparen, "grestore")

    cont.lx = 1000 - font.acclparen.rx

# ----------------------------------------------------------------------
# Braces between staves.

@define_glyph("braceupper")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 442, 109, -0.490261, 0.871576, 401, 692, 0.33035, 0.943858)
    c1 = CircleInvolute(cont, 401, 692, 0.33035, 0.943858, 313, 994, -0.810679, 0.585491)
    c0.weld_to(1, c1, 0)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: 2+30*sin(pi/2*t)**2
    c1.nib = lambda c,x,y,t,theta: 2+30*cos(pi/2*t)**2

    cont.scale = 1600
    cont.origin = 1000, 10

@define_glyph("bracelower")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 442, 919, -0.490261, -0.871576, 401, 336, 0.33035, -0.943858)
    c1 = CircleInvolute(cont, 401, 336, 0.33035, -0.943858, 313, 34, -0.810679, -0.585491)
    c0.weld_to(1, c1, 0)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: 2+30*sin(pi/2*t)**2
    c1.nib = lambda c,x,y,t,theta: 2+30*cos(pi/2*t)**2

    cont.scale = 1600
    cont.origin = 1000, 2170

@define_glyph("fixedbrace", args=(3982,)) # should be 'braceupper'+'bracelower'
def scaledbrace(cont, span): # arbitrarily sized brace
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 87, 20, -0.490261, 0.871576, 64, 313, 0.33035, 0.943858)
    c1 = CircleInvolute(cont, 64, 313, 0.33035, 0.943858, 20, 464, -0.810679, 0.585491)
    c2 = CircleInvolute(cont, 20, 464, 0.810679, 0.585491, 64, 615, -0.33035, 0.943858)
    c3 = CircleInvolute(cont, 64, 615, -0.33035, 0.943858, 87, 907, 0.490261, 0.871576)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0)
    # End saved data

    # We want the absolute distance between the _outer_ edges of the
    # tips - i.e. the logical tip positions incremented by the
    # thinnest nib width - to be equal to 'span'. The minimum nib
    # width is fixed at that which would have equalled 4 under the
    # scale of 1600, i.e. 4*3600/1600 = 9 in output coordinates.
    # Hence we want the logical distance between the tip centres to
    # be span-18.
    xtop = c0.compute_y(0)
    xbot = c3.compute_y(1)
    cont.scale = 3600 * (xbot-xtop) / float(span-18)

    # Now the maximum nib width is fixed relative to the brace
    # shape, and hence is (nearly) always 16. The minimum is
    # calculated from the above scale.
    nibmin = 4 * cont.scale / 1600
    nibmax = (8 + (32-8)*sqrt((span-525)/(4000.-525))) * cont.scale / 1600
    nibdiff = nibmax - nibmin

    c0.nib = lambda c,x,y,t,theta: nibmin+nibdiff*sin(pi/2*t)**2
    c1.nib = lambda c,x,y,t,theta: nibmin+nibdiff*cos(pi/2*t)**2
    c2.nib = lambda c,x,y,t,theta: nibmin+nibdiff*sin(pi/2*t)**2
    c3.nib = lambda c,x,y,t,theta: nibmin+nibdiff*cos(pi/2*t)**2

    cont.canvas_size = 105, 930

    cont.trace_res = max(8, int(ceil(8*sqrt(1600.0/cont.scale))))
    cont.curve_res = max(1001, int(span))

# ----------------------------------------------------------------------
# End pieces for an arbitrary-sized bracket between two staves.

@define_glyph("bracketlower", args=(75,))
@define_glyph("bracketlowerlily", args=(-1,)) # omit the vertical
def _(cont, vwid):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 616, 615, -0.808736, -0.588172, 407, 541, -1, 0)
    c1 = StraightLine(cont, 407, 541, 407, 441)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    if vwid < 0:
        c1.nib = 0
    else:
        c1.nib = (4,0,vwid,0)
    y0 = c0.compute_y(0)
    y1 = c0.compute_y(1)
    c0.nib = lambda c,x,y,t,theta: (4,pi/2,45*(y-y0)/(y1-y0),0)

    cont.hy = c1.compute_y(0)

@define_glyph("bracketupper", args=(font.bracketlower,))
@define_glyph("bracketupperlily", args=(font.bracketlowerlily,))
def _(cont, x):
    cont.extra = "0 946 translate 1 -1 scale", x

    cont.hy = 946 - x.hy

# ----------------------------------------------------------------------
# Note head indicating an artificial harmonic above another base
# note.

@define_glyph("harmart")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 526, 402, 0.526355, 0.850265, 609, 476, 0.884918, 0.465746)
    c1 = CircleInvolute(cont, 609, 476, -0.850265, 0.526355, 528, 541, -0.613941, 0.789352)
    c2 = CircleInvolute(cont, 528, 541, -0.526355, -0.850265, 445, 467, -0.884918, -0.465746)
    c3 = CircleInvolute(cont, 445, 467, 0.850265, -0.526355, 526, 402, 0.613941, -0.789352)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c3, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    c0.nib = c2.nib = lambda c,x,y,t,theta: (2,theta-pi/2,min(24,t*200,(1-t)*200),0)
    c1.nib = c3.nib = lambda c,x,y,t,theta: (2,theta-pi/2,min(6,t*50,(1-t)*50),0)

    cont.ay = c1.compute_y(0)

@define_glyph("harmartfilled")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 526, 402, 0.526355, 0.850265, 609, 476, 0.884918, 0.465746)
    c1 = CircleInvolute(cont, 609, 476, -0.850265, 0.526355, 528, 541, -0.613941, 0.789352)
    c2 = CircleInvolute(cont, 528, 541, -0.526355, -0.850265, 445, 467, -0.884918, -0.465746)
    c3 = CircleInvolute(cont, 445, 467, 0.850265, -0.526355, 526, 402, 0.613941, -0.789352)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c3, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,527,472,2)

    cont.ay = c1.compute_y(0)

# ----------------------------------------------------------------------
# Natural harmonic mark and a couple of other miscellaneous note flags.

@define_glyph("harmnat")
def _(cont):
    cont.extra = "newpath 527 439 40 0 360 arc 6 setlinewidth stroke "

@define_glyph("flagopen", args=(0,))
@define_glyph("flagthumb", args=(1,))
def _(cont, thumb):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 500, 450, 1, 0, 537, 500, 0, 1)
    c1 = CircleInvolute(cont, 537, 500, 0, 1, 500, 550, -1, 0)
    c2 = CircleInvolute(cont, 500, 550, -1, 0, 463, 500, 0, -1)
    c3 = CircleInvolute(cont, 463, 500, 0, -1, 500, 450, 1, 0)
    c4 = StraightLine(cont, 500, 580, 500, 554)
    c0.weld_to(1, c1, 0)
    c0.weld_to(0, c3, 1)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 6 + 4*sin(theta)**2
    if thumb:
        c4.nib = 10
    else:
        c4.nib = 0

    cont.cy = c0.compute_y(1)

@define_glyph("flaghalfopend")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 580, 570, 420)
    # End saved data

    c0.nib = 6
    cont.extra = font.flagopen

    cont.cy = font.flagopen.cy

@define_glyph("flaghalfopenv")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 500, 580, 500, 420)
    # End saved data

    c0.nib = 6
    cont.extra = font.flagopen

    cont.cy = font.flagopen.cy

# ----------------------------------------------------------------------
# Ditto (same as previous bar) mark.

@define_glyph("ditto")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 425, 604, 630, 339)
    # End saved data

    c0.nib = (4,0,40,40)

    cont.extra = \
    "newpath 423 397 35 0 360 arc fill " + \
    "newpath 632 546 35 0 360 arc fill "

# ----------------------------------------------------------------------
# Breath mark and related stuff.

@define_glyph("breath")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 577, 341, 0.843661, 0.536875, 548, 466, -0.894427, 0.447214)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: 4+30*cos(pi/2*t)**2

    blob(c0, 0, 'l', 5, 0)

@define_glyph("varbreath")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 547, 466, 587, 341)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: 4+14*t

@define_glyph("revbreath")
def _(cont):
    cont.extra = "1000 1000 translate -1 -1 scale", font.breath

@define_glyph("revvarbreath")
def _(cont):
    cont.extra = "1000 1000 translate -1 -1 scale", font.varbreath

@define_glyph("tickmark")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 425, 397, 0.808736, 0.588172, 500.5, 486.5, 0.393919, 0.919145)
    c1 = CircleInvolute(cont, 500.5, 486.5, 0.247004, -0.969015, 640, 280, 0.910366, -0.413803)
    c0.weld_to(1, c1, 0, 1)
    # End saved data
    c0.nib = lambda c,x,y,t,theta: 4+12*t
    c1.nib = lambda c,x,y,t,theta: 4+12*(1-t)**2
    cont.ox = c0.compute_x(1)

@define_glyph("caesura")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 400, 625, 550, 375)
    c1 = StraightLine(cont, 475, 625, 625, 375)
    # End saved data
    cont.default_nib = 8

@define_glyph("caesuracurved")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 400, 625, 550-400, 375-625, 500, 375, 0, -1)
    c1 = CircleInvolute(cont, 475, 625, 625-475, 375-625, 575, 375, 0, -1)
    # End saved data
    cont.default_nib = lambda c,x,y,t,theta: 8+4.0*(x-c.compute_x(0))/(c.compute_x(1)-c.compute_x(0))

# ----------------------------------------------------------------------
# Random functional stuff like arrowheads.

@define_glyph("openarrowright", args=(0,1))
@define_glyph("closearrowright", args=(0,0))
@define_glyph("openarrowleft", args=(180,1))
@define_glyph("closearrowleft", args=(180,0))
@define_glyph("openarrowup", args=(270,1))
@define_glyph("closearrowup", args=(270,0))
@define_glyph("openarrowdown", args=(90,1))
@define_glyph("closearrowdown", args=(90,0))
def _(cont, rotate, is_open):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 375, 450, 0.83205, 0.5547, 500, 500, 0.977802, 0.209529)
    c1 = CircleInvolute(cont, 500, 500, -0.977802, 0.209529, 375, 550, -0.83205, 0.5547)
    c2 = CircleInvolute(cont, 375, 550, 0.519947, -0.854199, 375, 450, -0.519947, -0.854199)
    c0.weld_to(1, c1, 0, 1)
    c0.weld_to(0, c2, 1, 1)
    c1.weld_to(1, c2, 0, 1)
    # End saved data

    if is_open:
        cont.default_nib = 10
        c2.nib = 0
    else:
        x0, y0 = c0.compute_x(0.5), c0.compute_y(1)
        cont.default_nib = lambda c,x,y,t,theta: ptp_nib(c,x,y,t,theta,x0,y0,10)

    if rotate:
        cont.before = "500 500 translate %g rotate -500 -500 translate" % rotate

    cont.cx = cont.cy = 500
    cont.extent = abs(c0.compute_y(0) - cont.cy) + 6

# ----------------------------------------------------------------------
# Flat (and multiples of flat).

@define_glyph("flat")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 236, 430, 548)
    c1 = Bezier(cont, 430, 548, 481, 499, 515.999, 458, 505, 424)
    c2 = CircleInvolute(cont, 505, 424, -0.307801, -0.951451, 430, 436, -0.462566, 0.886585)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0)
    # End saved data

    c0.nib = 8

    x0 = c1.compute_x(0)
    x1 = c1.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: 8+12*((x-x0)/(x1-x0))**2

    cont.ox = c0.compute_x(0.5) - 8
    #cont.hy = 469 # no sensible way to specify this except manually
    cont.hy = c0.compute_y(1) - 8  #500 # no sensible way to specify this except manually

@define_glyph("flatup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 236, 430, 548)
    c1 = Bezier(cont, 430, 548, 481, 499, 515.999, 458, 505, 424)
    c2 = CircleInvolute(cont, 505, 424, -0.307801, -0.951451, 430, 436, -0.462566, 0.886585)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0)
    # End saved data

    c0.nib = 8

    x0 = c1.compute_x(0)
    x1 = c1.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: 8+12*((x-x0)/(x1-x0))**2

    #cont.ox = c0.compute_x(0.5)
    cont.ox = font.flat.ox
    #cont.hy = 469 # no sensible way to specify this except manually
    cont.hy = font.flat.hy

    cont.extra = "gsave 430 236 16 add translate 0.7 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"

@define_glyph("flatupup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 266, 430, 548)
    c1 = Bezier(cont, 430, 548, 481, 499, 515.999, 458, 505, 424)
    c2 = CircleInvolute(cont, 505, 424, -0.307801, -0.951451, 430, 436, -0.462566, 0.886585)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0)
    # End saved data

    c0.nib = 8

    x0 = c1.compute_x(0)
    x1 = c1.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: 8+12*((x-x0)/(x1-x0))**2

    #cont.ox = c0.compute_x(0.5)
    cont.ox = font.flat.ox
    #cont.hy = 469 # no sensible way to specify this except manually
    cont.hy = font.flat.hy

    cont.extra = "gsave 430 266 16 add translate 0.55 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore", "gsave 430 204 16 add translate 0.55 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"

@define_glyph("flatdn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 236, 430, 568)
    c1 = Bezier(cont, 430, 548, 481, 499, 515.999, 458, 505, 424)
    c2 = CircleInvolute(cont, 505, 424, -0.307801, -0.951451, 430, 436, -0.462566, 0.886585)
    c1.weld_to(1, c2, 0)
    # End saved data

    c0.nib = 8

    x0 = c1.compute_x(0)
    x1 = c1.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: 8+12*((x-x0)/(x1-x0))**2

    #cont.ox = c0.compute_x(0.5)
    cont.ox = font.flat.ox
    cont.hy = font.flat.hy
    #cont.hy = 469 # no sensible way to specify this except manually

    cont.extra = "gsave 430 568 16 sub translate 0.7 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore"

@define_glyph("flatdndn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 236, 430, 558)
    c1 = Bezier(cont, 430, 548, 481, 499, 515.999, 458, 505, 424)
    c2 = CircleInvolute(cont, 505, 424, -0.307801, -0.951451, 430, 436, -0.462566, 0.886585)
    c1.weld_to(1, c2, 0)
    # End saved data

    c0.nib = 8

    x0 = c1.compute_x(0)
    x1 = c1.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: 8+12*((x-x0)/(x1-x0))**2

    #cont.ox = c0.compute_x(0.5)
    cont.ox = font.flat.ox
    cont.hy = font.flat.hy
    #cont.hy = 469 # no sensible way to specify this except manually

    cont.extra = "gsave 430 558 16 sub translate 0.55 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore", "gsave 430 620 16 sub translate 0.55 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore"

@define_glyph("flatupdn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 430, 236, 430, 568)
    c1 = Bezier(cont, 430, 548, 481, 499, 515.999, 458, 505, 424)
    c2 = CircleInvolute(cont, 505, 424, -0.307801, -0.951451, 430, 436, -0.462566, 0.886585)
    c1.weld_to(1, c2, 0)
    # End saved data

    c0.nib = 8

    x0 = c1.compute_x(0)
    x1 = c1.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: 8+12*((x-x0)/(x1-x0))**2

    cont.ox = c0.compute_x(0.5)
    cont.hy = font.flat.hy
    #cont.hy = 469 # no sensible way to specify this except manually

    cont.extra = font.flatup.extra + font.flatdn.extra

@define_glyph("doubleflat")
def _(cont):
    cont.extra = (font.flat, "gsave -90 0 translate",
                  font.flat, "grestore")
    cont.ox = font.flat.ox - 90
    cont.hy = font.flat.hy

@define_glyph("semiflat")
def _(cont):
    reflectpt = font.flat.ox - 20
    cont.extra = ("gsave %g 0 translate -1 1 scale" % (2*reflectpt),
                  font.flat, "grestore")
    cont.hy = font.flat.hy

@define_glyph("sesquiflat")
def _(cont):
    cont.extra = font.flat, font.semiflat
    cont.hy = font.flat.hy

@define_glyph("smallflat")
def _(cont):
    cont.extra = ("gsave 580 380 translate 0.5 dup scale -580 -380 translate",
                  font.flat, "grestore")

@define_glyph("flatslash")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 370, 363, 490, 303)
    # End saved data

    c0.nib = 8

    cont.ox = font.flat.ox
    cont.hy = font.flat.hy

    cont.extra = font.flat

@define_glyph("flatslash2")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 372, 373, 490, 333)
    c1 = StraightLine(cont, 372, 313, 490, 273)
    # End saved data

    c0.nib = c1.nib = 8

    cont.ox = font.flat.ox
    cont.hy = font.flat.hy

    cont.extra = font.flat

@define_glyph("semiflatslash")
def _(cont):
    reflectpt = font.flat.ox - 20
    cont.extra = ("gsave %g 0 translate -1 1 scale" % (2*reflectpt),
                  font.flatslash, "grestore")
    cont.hy = font.flatslash.hy

@define_glyph("doubleflatslash")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 282, 361, 490, 281)
    # End saved data

    c0.nib = 8

    cont.ox = font.doubleflat.ox
    cont.hy = font.doubleflat.hy

    cont.extra = font.doubleflat

# ----------------------------------------------------------------------
# Natural.

@define_glyph("natural")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 519, 622, 519, 399)
    c1 = StraightLine(cont, 519, 399, 442, 418)
    c2 = StraightLine(cont, 442, 318, 442, 539)
    c3 = StraightLine(cont, 442, 539, 519, 520)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.ox = c3.compute_x(0) - 8
    #cont.cy = (c0.compute_y(0) + c2.compute_y(0)) / 2.0
    cont.cy = c0.compute_y(0)

@define_glyph("naturalup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 519, 622, 519, 399)
    c1 = StraightLine(cont, 519, 399, 442, 418)
    c2 = StraightLine(cont, 442, 318, 442, 539)
    c3 = StraightLine(cont, 442, 539, 519, 520)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 442 318 translate 0.7 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"

    cont.ox = font.natural.ox
    cont.cy = font.natural.cy

@define_glyph("naturalupup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 519, 622, 519, 399)
    c1 = StraightLine(cont, 519, 399, 442, 418)
    c2 = StraightLine(cont, 442, 338, 442, 539)
    c3 = StraightLine(cont, 442, 539, 519, 520)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 442 338 translate 0.55 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore", "gsave 442 276 translate 0.55 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"

    cont.ox = font.natural.ox
    cont.cy = font.natural.cy

@define_glyph("naturaldn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 519, 622, 519, 399)
    c1 = StraightLine(cont, 519, 399, 442, 418)
    c2 = StraightLine(cont, 442, 318, 442, 539)
    c3 = StraightLine(cont, 442, 539, 519, 520)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 519 622 translate 0.7 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore"

    cont.ox = font.natural.ox
    cont.cy = font.natural.cy

@define_glyph("naturaldndn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 519, 602, 519, 399)
    c1 = StraightLine(cont, 519, 399, 442, 418)
    c2 = StraightLine(cont, 442, 318, 442, 539)
    c3 = StraightLine(cont, 442, 539, 519, 520)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 519 602 translate 0.55 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore", "gsave 519 664 translate 0.55 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore"

    cont.ox = font.natural.ox
    cont.cy = font.natural.cy

@define_glyph("naturalupdn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 519, 622, 519, 399)
    c1 = StraightLine(cont, 519, 399, 442, 418)
    c2 = StraightLine(cont, 442, 318, 442, 539)
    c3 = StraightLine(cont, 442, 539, 519, 520)
    c0.weld_to(1, c1, 0, 1)
    c2.weld_to(1, c3, 0, 1)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = font.naturalup.extra + font.naturaldn.extra

    cont.cy = font.natural.cy

@define_glyph("smallnatural")
def _(cont):
    cont.extra = "gsave 580 280 translate 0.5 dup scale -580 -280 translate", font.natural, "grestore"

# ----------------------------------------------------------------------
# Sharp.

@define_glyph("sharp")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 652)
    c1 = StraightLine(cont, 493, 291, 493, 637)
    c2 = StraightLine(cont, 413, 419, 523, 392)
    c3 = StraightLine(cont, 413, 551, 523, 524)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.ox = c2.compute_x(0) - 8
    #cont.cy = (c2.compute_y(0) + c3.compute_y(1))/2.0
    cont.cy = c0.compute_y(1)

@define_glyph("sharpup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 652)
    c1 = StraightLine(cont, 493, 271, 493, 637)
    c2 = StraightLine(cont, 413, 419, 523, 392)
    c3 = StraightLine(cont, 413, 551, 523, 524)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 493 271 translate 0.7 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"

    cont.ox = font.sharp.ox
    cont.cy = font.sharp.cy

@define_glyph("sharpupup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 301, 442, 652)
    c1 = StraightLine(cont, 493, 291, 493, 637)
    c2 = StraightLine(cont, 413, 419, 523, 392)
    c3 = StraightLine(cont, 413, 551, 523, 524)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    #cont.extra = "gsave 493 271 translate 0.7 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"
    cont.extra = "gsave 442 301 translate 0.55 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore", "gsave 442 239 translate 0.55 dup scale -500 dup 150 sub translate", font.closearrowup, "grestore"

    cont.ox = font.sharp.ox
    cont.cy = font.sharp.cy


@define_glyph("sharpdn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 672)
    c1 = StraightLine(cont, 493, 291, 493, 637)
    c2 = StraightLine(cont, 413, 419, 523, 392)
    c3 = StraightLine(cont, 413, 551, 523, 524)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 442 672 translate 0.7 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore"

    cont.ox = font.sharp.ox
    cont.cy = font.sharp.cy

@define_glyph("sharpdndn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 652)
    c1 = StraightLine(cont, 493, 291, 493, 642)
    c2 = StraightLine(cont, 413, 419, 523, 392)
    c3 = StraightLine(cont, 413, 551, 523, 524)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = "gsave 493 642 translate 0.55 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore", "gsave 493 704 translate 0.55 dup scale -500 dup 150 add translate", font.closearrowdown, "grestore"

    cont.ox = font.sharp.ox
    cont.cy = font.sharp.cy

@define_glyph("sharpupdn")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 672)
    c1 = StraightLine(cont, 493, 271, 493, 637)
    c2 = StraightLine(cont, 413, 419, 523, 392)
    c3 = StraightLine(cont, 413, 551, 523, 524)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.extra = font.sharpup.extra + font.sharpdn.extra

    cont.cy = font.sharp.cy

@define_glyph("smallsharp")
def _(cont):
    cont.extra = "gsave 580 280 translate 0.5 dup scale -580 -280 translate", font.sharp, "grestore"

@define_glyph("semisharp")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 652)
    c1 = StraightLine(cont, 413, 421, 472, 401.518)
    c2 = StraightLine(cont, 413, 555, 472, 533.981)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

@define_glyph("sesquisharp")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 300.351, 442, 646.351)
    c1 = StraightLine(cont, 493, 291, 493, 637)
    c2 = StraightLine(cont, 544, 281.649, 544, 627.649)
    c3 = StraightLine(cont, 413, 414, 574, 384.481)
    c4 = StraightLine(cont, 413, 547, 574, 517.481)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

@define_glyph("sharp3")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 652)
    c1 = StraightLine(cont, 493, 291, 493, 637)
    c2 = StraightLine(cont, 413, 397, 523, 370)
    c3 = StraightLine(cont, 413, 573, 523, 546)
    c4 = StraightLine(cont, 401, 487.945, 535, 455.055)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.cy = (c2.compute_y(0) + c3.compute_y(1))/2.0

@define_glyph("semisharp3")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 442, 306, 442, 652)
    c1 = StraightLine(cont, 413, 399, 472, 379.518)
    c2 = StraightLine(cont, 413, 577, 472, 555.981)
    c3 = StraightLine(cont, 400.5, 492.703, 483.5, 465.297)
    # End saved data

    cont.default_nib = (8, pi/2, 16, 16)

    cont.cy = (c2.compute_y(0) + c3.compute_y(1))/2.0

# ----------------------------------------------------------------------
# Double sharp.

@define_glyph("doublesharp")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 409, 426, 504, 521)
    c1 = StraightLine(cont, 409, 521, 504, 426)
    # End saved data

    cont.default_nib = 8

    # Blobs at the ends of the lines.
    cont.extra = \
    "/square { gsave 3 1 roll translate newpath dup dup moveto dup neg dup neg lineto dup neg dup lineto dup neg lineto closepath fill grestore } def " + \
    "newpath 409 426 24 square " + \
    "newpath 409 521 24 square " + \
    "newpath 504 426 24 square " + \
    "newpath 504 521 24 square "

# ----------------------------------------------------------------------
# Arpeggio mark and friends.

@define_glyph("arpeggio")
def _(cont):
    # Saved data from gui.py
    c0 = Bezier(cont, 491, 334, 516, 359, 516, 378, 491, 403)
    c1 = Bezier(cont, 491, 403, 466, 428, 466, 447, 491, 472)
    c2 = Bezier(cont, 491, 472, 516, 497, 516, 516, 491, 541)
    c3 = Bezier(cont, 491, 541, 466, 566, 466, 585, 491, 610)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 4+14*abs(cos(theta + 3*pi/4))**1.5

@define_glyph("arpeggioshort")
def _(cont):
    # Saved data from gui.py
    c0 = Bezier(cont, 491, 334, 516, 359, 516, 378, 491, 403)
    c1 = Bezier(cont, 491, 403, 466, 428, 466, 447, 491, 472)
    c0.weld_to(1, c1, 0)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 4+14*abs(cos(theta + 3*pi/4))**1.5

    cont.ty = c0.compute_y(0)
    cont.oy = c1.compute_y(1)
    cont.lx = c0.compute_x(0) - font.closearrowdown.extent
    cont.rx = c0.compute_x(0) + font.closearrowdown.extent

@define_glyph("arpeggioarrowdown")
def _(cont):
    # Saved data from gui.py
    c0 = Bezier(cont, 491, 334, 516, 359, 491, 370, 491, 403)
    # End saved data

    cont.default_nib = lambda c,x,y,t,theta: 4+16*t*(1-t)

    cont.extra = "-9 0 translate", font.closearrowdown

    cont.lx = font.arpeggioshort.lx
    cont.rx = font.arpeggioshort.rx
    cont.ey = c0.compute_y(0)

@define_glyph("arpeggioarrowup")
def _(cont):
    cont.extra = "1000 1000 translate -1 -1 scale", font.arpeggioarrowdown

    cont.ey = 1000 - font.arpeggioarrowdown.ey
    cont.lx = 1000 - font.arpeggioshort.rx
    cont.rx = 1000 - font.arpeggioshort.lx

@define_glyph("trillwiggle")
def _(cont):
    # Rotate the arpeggio mark by 90 degrees and use it as the wavy
    # line after 'tr' to indicate an extended trill.
    cont.extra = ("500 500 translate -90 rotate -500 -500 translate",
                  font.arpeggioshort)

    cont.lx = font.arpeggioshort.ty
    cont.rx = font.arpeggioshort.oy

# ----------------------------------------------------------------------
# Downbow and upbow marks.

@define_glyph("bowdown")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 447, 430, 447, 330)
    c1 = StraightLine(cont, 447, 330, 608, 330)
    c2 = StraightLine(cont, 608, 330, 608, 430)
    c0.weld_to(1, c1, 0, 1)
    c1.weld_to(1, c2, 0, 1)
    # End saved data

    cont.default_nib = 8, pi/2, 35, 35

@define_glyph("bowup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 475, 256, 535, 460)
    c1 = StraightLine(cont, 535, 460, 595, 256)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: (6, 0, min(25, (1-t)*100), 0)
    c1.nib = 6

# ----------------------------------------------------------------------
# Sforzando / marcato is an inverted upbow mark.

@define_glyph("sforzando")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 475, 460, 535, 256)
    c1 = StraightLine(cont, 535, 256, 595, 460)
    c0.weld_to(1, c1, 0, 1)
    # End saved data

    c0.nib = 6
    c1.nib = lambda c,x,y,t,theta: (6, pi, min(25, t*100), 0)

@define_glyph("sforzandodn")
def _(cont):
    cont.extra = "1000 1000 translate -1 -1 scale", font.sforzando

# ----------------------------------------------------------------------
# Repeat mark (just a pair of dots).

@define_glyph("repeatmarks")
def _(cont):
    cont.extra = \
    "newpath 561 401 32 0 360 arc fill " + \
    "newpath 561 542 32 0 360 arc fill "

# ----------------------------------------------------------------------
# Grace notes.

@define_glyph("appoggiatura")
def _(cont):
    cont.extra = [
    "gsave 495 472 translate 0.45 dup scale -527 -472 translate",
    "gsave 602.346 452.748 -450 add translate -535 -465 translate",
    font.tailquaverup,
    "grestore",
    font.headcrotchet,
    "newpath 602.346 452.748 moveto 0 -450 rlineto 16 setlinewidth stroke",
    "grestore",
    ]

@define_component("accslashup")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 502, 394, 601, 327)
    # End saved data

    c0.nib = 6

    cont.ox = 532
    cont.oy = 261

@define_glyph("acciaccatura")
def _(cont):
    cont.extra = font.appoggiatura, accslashup

@define_glyph("accslashbigup")
def _(cont):
    cont.extra = "-500 0 translate 1 .45 div dup scale", accslashup

    cont.ox = -500 + accslashup.ox / .45
    cont.oy = accslashup.oy / .45

@define_glyph("accslashbigdn")
def _(cont):
    cont.extra = '0 1000 translate 1 -1 scale', font.accslashbigup

    cont.ox = font.accslashbigup.ox
    cont.oy = 1000 - font.accslashbigup.oy

# ----------------------------------------------------------------------
# Piano pedal marks.

@define_glyph("pedP")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 340, 451, 0.039968, 0.999201, 293, 487, -0.664364, -0.747409)
    c1 = CircleInvolute(cont, 293, 487, -0.664364, -0.747409, 399, 373, 1, 0)
    c2 = CircleInvolute(cont, 399, 373, 1, 0, 472, 451, -0.485643, 0.874157)
    c3 = CircleInvolute(cont, 472, 451, -0.485643, 0.874157, 421, 441, -0.164399, -0.986394)
    c4 = Bezier(cont, 395, 376, 374.611, 410.556, 351.98, 449.02, 388.876, 485.371)
    c5 = Bezier(cont, 388.876, 485.371, 428.041, 523.958, 366, 586, 331.736, 624.799)
    c6 = CircleInvolute(cont, 331.736, 624.799, 0.225579, -0.974225, 440, 613.5, 0.464007, 0.885832)
    c7 = StraightLine(cont, 440, 613.5, 482, 580)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    c4.weld_to(1, c5, 0)
    c5.weld_to(1, c6, 0, 1)
    c6.weld_to(1, c7, 0, 1)
    # End saved data

    cont.default_nib = 6

    # Construct a quintic which is 0 with derivative 0 at both 0 and
    # 1, and 1 with derivative 0 at m. Second derivative at 0 is
    # non-negative iff m <= 0.6, so we require 0.4 <= m <= 0.6 for
    # the values on [0,1] to be contained within [0,1].
    def quintic(m,x):
        denom = m*m*m*(-1+m*(3+m*(-3+m)))
        a = (2-4*m)/denom
        b = (-4+m*(5+m*5))/denom
        c = (2+m*(2+m*-10))/denom
        d = (m*(-3+m*5))/denom
        return x*x*(d+x*(c+x*(b+x*a)))
    def shift(theta, phi):
        return (theta-(phi+pi)) % (2*pi) + (phi+pi)
    shift01 = 3*pi/4
    end0 = shift(c0.compute_theta(0),shift01)
    end1 = shift(c1.compute_theta(1),shift01)
    c0.nib = c1.nib = lambda c,x,y,t,theta: 6+10*quintic(0.4, (shift(theta, shift01)-end0)/(end1-end0))
    shift23 = -3*pi/4
    end2 = shift(c2.compute_theta(0),shift23)
    end3 = shift(c3.compute_theta(1),shift23)
    c2.nib = c3.nib = lambda c,x,y,t,theta: 6+10*quintic(0.6, (shift(theta, shift23)-end2)/(end3-end2))

    theta45 = (c4.compute_theta(0) + c5.compute_theta(1))/2
    c4.nib = c5.nib = lambda c,x,y,t,theta: 6 + 15*sin(theta-theta45)**2

    theta7 = c7.compute_theta(0)
    c6.nib = lambda c,x,y,t,theta: (6, theta7, 18*t**2, 18*t**2)

    cont.by = c5.compute_y(1) + c5.compute_nib(1)

@define_glyph("pede")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 482, 580, 0.786318, -0.617822, 533, 541, 0.804176, -0.594391)
    c1 = CircleInvolute(cont, 533, 541, 0.804176, -0.594391, 520, 496, -1, 0)
    c2 = CircleInvolute(cont, 520, 496, -1, 0, 495, 604, 0.485643, 0.874157)
    c3 = CircleInvolute(cont, 495, 604, 0.485643, 0.874157, 571, 591, 0.581238, -0.813733)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    theta0 = c0.compute_theta(0)
    theta1 = c3.compute_theta(1)
    c0.nib = 6, theta0, 7, 0 # avoid running over left edge of e
    c1.nib = 6, theta0, 7, 7 # avoid running over left edge of e
    c2.nib = lambda c,x,y,t,theta: (6, theta0, 7+3*t, 7+3*t)
    c3.nib = lambda c,x,y,t,theta: (6, (theta0+t*(theta1-theta0)), 10, 10)

    cont.by = font.pedP.by

@define_glyph("pedd")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 638, 484, -0.91707, 0.398726, 580, 567, 0, 1)
    c1 = CircleInvolute(cont, 580, 567, 0, 1, 623, 625, 1, 0)
    c2 = CircleInvolute(cont, 623, 625, 1, -0, 664, 527, -0.304776, -0.952424)
    c3 = CircleInvolute(cont, 664, 527, -0.304776, -0.952424, 514, 410, -0.980581, -0.196116)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    c2.weld_to(1, c3, 0)
    # End saved data

    theta0 = -pi
    theta1 = 0
    theta2 = +pi
    c0.nib = c1.nib = lambda c,x,y,t,theta: 6+8*sin(pi*(theta-theta0)/(theta1-theta0))**2
    c2.nib = c3.nib = lambda c,x,y,t,theta: 6+12*sin(pi*(theta-theta1)/(theta2-theta1))**2

    cont.by = font.pedP.by

@define_glyph("peddot")
def _(cont):
    cont.extra = "newpath 708 611 20 0 360 arc fill "

    cont.by = font.pedP.by

@define_glyph("pedPed")
def _(cont):
    cont.extra = font.pedP, font.pede, font.pedd

    cont.by = font.pedP.by

@define_glyph("pedPeddot")
def _(cont):
    cont.extra = (font.pedP, font.pede,
                  font.pedd, font.peddot)

    cont.by = font.pedP.by

# The pedal-up asterisk is drawn by drawing a single curved edge and
# repeating it around the circle eight times.

@define_component("pedstarcomponent")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 411, 448, 0.92388, -0.382683, 425, 425, 0, -1)
    c1 = CircleInvolute(cont, 425, 425, 0, -1, 413, 405, -0.747409, -0.664364)
    c2 = CircleInvolute(cont, 413, 405, -0.747409, -0.664364, 425, 373, 1, 0)
    c0.weld_to(1, c1, 0)
    c1.weld_to(1, c2, 0)
    # End saved data

    x0 = c0.compute_x(1)
    cont.default_nib = lambda c,x,y,t,theta: (6, 0, 2*(x0-x), 0)

    cont.cx = x0
    cont.cy = c0.compute_y(0) + (x0 - c0.compute_x(0)) / tan(pi/8)
    cont.r = sqrt((c0.compute_x(0) - cont.cx)**2 + (c0.compute_y(0) - cont.cy)**2)

@define_glyph("pedstar")
def _(cont):
    cx, cy, r = pedstarcomponent.cx, pedstarcomponent.cy, pedstarcomponent.r

    cont.extra = "8 {", pedstarcomponent, \
    "%g %g translate 45 rotate %g %g translate } repeat" % (cx,cy, -cx,-cy) + \
    " newpath %g %g %g 0 360 arc closepath 12 setlinewidth stroke" % (cx,cy, r-5)

    cont.by = font.pedP.by

@define_glyph("peddash")
def _(cont):
    # Saved data from gui.py
    c0 = Bezier(cont, 463, 538, 493, 518, 540, 544, 570, 524)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: (4, pi/3, 18, 18)

    cont.by = font.pedP.by

# ----------------------------------------------------------------------
# Some note flags I don't really understand, but which Lilypond's
# font supports so I must too.

@define_glyph("upedalheel")
def _(cont):
    cont.extra = \
    "newpath 450 420 moveto 500 500 50 180 0 arcn 550 420 lineto " + \
    "16 setlinewidth 1 setlinecap stroke"
    cont.cy = 500

@define_glyph("dpedalheel")
def _(cont):
    cont.extra = \
    "newpath 450 580 moveto 500 500 50 180 0 arc 550 580 lineto " + \
    "16 setlinewidth 1 setlinecap stroke"
    cont.cy = 500

@define_glyph("upedaltoe")
def _(cont):
    cont.extra = \
    "newpath 450 420 moveto 500 550 lineto 550 420 lineto " + \
    "16 setlinewidth 1 setlinecap 1 setlinejoin stroke"
    cont.cy = 500

@define_glyph("dpedaltoe")
def _(cont):
    cont.extra = \
    "newpath 450 580 moveto 500 450 lineto 550 580 lineto " + \
    "16 setlinewidth 1 setlinecap 1 setlinejoin stroke"
    cont.cy = 500

# ----------------------------------------------------------------------
# Accordion-specific markings.

@define_glyph("acc2", args=(2,))
@define_glyph("acc3", args=(3,))
@define_glyph("acc4", args=(4,))
def _(cont, n):
    cont.scale = 1440 # make life easier: one stave space is now 100px
    r = 50*n
    cont.extra = "newpath 500 500 %g 0 360 arc " % r
    for i in range(1,n):
        y = 100*i - r
        x = sqrt(r*r - y*y)
        cont.extra = cont.extra + "%g %g moveto %g %g lineto " % (500-x, 500+y, 500+x, 500+y)
    cont.extra = cont.extra + "8 setlinewidth stroke"

@define_glyph("accr", args=(2,3))
def _(cont, w,h):
    cont.scale = 1440 # make life easier: one stave space is now 100px
    ww = 50*w
    hh = 50*h
    cont.extra = ("newpath %g %g moveto %g %g lineto " + \
    "%g %g lineto %g %g lineto closepath ") % \
    (500-ww,500-hh,500+ww,500-hh,500+ww,500+hh,500-ww,500+hh)
    for i in range(1,h):
        y = 100*i - hh
        cont.extra = cont.extra + "%g %g moveto %g %g lineto " % (500-ww, 500+y, 500+ww, 500+y)
    cont.extra = cont.extra + "8 setlinewidth stroke"

@define_glyph("accdot")
def _(cont):
    cont.scale = 1440 # make life easier: one stave space is now 100px
    cont.extra = "newpath 500 500 25 0 360 arc fill "

@define_glyph("accstar")
def _(cont):
    cont.scale = 1440 # make life easier: one stave space is now 100px
    cont.extra = "500 500 translate " + \
    "newpath 0 0 100 0 360 arc 8 setlinewidth stroke " + \
    "8 { " + \
    "  newpath 0 65 20 0 360 arc fill " + \
    "  newpath -9 65 moveto 9 65 lineto 4 0 lineto -4 0 lineto fill" + \
    "  " + \
    "  45 rotate" + \
    "} repeat"

@define_glyph("accpush")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 435, 460, 535, 310)
    c1 = StraightLine(cont, 535, 310, 435, 160)
    c0.weld_to(1, c1, 0)
    # End saved data

    cont.default_nib = 10

    cont.ox = c1.compute_x(0)

@define_glyph("accpull")
def _(cont):
    # Saved data from gui.py
    c0 = StraightLine(cont, 435, 160, 535, 160)
    c1 = StraightLine(cont, 535, 160, 535, 460)
    c0.weld_to(1, c1, 0)
    # End saved data

    c0.nib = lambda c,x,y,t,theta: (10, pi/2, 0, 40) if x<525 else 10
    c1.nib = 10

    cont.ox = c1.compute_x(0)

# ----------------------------------------------------------------------
# Lyric ties.

@define_glyph("lyrictie")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 385, 552, 0.77193, 0.635707, 615, 552, 0.77193, -0.635707)
    c1 = CircleInvolute(cont, 385, 552, 0.879292, 0.476283, 615, 552, 0.879292, -0.476283)
    c0.weld_to(0, c1, 0, 1)
    c0.weld_to(1, c1, 1, 1)
    # End saved data

    cont.ox = 500
    cont.oy = 500

    cont.default_nib = 6

@define_glyph("lyrictieshort")
def _(cont):
    # Saved data from gui.py
    c0 = CircleInvolute(cont, 416, 552, 0.618865, 0.785497, 584, 553, 0.618865, -0.785497)
    c1 = CircleInvolute(cont, 416, 552, 0.791647, 0.610979, 584, 553, 0.791647, -0.610979)
    c0.weld_to(0, c1, 0, 1)
    c0.weld_to(1, c1, 1, 1)
    # End saved data

    cont.ox = 500
    cont.oy = 500

    cont.default_nib = 6

# ----------------------------------------------------------------------
# A blank glyph!

@define_glyph("blank")
def _(cont):
    cont.lx = 500
    cont.rx = 600
    cont.by = 600
    cont.ty = 500

@define_glyph("zerowidthspace")
def _(cont):
    cont.lx = 500
    cont.rx = 500
    cont.by = 600
    cont.ty = 500

@define_glyph("hairspace")
def _(cont):
    cont.lx = 500
    cont.rx = 532
    cont.by = 600
    cont.ty = 500
