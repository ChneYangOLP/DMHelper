package com.DMHelper.basic.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Trait_DAO {

    // 核心方法：传入特性名字，返回数据库里的详细描述
    public static String get_desc(String trait_name) {
        // 如果数据库没查到，默认返回这个提示
        String description = "暂无该特性的详细描述，请检查数据库录入情况。";

        String sql = "SELECT trait_desc FROM trait_dictionary WHERE trait_name = ?";

        try (Connection conn = DB_Helper.get_connection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, trait_name);
            ResultSet rs = pstmt.executeQuery();

            // 如果查到了对应的数据行，提取出 trait_desc 列的内容
            if (rs.next()) {
                description = rs.getString("trait_desc");
            }

        } catch (SQLException e) {
            System.out.println("[系统提示] 查询特性出错: " + e.getMessage());
        }

        return description;
    }
}