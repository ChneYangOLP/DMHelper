package com.DMDHelper.basic;

// 封装六维数据的独立实体类
public class Stats {
    public int str;
    public int dex;
    public int con;
    public int intel;
    public int wis;
    public int cha;

    // 构造函数，用于初始化具体的六维数值
    public Stats(int str, int dex, int con, int intel, int wis, int cha) {
        this.str = str;
        this.dex = dex;
        this.con = con;
        this.intel = intel;
        this.wis = wis;
        this.cha = cha;
    }

    // 核心算法：计算 DND 5e 的属性调整值 (向下取整)
    // 例如：15 -> +2，8 -> -1
    public int get_mod(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}