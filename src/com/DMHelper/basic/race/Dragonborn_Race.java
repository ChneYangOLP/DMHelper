package com.DMHelper.basic.race;

public class Dragonborn_Race extends Character_Race {
    public Dragonborn_Race() {
        // 龙裔力量+2，魅力+1
        super("Dragonborn", 30, Creature_Size.MEDIUM, new Ability_Bonuses(2, 0, 0, 0, 0, 1));
    }

    @Override
    public void apply_racial_traits() {
        // 龙裔特色：龙族血统带来的吐息武器和伤害抗性
        System.out.printf("Applying Dragonborn traits: Draconic_Ancestry, Breath_Weapon, Damage_Resistance.\n");
    }
}