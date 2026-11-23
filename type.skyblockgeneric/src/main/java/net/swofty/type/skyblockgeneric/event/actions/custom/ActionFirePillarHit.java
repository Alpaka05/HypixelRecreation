package net.swofty.type.skyblockgeneric.event.actions.custom;

import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import net.swofty.type.generic.event.EventNodes;
import net.swofty.type.generic.event.HypixelEventClass;
import net.swofty.type.generic.event.phase.EventPhase;
import net.swofty.type.generic.event.phase.PhasedEvent;
import net.swofty.type.skyblockgeneric.event.custom.PlayerDamageSkyBlockBlockEvent;
import net.swofty.type.skyblockgeneric.slayer.blaze.FirePillar;

/** Left clicks on an Inferno Demonlord's Fire Pillar count as hits on it. */
public class ActionFirePillarHit implements HypixelEventClass {
    @PhasedEvent(node = EventNodes.CUSTOM, requireDataLoaded = true, phase = EventPhase.GAMEPLAY)
    public void run(PlayerDamageSkyBlockBlockEvent event) {
        if (event.getStatus() != ClientPlayerActionPacket.Status.STARTED_DIGGING) {
            return;
        }
        FirePillar.hit(event.getPlayer().getInstance(), event.getBlockPosition());
    }
}
