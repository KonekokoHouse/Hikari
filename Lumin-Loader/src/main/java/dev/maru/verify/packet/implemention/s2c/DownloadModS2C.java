package dev.maru.verify.packet.implemention.s2c;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

public class DownloadModS2C implements IRCPacket {
    @ProtocolField("content")
    private String content; // Base64 encoded jar

    @ProtocolField("hash")
    private String hash;

    public DownloadModS2C() {
    }

    public DownloadModS2C(String content, String hash) {
        this.content = content;
        this.hash = hash;
    }

    public String getContent() {
        return content;
    }

    public String getHash() {
        return hash;
    }
}
