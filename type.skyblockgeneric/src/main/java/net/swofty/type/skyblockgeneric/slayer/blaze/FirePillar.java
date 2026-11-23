package net.swofty.type.skyblockgeneric.slayer.blaze;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.entity.NameTagStand;
import org.jetbrains.annotations.Nullable;

/**
 * A Fire Pillar: four orange blocks that have to be hit 8 times within 7 seconds, or it explodes
 * for everybody fighting the boss.
 *
 * <p>Its name tag reads {@code §6§l7s §c§l8 hits} and counts down every second; SkyHanni's
 * pattern is {@code §6§l(?<seconds>.*)s §c§l8 hits} (the 8 is fixed there, so the tag always says
 * 8) and Skyblocker's {@code (\d+)s \d+ hits}. Every second hit takes a block off the top. Hits
 * are left clicks on the pillar's blocks.
 */
public final class FirePillar {
    private static final int HEIGHT = 4;
    private static final int HITS = 8;
    private static final int SECONDS = 7;
    private static final Map<Long, FirePillar> BY_BLOCK = new ConcurrentHashMap<>();

    private final Instance instance;
    private final List<Point> blocks = new ArrayList<>();
    private final List<Block> originals = new ArrayList<>();
    private final NameTagStand tag;
    private final Runnable onExplode;
    private final Task countdown;
    private int hitsLeft = HITS;
    private int secondsLeft = SECONDS;
    private boolean done;

    private FirePillar(Instance instance, Point base, Runnable onExplode) {
        this.instance = instance;
        this.onExplode = onExplode;
        for (int i = 0; i < HEIGHT; i++) {
            Point point = base.add(0, i, 0);
            blocks.add(point);
            originals.add(instance.getBlock(point));
            instance.setBlock(point, Block.ORANGE_CONCRETE);
            BY_BLOCK.put(key(instance, point), this);
        }
        tag = new NameTagStand(label());
        tag.setInstance(instance, new Pos(base.blockX() + 0.5, base.blockY() + HEIGHT + 0.3, base.blockZ() + 0.5));
        instance.playSound(Sound.sound(Key.key("entity.blaze.shoot"), Sound.Source.HOSTILE, 1.5f, 0.6f), base);
        countdown = MinecraftServer.getSchedulerManager().buildTask(this::second)
            .delay(TaskSchedule.seconds(1)).repeat(TaskSchedule.seconds(1)).schedule();
    }

    /** Raises a pillar on the floor around {@code near}; null when there is no floor there. */
    public static @Nullable FirePillar spawn(Instance instance, Pos near, Runnable onExplode) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 3 + Math.random() * 4;
            Pos at = near.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            for (int dy = 2; dy >= -4; dy--) {
                Point base = new Vec(at.blockX(), at.blockY() + dy, at.blockZ());
                if (instance.isChunkLoaded(base) && instance.getBlock(base.sub(0, 1, 0)).isSolid()
                    && clear(instance, base)) {
                    return new FirePillar(instance, base, onExplode);
                }
            }
        }
        return null;
    }

    /** A left click on {@code block}; true if it was a pillar block. */
    public static boolean hit(Instance instance, Point block) {
        FirePillar pillar = BY_BLOCK.get(key(instance, block));
        if (pillar == null) {
            return false;
        }
        pillar.hit();
        return true;
    }

    public void remove() {
        if (done) {
            return;
        }
        done = true;
        countdown.cancel();
        tag.remove();
        for (int i = 0; i < blocks.size(); i++) {
            BY_BLOCK.remove(key(instance, blocks.get(i)));
            instance.setBlock(blocks.get(i), originals.get(i));
        }
    }

    private void hit() {
        if (done) {
            return;
        }
        hitsLeft--;
        instance.playSound(Sound.sound(Key.key("block.stone.hit"), Sound.Source.BLOCK, 1f, 1.2f), blocks.getFirst());
        if (hitsLeft % 2 == 0) {
            int top = HEIGHT - 1 - (HITS - hitsLeft) / 2 + 1;
            if (top >= 0 && top < blocks.size()) {
                instance.setBlock(blocks.get(top), originals.get(top));
            }
        }
        if (hitsLeft <= 0) {
            instance.playSound(Sound.sound(Key.key("block.fire.extinguish"), Sound.Source.BLOCK, 1f, 1f), blocks.getFirst());
            remove();
            return;
        }
        tag.setText(label());
    }

    private void second() {
        if (done) {
            return;
        }
        secondsLeft--;
        if (secondsLeft <= 0) {
            instance.playSound(Sound.sound(Key.key("entity.generic.explode"), Sound.Source.HOSTILE, 2f, 1f), blocks.getFirst());
            remove();
            onExplode.run();
            return;
        }
        tag.setText(label());
    }

    private Text label() {
        return Text.of("<6><l>{}s <c><l>{} hits", secondsLeft, HITS);
    }

    private static boolean clear(Instance instance, Point base) {
        for (int i = 0; i < HEIGHT; i++) {
            if (!instance.getBlock(base.add(0, i, 0)).air()) {
                return false;
            }
        }
        return true;
    }

    private static long key(Instance instance, Point point) {
        long hash = ((long) point.blockX() & 0x3FFFFFF) << 38 | ((long) point.blockZ() & 0x3FFFFFF) << 12 | (point.blockY() & 0xFFF);
        return hash ^ ((long) System.identityHashCode(instance) << 20);
    }
}
