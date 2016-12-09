
public class Square
{
    public static final int SIZE = 500;
    public final int x;
    public final int y;
    
    public Square(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
    
    public Square(Point point)
    {
        this(point.x, point.y);
    }
    
    public boolean equals(Point point)
    {
        return x == point.x && y == point.y;
    }
    
    public boolean equals(Square point)
    {
        return x == point.x && y == point.y;
    }
    
    public int getDistanceTo(Point point)
    {
        return StrictMath.abs(x-point.x)+StrictMath.abs(y-point.y);
    }
    
    public int getLeftX()
    {
        return x*SIZE;
    }
    
    public int getTopY()
    {
        return y*SIZE;
    }
    
    public int getRightX()
    {
        return x*SIZE + SIZE;
    }
    
    public int getBottomY()
    {
        return y*SIZE + SIZE;
    }
    
    public int getCenterX()
    {
        return x*SIZE + SIZE/2;
    }
    
    public int getCenterY()
    {
        return y*SIZE + SIZE/2;
    }
    
    public Point2D getCenter()
    {
        return new Point2D(getCenterX(), getCenterY());
    }
        
    public static Point getIndexByCoords(double x, double y)
    {
        return new Point((int)StrictMath.ceil(x/(double)SIZE)-1, (int)StrictMath.ceil(y/(double)SIZE)-1);
    }
    
    public static Point getIndexByCoords(Point2D point)
    {
        return getIndexByCoords(point.x, point.y);
    }
}