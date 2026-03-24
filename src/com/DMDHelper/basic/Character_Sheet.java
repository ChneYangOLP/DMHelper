package com.DMDHelper.basic;

import com.DMDHelper.basic.playerclass.Character_Class;
import com.DMDHelper.basic.playerclass.Dnd5e_Progression;
import com.DMDHelper.basic.armor.Armor;
import com.DMDHelper.basic.equipment.Equipment_Item;
import com.DMDHelper.basic.equipment.Equipment_Library;
import com.DMDHelper.basic.equipment.Equipment_Slot;
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
    public List<String> owned_equipment_keys;
    public String equipped_armor_key;
    public String equipped_main_hand_key;
    public String equipped_off_hand_key;
    public String equipped_cloak_key;
    public String equipped_accessory_key;
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
        this.owned_equipment_keys = new ArrayList<>();

        initialize_default_equipment();

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
        String defaultKey = Equipment_Library.get_default_equipped_key(class_key, Equipment_Slot.ARMOR);
        Equipment_Item item = Equipment_Library.get_item(defaultKey);
        if (item == null) {
            return new Armor("普通旅行者服装", "None", 10);
        }
        return new Armor(item.display_name, item.armor_type, item.base_ac);
    }

    private void initialize_default_equipment() {
        this.owned_equipment_keys.clear();
        this.owned_equipment_keys.addAll(Equipment_Library.get_default_inventory(this.job.class_key));
        this.equipped_armor_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.ARMOR);
        this.equipped_main_hand_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.MAIN_HAND);
        this.equipped_off_hand_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.OFF_HAND);
        this.equipped_cloak_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.CLOAK);
        this.equipped_accessory_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.ACCESSORY);
        ensure_equipment_ownership(this.equipped_armor_key);
        ensure_equipment_ownership(this.equipped_main_hand_key);
        ensure_equipment_ownership(this.equipped_off_hand_key);
        ensure_equipment_ownership(this.equipped_cloak_key);
        ensure_equipment_ownership(this.equipped_accessory_key);
        sync_legacy_equipment_state();
    }

    private void ensure_equipment_ownership(String itemKey) {
        if (itemKey == null || itemKey.trim().isEmpty()) {
            return;
        }
        if (!this.owned_equipment_keys.contains(itemKey) && Equipment_Library.get_item(itemKey) != null) {
            this.owned_equipment_keys.add(itemKey);
        }
    }

    private void sync_legacy_equipment_state() {
        Equipment_Item armorItem = get_equipped_item(Equipment_Slot.ARMOR);
        if (armorItem == null) {
            this.equipped_armor = new Armor("普通旅行者服装", "None", 10);
        } else {
            this.equipped_armor = new Armor(armorItem.display_name, armorItem.armor_type, armorItem.base_ac);
        }
        Equipment_Item offHand = get_equipped_item(Equipment_Slot.OFF_HAND);
        this.has_shield = offHand != null && offHand.shield_bonus > 0;
    }

    public void set_equipment(Armor armor, boolean use_shield) {
        String armorKey = Equipment_Library.map_legacy_armor_key(armor);
        equip_item(Equipment_Slot.ARMOR, armorKey);
        if (use_shield) {
            ensure_equipment_ownership("shield");
            this.equipped_off_hand_key = "shield";
        } else if ("shield".equals(this.equipped_off_hand_key)) {
            this.equipped_off_hand_key = "";
        }
        sync_legacy_equipment_state();
        recalculate_derived_stats();
    }

    public void equip_item(Equipment_Slot slot, String itemKey) {
        if (itemKey != null && !itemKey.trim().isEmpty()) {
            Equipment_Item item = Equipment_Library.get_item(itemKey);
            if (item == null || item.slot != slot || !this.owned_equipment_keys.contains(itemKey)) {
                return;
            }
        }

        if (slot == Equipment_Slot.ARMOR) this.equipped_armor_key = itemKey == null ? "" : itemKey;
        else if (slot == Equipment_Slot.MAIN_HAND) this.equipped_main_hand_key = itemKey == null ? "" : itemKey;
        else if (slot == Equipment_Slot.OFF_HAND) this.equipped_off_hand_key = itemKey == null ? "" : itemKey;
        else if (slot == Equipment_Slot.CLOAK) this.equipped_cloak_key = itemKey == null ? "" : itemKey;
        else if (slot == Equipment_Slot.ACCESSORY) this.equipped_accessory_key = itemKey == null ? "" : itemKey;

        sync_legacy_equipment_state();
        recalculate_derived_stats();
    }

    public Equipment_Item get_equipped_item(Equipment_Slot slot) {
        String key = "";
        if (slot == Equipment_Slot.ARMOR) key = this.equipped_armor_key;
        else if (slot == Equipment_Slot.MAIN_HAND) key = this.equipped_main_hand_key;
        else if (slot == Equipment_Slot.OFF_HAND) key = this.equipped_off_hand_key;
        else if (slot == Equipment_Slot.CLOAK) key = this.equipped_cloak_key;
        else if (slot == Equipment_Slot.ACCESSORY) key = this.equipped_accessory_key;
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return Equipment_Library.get_item(key);
    }

    public List<Equipment_Item> get_owned_items_for_slot(Equipment_Slot slot) {
        return Equipment_Library.get_items_for_slot(this.owned_equipment_keys, slot);
    }

    public void restore_equipment_state(List<String> ownedKeys,
                                        String armorKey,
                                        String mainHandKey,
                                        String offHandKey,
                                        String cloakKey,
                                        String accessoryKey,
                                        Armor legacyArmor,
                                        boolean legacyShield) {
        this.owned_equipment_keys.clear();
        if (ownedKeys != null && !ownedKeys.isEmpty()) {
            this.owned_equipment_keys.addAll(ownedKeys);
        } else {
            this.owned_equipment_keys.addAll(Equipment_Library.get_default_inventory(this.job.class_key));
        }

        this.equipped_armor_key = normalize_equipped_key(Equipment_Slot.ARMOR, armorKey);
        this.equipped_main_hand_key = normalize_equipped_key(Equipment_Slot.MAIN_HAND, mainHandKey);
        this.equipped_off_hand_key = normalize_equipped_key(Equipment_Slot.OFF_HAND, offHandKey);
        this.equipped_cloak_key = normalize_equipped_key(Equipment_Slot.CLOAK, cloakKey);
        this.equipped_accessory_key = normalize_equipped_key(Equipment_Slot.ACCESSORY, accessoryKey);

        if (this.equipped_armor_key.isEmpty() && legacyArmor != null) {
            this.equipped_armor_key = Equipment_Library.map_legacy_armor_key(legacyArmor);
        }
        if (this.equipped_off_hand_key.isEmpty() && legacyShield) {
            this.equipped_off_hand_key = "shield";
        }
        if (this.equipped_main_hand_key.isEmpty()) {
            this.equipped_main_hand_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.MAIN_HAND);
        }
        if (this.equipped_cloak_key.isEmpty()) {
            this.equipped_cloak_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.CLOAK);
        }
        if (this.equipped_accessory_key.isEmpty()) {
            this.equipped_accessory_key = Equipment_Library.get_default_equipped_key(this.job.class_key, Equipment_Slot.ACCESSORY);
        }

        ensure_equipment_ownership(this.equipped_armor_key);
        ensure_equipment_ownership(this.equipped_main_hand_key);
        ensure_equipment_ownership(this.equipped_off_hand_key);
        ensure_equipment_ownership(this.equipped_cloak_key);
        ensure_equipment_ownership(this.equipped_accessory_key);

        sync_legacy_equipment_state();
        recalculate_derived_stats();
    }

    private String normalize_equipped_key(Equipment_Slot slot, String key) {
        if (key == null || key.trim().isEmpty()) {
            return "";
        }
        Equipment_Item item = Equipment_Library.get_item(key);
        if (item == null || item.slot != slot) {
            return "";
        }
        return key;
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
        sync_legacy_equipment_state();
        this.hp = calculate_max_hp();
        this.ac = calculate_armor_class();
    }

    private int calculate_max_hp() {
        int con_mod = this.stats.get_mod(this.stats.con);
        int baseHp = this.job.hp_dice + con_mod;
        int higherLevels = Math.max(0, this.job.current_level - 1);
        int totalHp = baseHp + higherLevels * (this.job.get_average_hp_gain() + con_mod);
        totalHp += this.job.current_level * this.job.get_extra_hit_points_per_level();

        if (this.job.has_feat("Tough")) {
            totalHp += this.job.current_level * 2;
        }
        return Math.max(this.job.current_level, totalHp);
    }

    private int calculate_armor_class() {
        sync_legacy_equipment_state();
        int dex_mod = this.stats.get_mod(this.stats.dex);
        int calculatedAc = this.equipped_armor.base_ac;

        if ("None".equals(this.equipped_armor.armor_type) || "Light".equals(this.equipped_armor.armor_type)) {
            calculatedAc += dex_mod;
        } else if ("Medium".equals(this.equipped_armor.armor_type)) {
            calculatedAc += Math.min(dex_mod, 2);
        }

        Equipment_Item offHand = get_equipped_item(Equipment_Slot.OFF_HAND);
        if (offHand != null) {
            calculatedAc += offHand.shield_bonus;
        }

        calculatedAc += this.job.get_extra_armor_class_bonus(this.equipped_armor.armor_type);

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
