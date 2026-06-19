package com.DMHelper.basic.combat;

import java.util.ArrayList;
import java.util.List;

/**
 * D&D 5e 借机攻击系统 (Opportunity Attack)。
 * 当战斗者移出敌方近战触及范围时，敌方可以触发借机攻击。
 */
public class Opportunity_Attack {

    /** 借机攻击结果 */
    public static class Opportunity_Result {
        /** 攻击者 */
        public final Combatant attacker;
        /** 被攻击目标 */
        public final Combatant target;
        /** 攻击掷骰总值 */
        public final int attack_roll;
        /** 造成伤害 */
        public final int damage;
        /** 是否命中 */
        public final boolean hit;
        /** 结果描述 */
        public final String description;

        public Opportunity_Result(Combatant attacker, Combatant target, int attackRoll,
                                  int damage, boolean hit, String description) {
            this.attacker = attacker;
            this.target = target;
            this.attack_roll = attackRoll;
            this.damage = damage;
            this.hit = hit;
            this.description = description;
        }
    }

    private Opportunity_Attack() {
    }

    /**
     * 检查移动是否触发借机攻击。
     * 规则：当战斗者移出敌方近战触及范围（默认 5 尺）时，
     * 每个相邻的敌方可以触发一次借机攻击。
     * 以下情况不会触发借机攻击：
     * - 麻痹/沉睡状态的战斗者不能触发借机攻击
     * - 隐形战斗者不会触发借机攻击（敌人无法感知）
     *
     * @param movingCombatant 正在移动的战斗者
     * @param allCombatants   所有战斗者列表
     * @return 所有触发的借机攻击结果列表
     */
    public static List<Opportunity_Result> check_opportunity_attacks(Combatant movingCombatant,
                                                                      List<Combatant> allCombatants) {
        List<Opportunity_Result> results = new ArrayList<>();
        if (movingCombatant == null || allCombatants == null || allCombatants.isEmpty()) {
            return results;
        }

        // 移动者不会触发借机攻击的情况：隐形
        if (!can_provoke_opportunity(movingCombatant)) {
            return results;
        }

        for (Combatant combatant : allCombatants) {
            // 跳过自身和已倒下的战斗者
            if (combatant == movingCombatant || !combatant.is_alive()) {
                continue;
            }
            // 只有敌方会触发借机攻击
            if (combatant.side == movingCombatant.side) {
                continue;
            }
            // 麻痹/沉睡状态的战斗者不能触发借机攻击
            if (combatant.is_turn_blocked()) {
                continue;
            }

            // 假设所有存活的敌方都在近战触及范围内（默认 5 尺）
            // 实际距离追踪需要更复杂的网格/坐标系统
            Opportunity_Result result = resolve_opportunity_attack(combatant, movingCombatant);
            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    /**
     * 解算单次借机攻击。
     * 使用攻击者的主手武器攻击选项（如果有）；
     * 如果没有武器攻击选项，使用徒手攻击（1d4 + 力量调整值）。
     * 普通攻击掷骰 vs 目标 AC。
     *
     * @param attacker 攻击者
     * @param target   被攻击目标
     * @return 借机攻击结果，如果无法攻击则返回 null
     */
    public static Opportunity_Result resolve_opportunity_attack(Combatant attacker, Combatant target) {
        if (attacker == null || target == null || !attacker.is_alive() || !target.is_alive()) {
            return null;
        }

        // 查找主手武器攻击选项
        Attack_Option weaponAttack = find_primary_weapon_attack(attacker);

        int d20 = Dice_Util.roll_d20();
        int attackBonus;
        int damage;
        String attackName;
        String damageType;

        if (weaponAttack != null) {
            attackBonus = weaponAttack.attack_bonus + attacker.get_effective_attack_modifier();
            damage = Math.max(0, weaponAttack.roll_damage(false));
            attackName = weaponAttack.name;
            damageType = weaponAttack.damage_type;
        } else {
            // 徒手攻击：1d4 + 力量调整值
            int strMod = Combatant.get_modifier(attacker.strength);
            attackBonus = strMod + attacker.get_effective_attack_modifier();
            damage = Math.max(0, Dice_Util.roll_dice(1, 4) + strMod);
            attackName = "徒手打击 (Unarmed Strike)";
            damageType = "钝击";
        }

        int totalAttack = d20 + attackBonus;
        boolean critical = d20 == 20;
        int targetAc = target.get_effective_armor_class();
        boolean hit = critical || totalAttack >= targetAc;

        // 重击时伤害翻骰
        if (critical && weaponAttack != null) {
            damage = Math.max(0, weaponAttack.roll_damage(true));
        } else if (critical) {
            int strMod = Combatant.get_modifier(attacker.strength);
            damage = Math.max(0, Dice_Util.roll_dice(2, 4) + strMod);
        }

        // 命中则扣除目标生命值
        if (hit) {
            target.current_hp = Math.max(0, target.current_hp - damage);
        }

        StringBuilder desc = new StringBuilder();
        desc.append("[借机攻击] ").append(attacker.display_name)
                .append(" 对 ").append(target.display_name)
                .append(" 使用 ").append(attackName).append("：\n");
        desc.append("d20=").append(d20).append(" + ").append(attackBonus)
                .append(" = ").append(totalAttack);
        if (critical) {
            desc.append("（重击！）");
        }
        desc.append(hit ? "，命中" : "，未命中");
        desc.append("（目标 AC ").append(targetAc).append("）\n");
        if (hit) {
            desc.append("造成 ").append(damage).append(" 点").append(damageType)
                    .append("伤害，目标剩余 HP ").append(target.current_hp)
                    .append("/").append(target.max_hp);
            if (!target.is_alive()) {
                desc.append("\n").append(target.display_name).append(" 倒下了。");
            }
        }

        return new Opportunity_Result(attacker, target, totalAttack, damage, hit, desc.toString().trim());
    }

    /**
     * 检查战斗者是否会触发借机攻击。
     * 非麻痹、非沉睡、非隐形的战斗者会触发借机攻击。
     *
     * @param combatant 战斗者
     * @return 是否会触发借机攻击
     */
    public static boolean can_provoke_opportunity(Combatant combatant) {
        if (combatant == null) {
            return false;
        }
        // 麻痹或沉睡状态的战斗者不会触发借机攻击（无法移动）
        for (Combat_Status_Effect effect : combatant.status_effects) {
            if (effect.type == Combat_Status_Type.PARALYZED
                    || effect.type == Combat_Status_Type.ASLEEP) {
                return false;
            }
        }
        // 隐形战斗者不会触发借机攻击（敌人无法感知移动）
        for (Combat_Status_Effect effect : combatant.status_effects) {
            if (effect.type == Combat_Status_Type.INVISIBLE) {
                return false;
            }
        }
        return true;
    }

    /**
     * 格式化借机攻击日志。
     *
     * @param results 借机攻击结果列表
     * @return 格式化后的日志字符串
     */
    public static String format_opportunity_log(List<Opportunity_Result> results) {
        if (results == null || results.isEmpty()) {
            return "没有触发借机攻击。";
        }
        StringBuilder log = new StringBuilder();
        log.append("=== 借机攻击 (Opportunity Attacks) ===\n");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                log.append("\n---\n");
            }
            log.append(results.get(i).description);
        }
        return log.toString();
    }

    /**
     * 查找攻击者的主手武器攻击选项。
     * 优先查找非徒手的攻击掷骰类型攻击选项。
     *
     * @param attacker 攻击者
     * @return 主手武器攻击选项，如果没有则返回 null
     */
    private static Attack_Option find_primary_weapon_attack(Combatant attacker) {
        if (attacker.attack_options == null || attacker.attack_options.isEmpty()) {
            return null;
        }
        // 优先查找第一个攻击掷骰类型的攻击选项（通常是主手武器）
        for (Attack_Option option : attacker.attack_options) {
            if (option.resolution_type == Attack_Option.Resolution_Type.ATTACK_ROLL
                    && option.damage_dice_count > 0) {
                return option;
            }
        }
        return null;
    }
}
