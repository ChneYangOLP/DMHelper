package com.DMHelper.basic.playerclass;

public class Dnd5e_Progression {

    private static final int[] XP_THRESHOLDS = {
            0, 300, 900, 2700, 6500,
            14000, 23000, 34000, 48000, 64000,
            85000, 100000, 120000, 140000, 165000,
            195000, 225000, 265000, 305000, 355000
    };

    public static int get_xp_for_level(int level) {
        if (level <= 1) {
            return 0;
        }
        if (level > 20) {
            return XP_THRESHOLDS[XP_THRESHOLDS.length - 1];
        }
        return XP_THRESHOLDS[level - 1];
    }

    public static int get_level_for_xp(int experience_points) {
        int normalizedXp = Math.max(0, experience_points);
        int level = 1;
        for (int i = 0; i < XP_THRESHOLDS.length; i++) {
            if (normalizedXp >= XP_THRESHOLDS[i]) {
                level = i + 1;
            }
        }
        return Math.min(level, 20);
    }

    public static int get_next_level_xp(int current_level) {
        if (current_level >= 20) {
            return -1;
        }
        return get_xp_for_level(current_level + 1);
    }
}
