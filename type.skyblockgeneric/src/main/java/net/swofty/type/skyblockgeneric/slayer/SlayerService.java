package net.swofty.type.skyblockgeneric.slayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import net.swofty.commons.text.Text;
import net.swofty.commons.text.TextBody;
import net.swofty.type.skyblockgeneric.SkyBlockGenericLoader;
import net.swofty.type.skyblockgeneric.data.SkyBlockDataHandler;
import net.swofty.type.skyblockgeneric.data.datapoints.DatapointSlayer;
import net.swofty.type.skyblockgeneric.elections.ElectionManager;
import net.swofty.type.skyblockgeneric.elections.SkyBlockMayor;
import net.swofty.type.skyblockgeneric.entity.mob.SkyBlockMob;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.SlayerBossMob;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.SlayerBossProfile;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.SlayerMiniBossMob;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The slayer quest flow: start, grind, boss, and how it ends.
 *
 * <p>Everything a player reads here is written the way Hypixel writes it, because client mods
 * (SkyHanni, Skyblocker, AlpakaAddons) recognise slayers by these exact sidebar and chat lines:
 * <ul>
 *   <li>sidebar: {@code Slayer Quest}, {@code Inferno Demonlord I}, {@code (1.2k/4.8k) Combat XP},
 *       {@code Slay the boss!}, {@code Boss slain!}</li>
 *   <li>chat: {@code SLAYER QUEST STARTED!}, {@code » Slay 2,400 Combat XP worth of Blazes.},
 *       {@code NICE! SLAYER BOSS SLAIN!}, {@code SLAYER QUEST COMPLETE!}, {@code SLAYER QUEST FAILED!}</li>
 * </ul>
 * Grinding progress goes to the sidebar only; Hypixel sends no chat line per kill.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlayerService {
    /** How long "Boss slain!" stays on the sidebar after a kill. Hypixel keeps it about four seconds. */
    private static final long SLAIN_DISPLAY_MS = 4_000L;
    /** Grace after joining before a quest whose boss is not on this server counts as lost. */
    private static final long JOIN_GRACE_MS = 5_000L;

    /** Bosses alive on this server, by owner. */
    private static final Map<UUID, ActiveBoss> ACTIVE_BOSSES = new ConcurrentHashMap<>();
    /** Recently finished quests, by owner, for the sidebar's "Boss slain!" line. */
    private static final Map<UUID, SlainQuest> RECENTLY_SLAIN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SEEN_SINCE = new ConcurrentHashMap<>();

    private static boolean started;

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            tick();
            return TaskSchedule.tick(10);
        });
    }

    public static StartResult startQuest(SkyBlockPlayer player, SlayerType type, SlayerTier tier) {
        DatapointSlayer.SlayerData data = data(player);
        if (data.getActiveQuest() != null) {
            return StartResult.alreadyActive(data.getActiveQuest());
        }

        SlayerDefinition definition = SlayerRegistry.get(type);
        if (definition == null || !definition.enabled()) {
            return StartResult.unavailable(type);
        }

        SlayerTierDefinition tierDefinition = definition.tiers().get(tier);
        if (tierDefinition == null) {
            return StartResult.missingTier(type, tier);
        }

        // Staff skip the unlock chain so a single slayer can be tested on a fresh profile.
        boolean staff = player.getRank().isEqualOrHigherThan(net.swofty.type.generic.user.categories.Rank.STAFF);
        if (definition.unlockRequirement().isPresent() && !staff) {
            SlayerUnlockRequirement requirement = definition.unlockRequirement().get();
            if (data.progress(requirement.type()).completions(requirement.tier()) <= 0) {
                return StartResult.locked(requirement);
            }
        }

        int cost = questCost(tierDefinition);
        if (player.getCoins() < cost) {
            return StartResult.notEnoughCoins(cost);
        }

        player.removeCoins(cost);
        SlayerQuest quest = new SlayerQuest(type, tier, System.currentTimeMillis(), 0, false, null);
        data.setActiveQuest(quest);
        save(player, data);
        RECENTLY_SLAIN.remove(player.getUuid());

        player.sendMessage("  <5><l>SLAYER QUEST STARTED!");
        player.sendMessage("   <5><l>»</l> <7>Slay <c>{} Combat XP <7>worth of {}<7>.",
            SlayerFormat.commas(tierDefinition.requiredCombatXp()), type.pluralMobName());
        player.playSound(Sound.sound(Key.key("entity.wither.spawn"), Sound.Source.PLAYER, 0.5f, 1.5f));
        return StartResult.started(quest);
    }

    public static void cancelQuest(SkyBlockPlayer player) {
        DatapointSlayer.SlayerData data = data(player);
        if (data.getActiveQuest() == null) {
            player.sendMessage("<c>You do not have an active Slayer quest.");
            return;
        }
        despawnBoss(player.getUuid());
        data.setActiveQuest(null);
        save(player, data);
        player.sendMessage("<c>Your Slayer Quest has been cancelled!");
    }

    public static void handleMobKill(SkyBlockPlayer player, SkyBlockMob mob) {
        DatapointSlayer.SlayerData data = data(player);
        SlayerQuest quest = data.getActiveQuest();
        if (quest == null || mob == null) {
            return;
        }

        if (mob instanceof SlayerBossMob bossMob) {
            completeBoss(player, data, quest, bossMob);
            return;
        }

        if (quest.bossSpawned()) {
            return;
        }

        SlayerDefinition definition = SlayerRegistry.get(quest.type());
        if (definition == null || !definition.accepts(mob.getMobTypes())) {
            return;
        }

        SlayerTierDefinition tierDefinition = definition.tiers().get(quest.tier());
        if (tierDefinition == null) {
            return;
        }

        // One kill never fills more than 75 % of a quest (Hypixel, patch 0.20.9).
        int gainedXp = (int) Math.min(mob.getOtherLoot().getSkillXPAmount(), tierDefinition.requiredCombatXp() * 0.75D);
        gainedXp = Math.max(1, gainedXp);
        SlayerQuest progressedQuest = quest.addCombatXp(gainedXp, tierDefinition.requiredCombatXp());
        if (progressedQuest.combatXp() >= tierDefinition.requiredCombatXp()) {
            SlayerBossMob boss = spawnBoss(player, definition, tierDefinition, mob.getPosition());
            if (boss != null) {
                progressedQuest = progressedQuest.markBossSpawned(boss.getUuid());
            }
        } else if (!(mob instanceof SlayerMiniBossMob)) {
            maybeSpawnMiniBoss(player, quest, mob.getPosition());
        }

        data.setActiveQuest(progressedQuest);
        save(player, data);
    }

    /**
     * Inferno III+ grinding now and then brings a mini-boss: Flare Demon from III, Kindleheart and
     * Burningsoul Demon on IV. Hypixel's odds are not published; these are rough.
     */
    private static void maybeSpawnMiniBoss(SkyBlockPlayer player, SlayerQuest quest, Pos at) {
        if (quest.type() != SlayerType.INFERNO_DEMONLORD || player.getInstance() == null) {
            return;
        }
        double roll = Math.random();
        SlayerMiniBossMob.Kind kind = null;
        if (quest.tier().number() >= 4 && roll < 0.015) {
            kind = SlayerMiniBossMob.Kind.BURNINGSOUL_DEMON;
        } else if (quest.tier().number() >= 4 && roll < 0.045) {
            kind = SlayerMiniBossMob.Kind.KINDLEHEART_DEMON;
        } else if (quest.tier().number() >= 3 && roll < 0.08) {
            kind = SlayerMiniBossMob.Kind.FLARE_DEMON;
        }
        if (kind == null) {
            return;
        }
        SlayerMiniBossMob miniBoss = SlayerMiniBossMob.create(player.getUuid(), kind);
        miniBoss.setInstance(player.getInstance(), at);
        player.sendMessage("<c><l>SLAYER MINI-BOSS </l><c>{} <c>has spawned!", kind.displayName());
        player.playSound(Sound.sound(Key.key("entity.wither.ambient"), Sound.Source.HOSTILE, 1f, 1.2f));
    }

    /** Called from {@link SkyBlockPlayer#kill()}: dying with a boss up fails the quest, as on Hypixel. */
    public static void handlePlayerDeath(SkyBlockPlayer player) {
        if (player.getSkyblockDataHandler() == null) {
            return;
        }
        SlayerQuest quest = data(player).getActiveQuest();
        if (quest == null || !quest.bossSpawned()) {
            return;
        }
        failQuest(player, "You died! What a noob!");
    }

    /** The owner left this server: their boss cannot follow them, so it goes. */
    public static void handleDisconnect(SkyBlockPlayer player) {
        despawnBoss(player.getUuid());
        RECENTLY_SLAIN.remove(player.getUuid());
        SEEN_SINCE.remove(player.getUuid());
    }

    public static @Nullable SlayerBossMob activeBoss(UUID owner) {
        ActiveBoss active = ACTIVE_BOSSES.get(owner);
        return active == null ? null : active.boss();
    }

    /** Milliseconds the owner has left to kill their boss, or -1 without one. */
    public static long bossTimeLeft(UUID owner) {
        ActiveBoss active = ACTIVE_BOSSES.get(owner);
        if (active == null) {
            return -1;
        }
        return Math.max(0, active.deadline() - System.currentTimeMillis());
    }

    public static QuestStatus status(SkyBlockPlayer player) {
        DatapointSlayer.SlayerData data = data(player);
        SlayerQuest quest = data.getActiveQuest();
        if (quest == null) {
            return QuestStatus.none();
        }

        SlayerDefinition definition = SlayerRegistry.get(quest.type());
        SlayerTierDefinition tier = definition == null ? null : definition.tiers().get(quest.tier());
        return new QuestStatus(quest, definition, tier);
    }

    /**
     * Adds the sidebar block in Hypixel's layout, and says whether there was one:
     * <pre>
     * Slayer Quest
     * Inferno Demonlord I
     * (1.2k/4.8k) Combat XP     or  Slay the boss!   or  Boss slain!
     * </pre>
     */
    public static boolean appendSidebar(SkyBlockPlayer player, TextBody.Section section) {
        SlayerType type;
        SlayerTier tier;
        Text progress;

        SlainQuest slain = RECENTLY_SLAIN.get(player.getUuid());
        QuestStatus status = status(player);
        if (status.active() && status.tier() != null) {
            SlayerQuest quest = status.quest();
            type = quest.type();
            tier = quest.tier();
            if (quest.bossSpawned()) {
                progress = Text.of("<e>Slay the boss!");
            } else {
                progress = Text.of("<7>(<e>{}<7>/<c>{}<7>) Combat XP",
                    SlayerFormat.sidebar(quest.combatXp()),
                    SlayerFormat.sidebar(status.tier().requiredCombatXp()));
            }
        } else if (slain != null && System.currentTimeMillis() - slain.at() < SLAIN_DISPLAY_MS) {
            type = slain.type();
            tier = slain.tier();
            progress = Text.of("<a>Boss slain!");
        } else {
            return false;
        }

        section.line(Text.literal(" "));
        section.line(Text.of("Slayer Quest"));
        section.line(Text.of("<c>{} " + tier.colourTag() + "{}", type.displayName(), tier.numeral()));
        section.line(progress);
        return true;
    }

    private static void completeBoss(SkyBlockPlayer player, DatapointSlayer.SlayerData data, SlayerQuest quest, SlayerBossMob bossMob) {
        if (!bossMob.getOwnerUuid().equals(player.getUuid())) {
            return;
        }
        ACTIVE_BOSSES.remove(player.getUuid());

        SlayerDefinition definition = SlayerRegistry.get(quest.type());
        SlayerTierDefinition tierDefinition = definition == null ? null : definition.tiers().get(quest.tier());
        if (definition == null || tierDefinition == null) {
            return;
        }

        DatapointSlayer.SlayerProgress progress = data.progress(quest.type());
        int slayerXp = slayerXpReward(tierDefinition);
        progress.setXp(progress.getXp() + slayerXp);
        progress.addCompletion(quest.tier());
        data.setActiveQuest(null);
        save(player, data);
        RECENTLY_SLAIN.put(player.getUuid(), new SlainQuest(quest.type(), quest.tier(), System.currentTimeMillis()));

        player.sendMessage("  <6><l>NICE! SLAYER BOSS SLAIN!");
        player.sendMessage("  <a><l>SLAYER QUEST COMPLETE!");
        int level = definition.levelForXp(progress.getXp());
        Integer nextLevelXp = definition.xpForLevel(level + 1);
        if (nextLevelXp == null) {
            player.sendMessage("   <e>{} Slayer LVL {} <5>- <a><l>LVL MAXED OUT!",
                quest.type().categoryName(), level);
        } else {
            player.sendMessage("   <e>{} Slayer LVL {} <5>- <7>Next LVL in <d>{} XP<7>!",
                quest.type().categoryName(), level, SlayerFormat.commas(nextLevelXp - progress.getXp()));
        }
        player.playSound(Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 1f, 1f));
    }

    private static void failQuest(SkyBlockPlayer player, String reason) {
        despawnBoss(player.getUuid());
        DatapointSlayer.SlayerData data = data(player);
        if (data.getActiveQuest() == null) {
            return;
        }
        data.setActiveQuest(null);
        save(player, data);
        player.sendMessage("  <c><l>SLAYER QUEST FAILED!");
        player.sendMessage("   <5><l>»</l> <7>{}", reason);
    }

    private static void tick() {
        long now = System.currentTimeMillis();
        RECENTLY_SLAIN.values().removeIf(slain -> now - slain.at() > SLAIN_DISPLAY_MS * 4);

        ACTIVE_BOSSES.forEach((owner, active) -> {
            if (active.boss().isRemoved() || active.boss().isDead()) {
                // Killed by something that is not the owner, or removed by hand.
                ACTIVE_BOSSES.remove(owner, active);
                return;
            }
            if (now >= active.deadline()) {
                SkyBlockPlayer player = loadedPlayer(owner);
                if (player != null) {
                    failQuest(player, "Your Slayer boss ran out of time!");
                } else {
                    despawnBoss(owner);
                }
            }
        });

        for (SkyBlockPlayer player : SkyBlockGenericLoader.getLoadedPlayers()) {
            if (player.getSkyblockDataHandler() == null) {
                continue;
            }
            long seen = SEEN_SINCE.computeIfAbsent(player.getUuid(), uuid -> now);
            SlayerQuest quest = data(player).getActiveQuest();
            // A boss lives on the server it was spawned on. A quest that arrives here with its boss
            // already up (server change, crash) has lost it.
            if (quest != null && quest.bossSpawned() && !ACTIVE_BOSSES.containsKey(player.getUuid())
                && now - seen > JOIN_GRACE_MS) {
                failQuest(player, "Your Slayer boss despawned!");
            }
        }
    }

    private static @Nullable SkyBlockPlayer loadedPlayer(UUID uuid) {
        for (SkyBlockPlayer player : SkyBlockGenericLoader.getLoadedPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private static void despawnBoss(UUID owner) {
        ActiveBoss active = ACTIVE_BOSSES.remove(owner);
        if (active != null && !active.boss().isRemoved()) {
            active.boss().despawnBoss();
        }
    }

    private static int questCost(SlayerTierDefinition tierDefinition) {
        if (ElectionManager.isPerkActive(SkyBlockMayor.Perk.SLASHED_PRICING)) {
            return Math.max(1, tierDefinition.cost() / 2);
        }
        return tierDefinition.cost();
    }

    private static int slayerXpReward(SlayerTierDefinition tierDefinition) {
        if (ElectionManager.isPerkActive(SkyBlockMayor.Perk.SLAYER_XP_BUFF)) {
            return (int) Math.round(tierDefinition.slayerXp() * 1.25D);
        }
        return tierDefinition.slayerXp();
    }

    private static @Nullable SlayerBossMob spawnBoss(SkyBlockPlayer player, SlayerDefinition definition, SlayerTierDefinition tierDefinition, Pos position) {
        Instance instance = player.getInstance();
        if (instance == null) {
            return null;
        }

        SlayerBossProfile profile = new SlayerBossProfile(
            definition.type(),
            tierDefinition,
            definition.targetMobTypes(),
            definition.type().entityType()
        );
        SlayerBossMob mob = SlayerBossMob.create(player.getUuid(), player.getUsername(), profile);
        long deadline = System.currentTimeMillis() + tierDefinition.timeLimitSeconds() * 1000L;
        ACTIVE_BOSSES.put(player.getUuid(), new ActiveBoss(mob, deadline));
        mob.setInstance(instance, position);
        player.playSound(Sound.sound(Key.key("entity.wither.spawn"), Sound.Source.HOSTILE, 1f, 1f));
        return mob;
    }

    private static DatapointSlayer.SlayerData data(SkyBlockPlayer player) {
        return player.getSkyblockDataHandler().get(SkyBlockDataHandler.Data.SLAYER, DatapointSlayer.class).getValue();
    }

    private static void save(SkyBlockPlayer player, DatapointSlayer.SlayerData data) {
        player.getSkyblockDataHandler().get(SkyBlockDataHandler.Data.SLAYER, DatapointSlayer.class).setValue(data);
    }

    private record ActiveBoss(SlayerBossMob boss, long deadline) {
    }

    private record SlainQuest(SlayerType type, SlayerTier tier, long at) {
    }

    public record QuestStatus(
        @Nullable SlayerQuest quest,
        @Nullable SlayerDefinition definition,
        @Nullable SlayerTierDefinition tier
    ) {
        public static QuestStatus none() {
            return new QuestStatus(null, null, null);
        }

        public boolean active() {
            return quest != null;
        }
    }

    public record StartResult(boolean success, String message, Optional<SlayerQuest> quest) {
        public static StartResult started(SlayerQuest quest) {
            return new StartResult(true, "Started", Optional.of(quest));
        }

        public static StartResult alreadyActive(SlayerQuest quest) {
            return new StartResult(false, "You already have an active Slayer quest.", Optional.of(quest));
        }

        public static StartResult unavailable(SlayerType type) {
            return new StartResult(false, type.displayName() + " is not available here.", Optional.empty());
        }

        public static StartResult missingTier(SlayerType type, SlayerTier tier) {
            return new StartResult(false, type.displayName() + " " + tier.numeral() + " is not configured.", Optional.empty());
        }

        public static StartResult notEnoughCoins(int cost) {
            return new StartResult(false, "You need " + cost + " coins to start this quest.", Optional.empty());
        }

        public static StartResult locked(SlayerUnlockRequirement requirement) {
            return new StartResult(false, "You must slay " + requirement.type().displayName() + " "
                + requirement.tier().numeral() + " first.", Optional.empty());
        }
    }
}
