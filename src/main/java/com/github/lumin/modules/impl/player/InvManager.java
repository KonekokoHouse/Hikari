package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.gui.clickgui.ClickGuiScreen;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.math.MathUtils;
import com.github.lumin.utils.timer.TimerUtils;
import com.github.lumin.utils.player.ChatUtils;
import com.github.lumin.utils.player.InvHelper;
import com.github.lumin.utils.player.MoveUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InvManager extends Module {

    public static final InvManager INSTANCE = new InvManager();

    private InvManager() {
        super("InvManager", Category.PLAYER);
    }

    public enum OffhandItemMode {
        None,
        GoldenApple,
        Projectile,
        FishingRod,
        Block
    }

    public enum BowPriority {
        Crossbow,
        PowerBow,
        PunchBow
    }

    private final DoubleSetting minDelay = doubleSetting("Min Delay", 90.0, 0.0, 500.0, 5.0);
    private final DoubleSetting maxDelay = doubleSetting("Max Delay", 110.0, 0.0, 500.0, 5.0);
    private final EnumSetting<OffhandItemMode> offhandItems = enumSetting("Offhand Items", OffhandItemMode.Projectile);
    private final BoolSetting autoArmor = boolSetting("Auto Armor", true);
    private final BoolSetting inventoryOnly = boolSetting("Inventory Only", true);
    private final BoolSetting switchSword = boolSetting("Switch Sword", true);
    private final IntSetting swordSlot = intSetting("Sword Slot", 1, 1, 9, 1, switchSword::getValue);
    private final BoolSetting switchBlock = boolSetting("Switch Block", true, () -> !offhandItems.is(OffhandItemMode.Block));
    private final IntSetting blockSlot = intSetting("Block Slot", 9, 1, 9, 1, () -> switchBlock.getValue() && !offhandItems.is(OffhandItemMode.Block));
    private final IntSetting maxBlockSize = intSetting("Max Block Size", 256, 64, 512, 64, switchBlock::getValue);
    private final BoolSetting switchPickaxe = boolSetting("Switch Pickaxe", true);
    private final IntSetting pickaxeSlot = intSetting("Pickaxe Slot", 3, 1, 9, 1, switchPickaxe::getValue);
    private final BoolSetting switchAxe = boolSetting("Switch Axe", true);
    private final IntSetting axeSlot = intSetting("Axe Slot", 4, 1, 9, 1, switchAxe::getValue);
    private final BoolSetting switchBow = boolSetting("Switch Bow or Crossbow", true);
    private final IntSetting bowSlot = intSetting("Bow Slot", 5, 1, 9, 1, switchBow::getValue);
    private final EnumSetting<BowPriority> preferBow = enumSetting("Bow Priority", BowPriority.Crossbow, switchBow::getValue);
    private final IntSetting maxArrowSize = intSetting("Max Arrow Size", 256, 64, 512, 64, switchBow::getValue);
    private final BoolSetting switchWaterBucket = boolSetting("Switch Water Bucket", true);
    private final IntSetting waterBucketSlot = intSetting("Water Bucket Slot", 6, 1, 9, 1, switchWaterBucket::getValue);
    private final BoolSetting switchEnderPearl = boolSetting("Switch Ender Pearl", true);
    private final IntSetting enderPearlSlot = intSetting("Ender Pearl Slot", 7, 1, 9, 1, switchEnderPearl::getValue);
    private final BoolSetting switchFireball = boolSetting("Switch Fireball", true);
    private final IntSetting fireballSlot = intSetting("Fireball Slot", 8, 1, 9, 1, switchFireball::getValue);
    private final BoolSetting switchGoldenApple = boolSetting("Switch Golden Apple", true, () -> !offhandItems.is(OffhandItemMode.GoldenApple));
    private final IntSetting goldenAppleSlot = intSetting("Golden Apple Slot", 2, 1, 9, 1, () -> switchGoldenApple.getValue() && !offhandItems.is(OffhandItemMode.GoldenApple));
    private final BoolSetting throwItems = boolSetting("Throw Items", true);
    private final IntSetting waterBucketCount = intSetting("Keep Water Buckets", 1, 0, 5, 1, throwItems::getValue);
    private final IntSetting lavaBucketCount = intSetting("Keep Lava Buckets", 1, 0, 5, 1, throwItems::getValue);
    private final BoolSetting keepProjectile = boolSetting("Keep Eggs & Snowballs", true);
    private final BoolSetting switchProjectile = boolSetting("Switch Eggs & Snowballs", true, () -> keepProjectile.getValue() && !offhandItems.is(OffhandItemMode.Projectile));
    private final IntSetting projectileSlot = intSetting("Eggs & Snowballs Slot", 9, 1, 9, 1, () -> switchProjectile.getValue() && keepProjectile.getValue() && !offhandItems.is(OffhandItemMode.Projectile));
    private final IntSetting maxProjectileSize = intSetting("Max Eggs & Snowballs Size", 64, 16, 256, 16, keepProjectile::getValue);
    private final BoolSetting switchRod = boolSetting("Switch Rod", false, () -> !offhandItems.is(OffhandItemMode.FishingRod));
    private final IntSetting rodSlot = intSetting("Rod Slot", 9, 1, 9, 1, () -> switchRod.getValue() && !offhandItems.is(OffhandItemMode.FishingRod));

    int noMoveTicks = 0;
    private boolean clickOffHand = false;
    private boolean inventoryOpen = false;

    private final TimerUtils timer = new TimerUtils();

    public int getMaxBlockSize() {
        return maxBlockSize.getValue();
    }

    public boolean shouldKeepProjectile() {
        return keepProjectile.getValue();
    }

    public int getMaxProjectileSize() {
        return maxProjectileSize.getValue();
    }

    public int getMaxArrowSize() {
        return maxArrowSize.getValue();
    }

    public int getWaterBucketCount() {
        return waterBucketCount.getValue();
    }

    public int getLavaBucketCount() {
        return lavaBucketCount.getValue();
    }

    public boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (InvHelper.isGodItem(stack)) {
            return true;
        } else if (stack.getDisplayName().getString().contains("点击使用")) {
            return true;
        } else if (stack.is(ItemTags.ARMOR_ENCHANTABLE)) {
            float protection = InvHelper.getProtection(stack);
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable == null) return false;
            if (InvHelper.getCurrentArmorScore(equippable.slot()) >= protection) {
                return false;
            } else {
                float bestArmor = InvHelper.getBestArmorScore(equippable.slot());
                return !(protection < bestArmor);
            }
        } else if (stack.is(ItemTags.SWORDS)) {
            return InvHelper.getBestSword() == stack;
        } else if (stack.is(ItemTags.PICKAXES)) {
            return InvHelper.getBestPickaxe() == stack;
        } else if (stack.getItem() instanceof AxeItem && !InvHelper.isSharpnessAxe(stack)) {
            return InvHelper.getBestAxe() == stack;
        } else if (stack.getItem() instanceof ShovelItem) {
            return InvHelper.getBestShovel() == stack;
        } else if (stack.getItem() instanceof CrossbowItem) {
            return InvHelper.getBestCrossbow() == stack;
        } else if (stack.getItem() instanceof BowItem && InvHelper.isPunchBow(stack)) {
            return InvHelper.getBestPunchBow() == stack;
        } else if (stack.getItem() instanceof BowItem && InvHelper.isPowerBow(stack)) {
            return InvHelper.getBestPowerBow() == stack;
        } else if (stack.getItem() instanceof BowItem && InvHelper.getItemCount(Items.BOW) > 1) {
            return false;
        } else if (stack.getItem() == Items.WATER_BUCKET && InvHelper.getItemCount(Items.WATER_BUCKET) > getWaterBucketCount()) {
            return false;
        } else if (stack.getItem() == Items.LAVA_BUCKET && InvHelper.getItemCount(Items.LAVA_BUCKET) > getLavaBucketCount()) {
            return false;
        } else if (stack.getItem() instanceof FishingRodItem && InvHelper.getItemCount(Items.FISHING_ROD) > 1) {
            return false;
        } else if ((stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG) && !shouldKeepProjectile()) {
            return false;
        } else {
            return !stack.has(DataComponents.CUSTOM_NAME) && InvHelper.isCommonItemUseful(stack);
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Send e) {
        if (e.getPacket() instanceof ServerboundContainerClosePacket) {
            this.inventoryOpen = false;
        }

        if (this.inventoryOpen && !this.inventoryOnly.getValue()) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
                if (MoveUtils.isMoving()) {
                    mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
                }
            } else if (e.getPacket() instanceof ServerboundUseItemOnPacket
                    || e.getPacket() instanceof ServerboundUseItemPacket
                    || e.getPacket() instanceof ServerboundInteractPacket
                    || e.getPacket() instanceof ServerboundPlayerActionPacket) {
                mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
            }
        }
    }

    private boolean checkConfig() {
        List<Pair<BoolSetting, IntSetting>> pairs = new ArrayList<>();
        if (!this.keepProjectile.getValue()) {
            this.switchProjectile.setValue(false);
        }

        pairs.add(Pair.of(this.switchSword, this.swordSlot));
        pairs.add(Pair.of(this.switchPickaxe, this.pickaxeSlot));
        pairs.add(Pair.of(this.switchAxe, this.axeSlot));
        pairs.add(Pair.of(this.switchBow, this.bowSlot));
        pairs.add(Pair.of(this.switchWaterBucket, this.waterBucketSlot));
        pairs.add(Pair.of(this.switchEnderPearl, this.enderPearlSlot));
        pairs.add(Pair.of(this.switchFireball, this.fireballSlot));
        if (!this.offhandItems.is(OffhandItemMode.GoldenApple)) {
            pairs.add(Pair.of(this.switchGoldenApple, this.goldenAppleSlot));
        }

        if (!this.offhandItems.is(OffhandItemMode.Projectile)) {
            pairs.add(Pair.of(this.switchProjectile, this.projectileSlot));
        }

        if (!this.offhandItems.is(OffhandItemMode.FishingRod)) {
            pairs.add(Pair.of(this.switchRod, this.rodSlot));
        }

        if (!this.offhandItems.is(OffhandItemMode.Block)) {
            pairs.add(Pair.of(this.switchBlock, this.blockSlot));
        }

        Set<Integer> usedSlot = new HashSet<>();

        for (Pair<BoolSetting, IntSetting> pair : pairs) {
            if (pair.getKey().getValue()) {
                int targetSlot = (int) (pair.getValue().getValue() - 1.0F);
                if (usedSlot.contains(targetSlot)) {
                    return false;
                }

                usedSlot.add(targetSlot);
            }
        }

        return true;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        if (!(mc.screen instanceof ClickGuiScreen) && !this.checkConfig()) {
            ChatUtils.addChatMessage("Duplicate slot config in Inventory Manager! Please check your config!");
            this.toggle();
            return;
        }

        if (InvHelper.shouldDisableFeatures()) {
            return;
        }

        if (MoveUtils.isMoving()) {
            this.noMoveTicks = 0;
        } else {
            this.noMoveTicks++;
        }

        if (Stealer.INSTANCE.isWorking()
                || Scaffold.INSTANCE.isEnabled()
                || (this.inventoryOnly.getValue() ? !(mc.screen instanceof InventoryScreen) : this.noMoveTicks <= 1)) {
            this.clickOffHand = false;
            return;
        }

        if (mc.screen instanceof AbstractContainerScreen<?> container && container.getMenu().containerId != mc.player.inventoryMenu.containerId) {
            return;
        }

        if (this.autoArmor.getValue()) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            for (int i = 0; i < 4; i++) {
                ItemStack stack = mc.player.getItemBySlot(armorSlots[i]);
                if (stack.is(ItemTags.ARMOR_ENCHANTABLE)) {
                    Equippable equipment = stack.get(DataComponents.EQUIPPABLE);
                    if (equipment == null) return;
                    if (!stack.isEmpty() && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue())) && InvHelper.getBestArmorScore(equipment.slot()) > InvHelper.getProtection(stack)) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 4 + (4 - i), 1, ClickType.THROW, mc.player);
                        this.inventoryOpen = true;
                        timer.reset();
                    }
                }
            }

            for (int ix = 0; ix < 36; ix++) {
                ItemStack stack = mc.player.getInventory().getItem(ix);
                if (!stack.isEmpty() && stack.is(ItemTags.ARMOR_ENCHANTABLE)) {
                    float currentItemScore = InvHelper.getProtection(stack);
                    Equippable equipment = stack.get(DataComponents.EQUIPPABLE);
                    if (equipment == null) return;

                    boolean isBestItem = InvHelper.getBestArmorScore(equipment.slot()) == currentItemScore;
                    boolean isBetterItem = InvHelper.getCurrentArmorScore(equipment.slot()) < currentItemScore;
                    if (isBestItem && isBetterItem && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
                        if (ix < 9) {
                            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, ix + 36, 0, ClickType.QUICK_MOVE, mc.player);
                        } else {
                            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, ix, 0, ClickType.QUICK_MOVE, mc.player);
                        }

                        this.inventoryOpen = true;
                        timer.reset();
                    }
                }
            }
        }

        if (this.clickOffHand && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);
            this.inventoryOpen = true;
            this.clickOffHand = false;
            timer.reset();
        }

        if (this.offhandItems.is(OffhandItemMode.GoldenApple)) {
            ItemStack offHand = mc.player.getOffhandItem();
            int slot = InvHelper.getItemSlot(Items.GOLDEN_APPLE);
            if (slot != -1 && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
                if (offHand.getItem() == Items.GOLDEN_APPLE) {
                    ItemStack goldenAppleStack = mc.player.getInventory().getItem(slot);
                    if (offHand.getCount() + goldenAppleStack.getCount() <= 64) {
                        if (slot < 9) {
                            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, 0, ClickType.PICKUP, mc.player);
                        } else {
                            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
                        }

                        this.inventoryOpen = true;
                        this.clickOffHand = true;
                        timer.reset();
                    }
                } else {
                    this.swapOffHand(slot);
                }
            }
        } else if (this.offhandItems.is(OffhandItemMode.Projectile)) {
            ItemStack offHand = mc.player.getOffhandItem();
            ItemStack bestProjectile = InvHelper.getBestProjectile();
            if (bestProjectile != null) {
                int slot = InvHelper.getItemStackSlot(bestProjectile);
                boolean shouldSwap = false;
                if (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL) {
                    shouldSwap = true;
                } else if (offHand.getCount() < bestProjectile.getCount()) {
                    shouldSwap = true;
                }

                if (shouldSwap && slot != -1 && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
                    this.swapOffHand(slot);
                }
            }
        } else if (this.offhandItems.is(OffhandItemMode.FishingRod)) {
            ItemStack offHand = mc.player.getOffhandItem();
            int slotx = InvHelper.getItemSlot(Items.FISHING_ROD);
            if (slotx != -1 && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue())) && offHand.getItem() != Items.FISHING_ROD) {
                this.swapOffHand(slotx);
            }
        } else if (this.offhandItems.is(OffhandItemMode.Block)) {
            ItemStack offHand = mc.player.getOffhandItem();
            ItemStack bestBlock = InvHelper.getBestBlock();
            if (bestBlock != null) {
                int slotx = InvHelper.getItemStackSlot(bestBlock);
                boolean shouldSwapx = false;
                if (InvHelper.isValidStack(offHand)) {
                    if (offHand.getCount() < bestBlock.getCount()) {
                        shouldSwapx = true;
                    }
                } else {
                    shouldSwapx = true;
                }

                if (shouldSwapx && slotx != -1 && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
                    this.swapOffHand(slotx);
                }
            }
        }

        if (this.switchGoldenApple.getValue() && !this.offhandItems.is(OffhandItemMode.GoldenApple)) {
            this.swapItem((int) (this.goldenAppleSlot.getValue() - 1.0F), Items.GOLDEN_APPLE);
        }

        if (this.switchBlock.getValue()) {
            int blockSlot = (int) (this.blockSlot.getValue() - 1.0F);
            ItemStack currentBlock = mc.player.getInventory().getItem(blockSlot);
            ItemStack bestBlock = InvHelper.getBestBlock();
            if (bestBlock != null
                    && (bestBlock.getCount() > currentBlock.getCount() || !InvHelper.isValidStack(currentBlock))
                    && !this.offhandItems.is(OffhandItemMode.Block)) {
                this.swapItem(blockSlot, bestBlock);
            }

            if ((float) InvHelper.getBlockCountInInventory() > this.maxBlockSize.getValue()) {
                ItemStack worstBlock = InvHelper.getWorstBlock();
                this.throwItem(worstBlock);
            }
        }

        if (this.switchSword.getValue()) {
            int slotxx = (int) (this.swordSlot.getValue() - 1.0F);
            ItemStack currentSword = mc.player.getInventory().getItem(slotxx);
            ItemStack bestSword = InvHelper.getBestSword();
            ItemStack bestShapeAxe = InvHelper.getBestShapeAxe();
            if (InvHelper.getAxeDamage(bestShapeAxe) > InvHelper.getSwordDamage(bestSword)) {
                bestSword = bestShapeAxe;
            }

            if (bestSword != null) {
                float currentDamage = currentSword.is(ItemTags.SWORDS)
                        ? InvHelper.getSwordDamage(currentSword)
                        : InvHelper.getAxeDamage(currentSword);
                float bestWeaponDamage = currentSword.is(ItemTags.SWORDS)
                        ? InvHelper.getSwordDamage(bestSword)
                        : InvHelper.getAxeDamage(bestSword);
                if (bestWeaponDamage > currentDamage) {
                    this.swapItem(slotxx, bestSword);
                }
            }
        }

        if (this.switchPickaxe.getValue()) {
            int slotxxx = (int) (this.pickaxeSlot.getValue() - 1.0F);
            ItemStack bestPickaxe = InvHelper.getBestPickaxe();
            ItemStack currentPickaxe = mc.player.getInventory().getItem(slotxxx);
            if (bestPickaxe != null && bestPickaxe.is(ItemTags.PICKAXES) && (InvHelper.getToolScore(bestPickaxe) > InvHelper.getToolScore(currentPickaxe) || !currentPickaxe.is(ItemTags.PICKAXES))) {
                this.swapItem(slotxxx, bestPickaxe);
            }
        }

        if (this.switchAxe.getValue()) {
            int slotxxx = (int) (this.axeSlot.getValue() - 1.0F);
            ItemStack bestAxe = InvHelper.getBestAxe();
            ItemStack currentAxe = mc.player.getInventory().getItem(slotxxx);
            if (bestAxe != null && bestAxe.is(ItemTags.AXES) && (InvHelper.getToolScore(bestAxe) > InvHelper.getToolScore(currentAxe) || !currentAxe.is(ItemTags.AXES))) {
                this.swapItem(slotxxx, bestAxe);
            }
        }

        if (this.switchRod.getValue() && !this.offhandItems.is(OffhandItemMode.FishingRod)) {
            int slotxxx = (int) (this.rodSlot.getValue() - 1.0F);
            ItemStack bestRod = InvHelper.getFishingRod();
            ItemStack currentRod = mc.player.getInventory().getItem(slotxxx);
            if (!(currentRod.getItem() instanceof FishingRodItem)) {
                this.swapItem(slotxxx, bestRod);
            }
        }

        if (this.switchBow.getValue()) {
            int slotxxx = (int) (this.bowSlot.getValue() - 1.0F);
            ItemStack currentBow = mc.player.getInventory().getItem(slotxxx);
            ItemStack bestBow;
            float bestBowScore;
            float currentBowScore;
            if (this.preferBow.is(BowPriority.Crossbow)) {
                bestBow = InvHelper.getBestCrossbow();
                bestBowScore = InvHelper.getCrossbowScore(bestBow);
                currentBowScore = InvHelper.getCrossbowScore(currentBow);
            } else if (this.preferBow.is(BowPriority.PowerBow)) {
                bestBow = InvHelper.getBestPowerBow();
                bestBowScore = InvHelper.getPowerBowScore(bestBow);
                currentBowScore = InvHelper.getPowerBowScore(currentBow);
            } else {
                bestBow = InvHelper.getBestPunchBow();
                bestBowScore = InvHelper.getPunchBowScore(bestBow);
                currentBowScore = InvHelper.getPunchBowScore(currentBow);
            }

            if (bestBow == null) {
                bestBow = InvHelper.getBestCrossbow();
                bestBowScore = InvHelper.getCrossbowScore(bestBow);
                currentBowScore = InvHelper.getCrossbowScore(currentBow);
            }

            if (bestBow == null) {
                bestBow = InvHelper.getBestPowerBow();
                bestBowScore = InvHelper.getPowerBowScore(bestBow);
                currentBowScore = InvHelper.getPowerBowScore(currentBow);
            }

            if (bestBow == null) {
                bestBow = InvHelper.getBestPunchBow();
                bestBowScore = InvHelper.getPunchBowScore(bestBow);
                currentBowScore = InvHelper.getPunchBowScore(currentBow);
            }

            if (bestBow != null && bestBowScore > currentBowScore) {
                this.swapItem(slotxxx, bestBow);
            }

            if ((float) InvHelper.getItemCount(Items.ARROW) > this.maxArrowSize.getValue()) {
                ItemStack worstArrow = InvHelper.getWorstArrow();
                this.throwItem(worstArrow);
            }
        }

        if (this.switchEnderPearl.getValue()) {
            this.swapItem((int) (this.enderPearlSlot.getValue() - 1.0F), Items.ENDER_PEARL);
        }

        if (this.switchWaterBucket.getValue()) {
            this.swapItem((int) (this.waterBucketSlot.getValue() - 1.0F), Items.WATER_BUCKET);
        }

        if (this.switchFireball.getValue()) {
            this.swapItem((int) (this.fireballSlot.getValue() - 1.0F), Items.FIRE_CHARGE);
        }

        if (this.keepProjectile.getValue()) {
            if ((float) (InvHelper.getItemCount(Items.EGG) + InvHelper.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getValue()) {
                ItemStack worstProjectile = InvHelper.getWorstProjectile();
                this.throwItem(worstProjectile);
            }

            if (this.switchProjectile.getValue() && !this.offhandItems.is(OffhandItemMode.Projectile)) {
                int projectileSlot = (int) (this.projectileSlot.getValue() - 1.0F);
                if (InvHelper.getItemCount(Items.EGG) > 0) {
                    this.swapItem(projectileSlot, Items.EGG);
                } else if (InvHelper.getItemCount(Items.SNOWBALL) > 0) {
                    this.swapItem(projectileSlot, Items.SNOWBALL);
                }
            }
        }

        if (this.throwItems.getValue()) {
            List<Integer> slots = IntStream.range(0, 36).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);

            for (Integer slotxxxx : slots) {
                ItemStack stack = mc.player.getInventory().getItem(slotxxxx);
                if (!stack.isEmpty() && !this.isItemUseful(stack)) {
                    this.throwItem(stack);
                }
            }
        }
    }

    private void swapOffHand(int slot) {
        if (slot < 9) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, 40, ClickType.SWAP, mc.player);
        } else {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 40, ClickType.SWAP, mc.player);
        }

        this.inventoryOpen = true;
        timer.reset();
    }

    private void throwItem(ItemStack item) {
        if (InvHelper.isItemValid(item) && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
            int itemSlot = InvHelper.getItemStackSlot(item);
            if (itemSlot != -1) {
                if (itemSlot < 9) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, itemSlot + 36, 1, ClickType.THROW, mc.player);
                } else {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, itemSlot, 1, ClickType.THROW, mc.player);
                }

                this.inventoryOpen = true;
                timer.reset();
            }
        }
    }

    private void swapItem(int targetSlot, ItemStack bestItem) {
        ItemStack currentSlot = mc.player.getInventory().getItem(targetSlot);
        if (InvHelper.isItemValid(currentSlot) && bestItem != currentSlot && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
            int bestItemSlot = InvHelper.getItemStackSlot(bestItem);
            if (bestItemSlot != -1) {
                if (bestItemSlot < 9) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot + 36, targetSlot, ClickType.SWAP, mc.player);
                } else {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot, targetSlot, ClickType.SWAP, mc.player);
                }

                this.inventoryOpen = true;
                timer.reset();
            }
        }
    }

    private void swapItem(int targetSlot, Item item) {
        ItemStack currentSlot = mc.player.getInventory().getItem(targetSlot);
        if (InvHelper.isItemValid(currentSlot) && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
            int bestItemSlot = InvHelper.getItemSlot(item);
            if (bestItemSlot != -1) {
                ItemStack bestItemStack = mc.player.getInventory().getItem(bestItemSlot);
                if (currentSlot.getItem() != item || currentSlot.getCount() < bestItemStack.getCount()) {
                    if (bestItemSlot < 9) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot + 36, targetSlot, ClickType.SWAP, mc.player);
                    } else {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot, targetSlot, ClickType.SWAP, mc.player);
                    }

                    this.inventoryOpen = true;
                    timer.reset();
                }
            }
        }
    }

}
