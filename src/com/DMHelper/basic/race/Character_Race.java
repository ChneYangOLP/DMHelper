package com.DMHelper.basic.race;

import java.util.ArrayList;
import java.util.List;

// 种族的抽象基类
public abstract class Character_Race {
    public String race_name;
    public String subrace_name;
    public int base_speed;
    public Creature_Size creature_size;
    public Ability_Bonuses racial_bonuses;
    public int darkvision_range;
    public List<String> languages;
    public List<String> racial_traits;

    // 基类构造函数
    public Character_Race(String race_name, int base_speed, Creature_Size creature_size, Ability_Bonuses racial_bonuses) {
        this.race_name = race_name;
        this.subrace_name = "";
        this.base_speed = base_speed;
        this.creature_size = creature_size;
        this.racial_bonuses = racial_bonuses;
        this.darkvision_range = 0;
        this.languages = new ArrayList<>();
        this.racial_traits = new ArrayList<>();
    }

    // 抽象方法：应用种族特有能力交由具体子类实现
    public abstract void apply_racial_traits();

    protected void reset_traits() {
        this.darkvision_range = 0;
        this.languages.clear();
        this.racial_traits.clear();
    }

    protected void add_language(String language) {
        if (language != null && !language.trim().isEmpty() && !this.languages.contains(language)) {
            this.languages.add(language);
        }
    }

    protected void add_trait(String trait) {
        if (trait != null && !trait.trim().isEmpty()) {
            this.racial_traits.add(trait);
        }
    }

    public String get_size_label() {
        if (this.creature_size == Creature_Size.SMALL) {
            return "小型";
        }
        if (this.creature_size == Creature_Size.LARGE) {
            return "大型";
        }
        return "中型";
    }

    public String get_ability_bonus_summary() {
        List<String> bonuses = new ArrayList<>();
        append_bonus(bonuses, "力量", this.racial_bonuses.strength_bonus);
        append_bonus(bonuses, "敏捷", this.racial_bonuses.dexterity_bonus);
        append_bonus(bonuses, "体质", this.racial_bonuses.constitution_bonus);
        append_bonus(bonuses, "智力", this.racial_bonuses.intelligence_bonus);
        append_bonus(bonuses, "感知", this.racial_bonuses.wisdom_bonus);
        append_bonus(bonuses, "魅力", this.racial_bonuses.charisma_bonus);
        return bonuses.isEmpty() ? "无" : String.join("、", bonuses);
    }

    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>();
        summaries.add("属性加值： " + get_ability_bonus_summary());
        summaries.add("体型/速度： " + get_size_label() + " / " + this.base_speed + " 尺");
        if (this.darkvision_range > 0) {
            summaries.add("黑暗视觉： " + this.darkvision_range + " 尺");
        }
        if (!this.languages.isEmpty()) {
            summaries.add("语言： " + String.join("、", this.languages));
        }
        summaries.addAll(this.racial_traits);
        return summaries;
    }

    private void append_bonus(List<String> bonuses, String label, int value) {
        if (value > 0) {
            bonuses.add(label + " +" + value);
        }
    }
}
