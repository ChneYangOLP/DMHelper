package com.DMHelper.basic.race;

// 封装六维属性加值的实体类
public class Ability_Bonuses {
    public int strength_bonus;
    public int dexterity_bonus;
    public int constitution_bonus;
    public int intelligence_bonus;
    public int wisdom_bonus;
    public int charisma_bonus;

    // 构造函数，方便在实例化具体种族时一次性录入所有数据
    public Ability_Bonuses(int strength_bonus, int dexterity_bonus, int constitution_bonus,
                           int intelligence_bonus, int wisdom_bonus, int charisma_bonus) {
        this.strength_bonus = strength_bonus;
        this.dexterity_bonus = dexterity_bonus;
        this.constitution_bonus = constitution_bonus;
        this.intelligence_bonus = intelligence_bonus;
        this.wisdom_bonus = wisdom_bonus;
        this.charisma_bonus = charisma_bonus;
    }
}