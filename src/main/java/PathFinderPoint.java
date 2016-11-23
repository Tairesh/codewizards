
public class PathFinderPoint extends Point {
    
    public double h;
    public double g;
    public PathFinderPoint previous;
    
    public PathFinderPoint(int x, int y) {
        super(x, y);
    }
    
    public double getF()
    {
        return h+g;
    }
    
    public int compareTo(PathFinderPoint point)
    {
        return point.getF() > getF() ? -1 : 1;
    }
    
}
