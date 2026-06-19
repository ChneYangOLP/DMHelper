package com.DMHelper.basic.combat;

/**
 * D&D 5e 掩护类型枚举 (Cover Type)。
 * 掩护为被掩护方提供 AC 加值和敏捷豁免加值。
 */
public enum Cover_Type {
    NONE("无掩护", 0, 0, 0),
    HALF("半掩护 (Half Cover)", 2, 2, 0),
    THREE_QUARTERS("四分之三掩护 (Three-Quarters Cover)", 5, 5, 0),
    FULL("全掩护 (Full Cover)", 0, 0, 0);

    /** 显示名（中英双语） */
    public final String label;
    /** AC 加值 */
    public final int ac_bonus;
    /** 敏捷豁免加值 */
    public final int dexterity_save_bonus;
    /** 力量豁免加值 */
    public final int strength_save_bonus;

    Cover_Type(String label,
               int ac_bonus,
               int dexterity_save_bonus,
               int strength_save_bonus) {
        this.label = label;
        this.ac_bonus = ac_bonus;
        this.dexterity_save_bonus = dexterity_save_bonus;
        this.strength_save_bonus = strength_save_bonus;
    }

    /** 获取目标 AC 加值 */
    public int get_target_ac_bonus() {
        return this.ac_bonus;
    }

    /** 获取目标敏捷豁免加值 */
    public int get_target_dex_save_bonus() {
        return this.dexterity_save_bonus;
    }

    /** 获取显示名 */
    public String get_label() {
        return this.label;
    }
}
