package com.DMHelper.basic.race;

public final class Race_Factory {

    private Race_Factory() {
    }

    public static Character_Race from_saved_name(String raceName) {
        String safeName = raceName == null ? "" : raceName.trim();
        if (safeName.isEmpty()) {
            return new Human_Race();
        }

        if (safeName.contains("人类") || "Human".equalsIgnoreCase(safeName)) {
            return new Human_Race();
        }
        if (safeName.contains("山地矮人") || safeName.contains("Mountain Dwarf")) {
            return new Dwarf_Race("MOUNTAIN");
        }
        if (safeName.contains("丘陵矮人") || safeName.contains("Hill Dwarf") || safeName.contains("Dwarf")) {
            return new Dwarf_Race("HILL");
        }
        if (safeName.contains("木精灵") || safeName.contains("Wood Elf")) {
            return new Elf_Race("WOOD");
        }
        if (safeName.contains("卓尔") || safeName.contains("Drow")) {
            return new Elf_Race("DROW");
        }
        if (safeName.contains("高等精灵") || safeName.contains("High Elf") || safeName.contains("Elf")) {
            return new Elf_Race("HIGH");
        }
        if (safeName.contains("健壮半身人") || safeName.contains("Stout Halfling")) {
            return new Halfling_Race("STOUT");
        }
        if (safeName.contains("轻足半身人") || safeName.contains("Lightfoot Halfling") || safeName.contains("Halfling")) {
            return new Halfling_Race("LIGHTFOOT");
        }
        if (safeName.contains("岩侏儒") || safeName.contains("Rock Gnome")) {
            return new Gnome_Race("ROCK");
        }
        if (safeName.contains("森林侏儒") || safeName.contains("Forest Gnome") || safeName.contains("Gnome")) {
            return new Gnome_Race("FOREST");
        }
        if (safeName.contains("龙裔") || safeName.contains("Dragonborn")) {
            return new Dragonborn_Race(parse_dragonborn_ancestry(safeName));
        }
        if (safeName.contains("半精灵") || safeName.contains("Half-Elf") || safeName.contains("Half_Elf")) {
            return new Half_Elf_Race(parse_half_elf_bonus_one(safeName), parse_half_elf_bonus_two(safeName));
        }
        if (safeName.contains("半兽人") || safeName.contains("Half-Orc") || safeName.contains("Half_Orc")) {
            return new Half_Orc_Race();
        }
        if (safeName.contains("提夫林") || safeName.contains("Tiefling")) {
            return new Tiefling_Race();
        }
        return new Human_Race();
    }

    private static String parse_dragonborn_ancestry(String name) {
        if (name.contains("黑龙") || name.contains("Black")) return "BLACK";
        if (name.contains("蓝龙") || name.contains("Blue")) return "BLUE";
        if (name.contains("黄铜龙") || name.contains("Brass")) return "BRASS";
        if (name.contains("青铜龙") || name.contains("Bronze")) return "BRONZE";
        if (name.contains("赤铜龙") || name.contains("Copper")) return "COPPER";
        if (name.contains("金龙") || name.contains("Gold")) return "GOLD";
        if (name.contains("绿龙") || name.contains("Green")) return "GREEN";
        if (name.contains("银龙") || name.contains("Silver")) return "SILVER";
        if (name.contains("白龙") || name.contains("White")) return "WHITE";
        return "RED";
    }

    private static String parse_half_elf_bonus_one(String name) {
        if (name.contains("力量")) return "STR";
        if (name.contains("敏捷")) return "DEX";
        if (name.contains("体质")) return "CON";
        if (name.contains("智力")) return "INT";
        if (name.contains("感知")) return "WIS";
        return "DEX";
    }

    private static String parse_half_elf_bonus_two(String name) {
        String first = parse_half_elf_bonus_one(name);
        String[] keys = {"STR", "DEX", "CON", "INT", "WIS"};
        String[] labels = {"力量", "敏捷", "体质", "智力", "感知"};
        for (int i = 0; i < keys.length; i++) {
            if (!keys[i].equals(first) && name.contains(labels[i])) {
                return keys[i];
            }
        }
        return "DEX".equals(first) ? "INT" : "DEX";
    }
}
