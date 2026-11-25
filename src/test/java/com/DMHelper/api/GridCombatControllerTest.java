package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.database.Init_DB;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.race.Human_Race;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridCombatControllerTest {
    @TempDir Path directory;
    ApiTestServer app;
    List<Character_Sheet> savedPool;
    String previousDataDirectory;
    final HttpClient client = HttpClient.newHttpClient();
    String sessionId;

    @BeforeEach void setup() throws Exception {
        previousDataDirectory = System.getProperty("dmhelper.data.dir");
        System.setProperty("dmhelper.data.dir", directory.toString());
        Files.createFile(directory.resolve("dnd_data.db"));
        Init_DB.setup_database();
        savedPool = List.copyOf(Global_Data.character_pool);
        Global_Data.character_pool.clear();
        Character_Sheet actor = Character_Sheet.restore_saved_character("Grid test", 20, "", new Human_Race(), new Fighter_Class(), new Stats(10,10,10,10,10,10));
        actor.hp = actor.current_hp = 100;
        assertTrue(Character_DAO.save_character(actor));
        Global_Data.character_pool.add(actor);
        app = new ApiTestServer();
        JsonObject request = new JsonObject();
        request.add("characterIds", ApiServer.GSON.toJsonTree(List.of(actor.database_id)));
        request.add("monsterKeys", ApiServer.GSON.toJsonTree(List.of("ogre")));
        sessionId = json(post("/sessions", request.toString(), 200)).get("sessionId").getAsString();
    }

    @AfterEach void cleanup() {
        if (app != null) app.close();
        Global_Data.character_pool.clear();
        if (savedPool != null) Global_Data.character_pool.addAll(savedPool);
        if (previousDataDirectory == null) System.clearProperty("dmhelper.data.dir");
        else System.setProperty("dmhelper.data.dir", previousDataDirectory);
    }

    String post(String path, String body, int status) throws Exception {
        var response = client.send(app.request("/api/combat" + path)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, response.statusCode(), response.body());
        return response.body();
    }
    JsonObject json(String value) { return ApiServer.GSON.fromJson(value, JsonObject.class); }
    String endpoint(String suffix) { return "/sessions/"+sessionId+suffix; }
    JsonObject state() throws Exception {
        return json(client.send(app.request("/api/combat" + endpoint("")).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
    }

    @Test void deploymentAndManualTurnFlowAreEnforcedByServer() throws Exception {
        JsonObject initial = state();
        assertFalse(initial.get("started").getAsBoolean());
        assertTrue(initial.getAsJsonArray("actions").isEmpty());
        post(endpoint("/skip"), "{}", 409);
        var tokens = initial.getAsJsonArray("combatants");
        String firstId = tokens.get(0).getAsJsonObject().get("id").getAsString();
        String secondId = tokens.get(1).getAsJsonObject().get("id").getAsString();
        post(endpoint("/deployment"), "{\"combatantId\":\""+firstId+"\",\"x\":4,\"y\":4,\"speedFt\":30,\"footprint\":1}", 200);
        post(endpoint("/deployment"), "{\"combatantId\":\""+secondId+"\",\"x\":5,\"y\":4,\"footprint\":1}", 200);
        post(endpoint("/deployment"), "{\"obstacle\":true,\"x\":4,\"y\":4}", 400);
        post(endpoint("/deployment"), "{\"obstacle\":true,\"x\":7,\"y\":7}", 200);
        post(endpoint("/begin"), "{}", 200);
        post(endpoint("/begin"), "{}", 409);
        post(endpoint("/deployment"), "{\"obstacle\":true,\"x\":7,\"y\":7}", 409);
        post(endpoint("/move"), "{\"combatantId\":\""+secondId+"\",\"x\":6,\"y\":4}", 400);
        var moved = json(post(endpoint("/move"), "{\"combatantId\":\""+firstId+"\",\"x\":4,\"y\":5}", 200));
        assertEquals(25, moved.getAsJsonArray("combatants").get(0).getAsJsonObject().get("remainingMovementFt").getAsInt());
        var attacked = json(post(endpoint("/actions"), "{\"optionIndex\":0,\"targetId\":\""+secondId+"\"}", 200));
        assertTrue(attacked.get("actionSpent").getAsBoolean());
        assertTrue(attacked.getAsJsonArray("combatants").get(0).getAsJsonObject().get("active").getAsBoolean());
        post(endpoint("/actions"), "{\"optionIndex\":0,\"targetId\":\""+secondId+"\"}", 400);
        post(endpoint("/move"), "{\"combatantId\":\""+firstId+"\",\"x\":4,\"y\":6}", 200);
        var ended = json(post(endpoint("/skip"), "{}", 200));
        assertFalse(ended.get("actionSpent").getAsBoolean());
        assertTrue(ended.getAsJsonArray("combatants").get(1).getAsJsonObject().get("active").getAsBoolean());
    }

    @Test void invalidMovementAndAttacksDoNotMutateState() throws Exception {
        JsonObject initial = state();
        String actorId = initial.getAsJsonArray("combatants").get(0).getAsJsonObject().get("id").getAsString();
        String targetId = initial.getAsJsonArray("combatants").get(1).getAsJsonObject().get("id").getAsString();
        post(endpoint("/begin"), "{}", 200);
        post(endpoint("/move"), "{\"combatantId\":\""+actorId+"\",\"x\":-1,\"y\":0}", 400);
        post(endpoint("/move"), "{\"combatantId\":\""+actorId+"\",\"x\":8,\"y\":11}", 400);
        post(endpoint("/actions"), "{\"optionIndex\":0,\"targetId\":\""+targetId+"\"}", 400);
        assertFalse(state().get("actionSpent").getAsBoolean());
        assertEquals(initial.getAsJsonArray("combatants").get(0).getAsJsonObject().get("currentHp"), state().getAsJsonArray("combatants").get(0).getAsJsonObject().get("currentHp"));
    }

    @Test void undoEndpointRestoresMovementAndIsRejectedAfterAnActionOrActorChange() throws Exception {
        JsonObject initial=state();String actorId=initial.getAsJsonArray("combatants").get(0).getAsJsonObject().get("id").getAsString();
        String otherId=initial.getAsJsonArray("combatants").get(1).getAsJsonObject().get("id").getAsString();
        post(endpoint("/deployment"),"{\"combatantId\":\""+actorId+"\",\"x\":2,\"y\":5,\"footprint\":1,\"speedFt\":30}",200);
        post(endpoint("/deployment"),"{\"combatantId\":\""+otherId+"\",\"x\":12,\"y\":8,\"footprint\":1}",200);
        post(endpoint("/begin"),"{}",200);
        var moved=json(post(endpoint("/move"),"{\"combatantId\":\""+actorId+"\",\"x\":3,\"y\":5}",200));
        assertTrue(moved.get("canUndoMove").getAsBoolean());
        post(endpoint("/undo-move"),"{\"combatantId\":\""+otherId+"\"}",409);
        var undone=json(post(endpoint("/undo-move"),"{\"combatantId\":\""+actorId+"\"}",200));
        assertEquals(2,undone.getAsJsonArray("combatants").get(0).getAsJsonObject().get("x").getAsInt());
        assertEquals(30,undone.getAsJsonArray("combatants").get(0).getAsJsonObject().get("remainingMovementFt").getAsInt());
        post(endpoint("/undo-move"),"{}",400);
        post(endpoint("/move"),"{\"combatantId\":\""+actorId+"\",\"x\":3,\"y\":5}",200);
        post(endpoint("/tactical"),"{\"combatantId\":\""+actorId+"\",\"action\":\"DISENGAGE\"}",200);
        assertFalse(state().get("canUndoMove").getAsBoolean());post(endpoint("/undo-move"),"{}",400);
        post(endpoint("/move"),"{\"combatantId\":\""+actorId+"\",\"x\":4,\"y\":5}",200);
        post(endpoint("/undo-move"),"{}",400);post(endpoint("/skip"),"{}",200);
        post(endpoint("/undo-move"),"{\"combatantId\":\""+actorId+"\"}",409);
    }

    @Test void bonusActionLocksUndoOnServerWhileLeavingMainActionAvailable() throws Exception {
        var bardClass=new com.DMHelper.basic.playerclass.bard.Bard_Class();bardClass.known_spell_keys.add("healing_word");
        Character_Sheet bard=Character_Sheet.restore_saved_character("Bard test",20,"",new Human_Race(),bardClass,new Stats(10,10,10,10,10,16));
        assertTrue(Character_DAO.save_character(bard));Global_Data.character_pool.add(bard);
        JsonObject request=new JsonObject();request.add("characterIds",ApiServer.GSON.toJsonTree(List.of(bard.database_id)));request.add("monsterKeys",ApiServer.GSON.toJsonTree(List.of("ogre")));
        assertEquals(204,client.send(app.request("/api/combat/current").DELETE().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        sessionId=json(post("/sessions",request.toString(),200)).get("sessionId").getAsString();
        JsonObject initial=state();String bardId="";
        for(var item:initial.getAsJsonArray("combatants"))if(item.getAsJsonObject().get("side").getAsString().equals("PLAYER"))bardId=item.getAsJsonObject().get("id").getAsString();
        post(endpoint("/deployment"),"{\"combatantId\":\""+bardId+"\",\"x\":2,\"y\":6,\"speedFt\":30}",200);
        post(endpoint("/begin"),"{}",200);
        if(!state().getAsJsonArray("combatants").asList().stream().anyMatch(t->t.getAsJsonObject().get("active").getAsBoolean()&&t.getAsJsonObject().get("side").getAsString().equals("PLAYER")))post(endpoint("/skip"),"{}",200);
        post(endpoint("/move"),"{\"combatantId\":\""+bardId+"\",\"x\":3,\"y\":6}",200);
        assertTrue(state().get("canUndoMove").getAsBoolean());int healingIndex=-1;
        for(var item:state().getAsJsonArray("actions"))if(item.getAsJsonObject().get("name").getAsString().contains("Healing Word"))healingIndex=item.getAsJsonObject().get("index").getAsInt();
        assertTrue(healingIndex>=0);
        post(endpoint("/actions"),"{\"optionIndex\":"+healingIndex+",\"targetId\":\""+bardId+"\",\"combatantId\":\""+bardId+"\"}",200);
        assertTrue(state().get("bonusActionSpent").getAsBoolean());assertFalse(state().get("actionSpent").getAsBoolean());
        assertFalse(state().get("canUndoMove").getAsBoolean());post(endpoint("/undo-move"),"{}",400);
    }
}
