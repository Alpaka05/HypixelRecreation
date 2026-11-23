package net.swofty.type.skyblockgeneric.entity.mob.mobs.crimsonisle;

import java.util.List;
import lombok.NonNull;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.Material;
import net.swofty.commons.skyblock.item.ItemType;
import net.swofty.commons.skyblock.statistics.ItemStatistic;
import net.swofty.commons.skyblock.statistics.ItemStatistics;
import net.swofty.type.generic.gui.inventory.item.GUIMaterial;
import net.swofty.type.skyblockgeneric.entity.mob.MobType;
import net.swofty.type.skyblockgeneric.loottable.OtherLoot;
import net.swofty.type.skyblockgeneric.loottable.SkyBlockLootTable;
import net.swofty.type.skyblockgeneric.region.RegionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Millennia-Aged Blaze (Smoldering Tomb): Lv290, 12,000,000 health, 435 Combat XP. Counts for the Blaze slayer. Values as of Hypixel's 2026 mob level rework. */
public class MobMillenniaAgedBlaze extends CrimsonBlazeMob {

    public MobMillenniaAgedBlaze() {
        super(EntityType.BLAZE);
    }

    @Override
    public String getDisplayName() {
        return "Millennia-Aged Blaze";
    }

    @Override
    public Integer getLevel() {
        return 290;
    }

    @Override
    protected RegionType homeRegion() {
        return RegionType.SMOLDERING_TOMB;
    }

    @Override
    public ItemStatistics getBaseStatistics() {
        return ItemStatistics.builder()
                .withBase(ItemStatistic.HEALTH, 12000000D)
                .withBase(ItemStatistic.DAMAGE, 6000D)
                .withBase(ItemStatistic.SPEED, 100D)
                .build();
    }

    @Override
    public @Nullable SkyBlockLootTable getLootTable() {
        return new SkyBlockLootTable() {
            @Override
            public @NonNull List<LootRecord> getLootTable() {
                return List.of(
                        new LootRecord(ItemType.BLAZE_ROD, makeAmountBetween(16, 16), 100),
                        new LootRecord(ItemType.ENCHANTED_BLAZE_POWDER, makeAmountBetween(1, 1), 20),
                        new LootRecord(ItemType.ENCHANTED_BLAZE_ROD, makeAmountBetween(1, 1), 0.1)
                );
            }

            @Override
            public @NotNull CalculationMode getCalculationMode() {
                return CalculationMode.CALCULATE_INDIVIDUAL;
            }
        };
    }

    @Override
    public OtherLoot getOtherLoot() {
        return new OtherLoot(435, 96, 319);
    }

    @Override
    public List<MobType> getMobTypes() {
        return List.of(MobType.INFERNAL, MobType.SUBTERRANEAN);
    }

    @Override
    public String getMobID() {
        return "MILLENNIA_AGED_BLAZE";
    }

    @Override
    public GUIMaterial getGuiMaterial() {
        return new GUIMaterial(Material.FIRE_CHARGE);
    }

    @Override
    public List<Populator> getPopulators() {
        return List.of(new Populator(RegionType.SMOLDERING_TOMB, 3));
    }
}
