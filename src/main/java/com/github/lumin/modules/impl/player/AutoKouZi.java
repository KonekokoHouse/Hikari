package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.ModeSetting;
import com.github.lumin.settings.impl.StringSetting;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoKouZi extends Module {

    public static final AutoKouZi INSTANCE = new AutoKouZi();

    public final ModeSetting mode = modeSetting("模式", "Power", new String[]{"Power", "Safe"});
    public final DoubleSetting delay = doubleSetting("延迟", 3.0, 0.0, 20.0, 1.0);
    public final BoolSetting whisperMode = boolSetting("私聊模式", false);
    public final StringSetting targetPlayer = stringSetting("目标玩家", "");
    public final StringSetting whisperCmd = stringSetting("私聊命令", "msg");
    public final BoolSetting commandMode = boolSetting("命令模式", false);

    private final List<String> messages = new ArrayList<>();
    private final Random random = new Random();
    private long lastSendTime = 0;
    private String lastMode = null;

    public AutoKouZi() {
        super("自动扣字", "喷系少年", Category.PLAYER);
    }

    private void loadMessages() {
        messages.clear();
        String fileName = mode.is("Power") ? "CiHui1.txt" : "CiHui2.txt";
        File file = getConfigFile(fileName);
        
        if (!file.exists()) {
            extractResource(fileName, file);
        }
        
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        messages.add(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File getConfigFile(String fileName) {
        return Paths.get("lumin-config", fileName).toFile();
    }

    private void extractResource(String resourceName, File destination) {  //加载资源
        try (InputStream is = getClass().getResourceAsStream("/assets/minecraft/texts/" + resourceName)) {
            if (is != null) {
                Files.copy(is, destination.toPath());
            } else {
                destination.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onEnable() {
        if (nullCheck()) {
            toggle();
            return;
        }
        //恶俗的私聊模式
        if (whisperMode.getValue() && targetPlayer.getValue().trim().isEmpty()) {
            sendMessage("错误：私聊需填写目标玩家");
            toggle();
            return;
        }

        loadMessages();
        if (messages.isEmpty()) {
            sendMessage("错误：词库为空");
            toggle();
            return;
        }

        lastMode = mode.getValue();
        lastSendTime = System.currentTimeMillis();
        sendMessage("自动扣字已启动");
    }

    @Override
    protected void onDisable() {
        sendMessage("自动扣字已停止");
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post event) {

        if (!mode.getValue().equals(lastMode)) {
            lastMode = mode.getValue();
            loadMessages();
            if (messages.isEmpty()) {
                sendMessage("错误：词库在模式切换后为空");
            }
        }

        if (nullCheck() || mc.getConnection() == null) return;

        long now = System.currentTimeMillis();
        long delayMs = (long) (delay.getValue() * 1000);
        if (now - lastSendTime < delayMs) return;

        try {
            if (messages.isEmpty()) return;
            String msg = messages.get(random.nextInt(messages.size()));
            String finalContent;

            if (whisperMode.getValue()) {
                finalContent = "/" + whisperCmd.getValue().trim() + " " + targetPlayer.getValue().trim() + " " + msg;
            } else {
                finalContent = msg;
            }

            ClientPacketListener connection = mc.getConnection();
            if (connection != null) {
                if (whisperMode.getValue() || commandMode.getValue()) {
                    String cmdContent = finalContent.startsWith("/") ? finalContent.substring(1) : finalContent;
                    connection.sendCommand(cmdContent);
                } else {
                    connection.sendChat(finalContent);
                }
            }

            lastSendTime = now;
        } catch (Exception e) {
            sendMessage("发送失败：" + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }
}
