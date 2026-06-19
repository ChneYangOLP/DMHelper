package com.DMHelper.basic;

import com.DMHelper.basic.combat.Combat_Status_Effect;
import com.DMHelper.basic.combat.Combat_Status_Type;
import com.DMHelper.basic.combat.Dice_Util;

import java.util.List;

/**
 * D&D 5e 技能检定自动化工具类。
 * 提供技能检定、属性检定、豁免检定的执行与格式化输出，
 * 并支持从战斗状态效果中判定优势/劣势。
 */
public class Skill_Check {

    private Skill_Check() {
    }

    // ------------------------------------------------------------------ //
    //  技能检定结果
    // ------------------------------------------------------------------ //

    public static class Check_Result {
        public final String skill_name;       // 技能名
        public final int d20_roll;             // d20 掷骰值
        public final int bonus;                // 总加值
        public final int total;                // 总结果
        public final boolean advantage;        // 是否有优势
        public final boolean disadvantage;     // 是否有劣势
        public final boolean critical_success; // 自然20
        public final boolean critical_fail;    // 自然1
        public final String description;       // 描述文本

        public Check_Result(String skill_name,
                           int d20_roll,
                           int bonus,
                           int total,
                           boolean advantage,
                           boolean disadvantage,
                           boolean critical_success,
                           boolean critical_fail,
                           String description) {
            this.skill_name = skill_name;
            this.d20_roll = d20_roll;
            this.bonus = bonus;
            this.total = total;
            this.advantage = advantage;
            this.disadvantage = disadvantage;
            this.critical_success = critical_success;
            this.critical_fail = critical_fail;
            this.description = description;
        }
    }

    // ------------------------------------------------------------------ //
    //  核心：执行技能检定
    // ------------------------------------------------------------------ //

    /**
     * 执行技能检定。
     *
     * @param character       角色卡
     * @param skillName       技能名称（中文，如 "运动"、"察觉"）
     * @param hasAdvantage    外部优势
     * @param hasDisadvantage 外部劣势
     * @return 检定结果
     */
    public static Check_Result perform_check(Character_Sheet character,
                                             String skillName,
                                             boolean hasAdvantage,
                                             boolean hasDisadvantage) {
        int bonus = character.get_skill_bonus(skillName);

        // 优势/劣势互相抵消
        boolean effectiveAdvantage = hasAdvantage && !hasDisadvantage;
        boolean effectiveDisadvantage = hasDisadvantage && !hasAdvantage;

        int d20 = roll_with_advantage_disadvantage(effectiveAdvantage, effectiveDisadvantage);

        boolean criticalSuccess = (d20 == 20);
        boolean criticalFail = (d20 == 1);

        // 自然20/自然1时总结果固定为 20 或 1（D&D 5e 规则）
        int total;
        if (criticalSuccess) {
            total = 20 + bonus;
        } else if (criticalFail) {
            total = 1 + bonus;
        } else {
            total = d20 + bonus;
        }

        String description = build_check_description(skillName, d20, bonus, total,
                effectiveAdvantage, effectiveDisadvantage, criticalSuccess, criticalFail);

        return new Check_Result(skillName, d20, bonus, total,
                effectiveAdvantage, effectiveDisadvantage,
                criticalSuccess, criticalFail, description);
    }

    // ------------------------------------------------------------------ //
    //  属性检定（非技能）
    // ------------------------------------------------------------------ //

    /**
     * 执行属性检定（纯属性调整值，不包含技能熟练加值）。
     *
     * @param character       角色卡
     * @param abilityName     属性英文名（如 "Strength"、"Dexterity"）
     * @param hasAdvantage    外部优势
     * @param hasDisadvantage 外部劣势
     * @return 检定结果
     */
    public static Check_Result perform_ability_check(Character_Sheet character,
                                                     String abilityName,
                                                     boolean hasAdvantage,
                                                     boolean hasDisadvantage) {
        int bonus = character.get_ability_modifier(abilityName);

        boolean effectiveAdvantage = hasAdvantage && !hasDisadvantage;
        boolean effectiveDisadvantage = hasDisadvantage && !hasAdvantage;

        int d20 = roll_with_advantage_disadvantage(effectiveAdvantage, effectiveDisadvantage);

        boolean criticalSuccess = (d20 == 20);
        boolean criticalFail = (d20 == 1);

        int total;
        if (criticalSuccess) {
            total = 20 + bonus;
        } else if (criticalFail) {
            total = 1 + bonus;
        } else {
            total = d20 + bonus;
        }

        String label = get_ability_display_name(abilityName);
        String description = build_check_description(label + "属性检定", d20, bonus, total,
                effectiveAdvantage, effectiveDisadvantage, criticalSuccess, criticalFail);

        return new Check_Result(label + "属性检定", d20, bonus, total,
                effectiveAdvantage, effectiveDisadvantage,
                criticalSuccess, criticalFail, description);
    }

    // ------------------------------------------------------------------ //
    //  豁免检定
    // ------------------------------------------------------------------ //

    /**
     * 执行豁免检定。
     *
     * @param character       角色卡
     * @param abilityName     属性英文名（如 "Constitution"、"Wisdom"）
     * @param hasAdvantage    外部优势
     * @param hasDisadvantage 外部劣势
     * @return 检定结果
     */
    public static Check_Result perform_saving_throw(Character_Sheet character,
                                                    String abilityName,
                                                    boolean hasAdvantage,
                                                    boolean hasDisadvantage) {
        int bonus = character.get_saving_throw_bonus(abilityName);

        boolean effectiveAdvantage = hasAdvantage && !hasDisadvantage;
        boolean effectiveDisadvantage = hasDisadvantage && !hasAdvantage;

        int d20 = roll_with_advantage_disadvantage(effectiveAdvantage, effectiveDisadvantage);

        boolean criticalSuccess = (d20 == 20);
        boolean criticalFail = (d20 == 1);

        int total;
        if (criticalSuccess) {
            total = 20 + bonus;
        } else if (criticalFail) {
            total = 1 + bonus;
        } else {
            total = d20 + bonus;
        }

        String label = get_ability_display_name(abilityName);
        String checkName = label + "豁免";
        String description = build_check_description(checkName, d20, bonus, total,
                effectiveAdvantage, effectiveDisadvantage, criticalSuccess, criticalFail);

        return new Check_Result(checkName, d20, bonus, total,
                effectiveAdvantage, effectiveDisadvantage,
                criticalSuccess, criticalFail, description);
    }

    // ------------------------------------------------------------------ //
    //  格式化输出
    // ------------------------------------------------------------------ //

    /**
     * 将检定结果格式化为可读字符串。
     */
    public static String format_result(Check_Result result) {
        if (result == null) {
            return "无检定结果。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(result.skill_name).append("】");

        if (result.advantage) {
            sb.append(" [优势]");
        }
        if (result.disadvantage) {
            sb.append(" [劣势]");
        }

        sb.append("\n  d20 = ").append(result.d20_roll);

        if (result.critical_success) {
            sb.append(" (自然20!)");
        } else if (result.critical_fail) {
            sb.append(" (自然1!)");
        }

        sb.append("\n  加值 = ").append(format_bonus(result.bonus));
        sb.append("\n  总计 = ").append(result.total);

        if (result.description != null && !result.description.trim().isEmpty()) {
            sb.append("\n  ").append(result.description);
        }

        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  战斗状态 -> 优势/劣势判定
    // ------------------------------------------------------------------ //

    /**
     * 检查战斗状态列表中是否存在提供优势的状态。
     * INSPIRED（激励）状态提供优势。
     *
     * @param effects 战斗状态效果列表
     * @return 是否有优势
     */
    public static boolean has_advantage_from_status(List<Combat_Status_Effect> effects) {
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (Combat_Status_Effect effect : effects) {
            if (effect.type == Combat_Status_Type.INSPIRED) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查战斗状态列表中是否存在提供劣势的状态。
     * POISONED（中毒）、PRONE（倒地）、RESTRAINED（束缚）提供劣势。
     *
     * @param effects 战斗状态效果列表
     * @return 是否有劣势
     */
    public static boolean has_disadvantage_from_status(List<Combat_Status_Effect> effects) {
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (Combat_Status_Effect effect : effects) {
            if (effect.type == Combat_Status_Type.POISONED
                    || effect.type == Combat_Status_Type.PRONE
                    || effect.type == Combat_Status_Type.RESTRAINED) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  内部工具方法
    // ------------------------------------------------------------------ //

    /**
     * 根据优势/劣势掷 d20：有优势取两次较大值，有劣势取两次较小值。
     */
    private static int roll_with_advantage_disadvantage(boolean advantage, boolean disadvantage) {
        if (advantage) {
            int roll1 = Dice_Util.roll_d20();
            int roll2 = Dice_Util.roll_d20();
            return Math.max(roll1, roll2);
        }
        if (disadvantage) {
            int roll1 = Dice_Util.roll_d20();
            int roll2 = Dice_Util.roll_d20();
            return Math.min(roll1, roll2);
        }
        return Dice_Util.roll_d20();
    }

    /**
     * 将数值加值格式化为带正负号的字符串。
     */
    private static String format_bonus(int bonus) {
        if (bonus >= 0) {
            return "+" + bonus;
        }
        return String.valueOf(bonus);
    }

    /**
     * 构建检定描述文本。
     */
    private static String build_check_description(String checkName,
                                                  int d20,
                                                  int bonus,
                                                  int total,
                                                  boolean advantage,
                                                  boolean disadvantage,
                                                  boolean criticalSuccess,
                                                  boolean criticalFail) {
        StringBuilder sb = new StringBuilder();
        sb.append(checkName);

        if (advantage) {
            sb.append("（优势掷骰）");
        } else if (disadvantage) {
            sb.append("（劣势掷骰）");
        }

        sb.append("：d20 = ").append(d20);

        if (criticalSuccess) {
            sb.append("，自然20！");
        } else if (criticalFail) {
            sb.append("，自然1！");
        }

        sb.append("，加值 ").append(format_bonus(bonus));
        sb.append("，总计 ").append(total);

        if (criticalSuccess) {
            sb.append(" —— 大成功！");
        } else if (criticalFail) {
            sb.append(" —— 大失败！");
        }

        return sb.toString();
    }

    /**
     * 将属性英文名映射为中文显示名。
     */
    private static String get_ability_display_name(String abilityName) {
        if (abilityName == null) {
            return "未知";
        }
        switch (abilityName) {
            case "Strength":     return "力量";
            case "Dexterity":    return "敏捷";
            case "Constitution": return "体质";
            case "Intelligence": return "智力";
            case "Wisdom":       return "感知";
            case "Charisma":     return "魅力";
            default:             return abilityName;
        }
    }
}
