package com.github.lumin.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;

public final class WorldToScreen {

    private static final Minecraft mc = Minecraft.getInstance();

    public static Vector4d getEntityPositionsOn2D(LivingEntity target, float tickDelta) {
        final int[] viewport = new int[]{0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()};
        final PoseStack matrixStack = createMatrixStack(tickDelta);

        final Matrix4f projectionMatrix = matrixStack.last().pose();

        final Vec3 position = interpolate(target, tickDelta);

        final float width = target.getBbWidth() / 2f;
        final float height = target.getBbHeight() + (target.isCrouching() ? 0.1f : 0.2f);

        final AABB boundingBox = new AABB(
                position.x - width,
                position.y,
                position.z - width,
                position.x + width,
                position.y + height,
                position.z + width
        );

        final Vector4d projection = projectEntity(viewport, projectionMatrix, boundingBox);

        projection.div(mc.getWindow().getGuiScale());

        projection.z -= projection.x;
        projection.w -= projection.y;

        return projection;
    }

    public static Vector4d projectEntity(final int[] viewport, final Matrix4f matrix, final AABB boundingBox) {
        final Vector4f windowCoords = new Vector4f();

        final List<Vec3> list = getBoxBounds(boundingBox);
        Vector4d projected = null;

        for (final Vec3 pos : list) {
            matrix.project(pos.toVector3f(), viewport, windowCoords);
            windowCoords.y = viewport[3] - windowCoords.y;

            if (windowCoords.w != 1) {
                break;
            }

            if (projected == null) {
                projected = new Vector4d(windowCoords.x, windowCoords.y, 0, 0);
            } else {
                final double windowX = windowCoords.x;
                final double windowY = windowCoords.y;

                projected.x = Math.min(windowX, projected.x);
                projected.y = Math.min(windowY, projected.y);
                projected.z = Math.max(windowX, projected.z);
                projected.w = Math.max(windowY, projected.w);
            }
        }

        return projected;
    }

    private static PoseStack createMatrixStack(final float tickDelta) {
        PoseStack poseStack = new PoseStack();
        final Camera camera = mc.gameRenderer.getMainCamera();
        float fov = mc.gameRenderer.getFov(camera, tickDelta, true);

        poseStack.mulPose(mc.gameRenderer.getProjectionMatrix(fov));

        mc.gameRenderer.bobHurt(poseStack, camera.getPartialTickTime());

        if (mc.options.bobView().get()) {
            mc.gameRenderer.bobView(poseStack, camera.getPartialTickTime());
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0f));
        return poseStack;
    }

    public static List<Vec3> getBoxBounds(final AABB boundingBox) {
        return Arrays.asList(
                new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ),
                new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ),
                new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ),
                new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ),
                new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ),
                new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ),
                new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
        );
    }

    private static Vec3 interpolate(final LivingEntity entity, final float tickDelta) {
        final Camera camera = mc.gameRenderer.getMainCamera();
        return entity.position().add(Mth.lerp(tickDelta, entity.xOld, entity.getX()) - entity.getX(), Mth.lerp(tickDelta, entity.yOld, entity.getY()) - entity.getY(), Mth.lerp(tickDelta, entity.zOld, entity.getZ()) - entity.getZ()).subtract(camera.position());
    }

}
