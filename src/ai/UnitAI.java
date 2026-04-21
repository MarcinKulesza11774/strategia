package ai;

import model.GameState;
import model.entity.Unit;
import model.world.TileType;
import model.world.WorldMap;
import util.GameConfig;

/**
 * Maszyna stanów AI dla jednostek – parametry z game_config.json.
 */
public class UnitAI {
    private final GameState state;

    public UnitAI(GameState state) { this.state = state; }

    public void tick(Unit unit, double dt) {
        if (!unit.isAlive()) return;
        unit.tickCooldown();
        if (unit.isPlayerOwned()) tickPlayer(unit, dt);
        else                      tickEnemy(unit, dt);
    }

    private void tickPlayer(Unit unit, double dt) {
        GameConfig cfg = GameConfig.get();

        // Priorytet 1: walka jeśli wróg blisko
        if (unit.getUnitClass().canFight) {
            Unit enemy = findNearest(unit, state.getEnemyUnits(), cfg.fightRange());
            if (enemy != null) {
                if (dist(unit, enemy) <= cfg.attackRange()) { doAttack(unit, enemy); return; }
                unit.setState(Unit.State.ATTACKING);
                unit.stepTowards(enemy.getX(), enemy.getY(), dt);
                return;
            }
        }

        // Priorytet 2: rozkaz gracza
        if (unit.hasTarget()) {
            boolean arrived = unit.stepTowards(unit.getTargetX(), unit.getTargetY(), dt);
            unit.setState(arrived ? Unit.State.IDLE : Unit.State.MOVING);
            if (arrived) unit.clearTarget();
            return;
        }

        // Priorytet 3: domyślne zachowanie klasy
        String id = unit.getClassId();
        if (unit.getUnitClass().canHarvest || unit.getUnitClass().canBuild)
            tickWorker(unit, dt);
        else if (unit.getUnitClass().canFight)
            tickGuard(unit, dt);
        else if (unit.getUnitClass().canTrade)
            unit.setState(Unit.State.TRADING);
        else
            unit.setState(Unit.State.IDLE);
    }

    private void tickWorker(Unit unit, double dt) {
        WorldMap map = state.getMap();
        GameConfig cfg = GameConfig.get();

        // Budowanie ma priorytet nad zbieraniem
        if (unit.getUnitClass().canBuild) {
            int[] bt = map.findNearestBuildTask(unit.getTileX(), unit.getTileY());
            if (bt != null) {
                if (adjacent(unit, bt[0], bt[1])) {
                    unit.setState(Unit.State.BUILDING);
                    unit.tickActionProgress();
                    if (unit.getActionProgress() >= cfg.buildTicksRequired()) {
                        unit.resetActionProgress();
                        String pending = map.getTile(bt[0], bt[1]).getPendingBuildTypeId();
                        if (pending != null) {
                            map.setTile(bt[0], bt[1], pending);
                            map.getTile(bt[0], bt[1]).clearBuildMark();
                            state.getRoomSystem().rescan();
                            state.log("Wybudowano: " + TileType.get(pending).label);
                        }
                    }
                } else {
                    moveTowards(unit, bt[0], bt[1], dt);
                }
                return;
            }
        }

        // Zbieranie zasobów
        if (unit.getUnitClass().canHarvest) {
            int[] res = map.findNearestAnyResource(unit.getTileX(), unit.getTileY(), cfg.workerSearchRadius());
            if (res != null) {
                if (adjacent(unit, res[0], res[1])) {
                    unit.setState(Unit.State.HARVESTING);
                    var tile = map.getTile(res[0], res[1]);
                    if (tile != null && TileType.get(tile.getTypeId()).isResource) {
                        tile.setMarkedForHarvest(true);
                        tile.tickHarvest();
                        if (tile.isHarvestDone()) {
                            TileType tt  = TileType.get(tile.getTypeId());
                            int amount   = tt.harvestYieldMin + (int)(Math.random()*(tt.harvestYieldMax-tt.harvestYieldMin+1));
                            if (tt.harvestResource() != null)
                                state.addResource(tt.harvestResource().id, amount);
                            tile.setType(tt.harvestRemnant().id);
                            tile.setMarkedForHarvest(false);
                            unit.gainXp(cfg.xpPerHarvest());
                        }
                    }
                } else {
                    map.getTile(res[0], res[1]).setMarkedForHarvest(true);
                    moveTowards(unit, res[0], res[1], dt);
                }
                return;
            }
        }
        unit.setState(Unit.State.IDLE);
    }

    private void tickGuard(Unit unit, double dt) {
        Unit enemy = findNearest(unit, state.getEnemyUnits(), GameConfig.get().fightRange()*2);
        if (enemy != null) {
            if (dist(unit, enemy) <= GameConfig.get().attackRange()) doAttack(unit, enemy);
            else { unit.setState(Unit.State.ATTACKING); moveTowards(unit, (int)enemy.getX(), (int)enemy.getY(), dt); }
            return;
        }
        // Patrol
        if (!unit.hasTarget() || unit.getState() != Unit.State.PATROLLING) {
            double a = Math.random()*Math.PI*2, r = 3+Math.random()*4;
            unit.setTarget(state.getMap().fortressX + Math.cos(a)*r,
                           state.getMap().fortressY + Math.sin(a)*r);
            unit.setState(Unit.State.PATROLLING);
        } else {
            if (unit.stepTowards(unit.getTargetX(), unit.getTargetY(), dt)) unit.clearTarget();
        }
    }

    private void tickEnemy(Unit unit, double dt) {
        Unit target = findNearest(unit, state.getPlayerUnits(), 9999);
        if (target == null) {
            unit.stepTowards(state.getMap().fortressX, state.getMap().fortressY, dt);
            return;
        }
        if (dist(unit, target) <= GameConfig.get().attackRange()) doAttack(unit, target);
        else moveTowards(unit, (int)target.getX(), (int)target.getY(), dt);
    }

    private void doAttack(Unit attacker, Unit target) {
        if (attacker.getActionCooldown() > 0) return;
        int dmg = attacker.calculateDamage(target);
        target.takeDamage(dmg);
        attacker.setActionCooldown(GameConfig.get().attackCooldown());
        attacker.gainXp(GameConfig.get().xpPerAttack());
        if (!target.isAlive()) {
            state.log(attacker.getUnitClass().label + " pokonał " + target.getUnitClass().label + "!");
            attacker.gainXp(GameConfig.get().xpPerKill());
        }
    }

    private void moveTowards(Unit unit, double tx, double ty, double dt) {
        unit.setState(Unit.State.MOVING);
        if (unit.stepTowards(tx, ty, dt)) unit.clearTarget();
    }

    private Unit findNearest(Unit from, java.util.List<Unit> candidates, double maxDist) {
        Unit best = null; double bd = maxDist;
        for (Unit u : candidates) {
            if (!u.isAlive()) continue;
            double d = dist(from, u);
            if (d < bd) { bd = d; best = u; }
        }
        return best;
    }

    private double dist(Unit a, Unit b) { return Math.hypot(a.getX()-b.getX(), a.getY()-b.getY()); }
    private boolean adjacent(Unit u, int tx, int ty) {
        return Math.abs(u.getTileX()-tx) <= 1 && Math.abs(u.getTileY()-ty) <= 1;
    }
}
