package com.github.lumin.modules.impl.combat;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.utils.rotation.RotationUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AimAssist extends Module {
    public static final AimAssist INSTANCE = new AimAssist();

    public EnumSetting<SmoothMode> mode = enumSetting("平滑模式", SmoothMode.Fast);
    public DoubleSetting range = doubleSetting("目标距离", 4.2, 1.0, 8.0, 0.1);
    public DoubleSetting speed = doubleSetting("旋转速度", 10.0, 1.0, 180.0, 1.0);
    public DoubleSetting strength = doubleSetting("插值强度", 0.1, 0.01, 1.0, 0.01);
    public BoolSetting ignoreScreen = boolSetting("屏幕打开时忽略", true);
    public BoolSetting ignoreInventory = boolSetting("物品栏打开时忽略", true);
    public BoolSetting player = boolSetting("瞄准玩家", true);
    public BoolSetting mob = boolSetting("瞄准敌对生物", true);
    public BoolSetting animal = boolSetting("瞄准动物", true);
    public BoolSetting invisible = boolSetting("瞄准隐身实体", false);

    private Vector2f targetRotation;
    private Vector2f playerRotation;
    private LivingEntity currentTarget;

    private AimAssist() {
        super("AimAssist", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        targetRotation = null;
        playerRotation = null;
        currentTarget = null;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) {
            targetRotation = null;
            return;
        }

        playerRotation = new Vector2f(mc.player.getYRot(), mc.player.getXRot());

        if (ignoreScreen.getValue() && mc.screen != null) {
            targetRotation = null;
            return;
        }
        if (ignoreInventory.getValue() && (mc.screen instanceof AbstractContainerScreen)) {
            targetRotation = null;
            return;
        }

        currentTarget = findTarget();

        if (currentTarget != null) {
            Vector2f idealRotation = RotationUtils.calculate(currentTarget, true, range.getValue());
            AngleSmooth smoother = getAngleSmooth();
            targetRotation = smoother.process(playerRotation, idealRotation);
        } else {
            targetRotation = null;
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Pre event) {
        if (nullCheck()) return;

        // Check ignore conditions again for render safety
        if (ignoreScreen.getValue() && mc.screen != null) return;
        if (ignoreInventory.getValue() && (mc.screen instanceof AbstractContainerScreen)) return;

        if (targetRotation != null && playerRotation != null) {
            float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            float timerSpeed = 1.0f; // Assuming normal speed

            // Interpolate rotation: current + (target - current) * factor
            // This logic mimics the kotlin reference:
            // currentRotation.yaw + (rotation.yaw - currentRotation.yaw) * (timerSpeed * partialTicks)

            float yawDiff = targetRotation.x - playerRotation.x;
            float pitchDiff = targetRotation.y - playerRotation.y;

            // Wrap degrees for yaw to ensure shortest path
            yawDiff = Mth.wrapDegrees(yawDiff);

            float interpolatedYaw = playerRotation.x + yawDiff * (timerSpeed * partialTicks);
            float interpolatedPitch = playerRotation.y + pitchDiff * (timerSpeed * partialTicks);

            mc.player.setYRot(interpolatedYaw);
            mc.player.setXRot(interpolatedPitch);
        }
    }

    private LivingEntity findTarget() {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isDeadOrDying()) continue;

            double dist = RotationUtils.getEyeDistanceToEntity(living);
            if (dist > range.getValue()) continue;

            if (!isValidTarget(living)) continue;

            targets.add(living);
        }

        targets.sort(Comparator.comparingDouble(RotationUtils::getEyeDistanceToEntity));

        if (targets.isEmpty()) return null;
        return targets.getFirst();
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Player) {
            if (!player.getValue()) return false;
            return !entity.isInvisible() || invisible.getValue();
        } else if (entity instanceof Monster) {
            return mob.getValue();
        } else if (entity instanceof Animal) {
            return animal.getValue();
        }
        return false;
    }

    private AngleSmooth getAngleSmooth() {
        return switch (mode.getValue()) {
            case SmoothMode.Fast -> new LinearAngleSmooth(speed.getValue().floatValue());
            case SmoothMode.Stable -> new InterpolationAngleSmooth(strength.getValue().floatValue());
        };
    }

    private interface AngleSmooth {
        Vector2f process(Vector2f current, Vector2f target);
    }

    private record LinearAngleSmooth(float speed) implements AngleSmooth {
        @Override
        public Vector2f process(Vector2f current, Vector2f target) {
            float yawDiff = Mth.wrapDegrees(target.x - current.x);
            float pitchDiff = target.y - current.y;

            float yawChange = Mth.clamp(yawDiff, -speed, speed);
            float pitchChange = Mth.clamp(pitchDiff, -speed, speed);

            return new Vector2f(current.x + yawChange, current.y + pitchChange);
        }
    }

    private record InterpolationAngleSmooth(float strength) implements AngleSmooth {
        @Override
        public Vector2f process(Vector2f current, Vector2f target) {
            float yawDiff = Mth.wrapDegrees(target.x - current.x);
            float pitchDiff = target.y - current.y;

            float yawChange = yawDiff * strength;
            float pitchChange = pitchDiff * strength;

            return new Vector2f(current.x + yawChange, current.y + pitchChange);
        }
    }

    public enum SmoothMode {
        Fast,
        Stable,
    }
}