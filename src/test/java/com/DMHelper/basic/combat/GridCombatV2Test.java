package com.DMHelper.basic.combat;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class GridCombatV2Test {
    record Fixture(Combat_Engine engine,Battle_Grid grid,Combatant actor,Combatant ally,Combatant enemy) {}
    Battle_Grid.Cell at(int x,int y){return new Battle_Grid.Cell(x,y);}
    @SuppressWarnings("unchecked") Fixture fixture() throws Exception {
        Combat_Engine engine=new Combat_Engine(List.of(),List.of());
        Combatant actor=BattleGridTest.token("actor",Combatant.Side.PLAYER),ally=BattleGridTest.token("ally",Combatant.Side.PLAYER),enemy=BattleGridTest.token("enemy",Combatant.Side.ENEMY);
        Field field=Combat_Engine.class.getDeclaredField("initiative_order");field.setAccessible(true);
        ((List<Combatant>)field.get(engine)).addAll(List.of(actor,ally,enemy));
        Battle_Grid grid=new Battle_Grid(List.of(actor,ally,enemy));
        grid.place(actor,at(2,2),30);grid.place(ally,at(12,8),30);grid.place(enemy,at(8,8),30);engine.enable_grid(grid);
        return new Fixture(engine,grid,actor,ally,enemy);
    }
    Attack_Option damage(){return Attack_Option.auto_hit("hit","",1,1,0,"force").with_range(120);}
    Attack_Option bonus(){return Attack_Option.auto_hit("bonus","",0,1,0,"").with_range(0).with_target_mode(Attack_Option.Target_Mode.SELF).with_economy(Attack_Option.Action_Economy.BONUS_ACTION);}

    @Test void movementCanBeUndoneUntilAnActionAndNeverAfterItEvenAfterMovingAgain() throws Exception {
        Fixture f=fixture();f.engine.move_active(at(3,2));assertTrue(f.engine.can_undo_move());
        f.engine.undo_move();assertEquals(at(2,2),f.grid.position(f.actor));assertEquals(30,f.grid.remaining(f.actor));
        f.engine.move_active(at(3,2));f.engine.execute_attack(damage(),f.enemy);
        assertFalse(f.engine.can_undo_move());f.engine.move_active(at(4,2));assertFalse(f.engine.can_undo_move());
        assertThrows(IllegalArgumentException.class,f.engine::undo_move);
    }

    @Test void bonusActionLocksUndoButLeavesMainActionAvailableAndResetsNextTurn() throws Exception {
        Fixture f=fixture();f.engine.move_active(at(3,2));f.engine.execute_attack(bonus(),f.actor);
        assertTrue(f.engine.is_bonus_action_spent());assertFalse(f.engine.is_action_spent());assertFalse(f.engine.can_undo_move());
        assertNotNull(f.engine.action_error(bonus(),f.actor));f.engine.execute_attack(damage(),f.enemy);assertTrue(f.engine.is_action_spent());
        f.engine.skip_turn();assertFalse(f.engine.is_bonus_action_spent());assertFalse(f.engine.is_action_spent());
        assertFalse(f.engine.can_undo_move());
    }

    @Test void rejectedActionDoesNotLockUndoOrSpendResources() throws Exception {
        Fixture f=fixture();f.engine.move_active(at(3,2));
        var spell=damage().with_spell_slot_cost(1);assertNotNull(f.engine.action_error(spell,f.enemy));
        f.engine.execute_attack(spell,f.enemy);assertTrue(f.engine.can_undo_move());assertFalse(f.engine.is_action_spent());
        assertEquals(100,f.enemy.current_hp);
    }

    @Test void leavingReachUsesOneReactionAndLocksUndoEvenOnAMiss() throws Exception {
        Fixture f=fixture();f.grid.place(f.enemy,at(3,2),null);
        String log=f.engine.move_active(at(0,2));assertTrue(log.contains("借机攻击"));
        assertFalse(f.enemy.reaction_available);assertFalse(f.engine.can_undo_move());
        f.engine.move_active(at(2,2));assertFalse(f.engine.move_active(at(0,2)).contains("借机攻击"));
        f.engine.skip_turn();assertFalse(f.enemy.reaction_available);f.engine.skip_turn();assertTrue(f.enemy.reaction_available);
    }

    @Test void disengagePreventsOpportunityAttacksAndDashAddsMovement() throws Exception {
        Fixture f=fixture();f.grid.place(f.enemy,at(3,2),null);f.engine.tactical_action("DISENGAGE");
        assertFalse(f.engine.move_active(at(0,2)).contains("借机攻击"));assertTrue(f.enemy.reaction_available);
        assertTrue(f.engine.is_action_spent());assertFalse(f.engine.can_undo_move());
        Fixture d=fixture();d.engine.tactical_action("DASH");assertEquals(60,d.grid.remaining(d.actor));
        assertThrows(IllegalArgumentException.class,()->d.engine.tactical_action("DISENGAGE"));
    }

    @Test void opportunityAttackDeathStopsMovementAndInitializesNextActor() throws Exception {
        Fixture f=fixture();f.grid.place(f.enemy,at(3,2),null);
        f.enemy.attack_options.add(Attack_Option.attack_roll("lethal","",100,1,1,200,1,"piercing"));
        Field random=Dice_Util.class.getDeclaredField("RANDOM");random.setAccessible(true);((Random)random.get(null)).setSeed(1);
        f.engine.move_active(at(0,2));assertFalse(f.actor.is_alive());assertEquals(at(2,2),f.grid.position(f.actor));
        assertSame(f.ally,f.engine.get_active_combatant());assertEquals(30,f.grid.remaining(f.ally));assertFalse(f.engine.can_undo_move());
    }

    @Test void damageReactionCanBeAcceptedOrDeclinedAndCostsOnlyOnce() throws Exception {
        Fixture f=fixture();f.enemy.attack_options.add(damage().with_spell_slot_cost(1).on_damage());f.enemy.spell_slots_remaining[1]=2;
        f.engine.execute_attack(damage(),f.enemy);assertEquals(1,f.engine.get_reaction_offers().size());
        var offer=f.engine.get_reaction_offers().get(0);assertNotNull(f.engine.ability_error(bonus()));
        assertThrows(IllegalArgumentException.class,()->f.engine.move_active(at(3,2)));
        f.engine.resolve_reaction(offer.id(),true);assertEquals(99,f.actor.current_hp);assertEquals(1,f.enemy.spell_slots_remaining[1]);
        assertFalse(f.enemy.reaction_available);assertTrue(f.engine.get_reaction_offers().isEmpty());
        assertThrows(IllegalArgumentException.class,()->f.engine.resolve_reaction(offer.id(),true));
        Fixture g=fixture();g.enemy.attack_options.add(damage().with_spell_slot_cost(1).on_damage());g.enemy.spell_slots_remaining[1]=1;
        g.engine.execute_attack(damage(),g.enemy);g.engine.resolve_reaction(g.engine.get_reaction_offers().get(0).id(),false);
        assertEquals(1,g.enemy.spell_slots_remaining[1]);assertTrue(g.enemy.reaction_available);
    }

    @Test void bonusSpellRestrictsOtherSpellsButAllowsAnActionCantrip() throws Exception {
        Fixture f=fixture();f.actor.spell_slots_remaining[1]=2;
        f.engine.execute_attack(bonus().with_spell_slot_cost(1),f.actor);
        assertNotNull(f.engine.action_error(damage().with_spell_slot_cost(1),f.enemy));
        assertNull(f.engine.action_error(damage().as_spell(0),f.enemy));
        Fixture g=fixture();g.actor.spell_slots_remaining[1]=2;g.engine.execute_attack(damage().with_spell_slot_cost(1),g.enemy);
        assertNotNull(g.engine.action_error(bonus().with_spell_slot_cost(1),g.actor));
    }

    @Test void areaHitsAlliesAndEnemiesWithOneDamageRollAndOneSlotAndNoDuplicateLargeToken() throws Exception {
        Fixture f=fixture();f.grid.place(f.enemy,at(8,5),null,2);f.grid.place(f.ally,at(7,5),null);
        var blast=Attack_Option.save_dc("blast","",100,"Dexterity",2,6,0,true,"fire").with_range(120).with_spell_slot_cost(1)
                .with_area(Attack_Option.Area_Shape.CIRCLE,10,5,false);
        f.actor.spell_slots_remaining[1]=2;var targets=f.grid.area_targets(f.actor,at(8,5),blast);
        assertEquals(2,targets.size());assertTrue(targets.contains(f.ally));
        f.engine.execute_area(blast,at(8,5));assertEquals(f.enemy.current_hp,f.ally.current_hp);
        assertTrue(f.enemy.current_hp<100);assertEquals(1,f.actor.spell_slots_remaining[1]);assertTrue(f.engine.is_action_spent());
        assertSame(f.actor,f.engine.get_active_combatant());
    }

    @Test void invalidAreaDoesNotSpendActionOrSlotAndCoverBlocksSingleTarget() throws Exception {
        Fixture f=fixture();f.actor.spell_slots_remaining[1]=1;
        var blast=damage().with_spell_slot_cost(1).with_area(Attack_Option.Area_Shape.CIRCLE,5,5,false);
        f.engine.execute_area(blast,at(15,0));assertFalse(f.engine.is_action_spent());assertEquals(1,f.actor.spell_slots_remaining[1]);
        f.grid.place(f.enemy,at(8,2),null);f.grid.toggle_obstacle(at(5,2));
        assertNotNull(f.engine.action_error(damage(),f.enemy));
        assertFalse(f.engine.get_valid_targets(damage()).contains(f.enemy));
    }

    @Test void dyingAtTurnStartProcessesTheNextActorsTurnAndResetsMovement() throws Exception {
        Fixture f=fixture();f.ally.current_hp=1;f.ally.apply_status(Combat_Status_Type.BURNING,3);
        f.enemy.apply_status(Combat_Status_Type.BURNING,3);f.enemy.reaction_available=false;
        f.engine.skip_turn();
        assertFalse(f.ally.is_alive());assertSame(f.enemy,f.engine.get_active_combatant());
        assertTrue(f.enemy.current_hp<100);assertTrue(f.enemy.reaction_available);
        assertEquals(30,f.grid.remaining(f.enemy));assertTrue(f.engine.get_and_clear_pending_log().contains("enemy"));
    }

    @Test void areaKillingBothSidesIsNotReportedAsAPlayerVictory() throws Exception {
        Fixture f=fixture();f.grid.place(f.ally,at(3,2),null);f.grid.place(f.enemy,at(4,2),null);
        var blast=Attack_Option.auto_hit("lethal","",1,1,200,"force").with_range(120).with_area(Attack_Option.Area_Shape.CIRCLE,20,5,false);
        f.engine.execute_area(blast,at(3,2));
        assertTrue(f.engine.is_combat_finished());assertFalse(f.engine.did_players_win());
    }

    @Test void coverActuallyImprovesArmorClassAndDexteritySaves() throws Exception {
        Field random=Dice_Util.class.getDeclaredField("RANDOM");random.setAccessible(true);Random dice=(Random)random.get(null);
        Fixture plain=fixture(),covered=fixture();
        plain.grid.place(plain.enemy,at(8,2),null);covered.grid.place(covered.enemy,at(8,2),null);
        covered.grid.toggle_terrain(at(5,2),Battle_Grid.Terrain.HALF_COVER);
        var attack=Attack_Option.attack_roll("attack","",4,1,1,0,1,"piercing").with_ranged_range(80,320);
        dice.setSeed(1);plain.engine.execute_attack(attack,plain.enemy);
        dice.setSeed(1);covered.engine.execute_attack(attack,covered.enemy);
        assertEquals(99,plain.enemy.current_hp);assertEquals(100,covered.enemy.current_hp);
        Fixture savePlain=fixture(),saveCovered=fixture();
        savePlain.grid.place(savePlain.enemy,at(8,2),null);saveCovered.grid.place(saveCovered.enemy,at(8,2),null);
        saveCovered.grid.toggle_terrain(at(5,2),Battle_Grid.Terrain.HALF_COVER);
        var save=Attack_Option.save_dc("save","",7,"Dexterity",1,1,10,false,"fire").with_range(80);
        dice.setSeed(1);savePlain.engine.execute_attack(save,savePlain.enemy);
        dice.setSeed(1);saveCovered.engine.execute_attack(save,saveCovered.enemy);
        assertEquals(89,savePlain.enemy.current_hp);assertEquals(100,saveCovered.enemy.current_hp);
    }
}
