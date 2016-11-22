
import model.CircularUnit;


public class LineSegment2D {
    
    public static final double DEFAULT_EPSILON = 1.0E-6D;

    private double x1;
    private double y1;
    private double x2;
    private double y2;
    
    private double length;
    
    public LineSegment2D(double x1, double y1, double x2, double y2)
    {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        
        this.length = StrictMath.hypot(x2-x1, y2-y1);
    }
    
    public LineSegment2D(Point2D point1, Point2D point2)
    {
        x1 = point1.getX();
        y1 = point1.getY();
        x2 = point2.getX();
        y2 = point2.getY();
        
        length = StrictMath.hypot(x2-x1, y2-x1);
    }
    
    /**
     * @return the x1
     */
    public double getX1()
    {
        return x1;
    }

    /**
     * @param x1 the x1 to set
     */
    public void setX1(double x1) {
        this.x1 = x1;
    }

    /**
     * @return the y1
     */
    public double getY1() {
        return y1;
    }

    /**
     * @param y1 the y1 to set
     */
    public void setY1(double y1) {
        this.y1 = y1;
    }

    /**
     * @return the x2
     */
    public double getX2() {
        return x2;
    }

    /**
     * @param x2 the x2 to set
     */
    public void setX2(double x2) {
        this.x2 = x2;
    }

    /**
     * @return the y2
     */
    public double getY2() {
        return y2;
    }

    /**
     * @param y2 the y2 to set
     */
    public void setY2(double y2) {
        this.y2 = y2;
    }

    /**
     * @return the length
     */
    public double getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(double length) {
        this.length = length;
    }
    
    public Point2D getPoint1() {
        return new Point2D(x1,y1);
    }
    
    public Point2D getPoint2() {
        return new Point2D(x2,y2);
    }
    
    public Line2D getLine() {
        return new Line2D(getPoint1(), getPoint2());
    }
    
    public LineSegment2D copy() {
        return new LineSegment2D(x1,y1,x2,y2);
    }
    
    public LineSegment2D add(double x, double y) {
        this.x1 += x;
        this.x2 += x;
        this.y1 += y;
        this.y2 += y;
        return this;
    }
    
    public LineSegment2D add(Vector2D vector) {
        return add(vector.getX(), vector.getY());
    }
    
    public boolean isCrossingCircle(double x, double y, double r)
    {
        //сдвигаем окружность и линию, так что окружность оказывается в начале координат
        double cx1 = x1 - x;
        double cx2 = x2 - x;
        double cy1 = y1 - y;
        double cy2 = y2 - y;

        double dx= cx2 - cx1;
        double dy= cy2 - cy1;

        //составляем коэфициенты квадратного уравнения на перечение прямой и окружности.
        //если на отрезке [0..1] есть отрицательные значения, значит отрезок пересекает окружность
        double a = dx*dx + dy*dy;
        double b = 2*(cx1*dx + cy1*dy);
        double c = cx1*cx1 + cy1*cy1-r*r;

        //а теперь проверяем, есть ли на отрезке [0..1] решения
        if (-b < 0)
          return (c < 0);
        if (-b < (2*a))
          return (4*a*c - b*b<0);

        return (a+b+c <0);
    }
    
    public boolean isCrossingCircle(CircularUnit unit)
    {
        return isCrossingCircle(unit.getX(), unit.getY(), unit.getRadius());
    }

    @Override
    public String toString() {
        return x1+"."+y1+"-"+x2+","+y2;
    }
    
}
