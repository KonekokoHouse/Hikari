package com.github.lumin.modules.impl.player;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.combat.AimAssist;
import com.github.lumin.modules.impl.combat.AutoClicker;
import com.github.lumin.utils.math.MathUtils;
import com.github.lumin.utils.player.ChatUtils;
import com.github.lumin.utils.timer.TimerUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Collection;
import java.util.Random;

public class AutoAccount extends Module {

    public static final AutoAccount INSTANCE = new AutoAccount();

    private AutoAccount() {
        super("自动豁免", "AutoAccount", Category.PLAYER);
    }

    private final TimerUtils timer = new TimerUtils();
    private final Random random = new Random();
    private int gamesPlayed = 0;

    private State state = State.Hub;

    private enum State {
        Hub,
        SelectingGame,
        WaitingForPVPHub,
        PVPHub,
        SelectingMode,
        InGame,
        GameEnd
    }

    @Override
    protected void onEnable() {
        state = State.Hub;
        gamesPlayed = 0;
        timer.reset();

        mc.options.pauseOnLostFocus = false;
        if (mc.screen == null) {
            mc.mouseHandler.releaseMouse();
        }
    }

    @Override
    protected void onDisable() {
        mc.options.pauseOnLostFocus = true;
    }

    @SubscribeEvent
    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() != mc.player) return;
        timer.reset();
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;

        switch (state) {
            case Hub -> {
                if (isScoreboardContains("大厅")) {
                    if (mc.player.containerMenu instanceof InventoryMenu) {
                        ItemStack stack = mc.player.getInventory().getItem(0);
                        if (stack.is(Items.PAPER) && stack.getHoverName().getString().contains("玩法选择")) {
                            if (timer.passedMillise(5000)) {
                                if (mc.player.getInventory().getSelectedSlot() != 0) {
                                    mc.player.getInventory().setSelectedSlot(0);
                                }
                                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                                state = State.SelectingGame;
                                timer.reset();
                            }
                        }
                    }
                }
            }
            case SelectingGame -> {
                if (mc.player.containerMenu instanceof ChestMenu chestMenu) {
                    if (timer.passedMillise(500)) {
                        for (int i = 0; i < chestMenu.slots.size(); i++) {
                            ItemStack stack = chestMenu.getSlot(i).getItem();
                            if (stack.getHoverName().getString().contains("决斗竞技场")) {
                                mc.gameMode.handleInventoryMouseClick(chestMenu.containerId, i, 0, ClickType.PICKUP, mc.player);
                                state = State.WaitingForPVPHub;
                                timer.reset();
                                return;
                            }
                        }
                    }
                }
            }
            case WaitingForPVPHub -> {
                if (isScoreboardContains("竞技场")) {
                    state = State.PVPHub;
                    timer.reset();
                }
            }
            case PVPHub -> {
                if (mc.player.containerMenu instanceof InventoryMenu) {
                    ItemStack stack = mc.player.getInventory().getItem(0);
                    if (stack.is(Items.IRON_SWORD) && stack.getHoverName().getString().contains("无级匹配")) {
                        if (mc.player.getInventory().getSelectedSlot() != 0) {
                            mc.player.getInventory().setSelectedSlot(0);
                        }

                        if (timer.passedMillise(500)) {
                            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                            state = State.SelectingMode;
                            timer.reset();
                        }
                    }
                }
            }
            case SelectingMode -> {
                if (mc.player.containerMenu instanceof ChestMenu chestMenu) {
                    if (timer.passedMillise(500)) {
                        String[] whiteList = new String[]{"单刀赴会", "Boxing"};

                        // 1. 优先选择数量为2且在白名单的物品
                        for (int i = 0; i < chestMenu.slots.size(); i++) {
                            ItemStack stack = chestMenu.getSlot(i).getItem();
                            if (stack.isEmpty()) continue;

                            String stackName = stack.getHoverName().getString();
                            boolean isWhitelisted = false;
                            for (String white : whiteList) {
                                if (stackName.contains(white)) {
                                    isWhitelisted = true;
                                    break;
                                }
                            }

                            if (isWhitelisted && stack.getCount() == 2) {
                                mc.gameMode.handleInventoryMouseClick(chestMenu.containerId, i, 0, ClickType.PICKUP, mc.player);
                                timer.reset();
                                return;
                            }
                        }

                        // 2. 随机选择（兜底，同样应用白名单）
                        int i = MathUtils.getRandom(0, chestMenu.slots.size());
                        ItemStack stack = chestMenu.getSlot(i).getItem();
                        if (stack.isEmpty()) return;

                        String stackName = stack.getHoverName().getString();
                        boolean isWhitelisted = false;
                        for (String white : whiteList) {
                            if (stackName.contains(white)) {
                                isWhitelisted = true;
                                break;
                            }
                        }

                        if (isWhitelisted) {
                            mc.gameMode.handleInventoryMouseClick(chestMenu.containerId, i, 0, ClickType.PICKUP, mc.player);
                            timer.reset();
                        }
                    }
                }
            }
            case InGame -> {
                // Scoreboard Check
                if (!isScoreboardContains("竞技场")) return;
                if (!isScoreboardContains("对手")) return;

                // 自动游玩逻辑
                if (timer.passedMillise(500)) { // 每0.5秒执行一次动作
                    // 鼠标晃动
                    float yawShake = (random.nextFloat() - 0.5f) * 10;
                    float pitchShake = (random.nextFloat() - 0.5f) * 5;
                    mc.player.turn(yawShake, pitchShake);

                    // 往前走
                    mc.options.keyUp.setDown(true);

                    // 按左键
                    if (random.nextBoolean()) {
                        mc.options.keyAttack.setDown(true);
                    } else {
                        mc.options.keyAttack.setDown(false);
                    }

                    timer.reset();
                }
            }
            case GameEnd -> {
                mc.options.keyUp.setDown(false);
                mc.options.keyAttack.setDown(false);

                if (gamesPlayed >= 10) {
                    ChatUtils.addChatMessage("已完成10局游戏，自动停止。");
                    toggle(); // 关闭模块
                } else {
                    // 重置状态，准备下一局
                    if (timer.passedMillise(3000)) { // 等待3秒返回大厅或继续
                        state = State.WaitingForPVPHub;
                        timer.reset();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    private void onPacket(PacketEvent.Receive event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof ClientboundSetTitleTextPacket(Component text)) {
            String title = text.getString();

            if (title.contains("胜利")) {
                gameEnd();
            }
        } else if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            String chat = packet.content().getString();

            if (chat.contains("战斗开始")) {
                gameStart();
            }

            if (chat.contains("你输了")) {
                gameEnd();
            }
        }
    }

    private void gameStart() {
        state = State.InGame;
        timer.reset();
        ChatUtils.addChatMessage("检测到游戏开始");
        AimAssist.INSTANCE.setEnabled(true);
        AutoClicker.INSTANCE.setEnabled(true);
    }

    private void gameEnd() {
        state = State.GameEnd;
        gamesPlayed++;
        ChatUtils.addChatMessage("检测到游戏结束，当前局数: " + gamesPlayed);
        timer.reset();
        AimAssist.INSTANCE.setEnabled(false);
        AutoClicker.INSTANCE.setEnabled(false);
    }

    private boolean isScoreboardContains(String text) {
        if (mc.level == null) return false;
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (objective == null) return false;

        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
        for (PlayerScoreEntry score : scores) {
            String owner = score.owner();
            PlayerTeam team = scoreboard.getPlayersTeam(owner);
            Component displayName = PlayerTeam.formatNameForTeam(team, Component.literal(owner));
            if (displayName.getString().contains(text)) return true;
        }

        if (objective.getDisplayName().getString().contains(text)) return true;

        return false;
    }

}
