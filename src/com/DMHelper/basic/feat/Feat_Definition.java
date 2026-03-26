package com.DMHelper.basic.feat;

public class Feat_Definition {
    public final String key;
    public final String label;
    public final String description;
    public final String prerequisite;

    public Feat_Definition(String key, String label, String description, String prerequisite) {
        this.key = key;
        this.label = label;
        this.description = description;
        this.prerequisite = prerequisite == null ? "" : prerequisite;
    }

    public String to_prompt_line() {
        if (this.prerequisite.isEmpty()) {
            return this.label + "： " + this.description;
        }
        return this.label + "： " + this.description + " 前提：" + this.prerequisite;
    }

    public String to_summary_line() {
        if (this.prerequisite.isEmpty()) {
            return this.label + "： " + this.description;
        }
        return this.label + "： " + this.description + "（前提：" + this.prerequisite + "）";
    }
}
