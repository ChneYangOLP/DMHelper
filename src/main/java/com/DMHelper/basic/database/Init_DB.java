package com.DMHelper.basic.database;

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
                + "background_story TEXT DEFAULT '', "
                + "personality_traits TEXT DEFAULT '', "
                + "ideals TEXT DEFAULT '', "
                + "bonds TEXT DEFAULT '', "
                + "flaws TEXT DEFAULT '', "
                + "race_name TEXT NOT NULL, "
                + "class_name TEXT NOT NULL, "
                + "current_level INTEGER DEFAULT 1, "
                + "experience_points INTEGER DEFAULT 0, "
                + "hp INTEGER DEFAULT 0, "
                + "current_hp INTEGER DEFAULT 0, "
                + "available_hit_dice INTEGER DEFAULT -1, "
                + "ac INTEGER DEFAULT 0, "
                + "gold_pieces INTEGER DEFAULT 0, "
                + "silver_pieces INTEGER DEFAULT 0, "
                + "copper_pieces INTEGER DEFAULT 0, "
                + "str INTEGER, dex INTEGER, con INTEGER, "
                + "intel INTEGER, wis INTEGER, cha INTEGER, "
                + "armor_name TEXT DEFAULT '普通旅行者服装', "
                + "armor_type TEXT DEFAULT 'None', "
                + "armor_base_ac INTEGER DEFAULT 10, "
                + "has_shield INTEGER DEFAULT 0, "
                + "inventory_items TEXT DEFAULT '', "
                + "inventory_item_counts TEXT DEFAULT '', "
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

        String create_custom_items_table = "CREATE TABLE IF NOT EXISTS custom_equipment_items ("
                + "item_key TEXT PRIMARY KEY, "
                + "owner_character_id INTEGER NOT NULL, "
                + "display_name TEXT NOT NULL, "
                + "slot_name TEXT NOT NULL, "
                + "description TEXT DEFAULT '', "
                + "armor_type TEXT DEFAULT '', "
                + "base_ac INTEGER DEFAULT 0, "
                + "shield_bonus INTEGER DEFAULT 0, "
                + "attack_dice_count INTEGER DEFAULT 0, "
                + "attack_die_size INTEGER DEFAULT 0, "
                + "attack_bonus INTEGER DEFAULT 0, "
                + "damage_type TEXT DEFAULT '', "
                + "finesse INTEGER DEFAULT 0, "
                + "ranged INTEGER DEFAULT 0, "
                + "value_in_cp INTEGER DEFAULT 0"
                + ");";

        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             Statement stmt = conn.createStatement()) {

            stmt.execute(create_traits_table);
            stmt.execute(create_characters_table);
            stmt.execute(create_custom_items_table);

            ensure_column(stmt, "saved_characters", "age", "INTEGER DEFAULT 20");
            ensure_column(stmt, "saved_characters", "gender", "TEXT DEFAULT '未知'");
            ensure_column(stmt, "saved_characters", "background_story", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "personality_traits", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "ideals", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "bonds", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "flaws", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "experience_points", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "current_hp", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "available_hit_dice", "INTEGER DEFAULT -1");
            ensure_column(stmt, "saved_characters", "armor_name", "TEXT DEFAULT '普通旅行者服装'");
            ensure_column(stmt, "saved_characters", "armor_type", "TEXT DEFAULT 'None'");
            ensure_column(stmt, "saved_characters", "armor_base_ac", "INTEGER DEFAULT 10");
            ensure_column(stmt, "saved_characters", "has_shield", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "gold_pieces", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "silver_pieces", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "copper_pieces", "INTEGER DEFAULT 0");
            ensure_column(stmt, "saved_characters", "inventory_items", "TEXT DEFAULT ''");
            ensure_column(stmt, "saved_characters", "inventory_item_counts", "TEXT DEFAULT ''");
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

            ensure_column(stmt, "custom_equipment_items", "description", "TEXT DEFAULT ''");
            ensure_column(stmt, "custom_equipment_items", "armor_type", "TEXT DEFAULT ''");
            ensure_column(stmt, "custom_equipment_items", "base_ac", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "shield_bonus", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "attack_dice_count", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "attack_die_size", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "attack_bonus", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "damage_type", "TEXT DEFAULT ''");
            ensure_column(stmt, "custom_equipment_items", "finesse", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "ranged", "INTEGER DEFAULT 0");
            ensure_column(stmt, "custom_equipment_items", "value_in_cp", "INTEGER DEFAULT 0");

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
