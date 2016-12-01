
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathFinder {
    
    private static PathFinder instance;
    public static boolean[][] blocked;
    public static boolean[][] treesBlocked;
    
    private List<Point> lastCalculatedPath = null;
    
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
        lastCalculatedPath = result;
        return result;
    }
    
    private Point getOtherFinishPoint(Point _point, boolean recursive)
    {
        PathFinderPoint point = new PathFinderPoint(_point.x, _point.y);
        List<PathFinderPoint> neigbors = point.getNeighbours();
        PathFinderPoint max = neigbors.stream().max((o1, o2) -> {
            double val1 = MyStrategy.potentialGrid[o1.x][o1.y];
            double val2 = MyStrategy.potentialGrid[o2.x][o2.y];
            if (val1 == val2) return 0;
            else return val1 < val2 ? -1 : 1;
        }).orElse(point);
        Point maxPoint = new Point(max.x, max.y);
        return (blocked[max.x][max.y] && recursive) ? getOtherFinishPoint(maxPoint, false) : maxPoint;
    }
    
    private boolean checkPath(Point from, Point to)
    {
        if (blocked[to.x][to.y]) {
            return false;
        }
        PathFinderPoint end = new PathFinderPoint(to.x, to.y);
        boolean endAllBlocked = true;
        for (PathFinderPoint n : end.getNeighbours()) {
            if (!blocked[n.x][n.y]) {
                endAllBlocked = false;
                break;
            }
        }
        if (endAllBlocked) {
            return false;
        }
        PathFinderPoint start = new PathFinderPoint(from.x, from.y);
        boolean allBlocked = true;
        for (PathFinderPoint n : start.getNeighbours()) {
            if (!blocked[n.x][n.y]) {
                allBlocked = false;
                break;
            }
        }
        return !allBlocked;
    }
    
    private boolean isNeighbour(Point point1, Point point2)
    {
        if (point1.equals(point2)) {
            return true;
        }
        return (new PathFinderPoint(point1.x, point1.y)).getNeighbours().stream().anyMatch((n) -> (n.equals(point2)));
    }
    
    private boolean needRecalc(Point from, Point to)
    {
        if (null != lastCalculatedPath && isNeighbour(lastCalculatedPath.get(0),from) && isNeighbour(lastCalculatedPath.get(lastCalculatedPath.size()-1),to)) {
            return lastCalculatedPath.stream().anyMatch((p) -> (blocked[p.x][p.y]));
        }
        return true;
    }
    
    public List<Point> getPath(Point from, Point to)
    {
        if (blocked[to.x][to.y]) {
            to = getOtherFinishPoint(to, true);
        }
        if (!checkPath(from, to)) {
            return null;
        }
        if (!needRecalc(from, to)) {
            return lastCalculatedPath;
        }
        lastCalculatedPath = null;
        
        List<PathFinderPoint> openList = new ArrayList<>();
        boolean[][] added = new boolean[MyStrategy.POTENTIAL_GRID_SIZE][MyStrategy.POTENTIAL_GRID_SIZE];
        
        PathFinderPoint start = new PathFinderPoint(from.x, from.y);
        start.g = 0.0;
        start.h = from.getDistanceTo(to);
        
        openList.add(start);
        
        while(openList.size() > 0 && openList.size() < 10000) {
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
                if (added[newPoint.x][newPoint.y] || blocked[newPoint.x][newPoint.y]) {
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
