package dev.maru.verify.packet.implemention.c2s;

import dev.maru.verify.packet.IRCPacket;
import dev.maru.verify.packet.annotations.ProtocolField;

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
