package com.DMHelper.basic.playerclass.wizard;

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

public class Wizard_Class extends Character_Class {

    public Wizard_Subclass wizard_subclass;
    public int pending_asi_count;
    public int[] spell_slots;
    public int[] current_spell_slots;
    public List<String> traits;
    public int cantrips_known;
    public int spells_in_spellbook;
    public int prepared_spell_limit;
    public int arcane_recovery_level;
    public List<String> known_cantrip_keys;
    public List<String> spellbook_spell_keys;
    public List<String> prepared_spell_keys;
    private boolean preserve_loaded_current_slots;

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
        this.current_spell_slots = new int[10];
        this.traits = new ArrayList<>();
        this.cantrips_known = 3;
        this.spells_in_spellbook = 6;
        this.prepared_spell_limit = 1;
        this.arcane_recovery_level = 1;
        this.known_cantrip_keys = new ArrayList<>();
        this.spellbook_spell_keys = new ArrayList<>();
        this.prepared_spell_keys = new ArrayList<>();

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
        int[] previousMaxSlots = this.spell_slots == null ? new int[10] : this.spell_slots.clone();
        this.pending_asi_count = 0;
        this.traits.clear();
        this.spell_slots = WIZARD_SLOT_TABLE[Math.max(1, Math.min(this.current_level, 20))].clone();
        sync_current_spell_slots(previousMaxSlots);
        this.cantrips_known = get_cantrips_known_for_level(this.current_level);
        this.spells_in_spellbook = 6 + Math.max(0, (this.current_level - 1) * 2);
        this.arcane_recovery_level = Math.max(1, (int) Math.ceil(this.current_level / 2.0));
        this.prepared_spell_limit = Math.max(1, this.current_level);

        ensure_default_cantrips();
        trim_known_cantrips();
        ensure_default_spellbook();
        trim_spellbook_to_capacity();
        trim_prepared_spells_to_valid_selection();

        this.traits.add("施法 (Spellcasting)：以智力作为施法关键属性，法术书初始记录 6 个 1 环法术。");
        this.traits.add("奥术回能 (Arcane Recovery)：每天长休后首次短休时，可恢复总环级不超过一半法师等级（向上取整）的法术位。");
        this.traits.add("仪式施法 (Ritual Casting)：法术书中带有仪式标签的法术可作为仪式施放。");

        for (int level = 2; level <= this.current_level; level++) {
            apply_level_features(level);
        }

        int earned_asi_choices = get_earned_asi_choices();
        this.pending_asi_count = Math.max(0, earned_asi_choices - this.used_asi_choices);
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

    private void ensure_default_spellbook() {
        if (!this.spellbook_spell_keys.isEmpty()) {
            return;
        }

        List<String> defaults = Spell_Library.get_wizard_spell_keys_up_to_level(1);
        for (int i = 0; i < Math.min(6, defaults.size()); i++) {
            this.spellbook_spell_keys.add(defaults.get(i));
        }
        auto_prepare_spells();
    }

    private void ensure_default_cantrips() {
        if (!this.known_cantrip_keys.isEmpty()) {
            return;
        }

        List<String> defaults = Spell_Library.get_wizard_cantrip_keys();
        for (int i = 0; i < Math.min(this.cantrips_known, defaults.size()); i++) {
            this.known_cantrip_keys.add(defaults.get(i));
        }
    }

    private void trim_known_cantrips() {
        while (this.known_cantrip_keys.size() > this.cantrips_known) {
            this.known_cantrip_keys.remove(this.known_cantrip_keys.size() - 1);
        }
    }

    private void trim_spellbook_to_capacity() {
        while (this.spellbook_spell_keys.size() > this.spells_in_spellbook) {
            String removed = this.spellbook_spell_keys.remove(this.spellbook_spell_keys.size() - 1);
            this.prepared_spell_keys.remove(removed);
        }
    }

    private void trim_prepared_spells_to_valid_selection() {
        List<String> validPrepared = new ArrayList<>();
        for (String spellKey : this.prepared_spell_keys) {
            if (this.spellbook_spell_keys.contains(spellKey) && !validPrepared.contains(spellKey)) {
                validPrepared.add(spellKey);
            }
        }
        this.prepared_spell_keys.clear();
        this.prepared_spell_keys.addAll(validPrepared);
        if (this.prepared_spell_keys.isEmpty()) {
            auto_prepare_spells();
        }
    }

    private void auto_prepare_spells() {
        int maxPrepared = Math.max(1, this.prepared_spell_limit);
        for (String spellKey : this.spellbook_spell_keys) {
            if (this.prepared_spell_keys.size() >= maxPrepared) {
                break;
            }
            if (!this.prepared_spell_keys.contains(spellKey)) {
                this.prepared_spell_keys.add(spellKey);
            }
        }
    }

    private void apply_level_features(int level) {
        if (level == 2) {
            apply_subclass_features(2);
        } else if (level == 18) {
            this.traits.add("法术精通 (Spell Mastery)：选择一个 1 环法术和一个 2 环法术，可不消耗法术位反复施放。");
        } else if (level == 20) {
            this.traits.add("招牌法术 (Signature Spells)：选择两个 3 环法师法术作为招牌法术，每次长休后可各免费施放一次。");
        } else if (level == 3 || level == 5 || level == 7 || level == 9 || level == 11 || level == 13 || level == 15 || level == 17) {
            this.traits.add("法术成长：可以学习更高环级法术，并将新法术抄录进法术书。");
        }

        if (level == 4 || level == 10) {
            this.traits.add("戏法成长：你的已知戏法数量提升。");
        }

        if ((level == 6 || level == 10 || level == 14) && this.wizard_subclass != Wizard_Subclass.NONE) {
            apply_subclass_features(level);
        }
    }

    private int get_cantrips_known_for_level(int level) {
        if (level >= 10) {
            return 5;
        }
        if (level >= 4) {
            return 4;
        }
        return 3;
    }

    private void apply_subclass_features(int level) {
        if (this.wizard_subclass == Wizard_Subclass.NONE) {
            return;
        }

        switch (this.wizard_subclass) {
            case ABJURATION:
                if (level == 2) {
                    this.traits.add("防护学派 2级 - 防护专精 (Abjuration Savant)：抄录防护系法术时，时间与金钱成本减半。");
                    this.traits.add("防护学派 2级 - 奥术结界 (Arcane Ward)：施放防护系法术会生成结界，为你吸收伤害。");
                } else if (level == 6) {
                    this.traits.add("防护学派 6级 - 投射结界 (Projected Ward)：可用反应让结界替附近盟友吸收伤害。");
                } else if (level == 10) {
                    this.traits.add("防护学派 10级 - 强化防护 (Improved Abjuration)：进行反制法术或解除魔法时，将熟练加值加入检定。");
                } else if (level == 14) {
                    this.traits.add("防护学派 14级 - 法术抗性 (Spell Resistance)：对法术伤害拥有抗性，对抗法术的豁免检定具有优势。");
                }
                break;
            case CONJURATION:
                if (level == 2) {
                    this.traits.add("咒法学派 2级 - 咒法专精 (Conjuration Savant)：抄录咒法系法术时，时间与金钱成本减半。");
                    this.traits.add("咒法学派 2级 - 次级造物 (Minor Conjuration)：可临时 conjure 小型非魔法物体。");
                } else if (level == 6) {
                    this.traits.add("咒法学派 6级 - 良性迁跃 (Benign Transposition)：你或盟友可在短距离内进行安全传送。");
                } else if (level == 10) {
                    this.traits.add("咒法学派 10级 - 专注咒缚 (Focused Conjuration)：维持咒法系召唤法术时不因受伤而失去专注。");
                } else if (level == 14) {
                    this.traits.add("咒法学派 14级 - 坚固召唤 (Durable Summons)：你的召唤生物获得额外临时生命值。");
                }
                break;
            case DIVINATION:
                if (level == 2) {
                    this.traits.add("预言学派 2级 - 预言专精 (Divination Savant)：抄录预言系法术时，时间与金钱成本减半。");
                    this.traits.add("预言学派 2级 - 预兆 (Portent)：长休后掷两颗 d20，可替换你看到的攻击、豁免或检定结果。");
                } else if (level == 6) {
                    this.traits.add("预言学派 6级 - 专家预言 (Expert Divination)：施放预言系法术后可回复较低环级法术位。");
                } else if (level == 10) {
                    this.traits.add("预言学派 10级 - 第三只眼 (The Third Eye)：短时间获得黑暗视觉、识破隐形等感知强化。");
                } else if (level == 14) {
                    this.traits.add("预言学派 14级 - 强化预兆 (Greater Portent)：每日可储存并操控三次预兆骰。");
                }
                break;
            case ENCHANTMENT:
                if (level == 2) {
                    this.traits.add("惑控学派 2级 - 惑控专精 (Enchantment Savant)：抄录惑控系法术时，时间与金钱成本减半。");
                    this.traits.add("惑控学派 2级 - 催眠凝视 (Hypnotic Gaze)：近距离凝视可令生物失能且速度归零。");
                } else if (level == 6) {
                    this.traits.add("惑控学派 6级 - 本能魅惑 (Instinctive Charm)：让攻击你的敌人临时转而攻击其他目标。");
                } else if (level == 10) {
                    this.traits.add("惑控学派 10级 - 双重惑控 (Split Enchantment)：单体惑控法术可同时影响第二个目标。");
                } else if (level == 14) {
                    this.traits.add("惑控学派 14级 - 篡改记忆 (Alter Memories)：你可以重塑目标对被惑控过程的记忆。");
                }
                break;
            case EVOCATION:
                if (level == 2) {
                    this.traits.add("塑能学派 2级 - 塑能专精 (Evocation Savant)：抄录塑能系法术时，时间与金钱成本减半。");
                    this.traits.add("塑能学派 2级 - 塑能雕琢 (Sculpt Spells)：施放范围法术时，可保护少量盟友免受伤害。");
                } else if (level == 6) {
                    this.traits.add("塑能学派 6级 - 强效戏法 (Potent Cantrip)：敌人即使通过你戏法的豁免，仍会承受一半伤害。");
                } else if (level == 10) {
                    this.traits.add("塑能学派 10级 - 强化塑能 (Empowered Evocation)：塑能法术伤害额外加入智力调整值。");
                } else if (level == 14) {
                    this.traits.add("塑能学派 14级 - 超限塑能 (Overchannel)：低环伤害法术可以直接取最大伤害，但重复使用会反噬自己。");
                }
                break;
            case ILLUSION:
                if (level == 2) {
                    this.traits.add("幻术学派 2级 - 幻术专精 (Illusion Savant)：抄录幻术系法术时，时间与金钱成本减半。");
                    this.traits.add("幻术学派 2级 - 强化次级幻影 (Improved Minor Illusion)：次级幻影可同时制造声音与图像。");
                } else if (level == 6) {
                    this.traits.add("幻术学派 6级 - 可塑幻象 (Malleable Illusions)：可在法术持续期间改变自己的幻术细节。");
                } else if (level == 10) {
                    this.traits.add("幻术学派 10级 - 幻影替身 (Illusory Self)：在被命中时可用反应令攻击落空。");
                } else if (level == 14) {
                    this.traits.add("幻术学派 14级 - 幻象成真 (Illusory Reality)：短暂让大型以下的幻术物件变为真实。");
                }
                break;
            case NECROMANCY:
                if (level == 2) {
                    this.traits.add("死灵学派 2级 - 死灵专精 (Necromancy Savant)：抄录死灵系法术时，时间与金钱成本减半。");
                    this.traits.add("死灵学派 2级 - 凋零收割 (Grim Harvest)：以法术杀死生物时回复生命值。");
                } else if (level == 6) {
                    this.traits.add("死灵学派 6级 - 亡者仆从 (Undead Thralls)：你操纵的亡灵更强，且可额外制造亡灵。");
                } else if (level == 10) {
                    this.traits.add("死灵学派 10级 - 死亡亲和 (Inured to Undeath)：获得对死灵伤害的抗性，且最大生命值不易被降低。");
                } else if (level == 14) {
                    this.traits.add("死灵学派 14级 - 命令亡灵 (Command Undead)：强行控制一个亡灵生物。");
                }
                break;
            case TRANSMUTATION:
                if (level == 2) {
                    this.traits.add("变化学派 2级 - 变化专精 (Transmutation Savant)：抄录变化系法术时，时间与金钱成本减半。");
                    this.traits.add("变化学派 2级 - 次级炼成 (Minor Alchemy)：短暂改变物质的材质属性。");
                } else if (level == 6) {
                    this.traits.add("变化学派 6级 - 变化贤者之石 (Transmuter's Stone)：制作贤者之石，为持有者提供多种强化。");
                } else if (level == 10) {
                    this.traits.add("变化学派 10级 - 变形者 (Shapechanger)：可免费对自己施放变形术。");
                } else if (level == 14) {
                    this.traits.add("变化学派 14级 - 变化大师 (Master Transmuter)：牺牲贤者之石来复活、治疗、变形或转化资源。");
                }
                break;
            default:
                break;
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

    public int get_max_spell_level() {
        for (int spellLevel = 9; spellLevel >= 1; spellLevel--) {
            if (this.spell_slots[spellLevel] > 0) {
                return spellLevel;
            }
        }
        return 1;
    }

    public List<String> get_available_cantrip_options() {
        List<String> options = Spell_Library.get_wizard_cantrip_keys();
        options.removeAll(this.known_cantrip_keys);
        return options;
    }

    public List<String> get_available_spellbook_options() {
        List<String> options = Spell_Library.get_wizard_spell_keys_up_to_level(get_max_spell_level());
        options.removeAll(this.spellbook_spell_keys);
        return options;
    }

    public void set_known_cantrips(List<String> spell_keys) {
        this.known_cantrip_keys.clear();
        for (String spellKey : spell_keys) {
            if (!this.known_cantrip_keys.contains(spellKey) && this.known_cantrip_keys.size() < this.cantrips_known) {
                this.known_cantrip_keys.add(spellKey);
            }
        }
    }

    public void learn_spells(List<String> spell_keys) {
        for (String spellKey : spell_keys) {
            if (!this.spellbook_spell_keys.contains(spellKey) && this.spellbook_spell_keys.size() < this.spells_in_spellbook) {
                this.spellbook_spell_keys.add(spellKey);
            }
        }
        trim_prepared_spells_to_valid_selection();
    }

    public void forget_spells(List<String> spell_keys) {
        this.spellbook_spell_keys.removeAll(spell_keys);
        this.prepared_spell_keys.removeAll(spell_keys);
        auto_prepare_spells();
    }

    public void set_prepared_spells(List<String> spell_keys, int intelligence_modifier) {
        int maxPrepared = get_prepared_spell_count(intelligence_modifier);
        this.prepared_spell_keys.clear();
        for (String spellKey : spell_keys) {
            if (this.spellbook_spell_keys.contains(spellKey)
                    && !this.prepared_spell_keys.contains(spellKey)
                    && this.prepared_spell_keys.size() < maxPrepared) {
                this.prepared_spell_keys.add(spellKey);
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

    public List<String> get_spellbook_lines() {
        List<String> lines = new ArrayList<>();
        for (String spellKey : this.spellbook_spell_keys) {
            Spell_Definition spell = Spell_Library.get_spell(spellKey);
            if (spell != null) {
                lines.add(spell.to_detail_line());
            }
        }
        return lines;
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
        return 4;
    }

    @Override
    public String get_subclass_name() {
        switch (this.wizard_subclass) {
            case ABJURATION:
                return "防护学派";
            case CONJURATION:
                return "咒法学派";
            case DIVINATION:
                return "预言学派";
            case ENCHANTMENT:
                return "惑控学派";
            case EVOCATION:
                return "塑能学派";
            case ILLUSION:
                return "幻术学派";
            case NECROMANCY:
                return "死灵学派";
            case TRANSMUTATION:
                return "变化学派";
            default:
                return "未选择";
        }
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>();
        summaries.addAll(this.traits);
        summaries.add("戏法已知数： " + this.cantrips_known);
        summaries.add("法术书容量： " + this.spells_in_spellbook);
        summaries.add("可准备法术数： 法师等级 " + this.prepared_spell_limit + " + 智力调整值");
        summaries.add("奥术回能额度：可恢复总环级不超过 " + this.arcane_recovery_level);
        if (!this.feat_names.isEmpty()) {
            for (String feat_name : this.feat_names) {
                summaries.add("专长 - " + Feat_Library.get_summary_line(feat_name));
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
                sb.append(spellLevel).append("环 ").append(this.current_spell_slots[spellLevel]).append("/").append(this.spell_slots[spellLevel]);
                hasAny = true;
            }
        }
        if (!hasAny) {
            sb.append("暂无");
        }
        return sb.toString();
    }

    public int get_prepared_spell_count(int intelligence_modifier) {
        return Math.max(1, this.prepared_spell_limit + intelligence_modifier);
    }

    @Override
    public void restore_long_rest_resources() {
        this.current_spell_slots = this.spell_slots.clone();
    }

    @Override
    public void sync_from_combatant(Combatant combatant) {
        if (combatant == null || combatant.spell_slots_remaining == null) {
            return;
        }
        int[] remaining = combatant.spell_slots_remaining;
        for (int spellLevel = 1; spellLevel < this.current_spell_slots.length; spellLevel++) {
            int current = spellLevel < remaining.length ? remaining[spellLevel] : 0;
            this.current_spell_slots[spellLevel] = Math.max(0, Math.min(current, this.spell_slots[spellLevel]));
        }
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();
        if (this.current_level >= 2 && this.wizard_subclass == Wizard_Subclass.NONE) {
            pending.add("选择奥术传承");
        }
        int pendingCantrips = Math.max(0, this.cantrips_known - this.known_cantrip_keys.size());
        if (pendingCantrips > 0) {
            pending.add("选择 " + pendingCantrips + " 个法师戏法");
        }
        int pendingSpellbook = Math.max(0, this.spells_in_spellbook - this.spellbook_spell_keys.size());
        if (pendingSpellbook > 0) {
            pending.add("将 " + pendingSpellbook + " 个新法术抄录进法术书");
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
        state.put("known_cantrips", Persistence_Util.encode_list(this.known_cantrip_keys));
        state.put("spellbook", Persistence_Util.encode_list(this.spellbook_spell_keys));
        state.put("prepared", Persistence_Util.encode_list(this.prepared_spell_keys));
        state.put("current_spell_slots", Persistence_Util.encode_int_array(this.current_spell_slots));
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String subclass = class_state.get("subclass");
        if (subclass != null && !subclass.trim().isEmpty()) {
            this.wizard_subclass = Wizard_Subclass.valueOf(subclass);
        }
        this.known_cantrip_keys.clear();
        this.known_cantrip_keys.addAll(Persistence_Util.decode_list(class_state.get("known_cantrips")));
        this.spellbook_spell_keys.clear();
        this.spellbook_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("spellbook")));
        this.prepared_spell_keys.clear();
        this.prepared_spell_keys.addAll(Persistence_Util.decode_list(class_state.get("prepared")));
        if (class_state.containsKey("current_spell_slots")) {
            this.current_spell_slots = Persistence_Util.decode_int_array(class_state.get("current_spell_slots"), 10);
            this.preserve_loaded_current_slots = true;
        }
        rebuild_progression();
    }
}
