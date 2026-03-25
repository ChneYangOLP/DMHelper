package com.DMDHelper.basic.playerclass;

import com.DMDHelper.basic.combat.Combatant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Character_Class {
    public String class_key;
    public String class_name;
    public int hp_dice;
    public int current_level;

    public List<String> skill_options;
    public int skill_choose_count;

    public List<String> saving_throws;
    public List<String> skill_proficiencies;
    public List<String> equipment_proficiencies;
    public List<String> feat_names;
    public int used_asi_choices;

    public Character_Class(String class_key, String class_name, int hp_dice) {
        this.class_key = class_key;
        this.class_name = class_name;
        this.hp_dice = hp_dice;
        this.current_level = 1;

        this.skill_options = new ArrayList<>();
        this.saving_throws = new ArrayList<>();
        this.skill_proficiencies = new ArrayList<>();
        this.equipment_proficiencies = new ArrayList<>();
        this.feat_names = new ArrayList<>();
        this.used_asi_choices = 0;
    }

    public boolean select_skills(List<String> chosen_skills) {
        if (chosen_skills.size() != this.skill_choose_count) {
            System.out.printf("[系统提示] 技能选择失败：你需要选择 %d 项技能，但你选择了 %d 项。\n",
                    this.skill_choose_count, chosen_skills.size());
            return false;
        }

        for (String skill : chosen_skills) {
            if (!this.skill_options.contains(skill)) {
                System.out.printf("[系统提示] 技能选择失败：技能 '%s' 不在 %s 的可选列表中。\n",
                        skill, this.class_name);
                return false;
            }
        }

        this.skill_proficiencies.clear();
        this.skill_proficiencies.addAll(chosen_skills);
        System.out.println("[系统提示] 技能熟练项选择成功！");
        return true;
    }

    public boolean has_feat(String feat_name) {
        return this.feat_names.contains(feat_name);
    }

    public int get_extra_armor_class_bonus(String armor_type) {
        return 0;
    }

    public int get_extra_hit_points_per_level() {
        return 0;
    }

    public void restore_long_rest_resources() {
    }

    public void sync_from_combatant(Combatant combatant) {
    }

    public abstract void rebuild_progression();
    public abstract void level_up(int target_level);
    public abstract int get_average_hp_gain();
    public abstract String get_subclass_name();
    public abstract List<String> get_feature_summaries();
    public abstract List<String> get_pending_choices();
    public abstract Map<String, String> export_class_state();
    public abstract void import_class_state(Map<String, String> class_state);
}
