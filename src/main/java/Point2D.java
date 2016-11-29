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

    public void add(Vector2D vector) {
        x += vector.getX();
        y += vector.getY();
    }
    
    public boolean equals(Point2D point)
    {
        return StrictMath.abs(x-point.x) < 0.1 && StrictMath.abs(y-point.y) < 0.1;
    }
    
    public static Point2D pointBetween(Unit unit1, Unit unit2)
    {
        return new Point2D((unit1.getX()+unit2.getX())/2.0, (unit1.getY()+unit2.getY())/2.0);
    }

}
