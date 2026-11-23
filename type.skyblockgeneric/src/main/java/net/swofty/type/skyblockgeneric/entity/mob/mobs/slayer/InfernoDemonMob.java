package net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.ai.TargetSelector;
import net.minestom.server.entity.damage.Damage;
import net.swofty.commons.skyblock.statistics.ItemStatistic;
import net.swofty.commons.skyblock.statistics.ItemStatistics;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.entity.mob.MobType;
import net.swofty.type.skyblockgeneric.entity.mob.SkyBlockMob;
import net.swofty.type.skyblockgeneric.loottable.OtherLoot;
import net.swofty.type.skyblockgeneric.loottable.SkyBlockLootTable;
import net.swofty.type.skyblockgeneric.skill.SkillCategories;
import net.swofty.type.skyblockgeneric.slayer.SlayerFormat;
import net.swofty.type.skyblockgeneric.slayer.blaze.HellionShield;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One of the Inferno Demonlord's two demons: Quazii (a wither skeleton) or Typhoeus (a zombified
 * piglin).
 *
 * <p>Mods recognise them by the circled letters in the name ({@code ⓆⓊⒶⓏⒾⒾ}, {@code ⓉⓎⓅⒽⓄⒺⓊⓈ})
 * and tell the tier apart by the exact max health (500k / 1.75M / 5M / 10M). Name tag stack: name
 * (id + 1), shield or {@code IMMUNE} line (id + 2), owner line (id + 3).
 */
public final class InfernoDemonMob extends SkyBlockMob {
    private static final ThreadLocal<Profile> IN_CONSTRUCTION = new ThreadLocal<>();

    private final Profile profile;
    private final UUID ownerUuid;
    private @Nullable HellionShield shield;
    private boolean immune;
    private @Nullable Consumer<InfernoDemonMob> onShieldSwitch;
    private @Nullable Consumer<InfernoDemonMob> onDeath;

    private InfernoDemonMob(UUID ownerUuid, Profile profile) {
        super(profile.demon().entityType);
        this.ownerUuid = ownerUuid;
        this.profile = profile;
    }

    public static InfernoDemonMob create(UUID ownerUuid, Profile profile) {
        IN_CONSTRUCTION.set(profile);
        try {
            return new InfernoDemonMob(ownerUuid, profile);
        } finally {
            IN_CONSTRUCTION.remove();
        }
    }

    public Demon demon() {
        return live().demon();
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setShield(@Nullable HellionShield shield) {
        this.shield = shield;
        refreshShieldLine();
    }

    public @Nullable HellionShield getShield() {
        return shield;
    }

    public boolean isImmune() {
        return immune;
    }

    public void setImmune(boolean immune) {
        this.immune = immune;
        refreshShieldLine();
    }

    public void onShieldSwitch(Consumer<InfernoDemonMob> listener) {
        this.onShieldSwitch = listener;
    }

    public void onDeath(Consumer<InfernoDemonMob> listener) {
        this.onDeath = listener;
    }

    private void refreshShieldLine() {
        if (immune) {
            setExtraNameTagLine(0, Text.of("<c><l>IMMUNE"));
        } else if (shield != null) {
            setExtraNameTagLine(0, shield.line());
        } else {
            setExtraNameTagLine(0, Text.literal(""));
        }
    }

    @Override
    public Text buildNameTag() {
        return Text.of("<8>[<7>Lv{}<8>] {}{} <a>{}<f>/<a>{}<c>❤",
            live().level(),
            demon().colourTag,
            demon().circledName,
            SlayerFormat.health(getHealth()),
            SlayerFormat.health(live().health()));
    }

    @Override
    protected List<Text> getExtraNameTagLines() {
        return List.of(Text.literal(""), Text.of("<8>{}'s <c><l>SLAYER BOSS", live().ownerName()));
    }

    @Override
    public boolean damage(@NotNull Damage damage) {
        if (immune) {
            return false;
        }
        float amount = damage.getAmount();
        if (shield != null) {
            amount = shield.apply(damage, amount, () -> {
                refreshShieldLine();
                if (onShieldSwitch != null) {
                    onShieldSwitch.accept(this);
                }
            });
            refreshShieldLine();
        }
        Damage applied = amount == damage.getAmount() ? damage
            : new Damage(damage.getType(), damage.getSource(), damage.getAttacker(), damage.getSourcePosition(), amount);
        return super.damage(applied);
    }

    @Override
    public void kill() {
        super.kill();
        if (onDeath != null) {
            onDeath.accept(this);
        }
    }

    @Override public String getDisplayName() { return demon().displayName; }
    @Override public Integer getLevel() { return live().level(); }
    @Override public List<MobType> getMobTypes() { return List.of(MobType.INFERNAL); }
    @Override public @Nullable SkyBlockLootTable getLootTable() { return null; }
    @Override public SkillCategories getSkillCategory() { return SkillCategories.COMBAT; }
    @Override public long damageCooldown() { return 500L; }
    @Override public OtherLoot getOtherLoot() { return new OtherLoot(0, 0, 0); }
    @Override public List<GoalSelector> getGoalSelectors() { return SlayerBossBehaviour.goals(this); }
    @Override public List<TargetSelector> getTargetSelectors() { return SlayerBossBehaviour.targets(this); }

    @Override
    public ItemStatistics getBaseStatistics() {
        return ItemStatistics.builder()
            .withBase(ItemStatistic.HEALTH, live().health())
            .withBase(ItemStatistic.DAMAGE, live().damage())
            .withBase(ItemStatistic.SPEED, 120D)
            .build();
    }

    private Profile live() {
        return profile != null ? profile : IN_CONSTRUCTION.get();
    }

    public enum Demon {
        QUAZII("Quazii", "ⓆⓊⒶⓏⒾⒾ", "<3>", EntityType.WITHER_SKELETON),
        TYPHOEUS("Typhoeus", "ⓉⓎⓅⒽⓄⒺⓊⓈ", "<6>", EntityType.ZOMBIFIED_PIGLIN);

        final String displayName;
        final String circledName;
        final String colourTag;
        final EntityType entityType;

        Demon(String displayName, String circledName, String colourTag, EntityType entityType) {
            this.displayName = displayName;
            this.circledName = circledName;
            this.colourTag = colourTag;
            this.entityType = entityType;
        }
    }

    public record Profile(Demon demon, String ownerName, int level, double health, double damage) {
    }
}
