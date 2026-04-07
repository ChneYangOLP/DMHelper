package com.DMHelper.basic.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Monster_Loot_Helper {
    private Monster_Loot_Helper() {
    }

    public static List<String> roll_loot_for_encounter(List<Monster_Definition> monsters) {
        List<String> lootKeys = new ArrayList<>();
        if (monsters == null) {
            return lootKeys;
        }
        for (Monster_Definition monster : monsters) {
            lootKeys.addAll(roll_loot_for_monster(monster));
        }
        return lootKeys;
    }

    private static List<String> roll_loot_for_monster(Monster_Definition monster) {
        List<String> drops = new ArrayList<>();
        if (monster == null) {
            return drops;
        }

        switch (monster.key) {
            case "goblin":
            case "kobold":
                maybeAdd(drops, "goblin_ear", 75);
                maybeAddAny(drops, Arrays.asList("dagger", "traveler_cloak", "kobold_talisman"), 45);
                break;
            case "bandit":
            case "cultist":
            case "guard":
                maybeAddAny(drops, Arrays.asList("bandit_pouch", "dagger", "traveler_cloak"), 70);
                maybeAddAny(drops, Arrays.asList("leather_armor", "holy_relic_fragment"), 35);
                break;
            case "acolyte":
            case "cult_fanatic":
                maybeAdd(drops, "holy_relic_fragment", 80);
                maybeAddAny(drops, Arrays.asList("scholar_amulet", "mystic_scroll_fragment", "arcane_cloak"), 45);
                break;
            case "skeleton":
            case "zombie":
            case "crawling_claw":
            case "shadow":
            case "specter":
            case "ghoul":
            case "ghast":
            case "wight":
            case "minotaur_skeleton":
            case "flameskull":
            case "ghost":
            case "banshee":
            case "mummy":
            case "wraith":
            case "revenant":
            case "bone_naga":
            case "deathlock":
            case "vampire_spawn":
            case "lich":
                maybeAdd(drops, "undead_bone_charm", 80);
                maybeAddAny(drops, Arrays.asList("fiend_ash", "holy_relic_fragment", "mystic_scroll_fragment"), 40);
                break;
            case "wolf":
            case "mastiff":
            case "panther":
            case "crocodile":
            case "giant_badger":
            case "ape":
            case "boar":
            case "giant_crab":
            case "constrictor_snake":
            case "giant_poisonous_snake":
            case "reef_shark":
            case "dire_wolf":
            case "brown_bear":
            case "giant_boar":
            case "lion":
            case "tiger":
            case "giant_goat":
            case "giant_hyena":
            case "giant_eagle":
            case "giant_vulture":
            case "giant_toad":
            case "giant_constrictor_snake":
            case "giant_octopus":
            case "giant_crocodile":
            case "giant_ape":
            case "saber_toothed_tiger":
            case "giant_elk":
            case "owlbear":
            case "werewolf":
                maybeAddAny(drops, Arrays.asList("wolf_pelt", "monster_fang", "beast_claw"), 85);
                break;
            case "giant_rat":
            case "stirge":
            case "giant_wolf_spider":
            case "giant_spider":
                maybeAddAny(drops, Arrays.asList("venom_sac", "beast_claw", "monster_fang"), 75);
                break;
            case "orc":
            case "hobgoblin":
            case "bugbear":
            case "thug":
            case "scout":
            case "bandit_captain":
            case "veteran":
            case "knight":
                maybeAddAny(drops, Arrays.asList("longsword", "shield", "leather_armor", "bandit_pouch", "knight_token"), 70);
                maybeAddAny(drops, Arrays.asList("veteran_charm", "traveler_cloak", "light_crossbow"), 40);
                break;
            case "animated_armor":
                maybeAdd(drops, "chain_mail", 90);
                break;
            case "gargoyle":
            case "harpy":
            case "manticore":
            case "chimera":
            case "wyvern":
                maybeAddAny(drops, Arrays.asList("monster_fang", "beast_claw", "venom_sac"), 85);
                break;
            case "mimic":
                maybeAddAny(drops, Arrays.asList("bandit_pouch", "scholar_amulet", "blessed_ring"), 90);
                break;
            case "spectator":
                maybeAddAny(drops, Arrays.asList("strange_eye", "mystic_scroll_fragment", "scholar_amulet"), 95);
                break;
            case "gelatinous_cube":
                maybeAddAny(drops, Arrays.asList("venom_sac", "strange_eye"), 50);
                break;
            case "basilisk":
                maybeAddAny(drops, Arrays.asList("monster_fang", "strange_eye", "beast_claw"), 80);
                break;
            case "ogre":
            case "minotaur":
            case "ettin":
            case "hill_giant":
            case "troll":
                maybeAddAny(drops, Arrays.asList("giant_bone_shard", "greatsword", "monster_fang"), 85);
                break;
            case "young_green_dragon":
            case "young_black_dragon":
            case "young_red_dragon":
                maybeAdd(drops, "dragon_scale", 100);
                maybeAddAny(drops, Arrays.asList("dragon_scale", "monster_fang", "bloodline_amulet"), 85);
                break;
            default:
                maybeAddAny(drops, Arrays.asList("bandit_pouch", "monster_fang", "mystic_scroll_fragment"), 30);
                break;
        }

        return drops;
    }

    private static void maybeAdd(List<String> drops, String itemKey, int chancePercent) {
        if (Dice_Util.roll_die(100) <= chancePercent) {
            drops.add(itemKey);
        }
    }

    private static void maybeAddAny(List<String> drops, List<String> itemKeys, int chancePercent) {
        if (itemKeys == null || itemKeys.isEmpty()) {
            return;
        }
        if (Dice_Util.roll_die(100) <= chancePercent) {
            drops.add(itemKeys.get(Dice_Util.roll_die(itemKeys.size()) - 1));
        }
    }
}
