package dev.maru.verify.client;

public interface IRCHandler {
    void onMessage(String sender, String message);

    void onDisconnected(String message);

    void onConnected();

    String getInGameUsername();

    default void onLoginResult(boolean success, long expireAt, long timeWindow) {
    }

    default void onLoginResult(boolean success, long expireAt, long timeWindow, String message) {
        onLoginResult(success, expireAt, timeWindow);
    }

    default void onRegisterResult(boolean success, long expireAt, long timeWindow) {
    }

    default void onRegisterResult(boolean success, long expireAt, long timeWindow, String message) {
        onRegisterResult(success, expireAt, timeWindow);
    }

    default void onRechargeResult(boolean success, long expireAt, long timeWindow) {
    }

    default void onRechargeResult(boolean success, long expireAt, long timeWindow, String message) {
        onRechargeResult(success, expireAt, timeWindow);
    }

    default void onCloudConfigUploadResult(boolean success) {
    }

    default void onCloudConfigUploadResult(boolean success, String message, int max) {
        onCloudConfigUploadResult(success);
    }

    default void onCloudConfigGetResult(boolean success, String owner, String name, String content) {
    }

    default void onCloudConfigGetResult(boolean success, String owner, String name, String content, String message) {
        onCloudConfigGetResult(success, owner, name, content);
    }

    default void onCloudConfigListResult(boolean success, java.util.List<String> names, int max) {
    }

    default void onCloudConfigListResult(boolean success, java.util.List<String> names, int max, String message) {
        onCloudConfigListResult(success, names, max);
    }

    default void onCloudConfigDeleteResult(boolean success, String owner, String name, String message) {
    }

    default void onModDownload(String content, String hash) {
    }
}

