package net.swofty.commons.skyblock.item.attribute.attributes;

import net.swofty.commons.skyblock.item.attribute.ItemAttribute;
import net.swofty.commons.skyblock.statistics.ItemStatistics;
import org.jetbrains.annotations.Nullable;

/** The Hellion Shield attunement a Blaze slayer dagger is set to (ASHEN, AURIC, SPIRIT, CRYSTAL), or empty. */
public class ItemAttributeBlazeAttunement extends ItemAttribute<String> {
    @Override
    public String getKey() {
        return "blaze_attunement";
    }

    @Override
    public String getDefaultValue(@Nullable ItemStatistics defaultStatistics) {
        return "";
    }

    @Override
    public String loadFromString(String string) {
        return string == null ? "" : string;
    }

    @Override
    public String saveIntoString() {
        return getValue() == null ? "" : getValue();
    }
}
