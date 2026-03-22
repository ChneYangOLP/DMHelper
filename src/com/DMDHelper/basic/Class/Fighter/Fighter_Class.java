package com.DMDHelper.basic.Class.Fighter;

import com.DMDHelper.basic.Class.Character_Class;
import com.DMDHelper.basic.database.Persistence_Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Fighter_Class extends Character_Class {

    public int action_surge_uses;
    public int indomitable_uses;
    public int attacks_per_action;

    public int superiority_dice;
    public int superiority_dice_type;
    public List<String> maneuver_names;

    public Fighter_Subclass fighter_subclass;
    public int pending_asi_count;

    public List<Trait> traits;
    public String fighting_style_name;
    public String extra_fighting_style_name;

    public Fighter_Class() {
        super("FIGHTER", "战士 (Fighter)", 10);

        this.action_surge_uses = 0;
        this.indomitable_uses = 0;
        this.attacks_per_action = 1;
        this.superiority_dice = 0;
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

        int earned_asi_choices = get_earned_asi_choices();
        this.pending_asi_count = Math.max(0, earned_asi_choices - this.used_asi_choices);
    }

    private void add_base_traits() {
        this.traits.add(new Trait("战斗风格", get_fighting_style_description(this.fighting_style_name)));
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
                this.traits.add(new Trait("卓越重击 (Improved Critical)", "你的武器攻击在掷出 19 或 20 时造成重击。"));
            } else if (level == 7) {
                this.traits.add(new Trait("卓越运动 (Remarkable Athlete)", "未熟练的力量、敏捷或体质检定获得半熟练加值，且跳跃距离提高。"));
            } else if (level == 10) {
                this.traits.add(new Trait("额外战斗风格", get_fighting_style_description(this.extra_fighting_style_name)));
            } else if (level == 15) {
                this.traits.add(new Trait("高等重击 (Superior Critical)", "你的武器攻击在掷出 18-20 时造成重击。"));
            } else if (level == 18) {
                this.traits.add(new Trait("幸存者 (Survivor)", "若生命值低于一半且未失能，则每回合开始时恢复 5 + 体质调整值 的生命值。"));
            }
        } else if (this.fighter_subclass == Fighter_Subclass.BATTLE_MASTER) {
            if (level == 3) {
                this.superiority_dice = 4;
                this.superiority_dice_type = 8;
                this.traits.add(new Trait("战斗卓越 (Combat Superiority)", "获得 4 颗 d8 卓越骰，并学习 3 个战技。"));
                this.traits.add(new Trait("战场学识 (Student of War)", "获得一种工匠工具熟练项。"));
            } else if (level == 7) {
                this.superiority_dice = 5;
                this.traits.add(new Trait("知己知彼 (Know Your Enemy)", "战斗外观察目标至少 1 分钟，可比较若干关键战斗数值。"));
            } else if (level == 10) {
                this.superiority_dice_type = 10;
                this.traits.add(new Trait("强化战斗卓越", "卓越骰提升为 d10，并再学习 2 个战技。"));
            } else if (level == 15) {
                this.superiority_dice = 6;
                this.traits.add(new Trait("不懈 (Relentless)", "若掷先攻时没有卓越骰，立刻回复 1 颗。"));
            } else if (level == 18) {
                this.superiority_dice_type = 12;
                this.traits.add(new Trait("终极战斗卓越", "卓越骰提升为 d12。"));
            }
        }
    }

    public String get_fighting_style_description(String style_name) {
        if ("Archery".equals(style_name)) {
            return "箭术：你使用远程武器进行的攻击检定获得 +2 加值。";
        }
        if ("Defense".equals(style_name)) {
            return "防御：当你穿戴护甲时，AC 获得 +1 加值。";
        }
        if ("Dueling".equals(style_name)) {
            return "对决：单手持用一把近战武器且未装备其他武器时，该武器伤害掷骰获得 +2 加值。";
        }
        if ("Great Weapon Fighting".equals(style_name)) {
            return "巨武器战斗：双手持用近战武器攻击并掷出 1 或 2 时可以重掷一次。";
        }
        if ("Protection".equals(style_name)) {
            return "保护：持盾时可用反应使攻击你邻近盟友的攻击检定陷入劣势。";
        }
        if ("Two-Weapon Fighting".equals(style_name)) {
            return "双武器战斗：双持附赠动作攻击时，可以将属性调整值加入伤害。";
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
            return "缴械攻击：命中时消耗 1 颗卓越骰，目标失败则掉落手中物品。";
        }
        if ("Precision Attack".equals(maneuver_name)) {
            return "精准攻击：攻击检定前后消耗卓越骰，将结果加到命中判定。";
        }
        if ("Riposte".equals(maneuver_name)) {
            return "反击：敌人近战攻击未命中你时，用反应进行一次近战武器攻击。";
        }
        if ("Trip Attack".equals(maneuver_name)) {
            return "绊摔攻击：命中时让大型及以下目标进行力量豁免，否则倒地。";
        }
        if ("Menacing Attack".equals(maneuver_name)) {
            return "威吓攻击：命中时可让目标进行感知豁免，失败则陷入恐慌。";
        }
        if ("Parry".equals(maneuver_name)) {
            return "招架：被近战伤害命中时，用反应减少伤害。";
        }
        if ("Pushing Attack".equals(maneuver_name)) {
            return "推击攻击：命中时可将目标击退 15 尺。";
        }
        if ("Rally".equals(maneuver_name)) {
            return "鼓舞：用附赠动作让盟友获得临时生命值。";
        }
        return maneuver_name;
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
                summaries.add("战技 - " + maneuver_name + "： " + get_maneuver_description(maneuver_name));
            }
        }
        if (!this.feat_names.isEmpty()) {
            for (String feat_name : this.feat_names) {
                summaries.add("专长 - " + feat_name);
            }
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
        rebuild_progression();
    }

    private String empty_to_null(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }
}
