package net.swofty.type.skyblockgeneric.gui.inventories.slayer;

import java.util.ArrayList;
import java.util.List;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.Material;
import net.swofty.commons.text.Text;
import net.swofty.type.generic.gui.inventory.ItemStacks;
import net.swofty.type.generic.gui.v2.Components;
import net.swofty.type.generic.gui.v2.DefaultState;
import net.swofty.type.generic.gui.v2.StatelessView;
import net.swofty.type.generic.gui.v2.ViewConfiguration;
import net.swofty.type.generic.gui.v2.ViewLayout;
import net.swofty.type.generic.gui.v2.context.ViewContext;
import net.swofty.type.skyblockgeneric.slayer.SlayerDefinition;
import net.swofty.type.skyblockgeneric.slayer.SlayerFormat;
import net.swofty.type.skyblockgeneric.slayer.SlayerRegistry;
import net.swofty.type.skyblockgeneric.slayer.SlayerService;
import net.swofty.type.skyblockgeneric.slayer.SlayerTier;
import net.swofty.type.skyblockgeneric.slayer.SlayerTierDefinition;
import net.swofty.type.skyblockgeneric.slayer.SlayerType;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/** The tier picker for one boss; clicking a tier starts the quest straight away. */
public class GUISlayerTiers extends StatelessView {
    private static final int[] TIER_SLOTS = {11, 12, 13, 14, 15};
    private static final Material[] TIER_MATERIALS = {
        Material.LIME_DYE, Material.YELLOW_DYE, Material.RED_DYE, Material.ORANGE_DYE, Material.PURPLE_DYE
    };

    private final SlayerType type;

    public GUISlayerTiers(SlayerType type) {
        this.type = type;
    }

    @Override
    public ViewConfiguration<DefaultState> configuration() {
        return new ViewConfiguration<>("Slay " + type.displayName(), InventoryType.CHEST_4_ROW);
    }

    @Override
    public void layout(ViewLayout<DefaultState> layout, DefaultState state, ViewContext ctx) {
        Components.fill(layout);
        Components.close(layout, 31);
        Components.back(layout, 30, ctx);

        SlayerDefinition definition = SlayerRegistry.get(type);
        if (definition == null) {
            return;
        }

        SlayerTier[] tiers = SlayerTier.values();
        for (int i = 0; i < tiers.length; i++) {
            SlayerTier tier = tiers[i];
            SlayerTierDefinition tierDefinition = definition.tiers().get(tier);
            if (tierDefinition == null) {
                continue;
            }
            Material material = TIER_MATERIALS[i];
            layout.slot(TIER_SLOTS[i], (s, c) -> tierItem(tierDefinition, material), (click, c) -> {
                SkyBlockPlayer player = (SkyBlockPlayer) c.player();
                SlayerService.StartResult result = SlayerService.startQuest(player, type, tier);
                if (!result.success()) {
                    player.sendMessage("<c>{}", result.message());
                }
                player.closeInventory();
            });
        }
    }

    private net.minestom.server.item.ItemStack.Builder tierItem(SlayerTierDefinition tier, Material material) {
        List<Text> lore = new ArrayList<>();
        lore.add(Text.of("<8>{} Slayer", type.categoryName()));
        lore.add(Text.literal(" "));
        lore.add(Text.of("<7>Health: <c>{}<c>❤", SlayerFormat.health(tier.bossHealth())));
        lore.add(Text.of("<7>Damage: <c>{} <7>per second", SlayerFormat.commas(Math.round(tier.bossDamage()))));
        lore.add(Text.literal(" "));
        lore.add(Text.of("<7>Reward: <d>{} {} Slayer XP", SlayerFormat.commas(tier.slayerXp()), type.categoryName()));
        lore.add(Text.of("<7>Required: <c>{} Combat XP", SlayerFormat.commas(tier.requiredCombatXp())));
        lore.add(Text.of("<7>Time limit: <e>{}", SlayerFormat.timer(tier.timeLimitSeconds() * 1000L)));
        lore.add(Text.literal(" "));
        lore.add(Text.of("<7>Cost to start: <6>{} coins", SlayerFormat.commas(tier.cost())));
        lore.add(Text.literal(" "));
        lore.add(Text.of("<e>Click to slay!"));
        return ItemStacks.item(material, Text.of("<c>{} " + tier.tier().colourTag() + "{}", type.displayName(), tier.tier().numeral()), lore);
    }
}
