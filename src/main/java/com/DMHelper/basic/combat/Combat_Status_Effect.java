package com.DMHelper.basic.combat;

public class Combat_Status_Effect {
    public final Combat_Status_Type type;
    public int rounds_remaining;

    public Combat_Status_Effect(Combat_Status_Type type, int rounds_remaining) {
        this.type = type;
        this.rounds_remaining = Math.max(1, rounds_remaining);
    }

    public String get_label() {
        return this.type.label + " (" + this.rounds_remaining + "轮)";
    }
}
