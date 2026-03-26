package com.DMHelper.basic.race;

public class Half_Orc_Race extends Character_Race {
    public Half_Orc_Race() {
        // 半兽人力量+2，体质+1
        super("Half_Orc", 30, Creature_Size.MEDIUM, new Ability_Bonuses(2, 0, 1, 0, 0, 0));
    }

    @Override
    public void apply_racial_traits() {
        System.out.printf("Applying Half_Orc traits: Darkvision, Menacing, Relentless_Endurance, Savage_Attacks.\n");
    }
}