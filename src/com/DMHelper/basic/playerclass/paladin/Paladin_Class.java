package com.DMHelper.basic.playerclass.paladin;

import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.database.Persistence_Util;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Paladin_Class extends Character_Class {

    public Paladin_Oath sacred_oath;
    public List<String> traits;
    public int[] spell_slots;
    public int[] current_spell_slots;
    public int pending_asi_count;
    public int lay_on_hands_pool;
    public int current_lay_on_hands_pool;
    public int attacks_per_action;
    public String fighting_style_name;
    public List<String> prepared_spell_keys;
    public int current_divine_sense_uses;
    public int current_cleansing_touch_uses;
    private boolean preserve_loaded_current_slots;
    private boolean preserve_loaded_lay_on_hands;
    private boolean preserve_loaded_divine_sense;
    private boolean preserve_loaded_cleansing_touch;

    private static final int[][] PALADIN_SLOT_TABLE = {
            {0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0},
            {0, 2, 0, 0, 0, 0},
            {0, 3, 0, 0, 0, 0},
            {0, 3, 0, 0, 0, 0},
            {0, 4, 2, 0, 0, 0},
            {0, 4, 2, 0, 0, 0},
            {0, 4, 3, 0, 0, 0},
            {0, 4, 3, 0, 0, 0},
            {0, 4, 3, 2, 0, 0},
            {0, 4, 3, 2, 0, 0},
            {0, 4, 3, 3, 0, 0},
            {0, 4, 3, 3, 0, 0},
            {0, 4, 3, 3, 1, 0},
            {0, 4, 3, 3, 1, 0},
            {0, 4, 3, 3, 2, 0},
            {0, 4, 3, 3, 2, 0},
            {0, 4, 3, 3, 3, 1},
            {0, 4, 3, 3, 3, 1},
            {0, 4, 3, 3, 3, 2},
            {0, 4, 3, 3, 3, 2}
    };

    public Paladin_Class() {
        super("PALADIN", "圣武士 (Paladin)", 10);
        this.sacred_oath = Paladin_Oath.NONE;
        this.traits = new ArrayList<>();
        this.spell_slots = new int[6];
        this.current_spell_slots = new int[6];
        this.prepared_spell_keys = new ArrayList<>();
        this.attacks_per_action = 1;
        this.current_divine_sense_uses = -1;
        this.current_cleansing_touch_uses = -1;

        this.skill_choose_count = 2;
        this.skill_options.add("Athletics (运动)");
        this.skill_options.add("Insight (洞悉)");
        this.skill_options.add("Intimidation (威吓)");
        this.skill_options.add("Medicine (医药)");
        this.skill_options.add("Persuasion (游说)");
        this.skill_options.add("Religion (宗教)");

        this.saving_throws.add("Wisdom");
        this.saving_throws.add("Charisma");
        this.equipment_proficiencies.add("所有护甲");
        this.equipment_proficiencies.add("盾牌");
        this.equipment_proficiencies.add("简易武器");
        this.equipment_proficiencies.add("军用武器");
    }

    @Override
    public void rebuild_progression() {
        int[] previousMaxSlots = this.spell_slots == null ? new int[6] : this.spell_slots.clone();
        int previousLayOnHands = this.lay_on_hands_pool;
        this.traits.clear();
        this.spell_slots = PALADIN_SLOT_TABLE[Math.max(1, Math.min(this.current_level, 20))].clone();
        sync_current_spell_slots(previousMaxSlots);
        this.lay_on_hands_pool = this.current_level * 5;
        if (!this.preserve_loaded_lay_on_hands
                && previousLayOnHands == 0
                && this.lay_on_hands_pool > 0
                && this.current_lay_on_hands_pool == 0) {
            this.current_lay_on_hands_pool = this.lay_on_hands_pool;
        }
        this.current_lay_on_hands_pool = Math.max(0, Math.min(this.current_lay_on_hands_pool, this.lay_on_hands_pool));
        this.preserve_loaded_lay_on_hands = false;
        this.attacks_per_action = this.current_level >= 5 ? 2 : 1;
        this.pending_asi_count = Math.max(0, get_earned_asi_choices() - this.used_asi_choices);

        this.traits.add("神圣感知 (Divine Sense)：感知附近的邪魔、不死生物与圣洁/亵渎区域。");
        this.traits.add("圣疗之手 (Lay on Hands)：拥有 " + this.lay_on_hands_pool + " 点治疗池。");

        if (this.current_level >= 2) {
            this.traits.add("战斗风格 (Fighting Style)：圣武士战斗风格生效。");
            this.traits.add("施法 (Spellcasting)：从 2 级开始成为半施法者。");
            this.traits.add("神圣惩击 (Divine Smite)：命中后消耗法术位追加光耀伤害。");
        }
        if (this.current_level >= 3) {
            this.traits.add("圣佑免疫 (Divine Health)：免疫疾病。");
        }
        if (this.current_level >= 6) {
            this.traits.add("守护灵光 (Aura of Protection)：为自己与附近盟友的豁免检定加入魅力调整值。");
        }
        if (this.current_level >= 10) {
            this.traits.add("勇气灵光 (Aura of Courage)：你与附近盟友免疫恐慌。");
        }
        if (this.current_level >= 11) {
            this.traits.add("强化神圣惩击 (Improved Divine Smite)：每次近战武器命中自动附带光耀伤害。");
        }
        if (this.current_level >= 14) {
            this.traits.add("净化之触 (Cleansing Touch)：可多次终止自己或盟友身上的法术。");
        }

        apply_oath_features();
        trim_prepared_spells();
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

    public void sync_charisma_resource_caps(int charisma_modifier, boolean restore_to_full) {
        int maxDivineSense = Math.max(0, get_divine_sense_uses(charisma_modifier));
        if (maxDivineSense <= 0) {
            this.current_divine_sense_uses = 0;
        } else if (restore_to_full || (!this.preserve_loaded_divine_sense && this.current_divine_sense_uses < 0)) {
            this.current_divine_sense_uses = maxDivineSense;
        } else {
            this.current_divine_sense_uses = Math.max(0, Math.min(this.current_divine_sense_uses, maxDivineSense));
        }
        this.preserve_loaded_divine_sense = false;

        int maxCleansingTouch = Math.max(0, get_cleansing_touch_uses(charisma_modifier));
        if (maxCleansingTouch <= 0) {
            if (!this.preserve_loaded_cleansing_touch) {
                this.current_cleansing_touch_uses = -1;
            } else {
                this.current_cleansing_touch_uses = 0;
            }
        } else if (restore_to_full || (!this.preserve_loaded_cleansing_touch && this.current_cleansing_touch_uses < 0)) {
            this.current_cleansing_touch_uses = maxCleansingTouch;
        } else {
            this.current_cleansing_touch_uses = Math.max(0, Math.min(this.current_cleansing_touch_uses, maxCleansingTouch));
        }
        this.preserve_loaded_cleansing_touch = false;
    }

    private void apply_oath_features() {
        if (this.sacred_oath == Paladin_Oath.DEVOTION) {
            if (this.current_level >= 3) {
                this.traits.add("奉献誓言 3级 - 神圣武器 / 驱逐亵渎 (Sacred Weapon / Turn the Unholy)：增强武器命中，或驱逐邪魔与不死。");
            }
            if (this.current_level >= 7) {
                this.traits.add("奉献誓言 7级 - 奉献灵光 (Aura of Devotion)：你与附近盟友免疫魅惑。");
            }
            if (this.current_level >= 15) {
                this.traits.add("奉献誓言 15级 - 纯净之魂 (Purity of Spirit)：常驻防护善恶。");
            }
            if (this.current_level >= 20) {
                this.traits.add("奉献誓言 20级 - 神圣光环 (Holy Nimbus)：散发圣光，灼烧敌人并照亮战场。");
            }
        } else if (this.sacred_oath == Paladin_Oath.ANCIENTS) {
            if (this.current_level >= 3) {
                this.traits.add("远古誓言 3级 - 自然之怒 / 驱逐异界 (Nature's Wrath / Turn the Faithless)：控制敌人与驱散妖精/邪魔。");
            }
            if (this.current_level >= 7) {
                this.traits.add("远古誓言 7级 - 守护灵光 (Aura of Warding)：你与附近盟友对法术伤害拥有抗性。");
            }
            if (this.current_level >= 15) {
                this.traits.add("远古誓言 15级 - 不朽哨兵 (Undying Sentinel)：濒死时能顽强站住。");
            }
            if (this.current_level >= 20) {
                this.traits.add("远古誓言 20级 - 上古勇士 (Elder Champion)：化身自然冠军，施法与回复能力大增。");
            }
        } else if (this.sacred_oath == Paladin_Oath.VENGEANCE) {
            if (this.current_level >= 3) {
                this.traits.add("复仇誓言 3级 - 斥退仇敌 / 仇敌誓约 (Abjure Enemy / Vow of Enmity)：压制目标并强化单挑能力。");
            }
            if (this.current_level >= 7) {
                this.traits.add("复仇誓言 7级 - 无情复仇者 (Relentless Avenger)：借机攻击后可顺势追击。");
            }
            if (this.current_level >= 15) {
                this.traits.add("复仇誓言 15级 - 复仇之魂 (Soul of Vengeance)：仇敌攻击时你可追加反击。");
            }
            if (this.current_level >= 20) {
                this.traits.add("复仇誓言 20级 - 复仇天使 (Avenging Angel)：展开恐怖天使形态，飞行并震慑敌军。");
            }
        }
    }

    private void trim_prepared_spells() {
        List<String> valid = new ArrayList<>();
        for (String spellKey : this.prepared_spell_keys) {
            if (!valid.contains(spellKey) && get_available_spell_options().contains(spellKey)) {
                valid.add(spellKey);
            }
        }
        this.prepared_spell_keys.clear();
        this.prepared_spell_keys.addAll(valid);
    }

    public List<String> get_available_fighting_styles() {
        List<String> styles = new ArrayList<>();
        styles.add("Defense");
        styles.add("Dueling");
        styles.add("Great Weapon Fighting");
        styles.add("Protection");
        return styles;
    }

    public String get_fighting_style_label(String styleKey) {
        if ("Defense".equals(styleKey)) return "防御 (Defense)";
        if ("Dueling".equals(styleKey)) return "对决 (Dueling)";
        if ("Great Weapon Fighting".equals(styleKey)) return "巨武器战斗 (Great Weapon Fighting)";
        if ("Protection".equals(styleKey)) return "保护 (Protection)";
        return styleKey;
    }

    public String get_fighting_style_description(String styleKey) {
        if ("Defense".equals(styleKey)) return "穿戴护甲时 AC +1。";
        if ("Dueling".equals(styleKey)) return "单手持一把近战武器时伤害 +2。";
        if ("Great Weapon Fighting".equals(styleKey)) return "双手武器伤害骰掷出 1 或 2 时可重掷。";
        if ("Protection".equals(styleKey)) return "持盾时可用反应令攻击你邻近盟友的攻击陷入劣势。";
        return "待选择";
    }

    @Override
    public int get_extra_armor_class_bonus(String armor_type) {
        if ("Defense".equals(this.fighting_style_name) && !"None".equals(armor_type)) {
            return 1;
        }
        return 0;
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

    public int get_max_spell_level() {
        for (int level = 5; level >= 1; level--) {
            if (this.spell_slots[level] > 0) {
                return level;
            }
        }
        return 1;
    }

    public List<String> get_available_spell_options() {
        if (this.current_level < 2) {
            return new ArrayList<>();
        }
        return Spell_Library.get_paladin_spell_keys_up_to_level(get_max_spell_level());
    }

    public void set_prepared_spells(List<String> spell_keys, int charisma_modifier) {
        int maxPrepared = get_prepared_spell_count(charisma_modifier);
        this.prepared_spell_keys.clear();
        for (String spellKey : spell_keys) {
            if (!this.prepared_spell_keys.contains(spellKey)
                    && get_available_spell_options().contains(spellKey)
                    && this.prepared_spell_keys.size() < maxPrepared) {
                this.prepared_spell_keys.add(spellKey);
            }
        }
    }

    public int get_prepared_spell_count(int charisma_modifier) {
        return this.current_level < 2 ? 0 : Math.max(1, this.current_level / 2 + charisma_modifier);
    }

    public int get_divine_sense_uses(int charisma_modifier) {
        return Math.max(1, 1 + charisma_modifier);
    }

    public int get_cleansing_touch_uses(int charisma_modifier) {
        return this.current_level < 14 ? 0 : Math.max(1, charisma_modifier);
    }

    public List<String> get_prepared_spell_lines() {
        List<String> lines = new ArrayList<>();
        for (String spellKey : this.prepared_spell_keys) {
            Spell_Definition spell = Spell_Library.get_spell(spellKey);
            if (spell != null) {
                lines.add(spell.to_detail_line());
            }
        }
        return lines;
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
        return 6;
    }

    @Override
    public String get_subclass_name() {
        if (this.sacred_oath == Paladin_Oath.DEVOTION) return "奉献誓言";
        if (this.sacred_oath == Paladin_Oath.ANCIENTS) return "远古誓言";
        if (this.sacred_oath == Paladin_Oath.VENGEANCE) return "复仇誓言";
        return "未选择";
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>(this.traits);
        if (this.fighting_style_name != null && !this.fighting_style_name.trim().isEmpty()) {
            summaries.add("战斗风格 - " + get_fighting_style_label(this.fighting_style_name) + "： " + get_fighting_style_description(this.fighting_style_name));
        }
        summaries.add("圣疗池： " + this.current_lay_on_hands_pool + "/" + this.lay_on_hands_pool);
        if (this.current_level >= 2) {
            summaries.add("可准备法术数： 圣武士等级/2 + 魅力调整值");
            summaries.add(get_spell_slot_summary());
        }
        if (!this.feat_names.isEmpty()) {
            for (String featName : this.feat_names) {
                summaries.add("专长 - " + Feat_Library.get_summary_line(featName));
            }
        }
        return summaries;
    }

    public String get_spell_slot_summary() {
        StringBuilder sb = new StringBuilder("法术位：");
        boolean hasAny = false;
        for (int spellLevel = 1; spellLevel < this.spell_slots.length; spellLevel++) {
            if (this.spell_slots[spellLevel] > 0) {
                if (hasAny) sb.append(" | ");
                sb.append(spellLevel).append("环 ").append(this.current_spell_slots[spellLevel]).append("/").append(this.spell_slots[spellLevel]);
                hasAny = true;
            }
        }
        return hasAny ? sb.toString() : "法术位：暂无";
    }

    public String get_lay_on_hands_summary() {
        return this.current_lay_on_hands_pool + "/" + this.lay_on_hands_pool;
    }

    public String get_divine_sense_summary(int charisma_modifier) {
        return this.current_divine_sense_uses + "/" + Math.max(0, get_divine_sense_uses(charisma_modifier));
    }

    public String get_cleansing_touch_summary(int charisma_modifier) {
        return this.current_cleansing_touch_uses + "/" + Math.max(0, get_cleansing_touch_uses(charisma_modifier));
    }

    @Override
    public void restore_long_rest_resources() {
        this.current_spell_slots = this.spell_slots.clone();
        this.current_lay_on_hands_pool = this.lay_on_hands_pool;
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
        this.current_lay_on_hands_pool = Math.max(0, Math.min(combatant.lay_on_hands_remaining, this.lay_on_hands_pool));
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();
        if (this.current_level >= 2 && (this.fighting_style_name == null || this.fighting_style_name.trim().isEmpty())) {
            pending.add("选择圣武士战斗风格");
        }
        if (this.current_level >= 3 && this.sacred_oath == Paladin_Oath.NONE) {
            pending.add("选择圣武誓言");
        }
        if (this.pending_asi_count > 0) {
            pending.add("处理 " + this.pending_asi_count + " 次属性值提升/专长");
        }
        return pending;
    }

    @Override
    public Map<String, String> export_class_state() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("oath", this.sacred_oath.name());
        state.put("fighting_style", this.fighting_style_name == null ? "" : this.fighting_style_name);
        state.put("prepared_spells", Persistence_Util.encode_list(this.prepared_spell_keys));
        state.put("current_spell_slots", Persistence_Util.encode_int_array(this.current_spell_slots));
        state.put("current_lay_on_hands", Integer.toString(this.current_lay_on_hands_pool));
        state.put("current_divine_sense", Integer.toString(this.current_divine_sense_uses));
        state.put("current_cleansing_touch", Integer.toString(this.current_cleansing_touch_uses));
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String oath = class_state.get("oath");
        if (oath != null && !oath.trim().isEmpty()) {
            this.sacred_oath = Paladin_Oath.valueOf(oath);
        }
        this.fighting_style_name = class_state.get("fighting_style");
        if (this.fighting_style_name != null && this.fighting_style_name.trim().isEmpty()) {
            this.fighting_style_name = null;
        }
        this.prepared_spell_keys.clear();
        this.prepared_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("prepared_spells")));
        if (class_state.containsKey("current_spell_slots")) {
            this.current_spell_slots = Persistence_Util.decode_int_array(class_state.get("current_spell_slots"), 6);
            this.preserve_loaded_current_slots = true;
        }
        if (class_state.containsKey("current_lay_on_hands")) {
            try {
                this.current_lay_on_hands_pool = Integer.parseInt(class_state.get("current_lay_on_hands"));
                this.preserve_loaded_lay_on_hands = true;
            } catch (NumberFormatException ignored) {
                this.current_lay_on_hands_pool = 0;
            }
        }
        if (class_state.containsKey("current_divine_sense")) {
            try {
                this.current_divine_sense_uses = Integer.parseInt(class_state.get("current_divine_sense"));
                this.preserve_loaded_divine_sense = true;
            } catch (NumberFormatException ignored) {
                this.current_divine_sense_uses = 0;
            }
        }
        if (class_state.containsKey("current_cleansing_touch")) {
            try {
                this.current_cleansing_touch_uses = Integer.parseInt(class_state.get("current_cleansing_touch"));
                this.preserve_loaded_cleansing_touch = true;
            } catch (NumberFormatException ignored) {
                this.current_cleansing_touch_uses = 0;
            }
        }
        rebuild_progression();
    }
}
