package net.swofty.type.skyblockgeneric.slayer.blaze;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import net.swofty.commons.skyblock.item.ItemType;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.item.SkyBlockItem;
import net.swofty.type.skyblockgeneric.item.updater.PlayerItemUpdater;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The Blaze slayer daggers and their attunements.
 *
 * <p>Two lines, each switching between two attunements on right click:
 * <ul>
 *   <li>Firedust → Kindlebane → Pyrochaos: Ashen (stone sword look) / Auric (golden sword look)</li>
 *   <li>Twilight → Mawdredge → Deathripper: Spirit (iron sword look) / Crystal (diamond sword look)</li>
 * </ul>
 * Hypixel's internal ids for them are FIREDUST, BURSTFIRE, HEARTFIRE and MAWDUST, BURSTMAW,
 * HEARTMAW. Switching shows the attunement as a title, which SkyHanni's dagger helper reads, and the
 * lore carries {@code Attuned: Ashen}, which it reads from the inventory.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlazeDaggers {
    private static final Map<ItemType, Attunement[]> DAGGERS = Map.of(
        ItemType.FIREDUST_DAGGER, new Attunement[]{Attunement.ASHEN, Attunement.AURIC},
        ItemType.BURSTFIRE_DAGGER, new Attunement[]{Attunement.ASHEN, Attunement.AURIC},
        ItemType.HEARTFIRE_DAGGER, new Attunement[]{Attunement.ASHEN, Attunement.AURIC},
        ItemType.MAWDUST_DAGGER, new Attunement[]{Attunement.SPIRIT, Attunement.CRYSTAL},
        ItemType.BURSTMAW_DAGGER, new Attunement[]{Attunement.SPIRIT, Attunement.CRYSTAL},
        ItemType.HEARTMAW_DAGGER, new Attunement[]{Attunement.SPIRIT, Attunement.CRYSTAL}
    );

    public static boolean isDagger(@Nullable SkyBlockItem item) {
        return item != null && item.getAttributeHandler().getPotentialType() != null
            && DAGGERS.containsKey(item.getAttributeHandler().getPotentialType());
    }

    /** The attunement the dagger is set to, or null if the item is no blaze dagger. */
    public static @Nullable Attunement attunementOf(@Nullable SkyBlockItem item) {
        if (!isDagger(item)) {
            return null;
        }
        Attunement[] modes = DAGGERS.get(item.getAttributeHandler().getPotentialType());
        String stored = (String) item.getAttribute("blaze_attunement").getValue();
        for (Attunement mode : modes) {
            if (mode.name().equals(stored)) {
                return mode;
            }
        }
        return modes[0];
    }

    /** The two attunements a dagger switches between, or null if the item is no blaze dagger. */
    public static Attunement @Nullable [] modesOf(@Nullable SkyBlockItem item) {
        return isDagger(item) ? DAGGERS.get(item.getAttributeHandler().getPotentialType()).clone() : null;
    }

    /** Whether the dagger is on the second of its two attunements (golden / diamond sword look). */
    public static boolean onSecondMode(SkyBlockItem item) {
        if (!isDagger(item)) {
            return false;
        }
        Attunement current = attunementOf(item);
        return current != null && current == DAGGERS.get(item.getAttributeHandler().getPotentialType())[1];
    }

    /** The vanilla look Hypixel switches the dagger to on its second attunement. */
    public static @Nullable String secondModeModel(SkyBlockItem item) {
        if (!isDagger(item)) {
            return null;
        }
        Attunement[] modes = DAGGERS.get(item.getAttributeHandler().getPotentialType());
        return modes[1] == Attunement.AURIC ? "minecraft:golden_sword" : "minecraft:diamond_sword";
    }

    /** Right click: swap to the other attunement, show it as a title, update the held item. */
    public static void attune(SkyBlockPlayer player, SkyBlockItem item) {
        if (!isDagger(item)) {
            return;
        }
        Attunement[] modes = DAGGERS.get(item.getAttributeHandler().getPotentialType());
        Attunement next = attunementOf(item) == modes[0] ? modes[1] : modes[0];
        item.getAttribute("blaze_attunement").setValue(next.name());
        player.setItemInMainHand(PlayerItemUpdater.playerUpdate(player, item.getItemStack()).build());
        player.showTitle(Title.title(Text.of(next.formatted()).asComponent(), net.kyori.adventure.text.Component.empty(),
            Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(800), java.time.Duration.ofMillis(200))));
        player.playSound(Sound.sound(Key.key("entity.blaze.shoot"), Sound.Source.PLAYER, 0.6f, 1.6f));
    }
}
