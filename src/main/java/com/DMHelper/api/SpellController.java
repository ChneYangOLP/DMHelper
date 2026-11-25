package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.spell.Spell_Definition;
import java.util.List;
import java.util.stream.Collectors;

public class SpellController {

    public static void getAvailableSpells(ApiExchange ctx) {
        try {
            int characterId = Integer.parseInt(ctx.queryParam("characterId"));
            boolean cantrip = "true".equals(ctx.queryParam("cantrip"));
            Character_Sheet character = Global_Data.character_pool.stream()
                    .filter(candidate -> candidate.database_id == characterId)
                    .findFirst()
                    .orElse(null);
            if (character == null) {
                ctx.status(404).result("Character not found");
                return;
            }

            List<String> keys = SpellManagementService.getAvailableSpellKeys(character, cantrip);
            List<Spell_Definition> spells = keys.stream()
                    .map(com.DMHelper.basic.spell.Spell_Library::get_spell)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            ctx.result(ApiServer.GSON.toJson(spells)).contentType("application/json");
        } catch (NumberFormatException e) {
            ctx.status(400).result("Valid characterId is required");
        } catch (IllegalArgumentException e) {
            ctx.status(400).result(e.getMessage());
        }
    }
}
