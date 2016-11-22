import java.awt.Point;
import model.Unit;


public class Point2D {

    protected double x;
    protected double y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Point2D(Point2D point) {
        x = point.x;
        y = point.y;
    }
    
    public Point2D(Unit unit) {
        x = unit.getX();
        y = unit.getY();
    }
    
    public Point2D(Point point) {
        x = (double) point.x;
        y = (double) point.y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDistanceTo(double x, double y) {
        return StrictMath.hypot(this.x - x, this.y - y);
    }

    public double getDistanceTo(Point2D point) {
        return getDistanceTo(point.getX(), point.getY());
    }

    public double getDistanceTo(Unit unit) {
        return getDistanceTo(unit.getX(), unit.getY());
    }
    
    public Point toPoint()
    {
        return new Point((int) StrictMath.round(x), (int) StrictMath.round(y));
    }

    void add(Vector2D vector) {
        x += vector.getX();
        y += vector.getY();
    }

}
