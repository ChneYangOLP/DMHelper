package com.DMHelper.basic.database;

import com.DMHelper.basic.journal.Adventure_Session;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Adventure_Session_DAO {
    private Adventure_Session_DAO() {}
    private static final String SELECT = "SELECT s.*, (SELECT COUNT(*) FROM adventure_logs l WHERE l.adventure_session_id=s.id) AS log_count, "
            + "(SELECT COUNT(*) FROM adventure_recaps r WHERE r.adventure_session_id=s.id) AS recap_count FROM adventure_sessions s ";
    private static Connection connection() throws SQLException {
        Connection conn = DB_Helper.get_connection();
        if (conn == null) throw new SQLException("无法连接本地数据库");
        return conn;
    }
    public static List<Adventure_Session> list() throws SQLException {
        try (Connection conn = connection(); Statement stmt = conn.createStatement(); ResultSet rows = stmt.executeQuery(SELECT + "ORDER BY s.id DESC")) {
            List<Adventure_Session> result = new ArrayList<>();
            while (rows.next()) result.add(read(rows));
            return result;
        }
    }
    public static Adventure_Session get(int id) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE s.id=?")) {
            stmt.setInt(1, id);
            try (ResultSet rows = stmt.executeQuery()) { return rows.next() ? read(rows) : null; }
        }
    }
    public static Integer current_id() throws SQLException {
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
             ResultSet rows = stmt.executeQuery("SELECT id FROM adventure_sessions WHERE status='ACTIVE'")) {
            return rows.next() ? rows.getInt(1) : null;
        }
    }
    public static Adventure_Session start(String title, String notes) throws SQLException {
        int id;
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO adventure_sessions(title,notes,status,started_at) SELECT ?,?,'ACTIVE',? "
                        + "WHERE NOT EXISTS(SELECT 1 FROM adventure_sessions WHERE status='ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, title); stmt.setString(2, notes); stmt.setString(3, Instant.now().toString());
            if (stmt.executeUpdate() != 1) throw new IllegalStateException("已有进行中的场次，请先结束当前场次。");
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("未能取得新场次编号");
                id = keys.getInt(1);
            }
        }
        return get(id);
    }
    public static Adventure_Session update(int id, String title, String notes) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("UPDATE adventure_sessions SET title=?,notes=? WHERE id=?")) {
            stmt.setString(1, title); stmt.setString(2, notes); stmt.setInt(3, id);
            if (stmt.executeUpdate() != 1) return null;
        }
        return get(id);
    }
    public static Adventure_Session finish(int id) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(
                "UPDATE adventure_sessions SET status='COMPLETED',ended_at=? WHERE id=? AND status='ACTIVE'")) {
            stmt.setString(1, Instant.now().toString()); stmt.setInt(2, id); stmt.executeUpdate();
        }
        return get(id);
    }
    private static Adventure_Session read(ResultSet rows) throws SQLException {
        Adventure_Session value = new Adventure_Session();
        value.id = rows.getInt("id"); value.title = rows.getString("title"); value.notes = rows.getString("notes");
        value.status = rows.getString("status"); value.started_at = rows.getString("started_at"); value.ended_at = rows.getString("ended_at");
        value.log_count = rows.getInt("log_count"); value.recap_count = rows.getInt("recap_count");
        return value;
    }
}
