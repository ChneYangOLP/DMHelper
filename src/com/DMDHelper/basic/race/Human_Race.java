package com.DMDHelper.basic.race;

// 人类种族类，继承自种族抽象基类
public class Human_Race extends Character_Race {
    public Human_Race() {
        // 传入参数：种族名"Human"，速度30，体型MEDIUM，以及全属性+1的加值对象
        super("Human", 30, Creature_Size.MEDIUM, new Ability_Bonuses(1, 1, 1, 1, 1, 1));
    }

    // 重写父类的抽象方法，用于处理人类特有的种族能力
    @Override
    public void apply_racial_traits() {
        // 标准人类除了全属性+1外，主要的特性是能额外掌握一门语言
        System.out.printf("Applying Human traits: Extra language of your choice.\n");
    }
}