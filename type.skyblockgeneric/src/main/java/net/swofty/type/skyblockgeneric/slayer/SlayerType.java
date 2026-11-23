package net.swofty.type.skyblockgeneric.slayer;

import net.minestom.server.entity.EntityType;
import net.minestom.server.item.Material;
import net.swofty.commons.text.Text;

public enum SlayerType {
    REVENANT_HORROR("Zombie", "Zombies", "Revenant Horror", "<c>", EntityType.ZOMBIE, Material.ROTTEN_FLESH),
    TARANTULA_BROODFATHER("Spider", "Spiders", "Tarantula Broodfather", "<5>", EntityType.SPIDER, Material.SPIDER_EYE),
    SVEN_PACKMASTER("Wolf", "Wolves", "Sven Packmaster", "<f>", EntityType.WOLF, Material.BONE),
    VOIDGLOOM_SERAPH("Enderman", "Endermen", "Voidgloom Seraph", "<5>", EntityType.ENDERMAN, Material.ENDER_PEARL),
    RIFTSTALKER_BLOODFIEND("Vampire", "Vampires", "Riftstalker Bloodfiend", "<4>", EntityType.ZOMBIE, Material.RED_DYE),
    INFERNO_DEMONLORD("Blaze", "Blazes", "Inferno Demonlord", "<6>", EntityType.BLAZE, Material.BLAZE_ROD);

    private final String categoryName;
    private final String pluralMobName;
    private final String displayName;
    private final String colourTag;
    private final EntityType entityType;
    private final Material menuMaterial;

    SlayerType(String categoryName, String pluralMobName, String displayName, String colourTag, EntityType entityType, Material menuMaterial) {
        this.categoryName = categoryName;
        this.pluralMobName = pluralMobName;
        this.displayName = displayName;
        this.colourTag = colourTag;
        this.entityType = entityType;
        this.menuMaterial = menuMaterial;
    }

    public String categoryName() {
        return categoryName;
    }

    /** The mobs a quest asks for, as Hypixel writes them: "Slay 2,400 Combat XP worth of Blazes." */
    public String pluralMobName() {
        return pluralMobName;
    }

    public String displayName() {
        return displayName;
    }

    public Text coloredName() {
        return Text.of(colourTag + "{}", displayName);
    }

    public EntityType entityType() {
        return entityType;
    }

    public Material menuMaterial() {
        return menuMaterial;
    }
}
