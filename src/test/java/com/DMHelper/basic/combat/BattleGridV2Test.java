package com.DMHelper.basic.combat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BattleGridV2Test {
    Battle_Grid.Cell at(int x,int y) { return new Battle_Grid.Cell(x,y); }
    Combatant token(String name,Combatant.Side side) { return BattleGridTest.token(name,side); }

    @Test void undoRestoresOnlyTheLastMovementAndItsExactCost() {
        Combatant actor=token("actor",Combatant.Side.PLAYER);
        Battle_Grid grid=new Battle_Grid(List.of(actor));grid.place(actor,at(2,2),30);
        grid.move(actor,at(3,2));grid.toggle_terrain(at(4,2),Battle_Grid.Terrain.DIFFICULT);
        assertEquals(10,grid.move(actor,at(4,2)));
        assertEquals(15,grid.remaining(actor));assertTrue(grid.can_undo(actor));
        grid.undo_move(actor);assertEquals(at(3,2),grid.position(actor));assertEquals(25,grid.remaining(actor));
        assertFalse(grid.can_undo(actor));assertThrows(IllegalArgumentException.class,()->grid.undo_move(actor));
    }

    @Test void difficultyAndCrawlingAddTheirMovementCosts() {
        Combatant actor=token("actor",Combatant.Side.PLAYER);
        Battle_Grid grid=new Battle_Grid(List.of(actor));grid.place(actor,at(0,0),30);
        actor.apply_status(Combat_Status_Type.PRONE,2);grid.toggle_terrain(at(1,0),Battle_Grid.Terrain.DIFFICULT);
        assertEquals(15,grid.move(actor,at(1,0)));grid.undo_move(actor);
        assertEquals(30,grid.remaining(actor));grid.stand(actor);
        assertEquals(15,grid.remaining(actor));assertFalse(grid.can_undo(actor));
        assertTrue(actor.status_effects.isEmpty());
    }

    @Test void largeTokensReserveTheirEntireFootprintAndUseClosestSquareForRange() {
        Combatant actor=token("actor",Combatant.Side.PLAYER),enemy=token("enemy",Combatant.Side.ENEMY);
        Battle_Grid grid=new Battle_Grid(List.of(actor,enemy));grid.place(actor,at(2,2),30,2);grid.place(enemy,at(4,3),null);
        assertSame(actor,grid.occupant(at(3,3)));assertEquals(5,grid.distance(actor,enemy));
        assertThrows(IllegalArgumentException.class,()->grid.place(enemy,at(3,3),null));
        assertThrows(IllegalArgumentException.class,()->grid.place(actor,at(15,11),null,2));
        grid.toggle_obstacle(at(4,2));
        assertFalse(grid.available_moves(actor).stream().anyMatch(m->m.cell().equals(at(3,2))));
        assertEquals(at(2,2),grid.position(actor));
    }

    @Test void wallsBlockSightWhilePartialCoverCanBeShotThrough() {
        Combatant actor=token("actor",Combatant.Side.PLAYER),enemy=token("enemy",Combatant.Side.ENEMY);
        Battle_Grid grid=new Battle_Grid(List.of(actor,enemy));grid.place(actor,at(0,4),null);grid.place(enemy,at(6,4),null);
        var bow=Attack_Option.attack_roll("bow","",0,1,6,0,1,"piercing").with_ranged_range(80,320);
        grid.toggle_terrain(at(3,4),Battle_Grid.Terrain.HALF_COVER);
        assertEquals(Cover_Type.HALF,grid.cover(actor,enemy));assertTrue(grid.in_range(actor,enemy,bow));
        grid.toggle_terrain(at(3,4),Battle_Grid.Terrain.THREE_QUARTERS_COVER);
        assertEquals(Cover_Type.THREE_QUARTERS,grid.cover(actor,enemy));
        grid.toggle_obstacle(at(3,4));assertEquals(Cover_Type.FULL,grid.cover(actor,enemy));
        assertFalse(grid.in_range(actor,enemy,bow));
    }

    @Test void creaturesBetweenAttackerAndTargetProvideHalfCover() {
        Combatant actor=token("actor",Combatant.Side.PLAYER),enemy=token("enemy",Combatant.Side.ENEMY),blocker=token("blocker",Combatant.Side.PLAYER);
        Battle_Grid grid=new Battle_Grid(List.of(actor,enemy,blocker));
        grid.place(actor,at(0,3),null);grid.place(enemy,at(6,3),null);grid.place(blocker,at(3,3),null);
        assertEquals(Cover_Type.HALF,grid.cover(actor,enemy));blocker.current_hp=0;
        assertEquals(Cover_Type.NONE,grid.cover(actor,enemy));
    }

    @Test void cubeHasTheDeclaredNumberOfSquaresAndCircleIncludesAllies() {
        Combatant actor=token("actor",Combatant.Side.PLAYER),ally=token("ally",Combatant.Side.PLAYER);
        Battle_Grid grid=new Battle_Grid(List.of(actor,ally));grid.place(actor,at(0,0),null);grid.place(ally,at(6,6),null);
        var cube=Attack_Option.auto_hit("cube","",0,1,0,"").with_range(120).with_area(Attack_Option.Area_Shape.CUBE,30,5,false);
        assertEquals(36,grid.area_cells(actor,at(5,5),cube).size());
        var circle=Attack_Option.auto_hit("circle","",1,6,0,"").with_range(120).with_area(Attack_Option.Area_Shape.CIRCLE,10,5,false);
        assertTrue(grid.area_targets(actor,at(6,5),circle).contains(ally));
        assertThrows(IllegalArgumentException.class,()->grid.area_cells(actor,at(-1,2),circle));
    }

    @Test void directionalAreasExcludeTheCasterAndRequireADirection() {
        Combatant actor=token("actor",Combatant.Side.PLAYER);
        Battle_Grid grid=new Battle_Grid(List.of(actor));grid.place(actor,at(2,4),null);
        var cone=Attack_Option.auto_hit("cone","",1,6,0,"").with_area(Attack_Option.Area_Shape.CONE,15,5,true);
        var cells=grid.area_cells(actor,at(10,4),cone);
        assertTrue(cells.contains(at(5,4)));assertFalse(cells.contains(at(1,4)));assertFalse(cells.contains(at(4,7)));
        assertFalse(cells.contains(at(2,4)));assertThrows(IllegalArgumentException.class,()->grid.area_cells(actor,at(2,4),cone));
        var line=Attack_Option.auto_hit("line","",1,6,0,"").with_area(Attack_Option.Area_Shape.LINE,15,5,true);
        assertEquals(List.of(at(3,4),at(4,4),at(5,4)),grid.area_cells(actor,at(10,4),line));
    }

    @Test void areaCannotBePlacedBehindASealedWallAndDoesNotPassThroughIt() {
        Combatant actor=token("actor",Combatant.Side.PLAYER);
        Battle_Grid grid=new Battle_Grid(List.of(actor));grid.place(actor,at(1,4),null);
        for(int y=0;y<Battle_Grid.HEIGHT;y++)grid.toggle_obstacle(at(5,y));
        var circle=Attack_Option.auto_hit("circle","",1,6,0,"").with_range(120).with_area(Attack_Option.Area_Shape.CIRCLE,20,5,false);
        assertThrows(IllegalArgumentException.class,()->grid.area_cells(actor,at(7,4),circle));
        assertFalse(grid.area_cells(actor,at(4,4),circle).contains(at(6,4)));
        circle.around_corners();assertFalse(grid.area_cells(actor,at(4,4),circle).contains(at(6,4)));
    }
}
