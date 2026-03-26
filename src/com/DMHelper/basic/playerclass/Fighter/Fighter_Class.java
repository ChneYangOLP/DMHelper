package com.DMHelper.basic.playerclass.Fighter;

import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.database.Persistence_Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Fighter_Class extends Character_Class {

    public int action_surge_uses;
    public int current_action_surge_uses;
    public int indomitable_uses;
    public int current_indomitable_uses;
    public int attacks_per_action;

    public int superiority_dice;
    public int current_superiority_dice;
    public int superiority_dice_type;
    public List<String> maneuver_names;

    public Fighter_Subclass fighter_subclass;
    public int pending_asi_count;

    public List<Trait> traits;
    public String fighting_style_name;
    public String extra_fighting_style_name;
    private boolean preserve_loaded_action_surge;
    private boolean preserve_loaded_indomitable;
    private boolean preserve_loaded_superiority_dice;

    public Fighter_Class() {
        super("FIGHTER", "战士 (Fighter)", 10);

        this.action_surge_uses = 0;
        this.current_action_surge_uses = 0;
        this.indomitable_uses = 0;
        this.current_indomitable_uses = 0;
        this.attacks_per_action = 1;
        this.superiority_dice = 0;
        this.current_superiority_dice = 0;
        this.superiority_dice_type = 0;
        this.pending_asi_count = 0;
        this.fighter_subclass = Fighter_Subclass.NONE;
        this.traits = new ArrayList<>();
        this.maneuver_names = new ArrayList<>();

        this.skill_choose_count = 2;
        this.skill_options.add("Acrobatics (杂技)");
        this.skill_options.add("Animal Handling (驯兽)");
        this.skill_options.add("Athletics (运动)");
        this.skill_options.add("History (历史)");
        this.skill_options.add("Insight (洞悉)");
        this.skill_options.add("Intimidation (威吓)");
        this.skill_options.add("Perception (察觉)");
        this.skill_options.add("Survival (生存)");

        this.saving_throws.add("Strength");
        this.saving_throws.add("Constitution");
        this.equipment_proficiencies.add("所有护甲");
        this.equipment_proficiencies.add("盾牌");
        this.equipment_proficiencies.add("简易武器");
        this.equipment_proficiencies.add("军用武器");
    }

    @Override
    public void rebuild_progression() {
        int previousActionSurge = this.action_surge_uses;
        int previousIndomitable = this.indomitable_uses;
        int previousSuperiorityDice = this.superiority_dice;
        this.action_surge_uses = 0;
        this.indomitable_uses = 0;
        this.attacks_per_action = 1;
        this.superiority_dice = 0;
        this.superiority_dice_type = 0;
        this.pending_asi_count = 0;
        this.traits.clear();

        add_base_traits();

        for (int level = 2; level <= this.current_level; level++) {
            apply_level_features(level);
        }

        if (!this.preserve_loaded_action_surge
                && previousActionSurge == 0
                && this.action_surge_uses > 0
                && this.current_action_surge_uses == 0) {
            this.current_action_surge_uses = this.action_surge_uses;
        }
        if (!this.preserve_loaded_indomitable
                && previousIndomitable == 0
                && this.indomitable_uses > 0
                && this.current_indomitable_uses == 0) {
            this.current_indomitable_uses = this.indomitable_uses;
        }
        if (!this.preserve_loaded_superiority_dice
                && previousSuperiorityDice == 0
                && this.superiority_dice > 0
                && this.current_superiority_dice == 0) {
            this.current_superiority_dice = this.superiority_dice;
        }
        this.current_action_surge_uses = Math.max(0, Math.min(this.current_action_surge_uses, this.action_surge_uses));
        this.current_indomitable_uses = Math.max(0, Math.min(this.current_indomitable_uses, this.indomitable_uses));
        this.current_superiority_dice = Math.max(0, Math.min(this.current_superiority_dice, this.superiority_dice));
        this.preserve_loaded_action_surge = false;
        this.preserve_loaded_indomitable = false;
        this.preserve_loaded_superiority_dice = false;

        int earned_asi_choices = get_earned_asi_choices();
        this.pending_asi_count = Math.max(0, earned_asi_choices - this.used_asi_choices);
    }

    private void add_base_traits() {
        this.traits.add(new Trait("战斗风格 (Fighting Style)", get_fighting_style_description(this.fighting_style_name)));
        this.traits.add(new Trait("复苏之风 (Second Wind)", "你的回合内可用附赠动作恢复 1d10 + 战士等级 的生命值。短休或长休后恢复。"));
    }

    private void apply_level_features(int level) {
        switch (level) {
            case 2:
                this.action_surge_uses = 1;
                this.traits.add(new Trait("动作如潮 (Action Surge)", "在你的回合内额外进行一个动作。短休或长休后恢复。"));
                break;
            case 3:
                apply_subclass_features(3);
                break;
            case 4:
            case 6:
            case 8:
            case 12:
            case 14:
            case 16:
            case 19:
                break;
            case 5:
                this.attacks_per_action = 2;
                this.traits.add(new Trait("额外攻击 (Extra Attack)", "在你的回合内执行攻击动作时，可以攻击两次。"));
                break;
            case 7:
            case 10:
            case 15:
            case 18:
                apply_subclass_features(level);
                break;
            case 9:
                this.indomitable_uses = 1;
                this.traits.add(new Trait("不屈 (Indomitable)", "豁免失败时可以重掷，必须使用新结果。长休后恢复。"));
                break;
            case 11:
                this.attacks_per_action = 3;
                update_extra_attack_trait("在你的回合内执行攻击动作时，可以攻击三次。");
                break;
            case 13:
                this.indomitable_uses = 2;
                break;
            case 17:
                this.action_surge_uses = 2;
                this.indomitable_uses = 3;
                break;
            case 20:
                this.attacks_per_action = 4;
                update_extra_attack_trait("在你的回合内执行攻击动作时，可以攻击四次。");
                break;
            default:
                break;
        }
    }

    private void update_extra_attack_trait(String description) {
        for (Trait trait : this.traits) {
            if (trait.name.contains("额外攻击")) {
                trait.description = description;
            }
        }
    }

    private void apply_subclass_features(int level) {
        if (this.fighter_subclass == Fighter_Subclass.NONE) {
            return;
        }

        if (this.fighter_subclass == Fighter_Subclass.CHAMPION) {
            if (level == 3) {
                this.traits.add(new Trait("冠军勇士 3级 - 卓越重击 (Improved Critical)", "你的武器攻击在攻击骰掷出 19 或 20 时造成重击，让重击范围提前开启。"));
            } else if (level == 7) {
                this.traits.add(new Trait("冠军勇士 7级 - 卓越运动 (Remarkable Athlete)", "未熟练的力量、敏捷或体质检定获得半个熟练加值（向上取整），并提升跳跃能力。"));
            } else if (level == 10) {
                this.traits.add(new Trait("冠军勇士 10级 - 额外战斗风格 (Additional Fighting Style)", get_fighting_style_description(this.extra_fighting_style_name)));
            } else if (level == 15) {
                this.traits.add(new Trait("冠军勇士 15级 - 高等重击 (Superior Critical)", "你的武器攻击在攻击骰掷出 18-20 时造成重击。"));
            } else if (level == 18) {
                this.traits.add(new Trait("冠军勇士 18级 - 幸存者 (Survivor)", "若生命值低于最大值一半且未失能，则每回合开始时自动恢复 5 + 体质调整值 的生命值。"));
            }
        } else if (this.fighter_subclass == Fighter_Subclass.BATTLE_MASTER) {
            if (level == 3) {
                this.superiority_dice = 4;
                this.superiority_dice_type = 8;
                this.traits.add(new Trait("战斗大师 3级 - 战斗卓越 (Combat Superiority)", "获得 4 颗 d8 卓越骰，短休或长休后恢复；并学习 3 个战技来强化攻击、防御与支援能力。"));
                this.traits.add(new Trait("战斗大师 3级 - 战场学识 (Student of War)", "获得一种工匠工具熟练项，体现战场研究与训练。"));
            } else if (level == 7) {
                this.superiority_dice = 5;
                this.traits.add(new Trait("战斗大师 7级 - 知己知彼 (Know Your Enemy)", "战斗外观察目标至少 1 分钟后，可以比较力量、敏捷、体质、AC、当前生命值、总等级或战士等级等战斗信息。"));
            } else if (level == 10) {
                this.superiority_dice_type = 10;
                this.traits.add(new Trait("战斗大师 10级 - 强化战斗卓越 (Improved Combat Superiority)", "卓越骰从 d8 提升为 d10，并再学习 2 个战技。"));
            } else if (level == 15) {
                this.superiority_dice = 6;
                this.traits.add(new Trait("战斗大师 15级 - 不懈 (Relentless)", "若掷先攻时没有任何卓越骰，则立刻恢复 1 颗卓越骰。"));
            } else if (level == 18) {
                this.superiority_dice_type = 12;
                this.traits.add(new Trait("战斗大师 18级 - 终极战斗卓越 (Combat Superiority Improvement)", "卓越骰从 d10 提升为 d12。"));
            }
        }
    }

    public String get_fighting_style_description(String style_name) {
        if ("Archery".equals(style_name)) {
            return "箭术 (Archery)：你使用远程武器进行的攻击检定获得 +2 加值。";
        }
        if ("Defense".equals(style_name)) {
            return "防御 (Defense)：当你穿戴护甲时，AC 获得 +1 加值。";
        }
        if ("Dueling".equals(style_name)) {
            return "对决 (Dueling)：单手持用一把近战武器且未装备其他武器时，该武器伤害掷骰获得 +2 加值。";
        }
        if ("Great Weapon Fighting".equals(style_name)) {
            return "巨武器战斗 (Great Weapon Fighting)：双手持用近战武器攻击并掷出 1 或 2 时可以重掷一次。";
        }
        if ("Protection".equals(style_name)) {
            return "保护 (Protection)：持盾时可用反应使攻击你邻近盟友的攻击检定陷入劣势。";
        }
        if ("Two-Weapon Fighting".equals(style_name)) {
            return "双武器战斗 (Two-Weapon Fighting)：双持附赠动作攻击时，可以将属性调整值加入伤害。";
        }
        return "待选择";
    }

    public String get_fighting_style_label(String style_name) {
        if ("Archery".equals(style_name)) {
            return "箭术 (Archery)";
        }
        if ("Defense".equals(style_name)) {
            return "防御 (Defense)";
        }
        if ("Dueling".equals(style_name)) {
            return "对决 (Dueling)";
        }
        if ("Great Weapon Fighting".equals(style_name)) {
            return "巨武器战斗 (Great Weapon Fighting)";
        }
        if ("Protection".equals(style_name)) {
            return "保护 (Protection)";
        }
        if ("Two-Weapon Fighting".equals(style_name)) {
            return "双武器战斗 (Two-Weapon Fighting)";
        }
        return "待选择";
    }

    public List<String> get_available_fighting_styles(boolean is_extra_style) {
        List<String> styles = new ArrayList<>();
        styles.add("Archery");
        styles.add("Defense");
        styles.add("Dueling");
        styles.add("Great Weapon Fighting");
        styles.add("Protection");
        styles.add("Two-Weapon Fighting");

        if (is_extra_style && this.fighting_style_name != null) {
            styles.remove(this.fighting_style_name);
        }
        return styles;
    }

    public int get_expected_maneuver_count() {
        if (this.fighter_subclass != Fighter_Subclass.BATTLE_MASTER) {
            return 0;
        }

        int count = 3;
        if (this.current_level >= 7) {
            count += 2;
        }
        if (this.current_level >= 10) {
            count += 2;
        }
        if (this.current_level >= 15) {
            count += 2;
        }
        return count;
    }

    public List<String> get_available_maneuvers() {
        List<String> maneuvers = new ArrayList<>();
        maneuvers.add("Disarming Attack");
        maneuvers.add("Precision Attack");
        maneuvers.add("Riposte");
        maneuvers.add("Trip Attack");
        maneuvers.add("Menacing Attack");
        maneuvers.add("Parry");
        maneuvers.add("Pushing Attack");
        maneuvers.add("Rally");
        maneuvers.removeAll(this.maneuver_names);
        return maneuvers;
    }

    public String get_maneuver_description(String maneuver_name) {
        if ("Disarming Attack".equals(maneuver_name)) {
            return "命中时消耗 1 颗卓越骰，将卓越骰点数加入伤害；目标若力量豁免失败，则会丢掉手中一件物品。";
        }
        if ("Precision Attack".equals(maneuver_name)) {
            return "在武器攻击检定前或检定后、结果宣布前消耗 1 颗卓越骰，将骰值加入本次命中判定。";
        }
        if ("Riposte".equals(maneuver_name)) {
            return "有生物近战攻击未命中你时，你可用反应与 1 颗卓越骰立刻进行一次近战武器反击，并将骰值加入伤害。";
        }
        if ("Trip Attack".equals(maneuver_name)) {
            return "命中时消耗 1 颗卓越骰并把骰值加入伤害；大型及以下目标若力量豁免失败，则被击倒。";
        }
        if ("Menacing Attack".equals(maneuver_name)) {
            return "命中时消耗 1 颗卓越骰并把骰值加入伤害；目标若感知豁免失败，则在下一回合结束前陷入恐慌。";
        }
        if ("Parry".equals(maneuver_name)) {
            return "当你被近战攻击造成伤害时，可用反应与 1 颗卓越骰减少伤害，减伤值为骰值 + 敏捷调整值。";
        }
        if ("Pushing Attack".equals(maneuver_name)) {
            return "命中时消耗 1 颗卓越骰并把骰值加入伤害；若目标体型不大于大型且力量豁免失败，则被推开最多 15 尺。";
        }
        if ("Rally".equals(maneuver_name)) {
            return "用附赠动作鼓舞一名能听见你的盟友，消耗 1 颗卓越骰，使其获得等于骰值 + 魅力调整值的临时生命值。";
        }
        return maneuver_name;
    }

    public String get_maneuver_label(String maneuver_name) {
        if ("Disarming Attack".equals(maneuver_name)) {
            return "缴械攻击 (Disarming Attack)";
        }
        if ("Precision Attack".equals(maneuver_name)) {
            return "精准攻击 (Precision Attack)";
        }
        if ("Riposte".equals(maneuver_name)) {
            return "反击 (Riposte)";
        }
        if ("Trip Attack".equals(maneuver_name)) {
            return "绊摔攻击 (Trip Attack)";
        }
        if ("Menacing Attack".equals(maneuver_name)) {
            return "威吓攻击 (Menacing Attack)";
        }
        if ("Parry".equals(maneuver_name)) {
            return "招架 (Parry)";
        }
        if ("Pushing Attack".equals(maneuver_name)) {
            return "推击攻击 (Pushing Attack)";
        }
        if ("Rally".equals(maneuver_name)) {
            return "鼓舞 (Rally)";
        }
        return maneuver_name;
    }

    @Override
    public int get_extra_armor_class_bonus(String armor_type) {
        boolean hasDefenseStyle = "Defense".equals(this.fighting_style_name)
                || "Defense".equals(this.extra_fighting_style_name);
        if (hasDefenseStyle && !"None".equals(armor_type)) {
            return 1;
        }
        return 0;
    }

    public int get_earned_asi_choices() {
        int count = 0;
        int[] levels = {4, 6, 8, 12, 14, 16, 19};
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
        return 6;
    }

    @Override
    public String get_subclass_name() {
        if (this.fighter_subclass == Fighter_Subclass.CHAMPION) {
            return "冠军勇士";
        }
        if (this.fighter_subclass == Fighter_Subclass.BATTLE_MASTER) {
            return "战斗大师";
        }
        return "未选择";
    }

    @Override
    public List<String> get_feature_summaries() {
        List<String> summaries = new ArrayList<>();
        for (Trait trait : this.traits) {
            summaries.add(trait.name + "： " + trait.description);
        }
        if (!this.maneuver_names.isEmpty()) {
            for (String maneuver_name : this.maneuver_names) {
                summaries.add("战技 - " + get_maneuver_label(maneuver_name) + "： " + get_maneuver_description(maneuver_name));
            }
        }
        if (!this.feat_names.isEmpty()) {
            for (String feat_name : this.feat_names) {
                summaries.add("专长 - " + Feat_Library.get_summary_line(feat_name));
            }
        }
        summaries.add("动作如潮： " + get_action_surge_summary());
        summaries.add("不屈： " + get_indomitable_summary());
        if (this.superiority_dice > 0) {
            summaries.add("卓越骰： " + get_superiority_dice_summary());
        }
        return summaries;
    }

    @Override
    public List<String> get_pending_choices() {
        List<String> pending = new ArrayList<>();

        if (this.fighting_style_name == null || this.fighting_style_name.trim().isEmpty()) {
            pending.add("选择战斗风格");
        }
        if (this.current_level >= 3 && this.fighter_subclass == Fighter_Subclass.NONE) {
            pending.add("选择战士子职业");
        }
        if (this.current_level >= 10
                && this.fighter_subclass == Fighter_Subclass.CHAMPION
                && (this.extra_fighting_style_name == null || this.extra_fighting_style_name.trim().isEmpty())) {
            pending.add("选择冠军勇士的额外战斗风格");
        }
        if (this.fighter_subclass == Fighter_Subclass.BATTLE_MASTER) {
            int pendingManeuvers = Math.max(0, get_expected_maneuver_count() - this.maneuver_names.size());
            if (pendingManeuvers > 0) {
                pending.add("选择 " + pendingManeuvers + " 个战技");
            }
        }
        if (this.pending_asi_count > 0) {
            pending.add("处理 " + this.pending_asi_count + " 次属性值提升/专长");
        }
        return pending;
    }

    @Override
    public Map<String, String> export_class_state() {
        Map<String, String> state = new LinkedHashMap<>();
        state.put("subclass", this.fighter_subclass.name());
        state.put("fighting_style", this.fighting_style_name == null ? "" : this.fighting_style_name);
        state.put("extra_fighting_style", this.extra_fighting_style_name == null ? "" : this.extra_fighting_style_name);
        state.put("maneuvers", Persistence_Util.encode_list(this.maneuver_names));
        state.put("current_action_surge", Integer.toString(this.current_action_surge_uses));
        state.put("current_indomitable", Integer.toString(this.current_indomitable_uses));
        state.put("current_superiority_dice", Integer.toString(this.current_superiority_dice));
        return state;
    }

    @Override
    public void import_class_state(Map<String, String> class_state) {
        String subclass = class_state.get("subclass");
        if (subclass != null && !subclass.trim().isEmpty()) {
            this.fighter_subclass = Fighter_Subclass.valueOf(subclass);
        }
        this.fighting_style_name = empty_to_null(class_state.get("fighting_style"));
        this.extra_fighting_style_name = empty_to_null(class_state.get("extra_fighting_style"));
        this.maneuver_names.clear();
        this.maneuver_names.addAll(Persistence_Util.decode_list(class_state.get("maneuvers")));
        if (class_state.containsKey("current_action_surge")) {
            try {
                this.current_action_surge_uses = Integer.parseInt(class_state.get("current_action_surge"));
                this.preserve_loaded_action_surge = true;
            } catch (NumberFormatException ignored) {
                this.current_action_surge_uses = 0;
            }
        }
        if (class_state.containsKey("current_indomitable")) {
            try {
                this.current_indomitable_uses = Integer.parseInt(class_state.get("current_indomitable"));
                this.preserve_loaded_indomitable = true;
            } catch (NumberFormatException ignored) {
                this.current_indomitable_uses = 0;
            }
        }
        if (class_state.containsKey("current_superiority_dice")) {
            try {
                this.current_superiority_dice = Integer.parseInt(class_state.get("current_superiority_dice"));
                this.preserve_loaded_superiority_dice = true;
            } catch (NumberFormatException ignored) {
                this.current_superiority_dice = 0;
            }
        }
        rebuild_progression();
    }

    public String get_action_surge_summary() {
        return this.current_action_surge_uses + "/" + this.action_surge_uses;
    }

    public String get_indomitable_summary() {
        return this.current_indomitable_uses + "/" + this.indomitable_uses;
    }

    public String get_superiority_dice_summary() {
        if (this.superiority_dice <= 0) {
            return "暂无";
        }
        return this.current_superiority_dice + "/" + this.superiority_dice + " 颗 d" + this.superiority_dice_type;
    }

    @Override
    public void restore_long_rest_resources() {
        this.current_action_surge_uses = this.action_surge_uses;
        this.current_indomitable_uses = this.indomitable_uses;
        this.current_superiority_dice = this.superiority_dice;
    }

    @Override
    public void sync_from_combatant(Combatant combatant) {
        if (combatant == null) {
            return;
        }
        this.current_superiority_dice = Math.max(0, Math.min(combatant.superiority_dice_remaining, this.superiority_dice));
    }

    private String empty_to_null(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
