package com.DMHelper.basic.playerclass.sorcerer;

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

public class Sorcerer_Class extends Character_Class {

    public Sorcerous_Origin sorcerous_origin;
    public List<String> traits;
    public int[] spell_slots;
    public int[] current_spell_slots;
    public int cantrips_known;
    public int spells_known_count;
    public int sorcery_points;
    public int current_sorcery_points;
    public int pending_asi_count;
    public int pending_metamagic_choices;
    public List<String> metamagic_keys;
    public List<String> known_cantrip_keys;
    public List<String> known_spell_keys;
    public String dragon_ancestry;
    private boolean preserve_loaded_current_slots;
    private boolean preserve_loaded_current_sorcery_points;

    private static final int[][] SORCERER_SLOT_TABLE = {
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

    private static final int[] CANTRIPS_KNOWN_TABLE = {
            0, 4, 4, 4, 5, 5, 5, 5, 5, 5,
            6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6
    };

    private static final int[] SPELLS_KNOWN_TABLE = {
            0, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 12, 13, 13, 14, 14, 15, 15, 15, 15
    };

    public Sorcerer_Class() {
        super("SORCERER", "术士 (Sorcerer)", 6);
        this.sorcerous_origin = Sorcerous_Origin.NONE;
        this.traits = new ArrayList<>();
        this.spell_slots = new int[10];
        this.current_spell_slots = new int[10];
        this.metamagic_keys = new ArrayList<>();
        this.known_cantrip_keys = new ArrayList<>();
        this.known_spell_keys = new ArrayList<>();

        this.skill_choose_count = 2;
        this.skill_options.add("Arcana (奥秘)");
        this.skill_options.add("Deception (欺瞒)");
        this.skill_options.add("Insight (洞悉)");
        this.skill_options.add("Intimidation (威吓)");
        this.skill_options.add("Persuasion (游说)");
        this.skill_options.add("Religion (宗教)");

        this.saving_throws.add("Constitution");
        this.saving_throws.add("Charisma");
        this.equipment_proficiencies.add("匕首");
        this.equipment_proficiencies.add("飞镖");
        this.equipment_proficiencies.add("投石索");
        this.equipment_proficiencies.add("长棍");
        this.equipment_proficiencies.add("轻弩");
    }

    @Override
    public void rebuild_progression() {
        int[] previousMaxSlots = this.spell_slots == null ? new int[10] : this.spell_slots.clone();
        int previousSorceryPoints = this.sorcery_points;
        this.traits.clear();
        this.spell_slots = SORCERER_SLOT_TABLE[Math.max(1, Math.min(this.current_level, 20))].clone();
        sync_current_spell_slots(previousMaxSlots);
        this.cantrips_known = CANTRIPS_KNOWN_TABLE[this.current_level];
        this.spells_known_count = SPELLS_KNOWN_TABLE[this.current_level];
        this.sorcery_points = this.current_level >= 2 ? this.current_level : 0;
        if (!this.preserve_loaded_current_sorcery_points
                && previousSorceryPoints == 0
                && this.sorcery_points > 0
                && this.current_sorcery_points == 0) {
            this.current_sorcery_points = this.sorcery_points;
        }
        this.current_sorcery_points = Math.max(0, Math.min(this.current_sorcery_points, this.sorcery_points));
        this.preserve_loaded_current_sorcery_points = false;
        this.pending_asi_count = Math.max(0, get_earned_asi_choices() - this.used_asi_choices);
        this.pending_metamagic_choices = Math.max(0, get_expected_metamagic_count() - this.metamagic_keys.size());

        this.traits.add("施法 (Spellcasting)：以魅力作为施法关键属性，凭天生魔法施法。");
        if (this.current_level >= 1) {
            this.traits.add("术法起源 (Sorcerous Origin)：你的天生魔法决定子职业能力。");
        }
        if (this.current_level >= 2) {
            this.traits.add("魔力泉涌 (Font of Magic)：获得术法点，可与法术位互相转换。");
        }
        if (this.current_level >= 3) {
            this.traits.add("法术塑形 (Metamagic)：使用术法点改变法术的施放方式。");
        }
        if (this.current_level >= 20) {
            this.traits.add("术法回流 (Sorcerous Restoration)：短休后恢复 4 点术法点。");
        }

        apply_subclass_features();
        ensure_known_cantrips();
        ensure_known_spells();
        trim_known_spells();
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

    private void apply_subclass_features() {
        if (this.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE) {
            this.traits.add("龙脉术士 1级 - 龙族先祖 (Dragon Ancestor)：当前龙脉先祖为 " + get_dragon_ancestry_label() + "，后续元素能力围绕该伤害类型展开。");
            this.traits.add("龙脉术士 1级 - 龙鳞坚韧 (Draconic Resilience)：未穿甲时 AC 变为 13 + 敏捷调整值，且每级额外 +1 最大生命值。");
            if (this.current_level >= 6) {
                this.traits.add("龙脉术士 6级 - 元素亲和 (Elemental Affinity)：你的 " + get_dragon_damage_type_label() + " 法术伤害更强，并可获得对应能量抗性。");
            }
            if (this.current_level >= 14) {
                this.traits.add("龙脉术士 14级 - 龙翼 (Dragon Wings)：获得持续飞行能力。");
            }
            if (this.current_level >= 18) {
                this.traits.add("龙脉术士 18级 - 龙威显现 (Draconic Presence)：消耗术法点震慑或魅惑周围敌人。");
            }
        } else if (this.sorcerous_origin == Sorcerous_Origin.WILD_MAGIC) {
            this.traits.add("狂野魔法术士 1级 - 狂野魔法涌动 (Wild Magic Surge)：施法可能触发不可预料的魔法效果。");
            this.traits.add("狂野魔法术士 1级 - 混沌潮汐 (Tides of Chaos)：一次检定可获得优势，随后更容易引发魔涌。");
            if (this.current_level >= 6) {
                this.traits.add("狂野魔法术士 6级 - 扭转幸运 (Bend Luck)：消耗术法点干扰附近生物的检定结果。");
            }
            if (this.current_level >= 14) {
                this.traits.add("狂野魔法术士 14级 - 可控混沌 (Controlled Chaos)：投掷狂野魔法表时可掷两次选其一。");
            }
            if (this.current_level >= 18) {
                this.traits.add("狂野魔法术士 18级 - 法术轰击 (Spell Bombardment)：高伤害法术的伤害骰可再掷一次。");
            }
        }
    }

    private void ensure_known_cantrips() {
        if (!this.known_cantrip_keys.isEmpty()) {
            return;
        }
        List<String> available = Spell_Library.get_sorcerer_cantrip_keys();
        for (String spellKey : available) {
            if (this.known_cantrip_keys.size() >= this.cantrips_known) {
                break;
            }
            if (!this.known_cantrip_keys.contains(spellKey)) {
                this.known_cantrip_keys.add(spellKey);
            }
        }
    }

    private void ensure_known_spells() {
        if (!this.known_spell_keys.isEmpty()) {
            return;
        }
        List<String> available = get_available_spell_options();
        for (String spellKey : available) {
            if (this.known_spell_keys.size() >= this.spells_known_count) {
                break;
            }
            if (!this.known_spell_keys.contains(spellKey)) {
                this.known_spell_keys.add(spellKey);
            }
        }
    }

    private void trim_known_spells() {
        while (this.known_cantrip_keys.size() > this.cantrips_known) {
            this.known_cantrip_keys.remove(this.known_cantrip_keys.size() - 1);
        }
        while (this.known_spell_keys.size() > this.spells_known_count) {
            this.known_spell_keys.remove(this.known_spell_keys.size() - 1);
        }
    }

    public int get_expected_metamagic_count() {
        int count = 0;
        if (this.current_level >= 3) {
            count = 2;
        }
        if (this.current_level >= 10) {
            count = 3;
        }
        if (this.current_level >= 17) {
            count = 4;
        }
        return count;
    }

    public List<String> get_available_metamagic_options() {
        List<String> options = new ArrayList<>();
        options.add("Careful Spell");
        options.add("Distant Spell");
        options.add("Empowered Spell");
        options.add("Extended Spell");
        options.add("Heightened Spell");
        options.add("Quickened Spell");
        options.add("Subtle Spell");
        options.add("Twinned Spell");
        options.removeAll(this.metamagic_keys);
        return options;
    }

    public String get_metamagic_label(String key) {
        if ("Careful Spell".equals(key)) return "谨慎法术 (Careful Spell)";
        if ("Distant Spell".equals(key)) return "远距法术 (Distant Spell)";
        if ("Empowered Spell".equals(key)) return "强化法术 (Empowered Spell)";
        if ("Extended Spell".equals(key)) return "延时法术 (Extended Spell)";
        if ("Heightened Spell".equals(key)) return "高强法术 (Heightened Spell)";
        if ("Quickened Spell".equals(key)) return "迅捷法术 (Quickened Spell)";
        if ("Subtle Spell".equals(key)) return "隐秘法术 (Subtle Spell)";
        if ("Twinned Spell".equals(key)) return "双生法术 (Twinned Spell)";
        return key;
    }

    public String get_metamagic_description(String key) {
        if ("Careful Spell".equals(key)) return "保护部分豁免目标，避免他们完全吃到范围法术效果。";
        if ("Distant Spell".equals(key)) return "延长法术射程，或让接触法术改为远程施放。";
        if ("Empowered Spell".equals(key)) return "重掷部分伤害骰，提高输出稳定性。";
        if ("Extended Spell".equals(key)) return "延长持续时间较长的法术。";
        if ("Heightened Spell".equals(key)) return "让一个目标对你的法术豁免陷入劣势。";
        if ("Quickened Spell".equals(key)) return "把施法时间为动作的法术改成附赠动作。";
        if ("Subtle Spell".equals(key)) return "无声无势施法，几乎不暴露施法动作。";
        if ("Twinned Spell".equals(key)) return "把本来只影响一个目标的法术复制到第二个目标。";
        return key;
    }

    public int get_max_spell_level() {
        for (int level = 9; level >= 1; level--) {
            if (this.spell_slots[level] > 0) {
                return level;
            }
        }
        return 1;
    }

    public List<String> get_available_spell_options() {
        List<String> options = Spell_Library.get_sorcerer_spell_keys_up_to_level(get_max_spell_level());
        options.removeAll(this.known_spell_keys);
        return options;
    }

    public void set_known_spells(List<String> spell_keys) {
        this.known_spell_keys.clear();
        for (String spellKey : spell_keys) {
            if (!this.known_spell_keys.contains(spellKey) && this.known_spell_keys.size() < this.spells_known_count) {
                this.known_spell_keys.add(spellKey);
            }
        }
    }

    public void set_known_cantrips(List<String> spell_keys) {
        this.known_cantrip_keys.clear();
        for (String spellKey : spell_keys) {
            if (!this.known_cantrip_keys.contains(spellKey) && this.known_cantrip_keys.size() < this.cantrips_known) {
                this.known_cantrip_keys.add(spellKey);
            }
        }
    }

    public List<String> get_known_cantrip_lines() {
        List<String> lines = new ArrayList<>();
        for (String spellKey : this.known_cantrip_keys) {
            Spell_Definition spell = Spell_Library.get_spell(spellKey);
            if (spell != null) {
                lines.add(spell.to_detail_line());
            }
        }
        return lines;
    }

    public List<String> get_known_spell_lines() {
        List<String> lines = new ArrayList<>();
        for (String spellKey : this.known_spell_keys) {
            Spell_Definition spell = Spell_Library.get_spell(spellKey);
            if (spell != null) {
                lines.add(spell.to_detail_line());
            }
        }
        return lines;
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
        return 4;
    }

    @Override
    public String get_subclass_name() {
        if (this.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE) {
            return this.dragon_ancestry == null || this.dragon_ancestry.trim().isEmpty()
                    ? "龙脉术士"
                    : "龙脉术士 - " + get_dragon_ancestry_label();
        }
        if (this.sorcerous_origin == Sorcerous_Origin.WILD_MAGIC) {
            return "狂野魔法术士";
        }
        return "未选择";
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>(this.traits);
        if (this.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE) {
            summaries.add("龙族先祖： " + get_dragon_ancestry_label());
            summaries.add("龙脉元素： " + get_dragon_damage_type_label());
        }
        summaries.add("术法点： " + this.current_sorcery_points + "/" + this.sorcery_points);
        summaries.add("戏法已知数： " + this.cantrips_known);
        summaries.add("法术已知数： " + this.spells_known_count);
        if (!this.metamagic_keys.isEmpty()) {
            for (String key : this.metamagic_keys) {
                summaries.add("超魔技巧 - " + get_metamagic_label(key) + "： " + get_metamagic_description(key));
            }
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
    public int get_extra_armor_class_bonus(String armor_type) {
        if (this.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE && "None".equals(armor_type)) {
            return 3;
        }
        return 0;
    }

    @Override
    public int get_extra_hit_points_per_level() {
        return this.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE ? 1 : 0;
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

    public String get_sorcery_point_summary() {
        return this.current_sorcery_points + "/" + this.sorcery_points;
    }

    @Override
    public void restore_short_rest_resources() {
        if (this.current_level >= 20) {
            this.current_sorcery_points = Math.min(this.sorcery_points, this.current_sorcery_points + 4);
        }
    }

    @Override
    public void restore_long_rest_resources() {
        this.current_spell_slots = this.spell_slots.clone();
        this.current_sorcery_points = this.sorcery_points;
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
        this.current_sorcery_points = Math.max(0, Math.min(combatant.sorcery_points_remaining, this.sorcery_points));
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();
        if (this.sorcerous_origin == Sorcerous_Origin.NONE) {
            pending.add("选择术法起源");
        }
        int pendingCantrips = Math.max(0, this.cantrips_known - this.known_cantrip_keys.size());
        if (pendingCantrips > 0) {
            pending.add("选择 " + pendingCantrips + " 个术士戏法");
        }
        int pendingSpells = Math.max(0, this.spells_known_count - this.known_spell_keys.size());
        if (pendingSpells > 0) {
            pending.add("选择 " + pendingSpells + " 个术士法术");
        }
        if (this.pending_metamagic_choices > 0) {
            pending.add("选择 " + this.pending_metamagic_choices + " 个超魔技巧");
        }
        if (this.pending_asi_count > 0) {
            pending.add("处理 " + this.pending_asi_count + " 次属性值提升/专长");
        }
        return pending;
    }

    @Override
    public Map<String, String> export_class_state() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("origin", this.sorcerous_origin.name());
        state.put("dragon_ancestry", this.dragon_ancestry == null ? "" : this.dragon_ancestry);
        state.put("metamagic", Persistence_Util.encode_list(this.metamagic_keys));
        state.put("known_cantrips", Persistence_Util.encode_list(this.known_cantrip_keys));
        state.put("known_spells", Persistence_Util.encode_list(this.known_spell_keys));
        state.put("current_spell_slots", Persistence_Util.encode_int_array(this.current_spell_slots));
        state.put("current_sorcery_points", Integer.toString(this.current_sorcery_points));
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String origin = class_state.get("origin");
        if (origin != null && !origin.trim().isEmpty()) {
            this.sorcerous_origin = Sorcerous_Origin.valueOf(origin);
        }
        this.dragon_ancestry = class_state.get("dragon_ancestry");
        this.metamagic_keys.clear();
        this.metamagic_keys.addAll(Persistence_Util.decode_list(class_state.get("metamagic")));
        this.known_cantrip_keys.clear();
        this.known_cantrip_keys.addAll(Persistence_Util.decode_list(class_state.get("known_cantrips")));
        this.known_spell_keys.clear();
        this.known_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("known_spells")));
        if (class_state.containsKey("current_spell_slots")) {
            this.current_spell_slots = Persistence_Util.decode_int_array(class_state.get("current_spell_slots"), 10);
            this.preserve_loaded_current_slots = true;
        }
        if (class_state.containsKey("current_sorcery_points")) {
            try {
                this.current_sorcery_points = Integer.parseInt(class_state.get("current_sorcery_points"));
                this.preserve_loaded_current_sorcery_points = true;
            } catch (NumberFormatException ignored) {
                this.current_sorcery_points = 0;
            }
        }
        rebuild_progression();
    }

    public String get_dragon_ancestry_label() {
        if (this.dragon_ancestry == null || this.dragon_ancestry.trim().isEmpty()) {
            return "未选择";
        }
        return this.dragon_ancestry;
    }

    public String get_dragon_damage_type_label() {
        String ancestry = get_dragon_ancestry_label();
        if (ancestry.contains("火焰")) return "火焰";
        if (ancestry.contains("闪电")) return "闪电";
        if (ancestry.contains("寒冷")) return "寒冷";
        if (ancestry.contains("强酸")) return "强酸";
        if (ancestry.contains("毒素")) return "毒素";
        return "未确定";
    }
}
