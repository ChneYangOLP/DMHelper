package com.DMDHelper.basic.race;

public class Tiefling_Race extends Character_Race {
    public Tiefling_Race() {
        // 提夫林智力+1，魅力+2
        super("Tiefling", 30, Creature_Size.MEDIUM, new Ability_Bonuses(0, 0, 0, 1, 0, 2));
    }

    @Override
    public void apply_racial_traits() {
        System.out.printf("Applying Tiefling traits: Darkvision, Hellish_Resistance, Infernal_Legacy.\n");
    }
}