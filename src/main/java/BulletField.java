
import model.LivingUnit;
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
            LivingUnit owner = MyStrategy.allUnits.stream().filter(unit -> unit.getId() == bullet.getOwnerUnitId()).findFirst().orElse(null);
            if (null != owner) {
                speed = new Vector2D(owner.getX(), owner.getY(), bullet.getX(), bullet.getY());
            } else {
                speed = new Vector2D(10.0, MyMath.normalizeAngle(bullet.getAngle()+bullet.getAngleTo(self)));
            }
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
        double distance = 0.0;
        if (MyStrategy.bulletsCache.containsKey(bullet.getId())) {
            distance = MyStrategy.bulletsCache.get(bullet.getId());
        }
                
        double maxDistance = 0.0;
        switch(bullet.getType()) {
            case MAGIC_MISSILE:
            case FROST_BOLT:
            case FIREBALL:
                maxDistance = 600.0;
                break;
            case DART:
                maxDistance = MyStrategy.game.getFetishBlowdartAttackRange();
                break;
        }
        return maxDistance - distance;
    }
    
    private double getBulletRadius()
    {
        switch(bullet.getType()) {
            case MAGIC_MISSILE:
                return MyStrategy.game.getMagicMissileRadius();
            case FROST_BOLT:
                return MyStrategy.game.getFrostBoltRadius();
            case FIREBALL:
                return MyStrategy.game.getFireballExplosionMinDamageRange();
            case DART:
                return MyStrategy.game.getDartRadius();
        }
        return 0.0;
    }
    
    @Override
    public double getValue(int x, int y) {
        double distance = center.getDistanceTo(x, y)*MyStrategy.POTENTIAL_GRID_COL_SIZE;
        if (distance > getMaxDistance()+MyStrategy.POTENTIAL_GRID_COL_SIZE) {
            return 0.0;
        } else if (distance < bullet.getRadius()+MyStrategy.POTENTIAL_GRID_COL_SIZE) {
            return -1000.0;
        } else {
            double distToLine = StrictMath.abs(line.getDistanceFrom(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE));
            double distToPoint1 = lineSegment.getPoint1().getDistanceTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE);
            double distToPoint2 = lineSegment.getPoint2().getDistanceTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE);
            double minDist = StrictMath.min(distToLine, StrictMath.min(distToPoint1, distToPoint2));
            if (minDist <= self.getRadius()+getBulletRadius()+MyStrategy.POTENTIAL_GRID_COL_SIZE) {
                return -1000.0;
            } else {
                return -500.0 / (minDist - self.getRadius() - MyStrategy.POTENTIAL_GRID_COL_SIZE) ;
            }
        }
    }

}
