package com.DMHelper.basic.database;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.armor.Armor;
import com.DMHelper.basic.race.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 角色存档的数据访问层。
 * 角色的主表字段放在 saved_characters 中，职业细节状态统一收敛到 class_state 里，便于扩展更多职业能力。
 */
public class Character_DAO {

    public static void save_character(Character_Sheet character) {
        String sql = "INSERT INTO saved_characters "
                + "(name, age, gender, background_story, personality_traits, ideals, bonds, flaws, race_name, class_name, current_level, experience_points, hp, current_hp, ac, "
                + "gold_pieces, silver_pieces, copper_pieces, "
                + "str, dex, con, intel, wis, cha, armor_name, armor_type, armor_base_ac, has_shield, "
                + "inventory_items, inventory_item_counts, equipped_armor_key, equipped_main_hand_key, equipped_off_hand_key, equipped_cloak_key, equipped_accessory_key, "
                + "skill_proficiencies, feat_names, advancement_notes, class_state, used_asi_choices) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind_character(pstmt, character);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    character.database_id = generatedKeys.getInt(1);
                }
            }

            System.out.println("[系统提示] 角色 [" + character.name + "] 已永久保存至数据库！");
        } catch (SQLException e) {
            System.out.println("[系统提示] 保存角色失败: " + e.getMessage());
        }
    }

    public static void load_all_characters() {
        Global_Data.character_pool.clear();
        String sql = "SELECT * FROM saved_characters ORDER BY id ASC";

        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // 先恢复种族/职业，再恢复职业专属状态，最后再把角色本体字段组装起来。
                Character_Race race = build_race(rs.getString("race_name"));
                Character_Class job = build_job(rs.getString("class_name"));
                job.current_level = rs.getInt("current_level");
                job.used_asi_choices = rs.getInt("used_asi_choices");
                job.skill_proficiencies.addAll(Persistence_Util.decode_list(rs.getString("skill_proficiencies")));
                job.feat_names.addAll(Persistence_Util.decode_list(rs.getString("feat_names")));
                job.import_class_state(Persistence_Util.decode_map(rs.getString("class_state")));

                Stats finalStats = new Stats(
                        rs.getInt("str"), rs.getInt("dex"), rs.getInt("con"),
                        rs.getInt("intel"), rs.getInt("wis"), rs.getInt("cha")
                );

                Character_Sheet loadedChar = Character_Sheet.restore_saved_character(
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        race,
                        job,
                        finalStats
                );

                loadedChar.database_id = rs.getInt("id");
                loadedChar.experience_points = rs.getInt("experience_points");
                loadedChar.gold_pieces = rs.getInt("gold_pieces");
                loadedChar.silver_pieces = rs.getInt("silver_pieces");
                loadedChar.copper_pieces = rs.getInt("copper_pieces");
                loadedChar.background_story = rs.getString("background_story");
                loadedChar.personality_traits = rs.getString("personality_traits");
                loadedChar.ideals = rs.getString("ideals");
                loadedChar.bonds = rs.getString("bonds");
                loadedChar.flaws = rs.getString("flaws");
                loadedChar.advancement_notes.addAll(Persistence_Util.decode_list(rs.getString("advancement_notes")));
                String armorName = rs.getString("armor_name");
                String armorType = rs.getString("armor_type");
                int armorBaseAc = rs.getInt("armor_base_ac");
                loadedChar.restore_equipment_state(
                        Persistence_Util.decode_list(rs.getString("inventory_items")),
                        Persistence_Util.decode_int_map(rs.getString("inventory_item_counts")),
                        rs.getString("equipped_armor_key"),
                        rs.getString("equipped_main_hand_key"),
                        rs.getString("equipped_off_hand_key"),
                        rs.getString("equipped_cloak_key"),
                        rs.getString("equipped_accessory_key"),
                        new Armor(
                                armorName == null || armorName.trim().isEmpty() ? loadedChar.equipped_armor.armor_name : armorName,
                                armorType == null || armorType.trim().isEmpty() ? loadedChar.equipped_armor.armor_type : armorType,
                                armorBaseAc <= 0 ? loadedChar.equipped_armor.base_ac : armorBaseAc
                        ),
                        rs.getInt("has_shield") == 1
                );
                int savedCurrentHp = rs.getInt("current_hp");
                loadedChar.set_current_hp(savedCurrentHp <= 0 ? loadedChar.hp : savedCurrentHp);

                Global_Data.character_pool.add(loadedChar);
            }

            System.out.println("[系统提示] 成功从数据库读取了 " + Global_Data.character_pool.size() + " 个角色存档。");
        } catch (SQLException e) {
            System.out.println("[系统提示] 读取角色存档失败: " + e.getMessage());
        }
    }

    public static void update_character(Character_Sheet character) {
        if (character.database_id <= 0) {
            // 新角色尚未拿到数据库主键时，直接走首次保存。
            save_character(character);
            return;
        }

        String sql = "UPDATE saved_characters SET "
                + "name = ?, age = ?, gender = ?, background_story = ?, personality_traits = ?, ideals = ?, bonds = ?, flaws = ?, race_name = ?, class_name = ?, current_level = ?, experience_points = ?, "
                + "hp = ?, current_hp = ?, ac = ?, gold_pieces = ?, silver_pieces = ?, copper_pieces = ?, str = ?, dex = ?, con = ?, intel = ?, wis = ?, cha = ?, armor_name = ?, armor_type = ?, "
                + "armor_base_ac = ?, has_shield = ?, inventory_items = ?, inventory_item_counts = ?, equipped_armor_key = ?, equipped_main_hand_key = ?, "
                + "equipped_off_hand_key = ?, equipped_cloak_key = ?, equipped_accessory_key = ?, skill_proficiencies = ?, feat_names = ?, advancement_notes = ?, "
                + "class_state = ?, used_asi_choices = ? WHERE id = ?";

        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bind_character_for_update(pstmt, character);
            pstmt.executeUpdate();
            System.out.println("[系统提示] 角色 [" + character.name + "] 的最新状态已同步至数据库！");
        } catch (SQLException e) {
            System.out.println("[系统提示] 更新角色数据失败: " + e.getMessage());
        }
    }

    public static void delete_character(Character_Sheet character) {
        if (character == null) {
            return;
        }

        if (character.database_id <= 0) {
            Global_Data.character_pool.remove(character);
            return;
        }

        String sql = "DELETE FROM saved_characters WHERE id = ?";
        Connection conn = DB_Helper.get_connection();
        if (conn == null) {
            return;
        }

        try (Connection ignored = conn;
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, character.database_id);
            pstmt.executeUpdate();
            Custom_Equipment_DAO.delete_items_for_character(character.database_id);
            Global_Data.character_pool.remove(character);
            System.out.println("[系统提示] 角色 [" + character.name + "] 已从数据库删除。");
        } catch (SQLException e) {
            System.out.println("[系统提示] 删除角色失败: " + e.getMessage());
        }
    }

    private static void bind_character(PreparedStatement pstmt, Character_Sheet character) throws SQLException {
        // 这里同时序列化角色基础字段、装备持有状态、成长记录，以及职业专属状态。
        pstmt.setString(1, character.name);
        pstmt.setInt(2, character.age);
        pstmt.setString(3, character.gender);
        pstmt.setString(4, character.background_story == null ? "" : character.background_story);
        pstmt.setString(5, character.personality_traits == null ? "" : character.personality_traits);
        pstmt.setString(6, character.ideals == null ? "" : character.ideals);
        pstmt.setString(7, character.bonds == null ? "" : character.bonds);
        pstmt.setString(8, character.flaws == null ? "" : character.flaws);
        pstmt.setString(9, character.race.race_name);
        pstmt.setString(10, character.job.class_name);
        pstmt.setInt(11, character.job.current_level);
        pstmt.setInt(12, character.experience_points);
        pstmt.setInt(13, character.hp);
        pstmt.setInt(14, character.current_hp);
        pstmt.setInt(15, character.ac);
        pstmt.setInt(16, character.gold_pieces);
        pstmt.setInt(17, character.silver_pieces);
        pstmt.setInt(18, character.copper_pieces);
        pstmt.setInt(19, character.stats.str);
        pstmt.setInt(20, character.stats.dex);
        pstmt.setInt(21, character.stats.con);
        pstmt.setInt(22, character.stats.intel);
        pstmt.setInt(23, character.stats.wis);
        pstmt.setInt(24, character.stats.cha);
        pstmt.setString(25, character.equipped_armor.armor_name);
        pstmt.setString(26, character.equipped_armor.armor_type);
        pstmt.setInt(27, character.equipped_armor.base_ac);
        pstmt.setInt(28, character.has_shield ? 1 : 0);
        pstmt.setString(29, Persistence_Util.encode_list(character.owned_equipment_keys));
        pstmt.setString(30, Persistence_Util.encode_int_map(character.inventory_item_counts));
        pstmt.setString(31, character.equipped_armor_key == null ? "" : character.equipped_armor_key);
        pstmt.setString(32, character.equipped_main_hand_key == null ? "" : character.equipped_main_hand_key);
        pstmt.setString(33, character.equipped_off_hand_key == null ? "" : character.equipped_off_hand_key);
        pstmt.setString(34, character.equipped_cloak_key == null ? "" : character.equipped_cloak_key);
        pstmt.setString(35, character.equipped_accessory_key == null ? "" : character.equipped_accessory_key);
        pstmt.setString(36, Persistence_Util.encode_list(character.job.skill_proficiencies));
        pstmt.setString(37, Persistence_Util.encode_list(character.job.feat_names));
        pstmt.setString(38, Persistence_Util.encode_list(character.advancement_notes));
        pstmt.setString(39, Persistence_Util.encode_map(character.job.export_class_state()));
        pstmt.setInt(40, character.job.used_asi_choices);
    }

    private static void bind_character_for_update(PreparedStatement pstmt, Character_Sheet character) throws SQLException {
        bind_character(pstmt, character);
        pstmt.setInt(41, character.database_id);
    }

    private static Character_Race build_race(String race_name) {
        return Race_Factory.from_saved_name(race_name);
    }

    private static Character_Class build_job(String class_name) {
        if ("战士 (Fighter)".equals(class_name)) {
            return new Fighter_Class();
        }
        if ("法师 (Wizard)".equals(class_name)) {
            return new Wizard_Class();
        }
        if ("术士 (Sorcerer)".equals(class_name)) {
            return new Sorcerer_Class();
        }
        if ("邪术士 (Warlock)".equals(class_name)) {
            return new Warlock_Class();
        }
        if ("圣武士 (Paladin)".equals(class_name)) {
            return new Paladin_Class();
        }
        return new Fighter_Class();
    }
}
