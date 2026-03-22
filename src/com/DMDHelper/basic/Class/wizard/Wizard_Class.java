package com.DMDHelper.basic.Class.wizard;

import com.DMDHelper.basic.Class.Character_Class;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Wizard_Class extends Character_Class {

    public Wizard_Subclass wizard_subclass;
    public int pending_asi_count;
    public int[] spell_slots;
    public List<String> traits;

    private static final int[][] WIZARD_SLOT_TABLE = {
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

    public Wizard_Class() {
        super("WIZARD", "法师 (Wizard)", 6);

        this.wizard_subclass = Wizard_Subclass.NONE;
        this.pending_asi_count = 0;
        this.spell_slots = new int[10];
        this.traits = new ArrayList<>();

        this.skill_choose_count = 2;
        this.skill_options.add("Arcana (奥秘)");
        this.skill_options.add("History (历史)");
        this.skill_options.add("Insight (洞悉)");
        this.skill_options.add("Investigation (调查)");
        this.skill_options.add("Medicine (医药)");
        this.skill_options.add("Religion (宗教)");

        this.saving_throws.add("Intelligence");
        this.saving_throws.add("Wisdom");
        this.equipment_proficiencies.add("匕首");
        this.equipment_proficiencies.add("飞镖");
        this.equipment_proficiencies.add("投石索");
        this.equipment_proficiencies.add("长棍");
        this.equipment_proficiencies.add("轻弩");
    }

    @Override
    public void rebuild_progression() {
        this.pending_asi_count = 0;
        this.traits.clear();
        this.spell_slots = WIZARD_SLOT_TABLE[Math.max(1, Math.min(this.current_level, 20))].clone();

        this.traits.add("施法 (Spellcasting)");
        this.traits.add("奥术回能 (Arcane Recovery)");

        for (int level = 2; level <= this.current_level; level++) {
            apply_level_features(level);
        }

        int earned_asi_choices = get_earned_asi_choices();
        this.pending_asi_count = Math.max(0, earned_asi_choices - this.used_asi_choices);
    }

    private void apply_level_features(int level) {
        if (level == 2) {
            apply_subclass_features(2);
        } else if (level == 4 || level == 8 || level == 12 || level == 16 || level == 19) {
            return;
        } else if (level == 18) {
            this.traits.add("法术精通 (Spell Mastery)");
        } else if (level == 20) {
            this.traits.add("招牌法术 (Signature Spells)");
        }

        if ((level == 6 || level == 10 || level == 14) && this.wizard_subclass != Wizard_Subclass.NONE) {
            apply_subclass_features(level);
        }
    }

    private void apply_subclass_features(int level) {
        if (this.wizard_subclass == Wizard_Subclass.NONE) {
            return;
        }

        if (this.wizard_subclass == Wizard_Subclass.EVOCATION) {
            if (level == 2) {
                this.traits.add("塑能学派 - 塑能雕琢 (Sculpt Spells)");
            } else if (level == 6) {
                this.traits.add("塑能学派 - 强效戏法 (Potent Cantrip)");
            } else if (level == 10) {
                this.traits.add("塑能学派 - 强化塑能 (Empowered Evocation)");
            } else if (level == 14) {
                this.traits.add("塑能学派 - 超限塑能 (Overchannel)");
            }
        } else if (this.wizard_subclass == Wizard_Subclass.ABJURATION) {
            if (level == 2) {
                this.traits.add("防护学派 - 奥术结界 (Arcane Ward)");
            } else if (level == 6) {
                this.traits.add("防护学派 - 投射结界 (Projected Ward)");
            } else if (level == 10) {
                this.traits.add("防护学派 - 强化防护 (Improved Abjuration)");
            } else if (level == 14) {
                this.traits.add("防护学派 - 法术抗性 (Spell Resistance)");
            }
        }
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
        if (this.wizard_subclass == Wizard_Subclass.EVOCATION) {
            return "塑能学派";
        }
        if (this.wizard_subclass == Wizard_Subclass.ABJURATION) {
            return "防护学派";
        }
        return "未选择";
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>();
        for (String trait : this.traits) {
            summaries.add(trait);
        }
        if (!this.feat_names.isEmpty()) {
            for (String feat_name : this.feat_names) {
                summaries.add("专长 - " + feat_name);
            }
        }
        summaries.add(get_spell_slot_summary());
        return summaries;
    }

    public String get_spell_slot_summary() {
        StringBuilder sb = new StringBuilder("法术位：");
        boolean hasAny = false;
        for (int spellLevel = 1; spellLevel < this.spell_slots.length; spellLevel++) {
            if (this.spell_slots[spellLevel] > 0) {
                if (hasAny) {
                    sb.append(" | ");
                }
                sb.append(spellLevel).append("环 ").append(this.spell_slots[spellLevel]).append("个");
                hasAny = true;
            }
        }
        if (!hasAny) {
            sb.append("暂无");
        }
        return sb.toString();
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();
        if (this.current_level >= 2 && this.wizard_subclass == Wizard_Subclass.NONE) {
            pending.add("选择奥术传承");
        }
        if (this.pending_asi_count > 0) {
            pending.add("处理 " + this.pending_asi_count + " 次属性值提升/专长");
        }
        return pending;
    }

    @Override
    public Map<String, String> export_class_state() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("subclass", this.wizard_subclass.name());
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String subclass = class_state.get("subclass");
        if (subclass != null && !subclass.trim().isEmpty()) {
            this.wizard_subclass = Wizard_Subclass.valueOf(subclass);
        }
        rebuild_progression();
    }
}
