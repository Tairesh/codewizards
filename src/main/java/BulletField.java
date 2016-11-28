
import model.Projectile;
import model.Tree;
import model.Wizard;


public class BulletField extends PotentialField {
    
    private final Projectile bullet;
    private final Wizard self;
    private final Point center;
    private final LineSegment2D lineSegment;
    private final Line2D line;

    public BulletField(Projectile bullet, Wizard self) {
        this.bullet = bullet;
        this.self = self;
        this.center = new Point((int)bullet.getX()/MyStrategy.POTENTIAL_GRID_COL_SIZE, (int)bullet.getY()/MyStrategy.POTENTIAL_GRID_COL_SIZE);
        Vector2D speed = new Vector2D(new Point2D(bullet.getSpeedX(), bullet.getSpeedY()));
        if (speed.getLength() < 0.1) {
            speed = new Vector2D(10.0, MyMath.normalizeAngle(bullet.getAngle()+bullet.getAngleTo(self)));
        }
        double distance = getMaxDistance();
        speed.setLength(distance);
        lineSegment = new LineSegment2D(bullet.getX(),bullet.getY(),bullet.getX()+speed.getX(),bullet.getY()+speed.getY());
        for (Tree tree : MyStrategy.world.getTrees()) {
            double treeDist = tree.getDistanceTo(bullet);
            if (treeDist < distance) {
                if (lineSegment.isCrossingCircle(tree)) {
                    lineSegment.setLength(treeDist);
                    distance = treeDist;
                }
            }
        }
        line = new Line2D(bullet.getX(),bullet.getY(),bullet.getX()+speed.getX(),bullet.getY()+speed.getY());
    }
    
    private double getMaxDistance()
    {
        switch(bullet.getType()) {
            case MAGIC_MISSILE:
            case FROST_BOLT:
            case FIREBALL:
                return 600.0;
            case DART:
                return MyStrategy.game.getFetishBlowdartAttackRange();
        }
        return 0.0;
    }
    
    @Override
    public double getValue(int x, int y) {
        double distance = center.getDistanceTo(x, y)*MyStrategy.POTENTIAL_GRID_COL_SIZE;
        if (distance > getMaxDistance()+MyStrategy.POTENTIAL_GRID_COL_SIZE) {
            return 0.0;
        } else if (distance < bullet.getRadius()+MyStrategy.POTENTIAL_GRID_COL_SIZE) {
            return -200.0;
        } else {
            double distToLine = StrictMath.abs(line.getDistanceFrom(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE));
            double distToPoint1 = lineSegment.getPoint1().getDistanceTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE);
            double distToPoint2 = lineSegment.getPoint2().getDistanceTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE);
            double minDist = StrictMath.min(distToLine, StrictMath.min(distToPoint1, distToPoint2));
            if (minDist <= self.getRadius()+MyStrategy.POTENTIAL_GRID_COL_SIZE) {
                return -500.0;
            } else {
                return -300.0 / (minDist - self.getRadius() - MyStrategy.POTENTIAL_GRID_COL_SIZE) ;
            }
        }
    }

}
