package com.DMHelper.basic.combat;

import java.util.List;

/**
 * D&D 5e 优势/劣势系统工具类 (Advantage / Disadvantage)。
 * 提供优势掷骰、劣势掷骰、优势劣势抵消判定，
 * 以及从状态效果和掩护中推断优势/劣势的辅助方法。
 */
public class Advantage_Disadvantage {
    private Advantage_Disadvantage() {
    }

    /**
     * 掷两颗 d20 取较大值（优势掷骰）。
     *
     * @return 格式 "d20a=X,Y→Z" 的字符串，X 和 Y 是两颗骰子，Z 是取较大值
     */
    public static String roll_with_advantage() {
        int die1 = Dice_Util.roll_d20();
        int die2 = Dice_Util.roll_d20();
        int result = Math.max(die1, die2);
        return "d20a=" + die1 + "," + die2 + "→" + result;
    }

    /**
     * 掷两颗 d20 取较小值（劣势掷骰）。
     *
     * @return 格式 "d20d=X,Y→Z" 的字符串，X 和 Y 是两颗骰子，Z 是取较小值
     */
    public static String roll_with_disadvantage() {
        int die1 = Dice_Util.roll_d20();
        int die2 = Dice_Util.roll_d20();
        int result = Math.min(die1, die2);
        return "d20d=" + die1 + "," + die2 + "→" + result;
    }

    /**
     * 解算 d20 掷骰：优势劣势互相抵消，都为 true 或都为 false 时普通掷骰。
     *
     * @param hasAdvantage  是否具有优势
     * @param hasDisadvantage 是否具有劣势
     * @return 掷骰结果数值
     */
    public static int resolve_d20(boolean hasAdvantage, boolean hasDisadvantage) {
        if (hasAdvantage && !hasDisadvantage) {
            int die1 = Dice_Util.roll_d20();
            int die2 = Dice_Util.roll_d20();
            return Math.max(die1, die2);
        }
        if (hasDisadvantage && !hasAdvantage) {
            int die1 = Dice_Util.roll_d20();
            int die2 = Dice_Util.roll_d20();
            return Math.min(die1, die2);
        }
        // 优势劣势互相抵消，或都没有 → 普通掷骰
        return Dice_Util.roll_d20();
    }

    /**
     * 解算 d20 掷骰并返回带标签的掷骰结果字符串。
     *
     * @param hasAdvantage  是否具有优势
     * @param hasDisadvantage 是否具有劣势
     * @return 带标签的掷骰结果字符串，例如 "d20a=15,8→15"、"d20d=3,12→3"、"d20=7"
     */
    public static String resolve_d20_label(boolean hasAdvantage, boolean hasDisadvantage) {
        if (hasAdvantage && !hasDisadvantage) {
            return roll_with_advantage();
        }
        if (hasDisadvantage && !hasAdvantage) {
            return roll_with_disadvantage();
        }
        return "d20=" + Dice_Util.roll_d20();
    }

    /**
     * 检查状态效果列表是否提供优势。
     * INSPIRED（激励）状态提供优势。
     *
     * @param effects 状态效果列表
     * @return 是否具有优势
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
     * 检查状态效果列表是否提供劣势。
     * 以下状态提供攻击劣势：
     * - FRIGHTENED（恐慌）：对恐惧来源的攻击有劣势
     * - POISONED（中毒）：攻击掷骰有劣势
     * - PRONE（倒地）：近战攻击有劣势
     * - RESTRAINED（束缚）：攻击掷骰有劣势
     * - INVISIBLE（隐形）：被攻击者对隐形目标攻击有劣势
     *
     * @param effects 状态效果列表
     * @return 是否具有劣势
     */
    public static boolean has_disadvantage_from_status(List<Combat_Status_Effect> effects) {
        if (effects == null || effects.isEmpty()) {
            return false;
        }
        for (Combat_Status_Effect effect : effects) {
            if (effect.type == Combat_Status_Type.FRIGHTENED
                    || effect.type == Combat_Status_Type.POISONED
                    || effect.type == Combat_Status_Type.PRONE
                    || effect.type == Combat_Status_Type.RESTRAINED
                    || effect.type == Combat_Status_Type.INVISIBLE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查攻击者是否因掩护或隐形获得优势。
     * 掩护本身不提供攻击优势，但隐形对目标有攻击优势。
     *
     * @param attacker 攻击者
     * @param target   被攻击目标
     * @param cover    目标拥有的掩护类型
     * @return 是否具有优势
     */
    public static boolean has_advantage_from_cover(Combatant attacker, Combatant target, Cover_Type cover) {
        if (attacker == null) {
            return false;
        }
        // 隐形攻击者对目标有优势
        for (Combat_Status_Effect effect : attacker.status_effects) {
            if (effect.type == Combat_Status_Type.INVISIBLE) {
                return true;
            }
        }
        // 掩护不提供攻击优势
        return false;
    }

    /**
     * 检查掩护是否直接提供攻击劣势。
     * 掩护本身不直接提供攻击劣势（掩护效果通过 AC 加值体现）。
     *
     * @param cover 掩护类型
     * @return 始终返回 false
     */
    public static boolean has_disadvantage_from_cover(Cover_Type cover) {
        // 掩护不直接提供攻击劣势，效果通过 AC 加值体现
        return false;
    }
}
