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
        com.google.gson.JsonArray characters = new com.google.gson.JsonArray();
        for (Character_Sheet character : Global_Data.character_pool) {
            characters.add(ApiServer.GSON.fromJson(
                    serializeCharacterForFrontend(character),
                    com.google.gson.JsonObject.class
            ));
        }
        ctx.contentType("application/json").result(ApiServer.GSON.toJson(characters));
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
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }

            EquipRequest req = ApiServer.GSON.fromJson(ctx.body(), EquipRequest.class);
            if (req == null || req.slot == null) {
                ctx.status(400).result("Equipment slot is required");
                return;
            }
            com.DMHelper.basic.equipment.Equipment_Slot slot = com.DMHelper.basic.equipment.Equipment_Slot.valueOf(req.slot);
            if (slot == com.DMHelper.basic.equipment.Equipment_Slot.BACKPACK) {
                ctx.status(400).result("Backpack is not an equipment slot");
                return;
            }

            String itemKey = req.itemKey == null ? "" : req.itemKey.trim();
            if (!itemKey.isEmpty()) {
                com.DMHelper.basic.equipment.Equipment_Item item = com.DMHelper.basic.equipment.Equipment_Library.get_item(itemKey);
                if (item == null) {
                    ctx.status(404).result("Item not found");
                    return;
                }
                if (item.slot != slot) {
                    ctx.status(400).result("Item does not fit the selected slot");
                    return;
                }
                if (character.get_item_count(itemKey) <= 0) {
                    ctx.status(400).result("Item is not in inventory");
                    return;
                }
            }

            character.equip_item(slot, itemKey);
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid character ID");
        } catch (IllegalArgumentException e) {
            ctx.status(400).result("Invalid equipment slot");
        }
    }

    public static class InventoryRequest {
        public String action; // "ADD", "USE", "SELL", "BUY"
        public String itemKey;
        public int quantity;
    }

    public static void manageInventory(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }

            InventoryRequest req = ApiServer.GSON.fromJson(ctx.body(), InventoryRequest.class);
            if (req == null || req.action == null || req.itemKey == null || req.itemKey.trim().isEmpty()) {
                ctx.status(400).result("Action and itemKey are required");
                return;
            }
            if (!java.util.Set.of("ADD", "USE", "SELL", "BUY").contains(req.action)) {
                ctx.status(400).result("Unsupported inventory action");
                return;
            }
            if (!"USE".equals(req.action) && req.quantity <= 0) {
                ctx.status(400).result("Quantity must be greater than zero");
                return;
            }

            com.DMHelper.basic.equipment.Equipment_Item item = com.DMHelper.basic.equipment.Equipment_Library.get_item(req.itemKey);
            if (item == null) {
                item = character.get_owned_items_for_slot(com.DMHelper.basic.equipment.Equipment_Slot.BACKPACK)
                    .stream().filter(i -> i.key.equals(req.itemKey)).findFirst().orElse(null);
            }

            if (item == null) {
                ctx.status(404).result("Item not found");
                return;
            }
            if ("ADD".equals(req.action)) {
                CharacterActionService.addItem(character, item, req.quantity);
            } else if ("SELL".equals(req.action)) {
                CharacterActionService.sellItem(character, item, req.quantity);
            } else if ("BUY".equals(req.action)) {
                CharacterActionService.buyItem(character, item, req.quantity);
            } else if ("USE".equals(req.action)) {
                CharacterActionService.useItem(character, item);
            }

            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid character ID");
        } catch (ArithmeticException e) {
            ctx.status(400).result("Quantity is too large");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    public static class RestRequest {
        public String restType; // "SHORT" or "LONG" or "SECOND_WIND"
        public int spentHitDice;
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
                    itemObj.addProperty("auto_usable", item.is_healing_item() || item.is_coin_item());
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
        json.add("pending_advancement_choices", ApiServer.GSON.toJsonTree(character.job.get_pending_choices()));
        appendFighterAdvancementOptions(json, character);
        json.add("advancement_options", AdvancementOptionsBuilder.build(character));
        
        // Derived stats that frontend needs
        json.addProperty("max_hp", character.hp);
        int pb = character.get_proficiency_bonus();
        json.addProperty("proficiency_bonus", pb);
        json.addProperty("initiative_modifier", character.get_initiative_modifier());
        
        // Spell stats — compute based on class spellcasting ability
        String className = character.job.class_name;
        int spellAbilityMod = 0;
        String spellAbility = "—";
        if (className.contains("法师") || className.contains("Wizard")) {
            spellAbilityMod = character.stats.get_mod(character.stats.intel);
            spellAbility = "智力 (INT)";
        } else if (className.contains("术士") || className.contains("Sorcerer") || 
                   className.contains("吟游诗人") || className.contains("Bard") ||
                   className.contains("邪术士") || className.contains("Warlock")) {
            spellAbilityMod = character.stats.get_mod(character.stats.cha);
            spellAbility = "魅力 (CHA)";
        } else if (className.contains("圣武士") || className.contains("Paladin")) {
            spellAbilityMod = character.stats.get_mod(character.stats.cha);
            spellAbility = "魅力 (CHA)";
        }
        json.addProperty("spell_attack_bonus", spellAbilityMod + pb);
        json.addProperty("spell_save_dc", 8 + spellAbilityMod + pb);
        json.addProperty("spellcasting_ability_name", spellAbility);
        json.addProperty("max_cantrips", SpellManagementService.getCantripLimit(character));
        json.addProperty("max_known_spells", SpellManagementService.getKnownSpellLimit(character));
        json.addProperty("max_prepared_spells", SpellManagementService.getPreparedSpellLimit(character));
        json.addProperty("max_spell_level", SpellManagementService.getMaxSpellLevel(character));
        
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
            java.lang.reflect.Field spellbookField = character.job.getClass().getField("spellbook_spell_keys");
            java.util.List<String> spellbook = (java.util.List<String>) spellbookField.get(character.job);
            if (spellbook != null) allSpellKeys.addAll(spellbook);
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

    private static void appendFighterAdvancementOptions(com.google.gson.JsonObject json, Character_Sheet character) {
        if (!(character.job instanceof Fighter_Class fighter)) {
            return;
        }

        com.google.gson.JsonObject options = new com.google.gson.JsonObject();
        options.addProperty("needs_fighting_style",
                fighter.fighting_style_name == null || fighter.fighting_style_name.trim().isEmpty());
        options.addProperty("needs_subclass",
                fighter.current_level >= 3
                        && fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.NONE);
        options.addProperty("needs_extra_fighting_style",
                fighter.current_level >= 10
                        && fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.CHAMPION
                        && (fighter.extra_fighting_style_name == null || fighter.extra_fighting_style_name.trim().isEmpty()));
        options.addProperty("pending_maneuver_count",
                fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.BATTLE_MASTER
                        ? Math.max(0, fighter.get_expected_maneuver_count() - fighter.maneuver_names.size())
                        : 0);
        options.addProperty("pending_asi_count", fighter.pending_asi_count);

        com.google.gson.JsonArray styles = new com.google.gson.JsonArray();
        for (String key : fighter.get_available_fighting_styles(false)) {
            styles.add(optionJson(key, fighter.get_fighting_style_label(key), fighter.get_fighting_style_description(key)));
        }
        options.add("fighting_styles", styles);

        com.google.gson.JsonArray extraStyles = new com.google.gson.JsonArray();
        for (String key : fighter.get_available_fighting_styles(true)) {
            extraStyles.add(optionJson(key, fighter.get_fighting_style_label(key), fighter.get_fighting_style_description(key)));
        }
        options.add("extra_fighting_styles", extraStyles);

        com.google.gson.JsonArray subclasses = new com.google.gson.JsonArray();
        subclasses.add(optionJson("CHAMPION", "冠军勇士 (Champion)", "稳定强化武器重击、运动能力与生存能力。"));
        subclasses.add(optionJson("BATTLE_MASTER", "战斗大师 (Battle Master)", "使用卓越骰和战技控制战场。"));
        options.add("subclasses", subclasses);

        com.google.gson.JsonArray maneuvers = new com.google.gson.JsonArray();
        for (String key : fighter.get_available_maneuvers()) {
            maneuvers.add(optionJson(key, fighter.get_maneuver_label(key), fighter.get_maneuver_description(key)));
        }
        options.add("maneuvers", maneuvers);

        com.google.gson.JsonArray feats = new com.google.gson.JsonArray();
        for (String key : com.DMHelper.basic.feat.Feat_Library.get_available_feat_keys(character)) {
            feats.add(optionJson(
                    key,
                    com.DMHelper.basic.feat.Feat_Library.get_label(key),
                    com.DMHelper.basic.feat.Feat_Library.get_summary_line(key)
            ));
        }
        options.add("feats", feats);
        json.add("fighter_advancement", options);
    }

    private static com.google.gson.JsonObject optionJson(String key, String label, String description) {
        com.google.gson.JsonObject option = new com.google.gson.JsonObject();
        option.addProperty("key", key);
        option.addProperty("label", label);
        option.addProperty("description", description);
        return option;
    }

    public static void rest(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }

            RestRequest req = ApiServer.GSON.fromJson(ctx.body(), RestRequest.class);
            if (req == null || req.restType == null) {
                ctx.status(400).result("Rest type is required");
                return;
            }
            CharacterActionService.rest(character, req.restType, req.spentHitDice);
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid character ID");
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    public static class XpRequest {
        public int amount;
        public String reason;
    }

    public static void addXp(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }

            XpRequest req = ApiServer.GSON.fromJson(ctx.body(), XpRequest.class);
            if (req == null) {
                ctx.status(400).result("XP amount is required");
                return;
            }
            CharacterActionService.addXp(character, req.amount, req.reason);
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid character ID");
        } catch (ArithmeticException e) {
            ctx.status(400).result("XP amount is too large");
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    public static class SpellManageRequest {
        public String action; // "ADD_CANTRIP", "REMOVE_CANTRIP", "ADD_SPELL", "REMOVE_SPELL", "PREPARE", "UNPREPARE"
        public String spellKey;
    }

    public static void manageSpells(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Character_Sheet character = Global_Data.character_pool.stream().filter(c -> c.database_id == id).findFirst().orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }
            SpellManageRequest req = ApiServer.GSON.fromJson(ctx.body(), SpellManageRequest.class);
            SpellManagementService.applyAction(character, req == null ? null : req.action, req == null ? null : req.spellKey);
            Character_DAO.update_character(character);
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid character ID");
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
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
                if (!Character_DAO.update_character(character)) {
                    ctx.status(500).result("Failed to persist level-up");
                    return;
                }
                ctx.status(200).result(serializeCharacterForFrontend(character));
            } else {
                ctx.status(400).result("Not enough XP to level up");
            }
        } else {
            ctx.status(404).result("Character not found");
        }
    }

    public static class AdvancementChoiceRequest {
        public String type;
        public String value;
        public java.util.List<String> values;
    }

    public static void applyAdvancementChoice(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Character_Sheet character = Global_Data.character_pool.stream()
                    .filter(item -> item.database_id == id)
                    .findFirst()
                    .orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }

            AdvancementChoiceRequest request = ApiServer.GSON.fromJson(ctx.body(), AdvancementChoiceRequest.class);
            ClassAdvancementService.apply(
                    character,
                    request == null ? null : request.type,
                    request == null ? null : request.value,
                    request == null ? null : request.values
            );
            if (!Character_DAO.update_character(character)) {
                ctx.status(500).result("Failed to persist advancement choice");
                return;
            }
            ctx.status(200).result(serializeCharacterForFrontend(character));
        } catch (NumberFormatException error) {
            ctx.status(400).result("Invalid character ID");
        } catch (IllegalArgumentException error) {
            ctx.status(400).result(error.getMessage());
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

            if (!Character_DAO.save_character(newCharacter)) {
                ctx.status(500).result("Failed to persist character");
                return;
            }
            Global_Data.character_pool.add(newCharacter);

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
