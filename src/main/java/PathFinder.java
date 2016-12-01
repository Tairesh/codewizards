
import java.util.ArrayList;
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
        
    private Point getOtherFinishPoint(Point point, boolean recursive)
    {
        List<Point> neigbors = getNeighbours(point);
        Point max = neigbors.stream().max((o1, o2) -> {
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
        boolean endAllBlocked = true;
        for (Point n : getNeighbours(to)) {
            if (!blocked[n.x][n.y]) {
                endAllBlocked = false;
                break;
            }
        }
        if (endAllBlocked) {
            return false;
        }
        boolean allBlocked = true;
        for (Point n : getNeighbours(from)) {
            if (!blocked[n.x][n.y]) {
                allBlocked = false;
                break;
            }
        }
        return !allBlocked;
    }
    
    private List<Point> getNeighbours(Point point)
    {
        int x = point.x;
        int y = point.y;
        
        List<Point> neigbors = new ArrayList<>(8);
        if (x > 0) { // влево
            neigbors.add(new Point(x-1, y));
        }
        if (x < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вправо
            neigbors.add(new Point(x+1, y));
        }
        if (y > 0) { // вверх
            neigbors.add(new Point(x, y-1));
        }
        if (y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вниз
            neigbors.add(new Point(x, y+1));
        }
        if (x > 0 && y > 0) { // влево вверх
            neigbors.add(new Point(x-1, y-1));
        }
        if (x < MyStrategy.POTENTIAL_GRID_SIZE-1 && y > 0) { // вправо вверх
            neigbors.add(new Point(x+1, y-1));
        }
        if (x > 0 && y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // влево вниз
            neigbors.add(new Point(x-1, y+1));
        }
        if (x < MyStrategy.POTENTIAL_GRID_SIZE-1 && y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вправо вниз
            neigbors.add(new Point(x+1, y+1));
        }
        return neigbors;
    }
    
    private boolean isNeighbour(Point point1, Point point2)
    {
        if (point1.equals(point2)) {
            return true;
        }
        return getNeighbours(point1).stream().anyMatch((n) -> (n.equals(point2)));
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
        
        AAPFGridGraph graph = new AAPFGridGraph(MyStrategy.POTENTIAL_GRID_SIZE,MyStrategy.POTENTIAL_GRID_SIZE);
        for (int i = 0; i < MyStrategy.POTENTIAL_GRID_SIZE; i++) {
            for (int j = 0; j < MyStrategy.POTENTIAL_GRID_SIZE; j++) {
                graph.trySetBlocked(i, j, blocked[i][j]);
            }
        }
        
        int[][] path = AAPFUtility.generatePath(AAPFBasicThetaStar::postSmooth, graph, from.x, from.y, to.x, to.y);
        
        lastCalculatedPath = new ArrayList<>(path.length);
        for (int[] p : path) {
            lastCalculatedPath.add(new Point(p[0], p[1]));
        }
        
        return lastCalculatedPath;
    }
    
}
