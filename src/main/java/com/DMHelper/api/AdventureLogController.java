package com.DMHelper.api;

import com.DMHelper.basic.database.Adventure_Log_DAO;
import com.DMHelper.basic.database.Adventure_Atlas_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.database.Adventure_Session_DAO;
import com.DMHelper.basic.journal.Adventure_Log_Entry;

import java.sql.SQLException;
import java.util.Set;

public final class AdventureLogController {
    private static final Set<String> ENTRY_TYPES = Set.of("EVENT", "QUEST", "NPC");
    private static final Set<String> QUEST_STATUSES = Set.of("ACTIVE", "COMPLETED", "FAILED");

    private AdventureLogController() {}

    public static void list(ApiExchange ctx) {
        try {
            json(ctx, Adventure_Log_DAO.find_all());
        } catch (SQLException error) {
            databaseError(ctx, error);
        }
    }

    public static void create(ApiExchange ctx) {
        try {
            Adventure_Log_Entry entry = ApiServer.GSON.fromJson(ctx.body(), Adventure_Log_Entry.class);
            String validation = validate(entry);
            if (validation != null) {
                ctx.status(400).result(validation);
                return;
            }
            normalize(entry);
            entry.combat_source_id = null;
            json(ctx.status(201), Adventure_Log_DAO.create(entry));
        } catch (SQLException error) {
            databaseError(ctx, error);
        }
    }

    public static void update(ApiExchange ctx) {
        try {
            Adventure_Log_Entry entry = ApiServer.GSON.fromJson(ctx.body(), Adventure_Log_Entry.class);
            if (entry == null) {
                ctx.status(400).result("缺少日志内容");
                return;
            }
            entry.id = Integer.parseInt(ctx.pathParam("id"));
            String validation = validate(entry);
            if (validation != null) {
                ctx.status(400).result(validation);
                return;
            }
            normalize(entry);
            Adventure_Log_Entry updated = Adventure_Log_DAO.update(entry);
            if (updated == null) ctx.status(404).result("日志不存在");
            else json(ctx, updated);
        } catch (NumberFormatException error) {
            ctx.status(400).result("无效的日志 ID");
        } catch (SQLException error) {
            databaseError(ctx, error);
        }
    }

    public static void delete(ApiExchange ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (Adventure_Log_DAO.delete(id)) ctx.status(204);
            else ctx.status(404).result("日志不存在");
        } catch (NumberFormatException error) {
            ctx.status(400).result("无效的日志 ID");
        } catch (SQLException error) {
            databaseError(ctx, error);
        }
    }

    private static String validate(Adventure_Log_Entry entry) throws SQLException {
        if (entry == null) return "缺少日志内容";
        if (entry.title == null || entry.title.trim().isEmpty()) return "标题不能为空";
        if (entry.title.trim().length() > 120) return "标题不能超过 120 个字符";
        if (entry.entry_type == null || !ENTRY_TYPES.contains(entry.entry_type)) return "无效的日志类型";
        if (entry.location_id != null && Adventure_Atlas_DAO.location(entry.location_id) == null) return "关联地点不存在";
        if (entry.adventure_session_id != null && Adventure_Session_DAO.get(entry.adventure_session_id) == null) return "关联场次不存在";
        if (entry.character_id != null && Global_Data.character_pool.stream()
                .noneMatch(character -> character.database_id == entry.character_id)) return "关联角色不存在";
        if ("QUEST".equals(entry.entry_type) && (entry.quest_status == null || !QUEST_STATUSES.contains(entry.quest_status))) {
            return "请选择有效的任务状态";
        }
        return null;
    }

    private static void normalize(Adventure_Log_Entry entry) throws SQLException {
        entry.title = entry.title.trim();
        entry.content = clean(entry.content);
        entry.location = clean(entry.location);
        if (entry.location_id != null) {
            var location = Adventure_Atlas_DAO.location(entry.location_id);
            if (location != null) entry.location = location.name;
        }
        entry.tags = clean(entry.tags);
        entry.event_date = clean(entry.event_date);
        if (!"QUEST".equals(entry.entry_type)) entry.quest_status = "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void json(ApiExchange ctx, Object payload) {
        ctx.contentType("application/json").result(ApiServer.GSON.toJson(payload));
    }

    private static void databaseError(ApiExchange ctx, SQLException error) {
        error.printStackTrace();
        ctx.status(500).result("冒险日志数据库操作失败");
    }
}
