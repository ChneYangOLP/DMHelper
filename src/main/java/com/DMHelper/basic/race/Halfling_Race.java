package com.DMHelper.basic.race;

public class Halfling_Race extends Character_Race {
    private final String subrace_key;

    public Halfling_Race() {
        this("LIGHTFOOT");
    }

    public Halfling_Race(String subraceKey) {
        super(get_race_name(subraceKey), 25, Creature_Size.SMALL, get_bonuses(subraceKey));
        this.subrace_key = "STOUT".equalsIgnoreCase(subraceKey) ? "STOUT" : "LIGHTFOOT";
        this.subrace_name = get_race_name(this.subrace_key);
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        add_language("通用语");
        add_language("半身人语");
        add_trait("幸运 (Lucky)：攻击检定、属性检定或豁免掷出 1 时可以重掷。");
        add_trait("勇敢 (Brave)：对恐慌豁免具有优势。");
        add_trait("半身人灵巧 (Halfling Nimbleness)：可穿过体型比你大的生物占据空间。");
        if ("STOUT".equals(this.subrace_key)) {
            add_trait("健壮半身人：额外获得体质 +1，并对毒素豁免具有优势、对毒素伤害具有抗性。");
        } else {
            add_trait("轻足半身人：额外获得魅力 +1，并可借体型更大的生物遮蔽自己。");
        }
    }

    private static String get_race_name(String subraceKey) {
        return "STOUT".equalsIgnoreCase(subraceKey)
                ? "健壮半身人 (Stout Halfling)"
                : "轻足半身人 (Lightfoot Halfling)";
    }

    private static Ability_Bonuses get_bonuses(String subraceKey) {
        if ("STOUT".equalsIgnoreCase(subraceKey)) {
            return new Ability_Bonuses(0, 2, 1, 0, 0, 0);
        }
        return new Ability_Bonuses(0, 2, 0, 0, 0, 1);
    }
}
