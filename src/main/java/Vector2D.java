
public class Vector2D {
	public static final double DEFAULT_EPSILON = 1.0E-6D;

	private double _x;
	private double _y;
	
    public Vector2D(Point2D point) {
        _x = point.getX();
        _y = point.getY();
    }

    public Vector2D(double x1, double y1, double x2, double y2) {
    	this(x2 - x1, y2 - y1);
    }

    public Vector2D(Point2D point1, Point2D point2) {
        this(point2.getX() - point1.getX(), point2.getY() - point1.getY());
    }

    public Vector2D(Vector2D vector) {
    	this(vector.getX(), vector.getY());
    }
    
    public Vector2D(double length, double angle) {
        setX(StrictMath.cos(angle) * length);
        setY(StrictMath.sin(angle) * length);
    }

    public double getX() {
        return _x;
    }

    public void setX(double x) {
        _x = x;
    }

    public double getY() {
        return _y;
    }

    public void setY(double y) {
        _y = y;
    }

    public Vector2D add(Vector2D vector) {
        setX(getX() + vector.getX());
        setY(getY() + vector.getY());
        return this;
    }

    public Vector2D add(double x, double y) {
        setX(getX() + x);
        setY(getY() + y);
        return this;
    }

    public Vector2D subtract(Vector2D vector) {
        setX(getX() - vector.getX());
        setY(getY() - vector.getY());
        return this;
    }

    public Vector2D subtract(double x, double y) {
        setX(getX() - x);
        setY(getY() - y);
        return this;
    }

    public Vector2D multiply(double factor) {
        setX(factor * getX());
        setY(factor * getY());
        return this;
    }

    public Vector2D rotate(double angle) {
        double cos = StrictMath.cos(angle);
        double sin = StrictMath.sin(angle);

        double x = getX();
        double y = getY();

        setX(x * cos - y * sin);
        setY(x * sin + y * cos);

        return this;
    }

    public Vector2D negate() {
        setX(-getX());
        setY(-getY());
        return this;
    }

    public Vector2D normalize() {
        double length = getLength();
        if (length == 0.0D) {
            throw new IllegalStateException("Can't set angle of zero-width vector.");
        }
        setX(getX() / length);
        setY(getY() / length);
        return this;
    }

    public double getAngle() {
        return StrictMath.atan2(getY(), getX());
    }

    public Vector2D setAngle(double angle) {
        double length = getLength();
        if (length == 0.0D) {
            throw new IllegalStateException("Can't set angle of zero-width vector.");
        }
        setX(StrictMath.cos(angle) * length);
        setY(StrictMath.sin(angle) * length);
        return this;
    }

    public double getAngle(Vector2D vector) {
        return StrictMath.acos((getX()*vector.getX()+getY()*vector.getY())/(getLength()*vector.getLength()));
    }

    public double getLength() {
        return StrictMath.hypot(getX(), getY());
    }

    public Vector2D setLength(double length) {
        double currentLength = getLength();
        if (currentLength == 0.0D) {
            throw new IllegalStateException("Can't resize zero-width vector.");
        }
        return multiply(length / currentLength);
    }

    public double getSquaredLength() {
        return getX() * getX() + getY() * getY();
    }

    public Vector2D setSquaredLength(double squaredLength) {
        double currentSquaredLength = getSquaredLength();
        if (currentSquaredLength == 0.0D) {
            throw new IllegalStateException("Can't resize zero-width vector.");
        }
        return multiply(StrictMath.sqrt(squaredLength / currentSquaredLength));
    }

    public Vector2D copy() {
        return new Vector2D(this);
    }

    public Vector2D copyNegate() {
        return new Vector2D(-getX(), -getY());
    }

    public boolean nearlyEquals(Vector2D vector, double epsilon) {
        return vector != null
                && MyMath.nearlyEquals(getX(), vector.getX(), epsilon)
                && MyMath.nearlyEquals(getY(), vector.getY(), epsilon);
    }

    public boolean nearlyEquals(Vector2D vector) {
        return nearlyEquals(vector, DEFAULT_EPSILON);
    }

    public boolean nearlyEquals(double x, double y, double epsilon) {
        return MyMath.nearlyEquals(getX(), x, epsilon)
                && MyMath.nearlyEquals(getY(), y, epsilon);
    }

    public boolean nearlyEquals(double x, double y) {
        return nearlyEquals(x, y, DEFAULT_EPSILON);
    }
    
    public double dotProduct(Vector2D vector) {
        return linearCombination(getX(), vector.getX(), getY(), vector.getY());
    }

    @Override
    public String toString() {
        return _x+";"+_y;
    }
    /**
     * Compute a linear combination accurately.
     * <p>
     * This method computes a<sub>1</sub>&times;b<sub>1</sub> +
     * a<sub>2</sub>&times;b<sub>2</sub> to high accuracy. It does
     * so by using specific multiplication and addition algorithms to
     * preserve accuracy and reduce cancellation effects. It is based
     * on the 2005 paper <a
     * href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.2.1547">
     * Accurate Sum and Dot Product</a> by Takeshi Ogita,
     * Siegfried M. Rump, and Shin'ichi Oishi published in SIAM J. Sci. Comput.
     * </p>
     * @param a1 first factor of the first term
     * @param b1 second factor of the first term
     * @param a2 first factor of the second term
     * @param b2 second factor of the second term
     * @return a<sub>1</sub>&times;b<sub>1</sub> +
     * a<sub>2</sub>&times;b<sub>2</sub>
     * @see #linearCombination(double, double, double, double, double, double)
     * @see #linearCombination(double, double, double, double, double, double, double, double)
     */
    public static double linearCombination(final double a1, final double b1,
                                           final double a2, final double b2) {

        // the code below is split in many additions/subtractions that may
        // appear redundant. However, they should NOT be simplified, as they
        // use IEEE754 floating point arithmetic rounding properties.
        // The variable naming conventions are that xyzHigh contains the most significant
        // bits of xyz and xyzLow contains its least significant bits. So theoretically
        // xyz is the sum xyzHigh + xyzLow, but in many cases below, this sum cannot
        // be represented in only one double precision number so we preserve two numbers
        // to hold it as long as we can, combining the high and low order bits together
        // only at the end, after cancellation may have occurred on high order bits

        // split a1 and b1 as one 26 bits number and one 27 bits number
        final double a1High     = Double.longBitsToDouble(Double.doubleToRawLongBits(a1) & ((-1L) << 27));
        final double a1Low      = a1 - a1High;
        final double b1High     = Double.longBitsToDouble(Double.doubleToRawLongBits(b1) & ((-1L) << 27));
        final double b1Low      = b1 - b1High;

        // accurate multiplication a1 * b1
        final double prod1High  = a1 * b1;
        final double prod1Low   = a1Low * b1Low - (((prod1High - a1High * b1High) - a1Low * b1High) - a1High * b1Low);

        // split a2 and b2 as one 26 bits number and one 27 bits number
        final double a2High     = Double.longBitsToDouble(Double.doubleToRawLongBits(a2) & ((-1L) << 27));
        final double a2Low      = a2 - a2High;
        final double b2High     = Double.longBitsToDouble(Double.doubleToRawLongBits(b2) & ((-1L) << 27));
        final double b2Low      = b2 - b2High;

        // accurate multiplication a2 * b2
        final double prod2High  = a2 * b2;
        final double prod2Low   = a2Low * b2Low - (((prod2High - a2High * b2High) - a2Low * b2High) - a2High * b2Low);

        // accurate addition a1 * b1 + a2 * b2
        final double s12High    = prod1High + prod2High;
        final double s12Prime   = s12High - prod2High;
        final double s12Low     = (prod2High - (s12High - s12Prime)) + (prod1High - s12Prime);

        // final rounding, s12 may have suffered many cancellations, we try
        // to recover some bits from the extra words we have saved up to now
        double result = s12High + (prod1Low + prod2Low + s12Low);

        if (Double.isNaN(result)) {
            // either we have split infinite numbers or some coefficients were NaNs,
            // just rely on the naive implementation and let IEEE754 handle this
            result = a1 * b1 + a2 * b2;
        }

        return result;
    }
}
