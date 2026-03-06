package dev.sakura.server.packet.implemention.c2s;

import dev.sakura.server.packet.IRCPacket;
import dev.sakura.server.packet.annotations.ProtocolField;

public class RequestModC2S implements IRCPacket {
    @ProtocolField("hwid")
    private String hwid;

    public RequestModC2S() {
    }

    public RequestModC2S(String hwid) {
        this.hwid = hwid;
    }

    public String getHwid() {
        return hwid;
    }
}
