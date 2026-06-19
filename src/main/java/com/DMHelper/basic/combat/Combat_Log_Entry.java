package com.DMHelper.basic.combat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 战斗日志条目记录类。
 * 用于记录战斗过程中发生的各类事件，支持简短格式、详细格式和 PDF 导出格式的输出。
 */
public class Combat_Log_Entry {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------------ //
    //  日志类型枚举
    // ------------------------------------------------------------------ //

    public enum Log_Type {
        COMBAT_START("战斗开始"),
        ROUND_START("回合开始"),
        INITIATIVE_ROLL("先攻掷骰"),
        ATTACK_ROLL("攻击掷骰"),
        DAMAGE_DEALT("造成伤害"),
        HEALING_DONE("治疗"),
        SAVE_THROW("豁免检定"),
        STATUS_APPLIED("状态附加"),
        STATUS_EXPIRED("状态消失"),
        STATUS_DAMAGE("状态伤害"),
        CONCENTRATION_CHECK("专注检定"),
        CONCENTRATION_BROKEN("专注打破"),
        OPPORTUNITY_ATTACK("借机攻击"),
        TURN_START("回合开始效果"),
        TURN_END("回合结束"),
        TURN_SKIPPED("跳过回合"),
        DEATH("死亡"),
        COMBAT_END("战斗结束"),
        LOOT_GENERATED("战利品生成"),
        XP_DISTRIBUTED("经验分配"),
        SHORT_REST("短休"),
        LONG_REST("长休"),
        EXTERNAL_EFFECT("外部效果"),
        ITEM_USED("使用物品"),
        ADVANTAGE_ROLL("优势掷骰"),
        DISADVANTAGE_ROLL("劣势掷骰"),
        COVER_APPLIED("掩护生效"),
        SYSTEM("系统消息");

        public final String label;

        Log_Type(String label) {
            this.label = label;
        }
    }

    // ------------------------------------------------------------------ //
    //  字段
    // ------------------------------------------------------------------ //

    public final Log_Type type;              // 日志类型
    public final int round_number;           // 回合数
    public final String actor_name;          // 行动者名称
    public final String target_name;         // 目标名称
    public final String description;         // 描述文本
    public final int numeric_value;          // 伤害值、治疗值、掷骰结果等
    public final String detail;              // 额外详细信息
    public final LocalDateTime timestamp;    // 时间戳

    // ------------------------------------------------------------------ //
    //  构造器
    // ------------------------------------------------------------------ //

    /**
     * 完整构造器，接受所有字段。
     */
    public Combat_Log_Entry(Log_Type type,
                            int round_number,
                            String actor_name,
                            String target_name,
                            String description,
                            int numeric_value,
                            String detail,
                            LocalDateTime timestamp) {
        this.type = type;
        this.round_number = round_number;
        this.actor_name = (actor_name != null) ? actor_name : "";
        this.target_name = (target_name != null) ? target_name : "";
        this.description = (description != null) ? description : "";
        this.numeric_value = numeric_value;
        this.detail = (detail != null) ? detail : "";
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
    }

    /**
     * 简化构造器。其他字段使用默认值：
     * target_name = "", numeric_value = 0, detail = "", timestamp = 当前时间。
     */
    public Combat_Log_Entry(Log_Type type,
                            int roundNumber,
                            String actorName,
                            String description) {
        this(type, roundNumber, actorName, "", description, 0, "", LocalDateTime.now());
    }

    // ------------------------------------------------------------------ //
    //  格式化输出
    // ------------------------------------------------------------------ //

    /**
     * 简短格式："[回合N] [类型] 描述"
     *
     * @return 简短格式字符串
     */
    public String format_short() {
        StringBuilder sb = new StringBuilder();
        sb.append("[回合").append(round_number).append("] ");
        sb.append("[").append(type.label).append("] ");
        sb.append(description);
        return sb.toString();
    }

    /**
     * 详细格式：包含时间戳、回合、类型、行动者、目标、描述、数值、详情。
     *
     * @return 详细格式字符串
     */
    public String format_detailed() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp.format(TIMESTAMP_FORMATTER)).append("] ");
        sb.append("[回合").append(round_number).append("] ");
        sb.append("[").append(type.label).append("]");

        if (actor_name != null && !actor_name.isEmpty()) {
            sb.append(" 行动者: ").append(actor_name);
        }

        if (target_name != null && !target_name.isEmpty()) {
            sb.append(" -> 目标: ").append(target_name);
        }

        sb.append("\n  描述: ").append(description);

        if (numeric_value != 0) {
            sb.append("\n  数值: ").append(numeric_value);
        }

        if (detail != null && !detail.isEmpty()) {
            sb.append("\n  详情: ").append(detail);
        }

        return sb.toString();
    }

    /**
     * PDF 导出格式：使用制表符分隔的紧凑格式，适合 PDF 表格或纯文本导出。
     * 格式：时间戳 | 回合 | 类型 | 行动者 | 目标 | 描述 | 数值 | 详情
     *
     * @return PDF 导出格式字符串
     */
    public String format_for_pdf() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp.format(TIMESTAMP_FORMATTER));
        sb.append(" | ");
        sb.append(round_number);
        sb.append(" | ");
        sb.append(type.label);
        sb.append(" | ");
        sb.append(actor_name != null && !actor_name.isEmpty() ? actor_name : "-");
        sb.append(" | ");
        sb.append(target_name != null && !target_name.isEmpty() ? target_name : "-");
        sb.append(" | ");
        sb.append(description);
        sb.append(" | ");
        sb.append(numeric_value != 0 ? numeric_value : "-");
        sb.append(" | ");
        sb.append(detail != null && !detail.isEmpty() ? detail : "-");
        return sb.toString();
    }
}
