package de.hiveinvasion.human;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

/** Lange Wege in Etappen laufen (die Wegfindung reicht nur ein paar Dutzend Blöcke weit). */
public final class Navigation {
    private Navigation() {}

    public static boolean moveToward(MobEntity mob, Vec3d target, double speed) {
        Vec3d to = target.subtract(mob.getPos());
        double max = 36;
        Vec3d goal = to.length() > max ? mob.getPos().add(to.normalize().multiply(max)) : target;
        return mob.getNavigation().startMovingTo(goal.x, goal.y, goal.z, speed);
    }
}
