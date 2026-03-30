package com.DMHelper.basic.race;

public class Gnome_Race extends Character_Race {
    private final String subrace_key;

    public Gnome_Race() {
        this("FOREST");
    }

    public Gnome_Race(String subraceKey) {
        super(get_race_name(subraceKey), 25, Creature_Size.SMALL, get_bonuses(subraceKey));
        this.subrace_key = "ROCK".equalsIgnoreCase(subraceKey) ? "ROCK" : "FOREST";
        this.subrace_name = get_race_name(this.subrace_key);
        apply_racial_traits();
    }

    @Override
    public void apply_racial_traits() {
        reset_traits();
        this.darkvision_range = 60;
        add_language("通用语");
        add_language("侏儒语");
        add_trait("侏儒机敏 (Gnome Cunning)：对抗魔法造成的智力、感知、魅力豁免具有优势。");
        if ("ROCK".equals(this.subrace_key)) {
            add_trait("岩侏儒：额外获得体质 +1。");
            add_trait("工匠学识 (Artificer's Lore)：与魔法物品、炼金物件或科技装置相关的历史检定加入双倍熟练加值。");
            add_trait("修补匠 (Tinker)：熟练工匠工具，可制作小型发条装置。");
        } else {
            add_trait("森林侏儒：额外获得敏捷 +1。");
            add_trait("自然幻术师 (Natural Illusionist)：学习次级幻影戏法。");
            add_trait("与小兽交谈 (Speak with Small Beasts)：可与小型或更小的野兽传达简单意思。");
        }
    }

    private static String get_race_name(String subraceKey) {
        return "ROCK".equalsIgnoreCase(subraceKey)
                ? "岩侏儒 (Rock Gnome)"
                : "森林侏儒 (Forest Gnome)";
    }

    private static Ability_Bonuses get_bonuses(String subraceKey) {
        if ("ROCK".equalsIgnoreCase(subraceKey)) {
            return new Ability_Bonuses(0, 0, 1, 2, 0, 0);
        }
        return new Ability_Bonuses(0, 1, 0, 2, 0, 0);
    }
}
