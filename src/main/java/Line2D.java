
/**
 * ax + by + c = 0
 *
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 22.07.13
 */

public class Line2D {
    public static final double DEFAULT_EPSILON = 1.0E-6D;

    private final double a;
    private final double b;
    private final double c;

    private final double pseudoLength;

    public Line2D(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;

        this.pseudoLength = StrictMath.hypot(this.a, this.b);
    }

    public Line2D(Line2D line) {
        this.a = line.a;
        this.b = line.b;
        this.c = line.c;

        this.pseudoLength = line.pseudoLength;
    }
    
    public Line2D(double x1, double y1, double x2, double y2) {
        a = y2 - y1;
        b = x1 - x2;
        c = (y1 - y2) * x1 + (x2 - x1) * y1;

        pseudoLength = StrictMath.hypot(a, b);
    }

    public Line2D(Point2D point1, Point2D point2) {
        a = point2.getY() - point1.getY();
        b = point1.getX() - point2.getX();
        c = (point1.getY() - point2.getY()) * point1.getX() + (point2.getX() - point1.getX()) * point1.getY();

        pseudoLength = StrictMath.hypot(a, b);
    }

    public double getA() {
        return a;
    }

    public Line2D setA(double a) {
        return new Line2D(a, b, c);
    }

    public double getB() {
        return b;
    }

    public Line2D setB(double b) {
        return new Line2D(a, b, c);
    }

    public double getC() {
        return c;
    }

    public Line2D setC(double c) {
        return new Line2D(a, b, c);
    }

    public double getX(double y) {
        return a == 0.0D ? Double.NaN : -(b * y + c) / a;
    }

    public double getY(double x) {
        return b == 0.0D ? Double.NaN : -(a * x + c) / b;
    }

    public Line2D[] getParallelLines(double distance) {
        double shift = distance * pseudoLength;
        return new Line2D[]{new Line2D(a, b, c + shift), new Line2D(a, b, c - shift)};
    }

    public Line2D getParallelLine(double x, double y) {
        double shift = a * x + b * y + c;
        return new Line2D(a, b, c - shift);
    }

    public Line2D getParallelLine(Point2D point) {
        return getParallelLine(point.getX(), point.getY());
    }

    public double getDistanceFrom(double x, double y) {
        return StrictMath.abs((a * x + b * y + c) / pseudoLength);
    }

    public double getDistanceFrom(Point2D point) {
        return getDistanceFrom(point.getX(), point.getY());
    }

    public double getDistanceFrom(Line2D line, double epsilon) {
        if (getIntersectionPoint(line, epsilon) != null) {
            return Double.NaN;
        }

        return StrictMath.abs(c - line.c) / pseudoLength;
    }

    public double getDistanceFrom(Line2D line) {
        return getDistanceFrom(line, DEFAULT_EPSILON);
    }

    public double getSignedDistanceFrom(double x, double y) {
        return (a * x + b * y + c) / pseudoLength;
    }

    public double getSignedDistanceFrom(Point2D point) {
        return getSignedDistanceFrom(point.getX(), point.getY());
    }

    public double getSignedDistanceFrom(Line2D line, double epsilon) {
        if (getIntersectionPoint(line, epsilon) != null) {
            return Double.NaN;
        }

        return (c - line.c) / pseudoLength;
    }

    public double getSignedDistanceFrom(Line2D line) {
        return getSignedDistanceFrom(line, DEFAULT_EPSILON);
    }

    public Vector2D getUnitNormal() {
        return new Vector2D(a / pseudoLength, b / pseudoLength);
    }

    public Vector2D getUnitNormalFrom(double x, double y, double epsilon) {
        double signedDistance = getSignedDistanceFrom(x, y);

        if (signedDistance <= -epsilon) {
            return new Vector2D(a / pseudoLength, b / pseudoLength);
        } else if (signedDistance >= epsilon) {
            return new Vector2D(-a / pseudoLength, -b / pseudoLength);
        } else {
            throw new IllegalArgumentException(String.format("Point {x=%s, y=%s} is on the %s.", x, y, this));
        }
    }

    public Vector2D getUnitNormalFrom(double x, double y) {
        return getUnitNormalFrom(x, y, DEFAULT_EPSILON);
    }

    public Vector2D getUnitNormalFrom(Point2D point, double epsilon) {
        return getUnitNormalFrom(point.getX(), point.getY(), epsilon);
    }

    public Vector2D getUnitNormalFrom(Point2D point) {
        return getUnitNormalFrom(point.getX(), point.getY(), DEFAULT_EPSILON);
    }

    public Point2D getProjectionOf(double x, double y, double epsilon) {
        double distance = getDistanceFrom(x, y);
        if (distance < epsilon) {
            return new Point2D(x, y);
        }

        Vector2D unitNormal = getUnitNormalFrom(x, y, epsilon);
        return new Point2D(x + unitNormal.getX() * distance, y + unitNormal.getY() * distance);
    }

    public Point2D getProjectionOf(double x, double y) {
        return getProjectionOf(x, y, DEFAULT_EPSILON);
    }

    public Point2D getProjectionOf(Point2D point, double epsilon) {
        return getProjectionOf(point.getX(), point.getY(), epsilon);
    }

    public Point2D getProjectionOf(Point2D point) {
        return getProjectionOf(point.getX(), point.getY(), DEFAULT_EPSILON);
    }

    /**
     * Get intersection point or {@code null} if lines are parallel.
     *
     * @param line    to intersect with
     * @param epsilon to check if lines are parallel
     * @return intersection point or {@code null} if lines are parallel
     */
    public Point2D getIntersectionPoint(Line2D line, double epsilon) {
        double d = a * line.b - line.a * b;
        return StrictMath.abs(d) < StrictMath.abs(epsilon)
                ? null
                : new Point2D((b * line.c - line.b * c) / d, (line.a * c - a * line.c) / d);
    }

    /**
     * Get intersection point or {@code null} if lines are parallel.
     * Uses {@link #DEFAULT_EPSILON {@code DEFAULT_EPSILON}} as epsilon when checking if lines are parallel.
     *
     * @param line to intersect with
     * @return intersection point or {@code null} if lines are parallel
     */
    public Point2D getIntersectionPoint(Line2D line) {
        return getIntersectionPoint(line, DEFAULT_EPSILON);
    }
    
    public boolean isIntersect(Line2D line) {
    	return (getIntersectionPoint(line) != null);
    }

    public Line2D copy() {
        return new Line2D(this);
    }

    @Override
    public String toString() {
        return StrictMath.round(getA())+"x + "+StrictMath.round(getB())+"y + "+StrictMath.round(getC())+" = 0";
    }

}
