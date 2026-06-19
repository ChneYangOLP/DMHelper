package com.DMHelper.api;

import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.race.*;
import io.javalin.http.Context;

public class CharacterController {
    
    public static class StatDto {
        public int str, dex, con, intel, wis, cha;
    }

    public static class CharacterCreateRequest {
        public String name;
        public int age;
        public String gender;
        public String race;
        public String subrace;
        public String halfElfStat1;
        public String halfElfStat2;
        public String job;
        public StatDto stats;
        
        public String background_story;
        public String personality_traits;
        public String ideals;
        public String bonds;
        public String flaws;

        public java.util.List<String> skills;
        public String fightingStyle;
        public String sorcererOrigin;
        public String dragonAncestry;
        public String warlockPatron;
    }
    
    public static void getAllCharacters(Context ctx) {
        ctx.json(Global_Data.character_pool);
    }
    
    public static void getCharacterById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            var character = Global_Data.character_pool.stream()
                    .filter(c -> c.database_id == id)
                    .findFirst()
                    .orElse(null);
            
            if (character != null) {
                ctx.result(serializeCharacterForFrontend(character)).contentType("application/json");
            } else {
                ctx.status(404).result("Character not found");
            }
        } catch (Exception e) {
            ctx.status(400).result("Invalid ID");
        }
    }

    public static class UpdateRequest {
        public String background_story;
        public String personality_traits;
        public String ideals;
        public String bonds;
        public String flaws;
    }

    public static void updateCharacter(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            UpdateRequest req = ApiServer.GSON.fromJson(ctx.body(), UpdateRequest.class);
            if (req.background_story != null) character.background_story = req.background_story;
            if (req.personality_traits != null) character.personality_traits = req.personality_traits;
            if (req.ideals != null) character.ideals = req.ideals;
            if (req.bonds != null) character.bonds = req.bonds;
            if (req.flaws != null) character.flaws = req.flaws;
            
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static class EquipRequest {
        public String slot; // e.g. "ARMOR", "MAIN_HAND", "OFF_HAND", "CLOAK", "ACCESSORY"
        public String itemKey; // e.g. "" for empty
    }

    public static void equipItem(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            EquipRequest req = ApiServer.GSON.fromJson(ctx.body(), EquipRequest.class);
            com.DMHelper.basic.equipment.Equipment_Slot slot = com.DMHelper.basic.equipment.Equipment_Slot.valueOf(req.slot);
            character.equip_item(slot, req.itemKey);
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static class InventoryRequest {
        public String action; // "ADD", "USE", "SELL", "BUY"
        public String itemKey;
        public int quantity;
    }

    public static void manageInventory(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            InventoryRequest req = ApiServer.GSON.fromJson(ctx.body(), InventoryRequest.class);
            com.DMHelper.basic.equipment.Equipment_Item item = com.DMHelper.basic.equipment.Equipment_Library.get_item(req.itemKey);
            if (item == null && !req.itemKey.isEmpty()) {
                item = character.get_owned_items_for_slot(com.DMHelper.basic.equipment.Equipment_Slot.BACKPACK)
                    .stream().filter(i -> i.key.equals(req.itemKey)).findFirst().orElse(null);
            }

            if ("ADD".equals(req.action)) {
                for (int i = 0; i < req.quantity; i++) character.add_item_to_inventory(req.itemKey);
                character.record_advancement("获得物品：" + (item != null ? item.display_name : req.itemKey) + " x" + req.quantity);
            } else if ("SELL".equals(req.action) && item != null) {
                int totalGain = item.get_sale_value_cp() * req.quantity;
                for (int i = 0; i < req.quantity; i++) {
                    if (!character.remove_item_from_inventory(req.itemKey)) break;
                }
                character.add_currency_cp(totalGain);
                character.record_advancement("出售物品：" + item.display_name + " x" + req.quantity + "，获得 " + com.DMHelper.basic.equipment.Equipment_Item.format_cp_value(totalGain));
            } else if ("BUY".equals(req.action) && item != null) {
                int totalCost = item.value_in_cp * req.quantity;
                if (character.spend_currency_cp(totalCost)) {
                    for (int i = 0; i < req.quantity; i++) character.add_item_to_inventory(req.itemKey);
                    character.record_advancement("购买物品：" + item.display_name + " x" + req.quantity + "，花费 " + com.DMHelper.basic.equipment.Equipment_Item.format_cp_value(totalCost));
                } else {
                    ctx.status(400).result("Not enough currency");
                    return;
                }
            } else if ("USE".equals(req.action) && item != null) {
                if (item.is_healing_item()) {
                    int healAmount = item.get_flat_healing_amount();
                    if (healAmount <= 0 && item.get_healing_dice_count() > 0) {
                        healAmount = com.DMHelper.basic.combat.Dice_Util.roll_dice(item.get_healing_dice_count(), item.get_healing_die_size()) + item.get_healing_bonus();
                    }
                    character.set_current_hp(character.current_hp + healAmount);
                    character.remove_item_from_inventory(item.key);
                    character.record_advancement("使用物品：" + item.display_name + "，恢复生命值 " + healAmount);
                }
            }

            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static class RestRequest {
        public String restType; // "SHORT" or "LONG" or "SECOND_WIND"
    }

    private static String serializeCharacterForFrontend(Character_Sheet character) {
        com.google.gson.JsonObject json = ApiServer.GSON.toJsonTree(character).getAsJsonObject();
        
        // Inventory items with details
        com.google.gson.JsonArray inventoryArr = new com.google.gson.JsonArray();
        if (character.owned_equipment_keys != null) {
            for (String key : character.owned_equipment_keys) {
                com.DMHelper.basic.equipment.Equipment_Item item = com.DMHelper.basic.equipment.Equipment_Library.get_item(key);
                if (item != null) {
                    com.google.gson.JsonObject itemObj = ApiServer.GSON.toJsonTree(item).getAsJsonObject();
                    itemObj.addProperty("count", character.inventory_item_counts.getOrDefault(key, 1));
                    inventoryArr.add(itemObj);
                }
            }
        }
        json.add("inventory", inventoryArr);
        
        // Level up info
        json.addProperty("can_level_up", character.can_level_up());
        json.addProperty("next_level_xp", com.DMHelper.basic.playerclass.Dnd5e_Progression.get_next_level_xp(character.job.current_level));
        json.addProperty("currency_gp", character.gold_pieces);
        json.addProperty("currency_sp", character.silver_pieces);
        json.addProperty("currency_cp", character.copper_pieces);
        
        // Derived stats that frontend needs
        json.addProperty("max_hp", character.hp);
        int pb = character.get_proficiency_bonus();
        json.addProperty("proficiency_bonus", pb);
        
        // Spell stats — compute based on class spellcasting ability
        String className = character.job.class_name;
        int spellAbilityMod = 0;
        String spellAbility = "—";
        if (className.contains("法师") || className.contains("Wizard")) {
            spellAbilityMod = (character.stats.intel - 10) / 2;
            spellAbility = "智力 (INT)";
        } else if (className.contains("术士") || className.contains("Sorcerer") || 
                   className.contains("吟游诗人") || className.contains("Bard") ||
                   className.contains("邪术士") || className.contains("Warlock")) {
            spellAbilityMod = (character.stats.cha - 10) / 2;
            spellAbility = "魅力 (CHA)";
        } else if (className.contains("圣武士") || className.contains("Paladin")) {
            spellAbilityMod = (character.stats.cha - 10) / 2;
            spellAbility = "魅力 (CHA)";
        }
        json.addProperty("spell_attack_bonus", spellAbilityMod + pb);
        json.addProperty("spell_save_dc", 8 + spellAbilityMod + pb);
        json.addProperty("spellcasting_ability_name", spellAbility);
        
        // Spell display name map: key -> display_name
        com.google.gson.JsonObject spellMap = new com.google.gson.JsonObject();
        java.util.Set<String> allSpellKeys = new java.util.HashSet<>();
        try {
            java.lang.reflect.Field cantripField = character.job.getClass().getField("known_cantrip_keys");
            java.util.List<String> cantrips = (java.util.List<String>) cantripField.get(character.job);
            if (cantrips != null) allSpellKeys.addAll(cantrips);
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Field spellField = character.job.getClass().getField("known_spell_keys");
            java.util.List<String> spells = (java.util.List<String>) spellField.get(character.job);
            if (spells != null) allSpellKeys.addAll(spells);
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Field prepField = character.job.getClass().getField("prepared_spell_keys");
            java.util.List<String> prepared = (java.util.List<String>) prepField.get(character.job);
            if (prepared != null) allSpellKeys.addAll(prepared);
        } catch (Exception ignored) {}
        for (String key : allSpellKeys) {
            com.DMHelper.basic.spell.Spell_Definition def = com.DMHelper.basic.spell.Spell_Library.get_spell(key);
            if (def != null) {
                spellMap.addProperty(key, def.display_name);
            }
        }
        json.add("spell_map", spellMap);
        
        // Advancement history (rename from advancement_notes to match frontend)
        if (character.advancement_notes != null) {
            json.add("advancement_history", ApiServer.GSON.toJsonTree(character.advancement_notes));
        }
        
        return ApiServer.GSON.toJson(json);
    }

    public static void rest(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            RestRequest req = ApiServer.GSON.fromJson(ctx.body(), RestRequest.class);
            if ("LONG".equals(req.restType)) {
                character.set_current_hp(character.hp);
                character.job.restore_long_rest_resources();
                character.record_advancement("完成了一次长休，恢复所有生命值与能力。");
            } else if ("SHORT".equals(req.restType)) {
                character.job.restore_short_rest_resources();
                character.record_advancement("完成了一次短休。");
            } else if ("SECOND_WIND".equals(req.restType) && character.job instanceof com.DMHelper.basic.playerclass.Fighter.Fighter_Class) {
                com.DMHelper.basic.playerclass.Fighter.Fighter_Class fighter = (com.DMHelper.basic.playerclass.Fighter.Fighter_Class) character.job;
                if (fighter.current_second_wind_uses > 0) {
                    fighter.current_second_wind_uses--;
                    int heal = com.DMHelper.basic.combat.Dice_Util.roll_dice(1, 10) + fighter.current_level;
                    character.set_current_hp(character.current_hp + heal);
                    character.record_advancement("使用复苏之风，恢复了 " + heal + " 点生命值。");
                }
            }
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static class XpRequest {
        public int amount;
        public String reason;
    }

    public static void addXp(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            XpRequest req = ApiServer.GSON.fromJson(ctx.body(), XpRequest.class);
            character.experience_points += req.amount;
            character.record_advancement("获得经验值 " + req.amount + (req.reason != null && !req.reason.isEmpty() ? " (" + req.reason + ")" : ""));
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static class SpellManageRequest {
        public String action; // "ADD_CANTRIP", "REMOVE_CANTRIP", "ADD_SPELL", "REMOVE_SPELL", "PREPARE", "UNPREPARE"
        public String spellKey;
    }

    private static void modifySpellList(Character_Sheet character, String fieldName, String spellKey, boolean add) {
        try {
            java.lang.reflect.Field field = character.job.getClass().getField(fieldName);
            java.util.List<String> keys = (java.util.List<String>) field.get(character.job);
            if (keys == null && add) {
                keys = new java.util.ArrayList<>();
                field.set(character.job, keys);
            }
            if (keys != null) {
                if (add && !keys.contains(spellKey)) keys.add(spellKey);
                else if (!add) keys.remove(spellKey);
            }
        } catch (Exception e) {}
    }

    public static void manageSpells(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            SpellManageRequest req = ApiServer.GSON.fromJson(ctx.body(), SpellManageRequest.class);
            if (character.job != null) {
                if ("ADD_CANTRIP".equals(req.action)) modifySpellList(character, "known_cantrip_keys", req.spellKey, true);
                else if ("REMOVE_CANTRIP".equals(req.action)) modifySpellList(character, "known_cantrip_keys", req.spellKey, false);
                else if ("ADD_SPELL".equals(req.action)) modifySpellList(character, "known_spell_keys", req.spellKey, true);
                else if ("REMOVE_SPELL".equals(req.action)) {
                    modifySpellList(character, "known_spell_keys", req.spellKey, false);
                    modifySpellList(character, "prepared_spell_keys", req.spellKey, false);
                } else if ("PREPARE".equals(req.action)) modifySpellList(character, "prepared_spell_keys", req.spellKey, true);
                else if ("UNPREPARE".equals(req.action)) modifySpellList(character, "prepared_spell_keys", req.spellKey, false);
            }
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static void levelUp(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
        if (character != null) {
            if (character.can_level_up()) {
                character.job.level_up(character.job.current_level + 1);
                character.recalculate_derived_stats();
                character.record_advancement("升级到了 " + character.job.current_level + " 级！");
                Character_DAO.update_character(character);
                ctx.status(200).result(serializeCharacterForFrontend(character));
            } else {
                ctx.status(400).result("Not enough XP to level up");
            }
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static void createCharacter(Context ctx) {
        try {
            CharacterCreateRequest req = ApiServer.GSON.fromJson(ctx.body(), CharacterCreateRequest.class);
            if (req.name == null || req.name.trim().isEmpty()) {
                ctx.status(400).result("Name is required");
                return;
            }

            Character_Race race = resolveRace(req.race, req.subrace, req.halfElfStat1, req.halfElfStat2);
            Character_Class job = resolveClass(req.job);
            Stats rawStats = new Stats(req.stats.str, req.stats.dex, req.stats.con, req.stats.intel, req.stats.wis, req.stats.cha);

            Character_Sheet newCharacter = Character_Sheet.create_new_character(
                    req.name, req.age, req.gender, race, job, rawStats
            );

            newCharacter.background_story = req.background_story != null ? req.background_story : "";
            newCharacter.personality_traits = req.personality_traits != null ? req.personality_traits : "";
            newCharacter.ideals = req.ideals != null ? req.ideals : "";
            newCharacter.bonds = req.bonds != null ? req.bonds : "";
            newCharacter.flaws = req.flaws != null ? req.flaws : "";

            // Apply selected skills
            if (req.skills != null && !req.skills.isEmpty()) {
                newCharacter.job.skill_proficiencies.addAll(req.skills);
            }

            // Apply level 1 class features
            if (newCharacter.job instanceof Fighter_Class && req.fightingStyle != null) {
                ((Fighter_Class) newCharacter.job).fighting_style_name = req.fightingStyle;
            } else if (newCharacter.job instanceof Sorcerer_Class && req.sorcererOrigin != null) {
                Sorcerer_Class sorc = (Sorcerer_Class) newCharacter.job;
                if (req.sorcererOrigin.contains("龙脉")) {
                    sorc.sorcerous_origin = com.DMHelper.basic.playerclass.sorcerer.Sorcerous_Origin.DRACONIC_BLOODLINE;
                    sorc.dragon_ancestry = req.dragonAncestry;
                } else {
                    sorc.sorcerous_origin = com.DMHelper.basic.playerclass.sorcerer.Sorcerous_Origin.WILD_MAGIC;
                }
            } else if (newCharacter.job instanceof Warlock_Class && req.warlockPatron != null) {
                Warlock_Class warlock = (Warlock_Class) newCharacter.job;
                if (req.warlockPatron.contains("邪魔")) warlock.patron = com.DMHelper.basic.playerclass.warlock.Warlock_Patron.FIEND;
                else if (req.warlockPatron.contains("妖精")) warlock.patron = com.DMHelper.basic.playerclass.warlock.Warlock_Patron.ARCHFEY;
                else warlock.patron = com.DMHelper.basic.playerclass.warlock.Warlock_Patron.GREAT_OLD_ONE;
            }

            // Ensure derived stats are correct
            newCharacter.recalculate_derived_stats();

            Global_Data.character_pool.add(newCharacter);
            Character_DAO.save_character(newCharacter);

            ctx.result(ApiServer.GSON.toJson(newCharacter)).contentType("application/json");
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Failed to create character: " + e.getMessage());
        }
    }

    private static Character_Race resolveRace(String race, String subrace, String s1, String s2) {
        if (race == null) return new Human_Race();
        switch (race) {
            case "精灵 (Elf)": return new Elf_Race(subrace);
            case "矮人 (Dwarf)": return new Dwarf_Race(subrace);
            case "半身人 (Halfling)": return new Halfling_Race(subrace);
            case "龙裔 (Dragonborn)": return new Dragonborn_Race(subrace);
            case "侏儒 (Gnome)": return new Gnome_Race(subrace);
            case "半精灵 (Half-Elf)": return new Half_Elf_Race(s1, s2);
            case "半兽人 (Half-Orc)": return new Half_Orc_Race();
            case "提夫林 (Tiefling)": return new Tiefling_Race();
            default: return new Human_Race();
        }
    }

    private static Character_Class resolveClass(String job) {
        if (job == null) return new Fighter_Class();
        switch (job) {
            case "战士 (Fighter)": return new Fighter_Class();
            case "法师 (Wizard)": return new Wizard_Class();
            case "术士 (Sorcerer)": return new Sorcerer_Class();
            case "邪术士 (Warlock)": return new Warlock_Class();
            case "圣武士 (Paladin)": return new Paladin_Class();
            case "吟游诗人 (Bard)": return new Bard_Class();
            default: return new Fighter_Class();
        }
    }
}
