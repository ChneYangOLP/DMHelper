package com.DMHelper.basic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsTest {

    private final Stats stats = new Stats(10, 10, 10, 10, 10, 10);

    @Test
    void oddAbilityScoresBelowTenRoundDown() {
        assertEquals(-1, stats.get_mod(9));
        assertEquals(-2, stats.get_mod(7));
        assertEquals(-3, stats.get_mod(5));
        assertEquals(-4, stats.get_mod(3));
    }

    @Test
    void nonNegativeAbilityModifiersRemainCorrect() {
        assertEquals(0, stats.get_mod(10));
        assertEquals(0, stats.get_mod(11));
        assertEquals(1, stats.get_mod(12));
        assertEquals(5, stats.get_mod(20));
    }
}
