package com.DMHelper.basic.database;

import com.DMHelper.basic.atlas.Adventure_Location;
import com.DMHelper.basic.atlas.Adventure_Map;
import com.DMHelper.basic.atlas.Map_Marker;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class Adventure_Atlas_DAO {
    private Adventure_Atlas_DAO() {}

    private static Connection connection() throws SQLException {
        Connection conn = DB_Helper.get_connection();
        if (conn == null) throw new SQLException("无法连接本地数据库");
        return conn;
    }

    public static List<Adventure_Location> locations() throws SQLException {
        List<Adventure_Location> entries = new ArrayList<>();
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM adventure_locations ORDER BY is_current DESC, id DESC")) {
            while (rs.next()) entries.add(readLocation(rs));
        }
        return entries;
    }

    public static Adventure_Location location(int id) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM adventure_locations WHERE id=?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? readLocation(rs) : null; }
        }
    }

    private static Adventure_Location readLocation(ResultSet rs) throws SQLException {
        Adventure_Location entry = new Adventure_Location();
        entry.id = rs.getInt("id");
        entry.name = rs.getString("name");
        entry.kind = rs.getString("kind");
        entry.description = rs.getString("description");
        entry.status = rs.getString("status");
        entry.is_current = rs.getBoolean("is_current");
        return entry;
    }

    public static Adventure_Location saveLocation(Adventure_Location entry, boolean create) throws SQLException {
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            try {
                if (!create) {
                    try (PreparedStatement check = conn.prepareStatement("SELECT id FROM adventure_locations WHERE id=?")) {
                        check.setInt(1, entry.id);
                        try (ResultSet rs = check.executeQuery()) { if (!rs.next()) return null; }
                    }
                }
                if (entry.is_current) {
                    try (Statement stmt = conn.createStatement()) { stmt.executeUpdate("UPDATE adventure_locations SET is_current=0"); }
                }
                String sql = create ? "INSERT INTO adventure_locations(name,kind,description,status,is_current) VALUES(?,?,?,?,?)"
                        : "UPDATE adventure_locations SET name=?,kind=?,description=?,status=?,is_current=? WHERE id=?";
                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, entry.name); stmt.setString(2, entry.kind); stmt.setString(3, entry.description);
                    stmt.setString(4, entry.status); stmt.setBoolean(5, entry.is_current);
                    if (!create) stmt.setInt(6, entry.id);
                    stmt.executeUpdate();
                    if (create) try (ResultSet keys = stmt.getGeneratedKeys()) { if (keys.next()) entry.id = keys.getInt(1); }
                }
                // Keep a readable snapshot if the location is later deleted.
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE adventure_logs SET location=? WHERE location_id=?")) {
                    stmt.setString(1, entry.name); stmt.setInt(2, entry.id); stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException error) { conn.rollback(); throw error; }
        }
        return entry;
    }

    public static boolean deleteLocation(int id) throws SQLException {
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            try {
                executeId(conn, "DELETE FROM adventure_map_markers WHERE location_id=?", id);
                executeId(conn, "UPDATE adventure_logs SET location_id=NULL WHERE location_id=?", id);
                boolean removed = executeId(conn, "DELETE FROM adventure_locations WHERE id=?", id) == 1;
                conn.commit();
                return removed;
            } catch (SQLException error) { conn.rollback(); throw error; }
        }
    }

    public static List<Adventure_Map> maps() throws SQLException {
        List<Adventure_Map> entries = new ArrayList<>();
        try (Connection conn = connection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id,name FROM adventure_maps ORDER BY id DESC")) {
            while (rs.next()) {
                Adventure_Map entry = new Adventure_Map();
                entry.id = rs.getInt("id"); entry.name = rs.getString("name"); entries.add(entry);
            }
        }
        return entries;
    }

    public static Adventure_Map map(int id) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM adventure_maps WHERE id=?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                Adventure_Map entry = new Adventure_Map();
                entry.id = rs.getInt("id"); entry.name = rs.getString("name"); entry.image_data = rs.getString("image_data");
                return entry;
            }
        }
    }

    public static Adventure_Map createMap(Adventure_Map entry) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO adventure_maps(name,image_data) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entry.name); stmt.setString(2, entry.image_data); stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) { if (keys.next()) entry.id = keys.getInt(1); }
        }
        return entry;
    }

    public static boolean renameMap(int id, String name) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("UPDATE adventure_maps SET name=? WHERE id=?")) {
            stmt.setString(1, name); stmt.setInt(2, id); return stmt.executeUpdate() == 1;
        }
    }

    public static boolean deleteMap(int id) throws SQLException {
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            try {
                executeId(conn, "DELETE FROM adventure_map_markers WHERE map_id=?", id);
                boolean removed = executeId(conn, "DELETE FROM adventure_maps WHERE id=?", id) == 1;
                conn.commit(); return removed;
            } catch (SQLException error) { conn.rollback(); throw error; }
        }
    }

    public static List<Map_Marker> markers(int mapId) throws SQLException {
        List<Map_Marker> entries = new ArrayList<>();
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM adventure_map_markers WHERE map_id=? ORDER BY location_id")) {
            stmt.setInt(1, mapId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map_Marker entry = new Map_Marker();
                    entry.location_id = rs.getInt("location_id"); entry.x = rs.getDouble("x"); entry.y = rs.getDouble("y"); entries.add(entry);
                }
            }
        }
        return entries;
    }

    public static boolean saveMarker(int mapId, Map_Marker marker) throws SQLException {
        // Existence check and upsert are one statement so deleting a parent cannot leave an orphan.
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO adventure_map_markers(map_id,location_id,x,y) SELECT ?,?,?,? "
                        + "WHERE EXISTS(SELECT 1 FROM adventure_maps WHERE id=?) AND EXISTS(SELECT 1 FROM adventure_locations WHERE id=?) "
                        + "ON CONFLICT(map_id,location_id) DO UPDATE SET x=excluded.x,y=excluded.y")) {
            stmt.setInt(1, mapId); stmt.setInt(2, marker.location_id); stmt.setDouble(3, marker.x); stmt.setDouble(4, marker.y);
            stmt.setInt(5, mapId); stmt.setInt(6, marker.location_id); return stmt.executeUpdate() == 1;
        }
    }

    public static boolean deleteMarker(int mapId, int locationId) throws SQLException {
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM adventure_map_markers WHERE map_id=? AND location_id=?")) {
            stmt.setInt(1, mapId); stmt.setInt(2, locationId); return stmt.executeUpdate() == 1;
        }
    }

    private static int executeId(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) { stmt.setInt(1, id); return stmt.executeUpdate(); }
    }
}
