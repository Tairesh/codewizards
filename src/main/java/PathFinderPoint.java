
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

    public List<PathFinderPoint> getNeighbors()
    {
        List<PathFinderPoint> neigbors = new ArrayList<>(8);
        if (currentPoint.x > 0) { // влево
            neigbors.add(new PathFinderPoint(currentPoint.x-1, currentPoint.y));
        }
        if (currentPoint.x < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вправо
            neigbors.add(new PathFinderPoint(currentPoint.x+1, currentPoint.y));
        }
        if (currentPoint.y > 0) { // вверх
            neigbors.add(new PathFinderPoint(currentPoint.x, currentPoint.y-1));
        }
        if (currentPoint.y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вниз
            neigbors.add(new PathFinderPoint(currentPoint.x, currentPoint.y+1));
        }
        if (currentPoint.x > 0 && currentPoint.y > 0) { // влево вверх
            neigbors.add(new PathFinderPoint(currentPoint.x-1, currentPoint.y-1));
        }
        if (currentPoint.x < MyStrategy.POTENTIAL_GRID_SIZE-1 && currentPoint.y > 0) { // вправо вверх
            neigbors.add(new PathFinderPoint(currentPoint.x+1, currentPoint.y-1));
        }
        if (currentPoint.x > 0 && currentPoint.y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // влево вниз
            neigbors.add(new PathFinderPoint(currentPoint.x-1, currentPoint.y+1));
        }
        if (currentPoint.x < MyStrategy.POTENTIAL_GRID_SIZE-1 && currentPoint.y < MyStrategy.POTENTIAL_GRID_SIZE-1) { // вправо вниз
            neigbors.add(new PathFinderPoint(currentPoint.x+1, currentPoint.y+1));
        }
        return neigbors;
    }
    
}
