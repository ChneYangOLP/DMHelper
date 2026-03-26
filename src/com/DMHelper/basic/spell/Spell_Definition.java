package com.DMHelper.basic.spell;

public class Spell_Definition {
    public final String key;
    public final String display_name;
    public final int level;
    public final String school_or_theme;
    public final String short_description;

    public Spell_Definition(String key, String display_name, int level, String school_or_theme, String short_description) {
        this.key = key;
        this.display_name = display_name;
        this.level = level;
        this.school_or_theme = school_or_theme;
        this.short_description = short_description;
    }

    public String to_brief_label() {
        String levelText = this.level == 0 ? "戏法" : this.level + "环";
        return levelText + " - " + this.display_name;
    }

    public String to_detail_line() {
        String levelText = this.level == 0 ? "戏法" : this.level + "环";
        return levelText + " | " + this.display_name + " | " + this.school_or_theme + " | " + this.short_description;
    }
}
