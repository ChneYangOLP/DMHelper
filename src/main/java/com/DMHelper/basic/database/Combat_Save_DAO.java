package com.DMHelper.basic.database;

import com.DMHelper.basic.Character_Sheet;
import java.sql.*;
import java.time.Instant;
import java.util.List;

/** One current encounter; character changes and its checkpoint commit together. */
public final class Combat_Save_DAO {
    private Combat_Save_DAO() {}
    public record Saved(String id, String snapshot, String savedAt, Integer adventureSessionId, Integer locationId) {
        public Saved(String id, String snapshot, String savedAt) { this(id, snapshot, savedAt, null, null); }
    }
    private static Connection connection() throws SQLException {
        Connection conn = DB_Helper.get_connection();
        if (conn == null) throw new SQLException("无法连接本地数据库");
        return conn;
    }
    public static Saved load() throws SQLException {
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT session_id,snapshot,saved_at,adventure_session_id,location_id FROM combat_save WHERE slot=1")) {
            if (!rs.next()) return null;
            Object session = rs.getObject(4), location = rs.getObject(5);
            return new Saved(rs.getString(1), rs.getString(2), rs.getString(3), session == null ? null : ((Number) session).intValue(), location == null ? null : ((Number) location).intValue());
        }
    }
    public static String save(String id, String snapshot, List<Character_Sheet> characters) throws SQLException {
        return save(id, snapshot, characters, null, null);
    }
    public static String save(String id, String snapshot, List<Character_Sheet> characters, Integer adventureSessionId, Integer locationId) throws SQLException {
        String savedAt = Instant.now().toString();
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            try {
                for (Character_Sheet character : characters) Character_DAO.update_character(conn, character);
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO combat_save(slot,session_id,snapshot,saved_at,adventure_session_id,location_id) VALUES(1,?,?,?,?,?) "
                        + "ON CONFLICT(slot) DO UPDATE SET session_id=excluded.session_id,snapshot=excluded.snapshot,saved_at=excluded.saved_at,adventure_session_id=excluded.adventure_session_id,location_id=excluded.location_id")) {
                    stmt.setString(1, id); stmt.setString(2, snapshot); stmt.setString(3, savedAt);
                    if (adventureSessionId == null) stmt.setNull(4, Types.INTEGER); else stmt.setInt(4, adventureSessionId);
                    if (locationId == null) stmt.setNull(5, Types.INTEGER); else stmt.setInt(5, locationId);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException error) { conn.rollback(); throw error; }
        }
        return savedAt;
    }
    public static boolean delete(String id) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM combat_save WHERE slot=1 AND session_id=?")) {
            stmt.setString(1, id); return stmt.executeUpdate() == 1;
        }
    }
}
