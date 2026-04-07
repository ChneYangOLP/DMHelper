package com.DMHelper.basic.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Seed_Dictionary {

    public static void populate_traits() {
        String sql = "INSERT OR IGNORE INTO trait_dictionary (trait_name, trait_desc) VALUES (?, ?)";

        String[][] all_traits = {
                {"复苏之风 (Second Wind)", "你的回合内可用附赠动作恢复 1d10 + 战士等级 的生命值。短休或长休后重新充能。"},
                {"动作如潮 (Action Surge)", "在你的回合内额外进行一个动作。短休或长休后恢复。"},
                {"缴械攻击 (Disarming Attack)", "击中时消耗1颗卓越骰，目标需通过力量豁免，否则掉落手中物品。伤害额外增加卓越骰的数值。"},
                {"绊倒攻击 (Trip Attack)", "击中时消耗1颗卓越骰，大型及以下目标需通过力量豁免，否则倒地。伤害额外增加卓越骰的数值。"},
                {"精准攻击 (Precision Attack)", "攻击检定前或后消耗1颗卓越骰，将数值加到命中判定上。"}
        };

        try (Connection conn = DB_Helper.get_connection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (String[] trait : all_traits) {
                pstmt.setString(1, trait[0]);
                pstmt.setString(2, trait[1]);
                pstmt.executeUpdate();
            }
            System.out.println("[系统提示] 特性词典数据已成功录入数据库！");

        } catch (SQLException e) {
            System.out.println("[系统提示] 录入数据失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        populate_traits();
    }
}
