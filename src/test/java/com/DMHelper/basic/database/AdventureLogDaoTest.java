package com.DMHelper.basic.database;

import com.DMHelper.basic.journal.Adventure_Log_Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdventureLogDaoTest {
    @TempDir
    Path dataDirectory;

    @Test
    void journalEntrySupportsCreateUpdateListAndDelete() throws Exception {
        System.setProperty("dmhelper.data.dir", dataDirectory.toString());
        Init_DB.setup_database();

        Adventure_Log_Entry entry = new Adventure_Log_Entry();
        entry.entry_type = "QUEST";
        entry.title = "寻找失踪商队";
        entry.content = "最后在北门外出现。";
        entry.location = "灰港";
        entry.tags = "主线,商队";
        entry.quest_status = "ACTIVE";
        entry.event_date = "霜落月 12 日";

        Adventure_Log_Entry created = Adventure_Log_DAO.create(entry);
        assertNotNull(created);
        assertTrue(created.id > 0);
        assertEquals(1, Adventure_Log_DAO.find_all().size());

        created.quest_status = "COMPLETED";
        Adventure_Log_Entry updated = Adventure_Log_DAO.update(created);
        assertNotNull(updated);
        assertEquals("COMPLETED", updated.quest_status);

        assertTrue(Adventure_Log_DAO.delete(created.id));
        assertTrue(Adventure_Log_DAO.find_all().isEmpty());
    }
}
