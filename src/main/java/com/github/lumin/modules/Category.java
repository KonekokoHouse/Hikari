package com.github.lumin.modules;

public enum Category {

    COMBAT("b", "战斗", "ComBat"),
    PLAYER("5", "玩家", "Player"),
    RENDER("a", "渲染", "Render"),
    WORLD("3", "世界", "World"),
    CLIENT("7", "客户端", "Client");

    public final String icon;
    private final String cnName;
    public final String description;

    Category(String icon, String cnName, String description) {
        this.icon = icon;
        this.cnName = cnName;
        this.description = description;
    }

    public String getName() {
        return cnName;
    }
}
