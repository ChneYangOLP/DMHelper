package com.DMHelper.basic.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * D&D 5e 施法专注管理器 (Concentration Manager)。
 * 追踪战斗中所有活跃的专注法术，提供专注检定和伤害追踪功能。
 */
public class Concentration_Manager {

    /** 专注信息内部类 */
    public static class ConcentrationInfo {
        /** 专注的法术名 */
        public final String spell_name;
        /** 施法者名 */
        public final String caster_name;
        /** 法术环级 */
        public final int spell_level;
        /** 本次回合受到的伤害累计 */
        public int damage_taken;

        public ConcentrationInfo(String spell_name, String caster_name, int spell_level) {
            this.spell_name = spell_name;
            this.caster_name = caster_name;
            this.spell_level = spell_level;
            this.damage_taken = 0;
        }
    }

    /** 当前所有活跃的专注 */
    private final Map<Combatant, ConcentrationInfo> active_concentrations;

    public Concentration_Manager() {
        this.active_concentrations = new HashMap<>();
    }

    /**
     * 开始专注。如果施法者已有专注，则自动替换（新法术会打破旧专注）。
     *
     * @param caster    施法者
     * @param spellName 法术名
     * @param spellLevel 法术环级
     * @return 是否成功开始专注
     */
    public boolean start_concentration(Combatant caster, String spellName, int spellLevel) {
        if (caster == null || spellName == null || spellName.trim().isEmpty()) {
            return false;
        }
        ConcentrationInfo info = new ConcentrationInfo(
                spellName.trim(),
                caster.display_name,
                Math.max(0, spellLevel)
        );
        active_concentrations.put(caster, info);
        return true;
    }

    /**
     * 结束施法者的专注。
     *
     * @param caster 施法者
     */
    public void end_concentration(Combatant caster) {
        if (caster != null) {
            active_concentrations.remove(caster);
        }
    }

    /**
     * 检查施法者是否有活跃的专注。
     *
     * @param caster 施法者
     * @return 是否有活跃专注
     */
    public boolean has_concentration(Combatant caster) {
        return caster != null && active_concentrations.containsKey(caster);
    }

    /**
     * 获取施法者的专注信息。
     *
     * @param caster 施法者
     * @return 专注信息，如果没有则返回 null
     */
    public ConcentrationInfo get_concentration(Combatant caster) {
        if (caster == null) {
            return null;
        }
        return active_concentrations.get(caster);
    }

    /**
     * 专注检定：DC = 10 或 伤害值的一半（取较大值），进行 Constitution 豁免。
     *
     * @param caster      施法者
     * @param damageTaken 本次受到的伤害
     * @return 专注检定结果描述字符串
     */
    public String check_concentration(Combatant caster, int damageTaken) {
        if (caster == null) {
            return "无效的施法者。";
        }
        ConcentrationInfo info = active_concentrations.get(caster);
        if (info == null) {
            return caster.display_name + " 没有活跃的专注。";
        }

        // DC = 10 或 伤害值的一半（取较大值）
        int dc = Math.max(10, damageTaken / 2);

        // Constitution 豁免掷骰
        int saveRoll = Dice_Util.roll_d20();
        int saveBonus = caster.get_saving_throw_bonus("Constitution");
        int saveTotal = saveRoll + saveBonus;
        boolean success = saveTotal >= dc;

        StringBuilder result = new StringBuilder();
        result.append(caster.display_name).append(" 的专注【").append(info.spell_name).append("】受到冲击！\n");
        result.append("受到伤害：").append(damageTaken).append("，专注检定 DC = ").append(dc).append("\n");
        result.append("Constitution 豁免：d20=").append(saveRoll).append(" + ").append(saveBonus)
                .append(" = ").append(saveTotal);

        if (success) {
            result.append(" ≥ ").append(dc).append("，专注维持成功。\n");
        } else {
            result.append(" < ").append(dc).append("，专注被打断！\n");
            result.append(caster.display_name).append(" 失去了对【").append(info.spell_name).append("】的专注。\n");
            active_concentrations.remove(caster);
        }

        return result.toString().trim();
    }

    /**
     * 检查所有有专注的战斗者的专注状态（用于回合结束时统一检查）。
     * 使用各自追踪到的累计伤害进行检定。
     *
     * @return 所有检定结果的日志列表
     */
    public String check_all_concentrations() {
        List<String> logs = new ArrayList<>();
        List<Combatant> toCheck = new ArrayList<>(active_concentrations.keySet());

        for (Combatant caster : toCheck) {
            ConcentrationInfo info = active_concentrations.get(caster);
            if (info == null) {
                continue;
            }
            if (info.damage_taken > 0) {
                String result = check_concentration(caster, info.damage_taken);
                logs.add(result);
            }
        }

        if (logs.isEmpty()) {
            return "本轮无需专注检定。";
        }
        return String.join("\n---\n", logs);
    }

    /**
     * 追踪战斗者受到的伤害（用于回合结束时统一检定）。
     *
     * @param target 受到伤害的战斗者
     * @param damage 伤害值
     */
    public void track_damage(Combatant target, int damage) {
        if (target == null || damage <= 0) {
            return;
        }
        ConcentrationInfo info = active_concentrations.get(target);
        if (info != null) {
            info.damage_taken += damage;
        }
    }

    /**
     * 清除所有战斗者的回合伤害追踪。
     */
    public void clear_round_damage() {
        for (ConcentrationInfo info : active_concentrations.values()) {
            info.damage_taken = 0;
        }
    }

    /**
     * 获取所有活跃专注的摘要列表。
     *
     * @return 专注摘要字符串列表
     */
    public List<String> get_all_concentration_summaries() {
        List<String> summaries = new ArrayList<>();
        for (Map.Entry<Combatant, ConcentrationInfo> entry : active_concentrations.entrySet()) {
            ConcentrationInfo info = entry.getValue();
            summaries.add(info.caster_name + " → " + info.spell_name + "（" + info.spell_level + " 环）");
        }
        return summaries;
    }
}
