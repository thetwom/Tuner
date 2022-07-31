# Python code to find the crossing point of two lines.

# This function is optimised for big-integer or FP arithmetic: it
# multiplies up to find two big numbers, and then divides them. So
# if you use it on integers it will give the most accurate answer
# it possibly can within integers, but might overflow if you don't
# use longs. I haven't carefully analysed the FP properties, but I
# can't see it going _too_ far wrong.
#
# Of course there's no reason you can't feed it rationals if you
# happen to have a Rational class. It only does adds, subtracts,
# multiplies, divides and tests of equality on its arguments, so
# any data type supporting those would be fine.

def crosspoint(xa1,ya1,xa2,ya2,xb1,yb1,xb2,yb2):
    "Give the intersection point of the (possibly extrapolated) lines\n"\
    "segments (xa1,ya1)-(xa2,ya2) and (xb1,yb1)-(xb2,yb2)."
    dxa = xa2-xa1
    dya = ya2-ya1
    dxb = xb2-xb1
    dyb = yb2-yb1
    # Special case: if gradients are equal, die.
    if dya * dxb == dxa * dyb:
        return None
    # Second special case: if either gradient is horizontal or
    # vertical.
    if dxa == 0:
        # Because we've already dealt with the parallel case, dxb
        # is now known to be nonzero. So we can simply extrapolate
        # along the b line until it hits the common value xa1==xa2.
        return (xa1, (xa1 - xb1) * dyb / dxb + yb1)
    # Similar cases for dya == 0, dxb == 0 and dyb == 0.
    if dxb == 0:
        return (xb1, (xb1 - xa1) * dya / dxa + ya1)
    if dya == 0:
        return ((ya1 - yb1) * dxb / dyb + xb1, ya1)
    if dyb == 0:
        return ((yb1 - ya1) * dxa / dya + xa1, yb1)

    # General case: all four gradient components are nonzero. In
    # this case, we have
    #
    #     y - ya1   dya           y - yb1   dyb
    #     ------- = ---    and    ------- = ---
    #     x - xa1   dxa           x - xb1   dxb
    #
    # We rewrite these equations as
    #
    #     y = ya1 + dya (x - xa1) / dxa
    #     y = yb1 + dyb (x - xb1) / dxb
    #
    # and equate the RHSes of each
    #
    #     ya1 + dya (x - xa1) / dxa = yb1 + dyb (x - xb1) / dxb
    #  => ya1 dxa dxb + dya dxb (x - xa1) = yb1 dxb dxa + dyb dxa (x - xb1)
    #  => (dya dxb - dyb dxa) x =
    #          dxb dxa (yb1 - ya1) + dya dxb xa1 - dyb dxa xb1
    #
    # So we have a formula for x
    #
    #         dxb dxa (yb1 - ya1) + dya dxb xa1 - dyb dxa xb1
    #     x = -----------------------------------------------
    #                        dya dxb - dyb dxa
    #
    # and by a similar derivation we also obtain a formula for y
    #
    #         dya dyb (xa1 - xb1) + dxb dya yb1 - dxa dyb ya1
    #     y = -----------------------------------------------
    #                        dya dxb - dyb dxa

    det = dya * dxb - dyb * dxa
    xtop = dxb * dxa * (yb1-ya1) + dya * dxb * xa1 - dyb * dxa * xb1
    ytop = dya * dyb * (xa1-xb1) + dxb * dya * yb1 - dxa * dyb * ya1

    return (xtop / det, ytop / det)
