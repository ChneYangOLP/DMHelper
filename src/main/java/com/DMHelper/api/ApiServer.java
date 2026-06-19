package com.DMHelper.api;

import io.javalin.Javalin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ApiServer {
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static void start(int port) {
        Javalin app = Javalin.create().start(port);

        // Simple CORS filter
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "*");
        });
        app.options("/*", ctx -> ctx.status(200));

        app.get("/api/ping", ctx -> ctx.result("pong"));
        
        // Character API
        app.get("/api/characters", ctx -> {
            ctx.result(GSON.toJson(com.DMHelper.basic.database.Global_Data.character_pool))
               .contentType("application/json");
        });
        app.get("/api/characters/{id}", CharacterController::getCharacterById);
        app.put("/api/characters/{id}", CharacterController::updateCharacter);
        app.post("/api/characters", CharacterController::createCharacter);
        app.post("/api/characters/{id}/equipment", CharacterController::equipItem);
        app.post("/api/characters/{id}/inventory", CharacterController::manageInventory);
        app.post("/api/characters/{id}/rest", CharacterController::rest);
        app.post("/api/characters/{id}/xp", CharacterController::addXp);
        app.post("/api/characters/{id}/level-up", CharacterController::levelUp);
        app.post("/api/characters/{id}/spells", CharacterController::manageSpells);

        app.get("/api/items", ItemController::getAllItems);
        app.get("/api/spells", SpellController::getAvailableSpells);

        System.out.println("[API Server] Started on port " + port);
    }
}
