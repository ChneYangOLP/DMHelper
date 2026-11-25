package com.DMHelper.api;

import com.DMHelper.basic.database.Adventure_Recap_DAO;
import com.DMHelper.basic.database.Adventure_Session_DAO;
import com.DMHelper.basic.journal.Adventure_Recap;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import io.javalin.http.Context;
import java.sql.SQLException;
import java.util.Set;

public final class AdventureRecapController {
    private AdventureRecapController() {}
    private interface Operation { Object run() throws SQLException; }
    private static void handle(Context ctx, Operation operation) {
        try {
            Object value = operation.run();
            if (value == null) ctx.status(404).result("总结不存在");
            else ctx.contentType("application/json").result(ApiServer.GSON.toJson(value));
        } catch (IllegalArgumentException | JsonParseException error) { ctx.status(400).result(error.getMessage() == null ? "总结格式无效" : error.getMessage()); }
        catch (SQLException error) { ctx.status(500).result("总结保存失败，请检查本机数据库。"); }
    }
    private static Adventure_Recap draft(Context ctx) {
        Adventure_Recap value = ApiServer.GSON.fromJson(ctx.body(), Adventure_Recap.class);
        if (value == null || value.title == null || value.title.isBlank() || value.title.trim().length() > 120)
            throw new IllegalArgumentException("总结标题需为 1–120 个字符。");
        if (value.content == null || value.content.isBlank() || value.content.length() > 200000)
            throw new IllegalArgumentException("总结内容需为 1–200000 个字符。");
        value.title = value.title.trim(); value.content = value.content.trim();
        return value;
    }
    public static void list(Context ctx) { handle(ctx, Adventure_Recap_DAO::list); }
    public static void get(Context ctx) { handle(ctx, () -> Adventure_Recap_DAO.get(Integer.parseInt(ctx.pathParam("id")))); }
    public static void create(Context ctx) {
        handle(ctx, () -> {
            var value = draft(ctx);
            if (!Set.of("recap", "stories", "progress", "destination").contains(value.mode == null ? "" : value.mode)) throw new IllegalArgumentException("总结类型无效。");
            if (value.adventure_session_id != null && Adventure_Session_DAO.get(value.adventure_session_id) == null) throw new IllegalArgumentException("关联场次不存在。");
            value.model = value.model == null ? "" : value.model.trim(); value.focus = value.focus == null ? "" : value.focus.trim();
            if (value.model.length() > 200 || value.focus.length() > 2000) throw new IllegalArgumentException("模型名称或回顾重点过长。");
            if (value.context_snapshot == null || value.context_snapshot.length() > 60000) throw new IllegalArgumentException("原始资料快照过长或缺失。");
            var snapshot = JsonParser.parseString(value.context_snapshot);
            if (!snapshot.isJsonObject()) throw new IllegalArgumentException("原始资料快照无效。");
            for (String key : Set.of("events", "party", "quests", "locations")) {
                var section = snapshot.getAsJsonObject().get(key);
                if (section == null || !section.isJsonArray()) throw new IllegalArgumentException("原始资料快照无效。");
            }
            ctx.status(201); return Adventure_Recap_DAO.create(value);
        });
    }
    public static void update(Context ctx) {
        handle(ctx, () -> { var value = draft(ctx); return Adventure_Recap_DAO.update(Integer.parseInt(ctx.pathParam("id")), value.title, value.content); });
    }
}
