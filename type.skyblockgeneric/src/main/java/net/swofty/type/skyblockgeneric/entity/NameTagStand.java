package net.swofty.type.skyblockgeneric.entity;

import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.swofty.commons.text.Text;

/**
 * One line of a mob's name tag, as Hypixel draws it: an invisible marker armour stand carrying a
 * custom name.
 *
 * <p>Hypixel never names the mob itself. Every mob gets a stand whose entity id is the mob's id + 1,
 * and slayer bosses get further stands after that (timer, owner line). Client mods depend on this:
 * SkyHanni's {@code MobUtils.getArmorStand} looks up id + 1, and slayer helpers find a boss by the
 * stand reading {@code <player>'s SLAYER BOSS}. A stand therefore has to be constructed right after
 * the mob it belongs to, so it is handed the next id.
 */
public class NameTagStand extends Entity {

    public NameTagStand(Text text) {
        super(EntityType.ARMOR_STAND);
        setNoGravity(true);
        setInvisible(true);
        setCustomNameVisible(true);
        editEntityMeta(ArmorStandMeta.class, meta -> {
            meta.setMarker(true);
            meta.setSmall(true);
            meta.setHasNoBasePlate(true);
        });
        setText(text);
    }

    public void setText(Text text) {
        set(DataComponents.CUSTOM_NAME, text.asComponent());
    }
}
