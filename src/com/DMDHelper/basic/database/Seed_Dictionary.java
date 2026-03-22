package com.DMDHelper.basic.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// 字典数据播种机：负责把硬编码的文本灌入数据库
public class Seed_Dictionary {

    public static void populate_traits() {
        // 定义 SQL 插入语句，使用 ? 作为占位符
        String sql = "INSERT OR IGNORE INTO trait_dictionary (trait_name, trait_desc) VALUES (?, ?)";

        // 准备一个二维数组，存放我们要灌入数据库的所有特性和战技
        String[][] all_traits = {
                {"复苏之风 (Second Wind)", "你的回合内可用附赠动作恢复 1d10 + 战士等级 的生命值。短休或长休后重新充能。"},
                {"动作如潮 (Action Surge)", "在你的回合内额外进行一个动作。短休或长休后恢复。"},
                {"缴械攻击 (Disarming Attack)", "击中时消耗1颗卓越骰，目标需通过力量豁免，否则掉落手中物品。伤害额外增加卓越骰的数值。"},
                {"绊倒攻击 (Trip Attack)", "击中时消耗1颗卓越骰，大型及以下目标需通过力量豁免，否则倒地。伤害额外增加卓越骰的数值。"},
                {"精准攻击 (Precision Attack)", "攻击检定前或后消耗1颗卓越骰，将数值加到命中判定上。"}
                // 后续你可以随时在这里添加法术、专长的描述，再次运行即可存入数据库
        };

        try (Connection conn = DB_Helper.get_connection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 遍历数组，将每一对名称和描述填入 SQL 语句中
            for (String[] trait : all_traits) {
                pstmt.setString(1, trait[0]); // 替换第一个 ? (特性名)
                pstmt.setString(2, trait[1]); // 替换第二个 ? (描述)
                pstmt.executeUpdate();        // 执行写入
            }
            System.out.println("[系统提示] 特性词典数据已成功录入数据库！");

        } catch (SQLException e) {
            System.out.println("[系统提示] 录入数据失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 运行前请确保 Init_DB 已经建好了表
        populate_traits();
    }
}