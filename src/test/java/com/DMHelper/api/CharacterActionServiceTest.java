package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.race.Human_Race;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterActionServiceTest {

    @Test
    void nonStackableItemCannotBePurchasedInBulk() {
        Character_Sheet character = createCharacter(new Fighter_Class());
        Equipment_Item halfPlate = requireItem("half_plate");
        character.gold_pieces = 1_000;
        int currencyBefore = character.get_total_currency_cp();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.buyItem(character, halfPlate, 2)
        );

        assertEquals("Non-stackable items must use quantity 1", error.getMessage());
        assertEquals(currencyBefore, character.get_total_currency_cp());
        assertEquals(0, character.get_item_count(halfPlate.key));
    }

    @Test
    void sellingMoreThanInventoryIsRejectedWithoutMutation() {
        Character_Sheet character = createCharacter(new Fighter_Class());
        Equipment_Item potion = requireItem("healing_potion");
        character.add_item_to_inventory(potion.key);
        int currencyBefore = character.get_total_currency_cp();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.sellItem(character, potion, 2)
        );

        assertEquals("Not enough items to sell", error.getMessage());
        assertEquals(1, character.get_item_count(potion.key));
        assertEquals(currencyBefore, character.get_total_currency_cp());
    }

    @Test
    void nonFighterCannotUseSecondWind() {
        Character_Sheet wizard = createCharacter(new Wizard_Class());
        int hpBefore = wizard.current_hp;

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.rest(wizard, "SECOND_WIND")
        );

        assertEquals("Second Wind is only available to fighters", error.getMessage());
        assertEquals(hpBefore, wizard.current_hp);
    }

    @Test
    void fighterCannotUseSecondWindWhenNoUsesRemain() {
        Fighter_Class fighterClass = new Fighter_Class();
        Character_Sheet fighter = createCharacter(fighterClass);
        fighterClass.current_second_wind_uses = 0;
        int hpBefore = fighter.current_hp;

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.rest(fighter, "SECOND_WIND")
        );

        assertEquals("No Second Wind uses remaining", error.getMessage());
        assertEquals(0, fighterClass.current_second_wind_uses);
        assertEquals(hpBefore, fighter.current_hp);
    }

    @Test
    void shortRestSpendsSelectedHitDiceAndRecoversHp() {
        Character_Sheet fighter = createCharacter(new Fighter_Class());
        fighter.set_current_hp(1);
        int hitDiceBefore = fighter.available_hit_dice;
        int hpBefore = fighter.current_hp;

        CharacterActionService.rest(fighter, "SHORT", 1);

        assertEquals(hitDiceBefore - 1, fighter.available_hit_dice);
        assertTrue(fighter.current_hp > hpBefore);
        assertTrue(fighter.current_hp <= hpBefore + fighter.job.hp_dice);
    }

    @Test
    void shortRestRejectsUnavailableHitDiceWithoutMutation() {
        Character_Sheet fighter = createCharacter(new Fighter_Class());
        fighter.set_current_hp(1);
        int hpBefore = fighter.current_hp;
        int hitDiceBefore = fighter.available_hit_dice;

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.rest(fighter, "SHORT", hitDiceBefore + 1)
        );

        assertEquals("Not enough hit dice available", error.getMessage());
        assertEquals(hitDiceBefore, fighter.available_hit_dice);
        assertEquals(hpBefore, fighter.current_hp);
    }

    @Test
    void xpMustBePositive() {
        Character_Sheet character = createCharacter(new Fighter_Class());

        assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.addXp(character, 0, "invalid")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.addXp(character, -1, "invalid")
        );
        assertEquals(0, character.experience_points);

        CharacterActionService.addXp(character, 100, "quest");
        assertEquals(100, character.experience_points);
    }

    @Test
    void xpOverflowIsRejectedWithoutMutation() {
        Character_Sheet character = createCharacter(new Fighter_Class());
        character.experience_points = Integer.MAX_VALUE - 5;

        assertThrows(
                ArithmeticException.class,
                () -> CharacterActionService.addXp(character, 10, "overflow")
        );

        assertEquals(Integer.MAX_VALUE - 5, character.experience_points);
    }

    @Test
    void nonAutomaticUseItemIsNotConsumed() {
        Character_Sheet character = createCharacter(new Fighter_Class());
        Equipment_Item torch = requireItem("torch");
        character.add_item_to_inventory(torch.key);
        int notesBefore = character.advancement_notes.size();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CharacterActionService.useItem(character, torch)
        );

        assertEquals("This item has no automatic use effect", error.getMessage());
        assertEquals(1, character.get_item_count(torch.key));
        assertTrue(character.owned_equipment_keys.contains(torch.key));
        assertEquals(notesBefore, character.advancement_notes.size());
    }

    @Test
    void coinPouchAddsCurrencyAndIsConsumed() {
        Character_Sheet character = createCharacter(new Fighter_Class());
        Equipment_Item goldPouch = requireItem("gold_pouch");
        character.add_item_to_inventory(goldPouch.key);
        int currencyBefore = character.get_total_currency_cp();

        CharacterActionService.useItem(character, goldPouch);

        assertEquals(currencyBefore + goldPouch.get_currency_gain_cp(), character.get_total_currency_cp());
        assertEquals(0, character.get_item_count(goldPouch.key));
        assertFalse(character.owned_equipment_keys.contains(goldPouch.key));
    }

    private static Character_Sheet createCharacter(Character_Class characterClass) {
        return Character_Sheet.restore_saved_character(
                "测试角色",
                20,
                "未指定",
                new Human_Race(),
                characterClass,
                new Stats(10, 10, 10, 10, 10, 10)
        );
    }

    private static Equipment_Item requireItem(String key) {
        Equipment_Item item = Equipment_Library.get_item(key);
        if (item == null) {
            throw new IllegalStateException("Missing test item: " + key);
        }
        return item;
    }
}
