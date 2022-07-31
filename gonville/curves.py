import sys
import types
from math import *
from crosspoint import crosspoint

def transform(matrix, x, y, affine=1):
    # 6-element matrix like PostScript's: applying [a,b,c,d,e,f] to
    # transform the point (x,y) returns (ax+cy+e, bx+dy+f).
    a,b,c,d,e,f = matrix
    x, y = a*x+c*y, b*x+d*y
    if affine:
        x, y = x+e, y+f # can omit this for transforming vectors
    return x, y

def translate(x,y):
    return [1, 0, 0, 1, x, y]

def scale(x,y):
    return [x, 0, 0, y, 0, 0]

def atanh(x):
    return 0.5*log((1+x)/(1-x))

class Curve:
    def __init__(self, cont = None):
        self.tkitems = []
        self.welds = [None, None]
        self.weldpri = 1
        self.nib = None

    def postinit(self, cont):
        self.tk_addto(cont.canvas)
        cont.curves[cont.curveid] = self
        cont.curveid = cont.curveid + 1
        self.cid = cont.curveid
        self.cont = cont

    def weld_to(self, end, other, oend, half=0, special=None):
        assert(self.welds[end] == None)
        self.welds[end] = (other, oend, half, special)
        if special != None:
            x, y, par, perp = special
            special = -x, -y, par, perp
        other.welds[oend] = (self, end, half, special)
        self.weld_update(end)

    def unweld(self, end):
        if self.welds[end] != None:
            other, oend, half, special = self.welds[end]
            other.welds[oend] = None
            self.welds[end] = None

    def cleanup(self):
        for x in self.tkitems:
            self.canvas.delete(x)
        for end in (0,1):
            if self.welds[end] != None:
                other, oend, half, special = self.welds[end]
                other.welds[oend] = None

    def weld_update(self, end, thispri = None):
        if not self.welds[end]:
            return
        other, oend, half, special = self.welds[end]

        ourepri = 1
        ourdpri = self.weldpri
        if thispri is not None:
            ourepri = max(ourepri, thispri)
            ourdpri = max(ourdpri, thispri)
        otherepri = 1
        otherdpri = other.weldpri

        sx, sy, sdx, sdy = self.enddata(end)
        ox, oy, odx, ody = other.enddata(oend)

        slen = sqrt(sdx*sdx+sdy*sdy)

        if special:
            ox = ox - special[0] - special[2]*sdx/slen - special[3]*sdy/slen
            oy = oy - special[1] - special[2]*sdy/slen + special[3]*sdx/slen

        maxpri = max(ourepri, otherepri)
        ourmult = ourepri==maxpri
        othermult = otherepri==maxpri
        multsum = ourmult + othermult
        sx = ox = (sx * ourmult + ox * othermult) / multsum
        sy = oy = (sy * ourmult + oy * othermult) / multsum

        if special:
            ox = ox + special[0] + special[2]*sdx/slen + special[3]*sdy/slen
            oy = oy + special[1] + special[2]*sdy/slen - special[3]*sdx/slen

        if not half:
            maxpri = max(ourdpri, otherdpri)
            ourmult = ourdpri==maxpri
            othermult = otherdpri==maxpri
            multsum = ourmult + othermult
            dx = (sdx * ourmult - odx * othermult) / multsum
            dy = (sdy * ourmult - ody * othermult) / multsum
            if dx == 0 and dy == 0:
                dx = 1 # arbitrary
            sdx, sdy = dx, dy
            odx, ody = -dx, -dy

        self.setenddata(end, sx, sy, sdx, sdy)
        other.setenddata(oend, ox, oy, odx, ody)

    def compute_direction(self, t):
        x0, y0 = self.compute_point(t-0.0001)
        x2, y2 = self.compute_point(t+0.0001)
        return (x2-x0)/0.0002, (y2-y0)/0.0002

    def compute_theta(self, t):
        dx, dy = self.compute_direction(t)
        return atan2(-dy, dx)

    def compute_nib(self, t):
        x, y = self.compute_point(t)
        theta = self.compute_theta(t)
        nibfn = self.nib
        if nibfn == None:
            nibfn = self.cont.default_nib
        if type(nibfn) == types.FunctionType:
            return nibfn(self, x, y, t, theta)
        else:
            return nibfn

    def compute_x(self, t):
        return self.compute_point(t)[0]

    def compute_y(self, t):
        return self.compute_point(t)[1]

def squash(x, y, mx):
    return mx[0]*x + mx[1]*y, mx[2]*x + mx[3]*y
def unsquash(x, y, mx):
    det = mx[0]*mx[3] - mx[1]*mx[2]
    return (mx[3]*x - mx[1]*y) / det, (-mx[2]*x + mx[0]*y) / det

class CircleInvolute(Curve):
    def __init__(self, cont, x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx=None):
        Curve.__init__(self)
        self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)
        self.set_params()
        Curve.postinit(self, cont)

    def set_params(self):
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        try:
            # Normalise the direction vectors.
            dlen1 = sqrt(dx1**2 + dy1**2); dx1, dy1 = dx1/dlen1, dy1/dlen1
            dlen2 = sqrt(dx2**2 + dy2**2); dx2, dy2 = dx2/dlen2, dy2/dlen2

            self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)

            # Transform into the squashed coordinate system.
            if mx != None:
                x1, y1 = squash(x1, y1, mx)
                dx1, dy1 = squash(dx1, dy1, mx)
                x2, y2 = squash(x2, y2, mx)
                dx2, dy2 = squash(dx2, dy2, mx)
                # And renormalise.
                dlen1 = sqrt(dx1**2 + dy1**2); dx1, dy1 = dx1/dlen1, dy1/dlen1
                dlen2 = sqrt(dx2**2 + dy2**2); dx2, dy2 = dx2/dlen2, dy2/dlen2

            # Find the normal vectors at each end by rotating the
            # direction vectors.
            nx1, ny1 = dy1, -dx1
            nx2, ny2 = dy2, -dx2

            # Find the crossing point of the normals.
            cx, cy = crosspoint(x1, y1, x1+nx1, y1+ny1, x2, y2, x2+nx2, y2+ny2)

            # Measure the distance from that crossing point to each
            # endpoint, and find the difference.
            #
            # The distance is obtained by taking the dot product with
            # the line's defining vector, so that it's signed.
            d1 = (cx-x1) * nx1 + (cy-y1) * ny1
            d2 = (cx-x2) * nx2 + (cy-y2) * ny2

            dd = d2 - d1

            # Find the angle between the two direction vectors. Since
            # they're already normalised to unit length, the magnitude
            # of this is just the inverse cosine of their dot product.
            # The sign must be chosen to reflect which way round they
            # are.
            dp = dx1*dx2 + dy1*dy2
            if abs(dp) > 1: dp /= abs(dp) # avoid EDOM from rounding error
            theta = -acos(dp)
            if dx1*dy2 - dx2*dy1 > 0:
                theta = -theta

            # So we need a circular arc rotating through angle theta,
            # such that taking the involute of that arc with the right
            # length does the right thing.
            #
            # Suppose the circle has radius r. Then, when the circle
            # touches the line going to point 1, we need our string to
            # have length equal to d1 - r tan(theta/2). When it touches
            # the line going to point 2, the string length needs to be
            # d2 + r tan(theta/2). The difference between these numbers
            # must be equal to the arc length of the portion of the
            # circle in between, which is r*theta. Setting these equal
            # gives d2-d1 = r theta - 2 r tan(theta/2), which we solve
            # to get r = (d2-d1) / (theta - 2 tan (theta/2)).
            #
            # (In fact, we then flip the sign to take account of the way
            # we subsequently use r.)
            r = dd / (-theta + 2*tan(theta/2))

            # So how do we find the centre of a circle of radius r
            # tangent to both those lines? We shift the start point of
            # each line by r in the appropriate direction, and find
            # their crossing point again.
            cx2, cy2 = crosspoint(x1-r*dx1, y1-r*dy1, x1-r*dx1+nx1, y1-r*dy1+ny1, \
            x2-r*dx2, y2-r*dy2, x2-r*dx2+nx2, y2-r*dy2+ny2)

            # Now find the distance along each line to the centre of the
            # circle, which will be the string lengths at the endpoints.
            s1 = (cx2-x1) * nx1 + (cy2-y1) * ny1
            s2 = (cx2-x2) * nx2 + (cy2-y2) * ny2

            # Determine the starting angle.
            phi = atan2(dy1, dx1)

            # And that's it. We're involving a circle of radius r
            # centred at cx2,cy2; the centre of curvature proceeds from
            # angle phi to phi+theta, and the actual point on the curve
            # is displaced from that centre by an amount which changes
            # linearly with angle from s1 to s2. Store all that.
            self.params = (r, cx2, cy2, phi, theta, s1, s2-s1, mx)
        except ZeroDivisionError as e:
            self.params = None # it went pear-shaped
        except TypeError as e:
            self.params = None # it went pear-shaped

    def transform(self, matrix, full):
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        x1, y1 = transform(matrix, x1, y1)
        x2, y2 = transform(matrix, x2, y2)
        dx1, dy1 = transform(matrix, dx1, dy1, 0)
        dx2, dy2 = transform(matrix, dx2, dy2, 0)
        # We only transform mx optionally. (I think it's better in
        # many cases to leave it unchanged, so that transforming the
        # overall dimensions of a glyph alters its shape subtly so
        # as to leave the curve quality similar.)
        if full:
            if mx == None:
                a, b, c, d = 1, 0, 0, 1
            else:
                a, b, c, d = mx
            det = matrix[0]*matrix[3] - matrix[1]*matrix[2]
            invmatrix = [matrix[3]/det, -matrix[1]/det, \
            -matrix[2]/det, matrix[0]/det, 0, 0]
            a, c = transform(invmatrix, a, c, 0)
            b, d = transform(invmatrix, b, d, 0)
            mx = a, b, c, d
        self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)
        self.set_params()

    def compute_point(self, t): # t in [0,1]
        assert self.params != None
        (r, cx, cy, phi, theta, s1, ds, mx) = self.params

        angle = phi + theta * t
        s = s1 + ds * t
        dx = cos(angle)
        dy = sin(angle)
        nx, ny = -dy, dx
        x = cx + dx * r + nx * s
        y = cy + dy * r + ny * s
        if mx != None:
            return unsquash(x, y, mx)
        else:
            return x, y

    def compute_direction(self, t): # t in [0,1]
        assert self.params != None
        (r, cx, cy, phi, theta, s1, ds, mx) = self.params

        angle = phi + theta * t
        s = s1 + ds * t
        dx = cos(angle)
        dy = sin(angle)
        nx, ny = -dy, dx
        ddx = -sin(angle) * theta
        ddy = cos(angle) * theta
        dnx, dny = -ddy, ddx
        x = ddx * r + dnx * s + nx * ds
        y = ddy * r + dny * s + ny * ds
        if mx != None:
            return unsquash(x, y, mx)
        else:
            return x, y

    def tk_refresh(self):
        coords = []
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        if self.params == None:
            coords.extend([x1,y1])
            coords.extend([(2*x1+3*x2)/5 - (y1-y2)/14, (2*y1+3*y2)/5 + (x1-x2)/14])
            coords.extend([(3*x1+2*x2)/5 + (y1-y2)/14, (3*y1+2*y2)/5 - (x1-x2)/14])
            coords.extend([x2,y2])
        else:
            (r, cx, cy, phi, theta, s1, ds, mx) = self.params
            dt = min(2 / abs(s1 * theta), 2 / abs((s1+ds) * theta))
            itmax = min(10000, int(1 / dt + 1))
            for it in range(itmax+1):
                t = it / float(itmax)
                x, y = self.compute_point(t)
                coords.extend([x,y])
        for x in self.tkitems:
            self.canvas.delete(x)
        if self.params == None:
            fill = "red"
        else:
            dx1a, dy1a = self.compute_direction(0)
            dx2a, dy2a = self.compute_direction(1)
            if dx1a * dx1 + dy1a * dy1 < 0 or dx2a * dx2 + dy2a * dy2 < 0:
                fill = "#ff0000"
            else:
                fill = "#00c000"
        self.tkitems.append(self.canvas.create_line(x1, y1, x1+25*dx1, y1+25*dy1, fill=fill))
        self.tkitems.append(self.canvas.create_line(x2, y2, x2-25*dx2, y2-25*dy2, fill=fill))
        self.tkitems.append(self.canvas.create_line(coords))

    def tk_addto(self, canvas):
        self.canvas = canvas
        self.tk_refresh()

    def tk_drag(self, x, y, etype):
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        if etype == 1:
            xc1, yc1 = x1+25*dx1, y1+25*dy1
            xc2, yc2 = x2-25*dx2, y2-25*dy2
            if (x-x1)**2 + (y-y1)**2 < 32:
                self.dragpt = 1
            elif (x-x2)**2 + (y-y2)**2 < 32:
                self.dragpt = 4
            elif (x-xc1)**2 + (y-yc1)**2 < 32:
                self.dragpt = 2
            elif (x-xc2)**2 + (y-yc2)**2 < 32:
                self.dragpt = 3
            else:
                self.dragpt = None
                return 0
            return 1
        elif self.dragpt == None:
            return 0
        else:
            if self.dragpt == 1:
                x1 = x
                y1 = y
            elif self.dragpt == 4:
                x2 = x
                y2 = y
            elif self.dragpt == 2 and (x != x1 or y != y1):
                dx1 = x - x1
                dy1 = y - y1
            elif self.dragpt == 3 and (x != x1 or y != y1):
                dx2 = x2 - x
                dy2 = y2 - y
            self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)
            self.set_params()
            self.tk_refresh()
            end = (self.dragpt-1)//2
            self.weld_update(end, 2)
            return 1

    def enddata(self, end):
        x, y, dx, dy = self.inparams[4*end:4*(end+1)]
        if end:
            dx, dy = -dx, -dy
        return x, y, dx, dy

    def setenddata(self, end, x, y, dx, dy):
        ox, oy, odx, ody = self.inparams[4*end:4*(end+1)]
        if end:
            odx, ody = -odx, -ody
        changed = (x != ox or y != oy or dx * ody != dy * odx)
        if changed:
            if end:
                dx, dy = -dx, -dy
            p = list(self.inparams)
            p[4*end] = x
            p[4*end+1] = y
            p[4*end+2] = dx
            p[4*end+3] = dy
            self.inparams = tuple(p)
            self.set_params()
            self.tk_refresh()

    def findend(self, x, y):
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        if (x-x1)**2 + (y-y1)**2 < 32:
            return 0
        elif (x-x2)**2 + (y-y2)**2 < 32:
            return 1
        else:
            return None

    def serialise(self):
        extra = ""
        mx = self.inparams[8]
        if mx != None:
            extra = extra + ", mx=(%g, %g, %g, %g)" % mx
        s = "CircleInvolute(cont, %g, %g, %g, %g, %g, %g, %g, %g%s)" % \
        (self.inparams[:8] + (extra,))
        return s

class ExponentialInvolute(Curve):
    # Involute of an exponential curve. This curve form has zero
    # curvature at one end, where the thread length goes to
    # infinity, so it will hopefully be a good choice for joining on
    # to straight lines without a jolt.
    #
    # I pick an exponential because it's a curve asymptotic to the
    # x-axis with arc length we can find exactly. Arc length for a
    # curve y=f(x) is given by integrating sqrt(1+f'(x)^2) with
    # respect to x, so we'll start by doing that for f(x) =
    # exp(-bx). So we want to evaluate the indefinite integral
    #
    #        I = int sqrt(1 + b^2 exp(-2bx)) dx
    #
    # Substitution: let u be the entire integrand. So we have
    #
    #        u = sqrt(1 + b^2 exp(-2bx))
    # => du/dx = 1/(2 sqrt(1 + b^2 exp(-2bx))) . -2 b^3 exp(-2bx)
    #          = 1/(2u) . -2b(u^2-1)
    # =>    dx = { -u / b(u^2-1) } du
    #
    # and then we can substitute into I to get
    #
    #        I = int u dx
    #          = int { -u^2 / b(u^2 - 1) } du
    #          = -1/b int { u^2 / (u^2 - 1) } du 
    #          = -1/b int { 1 - 1 / (1 - u^2) } du
    #          = -1/b int { 1 - 1/2 / (1-u) - 1/2 / (1+u) } du
    #          = -1/b ( u + 1/2 log |1-u| - 1/2 log |1+u| ) + c
    #          = -u/b + 1/(2b) log |(1+u)/(1-u)|
    #          = -1/b (u + modatanh u)
    #
    # where modatanh(u) is the function defined by atanh(u) or
    # atanh(-1/u), whichever of those is valid. (In other words,
    # atanh(u) if |u| < 1, or atanh(-1/u) if |u| > 1.) In this case,
    # recall that u is the square root of 1 plus something positive,
    # so we always have |u| > 1, and thus
    #
    #        I = -1/b (u + atanh(-1/u))
    #
    # where u, of course, is sqrt(1 + b^2 exp(-2bx)) as defined
    # above.
    #
    # Another thing we need to know is the amount by which the whole
    # arc length of this curve (from a given point, say 0, onwards)
    # exceeds the distance to the x-axis. In other words, we need
    # the limit of I(x) - I(0) - x as x tends to infinity. I'll call
    # this the 'excess'.
    #
    # I was unable to find an analytic solution to that problem in
    # the general case with b as a parameter, but using a computer
    # algebra system to calculate some specific values and examining
    # those suggested empirically that the answer is
    #
    #   1/b (sqrt(b^2+1) - arctanh(1/sqrt(b^2+1)) - log(b/2) - 1)
    #
    # This curve type is currently very scrappily implemented: error
    # checking is nonexistent (really, set_params ought to have a
    # great big 'try' around it), plotting is very slow and lots of
    # things that could be cached are not, curve subdivision is
    # ad-hoc, there's a hideous hack to avoid loss of significance
    # causing wobbles at the flat end, and even the above piece of
    # analysis lacks a proof. However, it works well enough for the
    # one Gonville glyph (clefG) in which I've so far used it.

    def __init__(self, cont, x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx=None):
        Curve.__init__(self)
        self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)
        self.set_params()
        Curve.postinit(self, cont)

    def set_params(self):
        # x1,y1 is the end at which the thread becomes infinitely
        # long, so that the curvature of the involute is zero.
        #
        # x2,y2 is the other end.
        #
        # We begin by mentally transforming so that x2,y2 is at
        # (0,1), and x1,y1 lies on the x-axis. We know the starting
        # slope b we want for the exponential (it has to start off
        # tangent to the normal vector to x2,y2), so that leaves us
        # one degree of freedom, which is the involute's radius of
        # curvature (the starting thread length) at x2,y2. At one
        # extreme, the exponential itself starts off at (0,1) and so
        # the radius of curvature at that point is zero; at the
        # other extreme our exponential squashes itself entirely on
        # to the x-axis and we just have a circular arc.
        #
        # In general, let us suppose our exponential starts at some
        # y-coordinate k. That must put its starting x-coordinate at
        # (1-k)/b. Then the x-coordinate of the terminating point of
        # the involute on the x-axis must be equal to:
        #  - that starting position (1-k)/b
        #  - minus k times the excess of the curve as given above
        #  - minus the starting thread length, (1-k)*sqrt(1+1/b^2).
        #
        # The excess and sqrt(1+1/b^2) are complicated to work out,
        # but fortunately they don't depend on k, so we end up with
        # a simple linear equation to solve for our remaining
        # parameter.

        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams

        # Normalise the direction vectors.
        dlen1 = sqrt(dx1**2 + dy1**2); dx1, dy1 = dx1/dlen1, dy1/dlen1
        dlen2 = sqrt(dx2**2 + dy2**2); dx2, dy2 = dx2/dlen2, dy2/dlen2

        self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)

        # Decide on our notional canonical coordinate system. We
        # expect to end up with the straight end to the left of and
        # below the curvy end, so flip the coordinate senses round
        # as appropriate.
        ydx, ydy = dx1, dy1 # y-axis is along the curve direction at x1,y1
        xdx, xdy = -ydy, ydx # x-axis is perpendicular to that
        if (x2-x1)*xdx + (y2-y1)*xdy < 0:
            xdx, xdy = -xdx, -xdy # flip x-axis
        if (x2-x1)*ydx + (y2-y1)*ydy < 0:
            ydx, ydy = -ydx, -ydy # flip y-axis
        unit = (x2-x1)*ydx + (y2-y1)*ydy
        ox, oy = x2 - unit*ydx, y2 - unit*ydy

        # Determine the initial gradient b of the exponential.
        b = abs((dy2 * ydx - dx2 * ydy) / (dy2 * xdx - dx2 * xdy))

        # Compute the basic (unscaled) excess of the curve, and the
        # ratio between y-extent and length for lines slanting at b.
        excess = (sqrt(b*b+1) - atanh(1/sqrt(b*b+1)) - log(b/2.) - 1) / b
        slantratio = sqrt(1 + 1/(b*b))

        # Find the x-position of the flat end, in our coordinate
        # system.
        cx1 = ((x1-ox)*xdx + (y1-oy)*xdy) / unit

        # Solve for k. The equation is:
        #    (1-k)/b - k*excess - (1-k)*slantratio = cx1
        # => 1/b - slantratio - cx1 = k/b + k*excess - k*slantratio
        k = (1./b - slantratio - cx1) / (1./b - slantratio + excess)

        # Use k to work out the actual starting radius of curvature.
        r = (1-k) * slantratio

        # That should be all the parameters we need. Save them.
        self.params = (b, k, r, ox, oy, xdx, xdy, ydx, ydy, unit, cx1)

    def compute_point(self, t): # t in [0,1]
        assert self.params != None
        (b, k, r, ox, oy, xdx, xdy, ydx, ydy, unit, cx1) = self.params

        if t == 0:
            # The usual formula hits a singularity here, so we just
            # drop in the known value.
            x, y = cx1, 0
        else:
            # Find the position on the exponential curve itself, and
            # the current direction of that curve.
            t = (1-t) * 15
            x = (1-k)/b + k*t
            y = k*exp(-b*t)
            dx = 1
            dy = -b*exp(-b*t)
            dlen = sqrt(dx*dx + dy*dy)
            dx, dy = -dx/dlen, -dy/dlen
            # Find the current radius of curvature.
            u = sqrt(1 + b*b*exp(-2*b*t))
            u0 = sqrt(1 + b*b)
            if u < 1.000001:
                x, y = cx1, 0
            else:
                arc = ((u + atanh(-1/u)) - (u0 + atanh(-1/u0))) / -b
                r = r + k*arc
                # Augment x and y appropriately.
                x = x + dx * r
                y = y + dy * r

        # Now we have our position in logical coordinates, just
        # convert back to reality.
        return ox + xdx*unit*x + ydx*unit*y, oy + xdy*unit*x + ydy*unit*y

    def compute_direction(self, t): # t in [0,1]
        assert self.params != None
        (b, k, r, ox, oy, xdx, xdy, ydx, ydy, unit, cx1) = self.params

        if t == 0:
            dx, dy = 0, 1
        else:
            # We're computing the direction of the exponential curve,
            # same as above. But the direction of the involute is at
            # right angles to that, because in an involute, the
            # endpoint of the imaginary unreeling thread is always
            # moving at right angles to the thread direction itself.
            t = (1-t) * 15
            dx = b*exp(-b*t)
            dy = 1

        dx, dy = xdx*dx + ydx*dy, xdy*dx + ydy*dy
        dlen = sqrt(dx*dx + dy*dy)
        return dx/dlen, dy/dlen

    def tk_refresh(self):
        coords = []
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        if self.params == None:
            coords.extend([x1,y1])
            coords.extend([(2*x1+3*x2)/5 - (y1-y2)/14, (2*y1+3*y2)/5 + (x1-x2)/14])
            coords.extend([(3*x1+2*x2)/5 + (y1-y2)/14, (3*y1+2*y2)/5 - (x1-x2)/14])
            coords.extend([x2,y2])
        else:
            dt = 0.0001 # FIXME: should think up a better way of deciding how many points to use
            itmax = min(10000, int(1 / dt + 1))
            for it in range(itmax+1):
                t = it / float(itmax)
                x, y = self.compute_point(t)
                coords.extend([x,y])
        for x in self.tkitems:
            self.canvas.delete(x)
        if self.params == None:
            fill = "red"
        else:
            (b, k, r, ox, oy, xdx, xdy, ydx, ydy, unit, cx1) = self.params
            if k < 0 or k > 1:
                fill = "#ff0000"
            else:
                fill = "#00c000"
        self.tkitems.append(self.canvas.create_line(x1, y1, x1+25*dx1, y1+25*dy1, fill=fill))
        self.tkitems.append(self.canvas.create_line(x2, y2, x2-25*dx2, y2-25*dy2, fill=fill))
        self.tkitems.append(self.canvas.create_line(coords))

    def tk_addto(self, canvas):
        self.canvas = canvas
        self.tk_refresh()

    def tk_drag(self, x, y, etype):
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        if etype == 1:
            xc1, yc1 = x1+25*dx1, y1+25*dy1
            xc2, yc2 = x2-25*dx2, y2-25*dy2
            if (x-x1)**2 + (y-y1)**2 < 32:
                self.dragpt = 1
            elif (x-x2)**2 + (y-y2)**2 < 32:
                self.dragpt = 4
            elif (x-xc1)**2 + (y-yc1)**2 < 32:
                self.dragpt = 2
            elif (x-xc2)**2 + (y-yc2)**2 < 32:
                self.dragpt = 3
            else:
                self.dragpt = None
                return 0
            return 1
        elif self.dragpt == None:
            return 0
        else:
            if self.dragpt == 1:
                x1 = x
                y1 = y
            elif self.dragpt == 4:
                x2 = x
                y2 = y
            elif self.dragpt == 2 and (x != x1 or y != y1):
                dx1 = x - x1
                dy1 = y - y1
            elif self.dragpt == 3 and (x != x1 or y != y1):
                dx2 = x2 - x
                dy2 = y2 - y
            self.inparams = (x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx)
            self.set_params()
            self.tk_refresh()
            end = (self.dragpt-1) // 2
            self.weld_update(end, 2)
            return 1

    def enddata(self, end):
        x, y, dx, dy = self.inparams[4*end:4*(end+1)]
        if end:
            dx, dy = -dx, -dy
        return x, y, dx, dy

    def setenddata(self, end, x, y, dx, dy):
        ox, oy, odx, ody = self.inparams[4*end:4*(end+1)]
        if end:
            odx, ody = -odx, -ody
        changed = (x != ox or y != oy or dx * ody != dy * odx)
        if changed:
            if end:
                dx, dy = -dx, -dy
            p = list(self.inparams)
            p[4*end] = x
            p[4*end+1] = y
            p[4*end+2] = dx
            p[4*end+3] = dy
            self.inparams = tuple(p)
            self.set_params()
            self.tk_refresh()

    def findend(self, x, y):
        x1, y1, dx1, dy1, x2, y2, dx2, dy2, mx = self.inparams
        if (x-x1)**2 + (y-y1)**2 < 32:
            return 0
        elif (x-x2)**2 + (y-y2)**2 < 32:
            return 1
        else:
            return None

    def serialise(self):
        extra = ""
        mx = self.inparams[8]
        if mx != None:
            extra = extra + ", mx=(%g, %g, %g, %g)" % mx
        s = "ExponentialInvolute(cont, %g, %g, %g, %g, %g, %g, %g, %g%s)" % \
        (self.inparams[:8] + (extra,))
        return s

class StraightLine(Curve):
    def __init__(self, cont, x1, y1, x2, y2):
        Curve.__init__(self)
        self.inparams = (x1, y1, x2, y2)
        self.weldpri = 3
        Curve.postinit(self, cont)

    def transform(self, matrix, full):
        x1, y1, x2, y2 = self.inparams
        x1, y1 = transform(matrix, x1, y1)
        x2, y2 = transform(matrix, x2, y2)
        self.inparams = (x1, y1, x2, y2)

    def compute_point(self, t): # t in [0,1]
        assert self.inparams != None
        (x1, y1, x2, y2) = self.inparams
        x = x1 + t * (x2-x1)
        y = y1 + t * (y2-y1)
        return x, y

    def tk_refresh(self):
        x1, y1, x2, y2 = self.inparams
        for x in self.tkitems:
            self.canvas.delete(x)
        self.tkitems.append(self.canvas.create_line(x1, y1, x2, y2))

    def tk_addto(self, canvas):
        self.canvas = canvas
        self.tk_refresh()

    def tk_drag(self, x, y, etype):
        x1, y1, x2, y2 = self.inparams
        if etype == 1:
            if (x-x1)**2 + (y-y1)**2 < 32:
                self.dragpt = 1
            elif (x-x2)**2 + (y-y2)**2 < 32:
                self.dragpt = 2
            else:
                self.dragpt = None
                return 0
            return 1
        elif self.dragpt == None:
            return 0
        else:
            if self.dragpt == 1:
                x1 = x
                y1 = y
            elif self.dragpt == 2:
                x2 = x
                y2 = y
            self.inparams = (x1, y1, x2, y2)
            self.tk_refresh()
            for end in 0,1:
                self.weld_update(end)
            return 1

    def enddata(self, end):
        x1, y1, x2, y2 = self.inparams
        if end:
            x1, y1, x2, y2 = x2, y2, x1, y1
        return x1, y1, x2-x1, y2-y1

    def setenddata(self, end, x, y, dx, dy):
        ox, oy = self.inparams[2*end:2*(end+1)]
        changed = (x != ox or y != oy)
        if changed:
            p = list(self.inparams)
            p[2*end] = x
            p[2*end+1] = y
            self.inparams = tuple(p)
            self.tk_refresh()

    def findend(self, x, y):
        x1, y1, x2, y2 = self.inparams
        if (x-x1)**2 + (y-y1)**2 < 32:
            return 0
        elif (x-x2)**2 + (y-y2)**2 < 32:
            return 1
        else:
            return None

    def serialise(self):
        s = "StraightLine(cont, %g, %g, %g, %g)" % self.inparams
        return s
    
class Bezier(Curve):
    def __init__(self, cont, x1, y1, x2, y2, x3, y3, x4, y4):
        Curve.__init__(self)
        self.inparams = (x1, y1, x2, y2, x3, y3, x4, y4)
        self.weldpri = 1
        Curve.postinit(self, cont)

    def transform(self, matrix, full):
        (x1, y1, x2, y2, x3, y3, x4, y4) = self.inparams
        x1, y1 = transform(matrix, x1, y1)
        x2, y2 = transform(matrix, x2, y2)
        x3, y3 = transform(matrix, x3, y3)
        x4, y4 = transform(matrix, x4, y4)
        self.inparams = (x1, y1, x2, y2, x3, y3, x4, y4)

    def compute_point(self, t): # t in [0,1]
        assert self.inparams != None
        (x1, y1, x2, y2, x3, y3, x4, y4) = self.inparams
        x = x1 * (1-t)**3 + x2 * 3*(1-t)**2*t + x3 * 3*(1-t)*t**2 + x4 * t**3
        y = y1 * (1-t)**3 + y2 * 3*(1-t)**2*t + y3 * 3*(1-t)*t**2 + y4 * t**3
        return x, y

    def tk_refresh(self):
        coords = []
        (x1, y1, x2, y2, x3, y3, x4, y4) = self.inparams
        maxcdiff = max(abs(x1-x2),abs(x2-x3),abs(x3-x4),abs(x1-x3),abs(x2-x4),abs(x1-x4),abs(y1-y2),abs(y2-y3),abs(y3-y4),abs(y1-y3),abs(y2-y4),abs(y1-y4))
        itmax = min(int(maxcdiff*3+1), 1000)
        for it in range(itmax+1):
            t = it / float(itmax)
            x, y = self.compute_point(t)
            coords.extend([x,y])
        for x in self.tkitems:
            self.canvas.delete(x)
        fill = "#00c000"
        self.tkitems.append(self.canvas.create_line(x1, y1, x2, y2, fill=fill))
        self.tkitems.append(self.canvas.create_line(x3, y3, x4, y4, fill=fill))
        self.tkitems.append(self.canvas.create_line(coords))

    def tk_addto(self, canvas):
        self.canvas = canvas
        self.tk_refresh()

    def tk_drag(self, x, y, etype):
        (x1, y1, x2, y2, x3, y3, x4, y4) = self.inparams
        if etype == 1:
            if (x-x1)**2 + (y-y1)**2 < 32:
                self.dragpt = 1
            elif (x-x2)**2 + (y-y2)**2 < 32:
                self.dragpt = 2
            elif (x-x3)**2 + (y-y3)**2 < 32:
                self.dragpt = 3
            elif (x-x4)**2 + (y-y4)**2 < 32:
                self.dragpt = 4
            else:
                self.dragpt = None
                return 0
            return 1
        elif self.dragpt == None:
            return 0
        else:
            if self.dragpt == 1:
                x2 = x2 - x1 + x
                y2 = y2 - y1 + y
                x1 = x
                y1 = y
            elif self.dragpt == 2:
                x2 = x
                y2 = y
            elif self.dragpt == 3:
                x3 = x
                y3 = y
            elif self.dragpt == 4:
                x3 = x3 - x4 + x
                y3 = y3 - y4 + y
                x4 = x
                y4 = y
            self.inparams = (x1, y1, x2, y2, x3, y3, x4, y4)
            self.tk_refresh()
            for end in 0,1:
                self.weld_update(end)
            return 1

    def enddata(self, end):
        x1, y1, x2, y2, x3, y3, x4, y4 = self.inparams
        if end:
            x1, y1, x2, y2 = x4, y4, x3, y3
        return x1, y1, x2-x1, y2-y1

    def setenddata(self, end, x, y, dx, dy):
        dlen = sqrt(dx*dx + dy*dy)
        dx, dy = dx/dlen, dy/dlen

        ox, oy = self.inparams[6*end:6*end+2]
        oxc, oyc = self.inparams[2*end+2:2*end+4]
        odx, ody = oxc-ox, oyc-oy
        odlen = sqrt(odx*odx + ody*ody)
        odx, ody = odx/odlen, ody/odlen
        changed = (x != ox or y != oy or dx != odx or dy != ody)
        if changed:
            p = list(self.inparams)
            p[6*end] = x
            p[6*end+1] = y
            p[2*end+2] = x + dx * odlen
            p[2*end+3] = y + dy * odlen
            self.inparams = tuple(p)
            self.tk_refresh()

    def findend(self, x, y):
        x1, y1, x2, y2, x3, y3, x4, y4 = self.inparams
        if (x-x1)**2 + (y-y1)**2 < 32:
            return 0
        elif (x-x4)**2 + (y-y4)**2 < 32:
            return 1
        else:
            return None

    def serialise(self):
        s = "Bezier(cont, %g, %g, %g, %g, %g, %g, %g, %g)" % self.inparams
        return s
