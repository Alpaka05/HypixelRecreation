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

/** Smoldering Blaze (Smoldering Tomb): Lv201, 2,000,000 health, 301 Combat XP. Counts for the Blaze slayer. Values as of Hypixel's 2026 mob level rework. */
public class MobSmolderingBlaze extends CrimsonBlazeMob {

    public MobSmolderingBlaze() {
        super(EntityType.BLAZE);
    }

    @Override
    public String getDisplayName() {
        return "Smoldering Blaze";
    }

    @Override
    public Integer getLevel() {
        return 201;
    }

    @Override
    protected RegionType homeRegion() {
        return RegionType.SMOLDERING_TOMB;
    }

    @Override
    public ItemStatistics getBaseStatistics() {
        return ItemStatistics.builder()
                .withBase(ItemStatistic.HEALTH, 2000000D)
                .withBase(ItemStatistic.DAMAGE, 2500D)
                .withBase(ItemStatistic.SPEED, 100D)
                .build();
    }

    @Override
    public @Nullable SkyBlockLootTable getLootTable() {
        return new SkyBlockLootTable() {
            @Override
            public @NonNull List<LootRecord> getLootTable() {
                return List.of(
                        new LootRecord(ItemType.BLAZE_ROD, makeAmountBetween(8, 8), 100),
                        new LootRecord(ItemType.ENCHANTED_BLAZE_POWDER, makeAmountBetween(1, 1), 5)
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
        return new OtherLoot(301, 67, 221);
    }

    @Override
    public List<MobType> getMobTypes() {
        return List.of(MobType.INFERNAL, MobType.SUBTERRANEAN);
    }

    @Override
    public String getMobID() {
        return "SMOLDERING_BLAZE";
    }

    @Override
    public GUIMaterial getGuiMaterial() {
        return new GUIMaterial(Material.MAGMA_CREAM);
    }

    @Override
    public List<Populator> getPopulators() {
        return List.of(new Populator(RegionType.SMOLDERING_TOMB, 10));
    }
}
