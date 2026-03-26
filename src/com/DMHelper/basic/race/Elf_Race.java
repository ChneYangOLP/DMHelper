package com.DMHelper.basic.race;

public class Elf_Race extends Character_Race {
    public Elf_Race() {
        // 精灵敏捷+2，标准速度30
        super("Elf", 30, Creature_Size.MEDIUM, new Ability_Bonuses(0, 2, 0, 0, 0, 0));
    }

    @Override
    public void apply_racial_traits() {
        System.out.printf("Applying Elf traits: Darkvision, Keen_Senses, Fey_Ancestry.\n");
    }
}