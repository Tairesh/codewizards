
import model.Minion;
import model.Wizard;

public class MinionField extends PotentialField {

    private final double angleKoeff = 0.75;
    private final double selfCastRangeMinKoeff = 0.85;
    
    private final Minion minion;
    private final Wizard self;
    private final Point center; 
    private final double maxCastDist;
    private final double maxCastSector;
    private final int remainingTicks;
    private final int selfRemainingTicks;
    
    public MinionField(Minion minion, Wizard self) {
        this.minion = minion;
        this.self = self;
        this.center = new Point((int)minion.getX()/MyStrategy.POTENTIAL_GRID_COL_SIZE, (int)minion.getY()/MyStrategy.POTENTIAL_GRID_COL_SIZE);
        
        switch (minion.getType()) {
            case ORC_WOODCUTTER:
                maxCastDist = self.getRadius() + MyStrategy.game.getOrcWoodcutterAttackRange() + 50.0; // хардкод чтобы сильнее боялся орков
                maxCastSector = MyStrategy.game.getOrcWoodcutterAttackSector() / 2.0;
                break;
            case FETISH_BLOWDART:
                maxCastDist = self.getRadius() + MyStrategy.game.getDartRadius() + MyStrategy.game.getFetishBlowdartAttackRange();
                maxCastSector = MyStrategy.game.getFetishBlowdartAttackSector() / 2.0;
                break;
            default:
                maxCastDist = 0.0;
                maxCastSector = 0.0;
                break;
        }
        
        remainingTicks = minion.getRemainingActionCooldownTicks();
        selfRemainingTicks = StrictMath.max(self.getRemainingCooldownTicksByAction()[2], self.getRemainingActionCooldownTicks());
    }
    
    @Override
    public double getValue(int x, int y) {
        
        int colSize = MyStrategy.POTENTIAL_GRID_COL_SIZE;
        double distance = center.getDistanceTo(x, y)*colSize;
        
        
        if (distance < minion.getRadius() + colSize) {
            return -200.0;
        } else if (distance > maxCastDist + colSize) {
            if (minion.getFaction() == self.getFaction()) {
                return 0.0;
            } else {
                double selfCastRange = self.getCastRange() + minion.getRadius() + MyStrategy.game.getMagicMissileRadius();
                if (distance > selfCastRange + colSize) {
                    return 0.0;
                } else {
                    double value = -100.0/(distance/maxCastDist);
                    if (distance < selfCastRange + colSize && distance > selfCastRange*selfCastRangeMinKoeff - colSize) {
                        double selfCooldownFactor = selfRemainingTicks < 20 ? (double)(20-selfRemainingTicks) : 0.0;
                        value += 5.0 * selfCooldownFactor;
                    }
                    return value;
                }
            }
        } else {
            double value;
            if (minion.getFaction() == self.getFaction()) {
                
                double absAngle = StrictMath.abs(minion.getAngleTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE));
                double distFactor = distance / (maxCastDist + colSize); // 0 мы вплотную 1 мы на макс расстоянии атаки
                double dangerAngle = angleKoeff * (maxCastSector / 2.0) / distFactor;

                value = (absAngle <= dangerAngle) ? -10.0 : (absAngle-dangerAngle)*10.0;
            } else {
                
                double absAngle = StrictMath.abs(minion.getAngleTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE));
                double distFactor = distance / (maxCastDist + colSize); // 0 мы вплотную 1 мы на макс расстоянии атаки
                double dangerAngle = angleKoeff * (maxCastSector / 2.0) / distFactor;

                value = (absAngle <= dangerAngle) ? -200.0 : -75.0/(absAngle-dangerAngle);
                
                double cooldownFactor = remainingTicks < 20 ? (double)(20-remainingTicks)/20.0 : 1.0;
                value *= cooldownFactor;
                
                double selfCastRange = self.getCastRange() + minion.getRadius() + MyStrategy.game.getMagicMissileRadius();
                if (distance < selfCastRange + colSize && distance > selfCastRange*selfCastRangeMinKoeff - colSize) {
                    double selfCooldownFactor = selfRemainingTicks < 20 ? (double)(20-selfRemainingTicks) : 0.0;
                    value += 5.0 * selfCooldownFactor;
                }
            }
            
            return value;
        }
    }
}
