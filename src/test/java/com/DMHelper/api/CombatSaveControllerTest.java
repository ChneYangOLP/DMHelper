package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.combat.*;
import com.DMHelper.basic.database.*;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.race.Human_Race;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatSaveControllerTest {
    @TempDir Path directory;
    Javalin app;
    String previousDirectory, sessionId;
    List<Character_Sheet> previousPool;
    Character_Sheet character;
    final HttpClient client=HttpClient.newHttpClient();
    @BeforeEach void setup() throws Exception {
        previousDirectory=System.getProperty("dmhelper.data.dir"); previousPool=List.copyOf(Global_Data.character_pool);
        System.setProperty("dmhelper.data.dir",directory.toString()); Files.createFile(directory.resolve("dnd_data.db"));
        Init_DB.setup_database(); Global_Data.character_pool.clear(); clearCache();
        character=Character_Sheet.restore_saved_character("Save fighter",20,"",new Human_Race(),new Fighter_Class(),new Stats(12,12,12,12,12,12));
        assertTrue(Character_DAO.save_character(character));
        Global_Data.character_pool.add(character); startServer();
    }
    void startServer() {
        app=Javalin.create(config -> config.jetty.defaultHost="127.0.0.1");
        app.get("/current",CombatController::getCurrent); app.delete("/current",CombatController::clearCurrent);
        app.post("/sessions",CombatController::start); app.get("/sessions/{id}",CombatController::getState);
        app.post("/sessions/{id}/deployment",CombatController::deploy); app.post("/sessions/{id}/begin",CombatController::begin);
        app.post("/sessions/{id}/move",CombatController::move); app.post("/sessions/{id}/undo-move",CombatController::undoMove);
        app.post("/sessions/{id}/tactical",CombatController::tactical); app.post("/sessions/{id}/skip",CombatController::skip);
        app.post("/sessions/{id}/actions",CombatController::act); app.post("/sessions/{id}/loot",CombatController::assignLoot);
        app.start(0);
    }
    void clearCache() throws Exception {
        var field=CombatController.class.getDeclaredField("SESSIONS"); field.setAccessible(true); ((Map<?,?>)field.get(null)).clear();
    }
    void restart() throws Exception { app.stop(); clearCache(); Character_DAO.load_all_characters(); character=Global_Data.character_pool.get(0); startServer(); }
    @AfterEach void cleanup() throws Exception {
        if(app!=null)app.stop(); clearCache(); Global_Data.character_pool.clear(); Global_Data.character_pool.addAll(previousPool);
        if(previousDirectory==null)System.clearProperty("dmhelper.data.dir");else System.setProperty("dmhelper.data.dir",previousDirectory);
    }
    String request(String method,String path,String body,int expected) throws Exception {
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+app.port()+path))
                .header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,response.statusCode(),response.body()); return response.body();
    }
    JsonObject json(String value) { return ApiServer.GSON.fromJson(value,JsonObject.class); }
    String endpoint(String suffix) { return "/sessions/"+sessionId+suffix; }
    JsonObject create() throws Exception {
        JsonObject result=json(request("POST","/sessions","{\"characterIds\":["+character.database_id+"],\"monsterKeys\":[\"ogre\"]}",200));
        sessionId=result.get("sessionId").getAsString(); return result;
    }
    JsonObject current() throws Exception { return json(request("GET","/current",null,200)); }
    JsonObject active(JsonObject state) { return state.getAsJsonArray("combatants").asList().stream().map(v -> v.getAsJsonObject()).filter(v -> v.get("active").getAsBoolean()).findFirst().orElseThrow(); }
    JsonObject comparable(JsonObject state) { state.remove("latestLog"); return state; }
    void sql(String statement) throws Exception { try(var conn=DB_Helper.get_connection();var stmt=conn.createStatement()) { stmt.execute(statement); } }

    @Test void deploymentAndMidTurnResumeAfterLosingAllServerMemory() throws Exception {
        request("GET","/current",null,204); var created=create(); assertNotNull(created.get("savedAt"));
        request("POST","/sessions","{\"characterIds\":["+character.database_id+"],\"monsterKeys\":[\"ogre\"]}",409);
        request("POST",endpoint("/deployment"),"{\"obstacle\":true,\"x\":7,\"y\":7,\"terrain\":\"DIFFICULT\"}",200);
        JsonObject deployed=current(); restart(); assertEquals(comparable(deployed),comparable(current()));
        JsonObject begun=json(request("POST",endpoint("/begin"),"{}",200));
        String actor=active(begun).get("id").getAsString(); var move=begun.getAsJsonObject("grid").getAsJsonArray("moves").get(0).getAsJsonObject();
        JsonObject moved=json(request("POST",endpoint("/move"),"{\"combatantId\":\""+actor+"\",\"x\":"+move.get("x")+",\"y\":"+move.get("y")+"}",200));
        assertTrue(moved.get("canUndoMove").getAsBoolean()); restart(); assertEquals(comparable(moved),comparable(current()));
        request("POST",endpoint("/undo-move"),"{\"combatantId\":\""+actor+"\"}",200);
        JsonObject dashed=json(request("POST",endpoint("/tactical"),"{\"combatantId\":\""+actor+"\",\"action\":\"DASH\"}",200));
        restart(); assertEquals(comparable(dashed),comparable(current()));
        request("POST",endpoint("/tactical"),"{\"combatantId\":\""+actor+"\",\"action\":\"DASH\"}",400);
    }
    @Test void corruptCheckpointIsPreservedUntilTheUserExplicitlyClearsIt() throws Exception {
        create(); sql("UPDATE combat_save SET snapshot='{\"version\":99}'"); restart();
        request("GET","/current",null,409); assertEquals("{\"version\":99}",Combat_Save_DAO.load().snapshot());
        request("DELETE","/current",null,204); request("GET","/current",null,204);
        request("GET",endpoint(""),null,404); create();
    }
    @Test void missingCharacterDoesNotSilentlyDiscardTheEncounter() throws Exception {
        create(); Character_DAO.delete_character(character); clearCache();
        request("GET","/current",null,409); assertNotNull(Combat_Save_DAO.load());
        request("DELETE","/current",null,204); assertNull(Combat_Save_DAO.load());
    }
    @Test void restoreReloadsTheRosterIfAnEarlierStorageFailureEmptiedTheLivePool() throws Exception {
        create(); String expected=sessionId; clearCache(); Global_Data.character_pool.clear();
        assertEquals(expected,current().get("sessionId").getAsString());
        assertEquals(1,Global_Data.character_pool.size());
    }
    @Test void failingCheckpointWriteRollsBackCharacterUpdatesAndKeepsPreviousSave() throws Exception {
        create(); String snapshot=Combat_Save_DAO.load().snapshot(); int hp=character.current_hp;
        sql("CREATE TRIGGER fail_checkpoint BEFORE UPDATE ON combat_save BEGIN SELECT RAISE(ABORT,'test failure'); END");
        character.current_hp=1;
        assertThrows(java.sql.SQLException.class,() -> Combat_Save_DAO.save(sessionId,"bad save",List.of(character)));
        assertEquals(snapshot,Combat_Save_DAO.load().snapshot());
        Character_DAO.load_all_characters(); assertEquals(hp,Global_Data.character_pool.get(0).current_hp);
    }
    @Test void failedOperationEvictsUnsavedMemoryAndReloadsTheLastSuccessfulTurn() throws Exception {
        create(); request("POST",endpoint("/begin"),"{}",200); JsonObject before=current();
        sql("CREATE TRIGGER fail_checkpoint BEFORE UPDATE ON combat_save BEGIN SELECT RAISE(ABORT,'test failure'); END");
        request("POST",endpoint("/tactical"),"{\"action\":\"DASH\"}",500);
        assertEquals(comparable(before),comparable(current()));
        sql("DROP TRIGGER fail_checkpoint"); request("POST",endpoint("/tactical"),"{\"action\":\"DASH\"}",200);
    }
    @Test void victoryAndLootAllocationRemainFinalAcrossRestarts() throws Exception {
        create(); request("POST",endpoint("/begin"),"{}",200);
        while(!active(current()).get("side").getAsString().equals("PLAYER"))request("POST",endpoint("/skip"),"{}",200);
        var cache=CombatController.class.getDeclaredField("SESSIONS");cache.setAccessible(true);
        Object session=((Map<?,?>)cache.get(null)).get(sessionId); var field=session.getClass().getDeclaredField("engine");field.setAccessible(true);
        Combat_Engine engine=(Combat_Engine)field.get(session); Combatant actor=engine.get_active_combatant();
        actor.attack_options.add(Attack_Option.auto_hit("test finisher","",1,1,10000,"force").with_range(120));
        String enemy=current().getAsJsonArray("combatants").asList().stream().map(v -> v.getAsJsonObject()).filter(v -> v.get("side").getAsString().equals("ENEMY")).findFirst().orElseThrow().get("id").getAsString();
        request("POST",endpoint("/actions"),"{\"optionIndex\":"+(actor.attack_options.size()-1)+",\"targetId\":\""+enemy+"\"}",200);
        int xp=character.experience_points; JsonObject won=current(); assertTrue(won.get("victory").getAsBoolean());
        restart(); assertEquals(xp,character.experience_points); assertEquals(comparable(won),comparable(current()));
        JsonObject assigned=json(request("POST",endpoint("/loot"),"{\"receiverCharacterId\":"+character.database_id+"}",200));
        int items=character.inventory_item_counts.values().stream().mapToInt(Integer::intValue).sum();
        restart(); assertEquals(comparable(assigned),comparable(current()));
        request("POST",endpoint("/loot"),"{\"receiverCharacterId\":"+character.database_id+"}",200);
        assertEquals(items,character.inventory_item_counts.values().stream().mapToInt(Integer::intValue).sum()); assertEquals(xp,character.experience_points);
    }
}
