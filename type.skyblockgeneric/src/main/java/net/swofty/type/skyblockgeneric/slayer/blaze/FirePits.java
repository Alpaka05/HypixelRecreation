package net.swofty.type.skyblockgeneric.slayer.blaze;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/**
 * The Inferno Demonlord's fire pits (DDR Apocalypse).
 *
 * <p>A small cross of orange terracotta appears in the floor near the player and turns to fire
 * 1.5 s later; standing in it burns. The floor really changes: AlpakaAddons' fire pit marker and
 * SkyHanni's warning work from exactly these block updates (a block turning into clay or
 * terracotta during the fight), and the column goes away when the block is turned back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FirePits {
    private static final Vec[] CROSS = {
        Vec.ZERO, new Vec(1, 0, 0), new Vec(-1, 0, 0), new Vec(0, 0, 1), new Vec(0, 0, -1)
    };
    private static final int WARN_TICKS = 30;
    private static final int BURN_TICKS = 80;

    /** Blocks currently replaced by a pit, with what was there before. Restored on shutdown too. */
    private static final Map<Key, Block> REPLACED = new LinkedHashMap<>();

    /**
     * Opens one pit under or next to {@code around}.
     *
     * @param burn called once per second for every player standing in the burning pit
     * @param track receives the pit's tasks so the boss can cancel them when it goes
     */
    public static void open(Instance instance, Pos around, BiConsumer<SkyBlockPlayer, Integer> burn, java.util.function.Consumer<Task> track) {
        Pos centre = around.add((Math.random() - 0.5) * 6, 0, (Math.random() - 0.5) * 6);
        List<Point> floor = new ArrayList<>();
        for (Vec offset : CROSS) {
            Point floorBlock = findFloor(instance, centre.add(offset));
            if (floorBlock != null) {
                floor.add(floorBlock);
            }
        }
        if (floor.isEmpty()) {
            return;
        }

        for (Point point : floor) {
            Key key = new Key(instance, point.blockX(), point.blockY(), point.blockZ());
            REPLACED.putIfAbsent(key, instance.getBlock(point));
            instance.setBlock(point, Block.ORANGE_TERRACOTTA);
        }
        instance.playSound(Sound.sound(net.kyori.adventure.key.Key.key("block.fire.ambient"), Sound.Source.HOSTILE, 1f, 1f), centre);

        track.accept(MinecraftServer.getSchedulerManager().buildTask(() -> {
            for (Point point : floor) {
                Point above = point.add(0, 1, 0);
                if (instance.getBlock(above).air()) {
                    instance.setBlock(above, Block.FIRE);
                }
            }
            int[] ticks = {0};
            Task[] burner = new Task[1];
            burner[0] = MinecraftServer.getSchedulerManager().buildTask(() -> {
                ticks[0] += 20;
                for (var viewer : instance.getPlayers()) {
                    if (viewer instanceof SkyBlockPlayer player && standsOn(player, floor)) {
                        burn.accept(player, ticks[0]);
                    }
                }
                if (ticks[0] >= BURN_TICKS) {
                    close(instance, floor);
                    burner[0].cancel();
                }
            }).repeat(TaskSchedule.tick(20)).schedule();
            track.accept(burner[0]);
        }).delay(TaskSchedule.tick(WARN_TICKS)).schedule());
    }

    /** Puts every replaced block of this instance back (boss gone, quest over). */
    public static void restoreAll(Instance instance) {
        List<Point> points = new ArrayList<>();
        REPLACED.keySet().stream().filter(key -> key.instance() == instance)
            .forEach(key -> points.add(new Vec(key.x(), key.y(), key.z())));
        close(instance, points);
    }

    private static void close(Instance instance, List<Point> floor) {
        for (Point point : floor) {
            Point above = point.add(0, 1, 0);
            if (instance.getBlock(above).compare(Block.FIRE)) {
                instance.setBlock(above, Block.AIR);
            }
            Block original = REPLACED.remove(new Key(instance, point.blockX(), point.blockY(), point.blockZ()));
            if (original != null) {
                instance.setBlock(point, original);
            }
        }
    }

    private static boolean standsOn(SkyBlockPlayer player, List<Point> floor) {
        Pos position = player.getPosition();
        for (Point point : floor) {
            if (position.blockX() == point.blockX() && position.blockZ() == point.blockZ()
                && position.y() >= point.blockY() && position.y() <= point.blockY() + 2.5) {
                return true;
            }
        }
        return false;
    }

    private static Point findFloor(Instance instance, Pos at) {
        if (!instance.isChunkLoaded(at)) {
            return null;
        }
        for (int dy = 1; dy >= -4; dy--) {
            Point candidate = new Vec(at.blockX(), at.blockY() + dy - 1, at.blockZ());
            Block block = instance.getBlock(candidate);
            if (block.isSolid() && !block.compare(Block.ORANGE_TERRACOTTA)
                && instance.getBlock(candidate.add(0, 1, 0)).air()) {
                return candidate;
            }
        }
        return null;
    }

    private record Key(Instance instance, int x, int y, int z) {
    }
}
