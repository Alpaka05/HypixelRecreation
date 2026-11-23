package net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.ability;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.instance.Instance;
import net.minestom.server.particle.Particle;
import net.swofty.commons.text.Text;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.InfernoDemonMob;
import net.swofty.type.skyblockgeneric.entity.mob.mobs.slayer.SlayerBossMob;
import net.swofty.type.skyblockgeneric.slayer.blaze.Attunement;
import net.swofty.type.skyblockgeneric.slayer.blaze.FirePillar;
import net.swofty.type.skyblockgeneric.slayer.blaze.FirePits;
import net.swofty.type.skyblockgeneric.slayer.blaze.HellionShield;
import net.swofty.type.skyblockgeneric.user.SkyBlockPlayer;

/**
 * The Inferno Demonlord (Blaze slayer), after Hypixel's mechanics as of late 2026:
 * <ul>
 *   <li><b>Immolate</b>: true damage every second to everyone close by, rising as the boss loses health.</li>
 *   <li><b>Hellion Shield</b> (II+): only a blaze dagger on the shield's attunement gets through;
 *       8 correct strikes move it on, Ashen → Spirit → Auric → Crystal.</li>
 *   <li><b>Demonsplit</b>: at 50 % (I/II) or at 2/3 and 1/3 (III/IV) the boss goes immune and
 *       Quazii and Typhoeus fight in its place, one of them immune at a time; when both are dead the
 *       boss comes back.</li>
 *   <li><b>Fire Pillars</b> (II from 50 %, III/IV from 2/3): every 20 s; 8 hits within 7 s or they explode.</li>
 *   <li><b>Fire pits</b>: around each split, and as DDR Apocalypse below 1/3 on III/IV.</li>
 * </ul>
 */
final class InfernoDemonlordAbility extends SlayerAbilitySupport {
    /** Immolate true damage per second at more than 2/3, 1/3 to 2/3, and less than 1/3 health. */
    private static final double[][] IMMOLATE = {{45, 65, 115}, {65, 130, 190}, {80, 160, 320}, {385, 575, 955}};
    private static final double[] DEMON_HEALTH = {500_000, 1_750_000, 5_000_000, 10_000_000};
    private static final double[] DEMON_DAMAGE = {2_500, 8_000, 12_000, 25_000};
    private static final double IMMOLATE_RANGE = 12;

    private HellionShield shield;
    private final List<Double> splitsAt = new ArrayList<>();
    private final List<InfernoDemonMob> demons = new ArrayList<>();
    private final List<FirePillar> pillars = new ArrayList<>();
    private boolean splitting;
    private boolean pillarsStarted;
    private boolean apocalypseStarted;

    @Override
    public void onSpawn(SlayerBossMob boss) {
        int tier = tier(boss);
        if (tier >= 2) {
            shield = HellionShield.boss();
        }
        if (tier >= 3) {
            splitsAt.add(2D / 3D);
            splitsAt.add(1D / 3D);
        } else {
            splitsAt.add(0.5D);
        }
        refreshShieldLine(boss);

        repeating(boss, 20, 20, () -> {
            if (!splitting) {
                double perSecond = IMMOLATE[tier - 1][stage(boss)];
                nearbyPlayers(boss, IMMOLATE_RANGE).forEach(player -> burn(player, perSecond));
                particles(boss, Particle.FLAME, 1.1f, 20);
            }
            checkPhases(boss, tier);
        });
    }

    @Override
    public float modifyIncomingDamage(SlayerBossMob boss, Damage damage, float amount) {
        if (splitting) {
            return 0F;
        }
        float result = shield == null ? amount : shield.apply(damage, amount, () -> refreshShieldLine(boss));
        refreshShieldLine(boss);
        // Never skip past a split: a single huge hit stops at the threshold.
        if (!splitsAt.isEmpty()) {
            double floor = splitsAt.getFirst() * maxHealth(boss);
            if (boss.getHealth() - result < floor) {
                result = (float) Math.max(0, boss.getHealth() - floor);
            }
        }
        return result;
    }

    @Override
    public void onDeath(SlayerBossMob boss) {
        cleanUp(boss);
    }

    private void checkPhases(SlayerBossMob boss, int tier) {
        double ratio = healthRatio(boss);
        if (!splitting && !splitsAt.isEmpty() && ratio <= splitsAt.getFirst() + 1e-6) {
            splitsAt.removeFirst();
            demonsplit(boss, tier);
        }
        double pillarStart = tier >= 3 ? 2D / 3D : 0.5D;
        if (tier >= 2 && !pillarsStarted && ratio <= pillarStart + 1e-6) {
            pillarsStarted = true;
            repeating(boss, 100, 400, () -> raisePillar(boss));
        }
        if (tier >= 3 && !apocalypseStarted && !splitting && ratio <= 1D / 3D + 1e-6) {
            apocalypseStarted = true;
            repeating(boss, 20, 60, () -> {
                SkyBlockPlayer owner = owner(boss);
                if (owner != null && !splitting) {
                    openPit(boss, owner.getPosition());
                }
            });
        }
    }

    private void demonsplit(SlayerBossMob boss, int tier) {
        splitting = true;
        boss.setFrozen(true);
        boss.setInvisible(true);
        refreshShieldLine(boss);
        SkyBlockPlayer owner = owner(boss);
        if (owner != null) {
            openPit(boss, owner.getPosition());
        }
        if (boss.getInstance() != null) {
            boss.getInstance().playSound(Sound.sound(Key.key("entity.blaze.death"), Sound.Source.HOSTILE, 2f, 0.5f), boss.getPosition());
        }

        InfernoDemonMob quazii = spawnDemon(boss, InfernoDemonMob.Demon.QUAZII, tier, boss.getPosition().add(2, 0, 0));
        InfernoDemonMob typhoeus = spawnDemon(boss, InfernoDemonMob.Demon.TYPHOEUS, tier, boss.getPosition().add(-2, 0, 0));
        if (tier >= 2) {
            quazii.setShield(new HellionShield(Attunement.SPIRIT, Attunement.CRYSTAL));
            typhoeus.setShield(new HellionShield(Attunement.ASHEN, Attunement.AURIC));
            // A switch hands the vulnerability to the other demon.
            quazii.onShieldSwitch(demon -> passImmunity(demon, typhoeus));
            typhoeus.onShieldSwitch(demon -> passImmunity(demon, quazii));
        }
        typhoeus.setImmune(true);

        // Typhoeus' Scorched Anger: half its damage as true damage to everyone close by, every second.
        repeating(boss, 20, 20, () -> {
            if (!typhoeus.isRemoved() && !typhoeus.isDead() && typhoeus.getInstance() != null) {
                double perSecond = DEMON_DAMAGE[tier - 1] / 2D;
                typhoeus.getInstance().getPlayers().stream()
                    .filter(player -> player instanceof SkyBlockPlayer)
                    .filter(player -> player.getPosition().distance(typhoeus.getPosition()) <= 6)
                    .forEach(player -> burn((SkyBlockPlayer) player, perSecond));
            }
        });

        for (InfernoDemonMob demon : List.of(quazii, typhoeus)) {
            demon.onDeath(dead -> {
                demons.remove(dead);
                InfernoDemonMob other = dead == quazii ? typhoeus : quazii;
                if (!other.isDead() && !other.isRemoved()) {
                    other.setImmune(false);
                } else {
                    reform(boss);
                }
            });
        }
    }

    private void passImmunity(InfernoDemonMob from, InfernoDemonMob to) {
        if (to.isDead() || to.isRemoved()) {
            return;
        }
        from.setImmune(true);
        to.setImmune(false);
    }

    private void reform(SlayerBossMob boss) {
        delayed(boss, 20, () -> {
            splitting = false;
            boss.setInvisible(false);
            boss.setFrozen(false);
            refreshShieldLine(boss);
            SkyBlockPlayer owner = owner(boss);
            if (owner != null) {
                openPit(boss, owner.getPosition());
            }
        });
    }

    private InfernoDemonMob spawnDemon(SlayerBossMob boss, InfernoDemonMob.Demon demon, int tier, Pos at) {
        InfernoDemonMob mob = InfernoDemonMob.create(boss.getOwnerUuid(), new InfernoDemonMob.Profile(
            demon, boss.getOwnerName(), boss.getLevel(), DEMON_HEALTH[tier - 1], DEMON_DAMAGE[tier - 1]));
        mob.setInstance(boss.getInstance(), at);
        demons.add(mob);
        return mob;
    }

    private void raisePillar(SlayerBossMob boss) {
        Instance instance = boss.getInstance();
        SkyBlockPlayer owner = owner(boss);
        if (instance == null || owner == null) {
            return;
        }
        FirePillar pillar = FirePillar.spawn(instance, owner.getPosition(), () ->
            nearbyPlayers(boss, 25).forEach(player -> burn(player, player.getMaxHealth() * 1000D)));
        if (pillar != null) {
            pillars.add(pillar);
        }
    }

    private void openPit(SlayerBossMob boss, Pos around) {
        Instance instance = boss.getInstance();
        if (instance == null) {
            return;
        }
        int tier = tier(boss);
        double perSecond = tier >= 3 ? (tier == 3 ? 950 : 2865) : 3 * IMMOLATE[tier - 1][2];
        FirePits.open(instance, around, (player, ticks) -> burn(player, perSecond), boss::trackAbilityTask);
    }

    private void refreshShieldLine(SlayerBossMob boss) {
        if (splitting) {
            boss.setShieldLine(Text.of("<c><l>IMMUNE"));
        } else if (shield != null) {
            boss.setShieldLine(shield.line());
        } else {
            boss.setShieldLine(Text.literal(""));
        }
    }

    private void cleanUp(SlayerBossMob boss) {
        new ArrayList<>(demons).forEach(InfernoDemonMob::despawnQuietly);
        demons.clear();
        pillars.forEach(FirePillar::remove);
        pillars.clear();
        if (boss.getInstance() != null) {
            FirePits.restoreAll(boss.getInstance());
        }
    }

    /** True damage that ignores defense; kills when it empties the health bar. */
    private void burn(SkyBlockPlayer player, double damage) {
        if (damage <= 0 || player.isRemoved() || player.isDead()) {
            return;
        }
        float left = (float) (player.getHealth() - damage);
        if (left <= 0) {
            player.kill();
            return;
        }
        player.setHealth(left);
    }

    private int stage(SlayerBossMob boss) {
        double ratio = healthRatio(boss);
        return ratio > 2D / 3D ? 0 : ratio > 1D / 3D ? 1 : 2;
    }

    private static int tier(SlayerBossMob boss) {
        return boss.getProfile().tier().tier().number();
    }

    private static double maxHealth(SlayerBossMob boss) {
        return boss.getProfile().tier().bossHealth();
    }
}
