package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.utils.player.ChatUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class AutoAccount extends Module {

    public static final AutoAccount INSTANCE = new AutoAccount();

    private AutoAccount() {
        super("AutoAccount", Category.PLAYER);
    }

    @SubscribeEvent
    private void GameJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() != mc.player) return;

        ChatUtils.addChatMessage("joined " + event.getEntity().getName().getString());

    }

}
