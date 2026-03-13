package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.utils.render.Render3DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;

public class ESP extends Module {

    public static final ESP INSTANCE = new ESP();

    private ESP() {
        super("ESP", Category.RENDER);
    }

    private final BoolSetting targetChests = boolSetting("箱子", true);

    @SubscribeEvent
    public void onRender3D(RenderLevelStageEvent.AfterEntities event) {
        if (!targetChests.getValue()) return;
        if (nullCheck()) return;

        PoseStack poseStack = event.getPoseStack();
        Level level = mc.level;
        int renderDistance = mc.options.renderDistance().get();
        ChunkPos playerChunk = new ChunkPos(mc.player.blockPosition());

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                int chunkX = playerChunk.x + x;
                int chunkZ = playerChunk.z + z;
                if (level.hasChunk(chunkX, chunkZ)) {
                    for (BlockEntity blockEntity : level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                        if (blockEntity instanceof RandomizableContainerBlockEntity) {
                            int color = getColor(blockEntity);
                            Color sideColor = new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 60);
                            Color lineColor = new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);
                            Render3DUtils.drawFullBox(poseStack, blockEntity.getBlockPos(), sideColor, lineColor, 2f);
                        }
                    }
                }
            }
        }
    }

    public int getColor(BlockEntity blockEntity) {
        if (blockEntity instanceof RandomizableContainerBlockEntity) {
            return Color.GREEN.getRGB();
        }
        return 0xFFFFFFFF;
    }
}