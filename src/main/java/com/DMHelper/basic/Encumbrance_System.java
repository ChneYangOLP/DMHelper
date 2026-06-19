package com.DMHelper.basic;

import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;

/**
 * D&D 5e 负重系统。
 * 根据角色力量属性和所持物品计算携带重量、负重等级及速度惩罚。
 * 由于 Equipment_Item 没有专门的重量字段，本系统使用基于物品类型的估算规则。
 */
public class Encumbrance_System {

    private Encumbrance_System() {
    }

    // ------------------------------------------------------------------ //
    //  负重等级枚举
    // ------------------------------------------------------------------ //

    public enum Encumbrance_Level {
        UNENCUMBERED("无负重", 0, 1.0),
        LIGHTLY_ENCUMBERED("轻度负重", 5, 1.0),
        HEAVILY_ENCUMBERED("重度负重", 10, -20),
        OVERENCUMBERED("超重", 0, -20);

        public final String name;                // 显示名称
        public final int speed_penalty_ft;       // 速度惩罚（尺）
        public final double speed_penalty_percent; // 速度惩罚百分比

        Encumbrance_Level(String name, int speed_penalty_ft, double speed_penalty_percent) {
            this.name = name;
            this.speed_penalty_ft = speed_penalty_ft;
            this.speed_penalty_percent = speed_penalty_percent;
        }
    }

    // ------------------------------------------------------------------ //
    //  负重结果
    // ------------------------------------------------------------------ //

    public static class Encumbrance_Result {
        public final int total_weight_lb;            // 总重量（磅）
        public final int carry_capacity_lb;          // 携带能力（磅）
        public final int push_drag_lift_lb;          // 推/拉/举能力
        public final Encumbrance_Level level;        // 当前负重等级
        public final int speed_penalty_ft;           // 速度惩罚（尺）
        public final double speed_penalty_percent;   // 速度惩罚百分比
        public final String description;             // 描述

        public Encumbrance_Result(int total_weight_lb,
                                  int carry_capacity_lb,
                                  int push_drag_lift_lb,
                                  Encumbrance_Level level,
                                  int speed_penalty_ft,
                                  double speed_penalty_percent,
                                  String description) {
            this.total_weight_lb = total_weight_lb;
            this.carry_capacity_lb = carry_capacity_lb;
            this.push_drag_lift_lb = push_drag_lift_lb;
            this.level = level;
            this.speed_penalty_ft = speed_penalty_ft;
            this.speed_penalty_percent = speed_penalty_percent;
            this.description = description;
        }
    }

    // ------------------------------------------------------------------ //
    //  核心：计算负重
    // ------------------------------------------------------------------ //

    /**
     * 计算角色当前负重。
     * <p>
     * 携带能力 = 力量 x 15
     * 推/拉/举 = 力量 x 30
     * <p>
     * 物品重量估算规则（Equipment_Item 无专用重量字段）：
     * <ul>
     *   <li>护甲类：轻甲 10 磅，中甲 20 磅，重甲 55 磅，无甲 3 磅</li>
     *   <li>主手武器：简易近战 2 磅，军用近战 4 磅，远程武器 3 磅</li>
     *   <li>盾牌（shield_bonus > 0 的副手物品）：6 磅</li>
     *   <li>其他副手物品（法术书、圣徽等）：3 磅</li>
     *   <li>披风：1 磅</li>
     *   <li>护符：0 磅</li>
     *   <li>背包物品：value_in_cp / 50（最低 0 磅），特殊物品使用固定重量</li>
     * </ul>
     *
     * @param character 角色卡
     * @return 负重结果
     */
    public static Encumbrance_Result calculate(Character_Sheet character) {
        if (character == null) {
            return new Encumbrance_Result(0, 0, 0,
                    Encumbrance_Level.UNENCUMBERED, 0, 1.0, "角色为空。");
        }

        int strength = character.stats.str;
        int carryCapacity = strength * 15;
        int pushDragLift = strength * 30;

        // 计算总重量
        int totalWeight = 0;

        // 遍历所有已拥有物品
        if (character.owned_equipment_keys != null) {
            for (String itemKey : character.owned_equipment_keys) {
                Equipment_Item item = Equipment_Library.get_item(itemKey);
                if (item == null) {
                    continue;
                }
                int itemWeight = estimate_item_weight(item);
                int count = character.get_item_count(itemKey);
                totalWeight += itemWeight * count;
            }
        }

        // 判定负重等级
        Encumbrance_Level level = determine_encumbrance_level(totalWeight, carryCapacity);

        // 构建描述
        String description = build_description(character.name, totalWeight, carryCapacity,
                pushDragLift, level);

        return new Encumbrance_Result(totalWeight, carryCapacity, pushDragLift,
                level, level.speed_penalty_ft, level.speed_penalty_percent, description);
    }

    // ------------------------------------------------------------------ //
    //  有效速度
    // ------------------------------------------------------------------ //

    /**
     * 获取考虑负重后的有效速度。
     * <p>
     * D&D 5e 基础移动速度为 30 尺（标准种族）。
     * 轻度负重：无惩罚。
     * 重度负重：速度减少 10 尺。
     * 超重：速度减少 20 尺。
     *
     * @param character 角色卡
     * @return 有效速度（尺）
     */
    public static int get_effective_speed(Character_Sheet character) {
        if (character == null) {
            return 0;
        }

        // D&D 5e 标准基础速度为 30 尺
        int baseSpeed = 30;

        // 如果角色种族有特殊速度，此处可扩展
        // 目前使用标准 30 尺

        Encumbrance_Result result = calculate(character);

        int effectiveSpeed = baseSpeed;

        if (result.level == Encumbrance_Level.HEAVILY_ENCUMBERED) {
            effectiveSpeed -= 10;
        } else if (result.level == Encumbrance_Level.OVERENCUMBERED) {
            effectiveSpeed -= 20;
        }

        return Math.max(5, effectiveSpeed); // 最低保留 5 尺移动能力
    }

    // ------------------------------------------------------------------ //
    //  负重摘要
    // ------------------------------------------------------------------ //

    /**
     * 获取负重摘要文本。
     *
     * @param character 角色卡
     * @return 摘要字符串
     */
    public static String get_encumbrance_summary(Character_Sheet character) {
        if (character == null) {
            return "角色为空，无法计算负重。";
        }

        Encumbrance_Result result = calculate(character);

        StringBuilder sb = new StringBuilder();
        sb.append("【负重信息】\n");
        sb.append("  角色名：").append(character.name).append("\n");
        sb.append("  力量值：").append(character.stats.str).append("\n");
        sb.append("  携带能力：").append(result.carry_capacity_lb).append(" 磅\n");
        sb.append("  推/拉/举：").append(result.push_drag_lift_lb).append(" 磅\n");
        sb.append("  当前负重：").append(result.total_weight_lb).append(" 磅\n");
        sb.append("  负重等级：").append(result.level.name).append("\n");

        int effectiveSpeed = get_effective_speed(character);
        sb.append("  有效速度：").append(effectiveSpeed).append(" 尺");

        if (result.level != Encumbrance_Level.UNENCUMBERED) {
            sb.append("（基础 30 尺，惩罚 ").append(30 - effectiveSpeed).append(" 尺）");
        }

        if (result.level == Encumbrance_Level.OVERENCUMBERED) {
            sb.append("\n  警告：已超重！速度大幅降低，且无法进行冲刺、跳跃或攀爬！");
        } else if (result.level == Encumbrance_Level.HEAVILY_ENCUMBERED) {
            sb.append("\n  注意：重度负重，速度降低。");
        }

        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  内部工具方法
    // ------------------------------------------------------------------ //

    /**
     * 估算单个物品的重量（磅）。
     */
    private static int estimate_item_weight(Equipment_Item item) {
        if (item == null) {
            return 0;
        }

        switch (item.slot) {
            case ARMOR:
                return estimate_armor_weight(item);

            case MAIN_HAND:
                return estimate_weapon_weight(item);

            case OFF_HAND:
                // 盾牌 6 磅，其他副手物品（法术书、圣徽等）3 磅
                if (item.shield_bonus > 0) {
                    return 6;
                }
                return 3;

            case CLOAK:
                return 1;

            case ACCESSORY:
                return 0;

            case BACKPACK:
                return estimate_backpack_item_weight(item);

            default:
                return 0;
        }
    }

    /**
     * 估算护甲重量。
     * 轻甲 10 磅，中甲 20 磅，重甲 55 磅，无甲 3 磅。
     */
    private static int estimate_armor_weight(Equipment_Item item) {
        if (item.armor_type == null) {
            return 3;
        }
        switch (item.armor_type) {
            case "Light":
                return 10;
            case "Medium":
                return 20;
            case "Heavy":
                return 55;
            default:
                // "None" 或其他 -> 普通衣物
                return 3;
        }
    }

    /**
     * 估算武器重量。
     * 简易近战武器 2 磅，军用近战武器 4 磅，远程武器 3 磅。
     * 特殊武器（如捕网）3 磅。
     */
    private static int estimate_weapon_weight(Equipment_Item item) {
        if (item.attack_dice_count == 0 && item.attack_die_size == 0) {
            // 特殊武器（如 net）
            return 3;
        }

        if (item.ranged) {
            // 远程武器
            return 3;
        }

        // 判断简易 vs 军用：通过描述中是否包含"简易"来区分
        String desc = item.description == null ? "" : item.description;
        if (desc.contains("简易")) {
            return 2;
        }

        // 军用近战武器
        return 4;
    }

    /**
     * 估算背包物品重量。
     * 使用 value_in_cp / 50 作为基础估算，同时对已知物品使用固定重量。
     */
    private static int estimate_backpack_item_weight(Equipment_Item item) {
        if (item == null) {
            return 0;
        }

        // 已知物品的固定重量映射
        int fixedWeight = get_known_backpack_item_weight(item.key);
        if (fixedWeight >= 0) {
            return fixedWeight;
        }

        // 通用估算：value_in_cp / 50，最低 0 磅
        if (item.value_in_cp <= 0) {
            return 0;
        }
        return Math.max(0, item.value_in_cp / 50);
    }

    /**
     * 获取已知背包物品的固定重量。
     * 返回 -1 表示使用通用估算。
     */
    private static int get_known_backpack_item_weight(String key) {
        if (key == null) {
            return -1;
        }
        switch (key) {
            // 消耗品
            case "healing_potion":
            case "greater_healing_potion":
            case "superior_healing_potion":
            case "supreme_healing_potion":
            case "potion_of_climbing":
            case "potion_of_fire_breath":
            case "potion_of_invisibility":
            case "antitoxin":
            case "holy_water":
                return 1; // 药水/小瓶装 1 磅

            case "fire_bomb":
            case "thunder_bomb":
                return 1; // 炸弹 1 磅

            // 工具与冒险装备
            case "torch":
                return 1;
            case "oil_flask":
                return 1;
            case "rations":
                return 2;
            case "waterskin":
                return 5; // 装满水时
            case "hempen_rope":
                return 10; // 50尺麻绳
            case "crowbar":
                return 5;
            case "grappling_hook":
                return 4;
            case "healers_kit":
                return 3;
            case "thieves_tools":
                return 1;
            case "iron_key":
                return 0;

            // 战利品/材料
            case "goblin_ear":
            case "kobold_talisman":
            case "bandit_pouch":
            case "holy_relic_fragment":
            case "undead_bone_charm":
            case "monster_fang":
            case "fiend_ash":
            case "strange_eye":
            case "knight_token":
            case "beast_claw":
                return 0; // 小型战利品忽略不计

            case "wolf_pelt":
                return 5;
            case "venom_sac":
                return 0;
            case "dragon_scale":
                return 2;
            case "giant_bone_shard":
                return 10;
            case "ancient_ruin_map":
                return 0;

            // 钱币包
            case "gold_pouch":
                return 1;
            case "silver_pouch":
                return 1;
            case "copper_pouch":
                return 1;

            // 卷轴
            default:
                if (key.startsWith("scroll_of_")) {
                    return 0; // 卷轴重量忽略不计
                }
                return -1; // 使用通用估算
        }
    }

    /**
     * 判定负重等级。
     */
    private static Encumbrance_Level determine_encumbrance_level(int totalWeight, int carryCapacity) {
        if (carryCapacity <= 0) {
            return Encumbrance_Level.OVERENCUMBERED;
        }

        double ratio = (double) totalWeight / carryCapacity;

        if (ratio > 1.0) {
            return Encumbrance_Level.OVERENCUMBERED;
        } else if (ratio > 2.0 / 3.0) {
            // 超过携带能力的 2/3
            return Encumbrance_Level.HEAVILY_ENCUMBERED;
        } else if (ratio > 1.0 / 3.0) {
            // 超过携带能力的 1/3
            return Encumbrance_Level.LIGHTLY_ENCUMBERED;
        } else {
            return Encumbrance_Level.UNENCUMBERED;
        }
    }

    /**
     * 构建负重描述文本。
     */
    private static String build_description(String characterName,
                                             int totalWeight,
                                             int carryCapacity,
                                             int pushDragLift,
                                             Encumbrance_Level level) {
        StringBuilder sb = new StringBuilder();
        sb.append(characterName).append(" 当前负重 ");
        sb.append(totalWeight).append("/").append(carryCapacity).append(" 磅");

        if (totalWeight > carryCapacity) {
            sb.append("（超重 ").append(totalWeight - carryCapacity).append(" 磅）");
        }

        sb.append("，推/拉/举能力 ").append(pushDragLift).append(" 磅。");
        sb.append(" 负重等级：").append(level.name).append("。");

        if (level == Encumbrance_Level.OVERENCUMBERED) {
            sb.append(" 超重状态下速度大幅降低，且无法冲刺！");
        } else if (level == Encumbrance_Level.HEAVILY_ENCUMBERED) {
            sb.append(" 重度负重状态下速度降低 10 尺。");
        }

        return sb.toString();
    }
}
