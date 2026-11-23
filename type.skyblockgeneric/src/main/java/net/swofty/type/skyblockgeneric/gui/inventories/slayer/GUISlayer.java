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
import net.swofty.type.skyblockgeneric.data.SkyBlockDataHandler;
import net.swofty.type.skyblockgeneric.data.datapoints.DatapointSlayer;
import net.swofty.type.skyblockgeneric.slayer.SlayerDefinition;
import net.swofty.type.skyblockgeneric.slayer.SlayerFormat;
import net.swofty.type.skyblockgeneric.slayer.SlayerQuest;
import net.swofty.type.skyblockgeneric.slayer.SlayerRegistry;
import net.swofty.type.skyblockgeneric.slayer.SlayerService;
import net.swofty.type.skyblockgeneric.slayer.SlayerTierDefinition;
import net.swofty.type.skyblockgeneric.slayer.SlayerType;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/**
 * Maddox's "Slayer" menu, opened from Maddox in the Hub and from the Maddox Batphone.
 *
 * <p>Titled "Slayer" like Hypixel's, with the "Slayer Leaderboards" item whose lore lists lifetime
 * XP per boss ({@code Inferno Demonlord: 1,234 (#1)}); AlpakaAddons reads exactly that.
 */
public class GUISlayer extends StatelessView {
    private static final int[] BOSS_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final SlayerType[] ORDER = {
        SlayerType.REVENANT_HORROR, SlayerType.TARANTULA_BROODFATHER, SlayerType.SVEN_PACKMASTER,
        SlayerType.VOIDGLOOM_SERAPH, SlayerType.INFERNO_DEMONLORD, SlayerType.RIFTSTALKER_BLOODFIEND
    };

    @Override
    public ViewConfiguration<DefaultState> configuration() {
        return new ViewConfiguration<>("Slayer", InventoryType.CHEST_4_ROW);
    }

    @Override
    public void layout(ViewLayout<DefaultState> layout, DefaultState state, ViewContext ctx) {
        Components.fill(layout);
        Components.close(layout, 31);

        for (int i = 0; i < ORDER.length; i++) {
            SlayerType type = ORDER[i];
            layout.slot(BOSS_SLOTS[i], (s, c) -> bossItem((SkyBlockPlayer) c.player(), type), (click, c) -> {
                SkyBlockPlayer player = (SkyBlockPlayer) c.player();
                SlayerDefinition definition = SlayerRegistry.get(type);
                if (definition == null || definition.tiers().isEmpty()) {
                    player.sendMessage("<c>This Slayer is not available yet!");
                    return;
                }
                c.push(new GUISlayerTiers(type));
            });
        }

        layout.slot(22, (s, c) -> activeQuestItem((SkyBlockPlayer) c.player()), (click, c) -> {
            SkyBlockPlayer player = (SkyBlockPlayer) c.player();
            if (SlayerService.status(player).active()) {
                SlayerService.cancelQuest(player);
                player.closeInventory();
            }
        });

        layout.slot(27, (s, c) -> leaderboardItem((SkyBlockPlayer) c.player()));
    }

    private static net.minestom.server.item.ItemStack.Builder bossItem(SkyBlockPlayer player, SlayerType type) {
        SlayerDefinition definition = SlayerRegistry.get(type);
        List<Text> lore = new ArrayList<>();
        if (definition == null || definition.tiers().isEmpty()) {
            lore.add(Text.of("<7>Coming soon!"));
            return ItemStacks.item(type.menuMaterial(), Text.of("<c>☠ {}", type.displayName()), lore);
        }
        DatapointSlayer.SlayerProgress progress = data(player).progress(type);
        int level = definition.levelForXp(progress.getXp());
        lore.add(Text.of("<8>{} Slayer", type.categoryName()));
        lore.add(Text.literal(" "));
        lore.add(Text.of("<7>{} Slayer: <e>LVL {}", type.categoryName(), level));
        lore.add(Text.of("<7>Slayer XP: <d>{}", SlayerFormat.commas(progress.getXp())));
        lore.add(Text.literal(" "));
        lore.add(Text.of("<e>Click to view boss!"));
        return ItemStacks.item(type.menuMaterial(), Text.of("<c>☠ <e>{}", type.displayName()), lore);
    }

    private static net.minestom.server.item.ItemStack.Builder activeQuestItem(SkyBlockPlayer player) {
        SlayerService.QuestStatus status = SlayerService.status(player);
        if (!status.active() || status.tier() == null) {
            return ItemStacks.item(Material.MAP, Text.of("<a>No Active Quest"),
                List.of(Text.of("<7>Pick a boss above to start a"), Text.of("<7>Slayer Quest.")));
        }
        SlayerQuest quest = status.quest();
        SlayerTierDefinition tier = status.tier();
        List<Text> lore = new ArrayList<>();
        lore.add(Text.of("<7>Boss: <c>{} {}", quest.type().displayName(), quest.tier().numeral()));
        if (quest.bossSpawned()) {
            lore.add(Text.of("<7>Status: <e>Slay the boss!"));
        } else {
            lore.add(Text.of("<7>Progress: <e>{}<7>/<c>{} <7>Combat XP",
                SlayerFormat.commas(quest.combatXp()), SlayerFormat.commas(tier.requiredCombatXp())));
        }
        lore.add(Text.literal(" "));
        lore.add(Text.of("<c>Click to cancel the quest!"));
        return ItemStacks.item(Material.FILLED_MAP, Text.of("<e>Ongoing Quest"), lore);
    }

    private static net.minestom.server.item.ItemStack.Builder leaderboardItem(SkyBlockPlayer player) {
        DatapointSlayer.SlayerData data = data(player);
        List<Text> lore = new ArrayList<>();
        lore.add(Text.of("<7>Your lifetime Slayer XP:"));
        lore.add(Text.literal(" "));
        for (SlayerType type : ORDER) {
            SlayerDefinition definition = SlayerRegistry.get(type);
            if (definition == null || definition.tiers().isEmpty()) {
                continue;
            }
            lore.add(Text.of("<7>{}: <d>{} <8>(#1)", type.displayName(), SlayerFormat.commas(data.progress(type).getXp())));
        }
        return ItemStacks.item(Material.PAINTING, Text.of("<a>Slayer Leaderboards"), lore);
    }

    static DatapointSlayer.SlayerData data(SkyBlockPlayer player) {
        return player.getSkyblockDataHandler().get(SkyBlockDataHandler.Data.SLAYER, DatapointSlayer.class).getValue();
    }
}
