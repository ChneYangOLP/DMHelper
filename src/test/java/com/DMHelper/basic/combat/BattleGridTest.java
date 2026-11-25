package com.DMHelper.basic.combat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BattleGridTest {
    static Combatant token(String name, Combatant.Side side) {
        return new Combatant(name, side, 10, 100, 0, 10, 10, 10, 10, 10, 10, 2, List.of(), null, null, 0);
    }

    @Test void straightAndDiagonalMovesSpendFiveFeetEachAndAccumulate() {
        Combatant actor = token("actor", Combatant.Side.PLAYER);
        Battle_Grid grid = new Battle_Grid(List.of(actor));
        grid.place(actor, new Battle_Grid.Cell(2, 2), 30);
        assertEquals(5, grid.move(actor, new Battle_Grid.Cell(3, 3)));
        assertEquals(10, grid.move(actor, new Battle_Grid.Cell(5, 3)));
        assertEquals(15, grid.remaining(actor));
        assertThrows(IllegalArgumentException.class, () -> grid.move(actor, new Battle_Grid.Cell(10, 3)));
        assertEquals(new Battle_Grid.Cell(5, 3), grid.position(actor));
        grid.begin_turn(actor);
        assertEquals(30, grid.remaining(actor));
    }

    @Test void pathsCannotCutObstacleCornersButCanRouteAroundWalls() {
        Combatant actor = token("actor", Combatant.Side.PLAYER);
        Battle_Grid grid = new Battle_Grid(List.of(actor));
        grid.place(actor, new Battle_Grid.Cell(0, 0), 30);
        grid.toggle_obstacle(new Battle_Grid.Cell(1, 0));
        assertEquals(10, grid.move(actor, new Battle_Grid.Cell(1, 1)));
        grid.place(actor, new Battle_Grid.Cell(0, 0), 5);
        grid.begin_turn(actor);
        assertFalse(grid.available_moves(actor).stream().anyMatch(m -> m.cell().equals(new Battle_Grid.Cell(1, 1))));
    }

    @Test void occupiedAndOutOfBoundsDestinationsAreRejectedWithoutSpendingMovement() {
        Combatant actor = token("actor", Combatant.Side.PLAYER), enemy = token("enemy", Combatant.Side.ENEMY);
        Battle_Grid grid = new Battle_Grid(List.of(actor, enemy));
        grid.place(actor, new Battle_Grid.Cell(2, 2), null);
        grid.place(enemy, new Battle_Grid.Cell(3, 2), null);
        assertThrows(IllegalArgumentException.class, () -> grid.move(actor, grid.position(enemy)));
        assertThrows(IllegalArgumentException.class, () -> grid.place(actor, new Battle_Grid.Cell(-1, 0), null));
        assertThrows(IllegalArgumentException.class, () -> grid.toggle_obstacle(grid.position(enemy)));
        assertThrows(IllegalArgumentException.class, () -> grid.place(actor, grid.position(actor), 33));
        assertEquals(30, grid.remaining(actor));
        enemy.current_hp = 0;
        assertEquals(5, grid.move(actor, grid.position(enemy)));
    }

    @Test void restrainedAndBlockedActorsCannotMoveAndProneActorsCrawl() {
        Combatant actor = token("actor", Combatant.Side.PLAYER);
        Battle_Grid grid = new Battle_Grid(List.of(actor));
        grid.place(actor, new Battle_Grid.Cell(2, 2), 30);
        actor.apply_status(Combat_Status_Type.RESTRAINED, 1);
        assertTrue(grid.available_moves(actor).isEmpty());
        actor.status_effects.clear();
        actor.apply_status(Combat_Status_Type.PARALYZED, 1);
        assertTrue(grid.available_moves(actor).isEmpty());
        actor.status_effects.clear();
        actor.apply_status(Combat_Status_Type.PRONE, 1);
        assertEquals(10, grid.move(actor, new Battle_Grid.Cell(3, 3)));
        assertEquals(20, grid.remaining(actor));
        actor.apply_status(Combat_Status_Type.SLOWED, 1);
        assertEquals(5, grid.remaining(actor));
    }

    @Test void distanceAndMaximumRangeUseGridSquaresRatherThanMovementPaths() {
        Combatant actor = token("actor", Combatant.Side.PLAYER), enemy = token("enemy", Combatant.Side.ENEMY);
        Battle_Grid grid = new Battle_Grid(List.of(actor, enemy));
        grid.place(actor, new Battle_Grid.Cell(0, 0), null);
        grid.place(enemy, new Battle_Grid.Cell(6, 6), null);
        Attack_Option bow = Attack_Option.attack_roll("bow", "", 0, 1, 6, 0, 1, "piercing").with_ranged_range(20, 30);
        assertEquals(30, grid.distance(actor, enemy));
        assertTrue(grid.in_range(actor, enemy, bow));
        assertTrue(grid.ranged_disadvantage(actor, enemy, bow));
        grid.place(enemy, new Battle_Grid.Cell(7, 6), null);
        assertFalse(grid.in_range(actor, enemy, bow));
        grid.place(enemy, new Battle_Grid.Cell(1, 1), null);
        assertTrue(grid.ranged_disadvantage(actor, enemy, bow));
        grid.place(enemy, new Battle_Grid.Cell(3, 3), null);
        assertFalse(grid.ranged_disadvantage(actor, enemy, bow));
    }

    @Test void automaticPlacementSupportsFullCapacityWithoutOverlaps() {
        List<Combatant> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < 48; i++) {
            tokens.add(token("player" + i, Combatant.Side.PLAYER));
            tokens.add(token("enemy" + i, Combatant.Side.ENEMY));
        }
        Battle_Grid grid = new Battle_Grid(tokens);
        assertEquals(96, tokens.stream().map(grid::position).distinct().count());
        assertTrue(tokens.stream().allMatch(c -> grid.in_bounds(grid.position(c))));
        tokens.add(token("overflow", Combatant.Side.PLAYER));
        assertThrows(IllegalArgumentException.class, () -> new Battle_Grid(tokens));
    }
}
