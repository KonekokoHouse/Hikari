package com.github.lumin.modules.impl.combat;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.player.Speedmine;
import com.github.lumin.settings.impl.*;
import com.github.lumin.utils.player.FindItemResult;
import com.github.lumin.utils.player.InvUtils;
import com.github.lumin.utils.render.Render3DUtils;
import com.github.lumin.utils.rotation.MovementFix;
import com.github.lumin.utils.rotation.Priority;
import com.github.lumin.utils.rotation.RotationUtils;
import com.github.lumin.utils.timer.TimerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AutoCrystal extends Module {

    public static final AutoCrystal INSTANCE = new AutoCrystal();

    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final Speedmine SPEEDMINE = Speedmine.INSTANCE;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private enum SequentialMode {
        Off,
        Once,
        Always
    }

    private enum InstantMode {
        Normal,
        Always,
        Off
    }

    private enum AutoSwapMode {
        BlockTarget,
        Normal,
        Silent
    }

    private enum SilentMode {
        Off,
        Spoof,
        Inventory
    }

    private enum MultipointMode {
        Single,
        Air,
        Corners,
        Full,
        Fast
    }

    private enum WeaknessMode {
        Off,
        Normal,
        Swap
    }

    private enum PlaceMode {
        Visible,
        Wall,
        Base
    }

    private final BoolSetting place = boolSetting("Place", true);
    private final BoolSetting breakCrystals = boolSetting("Break", true);
    private final IntSetting delay = intSetting("Delay", 2, 0, 20, 1);
    private final IntSetting factor = intSetting("Factor", 1, 1, 10, 1);
    private final DoubleSetting range = doubleSetting("Range", 4.5, 1.0, 6.0, 0.1);
    private final DoubleSetting wallRange = doubleSetting("WallRange", 3.5, 1.0, 6.0, 0.1);
    private final DoubleSetting minDamage = doubleSetting("MinDamage", 6.0, 0.0, 20.0, 0.1);
    private final DoubleSetting maxSelfDamage = doubleSetting("MaxSelf", 10.0, 0.0, 20.0, 0.1);
    private final DoubleSetting damageRatio = doubleSetting("DamageRatio", 1.5, 0.1, 5.0, 0.1);
    private final EnumSetting<SequentialMode> sequential = enumSetting("Sequential", SequentialMode.Off);
    private final BoolSetting oneDotTwelve = boolSetting("1.12", false);
    private final BoolSetting strictDirection = boolSetting("StrictDir", false);
    private final BoolSetting mineIgnore = boolSetting("MineIgnore", false);
    private final BoolSetting limit = boolSetting("Limit", true);
    private final BoolSetting lowerHitBox = boolSetting("LowerHitBox", false);
    private final BoolSetting smartTrace = boolSetting("SmartTrace", true);
    private final BoolSetting basePlace = boolSetting("BasePlace", true);
    private final BoolSetting airPlace = boolSetting("Air", false);
    private final BoolSetting balance = boolSetting("Balance", true);
    private final BoolSetting antiSuicide = boolSetting("AntiSuicide", true);
    private final BoolSetting inhibit = boolSetting("Inhibit", true);
    private final BoolSetting resync = boolSetting("Resync", true);
    private final EnumSetting<InstantMode> instant = enumSetting("Instant", InstantMode.Normal);
    private final BoolSetting await = boolSetting("Await", true);
    private final IntSetting ticksExisted = intSetting("TicksExisted", 1, 0, 20, 1);
    private final EnumSetting<WeaknessMode> weakness = enumSetting("Weakness", WeaknessMode.Normal);
    private final BoolSetting rotate = boolSetting("Rotate", true);
    private final BoolSetting movementSync = boolSetting("MovementSync", true, rotate::getValue);
    private final DoubleSetting rotateSpeed = doubleSetting("RotateSpeed", 10.0, 1.0, 20.0, 0.5, rotate::getValue);
    private final DoubleSetting yawStep = doubleSetting("YawStep", 0.0, 0.0, 180.0, 1.0, rotate::getValue);
    private final BoolSetting facePlace = boolSetting("FacePlace", true);
    private final DoubleSetting facePlaceHealth = doubleSetting("Health", 8.0, 0.0, 36.0, 0.5);
    private final DoubleSetting armorThreshold = doubleSetting("Armor", 10.0, 0.0, 20.0, 0.5);
    private final BoolSetting damageSync = boolSetting("DamageSync", true);
    private final BoolSetting extrapolation = boolSetting("Extrapolation", false);
    private final IntSetting extrapolationTicks = intSetting("Ticks", 1, 0, 10, 1);
    private final BoolSetting draw = boolSetting("Draw", true);
    private final BoolSetting render = boolSetting("Render", true);
    private final BoolSetting fill = boolSetting("Fill", true);
    private final BoolSetting outline = boolSetting("Outline", true);
    private final DoubleSetting lineWidth = doubleSetting("LineWidth", 2.0, 0.5, 5.0, 0.5);
    private final BoolSetting fade = boolSetting("Fade", false);
    private final BoolSetting multiFade = boolSetting("MultiFade", false);
    private final IntSetting fadeTime = intSetting("FadeTime", 500, 100, 2000, 50);
    private final BoolSetting pause = boolSetting("Pause", true);
    private final BoolSetting mining = boolSetting("Mining", true);
    private final BoolSetting eating = boolSetting("Eating", true);
    private final BoolSetting targeting = boolSetting("Targeting", true);
    private final DoubleSetting targetRange = doubleSetting("TargetRange", 12.0, 4.0, 20.0, 0.1);
    private final DoubleSetting crystalRange = doubleSetting("CrystalRange", 6.0, 1.0, 12.0, 0.1);
    private final BoolSetting antiFriendKill = boolSetting("AntiFriend", false);
    private final BoolSetting forceSuicide = boolSetting("ForceSuicide", false);
    private final BoolSetting ignoreNakeds = boolSetting("IgnoreNakeds", false);
    private final BoolSetting assumeBestArmor = boolSetting("AssumeBestArmor", false);
    private final BoolSetting assumeInvincibility = boolSetting("AssumeInvincible", false);
    private final BoolSetting self = boolSetting("Self", false);
    private final DoubleSetting timeout = doubleSetting("Timeout", 0.5, 0.1, 5.0, 0.1);
    private final EnumSetting<AutoSwapMode> autoSwap = enumSetting("AutoSwap", AutoSwapMode.Normal);
    private final EnumSetting<SilentMode> silent = enumSetting("Silent", SilentMode.Off);
    private final BoolSetting swapBack = boolSetting("SwapBack", true);
    private final BoolSetting strictTiming = boolSetting("StrictTiming", false);
    private final DoubleSetting swapDelay = doubleSetting("SwapDelay", 0.0, 0.0, 5.0, 0.1);
    private final DoubleSetting swapPenalty = doubleSetting("SwapPenalty", 0.0, 0.0, 5.0, 0.1);
    private final IntSetting attackDelayMs = intSetting("AttackDelay", 150, 0, 1000, 10);
    private final IntSetting tickMs = intSetting("TickMs", 50, 1, 100, 1);
    private final IntSetting awaitDelayMs = intSetting("AwaitDelay", 200, 0, 1000, 10);
    private final IntSetting pendingClearMs = intSetting("PendingClear", 500, 0, 2000, 50);
    private final IntSetting resyncWindowMs = intSetting("ResyncWindow", 500, 0, 2000, 50);
    private final IntSetting instantWindowMs = intSetting("InstantWindow", 200, 0, 1000, 10);
    private final IntSetting instantOffCooldownMs = intSetting("InstantOffDelay", 150, 0, 1000, 10);
    private final IntSetting inhibitAttackWindowMs = intSetting("InhibitAttackWindow", 250, 0, 1000, 10);
    private final IntSetting inhibitRenderDelayMs = intSetting("InhibitRenderDelay", 50, 0, 500, 10);
    private final IntSetting attackHistorySize = intSetting("AttackHistorySize", 200, 50, 1000, 10);
    private final IntSetting attackHistoryMs = intSetting("AttackHistoryTime", 2000, 500, 10000, 100);
    private final IntSetting spawnHistoryMs = intSetting("SpawnHistoryTime", 5000, 500, 20000, 100);
    private final DoubleSetting spawnTrackRange = doubleSetting("SpawnTrackRange", 1.0, 0.0, 6.0, 0.1);
    private final IntSetting spawnTrackSize = intSetting("SpawnTrackSize", 20, 1, 200, 1);
    private final DoubleSetting crystalRangeBoost = doubleSetting("CrystalRangeBoost", 0.0, 0.0, 6.0, 0.1);
    private final DoubleSetting placeRangeBoost = doubleSetting("PlaceRangeBoost", 0.0, 0.0, 6.0, 0.1);
    private final DoubleSetting wallRangeBoost = doubleSetting("WallRangeBoost", 0.0, 0.0, 6.0, 0.1);
    private final BoolSetting renderDamageText = boolSetting("RenderDamageText", true, render::getValue);
    private final EnumSetting<MultipointMode> multipoint = enumSetting("Multipoint", MultipointMode.Corners);
    private final BoolSetting hbFix = boolSetting("HBFix", false);
    private final DoubleSetting multipointOffset = doubleSetting("MultipointOffset", 0.45, 0.0, 1.0, 0.05);
    private final DoubleSetting multipointLowY = doubleSetting("MultipointLowY", 0.05, 0.0, 1.0, 0.05);
    private final DoubleSetting multipointHighY = doubleSetting("MultipointHighY", 0.95, 0.0, 1.5, 0.05);
    private final DoubleSetting multipointAirY = doubleSetting("MultipointAirY", 1.0, 0.0, 2.0, 0.05);
    private final DoubleSetting lowerHitBoxOffset = doubleSetting("LowerHitBoxOffset", 0.3, 0.0, 1.0, 0.05, lowerHitBox::getValue);
    private final DoubleSetting placeHitPointRange = doubleSetting("PlaceHitPointRange", 4.5, 0.0, 10.0, 0.1);
    private final DoubleSetting placeHitRayRange = doubleSetting("PlaceHitRayRange", 4.5, 0.0, 10.0, 0.1);
    private final DoubleSetting hitSampleMin = doubleSetting("HitSampleMin", 0.0, -1.0, 1.0, 0.05);
    private final DoubleSetting hitSampleMax = doubleSetting("HitSampleMax", 1.0, -1.0, 2.0, 0.05);
    private final DoubleSetting hitSampleYLow = doubleSetting("HitSampleYLow", 0.0, -1.0, 2.0, 0.05);
    private final DoubleSetting hitSampleYMid = doubleSetting("HitSampleYMid", 0.5, -1.0, 2.0, 0.05);
    private final DoubleSetting hitSampleYHigh = doubleSetting("HitSampleYHigh", 1.0, -1.0, 2.0, 0.05);
    private final IntSetting inhibitMaxTicks = intSetting("InhibitMaxTicks", 10, 0, 40, 1);
    private final DoubleSetting explosionRadius = doubleSetting("ExplosionRadius", 12.0, 1.0, 20.0, 0.5);
    private final DoubleSetting explosionScale = doubleSetting("ExplosionScale", 7.0, 0.0, 20.0, 0.5);
    private final DoubleSetting explosionPower = doubleSetting("ExplosionPower", 12.0, 0.0, 30.0, 0.5);
    private final DoubleSetting explosionBase = doubleSetting("ExplosionBase", 1.0, 0.0, 10.0, 0.5);
    private final ColorSetting fillColor = colorSetting("FillColor", new Color(255, 80, 120, 80));
    private final ColorSetting outlineColor = colorSetting("OutlineColor", new Color(255, 80, 120, 160));

    private LivingEntity currentTarget;
    private BlockPos renderPos;
    private float renderDamage;
    private BlockPos lastPlaced;
    private BlockPos lastBroken;
    private BlockPos pendingPlace;
    private Vec3 pendingRotation;
    private float[] pendingRotationAngles;
    private boolean skip;
    private int lastSwapSlot = -1;
    private int lastBreakId = -1;
    private int breakRepeats;

    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();
    private final TimerUtils swapDelayTimer = new TimerUtils();
    private final TimerUtils swapPenaltyTimer = new TimerUtils();
    private final TimerUtils resyncTimer = new TimerUtils();
    private final TimerUtils rotationTimer = new TimerUtils();
    private final TimerUtils timeoutTimer = new TimerUtils();
    private final TimerUtils miningTimer = new TimerUtils();

    private final AtomicReference<PlacementResult> bestPlacement = new AtomicReference<>();
    private final AtomicReference<Vec3> rotationTarget = new AtomicReference<>();
    private final AtomicBoolean swapping = new AtomicBoolean(false);
    private CompletableFuture<PlacementResult> calcFuture;
    private final Set<Integer> recentAttacks = new HashSet<>();
    private final Map<Integer, Long> attackTimes = new HashMap<>();
    private final Map<Integer, Long> spawnTimes = new HashMap<>();
    private final List<FadeEntry> fadeEntries = new ArrayList<>();
    private final java.util.Deque<BlockPos> recentSpawns = new java.util.ArrayDeque<>();

    private AutoCrystal() {
        super("AutoCrystal", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        renderPos = null;
        renderDamage = 0.0f;
        lastPlaced = null;
        lastBroken = null;
        pendingPlace = null;
        pendingRotation = null;
        pendingRotationAngles = null;
        skip = false;
        lastSwapSlot = -1;
        lastBreakId = -1;
        breakRepeats = 0;
        bestPlacement.set(null);
        rotationTarget.set(null);
        recentAttacks.clear();
        attackTimes.clear();
        spawnTimes.clear();
        fadeEntries.clear();
        recentSpawns.clear();
        swapBack();
        cancelFuture();
    }

    @Override
    public String getInfo() {
        if (currentTarget != null) {
            String name = currentTarget.getName().getString();
            if (renderDamageText.getValue()) {
                return name + " " + String.format("%.1f", renderDamage) + " " + recentSpawns.size();
            }
            return name + " " + recentSpawns.size();
        }
        return super.getInfo();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        updateTimers();
        cleanupAttacks();
        if (shouldPause()) return;

        updateTarget();
        if (currentTarget == null) {
            renderPos = null;
            renderDamage = 0.0f;
            return;
        }

        handleRotationSync();
        handleCalculation();

        if (breakCrystals.getValue()) {
            handleBreak();
        }

        if (place.getValue()) {
            handlePlace();
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck() || !breakCrystals.getValue()) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundAddEntityPacket addEntity) {
            if (addEntity.getType() != net.minecraft.world.entity.EntityType.END_CRYSTAL) return;
            if (instant.getValue() == InstantMode.Off) return;

            Vec3 pos = new Vec3(addEntity.getX(), addEntity.getY(), addEntity.getZ());
            spawnTimes.put(addEntity.getId(), System.currentTimeMillis());
            markSpawn(addEntity.getId());
            if (!isInRange(pos)) return;
            if (currentTarget == null) return;

            if (lastPlaced != null) {
                double dist = pos.distanceTo(Vec3.atCenterOf(lastPlaced));
                if (dist <= spawnTrackRange.getValue()) {
                    recentSpawns.addFirst(lastPlaced);
                    while (recentSpawns.size() > spawnTrackSize.getValue()) {
                        recentSpawns.removeLast();
                    }
                }
            }

            if (pendingPlace != null) {
                BlockPos spawned = new BlockPos((int) pos.x, (int) (pos.y - 1), (int) pos.z);
                if (spawned.equals(pendingPlace)) {
                    pendingPlace = null;
                }
            }

            double targetDamage = calculateDamage(CLIENT.level, pos, currentTarget);
            double selfDamage = calculateDamage(CLIENT.level, pos, CLIENT.player);
            SelfPreserve preserve = SelfPreserve.fromSelfDamage(CLIENT.player, selfDamage);

            if (!isDamageAcceptable(targetDamage, selfDamage, preserve)) {
                return;
            }

            if (instant.getValue() == InstantMode.Always && lastBroken != null && lastBroken.equals(new BlockPos((int) pos.x, (int) pos.y - 1, (int) pos.z))) {
                return;
            }

            if (rotate.getValue()) {
                rotateTo(pos);
            }

            attackById(addEntity.getId());
        }

        if (packet instanceof ClientboundEntityEventPacket eventPacket) {
            if (currentTarget != null && eventPacket instanceof com.github.lumin.mixins.ducks.EntityEventPacketAccess access) {
                if (access.lumin$getEntityId() == currentTarget.getId() && access.lumin$getEventId() == 3) {
                    if (currentTarget instanceof com.github.lumin.mixins.ducks.PlayerHurtAccess hurtAccess) {
                        hurtAccess.lumin$setHurt(true);
                    }
                    timeoutTimer.reset();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck() || !render.getValue() || !draw.getValue()) return;

        if (fade.getValue()) {
            renderFadeEntries(event);
            return;
        }

        if (renderPos == null) return;
        drawBox(event, renderPos, 1.0f);
    }

    private void updateTimers() {
        if (swapDelayTimer.getTime() > 10000) swapDelayTimer.reset();
        if (swapPenaltyTimer.getTime() > 10000) swapPenaltyTimer.reset();
        if (resyncTimer.getTime() > 10000) resyncTimer.reset();
        if (rotationTimer.getTime() > 10000) rotationTimer.reset();
        if (timeoutTimer.getTime() > 10000) timeoutTimer.reset();
        if (miningTimer.getTime() > 10000) miningTimer.reset();
    }

    private boolean shouldPause() {
        if (!pause.getValue()) return false;
        if (CLIENT.player.isSleeping()) return true;
        if (CLIENT.player.isUsingItem() && eating.getValue()) return true;
        if (CLIENT.gameMode != null && CLIENT.gameMode.isDestroying() && mining.getValue()) {
            if (!mineIgnore.getValue()) return true;
            if (!isMineIgnoreReady()) return true;
        }
        return false;
    }

    private void updateTarget() {
        if (!targeting.getValue()) return;
        if (currentTarget != null && isInvalidTarget(currentTarget)) {
            currentTarget = null;
        }

        if (currentTarget == null) {
            currentTarget = findTarget();
        }

        if (currentTarget instanceof com.github.lumin.mixins.ducks.PlayerHurtAccess hurtAccess) {
            if (timeoutTimer.passedSecond(timeout.getValue())) {
                hurtAccess.lumin$setHurt(false);
            }
        }
    }

    private LivingEntity findTarget() {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (isInvalidTarget(living)) continue;
            targets.add(living);
        }
        if (self.getValue() && CLIENT.player != null && !targets.contains(CLIENT.player)) {
            targets.add(CLIENT.player);
        }
        if (targets.isEmpty()) return null;
        targets.sort(Comparator.comparingDouble(o -> -estimateTargetDamage(o)));
        return targets.getFirst();
    }

    private boolean isInvalidTarget(LivingEntity entity) {
        if (!(entity instanceof Player)) return true;
        if (entity == CLIENT.player && !(forceSuicide.getValue() || self.getValue())) return true;
        if (!entity.isAlive() || entity.isDeadOrDying()) return true;
        if (entity.isSpectator()) return true;
        if (entity.distanceTo(CLIENT.player) > targetRange.getValue()) return true;
        if (antiFriendKill.getValue() && isFriendLike(entity)) return true;
        if (ignoreNakeds.getValue() && entity instanceof Player player) {
            if (player.getArmorValue() <= 0) return true;
        }
        return false;
    }

    private boolean isFriendLike(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Player selfPlayer = CLIENT.player;
        if (selfPlayer == null) return false;
        if (player.getTeam() != null && selfPlayer.getTeam() != null) {
            return player.getTeam().isAlliedTo(selfPlayer.getTeam());
        }
        return false;
    }

    private double estimateTargetDamage(LivingEntity target) {
        Vec3 pos = getPredictedPosition(target);
        double damage = calculateDamage(CLIENT.level, pos, target);
        double dist = CLIENT.player.distanceTo(target);
        return damage * 2.0 - dist;
    }

    private void handleCalculation() {
        if (calcFuture != null && calcFuture.isDone()) {
            try {
                PlacementResult result = calcFuture.get();
                bestPlacement.set(result);
                if (result != null) {
                    renderDamage = (float) result.damage;
                    renderPos = result.pos;
                } else {
                    renderPos = null;
                    renderDamage = 0.0f;
                }
            } catch (Exception ignored) {
            }
        }

        if (calcFuture == null || calcFuture.isDone() || calcFuture.isCancelled()) {
            calcFuture = CompletableFuture.supplyAsync(this::calculatePlacement, EXECUTOR);
        }
    }

    private PlacementResult calculatePlacement() {
        if (currentTarget == null) return null;

        PlacementResult best = null;
        int r = (int) Math.ceil(range.getValue());
        BlockPos center = currentTarget.blockPosition();

        Iterable<BlockPos> positions;
        if (sequential.getValue() == SequentialMode.Always && lastPlaced != null && canPlace(lastPlaced)) {
            positions = List.of(lastPlaced);
        } else {
            positions = BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r));
        }

        for (BlockPos pos : positions) {
            if (!canPlace(pos)) continue;
            if (sequential.getValue() == SequentialMode.Once && lastPlaced != null && !pos.equals(lastPlaced)) {
                continue;
            }

            DamageSample sample = calculateMultipointDamage(pos, currentTarget);
            if (sample == null) continue;

            double targetDamage = sample.targetDamage;
            double selfDamage = sample.selfDamage;
            SelfPreserve preserve = SelfPreserve.fromSelfDamage(CLIENT.player, selfDamage);

            if (!isDamageAcceptable(targetDamage, selfDamage, preserve)) continue;

            Vec3 placeVec = sample.point;
            double dist = CLIENT.player.getEyePosition().distanceTo(placeVec);
            boolean visible = isVisible(placeVec);
            if (!visible && !smartTrace.getValue()) continue;
            double maxRange = visible ? range.getValue() + placeRangeBoost.getValue() : wallRange.getValue() + wallRangeBoost.getValue();
            if (dist > maxRange) continue;

            PlaceMode mode = visible ? PlaceMode.Visible : PlaceMode.Wall;
            if (!isBaseBlock(pos)) {
                if (basePlace.getValue()) {
                    mode = PlaceMode.Base;
                } else {
                    continue;
                }
            }

            PlacementResult candidate = new PlacementResult(pos.immutable(), currentTarget, targetDamage, mode, placeVec);
            best = candidate.betterThan(best);
        }
        return best;
    }

    private void handlePlace() {
        if (SPEEDMINE.isEnabled() && SPEEDMINE.swapReset.getValue() && SPEEDMINE.swapResetFlag) {
            SPEEDMINE.swapResetFlag = false;
            if (swapBack.getValue()) {
                swapBack();
            }
            return;
        }
        if (!placeTimer.passedMillise((long) delay.getValue() * tickMs.getValue())) return;

        if (SPEEDMINE.isEnabled() && SPEEDMINE.isRebreakActive()) {
            if (SPEEDMINE.getProgressRatio() > 0.0f && SPEEDMINE.getProgressRatio() < 1.0f) {
                return;
            }
        }
        if (pendingPlace != null && !canPlace(pendingPlace)) pendingPlace = null;
        if (strictTiming.getValue() && skip) return;

        if (damageSync.getValue() && renderDamage < minDamage.getValue()) {
            if (!placeTimer.passedMillise((long) delay.getValue() * tickMs.getValue())) {
                return;
            }
        }

        PlacementResult result = bestPlacement.get();
        if (result == null || result.pos == null || currentTarget == null) {
            return;
        }

        BlockHitResult placeHit = computePlaceHit(result.pos);
        Vec3 placeVec = placeHit != null ? placeHit.getLocation() : (result.point != null ? result.point : Vec3.atCenterOf(result.pos.above()));
        double targetDamage = result.damage;
        double selfDamage = calculateDamage(CLIENT.level, placeVec, CLIENT.player);
        SelfPreserve preserve = SelfPreserve.fromSelfDamage(CLIENT.player, selfDamage);

        if (!isDamageAcceptable(targetDamage, selfDamage, preserve)) {
            return;
        }

        int crystalSlot = findCrystalSlot();
        boolean offhandCrystal = CLIENT.player.getOffhandItem().is(Items.END_CRYSTAL);
        if (!offhandCrystal && crystalSlot == -1 && autoSwap.getValue() != AutoSwapMode.BlockTarget) {
            return;
        }

        if (!swapDelayReady()) return;

        if (rotate.getValue()) {
            rotateTo(placeVec);
        }

        if (autoSwap.getValue() == AutoSwapMode.BlockTarget && !offhandCrystal && !CLIENT.player.getMainHandItem().is(Items.END_CRYSTAL)) {
            return;
        }
        if (autoSwap.getValue() != AutoSwapMode.BlockTarget && !offhandCrystal) {
            if (!swapToSlot(crystalSlot)) return;
        }

        placeCrystal(result.pos, placeHit, placeVec, offhandCrystal);
        lastPlaced = result.pos;
        pendingPlace = result.pos;
        addFadeEntry(result.pos);
        placeTimer.reset();

        if (swapBack.getValue()) {
            swapBack();
        }

        if (strictTiming.getValue()) {
            skip = !skip;
        }
    }

    private void handleBreak() {
        if (SPEEDMINE.isEnabled() && SPEEDMINE.swapReset.getValue() && SPEEDMINE.swapResetFlag) {
            SPEEDMINE.swapResetFlag = false;
            if (swapBack.getValue()) {
                swapBack();
            }
            return;
        }
        if (!breakTimer.passedMillise((long) delay.getValue() * tickMs.getValue())) return;

        if (SPEEDMINE.isEnabled() && SPEEDMINE.isRebreakActive()) {
            if (SPEEDMINE.getProgressRatio() > 0.0f && SPEEDMINE.getProgressRatio() < 1.0f) {
                return;
            }
        }
        if (currentTarget == null) return;
        if (!swapPenaltyReady()) return;
        if (strictTiming.getValue() && skip) return;
        if (pendingPlace != null && await.getValue() && !placeTimer.passedMillise(awaitDelayMs.getValue())) {
            if (!hasCrystalAt(pendingPlace)) {
                return;
            }
        }
        if (instant.getValue() == InstantMode.Off && lastBroken != null && !resyncTimer.passedMillise(instantOffCooldownMs.getValue())) {
            return;
        }
        if (pendingPlace != null) {
            if (hasCrystalAt(pendingPlace) || placeTimer.passedMillise(pendingClearMs.getValue())) {
                pendingPlace = null;
            }
        }
        if (await.getValue() && pendingPlace != null && !hasCrystalAt(pendingPlace) && !placeTimer.passedMillise(awaitDelayMs.getValue())) return;
        if (resync.getValue() && lastBreakId != -1 && !resyncTimer.passedMillise(resyncWindowMs.getValue())) {
            Entity entity = CLIENT.level.getEntity(lastBreakId);
            if (entity instanceof EndCrystal) return;
        }
        if (resync.getValue() && lastBreakId != -1 && resyncTimer.passedMillise(resyncWindowMs.getValue())) {
            lastBreakId = -1;
        }

        EndCrystal best = null;
        double bestDamage = -1.0;

        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!isCrystalValid(crystal)) continue;

            Vec3 pos = crystal.position();
            Vec3 predicted = getPredictedPosition(currentTarget);
            double rangeValue = crystalRange.getValue() + crystalRangeBoost.getValue();
            if (predicted.distanceToSqr(pos) > (rangeValue * rangeValue)) continue;
            double targetDamage = calculateDamage(CLIENT.level, pos, currentTarget);
            double selfDamage = calculateDamage(CLIENT.level, pos, CLIENT.player);
            SelfPreserve preserve = SelfPreserve.fromSelfDamage(CLIENT.player, selfDamage);

            if (!isDamageAcceptable(targetDamage, selfDamage, preserve)) continue;

            if (targetDamage > bestDamage) {
                bestDamage = targetDamage;
                best = crystal;
            }
        }

        if (best == null) return;

        if (instant.getValue() == InstantMode.Always && lastBroken != null) {
            BlockPos candidate = best.blockPosition().below();
            if (candidate.equals(lastBroken) && !resyncTimer.passedMillise(instantWindowMs.getValue())) {
                return;
            }
        }

        Long attackedAt = attackTimes.get(best.getId());
        int delayMs = attackDelayMs.getValue();
        if (attackedAt != null && delayMs > 0 && System.currentTimeMillis() - attackedAt < delayMs) {
            return;
        }

        if (rotate.getValue()) {
            rotateTo(best.position());
        }

        int repeats = Math.max(1, factor.getValue());
        for (int i = 0; i < repeats; i++) {
            attackCrystal(best);
        }
        lastBroken = best.blockPosition().below();
        addFadeEntry(lastBroken);
        breakTimer.reset();
        if (strictTiming.getValue()) {
            skip = !skip;
        }
    }

    private boolean isCrystalValid(EndCrystal crystal) {
        if (!crystal.isAlive() || crystal.isRemoved()) return false;
        if (crystal.tickCount < ticksExisted.getValue()) return false;
        if (recentAttacks.contains(crystal.getId())) return false;
        if (inhibit.getValue()) {
            if (crystal instanceof com.github.lumin.mixins.ducks.EndCrystalAccess access && access.lumin$getSpawnTime() == 0L) {
                Long stored = spawnTimes.get(crystal.getId());
                if (stored != null) {
                    access.lumin$setSpawnTime(stored);
                }
            }
            Long attackedAt = attackTimes.get(crystal.getId());
            if (attackedAt != null && System.currentTimeMillis() - attackedAt < inhibitAttackWindowMs.getValue()) {
                return false;
            }
        }

        Vec3 pos = crystal.position();
        if (!isInRange(pos)) return false;
        return true;
    }

    private boolean isInRange(Vec3 pos) {
        boolean visible = isVisible(pos);
        double max = visible ? range.getValue() + placeRangeBoost.getValue() : wallRange.getValue() + wallRangeBoost.getValue();
        if (smartTrace.getValue() && !visible) {
            max = Math.min(max, wallRange.getValue());
        }
        return CLIENT.player.getEyePosition().distanceTo(pos) <= max;
    }

    private boolean isDamageAcceptable(double targetDamage, double selfDamage, SelfPreserve preserve) {
        if (preserve.force) return false;
        if (!isValidDamage(targetDamage)) return false;
        double targetHealth = currentTarget != null ? currentTarget.getHealth() + currentTarget.getAbsorptionAmount() : 0.0;
        boolean targetLethal = targetHealth > 0.0 && targetDamage >= targetHealth;
        boolean targetTotem = currentTarget instanceof Player player && hasTotem(player);
        if (antiSuicide.getValue() && !forceSuicide.getValue()) {
            float health = CLIENT.player.getHealth() + CLIENT.player.getAbsorptionAmount();
            if (selfDamage >= health && !hasTotem(CLIENT.player)) {
                return false;
            }
        }
        if (damageSync.getValue() && currentTarget instanceof com.github.lumin.mixins.ducks.PlayerHurtAccess hurtAccess) {
            if (hurtAccess.lumin$isHurt() && !timeoutTimer.passedSecond(timeout.getValue())) {
                return false;
            }
        }
        if (assumeInvincibility.getValue() && !timeoutTimer.passedSecond(timeout.getValue())) return false;
        if (selfDamage > maxSelfDamage.getValue() && !forceSuicide.getValue()) return false;
        if (balance.getValue() && targetDamage <= selfDamage && !forceSuicide.getValue() && !(targetLethal && targetTotem)) return false;
        if (targetDamage > 0.0 && selfDamage > 0.0) {
            if (!targetLethal && targetDamage / selfDamage < damageRatio.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidDamage(double damage) {
        if (facePlace.getValue() && currentTarget != null) {
            if (currentTarget.getHealth() + currentTarget.getAbsorptionAmount() <= facePlaceHealth.getValue()) {
                return true;
            }
            if (currentTarget instanceof Player player && !assumeBestArmor.getValue()) {
                if (player.getArmorValue() <= armorThreshold.getValue()) {
                    return true;
                }
            }
        }
        if (assumeBestArmor.getValue() && currentTarget instanceof Player player) {
            if (player.getArmorValue() > armorThreshold.getValue()) {
                return false;
            }
        }
        if (currentTarget != null) {
            double targetHealth = currentTarget.getHealth() + currentTarget.getAbsorptionAmount();
            if (damage >= targetHealth) {
                return true;
            }
        }
        return damage >= minDamage.getValue();
    }

    private boolean canPlace(BlockPos pos) {
        if (CLIENT.level == null) return false;

        BlockState base = CLIENT.level.getBlockState(pos);
        if (base.isAir() && !airPlace.getValue()) return false;
        if (!isBaseBlock(pos) && !basePlace.getValue()) return false;

        BlockPos up = pos.above();
        if (!CLIENT.level.getBlockState(up).canBeReplaced()) return false;
        if (!oneDotTwelve.getValue() && !CLIENT.level.getBlockState(up.above()).canBeReplaced()) return false;

        AABB box = new AABB(up.getX(), up.getY(), up.getZ(), up.getX() + 1.0, up.getY() + (oneDotTwelve.getValue() ? 2.0 : 1.0), up.getZ() + 1.0);
        List<Entity> entities = CLIENT.level.getEntities((Entity) null, box, entity -> !(entity instanceof EndCrystal));
        if (!entities.isEmpty()) {
            if (hbFix.getValue()) {
                boolean blocked = false;
                for (Entity entity : entities) {
                    if (entity instanceof ItemEntity) continue;
                    if (entity == CLIENT.player && lowerHitBox.getValue()) continue;
                    blocked = true;
                    break;
                }
                if (blocked) return false;
            } else {
                return false;
            }
        }

        if (limit.getValue() && !base.isAir() && base.getBlock() == Blocks.FIRE) {
            return false;
        }

        if (strictDirection.getValue()) {
            Direction dir = getPlaceFacing(pos);
            if (dir == null) return false;
        }

        return true;
    }

    private boolean isBaseBlock(BlockPos pos) {
        BlockState state = CLIENT.level.getBlockState(pos);
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) return true;
        if (SPEEDMINE.isEnabled() && SPEEDMINE.isRebreakActive()) {
            BlockPos breakPos = SPEEDMINE.getBreakPos();
            if (breakPos != null && breakPos.equals(pos)) return true;
            BlockPos assistPos = SPEEDMINE.getAssistPos();
            if (assistPos != null && assistPos.equals(pos) && SPEEDMINE.isAssistActive()) return true;
        }
        return false;
    }

    private Direction getPlaceFacing(BlockPos basePos) {
        if (!strictDirection.getValue()) return Direction.UP;
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) continue;
            BlockPos neighbor = basePos.relative(direction);
            if (!CLIENT.level.getBlockState(neighbor).isAir()) {
                return direction.getOpposite();
            }
        }
        return Direction.UP;
    }

    private boolean isVisible(Vec3 pos) {
        Vec3 eyes = CLIENT.player.getEyePosition();
        BlockHitResult result = CLIENT.level.clip(new ClipContext(eyes, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player));
        if (result.getType() == HitResult.Type.MISS) return true;
        return result.getLocation().distanceTo(pos) < 0.3;
    }

    private void placeCrystal(BlockPos pos, BlockHitResult hitResult, Vec3 placeVec, boolean useOffhand) {
        InteractionHand hand = useOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Direction side = hitResult != null ? hitResult.getDirection() : (strictDirection.getValue() ? getPlaceFacing(pos) : Direction.UP);
        if (side == null) side = Direction.UP;
        Vec3 hitVec = hitResult != null ? hitResult.getLocation() : placeVec.add(new Vec3(side.getStepX(), side.getStepY(), side.getStepZ()).scale(0.5));
        BlockHitResult hit = hitResult != null ? hitResult : new BlockHitResult(hitVec, side, pos, false);
        CLIENT.gameMode.useItemOn(CLIENT.player, hand, hit);
        CLIENT.player.swing(hand);
    }

    private BlockHitResult computePlaceHit(BlockPos pos) {
        Vec3 base = Vec3.atLowerCornerOf(pos);
        double[] offsetsXZ = new double[]{hitSampleMin.getValue(), hitSampleMax.getValue()};
        double[] offsetsY = new double[]{hitSampleYLow.getValue(), hitSampleYMid.getValue(), hitSampleYHigh.getValue()};

        Direction preferred = getPlaceFacing(pos);
        BlockHitResult best = null;
        double bestDistance = Double.MAX_VALUE;

        for (double ox : offsetsXZ) {
            for (double oy : offsetsY) {
                for (double oz : offsetsXZ) {
                    Vec3 point = base.add(ox, oy, oz);
                    BlockHitResult hit = CLIENT.level.clip(new ClipContext(CLIENT.player.getEyePosition(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player));
                    if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
                        double pointDist = point.distanceTo(CLIENT.player.getEyePosition());
                        double hitDist = hit.getLocation().distanceTo(CLIENT.player.getEyePosition());
                        if (pointDist <= placeHitPointRange.getValue() && hitDist <= placeHitRayRange.getValue()) {
                            if (strictDirection.getValue() && preferred != null && hit.getType() != HitResult.Type.MISS && hit.getDirection() != preferred) {
                                continue;
                            }
                            if (hitDist < bestDistance) {
                                bestDistance = hitDist;
                                best = hit;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }

    private void attackCrystal(EndCrystal crystal) {
        if (weaknessSwap()) return;
        CLIENT.gameMode.attack(CLIENT.player, crystal);
        CLIENT.player.swing(InteractionHand.MAIN_HAND);
        markAttacked(crystal.getId());
        if (crystal instanceof com.github.lumin.mixins.ducks.EndCrystalAccess access) {
            access.lumin$setMioAttacked(true);
        }
    }

    private void attackById(int id) {
        Entity entity = CLIENT.level.getEntity(id);
        if (entity instanceof EndCrystal crystal) {
            attackCrystal(crystal);
        }
    }

    private void rotateTo(Vec3 pos) {
        Vector2f rotations = RotationUtils.calculate(pos);
        rotations = applyYawStep(rotations);
        MovementFix fix = movementSync.getValue() ? MovementFix.ON : MovementFix.OFF;
        RotationManager.INSTANCE.setRotations(rotations, rotateSpeed.getValue(), fix, Priority.High);
    }

    private Vector2f applyYawStep(Vector2f target) {
        double step = yawStep.getValue();
        if (step <= 0.0) return target;
        float currentYaw = RotationManager.INSTANCE.getYaw();
        float yawDiff = Mth.wrapDegrees(target.x - currentYaw);
        if (Math.abs(yawDiff) <= step) return target;
        float limitedYaw = currentYaw + (float) (Math.copySign(step, yawDiff));
        return new Vector2f(limitedYaw, target.y);
    }

    private int findCrystalSlot() {
        FindItemResult result = InvUtils.findInHotbar(Items.END_CRYSTAL);
        return result.slot();
    }

    private boolean swapDelayReady() {
        if (autoSwap.getValue() != AutoSwapMode.Normal) return true;
        return swapDelayTimer.passedMillise((long) (swapDelay.getValue() * tickMs.getValue()));
    }

    private boolean swapPenaltyReady() {
        if (autoSwap.getValue() == AutoSwapMode.BlockTarget) return true;
        if (swapPenalty.getValue() <= 0.0) return true;
        return swapPenaltyTimer.passedMillise((long) (swapPenalty.getValue() * tickMs.getValue()));
    }

    private boolean swapToSlot(int slot) {
        if (slot == -1) return false;
        if (autoSwap.getValue() == AutoSwapMode.BlockTarget) return false;
        if (slot == CLIENT.player.getInventory().getSelectedSlot()) return true;

        if (autoSwap.getValue() == AutoSwapMode.Silent) {
            if (!InvUtils.invSwap(slot)) return false;
            swapping.set(true);
        } else if (silent.getValue() == SilentMode.Inventory) {
            if (!InvUtils.invSwap(slot)) return false;
        } else if (silent.getValue() == SilentMode.Spoof) {
            if (!InvUtils.invSwap(slot)) return false;
            swapping.set(true);
        } else {
            lastSwapSlot = CLIENT.player.getInventory().getSelectedSlot();
            InvUtils.swap(slot, false);
        }
        swapDelayTimer.reset();
        return true;
    }

    private void swapBack() {
        if (autoSwap.getValue() == AutoSwapMode.BlockTarget) return;
        if (autoSwap.getValue() == AutoSwapMode.Silent || silent.getValue() == SilentMode.Inventory || silent.getValue() == SilentMode.Spoof) {
            InvUtils.invSwapBack();
            swapping.set(false);
            return;
        }
        if (lastSwapSlot != -1) {
            InvUtils.swap(lastSwapSlot, false);
            lastSwapSlot = -1;
        }
    }

    private boolean weaknessSwap() {
        if (weakness.getValue() == WeaknessMode.Off) return false;
        if (!CLIENT.player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) return false;
        ItemStack main = CLIENT.player.getMainHandItem();
        boolean hasWeapon = main.is(ItemTags.SWORDS) || main.is(ItemTags.AXES);
        if (hasWeapon) return false;
        if (weakness.getValue() == WeaknessMode.Normal) return true;
        int slot = findWeaponSlot();
        if (slot == -1) return true;
        InvUtils.swap(slot, true);
        return false;
    }

    private int findWeaponSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = CLIENT.player.getInventory().getItem(i);
            if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
                return i;
            }
        }
        return -1;
    }

    private void handleRotationSync() {
        Vec3 rot = rotationTarget.get();
        if (rot != null && rotate.getValue()) {
            rotateTo(rot);
            rotationTarget.set(null);
        }
    }

    private void markAttacked(int id) {
        recentAttacks.add(id);
        attackTimes.put(id, System.currentTimeMillis());
        if (CLIENT.level != null) {
            Entity entity = CLIENT.level.getEntity(id);
            if (entity instanceof EndCrystal crystal && crystal instanceof com.github.lumin.mixins.ducks.EndCrystalAccess access) {
                access.lumin$setMioAttacked(true);
            }
        }
        lastBreakId = id;
        resyncTimer.reset();
        swapPenaltyTimer.reset();
    }

    private void markSpawn(int id) {
        if (CLIENT.level == null) return;
        Entity entity = CLIENT.level.getEntity(id);
        if (entity instanceof EndCrystal crystal && crystal instanceof com.github.lumin.mixins.ducks.EndCrystalAccess access) {
            if (access.lumin$getSpawnTime() == 0L) {
                access.lumin$setSpawnTime(System.currentTimeMillis());
            }
        }
    }

    public boolean shouldInhibitRender(EndCrystal crystal) {
        if (!isEnabled() || !inhibit.getValue()) return false;
        if (!(crystal instanceof com.github.lumin.mixins.ducks.EndCrystalAccess access)) return false;
        if (!access.lumin$isMioAttacked()) return false;
        if (crystal.tickCount >= inhibitMaxTicks.getValue()) return false;
        long spawn = access.lumin$getSpawnTime();
        if (spawn == 0L) {
            Long stored = spawnTimes.get(crystal.getId());
            spawn = stored != null ? stored : 0L;
        }
        long now = System.currentTimeMillis();
        return spawn != 0L && now - spawn > inhibitRenderDelayMs.getValue();
    }

    private void cleanupAttacks() {
        long now = System.currentTimeMillis();
        if (recentAttacks.size() > attackHistorySize.getValue()) {
            recentAttacks.clear();
        }
        attackTimes.entrySet().removeIf(entry -> now - entry.getValue() > attackHistoryMs.getValue());
        spawnTimes.entrySet().removeIf(entry -> now - entry.getValue() > spawnHistoryMs.getValue());
    }

    private boolean hasCrystalAt(BlockPos pos) {
        AABB box = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        List<Entity> entities = CLIENT.level.getEntities((Entity) null, box, entity -> entity instanceof EndCrystal);
        return !entities.isEmpty();
    }

    private boolean isMineIgnoreReady() {
        if (!mineIgnore.getValue()) return false;
        if (!SPEEDMINE.isEnabled() || !SPEEDMINE.isRebreakActive()) return true;
        return SPEEDMINE.recentMine(100L);
    }

    private void cancelFuture() {
        if (calcFuture != null && !calcFuture.isDone() && !calcFuture.isCancelled()) {
            calcFuture.cancel(true);
        }
    }

    private void addFadeEntry(BlockPos pos) {
        if (pos == null || !fade.getValue()) return;
        long now = System.currentTimeMillis();
        if (!multiFade.getValue()) {
            fadeEntries.clear();
        }
        fadeEntries.add(new FadeEntry(pos, now));
    }

    private void renderFadeEntries(RenderLevelStageEvent.AfterEntities event) {
        if (fadeEntries.isEmpty()) return;
        long now = System.currentTimeMillis();
        int duration = Math.max(1, fadeTime.getValue());

        Iterator<FadeEntry> iterator = fadeEntries.iterator();
        while (iterator.hasNext()) {
            FadeEntry entry = iterator.next();
            float progress = (now - entry.startMs) / (float) duration;
            if (progress >= 1.0f) {
                iterator.remove();
                continue;
            }
            float alpha = 1.0f - progress;
            drawBox(event, entry.pos, alpha);
        }
    }

    private void drawBox(RenderLevelStageEvent.AfterEntities event, BlockPos pos, float alpha) {
        AABB box = new AABB(pos);
        Color fillCol = applyAlpha(fillColor.getValue(), alpha);
        Color outlineCol = applyAlpha(outlineColor.getValue(), alpha);
        if (fill.getValue() && outline.getValue()) {
            Render3DUtils.drawFullBox(event.getPoseStack(), box, fillCol, outlineCol, lineWidth.getValue().floatValue());
        } else if (fill.getValue()) {
            Render3DUtils.drawFilledBox(event.getPoseStack(), box, fillCol);
        } else if (outline.getValue()) {
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, outlineCol.getRGB(), lineWidth.getValue().floatValue());
        }
    }

    private Color applyAlpha(Color color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (color.getAlpha() * alpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private DamageSample calculateMultipointDamage(BlockPos basePos, LivingEntity target) {
        List<Vec3> points = getMultipoints(basePos);
        if (points.isEmpty()) return null;

        DamageSample best = null;
        for (Vec3 point : points) {
            double targetDamage = calculateDamage(CLIENT.level, point, target);
            double selfDamage = calculateDamage(CLIENT.level, point, CLIENT.player);
            if (best == null || targetDamage > best.targetDamage) {
                best = new DamageSample(targetDamage, selfDamage, point);
            }
        }
        return best;
    }

    private List<Vec3> getMultipoints(BlockPos basePos) {
        Vec3 base = Vec3.atCenterOf(basePos.above());
        if (lowerHitBox.getValue()) {
            base = base.add(0.0, -lowerHitBoxOffset.getValue(), 0.0);
        }
        List<Vec3> points = new ArrayList<>();
        switch (multipoint.getValue()) {
            case Air -> {
                points.add(base.add(0.0, multipointLowY.getValue(), 0.0));
                points.add(base.add(0.0, multipointAirY.getValue(), 0.0));
            }
            case Corners -> {
                double o = multipointOffset.getValue();
                double y = multipointLowY.getValue();
                points.add(base.add(-o, y, -o));
                points.add(base.add(-o, y, o));
                points.add(base.add(o, y, -o));
                points.add(base.add(o, y, o));
            }
            case Full -> {
                double o = multipointOffset.getValue();
                double yLow = multipointLowY.getValue();
                double yHigh = multipointHighY.getValue();
                points.add(base.add(-o, yLow, -o));
                points.add(base.add(-o, yLow, o));
                points.add(base.add(o, yLow, -o));
                points.add(base.add(o, yLow, o));
                points.add(base.add(-o, yHigh, -o));
                points.add(base.add(-o, yHigh, o));
                points.add(base.add(o, yHigh, -o));
                points.add(base.add(o, yHigh, o));
            }
            case Fast -> {
                points.add(base);
                double o = multipointOffset.getValue();
                double y = multipointLowY.getValue();
                points.add(base.add(o, y, o));
            }
            default -> points.add(base);
        }
        return points;
    }

    private Vec3 getPredictedPosition(LivingEntity target) {
        if (!extrapolation.getValue() || target == null) return target.position();
        Vec3 delta = target.getDeltaMovement();
        return target.position().add(delta.scale(extrapolationTicks.getValue()));
    }

    private double calculateDamage(Level level, Vec3 pos, LivingEntity target) {
        if (level == null || target == null) return 0.0;

        Vec3 targetPos = target.position();
        if (extrapolation.getValue() && target != CLIENT.player) {
            targetPos = getPredictedPosition(target);
        }
        Vec3 offset = targetPos.subtract(target.position());
        AABB box = target.getBoundingBox().move(offset);

        double distance = targetPos.distanceTo(pos);
        double exposure = getExposure(pos, box);
        double impact = (1.0 - (distance / explosionRadius.getValue())) * exposure;
        if (impact <= 0.0) return 0.0;

        float damage = (float) ((impact * impact + impact) * explosionScale.getValue() * explosionPower.getValue() + explosionBase.getValue());
        if (target instanceof Player player) {
            float armor = player.getArmorValue();
            float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            if (assumeBestArmor.getValue() && player != CLIENT.player) {
                armor = Math.max(armor, 20.0f);
                toughness = Math.max(toughness, 8.0f);
            }
            float armorReduction = Mth.clamp(armor - damage / (2.0f + toughness / 4.0f), armor * 0.2f, 20.0f);
            damage *= 1.0f - armorReduction / 25.0f;
        }
        return Math.max(damage, 0.0f);
    }

    private double getExposure(Vec3 pos, AABB box) {
        double x = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double y = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double z = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

        int count = 0;
        int total = 0;
        for (double ix = 0.0; ix <= 1.0; ix += x) {
            for (double iy = 0.0; iy <= 1.0; iy += y) {
                for (double iz = 0.0; iz <= 1.0; iz += z) {
                    double dx = Mth.lerp(ix, box.minX, box.maxX);
                    double dy = Mth.lerp(iy, box.minY, box.maxY);
                    double dz = Mth.lerp(iz, box.minZ, box.maxZ);
                    Vec3 sample = new Vec3(dx, dy, dz);
                    HitResult hit = CLIENT.level.clip(new ClipContext(sample, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player));
                    if (hit.getType() == HitResult.Type.MISS) {
                        count++;
                    }
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) count / (double) total;
    }

    private boolean hasTotem(Player player) {
        return player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private static class PlacementResult {
        final BlockPos pos;
        final LivingEntity target;
        final double damage;
        final PlaceMode mode;
        final Vec3 point;

        private PlacementResult(BlockPos pos, LivingEntity target, double damage, PlaceMode mode, Vec3 point) {
            this.pos = pos;
            this.target = target;
            this.damage = damage;
            this.mode = mode;
            this.point = point;
        }

        private PlacementResult betterThan(PlacementResult other) {
            if (other == null) return this;
            if (this.mode.ordinal() < other.mode.ordinal()) return this;
            if (this.mode.ordinal() > other.mode.ordinal()) return other;
            return this.damage > other.damage ? this : other;
        }
    }

    private static class FadeEntry {
        final BlockPos pos;
        final long startMs;

        private FadeEntry(BlockPos pos, long startMs) {
            this.pos = pos;
            this.startMs = startMs;
        }
    }

    private record DamageSample(double targetDamage, double selfDamage, Vec3 point) {
    }

    private static class SelfPreserve {
        final boolean force;

        private SelfPreserve(boolean force) {
            this.force = force;
        }

        static SelfPreserve fromSelfDamage(Player player, double selfDamage) {
            boolean lethal = selfDamage >= player.getHealth() + player.getAbsorptionAmount();
            boolean totem = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
            return new SelfPreserve(lethal && !totem);
        }
    }
}
