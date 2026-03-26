package com.DMHelper.basic.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * SQLite 连接入口。
 * 打包成桌面应用后，工作目录可能落在临时路径里，所以数据库不能再依赖相对路径。
 * 这里统一把数据库放到用户目录下的应用数据文件夹，并在首次启动时尝试迁移旧版相对路径数据库。
 */
public class DB_Helper {
    private static final String DB_FILE_NAME = "dnd_data.db";
    private static final String APP_FOLDER_NAME = "DMHelper";
    private static String cached_db_url;

    public static Connection get_connection() {
        Connection conn = null;
        try {
            String dbUrl = get_db_url();
            conn = DriverManager.getConnection(dbUrl);
            System.out.println("[系统提示] 成功连接到本地 SQLite 数据库！路径: " + get_database_path());
        } catch (SQLException e) {
            System.out.println("[系统提示] 数据库连接失败: " + e.getMessage());
        }
        return conn;
    }

    private static String get_db_url() {
        if (cached_db_url != null) {
            return cached_db_url;
        }

        Path databasePath = get_database_path();
        // 先保证目录存在，再做旧数据库迁移，最后再拼 JDBC URL。
        ensure_database_parent(databasePath);
        migrate_legacy_database_if_needed(databasePath);
        cached_db_url = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        return cached_db_url;
    }

    private static Path get_database_path() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");

        // mac / Windows / Linux 各自写到更符合桌面应用习惯的位置。
        if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", APP_FOLDER_NAME, DB_FILE_NAME);
        }
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.trim().isEmpty()) {
                return Paths.get(appData, APP_FOLDER_NAME, DB_FILE_NAME);
            }
            return Paths.get(userHome, "AppData", "Roaming", APP_FOLDER_NAME, DB_FILE_NAME);
        }
        return Paths.get(userHome, ".local", "share", APP_FOLDER_NAME, DB_FILE_NAME);
    }

    private static void ensure_database_parent(Path databasePath) {
        try {
            Files.createDirectories(databasePath.getParent());
        } catch (IOException e) {
            System.out.println("[系统提示] 创建数据库目录失败: " + e.getMessage());
        }
    }

    private static void migrate_legacy_database_if_needed(Path newDatabasePath) {
        if (Files.exists(newDatabasePath)) {
            return;
        }

        // 兼容老版本：如果项目根目录里已有 dnd_data.db，则复制到新的正式存档目录。
        Path legacyPath = Paths.get(DB_FILE_NAME).toAbsolutePath();
        if (!Files.exists(legacyPath)) {
            return;
        }

        try {
            Files.copy(legacyPath, newDatabasePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[系统提示] 已将旧数据库迁移到新的应用数据目录: " + newDatabasePath);
        } catch (IOException e) {
            System.out.println("[系统提示] 迁移旧数据库失败: " + e.getMessage());
        }
    }
}
