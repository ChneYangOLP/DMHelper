package com.DMHelper.api;

import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Custom_Equipment_DAO;
import com.DMHelper.basic.database.Init_DB;

/**
 * Headless entry point used by the packaged Electron application.
 */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) {
        Init_DB.setup_database();
        Custom_Equipment_DAO.load_all_custom_items();
        Character_DAO.load_all_characters();
        ApiServer.start(resolvePort());
    }

    private static int resolvePort() {
        String configuredPort = System.getenv("DMHELPER_API_PORT");
        if (configuredPort == null || configuredPort.isBlank()) {
            return 8080;
        }
        try {
            return Integer.parseInt(configuredPort);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("DMHELPER_API_PORT must be a valid integer", error);
        }
    }
}
