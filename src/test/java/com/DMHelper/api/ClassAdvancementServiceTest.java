package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.bard.Bard_College;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Oath;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerous_Origin;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Pact;
import com.DMHelper.basic.playerclass.warlock.Warlock_Patron;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Subclass;
import com.DMHelper.basic.race.Human_Race;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassAdvancementServiceTest {

    @Test
    void wizardCanChooseArcaneTraditionAtLevelTwo() {
        Character_Sheet character = characterAtLevel(new Wizard_Class(), 2);
        Wizard_Class wizard = (Wizard_Class) character.job;

        ClassAdvancementService.apply(character, "WIZARD_SUBCLASS", "EVOCATION", null);

        assertEquals(Wizard_Subclass.EVOCATION, wizard.wizard_subclass);
        assertTrue(wizard.traits.stream().anyMatch(trait -> trait.contains("塑能雕琢")));
    }

    @Test
    void sorcererCanChooseOriginAndMetamagic() {
        Character_Sheet character = characterAtLevel(new Sorcerer_Class(), 3);
        Sorcerer_Class sorcerer = (Sorcerer_Class) character.job;

        ClassAdvancementService.apply(character, "SORCERER_ORIGIN", "WILD_MAGIC", null);
        List<String> choices = sorcerer.get_available_metamagic_options().subList(0, 2);
        ClassAdvancementService.apply(character, "SORCERER_METAMAGIC", null, choices);

        assertEquals(Sorcerous_Origin.WILD_MAGIC, sorcerer.sorcerous_origin);
        assertEquals(2, sorcerer.metamagic_keys.size());
        assertEquals(0, sorcerer.pending_metamagic_choices);
    }

    @Test
    void draconicOriginUnlocksRequiredAncestryChoice() {
        Character_Sheet character = characterAtLevel(new Sorcerer_Class(), 1);
        Sorcerer_Class sorcerer = (Sorcerer_Class) character.job;

        ClassAdvancementService.apply(character, "SORCERER_ORIGIN", "DRACONIC_BLOODLINE", null);

        assertTrue(sorcerer.get_pending_choices().contains("选择龙族先祖"));
        assertEquals("DRAGON_ANCESTRY",
                AdvancementOptionsBuilder.build(character).getAsJsonArray("groups").get(0).getAsJsonObject().get("type").getAsString());

        ClassAdvancementService.apply(character, "DRAGON_ANCESTRY", "红龙 (Red Dragon) - 火焰", null);

        assertEquals("红龙 (Red Dragon) - 火焰", sorcerer.dragon_ancestry);
    }

    @Test
    void paladinCanChooseStyleAndOath() {
        Character_Sheet character = characterAtLevel(new Paladin_Class(), 3);
        Paladin_Class paladin = (Paladin_Class) character.job;
        String style = paladin.get_available_fighting_styles().get(0);

        ClassAdvancementService.apply(character, "PALADIN_STYLE", style, null);
        ClassAdvancementService.apply(character, "PALADIN_OATH", "DEVOTION", null);

        assertEquals(style, paladin.fighting_style_name);
        assertEquals(Paladin_Oath.DEVOTION, paladin.sacred_oath);
        assertTrue(paladin.traits.stream().anyMatch(trait -> trait.contains("奉献誓言")));
    }

    @Test
    void warlockPactRefreshesInvocationChoices() {
        Character_Sheet character = characterAtLevel(new Warlock_Class(), 3);
        Warlock_Class warlock = (Warlock_Class) character.job;

        ClassAdvancementService.apply(character, "WARLOCK_PATRON", "FIEND", null);
        ClassAdvancementService.apply(character, "WARLOCK_PACT", "BLADE", null);
        List<String> invocations = warlock.get_available_invocation_options().subList(0, 2);
        ClassAdvancementService.apply(character, "WARLOCK_INVOCATIONS", null, invocations);

        assertEquals(Warlock_Patron.FIEND, warlock.patron);
        assertEquals(Warlock_Pact.BLADE, warlock.pact_boon);
        assertEquals(2, warlock.invocation_keys.size());
        assertEquals(0, warlock.pending_invocation_choices);
    }

    @Test
    void loreCollegeUnlocksAndAcceptsBonusSkills() {
        Character_Sheet character = characterAtLevel(new Bard_Class(), 3);
        Bard_Class bard = (Bard_Class) character.job;

        ClassAdvancementService.apply(character, "BARD_COLLEGE", "LORE", null);
        List<String> bonusSkills = bard.get_available_bonus_skill_options().subList(0, 3);
        ClassAdvancementService.apply(character, "BARD_BONUS_SKILLS", null, bonusSkills);

        assertEquals(Bard_College.LORE, bard.bard_college);
        assertEquals(3, bard.lore_bonus_skill_keys.size());
        assertEquals(0, bard.pending_bonus_skill_choices);
    }

    @Test
    void nonFighterCanSpendAbilityScoreImprovement() {
        Character_Sheet character = characterAtLevel(new Wizard_Class(), 4);
        Wizard_Class wizard = (Wizard_Class) character.job;
        int intelligenceBefore = character.stats.intel;

        ClassAdvancementService.apply(character, "ASI_PLUS_TWO", "INT", null);

        assertEquals(intelligenceBefore + 2, character.stats.intel);
        assertEquals(1, wizard.used_asi_choices);
        assertEquals(0, wizard.pending_asi_count);
    }

    @Test
    void splitAbilityIncreaseRequiresDifferentAbilities() {
        Character_Sheet character = characterAtLevel(new Wizard_Class(), 4);

        assertThrows(IllegalArgumentException.class, () ->
                ClassAdvancementService.apply(character, "ASI_PLUS_ONE_TWO", null, List.of("INT", "INT")));
    }

    private static Character_Sheet characterAtLevel(Character_Class job, int level) {
        job.current_level = level;
        Character_Sheet character = Character_Sheet.restore_saved_character(
                "class-upgrade-test",
                20,
                "unspecified",
                new Human_Race(),
                job,
                new Stats(12, 12, 12, 14, 12, 14)
        );
        character.recalculate_derived_stats();
        return character;
    }
}
