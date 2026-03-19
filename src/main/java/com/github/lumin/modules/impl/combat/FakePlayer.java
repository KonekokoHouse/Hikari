package com.github.lumin.modules.impl.combat;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.StringSetting;
import com.github.lumin.utils.timer.TimerUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer extends Module {

    public static final FakePlayer INSTANCE = new FakePlayer();

    private static final UUID FAKE_UUID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final StringSetting name = stringSetting("Name", "FakePlayer");
    private final BoolSetting damage = boolSetting("Damage", true);
    private final BoolSetting autoTotem = boolSetting("AutoTotem", true);
    private final BoolSetting totemEffect = boolSetting("TotemEffect", true);
    public final BoolSetting record = boolSetting("Record", false);
    public final BoolSetting play = boolSetting("Play", false);

    private final List<PlayerState> positions = new ArrayList<>();
    private final TimerUtils attackTimer = new TimerUtils();

    private RemotePlayer fakePlayer;
    private Vec3 spawnPos;
    private int movementTick;
    private boolean lastRecordValue;

    private FakePlayer() {
        super("FakePlayer", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        if (nullCheck()) return;
        spawnFake();
    }

    @Override
    protected void onDisable() {
        removeFake();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck() || fakePlayer == null) return;

        if (autoTotem.getValue()) {
            ensureTotem(fakePlayer);
        }

        if (record.getValue() != lastRecordValue && record.getValue()) {
            positions.clear();
            movementTick = 0;
        }
        lastRecordValue = record.getValue();

        if (record.getValue()) {
            positions.add(new PlayerState(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot()));
        }

        if (play.getValue() && !positions.isEmpty()) {
            movementTick++;
            if (movementTick >= positions.size()) {
                movementTick = 0;
            }
            PlayerState state = positions.get(movementTick);
            fakePlayer.setYRot(state.yaw);
            fakePlayer.setXRot(state.pitch);
            fakePlayer.setYHeadRot(state.yaw);
            fakePlayer.setPos(state.x, state.y, state.z);
        } else if (spawnPos != null) {
            fakePlayer.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        }

        fakePlayer.setDeltaMovement(Vec3.ZERO);
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (!damage.getValue() || fakePlayer == null || nullCheck()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundInteractPacket)) return;

        if (!isAttackPacket(packet)) return;
        Integer entityId = getEntityId(packet);
        if (entityId == null) return;

        Entity target = mc.level.getEntity(entityId);
        if (target != fakePlayer) return;

        if (!attackTimer.passedMillise(150)) return;

        float damageValue = (float) mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (isCritical()) {
            damageValue *= 1.5f;
            mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        applyDamage(damageValue);
        attackTimer.reset();
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!damage.getValue() || fakePlayer == null || nullCheck()) return;

        Packet<?> packet = event.getPacket();
        if (packet == null) return;
        if (!"ClientboundExplodePacket".equals(packet.getClass().getSimpleName())) return;

        Vec3 explosionPos = readExplosionPos(packet);
        if (explosionPos == null) return;

        if (explosionPos.distanceTo(fakePlayer.position()) > 10.0) return;

        float damageValue = (float) calculateDamage(explosionPos, fakePlayer);
        applyDamage(damageValue);
    }

    private void applyDamage(float amount) {
        if (amount <= 0.0f || fakePlayer == null) return;

        mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_HURT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        float absorption = fakePlayer.getAbsorptionAmount();
        if (absorption >= amount) {
            fakePlayer.setAbsorptionAmount(absorption - amount);
            return;
        }

        float remaining = amount - absorption;
        fakePlayer.setAbsorptionAmount(0.0f);
        fakePlayer.setHealth(fakePlayer.getHealth() - remaining);

        if (fakePlayer.getHealth() <= 0.0f) {
            boolean hasTotem = fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                    || fakePlayer.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
            if (autoTotem.getValue() || hasTotem) {
                fakePlayer.setHealth(10.0f);
                if (totemEffect.getValue()) {
                    fakePlayer.handleEntityEvent((byte) 35);
                    fakePlayer.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);
                }
            }
        }
    }

    private boolean isCritical() {
        if (mc.player == null) return false;
        if (mc.player.isInWater() || mc.player.isInLava()) return false;
        if (mc.player.onClimbable()) return false;
        if (mc.player.isPassenger()) return false;
        if (mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) return false;
        return mc.player.fallDistance > 0.0f && !mc.player.onGround();
    }

    private void ensureTotem(RemotePlayer player) {
        if (!player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }
        if (!player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }
    }

    private void spawnFake() {
        if (fakePlayer != null) return;
        if (!(mc.level instanceof ClientLevel level)) return;

        spawnPos = mc.player.position();

        fakePlayer = new RemotePlayer(level, new GameProfile(FAKE_UUID, name.getValue()));
        fakePlayer.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        fakePlayer.setYRot(mc.player.getYRot());
        fakePlayer.setXRot(mc.player.getXRot());
        fakePlayer.setYHeadRot(mc.player.getYRot());
        fakePlayer.setDeltaMovement(Vec3.ZERO);
        fakePlayer.setHealth(fakePlayer.getMaxHealth());
        fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());

        if (autoTotem.getValue()) {
            ensureTotem(fakePlayer);
        }

        level.addEntity(fakePlayer);
    }

    private void removeFake() {
        if (fakePlayer != null) {
            fakePlayer.discard();
            fakePlayer = null;
        }
        spawnPos = null;
        positions.clear();
        movementTick = 0;
    }

    private boolean isAttackPacket(Packet<?> packet) {
        try {
            Method method = packet.getClass().getMethod("getActionType");
            Object type = method.invoke(packet);
            return type != null && type.toString().equals("ATTACK");
        } catch (Exception ignored) {
        }
        try {
            Field field = packet.getClass().getDeclaredField("actionType");
            field.setAccessible(true);
            Object type = field.get(packet);
            return type != null && type.toString().equals("ATTACK");
        } catch (Exception ignored) {
        }
        return false;
    }

    private Integer getEntityId(Packet<?> packet) {
        try {
            Method method = packet.getClass().getMethod("getEntityId");
            Object id = method.invoke(packet);
            return id instanceof Integer ? (Integer) id : null;
        } catch (Exception ignored) {
        }
        try {
            Field field = packet.getClass().getDeclaredField("entityId");
            field.setAccessible(true);
            return (Integer) field.get(packet);
        } catch (Exception ignored) {
        }
        return null;
    }

    private Vec3 readExplosionPos(Packet<?> packet) {
        try {
            Method getX = packet.getClass().getMethod("getX");
            Method getY = packet.getClass().getMethod("getY");
            Method getZ = packet.getClass().getMethod("getZ");
            double x = ((Number) getX.invoke(packet)).doubleValue();
            double y = ((Number) getY.invoke(packet)).doubleValue();
            double z = ((Number) getZ.invoke(packet)).doubleValue();
            return new Vec3(x, y, z);
        } catch (Exception ignored) {
        }
        return null;
    }

    private double calculateDamage(Vec3 pos, LivingEntity target) {
        if (mc.level == null || target == null) return 0.0;

        double distance = target.position().distanceTo(pos);
        if (distance > 12.0) return 0.0;

        double exposure = getExposure(pos, target.getBoundingBox());
        double impact = (1.0 - (distance / 12.0)) * exposure;
        if (impact <= 0.0) return 0.0;

        float damage = (float) ((impact * impact + impact) * 7.0 * 12.0 + 1.0);

        float armor = target.getArmorValue();
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float armorReduction = net.minecraft.util.Mth.clamp(armor - damage / (2.0f + toughness / 4.0f), armor * 0.2f, 20.0f);
        damage *= 1.0f - armorReduction / 25.0f;

        return Math.max(damage, 0.0f);
    }

    private double getExposure(Vec3 pos, AABB box) {
        double xStep = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double yStep = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zStep = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

        int count = 0;
        int total = 0;
        for (double ix = 0.0; ix <= 1.0; ix += xStep) {
            for (double iy = 0.0; iy <= 1.0; iy += yStep) {
                for (double iz = 0.0; iz <= 1.0; iz += zStep) {
                    double dx = net.minecraft.util.Mth.lerp(ix, box.minX, box.maxX);
                    double dy = net.minecraft.util.Mth.lerp(iy, box.minY, box.maxY);
                    double dz = net.minecraft.util.Mth.lerp(iz, box.minZ, box.maxZ);
                    Vec3 sample = new Vec3(dx, dy, dz);
                    HitResult hit = mc.level.clip(new ClipContext(sample, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                    if (hit.getType() == HitResult.Type.MISS) count++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) count / (double) total;
    }

    private record PlayerState(double x, double y, double z, float yaw, float pitch) {
    }
}
