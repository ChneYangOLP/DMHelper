package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;

import java.util.ArrayList;
import java.util.List;

/** Centralizes spell eligibility and selection limits for API operations. */
public final class SpellManagementService {
    private SpellManagementService() {
    }

    public static List<String> getAvailableSpellKeys(Character_Sheet character, boolean cantrip) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) {
            return cantrip
                    ? Spell_Library.get_wizard_cantrip_keys()
                    : Spell_Library.get_wizard_spell_keys_up_to_level(wizard.get_max_spell_level());
        }
        if (job instanceof Sorcerer_Class sorcerer) {
            return cantrip
                    ? Spell_Library.get_sorcerer_cantrip_keys()
                    : Spell_Library.get_sorcerer_spell_keys_up_to_level(sorcerer.get_max_spell_level());
        }
        if (job instanceof Warlock_Class warlock) {
            return cantrip
                    ? Spell_Library.get_warlock_cantrip_keys()
                    : Spell_Library.get_warlock_spell_keys_up_to_level(warlock.pact_slot_level);
        }
        if (job instanceof Paladin_Class paladin) {
            return cantrip ? List.of() : paladin.get_available_spell_options();
        }
        if (job instanceof Bard_Class bard) {
            return cantrip
                    ? Spell_Library.get_bard_cantrip_keys()
                    : Spell_Library.get_bard_spell_keys_up_to_level(bard.get_max_spell_level());
        }
        return List.of();
    }

    public static void applyAction(Character_Sheet character, String action, String spellKey) {
        if (action == null || spellKey == null || spellKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Action and spellKey are required");
        }

        Spell_Definition spell = Spell_Library.get_spell(spellKey);
        if (spell == null) {
            throw new IllegalArgumentException("Unknown spell");
        }

        switch (action) {
            case "ADD_CANTRIP" -> addKnown(character, spellKey, true);
            case "REMOVE_CANTRIP" -> removeKnown(character, spellKey, true);
            case "ADD_SPELL" -> addKnown(character, spellKey, false);
            case "REMOVE_SPELL" -> removeKnown(character, spellKey, false);
            case "PREPARE" -> prepare(character, spellKey);
            case "UNPREPARE" -> unprepare(character, spellKey);
            default -> throw new IllegalArgumentException("Unsupported spell action");
        }
    }

    public static int getCantripLimit(Character_Sheet character) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) return wizard.cantrips_known;
        if (job instanceof Sorcerer_Class sorcerer) return sorcerer.cantrips_known;
        if (job instanceof Warlock_Class warlock) return warlock.cantrips_known;
        if (job instanceof Bard_Class bard) return bard.cantrips_known;
        return 0;
    }

    public static int getKnownSpellLimit(Character_Sheet character) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) return wizard.spells_in_spellbook;
        if (job instanceof Sorcerer_Class sorcerer) return sorcerer.spells_known_count;
        if (job instanceof Warlock_Class warlock) return warlock.spells_known_count;
        if (job instanceof Bard_Class bard) return bard.base_spells_known_count;
        return 0;
    }

    public static int getPreparedSpellLimit(Character_Sheet character) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) {
            return wizard.get_prepared_spell_count(character.stats.get_mod(character.stats.intel));
        }
        if (job instanceof Paladin_Class paladin) {
            return paladin.get_prepared_spell_count(character.stats.get_mod(character.stats.cha));
        }
        return 0;
    }

    public static int getMaxSpellLevel(Character_Sheet character) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) return wizard.get_max_spell_level();
        if (job instanceof Sorcerer_Class sorcerer) return sorcerer.get_max_spell_level();
        if (job instanceof Warlock_Class warlock) return warlock.pact_slot_level;
        if (job instanceof Paladin_Class paladin) return character.job.current_level < 2 ? 0 : paladin.get_max_spell_level();
        if (job instanceof Bard_Class bard) return bard.get_max_spell_level();
        return 0;
    }

    private static void addKnown(Character_Sheet character, String spellKey, boolean cantrip) {
        Spell_Definition spell = Spell_Library.get_spell(spellKey);
        if ((spell.level == 0) != cantrip) {
            throw new IllegalArgumentException(cantrip ? "Selected spell is not a cantrip" : "Selected spell is a cantrip");
        }
        if (!getAvailableSpellKeys(character, cantrip).contains(spellKey)) {
            throw new IllegalArgumentException("Spell is not available to this class or level");
        }

        List<String> selected = getKnownList(character, cantrip);
        int limit = cantrip ? getCantripLimit(character) : getKnownSpellLimit(character);
        if (selected.contains(spellKey)) return;
        if (selected.size() >= limit) {
            throw new IllegalArgumentException(cantrip ? "Cantrip limit reached" : "Known spell limit reached");
        }
        selected.add(spellKey);
    }

    private static void removeKnown(Character_Sheet character, String spellKey, boolean cantrip) {
        List<String> selected = getKnownList(character, cantrip);
        selected.remove(spellKey);
        if (!cantrip) {
            List<String> prepared = getPreparedList(character, false);
            if (prepared != null) prepared.remove(spellKey);
        }
    }

    private static void prepare(Character_Sheet character, String spellKey) {
        Spell_Definition spell = Spell_Library.get_spell(spellKey);
        if (spell.level <= 0) {
            throw new IllegalArgumentException("Cantrips do not need preparation");
        }

        List<String> prepared = getPreparedList(character, true);
        Character_Class job = character.job;
        if (job instanceof Wizard_Class) {
            if (!getKnownList(character, false).contains(spellKey)) {
                throw new IllegalArgumentException("Wizard can only prepare spells in the spellbook");
            }
        } else if (job instanceof Paladin_Class) {
            if (!getAvailableSpellKeys(character, false).contains(spellKey)) {
                throw new IllegalArgumentException("Spell is not available to this paladin level");
            }
        }

        if (prepared.contains(spellKey)) return;
        if (prepared.size() >= getPreparedSpellLimit(character)) {
            throw new IllegalArgumentException("Prepared spell limit reached");
        }
        prepared.add(spellKey);
    }

    private static void unprepare(Character_Sheet character, String spellKey) {
        List<String> prepared = getPreparedList(character, true);
        prepared.remove(spellKey);
    }

    private static List<String> getKnownList(Character_Sheet character, boolean cantrip) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) {
            return cantrip ? wizard.known_cantrip_keys : wizard.spellbook_spell_keys;
        }
        if (job instanceof Sorcerer_Class sorcerer) {
            return cantrip ? sorcerer.known_cantrip_keys : sorcerer.known_spell_keys;
        }
        if (job instanceof Warlock_Class warlock) {
            return cantrip ? warlock.known_cantrip_keys : warlock.known_spell_keys;
        }
        if (job instanceof Bard_Class bard) {
            return cantrip ? bard.known_cantrip_keys : bard.known_spell_keys;
        }
        throw new IllegalArgumentException(cantrip
                ? "This class cannot learn cantrips"
                : "This class does not learn known spells");
    }

    private static List<String> getPreparedList(Character_Sheet character, boolean required) {
        Character_Class job = requireJob(character);
        if (job instanceof Wizard_Class wizard) return wizard.prepared_spell_keys;
        if (job instanceof Paladin_Class paladin) return paladin.prepared_spell_keys;
        if (required) throw new IllegalArgumentException("This class does not prepare spells");
        return null;
    }

    private static Character_Class requireJob(Character_Sheet character) {
        if (character == null || character.job == null) {
            throw new IllegalArgumentException("Character class is required");
        }
        return character.job;
    }
}
