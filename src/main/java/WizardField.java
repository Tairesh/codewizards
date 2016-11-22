
import model.Wizard;

public class WizardField extends PotentialField {

    private final double angleKoeff = 0.75;
    private final double selfCastRangeMinKoeff = 0.85;
    
    private final Wizard wizard;
    private final Wizard self;
    private final Point center;
    private final double maxCastDist;
    private final int wizardRemainingTicks;
    private final int selfRemainingTicks;

    public WizardField(Wizard wizard, Wizard self)
    {
        this.wizard = wizard;
        this.self = self;
        this.center = new Point((int) wizard.getX() / MyStrategy.POTENTIAL_GRID_COL_SIZE, (int) wizard.getY() / MyStrategy.POTENTIAL_GRID_COL_SIZE);
        
        maxCastDist = wizard.getCastRange() + self.getRadius() + MyStrategy.game.getMagicMissileRadius();
        
        wizardRemainingTicks = StrictMath.max(wizard.getRemainingCooldownTicksByAction()[2], wizard.getRemainingActionCooldownTicks());
        selfRemainingTicks = StrictMath.max(self.getRemainingCooldownTicksByAction()[2], self.getRemainingActionCooldownTicks());
    }

    @Override
    public double getValue(int x, int y)
    {
        int colSize = MyStrategy.POTENTIAL_GRID_COL_SIZE;
        double distance = center.getDistanceTo(x, y)*colSize;
        if (distance < wizard.getRadius() + colSize) {
            return -200.0;
        } else if (distance > maxCastDist - colSize) {
            return 0.0;
        } else {
            double absAngle = StrictMath.abs(wizard.getAngleTo(x*MyStrategy.POTENTIAL_GRID_COL_SIZE, y*MyStrategy.POTENTIAL_GRID_COL_SIZE));
            double distFactor = distance / (maxCastDist + colSize); // 0 мы вплотную 1 мы на макс расстоянии атаки
            double dangerAngle = angleKoeff * (MyStrategy.game.getStaffSector() / 2.0) / distFactor;
            
            double value = (absAngle <= dangerAngle) ? -200.0 : -10.0/(absAngle-dangerAngle);
            
            double cooldownFactor = wizardRemainingTicks < 20 ? (double)(20-wizardRemainingTicks)/20.0 : 1.0;
            value *= cooldownFactor;
            
            double selfCastRange = self.getCastRange() + wizard.getRadius() + MyStrategy.game.getMagicMissileRadius();
            if (distance < selfCastRange + colSize && distance > selfCastRange*selfCastRangeMinKoeff - colSize) {
                double selfCooldownFactor = selfRemainingTicks < 5 ? (double)(5-selfRemainingTicks) : 0.0;
                value += 50.0 * selfCooldownFactor;
            }
            
            return value;
        }
    }

}
