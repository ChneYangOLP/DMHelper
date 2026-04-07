package com.DMHelper.basic.race;

public class Half_Elf_Race extends Character_Race {
    private final String bonus_one_key;
    private final String bonus_two_key;

    public Half_Elf_Race() {
        this("DEX", "INT");
    }

    public Half_Elf_Race(String bonusOneKey, String bonusTwoKey) {
        super(build_race_name(normalize_bonus_key(bonusOneKey), normalize_bonus_two(normalize_bonus_key(bonusOneKey), bonusTwoKey)),
                30,
                Creature_Size.MEDIUM,
                build_bonuses(normalize_bonus_key(bonusOneKey), normalize_bonus_two(normalize_bonus_key(bonusOneKey), bonusTwoKey)));
        this.bonus_one_key = normalize_bonus_key(bonusOneKey);
        this.bonus_two_key = normalize_bonus_two(this.bonus_one_key, bonusTwoKey);
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        this.darkvision_range = 60;
        add_language("通用语");
        add_language("精灵语");
        add_language("额外自选语言 1 门");
        add_trait("混血适应力：魅力 +2，另有两项不同属性各 +1，当前为 " + get_bonus_label(this.bonus_one_key) + " 与 " + get_bonus_label(this.bonus_two_key) + "。");
        add_trait("妖精血统 (Fey Ancestry)：对魅惑豁免具有优势，且不会被魔法强制入睡。");
        add_trait("技能多才 (Skill Versatility)：额外获得任意 2 项技能熟练。");
    }

    private static String normalize_bonus_key(String bonusKey) {
        if ("STR".equalsIgnoreCase(bonusKey)
                || "DEX".equalsIgnoreCase(bonusKey)
                || "CON".equalsIgnoreCase(bonusKey)
                || "INT".equalsIgnoreCase(bonusKey)
                || "WIS".equalsIgnoreCase(bonusKey)) {
            return bonusKey.toUpperCase();
        }
        return "DEX";
    }

    private static String normalize_bonus_two(String firstKey, String bonusKey) {
        String normalized = normalize_bonus_key(bonusKey);
        if (firstKey.equals(normalized)) {
            return "DEX".equals(firstKey) ? "INT" : "DEX";
        }
        return normalized;
    }

    private static Ability_Bonuses build_bonuses(String firstKey, String secondKey) {
        Ability_Bonuses bonuses = new Ability_Bonuses(0, 0, 0, 0, 0, 2);
        apply_bonus(bonuses, firstKey);
        apply_bonus(bonuses, secondKey);
        return bonuses;
    }

    private static void apply_bonus(Ability_Bonuses bonuses, String key) {
        if ("STR".equals(key)) bonuses.strength_bonus++;
        else if ("DEX".equals(key)) bonuses.dexterity_bonus++;
        else if ("CON".equals(key)) bonuses.constitution_bonus++;
        else if ("INT".equals(key)) bonuses.intelligence_bonus++;
        else if ("WIS".equals(key)) bonuses.wisdom_bonus++;
    }

    private static String build_race_name(String firstKey, String secondKey) {
        return "半精灵 (Half-Elf) [" + get_bonus_label(firstKey) + " +1 / " + get_bonus_label(secondKey) + " +1]";
    }

    private static String get_bonus_label(String key) {
        if ("STR".equals(key)) return "力量";
        if ("DEX".equals(key)) return "敏捷";
        if ("CON".equals(key)) return "体质";
        if ("INT".equals(key)) return "智力";
        if ("WIS".equals(key)) return "感知";
        return key;
    }
}
