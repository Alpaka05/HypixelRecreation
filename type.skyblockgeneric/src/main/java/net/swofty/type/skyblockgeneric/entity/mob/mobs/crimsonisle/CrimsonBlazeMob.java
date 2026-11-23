package net.swofty.type.skyblockgeneric.entity.mob.mobs.crimsonisle;

import java.util.List;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.ai.TargetSelector;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.utils.time.TimeUnit;
import net.swofty.type.skyblockgeneric.entity.mob.BestiaryMob;
import net.swofty.type.skyblockgeneric.entity.mob.ai.ClosestEntityRegionTarget;
import net.swofty.type.skyblockgeneric.entity.mob.ai.FireballBurstGoal;
import net.swofty.type.skyblockgeneric.entity.mob.ai.MeleeAttackWithinRegionGoal;
import net.swofty.type.skyblockgeneric.entity.mob.ai.RandomRegionStrollGoal;
import net.swofty.type.skyblockgeneric.entity.mob.impl.RegionPopulator;
import net.swofty.type.skyblockgeneric.region.RegionType;
import net.swofty.type.skyblockgeneric.skill.SkillCategories;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/**
 * Shared behaviour of the Crimson Isle's hostile mobs: they keep to their zone, fight players who
 * come close, and the blazes among them shoot fireball bursts instead of walking up to melee.
 *
 * <p>Subclasses must answer every getter with constants: {@code SkyBlockMob}'s constructor reads
 * them before any subclass field exists.
 */
public abstract class CrimsonBlazeMob extends BestiaryMob implements RegionPopulator {

    protected CrimsonBlazeMob(EntityType type) {
        super(type);
    }

    /** The zone this mob lives and fights in. */
    protected abstract RegionType homeRegion();

    /** Whether it attacks with fireball bursts (blazes) rather than in melee. */
    protected boolean shootsFireballs() {
        return true;
    }

    @Override
    public List<GoalSelector> getGoalSelectors() {
        if (shootsFireballs()) {
            return List.of(
                new FireballBurstGoal(this, 16, 3, 6, 60, 0.9),
                new RandomRegionStrollGoal(this, 12, homeRegion())
            );
        }
        return List.of(
            new MeleeAttackWithinRegionGoal(this, 1.8, 20, TimeUnit.SERVER_TICK, homeRegion()),
            new RandomRegionStrollGoal(this, 12, homeRegion())
        );
    }

    @Override
    public List<TargetSelector> getTargetSelectors() {
        return List.of(
            new LastEntityDamagerTarget(this, 16),
            new ClosestEntityRegionTarget(this, 12, entity -> entity instanceof SkyBlockPlayer, homeRegion())
        );
    }

    @Override
    public SkillCategories getSkillCategory() {
        return SkillCategories.COMBAT;
    }

    @Override
    public long damageCooldown() {
        return 500;
    }

    @Override
    public int getMaxBestiaryTier() {
        return 5;
    }

    @Override
    public int getBestiaryBracket() {
        return 4;
    }
}
