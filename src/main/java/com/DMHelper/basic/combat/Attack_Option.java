package com.DMHelper.basic.combat;

public class Attack_Option {
    public enum Resolution_Type {
        ATTACK_ROLL,
        SAVE_DC,
        AUTO_HIT
    }

    public enum Target_Mode {
        HOSTILE,
        FRIENDLY,
        FRIENDLY_OTHER,
        SELF
    }

    public final String name;
    public final String description;
    public final Resolution_Type resolution_type;
    public final int attack_bonus;
    public final int save_dc;
    public final String save_ability;
    public final int damage_dice_count;
    public final int damage_dice_size;
    public final int damage_bonus;
    public final int attack_count;
    public final boolean half_damage_on_save;
    public final String damage_type;
    public Target_Mode target_mode;
    public int healing_dice_count;
    public int healing_dice_size;
    public int healing_bonus;
    public int spell_slot_cost_level;
    public int pact_slot_cost;
    public int sorcery_point_cost;
    public int bardic_inspiration_cost;
    public int superiority_die_cost;
    public int lay_on_hands_cost;
    public Combat_Status_Type applied_status_type;
    public int status_duration_rounds;
    public String status_save_ability;
    public int status_save_dc;

    private Attack_Option(String name,
                          String description,
                          Resolution_Type resolution_type,
                          int attack_bonus,
                          int save_dc,
                          String save_ability,
                          int damage_dice_count,
                          int damage_dice_size,
                          int damage_bonus,
                          int attack_count,
                          boolean half_damage_on_save,
                          String damage_type) {
        this.name = name;
        this.description = description;
        this.resolution_type = resolution_type;
        this.attack_bonus = attack_bonus;
        this.save_dc = save_dc;
        this.save_ability = save_ability;
        this.damage_dice_count = damage_dice_count;
        this.damage_dice_size = damage_dice_size;
        this.damage_bonus = damage_bonus;
        this.attack_count = Math.max(1, attack_count);
        this.half_damage_on_save = half_damage_on_save;
        this.damage_type = damage_type;
        this.target_mode = Target_Mode.HOSTILE;
    }

    public static Attack_Option attack_roll(String name,
                                            String description,
                                            int attack_bonus,
                                            int damage_dice_count,
                                            int damage_dice_size,
                                            int damage_bonus,
                                            int attack_count,
                                            String damage_type) {
        return new Attack_Option(
                name,
                description,
                Resolution_Type.ATTACK_ROLL,
                attack_bonus,
                0,
                "",
                damage_dice_count,
                damage_dice_size,
                damage_bonus,
                attack_count,
                false,
                damage_type
        );
    }

    public static Attack_Option save_dc(String name,
                                        String description,
                                        int save_dc,
                                        String save_ability,
                                        int damage_dice_count,
                                        int damage_dice_size,
                                        int damage_bonus,
                                        boolean half_damage_on_save,
                                        String damage_type) {
        return new Attack_Option(
                name,
                description,
                Resolution_Type.SAVE_DC,
                0,
                save_dc,
                save_ability,
                damage_dice_count,
                damage_dice_size,
                damage_bonus,
                1,
                half_damage_on_save,
                damage_type
        );
    }

    public static Attack_Option auto_hit(String name,
                                         String description,
                                         int damage_dice_count,
                                         int damage_dice_size,
                                         int damage_bonus,
                                         String damage_type) {
        return new Attack_Option(
                name,
                description,
                Resolution_Type.AUTO_HIT,
                0,
                0,
                "",
                damage_dice_count,
                damage_dice_size,
                damage_bonus,
                1,
                false,
                damage_type
        );
    }

    public int roll_damage(boolean critical) {
        int diceCount = critical ? this.damage_dice_count * 2 : this.damage_dice_count;
        return Dice_Util.roll_dice(diceCount, this.damage_dice_size) + this.damage_bonus;
    }

    public int roll_healing() {
        return Dice_Util.roll_dice(this.healing_dice_count, this.healing_dice_size) + this.healing_bonus;
    }

    public String get_damage_label() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.damage_dice_count).append("d").append(this.damage_dice_size);
        if (this.damage_bonus > 0) {
            sb.append("+").append(this.damage_bonus);
        } else if (this.damage_bonus < 0) {
            sb.append(this.damage_bonus);
        }
        return sb.toString();
    }

    public String to_display_label() {
        String resourceText = get_resource_cost_label();
        String effectLabel = get_primary_effect_label();
        if (this.resolution_type == Resolution_Type.ATTACK_ROLL) {
            return this.name + " | 命中 +" + this.attack_bonus + " | " + effectLabel + format_attack_count() + resourceText;
        }
        if (this.resolution_type == Resolution_Type.SAVE_DC) {
            return this.name + " | " + this.save_ability + " 豁免 DC " + this.save_dc + " | " + effectLabel + resourceText;
        }
        return this.name + " | 自动命中 | " + effectLabel + resourceText;
    }

    private String get_primary_effect_label() {
        if (this.healing_dice_count > 0) {
            return "治疗 " + get_healing_label();
        }
        if (this.damage_dice_count > 0 && this.damage_dice_size > 0) {
            return "伤害 " + get_damage_label() + " " + this.damage_type;
        }
        return this.applied_status_type == null ? "效果" : "附加状态 " + this.applied_status_type.label;
    }

    private String format_attack_count() {
        return this.attack_count > 1 ? " | 次数 x" + this.attack_count : "";
    }

    private String get_resource_cost_label() {
        if (this.spell_slot_cost_level > 0) {
            return " | 消耗 " + this.spell_slot_cost_level + " 环法术位";
        }
        if (this.pact_slot_cost > 0) {
            return " | 消耗 1 个契约法术位";
        }
        if (this.sorcery_point_cost > 0) {
            return " | 消耗 " + this.sorcery_point_cost + " 点术法点";
        }
        if (this.bardic_inspiration_cost > 0) {
            return " | 消耗 " + this.bardic_inspiration_cost + " 次吟游激励";
        }
        if (this.superiority_die_cost > 0) {
            return " | 消耗 " + this.superiority_die_cost + " 颗卓越骰";
        }
        if (this.lay_on_hands_cost > 0) {
            return " | 消耗 " + this.lay_on_hands_cost + " 点圣疗池";
        }
        return "";
    }

    public Attack_Option with_spell_slot_cost(int level) {
        this.spell_slot_cost_level = Math.max(0, level);
        return this;
    }

    public Attack_Option with_pact_slot_cost() {
        this.pact_slot_cost = 1;
        return this;
    }

    public Attack_Option with_sorcery_point_cost(int points) {
        this.sorcery_point_cost = Math.max(0, points);
        return this;
    }

    public Attack_Option with_bardic_inspiration_cost(int count) {
        this.bardic_inspiration_cost = Math.max(0, count);
        return this;
    }

    public Attack_Option with_superiority_die_cost(int count) {
        this.superiority_die_cost = Math.max(0, count);
        return this;
    }

    public Attack_Option with_lay_on_hands_cost(int amount) {
        this.lay_on_hands_cost = Math.max(0, amount);
        return this;
    }

    public Attack_Option with_status(Combat_Status_Type statusType, int durationRounds, String saveAbility, int saveDc) {
        this.applied_status_type = statusType;
        this.status_duration_rounds = Math.max(1, durationRounds);
        this.status_save_ability = saveAbility == null ? "" : saveAbility;
        this.status_save_dc = saveDc;
        return this;
    }

    public Attack_Option with_target_mode(Target_Mode targetMode) {
        this.target_mode = targetMode == null ? Target_Mode.HOSTILE : targetMode;
        return this;
    }

    public Attack_Option with_healing(int diceCount, int dieSize, int bonus) {
        this.healing_dice_count = Math.max(0, diceCount);
        this.healing_dice_size = Math.max(0, dieSize);
        this.healing_bonus = bonus;
        return this;
    }

    public String get_healing_label() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.healing_dice_count).append("d").append(this.healing_dice_size);
        if (this.healing_bonus > 0) {
            sb.append("+").append(this.healing_bonus);
        } else if (this.healing_bonus < 0) {
            sb.append(this.healing_bonus);
        }
        return sb.toString();
    }
}
