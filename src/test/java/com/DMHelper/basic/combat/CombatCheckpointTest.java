package com.DMHelper.basic.combat;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.race.Human_Race;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CombatCheckpointTest {
    record Fixture(Combat_Engine engine, List<Combatant> all, Battle_Grid grid, Character_Sheet character) {
        Combatant actor() { return all.stream().filter(c -> c.side==Combatant.Side.PLAYER).findFirst().orElseThrow(); }
        Combatant enemy() { return all.stream().filter(c -> c.side==Combatant.Side.ENEMY).findFirst().orElseThrow(); }
    }
    Fixture fixture() {
        var character=Character_Sheet.restore_saved_character("Snapshot fighter",20,"",new Human_Race(),new Fighter_Class(),new Stats(12,12,12,12,12,12));
        character.database_id=123; character.hp=character.current_hp=100;
        var ogre=Monster_Library.get_all_monsters().stream().filter(m -> m.key.equals("ogre")).findFirst().orElseThrow();
        var engine=new Combat_Engine(List.of(character),List.of(ogre)); engine.defer_character_persistence();
        var all=engine.get_initiative_order(); var grid=new Battle_Grid(all); engine.enable_grid(grid);
        var actor=all.stream().filter(c -> c.side==Combatant.Side.PLAYER).findFirst().orElseThrow();
        var enemy=all.stream().filter(c -> c.side==Combatant.Side.ENEMY).findFirst().orElseThrow();
        grid.place(actor,new Battle_Grid.Cell(2,5),35,1); grid.place(enemy,new Battle_Grid.Cell(12,6),30,2);
        var state=engine.snapshot(all); state.order=List.of(all.indexOf(actor),all.indexOf(enemy));
        var restored=Combat_Engine.restore(state,grid.snapshot(),List.of(character));
        return new Fixture(restored.engine(),restored.combatants(),restored.grid(),character);
    }
    Combat_Checkpoint.Saved snapshot(Fixture f) {
        return new Combat_Checkpoint.Saved(1,"test-encounter",true,List.of("saved log"),f.engine.snapshot(f.all),f.grid.snapshot());
    }
    Fixture roundTrip(Fixture f) {
        var saved=Combat_Checkpoint.decode(Combat_Checkpoint.encode(snapshot(f)));
        var restored=Combat_Engine.restore(saved.engine(),saved.grid(),List.of(f.character));
        return new Fixture(restored.engine(),restored.combatants(),restored.grid(),f.character);
    }
    Attack_Option hit() { return Attack_Option.auto_hit("test hit","",1,1,0,"force").with_range(120); }

    @Test void fullSnapshotRoundTripsResourcesOrderStatusesAndConcentrationWithoutTicking() {
        Fixture f=fixture();
        f.actor().spell_slots_remaining[1]=2; f.actor().pact_slots_remaining=1; f.actor().pact_slot_level=3;
        f.actor().sorcery_points_remaining=4; f.actor().bardic_inspiration_remaining=2;
        f.actor().superiority_dice_remaining=3; f.actor().superiority_dice_size=10; f.actor().lay_on_hands_remaining=17;
        f.actor().apply_status(Combat_Status_Type.BURNING,3);
        f.enemy().reaction_available=false;
        f.engine.get_concentration_manager().start_concentration(f.actor(),"test concentration",2);
        f.engine.get_concentration_manager().get_concentration(f.actor()).damage_taken=7;
        f.grid.toggle_terrain(new Battle_Grid.Cell(5,5),Battle_Grid.Terrain.DIFFICULT);
        f.grid.toggle_terrain(new Battle_Grid.Cell(8,3),Battle_Grid.Terrain.HALF_COVER);
        String before=Combat_Checkpoint.encode(snapshot(f));
        Fixture loaded=roundTrip(f);
        assertEquals(before,Combat_Checkpoint.encode(snapshot(loaded)));
        assertEquals(100,loaded.actor().current_hp); // Loading must not apply burning/start-of-turn damage.
        assertSame(f.character,loaded.actor().linked_character);
        assertEquals(7,loaded.engine.get_concentration_manager().get_concentration(loaded.actor()).damage_taken);
    }
    @Test void movementBudgetAndUndoSurviveReload() {
        Fixture f=fixture(); f.engine.move_active(new Battle_Grid.Cell(3,5));
        Fixture loaded=roundTrip(f);
        assertTrue(loaded.engine.can_undo_move()); assertEquals(30,loaded.grid.remaining(loaded.actor()));
        loaded.engine.undo_move();
        assertEquals(new Battle_Grid.Cell(2,5),loaded.grid.position(loaded.actor()));
        assertEquals(35,loaded.grid.remaining(loaded.actor()));
    }
    @Test void dashActionAndExtraMovementRemainSpentAfterReload() {
        Fixture f=fixture(); f.engine.tactical_action("DASH"); f.engine.move_active(new Battle_Grid.Cell(3,5));
        Fixture loaded=roundTrip(f);
        assertTrue(loaded.engine.is_action_spent()); assertFalse(loaded.engine.can_undo_move());
        assertEquals(65,loaded.grid.remaining(loaded.actor()));
        assertThrows(IllegalArgumentException.class,() -> loaded.engine.tactical_action("DASH"));
    }
    @Test void bonusSpellRestrictionsAndIndependentActionFlagsSurviveReload() {
        Fixture f=fixture(); f.actor().spell_slots_remaining[1]=3;
        var bonus=Attack_Option.auto_hit("bonus heal","",0,1,0,"").with_healing(1,1,0).with_target_mode(Attack_Option.Target_Mode.SELF).with_economy(Attack_Option.Action_Economy.BONUS_ACTION).with_spell_slot_cost(1).with_range(120);
        f.actor().attack_options.add(bonus);
        f.engine.execute_attack(bonus,f.actor());
        Fixture loaded=roundTrip(f);
        assertTrue(loaded.engine.is_bonus_action_spent()); assertFalse(loaded.engine.is_action_spent());
        assertEquals(2,loaded.actor().spell_slots_remaining[1]);
        assertNotNull(loaded.engine.action_error(hit().with_spell_slot_cost(1),loaded.enemy()));
        assertNull(loaded.engine.action_error(hit().as_spell(0),loaded.enemy()));
    }
    @Test void pendingReactionKeepsItsIdAndCanOnlyConsumeResourcesOnce() {
        Fixture f=fixture(); f.enemy().spell_slots_remaining[1]=2;
        f.enemy().attack_options.add(hit().with_spell_slot_cost(1).on_damage());
        f.engine.execute_attack(hit(),f.enemy());
        String offer=f.engine.get_reaction_offers().get(0).id();
        Fixture loaded=roundTrip(f);
        assertEquals(offer,loaded.engine.get_reaction_offers().get(0).id());
        assertThrows(IllegalArgumentException.class,() -> loaded.engine.move_active(new Battle_Grid.Cell(3,5)));
        loaded.engine.resolve_reaction(offer,true);
        Fixture again=roundTrip(loaded);
        assertEquals(1,again.enemy().spell_slots_remaining[1]); assertFalse(again.enemy().reaction_available);
        assertTrue(again.engine.get_reaction_offers().isEmpty());
        assertThrows(IllegalArgumentException.class,() -> again.engine.resolve_reaction(offer,true));
    }
    @Test void finishedCombatDoesNotRerollRewardsOrGrantXpAgain() {
        Fixture f=fixture(); f.engine.execute_attack(Attack_Option.auto_hit("finish","",1,1,10000,"force").with_range(120),f.enemy());
        int xp=f.character.experience_points; var loot=f.engine.get_pending_loot_keys();
        Fixture loaded=roundTrip(f); Fixture again=roundTrip(loaded);
        assertTrue(again.engine.is_combat_finished()); assertTrue(again.engine.did_players_win());
        assertEquals(xp,f.character.experience_points); assertEquals(loot,again.engine.get_pending_loot_keys());
        assertEquals(0,again.enemy().current_hp);
    }
    @Test void unsupportedOrIncompleteSnapshotsAreRejected() {
        assertThrows(IllegalArgumentException.class,() -> Combat_Checkpoint.decode("null"));
        assertThrows(IllegalArgumentException.class,() -> Combat_Checkpoint.decode("{\"version\":99}"));
        Fixture f=fixture(); var saved=Combat_Checkpoint.decode(Combat_Checkpoint.encode(snapshot(f)));
        assertThrows(IllegalArgumentException.class,() -> Combat_Engine.restore(saved.engine(),saved.grid(),List.of()));
    }
}
