package com.DMHelper.basic.race;

public class Half_Orc_Race extends Character_Race {
    public Half_Orc_Race() {
        super("半兽人 (Half-Orc)", 30, Creature_Size.MEDIUM, new Ability_Bonuses(2, 0, 1, 0, 0, 0));
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        this.darkvision_range = 60;
        add_language("通用语");
        add_language("兽人语");
        add_trait("威吓 (Menacing)：熟练威吓。");
        add_trait("顽强不屈 (Relentless Endurance)：每长休一次，生命值降至 0 但未被直接杀死时改为降至 1。");
        add_trait("野蛮重击 (Savage Attacks)：近战武器重击时额外掷 1 个武器伤害骰。");
    }
}
