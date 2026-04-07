package com.DMHelper.basic.combat;

import java.util.ArrayList;
import java.util.List;

public class Monster_Definition {
    public final String key;
    public final String monster_type;
    public final String chinese_name;
    public final String english_name;
    public final String recommended_level;
    public final int xp_reward;
    public final int armor_class;
    public final int hit_dice_count;
    public final int hit_dice_size;
    public final int hit_point_bonus;
    public final int str;
    public final int dex;
    public final int con;
    public final int intel;
    public final int wis;
    public final int cha;
    public final List<Attack_Option> attack_options;

    public Monster_Definition(String key,
                              String monster_type,
                              String chinese_name,
                              String english_name,
                              String recommended_level,
                              int xp_reward,
                              int armor_class,
                              int hit_dice_count,
                              int hit_dice_size,
                              int hit_point_bonus,
                              int str,
                              int dex,
                              int con,
                              int intel,
                              int wis,
                              int cha,
                              List<Attack_Option> attack_options) {
        this.key = key;
        this.monster_type = monster_type == null || monster_type.trim().isEmpty() ? "通用" : monster_type;
        this.chinese_name = chinese_name;
        this.english_name = english_name;
        this.recommended_level = recommended_level;
        this.xp_reward = xp_reward;
        this.armor_class = armor_class;
        this.hit_dice_count = hit_dice_count;
        this.hit_dice_size = hit_dice_size;
        this.hit_point_bonus = hit_point_bonus;
        this.str = str;
        this.dex = dex;
        this.con = con;
        this.intel = intel;
        this.wis = wis;
        this.cha = cha;
        this.attack_options = new ArrayList<>(attack_options);
    }

    public Monster_Definition(String key,
                              String chinese_name,
                              String english_name,
                              String recommended_level,
                              int xp_reward,
                              int armor_class,
                              int hit_dice_count,
                              int hit_dice_size,
                              int hit_point_bonus,
                              int str,
                              int dex,
                              int con,
                              int intel,
                              int wis,
                              int cha,
                              List<Attack_Option> attack_options) {
        this(key, "通用", chinese_name, english_name, recommended_level, xp_reward, armor_class,
                hit_dice_count, hit_dice_size, hit_point_bonus, str, dex, con, intel, wis, cha, attack_options);
    }

    public String get_display_label() {
        return this.chinese_name + " (" + this.english_name + ") | " + this.monster_type + " | 推荐等级 " + this.recommended_level
                + " | XP " + this.xp_reward;
    }

    public String get_full_name() {
        return this.chinese_name + " (" + this.english_name + ")";
    }

    public int roll_hit_points() {
        return Dice_Util.roll_dice(this.hit_dice_count, this.hit_dice_size) + this.hit_point_bonus;
    }
}
