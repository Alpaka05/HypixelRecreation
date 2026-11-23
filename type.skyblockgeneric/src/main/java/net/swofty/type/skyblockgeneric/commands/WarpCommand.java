package net.swofty.type.skyblockgeneric.commands;

import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.swofty.commons.ServerType;
import net.swofty.type.generic.HypixelConst;
import net.swofty.type.generic.command.CommandParameters;
import net.swofty.type.generic.command.HypixelCommand;
import net.swofty.type.generic.data.datapoints.DatapointStringList;
import net.swofty.type.generic.user.categories.Rank;
import net.swofty.type.skyblockgeneric.data.SkyBlockDataHandler;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;
import net.swofty.type.skyblockgeneric.warps.TravelScrollIslands;
import net.swofty.type.skyblockgeneric.warps.TravelScrollType;

import java.util.List;

@CommandParameters(description = "Warps to the given destination",
        usage = "/warp <warp>",
        permission = Rank.DEFAULT,
    labels = "teleportregion warp",
        allowsConsole = false)
public class WarpCommand extends HypixelCommand {
    @Override
    public void registerUsage(MinestomCommand command) {
        ArgumentString warpArgument = ArgumentType.String("warp");
        warpArgument.setSuggestionCallback(((sender, context, suggestion) -> {
            for (TravelScrollIslands island : TravelScrollIslands.values()) {
                suggest(suggestion, island.getInternalName(), island.getDescriptiveName());
            }
        }));

        command.addSyntax((sender, context) -> {
            if (!permissionCheck(sender)) return;
            final String warp = context.get(warpArgument);
            SkyBlockPlayer player = (SkyBlockPlayer) sender;

            // Hypixel's /warp smoldering (Travel Scroll to the Smoldering Tomb, Blaze Slayer 4).
            // Open to everyone here: this is a test server and the walk is 600 blocks.
            if (warp.equalsIgnoreCase("smoldering")) {
                net.minestom.server.coordinate.Pos tomb = new net.minestom.server.coordinate.Pos(-291.5, 124, -998.5, 180, 0);
                if (HypixelConst.getTypeLoader().getType() == ServerType.SKYBLOCK_CRIMSON_ISLE) {
                    player.teleport(tomb);
                    player.sendMessage("<7>You have been warped to <c>Smoldering Tomb<7>!");
                } else {
                    player.asProxyPlayer().transferToWithIndication(ServerType.SKYBLOCK_CRIMSON_ISLE).thenRun(() -> {
                        player.asProxyPlayer().sendMessage("<7>You have been warped to <c>Smoldering Tomb<7>!");
                        player.asProxyPlayer().teleport(tomb);
                    });
                }
                return;
            }

            TravelScrollIslands island = TravelScrollIslands.getFromInternalName(warp);
            if (island != null) {
                List<String> unlockedIslands = player.getSkyblockDataHandler()
                        .get(SkyBlockDataHandler.Data.VISITED_ISLANDS, DatapointStringList.class)
                        .getValue();
                // Staff may warp anywhere, to test islands on a fresh profile.
                boolean staff = player.getRank().isEqualOrHigherThan(Rank.STAFF);
                if (unlockedIslands.contains(warp) || staff) {
                    player.asProxyPlayer().transferToWithIndication(island.getServerType()).thenRun(() -> {
                        player.asProxyPlayer().sendMessage("<7>You have been warped to {}<7>!", island.getDescriptiveName());
                    });
                } else {
                    player.sendMessage("<c>You have not unlocked this warp.");
                }
                return;
            }

            TravelScrollType scroll = TravelScrollType.getFromInternalName(warp);
            if (scroll != null) {
                List<String> unlockedWarps = player.getSkyblockDataHandler()
                        .get(SkyBlockDataHandler.Data.USED_SCROLLS, DatapointStringList.class)
                        .getValue();
                TravelScrollIslands islandFromScroll = TravelScrollIslands.getFromTravelScroll(scroll);
                if (unlockedWarps.contains(warp)) {
                    ServerType serverType = islandFromScroll.getServerType();

                    if (HypixelConst.getTypeLoader().getType() == serverType) {
                        player.asProxyPlayer().sendMessage("<7>You have been warped to {}<7>!", scroll.getDisplayName());
                        player.teleport(scroll.getLocation());
                        return;
                    }

                    player.asProxyPlayer().transferToWithIndication(serverType).thenRun(() -> {
                        player.asProxyPlayer().sendMessage("<7>You have been warped to {}<7>!", scroll.getDisplayName());
                        player.asProxyPlayer().teleport(scroll.getLocation());
                    });
                } else {
                    player.sendMessage("<c>You have not unlocked this warp.");
                }
                return;
            }

            player.sendMessage("<c>Could not find a warp with the name '{}'.", warp);
        }, warpArgument);
    }
}
