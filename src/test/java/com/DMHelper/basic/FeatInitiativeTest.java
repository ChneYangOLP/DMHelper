package com.DMHelper.basic;

import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatInitiativeTest {
    @Test
    void alertAddsFiveToInitiativeBonus() {
        Fighter_Class fighter = new Fighter_Class();
        fighter.feat_names.add("Alert");

        assertEquals(5, fighter.get_initiative_bonus());
    }

    @Test
    void bardCombinesJackOfAllTradesWithAlert() {
        Bard_Class bard = new Bard_Class();
        bard.current_level = 2;
        bard.feat_names.add("Alert");

        assertEquals(6, bard.get_initiative_bonus());
    }
}
