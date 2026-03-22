package com.DMDHelper.basic.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// 数据库连接助手类
public class DB_Helper {

    // 指向你项目根目录下的 SQLite 数据库文件
    // 如果文件不存在，SQLite 驱动会在第一次连接时自动帮你创建一个空的 dnd_data.db
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