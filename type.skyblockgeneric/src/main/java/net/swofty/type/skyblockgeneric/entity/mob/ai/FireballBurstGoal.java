package net.swofty.type.skyblockgeneric.entity.mob.ai;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.instance.Instance;
import net.swofty.type.skyblockgeneric.entity.mob.SkyBlockMob;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * A blaze's attack: a burst of small fireballs at its target, then a pause, like the vanilla blaze.
 *
 * <p>A fireball that reaches a player counts as the mob attacking them, so damage goes through the
 * normal mob-damage path (defense, pets, damage indicators).
 */
public class FireballBurstGoal extends GoalSelector {
    private final double range;
    private final int shotsPerBurst;
    private final int ticksBetweenShots;
    private final int ticksBetweenBursts;
    private final double speed;

    private Entity target;
    private int shotsLeft;
    private int cooldown;

    public FireballBurstGoal(@NotNull EntityCreature entityCreature, double range, int shotsPerBurst,
                             int ticksBetweenShots, int ticksBetweenBursts, double speed) {
        super(entityCreature);
        this.range = range;
        this.shotsPerBurst = shotsPerBurst;
        this.ticksBetweenShots = ticksBetweenShots;
        this.ticksBetweenBursts = ticksBetweenBursts;
        this.speed = speed;
    }

    @Override
    public boolean shouldStart() {
        target = validTarget(findTarget());
        return target != null;
    }

    @Override
    public void start() {
        shotsLeft = shotsPerBurst;
        cooldown = 20;
        if (entityCreature instanceof SkyBlockMob mob) {
            mob.getVanillaNavigator().stop();
        }
    }

    @Override
    public void tick(long time) {
        target = validTarget(findTarget());
        if (target == null) {
            return;
        }
        entityCreature.lookAt(target);
        if (--cooldown > 0) {
            return;
        }
        if (shotsLeft <= 0) {
            shotsLeft = shotsPerBurst;
            cooldown = ticksBetweenBursts;
            return;
        }
        shoot(target);
        shotsLeft--;
        cooldown = ticksBetweenShots;
    }

    @Override
    public boolean shouldEnd() {
        return target == null || entityCreature.isDead();
    }

    @Override
    public void end() {
        target = null;
    }

    private Entity validTarget(Entity candidate) {
        if (candidate == null || candidate.isRemoved() || candidate.getInstance() != entityCreature.getInstance()) {
            return null;
        }
        if (candidate.getDistanceSquared(entityCreature) > range * range) {
            return null;
        }
        return candidate;
    }

    private void shoot(Entity target) {
        Instance instance = entityCreature.getInstance();
        if (instance == null) {
            return;
        }
        Pos from = entityCreature.getPosition().add(0, entityCreature.getEyeHeight(), 0);
        Vec aim = target.getPosition().add(0, 1.0, 0).sub(from).asVec();
        // A little spread, like the vanilla blaze.
        aim = aim.normalize().add((Math.random() - 0.5) * 0.08, (Math.random() - 0.5) * 0.04, (Math.random() - 0.5) * 0.08).normalize();
        new MobFireball(entityCreature, aim.mul(speed)).setInstance(instance, from.add(aim.mul(0.8)));
        instance.playSound(Sound.sound(Key.key("entity.blaze.shoot"), Sound.Source.HOSTILE, 1f, 1f), from);
    }

    /** The projectile. Moves in a straight line, hurts the first player it touches, dies on blocks. */
    public static class MobFireball extends Entity {
        private static final int MAX_AGE_TICKS = 80;
        private final EntityCreature shooter;
        private final Vec perTick;
        private int age;

        public MobFireball(EntityCreature shooter, Vec blocksPerTick) {
            super(EntityType.SMALL_FIREBALL);
            this.shooter = shooter;
            this.perTick = blocksPerTick;
            setNoGravity(true);
        }

        @Override
        public void update(long time) {
            Instance instance = getInstance();
            if (instance == null || ++age > MAX_AGE_TICKS || shooter.isRemoved()) {
                remove();
                return;
            }
            Pos next = getPosition().add(perTick);
            if (!instance.isChunkLoaded(next) || instance.getBlock(next).isSolid()) {
                remove();
                return;
            }
            for (Entity nearby : instance.getNearbyEntities(next, 1.2)) {
                if (nearby instanceof SkyBlockPlayer player && !player.isDead()) {
                    shooter.attack(player, false);
                    remove();
                    return;
                }
            }
            refreshPosition(next);
        }
    }
}
