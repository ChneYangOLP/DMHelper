package com.DMHelper.basic;

import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.race.Human_Race;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatPrerequisiteTest {
    @Test
    void bardAndWarlockQualifyForSpellcastingFeats() {
        Character_Sheet bard = characterWith(new Bard_Class());
        Character_Sheet warlock = characterWith(new Warlock_Class());

        assertTrue(Feat_Library.meets_prerequisite(bard, "Spell Sniper"));
        assertTrue(Feat_Library.meets_prerequisite(bard, "War Caster"));
        assertTrue(Feat_Library.meets_prerequisite(warlock, "Spell Sniper"));
        assertTrue(Feat_Library.meets_prerequisite(warlock, "War Caster"));
    }

    @Test
    void paladinQualifiesOnlyAfterGainingSpellcasting() {
        Paladin_Class paladinClass = new Paladin_Class();
        Character_Sheet paladin = characterWith(paladinClass);
        assertFalse(Feat_Library.meets_prerequisite(paladin, "War Caster"));

        paladinClass.current_level = 2;
        assertTrue(Feat_Library.meets_prerequisite(paladin, "War Caster"));
    }

    @Test
    void nonCasterDoesNotQualifyForSpellcastingFeats() {
        Character_Sheet fighter = characterWith(new Fighter_Class());
        assertFalse(Feat_Library.meets_prerequisite(fighter, "Spell Sniper"));
        assertFalse(Feat_Library.meets_prerequisite(fighter, "War Caster"));
    }

    private static Character_Sheet characterWith(com.DMHelper.basic.playerclass.Character_Class job) {
        return Character_Sheet.create_new_character(
                "测试角色", 20, "未指定", new Human_Race(), job,
                new Stats(12, 12, 12, 12, 12, 12)
        );
    }
}
