package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.combat.Dice_Util;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;

/**
 * Pure business operations shared by the HTTP controller and unit tests.
 * Persistence and HTTP status mapping deliberately remain in the controller.
 */
public final class CharacterActionService {
    private CharacterActionService() {
    }

    public static void addItem(Character_Sheet character, Equipment_Item item, int quantity) {
        requireCharacterAndItem(character, item);
        validateQuantity(item, quantity);
        for (int i = 0; i < quantity; i++) {
            character.add_item_to_inventory(item.key);
        }
        character.record_advancement("获得物品：" + item.display_name + " x" + quantity);
    }

    public static void buyItem(Character_Sheet character, Equipment_Item item, int quantity) {
        requireCharacterAndItem(character, item);
        validateQuantity(item, quantity);
        int totalCost = Math.multiplyExact(item.value_in_cp, quantity);
        if (!character.spend_currency_cp(totalCost)) {
            throw new IllegalArgumentException("Not enough currency");
        }
        for (int i = 0; i < quantity; i++) {
            character.add_item_to_inventory(item.key);
        }
        character.record_advancement("购买物品：" + item.display_name + " x" + quantity + "，花费 "
                + Equipment_Item.format_cp_value(totalCost));
    }

    public static void sellItem(Character_Sheet character, Equipment_Item item, int quantity) {
        requireCharacterAndItem(character, item);
        validateQuantity(item, quantity);
        if (character.get_item_count(item.key) < quantity) {
            throw new IllegalArgumentException("Not enough items to sell");
        }
        int totalGain = Math.multiplyExact(item.get_sale_value_cp(), quantity);
        for (int i = 0; i < quantity; i++) {
            if (!character.remove_item_from_inventory(item.key)) {
                throw new IllegalStateException("Inventory changed while selling items");
            }
        }
        character.add_currency_cp(totalGain);
        character.record_advancement("出售物品：" + item.display_name + " x" + quantity + "，获得 "
                + Equipment_Item.format_cp_value(totalGain));
    }

    public static void useItem(Character_Sheet character, Equipment_Item item) {
        requireCharacterAndItem(character, item);
        if (character.get_item_count(item.key) <= 0) {
            throw new IllegalArgumentException("Item is not in inventory");
        }

        if (item.is_healing_item()) {
            int healAmount = item.get_flat_healing_amount();
            if (healAmount <= 0 && item.get_healing_dice_count() > 0) {
                healAmount = Dice_Util.roll_dice(item.get_healing_dice_count(), item.get_healing_die_size())
                        + item.get_healing_bonus();
            }
            int hpBefore = character.current_hp;
            character.set_current_hp(character.current_hp + healAmount);
            character.remove_item_from_inventory(item.key);
            character.record_advancement("使用物品：" + item.display_name + "，恢复生命值 "
                    + (character.current_hp - hpBefore));
            return;
        }

        if (item.is_coin_item()) {
            int currencyGain = item.get_currency_gain_cp();
            character.add_currency_cp(currencyGain);
            character.remove_item_from_inventory(item.key);
            character.record_advancement("使用物品：" + item.display_name + "，获得 "
                    + Equipment_Item.format_cp_value(currencyGain));
            return;
        }

        throw new IllegalArgumentException("This item has no automatic use effect");
    }

    public static void rest(Character_Sheet character, String restType) {
        rest(character, restType, 0);
    }

    public static void rest(Character_Sheet character, String restType, int spentHitDice) {
        if (character == null) {
            throw new IllegalArgumentException("Character is required");
        }
        if (restType == null) {
            throw new IllegalArgumentException("Rest type is required");
        }

        switch (restType) {
            case "LONG" -> character.take_long_rest();
            case "SHORT" -> takeShortRest(character, spentHitDice);
            case "SECOND_WIND" -> useSecondWind(character);
            default -> throw new IllegalArgumentException("Unsupported rest type");
        }
    }

    private static void takeShortRest(Character_Sheet character, int spentHitDice) {
        if (spentHitDice < 0) {
            throw new IllegalArgumentException("Spent hit dice cannot be negative");
        }
        if (spentHitDice > character.available_hit_dice) {
            throw new IllegalArgumentException("Not enough hit dice available");
        }

        int constitutionModifier = character.stats.get_mod(character.stats.con);
        int recoveredHp = 0;
        for (int i = 0; i < spentHitDice; i++) {
            recoveredHp += Math.max(0, Dice_Util.roll_dice(1, character.job.hp_dice) + constitutionModifier);
        }
        character.take_short_rest(spentHitDice, recoveredHp);
    }

    public static void addXp(Character_Sheet character, int amount, String reason) {
        if (character == null) {
            throw new IllegalArgumentException("Character is required");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("XP amount must be greater than zero");
        }
        character.experience_points = Math.addExact(character.experience_points, amount);
        character.record_advancement("获得经验值 " + amount
                + (reason != null && !reason.isEmpty() ? " (" + reason + ")" : ""));
    }

    private static void useSecondWind(Character_Sheet character) {
        if (!(character.job instanceof Fighter_Class fighter)) {
            throw new IllegalArgumentException("Second Wind is only available to fighters");
        }
        if (fighter.current_second_wind_uses <= 0) {
            throw new IllegalArgumentException("No Second Wind uses remaining");
        }
        fighter.current_second_wind_uses--;
        int heal = Dice_Util.roll_dice(1, 10) + fighter.current_level;
        int hpBefore = character.current_hp;
        character.set_current_hp(character.current_hp + heal);
        character.record_advancement("使用复苏之风，恢复了 " + (character.current_hp - hpBefore) + " 点生命值。");
    }

    private static void validateQuantity(Equipment_Item item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (!item.is_stackable() && quantity != 1) {
            throw new IllegalArgumentException("Non-stackable items must use quantity 1");
        }
    }

    private static void requireCharacterAndItem(Character_Sheet character, Equipment_Item item) {
        if (character == null) {
            throw new IllegalArgumentException("Character is required");
        }
        if (item == null) {
            throw new IllegalArgumentException("Item is required");
        }
    }
}
