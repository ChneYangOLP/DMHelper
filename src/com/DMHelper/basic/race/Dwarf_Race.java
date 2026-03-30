package com.DMHelper.basic.race;

public class Dwarf_Race extends Character_Race {
    private final String subrace_key;

    public Dwarf_Race() {
        this("HILL");
    }

    public Dwarf_Race(String subraceKey) {
        super(get_race_name(subraceKey), 25, Creature_Size.MEDIUM, get_bonuses(subraceKey));
        this.subrace_key = "MOUNTAIN".equalsIgnoreCase(subraceKey) ? "MOUNTAIN" : "HILL";
        this.subrace_name = get_subrace_name(this.subrace_key);
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        this.darkvision_range = 60;
        add_language("通用语");
        add_language("矮人语");
        add_trait("矮人坚韧 (Dwarven Resilience)：对毒素豁免具有优势，并对毒素伤害具有抗性。");
        add_trait("矮人战斗训练 (Dwarven Combat Training)：熟练战斧、手斧、轻锤与战锤。");
        add_trait("工具熟练：从铁匠工具、酿酒工具、石匠工具中选择 1 项熟练。");
        add_trait("石工巧识 (Stonecunning)：与石制结构相关的历史检定视为熟练，并加入双倍熟练加值。");
        if ("MOUNTAIN".equals(this.subrace_key)) {
            add_trait("山地矮人：额外获得力量 +2，并熟练轻甲与中甲。");
        } else {
            add_trait("丘陵矮人：额外获得感知 +1，并因矮人硬朗每级额外 +1 最大生命值。");
        }
    }

    private static String get_race_name(String subraceKey) {
        return "MOUNTAIN".equalsIgnoreCase(subraceKey)
                ? "山地矮人 (Mountain Dwarf)"
                : "丘陵矮人 (Hill Dwarf)";
    }

    private static String get_subrace_name(String subraceKey) {
        return "MOUNTAIN".equalsIgnoreCase(subraceKey)
                ? "山地矮人 (Mountain Dwarf)"
                : "丘陵矮人 (Hill Dwarf)";
    }

    private static Ability_Bonuses get_bonuses(String subraceKey) {
        if ("MOUNTAIN".equalsIgnoreCase(subraceKey)) {
            return new Ability_Bonuses(2, 0, 2, 0, 0, 0);
        }
        return new Ability_Bonuses(0, 0, 2, 0, 1, 0);
    }
}
