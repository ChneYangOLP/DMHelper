package com.DMHelper.api;

import com.DMHelper.basic.database.Init_DB;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.http.*;
import java.nio.file.*;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class AdventureAtlasControllerTest {
    @TempDir Path directory;
    ApiTestServer app;
    String previousDirectory;
    final HttpClient client = HttpClient.newHttpClient();
    static final String IMAGE = "data:image/webp;base64,UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA";

    @BeforeEach void setup() throws Exception {
        previousDirectory = System.getProperty("dmhelper.data.dir");
        System.setProperty("dmhelper.data.dir", directory.toString());
        Files.createFile(directory.resolve("dnd_data.db"));
        Init_DB.setup_database();
        app = new ApiTestServer();
    }
    @AfterEach void cleanup() {
        if (app != null) app.close();
        if (previousDirectory == null) System.clearProperty("dmhelper.data.dir");
        else System.setProperty("dmhelper.data.dir", previousDirectory);
    }
    String request(String method, String path, Object body, int status) throws Exception {
        String json = body instanceof String ? (String) body : ApiServer.GSON.toJson(body);
        var response = client.send(app.request("/api" + path)
                .header("Content-Type", "application/json").method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, response.statusCode(), response.body());
        return response.body();
    }
    JsonObject object(String value) { return ApiServer.GSON.fromJson(value, JsonObject.class); }
    JsonArray list(String path) throws Exception { return ApiServer.GSON.fromJson(request("GET", path, null, 200), JsonArray.class); }
    JsonObject location(String name, boolean current) throws Exception {
        JsonObject body = new JsonObject(); body.addProperty("name", name); body.addProperty("is_current", current);
        return object(request("POST", "/locations", body, 201));
    }
    int map(String name) throws Exception {
        JsonObject body = new JsonObject(); body.addProperty("name", name); body.addProperty("image_data", IMAGE);
        return object(request("POST", "/maps", body, 201)).get("id").getAsInt();
    }
    String pins(int id) { return "/maps/" + id + "/markers"; }

    @Test void currentLocationIsUniqueAndMissingUpdatesDoNotClearIt() throws Exception {
        JsonObject first = location("灰港", true);
        JsonObject second = location("矿井", true);
        assertEquals(1, list("/locations").asList().stream().filter(value -> value.getAsJsonObject().get("is_current").getAsBoolean()).count());
        request("PUT", "/locations/999", second, 404);
        assertTrue(list("/locations").get(0).getAsJsonObject().get("is_current").getAsBoolean());
        first.addProperty("status", "VISITED"); first.addProperty("is_current", true);
        request("PUT", "/locations/" + first.get("id").getAsInt(), first, 200);
        assertEquals("灰港", list("/locations").get(0).getAsJsonObject().get("name").getAsString());
    }
    @Test void markersSupportSeveralMapsAndReplacingCoordinatesDoesNotDuplicate() throws Exception {
        int location = location("遗迹", false).get("id").getAsInt(), first = map("区域图"), second = map("地牢图");
        request("PUT", pins(first), "{\"location_id\":" + location + ",\"x\":0,\"y\":1}", 200);
        request("PUT", pins(second), "{\"location_id\":" + location + ",\"x\":0.5,\"y\":0.5}", 200);
        request("PUT", pins(first), "{\"location_id\":" + location + ",\"x\":0.25,\"y\":0.75}", 200);
        assertEquals(1, list(pins(first)).size());
        assertEquals(.25, list(pins(first)).get(0).getAsJsonObject().get("x").getAsDouble());
        request("DELETE", pins(first) + "/" + location, null, 204);
        assertTrue(list(pins(first)).isEmpty());
        assertEquals(1, list(pins(second)).size());
    }
    @Test void deletingMapKeepsLocationsAndOtherMapMarkers() throws Exception {
        int location = location("营地", false).get("id").getAsInt(), first = map("第一张"), second = map("第二张");
        String marker = "{\"location_id\":" + location + ",\"x\":0.4,\"y\":0.6}";
        request("PUT", pins(first), marker, 200); request("PUT", pins(second), marker, 200);
        request("DELETE", "/maps/" + first, null, 204);
        assertEquals(1, list("/locations").size()); assertEquals(1, list(pins(second)).size());
        request("GET", "/maps/" + first, null, 404);
        request("GET", pins(first), null, 404);
    }
    @Test void journalReferencesFollowRenamesAndSurviveLocationDeletion() throws Exception {
        JsonObject location = location("灰港", false); int id = location.get("id").getAsInt(), map = map("世界");
        request("PUT", pins(map), "{\"location_id\":" + id + ",\"x\":0.3,\"y\":0.5}", 200);
        JsonObject log = object(request("POST", "/adventure-logs", "{\"entry_type\":\"QUEST\",\"title\":\"寻找矿工\",\"quest_status\":\"ACTIVE\",\"location_id\":" + id + "}", 201));
        assertEquals("灰港", log.get("location").getAsString());
        location.addProperty("name", "新灰港"); request("PUT", "/locations/" + id, location, 200);
        assertEquals("新灰港", list("/adventure-logs").get(0).getAsJsonObject().get("location").getAsString());
        request("DELETE", "/locations/" + id, null, 204);
        JsonObject surviving = list("/adventure-logs").get(0).getAsJsonObject();
        assertTrue(surviving.get("location_id").isJsonNull()); assertEquals("新灰港", surviving.get("location").getAsString());
        assertTrue(list(pins(map)).isEmpty());
        assertEquals(1, list("/maps").size());
    }
    @Test void mapMetadataDoesNotReturnImagesAndImageSurvivesDatabaseReinitialization() throws Exception {
        int id = map("旧名字");
        assertTrue(list("/maps").get(0).getAsJsonObject().get("image_data").isJsonNull());
        request("PUT", "/maps/" + id, "{\"name\":\"新名字\"}", 204);
        Init_DB.setup_database();
        JsonObject map = object(request("GET", "/maps/" + id, null, 200));
        assertEquals("新名字", map.get("name").getAsString()); assertEquals(IMAGE, map.get("image_data").getAsString());
    }
    @Test void invalidRequestsAreRejectedWithoutPersistingPartialData() throws Exception {
        for (String body : new String[] {"null", "{", "{\"name\":\" \"}", "{\"name\":\"地点\",\"kind\":null}", "{\"name\":\"地点\",\"status\":\"BAD\"}"})
            request("POST", "/locations", body, 400);
        assertTrue(list("/locations").isEmpty());
        int location = location("测试", false).get("id").getAsInt(), map = map("地图");
        for (String point : new String[] {"\"x\":-0.1,\"y\":0.2", "\"x\":1.1,\"y\":0", "\"x\":null,\"y\":0", "\"x\":\"NaN\",\"y\":0"})
            request("PUT", pins(map), "{\"location_id\":" + location + "," + point + "}", 400);
        request("PUT", pins(map), "{\"location_id\":999,\"x\":0.5,\"y\":0.5}", 404);
        request("PUT", pins(999), "{\"location_id\":" + location + ",\"x\":0.5,\"y\":0.5}", 404);
        request("POST", "/maps", "{\"name\":\"坏图\",\"image_data\":\"data:image/webp;base64,YmFk\"}", 400);
        request("POST", "/adventure-logs", "{\"entry_type\":\"EVENT\",\"title\":\"无效地点\",\"location_id\":999}", 400);
        assertTrue(list(pins(map)).isEmpty()); assertEquals(1, list("/maps").size());
    }
    @Test void journalLocationLinkCanBeChangedOrRemoved() throws Exception {
        int first = location("第一处", false).get("id").getAsInt(), second = location("第二处", false).get("id").getAsInt();
        JsonObject log = object(request("POST", "/adventure-logs", "{\"entry_type\":\"NPC\",\"title\":\"向导\",\"location_id\":" + first + "}", 201));
        log.addProperty("location_id", second);
        log = object(request("PUT", "/adventure-logs/" + log.get("id").getAsInt(), log, 200));
        assertEquals("第二处", log.get("location").getAsString());
        log.add("location_id", null); log.addProperty("location", "手写地点");
        log = object(request("PUT", "/adventure-logs/" + log.get("id").getAsInt(), log, 200));
        assertTrue(log.get("location_id").isJsonNull()); assertEquals("手写地点", log.get("location").getAsString());
    }
    @Test void legacyJournalTableIsMigratedWithoutLosingText() throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("dnd_data.db")); var stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE adventure_logs DROP COLUMN location_id");
            stmt.execute("INSERT INTO adventure_logs(entry_type,title,location) VALUES('EVENT','旧记录','旧地点')");
        }
        Init_DB.setup_database(); Init_DB.setup_database();
        JsonObject old = list("/adventure-logs").get(0).getAsJsonObject();
        assertEquals("旧地点", old.get("location").getAsString()); assertTrue(old.get("location_id").isJsonNull());
    }
}
