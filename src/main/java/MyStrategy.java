import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import model.*;

public final class MyStrategy implements Strategy {
    
    private final IVisualClient debug = new VisualClient();
    private Random random;
    
    private Wizard self;
    private World world;
    public static Game game;
    private Move move;
    
    public static final int POTENTIAL_GRID_COL_SIZE = 50;
    public static int POTENTIAL_GRID_SIZE;
    public static double[][] potentialGrid;
    private double[][] staticPotentialGrid;
    private double[][] treesPotentialGrid;
    
    private Faction enemyFaction;
    
    private final boolean[] buildingsDestroyed = {
        false, false, false, false, false, false, false
    };    
    private Building[] fakeBuildings;
    private final Set<Long> neutralMinionsInAgre  = new HashSet<>(10);
    
    private List<Building> enemyBuildings; // вместе с фейковыми
    private List<Minion> enemyMinions; // вместе с нейтралами в агре
    
    private List<LivingUnit> allUnits;
    
    private boolean isEnemiesNear;
    
    @Override
    public void move(Wizard self, World world, Game game, Move move)
    {
        initStrategy(self, world, game, move);
        initTick(self, world, game, move);
        debug.beginPost();
        if (isEnemiesNear) {
            Point maxPotentialPoint = getBestExtremumPoint();
            Color color = Color.YELLOW;
            if (null == maxPotentialPoint) {
                maxPotentialPoint = getMaxPotentialPoint();
                color = Color.ORANGE;
            }
            double x = maxPotentialPoint.x * POTENTIAL_GRID_COL_SIZE;
            double y = maxPotentialPoint.y * POTENTIAL_GRID_COL_SIZE;
            debug.line(self.getX(), self.getY(), x, y, color);
            debug.fillCircle(x, y, POTENTIAL_GRID_COL_SIZE/2, Color.YELLOW);
            Vector2D vector = new Vector2D(4.0, self.getAngleTo(x, y));
            move.setSpeed(vector.getX());
            move.setStrafeSpeed(vector.getY());
            
            LivingUnit bestTarget = getBestTarget();
            if (null == bestTarget) {
                bestTarget = getNearestEnemy();
            }
            if (null == bestTarget) {
                bestTarget = fakeBuildings[6];
            }
            double angle = self.getAngleTo(bestTarget);
            double distance = self.getDistanceTo(bestTarget);
            move.setTurn(angle);
            if (distance <= self.getCastRange()+bestTarget.getRadius()+game.getMagicMissileRadius()
                && StrictMath.abs(angle) < game.getStaffSector() / 2.0) {
                move.setAction(ActionType.MAGIC_MISSILE);
                move.setCastAngle(angle);
                move.setMinCastDistance(distance-bestTarget.getRadius()-game.getMagicMissileRadius());
            }
        } else {
            move.setTurn(self.getAngleTo((self.getY()<500)?3700:300, 300));
            move.setSpeed(4.0);
        }
        debug.endPost();
        debug.beginPre();
        int startX = (int)(self.getX()-self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int startY = (int)(self.getY()-self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int endX = (int)(self.getX()+self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int endY = (int)(self.getY()+self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        startX = (startX < 0) ? 0 : startX;
        startY = (startY < 0) ? 0 : startY;
        endX = (endX >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endX;
        endY = (endY >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endY;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                debug.fillRect(x*POTENTIAL_GRID_COL_SIZE, y*POTENTIAL_GRID_COL_SIZE, (x+1)*POTENTIAL_GRID_COL_SIZE, (y+1)*POTENTIAL_GRID_COL_SIZE, debugColor(potentialGrid[x][y]));
                debug.rect(x*POTENTIAL_GRID_COL_SIZE, y*POTENTIAL_GRID_COL_SIZE, (x+1)*POTENTIAL_GRID_COL_SIZE, (y+1)*POTENTIAL_GRID_COL_SIZE, Color.BLACK);
                debug.text(x*POTENTIAL_GRID_COL_SIZE+5, (y+1)*POTENTIAL_GRID_COL_SIZE-5, ""+(int)potentialGrid[x][y], Color.BLACK);
            }
        }
        debug.endPre();
        
    }
    
    private boolean isEnemiesNear()
    {
        if (enemyBuildings.stream().anyMatch((building) -> (self.getDistanceTo(building) < building.getAttackRange()+self.getRadius()))) {
            return true;
        }
        if (enemyMinions.stream().anyMatch((minion) -> (self.getDistanceTo(minion) < self.getVisionRange()))) {
            return true;
        }        
        if (Arrays.asList(world.getWizards()).stream().anyMatch((wizard) -> (wizard.getFaction() == enemyFaction && self.getDistanceTo(wizard) < self.getVisionRange()))) {
            return true;
        }
        return false;
    }
    
    private LivingUnit getNearestEnemy()
    {
        double minDist = Double.MAX_VALUE;
        LivingUnit nearestUnit = null;
        
        List<LivingUnit> enemies = new ArrayList<>();
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> (wizard.getFaction() == enemyFaction)).forEachOrdered((wizard) -> {
            enemies.add(wizard);
        });
        enemies.addAll(enemyMinions);
        enemies.addAll(enemyBuildings);
        
        for (LivingUnit unit : enemies) {
            double distance = self.getDistanceTo(unit);
            if (distance < minDist) {
                minDist = distance;
                nearestUnit = unit;
            }
        }
            
        return nearestUnit;
    }
    
    private LivingUnit getBestTarget()
    {
        int maxScore = -1;
        LivingUnit bestTarget = null;
        
        List<LivingUnit> enemies = new ArrayList<>();
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> (wizard.getFaction() == enemyFaction)).forEachOrdered((wizard) -> {
            enemies.add(wizard);
        });
        enemies.addAll(enemyMinions);
        enemies.addAll(enemyBuildings);
        
        for (LivingUnit unit : enemies) {
            if (self.getDistanceTo(unit) > self.getVisionRange()) {
                continue;
            }
            int score = 0;
            
            if (unit.getLife() < game.getMagicMissileDirectDamage()) {
                score += 10;
            } else if (unit.getLife() < game.getMagicMissileDirectDamage()*2.0) {
                score += 5;
            } else if (unit.getLife() < unit.getMaxLife()*0.55) {
                score += 1;
            }
            
            if (unit.getClass() == Wizard.class) {
                score *= 100;
            } else if (unit.getClass() == Building.class) {
                score *= 10;
            }
            
            if (score > maxScore) {
                maxScore = score;
                bestTarget = unit;
            }
        }
        
        return bestTarget;
    }
    
    private boolean isExtremum(int x, int y)
    {
        double value = potentialGrid[x][y];
        
        if (x > 0) {
            if (potentialGrid[x-1][y] > value) {
                return false;
            }
        }
        if (y > 0) {
            if (potentialGrid[x][y-1] > value) {
                return false;
            }
        }
        if (x < POTENTIAL_GRID_SIZE-1) {
            if (potentialGrid[x+1][y] > value) {
                return false;
            }
        }
        if (y < POTENTIAL_GRID_SIZE-1) {
            if (potentialGrid[x][y+1] > value) {
                return false;
            }
        }
        return true;
    }
    
    private List<Point> getExtremumPoints()
    {
        List<Point> points = new ArrayList<>();
        int startX = (int)(self.getX()-self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int startY = (int)(self.getY()-self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int endX = (int)(self.getX()+self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int endY = (int)(self.getY()+self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        startX = (startX < 0) ? 0 : startX;
        startY = (startY < 0) ? 0 : startY;
        endX = (endX >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endX;
        endY = (endY >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endY;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (isExtremum(x,y)) {
                    points.add(new Point(x,y));
                }
            }
        }
        return points;
    }
    
    private Point getBestExtremumPoint()
    {
        List<Point> extremums = getExtremumPoints();
        if (extremums.isEmpty()) {
            return null;
        }
        List<Point> bestPoints = new ArrayList<>();
        for (Point point : extremums) {
            LineSegment2D segment = new LineSegment2D(self.getX(), self.getY(), point.x*POTENTIAL_GRID_COL_SIZE, point.y*POTENTIAL_GRID_COL_SIZE);
            
            Vector2D vector = new Vector2D(self.getRadius()+1.0, MyMath.normalizeAngle(self.getAngle()-StrictMath.PI/2.0));
            LineSegment2D segmentLeft = segment.copy().add(vector);
            vector.rotate(StrictMath.PI);
            LineSegment2D segmentRight = segment.copy().add(vector);
            
            boolean isCrossing = false;
            for (LivingUnit unit : allUnits) {
                if (segmentLeft.isCrossingCircle(unit) || segmentRight.isCrossingCircle(unit) || segment.isCrossingCircle(unit)) {
                    isCrossing = true;
                    break;
                }
            }
            if (!isCrossing) {
                bestPoints.add(point);
            }
        }
        int selfX = (int)self.getX()/POTENTIAL_GRID_COL_SIZE;
        int selfY = (int)self.getY()/POTENTIAL_GRID_COL_SIZE;
        bestPoints.sort((Point point1, Point point2) -> {
            return (StrictMath.abs(point1.x-selfX) + StrictMath.abs(point1.y-selfY) < StrictMath.abs(point2.x-selfX) + StrictMath.abs(point2.y-selfY)) ? 1 : -1;
        });
        return bestPoints.size() > 0 ? bestPoints.get(0) : null;
    }
    
    private Point getMaxPotentialPoint()
    {
        double max = -Double.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        
        int startX = (int)(self.getX()-self.getVisionRange()/3.0)/POTENTIAL_GRID_COL_SIZE;
        int startY = (int)(self.getY()-self.getVisionRange()/3.0)/POTENTIAL_GRID_COL_SIZE;
        int endX = (int)(self.getX()+self.getVisionRange()/3.0)/POTENTIAL_GRID_COL_SIZE;
        int endY = (int)(self.getY()+self.getVisionRange()/3.0)/POTENTIAL_GRID_COL_SIZE;
        startX = (startX < 0) ? 0 : startX;
        startY = (startY < 0) ? 0 : startY;
        endX = (endX >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endX;
        endY = (endY >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endY;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (potentialGrid[x][y] > max) {
                    max = potentialGrid[x][y];
                    maxX = x;
                    maxY = y;
                }
            }
        }
        
        return new Point(maxX, maxY);
    }
    
    private Color debugColor(double value)
    {
        int val = (int)StrictMath.round(value);
        if (val == 0) {
            return Color.WHITE;
        }
        int red;
        int green;
        int blue;
        if (val < 0) {
            blue = 255;
            green = 255+val;
            red = 255+val;
        } else {
            red = 255;
            green = 255-val;
            blue = 255-val;
        }
        if (blue < 0) blue = 0;
        if (green < 0) green = 0;
        if (red < 0) red = 0;
        if (red > 255) red = 255;
        if (green > 255) green = 255;
        if (blue > 255) blue = 255;
        return new Color(red, green, blue);
    }
    
    private void initStrategy(Wizard self, World world, Game game, Move move)
    {
        if (random == null) {
            random = new Random(game.getRandomSeed());
            
            POTENTIAL_GRID_SIZE = (int)game.getMapSize()/POTENTIAL_GRID_COL_SIZE;
            potentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            staticPotentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            treesPotentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            
            // заполнение статического поля потенциалов
            double value = -10.0;
            for (int x = 0; x < POTENTIAL_GRID_SIZE; x++) {
                for (int y = POTENTIAL_GRID_SIZE-1; y >= 0; y--) {                    
                    if (x == 0 || y == 0 || x == POTENTIAL_GRID_SIZE-1 || y == POTENTIAL_GRID_SIZE-1) {
                        staticPotentialGrid[x][y] = -500.0;
                    } else {
                        staticPotentialGrid[x][y] = value;
                    }
                    value += 10.0 / POTENTIAL_GRID_SIZE;
                }
                value -= 10.0 - (10.0 / POTENTIAL_GRID_SIZE);
            }
            
//            int bonus1coords = (int)(game.getMapSize()*0.3/POTENTIAL_GRID_COL_SIZE);
//            int bonus2coords = (int)(game.getMapSize()*0.7/POTENTIAL_GRID_COL_SIZE);
                        
            enemyFaction = self.getFaction() == Faction.ACADEMY ? Faction.RENEGADES : Faction.ACADEMY;
            fakeBuildings = new Building[]{
                new Building(124214, 2070.7106781186544, 1600.0, 0, 0, 0, enemyFaction,game.getGuardianTowerRadius(),(int)StrictMath.round(game.getGuardianTowerLife()),(int)StrictMath.round(game.getGuardianTowerLife()), self.getStatuses(),BuildingType.GUARDIAN_TOWER,game.getGuardianTowerVisionRange(),game.getGuardianTowerAttackRange(),game.getGuardianTowerDamage(),0,0),
                new Building(124215, 3097.386941332821, 1231.9023805485247, 0, 0, 0, enemyFaction,game.getGuardianTowerRadius(),(int)StrictMath.round(game.getGuardianTowerLife()),(int)StrictMath.round(game.getGuardianTowerLife()), self.getStatuses(),BuildingType.GUARDIAN_TOWER,game.getGuardianTowerVisionRange(),game.getGuardianTowerAttackRange(),game.getGuardianTowerDamage(),0,0),
                new Building(124215, 1687.8740025771563, 50.0, 0, 0, 0, enemyFaction,game.getGuardianTowerRadius(),(int)StrictMath.round(game.getGuardianTowerLife()),(int)StrictMath.round(game.getGuardianTowerLife()), self.getStatuses(),BuildingType.GUARDIAN_TOWER,game.getGuardianTowerVisionRange(),game.getGuardianTowerAttackRange(),game.getGuardianTowerDamage(),0,0),
                new Building(124215, 2629.3396796483976, 350.0, 0, 0, 0, enemyFaction,game.getGuardianTowerRadius(),(int)StrictMath.round(game.getGuardianTowerLife()),(int)StrictMath.round(game.getGuardianTowerLife()), self.getStatuses(),BuildingType.GUARDIAN_TOWER,game.getGuardianTowerVisionRange(),game.getGuardianTowerAttackRange(),game.getGuardianTowerDamage(),0,0),
                new Building(124215, 3650.0, 2343.2513553373133, 0, 0, 0, enemyFaction,game.getGuardianTowerRadius(),(int)StrictMath.round(game.getGuardianTowerLife()),(int)StrictMath.round(game.getGuardianTowerLife()), self.getStatuses(),BuildingType.GUARDIAN_TOWER,game.getGuardianTowerVisionRange(),game.getGuardianTowerAttackRange(),game.getGuardianTowerDamage(),0,0),
                new Building(124215, 3950.0, 1306.7422221916638, 0, 0, 0, enemyFaction,game.getGuardianTowerRadius(),(int)StrictMath.round(game.getGuardianTowerLife()),(int)StrictMath.round(game.getGuardianTowerLife()), self.getStatuses(),BuildingType.GUARDIAN_TOWER,game.getGuardianTowerVisionRange(),game.getGuardianTowerAttackRange(),game.getGuardianTowerDamage(),0,0),
                new Building(124215, 3600.0, 400.0, 0, 0, 0, enemyFaction,game.getFactionBaseRadius(),(int)StrictMath.round(game.getFactionBaseLife()),(int)StrictMath.round(game.getFactionBaseLife()), self.getStatuses(),BuildingType.FACTION_BASE,game.getFactionBaseVisionRange(),game.getFactionBaseAttackRange(),game.getFactionBaseDamage(),0,0),
            };
        }
    }
    
    private void initTick(Wizard self, World world, Game game, Move move)
    {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        
        checkMinionsInAgre();
        
        enemyBuildings = getEnemyBuildings();
        enemyMinions = getEnemyMinions();
        
        calcTreesPotentials();
        calcPotentials();
        isEnemiesNear = isEnemiesNear();
        
        allUnits = new ArrayList<>();
        allUnits.addAll(Arrays.asList(world.getBuildings()));
        allUnits.addAll(Arrays.asList(world.getMinions()));
        allUnits.addAll(Arrays.asList(world.getWizards()));
        allUnits.addAll(Arrays.asList(world.getTrees()));
        allUnits.remove(self);
    }
    
    private void calcTreesPotentials()
    {
        if (world.getTickIndex() % 100 == 0) {
            Tree[] trees = world.getTrees();
            TreeField[] fields = new TreeField[trees.length];
            for (int i = 0, l = trees.length; i < l; i++) {
                fields[i] = new TreeField(trees[i]);
            }
            for (int x = 0; x < POTENTIAL_GRID_SIZE; x++) {
                for (int y = 0; y < POTENTIAL_GRID_SIZE; y++) {
                    treesPotentialGrid[x][y] = 0.0;
                    for (TreeField field : fields) {
                        treesPotentialGrid[x][y] += field.getValue(x, y);
                    }
                }
            }
        }
    }
    
    private void checkMinionsInAgre()
    {
        Set<Long> aliveIds = new HashSet<>(10);
        for (Minion minion : world.getMinions()) {
            if (minion.getFaction() == Faction.NEUTRAL) {
                if (StrictMath.abs(minion.getSpeedX()) > 0.0 || StrictMath.abs(minion.getSpeedY()) > 0.0 || minion.getLife() < minion.getMaxLife()) {
                    if (neutralMinionsInAgre.isEmpty() || !neutralMinionsInAgre.contains(minion.getId())) {
                        neutralMinionsInAgre.add(minion.getId());
                    }
                }
                aliveIds.add(minion.getId());
            }
        }
        Set<Long> diedIds = new HashSet<>(10);        
        neutralMinionsInAgre.stream().filter((id) -> (!aliveIds.contains(id))).forEachOrdered((id) -> {
            diedIds.add(id);
        });
        neutralMinionsInAgre.removeAll(diedIds);
    }
    
    private void calcPotentials()
    {
        List<PotentialField> fields = new ArrayList<>(20);
        
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> (!wizard.isMe() && self.getDistanceTo(wizard) < self.getVisionRange()*2.0)).forEachOrdered((wizard) -> {
            fields.add(new WizardField(wizard, self));
        });
        List<Minion> minions = new ArrayList<>();
        enemyMinions.stream().filter((minion) -> (self.getDistanceTo(minion) < self.getVisionRange()*2.0)).forEach((minion) -> {
            minions.add(minion);
        });
        Arrays.asList(world.getMinions()).stream().filter((minion) -> (minion.getFaction() == self.getFaction() && self.getDistanceTo(minion) < self.getVisionRange()*2.0)).forEachOrdered((minion) -> {
            minions.add(minion);
        });        
        minions.stream().filter((minion) -> (minion.getDistanceTo(self) < self.getVisionRange())).forEachOrdered((minion) -> {
            fields.add(new MinionField(minion, self));
        });
        enemyBuildings.stream().filter((building) -> (building.getDistanceTo(self) < building.getAttackRange()*2.0)).forEachOrdered((building) -> {
            fields.add(new EnemyTowerField(building, self));
        });
        Arrays.asList(world.getProjectiles()).stream().filter((bullet) -> (bullet.getFaction() != self.getFaction() && self.getDistanceTo(bullet) < self.getVisionRange()*2.0)).forEachOrdered((bullet) -> {
            fields.add(new BulletField(bullet, self));
        });
        
        int startX = (int)(self.getX()-self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int startY = (int)(self.getY()-self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int endX = (int)(self.getX()+self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        int endY = (int)(self.getY()+self.getVisionRange())/POTENTIAL_GRID_COL_SIZE;
        startX = (startX < 0) ? 0 : startX;
        startY = (startY < 0) ? 0 : startY;
        endX = (endX >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endX;
        endY = (endY >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endY;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                potentialGrid[x][y] = 0.0;
                double max = -Double.MAX_VALUE;
                for (PotentialField field : fields) {
                    double value = field.getValue(x, y);
                    if (value > 0)  {
                        if (value > max) {
                            max = value;
                        }
                    } else {
                        potentialGrid[x][y] += value;
                    }
                }
                if (max > -Double.MAX_VALUE+1) {
                    potentialGrid[x][y] += max;
                }
                
                potentialGrid[x][y] += staticPotentialGrid[x][y];
                potentialGrid[x][y] += treesPotentialGrid[x][y];
            }            
        }
    }
    
    
    private List<Building> getEnemyBuildings()
    {
        
        List<Building> buildings = new ArrayList();        
        for (Building building : world.getBuildings()) {
            if (building.getFaction() != self.getFaction()) {
                buildings.add(building);
            }
        }
        
        boolean[] buildingsIn = {
            false, false, false, false, false, false, false
        };
        for (Building building : buildings) {
            //2070.7106781186544:1600.0
            if (StrictMath.round(building.getX()) == 2071 && StrictMath.round(building.getY()) == 1600) {
                buildingsIn[0] = true;
            }
            //3097.386941332821:1231.9023805485247
            if (StrictMath.round(building.getX()) == 3097 && StrictMath.round(building.getY()) == 1232) {
                buildingsIn[1] = true;
            }
            //1687.8740025771563:50.0
            if (StrictMath.round(building.getX()) == 1688 && StrictMath.round(building.getY()) == 50) {
                buildingsIn[2] = true;
            }
            //2629.3396796483976:350.0
            if (StrictMath.round(building.getX()) == 2629 && StrictMath.round(building.getY()) == 350) {
                buildingsIn[3] = true;
            }
            //3650.0:2343.2513553373133
            if (StrictMath.round(building.getX()) == 3650 && StrictMath.round(building.getY()) == 2343) {
                buildingsIn[4] = true;
            }
            //3950.0:1306.7422221916638
            if (StrictMath.round(building.getX()) == 3950 && StrictMath.round(building.getY()) == 1307) {
                buildingsIn[5] = true;
            }
            //3600.0:400.0
            if (StrictMath.round(building.getX()) == 3600 && StrictMath.round(building.getY()) == 400) {
                buildingsIn[6] = true;
            }
        }
        
        for (int i = 0; i < 7; i++) {
            if (!buildingsIn[i] && !buildingsDestroyed[i]) {
                if (self.getDistanceTo(fakeBuildings[i]) > self.getVisionRange()) {
                    buildings.add(fakeBuildings[i]);
                } else {
                    buildingsDestroyed[i] = true;
                }
            }
        }
        return buildings;
    }
    
    private List<Minion> getEnemyMinions()
    {
        List<Minion> minions = new ArrayList<>();
        Arrays.asList(world.getMinions()).stream().filter((minion) -> (minion.getFaction() == enemyFaction || neutralMinionsInAgre.contains(minion.getId()))).forEachOrdered((minion) -> {
            minions.add(minion);
        });
        return minions;
    }
}
