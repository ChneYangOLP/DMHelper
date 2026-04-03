package com.DMHelper.basic.playerclass.bard;

import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.database.Persistence_Util;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Bard_Class extends Character_Class {
    private static final int[][] BARD_SLOT_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 2, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 3, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 4, 2, 0, 0, 0, 0, 0, 0, 0},
            {0, 4, 3, 0, 0, 0, 0, 0, 0, 0},
            {0, 4, 3, 2, 0, 0, 0, 0, 0, 0},
            {0, 4, 3, 3, 0, 0, 0, 0, 0, 0},
            {0, 4, 3, 3, 1, 0, 0, 0, 0, 0},
            {0, 4, 3, 3, 2, 0, 0, 0, 0, 0},
            {0, 4, 3, 3, 3, 1, 0, 0, 0, 0},
            {0, 4, 3, 3, 3, 2, 0, 0, 0, 0},
            {0, 4, 3, 3, 3, 2, 1, 0, 0, 0},
            {0, 4, 3, 3, 3, 2, 1, 0, 0, 0},
            {0, 4, 3, 3, 3, 2, 1, 1, 0, 0},
            {0, 4, 3, 3, 3, 2, 1, 1, 0, 0},
            {0, 4, 3, 3, 3, 2, 1, 1, 1, 0},
            {0, 4, 3, 3, 3, 2, 1, 1, 1, 0},
            {0, 4, 3, 3, 3, 2, 1, 1, 1, 1},
            {0, 4, 3, 3, 3, 3, 1, 1, 1, 1},
            {0, 4, 3, 3, 3, 3, 2, 1, 1, 1},
            {0, 4, 3, 3, 3, 3, 2, 2, 1, 1}
    };

    private static final int[] BARD_CANTRIPS_TABLE = {
            0, 2, 2, 2, 3, 3, 3, 3, 3, 3,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4
    };

    private static final int[] BARD_BASE_SPELLS_TABLE = {
            0, 4, 5, 6, 7, 8, 9, 10, 11, 12,
            14, 15, 15, 16, 18, 19, 19, 20, 22, 22, 22
    };

    private static final List<String> ALL_SKILLS = Arrays.asList(
            "Acrobatics (杂技)", "Animal Handling (驯兽)", "Arcana (奥秘)", "Athletics (运动)",
            "Deception (欺瞒)", "History (历史)", "Insight (洞悉)", "Intimidation (威吓)",
            "Investigation (调查)", "Medicine (医药)", "Nature (自然)", "Perception (察觉)",
            "Performance (表演)", "Persuasion (游说)", "Religion (宗教)", "Sleight of Hand (巧手)",
            "Stealth (隐匿)", "Survival (生存)"
    );

    public Bard_College bard_college;
    public List<String> traits;
    public int[] spell_slots;
    public int[] current_spell_slots;
    public int cantrips_known;
    public int base_spells_known_count;
    public int magical_secrets_count;
    public int spells_known_count;
    public int bardic_inspiration_die_size;
    public int bardic_inspiration_uses;
    public int current_bardic_inspiration_uses;
    public int song_of_rest_die_size;
    public int attacks_per_action;
    public int pending_asi_count;
    public int pending_expertise_choices;
    public int pending_bonus_skill_choices;
    public int pending_magical_secret_choices;
    public List<String> expertise_skill_keys;
    public List<String> lore_bonus_skill_keys;
    public List<String> known_cantrip_keys;
    public List<String> known_spell_keys;
    public List<String> magical_secret_spell_keys;
    private boolean preserve_loaded_current_slots;
    private boolean preserve_loaded_bardic_inspiration;

    public Bard_Class() {
        super("BARD", "吟游诗人 (Bard)", 8);
        this.bard_college = Bard_College.NONE;
        this.traits = new ArrayList<>();
        this.spell_slots = new int[10];
        this.current_spell_slots = new int[10];
        this.expertise_skill_keys = new ArrayList<>();
        this.lore_bonus_skill_keys = new ArrayList<>();
        this.known_cantrip_keys = new ArrayList<>();
        this.known_spell_keys = new ArrayList<>();
        this.magical_secret_spell_keys = new ArrayList<>();
        this.skill_choose_count = 3;
        this.skill_options.addAll(ALL_SKILLS);

        this.saving_throws.add("Dexterity");
        this.saving_throws.add("Charisma");
        this.equipment_proficiencies.add("轻甲");
        this.equipment_proficiencies.add("简易武器");
        this.equipment_proficiencies.add("手弩");
        this.equipment_proficiencies.add("长剑");
        this.equipment_proficiencies.add("刺剑");
        this.equipment_proficiencies.add("短剑");
        this.equipment_proficiencies.add("乐器（三种）");
    }

    @Override
    public void rebuild_progression() {
        int[] previousMaxSlots = this.spell_slots == null ? new int[10] : this.spell_slots.clone();
        this.traits.clear();
        reset_dynamic_proficiencies();
        this.spell_slots = BARD_SLOT_TABLE[Math.max(1, Math.min(this.current_level, 20))].clone();
        sync_current_spell_slots(previousMaxSlots);
        this.cantrips_known = BARD_CANTRIPS_TABLE[this.current_level];
        // 普通已知法术和魔法秘辛分开统计，升级时才能正确区分“本职法术”与“跨职业额外法术”。
        this.base_spells_known_count = BARD_BASE_SPELLS_TABLE[this.current_level];
        this.magical_secrets_count = get_expected_magical_secrets_count();
        this.spells_known_count = this.base_spells_known_count + this.magical_secrets_count;
        this.bardic_inspiration_die_size = get_bardic_inspiration_die();
        this.song_of_rest_die_size = get_song_of_rest_die();
        this.attacks_per_action = this.bard_college == Bard_College.VALOR && this.current_level >= 6 ? 2 : 1;
        this.pending_asi_count = Math.max(0, get_earned_asi_choices() - this.used_asi_choices);
        this.pending_expertise_choices = Math.max(0, get_expected_expertise_count() - this.expertise_skill_keys.size());
        this.pending_bonus_skill_choices = Math.max(0, get_expected_lore_bonus_skill_count() - this.lore_bonus_skill_keys.size());
        this.pending_magical_secret_choices = Math.max(0, this.magical_secrets_count - this.magical_secret_spell_keys.size());

        ensure_known_cantrips();
        ensure_known_spells();
        trim_known_lists();
        apply_base_traits();
        apply_college_traits();
    }

    private void reset_dynamic_proficiencies() {
        this.equipment_proficiencies.clear();
        this.equipment_proficiencies.add("轻甲");
        this.equipment_proficiencies.add("简易武器");
        this.equipment_proficiencies.add("手弩");
        this.equipment_proficiencies.add("长剑");
        this.equipment_proficiencies.add("刺剑");
        this.equipment_proficiencies.add("短剑");
        this.equipment_proficiencies.add("乐器（三种）");
        if (this.bard_college == Bard_College.VALOR && this.current_level >= 3) {
            this.equipment_proficiencies.add("中甲");
            this.equipment_proficiencies.add("盾牌");
            this.equipment_proficiencies.add("军用武器");
        }
    }

    private void sync_current_spell_slots(int[] previousMaxSlots) {
        if (this.current_spell_slots == null || this.current_spell_slots.length != this.spell_slots.length) {
            this.current_spell_slots = this.spell_slots.clone();
            this.preserve_loaded_current_slots = false;
            return;
        }

        for (int spellLevel = 1; spellLevel < this.spell_slots.length; spellLevel++) {
            if (!this.preserve_loaded_current_slots
                    && previousMaxSlots[spellLevel] == 0
                    && this.spell_slots[spellLevel] > 0
                    && this.current_spell_slots[spellLevel] == 0) {
                this.current_spell_slots[spellLevel] = this.spell_slots[spellLevel];
            }
            this.current_spell_slots[spellLevel] = Math.max(0, Math.min(this.current_spell_slots[spellLevel], this.spell_slots[spellLevel]));
        }
        this.preserve_loaded_current_slots = false;
    }

    private void apply_base_traits() {
        this.traits.add("施法 (Spellcasting)：以魅力作为施法关键属性，使用已知法术进行施法。");
        this.traits.add("吟游激励 (Bardic Inspiration)：可将灵感骰交给盟友，用于强化攻击、检定或豁免。");
        if (this.current_level >= 2) {
            this.traits.add("包打听 (Jack of All Trades)：未熟练的属性检定获得半个熟练加值。");
            this.traits.add("休憩之歌 (Song of Rest)：短休时为队友额外恢复 1d" + this.song_of_rest_die_size + " 生命值。");
        }
        if (this.current_level >= 3) {
            this.traits.add("专精 (Expertise)：将两项已熟练技能的熟练加值翻倍。");
        }
        if (this.current_level >= 5) {
            this.traits.add("灵感泉涌 (Font of Inspiration)：吟游激励在短休或长休后恢复。");
        }
        if (this.current_level >= 6) {
            this.traits.add("反制魅音 (Countercharm)：用表演帮助盟友对抗魅惑与恐惧。");
        }
        if (this.magical_secrets_count > 0) {
            this.traits.add("魔法秘辛 (Magical Secrets)：可从任意职业法术表学习额外法术。");
        }
        if (this.current_level >= 20) {
            this.traits.add("至高灵感 (Superior Inspiration)：若掷先攻时没有灵感骰，会恢复 1 枚。");
        }
    }

    private void apply_college_traits() {
        if (this.bard_college == Bard_College.LORE) {
            if (this.current_level >= 3) {
                this.traits.add("学识学院 3级 - 额外熟练 (Bonus Proficiencies)：再获得 3 项技能熟练。");
                this.traits.add("学识学院 3级 - 犀利辞锋 (Cutting Words)：消耗灵感骰削弱敌人的攻击、伤害或检定。");
            }
            if (this.current_level >= 6) {
                this.traits.add("学识学院 6级 - 额外魔法秘辛 (Additional Magical Secrets)：提前学习两项跨职业法术。");
            }
            if (this.current_level >= 14) {
                this.traits.add("学识学院 14级 - 无与伦比的技巧 (Peerless Skill)：检定后可用灵感骰强化自己。");
            }
        } else if (this.bard_college == Bard_College.VALOR) {
            if (this.current_level >= 3) {
                this.traits.add("勇气学院 3级 - 额外熟练 (Bonus Proficiencies)：获得中甲、盾牌与军用武器熟练。");
                this.traits.add("勇气学院 3级 - 战斗激励 (Combat Inspiration)：灵感骰也可用于提高伤害或 AC。");
            }
            if (this.current_level >= 6) {
                this.traits.add("勇气学院 6级 - 额外攻击 (Extra Attack)：攻击动作可以攻击两次。");
            }
            if (this.current_level >= 14) {
                this.traits.add("勇气学院 14级 - 战斗施法 (Battle Magic)：施放法术后可用附赠动作进行武器攻击。");
            }
        }
    }

    private void ensure_known_cantrips() {
        if (!this.known_cantrip_keys.isEmpty()) {
            return;
        }
        for (String spellKey : Spell_Library.get_bard_cantrip_keys()) {
            if (this.known_cantrip_keys.size() >= this.cantrips_known) {
                break;
            }
            this.known_cantrip_keys.add(spellKey);
        }
    }

    private void ensure_known_spells() {
        if (!this.known_spell_keys.isEmpty()) {
            return;
        }
        for (String spellKey : Spell_Library.get_bard_spell_keys_up_to_level(get_max_spell_level())) {
            if (this.known_spell_keys.size() >= this.base_spells_known_count) {
                break;
            }
            this.known_spell_keys.add(spellKey);
        }
    }

    private void trim_known_lists() {
        while (this.known_cantrip_keys.size() > this.cantrips_known) {
            this.known_cantrip_keys.remove(this.known_cantrip_keys.size() - 1);
        }
        while (this.known_spell_keys.size() > this.base_spells_known_count) {
            this.known_spell_keys.remove(this.known_spell_keys.size() - 1);
        }
        while (this.magical_secret_spell_keys.size() > this.magical_secrets_count) {
            this.magical_secret_spell_keys.remove(this.magical_secret_spell_keys.size() - 1);
        }
        this.known_spell_keys.removeAll(this.magical_secret_spell_keys);
    }

    public void sync_charisma_resource_caps(int charismaModifier, boolean restoreToFull) {
        int maxUses = get_bardic_inspiration_uses(charismaModifier);
        this.bardic_inspiration_uses = maxUses;
        // 吟游激励次数受魅力调整值影响，因此属性提升后需要重新夹紧当前值。
        if (restoreToFull || (!this.preserve_loaded_bardic_inspiration && this.current_bardic_inspiration_uses <= 0)) {
            this.current_bardic_inspiration_uses = maxUses;
        } else {
            this.current_bardic_inspiration_uses = Math.max(0, Math.min(this.current_bardic_inspiration_uses, maxUses));
        }
        this.preserve_loaded_bardic_inspiration = false;
    }

    public int get_earned_asi_choices() {
        int count = 0;
        int[] levels = {4, 8, 12, 16, 19};
        for (int level : levels) {
            if (this.current_level >= level) {
                count++;
            }
        }
        return count;
    }

    public int get_expected_expertise_count() {
        int count = 0;
        if (this.current_level >= 3) {
            count += 2;
        }
        if (this.current_level >= 10) {
            count += 2;
        }
        return count;
    }

    public int get_expected_lore_bonus_skill_count() {
        return this.bard_college == Bard_College.LORE && this.current_level >= 3 ? 3 : 0;
    }

    public int get_expected_magical_secrets_count() {
        int count = 0;
        if (this.bard_college == Bard_College.LORE && this.current_level >= 6) {
            count += 2;
        }
        if (this.current_level >= 10) {
            count += 2;
        }
        if (this.current_level >= 14) {
            count += 2;
        }
        if (this.current_level >= 18) {
            count += 2;
        }
        return count;
    }

    private int get_bardic_inspiration_die() {
        if (this.current_level >= 15) return 12;
        if (this.current_level >= 10) return 10;
        if (this.current_level >= 5) return 8;
        return 6;
    }

    private int get_song_of_rest_die() {
        if (this.current_level < 2) return 0;
        if (this.current_level >= 17) return 12;
        if (this.current_level >= 13) return 10;
        if (this.current_level >= 9) return 8;
        return 6;
    }

    public int get_bardic_inspiration_uses(int charismaModifier) {
        return Math.max(1, charismaModifier);
    }

    public int get_max_spell_level() {
        for (int spellLevel = 9; spellLevel >= 1; spellLevel--) {
            if (this.spell_slots[spellLevel] > 0) {
                return spellLevel;
            }
        }
        return 1;
    }

    public List<String> get_available_cantrip_options() {
        List<String> options = Spell_Library.get_bard_cantrip_keys();
        options.removeAll(this.known_cantrip_keys);
        return options;
    }

    public List<String> get_available_spell_options() {
        List<String> options = Spell_Library.get_bard_spell_keys_up_to_level(get_max_spell_level());
        options.removeAll(this.known_spell_keys);
        options.removeAll(this.magical_secret_spell_keys);
        return options;
    }

    public List<String> get_available_magical_secret_options() {
        List<String> options = Spell_Library.get_all_spell_keys_up_to_level(get_max_spell_level());
        options.removeAll(this.known_spell_keys);
        options.removeAll(this.magical_secret_spell_keys);
        return options;
    }

    public List<String> get_available_expertise_options() {
        List<String> options = new ArrayList<>(this.skill_proficiencies);
        options.removeAll(this.expertise_skill_keys);
        return options;
    }

    public List<String> get_available_bonus_skill_options() {
        List<String> options = new ArrayList<>(ALL_SKILLS);
        options.removeAll(this.skill_proficiencies);
        return options;
    }

    public void add_expertise_skills(List<String> skills) {
        for (String skill : skills) {
            if (this.skill_proficiencies.contains(skill)
                    && !this.expertise_skill_keys.contains(skill)
                    && this.expertise_skill_keys.size() < get_expected_expertise_count()) {
                this.expertise_skill_keys.add(skill);
            }
        }
    }

    public void add_lore_bonus_skills(List<String> skills) {
        for (String skill : skills) {
            if (!this.skill_proficiencies.contains(skill)
                    && !this.lore_bonus_skill_keys.contains(skill)
                    && this.lore_bonus_skill_keys.size() < get_expected_lore_bonus_skill_count()) {
                this.skill_proficiencies.add(skill);
                this.lore_bonus_skill_keys.add(skill);
            }
        }
    }

    public void add_magical_secret_spells(List<String> spellKeys) {
        for (String spellKey : spellKeys) {
            if (!this.known_spell_keys.contains(spellKey)
                    && !this.magical_secret_spell_keys.contains(spellKey)
                    && this.magical_secret_spell_keys.size() < this.magical_secrets_count) {
                this.magical_secret_spell_keys.add(spellKey);
            }
        }
    }

    public void set_known_cantrips(List<String> spellKeys) {
        this.known_cantrip_keys.clear();
        for (String key : spellKeys) {
            if (!this.known_cantrip_keys.contains(key) && this.known_cantrip_keys.size() < this.cantrips_known) {
                this.known_cantrip_keys.add(key);
            }
        }
    }

    public void set_known_spells(List<String> spellKeys) {
        this.known_spell_keys.clear();
        for (String key : spellKeys) {
            if (!this.magical_secret_spell_keys.contains(key)
                    && !this.known_spell_keys.contains(key)
                    && this.known_spell_keys.size() < this.base_spells_known_count) {
                this.known_spell_keys.add(key);
            }
        }
    }

    public List<String> get_all_known_spell_keys() {
        List<String> all = new ArrayList<>(this.known_spell_keys);
        for (String key : this.magical_secret_spell_keys) {
            if (!all.contains(key)) {
                all.add(key);
            }
        }
        return all;
    }

    public List<String> get_known_cantrip_lines() {
        return get_spell_lines(this.known_cantrip_keys);
    }

    public List<String> get_known_spell_lines() {
        return get_spell_lines(get_all_known_spell_keys());
    }

    private List<String> get_spell_lines(List<String> spellKeys) {
        List<String> lines = new ArrayList<>();
        for (String spellKey : spellKeys) {
            Spell_Definition spell = Spell_Library.get_spell(spellKey);
            if (spell != null) {
                lines.add(spell.to_detail_line());
            }
        }
        return lines;
    }

    @Override
    public boolean has_skill_expertise(String skill_name) {
        return this.expertise_skill_keys.contains(skill_name);
    }

    @Override
    public int get_untrained_ability_check_bonus() {
        return this.current_level >= 2 ? get_current_proficiency_bonus() / 2 : 0;
    }

    @Override
    public int get_initiative_bonus() {
        return get_untrained_ability_check_bonus();
    }

    @Override
    public void level_up(int target_level) {
        if (target_level <= this.current_level || target_level > 20) {
            return;
        }
        this.current_level = target_level;
        rebuild_progression();
    }

    @Override
    public int get_average_hp_gain() {
        return 5;
    }

    @Override
    public String get_subclass_name() {
        if (this.bard_college == Bard_College.LORE) {
            return "学识学院";
        }
        if (this.bard_college == Bard_College.VALOR) {
            return "勇气学院";
        }
        return "未选择";
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>(this.traits);
        summaries.add("吟游激励骰： d" + this.bardic_inspiration_die_size);
        summaries.add("吟游激励次数： " + this.current_bardic_inspiration_uses + "/" + this.bardic_inspiration_uses);
        if (this.song_of_rest_die_size > 0) {
            summaries.add("休憩之歌： d" + this.song_of_rest_die_size);
        }
        summaries.add("戏法已知数： " + this.cantrips_known);
        summaries.add("已知法术数： " + this.spells_known_count + "（其中魔法秘辛 " + this.magical_secrets_count + "）");
        if (!this.expertise_skill_keys.isEmpty()) {
            summaries.add("专精技能： " + String.join("、", this.expertise_skill_keys));
        }
        if (!this.magical_secret_spell_keys.isEmpty()) {
            summaries.add("魔法秘辛法术： " + String.join("、", describe_spell_names(this.magical_secret_spell_keys)));
        }
        if (!this.feat_names.isEmpty()) {
            for (String featName : this.feat_names) {
                summaries.add("专长 - " + Feat_Library.get_summary_line(featName));
            }
        }
        summaries.add(get_spell_slot_summary());
        return summaries;
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();
        if (this.current_level >= 3 && this.bard_college == Bard_College.NONE) {
            pending.add("选择吟游学院");
        }
        if (this.pending_bonus_skill_choices > 0) {
            pending.add("选择 " + this.pending_bonus_skill_choices + " 项学院额外技能");
        }
        int pendingCantrips = Math.max(0, this.cantrips_known - this.known_cantrip_keys.size());
        if (pendingCantrips > 0) {
            pending.add("选择 " + pendingCantrips + " 个吟游诗人戏法");
        }
        int pendingSpells = Math.max(0, this.base_spells_known_count - this.known_spell_keys.size());
        if (pendingSpells > 0) {
            pending.add("选择 " + pendingSpells + " 个吟游诗人法术");
        }
        if (this.pending_expertise_choices > 0) {
            pending.add("选择 " + this.pending_expertise_choices + " 项专精技能");
        }
        if (this.pending_magical_secret_choices > 0) {
            pending.add("选择 " + this.pending_magical_secret_choices + " 个魔法秘辛法术");
        }
        if (this.pending_asi_count > 0) {
            pending.add("处理 " + this.pending_asi_count + " 次属性值提升/专长");
        }
        return pending;
    }

    @Override
    public Map<String, String> export_class_state() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("college", this.bard_college.name());
        state.put("expertise_skills", Persistence_Util.encode_list(this.expertise_skill_keys));
        state.put("lore_bonus_skills", Persistence_Util.encode_list(this.lore_bonus_skill_keys));
        state.put("known_cantrips", Persistence_Util.encode_list(this.known_cantrip_keys));
        state.put("known_spells", Persistence_Util.encode_list(this.known_spell_keys));
        state.put("magical_secrets", Persistence_Util.encode_list(this.magical_secret_spell_keys));
        state.put("current_spell_slots", Persistence_Util.encode_int_array(this.current_spell_slots));
        state.put("current_bardic_inspiration", Integer.toString(this.current_bardic_inspiration_uses));
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String collegeValue = class_state.get("college");
        if (collegeValue != null && !collegeValue.trim().isEmpty()) {
            this.bard_college = Bard_College.valueOf(collegeValue);
        }
        this.expertise_skill_keys.clear();
        this.expertise_skill_keys.addAll(Persistence_Util.decode_list(class_state.get("expertise_skills")));
        this.lore_bonus_skill_keys.clear();
        this.lore_bonus_skill_keys.addAll(Persistence_Util.decode_list(class_state.get("lore_bonus_skills")));
        this.known_cantrip_keys.clear();
        this.known_cantrip_keys.addAll(Persistence_Util.decode_list(class_state.get("known_cantrips")));
        this.known_spell_keys.clear();
        this.known_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("known_spells")));
        this.magical_secret_spell_keys.clear();
        this.magical_secret_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("magical_secrets")));
        if (class_state.containsKey("current_spell_slots")) {
            this.current_spell_slots = Persistence_Util.decode_int_array(class_state.get("current_spell_slots"), 10);
            this.preserve_loaded_current_slots = true;
        }
        if (class_state.containsKey("current_bardic_inspiration")) {
            try {
                this.current_bardic_inspiration_uses = Integer.parseInt(class_state.get("current_bardic_inspiration"));
                this.preserve_loaded_bardic_inspiration = true;
            } catch (NumberFormatException ignored) {
                this.current_bardic_inspiration_uses = 0;
            }
        }
        rebuild_progression();
    }

    public String get_spell_slot_summary() {
        StringBuilder sb = new StringBuilder("法术位：");
        boolean hasAny = false;
        for (int spellLevel = 1; spellLevel < this.spell_slots.length; spellLevel++) {
            if (this.spell_slots[spellLevel] > 0) {
                if (hasAny) {
                    sb.append(" | ");
                }
                sb.append(spellLevel).append("环 ").append(this.current_spell_slots[spellLevel]).append("/").append(this.spell_slots[spellLevel]);
                hasAny = true;
            }
        }
        return hasAny ? sb.toString() : "法术位：暂无";
    }

    public String get_bardic_inspiration_summary() {
        return this.current_bardic_inspiration_uses + "/" + this.bardic_inspiration_uses + "（d" + this.bardic_inspiration_die_size + "）";
    }

    @Override
    public void restore_short_rest_resources() {
        if (this.current_level >= 5) {
            this.current_bardic_inspiration_uses = this.bardic_inspiration_uses;
        }
    }

    @Override
    public void restore_long_rest_resources() {
        this.current_spell_slots = this.spell_slots.clone();
        this.current_bardic_inspiration_uses = this.bardic_inspiration_uses;
    }

    @Override
    public void sync_from_combatant(Combatant combatant) {
        if (combatant == null) {
            return;
        }
        if (combatant.spell_slots_remaining != null) {
            for (int spellLevel = 1; spellLevel < this.current_spell_slots.length; spellLevel++) {
                int current = spellLevel < combatant.spell_slots_remaining.length ? combatant.spell_slots_remaining[spellLevel] : 0;
                this.current_spell_slots[spellLevel] = Math.max(0, Math.min(current, this.spell_slots[spellLevel]));
            }
        }
        this.current_bardic_inspiration_uses = Math.max(0, Math.min(combatant.bardic_inspiration_remaining, this.bardic_inspiration_uses));
    }

    private List<String> describe_spell_names(List<String> spellKeys) {
        List<String> names = new ArrayList<>();
        for (String spellKey : spellKeys) {
            Spell_Definition spell = Spell_Library.get_spell(spellKey);
            names.add(spell == null ? spellKey : spell.display_name);
        }
        return names;
    }

    private int get_current_proficiency_bonus() {
        return (this.current_level - 1) / 4 + 2;
    }
}
