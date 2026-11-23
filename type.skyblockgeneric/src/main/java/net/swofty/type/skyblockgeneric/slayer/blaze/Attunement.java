package net.swofty.type.skyblockgeneric.slayer.blaze;

/**
 * The four Hellion Shield attunements, written the way Hypixel writes them.
 *
 * <p>The formatted names are what SkyHanni matches in titles and chat
 * ({@code §8§lASHEN}, {@code §e§lAURIC}, {@code §f§lSPIRIT}, {@code §b§lCRYSTAL}); Skyblocker and
 * AlpakaAddons colour the boss by the plain word on its name tag.
 */
public enum Attunement {
    ASHEN("<8>", "Ashen"),
    AURIC("<e>", "Auric"),
    SPIRIT("<f>", "Spirit"),
    CRYSTAL("<b>", "Crystal");

    private final String colourTag;
    private final String cleanName;

    Attunement(String colourTag, String cleanName) {
        this.colourTag = colourTag;
        this.cleanName = cleanName;
    }

    public String colourTag() {
        return colourTag;
    }

    public String cleanName() {
        return cleanName;
    }

    /** {@code <8><l>ASHEN} - bold, upper case, in its colour. */
    public String formatted() {
        return colourTag + "<l>" + name();
    }
}
