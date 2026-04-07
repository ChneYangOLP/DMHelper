package com.DMHelper.basic.race;

public class Dragonborn_Race extends Character_Race {
    private final String ancestry_key;

    public Dragonborn_Race() {
        this("RED");
    }

    public Dragonborn_Race(String ancestryKey) {
        super(get_race_name(ancestryKey), 30, Creature_Size.MEDIUM, new Ability_Bonuses(2, 0, 0, 0, 0, 1));
        this.ancestry_key = normalize_ancestry(ancestryKey);
        this.subrace_name = get_race_name(this.ancestry_key);
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        add_language("通用语");
        add_language("龙语");
        add_trait("龙族先祖 (Draconic Ancestry)：当前血脉为 " + get_ancestry_label(this.ancestry_key) + "。");
        add_trait("吐息武器 (Breath Weapon)：以 " + get_breath_shape(this.ancestry_key) + " 造成 " + get_damage_type(this.ancestry_key) + " 伤害。");
        add_trait("伤害抗性 (Damage Resistance)：获得 " + get_damage_type(this.ancestry_key) + " 抗性。");
    }

    private static String normalize_ancestry(String ancestryKey) {
        String normalized = ancestryKey == null ? "RED" : ancestryKey.trim().toUpperCase();
        switch (normalized) {
            case "BLACK":
            case "BLUE":
            case "BRASS":
            case "BRONZE":
            case "COPPER":
            case "GOLD":
            case "GREEN":
            case "RED":
            case "SILVER":
            case "WHITE":
                return normalized;
            default:
                return "RED";
        }
    }

    private static String get_race_name(String ancestryKey) {
        return "龙裔 (Dragonborn) - " + get_ancestry_label(normalize_ancestry(ancestryKey));
    }

    private static String get_ancestry_label(String ancestryKey) {
        switch (ancestryKey) {
            case "BLACK": return "黑龙裔 / 强酸";
            case "BLUE": return "蓝龙裔 / 闪电";
            case "BRASS": return "黄铜龙裔 / 火焰";
            case "BRONZE": return "青铜龙裔 / 闪电";
            case "COPPER": return "赤铜龙裔 / 强酸";
            case "GOLD": return "金龙裔 / 火焰";
            case "GREEN": return "绿龙裔 / 毒素";
            case "SILVER": return "银龙裔 / 寒冷";
            case "WHITE": return "白龙裔 / 寒冷";
            default: return "红龙裔 / 火焰";
        }
    }

    private static String get_damage_type(String ancestryKey) {
        switch (ancestryKey) {
            case "BLACK":
            case "COPPER":
                return "强酸";
            case "BLUE":
            case "BRONZE":
                return "闪电";
            case "GREEN":
                return "毒素";
            case "SILVER":
            case "WHITE":
                return "寒冷";
            default:
                return "火焰";
        }
    }

    private static String get_breath_shape(String ancestryKey) {
        switch (ancestryKey) {
            case "BLACK":
            case "BLUE":
            case "BRASS":
            case "BRONZE":
            case "COPPER":
                return "5 尺宽、30 尺长线形";
            default:
                return "15 尺锥形";
        }
    }
}
