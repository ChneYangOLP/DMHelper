package com.DMDHelper.basic.armor;

// 防具封装类
public class Armor {
    public String armor_name;
    public String armor_type; // 可选值: "None" (无甲), "Light" (轻甲), "Medium" (中甲), "Heavy" (重甲)
    public int base_ac;

    public Armor(String armor_name, String armor_type, int base_ac) {
        this.armor_name = armor_name;
        this.armor_type = armor_type;
        this.base_ac = base_ac;
    }
}