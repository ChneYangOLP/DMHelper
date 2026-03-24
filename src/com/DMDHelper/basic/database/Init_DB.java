package com.DMDHelper.basic.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Init_DB {

    public static void setup_database() {
        String create_traits_table = "CREATE TABLE IF NOT EXISTS trait_dictionary ("
                + "trait_name TEXT PRIMARY KEY, "
                + "trait_desc TEXT NOT NULL"
                + ");";

        String create_characters_table = "CREATE TABLE IF NOT EXISTS saved_characters ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "age INTEGER DEFAULT 20, "
                + "gender TEXT DEFAULT '未知', "
                + "race_name TEXT NOT NULL, "
                + "class_name TEXT NOT NULL, "
                + "current_level INTEGER DEFAULT 1, "
                + "experience_points INTEGER DEFAULT 0, "
                + "hp INTEGER DEFAULT 0, "
                + "ac INTEGER DEFAULT 0, "
                + "str INTEGER, dex INTEGER, con INTEGER, "
                + "intel INTEGER, wis INTEGER, cha INTEGER, "
                + "armor_name TEXT DEFAULT '普通旅行者服装', "
                + "armor_type TEXT DEFAULT 'None', "
                + "armor_base_ac INTEGER DEFAULT 10, "
                + "has_shield INTEGER DEFAULT 0, "
                + "inventory_items TEXT DEFAULT '', "
                + "equipped_armor_key TEXT DEFAULT '', "
                + "equipped_main_hand_key TEXT DEFAULT '', "
                + "equipped_off_hand_key TEXT DEFAULT '', "
                + "equipped_cloak_key TEXT DEFAULT '', "
                + "equipped_accessory_key TEXT DEFAULT '', "
                + "skill_proficiencies TEXT DEFAULT '', "
                + "feat_names TEXT DEFAULT '', "
                + "advancement_notes TEXT DEFAULT '', "
                + "class_state TEXT DEFAULT '', "
                + "used_asi_choices INTEGER DEFAULT 0"
                + ");";

        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             Statement stmt = conn.createStatement()) {

            stmt.execute(create_traits_table);
            stmt.execute(create_characters_table);

            ensure_column(stmt, "saved_characters", "age", "INTEGER DEFAULT 20");
            ensure_column(stmt, "saved_characters", "gender", "TEXT DEFAULT '未知'");
            ensure_column(stmt, "saved_characters", "experience_points", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "armor_name", "TEXT DEFAULT '普通旅行者服装'");
            ensure_column(stmt, "saved_characters", "armor_type", "TEXT DEFAULT 'None'");
            ensure_column(stmt, "saved_characters", "armor_base_ac", "INTEGER DEFAULT 10");
            ensure_column(stmt, "saved_characters", "has_shield", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "inventory_items", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "equipped_armor_key", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "equipped_main_hand_key", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "equipped_off_hand_key", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "equipped_cloak_key", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "equipped_accessory_key", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "skill_proficiencies", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "feat_names", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "advancement_notes", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "class_state", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "used_asi_choices", "INTEGER DEFAULT 0");

            System.out.println("[系统提示] 数据库表结构初始化成功！");
        } catch (SQLException e) {
            System.out.println("[系统提示] 初始化数据库表失败: " + e.getMessage());
        }
    }

    private static void ensure_column(Statement stmt, String table_name, String column_name, String definition) throws SQLException {
        if (!has_column(stmt, table_name, column_name)) {
            stmt.execute("ALTER TABLE " + table_name + " ADD COLUMN " + column_name + " " + definition);
        }
    }

    private static boolean has_column(Statement stmt, String table_name, String column_name) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table_name + ")")) {
            while (rs.next()) {
                if (column_name.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        setup_database();
    }
}
