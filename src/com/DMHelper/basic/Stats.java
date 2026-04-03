package com.DMHelper.basic;

public class Stats {
    public int str;
    public int dex;
    public int con;
    public int intel;
    public int wis;
    public int cha;

    public Stats(int str, int dex, int con, int intel, int wis, int cha) {
        this.str = str;
        this.dex = dex;
        this.con = con;
        this.intel = intel;
        this.wis = wis;
        this.cha = cha;
    }

    // D&D 5e 的属性调整值按 (score - 10) / 2 向下取整。
    public int get_mod(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
