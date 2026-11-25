package com.DMHelper.basic.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpellLibraryTest {
    @Test
    void bardHasSelectableSpellsAtEverySpellLevel() {
        List<String> bardSpells = Spell_Library.get_bard_spell_keys_up_to_level(9);

        for (int spellLevel = 1; spellLevel <= 9; spellLevel++) {
            int expectedLevel = spellLevel;
            assertFalse(
                    bardSpells.stream()
                            .map(Spell_Library::get_spell)
                            .filter(spell -> spell != null)
                            .noneMatch(spell -> spell.level == expectedLevel),
                    "吟游诗人缺少 " + spellLevel + " 环法术"
            );
        }
    }

    @Test
    void everyPublicBardSpellKeyResolvesToDefinition() {
        for (String key : Spell_Library.get_bard_cantrip_keys()) {
            assertNotNull(Spell_Library.get_spell(key), "未定义的吟游诗人戏法：" + key);
        }
        for (String key : Spell_Library.get_bard_spell_keys_up_to_level(9)) {
            assertNotNull(Spell_Library.get_spell(key), "未定义的吟游诗人法术：" + key);
        }
    }
}
