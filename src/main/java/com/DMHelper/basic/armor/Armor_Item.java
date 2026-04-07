package com.DMHelper.basic.armor;

public class Armor_Item {
    public String armor_name;
    public Armor_Type armor_type;
    public int base_ac;

    public Armor_Item(String armor_name, Armor_Type armor_type, int base_ac) {
        this.armor_name = armor_name;
        this.armor_type = armor_type;
        this.base_ac = base_ac;
    }
}
