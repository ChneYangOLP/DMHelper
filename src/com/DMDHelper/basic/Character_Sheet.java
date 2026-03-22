package com.DMDHelper.basic;

import com.DMDHelper.basic.Class.Character_Class;
import com.DMDHelper.basic.Class.Dnd5e_Progression;
import com.DMDHelper.basic.Class.Fighter.Fighter_Class;
import com.DMDHelper.basic.armor.Armor;
import com.DMDHelper.basic.race.Character_Race;

import java.util.ArrayList;
import java.util.List;

public class Character_Sheet {
    public int database_id;
    public String name;
    public int age;
    public String gender;

    public Character_Race race;
    public Character_Class job;
    public Stats stats;

    public int hp;
    public int ac;
    public int experience_points;

    public Armor equipped_armor;
    public boolean has_shield;
    public List<String> advancement_notes;

    private Character_Sheet(String name,
                            int age,
                            String gender,
                            Character_Race race,
                            Character_Class job,
                            Stats final_stats) {
        this.database_id = -1;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.race = race;
        this.job = job;
        this.stats = final_stats;
        this.experience_points = 0;
        this.advancement_notes = new ArrayList<>();

        this.equipped_armor = get_default_armor(job.class_key);
        this.has_shield = "FIGHTER".equals(job.class_key);

        this.job.rebuild_progression();
        recalculate_derived_stats();
    }

    public static Character_Sheet create_new_character(String name,
                                                       int age,
                                                       String gender,
                                                       Character_Race race,
                                                       Character_Class job,
                                                       Stats raw_stats) {
        Stats final_stats = new Stats(
                raw_stats.str + race.racial_bonuses.strength_bonus,
                raw_stats.dex + race.racial_bonuses.dexterity_bonus,
                raw_stats.con + race.racial_bonuses.constitution_bonus,
                raw_stats.intel + race.racial_bonuses.intelligence_bonus,
                raw_stats.wis + race.racial_bonuses.wisdom_bonus,
                raw_stats.cha + race.racial_bonuses.charisma_bonus
        );
        return new Character_Sheet(name, age, gender, race, job, final_stats);
    }

    public static Character_Sheet restore_saved_character(String name,
                                                          int age,
                                                          String gender,
                                                          Character_Race race,
                                                          Character_Class job,
                                                          Stats final_stats) {
        return new Character_Sheet(name, age, gender, race, job, final_stats);
    }

    private Armor get_default_armor(String class_key) {
        if ("FIGHTER".equals(class_key)) {
            return new Armor("锁子甲 (Chain Mail)", "Heavy", 16);
        }
        if ("WIZARD".equals(class_key)) {
            return new Armor("学者的长袍", "None", 10);
        }
        return new Armor("普通旅行者服装", "None", 10);
    }

    public void set_equipment(Armor armor, boolean use_shield) {
        this.equipped_armor = armor;
        this.has_shield = use_shield;
        recalculate_derived_stats();
    }

    public void add_experience(int amount) {
        if (amount <= 0) {
            return;
        }
        this.experience_points += amount;
    }

    public boolean can_level_up() {
        if (this.job.current_level >= 20) {
            return false;
        }
        return this.experience_points >= Dnd5e_Progression.get_next_level_xp(this.job.current_level);
    }

    public int get_next_level_xp() {
        return Dnd5e_Progression.get_next_level_xp(this.job.current_level);
    }

    public int get_xp_to_next_level() {
        int next = get_next_level_xp();
        if (next < 0) {
            return 0;
        }
        return Math.max(0, next - this.experience_points);
    }

    public void recalculate_derived_stats() {
        this.job.rebuild_progression();
        this.hp = calculate_max_hp();
        this.ac = calculate_armor_class();
    }

    private int calculate_max_hp() {
        int con_mod = this.stats.get_mod(this.stats.con);
        int baseHp = this.job.hp_dice + con_mod;
        int higherLevels = Math.max(0, this.job.current_level - 1);
        int totalHp = baseHp + higherLevels * (this.job.get_average_hp_gain() + con_mod);

        if (this.job.has_feat("Tough")) {
            totalHp += this.job.current_level * 2;
        }
        return Math.max(this.job.current_level, totalHp);
    }

    private int calculate_armor_class() {
        int dex_mod = this.stats.get_mod(this.stats.dex);
        int calculatedAc = this.equipped_armor.base_ac;

        if ("None".equals(this.equipped_armor.armor_type) || "Light".equals(this.equipped_armor.armor_type)) {
            calculatedAc += dex_mod;
        } else if ("Medium".equals(this.equipped_armor.armor_type)) {
            calculatedAc += Math.min(dex_mod, 2);
        }

        if (this.has_shield) {
            calculatedAc += 2;
        }

        if (this.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) this.job;
            boolean hasDefenseStyle = "Defense".equals(fighter.fighting_style_name)
                    || "Defense".equals(fighter.extra_fighting_style_name);
            if (hasDefenseStyle && !"None".equals(this.equipped_armor.armor_type)) {
                calculatedAc += 1;
            }
        }

        return calculatedAc;
    }

    public void record_advancement(String note) {
        if (note != null && !note.trim().isEmpty()) {
            this.advancement_notes.add(note);
        }
    }

    public int get_proficiency_bonus() {
        return (this.job.current_level - 1) / 4 + 2;
    }

    public int get_saving_throw_bonus(String stat_name) {
        int bonus = get_ability_modifier(stat_name);
        if (this.job.saving_throws.contains(stat_name)) {
            bonus += get_proficiency_bonus();
        }
        return bonus;
    }

    public int get_skill_bonus(String skill_name) {
        int bonus = 0;
        if (skill_name.contains("运动")) {
            bonus = this.stats.get_mod(this.stats.str);
        } else if (skill_name.contains("杂技") || skill_name.contains("巧手") || skill_name.contains("隐匿")) {
            bonus = this.stats.get_mod(this.stats.dex);
        } else if (skill_name.contains("奥秘") || skill_name.contains("历史") || skill_name.contains("调查")
                || skill_name.contains("自然") || skill_name.contains("宗教")) {
            bonus = this.stats.get_mod(this.stats.intel);
        } else if (skill_name.contains("驯兽") || skill_name.contains("洞悉") || skill_name.contains("医药")
                || skill_name.contains("察觉") || skill_name.contains("生存")) {
            bonus = this.stats.get_mod(this.stats.wis);
        } else if (skill_name.contains("欺瞒") || skill_name.contains("威吓") || skill_name.contains("表演")
                || skill_name.contains("游说")) {
            bonus = this.stats.get_mod(this.stats.cha);
        }

        if (this.job.skill_proficiencies.contains(skill_name)) {
            bonus += get_proficiency_bonus();
        }
        return bonus;
    }

    public int get_ability_modifier(String stat_name) {
        if ("Strength".equals(stat_name)) {
            return this.stats.get_mod(this.stats.str);
        }
        if ("Dexterity".equals(stat_name)) {
            return this.stats.get_mod(this.stats.dex);
        }
        if ("Constitution".equals(stat_name)) {
            return this.stats.get_mod(this.stats.con);
        }
        if ("Intelligence".equals(stat_name)) {
            return this.stats.get_mod(this.stats.intel);
        }
        if ("Wisdom".equals(stat_name)) {
            return this.stats.get_mod(this.stats.wis);
        }
        if ("Charisma".equals(stat_name)) {
            return this.stats.get_mod(this.stats.cha);
        }
        return 0;
    }
}
