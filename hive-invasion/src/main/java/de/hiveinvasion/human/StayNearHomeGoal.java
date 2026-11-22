package de.hiveinvasion.human;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/** Hält Menschen in der Nähe ihres Zuhauses (Stadt bzw. Stützpunkt). */
public class StayNearHomeGoal extends Goal {
    private final HumanEntity mob;
    private final int radius;
    private final double speed;

    public StayNearHomeGoal(HumanEntity mob, int radius, double speed) {
        this.mob = mob; this.radius = radius; this.speed = speed;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        BlockPos home = mob.getHome();
        return home != null && mob.getTarget() == null && !home.isWithinDistance(mob.getPos(), radius);
    }

    @Override
    public void start() {
        Navigation.moveToward(mob, Vec3d.ofBottomCenter(mob.getHome()), speed);
    }

    @Override
    public boolean shouldContinue() {
        return !mob.getNavigation().isIdle() && !mob.getHome().isWithinDistance(mob.getPos(), radius * 0.5);
    }
}
