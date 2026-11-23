package net.swofty.type.skyblockgeneric.commands;

import net.minestom.server.command.builder.arguments.ArgumentType;
import net.swofty.type.generic.command.CommandParameters;
import net.swofty.type.generic.command.HypixelCommand;
import net.swofty.type.generic.user.categories.Rank;
import net.swofty.type.skyblockgeneric.slayer.MaddoxPhone;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/**
 * {@code /call slayer}: rings Maddox from anywhere, like Hypixel's shortcut that Skyblocker's
 * "Call Maddox" button runs.
 */
@CommandParameters(labels = "call", description = "Calls a contact", usage = "/call slayer",
    permission = Rank.DEFAULT, allowsConsole = false)
public class CallCommand extends HypixelCommand {
    @Override
    public void registerUsage(MinestomCommand command) {
        command.addSyntax((sender, context) -> {
            if (!permissionCheck(sender)) return;
            MaddoxPhone.call((SkyBlockPlayer) sender);
        }, ArgumentType.Literal("slayer"));
    }
}
