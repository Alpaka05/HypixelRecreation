package net.swofty.type.skyblockgeneric.slayer;

public enum SlayerTier {
    I(1, "I", "<a>"),
    II(2, "II", "<e>"),
    III(3, "III", "<c>"),
    IV(4, "IV", "<4>"),
    V(5, "V", "<5>");

    private final int number;
    private final String numeral;
    private final String colourTag;

    SlayerTier(int number, String numeral, String colourTag) {
        this.number = number;
        this.numeral = numeral;
        this.colourTag = colourTag;
    }

    /** The colour Hypixel writes a tier's numeral in on the sidebar and on the boss name tag. */
    public String colourTag() {
        return colourTag;
    }

    public int number() {
        return number;
    }

    public String numeral() {
        return numeral;
    }
}
