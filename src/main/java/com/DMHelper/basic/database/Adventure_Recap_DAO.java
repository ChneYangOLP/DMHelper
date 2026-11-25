package com.DMHelper.basic.database;

import com.DMHelper.basic.journal.Adventure_Recap;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class Adventure_Recap_DAO {
    private Adventure_Recap_DAO() {}
    private static Connection connection() throws SQLException {
        Connection conn = DB_Helper.get_connection();
        if (conn == null) throw new SQLException("无法连接本地数据库");
        return conn;
    }
    public static List<Adventure_Recap> list() throws SQLException {
        String sql = "SELECT r.id,r.title,r.mode,r.model,r.focus,r.adventure_session_id,r.created_at,r.updated_at,s.title AS session_title "
                + "FROM adventure_recaps r LEFT JOIN adventure_sessions s ON s.id=r.adventure_session_id ORDER BY r.id DESC";
        try (Connection conn = connection(); Statement stmt = conn.createStatement(); ResultSet rows = stmt.executeQuery(sql)) {
            List<Adventure_Recap> result = new ArrayList<>();
            while (rows.next()) result.add(read(rows, false));
            return result;
        }
    }
    public static Adventure_Recap get(int id) throws SQLException {
        String sql = "SELECT r.*,s.title AS session_title FROM adventure_recaps r LEFT JOIN adventure_sessions s ON s.id=r.adventure_session_id WHERE r.id=?";
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rows = stmt.executeQuery()) { return rows.next() ? read(rows, true) : null; }
        }
    }
    public static Adventure_Recap create(Adventure_Recap value) throws SQLException {
        String now = Instant.now().toString();
        String sql = "INSERT INTO adventure_recaps(title,content,mode,model,focus,adventure_session_id,context_snapshot,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)";
        int id;
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, value.title); stmt.setString(2, value.content); stmt.setString(3, value.mode);
            stmt.setString(4, value.model); stmt.setString(5, value.focus);
            if (value.adventure_session_id == null) stmt.setNull(6, Types.INTEGER); else stmt.setInt(6, value.adventure_session_id);
            stmt.setString(7, value.context_snapshot); stmt.setString(8, now); stmt.setString(9, now); stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("未能取得总结编号");
                id = keys.getInt(1);
            }
        }
        return get(id);
    }
    public static Adventure_Recap update(int id, String title, String content) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("UPDATE adventure_recaps SET title=?,content=?,updated_at=? WHERE id=?")) {
            stmt.setString(1, title); stmt.setString(2, content); stmt.setString(3, Instant.now().toString()); stmt.setInt(4, id);
            if (stmt.executeUpdate() != 1) return null;
        }
        return get(id);
    }
    private static Adventure_Recap read(ResultSet rows, boolean full) throws SQLException {
        Adventure_Recap value = new Adventure_Recap();
        value.id = rows.getInt("id"); value.title = rows.getString("title"); value.mode = rows.getString("mode");
        value.model = rows.getString("model"); value.focus = rows.getString("focus");
        Object session = rows.getObject("adventure_session_id"); value.adventure_session_id = session == null ? null : ((Number) session).intValue();
        value.session_title = rows.getString("session_title"); value.created_at = rows.getString("created_at"); value.updated_at = rows.getString("updated_at");
        if (full) { value.content = rows.getString("content"); value.context_snapshot = rows.getString("context_snapshot"); }
        return value;
    }
}
