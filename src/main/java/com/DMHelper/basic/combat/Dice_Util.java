package com.DMHelper.basic.combat;

import java.util.Random;

public class Dice_Util {
    private static final Random RANDOM = new Random();

    private Dice_Util() {
    }

    public static int roll_die(int sides) {
        return RANDOM.nextInt(Math.max(1, sides)) + 1;
    }

    public static int roll_d20() {
        return roll_die(20);
    }

    public static int roll_dice(int count, int sides) {
        int total = 0;
        for (int i = 0; i < Math.max(0, count); i++) {
            total += roll_die(sides);
        }
        return total;
    }
}
