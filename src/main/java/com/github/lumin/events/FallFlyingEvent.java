package com.github.lumin.events;

import net.neoforged.bus.api.Event;

 //@Getter
public class FallFlyingEvent extends Event {
    private float pitch;

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPitch() {
        return this.pitch;
    }

    public FallFlyingEvent(float pitch) {
        this.pitch = pitch;
    }
}
