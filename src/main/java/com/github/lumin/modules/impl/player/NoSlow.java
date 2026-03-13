package com.github.lumin.modules.impl.player;

import com.github.lumin.events.SlowdownEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.EnumSetting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

public class NoSlow extends Module {
    public static final NoSlow INSTANCE = new NoSlow();

    private NoSlow() {
        super("NoSlow", Category.PLAYER);
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Jump);
    private final BoolSetting food = boolSetting("Food", true);
    private final BoolSetting bow = boolSetting("Bow", true);
    private final BoolSetting crossbow = boolSetting("Crossbow", true);

    private int onGroundTick = 0;

    @Override
    public void onEnable() {
        onGroundTick = 0;
    }

    @Override
    public void onDisable() {
        onGroundTick = 0;
    }

    @SubscribeEvent
    private void onSlowdown(SlowdownEvent event) {
        if (nullCheck() || (checkFood() && mc.player.getUseItemRemainingTicks() > 30)) return;

        if (!food.getValue() && checkFood()) return;
        if (!bow.getValue() && checkItem(Items.BOW)) return;
        if (!crossbow.getValue() && checkItem(Items.CROSSBOW)) return;

        switch (mode.getValue()) {
            case Mode.Vanilla -> cancel(event);
            case Mode.Jump -> jump(event);
            case Mode.Grim1_2 -> grim50(event);
            case Mode.Grim1_3 -> grim33(event);
        }
    }

    private void cancel(SlowdownEvent event) {
        event.setSlowdown(false);
    }

    private void jump(SlowdownEvent event) {
        if (onGroundTick == 1 && mc.player.getUseItemRemainingTicks() <= 30) {
            event.setSlowdown(false);
        }
    }

    private void grim50(SlowdownEvent event) {
        if (mc.player.getUseItemRemainingTicks() % 2 == 0 && mc.player.getUseItemRemainingTicks() <= 30) {
            event.setSlowdown(false);
        }
    }

    private void grim33(SlowdownEvent event) {
        if (mc.player.getUseItemRemainingTicks() % 3 == 0 && (!checkFood() || mc.player.getUseItemRemainingTicks() <= 30)) {
            event.setSlowdown(false);
        }
    }

    private boolean checkItem(Item item) {
        return mc.player.getMainHandItem().is(item) || mc.player.getOffhandItem().is(item);
    }

    private boolean checkFood() {
        ItemStack mainHandItem = mc.player.getMainHandItem();
        ItemStack offhandItem = mc.player.getOffhandItem();
        return mainHandItem.is(Items.GOLDEN_APPLE) || offhandItem.is(Items.GOLDEN_APPLE) || mainHandItem.is(Items.ENCHANTED_GOLDEN_APPLE) || offhandItem.is(Items.ENCHANTED_GOLDEN_APPLE) || mainHandItem.is(Items.POTION) || offhandItem.is(Items.POTION);
    }

    private enum Mode {
        Vanilla,
        Jump,
        Grim1_2,
        Grim1_3,
    }

}
