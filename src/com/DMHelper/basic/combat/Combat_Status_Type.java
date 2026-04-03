package com.DMHelper.basic.combat;

public enum Combat_Status_Type {
    POISONED("中毒", -2, 0, -1, false, 0, 0, ""),
    RESTRAINED("束缚", -2, -2, -2, false, 0, 0, ""),
    PRONE("倒地", -2, -2, 0, false, 0, 0, ""),
    FRIGHTENED("恐慌", -2, 0, 0, false, 0, 0, ""),
    CHARMED("魅惑", -1, 0, 0, false, 0, 0, ""),
    PARALYZED("麻痹", 0, -4, -4, true, 0, 0, ""),
    ASLEEP("沉睡", 0, -4, -4, true, 0, 0, ""),
    BURNING("燃烧", 0, 0, 0, false, 1, 6, "火焰"),
    CURSED("诅咒", -1, 0, 0, false, 0, 0, ""),
    SLOWED("迟缓", 0, 0, 0, false, 0, 0, ""),
    SHIELDED("护盾", 0, 5, 0, false, 0, 0, ""),
    INSPIRED("激励", 0, 0, 0, false, 0, 0, ""),
    INVISIBLE("隐形", 2, 2, 0, false, 0, 0, "");

    public final String label;
    public final int attack_modifier;
    public final int armor_class_modifier;
    public final int dex_save_modifier;
    public final boolean turn_blocked;
    public final int start_turn_damage_dice_count;
    public final int start_turn_damage_dice_size;
    public final String damage_type;

    Combat_Status_Type(String label,
                       int attack_modifier,
                       int armor_class_modifier,
                       int dex_save_modifier,
                       boolean turn_blocked,
                       int start_turn_damage_dice_count,
                       int start_turn_damage_dice_size,
                       String damage_type) {
        this.label = label;
        this.attack_modifier = attack_modifier;
        this.armor_class_modifier = armor_class_modifier;
        this.dex_save_modifier = dex_save_modifier;
        this.turn_blocked = turn_blocked;
        this.start_turn_damage_dice_count = start_turn_damage_dice_count;
        this.start_turn_damage_dice_size = start_turn_damage_dice_size;
        this.damage_type = damage_type;
    }
}
