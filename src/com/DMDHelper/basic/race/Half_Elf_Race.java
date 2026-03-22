package com.DMDHelper.basic.race;

public class Half_Elf_Race extends Character_Race {
    public Half_Elf_Race() {
        // 半精灵魅力+2，另外两项+1（此处为了底层类固定，默认分配给敏捷和智力）
        super("Half_Elf", 30, Creature_Size.MEDIUM, new Ability_Bonuses(0, 1, 0, 1, 0, 2));
    }

    @Override
    public void apply_racial_traits() {
        System.out.printf("Applying Half_Elf traits: Darkvision, Fey_Ancestry, Skill_Versatility.\n");
    }
}