package com.DMHelper.basic.race;

public class Tiefling_Race extends Character_Race {
    public Tiefling_Race() {
        super("提夫林 (Tiefling)", 30, Creature_Size.MEDIUM, new Ability_Bonuses(0, 0, 0, 1, 0, 2));
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        this.darkvision_range = 60;
        add_language("通用语");
        add_language("炼狱语");
        add_trait("炼狱抗性 (Hellish Resistance)：获得火焰伤害抗性。");
        add_trait("地狱传承 (Infernal Legacy)：掌握奇术之手，并在更高等级获得地狱斥责与黑暗术。");
    }
}
