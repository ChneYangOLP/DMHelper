package com.DMHelper.basic;

import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.Dnd5e_Progression;
import com.DMHelper.basic.armor.Armor;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.DMHelper.basic.race.Character_Race;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色核心数据模型。
 * 这个类聚合了角色的基础信息、种族、职业、装备、成长记录以及需要持久化的当前状态。
 */
public class Character_Sheet {
    public int database_id;
    public String name;
    public int age;
    public String gender;

    public Character_Race race;
    public Character_Class job;
    public Stats stats;

    public int hp;
    public int current_hp;
    public int max_hit_dice;
    public int available_hit_dice;
    public int ac;
    public int experience_points;
    public int gold_pieces;
    public int silver_pieces;
    public int copper_pieces;

    public Armor equipped_armor;
    public boolean has_shield;
    public List<String> owned_equipment_keys;
    public Map<String, Integer> inventory_item_counts;
    public String equipped_armor_key;
    public String equipped_main_hand_key;
    public String equipped_off_hand_key;
    public String equipped_cloak_key;
    public String equipped_accessory_key;
    public String background_story;
    public String personality_traits;
    public String ideals;
    public String bonds;
    public String flaws;
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
        this.gold_pieces = 0;
        this.silver_pieces = 0;
        this.copper_pieces = 0;
        this.advancement_notes = new ArrayList<>();
        this.owned_equipment_keys = new ArrayList<>();
        this.inventory_item_counts = new LinkedHashMap<>();
        this.background_story = "";
        this.personality_traits = "";
        this.ideals = "";
        this.bonds = "";
        this.flaws = "";

        initialize_default_equipment();

        this.job.rebuild_progression();
        recalculate_derived_stats();
        this.current_hp = this.hp;
    }

    public static Character_Sheet create_new_character(String name,
                                                       int age,
                                                       String gender,
                                                       Character_Race race,
                                                       Character_Class job,
                                                       Stats raw_stats) {
        // 新建角色时直接把种族加值并入最终属性，后续读档则不再重复叠加。
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
        // 读档数据已经是最终属性，因此这里不再重新计算种族加值。
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
        // 默认装备来自职业模板，同时把默认穿戴物也放进“已拥有”列表，避免装备页出现可穿却不持有的状态。
        this.owned_equipment_keys.clear();
        this.inventory_item_counts.clear();
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
        ensure_inventory_count(itemKey, 1);
    }

    public void add_item_to_inventory(String itemKey) {
        Equipment_Item item = Equipment_Library.get_item(itemKey);
        if (item == null) {
            return;
        }
        if (item.is_stackable()) {
            int currentCount = get_item_count(itemKey);
            ensure_equipment_ownership(itemKey);
            set_item_count(itemKey, currentCount <= 0 ? 1 : currentCount + 1);
            return;
        }
        ensure_equipment_ownership(itemKey);
    }

    public boolean remove_item_from_inventory(String itemKey) {
        if (itemKey == null || itemKey.trim().isEmpty()) {
            return false;
        }
        Equipment_Item item = Equipment_Library.get_item(itemKey);
        if (item != null && item.is_stackable() && get_item_count(itemKey) > 1) {
            set_item_count(itemKey, get_item_count(itemKey) - 1);
            return true;
        }
        boolean removed = this.owned_equipment_keys.remove(itemKey);
        if (!removed) {
            return false;
        }
        this.inventory_item_counts.remove(itemKey);
        if (itemKey.equals(this.equipped_armor_key)) this.equipped_armor_key = "";
        if (itemKey.equals(this.equipped_main_hand_key)) this.equipped_main_hand_key = "";
        if (itemKey.equals(this.equipped_off_hand_key)) this.equipped_off_hand_key = "";
        if (itemKey.equals(this.equipped_cloak_key)) this.equipped_cloak_key = "";
        if (itemKey.equals(this.equipped_accessory_key)) this.equipped_accessory_key = "";
        sync_legacy_equipment_state();
        recalculate_derived_stats();
        return true;
    }

    public int get_total_currency_cp() {
        return Math.max(0, this.gold_pieces) * 100 + Math.max(0, this.silver_pieces) * 10 + Math.max(0, this.copper_pieces);
    }

    public void add_currency_cp(int amountCp) {
        if (amountCp <= 0) {
            return;
        }
        set_currency_from_cp(get_total_currency_cp() + amountCp);
    }

    public boolean spend_currency_cp(int amountCp) {
        if (amountCp <= 0) {
            return true;
        }
        int currentTotal = get_total_currency_cp();
        if (currentTotal < amountCp) {
            return false;
        }
        set_currency_from_cp(currentTotal - amountCp);
        return true;
    }

    public String get_currency_summary() {
        return this.gold_pieces + " gp / " + this.silver_pieces + " sp / " + this.copper_pieces + " cp";
    }

    private void set_currency_from_cp(int totalCp) {
        int safeTotal = Math.max(0, totalCp);
        this.gold_pieces = safeTotal / 100;
        this.silver_pieces = (safeTotal % 100) / 10;
        this.copper_pieces = safeTotal % 10;
    }

    public int get_item_count(String itemKey) {
        if (itemKey == null || itemKey.trim().isEmpty()) {
            return 0;
        }
        Integer count = this.inventory_item_counts.get(itemKey);
        return count == null ? (this.owned_equipment_keys.contains(itemKey) ? 1 : 0) : Math.max(0, count);
    }

    private void ensure_inventory_count(String itemKey, int defaultCount) {
        if (itemKey == null || itemKey.trim().isEmpty()) {
            return;
        }
        if (!this.inventory_item_counts.containsKey(itemKey)) {
            this.inventory_item_counts.put(itemKey, Math.max(1, defaultCount));
        }
    }

    private void set_item_count(String itemKey, int count) {
        if (itemKey == null || itemKey.trim().isEmpty()) {
            return;
        }
        this.inventory_item_counts.put(itemKey, Math.max(1, count));
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
                                        Map<String, Integer> inventoryCounts,
                                        String armorKey,
                                        String mainHandKey,
                                        String offHandKey,
                                        String cloakKey,
                                        String accessoryKey,
                                        Armor legacyArmor,
                                        boolean legacyShield) {
        // 这个方法同时承担“新系统槽位恢复”和“旧版护甲/盾牌字段兼容恢复”两项工作。
        this.owned_equipment_keys.clear();
        this.inventory_item_counts.clear();
        if (ownedKeys != null && !ownedKeys.isEmpty()) {
            this.owned_equipment_keys.addAll(ownedKeys);
        } else {
            this.owned_equipment_keys.addAll(Equipment_Library.get_default_inventory(this.job.class_key));
        }
        if (inventoryCounts != null && !inventoryCounts.isEmpty()) {
            for (Map.Entry<String, Integer> entry : inventoryCounts.entrySet()) {
                if (Equipment_Library.get_item(entry.getKey()) != null) {
                    this.inventory_item_counts.put(entry.getKey(), Math.max(1, entry.getValue()));
                }
            }
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
        for (String itemKey : this.owned_equipment_keys) {
            ensure_inventory_count(itemKey, 1);
        }

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

    public void take_long_rest() {
        recalculate_derived_stats();
        this.current_hp = this.hp;
        recover_hit_dice_after_long_rest();
        this.job.restore_long_rest_resources();
        if (this.job instanceof Bard_Class) {
            ((Bard_Class) this.job).sync_charisma_resource_caps(this.stats.get_mod(this.stats.cha), true);
        }
        if (this.job instanceof Paladin_Class) {
            ((Paladin_Class) this.job).sync_charisma_resource_caps(this.stats.get_mod(this.stats.cha), true);
        }
        record_advancement("完成一次长休：恢复生命值、法术位与职业资源到完整状态，并回补部分生命骰。");
    }

    public int take_short_rest(int spentHitDice, int recoveredHp) {
        int beforeHp = this.current_hp;
        int actualSpentHitDice = Math.max(0, Math.min(spentHitDice, this.available_hit_dice));
        this.available_hit_dice -= actualSpentHitDice;
        set_current_hp(this.current_hp + Math.max(0, recoveredHp));
        this.job.restore_short_rest_resources();
        int actualRecoveredHp = this.current_hp - beforeHp;
        record_advancement("完成一次短休：消耗生命骰 " + actualSpentHitDice + " 颗，恢复生命值 "
                + actualRecoveredHp + " 点，并结算短休资源恢复。");
        return actualRecoveredHp;
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
        int previousMaxHp = this.hp;
        int previousMaxHitDice = this.max_hit_dice;
        this.job.rebuild_progression();
        if (this.job instanceof Bard_Class) {
            ((Bard_Class) this.job).sync_charisma_resource_caps(this.stats.get_mod(this.stats.cha), false);
        }
        if (this.job instanceof Paladin_Class) {
            ((Paladin_Class) this.job).sync_charisma_resource_caps(this.stats.get_mod(this.stats.cha), false);
        }
        sync_legacy_equipment_state();
        this.hp = calculate_max_hp();
        this.max_hit_dice = Math.max(1, this.job.current_level);
        sync_hit_dice_progress(previousMaxHitDice);
        this.ac = calculate_armor_class();
        // 升级、换装、专长变化后最大 HP 可能变化，这里尽量保留“当前血量相对进度”。
        if (previousMaxHp <= 0) {
            this.current_hp = this.hp;
        } else {
            int hpDelta = this.hp - previousMaxHp;
            this.current_hp = Math.max(0, Math.min(this.current_hp + hpDelta, this.hp));
        }
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

    public void set_current_hp(int value) {
        this.current_hp = Math.max(0, Math.min(value, this.hp));
    }

    public void set_available_hit_dice(int value) {
        this.available_hit_dice = Math.max(0, Math.min(value, this.max_hit_dice));
    }

    public String get_hp_summary() {
        return this.current_hp + "/" + this.hp;
    }

    public String get_hit_dice_summary() {
        return this.available_hit_dice + "/" + this.max_hit_dice + " 颗 d" + this.job.hp_dice;
    }

    public boolean is_alive() {
        return this.current_hp > 0;
    }

    public void sync_from_combatant(Combatant combatant) {
        if (combatant == null) {
            return;
        }
        // 战斗系统里的资源是临时 Combatant 状态，战斗后要回写回角色本体和数据库。
        set_current_hp(combatant.current_hp);
        this.job.sync_from_combatant(combatant);
    }

    public int get_proficiency_bonus() {
        return (this.job.current_level - 1) / 4 + 2;
    }

    private void sync_hit_dice_progress(int previousMaxHitDice) {
        if (previousMaxHitDice <= 0) {
            this.available_hit_dice = this.max_hit_dice;
            return;
        }
        int gainedHitDice = Math.max(0, this.max_hit_dice - previousMaxHitDice);
        this.available_hit_dice = Math.max(0, Math.min(this.available_hit_dice + gainedHitDice, this.max_hit_dice));
    }

    private void recover_hit_dice_after_long_rest() {
        int recovery = Math.max(1, this.max_hit_dice / 2);
        this.available_hit_dice = Math.min(this.max_hit_dice, this.available_hit_dice + recovery);
    }

    public int get_initiative_modifier() {
        return this.stats.get_mod(this.stats.dex) + this.job.get_initiative_bonus();
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
        boolean proficient = this.job.skill_proficiencies.contains(skill_name);
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

        if (proficient) {
            bonus += get_proficiency_bonus();
            if (this.job.has_skill_expertise(skill_name)) {
                bonus += get_proficiency_bonus();
            }
        } else {
            bonus += this.job.get_untrained_ability_check_bonus();
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
