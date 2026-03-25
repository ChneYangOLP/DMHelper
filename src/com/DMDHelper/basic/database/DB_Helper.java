package com.DMDHelper.basic.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// 数据库连接助手类
public class DB_Helper {
    private static final String DB_URL = "jdbc:sqlite:dnd_data.db";

    // 获取数据库连接的静态方法
    public static Connection get_connection() {
        Connection conn = null;
        try {
            // 通过 DriverManager 获取连接
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("[系统提示] 成功连接到本地 SQLite 数据库！");
        } catch (SQLException e) {
            System.out.println("[系统提示] 数据库连接失败: " + e.getMessage());
        }
        return conn;
    }
}