package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class HUD extends Module {

    public static final HUD INSTANCE = new HUD();

    private HUD() {
        super("HUD", Category.RENDER);
    }

    @SubscribeEvent
    private void onRenderGui(RenderGuiEvent.Post event) {

    }

}