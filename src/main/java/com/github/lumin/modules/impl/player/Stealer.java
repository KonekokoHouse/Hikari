package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.math.MathUtils;
import com.github.lumin.utils.player.InvHelper;
import com.github.lumin.utils.timer.TimerUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Stealer extends Module {

    public static final Stealer INSTANCE = new Stealer();

    private Stealer() {
        super("Stealer", Category.PLAYER);
    }

    private final BoolSetting pickEnderChest = boolSetting("EnderChest", false);
    private final IntSetting minDelay = intSetting("MinDelay", 90, 0, 500, 5);
    private final IntSetting maxDelay = intSetting("MaxDelay", 110, 0, 500, 5);

    private Screen lastTickScreen;
    private final TimerUtils timer = new TimerUtils();

    public boolean isWorking() {
        return !timer.delay(3);
    }

    public static boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (InvHelper.isGodItem(stack) || InvHelper.isSharpnessAxe(stack)) {
            return true;
        } else if (stack.is(ItemTags.ARMOR_ENCHANTABLE)) {
            float protection = InvHelper.getProtection(stack);
            float bestArmor = InvHelper.getBestArmorScore(stack.getEquipmentSlot());
            return !(protection <= bestArmor);
        } else if (stack.is(ItemTags.SWORDS)) {
            float damage = InvHelper.getSwordDamage(stack);
            float bestDamage = InvHelper.getBestSwordDamage();
            return !(damage <= bestDamage);
        } else if (stack.is(ItemTags.PICKAXES)) {
            float score = InvHelper.getToolScore(stack);
            float bestScore = InvHelper.getBestPickaxeScore();
            return !(score <= bestScore);
        } else if (stack.is(ItemTags.AXES)) {
            float score = InvHelper.getToolScore(stack);
            float bestScore = InvHelper.getBestAxeScore();
            return !(score <= bestScore);
        } else if (stack.is(ItemTags.SHOVELS)) {
            float score = InvHelper.getToolScore(stack);
            float bestScore = InvHelper.getBestShovelScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof CrossbowItem) {
            float score = InvHelper.getCrossbowScore(stack);
            float bestScore = InvHelper.getBestCrossbowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof BowItem && InvHelper.isPunchBow(stack)) {
            float score = InvHelper.getPunchBowScore(stack);
            float bestScore = InvHelper.getBestPunchBowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof BowItem && InvHelper.isPowerBow(stack)) {
            float score = InvHelper.getPowerBowScore(stack);
            float bestScore = InvHelper.getBestPowerBowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() == Items.COMPASS) {
            return !InvHelper.hasItem(stack.getItem());
        } else if (stack.getItem() == Items.WATER_BUCKET && InvHelper.getItemCount(Items.WATER_BUCKET) >= InvManager.INSTANCE.getWaterBucketCount()) {
            return false;
        } else if (stack.getItem() == Items.LAVA_BUCKET && InvHelper.getItemCount(Items.LAVA_BUCKET) >= InvManager.INSTANCE.getLavaBucketCount()) {
            return false;
        } else if (stack.getItem() instanceof BlockItem
                && InvHelper.isValidStack(stack)
                && InvHelper.getBlockCountInInventory() + stack.getCount() >= InvManager.INSTANCE.getMaxBlockSize()) {
            return false;
        } else if (stack.getItem() == Items.ARROW && InvHelper.getItemCount(Items.ARROW) + stack.getCount() >= InvManager.INSTANCE.getMaxArrowSize()) {
            return false;
        } else if (stack.getItem() instanceof FishingRodItem && InvHelper.getItemCount(Items.FISHING_ROD) >= 1) {
            return false;
        } else if (stack.getItem() != Items.SNOWBALL && stack.getItem() != Items.EGG
                || InvHelper.getItemCount(Items.SNOWBALL) + InvHelper.getItemCount(Items.EGG) + stack.getCount() < InvManager.INSTANCE.getMaxProjectileSize()
                && InvManager.INSTANCE.shouldKeepProjectile()) {
            return !stack.has(DataComponents.CUSTOM_NAME) && InvHelper.isCommonItemUseful(stack);
        } else {
            return false;
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        Screen currentScreen = mc.screen;
        if (currentScreen instanceof ContainerScreen container) {
            ChestMenu menu = container.getMenu();
            if (currentScreen != this.lastTickScreen) {
                timer.reset();
            } else {
                String chestTitle = container.getTitle().getString();
                String chest = Component.translatable("container.chest").getString();
                String largeChest = Component.translatable("container.chestDouble").getString();
                String enderChest = Component.translatable("container.enderchest").getString();
                if (chestTitle.equals(chest)
                        || chestTitle.equals(largeChest)
                        || chestTitle.equals("Chest")
                        || this.pickEnderChest.getValue() && chestTitle.equals(enderChest)) {
                    if (this.isChestEmpty(menu) && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
                        mc.player.closeContainer();
                    } else {
                        List<Integer> slots = IntStream.range(0, menu.getRowCount() * 9).boxed().collect(Collectors.toList());
                        Collections.shuffle(slots);

                        for (Integer pSlotId : slots) {
                            ItemStack stack = menu.getSlot(pSlotId).getItem();
                            if (isItemUseful(stack) && this.isBestItemInChest(menu, stack) && timer.passedMillise(MathUtils.getRandom(minDelay.getValue(), maxDelay.getValue()))) {
                                mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, 0, ClickType.QUICK_MOVE, mc.player);
                                timer.reset();
                                break;
                            }
                        }
                    }
                }
            }
        }

        this.lastTickScreen = currentScreen;
    }

    private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
        if (!InvHelper.isGodItem(stack) && !InvHelper.isSharpnessAxe(stack)) {
            for (int i = 0; i < menu.getRowCount() * 9; i++) {
                ItemStack checkStack = menu.getSlot(i).getItem();
                if (stack.is(ItemTags.ARMOR_ENCHANTABLE) && checkStack.is(ItemTags.ARMOR_ENCHANTABLE)) {
                    if (stack.getEquipmentSlot() == checkStack.getEquipmentSlot() && InvHelper.getProtection(checkStack) > InvHelper.getProtection(stack)) {
                        return false;
                    }
                } else if (stack.is(ItemTags.SWORDS) && checkStack.is(ItemTags.SWORDS)) {
                    if (InvHelper.getSwordDamage(checkStack) > InvHelper.getSwordDamage(stack)) {
                        return false;
                    }
                } else if (stack.is(ItemTags.PICKAXES) && checkStack.is(ItemTags.PICKAXES)) {
                    if (InvHelper.getToolScore(checkStack) > InvHelper.getToolScore(stack)) {
                        return false;
                    }
                } else if (stack.is(ItemTags.AXES) && checkStack.is(ItemTags.AXES)) {
                    if (InvHelper.getToolScore(checkStack) > InvHelper.getToolScore(stack)) {
                        return false;
                    }
                } else if (stack.is(ItemTags.SHOVELS)
                        && checkStack.is(ItemTags.SHOVELS)
                        && InvHelper.getToolScore(checkStack) > InvHelper.getToolScore(stack)) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    private boolean isChestEmpty(ChestMenu menu) {
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            if (!item.isEmpty() && isItemUseful(item) && this.isBestItemInChest(menu, item)) {
                return false;
            }
        }

        return true;
    }

}
