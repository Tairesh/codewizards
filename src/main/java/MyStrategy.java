import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Collections;
import javafx.util.Pair;
import model.*;

public final class MyStrategy implements Strategy {
    
    private final IVisualClient debug = new EmptyVisualClient();
    private final boolean debugEnabled = false;
    private final PathFinder pathFinder = PathFinder.getInstance();
    private final GlobalMap globalMap = GlobalMap.getInstance();
    private Random random;
    
    private Wizard self;
    public static World world;
    public static Game game;
    private Move move;
    
    private static final double PSEUDO_SAFE_POTENTIAL = -300.0;
    
    public static final int POTENTIAL_GRID_COL_SIZE = 25;
    public static int POTENTIAL_GRID_SIZE;
    public static double[][] potentialGrid;
    private double[][] staticPotentialGrid;
    private double[][] treesPotentialGrid;
    private double[][] lanePotentialGrid;
    
    private Faction enemyFaction;
    
    private final boolean[] buildingsDestroyed = {
    //    м1     м2     т1     т2     б1     б2   трон
        false, false, false, false, false, false, false
    };    
    private Building[] fakeBuildings;
    private final Set<Long> secondTowers = new HashSet<>(6);
    private Wizard[] fakeWizards;
    private final Set<Long> neutralMinionsInAgre  = new HashSet<>(10);
    
    public static Map<Long,Double> bulletsCache = new HashMap<>(10);
    
    private List<Building> enemyBuildings; // вместе с фейковыми
    private List<Minion> enemyMinions; // вместе с нейтралами в агре
    private List<Wizard> enemyWizards; // вместе с фейковыми
    
    public static List<LivingUnit> allUnits = new ArrayList<>(500);
    private final List<LivingUnit> allUnitsWithoutTrees = new ArrayList<>(200);
    
    private final List<Wizard> alliesWizards = new ArrayList<>(5);
    private final List<Building> alliesBuildings = new ArrayList<>(6);
    private final List<Minion> alliesMinions = new ArrayList<>(50);
    
    private boolean isEnemiesNear;
    private FakeLaneType lane;
    
    private Point2D previousWaypoint;
    private Point2D nextWaypoint;
    
    private int ticksToNextBonus;
    
    private Point2D bonusPoint1;
    private Point2D bonusPoint2;
    private boolean bonus1 = false;
    private boolean bonus2 = false;
    
    private Point selfPoint;
    private Pair<LivingUnit,Integer> bestTargetPair;
    
    private double speedBonus;
    private double maxForwardSpeed;
    private double maxBackwardSpeed;
    private double maxStrafeSpeed;
    
    @Override
    public void move(Wizard self, World world, Game game, Move move)
    {
        initStrategy(self, world, game, move);
        initTick(self, world, game, move);
        
        if (debugEnabled) {
            debug.beginPost();
            debug.fillRect(self.getX()-1.0, self.getY()+self.getRadius()-1.0, self.getX()+200.0, self.getY()+self.getRadius()+66.0, Color.WHITE);
            debug.text(self.getX(), self.getY()+self.getRadius()+5.0, "Next bonus: "+ticksToNextBonus, Color.BLACK);
            debug.text(self.getX(), self.getY()+self.getRadius()+20.0, "Lane: "+lane.name(), Color.BLACK);
            debug.text(self.getX(), self.getY()+self.getRadius()+35.0, "Bonuses: "+bonus1+" "+bonus2, Color.BLACK);
            debug.text(self.getX(), self.getY()+self.getRadius()+50.0, "Level: "+self.getLevel(), Color.BLACK);
            debug.text(self.getX(), self.getY()+self.getRadius()+65.0, "Skille: "+self.getSkills().length, Color.BLACK);
        }
              
        walk();
        shoot();
                
        if (debugEnabled) {
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
                    if (pathFinder.blocked[x][y]) {
                        debug.fillCircle(x*POTENTIAL_GRID_COL_SIZE, y*POTENTIAL_GRID_COL_SIZE, POTENTIAL_GRID_COL_SIZE/2, Color.DARK_GRAY);
                    } else {
                        debug.fillCircle(x*POTENTIAL_GRID_COL_SIZE, y*POTENTIAL_GRID_COL_SIZE, POTENTIAL_GRID_COL_SIZE/2, debugColor(potentialGrid[x][y]));
                    }
                    debug.text(x*POTENTIAL_GRID_COL_SIZE-10.0, y*POTENTIAL_GRID_COL_SIZE+5, ""+(int)potentialGrid[x][y], Color.BLACK);
                }
            }
            debug.fillCircle(selfPoint.x*POTENTIAL_GRID_COL_SIZE, selfPoint.y*POTENTIAL_GRID_COL_SIZE, POTENTIAL_GRID_COL_SIZE/2, Color.LIGHT_GRAY);
            enemyWizards.forEach((wizard) -> {
                debug.circle(wizard.getX(), wizard.getY(), wizard.getRadius()-1.0, Color.PINK);
            });
            enemyMinions.forEach((minion) -> {
                debug.circle(minion.getX(), minion.getY(), minion.getRadius()-1.0, Color.PINK);
            });
            enemyBuildings.forEach((building) -> {
                debug.circle(building.getX(), building.getY(), building.getRadius()-1.0, Color.PINK);
            });
            debug.endPre();
        }
        
    }
    
    private boolean isSecondTower(Building building)
    {
        return secondTowers.contains(building.getId());
    }
    
    private boolean isUndeadable(Building building)
    {
        if (!isSecondTower(building)) {
            return false;
        }
        if (fakeBuildings[1].getDistanceTo(building) < 10.0) {
            return buildingsDestroyed[0];
        }
        if (fakeBuildings[3].getDistanceTo(building) < 10.0) {
            return buildingsDestroyed[2];
        }
        if (fakeBuildings[5].getDistanceTo(building) < 10.0) {
            return buildingsDestroyed[4];
        }
        return false;
    }
    
    private boolean isExistNearLifelessTowersOrWizards()
    {
        if (enemyWizards.stream().anyMatch((wizard) -> (wizard.getDistanceTo(self) < 600.0 && wizard.getLife() < wizard.getMaxLife()*0.35 && !isCrossingTree(wizard)))) {
            return true;
        }
        
        if (enemyBuildings.stream().anyMatch((building) -> (building.getDistanceTo(self) < 750.0 && (building.getLife() < building.getMaxLife()*0.2 || building.getType() == BuildingType.FACTION_BASE) && !isCrossingTree(building)))) {
            return true;
        }
        
        return false;
    }
    
    private void shoot()
    {
        LivingUnit bestTarget = null;
        if (isEnemiesNear) {
            int bestTargetScore = 0;
            if (null != bestTargetPair) {
                bestTarget = bestTargetPair.getKey();
                bestTargetScore = bestTargetPair.getValue();
            } else {
                bestTarget = getNearestEnemy();
            }
            if (null == bestTarget) {
                bestTarget = fakeBuildings[6];
            }
        } else {
            Tree nearestTree = null;
            double minDist = Double.MAX_VALUE;
            for (Tree tree : world.getTrees()) {
                double distance = self.getDistanceTo(tree);
                if (distance < minDist && (distance < self.getRadius() + tree.getRadius() + POTENTIAL_GRID_COL_SIZE || (self.getAngleTo(tree) < game.getStaffSector()/2.0 && distance < 200.0))) {
                    nearestTree = tree;
                    minDist = distance;
                }
            }
            if (null != nearestTree) {
                bestTarget = nearestTree;
            }
        }
        
        if (null != bestTarget) {
            Point2D bestTargetPoint = new Point2D(bestTarget);
            if (bestTarget.getClass() == Minion.class) {
                bestTargetPoint.x += bestTarget.getSpeedX()*5.0;
                bestTargetPoint.y += bestTarget.getSpeedY()*5.0;
            } else if (bestTarget.getClass() == Wizard.class) {
                double maxMove = 4.0*(self.getDistanceTo(bestTarget) / game.getMagicMissileSpeed());
                bestTargetPoint.add(new Vector2D(maxMove/3.0, bestTarget.getAngle()));
            }
            double angle = self.getAngleTo(bestTargetPoint.x,bestTargetPoint.y);
            double distance = self.getDistanceTo(bestTargetPoint.x,bestTargetPoint.y);
            move.setTurn(angle);
            move.setCastAngle(angle);
            
            boolean inAngle = StrictMath.abs(angle) < game.getStaffSector() / 2.0;
            boolean inCastRange = distance <= self.getCastRange()+bestTarget.getRadius()/*+game.getMagicMissileRadius()*/;
            
            boolean learnedFrostbolt = false;
            boolean learnedFireball = false;
            boolean learnedShield = false;
            boolean learnedHaste = false;
            for (SkillType skill : self.getSkills()) {
                switch (skill) {
                    case FROST_BOLT:
                        learnedFrostbolt = true;
                        break;
                    case FIREBALL:
                        learnedFireball = true;
                        break;
                    case SHIELD:
                        learnedShield = true;
                        break;
                    case HASTE:
                        learnedHaste = true;
                        break;
                }
            }
            boolean canMakeAction = self.getRemainingActionCooldownTicks() == 0;
            boolean canThrowMissile = canMakeAction && self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0 && self.getMana() >= game.getMagicMissileManacost();
            boolean canThrowFrostbolt = canMakeAction && learnedFrostbolt && self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()] == 0 && self.getMana() >= game.getFrostBoltManacost();
            boolean canThrowFireball = canMakeAction && learnedFireball && self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()] == 0 && self.getMana() >= game.getFireballManacost();
            boolean canThrowShield = canMakeAction && learnedShield && self.getRemainingCooldownTicksByAction()[ActionType.SHIELD.ordinal()] == 0 && self.getMana() >= game.getShieldManacost();
            boolean canThrowHaste = canMakeAction && learnedHaste && self.getRemainingCooldownTicksByAction()[ActionType.HASTE.ordinal()] == 0 && self.getMana() >= game.getHasteManacost();
            boolean isTargetFrozen = false;
            boolean isTargetBurning = false;
            for (Status status : bestTarget.getStatuses()) {
                switch (status.getType()) {
                    case FROZEN:
                        isTargetFrozen = true;
                        break;
                    case BURNING:
                        isTargetBurning = true;
                        break;
                }
            }
            boolean isMeShielded = false;
            boolean isMeHastened = false;
            for (Status status : self.getStatuses()) {
                switch (status.getType()) {
                    case SHIELDED:
                        isMeShielded = true;
                        break;
                    case HASTENED:
                        isMeHastened = true;
                        break;
                }
            }
            
            if (canThrowShield && !isMeShielded) {
                move.setAction(ActionType.SHIELD);
                move.setStatusTargetId(self.getId());
            } else if (canThrowHaste && !isMeHastened) {
                move.setAction(ActionType.HASTE);
                move.setStatusTargetId(self.getId());
            } else if (canThrowFireball 
                    && inCastRange
                    && inAngle
                    && distance > self.getRadius()+bestTarget.getRadius()+game.getFireballExplosionMinDamageRange()
                    && !isTargetBurning
                    && bestTarget.getLife() > game.getFireballExplosionMaxDamage()
                    && !isCrossingTree(bestTarget, game.getFireballRadius())
                    && ((countNearEnemies(bestTarget) > 1 && bestTarget.getClass() == Minion.class) || bestTarget.getClass() != Minion.class)
                    && bestTarget.getClass() != Tree.class) {
                move.setAction(ActionType.FIREBALL);
                move.setMinCastDistance(distance-bestTarget.getRadius()-game.getFireballRadius());
            } else if (canThrowFrostbolt
                    && inCastRange
                    && inAngle
                    && !isTargetFrozen
//                    && bestTargetScore >= 100
                    && bestTarget.getLife() > game.getFrostBoltDirectDamage()
                    && !isCrossingTree(bestTarget, game.getFrostBoltRadius())
                    && (bestTarget.getClass() == Wizard.class || (bestTarget.getClass() == Minion.class && distance < 100.0))) {
                move.setAction(ActionType.FROST_BOLT);
                move.setMinCastDistance(distance-bestTarget.getRadius()-game.getFrostBoltRadius());
            } else if (StrictMath.max(self.getRemainingActionCooldownTicks(), self.getRemainingCooldownTicksByAction()[ActionType.STAFF.ordinal()]) == 0
                    && distance <= game.getStaffRange()
                    && inAngle) {
                move.setAction(ActionType.STAFF);
            } else if (canThrowMissile
                    && inCastRange
                    && inAngle) {
                move.setAction(ActionType.MAGIC_MISSILE);
                move.setMinCastDistance(distance-bestTarget.getRadius()-game.getMagicMissileRadius());
            }
        }
    }
    
    private int countNearEnemies(LivingUnit target)
    {
        int count = 0;
        count = allUnits.stream().filter((unit) -> !(unit.getFaction() == self.getFaction() || unit.getId() == target.getId() || unit.getClass() == Tree.class)).filter((unit) -> (target.getDistanceTo(unit) < game.getFireballExplosionMinDamageRange())).map((_item) -> 1).reduce(count, Integer::sum);
        return count;
    }
    
    private void walk()
    {
        Point2D targetPoint2D;
        if (isEnemiesNear) {
            LivingUnit bestTarget;
            if (null != bestTargetPair) {
                bestTarget = bestTargetPair.getKey();
            } else {
                bestTarget = getNearestEnemy();
            }
            if (null == bestTarget) {
                bestTarget = fakeBuildings[6];
            }
            
            int nearAlliesWizardsCount = countNearAlliesWizardsIncludeMe();
            int nearEnemyWizardsCount = countNearEnemyWizards();
            
            double distance = self.getDistanceTo(bestTarget);
            double selfPotential = potentialGrid[selfPoint.x][selfPoint.y];
            if (isCurrentLaneToBonus() && self.getLife() > 0.75*self.getMaxLife() && self.getDistanceTo(fakeBuildings[6]) > 1500.0 && self.getDistanceTo(400, 3600) > 1500.0 && !isExistNearLifelessTowersOrWizards()) {
                targetPoint2D = getPathPointToTarget(nextWaypoint);
            } else if (selfPotential < PSEUDO_SAFE_POTENTIAL || self.getLife() < 0.5*self.getMaxLife()) {
                Point safe = getNearestPseudoSafePoint();
                if (safe != null) {
                    targetPoint2D = getPathPointToTarget(safe);
                } else {
                    targetPoint2D = new Point2D(self);
                    targetPoint2D.add(new Vector2D(-10.0, self.getAngle()));
                }
            } else if (self.getLife() < 0.8*self.getMaxLife() && nearAlliesWizardsCount < nearEnemyWizardsCount) {
                debug.line(self.getX(), self.getY(), previousWaypoint.x, previousWaypoint.y, Color.MAGENTA);
                targetPoint2D = getPathPointToTarget(previousWaypoint);
            } else if (distance > self.getCastRange()+bestTarget.getRadius() && self.getRemainingActionCooldownTicks() < 10 && !isCrossing(bestTarget)) { // идти к врагу если он далеко
                targetPoint2D = getPathPointToTarget(Point2D.pointBetween(self, bestTarget));
            } else {
                Point best = getBestPoint();
                if (best != null) {
                    targetPoint2D = getPathPointToTarget(best);
                } else {
                    targetPoint2D = new Point2D(self);
                    targetPoint2D.add(new Vector2D(10.0, self.getAngle()));
                }
            }
        } else {
            targetPoint2D = getPathPointToTarget(getPseudoNextPoint());
        }

        double distance = targetPoint2D.getDistanceTo(self);
        double angle = self.getAngleTo(targetPoint2D.x, targetPoint2D.y);
        
        boolean needStayAroundBonus = ((isCurrentLaneToBonus1() && targetPoint2D.getDistanceTo(bonusPoint1) < 5.0 && !bonus1) || (isCurrentLaneToBonus2() && targetPoint2D.getDistanceTo(bonusPoint2) < 5.0 && !bonus2));
        boolean weAreAroundNextPoint = distance < self.getRadius()+game.getBonusRadius()+5.0;
        
        if (needStayAroundBonus && weAreAroundNextPoint) {
            if (distance < self.getRadius()+game.getBonusRadius()+0.1) {
                move.setSpeed(-10.0);
            }
        } else {
            targetPoint2D = checkForBlocking(targetPoint2D);

            debug.line(self.getX(), self.getY(), targetPoint2D.x, targetPoint2D.y, Color.CYAN);
            debug.circle(targetPoint2D.x, targetPoint2D.y, self.getRadius(), Color.CYAN);
            Vector2D vector = new Vector2D(10.0, angle);
            double speed = vector.getX();
            double strafeSpeed = vector.getY();
            
            double maxSpeed = speed > 0 ? maxForwardSpeed : maxBackwardSpeed;
            if (StrictMath.abs(speed) > maxSpeed || StrictMath.abs(strafeSpeed) > maxStrafeSpeed) {
                double k =  StrictMath.min(maxSpeed / StrictMath.abs(speed), maxStrafeSpeed / StrictMath.abs(strafeSpeed));
                speed *= k;
                strafeSpeed *= k;
            }
            
            move.setSpeed(speed);
            move.setStrafeSpeed(strafeSpeed);
            move.setTurn(angle);
        }
    }
    
    private double getMaxForwardSpeed()
    {
        return game.getWizardForwardSpeed()*speedBonus;
    }
    
    private double getMaxBackwardSpeed()
    {
        return game.getWizardBackwardSpeed()*speedBonus;
    }
    
    private double getMaxStrafeSpeed()
    {
        return game.getWizardStrafeSpeed()*speedBonus;
    }
    
    private double getSpeedBonus()
    {
        int countSkills = 0;
        for (SkillType skill : self.getSkills()) {
            switch (skill) {
                case MOVEMENT_BONUS_FACTOR_PASSIVE_1:
                case MOVEMENT_BONUS_FACTOR_PASSIVE_2:
                case MOVEMENT_BONUS_FACTOR_AURA_1:
                case MOVEMENT_BONUS_FACTOR_AURA_2:
                    countSkills++;
                    break;
            }
        }
        if (countSkills < 4) {
            for(Wizard wizard : alliesWizards) {
                if (wizard.getDistanceTo(self) < game.getAuraSkillRange()) {
                    for (SkillType skill : wizard.getSkills()) {
                        switch (skill) {
                            case MOVEMENT_BONUS_FACTOR_AURA_1:
                            case MOVEMENT_BONUS_FACTOR_AURA_2:
                                countSkills++;
                                break;
                        }
                    }
                }
            }
        }
        
        boolean hastened = false;
        for (Status status : self.getStatuses()) {
            if (status.getType() == StatusType.HASTENED) {
                hastened = true;
                break;
            }
        }
        
        double bonus = (double)countSkills * 0.05 + 1.0;
        if (hastened) {
            bonus += game.getHastenedMovementBonusFactor();
        }
        
        return bonus;
    }
    
    private boolean isBlocked(Point point)
    {
        return pathFinder.blocked[point.x][point.y];
    }
    
    private Point2D checkForBlocking(Point2D point2D)
    {
        Point point = convert2DToPoint(point2D);
        if (isBlocked(point)) {
            return getNearestUnblockedPoint2D(point);
        } else {
            return point2D;
        }
    }
    
    private int countNearAlliesWizardsIncludeMe()
    {
        int counter = 0;
        counter = Arrays.asList(world.getWizards()).stream().filter((wizard) -> (wizard.getFaction() == self.getFaction() && self.getDistanceTo(wizard) < 700.0)).map((_item) -> 1).reduce(counter, Integer::sum);
        return counter;
    }
    
    private int countNearEnemyWizards()
    {
        int counter = 0;
        counter = Arrays.asList(world.getWizards()).stream().filter((wizard) -> (wizard.getFaction() != self.getFaction() && self.getDistanceTo(wizard) < 700.0)).map((_item) -> 1).reduce(counter, Integer::sum);
        return counter;
    }
    
    private Point2D getPseudoNextPoint()
    {
        if (nextWaypoint.getDistanceTo(self) > 500.0) {
            Point2D pseudoNextWaypoint = new Point2D(self);
            pseudoNextWaypoint.add(new Vector2D(500, MyMath.normalizeAngle(self.getAngle() + self.getAngleTo(nextWaypoint.x, nextWaypoint.y))));
            Point pnwp = convert2DToPoint(pseudoNextWaypoint);
            while (pathFinder.blocked[pnwp.x][pnwp.y]) {
                pseudoNextWaypoint.add(new Vector2D(-100.0, MyMath.normalizeAngle(self.getAngle() + self.getAngleTo(nextWaypoint.x, nextWaypoint.y))));
                pnwp = convert2DToPoint(pseudoNextWaypoint);
                if (pseudoNextWaypoint.getDistanceTo(self) < 200.0) {
                    return nextWaypoint;
                }
            }
            return pseudoNextWaypoint;
        }
        return nextWaypoint;
    }
    
    private Point2D convertPointTo2D(Point point)
    {
        return new Point2D(point.x*POTENTIAL_GRID_COL_SIZE, point.y*POTENTIAL_GRID_COL_SIZE);
    }
    
    private Point convert2DToPoint(Point2D point)
    {
        return new Point((int)StrictMath.round(point.x/(double)POTENTIAL_GRID_COL_SIZE),(int)StrictMath.round(point.y/(double)POTENTIAL_GRID_COL_SIZE));
    }
    
    private Point2D getPathPointToTarget(Point2D targetPoint2D)
    {
        if (isCrossing(targetPoint2D)) {
            List<Point> path = pathFinder.getPath(selfPoint, convert2DToPoint(targetPoint2D));
            Point2D point2D = null;
            if (null != path && path.size() > 1) {

                if (debugEnabled) {
                    Point tmp = path.get(0);
                    for (Point point : path) {
                        debug.line(tmp.x*POTENTIAL_GRID_COL_SIZE, tmp.y*POTENTIAL_GRID_COL_SIZE, point.x*POTENTIAL_GRID_COL_SIZE, point.y*POTENTIAL_GRID_COL_SIZE, Color.GREEN);
                        tmp = point;
                    }
                }
                path.remove(0);
                for (Point p : path) {
                    Point2D p2d = convertPointTo2D(p);
                    if (isCrossing(p2d)) {
                        break;
                    }
                    point2D = p2d;
                }
                
                if (point2D != null)
                    return point2D;
            }
            point2D = new Point2D(self.getX(), self.getY());
            point2D.add(new Vector2D(self.getRadius()+5.0, MyMath.normalizeAngle(self.getAngle() + self.getAngleTo(targetPoint2D.x, targetPoint2D.y))));
            if (isCrossing(point2D)) {
                point2D = getNearestUnblockedPoint2D(convert2DToPoint(point2D));
            }
            return point2D;
        }
        return targetPoint2D;
    }
    
    private Point getNearestUnblockedPoint(Point point)
    {
        List<Point> list = new ArrayList<>(120);
        for (int i = StrictMath.max(point.x-20, 0); i < StrictMath.min(point.x+20, POTENTIAL_GRID_SIZE); i++) {
            for (int j = StrictMath.max(point.y-20, 0); j < StrictMath.min(point.y+20, POTENTIAL_GRID_SIZE); j++) {
                if (!pathFinder.blocked[i][j] && !selfPoint.equals(i, j)) {
                    list.add(new Point(i, j));
                }
            }
        }
        return Collections.min(list, (o1, o2) -> {
            double distance1 = point.getDistanceTo(o1);
            double distance2 = point.getDistanceTo(o2);
            if (distance1 == distance2) return 0;
            else return distance1 < distance2 ? -1 : 1;
        });
    }
    
    private Point getNearestUnblockedPoint()
    {
        return getNearestUnblockedPoint(selfPoint);
    }
    
    private Point2D getNearestUnblockedPoint2D(Point point)
    {
        return convertPointTo2D(getNearestUnblockedPoint(point));
    }
    
    private Point2D getNearestUnblockedPoint2D()
    {
        return convertPointTo2D(getNearestUnblockedPoint());
    }
    private Point2D getPathPointToTarget(Point targetPoint)
    {
        return getPathPointToTarget(convertPointTo2D(targetPoint));
    }
    
    private boolean isEnemiesNear()
    {
        if (enemyBuildings.stream().anyMatch((building) -> (self.getDistanceTo(building) <= building.getAttackRange()+self.getRadius()))) {
            return true;
        }
        if (enemyMinions.stream().anyMatch((minion) -> (self.getDistanceTo(minion) <= StrictMath.max(self.getCastRange()+minion.getRadius(), getMinionAttackRange(minion)+self.getRadius())))) {
            return true;
        }        
        if (enemyWizards.stream().anyMatch((wizard) -> (self.getDistanceTo(wizard) <= StrictMath.max(self.getCastRange(), wizard.getCastRange())+game.getWizardRadius()))) {
            return true;
        }
        return false;
    }
    
    private double getMinionAttackRange(Minion minion)
    {
        switch (minion.getType()) {
            case ORC_WOODCUTTER:
                return game.getOrcWoodcutterAttackRange();
            case FETISH_BLOWDART:
                return game.getFetishBlowdartAttackRange();
        }
        
        return 0.0;
    }
    
    private double getMinionAttackSector(Minion minion)
    {
        switch (minion.getType()) {
            case ORC_WOODCUTTER:
                return game.getOrcWoodcutterAttackSector();
            case FETISH_BLOWDART:
                return game.getFetishBlowdartAttackSector();
        }
        
        return 0.0;
    }
    
    private LivingUnit getNearestEnemy()
    {
        double minDist = Double.MAX_VALUE;
        LivingUnit nearestUnit = null;
        
        List<LivingUnit> enemies = new ArrayList<>();
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> (wizard.getFaction() == enemyFaction)).forEach(enemies::add);
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
    
    private Pair<LivingUnit,Integer> getBestTarget()
    {
        
        int maxScore = -1;
        LivingUnit bestTarget = null;
        
        List<LivingUnit> enemies = new ArrayList<>();
        enemies.addAll(enemyWizards);
        enemies.addAll(enemyMinions);
        enemies.addAll(enemyBuildings);
        
        for (LivingUnit unit : enemies) {
            if (self.getDistanceTo(unit) > 600.0) {
                continue;
            }
            
            int score = 0;
            
            if (unit.getLife() <= game.getMagicMissileDirectDamage()) {
                score += 10;
            } else if (unit.getLife() <= game.getMagicMissileDirectDamage()*2.0) {
                score += 5;
            } else if (unit.getLife() < unit.getMaxLife()*0.55) {
                score += 1;
            }
            
            if (unit.getClass() == Wizard.class) {
                score += 100;
            } else if (unit.getClass() == Building.class) {
                score += 10;
            } else if (unit.getClass() == Minion.class) {
                switch (((Minion)unit).getType()) {
                    case ORC_WOODCUTTER:
                        if (unit.getDistanceTo(self)+self.getRadius() < game.getOrcWoodcutterAttackRange()*2.0 /*&& unit.getAngleTo(self) <= game.getOrcWoodcutterAttackSector()/2.0*/) {
                            score += 100;
                        }
                        break;
//                    case FETISH_BLOWDART:
//                        if (unit.getDistanceTo(self)+self.getRadius() < game.getFetishBlowdartAttackRange()*1.5 && unit.getAngleTo(self) <= game.getFetishBlowdartAttackSector()/2.0) {
//                            score += 100;
//                        }
//                        break;
                }
            }
            
            
            
            score += 1 - unit.getLife()/unit.getMaxLife();
            
//            if (unit.getClass() == Wizard.class) {
//                Player p = Arrays.asList(world.getPlayers()).stream().filter((player) -> player.getId() == ((Wizard)unit).getOwnerPlayerId()).findFirst().get();
//                score += (double)p.getScore() / 1000.0;
//            }

            if (isCrossingTree(unit)) {
                score -= 100;
            }
            
            for (Status status : unit.getStatuses()) {
                if (status.getType() == StatusType.FROZEN) {
                    score += 100;
                }
            }
            
            debug.text(unit.getX(), unit.getY()+unit.getRadius()+5.0, ""+score, Color.CYAN);
            if (score > maxScore) {
                maxScore = score;
                bestTarget = unit;
            }
        }
        
        if (bestTarget == null) {
            return null;
        }
        return new Pair<>(bestTarget, maxScore);
    }
    
    private double getPointValueWithNeighbours(Point point)
    {
        double value = potentialGrid[point.x][point.y];
        if (point.x > 0) {
            value += potentialGrid[point.x-1][point.y];
        }
        if (point.x < POTENTIAL_GRID_SIZE-1) {
            value += potentialGrid[point.x+1][point.y];
        }
        if (point.y > 0) {
            value += potentialGrid[point.x][point.y-1];
        }
        if (point.y < POTENTIAL_GRID_SIZE-1) {
            value += potentialGrid[point.x][point.y+1];
        }
        if (point.x > 0 && point.y > 0) {
            value += potentialGrid[point.x-1][point.y-1];
        }
        if (point.x < POTENTIAL_GRID_SIZE-1 && point.y > 0) {
            value += potentialGrid[point.x+1][point.y-1];
        }
        if (point.x > 0 && point.y < POTENTIAL_GRID_SIZE-1) {
            value += potentialGrid[point.x-1][point.y+1];
        }
        if (point.x < POTENTIAL_GRID_SIZE-1 && point.y < POTENTIAL_GRID_SIZE-1) {
            value += potentialGrid[point.x+1][point.y+1];
        }
        
        return value;
    }
    
    private Point lastNearestPseudoSafePoint = null;
    private int lastNearestPseudoSafePointTick = 0;
    
    private Point getNearestPseudoSafePoint()
    {   
        if (null != lastNearestPseudoSafePoint && world.getTickIndex() - lastNearestPseudoSafePointTick < 20 && !isBlocked(lastNearestPseudoSafePoint) && potentialGrid[lastNearestPseudoSafePoint.x][lastNearestPseudoSafePoint.y] > PSEUDO_SAFE_POTENTIAL) {
            return lastNearestPseudoSafePoint;
        }
        
        double minDist = Double.POSITIVE_INFINITY;
        int[] coords = {-1,-1};
                
        int startX = (int)(self.getX()-self.getVisionRange()/2.0)/POTENTIAL_GRID_COL_SIZE;
        int startY = (int)(self.getY()-self.getVisionRange()/2.0)/POTENTIAL_GRID_COL_SIZE;
        int endX = (int)(self.getX()+self.getVisionRange()/2.0)/POTENTIAL_GRID_COL_SIZE;
        int endY = (int)(self.getY()+self.getVisionRange()/2.0)/POTENTIAL_GRID_COL_SIZE;
        startX = (startX < 0) ? 0 : startX;
        startY = (startY < 0) ? 0 : startY;
        endX = (endX >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endX;
        endY = (endY >= POTENTIAL_GRID_SIZE) ? POTENTIAL_GRID_SIZE-1 : endY;
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (!pathFinder.blocked[x][y] && potentialGrid[x][y] > PSEUDO_SAFE_POTENTIAL) {
                    double dist = selfPoint.getDistanceTo(x, y);
                    if (dist < minDist) {
                        minDist = dist;
                        coords[0] = x;
                        coords[1] = y;
                    }
                }
            }
        }
        
        if (coords[0] >=0 && coords[1] >= 0) {
            lastNearestPseudoSafePointTick = world.getTickIndex();
            lastNearestPseudoSafePoint = new Point(coords[0], coords[1]);
            return lastNearestPseudoSafePoint;
        }
                
        return null;
    }
    
    private Point getBestPoint()
    {
        List<Point> list = getBestPoints();
        Point best = null;
        double minDist = Double.MAX_VALUE;
        for (Point point : list) {
            double dist = selfPoint.getDistanceTo(point);
            if (dist < minDist) {
                best = point;
                minDist = dist;
            }
        }
        return best;
    }
    
    private List<Point> getBestPoints(int n)
    {
        List<Point> points = new ArrayList<>(POTENTIAL_GRID_SIZE*POTENTIAL_GRID_SIZE);
        
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
                if (!pathFinder.blocked[x][y]) {
                    points.add(new Point(x,y));
                }
            }
        }
        
        points.sort((o1, o2) -> {
            double val1 = getPointValueWithNeighbours(o1);
            double val2 = getPointValueWithNeighbours(o2);
            if (val1 == val2) return 0;
            else return val1 > val2 ? -1 : 1;
        });

        List<Point> list = new ArrayList<>(n);
        for (Point point : points){
            if (!isCrossing(convertPointTo2D(point))) {
                list.add(point);
                if (list.size() == n) {
                    break;
                }
            }
        }
        return list;
    }
    
    private List<Point> getBestPoints()
    {
        return getBestPoints(10);
    }
        
    private Color debugColor(double value)
    {
        int val = (int)StrictMath.round(value/2.0);
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
            
            bonusPoint1 = new Point2D(game.getMapSize()*0.3, game.getMapSize()*0.3);
            bonusPoint2 = new Point2D(game.getMapSize()*0.7, game.getMapSize()*0.7);
            
            POTENTIAL_GRID_SIZE = (int)game.getMapSize()/POTENTIAL_GRID_COL_SIZE;
            potentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            staticPotentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            treesPotentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            lanePotentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            
            // заполнение статического поля потенциалов
            double value = -10.0;
            for (int x = 0; x < POTENTIAL_GRID_SIZE; x++) {
                for (int y = POTENTIAL_GRID_SIZE-1; y >= 0; y--) {                    
                    if (x == 0 || y == 0 || x == POTENTIAL_GRID_SIZE-1 || y == POTENTIAL_GRID_SIZE-1) {
                        staticPotentialGrid[x][y] = -500.0;
                    } else if (x+y < 20) {
                        staticPotentialGrid[x][y] = -30.0*(20-x-y);
                    } else if ((POTENTIAL_GRID_SIZE-x)+(POTENTIAL_GRID_SIZE-y) < 20) {
                        staticPotentialGrid[x][y] = -30.0*(20-(POTENTIAL_GRID_SIZE-x)+(POTENTIAL_GRID_SIZE-y));
                    } else {
                        staticPotentialGrid[x][y] = value;
                    } 
                    value += 10.0 / POTENTIAL_GRID_SIZE;
                }
                value -= 10.0 - (10.0 / POTENTIAL_GRID_SIZE);
            }
                                    
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
          
            secondTowers.add(15L);
            secondTowers.add(16L);
            secondTowers.add(18L);
            secondTowers.add(20L);
            secondTowers.add(22L);
            secondTowers.add(24L);
            
            fakeWizards = (self.getId() < 6) ? new Wizard[]{
                new Wizard(6, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(7, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(8, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(9, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(10, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
            } : new Wizard[]{
                new Wizard(1, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(2, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(3, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(4, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
                new Wizard(5, 3700, 300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), game.getWizardVisionRange(), game.getWizardCastRange(), 0, 1, new SkillType[]{}, 0, new int[]{0,0,0,0,0}, false, new Message[]{}),
            };            
            
            switch ((int) self.getId()) {
                case 1:
                case 2:
                case 6:
                case 7:
                    lane = FakeLaneType.TOP;
                    break;
                case 3:
                case 8:
                    lane = FakeLaneType.MIDDLE;
                    break;
                case 4:
                case 5:
                case 9:
                case 10:
                    lane = FakeLaneType.BOTTOM;
                    break;
                default:
            }
        }
    }
    
    private void initTick(Wizard _self, World _world, Game _game, Move _move)
    {
        self = _self;
        world = _world;
        game = _game;
        move = _move;
        
        selfPoint = new Point((int)StrictMath.round(self.getX()/(double)POTENTIAL_GRID_COL_SIZE), (int)StrictMath.round(self.getY()/(double)POTENTIAL_GRID_COL_SIZE));
                
        changeLaneToBest();
        ticksToNextBonus = game.getBonusAppearanceIntervalTicks() - (world.getTickIndex() % game.getBonusAppearanceIntervalTicks()) - 1;
        if (game.getTickCount() - world.getTickIndex() < game.getBonusAppearanceIntervalTicks() / 2.0) {
            ticksToNextBonus = Integer.MAX_VALUE;
        }
        if (!isCurrentLaneToBonus() && (bonus1 || bonus2)) {
            if (bonus1 && bonus2) {
                if (bonusPoint1.getDistanceTo(self) < bonusPoint2.getDistanceTo(self)) {
                    changeLaneToBonus1();
                } else {
                    changeLaneToBonus2();
                }
            } else if (bonus1) {
                changeLaneToBonus1();
            } else {
                changeLaneToBonus2();
            }
        }
        checkBonuses();
        if (ticksToNextBonus < 1000 || bonus1 || bonus2) {
            double bonusTimeFactor = 2.5;
            double bonus1distance = bonusPoint1.getDistanceTo(self);
            double bonus2distance = bonusPoint2.getDistanceTo(self);
            if (bonus1 && bonus2) {
            	if (bonus1distance < bonus2distance) {
                    changeLaneToBonus1();
            	} else {
                    changeLaneToBonus2();
                }
            } else if (bonus1) {
            	changeLaneToBonus1();
            } else if (bonus2) {
            	changeLaneToBonus2();
            } else {
            	boolean pora1 = ticksToNextBonus < bonus1distance/bonusTimeFactor;
            	boolean pora2 = ticksToNextBonus < bonus2distance/bonusTimeFactor;
            	if (pora1 && bonus1distance < bonus2distance) {
                    changeLaneToBonus1();
                } else if (pora2 && bonus2distance < bonus1distance) {
                    changeLaneToBonus2();
                }
            } 
        }
        checkLane();
        
        checkBulletsCache();
        
        previousWaypoint = globalMap.getPreviousWayPoint(lane, self);
        nextWaypoint = globalMap.getNextWayPoint(lane, self);
        
        checkMinionsInAgre();
        checkEnemyWizards();
        
        enemyBuildings = getEnemyBuildings();
        enemyMinions = getEnemyMinions();
        enemyWizards = getEnemyWizards();
        
        allUnits.clear();
        allUnitsWithoutTrees.clear();
        allUnits.addAll(Arrays.asList(world.getBuildings()));
        allUnitsWithoutTrees.addAll(Arrays.asList(world.getBuildings()));
        allUnits.addAll(Arrays.asList(world.getMinions()));
        allUnitsWithoutTrees.addAll(Arrays.asList(world.getMinions()));
        allUnits.addAll(Arrays.asList(world.getTrees()));
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> !wizard.isMe()).forEach(allUnits::add);
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> !wizard.isMe()).forEach(allUnitsWithoutTrees::add);
                
        alliesWizards.clear();
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> !wizard.isMe() && wizard.getFaction() == self.getFaction()).forEach(alliesWizards::add);
        alliesBuildings.clear();
        Arrays.asList(world.getBuildings()).stream().filter((building) -> building.getFaction() == self.getFaction()).forEach(alliesBuildings::add);
        alliesMinions.clear();
        Arrays.asList(world.getMinions()).stream().filter((minion) -> minion.getFaction() == self.getFaction()).forEach(alliesMinions::add);
        
        isEnemiesNear = isEnemiesNear();
        
        updateBlockedTiles();
        if (isEnemiesNear) {
            calcTreesPotentials();
            calcLanePotentials();
            calcPotentials();
            bestTargetPair = getBestTarget();
        } else {
            bestTargetPair = null;
        }
        
        learnSkills();

        speedBonus = getSpeedBonus();
        maxForwardSpeed = getMaxForwardSpeed();
        maxBackwardSpeed = getMaxBackwardSpeed();
        maxStrafeSpeed = getMaxStrafeSpeed();
        
    }
    
    private void changeLaneToBest()
    {
        if (world.getTickIndex() % 50 == 0 && self.getDistanceTo(400, 3600) < 700.0) {
            lane = getBestLane();
        }
    }
    
    private void updateBlockedTiles()
    {
        pathFinder.blocked = new boolean[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
        for (int i = 0; i < POTENTIAL_GRID_SIZE; i++) {
            int distance = 0;
            int j = 0;
            while(distance <= self.getRadius()) {
                pathFinder.blocked[i][j] = true;
                pathFinder.blocked[j][i] = true;
                pathFinder.blocked[i][POTENTIAL_GRID_SIZE-1-j] = true;
                pathFinder.blocked[POTENTIAL_GRID_SIZE-1-j][i] = true;
                distance += POTENTIAL_GRID_COL_SIZE;
                j++;
            }
        }
        allUnitsWithoutTrees.stream().forEach((unit) -> {
            blockTilesByUnit(unit);
        });
        if (world.getTickIndex() % 50 == 0) {
            pathFinder.treesBlocked = new boolean[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
            for (Tree tree : world.getTrees()) {
                blockTilesByUnit(tree, true);
            }
        }
        for (int x = 0; x < POTENTIAL_GRID_SIZE; x++) {
            for (int y = 0; y < POTENTIAL_GRID_SIZE; y++) {
                pathFinder.blocked[x][y] |= pathFinder.treesBlocked[x][y];
            }
        }
    }
    
    private void blockTilesByUnit(LivingUnit unit, boolean isTree)
    {
        Point unitPoint = convert2DToPoint(new Point2D(unit));
        double r = unit.getRadius()+self.getRadius()+1.0;
        for (int i = StrictMath.max(unitPoint.x-(int)r,0); i <= StrictMath.min(unitPoint.x+(int)r,POTENTIAL_GRID_SIZE-1); i++) {
            for (int j = StrictMath.max(unitPoint.y-(int)r,0); j <= StrictMath.min(unitPoint.y+(int)r,POTENTIAL_GRID_SIZE-1); j++) {
                if ((unitPoint.getDistanceTo(i, j)-0.5)*POTENTIAL_GRID_COL_SIZE < r) {
                    if (isTree) {
                        pathFinder.treesBlocked[i][j] = true;
                    } else {
                        pathFinder.blocked[i][j] = true;
                    }
                }
            }                
        }
    }
    
    private void blockTilesByUnit(LivingUnit unit)
    {
        blockTilesByUnit(unit, false);
    }
    
    private void checkBulletsCache()
    {
        Projectile[] bullets = world.getProjectiles();
        Set<Long> aliveIds = new HashSet<>(bullets.length);
        for (Projectile bullet : bullets) {
            double speed = StrictMath.hypot(bullet.getSpeedX(), bullet.getSpeedY());
            double distance = 0.0;
            if (bulletsCache.containsKey(bullet.getId())) {
                distance = bulletsCache.get(bullet.getId());
            }
            bulletsCache.put(bullet.getId(), distance + speed);
            aliveIds.add(bullet.getId());
        }
        
        Set<Long> diedIds = new HashSet<>(bullets.length);
        bulletsCache.keySet().stream().filter((id) -> (!aliveIds.contains(id))).forEach((id) -> {
            diedIds.add(id);
        });
        
        diedIds.forEach((id) -> {
            bulletsCache.remove(id);
        });
        
    }
    
    private void learnSkills()
    {
        if (!game.isSkillsEnabled()) {
            return;
        }
        
        int learned = self.getSkills().length;
        boolean canLearn = self.getLevel() - learned > 0;
        if (canLearn) {
            move.setSkillToLearn(SkillsOrdered.STACK[learned]);
        }
    }
    
    private boolean anyoneSee(Point2D point)
    {
        if (point.getDistanceTo(self) < self.getVisionRange()) {
            return true;
        }
        for (Wizard wizard : alliesWizards) {
            if (point.getDistanceTo(wizard) < wizard.getVisionRange()) {
                return true;
            }
        }
        for (Building building : alliesBuildings) {
            if (point.getDistanceTo(building) < building.getVisionRange()) {
                return true;
            }
        }
        for (Minion minion : alliesMinions) {
            if (point.getDistanceTo(minion) < minion.getVisionRange()) {
                return true;
            }
        }
        return false;
    }
    
    private boolean anyoneSee(Unit unit)
    {
        return anyoneSee(new Point2D(unit));
    }
    
    private void checkBonuses()
    {
        if (world.getTickIndex() < 2000) return;
        
        if (ticksToNextBonus == game.getBonusAppearanceIntervalTicks()-2) {
            bonus1 = true;
            bonus2 = true;
        }
        boolean[] bonusExist = new boolean[]{false, false};
        for (Bonus bonus : world.getBonuses()) {
            if (bonus.getX() < game.getMapSize()*0.5) {
                bonusExist[0] = true;
                bonus1 = true;
            } else {
                bonusExist[1] = true;
                bonus2 = true;
            }
        }
        
        if (bonus1 || bonus2) {
            if (bonus1 && !bonusExist[0] && anyoneSee(bonusPoint1)) {
                bonus1 = false;
            }
            if (bonus2 && !bonusExist[1] && anyoneSee(bonusPoint2)) {
                bonus2 = false;
            }
        }
    }
    
    private void checkLane()
    {
        if (isCurrentLaneToBonus() && ticksToNextBonus >= 1000 && ticksToNextBonus < game.getBonusAppearanceIntervalTicks()-2) {
            if ((!bonus1 && isCurrentLaneToBonus1()) || (!bonus2 && isCurrentLaneToBonus2())) {
                changeLaneFromBonus();
            }
        }
        if (isCurrentLaneFromBonus()) {
            if (self.getX() < 750.0 && self.getY() < 750.0) {
                lane = FakeLaneType.TOP;
            }
            if (self.getX() > world.getWidth() - 750.0 && self.getY() > world.getWidth() - 750.0) {
                lane = FakeLaneType.BOTTOM;
            }
            if (self.getX() > world.getWidth()/2.0-375.0 && self.getX() < world.getWidth()/2.0+375.0 && self.getY() > world.getHeight()/2.0-500.0 && self.getY() < world.getHeight()/2.0+500.0) {
                lane = FakeLaneType.MIDDLE;
            }
        }
    }
    
    private boolean isCurrentLaneFromBonus()
    {
        switch (lane) {
            case BONUS1_TO_TOP:
            case BONUS1_TO_MIDDLE:
            case BONUS2_TO_MIDDLE:
            case BONUS2_TO_BOTTOM:
                return true;
        }
        return false;
    }
    
    private void changeLaneFromBonus()
    {
        switch (lane) {
            case TOP_TO_BONUS1:
                lane = FakeLaneType.BONUS1_TO_TOP;
                break;
            case MIDDLE_TO_BONUS1:
                lane = FakeLaneType.BONUS1_TO_MIDDLE;
                break;
            case BOTTOM_TO_BONUS2:                
                lane = FakeLaneType.BONUS2_TO_BOTTOM;
                break;
            case MIDDLE_TO_BONUS2:
                lane = FakeLaneType.BONUS2_TO_MIDDLE;
                break;
        }
    }
    
    private FakeLaneType getBestLane()
    {
        int[] buildingsCount = {0,0,0};
        alliesBuildings.forEach((building) -> {
            if (building.getY() > 3500) {
                buildingsCount[2]++;
            } else if (building.getX() < 500) {
                buildingsCount[0]++;
            } else {
                buildingsCount[1]++;
            }
        });
        if (buildingsCount[1] == 0) {
            return FakeLaneType.MIDDLE;
        }
        if (buildingsCount[0] == 0) {
            return FakeLaneType.TOP;
        }
        if (buildingsCount[2] == 0) {
            return FakeLaneType.BOTTOM;
        }
        
        int[] wizardsCount = {0,0,0};
        alliesWizards.forEach((wizard) -> {
            if (Math.abs(wizard.getX()+wizard.getY() - 4000) <= 400 && wizard.getX() > 500 && wizard.getY() < 3500) {
                wizardsCount[1]++;
            } else if (wizard.getX() < 800 || wizard.getY() < 800) {
                wizardsCount[0]++;
            } else if (wizard.getX() > 3200 || wizard.getY() > 3200) {
                wizardsCount[2]++;
            }
        });
        if (wizardsCount[1] == 0) {
            return FakeLaneType.MIDDLE;
        }
        if (wizardsCount[0] == 0) {
            return FakeLaneType.TOP;
        }
        if (wizardsCount[2] == 0) {
            return FakeLaneType.BOTTOM;
        }
        
        int[] sumCount = {wizardsCount[0]+buildingsCount[0],wizardsCount[1]+buildingsCount[1],wizardsCount[2]+buildingsCount[2]};
        if (sumCount[0] < sumCount[1] && sumCount[0] < sumCount[2]) {
            return FakeLaneType.TOP;
        } else if (sumCount[2] < sumCount[1] && sumCount[2] < sumCount[0]) {
            return FakeLaneType.BOTTOM;
        } else {
            return FakeLaneType.MIDDLE;
        }
    }
        
    private boolean isCurrentLaneToBonus1()
    {
        switch (lane) {
            case TOP_TO_BONUS1:
            case MIDDLE_TO_BONUS1:
                return true;
        }
        return false;
    }
    
    private boolean isCurrentLaneToBonus2()
    {
        switch (lane) {
            case BOTTOM_TO_BONUS2:
            case MIDDLE_TO_BONUS2:
                return true;
        }
        return false;
    }
    
    private boolean isCurrentLaneToBonus()
    {
        return isCurrentLaneToBonus1() || isCurrentLaneToBonus2();
    }
    
    private void changeLaneToBonus1()
    {
        switch (lane) {
            case TOP:
            case BONUS1_TO_TOP:
                if (self.getX() > 3000.0) {
//                    lane = FakeLaneType.ENEMYBASE_TO_TOP_BONUS1;
                } else {
                    lane = FakeLaneType.TOP_TO_BONUS1;
                }
                break;
            case MIDDLE:
            case BONUS1_TO_MIDDLE:
            case BONUS2_TO_MIDDLE:
                if (self.getX() > 3000.0 && self.getY() < game.getMapSize() - 3000.0) {
//                    lane = FakeLaneType.ENEMYBASE_TO_MIDDLE_BONUS1;
                } else {
                    lane = FakeLaneType.MIDDLE_TO_BONUS1;
                }
                break;
        }
    }
    
    private void changeLaneToBonus2()
    {
        switch (lane) {
            case BOTTOM:
            case BONUS2_TO_BOTTOM:
                lane = FakeLaneType.BOTTOM_TO_BONUS2;
                break;
            case MIDDLE:
            case BONUS1_TO_MIDDLE:
            case BONUS2_TO_MIDDLE:
                lane = FakeLaneType.MIDDLE_TO_BONUS2;
                break;
        }
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
    
    private void calcLanePotentials()
    {
        if (world.getTickIndex() % 10 == 0) {
            
            Point prev = convert2DToPoint(previousWaypoint); 
            Point next = convert2DToPoint(nextWaypoint);
            
            lanePotentialGrid = new double[POTENTIAL_GRID_SIZE][POTENTIAL_GRID_SIZE];
//            
            for (int x = StrictMath.max(next.x-20,0); x < StrictMath.min(next.x+20,POTENTIAL_GRID_SIZE); x++) {
                for (int y = StrictMath.max(next.y-20,0); y < StrictMath.min(next.y+20,POTENTIAL_GRID_SIZE); y++) {
                    double dist = next.getDistanceTo(x, y);
                    lanePotentialGrid[x][y] = (30.0 - dist);
                }
            }
            double prevKoeff = self.getMaxLife()/self.getLife();
            for (int x = StrictMath.max(prev.x-20,0); x < StrictMath.min(prev.x+20,POTENTIAL_GRID_SIZE); x++) {
                for (int y = StrictMath.max(prev.y-20,0); y < StrictMath.min(prev.y+20,POTENTIAL_GRID_SIZE); y++) {
                    double dist = prev.getDistanceTo(x, y);
                    lanePotentialGrid[x][y] += (30.0 - dist)*prevKoeff;
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
        neutralMinionsInAgre.stream().filter((id) -> (!aliveIds.contains(id))).forEach((id) -> {
            diedIds.add(id);
        });
        neutralMinionsInAgre.removeAll(diedIds);
    }
    
    private void checkEnemyWizards()
    {
        Set<Long> aliveIds = new HashSet<>(5);
        for (Wizard wizard : world.getWizards()) {
            if (wizard.getFaction() == enemyFaction) {
                int i = (int)wizard.getId() - 1;
                if (i > 4) {
                    i -= 5;
                }
                fakeWizards[i] = wizard;
                aliveIds.add(wizard.getId());
            }
        }
        for (Wizard wizard : fakeWizards) {
            if (!aliveIds.contains(wizard.getId())) {
                int i = (int)wizard.getId() - 1;
                if (i > 4) {
                    i -= 5;
                }
                fakeWizards[i] = new Wizard(wizard.getId(), 4300, -300, 0, 0, 0, enemyFaction, game.getWizardRadius(), game.getWizardBaseLife(), game.getWizardBaseLife(), new Status[]{}, 12345, false, game.getWizardBaseMana(), game.getWizardBaseMana(), wizard.getVisionRange(), wizard.getCastRange(), 0, 1, wizard.getSkills(), 0, new int[]{0,0,0,0,0}, false, new Message[]{});
            }
        }
        
    }
    
    private void calcPotentials()
    {
        List<PotentialField> fields = new ArrayList<>(20);
        
        List<Wizard> wizards = new ArrayList<>(10);
        wizards.addAll(enemyWizards);
        for (Wizard wizard : world.getWizards()) {
            if (!wizard.isMe() && wizard.getFaction() != enemyFaction) {
                wizards.add(wizard);
            }
        }
        wizards.stream().filter((wizard) -> (!wizard.isMe() && self.getDistanceTo(wizard) < self.getVisionRange()*2.0)).forEach((wizard) -> {
            fields.add(new WizardField(wizard, self));
        });
        List<Minion> minions = new ArrayList<>();
        enemyMinions.stream().filter((minion) -> (self.getDistanceTo(minion) < self.getVisionRange()*2.0)).forEach((minion) -> {
            minions.add(minion);
        });
        Arrays.asList(world.getMinions()).stream().filter((minion) -> (minion.getFaction() == self.getFaction() && self.getDistanceTo(minion) < self.getVisionRange()*2.0)).forEach((minion) -> {
            minions.add(minion);
        });        
        minions.stream().filter((minion) -> (minion.getDistanceTo(self) < self.getVisionRange())).forEach((minion) -> {
            fields.add(new MinionField(minion, self));
        });
        enemyBuildings.stream().filter((building) -> (building.getDistanceTo(self) < building.getAttackRange()*2.0)).forEach((building) -> {
            fields.add(new EnemyTowerField(building, self));
        });
        Arrays.asList(world.getProjectiles()).stream().filter((bullet) -> (bullet.getFaction() != self.getFaction() && self.getDistanceTo(bullet) < self.getVisionRange()*2.0)).forEach((bullet) -> {
            fields.add(new BulletField(bullet, self));
        });
        enemyWizards.stream().filter((Wizard wizard) -> (wizard.getDistanceTo(self) < wizard.getCastRange()*1.5 && wizard.getAngleTo(self) < game.getStaffSector()/2.0 && wizard.getRemainingActionCooldownTicks() < 10)).map((Wizard wizard) -> {
            Point2D bulletPosition = new Point2D(wizard);
            bulletPosition.add(new Vector2D(game.getMagicMissileSpeed(), MyMath.normalizeAngle(wizard.getAngle()+wizard.getAngleTo(self))));
            double speedX = bulletPosition.x - wizard.getX();
            double speedY = bulletPosition.y - wizard.getY();
            Projectile fakeBullet = new Projectile(100000+random.nextInt(), bulletPosition.x, bulletPosition.y, speedX, speedY, 0, wizard.getFaction(), game.getMagicMissileRadius(), ProjectileType.MAGIC_MISSILE, wizard.getId(), wizard.getOwnerPlayerId());
            return fakeBullet;
        }).forEach((Projectile fakeBullet) -> {
            fields.add(new BulletField(fakeBullet, self));
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
                potentialGrid[x][y] += lanePotentialGrid[x][y];
            }            
        }
    }
    
    
    private List<Building> getEnemyBuildings()
    {
        
        List<Building> buildings = new ArrayList<>(7);        
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
                if (anyoneSee(fakeBuildings[i])) {
                    buildingsDestroyed[i] = true;
                } else {
                    buildings.add(fakeBuildings[i]);
                }
            }
        }
        return buildings;
    }
    
    private List<Minion> getEnemyMinions()
    {
        List<Minion> minions = new ArrayList<>();
        Arrays.asList(world.getMinions()).stream().filter((minion) -> (minion.getFaction() == enemyFaction || neutralMinionsInAgre.contains(minion.getId()))).forEach((minion) -> {
            minions.add(minion);
        });
        return minions;
    }
    
    private List<Wizard> getEnemyWizards()
    {
        List<Wizard> wizards = new ArrayList<>();
        Set<Long> added = new HashSet<>(5);
        Arrays.asList(world.getWizards()).stream().filter((wizard) -> (wizard.getFaction() == enemyFaction || (game.isRawMessagesEnabled() && wizard.getLife() <= 10))).forEach((wizard) -> {
            wizards.add(wizard);
            added.add(wizard.getId());
        });
        for (Wizard wizard : fakeWizards) {
            if (!added.contains(wizard.getId())) {
                wizards.add(wizard);
            }
        }
        return wizards;
    }
    
    private boolean isCrossing(LivingUnit unit)
    {
        return isCrossing(new Point2D(unit));
    }
        
    private boolean isCrossing(Point2D point)
    {
        LineSegment2D segment = new LineSegment2D(self.getX(), self.getY(), point.x, point.y);
        Vector2D vector = new Vector2D(self.getRadius(), MyMath.normalizeAngle(self.getAngle()+self.getAngleTo(point.x, point.y)-StrictMath.PI/2.0));
        LineSegment2D segmentLeft = segment.copy().add(vector);
        vector.rotate(StrictMath.PI);
        LineSegment2D segmentRight = segment.copy().add(vector);
        double distance = StrictMath.min(point.getDistanceTo(self), 200.0);

        debug.circle(point.x, point.y, self.getRadius()+1.0, Color.YELLOW);
        debug.line(segmentLeft.getX1(), segmentLeft.getY1(), segmentLeft.getX2(), segmentLeft.getY2(), Color.YELLOW);
        debug.line(segment.getX1(), segment.getY1(), segment.getX2(), segment.getY2(), Color.YELLOW);
        debug.line(segmentRight.getX1(), segmentRight.getY1(), segmentRight.getX2(), segmentRight.getY2(), Color.YELLOW);

        for (LivingUnit unit : allUnits) {
            if (self.getDistanceTo(unit)-self.getRadius()-unit.getRadius() > distance) {
                continue;
            }
            if (segmentLeft.isCrossingCircle(unit) || segmentRight.isCrossingCircle(unit) || segment.isCrossingCircle(unit)) {
                debug.fillCircle(unit.getX(), unit.getY(), unit.getRadius(), Color.ORANGE);
                return true;
            }
        }
        return false;
    }
    
    private boolean isCrossingTree(LivingUnit unit, double radius)
    {
        LineSegment2D segment = new LineSegment2D(self.getX(), self.getY(), unit.getX(), unit.getY());
        Vector2D vector = new Vector2D(radius, MyMath.normalizeAngle(self.getAngle()+self.getAngleTo(unit.getX(), unit.getY())-StrictMath.PI/2.0));
        LineSegment2D segmentLeft = segment.copy().add(vector);
        vector.rotate(StrictMath.PI);
        LineSegment2D segmentRight = segment.copy().add(vector);
        double distance = self.getDistanceTo(unit);
        
        debug.circle(unit.getX(), unit.getY(), unit.getRadius()+1.0, Color.RED);
        debug.line(segmentLeft.getX1(), segmentLeft.getY1(), segmentLeft.getX2(), segmentLeft.getY2(), Color.RED);
        debug.line(segment.getX1(), segment.getY1(), segment.getX2(), segment.getY2(), Color.RED);
        debug.line(segmentRight.getX1(), segmentRight.getY1(), segmentRight.getX2(), segmentRight.getY2(), Color.RED);

        for (Tree tree : world.getTrees()) {
            if (self.getDistanceTo(tree) > distance) {
                continue;
            }
            if (segmentLeft.isCrossingCircle(tree) || segment.isCrossingCircle(tree) || segmentRight.isCrossingCircle(tree)) {
                debug.fillCircle(tree.getX(), tree.getY(), tree.getRadius(), Color.RED);
                return true;
            }
        }
        return false;
    }
    
    private boolean isCrossingTree(LivingUnit unit)
    {
        return isCrossingTree(unit, game.getMagicMissileRadius());
    }
}
