package com.DMDHelper.basic.armor;

// 护甲实体类，玩家装备的每件衣服都是它的实例
public class Armor_Item {
    public String armor_name;
    public Armor_Type armor_type;
    public int base_ac; // 护甲提供的基础 AC

    public Armor_Item(String armor_name, Armor_Type armor_type, int base_ac) {
        this.armor_name = armor_name;
        this.armor_type = armor_type;
        this.base_ac = base_ac;
    }
}