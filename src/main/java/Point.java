public class Point {
    
    private static final double SQRT_TWO_DOUBLE = Math.sqrt(2);
    
    public final int x;
    public final int y;
    
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    public boolean equals(Point other) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }
    
    public boolean equals(int x, int y)
    {
        return this.x == x && this.y == y;
    }
    
    public double getDistanceTo(int x, int y) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        
        if (xDiff == 0) {
            return StrictMath.abs(yDiff);
        }
        if (yDiff == 0) {
            return StrictMath.abs(xDiff);
        }
        if (xDiff == yDiff || xDiff == -yDiff) {
            return SQRT_TWO_DOUBLE*Math.abs(xDiff);
        }
        
        int squareDistance = xDiff*xDiff + yDiff*yDiff;
        
        return StrictMath.sqrt(squareDistance);
    }
    
    public double getDistanceTo(Point point) {
        return getDistanceTo(point.x, point.y);
    }
    
    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
    
}
