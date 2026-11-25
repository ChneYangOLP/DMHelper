package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass;
import com.DMHelper.basic.race.Human_Race;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FighterAdvancementServiceTest {

    @Test
    void levelThreeFighterCanChooseBattleMasterAndRequiredManeuvers() {
        Character_Sheet character = fighterAtLevel(3);
        Fighter_Class fighter = (Fighter_Class) character.job;

        FighterAdvancementService.apply(character, "SUBCLASS", "BATTLE_MASTER", null);

        assertEquals(Fighter_Subclass.BATTLE_MASTER, fighter.fighter_subclass);
        assertEquals(3, fighter.get_expected_maneuver_count());

        List<String> maneuvers = fighter.get_available_maneuvers().subList(0, 3);
        FighterAdvancementService.apply(character, "MANEUVERS", null, maneuvers);

        assertEquals(3, fighter.maneuver_names.size());
        assertTrue(fighter.maneuver_names.containsAll(maneuvers));
    }

    @Test
    void levelFourFighterCanSpendAbilityScoreImprovement() {
        Character_Sheet character = fighterAtLevel(4);
        Fighter_Class fighter = (Fighter_Class) character.job;
        int strengthBefore = character.stats.str;

        FighterAdvancementService.apply(character, "ASI_PLUS_TWO", "STR", null);

        assertEquals(strengthBefore + 2, character.stats.str);
        assertEquals(1, fighter.used_asi_choices);
        assertEquals(0, fighter.pending_asi_count);
    }

    private static Character_Sheet fighterAtLevel(int level) {
        Fighter_Class fighter = new Fighter_Class();
        fighter.fighting_style_name = "Defense";
        fighter.current_level = level;
        Character_Sheet character = Character_Sheet.restore_saved_character(
                "fighter-upgrade-test",
                20,
                "unspecified",
                new Human_Race(),
                fighter,
                new Stats(14, 12, 14, 10, 10, 10)
        );
        character.recalculate_derived_stats();
        return character;
    }
}
