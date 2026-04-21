package model.entity;

import util.GameConfig;

/**
 * Jednostka na mapie. Klasa przechowuje ID klasy jako String
 * i pobiera dane z rejestru UnitClass.
 */
public class Unit {
    private static int nextId = 1;

    public enum State { IDLE, MOVING, HARVESTING, BUILDING, ATTACKING, FLEEING, PATROLLING, TRADING }

    private final int     id;
    private String        classId;
    private double        x, y;
    private int           hp;
    private int           xp;
    private State         state = State.IDLE;
    private double        targetX, targetY;
    private boolean       hasTarget;
    private int           actionTargetX, actionTargetY;
    private int           actionProgress;
    private int           actionCooldown;
    private int           speedBuff, attackBuff, defenseBuff;
    private final boolean playerOwned;
    private int           attackTargetId = -1;

    public Unit(String classId, double x, double y, boolean playerOwned) {
        this.id          = nextId++;
        this.classId     = classId;
        this.x           = x;
        this.y           = y;
        this.hp          = getUnitClass().maxHp;
        this.playerOwned = playerOwned;
    }

    public UnitClass getUnitClass() { return UnitClass.get(classId); }
    public String    getClassId()   { return classId; }

    public boolean stepTowards(double tx, double ty, double dt) {
        double dx = tx - x, dy = ty - y;
        double dist = Math.hypot(dx, dy);
        double spd  = getEffectiveSpeed() * dt;
        if (dist <= spd) { x = tx; y = ty; return true; }
        x += dx / dist * spd; y += dy / dist * spd;
        return false;
    }

    public int    getEffectiveAttack()  { return getUnitClass().attack  + attackBuff; }
    public int    getEffectiveDefense() { return getUnitClass().defense + defenseBuff; }
    public double getEffectiveSpeed()   { return getUnitClass().speed * (1.0 + speedBuff / 100.0); }

    public int calculateDamage(Unit target) {
        int base     = Math.max(1, getEffectiveAttack() - target.getEffectiveDefense() / 2);
        int variance = Math.max(1, (int)(base * 0.25));
        return base + (int)(Math.random() * variance);
    }

    public boolean takeDamage(int dmg) { hp = Math.max(0, hp - dmg); return hp > 0; }
    public void    gainXp(int amount)  { xp += amount; }
    public boolean canPromote()        { return getUnitClass().promoteTo() != null && xp >= getUnitClass().xpToPromote; }

    public void promote() {
        UnitClass next = getUnitClass().promoteTo();
        if (next == null) return;
        int ratio = hp * 100 / getUnitClass().maxHp;
        classId = next.id;
        hp = next.maxHp * ratio / 100;
        xp = 0;
    }

    public int     getId()             { return id; }
    public double  getX()              { return x; }
    public double  getY()              { return y; }
    public int     getTileX()          { return (int) Math.round(x); }
    public int     getTileY()          { return (int) Math.round(y); }
    public int     getHp()             { return hp; }
    public int     getMaxHp()          { return getUnitClass().maxHp; }
    public int     getXp()             { return xp; }
    public int     xpForNextTier()     { return getUnitClass().xpToPromote; }
    public State   getState()          { return state; }
    public boolean isPlayerOwned()     { return playerOwned; }
    public boolean isAlive()           { return hp > 0; }

    public void setX(double x)         { this.x = x; }
    public void setY(double y)         { this.y = y; }
    public void setState(State s)      { this.state = s; }
    public double  getTargetX()        { return targetX; }
    public double  getTargetY()        { return targetY; }
    public boolean hasTarget()         { return hasTarget; }
    public void    setTarget(double tx, double ty) { targetX=tx; targetY=ty; hasTarget=true; }
    public void    clearTarget()       { hasTarget = false; }
    public int     getActionTargetX()  { return actionTargetX; }
    public int     getActionTargetY()  { return actionTargetY; }
    public void    setActionTarget(int x, int y) { actionTargetX=x; actionTargetY=y; }
    public int     getActionProgress() { return actionProgress; }
    public void    tickActionProgress(){ actionProgress++; }
    public void    resetActionProgress(){ actionProgress = 0; }
    public int     getActionCooldown() { return actionCooldown; }
    public void    setActionCooldown(int v){ actionCooldown = v; }
    public void    tickCooldown()      { if (actionCooldown > 0) actionCooldown--; }
    public int     getAttackTargetId() { return attackTargetId; }
    public void    setAttackTargetId(int v){ attackTargetId = v; }
    public void    setSpeedBuff(int v) { speedBuff   = v; }
    public void    setAttackBuff(int v){ attackBuff  = v; }
    public void    setDefenseBuff(int v){ defenseBuff = v; }
    public void    heal(int amount)    { hp = Math.min(getUnitClass().maxHp, hp + amount); }
}
