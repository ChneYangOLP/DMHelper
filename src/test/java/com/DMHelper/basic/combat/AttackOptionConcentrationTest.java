package com.DMHelper.basic.combat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttackOptionConcentrationTest {

    @Test
    void spellSlotCostDoesNotImplicitlyRequireConcentration() {
        Attack_Option fireball = Attack_Option.save_dc(
                "Fireball", "", 15, "Dexterity", 8, 6, 0, true, "Fire"
        ).with_spell_slot_cost(3);

        assertFalse(fireball.requires_concentration);
    }

    @Test
    void concentrationMustBeMarkedExplicitly() {
        Attack_Option holdPerson = Attack_Option.save_dc(
                "Hold Person", "", 15, "Wisdom", 0, 1, 0, false, "Control"
        ).with_spell_slot_cost(2).with_concentration();

        assertTrue(holdPerson.requires_concentration);
    }

    @Test
    void combatEngineStartsConcentrationOnlyForMarkedActions() throws Exception {
        Combat_Engine normalSpellEngine = engineWithTwoCombatants();
        Combatant normalCaster = normalSpellEngine.get_active_combatant();
        normalCaster.spell_slots_remaining[1] = 1;
        Attack_Option magicMissile = Attack_Option.auto_hit(
                "Magic Missile", "", 0, 1, 0, "Force"
        ).with_spell_slot_cost(1);

        normalSpellEngine.execute_attack(magicMissile, normalSpellEngine.get_valid_targets(magicMissile).get(0));
        assertFalse(normalSpellEngine.get_concentration_manager().has_concentration(normalCaster));

        Combat_Engine concentrationEngine = engineWithTwoCombatants();
        Combatant concentrationCaster = concentrationEngine.get_active_combatant();
        concentrationCaster.spell_slots_remaining[1] = 1;
        Attack_Option hex = Attack_Option.auto_hit(
                "Hex", "", 0, 1, 0, "Curse"
        ).with_spell_slot_cost(1).with_concentration();

        concentrationEngine.execute_attack(hex, concentrationEngine.get_valid_targets(hex).get(0));
        assertTrue(concentrationEngine.get_concentration_manager().has_concentration(concentrationCaster));
    }

    @SuppressWarnings("unchecked")
    private static Combat_Engine engineWithTwoCombatants() throws Exception {
        Combat_Engine engine = new Combat_Engine(List.of(), List.of());
        Field initiativeOrderField = Combat_Engine.class.getDeclaredField("initiative_order");
        initiativeOrderField.setAccessible(true);
        List<Combatant> initiativeOrder = (List<Combatant>) initiativeOrderField.get(engine);
        initiativeOrder.add(combatant("Caster", Combatant.Side.PLAYER));
        initiativeOrder.add(combatant("Target", Combatant.Side.ENEMY));
        return engine;
    }

    private static Combatant combatant(String name, Combatant.Side side) {
        return new Combatant(
                name, side, 10, 100, 0,
                10, 10, 10, 10, 10, 10, 2,
                new ArrayList<>(), null, null, 0
        );
    }
}
