
import java.util.Map;
import java.util.EnumMap;
import model.*;


public class GlobalMap
{
    
    private static GlobalMap instance;
    public Map<FakeLaneType,Square[]> map;
    
    private GlobalMap()
    {
        map = new EnumMap<>(FakeLaneType.class);
        map.put(FakeLaneType.TOP, new Square[]{
            new Square(0,7),
            new Square(0,6),
            new Square(0,5),
            new Square(0,4),
            new Square(0,3),
            new Square(0,2),
            new Square(0,1),
            new Square(1,0),
            new Square(2,0),
            new Square(3,0),
            new Square(4,0),
            new Square(5,0),
            new Square(6,0),
            new Square(7,0)
        });
        map.put(FakeLaneType.BOTTOM, new Square[]{
            new Square(0,7),
            new Square(1,7),
            new Square(2,7),
            new Square(3,7),
            new Square(4,7),
            new Square(5,7),
            new Square(6,7),
            new Square(7,6),
            new Square(7,5),
            new Square(7,4),
            new Square(7,3),
            new Square(7,2),
            new Square(7,1),
            new Square(7,0)
        });
        map.put(FakeLaneType.MIDDLE, new Square[]{
            new Square(0,7),
            new Square(1,6),
            new Square(2,5),
            new Square(3,4),
            new Square(4,3),
            new Square(5,2),
            new Square(6,1),
            new Square(7,0)
        });
        map.put(FakeLaneType.TOP_TO_BONUS1, new Square[]{
            new Square(0,7),
            new Square(0,6),
            new Square(0,5),
            new Square(0,4),
            new Square(0,3),
            new Square(0,2),
            new Square(0,1),
            new Square(1,1),
            new Square(2,2)            
        });
        map.put(FakeLaneType.BONUS1_TO_TOP, new Square[]{
            new Square(2,2),
            new Square(1,1),
            new Square(1,0),
            new Square(2,0),
            new Square(3,0),
            new Square(4,0),
            new Square(5,0),
            new Square(6,0),
            new Square(7,0)
        });
        map.put(FakeLaneType.MIDDLE_TO_BONUS1, new Square[]{
            new Square(0,7),
            new Square(1,6),
            new Square(2,5),
            new Square(3,4),
            new Square(3,3),
            new Square(2,2)
        });
        map.put(FakeLaneType.BONUS1_TO_MIDDLE, new Square[]{
            new Square(2,2),
            new Square(3,3),
            new Square(4,3),
            new Square(5,2),
            new Square(6,1),
            new Square(7,0)
        });
        map.put(FakeLaneType.MIDDLE_TO_BONUS2, new Square[]{
            new Square(0,7),
            new Square(1,6),
            new Square(2,5),
            new Square(3,4),
            new Square(4,4),
            new Square(5,5)
        });
        map.put(FakeLaneType.BONUS2_TO_MIDDLE, new Square[]{
            new Square(5,5),
            new Square(4,4),
            new Square(4,3),
            new Square(5,2),
            new Square(6,1),
            new Square(7,0)
        });
        map.put(FakeLaneType.BONUS2_TO_BOTTOM, new Square[]{
            new Square(5,5),
            new Square(6,6),
            new Square(7,6),
            new Square(7,5),
            new Square(7,4),
            new Square(7,3),
            new Square(7,2),
            new Square(7,1),
            new Square(7,0)
        });
        map.put(FakeLaneType.BOTTOM_TO_BONUS2, new Square[]{
            new Square(0,7),
            new Square(1,7),
            new Square(2,7),
            new Square(3,7),
            new Square(4,7),
            new Square(5,7),
            new Square(6,7),
            new Square(7,7),
            new Square(6,6),
            new Square(5,5)
        });
        map.put(FakeLaneType.ENEMYBASE_TO_TOP_BONUS1, new Square[]{
            new Square(7,0),
            new Square(6,0),
            new Square(5,0),
            new Square(4,0),
            new Square(3,0),
            new Square(2,0),
            new Square(1,0),
            new Square(1,1),
            new Square(2,2)   
        });
        map.put(FakeLaneType.ENEMYBASE_TO_BOTTOM_BONUS2, new Square[]{
            new Square(7,0),
            new Square(7,1),
            new Square(7,2),
            new Square(7,3),
            new Square(7,4),
            new Square(7,5),
            new Square(7,6),
            new Square(7,7),
            new Square(6,6),  
            new Square(5,5),  
        });
        map.put(FakeLaneType.ENEMYBASE_TO_MIDDLE_BONUS1, new Square[]{
            new Square(7,0),
            new Square(6,1),
            new Square(5,2),
            new Square(4,3),
            new Square(3,3),
            new Square(2,2)
        });
        map.put(FakeLaneType.ENEMYBASE_TO_MIDDLE_BONUS2, new Square[]{
            new Square(7,0),
            new Square(6,1),
            new Square(5,2),
            new Square(4,3),
            new Square(4,4),
            new Square(5,5)
        });
    }
    
    public static GlobalMap getInstance()
    {
        if (instance == null) {
            instance = new GlobalMap();
        }
        return instance;
    }
    
    public Point2D getNextWayPoint(FakeLaneType line, Wizard self)
    {
        Point point = Square.getIndexByCoords(self.getX(), self.getY());
        Square[] squares = map.get(line);
        if (squares[squares.length-1].equals(point)) {
            if (line == FakeLaneType.TOP_TO_BONUS1 || line == FakeLaneType.MIDDLE_TO_BONUS1 || line == FakeLaneType.ENEMYBASE_TO_TOP_BONUS1 || line == FakeLaneType.ENEMYBASE_TO_MIDDLE_BONUS1) {
                return new Point2D(MyStrategy.game.getMapSize()*0.3D, MyStrategy.game.getMapSize()*0.3D);
            }
            if (line == FakeLaneType.BOTTOM_TO_BONUS2 || line == FakeLaneType.MIDDLE_TO_BONUS2 || line == FakeLaneType.ENEMYBASE_TO_BOTTOM_BONUS2 || line == FakeLaneType.ENEMYBASE_TO_MIDDLE_BONUS2) {
                return new Point2D(MyStrategy.game.getMapSize()*0.7D, MyStrategy.game.getMapSize()*0.7D);
            }
        }
        for (int i = 0; i < squares.length-1; i++) {
            if (squares[i].equals(point)) {
                return squares[i+1].getCenter();
            }
        }
        
        int minDist = Integer.MAX_VALUE;
        int min = -1;
        for (int i = 0; i < squares.length-1; i++) {
            int distance = squares[i].getDistanceTo(point);
            if (distance < minDist) {
                min = i;
                minDist = distance;
            }
        }
        if (min > -1 && min < squares.length-1) {
            return squares[min+1].getCenter();
        }
        return squares[squares.length-1].getCenter();
    }
    
    public Point2D getPreviousWayPoint(FakeLaneType line, Wizard self)
    {
        
        Point point = Square.getIndexByCoords(self.getX(), self.getY());
        Square[] lineSquares = map.get(line);
        for (int i = lineSquares.length-1; i > 0; i--) {
            if (lineSquares[i].equals(point)) {
                return lineSquares[i-1].getCenter();
            }
        }
        
        Square closestSquare = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = lineSquares.length-1; i > 0; i--) {
            int dist = lineSquares[i].getDistanceTo(point);
            if (dist < minDist) {
                closestSquare = lineSquares[i];
                minDist = dist;
            }
        }
        if (closestSquare != null) {
            return closestSquare.getCenter();
        }
        
        return lineSquares[0].getCenter();
        
    }
    
    
}

class Square
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