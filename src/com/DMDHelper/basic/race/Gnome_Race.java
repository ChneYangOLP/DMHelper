package com.DMDHelper.basic.race;

public class Gnome_Race extends Character_Race {
    public Gnome_Race() {
        // 侏儒智力+2，速度25，小型体型
        super("Gnome", 25, Creature_Size.SMALL, new Ability_Bonuses(0, 0, 0, 2, 0, 0));
    }

    @Override
    public void apply_racial_traits() {
        System.out.printf("Applying Gnome traits: Darkvision, Gnome_Cunning.\n");
    }
}