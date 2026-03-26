package com.DMHelper.basic.playerclass.warlock;

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

public class Warlock_Class extends Character_Class {
    public Warlock_Patron patron;
    public List<String> traits;
    public int cantrips_known;
    public int spells_known_count;
    public int pact_slot_count;
    public int current_pact_slot_count;
    public int pact_slot_level;
    public int mystic_arcanum_level;
    public int pending_asi_count;
    public int pending_invocation_choices;
    public List<String> invocation_keys;
    public List<String> known_cantrip_keys;
    public List<String> known_spell_keys;
    private boolean preserve_loaded_current_pact_slots;

    private static final int[] CANTRIPS_KNOWN_TABLE = {
            0, 2, 2, 2, 3, 3, 3, 3, 3, 3,
            4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4
    };

    private static final int[] SPELLS_KNOWN_TABLE = {
            0, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15
    };

    private static final int[] PACT_SLOT_COUNT_TABLE = {
            0, 1, 2, 2, 2, 2, 2, 2, 2, 2,
            2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4
    };

    private static final int[] PACT_SLOT_LEVEL_TABLE = {
            0, 1, 1, 2, 2, 3, 3, 4, 4, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5
    };

    public Warlock_Class() {
        super("WARLOCK", "邪术士 (Warlock)", 8);
        this.patron = Warlock_Patron.NONE;
        this.traits = new ArrayList<>();
        this.invocation_keys = new ArrayList<>();
        this.known_cantrip_keys = new ArrayList<>();
        this.known_spell_keys = new ArrayList<>();

        this.skill_choose_count = 2;
        this.skill_options.add("Arcana (奥秘)");
        this.skill_options.add("Deception (欺瞒)");
        this.skill_options.add("History (历史)");
        this.skill_options.add("Intimidation (威吓)");
        this.skill_options.add("Investigation (调查)");
        this.skill_options.add("Nature (自然)");
        this.skill_options.add("Religion (宗教)");

        this.saving_throws.add("Wisdom");
        this.saving_throws.add("Charisma");
        this.equipment_proficiencies.add("轻甲");
        this.equipment_proficiencies.add("简易武器");
    }

    @Override
    public void rebuild_progression() {
        int previousPactSlotCount = this.pact_slot_count;
        this.traits.clear();
        this.cantrips_known = CANTRIPS_KNOWN_TABLE[this.current_level];
        this.spells_known_count = SPELLS_KNOWN_TABLE[this.current_level];
        this.pact_slot_count = PACT_SLOT_COUNT_TABLE[this.current_level];
        if (!this.preserve_loaded_current_pact_slots
                && previousPactSlotCount == 0
                && this.pact_slot_count > 0
                && this.current_pact_slot_count == 0) {
            this.current_pact_slot_count = this.pact_slot_count;
        }
        this.current_pact_slot_count = Math.max(0, Math.min(this.current_pact_slot_count, this.pact_slot_count));
        this.preserve_loaded_current_pact_slots = false;
        this.pact_slot_level = PACT_SLOT_LEVEL_TABLE[this.current_level];
        this.pending_asi_count = Math.max(0, get_earned_asi_choices() - this.used_asi_choices);
        this.pending_invocation_choices = Math.max(0, get_expected_invocation_count() - this.invocation_keys.size());
        this.mystic_arcanum_level = get_mystic_arcanum_level();

        this.traits.add("契约魔法 (Pact Magic)：你的法术位数量较少，但会按固定最高环级恢复。");
        this.traits.add("异界恩主 (Otherworldly Patron)：你的恩主决定子职业能力。");
        if (this.current_level >= 2) {
            this.traits.add("邪术祈请 (Eldritch Invocations)：从多个持续强化中挑选独特秘法。");
        }
        if (this.current_level >= 3) {
            this.traits.add("契约恩赐 (Pact Boon)：获得额外的契约赠礼。");
        }
        if (this.current_level >= 11) {
            this.traits.add("神秘秘法 (Mystic Arcanum)：可获得高环一次性法术。");
        }
        if (this.current_level >= 20) {
            this.traits.add("邪术大师 (Eldritch Master)：短暂祈祷即可恢复全部契约法术位。");
        }

        apply_patron_features();
        ensure_known_cantrips();
        ensure_known_spells();
        trim_known_lists();
    }

    private void apply_patron_features() {
        if (this.patron == Warlock_Patron.FIEND) {
            this.traits.add("邪魔恩主 - 黑暗祝福 (Dark One's Blessing)：击杀敌人后可获得临时生命值。");
            if (this.current_level >= 6) this.traits.add("邪魔恩主 - 黑暗幸运 (Dark One's Own Luck)：可短暂提升一次检定或豁免。");
            if (this.current_level >= 10) this.traits.add("邪魔恩主 - 邪魔韧性 (Fiendish Resilience)：长休后选择一种伤害类型获得抗性。");
            if (this.current_level >= 14) this.traits.add("邪魔恩主 - 地狱拖曳 (Hurl Through Hell)：短暂把目标拖入炼狱。");
        } else if (this.patron == Warlock_Patron.ARCHFEY) {
            this.traits.add("妖精恩主 - 妖精临现 (Fey Presence)：短时间魅惑或恐吓周围目标。");
            if (this.current_level >= 6) this.traits.add("妖精恩主 - 迷雾遁形 (Misty Escape)：受伤后可隐形并瞬移脱身。");
            if (this.current_level >= 10) this.traits.add("妖精恩主 - 妖精防御 (Beguiling Defenses)：更难被魅惑，还可反制魅惑。");
            if (this.current_level >= 14) this.traits.add("妖精恩主 - 暗夜幻景 (Dark Delirium)：把目标拉入虚幻梦魇空间。");
        } else if (this.patron == Warlock_Patron.GREAT_OLD_ONE) {
            this.traits.add("旧日支配者 - 心灵唤语 (Awakened Mind)：可进行短距离心灵交流。");
            if (this.current_level >= 6) this.traits.add("旧日支配者 - 熵能守护 (Entropic Ward)：扭曲命运，让敌人攻击失手。");
            if (this.current_level >= 10) this.traits.add("旧日支配者 - 思维屏障 (Thought Shield)：心灵防护更强并反弹部分精神伤害。");
            if (this.current_level >= 14) this.traits.add("旧日支配者 - 创造奴仆 (Create Thrall)：以心灵支配一个 humanoid。");
        }
    }

    private void ensure_known_cantrips() {
        if (!this.known_cantrip_keys.isEmpty()) {
            return;
        }
        List<String> available = Spell_Library.get_warlock_cantrip_keys();
        for (String spellKey : available) {
            if (this.known_cantrip_keys.size() >= this.cantrips_known) break;
            if (!this.known_cantrip_keys.contains(spellKey)) this.known_cantrip_keys.add(spellKey);
        }
    }

    private void ensure_known_spells() {
        if (!this.known_spell_keys.isEmpty()) {
            return;
        }
        List<String> available = get_available_spell_options();
        for (String spellKey : available) {
            if (this.known_spell_keys.size() >= this.spells_known_count) break;
            if (!this.known_spell_keys.contains(spellKey)) this.known_spell_keys.add(spellKey);
        }
    }

    private void trim_known_lists() {
        while (this.known_cantrip_keys.size() > this.cantrips_known) {
            this.known_cantrip_keys.remove(this.known_cantrip_keys.size() - 1);
        }
        while (this.known_spell_keys.size() > this.spells_known_count) {
            this.known_spell_keys.remove(this.known_spell_keys.size() - 1);
        }
    }

    public int get_expected_invocation_count() {
        if (this.current_level >= 18) return 8;
        if (this.current_level >= 15) return 7;
        if (this.current_level >= 12) return 6;
        if (this.current_level >= 9) return 5;
        if (this.current_level >= 7) return 4;
        if (this.current_level >= 5) return 3;
        if (this.current_level >= 2) return 2;
        return 0;
    }

    private int get_mystic_arcanum_level() {
        if (this.current_level >= 17) return 9;
        if (this.current_level >= 15) return 8;
        if (this.current_level >= 13) return 7;
        if (this.current_level >= 11) return 6;
        return 0;
    }

    public List<String> get_available_invocation_options() {
        List<String> options = new ArrayList<>();
        options.add("Agonizing Blast");
        options.add("Armor of Shadows");
        options.add("Devil's Sight");
        options.add("Eldritch Sight");
        options.add("Fiendish Vigor");
        options.add("Mask of Many Faces");
        options.add("Misty Visions");
        options.add("Repelling Blast");
        options.removeAll(this.invocation_keys);
        return options;
    }

    public String get_invocation_label(String key) {
        if ("Agonizing Blast".equals(key)) return "苦痛魔能爆 (Agonizing Blast)";
        if ("Armor of Shadows".equals(key)) return "阴影护甲 (Armor of Shadows)";
        if ("Devil's Sight".equals(key)) return "魔鬼视界 (Devil's Sight)";
        if ("Eldritch Sight".equals(key)) return "异界视界 (Eldritch Sight)";
        if ("Fiendish Vigor".equals(key)) return "邪魔活力 (Fiendish Vigor)";
        if ("Mask of Many Faces".equals(key)) return "千面伪装 (Mask of Many Faces)";
        if ("Misty Visions".equals(key)) return "迷雾幻景 (Misty Visions)";
        if ("Repelling Blast".equals(key)) return "斥退魔能爆 (Repelling Blast)";
        return key;
    }

    public String get_invocation_description(String key) {
        if ("Agonizing Blast".equals(key)) return "你的魔能爆伤害额外加入魅力调整值。";
        if ("Armor of Shadows".equals(key)) return "可随意施放法师护甲，不消耗法术位。";
        if ("Devil's Sight".equals(key)) return "可在黑暗与魔法黑暗中正常视物。";
        if ("Eldritch Sight".equals(key)) return "可随意施放侦测魔法。";
        if ("Fiendish Vigor".equals(key)) return "可随意施放虚假生命。";
        if ("Mask of Many Faces".equals(key)) return "可随意施放易容术。";
        if ("Misty Visions".equals(key)) return "可随意施放寂静幻影。";
        if ("Repelling Blast".equals(key)) return "你的魔能爆命中时可把目标推开。";
        return key;
    }

    public List<String> get_available_spell_options() {
        return Spell_Library.get_warlock_spell_keys_up_to_level(this.pact_slot_level);
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
            if (!this.known_spell_keys.contains(key) && this.known_spell_keys.size() < this.spells_known_count) {
                this.known_spell_keys.add(key);
            }
        }
    }

    public List<String> get_known_cantrip_lines() {
        return get_spell_lines(this.known_cantrip_keys);
    }

    public List<String> get_known_spell_lines() {
        return get_spell_lines(this.known_spell_keys);
    }

    private List<String> get_spell_lines(List<String> keys) {
        List<String> lines = new ArrayList<>();
        for (String key : keys) {
            Spell_Definition spell = Spell_Library.get_spell(key);
            if (spell != null) lines.add(spell.to_detail_line());
        }
        return lines;
    }

    public int get_earned_asi_choices() {
        int count = 0;
        int[] levels = {4, 8, 12, 16, 19};
        for (int level : levels) if (this.current_level >= level) count++;
        return count;
    }

    @Override
    public void level_up(int target_level) {
        if (target_level <= this.current_level || target_level > 20) return;
        this.current_level = target_level;
        rebuild_progression();
    }

    @Override
    public int get_average_hp_gain() {
        return 5;
    }

    @Override
    public String get_subclass_name() {
        if (this.patron == Warlock_Patron.FIEND) return "邪魔恩主";
        if (this.patron == Warlock_Patron.ARCHFEY) return "妖精恩主";
        if (this.patron == Warlock_Patron.GREAT_OLD_ONE) return "旧日支配者";
        return "未选择";
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>(this.traits);
        summaries.add("契约法术位： " + get_pact_slot_summary());
        summaries.add("戏法已知数： " + this.cantrips_known);
        summaries.add("法术已知数： " + this.spells_known_count);
        if (this.mystic_arcanum_level > 0) {
            summaries.add("神秘秘法：可使用 1 个 " + this.mystic_arcanum_level + " 环秘法。");
        }
        for (String key : this.invocation_keys) {
            summaries.add("邪术祈请 - " + get_invocation_label(key) + "： " + get_invocation_description(key));
        }
        for (String featName : this.feat_names) {
            summaries.add("专长 - " + Feat_Library.get_summary_line(featName));
        }
        return summaries;
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();
        if (this.patron == Warlock_Patron.NONE) pending.add("选择异界恩主");
        int pendingCantrips = Math.max(0, this.cantrips_known - this.known_cantrip_keys.size());
        if (pendingCantrips > 0) pending.add("选择 " + pendingCantrips + " 个邪术士戏法");
        int pendingSpells = Math.max(0, this.spells_known_count - this.known_spell_keys.size());
        if (pendingSpells > 0) pending.add("选择 " + pendingSpells + " 个邪术士法术");
        if (this.pending_invocation_choices > 0) pending.add("选择 " + this.pending_invocation_choices + " 个邪术祈请");
        if (this.pending_asi_count > 0) pending.add("处理 " + this.pending_asi_count + " 次属性值提升/专长");
        return pending;
    }

    @Override
    public Map<String, String> export_class_state() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("patron", this.patron.name());
        state.put("invocations", Persistence_Util.encode_list(this.invocation_keys));
        state.put("known_cantrips", Persistence_Util.encode_list(this.known_cantrip_keys));
        state.put("known_spells", Persistence_Util.encode_list(this.known_spell_keys));
        state.put("current_pact_slots", Integer.toString(this.current_pact_slot_count));
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String patronValue = class_state.get("patron");
        if (patronValue != null && !patronValue.trim().isEmpty()) this.patron = Warlock_Patron.valueOf(patronValue);
        this.invocation_keys.clear();
        this.invocation_keys.addAll(Persistence_Util.decode_list(class_state.get("invocations")));
        this.known_cantrip_keys.clear();
        this.known_cantrip_keys.addAll(Persistence_Util.decode_list(class_state.get("known_cantrips")));
        this.known_spell_keys.clear();
        this.known_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("known_spells")));
        if (class_state.containsKey("current_pact_slots")) {
            try {
                this.current_pact_slot_count = Integer.parseInt(class_state.get("current_pact_slots"));
                this.preserve_loaded_current_pact_slots = true;
            } catch (NumberFormatException ignored) {
                this.current_pact_slot_count = 0;
            }
        }
        rebuild_progression();
    }

    public String get_pact_slot_summary() {
        if (this.pact_slot_count <= 0) {
            return "暂无";
        }
        return this.current_pact_slot_count + "/" + this.pact_slot_count + " 个 " + this.pact_slot_level + " 环位";
    }

    @Override
    public void restore_long_rest_resources() {
        this.current_pact_slot_count = this.pact_slot_count;
    }

    @Override
    public void sync_from_combatant(Combatant combatant) {
        if (combatant == null) {
            return;
        }
        this.current_pact_slot_count = Math.max(0, Math.min(combatant.pact_slots_remaining, this.pact_slot_count));
    }
}
