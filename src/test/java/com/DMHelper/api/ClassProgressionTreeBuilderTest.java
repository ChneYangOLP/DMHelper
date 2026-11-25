package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.race.Human_Race;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassProgressionTreeBuilderTest {
    @Test
    void everySupportedClassHasTwentyPopulatedLevels() {
        List<Character_Class> classes = List.of(
                new Fighter_Class(), new Wizard_Class(), new Sorcerer_Class(),
                new Warlock_Class(), new Paladin_Class(), new Bard_Class()
        );

        for (Character_Class job : classes) {
            JsonArray tree = ClassProgressionTreeBuilder.build(character(job, 7));
            assertEquals(20, tree.size(), job.class_name);
            for (int index = 0; index < tree.size(); index++) {
                JsonObject node = tree.get(index).getAsJsonObject();
                assertEquals(index + 1, node.get("level").getAsInt());
                assertTrue(node.getAsJsonArray("features").size() > 0, job.class_name + " level " + (index + 1));
            }
            assertEquals("CURRENT", tree.get(6).getAsJsonObject().get("status").getAsString());
            assertEquals("NEXT", tree.get(7).getAsJsonObject().get("status").getAsString());
        }
    }

    @Test
    void keyHighLevelMilestonesArePresent() {
        assertLevelContains(new Wizard_Class(), 18, "法术精通");
        assertLevelContains(new Warlock_Class(), 17, "神秘秘法（9 环）");
        assertLevelContains(new Paladin_Class(), 18, "灵光范围提升至 30 尺");
        assertLevelContains(new Bard_Class(), 20, "先发激励");
        assertLevelContains(new Fighter_Class(), 20, "攻击 4 次");
        assertLevelContains(new Sorcerer_Class(), 20, "术法复苏");
    }

    private static void assertLevelContains(Character_Class job, int level, String expected) {
        JsonObject node = ClassProgressionTreeBuilder.build(character(job, level))
                .get(level - 1).getAsJsonObject();
        assertTrue(node.getAsJsonArray("features").toString().contains(expected), job.class_name);
    }

    private static Character_Sheet character(Character_Class job, int level) {
        job.current_level = level;
        return Character_Sheet.restore_saved_character(
                "tree-test", 20, "unspecified", new Human_Race(), job,
                new Stats(12, 12, 12, 12, 12, 12)
        );
    }
}
