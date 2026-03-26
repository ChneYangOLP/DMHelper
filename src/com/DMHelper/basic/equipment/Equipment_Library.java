package com.DMHelper.basic.equipment;

import com.DMHelper.basic.armor.Armor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class Equipment_Library {
    public static final String CUSTOM_KEY_PREFIX = "custom_";
    private static final Map<String, Equipment_Item> ITEMS = new LinkedHashMap<>();

    static {
        add(new Equipment_Item("traveler_clothes", "旅行者服装", Equipment_Slot.ARMOR,
                "简单耐穿的常服，没有额外护甲效果。", "None", 10, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("scholar_robes", "学者长袍", Equipment_Slot.ARMOR,
                "适合法师与学者的轻便长袍，方便施法动作。", "None", 10, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("sorcerer_robes", "术士礼袍", Equipment_Slot.ARMOR,
                "华丽而轻盈的礼袍，更像是力量象征而非防具。", "None", 10, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("warlock_robes", "邪术士长袍", Equipment_Slot.ARMOR,
                "带有神秘纹样的长袍，常见于契约施法者。", "None", 10, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("leather_armor", "皮甲 (Leather Armor)", Equipment_Slot.ARMOR,
                "轻甲，提供基础保护且不妨碍敏捷行动。", "Light", 11, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("half_plate", "半身甲 (Half Plate)", Equipment_Slot.ARMOR,
                "厚重的中甲，提供较高 AC，但敏捷加值最多 +2。", "Medium", 15, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("chain_mail", "锁子甲 (Chain Mail)", Equipment_Slot.ARMOR,
                "经典重甲，提供稳定的高防护。", "Heavy", 16, 0, 0, 0, 0, "", false, false));

        add(new Equipment_Item("longsword", "长剑 (Longsword)", Equipment_Slot.MAIN_HAND,
                "标准近战武器，适合战士与圣武士。", "", 0, 0, 1, 8, 0, "挥砍", false, false));
        add(new Equipment_Item("greatsword", "巨剑 (Greatsword)", Equipment_Slot.MAIN_HAND,
                "双手重武器，单次伤害更高。", "", 0, 0, 2, 6, 0, "挥砍", false, false));
        add(new Equipment_Item("quarterstaff", "长棍 (Quarterstaff)", Equipment_Slot.MAIN_HAND,
                "稳妥的法师近战武器，也能当法术施法媒介。", "", 0, 0, 1, 6, 0, "钝击", false, false));
        add(new Equipment_Item("dagger", "匕首 (Dagger)", Equipment_Slot.MAIN_HAND,
                "轻便的灵巧武器，可近战也可投掷。", "", 0, 0, 1, 4, 0, "穿刺", true, false));
        add(new Equipment_Item("light_crossbow", "轻弩 (Light Crossbow)", Equipment_Slot.MAIN_HAND,
                "可靠的远程武器，适合缺少法术位时使用。", "", 0, 0, 1, 8, 0, "穿刺", false, true));

        add(new Equipment_Item("shield", "盾牌 (Shield)", Equipment_Slot.OFF_HAND,
                "标准盾牌，装备时 AC +2。", "", 0, 2, 0, 0, 0, "", false, false));
        add(new Equipment_Item("spellbook_focus", "奥术书册", Equipment_Slot.OFF_HAND,
                "记录法术与注记的厚重书册，法师常随身携带。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("pact_tome", "契约之书", Equipment_Slot.OFF_HAND,
                "写满异界符号的秘典，散发不安的低语。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("holy_symbol", "圣徽", Equipment_Slot.OFF_HAND,
                "圣武士常持的神圣徽记，用于祈祷与施法。", "", 0, 0, 0, 0, 0, "", false, false));

        add(new Equipment_Item("traveler_cloak", "旅行披风", Equipment_Slot.CLOAK,
                "防风耐磨的披风，适合长途冒险。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("arcane_cloak", "奥术披风", Equipment_Slot.CLOAK,
                "边缘绣有符文的披风，看起来很像学院制式装备。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("shadow_cloak", "暗影披风", Equipment_Slot.CLOAK,
                "颜色晦暗的披风，适合邪术士与潜行者气质。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("sanctified_cloak", "圣佑披风", Equipment_Slot.CLOAK,
                "披风内衬缝有祷文，象征信仰与职责。", "", 0, 0, 0, 0, 0, "", false, false));

        add(new Equipment_Item("scholar_amulet", "学者护符", Equipment_Slot.ACCESSORY,
                "刻有星象图案的小护符，偏向知识与研究。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("bloodline_amulet", "血脉护符", Equipment_Slot.ACCESSORY,
                "用于彰显体内魔力血统的饰品。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("pact_amulet", "契约护符", Equipment_Slot.ACCESSORY,
                "与异界恩主契约相连的信物。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("veteran_charm", "老兵徽记", Equipment_Slot.ACCESSORY,
                "朴素的军旅纪念物，象征战斗经历。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("blessed_ring", "祝圣戒指", Equipment_Slot.ACCESSORY,
                "镌刻祷文的戒指，是圣武士常见随身圣物。", "", 0, 0, 0, 0, 0, "", false, false));

        add(new Equipment_Item("goblin_ear", "地精耳串", Equipment_Slot.BACKPACK,
                "粗糙的战利品串饰，常见于地精巢穴。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("kobold_talisman", "狗头人护符", Equipment_Slot.BACKPACK,
                "以骨头和碎牙做成的小护符。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("bandit_pouch", "强盗钱袋", Equipment_Slot.BACKPACK,
                "装着零散铜币和小赃物的旧钱袋。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("holy_relic_fragment", "圣物碎片", Equipment_Slot.BACKPACK,
                "刻有祷文的碎片，可能来自废弃祭坛。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("wolf_pelt", "狼皮", Equipment_Slot.BACKPACK,
                "还带着野性气息的兽皮，可加工为皮具。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("venom_sac", "毒囊", Equipment_Slot.BACKPACK,
                "从毒性生物身上取下的危险器官。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("undead_bone_charm", "亡骸骨饰", Equipment_Slot.BACKPACK,
                "残留着阴冷死气的骨质饰物。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("monster_fang", "怪物尖牙", Equipment_Slot.BACKPACK,
                "锋利的巨兽牙齿，经常被当作材料或纪念。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("dragon_scale", "龙鳞", Equipment_Slot.BACKPACK,
                "坚硬的龙类鳞片，十分罕见。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("fiend_ash", "炼狱灰烬", Equipment_Slot.BACKPACK,
                "带着焦灼气味的黑灰，常见于邪异生物遗骸。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("strange_eye", "异怪眼球", Equipment_Slot.BACKPACK,
                "令人不适的眼球样本，仍然泛着微光。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("knight_token", "骑士纹章牌", Equipment_Slot.BACKPACK,
                "刻着家族徽记的小金属牌。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("giant_bone_shard", "巨人骨片", Equipment_Slot.BACKPACK,
                "从大型怪物身上取下的巨大骨片。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("mystic_scroll_fragment", "秘法卷轴残页", Equipment_Slot.BACKPACK,
                "写满晦涩符号的破损卷轴。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("scroll_of_arcane_insight", "奥术洞察卷轴", Equipment_Slot.BACKPACK,
                "阅读后在短时间内理清秘法脉络，记录一条有价值的调查笔记。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("scroll_of_healing_touch", "疗伤术卷轴", Equipment_Slot.BACKPACK,
                "施放一次疗伤术，恢复 1d8+3 点生命值。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("scroll_of_fireball", "火球术卷轴", Equipment_Slot.BACKPACK,
                "施放一次火球术，对范围内目标造成 8d6 火焰伤害。", "", 0, 0, 0, 0, 0, "火焰", false, false));
        add(new Equipment_Item("scroll_of_identify", "鉴定术卷轴", Equipment_Slot.BACKPACK,
                "施放一次鉴定术，解析物品或现象的魔法性质。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("beast_claw", "兽爪", Equipment_Slot.BACKPACK,
                "完整保留下来的大型兽爪。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("healing_potion", "治疗药水 (Potion of Healing)", Equipment_Slot.BACKPACK,
                "饮用后恢复 2d4+2 点生命值。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("greater_healing_potion", "强效治疗药水 (Greater Healing Potion)", Equipment_Slot.BACKPACK,
                "饮用后恢复 4d4+4 点生命值。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("fire_bomb", "火焰炸弹", Equipment_Slot.BACKPACK,
                "投掷后造成 2d6 火焰伤害，适合清理成群敌人。", "", 0, 0, 0, 0, 0, "火焰", false, false));
        add(new Equipment_Item("thunder_bomb", "震雷炸弹", Equipment_Slot.BACKPACK,
                "投掷后造成 2d8 雷鸣伤害并发出巨大声响。", "", 0, 0, 0, 0, 0, "雷鸣", false, false));
        add(new Equipment_Item("iron_key", "铁钥匙", Equipment_Slot.BACKPACK,
                "常见的金属钥匙，可用于开启锁具或机关。", "", 0, 0, 0, 0, 0, "", false, false));
        add(new Equipment_Item("ancient_ruin_map", "古遗迹地图", Equipment_Slot.BACKPACK,
                "记载古老遗迹通道和标记的任务地图。", "", 0, 0, 0, 0, 0, "", false, false));
    }

    private Equipment_Library() {
    }

    private static void add(Equipment_Item item) {
        ITEMS.put(item.key, item);
    }

    public static void register_custom_item(Equipment_Item item) {
        if (item == null || item.key == null || !item.key.startsWith(CUSTOM_KEY_PREFIX)) {
            return;
        }
        ITEMS.put(item.key, item);
    }

    public static void clear_custom_items() {
        ITEMS.entrySet().removeIf(entry -> entry.getKey() != null && entry.getKey().startsWith(CUSTOM_KEY_PREFIX));
    }

    public static Equipment_Item get_item(String key) {
        return ITEMS.get(key);
    }

    public static List<Equipment_Item> get_all_items() {
        return new ArrayList<>(ITEMS.values());
    }

    public static List<Equipment_Item> get_items(List<String> keys) {
        List<Equipment_Item> items = new ArrayList<>();
        if (keys == null) {
            return items;
        }
        for (String key : keys) {
            Equipment_Item item = get_item(key);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public static List<Equipment_Item> get_items_for_slot(List<String> ownedKeys, Equipment_Slot slot) {
        List<Equipment_Item> items = new ArrayList<>();
        for (Equipment_Item item : get_items(ownedKeys)) {
            if (item.slot == slot) {
                items.add(item);
            }
        }
        return items;
    }

    public static List<Equipment_Item> search_items(String keyword) {
        return search_items(keyword, null, false);
    }

    public static List<Equipment_Item> search_items(String keyword, Equipment_Slot slot, boolean builtinsOnly) {
        String normalized = normalize_search_text(keyword);
        List<Equipment_Item> result = new ArrayList<>();
        for (Equipment_Item item : ITEMS.values()) {
            if (slot != null && item.slot != slot) {
                continue;
            }
            if (builtinsOnly && is_custom_key(item.key)) {
                continue;
            }
            String haystack = normalize_search_text(
                    item.display_name + " " + item.description + " " + item.get_slot_label() + " "
                            + item.get_armor_type_label() + " " + item.damage_type
            );
            if (normalized.isEmpty() || haystack.contains(normalized)) {
                result.add(item);
            }
        }
        return result;
    }

    public static boolean is_custom_key(String key) {
        return key != null && key.startsWith(CUSTOM_KEY_PREFIX);
    }

    public static String build_custom_key(int ownerCharacterId, String displayName) {
        String rawName = displayName == null ? "" : displayName.trim().toLowerCase(Locale.ROOT);
        String sanitized = rawName.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (sanitized.isEmpty()) {
            sanitized = "item";
        }
        return CUSTOM_KEY_PREFIX + ownerCharacterId + "_" + sanitized + "_" + System.currentTimeMillis();
    }

    private static String normalize_search_text(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> get_default_inventory(String classKey) {
        List<String> keys = new ArrayList<>();
        if ("FIGHTER".equals(classKey)) {
            keys.add("chain_mail");
            keys.add("longsword");
            keys.add("greatsword");
            keys.add("shield");
            keys.add("traveler_cloak");
            keys.add("veteran_charm");
        } else if ("WIZARD".equals(classKey)) {
            keys.add("scholar_robes");
            keys.add("quarterstaff");
            keys.add("spellbook_focus");
            keys.add("arcane_cloak");
            keys.add("scholar_amulet");
        } else if ("SORCERER".equals(classKey)) {
            keys.add("sorcerer_robes");
            keys.add("dagger");
            keys.add("arcane_cloak");
            keys.add("bloodline_amulet");
        } else if ("WARLOCK".equals(classKey)) {
            keys.add("warlock_robes");
            keys.add("leather_armor");
            keys.add("dagger");
            keys.add("pact_tome");
            keys.add("shadow_cloak");
            keys.add("pact_amulet");
        } else if ("PALADIN".equals(classKey)) {
            keys.add("chain_mail");
            keys.add("longsword");
            keys.add("shield");
            keys.add("sanctified_cloak");
            keys.add("holy_symbol");
            keys.add("blessed_ring");
        } else {
            keys.add("traveler_clothes");
            keys.add("dagger");
            keys.add("traveler_cloak");
        }
        return keys;
    }

    public static String get_default_equipped_key(String classKey, Equipment_Slot slot) {
        if (slot == Equipment_Slot.ARMOR) {
            if ("FIGHTER".equals(classKey) || "PALADIN".equals(classKey)) return "chain_mail";
            if ("WIZARD".equals(classKey)) return "scholar_robes";
            if ("SORCERER".equals(classKey)) return "sorcerer_robes";
            if ("WARLOCK".equals(classKey)) return "warlock_robes";
            return "traveler_clothes";
        }
        if (slot == Equipment_Slot.MAIN_HAND) {
            if ("WIZARD".equals(classKey)) return "quarterstaff";
            if ("SORCERER".equals(classKey) || "WARLOCK".equals(classKey)) return "dagger";
            return "longsword";
        }
        if (slot == Equipment_Slot.OFF_HAND) {
            if ("FIGHTER".equals(classKey) || "PALADIN".equals(classKey)) return "shield";
            if ("WIZARD".equals(classKey)) return "spellbook_focus";
            if ("WARLOCK".equals(classKey)) return "pact_tome";
            return "";
        }
        if (slot == Equipment_Slot.CLOAK) {
            if ("PALADIN".equals(classKey)) return "sanctified_cloak";
            if ("WIZARD".equals(classKey) || "SORCERER".equals(classKey)) return "arcane_cloak";
            if ("WARLOCK".equals(classKey)) return "shadow_cloak";
            return "traveler_cloak";
        }
        if (slot == Equipment_Slot.ACCESSORY) {
            if ("FIGHTER".equals(classKey)) return "veteran_charm";
            if ("WIZARD".equals(classKey)) return "scholar_amulet";
            if ("SORCERER".equals(classKey)) return "bloodline_amulet";
            if ("WARLOCK".equals(classKey)) return "pact_amulet";
            if ("PALADIN".equals(classKey)) return "blessed_ring";
        }
        return "";
    }

    public static String map_legacy_armor_key(Armor armor) {
        if (armor == null) {
            return "traveler_clothes";
        }
        if ("Heavy".equals(armor.armor_type) || armor.armor_name.contains("锁子甲")) {
            return "chain_mail";
        }
        if ("Medium".equals(armor.armor_type) || armor.armor_name.contains("半身甲")) {
            return "half_plate";
        }
        if ("Light".equals(armor.armor_type) || armor.armor_name.contains("皮甲")) {
            return "leather_armor";
        }
        if (armor.armor_name.contains("长袍")) {
            if (armor.armor_name.contains("术士")) return "sorcerer_robes";
            if (armor.armor_name.contains("邪术士")) return "warlock_robes";
            return "scholar_robes";
        }
        return "traveler_clothes";
    }
}
