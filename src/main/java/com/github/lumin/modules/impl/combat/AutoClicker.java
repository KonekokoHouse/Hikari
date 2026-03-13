package com.github.lumin.modules.impl.combat;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.math.MathUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class AutoClicker extends Module {

    public static final AutoClicker INSTANCE = new AutoClicker();

    private final EnumSetting<Mode> mode = enumSetting("模式", Mode.VERSION_ABOVE_1_9);
    private final IntSetting minCPS = intSetting("最小CPS", 8, 1, 20, 1, () -> mode.is("1.8"));
    private final IntSetting maxCPS = intSetting("最大CPS", 12, 1, 20, 1, () -> mode.is("1.8"));
    private final BoolSetting jitter = boolSetting("抖动", false, () -> mode.is("1.8"));
    private final BoolSetting autoAttack = boolSetting("自动攻击", false);

    private final IntSetting minDelay = intSetting("最小延迟", 100, 0, 500, 10, () -> mode.is("1.9+"));
    private final IntSetting maxDelay = intSetting("最大延迟", 200, 0, 500, 10, () -> mode.is("1.9+"));

    private long lastClickTime = 0;
    private long nextDelay = 0;
    private long readyTime = 0;

    private AutoClicker() {
        super("AutoClicker", Category.COMBAT);
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        boolean shouldClick = mc.mouseHandler.isLeftPressed();
        if (autoAttack.getValue() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            shouldClick = true;
        }

        if (shouldClick && mc.screen == null) {
            if (mode.is("1.9+")) {
                if (mc.player.getAttackStrengthScale(0.5f) >= 1.0f) {
                    if (readyTime == 0) {
                        int min = minDelay.getValue();
                        int max = maxDelay.getValue();
                        if (min > max) {
                            int temp = min;
                            min = max;
                            max = temp;
                        }
                        readyTime = System.currentTimeMillis() + (long) (min + Math.random() * (max - min));
                    }

                    if (System.currentTimeMillis() >= readyTime) {
                        performClick();
                    }
                } else {
                    readyTime = 0;
                }
            } else {
                if (System.currentTimeMillis() - lastClickTime >= nextDelay) {
                    performClick();
                    lastClickTime = System.currentTimeMillis();
                    updateNextDelay();
                }
            }
        } else {
            lastClickTime = 0;
            readyTime = 0;
        }
    }

    private void performClick() {
        KeyMapping attackKey = mc.options.keyAttack;
        KeyMapping.click(attackKey.getKey());

        if (mode.is("1.8") && jitter.getValue()) {
            float yaw = mc.player.getYRot();
            float pitch = mc.player.getXRot();
            float yawRandom = (float) ((Math.random() - 0.5) * 0.5);
            float pitchRandom = (float) ((Math.random() - 0.5) * 0.5);
            mc.player.setYRot(yaw + yawRandom);
            mc.player.setXRot(pitch + pitchRandom);
        }
    }

    private void updateNextDelay() {
        nextDelay = 1000 / MathUtils.getRandom(minCPS.getValue(), maxCPS.getValue());
    }

    private enum Mode {
        VERSION_1_8("1.8"),
        VERSION_ABOVE_1_9("1.9+");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}