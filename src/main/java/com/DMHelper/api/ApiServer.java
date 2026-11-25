package com.DMHelper.api;

import io.javalin.Javalin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.UUID;

public class ApiServer {
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String CLIENT_TOKEN = resolveClientToken();
    private static final String CLIENT_TOKEN_HEADER = "X-DMHelper-Token";

    public static void start(int port) {
        Javalin app = Javalin.create(config -> {
            config.jetty.defaultHost = "127.0.0.1";
            config.http.maxRequestSize = 10_000_000L;
        }).start(port);

        // The API is intended for the bundled Electron renderer only. Binding to loopback
        // prevents LAN access, while the per-launch token prevents arbitrary web pages from
        // mutating local data through localhost requests.
        app.before(ctx -> {
            String origin = ctx.header("Origin");
            if (!isAllowedOrigin(origin)) {
                ctx.status(403).result("Origin not allowed").skipRemainingHandlers();
                return;
            }

            if (origin != null) {
                ctx.header("Access-Control-Allow-Origin", origin);
                ctx.header("Vary", "Origin");
            }
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, " + CLIENT_TOKEN_HEADER);
            ctx.header("Access-Control-Max-Age", "600");

            if (ctx.method() == io.javalin.http.HandlerType.OPTIONS) {
                return;
            }
            if (!CLIENT_TOKEN.equals(ctx.header(CLIENT_TOKEN_HEADER))) {
                ctx.status(401).result("Unauthorized client").skipRemainingHandlers();
            }
        });
        app.options("/*", ctx -> ctx.status(204));

        app.get("/api/ping", ctx -> ctx.result("pong"));
        
        // Character API
        app.get("/api/characters", CharacterController::getAllCharacters);
        app.get("/api/characters/{id}", CharacterController::getCharacterById);
        app.put("/api/characters/{id}", CharacterController::updateCharacter);
        app.post("/api/characters", CharacterController::createCharacter);
        app.post("/api/characters/{id}/equipment", CharacterController::equipItem);
        app.post("/api/characters/{id}/inventory", CharacterController::manageInventory);
        app.post("/api/characters/{id}/rest", CharacterController::rest);
        app.post("/api/characters/{id}/xp", CharacterController::addXp);
        app.post("/api/characters/{id}/level-up", CharacterController::levelUp);
        app.post("/api/characters/{id}/advancement-choice", CharacterController::applyAdvancementChoice);
        app.post("/api/characters/{id}/spells", CharacterController::manageSpells);

        app.get("/api/items", ItemController::getAllItems);
        app.get("/api/spells", SpellController::getAvailableSpells);

        // Combat API (adapts the original Java combat engine for Electron)
        app.get("/api/combat/setup", CombatController::getSetup);
        app.get("/api/combat/current", CombatController::getCurrent);
        app.delete("/api/combat/current", CombatController::clearCurrent);
        app.post("/api/combat/sessions", CombatController::start);
        app.get("/api/combat/sessions/{id}", CombatController::getState);
        app.post("/api/combat/sessions/{id}/deployment", CombatController::deploy);
        app.post("/api/combat/sessions/{id}/begin", CombatController::begin);
        app.post("/api/combat/sessions/{id}/move", CombatController::move);
        app.post("/api/combat/sessions/{id}/undo-move", CombatController::undoMove);
        app.post("/api/combat/sessions/{id}/tactical", CombatController::tactical);
        app.post("/api/combat/sessions/{id}/reaction", CombatController::reaction);
        app.post("/api/combat/sessions/{id}/area-preview", CombatController::previewArea);
        app.post("/api/combat/sessions/{id}/actions", CombatController::act);
        app.post("/api/combat/sessions/{id}/skip", CombatController::skip);
        app.post("/api/combat/sessions/{id}/loot", CombatController::assignLoot);
        app.get("/api/combat/sessions/{id}/archive", CombatController::archivePreview);
        app.post("/api/combat/sessions/{id}/archive", CombatController::archive);

        app.get("/api/adventure-sessions", AdventureSessionController::list);
        app.post("/api/adventure-sessions", AdventureSessionController::start);
        app.put("/api/adventure-sessions/{id}", AdventureSessionController::update);
        app.post("/api/adventure-sessions/{id}/finish", AdventureSessionController::finish);
        app.get("/api/adventure-recaps", AdventureRecapController::list);
        app.get("/api/adventure-recaps/{id}", AdventureRecapController::get);
        app.post("/api/adventure-recaps", AdventureRecapController::create);
        app.put("/api/adventure-recaps/{id}", AdventureRecapController::update);

        app.get("/api/locations", AdventureAtlasController::locations);
        app.post("/api/locations", AdventureAtlasController::createLocation);
        app.put("/api/locations/{id}", AdventureAtlasController::updateLocation);
        app.delete("/api/locations/{id}", AdventureAtlasController::deleteLocation);
        app.get("/api/maps", AdventureAtlasController::maps);
        app.post("/api/maps", AdventureAtlasController::createMap);
        app.get("/api/maps/{id}", AdventureAtlasController::map);
        app.put("/api/maps/{id}", AdventureAtlasController::renameMap);
        app.delete("/api/maps/{id}", AdventureAtlasController::deleteMap);
        app.get("/api/maps/{id}/markers", AdventureAtlasController::markers);
        app.put("/api/maps/{id}/markers", AdventureAtlasController::saveMarker);
        app.delete("/api/maps/{id}/markers/{locationId}", AdventureAtlasController::deleteMarker);

        // Adventure journal API
        app.get("/api/adventure-logs", AdventureLogController::list);
        app.post("/api/adventure-logs", AdventureLogController::create);
        app.put("/api/adventure-logs/{id}", AdventureLogController::update);
        app.delete("/api/adventure-logs/{id}", AdventureLogController::delete);

        System.out.println("[API Server] Started on port " + port);
    }

    public static String getClientToken() {
        return CLIENT_TOKEN;
    }

    private static String resolveClientToken() {
        String configuredToken = System.getenv("DMHELPER_API_TOKEN");
        if (configuredToken != null && !configuredToken.trim().isEmpty()) {
            return configuredToken;
        }
        return UUID.randomUUID().toString();
    }

    private static boolean isAllowedOrigin(String origin) {
        return origin == null
                || "null".equals(origin)
                || "http://localhost:5173".equals(origin)
                || "http://127.0.0.1:5173".equals(origin);
    }
}
