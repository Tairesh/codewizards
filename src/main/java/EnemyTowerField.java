
import model.Building;
import model.Wizard;

public class EnemyTowerField extends PotentialField {

    private final double selfCastRangeMinKoeff = 0.85;
    
    private final Building building;
    private final Point center;
    private final Wizard self;
    
    private final double maxCastDist;
    private final int buildingRemainingTicks;
    private final int selfRemainingTicks;

    public EnemyTowerField(Building building, Wizard self) {
        this.building = building;
        this.center = new Point((int) building.getX() / MyStrategy.POTENTIAL_GRID_COL_SIZE, (int) building.getY() / MyStrategy.POTENTIAL_GRID_COL_SIZE);
        this.self = self;
        
        maxCastDist = building.getAttackRange() + self.getRadius();
        selfRemainingTicks = StrictMath.max(self.getRemainingCooldownTicksByAction()[2], self.getRemainingActionCooldownTicks());
        buildingRemainingTicks = building.getRemainingActionCooldownTicks();
    }

    @Override
    public double getValue(int x, int y) {
        
        int colSize = MyStrategy.POTENTIAL_GRID_COL_SIZE;
        double distance = center.getDistanceTo(x, y)*colSize;
        
        if (distance < building.getRadius() + colSize) {
            return -200.0;
        } else if (distance > maxCastDist + colSize) {
            double value;
            double selfCastRange = self.getCastRange() + building.getRadius() + MyStrategy.game.getMagicMissileRadius();
            if (distance > selfCastRange + colSize) {
                value = 0.0;
            } else {
                value = 50.0/(distance/selfCastRange);
            }

            if (distance < selfCastRange + colSize && distance > selfCastRange*selfCastRangeMinKoeff - colSize) {
                double selfCooldownFactor = selfRemainingTicks < 5 ? (double)(5-selfRemainingTicks) : 0.0;
                value += 50.0 * selfCooldownFactor;
            }
            return value;
        } else {
            double value = -200.0;
            double cooldownFactor = buildingRemainingTicks < 30 ? (double)(30-buildingRemainingTicks)/30.0 : 0.0;
            value *= cooldownFactor;
            
            double selfCastRange = self.getCastRange() + building.getRadius() + MyStrategy.game.getMagicMissileRadius();
            if (distance < selfCastRange + colSize && distance > selfCastRange*selfCastRangeMinKoeff - colSize) {
                double selfCooldownFactor = selfRemainingTicks < 5 ? (double)(5-selfRemainingTicks) : 0.0;
                value += 50.0 * selfCooldownFactor;
            }
            
            return value;
        }
    }

}
