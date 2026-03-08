package com.github.lumin.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final String GRADIENT_SYNC_CODE = "§)";
    private static final String CLIENT_PREFIX_TEXT = "[Lumin]";
    private static final String CLIENT_PREFIX = GRADIENT_SYNC_CODE + "§r" + CLIENT_PREFIX_TEXT + "§f ";

    public static void addChatMessage(String message) {
        addChatMessage(true, message);
    }

    public static void addChatMessage(boolean prefix, String message) {
        mc.execute(() -> mc.gui.getChat().addMessage(Component.literal((prefix ? CLIENT_PREFIX : "") + message)));
    }

    public static void addPlayerMessage(String message) {
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(message.replace('&', '§')), false);
            }
        });
    }

}
