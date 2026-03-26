package com.DMHelper.basic.race;

// 种族的抽象基类
public abstract class Character_Race {
    public String race_name;
    public int base_speed;
    public Creature_Size creature_size;
    public Ability_Bonuses racial_bonuses;

    // 基类构造函数
    public Character_Race(String race_name, int base_speed, Creature_Size creature_size, Ability_Bonuses racial_bonuses) {
        this.race_name = race_name;
        this.base_speed = base_speed;
        this.creature_size = creature_size;
        this.racial_bonuses = racial_bonuses;
    }

    // 抽象方法：应用种族特有能力交由具体子类实现
    public abstract void apply_racial_traits();
}