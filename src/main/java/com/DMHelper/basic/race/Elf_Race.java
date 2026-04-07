package com.DMHelper.basic.race;

public class Elf_Race extends Character_Race {
    private final String subrace_key;

    public Elf_Race() {
        this("HIGH");
    }

    public Elf_Race(String subraceKey) {
        super(get_race_name(subraceKey), get_speed(subraceKey), Creature_Size.MEDIUM, get_bonuses(subraceKey));
        String normalized = subraceKey == null ? "HIGH" : subraceKey.trim().toUpperCase();
        if (!"WOOD".equals(normalized) && !"DROW".equals(normalized)) {
            normalized = "HIGH";
        }
        this.subrace_key = normalized;
        this.subrace_name = get_race_name(this.subrace_key);
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        this.darkvision_range = "DROW".equals(this.subrace_key) ? 120 : 60;
        add_language("通用语");
        add_language("精灵语");
        add_trait("敏锐感官 (Keen Senses)：熟练察觉。");
        add_trait("妖精血统 (Fey Ancestry)：对魅惑豁免具有优势，且不会被魔法强制入睡。");
        add_trait("冥想 (Trance)：以 4 小时冥想代替 8 小时睡眠。");
        if ("WOOD".equals(this.subrace_key)) {
            add_trait("木精灵：额外获得感知 +1，速度提升至 35 尺。");
            add_trait("精灵武器训练 (Elf Weapon Training)：熟练长剑、短剑、短弓、长弓。");
            add_trait("野性面具 (Mask of the Wild)：在轻度遮蔽的自然环境中也能尝试隐藏。");
        } else if ("DROW".equals(this.subrace_key)) {
            add_trait("卓尔：额外获得魅力 +1，并拥有卓尔魔法。");
            add_trait("卓尔武器训练 (Drow Weapon Training)：熟练细剑、短剑、手弩。");
            add_trait("阳光敏感 (Sunlight Sensitivity)：阳光下的攻击检定与依赖视觉的察觉检定具有劣势。");
        } else {
            add_trait("高等精灵：额外获得智力 +1。");
            add_trait("精灵武器训练 (Elf Weapon Training)：熟练长剑、短剑、短弓、长弓。");
            add_trait("戏法与语言：额外学习 1 个法师戏法，并掌握 1 门额外语言。");
            add_language("额外自选语言 1 门");
        }
    }

    private static String get_race_name(String subraceKey) {
        if ("WOOD".equalsIgnoreCase(subraceKey)) {
            return "木精灵 (Wood Elf)";
        }
        if ("DROW".equalsIgnoreCase(subraceKey)) {
            return "卓尔精灵 (Drow)";
        }
        return "高等精灵 (High Elf)";
    }

    private static int get_speed(String subraceKey) {
        return "WOOD".equalsIgnoreCase(subraceKey) ? 35 : 30;
    }

    private static Ability_Bonuses get_bonuses(String subraceKey) {
        if ("WOOD".equalsIgnoreCase(subraceKey)) {
            return new Ability_Bonuses(0, 2, 0, 0, 1, 0);
        }
        if ("DROW".equalsIgnoreCase(subraceKey)) {
            return new Ability_Bonuses(0, 2, 0, 0, 0, 1);
        }
        return new Ability_Bonuses(0, 2, 0, 1, 0, 0);
    }
}
