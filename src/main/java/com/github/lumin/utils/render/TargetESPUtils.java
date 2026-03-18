package com.github.lumin.utils.render;

import com.github.lumin.assets.resources.ResourceLocationUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.function.Function;

public class TargetESPUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final Identifier CAPTUREMARK = ResourceLocationUtils.getIdentifier("textures/particles/target.png");

    private static final RenderPipeline TARGET_ICON_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation("pipeline/sakura_target_icon")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    private static final Function<Identifier, RenderType> TARGET_ICON_LAYER = Util.memoize(texture -> RenderType.create(
            "sakura_target_icon",
            RenderSetup.builder(TARGET_ICON_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .sortOnUpload()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.MAIN_TARGET)
                    .createRenderSetup()
    ));

    private static float rotation = 0f;

    public static void drawCaptureMark(PoseStack poseStack, LivingEntity target, double espSize, double rotSpeed, double waveSpeed, Color color1, Color color2) {
        rotation -= (float) rotSpeed;
        if (rotation <= -360f) rotation += 360f;

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double ex = Mth.lerp(partialTick, target.xOld, target.getX()) - cam.x;
        double ey = Mth.lerp(partialTick, target.yOld, target.getY()) - cam.y;
        double ez = Mth.lerp(partialTick, target.zOld, target.getZ()) - cam.z;

        float entityHeight = target.getBbHeight();
        float size = (float) espSize * 0.5f;

        poseStack.pushPose();
        poseStack.translate(ex, ey + entityHeight * 0.5, ez);

        Camera camera = mc.gameRenderer.getMainCamera();
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Color c1 = getColorForProgress(0, waveSpeed, color1, color2);
        Color c2 = getColorForProgress(0.25f, waveSpeed, color1, color2);
        Color c3 = getColorForProgress(0.5f, waveSpeed, color1, color2);
        Color c4 = getColorForProgress(0.75f, waveSpeed, color1, color2);

        buffer.addVertex(matrix, -size, -size, 0).setUv(0, 0).setColor(c1.getRGB());
        buffer.addVertex(matrix, -size, size, 0).setUv(0, 1).setColor(c2.getRGB());
        buffer.addVertex(matrix, size, size, 0).setUv(1, 1).setColor(c3.getRGB());
        buffer.addVertex(matrix, size, -size, 0).setUv(1, 0).setColor(c4.getRGB());

        TARGET_ICON_LAYER.apply(CAPTUREMARK).draw(buffer.buildOrThrow());

        poseStack.popPose();
    }

    private static Color getColorForProgress(float progress, double waveSpeed, Color color1, Color color2) {
        float wave = (float) Math.sin((progress * Math.PI * 2) + (System.currentTimeMillis() / 1000f * (float) waveSpeed));
        wave = (wave + 1f) / 2f;
        return ColorUtils.interpolateColor(color1, color2, wave);
    }

}
