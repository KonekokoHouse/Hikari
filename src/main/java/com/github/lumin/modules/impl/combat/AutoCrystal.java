package com.github.lumin.modules.impl.combat;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.managers.TwoBTwoTRotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.player.MoveFix;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class AutoCrystal extends Module {

    public static final AutoCrystal INSTANCE = new AutoCrystal();
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final Speedmine SPEEDMINE = Speedmine.INSTANCE;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    // =================== ENUMS ===================
    private enum PlaceMode { Visible, Wall, Base }
    private enum RotationEngine { Lumin, TwoBTwoT }

    // =================== SETTINGS ===================
    // Core
    private final BoolSetting placeSetting = boolSetting("Place", true);
    private final BoolSetting breakSetting = boolSetting("Break", true);
    private final IntSetting delay = intSetting("Delay", 2, 0, 20, 1);
    private final IntSetting factor = intSetting("Factor", 1, 1, 10, 1);

    // Range
    private final DoubleSetting placeRange = doubleSetting("PlaceRange", 4.5, 1.0, 6.0, 0.1);
    private final DoubleSetting placeWallRange = doubleSetting("PlaceWallRange", 3.5, 1.0, 6.0, 0.1);
    private final DoubleSetting breakRange = doubleSetting("BreakRange", 4.5, 1.0, 6.0, 0.1);
    private final DoubleSetting breakWallRange = doubleSetting("BreakWallRange", 3.5, 1.0, 6.0, 0.1);
    private final DoubleSetting targetRange = doubleSetting("TargetRange", 12.0, 4.0, 20.0, 0.1);

    // Damage
    private final DoubleSetting minDamage = doubleSetting("MinDamage", 6.0, 0.0, 20.0, 0.1);
    private final DoubleSetting maxSelfDamage = doubleSetting("MaxSelf", 10.0, 0.0, 20.0, 0.1);
    private final DoubleSetting damageRatio = doubleSetting("DamageRatio", 1.5, 0.1, 5.0, 0.1);
    private final DoubleSetting facePlaceHealth = doubleSetting("Health", 8.0, 0.0, 36.0, 0.5);
    private final DoubleSetting armorThreshold = doubleSetting("Armor", 10.0, 0.0, 20.0, 0.5);
    private final BoolSetting facePlace = boolSetting("FacePlace", true);
    private final BoolSetting assumeBestArmor = boolSetting("AssumeBestArmor", false);
    private final BoolSetting assumeInvincibility = boolSetting("AssumeInvincible", false);
    private final DoubleSetting timeout = doubleSetting("Timeout", 0.5, 0.1, 5.0, 0.1);

    // Safety
    private final BoolSetting balance = boolSetting("Balance", true);
    private final BoolSetting antiSuicide = boolSetting("AntiSuicide", true);
    private final BoolSetting forceSuicide = boolSetting("ForceSuicide", false);
    private final BoolSetting self = boolSetting("Self", false);
    private final BoolSetting ignoreNakeds = boolSetting("IgnoreNakeds", false);
    private final BoolSetting antiFriendKill = boolSetting("AntiFriend", false);

    // Placement
    private final BoolSetting basePlace = boolSetting("BasePlace", true);
    private final BoolSetting airPlace = boolSetting("Air", false);
    private final BoolSetting strictDirection = boolSetting("StrictDir", false);
    private final BoolSetting smartTrace = boolSetting("SmartTrace", true);
    private final BoolSetting oneDotTwelve = boolSetting("1.12", false);
    private final BoolSetting hbFix = boolSetting("HBFix", true);

    // Rotation
    private final BoolSetting rotate = boolSetting("Rotate", true);
    private final BoolSetting movementSync = boolSetting("MovementSync", true, rotate::getValue);
    private final DoubleSetting rotateSpeed = doubleSetting("RotateSpeed", 10.0, 1.0, 20.0, 0.5, rotate::getValue);
    private final DoubleSetting yawStep = doubleSetting("YawStep", 0.0, 0.0, 180.0, 1.0, rotate::getValue);
    private final EnumSetting<RotationEngine> rotationEngine = enumSetting("RotationEngine", RotationEngine.Lumin, rotate::getValue);

    // Swap
    private final EnumSetting<AutoSwapMode> autoSwap = enumSetting("AutoSwap", AutoSwapMode.Normal);
    private final BoolSetting swapBack = boolSetting("SwapBack", true);
    private final DoubleSetting swapDelay = doubleSetting("SwapDelay", 0.0, 0.0, 5.0, 0.1);
    private final DoubleSetting swapPenalty = doubleSetting("SwapPenalty", 0.0, 0.0, 5.0, 0.1);

    // Inhibit
    private final BoolSetting inhibit = boolSetting("Inhibit", true);
    private final IntSetting inhibitAttackWindowMs = intSetting("InhibitAttackWindow", 250, 0, 1000, 10);

    // Instant
    private final BoolSetting instant = boolSetting("Instant", true);

    // Speedmine
    private final BoolSetting mineIgnore = boolSetting("MineIgnore", false);

    // Render
    private final BoolSetting renderSetting = boolSetting("Render", true);
    private final BoolSetting draw = boolSetting("Draw", true);
    private final BoolSetting fill = boolSetting("Fill", true);
    private final BoolSetting outline = boolSetting("Outline", true);
    private final DoubleSetting lineWidth = doubleSetting("LineWidth", 2.0, 0.5, 5.0, 0.5);
    private final BoolSetting fade = boolSetting("Fade", false);
    private final BoolSetting multiFade = boolSetting("MultiFade", false);
    private final IntSetting fadeTime = intSetting("FadeTime", 500, 100, 2000, 50);
    private final ColorSetting fillColor = colorSetting("FillColor", new Color(255, 80, 120, 80));
    private final ColorSetting outlineColor = colorSetting("OutlineColor", new Color(255, 80, 120, 160));

    // =================== STATE ===================
    private DamageData<EndCrystal> attackCrystal;
    private DamageData<BlockPos> placeCrystal;

    private BlockPos renderPos;
    private float renderDamage;
    private LivingEntity currentTarget;
    private int lastSwapSlot = -1;
    private int lastBreakId = -1;
    private Vec3 rotationTarget;

    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();
    private final TimerUtils swapPenaltyTimer = new TimerUtils();
    private final TimerUtils resyncTimer = new TimerUtils();
    private final TimerUtils timeoutTimer = new TimerUtils();

    private final Set<Integer> recentAttacks = new HashSet<>();
    private final Map<Integer, Long> attackTimes = new HashMap<>();
    private final Map<Integer, Long> spawnTimes = new HashMap<>();
    private final List<FadeEntry> fadeEntries = new ArrayList<>();
    private final Deque<BlockPos> recentSpawns = new ArrayDeque<>();
    private final Map<BlockPos, Long> placeTimes = new HashMap<>();

    private CompletableFuture<DamageData<BlockPos>> calcFuture;

    // =================== AUTO SWAP MODE ===================
    private enum AutoSwapMode { BlockTarget, Normal, Silent }

    // =================== CONSTRUCTOR ===================
    private AutoCrystal() {
        super("AutoCrystal", Category.COMBAT);
    }

    // =================== LIFECYCLE ===================
    @Override
    protected void onDisable() {
        attackCrystal = null;
        placeCrystal = null;
        currentTarget = null;
        renderPos = null;
        renderDamage = 0;
        rotationTarget = null;
        lastSwapSlot = -1;
        lastBreakId = -1;
        recentAttacks.clear();
        attackTimes.clear();
        spawnTimes.clear();
        placeTimes.clear();
        fadeEntries.clear();
        recentSpawns.clear();
        cleanupSwap();
        cancelFuture();
    }

    // =================== TICK ===================
    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        cleanupAttacks();
        if (shouldPause()) return;

        updateTarget();
        if (currentTarget == null) {
            renderPos = null;
            renderDamage = 0.0f;
            return;
        }

        List<BlockPos> positions = getPlacePositions();
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            entities.add(entity);
        }

        // calculate place
        if (placeSetting.getValue()) {
            placeCrystal = calculatePlace(positions);
        } else {
            placeCrystal = null;
        }

        // calculate attack
        if (breakSetting.getValue()) {
            attackCrystal = calculateAttack(entities);
        } else {
            attackCrystal = null;
        }

        updateRenderState();

        // if no attack target, check if there is crystal at place pos
        if (attackCrystal == null && placeCrystal != null) {
            EndCrystal crystal = getCrystalAt(placeCrystal.damageData());
            if (crystal != null) {
                Vec3 pos = crystal.position();
                double targetDamage = calculateDamage(CLIENT.level, pos, currentTarget);
                double selfDamage = calculateDamage(CLIENT.level, pos, CLIENT.player);
                if (isDamageOk(targetDamage, selfDamage)) {
                    attackCrystal = new DamageData<>(crystal, currentTarget, targetDamage, placeCrystal.damageData());
                }
            }
        }

        // rotate
        if (rotate.getValue()) {
            rotationTarget = null;
            if (attackCrystal != null) {
                rotationTarget = attackCrystal.damageData().position();
            } else if (placeCrystal != null) {
                rotationTarget = Vec3.atCenterOf(placeCrystal.damageData()).add(0, 0.5, 0);
            }
            if (rotationTarget != null) {
                rotateTo(rotationTarget);
            }
        }

        // attack
        if (attackCrystal != null && breakTimer.passedMillise(delay.getValue() * 50L)) {
            if (!isInhibited(attackCrystal.damageData())) {
                InteractionHand hand = getCrystalHand();
                attackExecute(attackCrystal.damageData(), hand);
                breakTimer.reset();
            }
        }

        // place
        if (placeCrystal != null && placeTimer.passedMillise(delay.getValue() * 50L)) {
            if (canHoldCrystal() && shouldPlaceBreakLinked()) {
                placeExecute(placeCrystal.damageData());
                placeTimer.reset();
            }
        }
    }

    // =================== ATTACK ===================
    private DamageData<EndCrystal> calculateAttack(List<Entity> entities) {
        DamageData<EndCrystal> best = null;
        for (Entity entity : entities) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!isCrystalValid(crystal)) continue;
            Vec3 pos = crystal.position();
            if (!isInAttackRange(pos)) continue;
            double targetDamage = calculateDamage(CLIENT.level, pos, currentTarget);
            double selfDamage = calculateDamage(CLIENT.level, pos, CLIENT.player);
            if (!isDamageOk(targetDamage, selfDamage)) continue;
            if (best == null || targetDamage > best.damage()) {
                best = new DamageData<>(crystal, currentTarget, targetDamage, crystal.blockPosition().below());
            }
        }
        return best;
    }

    private void attackExecute(EndCrystal crystal, InteractionHand hand) {
        if (attackCheckPre(hand)) return;
        if (weaknessSwap()) return;
        CLIENT.gameMode.attack(CLIENT.player, crystal);
        CLIENT.player.swing(hand != null ? hand : InteractionHand.MAIN_HAND);
        markAttacked(crystal.getId());
        if (crystal instanceof com.github.lumin.ducks.EndCrystalAccess access) {
            access.lumin$setMioAttacked(true);
        }
        int repeats = Math.max(1, factor.getValue());
        for (int i = 1; i < repeats; i++) {
            CLIENT.gameMode.attack(CLIENT.player, crystal);
        }
    }

    // =================== PLACE ===================
    private DamageData<BlockPos> calculatePlace(List<BlockPos> positions) {
        DamageData<BlockPos> best = null;
        for (BlockPos pos : positions) {
            if (!canPlace(pos)) continue;
            Vec3 crystalPos = crystalDamageVec(pos);
            if (placeRangeCheck(pos)) continue;
            double targetDamage = calculateDamage(CLIENT.level, crystalPos, currentTarget);
            double selfDamage = calculateDamage(CLIENT.level, crystalPos, CLIENT.player);
            if (!isDamageOk(targetDamage, selfDamage)) continue;
            PlaceMode mode = isVisible(crystalPos) ? PlaceMode.Visible : PlaceMode.Wall;
            if (best == null || targetDamage > best.damage()) {
                best = new DamageData<>(pos, currentTarget, targetDamage, pos);
            }
        }
        return best;
    }

    private void updateRenderState() {
        if (placeCrystal != null) {
            renderPos = placeCrystal.damageData();
            renderDamage = (float) placeCrystal.damage();
            return;
        }
        renderPos = null;
        renderDamage = 0.0f;
    }

    private void placeExecute(BlockPos pos) {
        if (checkCanUseCrystal()) return;
        Direction side = getPlaceDirection(pos);
        BlockHitResult result = new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false);
        InteractionHand hand = getCrystalHand();
        boolean swapped = false;
        if (hand == null) {
            swapped = prepareSwap();
            if (!swapped) return;
            hand = getHand();
        }
        CLIENT.gameMode.useItemOn(CLIENT.player, hand, result);
        CLIENT.player.swing(hand);
        placeTimes.put(pos, System.currentTimeMillis());
        if (swapped && swapBack.getValue()) cleanupSwap();
    }

    // =================== CRYSTAL VALIDITY ===================
    private boolean isCrystalValid(EndCrystal crystal) {
        if (!crystal.isAlive() || crystal.isRemoved()) return false;
        if (crystal.tickCount < 1) return false;
        if (recentAttacks.contains(crystal.getId())) return false;
        if (inhibit.getValue()) {
            Long attackedAt = attackTimes.get(crystal.getId());
            if (attackedAt != null && System.currentTimeMillis() - attackedAt < inhibitAttackWindowMs.getValue()) return false;
        }
        return true;
    }

    private boolean isInhibited(EndCrystal crystal) {
        if (!inhibit.getValue()) return false;
        Long attackedAt = attackTimes.get(crystal.getId());
        return attackedAt != null && System.currentTimeMillis() - attackedAt < inhibitAttackWindowMs.getValue();
    }

    // =================== PLACEMENT CHECKS ===================
    private boolean canPlace(BlockPos pos) {
        if (CLIENT.level == null) return false;
        BlockState base = CLIENT.level.getBlockState(pos);
        if (!isBaseBlock(pos)) return false;
        BlockPos up = pos.above();
        BlockState upState = CLIENT.level.getBlockState(up);
        if (!upState.isAir() && !upState.canBeReplaced() && !upState.is(Blocks.FIRE)) return false;
        if (!oneDotTwelve.getValue() && !CLIENT.level.getBlockState(up.above()).canBeReplaced()) return false;
        AABB box = new AABB(up.getX(), up.getY(), up.getZ(), up.getX() + 1.0, up.getY() + (oneDotTwelve.getValue() ? 2.0 : 1.0), up.getZ() + 1.0);
        if (hasBlockingEntity(box)) return false;
        if (strictDirection.getValue() && getPlaceDirection(pos) == null) return false;
        return true;
    }

    private boolean hasBlockingEntity(AABB box) {
        List<Entity> entities = CLIENT.level.getEntities((Entity) null, box, entity -> !(entity instanceof EndCrystal));
        if (entities.isEmpty()) {
            return false;
        }
        if (!hbFix.getValue()) {
            return true;
        }
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity) continue;
            if (entity == CLIENT.player) continue;
            if (entity == currentTarget) continue;
            return true;
        }
        return false;
    }

    private boolean isBaseBlock(BlockPos pos) {
        BlockState state = CLIENT.level.getBlockState(pos);
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) return true;
        if (SPEEDMINE.isEnabled() && SPEEDMINE.isRebreakActive()) {
            BlockPos bp = SPEEDMINE.getBreakPos();
            if (bp != null && bp.equals(pos)) return true;
            BlockPos ap = SPEEDMINE.getAssistPos();
            if (ap != null && ap.equals(pos) && SPEEDMINE.isAssistActive()) return true;
        }
        return false;
    }

    private Direction getPlaceDirection(BlockPos pos) {
        if (!strictDirection.getValue()) return Direction.UP;
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            if (!CLIENT.level.getBlockState(pos.relative(d)).isAir()) return d.getOpposite();
        }
        return Direction.UP;
    }

    // =================== RANGE / VISIBILITY ===================
    private boolean isInAttackRange(Vec3 pos) {
        boolean visible = isVisible(pos);
        double max = visible ? breakRange.getValue() : breakWallRange.getValue();
        return CLIENT.player.getEyePosition().distanceTo(pos) <= max;
    }

    private boolean isVisible(Vec3 pos) {
        Vec3 eyes = CLIENT.player.getEyePosition();
        BlockHitResult hit = CLIENT.level.clip(new ClipContext(eyes, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(pos) < 0.3;
    }

    // =================== DAMAGE ===================
    private boolean isDamageOk(double targetDamage, double selfDamage) {
        if (selfDamage > maxSelfDamage.getValue() && !forceSuicide.getValue()) return false;
        if (balance.getValue() && targetDamage <= selfDamage && !forceSuicide.getValue()) return false;
        if (targetDamage > 0 && selfDamage > 0 && targetDamage / selfDamage < damageRatio.getValue()) return false;
        if (antiSuicide.getValue() && !forceSuicide.getValue()) {
            float health = CLIENT.player.getHealth() + CLIENT.player.getAbsorptionAmount();
            if (selfDamage >= health && !hasTotem(CLIENT.player)) return false;
        }
        if (!isValidDamage(targetDamage)) return false;
        return true;
    }

    private boolean isValidDamage(double damage) {
        if (facePlace.getValue() && currentTarget != null) {
            float health = currentTarget.getHealth() + currentTarget.getAbsorptionAmount();
            if (health <= facePlaceHealth.getValue()) return true;
            if (currentTarget instanceof Player player && !assumeBestArmor.getValue()) {
                if (player.getArmorValue() <= armorThreshold.getValue()) return true;
            }
        }
        if (currentTarget != null) {
            float health = currentTarget.getHealth() + currentTarget.getAbsorptionAmount();
            if (damage >= health) return true;
        }
        return damage >= minDamage.getValue();
    }

    private double calculateDamage(Level level, Vec3 pos, LivingEntity target) {
        if (level == null || target == null) return 0;
        Vec3 targetPos = target.position();
        double distance = targetPos.distanceTo(pos);
        AABB box = target.getBoundingBox();
        double exposure = getExposure(pos, box);
        double impact = (1.0 - distance / 12.0) * exposure;
        if (impact <= 0) return 0;
        float damage = (float) ((impact * impact + impact) * 7.0 * 12.0 + 1.0);
        if (target instanceof Player player) {
            float armor = player.getArmorValue();
            float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            if (assumeBestArmor.getValue() && player != CLIENT.player) {
                armor = Math.max(armor, 20);
                toughness = Math.max(toughness, 8);
            }
            float armorReduction = Mth.clamp(armor - damage / (2.0f + toughness / 4.0f), armor * 0.2f, 20.0f);
            damage *= 1.0f - armorReduction / 25.0f;
        }
        return Math.max(damage, 0);
    }

    private double getExposure(Vec3 pos, AABB box) {
        double x = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double y = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double z = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        int count = 0, total = 0;
        for (double ix = 0; ix <= 1; ix += x) {
            for (double iy = 0; iy <= 1; iy += y) {
                for (double iz = 0; iz <= 1; iz += z) {
                    Vec3 sample = new Vec3(Mth.lerp(ix, box.minX, box.maxX), Mth.lerp(iy, box.minY, box.maxY), Mth.lerp(iz, box.minZ, box.maxZ));
                    HitResult hit = CLIENT.level.clip(new ClipContext(sample, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player));
                    if (hit.getType() == HitResult.Type.MISS) count++;
                    total++;
                }
            }
        }
        return total == 0 ? 0 : (double) count / total;
    }

    // =================== TARGET ===================
    private void updateTarget() {
        if (currentTarget != null && isInvalidTarget(currentTarget)) currentTarget = null;
        if (currentTarget == null) currentTarget = findTarget();
        if (currentTarget instanceof com.github.lumin.ducks.PlayerHurtAccess hurtAccess) {
            if (timeoutTimer.passedSecond(timeout.getValue())) hurtAccess.lumin$setHurt(false);
        }
    }

    private LivingEntity findTarget() {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (isInvalidTarget(living)) continue;
            targets.add(living);
        }
        if (self.getValue() && !targets.contains(CLIENT.player)) targets.add(CLIENT.player);
        if (targets.isEmpty()) return null;
        targets.sort(Comparator.comparingDouble(o -> o.distanceTo(CLIENT.player)));
        return targets.getFirst();
    }

    private boolean isInvalidTarget(LivingEntity entity) {
        if (!(entity instanceof Player)) return true;
        if (entity == CLIENT.player && !(forceSuicide.getValue() || self.getValue())) return true;
        if (!entity.isAlive() || entity.isDeadOrDying()) return true;
        if (entity.isSpectator()) return true;
        if (entity.distanceTo(CLIENT.player) > targetRange.getValue()) return true;
        if (antiFriendKill.getValue() && isFriendLike(entity)) return true;
        if (ignoreNakeds.getValue() && entity instanceof Player player && player.getArmorValue() <= 0) return true;
        return false;
    }

    private boolean isFriendLike(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        Player selfPlayer = CLIENT.player;
        if (selfPlayer == null) return false;
        if (player.getTeam() != null && selfPlayer.getTeam() != null) return player.getTeam().isAlliedTo(selfPlayer.getTeam());
        return false;
    }

    // =================== SWAP ===================
    private boolean prepareSwap() {
        if (autoSwap.getValue() == AutoSwapMode.BlockTarget) return CLIENT.player.getOffhandItem().is(Items.END_CRYSTAL) || CLIENT.player.getMainHandItem().is(Items.END_CRYSTAL);
        FindItemResult result = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (result.slot() == -1) return false;
        if (autoSwap.getValue() == AutoSwapMode.Silent) {
            if (!InvUtils.invSwap(result.slot())) return false;
        } else {
            lastSwapSlot = CLIENT.player.getInventory().getSelectedSlot();
            InvUtils.swap(result.slot(), false);
        }
        return true;
    }

    private void cleanupSwap() {
        if (autoSwap.getValue() == AutoSwapMode.Silent) {
            InvUtils.invSwapBack();
        } else if (lastSwapSlot != -1) {
            InvUtils.swap(lastSwapSlot, false);
            lastSwapSlot = -1;
        }
    }

    private InteractionHand getHand() {
        return CLIENT.player.getOffhandItem().is(Items.END_CRYSTAL) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    // =================== ROTATION ===================
    private void rotateTo(Vec3 pos) {
        Vector2f rotations = RotationUtils.calculate(pos);
        MovementFix fix = movementSync.getValue() ? MovementFix.ON : MovementFix.OFF;
        if (rotationEngine.getValue() == RotationEngine.TwoBTwoT) {
            TwoBTwoTRotationManager.INSTANCE.setRotations(rotations, rotateSpeed.getValue(), fix, Priority.High);
        } else {
            RotationManager.INSTANCE.setRotations(rotations, rotateSpeed.getValue(), fix, Priority.High);
        }
    }

    // =================== UTILITY ===================
    private boolean shouldPause() {
        if (CLIENT.player.isSleeping()) return true;
        if (CLIENT.player.isUsingItem()) return true;
        if (CLIENT.gameMode != null && CLIENT.gameMode.isDestroying() && mineIgnore.getValue() && !isMineReady()) return true;
        return false;
    }

    private boolean isMineReady() {
        return SPEEDMINE.isEnabled() && SPEEDMINE.isRebreakActive() && SPEEDMINE.recentMine(100);
    }

    private boolean shouldPlaceBreakLinked() {
        return true;
    }

    private boolean weaknessSwap() {
        ItemStack main = CLIENT.player.getMainHandItem();
        if (!CLIENT.player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) return false;
        if (main.is(ItemTags.SWORDS) || main.is(ItemTags.AXES)) return false;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = CLIENT.player.getInventory().getItem(i);
            if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) { slot = i; break; }
        }
        if (slot == -1) return true;
        InvUtils.swap(slot, true);
        return false;
    }

    private void markAttacked(int id) {
        recentAttacks.add(id);
        attackTimes.put(id, System.currentTimeMillis());
        lastBreakId = id;
        resyncTimer.reset();
        swapPenaltyTimer.reset();
    }

    private void cleanupAttacks() {
        long now = System.currentTimeMillis();
        if (recentAttacks.size() > 200) recentAttacks.clear();
        attackTimes.entrySet().removeIf(e -> now - e.getValue() > 2000);
        spawnTimes.entrySet().removeIf(e -> now - e.getValue() > 5000);
        placeTimes.entrySet().removeIf(e -> now - e.getValue() > 5000);
    }

    private void cancelFuture() {
        if (calcFuture != null && !calcFuture.isDone() && !calcFuture.isCancelled()) calcFuture.cancel(true);
    }

    private boolean hasTotem(Player player) {
        return player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || player.getMainHandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private boolean attackCheckPre(InteractionHand hand) {
        if (!swapPenaltyTimer.passedMillise((long) (swapPenalty.getValue() * 25.0f))) return true;
        return hand == InteractionHand.MAIN_HAND && checkCanUseCrystal();
    }

    private boolean checkCanUseCrystal() {
        return CLIENT.gameMode != null && CLIENT.gameMode.isDestroying() && !mineIgnore.getValue();
    }

    private boolean canHoldCrystal() {
        return getCrystalHand() != null || autoSwap.getValue() != AutoSwapMode.BlockTarget && findCrystalSlot() != -1;
    }

    private int findCrystalSlot() {
        FindItemResult result = InvUtils.findInHotbar(Items.END_CRYSTAL);
        return result.slot();
    }

    private InteractionHand getCrystalHand() {
        if (CLIENT.player.getOffhandItem().is(Items.END_CRYSTAL)) return InteractionHand.OFF_HAND;
        if (CLIENT.player.getMainHandItem().is(Items.END_CRYSTAL)) return InteractionHand.MAIN_HAND;
        return null;
    }

    private Vec3 crystalDamageVec(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 1.0, 0.5);
    }

    private boolean placeRangeCheck(BlockPos pos) {
        Vec3 player = CLIENT.player.getEyePosition();
        double dist = player.distanceToSqr(crystalDamageVec(pos));
        double placeSq = placeRange.getValue() * placeRange.getValue();
        if (dist > placeSq) return true;
        Vec3 raytrace = Vec3.atLowerCornerOf(pos).add(0.5, 2.7, 0.5);
        BlockHitResult result = CLIENT.level.clip(new ClipContext(player, raytrace, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player));
        double maxDist = breakRange.getValue() * breakRange.getValue();
        if (result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos)) {
            maxDist = breakWallRange.getValue() * breakWallRange.getValue();
            if (!smartTrace.getValue() || dist > placeWallRange.getValue() * placeWallRange.getValue()) {
                return true;
            }
        }
        return dist > maxDist;
    }

    private EndCrystal getCrystalAt(BlockPos pos) {
        AABB box = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        List<Entity> entities = CLIENT.level.getEntities((Entity) null, box, e -> e instanceof EndCrystal);
        return entities.isEmpty() ? null : (EndCrystal) entities.getFirst();
    }

    private List<BlockPos> getPlacePositions() {
        if (currentTarget == null) return Collections.emptyList();
        int r = (int) Math.ceil(placeRange.getValue());
        BlockPos center = currentTarget.blockPosition();
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            positions.add(pos.immutable());
        }
        return positions;
    }

    // =================== RENDER ===================
    @SubscribeEvent
    public void onRender(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck() || !renderSetting.getValue() || !draw.getValue()) return;
        if (fade.getValue()) {
            renderFade(event);
            return;
        }
        if (renderPos == null) return;
        drawBox(event, renderPos, 1.0f);
    }

    private void addFadeEntry(BlockPos pos) {
        if (pos == null || !fade.getValue()) return;
        if (!multiFade.getValue()) fadeEntries.clear();
        fadeEntries.add(new FadeEntry(pos, System.currentTimeMillis()));
    }

    private void renderFade(RenderLevelStageEvent.AfterEntities event) {
        Iterator<FadeEntry> it = fadeEntries.iterator();
        while (it.hasNext()) {
            FadeEntry entry = it.next();
            float progress = (System.currentTimeMillis() - entry.startMs) / (float) Math.max(1, fadeTime.getValue());
            if (progress >= 1) { it.remove(); continue; }
            drawBox(event, entry.pos, 1 - progress);
        }
    }

    private void drawBox(RenderLevelStageEvent.AfterEntities event, BlockPos pos, float alpha) {
        AABB box = new AABB(pos);
        Color fc = applyAlpha(fillColor.getValue(), alpha);
        Color oc = applyAlpha(outlineColor.getValue(), alpha);
        if (fill.getValue() && outline.getValue()) {
            Render3DUtils.drawFullBox(event.getPoseStack(), box, fc, oc, lineWidth.getValue().floatValue());
        } else if (fill.getValue()) {
            Render3DUtils.drawFilledBox(event.getPoseStack(), box, fc);
        } else if (outline.getValue()) {
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, oc.getRGB(), lineWidth.getValue().floatValue());
        }
    }

    private Color applyAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int) (color.getAlpha() * alpha))));
    }

    // =================== PACKET ===================
    @SubscribeEvent
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck() || !breakSetting.getValue()) return;
        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundAddEntityPacket addEntity) {
            if (addEntity.getType() != net.minecraft.world.entity.EntityType.END_CRYSTAL) return;
            Vec3 pos = new Vec3(addEntity.getX(), addEntity.getY(), addEntity.getZ());
            spawnTimes.put(addEntity.getId(), System.currentTimeMillis());
            if (!isInAttackRange(pos) || currentTarget == null) return;
            if (!instant.getValue()) return;
            double targetDamage = calculateDamage(CLIENT.level, pos, currentTarget);
            double selfDamage = calculateDamage(CLIENT.level, pos, CLIENT.player);
            if (!isDamageOk(targetDamage, selfDamage)) return;
            if (rotate.getValue()) rotateTo(pos);
            EndCrystal crystal = CLIENT.level.getEntity(addEntity.getId()) instanceof EndCrystal ec ? ec : null;
            if (crystal != null) {
                attackExecute(crystal, getCrystalHand());
                breakTimer.reset();
            }
        }

        if (packet instanceof ClientboundEntityEventPacket eventPacket) {
            if (currentTarget != null && eventPacket instanceof com.github.lumin.ducks.EntityEventPacketAccess access) {
                if (access.lumin$getEntityId() == currentTarget.getId() && access.lumin$getEventId() == 3) {
                    if (currentTarget instanceof com.github.lumin.ducks.PlayerHurtAccess hurtAccess) hurtAccess.lumin$setHurt(true);
                    timeoutTimer.reset();
                }
            }
        }
    }

    public boolean shouldInhibitRender(EndCrystal crystal) {
        if (!isEnabled() || !inhibit.getValue()) return false;
        if (!(crystal instanceof com.github.lumin.ducks.EndCrystalAccess access)) return false;
        if (!access.lumin$isMioAttacked()) return false;
        if (crystal.tickCount >= 10) return false;
        long spawn = access.lumin$getSpawnTime();
        if (spawn == 0) {
            Long stored = spawnTimes.get(crystal.getId());
            spawn = stored != null ? stored : 0;
        }
        return spawn != 0 && System.currentTimeMillis() - spawn > 50;
    }

    // =================== RECORDS ===================
    private record DamageData<T>(T damageData, LivingEntity target, double damage, BlockPos blockPos) {}
    private record FadeEntry(BlockPos pos, long startMs) {}
}
