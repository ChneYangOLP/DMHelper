package com.DMHelper.basic.combat;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GridCombatTest {
    record Fixture(Combat_Engine engine, Battle_Grid grid, Combatant actor, Combatant enemy) {}

    @SuppressWarnings("unchecked")
    Fixture fixture(boolean useGrid) throws Exception {
        Combat_Engine engine = new Combat_Engine(List.of(), List.of());
        Field order = Combat_Engine.class.getDeclaredField("initiative_order");
        order.setAccessible(true);
        Combatant actor = BattleGridTest.token("actor", Combatant.Side.PLAYER);
        Combatant enemy = BattleGridTest.token("enemy", Combatant.Side.ENEMY);
        ((List<Combatant>) order.get(engine)).addAll(List.of(actor, enemy));
        Battle_Grid grid = new Battle_Grid(List.of(actor, enemy));
        grid.place(actor, new Battle_Grid.Cell(2, 2), 30);
        grid.place(enemy, new Battle_Grid.Cell(3, 2), 30);
        if (useGrid) engine.enable_grid(grid);
        return new Fixture(engine, grid, actor, enemy);
    }

    @Test void attackKeepsTurnAllowsRemainingMovementAndRejectsSecondAction() throws Exception {
        Fixture f = fixture(true);
        Attack_Option attack = Attack_Option.auto_hit("attack", "", 1, 1, 0, "force");
        f.grid.move(f.actor, new Battle_Grid.Cell(2, 3));
        f.engine.execute_attack(attack, f.enemy);
        assertSame(f.actor, f.engine.get_active_combatant());
        assertEquals(99, f.enemy.current_hp);
        assertTrue(f.engine.is_action_spent());
        assertEquals(5, f.grid.move(f.actor, new Battle_Grid.Cell(2, 4)));
        assertEquals(20, f.grid.remaining(f.actor));
        assertNotNull(f.engine.action_error(attack, f.enemy));
        f.engine.execute_attack(attack, f.enemy);
        assertEquals(99, f.enemy.current_hp);
        f.engine.skip_turn();
        assertSame(f.enemy, f.engine.get_active_combatant());
        assertEquals(0, f.engine.get_current_round());
        assertFalse(f.engine.is_action_spent());
        f.engine.skip_turn();
        assertSame(f.actor, f.engine.get_active_combatant());
        assertEquals(1, f.engine.get_current_round());
        assertEquals(30, f.grid.remaining(f.actor));
    }

    @Test void rejectedRangeOrResourceCheckDoesNotSpendSlotOrAction() throws Exception {
        Fixture f = fixture(true);
        Attack_Option spell = Attack_Option.auto_hit("spell", "", 1, 1, 0, "force").with_range(5).with_spell_slot_cost(1);
        f.actor.spell_slots_remaining[1] = 1;
        f.grid.place(f.enemy, new Battle_Grid.Cell(4, 2), null);
        assertTrue(f.engine.get_valid_targets(spell).isEmpty());
        f.engine.execute_attack(spell, f.enemy);
        assertEquals(1, f.actor.spell_slots_remaining[1]);
        assertFalse(f.engine.is_action_spent());
        assertEquals(100, f.enemy.current_hp);
        f.grid.place(f.enemy, new Battle_Grid.Cell(3, 2), null);
        f.actor.spell_slots_remaining[1] = 0;
        f.engine.execute_attack(spell, f.enemy);
        assertFalse(f.engine.is_action_spent());
        f.actor.spell_slots_remaining[1] = 1;
        f.engine.execute_attack(spell, f.enemy);
        assertTrue(f.engine.is_action_spent());
        assertEquals(0, f.actor.spell_slots_remaining[1]);
    }

    @Test void legacyCombatStillAdvancesAutomatically() throws Exception {
        Fixture f = fixture(false);
        f.engine.execute_attack(Attack_Option.auto_hit("attack", "", 1, 1, 0, "force"), f.enemy);
        assertSame(f.enemy, f.engine.get_active_combatant());
    }

    @Test void lastEnemyDeathStillCompletesCombatWithoutEndingTurn() throws Exception {
        Fixture f = fixture(true);
        f.enemy.current_hp = 1;
        f.engine.execute_attack(Attack_Option.auto_hit("attack", "", 1, 1, 0, "force"), f.enemy);
        assertTrue(f.engine.is_combat_finished());
        assertTrue(f.engine.did_players_win());
    }

    @Test void builtInWeaponsAndSpellsHaveExplicitRanges() {
        var goblin = Monster_Library.get_all_monsters().stream().filter(m -> m.key.equals("goblin")).findFirst().orElseThrow();
        assertEquals(5, goblin.attack_options.get(0).range_ft);
        assertEquals(80, goblin.attack_options.get(1).range_ft);
        assertEquals(320, goblin.attack_options.get(1).max_range_ft);
    }

    @Test void initiativeOrderRunsFromHighestToLowestRoll() {
        var goblin = Monster_Library.get_all_monsters().stream().filter(m -> m.key.equals("goblin")).findFirst().orElseThrow();
        Combat_Engine engine = new Combat_Engine(List.of(), java.util.Collections.nCopies(20, goblin));
        var order = engine.get_initiative_order();
        for (int i = 1; i < order.size(); i++) {
            assertTrue(order.get(i - 1).initiative_total >= order.get(i).initiative_total);
        }
    }
}
