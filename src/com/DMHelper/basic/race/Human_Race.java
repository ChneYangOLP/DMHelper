package com.DMHelper.basic.race;

// 人类种族类，继承自种族抽象基类
public class Human_Race extends Character_Race {
    public Human_Race() {
        super("人类 (Human)", 30, Creature_Size.MEDIUM, new Ability_Bonuses(1, 1, 1, 1, 1, 1));
        apply_racial_traits();
    }

    // 重写父类的抽象方法，用于处理人类特有的种族能力
    @Override
    public void apply_racial_traits() {
        reset_traits();
        add_language("通用语");
        add_language("额外自选语言 1 门");
        add_trait("标准人类：六项属性各 +1，拥有极高的职业适应性。");
        add_trait("额外语言：除通用语外，再掌握 1 门自选语言。");
    }
}
