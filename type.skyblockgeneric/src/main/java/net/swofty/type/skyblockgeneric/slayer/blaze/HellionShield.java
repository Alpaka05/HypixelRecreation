package net.swofty.type.skyblockgeneric.slayer.blaze;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.damage.Damage;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.item.SkyBlockItem;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A Hellion Shield (Inferno Demonlord II+ and its demons).
 *
 * <p>Damage that is not a strike from a blaze dagger on the shield's attunement is cut by 99 %.
 * After {@link #HITS_PER_SWITCH} correct strikes the shield moves on to its next attunement. The
 * shield's state is shown on a name tag as {@code ASHEN ♨8}: the attunement, then the strikes left.
 * SkyHanni reads the number as the single character after {@code " ♨"}; Skyblocker and AlpakaAddons
 * colour the mob by the word.
 */
public final class HellionShield {
    public static final int HITS_PER_SWITCH = 8;
    private static final float REDUCTION = 0.01F;
    /** One swing can arrive as several damage events (enchant procs); count it once. */
    private static final long SAME_SWING_MS = 120L;

    private final Attunement[] cycle;
    private int index;
    private int hitsLeft = HITS_PER_SWITCH;
    private long lastCountedHit;

    /** @param cycle the attunements in the order the shield goes through them */
    public HellionShield(Attunement... cycle) {
        this.cycle = cycle;
    }

    /** The boss's own shield: Ashen, Spirit, Auric, Crystal, Ashen ... */
    public static HellionShield boss() {
        return new HellionShield(Attunement.ASHEN, Attunement.SPIRIT, Attunement.AURIC, Attunement.CRYSTAL);
    }

    public Attunement current() {
        return cycle[index];
    }

    public int hitsLeft() {
        return hitsLeft;
    }

    /** {@code ASHEN ♨8}, bold and in the attunement's colour. */
    public Text line() {
        return Text.of(current().colourTag() + "<l>{} ♨{}", current().name(), hitsLeft);
    }

    /**
     * Applies the shield to one incoming hit and returns the damage that gets through.
     *
     * @param onSwitch run when this hit used up the current attunement (after the switch)
     */
    public float apply(Damage damage, float amount, @Nullable Runnable onSwitch) {
        Entity attacker = damage.getAttacker();
        if (!(attacker instanceof SkyBlockPlayer player)) {
            return amount * REDUCTION;
        }
        SkyBlockItem held = new SkyBlockItem(player.getItemInMainHand());
        Attunement attunement = BlazeDaggers.attunementOf(held);
        if (attunement == null) {
            player.sendMessage("<c>Your hit was reduced by Hellion Shield!");
            return amount * REDUCTION;
        }
        if (attunement != current()) {
            player.sendMessage(Text.of("<c>Strike using the " + current().formatted() + "</l> <c>attunement on your dagger!"));
            return amount * REDUCTION;
        }

        long now = System.currentTimeMillis();
        if (now - lastCountedHit > SAME_SWING_MS) {
            lastCountedHit = now;
            if (--hitsLeft <= 0) {
                index = (index + 1) % cycle.length;
                hitsLeft = HITS_PER_SWITCH;
                if (onSwitch != null) {
                    onSwitch.run();
                }
            }
        }
        return amount;
    }
}
