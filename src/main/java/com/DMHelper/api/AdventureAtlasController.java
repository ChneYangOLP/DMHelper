package com.DMHelper.api;

import com.DMHelper.basic.atlas.*;
import com.DMHelper.basic.database.Adventure_Atlas_DAO;
import com.google.gson.JsonParseException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Set;

public final class AdventureAtlasController {
    private AdventureAtlasController() {}
    private interface Operation { void run() throws SQLException; }

    private static void handle(ApiExchange ctx, Operation operation) {
        try { operation.run(); }
        catch (IllegalArgumentException | JsonParseException error) { ctx.status(400).result("请求格式无效：" + error.getMessage()); }
        catch (SQLException error) { error.printStackTrace(); ctx.status(500).result("地图与地点数据库操作失败"); }
    }
    private static void json(ApiExchange ctx, Object value) { ctx.contentType("application/json").result(ApiServer.GSON.toJson(value)); }
    private static int id(ApiExchange ctx) { return Integer.parseInt(ctx.pathParam("id")); }
    private static String name(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120) throw new IllegalArgumentException("名称需为 1–120 个字符");
        return value.trim();
    }
    public static void locations(ApiExchange ctx) { handle(ctx, () -> json(ctx, Adventure_Atlas_DAO.locations())); }
    public static void createLocation(ApiExchange ctx) { saveLocation(ctx, true); }
    public static void updateLocation(ApiExchange ctx) { saveLocation(ctx, false); }
    private static void saveLocation(ApiExchange ctx, boolean create) {
        handle(ctx, () -> {
            Adventure_Location entry = ApiServer.GSON.fromJson(ctx.body(), Adventure_Location.class);
            if (entry == null) throw new IllegalArgumentException("缺少地点内容");
            entry.name = name(entry.name);
            if (!Set.of("TOWN", "DUNGEON", "CAMP", "LANDMARK", "OTHER").contains(entry.kind == null ? "" : entry.kind))
                throw new IllegalArgumentException("地点类型无效");
            if (!Set.of("UNKNOWN", "VISITED", "PLANNED").contains(entry.status == null ? "" : entry.status))
                throw new IllegalArgumentException("探索状态无效");
            entry.description = entry.description == null ? "" : entry.description.trim();
            if (entry.description.length() > 20000) throw new IllegalArgumentException("地点描述不能超过 20000 字符");
            if (!create) entry.id = id(ctx);
            Adventure_Location saved = Adventure_Atlas_DAO.saveLocation(entry, create);
            if (saved == null) ctx.status(404).result("地点不存在");
            else json(ctx.status(create ? 201 : 200), saved);
        });
    }
    public static void deleteLocation(ApiExchange ctx) { handle(ctx, () -> ctx.status(Adventure_Atlas_DAO.deleteLocation(id(ctx)) ? 204 : 404)); }
    public static void maps(ApiExchange ctx) { handle(ctx, () -> json(ctx, Adventure_Atlas_DAO.maps())); }
    public static void map(ApiExchange ctx) {
        handle(ctx, () -> {
            Adventure_Map map = Adventure_Atlas_DAO.map(id(ctx));
            if (map == null) ctx.status(404).result("地图不存在"); else json(ctx, map);
        });
    }
    public static void createMap(ApiExchange ctx) {
        handle(ctx, () -> {
            Adventure_Map map = ApiServer.GSON.fromJson(ctx.body(), Adventure_Map.class);
            if (map == null) throw new IllegalArgumentException("缺少地图内容");
            map.name = name(map.name);
            if (map.image_data == null || !map.image_data.startsWith("data:image/webp;base64,") || map.image_data.length() > 8_000_000)
                throw new IllegalArgumentException("请导入有效的地图图片");
            byte[] bytes = Base64.getDecoder().decode(map.image_data.substring(23));
            if (bytes.length < 20 || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F'
                    || bytes[8] != 'W' || bytes[9] != 'E' || bytes[10] != 'B' || bytes[11] != 'P')
                throw new IllegalArgumentException("地图图片格式无效");
            json(ctx.status(201), Adventure_Atlas_DAO.createMap(map));
        });
    }
    public static void renameMap(ApiExchange ctx) {
        handle(ctx, () -> {
            Adventure_Map map = ApiServer.GSON.fromJson(ctx.body(), Adventure_Map.class);
            if (map == null) throw new IllegalArgumentException("缺少地图名称");
            if (Adventure_Atlas_DAO.renameMap(id(ctx), name(map.name))) ctx.status(204); else ctx.status(404).result("地图不存在");
        });
    }
    public static void deleteMap(ApiExchange ctx) { handle(ctx, () -> ctx.status(Adventure_Atlas_DAO.deleteMap(id(ctx)) ? 204 : 404)); }
    public static void markers(ApiExchange ctx) {
        handle(ctx, () -> {
            if (Adventure_Atlas_DAO.map(id(ctx)) == null) ctx.status(404).result("地图不存在");
            else json(ctx, Adventure_Atlas_DAO.markers(id(ctx)));
        });
    }
    public static void saveMarker(ApiExchange ctx) {
        handle(ctx, () -> {
            Map_Marker marker = ApiServer.GSON.fromJson(ctx.body(), Map_Marker.class);
            if (marker == null || marker.x == null || marker.y == null || !Double.isFinite(marker.x) || !Double.isFinite(marker.y)
                    || marker.x < 0 || marker.x > 1 || marker.y < 0 || marker.y > 1) throw new IllegalArgumentException("标记必须位于地图内");
            if (Adventure_Atlas_DAO.saveMarker(id(ctx), marker)) json(ctx, marker);
            else ctx.status(404).result("地图或地点不存在");
        });
    }
    public static void deleteMarker(ApiExchange ctx) {
        handle(ctx, () -> ctx.status(Adventure_Atlas_DAO.deleteMarker(id(ctx), Integer.parseInt(ctx.pathParam("locationId"))) ? 204 : 404));
    }
}
