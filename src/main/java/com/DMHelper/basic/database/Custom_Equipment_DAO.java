package com.DMHelper.basic.database;

import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Custom_Equipment_DAO {

    private Custom_Equipment_DAO() {
    }

    public static void load_all_custom_items() {
        Equipment_Library.clear_custom_items();

        String sql = "SELECT * FROM custom_equipment_items ORDER BY owner_character_id ASC, item_key ASC";
        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                try {
                    Equipment_Slot slot = Equipment_Slot.valueOf(rs.getString("slot_name"));
                    Equipment_Library.register_custom_item(new Equipment_Item(
                            rs.getString("item_key"),
                            rs.getString("display_name"),
                            slot,
                            rs.getString("description"),
                            rs.getString("armor_type"),
                            rs.getInt("base_ac"),
                            rs.getInt("shield_bonus"),
                            rs.getInt("attack_dice_count"),
                            rs.getInt("attack_die_size"),
                            rs.getInt("attack_bonus"),
                            rs.getString("damage_type"),
                            rs.getInt("finesse") == 1,
                            rs.getInt("ranged") == 1,
                            rs.getInt("value_in_cp")
                    ));
                } catch (IllegalArgumentException slotError) {
                    // 兼容旧数据或异常记录：无法识别槽位时跳过，避免影响整个物品库加载。
                }
            }
        } catch (SQLException e) {
            System.out.println("[系统提示] 读取自定义物品失败: " + e.getMessage());
        }
    }

    public static boolean save_custom_item(int ownerCharacterId, Equipment_Item item) {
        if (ownerCharacterId <= 0 || item == null || item.key == null || item.key.trim().isEmpty()) {
            return false;
        }

        String sql = "INSERT OR REPLACE INTO custom_equipment_items "
                + "(item_key, owner_character_id, display_name, slot_name, description, armor_type, base_ac, shield_bonus, "
                + "attack_dice_count, attack_die_size, attack_bonus, damage_type, finesse, ranged, value_in_cp) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return false;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.key);
            pstmt.setInt(2, ownerCharacterId);
            pstmt.setString(3, item.display_name);
            pstmt.setString(4, item.slot.name());
            pstmt.setString(5, item.description == null ? "" : item.description);
            pstmt.setString(6, item.armor_type == null ? "" : item.armor_type);
            pstmt.setInt(7, item.base_ac);
            pstmt.setInt(8, item.shield_bonus);
            pstmt.setInt(9, item.attack_dice_count);
            pstmt.setInt(10, item.attack_die_size);
            pstmt.setInt(11, item.attack_bonus);
            pstmt.setString(12, item.damage_type == null ? "" : item.damage_type);
            pstmt.setInt(13, item.finesse ? 1 : 0);
            pstmt.setInt(14, item.ranged ? 1 : 0);
            pstmt.setInt(15, item.value_in_cp);
            pstmt.executeUpdate();
            Equipment_Library.register_custom_item(item);
            return true;
        } catch (SQLException e) {
            System.out.println("[系统提示] 保存自定义物品失败: " + e.getMessage());
            return false;
        }
    }

    public static void delete_items_for_character(int ownerCharacterId) {
        if (ownerCharacterId <= 0) {
            return;
        }

        String sql = "DELETE FROM custom_equipment_items WHERE owner_character_id = ?";
        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ownerCharacterId);
            pstmt.executeUpdate();
            load_all_custom_items();
        } catch (SQLException e) {
            System.out.println("[系统提示] 删除角色自定义物品失败: " + e.getMessage());
        }
    }
}
