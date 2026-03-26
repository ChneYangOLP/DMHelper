package com.DMHelper.basic.race;

public class Halfling_Race extends Character_Race {
    public Halfling_Race() {
        // 半身人敏捷+2，速度25，体型为小型(SMALL)
        super("Halfling", 25, Creature_Size.SMALL, new Ability_Bonuses(0, 2, 0, 0, 0, 0));
    }

    @Override
    public void apply_racial_traits() {
        // 半身人标志性能力：幸运（投出1可重投）
        System.out.printf("Applying Halfling traits: Lucky, Brave, Halfling_Nimbleness.\n");
    }
}