package com.DMHelper.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.UUID;

import static com.DMHelper.api.ApiExchange.handle;

@SpringBootApplication(proxyBeanMethods = false)
public class ApiServer {
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String CLIENT_TOKEN = resolveClientToken();

    public static ConfigurableApplicationContext start(int port) {
        return new SpringApplicationBuilder(ApiServer.class).headless(false).run(
                "--server.address=127.0.0.1",
                "--server.port=" + port,
                "--spring.main.banner-mode=off"
        );
    }

    @Bean
    DesktopClientFilter desktopClientFilter() {
        return new DesktopClientFilter(CLIENT_TOKEN);
    }

    @Bean
    RouterFunction<ServerResponse> apiRoutes() {
        return RouterFunctions.route()
                .GET("/api/ping", handle(ctx -> ctx.result("pong")))

                // Character API
                .GET("/api/characters", handle(CharacterController::getAllCharacters))
                .GET("/api/characters/{id}", handle(CharacterController::getCharacterById))
                .PUT("/api/characters/{id}", handle(CharacterController::updateCharacter))
                .POST("/api/characters", handle(CharacterController::createCharacter))
                .POST("/api/characters/{id}/equipment", handle(CharacterController::equipItem))
                .POST("/api/characters/{id}/inventory", handle(CharacterController::manageInventory))
                .POST("/api/characters/{id}/rest", handle(CharacterController::rest))
                .POST("/api/characters/{id}/xp", handle(CharacterController::addXp))
                .POST("/api/characters/{id}/level-up", handle(CharacterController::levelUp))
                .POST("/api/characters/{id}/advancement-choice", handle(CharacterController::applyAdvancementChoice))
                .POST("/api/characters/{id}/spells", handle(CharacterController::manageSpells))

                .GET("/api/items", handle(ItemController::getAllItems))
                .GET("/api/spells", handle(SpellController::getAvailableSpells))

                // Combat API (adapts the original Java combat engine for Electron)
                .GET("/api/combat/setup", handle(CombatController::getSetup))
                .GET("/api/combat/current", handle(CombatController::getCurrent))
                .DELETE("/api/combat/current", handle(CombatController::clearCurrent))
                .POST("/api/combat/sessions", handle(CombatController::start))
                .GET("/api/combat/sessions/{id}", handle(CombatController::getState))
                .POST("/api/combat/sessions/{id}/deployment", handle(CombatController::deploy))
                .POST("/api/combat/sessions/{id}/begin", handle(CombatController::begin))
                .POST("/api/combat/sessions/{id}/move", handle(CombatController::move))
                .POST("/api/combat/sessions/{id}/undo-move", handle(CombatController::undoMove))
                .POST("/api/combat/sessions/{id}/tactical", handle(CombatController::tactical))
                .POST("/api/combat/sessions/{id}/reaction", handle(CombatController::reaction))
                .POST("/api/combat/sessions/{id}/area-preview", handle(CombatController::previewArea))
                .POST("/api/combat/sessions/{id}/actions", handle(CombatController::act))
                .POST("/api/combat/sessions/{id}/skip", handle(CombatController::skip))
                .POST("/api/combat/sessions/{id}/loot", handle(CombatController::assignLoot))
                .GET("/api/combat/sessions/{id}/archive", handle(CombatController::archivePreview))
                .POST("/api/combat/sessions/{id}/archive", handle(CombatController::archive))

                .GET("/api/adventure-sessions", handle(AdventureSessionController::list))
                .POST("/api/adventure-sessions", handle(AdventureSessionController::start))
                .PUT("/api/adventure-sessions/{id}", handle(AdventureSessionController::update))
                .POST("/api/adventure-sessions/{id}/finish", handle(AdventureSessionController::finish))
                .GET("/api/adventure-recaps", handle(AdventureRecapController::list))
                .GET("/api/adventure-recaps/{id}", handle(AdventureRecapController::get))
                .POST("/api/adventure-recaps", handle(AdventureRecapController::create))
                .PUT("/api/adventure-recaps/{id}", handle(AdventureRecapController::update))

                .GET("/api/locations", handle(AdventureAtlasController::locations))
                .POST("/api/locations", handle(AdventureAtlasController::createLocation))
                .PUT("/api/locations/{id}", handle(AdventureAtlasController::updateLocation))
                .DELETE("/api/locations/{id}", handle(AdventureAtlasController::deleteLocation))
                .GET("/api/maps", handle(AdventureAtlasController::maps))
                .POST("/api/maps", handle(AdventureAtlasController::createMap))
                .GET("/api/maps/{id}", handle(AdventureAtlasController::map))
                .PUT("/api/maps/{id}", handle(AdventureAtlasController::renameMap))
                .DELETE("/api/maps/{id}", handle(AdventureAtlasController::deleteMap))
                .GET("/api/maps/{id}/markers", handle(AdventureAtlasController::markers))
                .PUT("/api/maps/{id}/markers", handle(AdventureAtlasController::saveMarker))
                .DELETE("/api/maps/{id}/markers/{locationId}", handle(AdventureAtlasController::deleteMarker))

                // Adventure journal API
                .GET("/api/adventure-logs", handle(AdventureLogController::list))
                .POST("/api/adventure-logs", handle(AdventureLogController::create))
                .PUT("/api/adventure-logs/{id}", handle(AdventureLogController::update))
                .DELETE("/api/adventure-logs/{id}", handle(AdventureLogController::delete))

                .build();
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
}
