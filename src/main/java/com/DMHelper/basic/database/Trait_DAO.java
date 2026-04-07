package com.DMHelper.basic.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Trait_DAO {

    public static String get_desc(String trait_name) {
        String description = "暂无该特性的详细描述，请检查数据库录入情况。";

        String sql = "SELECT trait_desc FROM trait_dictionary WHERE trait_name = ?";

        try (Connection conn = DB_Helper.get_connection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, trait_name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                description = rs.getString("trait_desc");
            }

        } catch (SQLException e) {
            System.out.println("[系统提示] 查询特性出错: " + e.getMessage());
        }

        return description;
    }
}
