package com.DMHelper.basic.database;

import com.DMHelper.basic.journal.Adventure_Log_Entry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class Adventure_Log_DAO {
    private Adventure_Log_DAO() {}

    public static List<Adventure_Log_Entry> find_all() throws SQLException {
        String sql = "SELECT l.*, c.name AS character_name, s.title AS session_title FROM adventure_logs l "
                + "LEFT JOIN saved_characters c ON c.id = l.character_id "
                + "LEFT JOIN adventure_sessions s ON s.id = l.adventure_session_id "
                + "ORDER BY l.pinned DESC, l.updated_at DESC, l.id DESC";
        List<Adventure_Log_Entry> entries = new ArrayList<>();
        Connection conn = require_connection();
        try (Connection ignored = conn;
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) entries.add(read(result));
        }
        return entries;
    }

    public static Adventure_Log_Entry create(Adventure_Log_Entry entry) throws SQLException {
        String sql = "INSERT INTO adventure_logs "
                + "(entry_type, title, content, character_id, location, tags, quest_status, event_date, pinned, location_id, adventure_session_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = require_connection();
        try (Connection ignored = conn;
             PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, entry);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) entry.id = keys.getInt(1);
            }
        }
        return find_by_id(entry.id);
    }

    public static Adventure_Log_Entry update(Adventure_Log_Entry entry) throws SQLException {
        String sql = "UPDATE adventure_logs SET entry_type=?, title=?, content=?, character_id=?, location=?, "
                + "tags=?, quest_status=?, event_date=?, pinned=?, location_id=?, adventure_session_id=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
        Connection conn = require_connection();
        try (Connection ignored = conn;
             PreparedStatement statement = conn.prepareStatement(sql)) {
            bind(statement, entry);
            statement.setInt(12, entry.id);
            if (statement.executeUpdate() != 1) return null;
        }
        return find_by_id(entry.id);
    }

    public static boolean delete(int id) throws SQLException {
        Connection conn = require_connection();
        try (Connection ignored = conn;
             PreparedStatement statement = conn.prepareStatement("DELETE FROM adventure_logs WHERE id=?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private static Adventure_Log_Entry find_by_id(int id) throws SQLException {
        String sql = "SELECT l.*, c.name AS character_name, s.title AS session_title FROM adventure_logs l "
                + "LEFT JOIN saved_characters c ON c.id = l.character_id "
                + "LEFT JOIN adventure_sessions s ON s.id = l.adventure_session_id WHERE l.id=?";
        Connection conn = require_connection();
        try (Connection ignored = conn;
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? read(result) : null;
            }
        }
    }

    private static void bind(PreparedStatement statement, Adventure_Log_Entry entry) throws SQLException {
        statement.setString(1, entry.entry_type);
        statement.setString(2, entry.title);
        statement.setString(3, entry.content == null ? "" : entry.content);
        if (entry.character_id == null) statement.setNull(4, Types.INTEGER);
        else statement.setInt(4, entry.character_id);
        statement.setString(5, entry.location == null ? "" : entry.location);
        statement.setString(6, entry.tags == null ? "" : entry.tags);
        statement.setString(7, entry.quest_status == null ? "" : entry.quest_status);
        statement.setString(8, entry.event_date == null ? "" : entry.event_date);
        statement.setInt(9, entry.pinned ? 1 : 0);
        if (entry.location_id == null) statement.setNull(10, Types.INTEGER);
        else statement.setInt(10, entry.location_id);
        if (entry.adventure_session_id == null) statement.setNull(11, Types.INTEGER);
        else statement.setInt(11, entry.adventure_session_id);
    }

    private static Adventure_Log_Entry read(ResultSet result) throws SQLException {
        Adventure_Log_Entry entry = new Adventure_Log_Entry();
        entry.id = result.getInt("id");
        entry.entry_type = result.getString("entry_type");
        entry.title = result.getString("title");
        entry.content = result.getString("content");
        Object characterId = result.getObject("character_id");
        entry.character_id = characterId == null ? null : ((Number) characterId).intValue();
        entry.location = result.getString("location");
        Object locationId = result.getObject("location_id");
        entry.location_id = locationId == null ? null : ((Number) locationId).intValue();
        entry.tags = result.getString("tags");
        entry.quest_status = result.getString("quest_status");
        entry.event_date = result.getString("event_date");
        entry.pinned = result.getInt("pinned") == 1;
        entry.created_at = result.getString("created_at");
        entry.updated_at = result.getString("updated_at");
        entry.character_name = result.getString("character_name");
        Object sessionId = result.getObject("adventure_session_id");
        entry.adventure_session_id = sessionId == null ? null : ((Number) sessionId).intValue();
        entry.session_title = result.getString("session_title");
        entry.combat_source_id = result.getString("combat_source_id");
        return entry;
    }

    public static Adventure_Log_Entry find_by_combat_source(String source) throws SQLException {
        try (Connection conn = require_connection()) { return find_by_combat_source(conn, source); }
    }

    private static Adventure_Log_Entry find_by_combat_source(Connection conn, String source) throws SQLException {
        String sql = "SELECT l.*,c.name AS character_name,s.title AS session_title FROM adventure_logs l "
                + "LEFT JOIN saved_characters c ON c.id=l.character_id "
                + "LEFT JOIN adventure_sessions s ON s.id=l.adventure_session_id WHERE l.combat_source_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, source);
            try (ResultSet rows = stmt.executeQuery()) { return rows.next() ? read(rows) : null; }
        }
    }

    public static Adventure_Log_Entry archive_combat(Adventure_Log_Entry entry, String source) throws SQLException {
        String sql = "INSERT INTO adventure_logs(entry_type,title,content,character_id,location,tags,quest_status,event_date,pinned,location_id,adventure_session_id,combat_source_id) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        try (Connection conn = require_connection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) { bind(stmt, entry); stmt.setString(12, source); stmt.executeUpdate(); }
                Adventure_Log_Entry saved = find_by_combat_source(conn, source);
                if (saved == null) throw new SQLException("未能保存战斗归档");
                conn.commit(); return saved;
            } catch (SQLException error) { conn.rollback(); throw error; }
        }
    }

    private static Connection require_connection() throws SQLException {
        Connection connection = DB_Helper.get_connection();
        if (connection == null) throw new SQLException("无法连接本地数据库");
        return connection;
    }
}
