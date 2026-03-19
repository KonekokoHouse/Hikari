package com.github.lumin.modules.impl.render;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.neoforged.bus.api.SubscribeEvent;

public class NoRender extends Module {

    public static final NoRender INSTANCE = new NoRender();

    public final BoolSetting potionEffects = boolSetting("PotionEffects", true);
    public final BoolSetting playerNameTags = boolSetting("PlayerNameTags", true);
    public final BoolSetting explosions = boolSetting("Explosions", false);
    public final BoolSetting totems = boolSetting("Totems", false);

    private NoRender() {
        super("NoRender", Category.RENDER);
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!isEnabled()) return;
        if (explosions.getValue() && event.getPacket() instanceof ClientboundExplodePacket) {
            event.setCanceled(true);
            return;
        }
        if (totems.getValue() && event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet instanceof com.github.lumin.ducks.EntityEventPacketAccess access && access.lumin$getEventId() == 35) {
                event.setCanceled(true);
            }
        }
    }
}
