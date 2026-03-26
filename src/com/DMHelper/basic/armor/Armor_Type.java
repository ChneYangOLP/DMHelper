package com.DMHelper.basic.armor;

// 规范护甲分类，方便后续做敏捷调整值的上限判断
public enum Armor_Type {
    NONE,   // 无甲
    LIGHT,  // 轻甲 (全额敏捷加成)
    MEDIUM, // 中甲 (最大 +2 敏捷加成)
    HEAVY   // 重甲 (无敏捷加成)
}