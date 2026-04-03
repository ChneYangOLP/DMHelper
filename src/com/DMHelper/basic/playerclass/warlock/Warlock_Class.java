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
    private static final class Invocation_Option {
        final String key;
        final String label;
        final String description;
        final int minLevel;
        final Warlock_Pact requiredPact;
        final String requiredSpellKey;

        Invocation_Option(String key, String label, String description, int minLevel, Warlock_Pact requiredPact, String requiredSpellKey) {
            this.key = key;
            this.label = label;
            this.description = description;
            this.minLevel = minLevel;
            this.requiredPact = requiredPact;
            this.requiredSpellKey = requiredSpellKey;
        }
    }

    public Warlock_Patron patron;
    public Warlock_Pact pact_boon;
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
        this.pact_boon = Warlock_Pact.NONE;
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
            this.traits.add("契约恩赐 (Pact Boon)：" + get_pact_boon_summary());
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
        for (Invocation_Option option : get_all_invocation_options()) {
            if (!this.invocation_keys.contains(option.key) && meets_invocation_prerequisite(option)) {
                options.add(option.key);
            }
        }
        return options;
    }

    public String get_invocation_label(String key) {
        Invocation_Option option = find_invocation_option(key);
        return option == null ? key : option.label;
    }

    public String get_invocation_description(String key) {
        Invocation_Option option = find_invocation_option(key);
        return option == null ? key : option.description;
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
        if (this.current_level >= 3) {
            summaries.add("契约恩赐详情： " + get_pact_boon_summary());
        }
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
        if (this.current_level >= 3 && this.pact_boon == Warlock_Pact.NONE) pending.add("选择契约恩赐");
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
        state.put("pact_boon", this.pact_boon.name());
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
        String pactValue = class_state.get("pact_boon");
        if (pactValue != null && !pactValue.trim().isEmpty()) this.pact_boon = Warlock_Pact.valueOf(pactValue);
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

    public String get_pact_boon_name() {
        if (this.pact_boon == Warlock_Pact.BLADE) return "刃之契约 (Pact of the Blade)";
        if (this.pact_boon == Warlock_Pact.CHAIN) return "链之契约 (Pact of the Chain)";
        if (this.pact_boon == Warlock_Pact.TOME) return "书之契约 (Pact of the Tome)";
        return "待选择";
    }

    public String get_pact_boon_summary() {
        if (this.pact_boon == Warlock_Pact.BLADE) {
            return get_pact_boon_name() + "：召唤并熟练使用契约武器，后续可解锁近战向祈请。";
        }
        if (this.pact_boon == Warlock_Pact.CHAIN) {
            return get_pact_boon_name() + "：获得强化魔宠，后续可解锁链契专属祈请。";
        }
        if (this.pact_boon == Warlock_Pact.TOME) {
            return get_pact_boon_name() + "：获得影之书与额外戏法，后续可解锁仪式施法祈请。";
        }
        return "获得额外的契约赠礼，但尚未选择具体恩赐。";
    }

    private boolean meets_invocation_prerequisite(Invocation_Option option) {
        if (this.current_level < option.minLevel) {
            return false;
        }
        if (option.requiredPact != Warlock_Pact.NONE && this.pact_boon != option.requiredPact) {
            return false;
        }
        if (option.requiredSpellKey != null && !option.requiredSpellKey.trim().isEmpty()) {
            return this.known_cantrip_keys.contains(option.requiredSpellKey) || this.known_spell_keys.contains(option.requiredSpellKey);
        }
        return true;
    }

    private Invocation_Option find_invocation_option(String key) {
        for (Invocation_Option option : get_all_invocation_options()) {
            if (option.key.equals(key)) {
                return option;
            }
        }
        return null;
    }

    private List<Invocation_Option> get_all_invocation_options() {
        List<Invocation_Option> options = new ArrayList<>();
        options.add(new Invocation_Option("Agonizing Blast", "苦痛魔能爆 (Agonizing Blast)", "你的魔能爆伤害额外加入魅力调整值。", 2, Warlock_Pact.NONE, "eldritch_blast"));
        options.add(new Invocation_Option("Armor of Shadows", "阴影护甲 (Armor of Shadows)", "可随意施放法师护甲，不消耗法术位。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Ascendant Step", "登天步 (Ascendant Step)", "9 级起可对自己随意施放浮空术。", 9, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Beast Speech", "兽语者 (Beast Speech)", "可随意施放与动物交谈。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Beguiling Influence", "蛊惑影响 (Beguiling Influence)", "获得欺瞒与游说熟练。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Bewitching Whispers", "惑心低语 (Bewitching Whispers)", "7 级起可用法术位每长休施放一次强迫术。", 7, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Book of Ancient Secrets", "古秘之书 (Book of Ancient Secrets)", "书契专属：影之书可抄录并施放 1 环仪式法术。", 2, Warlock_Pact.TOME, null));
        options.add(new Invocation_Option("Chains of Carceri", "卡瑟瑞锁链 (Chains of Carceri)", "15 级起，链契专属：可随意对天界、邪魔或元素生物施放怪物定身术。", 15, Warlock_Pact.CHAIN, null));
        options.add(new Invocation_Option("Devil's Sight", "魔鬼视界 (Devil's Sight)", "可在黑暗与魔法黑暗中正常视物，范围 120 尺。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Dreadful Word", "可怖真言 (Dreadful Word)", "7 级起可用法术位每长休施放一次困惑术。", 7, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Eldritch Sight", "异界视界 (Eldritch Sight)", "可随意施放侦测魔法。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Eldritch Spear", "魔能长枪 (Eldritch Spear)", "你的魔能爆射程提升至 300 尺。", 2, Warlock_Pact.NONE, "eldritch_blast"));
        options.add(new Invocation_Option("Eyes of the Rune Keeper", "符文守秘之眼 (Eyes of the Rune Keeper)", "你可以阅读一切文字。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Fiendish Vigor", "邪魔活力 (Fiendish Vigor)", "可随意施放虚假生命。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Gaze of Two Minds", "双心注视 (Gaze of Two Minds)", "接触一名自愿目标后，可借其感官观察世界。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Lifedrinker", "生命饮者 (Lifedrinker)", "12 级起，刃契专属：契约武器命中时额外造成魅力调整值的死灵伤害。", 12, Warlock_Pact.BLADE, null));
        options.add(new Invocation_Option("Mask of Many Faces", "千面伪装 (Mask of Many Faces)", "可随意施放易容术。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Master of Myriad Forms", "万相之主 (Master of Myriad Forms)", "15 级起可随意施放变身术。", 15, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Minions of Chaos", "混沌仆从 (Minions of Chaos)", "9 级起可用法术位每长休施放一次召唤元素。", 9, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Mire the Mind", "泥沼缠心 (Mire the Mind)", "5 级起可用法术位每长休施放一次缓慢术。", 5, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Misty Visions", "迷雾幻景 (Misty Visions)", "可随意施放寂静幻影。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("One with Shadows", "影中同化 (One with Shadows)", "5 级起，在昏暗或黑暗中静止时可令自己隐形。", 5, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Otherworldly Leap", "异界长跃 (Otherworldly Leap)", "9 级起可随意对自己施放跳跃术。", 9, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Repelling Blast", "斥退魔能爆 (Repelling Blast)", "你的魔能爆命中时可把目标推开最多 10 尺。", 2, Warlock_Pact.NONE, "eldritch_blast"));
        options.add(new Invocation_Option("Sculptor of Flesh", "血肉雕塑师 (Sculptor of Flesh)", "7 级起可用法术位每长休施放一次变形术。", 7, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Sign of Ill Omen", "凶兆印记 (Sign of Ill Omen)", "5 级起可用法术位每长休施放一次降咒术。", 5, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Thief of Five Fates", "五命窃贼 (Thief of Five Fates)", "可用法术位每长休施放一次灾祸术。", 2, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Thirsting Blade", "饥渴之刃 (Thirsting Blade)", "5 级起，刃契专属：使用契约武器执行攻击动作时可攻击两次。", 5, Warlock_Pact.BLADE, null));
        options.add(new Invocation_Option("Visions of Distant Realms", "远界幻见 (Visions of Distant Realms)", "15 级起可随意施放探知秘眼。", 15, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Voice of the Chain Master", "链主之声 (Voice of the Chain Master)", "链契专属：可透过魔宠感知，并能借其说话。", 2, Warlock_Pact.CHAIN, null));
        options.add(new Invocation_Option("Whispers of the Grave", "墓中低语 (Whispers of the Grave)", "9 级起可随意施放死者交谈。", 9, Warlock_Pact.NONE, null));
        options.add(new Invocation_Option("Witch Sight", "巫视 (Witch Sight)", "15 级起，可看穿 30 尺内变形者与幻术伪装的真实形态。", 15, Warlock_Pact.NONE, null));
        return options;
    }

    @Override
    public void restore_short_rest_resources() {
        this.current_pact_slot_count = this.pact_slot_count;
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
