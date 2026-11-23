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

/** Bezal (Stronghold): Lv144, 300,000 health, 216 Combat XP. Counts for the Blaze slayer. Values as of Hypixel's 2026 mob level rework. */
public class MobBezal extends CrimsonBlazeMob {

    public MobBezal() {
        super(EntityType.BLAZE);
    }

    @Override
    public String getDisplayName() {
        return "Bezal";
    }

    @Override
    public Integer getLevel() {
        return 144;
    }

    @Override
    protected RegionType homeRegion() {
        return RegionType.STRONGHOLD;
    }

    @Override
    public ItemStatistics getBaseStatistics() {
        return ItemStatistics.builder()
                .withBase(ItemStatistic.HEALTH, 300000D)
                .withBase(ItemStatistic.DAMAGE, 2000D)
                .withBase(ItemStatistic.SPEED, 100D)
                .build();
    }

    @Override
    public @Nullable SkyBlockLootTable getLootTable() {
        return new SkyBlockLootTable() {
            @Override
            public @NonNull List<LootRecord> getLootTable() {
                return List.of(
                        new LootRecord(ItemType.BLAZE_ROD, makeAmountBetween(4, 4), 100)
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
        return new OtherLoot(216, 48, 158);
    }

    @Override
    public List<MobType> getMobTypes() {
        return List.of(MobType.INFERNAL);
    }

    @Override
    public String getMobID() {
        return "BEZAL";
    }

    @Override
    public GUIMaterial getGuiMaterial() {
        return new GUIMaterial(Material.BLAZE_POWDER);
    }

    @Override
    public List<Populator> getPopulators() {
        return List.of(new Populator(RegionType.STRONGHOLD, 4));
    }
}
