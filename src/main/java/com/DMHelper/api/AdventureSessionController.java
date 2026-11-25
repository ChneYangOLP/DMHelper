package com.DMHelper.api;

import com.DMHelper.basic.database.Adventure_Session_DAO;
import com.DMHelper.basic.journal.Adventure_Session;
import com.google.gson.JsonParseException;
import io.javalin.http.Context;
import java.sql.SQLException;

public final class AdventureSessionController {
    private AdventureSessionController() {}
    private interface Operation { Object run() throws SQLException; }
    private static void handle(Context ctx, Operation operation) {
        try {
            Object value = operation.run();
            if (value == null) ctx.status(404).result("场次不存在");
            else ctx.contentType("application/json").result(ApiServer.GSON.toJson(value));
        } catch (IllegalArgumentException | JsonParseException error) { ctx.status(400).result(error.getMessage() == null ? "场次格式无效" : error.getMessage()); }
        catch (IllegalStateException error) { ctx.status(409).result(error.getMessage()); }
        catch (SQLException error) { ctx.status(500).result("场次保存失败，请检查本机数据库。"); }
    }
    private static Adventure_Session draft(Context ctx) {
        Adventure_Session value = ApiServer.GSON.fromJson(ctx.body(), Adventure_Session.class);
        if (value == null || value.title == null || value.title.isBlank() || value.title.trim().length() > 120)
            throw new IllegalArgumentException("场次名称需为 1–120 个字符。");
        value.title = value.title.trim(); value.notes = value.notes == null ? "" : value.notes.trim();
        if (value.notes.length() > 20000) throw new IllegalArgumentException("场次备注不能超过 20000 字。");
        return value;
    }
    public static void list(Context ctx) { handle(ctx, Adventure_Session_DAO::list); }
    public static synchronized void start(Context ctx) {
        handle(ctx, () -> { var value = draft(ctx); ctx.status(201); return Adventure_Session_DAO.start(value.title, value.notes); });
    }
    public static void update(Context ctx) {
        handle(ctx, () -> { var value = draft(ctx); return Adventure_Session_DAO.update(Integer.parseInt(ctx.pathParam("id")), value.title, value.notes); });
    }
    public static synchronized void finish(Context ctx) { handle(ctx, () -> Adventure_Session_DAO.finish(Integer.parseInt(ctx.pathParam("id")))); }
}
