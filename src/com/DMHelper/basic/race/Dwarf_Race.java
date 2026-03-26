package com.DMHelper.basic.race;

public class Dwarf_Race extends Character_Race {
    public Dwarf_Race() {
        // 矮人体质+2，速度较慢为25，体型中等
        super("Dwarf", 25, Creature_Size.MEDIUM, new Ability_Bonuses(0, 0, 2, 0, 0, 0));
    }

    @Override
    public void apply_racial_traits() {
        // 矮人特有能力：黑暗视觉，毒素抗性等
        System.out.printf("Applying Dwarf traits: Darkvision, Dwarven_Resilience.\n");
    }
}