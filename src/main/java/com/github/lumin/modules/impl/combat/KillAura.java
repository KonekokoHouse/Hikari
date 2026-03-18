package com.github.lumin.modules.impl.combat;

import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.*;
import com.github.lumin.utils.math.MathUtils;
import com.github.lumin.utils.render.TargetESPUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RotationUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {

    public static final KillAura INSTANCE = new KillAura();

    private KillAura() {
        super("KillAura", Category.COMBAT);
    }

    public enum TargetMode {
        Single,
        Switch,
        Multiple,
    }

    private enum ESPMode {
        CaptureMark,
        Firefly
    }

    private final EnumSetting<TargetMode> targetMode = enumSetting("TargetMode", TargetMode.Single);
    private final DoubleSetting range = doubleSetting("Range", 3.0, 1.0, 6.0, 0.01);
    private final DoubleSetting aimRange = doubleSetting("AimRange", 4.0, 1.0, 6.0, 0.1);
    private final IntSetting rotationSpeed = intSetting("roationspeed", 10, 1, 10, 1);
    private final DoubleSetting fov = doubleSetting("FOV", 360.0, 10.0, 360.0, 1.0);
    private final BoolSetting cooldownATK = boolSetting("CooldownATK", false);
    private final DoubleSetting cps = doubleSetting("CPS", 10.0, 1.0, 20.0, 1.0);
    private final DoubleSetting maxCps = doubleSetting("MaxCPS", 12, 1, 20, 1);
    private final BoolSetting player = boolSetting("Player", true);
    private final BoolSetting mob = boolSetting("Mob", true);
    private final BoolSetting animal = boolSetting("Animal", true);
    private final BoolSetting Invisible = boolSetting("Invisible", true);
    private final BoolSetting esp = boolSetting("ESP", false);
    private final EnumSetting<ESPMode> espMode = enumSetting("ESPMode", ESPMode.Firefly, esp::getValue);
    private final ColorSetting espColor1 = colorSetting("ESPMain", new Color(255, 183, 197), esp::getValue);
    private final ColorSetting espColor2 = colorSetting("ESPSecond", new Color(255, 133, 161), esp::getValue);
    private final DoubleSetting espSize = doubleSetting("ESPSize", 1.2, 0.5, 3.0, 0.1, esp::getValue);
    private final DoubleSetting espRotSpeed = doubleSetting("RotSpeed", 2.0, 0.5, 10.0, 0.1, esp::getValue);
    private final DoubleSetting waveSpeed = doubleSetting("WaveSpeed", 3.0, 0.5, 10.0, 0.1, esp::getValue);

    private LivingEntity target;
    private final List<LivingEntity> targets = new ArrayList<>();

    private int switchIndex = 0;
    private float attacks = 0;

    @Override
    protected void onDisable() {
        target = null;
        targets.clear();
        switchIndex = 0;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre e) {
        if (nullCheck()) return;

        targets.clear();
        updateTargets();

        if (targets.isEmpty()) {
            target = null;
            return;
        }

        if (targetMode.is("Single")) {
            target = targets.getFirst();
        } else if (targetMode.is("Switch")) {
            if (switchIndex >= targets.size()) switchIndex = 0;
            target = targets.get(switchIndex);
        } else if (targetMode.is("Multi")) {
            target = targets.getFirst();
        }

        attacks += MathUtils.getRandom(cps.getValue().floatValue(), maxCps.getValue().floatValue()) / 20f;

        if (target != null) {
            float[] rotations = RotationUtils.getRotationsToEntity(target);
            RotationManager.INSTANCE.setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed.getValue().floatValue(), MovementFix.ON, Priority.Medium);
        }
    }

    @SubscribeEvent
    public void onClick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (target == null) return;
        if (mc.player.isUsingItem() || mc.player.isBlocking()) return;
        if (mc.player.getAttackStrengthScale(0.5f) < 1.0f && cooldownATK.getValue()) return;

        while (attacks >= 1) {
            if (targetMode.is("Multi")) {
                for (LivingEntity t : targets) {
                    if (RotationUtils.getEyeDistanceToEntity(t) <= range.getValue() && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                        doAttack();
                    }
                }
                switchIndex++;
            } else {
                if (RotationUtils.getEyeDistanceToEntity(target) <= range.getValue() && mc.hitResult.getType() == HitResult.Type.ENTITY && mc.crosshairPickEntity.is(target)) {
                    doAttack();
                    if (targetMode.is("Switch")) switchIndex++;
                } else if (targetMode.is("Switch")) {
                    switchIndex++;
                }
            }
            attacks -= 1;
        }
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck() || !esp.getValue() || target == null) return;

        switch (espMode.getValue()) {
            case CaptureMark ->
                    TargetESPUtils.drawCaptureMark(event.getPoseStack(), target, espSize.getValue(), espRotSpeed.getValue(), waveSpeed.getValue(), espColor1.getValue(), espColor2.getValue());
        }

    }

    private void doAttack() {
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void updateTargets() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.isDeadOrDying()) continue;
            if (AntiBot.INSTANCE.isBot(entity)) continue;

            double dist = RotationUtils.getEyeDistanceToEntity(living);
            if (dist > aimRange.getValue()) continue;

            if (!isValidTarget(living)) continue;
            if (!RotationUtils.isInFov(living, fov.getValue().floatValue())) continue;
            targets.sort(Comparator.comparingDouble(o -> (double) o.distanceTo(mc.player)));
            targets.add(living);
        }
        targets.sort(Comparator.comparingDouble(RotationUtils::getEyeDistanceToEntity));
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Player) {
            if (!player.getValue()) return false;
            return !entity.isInvisible() || Invisible.getValue();
        } else if (entity instanceof Animal || entity instanceof Villager) {
            return animal.getValue();
        } else if (entity instanceof Monster) {
            return mob.getValue();
        } else {
            return false;
        }
    }

}