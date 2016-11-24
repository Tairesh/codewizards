
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathFinder {
    
    private static PathFinder instance;
    
    private PathFinder()
    {
        
    }
    
    public static PathFinder getInstance()
    {
        if (instance == null) {
            instance = new PathFinder();
        }
        return instance;
    }
    
    private double getHeuristicValue(Point point)
    {
        double potential = MyStrategy.potentialGrid[point.x][point.y];
        if (potential < 0) {
            return potential*-0.1;
        } else {
            return 0.0;
        }
    }
        
    private List<Point> smoothAndReversePath(List<PathFinderPoint> path)
    {
        List<Point> result = new ArrayList<>(path.size());
        
        // @todo smooth
        path.forEach((point) -> {
            result.add(new Point(point.x, point.y));
        });
        
        Collections.reverse(result);
        return result;
    }
    
    public List<Point> getPath(Point from, Point to)
    {
        List<PathFinderPoint> openList = new ArrayList<>();
        boolean[][] added = new boolean[MyStrategy.POTENTIAL_GRID_SIZE][MyStrategy.POTENTIAL_GRID_SIZE];
        
        PathFinderPoint start = new PathFinderPoint(from.x, from.y);
        start.g = 0.0;
        start.h = from.getDistanceTo(to);
        
        openList.add(start);
        
        while(openList.size() > 0) {
            PathFinderPoint currentPoint = Collections.min(openList, (o1, o2) -> o1.compareTo(o2));
                        
            openList.remove(currentPoint);
            added[currentPoint.x][currentPoint.y] = true;
            
            if (currentPoint.equals(to)) {
                List<PathFinderPoint> path = new ArrayList<>();
                do {
                    path.add(currentPoint);
                    currentPoint = currentPoint.previous;
                } while (currentPoint != null && !currentPoint.equals(start));
 
                return smoothAndReversePath(path);
            }
            
            // поиск соседей
            List<PathFinderPoint> neigbors = currentPoint.getNeighbours();
            
            for (PathFinderPoint newPoint : neigbors) {
                if (added[newPoint.x][newPoint.y]) {
                    continue;
                }
                newPoint.previous = currentPoint;                                                
                newPoint.g = currentPoint.g + getHeuristicValue(newPoint);
                newPoint.h = newPoint.getDistanceTo(to);
                openList.add(newPoint);
            }
        }
        
        return null;
    }
    
}
