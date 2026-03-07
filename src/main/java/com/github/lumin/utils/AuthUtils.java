package com.github.lumin.utils;

import com.github.lumin.Lumin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;
import java.util.Base64;

@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public class AuthUtils {

    private static int tickCounter = 0;

    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= 100) { // Check every 100 ticks (approx 5 seconds)
            tickCounter = 0;
            AuthUtils.checkHeartbeat();
        }
    }

    public static void checkConnection() {
        try {
            Class<?> luminApiClass = Class.forName("dev.maru.api.LuminAPI");

            // 检查是否连接
            Method isConnectedMethod = luminApiClass.getMethod("isConnected");
            boolean isConnected = (boolean) isConnectedMethod.invoke(null);
            if (!isConnected) {
                // 没连接 -> 退出
                forceExit();
            }
        } catch (Exception exception) {
            forceExit();
        }
    }

    public static void checkHeartbeat() {
        try {
            Class<?> luminApiClass = Class.forName("dev.maru.api.LuminAPI");

            // 再次检查连接
            Method isConnectedMethod = luminApiClass.getMethod("isConnected");
            boolean isConnected = (boolean) isConnectedMethod.invoke(null);
            if (!isConnected) {
                forceExit();
                return;
            }

            // 获取动态参数
            Method getMethod = luminApiClass.getMethod("get", String.class);
            
            // 校验时间戳防止重放（可选）
            String timestampStr = (String) getMethod.invoke(null, "timestamp");
            if (timestampStr != null) {
                long serverTime = Long.parseLong(timestampStr);
                if (System.currentTimeMillis() - serverTime > 300000) { // 5分钟有效期
                    forceExit();
                    return;
                }
            }

            // 心跳验证：检查上次心跳时间
            String lastHeartbeatStr = (String) getMethod.invoke(null, "last_heartbeat");
            if (lastHeartbeatStr != null) {
                long lastHeartbeat = Long.parseLong(lastHeartbeatStr);
                // 如果超过 30 秒没有收到心跳更新
                if (System.currentTimeMillis() - lastHeartbeat > 30000) {
                    forceExit();
                }
            }
            // 如果 lastHeartbeatStr 为 null，可能是刚启动尚未同步，暂时不退出

        } catch (Exception exception) {
            forceExit();
        }
    }

    private static void forceExit() {
        try {
            Class<?> System = AuthUtils.class.getClassLoader().loadClass(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));
            Method exit = System.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), int.class);
            exit.invoke(null, 0);
        } catch (Exception ignored) {
        }
    }
}
