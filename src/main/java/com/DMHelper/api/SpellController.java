package com.DMHelper.api;

import com.DMHelper.basic.spell.Spell_Library;
import com.DMHelper.basic.spell.Spell_Definition;
import io.javalin.http.Context;
import java.util.List;
import java.util.stream.Collectors;

public class SpellController {

    public static void getAvailableSpells(Context ctx) {
        String job = ctx.queryParam("job");
        int level = ctx.queryParam("level") != null ? Integer.parseInt(ctx.queryParam("level")) : 0;
        boolean cantrip = "true".equals(ctx.queryParam("cantrip"));

        List<String> keys;
        if (job != null) {
            job = job.toLowerCase();
            if (job.contains("wizard") || job.contains("法师")) {
                keys = cantrip ? Spell_Library.get_wizard_cantrip_keys() : Spell_Library.get_wizard_spell_keys_up_to_level(level);
            } else if (job.contains("sorcerer") || job.contains("术士")) {
                keys = cantrip ? Spell_Library.get_sorcerer_cantrip_keys() : Spell_Library.get_sorcerer_spell_keys_up_to_level(level);
            } else if (job.contains("warlock") || job.contains("邪术士")) {
                keys = cantrip ? Spell_Library.get_warlock_cantrip_keys() : Spell_Library.get_warlock_spell_keys_up_to_level(level);
            } else if (job.contains("paladin") || job.contains("圣武士")) {
                keys = cantrip ? List.of() : Spell_Library.get_paladin_spell_keys_up_to_level(level);
            } else if (job.contains("bard") || job.contains("吟游诗人")) {
                keys = cantrip ? Spell_Library.get_bard_cantrip_keys() : Spell_Library.get_bard_spell_keys_up_to_level(level);
            } else {
                keys = cantrip ? Spell_Library.get_wizard_cantrip_keys() : Spell_Library.get_all_spell_keys_up_to_level(level);
            }
        } else {
            keys = cantrip ? Spell_Library.get_wizard_cantrip_keys() : Spell_Library.get_all_spell_keys_up_to_level(level);
        }

        List<Spell_Definition> spells = keys.stream().map(Spell_Library::get_spell).collect(Collectors.toList());
        ctx.result(ApiServer.GSON.toJson(spells)).contentType("application/json");
    }
}
