package net.swofty.type.skyblockgeneric.slayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import net.swofty.type.skyblockgeneric.gui.inventories.slayer.GUISlayer;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/**
 * Calling Maddox (Maddox Batphone, {@code /call slayer}): three rings, an answer, then his Slayer
 * menu, with Hypixel's lines ({@code §e✆ Ring...}, {@code §a✆ Hello?}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaddoxPhone {
    private static final String[] ANSWERS = {
        "Hello?", "Someone answers!", "How does a lobster answer? Shello!", "Hey what do you need?",
        "You hear the line pick up...", "You again? What do you want this time?"
    };
    private static final Map<UUID, Long> CALLING = new ConcurrentHashMap<>();

    public static void call(SkyBlockPlayer player) {
        long now = System.currentTimeMillis();
        Long since = CALLING.get(player.getUuid());
        if (since != null && now - since < 3_000) {
            return;
        }
        CALLING.put(player.getUuid(), now);
        String[] rings = {"Ring...", "Ring... Ring...", "Ring... Ring... Ring..."};
        for (int i = 0; i < rings.length; i++) {
            String ring = rings[i];
            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                if (player.isOnline()) {
                    player.sendMessage("<e>✆ {}", ring);
                    player.playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.PLAYER, 0.6f, 1.6f));
                }
            }, TaskSchedule.tick(i * 10), TaskSchedule.stop());
        }
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            CALLING.remove(player.getUuid());
            if (!player.isOnline()) {
                return;
            }
            player.sendMessage("<a>✆ {} <2><l>[OPEN MENU]", ANSWERS[(int) (Math.random() * ANSWERS.length)]);
            player.openView(new GUISlayer());
        }, TaskSchedule.tick(30), TaskSchedule.stop());
    }
}
