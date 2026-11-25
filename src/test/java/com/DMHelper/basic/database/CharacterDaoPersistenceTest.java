package com.DMHelper.basic.database;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.race.Human_Race;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterDaoPersistenceTest {

    @TempDir
    Path dataDirectory;

    @Test
    void savedCharacterCanBeReloadedFromDatabase() {
        System.setProperty("dmhelper.data.dir", dataDirectory.toString());
        Init_DB.setup_database();

        Character_Sheet character = Character_Sheet.restore_saved_character(
                "persistence-regression-test",
                20,
                "unspecified",
                new Human_Race(),
                new Fighter_Class(),
                new Stats(10, 10, 10, 10, 10, 10)
        );

        assertTrue(Character_DAO.save_character(character));
        assertTrue(character.database_id > 0);

        Global_Data.character_pool.clear();
        Character_DAO.load_all_characters();

        assertTrue(Global_Data.character_pool.stream()
                .anyMatch(item -> "persistence-regression-test".equals(item.name)));
    }

    @Test
    void garbledLocalizedClassNameFallsBackToStableEnglishMarker() throws Exception {
        System.setProperty("dmhelper.data.dir", dataDirectory.toString());
        Init_DB.setup_database();

        try (Connection connection = DB_Helper.get_connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO saved_characters "
                             + "(name, race_name, class_name, current_level, hp, ac, str, dex, con, intel, wis, cha) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, "legacy-wizard");
            statement.setString(2, "legacy-garbled (Human)");
            statement.setString(3, "legacy-garbled (Wizard)");
            statement.setInt(4, 3);
            statement.setInt(5, 14);
            statement.setInt(6, 12);
            for (int index = 7; index <= 12; index++) {
                statement.setInt(index, 10);
            }
            statement.executeUpdate();
        }

        Global_Data.character_pool.clear();
        Character_DAO.load_all_characters();

        assertTrue(Global_Data.character_pool.stream()
                .anyMatch(item -> "legacy-wizard".equals(item.name)
                        && item.job instanceof Wizard_Class));
    }
}
