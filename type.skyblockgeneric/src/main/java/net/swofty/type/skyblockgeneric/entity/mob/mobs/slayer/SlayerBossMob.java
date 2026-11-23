package net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer;

import java.util.List;
import java.util.UUID;
import net.minestom.server.entity.ai.GoalSelector;
import net.minestom.server.entity.ai.TargetSelector;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.timer.Task;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.slayer.SlayerFormat;
import net.swofty.type.skyblockgeneric.slayer.SlayerService;
import net.swofty.commons.skyblock.statistics.ItemStatistics;
import net.swofty.type.skyblockgeneric.entity.mob.MobType;
import net.swofty.type.skyblockgeneric.entity.mob.SkyBlockMob;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.ability.SlayerBossAbility;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.ability.SlayerBossAbilityFactory;
import net.swofty.type.skyblockgeneric.loottable.OtherLoot;
import net.swofty.type.skyblockgeneric.loottable.SkyBlockLootTable;
import net.swofty.type.skyblockgeneric.skill.SkillCategories;
import org.jetbrains.annotations.Nullable;

public final class SlayerBossMob extends SkyBlockMob {
    private static final ThreadLocal<SlayerBossProfile> IN_CONSTRUCTION = new ThreadLocal<>();
    private static final ThreadLocal<String> OWNER_NAME_IN_CONSTRUCTION = new ThreadLocal<>();

    private final SlayerBossProfile profile;
    private final UUID ownerUuid;
    private final String ownerName;
    private long lastTimerUpdate;
    private final SlayerBossAbility ability;
    private final List<Task> abilityTasks = new java.util.concurrent.CopyOnWriteArrayList<>();

    private SlayerBossMob(UUID ownerUuid, String ownerName, SlayerBossProfile profile) {
        super(profile.entityType());
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.profile = profile;
        this.ability = SlayerBossAbilityFactory.create(profile.type());
    }

    public static SlayerBossMob create(UUID ownerUuid, String ownerName, SlayerBossProfile profile) {
        IN_CONSTRUCTION.set(profile);
        OWNER_NAME_IN_CONSTRUCTION.set(ownerName);
        try {
            return new SlayerBossMob(ownerUuid, ownerName, profile);
        } finally {
            IN_CONSTRUCTION.remove();
            OWNER_NAME_IN_CONSTRUCTION.remove();
        }
    }

    public String getOwnerName() {
        return ownerName != null ? ownerName : OWNER_NAME_IN_CONSTRUCTION.get();
    }

    /**
     * Hypixel's boss tag: {@code [Lv436] ☠ Inferno Demonlord I 2.5M/2.5M❤}. SkyHanni's slayer name
     * filter, Skyblocker and AlpakaAddons find a boss by the ☠, the boss name and the tier after it;
     * Skyblocker treats a tag ending in {@code  0❤} as a dead boss.
     */
    @Override
    public Text buildNameTag() {
        SlayerBossProfile live = live();
        return Text.of("<8>[<7>Lv{}<8>] <c>☠ <b>{} " + live.tier().tier().colourTag() + "{} <a>{}<f>/<a>{}<c>❤",
            live.tier().bossLevel(),
            live.type().displayName(),
            live.tier().tier().numeral(),
            SlayerFormat.health(getHealth()),
            SlayerFormat.health(live.tier().bossHealth()));
    }

    /** Above the name, bottom to top: the time left, then whose boss it is (entity ids + 2 and + 3). */
    @Override
    protected List<Text> getExtraNameTagLines() {
        return List.of(
            Text.of("<c>{}", SlayerFormat.timer(live().tier().timeLimitSeconds() * 1000L)),
            Text.of("<8>{}'s <c><l>SLAYER BOSS", getOwnerName()));
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (time - lastTimerUpdate >= 250) {
            lastTimerUpdate = time;
            long left = SlayerService.bossTimeLeft(ownerUuid);
            if (left >= 0) {
                setExtraNameTagLine(0, Text.of("<c>{}", SlayerFormat.timer(left)));
            }
        }
    }

    /** Takes the boss away without a kill (quest failed, cancelled, owner gone). */
    public void despawnBoss() {
        abilityTasks.forEach(Task::cancel);
        abilityTasks.clear();
        despawnQuietly();
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public SlayerBossProfile getProfile() {
        return live();
    }

    public SlayerBossAbility getAbility() {
        return ability;
    }

    public void trackAbilityTask(Task task) {
        abilityTasks.add(task);
    }

    @Override public String getDisplayName() { return live().displayName(); }
    @Override public Integer getLevel() { return live().tier().bossLevel(); }
    @Override public List<MobType> getMobTypes() { return live().mobTypes(); }
    @Override public ItemStatistics getBaseStatistics() { return live().asBaseStatistics(); }
    @Override public OtherLoot getOtherLoot() { return live().asOtherLoot(); }
    @Override public long damageCooldown() { return 500L; }
    @Override public @Nullable SkyBlockLootTable getLootTable() { return live().asLootTable(); }
    @Override public SkillCategories getSkillCategory() { return SkillCategories.COMBAT; }

    @Override
    public List<GoalSelector> getGoalSelectors() {
        return SlayerBossBehaviour.goals(this);
    }

    @Override
    public List<TargetSelector> getTargetSelectors() {
        return SlayerBossBehaviour.targets(this);
    }

    @Override
    public void onSpawn() {
        ability.onSpawn(this);
    }

    @Override
    public boolean damage(@org.jetbrains.annotations.NotNull Damage damage) {
        float amount = damage.getAmount();
        float modified = ability.modifyIncomingDamage(this, damage, amount);
        if (modified <= 0F) {
            return false;
        }

        Damage applied = modified == amount
            ? damage
            : new Damage(damage.getType(), damage.getSource(), damage.getAttacker(), damage.getSourcePosition(), modified);
        boolean result = super.damage(applied);
        ability.onDamaged(this, applied, modified);
        return result;
    }

    @Override
    public void kill() {
        ability.onDeath(this);
        abilityTasks.forEach(Task::cancel);
        abilityTasks.clear();
        super.kill();
    }

    private SlayerBossProfile live() {
        return profile != null ? profile : IN_CONSTRUCTION.get();
    }
}
