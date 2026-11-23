package net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer;

import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.ai.TargetSelector;
import net.minestom.server.entity.damage.Damage;
import net.swofty.commons.skyblock.item.ItemType;
import net.swofty.commons.skyblock.statistics.ItemStatistic;
import net.swofty.commons.skyblock.statistics.ItemStatistics;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.entity.mob.MobType;
import net.swofty.type.skyblockgeneric.entity.mob.SkyBlockMob;
import net.swofty.type.skyblockgeneric.loottable.OtherLoot;
import net.swofty.type.skyblockgeneric.loottable.SkyBlockLootTable;
import net.swofty.type.skyblockgeneric.skill.SkillCategories;
import net.swofty.type.skyblockgeneric.slayer.SlayerFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A slayer mini-boss that turns up while grinding a high-tier quest (Flare Demon on Inferno III+,
 * Kindleheart and Burningsoul Demon on IV). Its kill counts toward the quest; it leaves after 60 s
 * without being hit. Mods spot them by the name ({@code Flare Demon} ...).
 */
public final class SlayerMiniBossMob extends SkyBlockMob {
    private static final ThreadLocal<Kind> IN_CONSTRUCTION = new ThreadLocal<>();
    private static final long IDLE_DESPAWN_MS = 60_000L;

    private final Kind kind;
    private final UUID ownerUuid;
    private long lastHit = System.currentTimeMillis();

    private SlayerMiniBossMob(UUID ownerUuid, Kind kind) {
        super(kind.entityType);
        this.kind = kind;
        this.ownerUuid = ownerUuid;
    }

    public static SlayerMiniBossMob create(UUID ownerUuid, Kind kind) {
        IN_CONSTRUCTION.set(kind);
        try {
            return new SlayerMiniBossMob(ownerUuid, kind);
        } finally {
            IN_CONSTRUCTION.remove();
        }
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Kind kind() {
        return live();
    }

    @Override
    public Text buildNameTag() {
        return Text.of("<8>[<7>Lv{}<8>] <4>♨ " + live().colourTag + "<l>{}</l> <a>{}<f>/<a>{}<c>❤",
            live().level, live().displayName, SlayerFormat.health(getHealth()), SlayerFormat.health(live().health));
    }

    @Override
    public boolean damage(@NotNull Damage damage) {
        lastHit = System.currentTimeMillis();
        return super.damage(damage);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (time - lastHit > IDLE_DESPAWN_MS) {
            despawnQuietly();
        }
    }

    @Override public String getDisplayName() { return live().displayName; }
    @Override public Integer getLevel() { return live().level; }
    @Override public List<MobType> getMobTypes() { return List.of(MobType.INFERNAL); }
    @Override public SkillCategories getSkillCategory() { return SkillCategories.COMBAT; }
    @Override public long damageCooldown() { return 500L; }
    @Override public OtherLoot getOtherLoot() { return new OtherLoot(live().combatXp, 0, 0); }
    @Override public List<GoalSelector> getGoalSelectors() { return SlayerBossBehaviour.goals(this); }
    @Override public List<TargetSelector> getTargetSelectors() { return SlayerBossBehaviour.targets(this); }

    @Override
    public ItemStatistics getBaseStatistics() {
        return ItemStatistics.builder()
            .withBase(ItemStatistic.HEALTH, live().health)
            .withBase(ItemStatistic.DAMAGE, live().damage)
            .withBase(ItemStatistic.SPEED, 110D)
            .build();
    }

    @Override
    public @Nullable SkyBlockLootTable getLootTable() {
        int ashes = live().derelictAshe;
        return new SkyBlockLootTable() {
            @Override
            public @NonNull List<LootRecord> getLootTable() {
                return List.of(new LootRecord(ItemType.DERELICT_ASHE, ashes, 100));
            }

            @Override
            public @NotNull CalculationMode getCalculationMode() {
                return CalculationMode.CALCULATE_INDIVIDUAL;
            }
        };
    }

    private Kind live() {
        return kind != null ? kind : IN_CONSTRUCTION.get();
    }

    /** Values from Hypixel after the 2026 mob level rework. */
    public enum Kind {
        FLARE_DEMON("Flare Demon", "<c>", EntityType.BLAZE, 381, 8_750_000, 3_000, 2_857, 1),
        KINDLEHEART_DEMON("Kindleheart Demon", "<c>", EntityType.BLAZE, 456, 17_500_000, 6_000, 3_420, 2),
        BURNINGSOUL_DEMON("Burningsoul Demon", "<4>", EntityType.BLAZE, 522, 35_000_000, 9_000, 3_915, 5);

        final String displayName;
        final String colourTag;
        final EntityType entityType;
        final int level;
        final double health;
        final double damage;
        final int combatXp;
        final int derelictAshe;

        Kind(String displayName, String colourTag, EntityType entityType, int level, double health,
             double damage, int combatXp, int derelictAshe) {
            this.displayName = displayName;
            this.colourTag = colourTag;
            this.entityType = entityType;
            this.level = level;
            this.health = health;
            this.damage = damage;
            this.combatXp = combatXp;
            this.derelictAshe = derelictAshe;
        }

        public String displayName() {
            return displayName;
        }
    }
}
