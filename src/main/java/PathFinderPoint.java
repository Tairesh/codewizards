
import java.util.ArrayList;
import java.util.List;

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

    public List<PathFinderPoint> getNeighbours()
    {
        List<PathFinderPoint> neigbors = new ArrayList<>(8);
        if (x > 0) { // влево
            neigbors.add(new PathFinderPoint(x-1, y));
        }
        if (x < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вправо
            neigbors.add(new PathFinderPoint(x+1, y));
        }
        if (y > 0) { // вверх
            neigbors.add(new PathFinderPoint(x, y-1));
        }
        if (y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вниз
            neigbors.add(new PathFinderPoint(x, y+1));
        }
        if (x > 0 && y > 0) { // влево вверх
            neigbors.add(new PathFinderPoint(x-1, y-1));
        }
        if (x < MyStrategy.POTENTIAL_GRID_SIZE-1 && y > 0) { // вправо вверх
            neigbors.add(new PathFinderPoint(x+1, y-1));
        }
        if (x > 0 && y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // влево вниз
            neigbors.add(new PathFinderPoint(x-1, y+1));
        }
        if (x < MyStrategy.POTENTIAL_GRID_SIZE-1 && y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вправо вниз
            neigbors.add(new PathFinderPoint(x+1, y+1));
        }
        return neigbors;
    }
    
}
