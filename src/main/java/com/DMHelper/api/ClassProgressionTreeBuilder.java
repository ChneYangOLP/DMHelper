package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the complete level 1-20 roadmap shown by the Electron character manager.
 * The roadmap is deliberately presentation-only: the class implementations remain
 * the source of truth for resource values and applying level-up effects.
 */
final class ClassProgressionTreeBuilder {
    private ClassProgressionTreeBuilder() {}

    static JsonArray build(Character_Sheet character) {
        JsonArray tree = new JsonArray();
        for (int level = 1; level <= 20; level++) {
            JsonObject node = new JsonObject();
            node.addProperty("level", level);
            node.addProperty("proficiency_bonus", 2 + (level - 1) / 4);
            node.addProperty("status", status(level, character.job.current_level));
            node.addProperty("choice_required", isChoiceLevel(character.job, level));

            JsonArray features = new JsonArray();
            for (String feature : featuresFor(character.job, level)) {
                features.add(feature);
            }
            node.add("features", features);
            tree.add(node);
        }
        return tree;
    }

    private static String status(int level, int currentLevel) {
        if (level < currentLevel) return "COMPLETED";
        if (level == currentLevel) return "CURRENT";
        if (level == currentLevel + 1) return "NEXT";
        return "LOCKED";
    }

    private static List<String> featuresFor(Character_Class job, int level) {
        List<String> features;
        if (job instanceof Fighter_Class fighter) features = fighterFeatures(fighter, level);
        else if (job instanceof Wizard_Class wizard) features = wizardFeatures(wizard, level);
        else if (job instanceof Sorcerer_Class sorcerer) features = sorcererFeatures(sorcerer, level);
        else if (job instanceof Warlock_Class warlock) features = warlockFeatures(warlock, level);
        else if (job instanceof Paladin_Class paladin) features = paladinFeatures(paladin, level);
        else if (job instanceof Bard_Class bard) features = bardFeatures(bard, level);
        else features = new ArrayList<>();

        if (features.isEmpty()) features.add("职业能力与资源成长");
        return features;
    }

    private static List<String> fighterFeatures(Fighter_Class fighter, int level) {
        String subclass = chosenOrGeneric(fighter.get_subclass_name(), "武术范型能力");
        return switch (level) {
            case 1 -> list("战斗风格", "复苏之风");
            case 2 -> list("动作如潮（1 次）");
            case 3 -> list("选择武术范型", subclass);
            case 4, 6, 8, 12, 14, 16, 19 -> list("属性值提升或专长");
            case 5 -> list("额外攻击（每次动作攻击 2 次）");
            case 7, 15, 18 -> list(subclass);
            case 9 -> list("不屈（1 次）");
            case 10 -> list(subclass, "子职业成长选择");
            case 11 -> list("额外攻击（每次动作攻击 3 次）");
            case 13 -> list("不屈（2 次）");
            case 17 -> list("动作如潮（2 次）", "不屈（3 次）");
            case 20 -> list("额外攻击（每次动作攻击 4 次）");
            default -> list();
        };
    }

    private static List<String> wizardFeatures(Wizard_Class wizard, int level) {
        String tradition = chosenOrGeneric(wizard.get_subclass_name(), "奥术传承能力");
        return switch (level) {
            case 1 -> list("施法与法术书", "奥术回能", "可施放 1 环法术");
            case 2 -> list("选择奥术传承", tradition);
            case 3 -> list("解锁 2 环法术");
            case 4 -> list("属性值提升或专长", "已知戏法增至 4 个");
            case 5 -> list("解锁 3 环法术");
            case 6, 10, 14 -> list(tradition);
            case 7 -> list("解锁 4 环法术");
            case 8, 12, 16, 19 -> list("属性值提升或专长");
            case 9 -> list("解锁 5 环法术");
            case 11 -> list("解锁 6 环法术");
            case 13 -> list("解锁 7 环法术");
            case 15 -> list("解锁 8 环法术");
            case 17 -> list("解锁 9 环法术");
            case 18 -> list("法术精通");
            case 20 -> list("招牌法术");
            default -> list();
        };
    }

    private static List<String> sorcererFeatures(Sorcerer_Class sorcerer, int level) {
        String origin = chosenOrGeneric(sorcerer.get_subclass_name(), "术法起源能力");
        return switch (level) {
            case 1 -> list("施法", "选择术法起源", origin);
            case 2 -> list("魔力源泉（术法点）");
            case 3 -> list("选择 2 项超魔技巧", "解锁 2 环法术");
            case 4 -> list("属性值提升或专长", "已知戏法增至 5 个");
            case 5 -> list("解锁 3 环法术");
            case 6, 14, 18 -> list(origin);
            case 7 -> list("解锁 4 环法术");
            case 8, 12, 16, 19 -> list("属性值提升或专长");
            case 9 -> list("解锁 5 环法术");
            case 10 -> list("新增 1 项超魔技巧", "已知戏法增至 6 个");
            case 11 -> list("解锁 6 环法术");
            case 13 -> list("解锁 7 环法术");
            case 15 -> list("解锁 8 环法术");
            case 17 -> list("解锁 9 环法术", "新增 1 项超魔技巧");
            case 20 -> list("术法复苏");
            default -> list();
        };
    }

    private static List<String> warlockFeatures(Warlock_Class warlock, int level) {
        String patron = chosenOrGeneric(warlock.get_subclass_name(), "异界恩主能力");
        return switch (level) {
            case 1 -> list("契约魔法", "选择异界恩主", patron);
            case 2 -> list("选择 2 项邪术祈请");
            case 3 -> list("选择契约恩赐", "契约法术位升至 2 环");
            case 4 -> list("属性值提升或专长", "已知戏法增至 3 个");
            case 5 -> list("契约法术位升至 3 环", "邪术祈请增至 3 项");
            case 6, 14 -> list(patron);
            case 7 -> list("契约法术位升至 4 环", "邪术祈请增至 4 项");
            case 8, 16, 19 -> list("属性值提升或专长");
            case 9 -> list("契约法术位升至 5 环", "邪术祈请增至 5 项");
            case 10 -> list(patron, "已知戏法增至 4 个");
            case 11 -> list("神秘秘法（6 环）", "契约法术位增至 3 个");
            case 12 -> list("属性值提升或专长", "邪术祈请增至 6 项");
            case 13 -> list("神秘秘法（7 环）");
            case 15 -> list("神秘秘法（8 环）", "邪术祈请增至 7 项");
            case 17 -> list("神秘秘法（9 环）", "契约法术位增至 4 个");
            case 18 -> list("邪术祈请增至 8 项");
            case 20 -> list("邪术大师");
            default -> list();
        };
    }

    private static List<String> paladinFeatures(Paladin_Class paladin, int level) {
        String oath = chosenOrGeneric(paladin.get_subclass_name(), "圣武誓言能力");
        return switch (level) {
            case 1 -> list("神圣感知", "圣疗之手");
            case 2 -> list("战斗风格", "施法", "神圣惩击");
            case 3 -> list("圣佑免疫", "选择圣武誓言", oath);
            case 4, 8, 12, 16, 19 -> list("属性值提升或专长");
            case 5 -> list("额外攻击", "解锁 2 环法术");
            case 6 -> list("守护灵光");
            case 7, 15, 20 -> list(oath);
            case 9 -> list("解锁 3 环法术");
            case 10 -> list("勇气灵光");
            case 11 -> list("强化神圣惩击");
            case 13 -> list("解锁 4 环法术");
            case 14 -> list("净化之触");
            case 17 -> list("解锁 5 环法术");
            case 18 -> list("灵光范围提升至 30 尺");
            default -> list();
        };
    }

    private static List<String> bardFeatures(Bard_Class bard, int level) {
        String college = chosenOrGeneric(bard.get_subclass_name(), "吟游学院能力");
        return switch (level) {
            case 1 -> list("施法", "吟游激励（d6）");
            case 2 -> list("万事通", "休憩曲（d6）");
            case 3 -> list("选择吟游学院", college, "技能专精（2 项）", "解锁 2 环法术");
            case 4 -> list("属性值提升或专长", "已知戏法增至 3 个");
            case 5 -> list("吟游激励提升至 d8", "激励之泉", "解锁 3 环法术");
            case 6 -> list("反迷惑", college);
            case 7 -> list("解锁 4 环法术");
            case 8, 12, 16, 19 -> list("属性值提升或专长");
            case 9 -> list("休憩曲提升至 d8", "解锁 5 环法术");
            case 10 -> list("吟游激励提升至 d10", "技能专精（再选 2 项）", "魔法秘辛（2 个）", "已知戏法增至 4 个");
            case 11 -> list("解锁 6 环法术");
            case 13 -> list("休憩曲提升至 d10", "解锁 7 环法术");
            case 14 -> list(college, "魔法秘辛（再选 2 个）");
            case 15 -> list("吟游激励提升至 d12", "解锁 8 环法术");
            case 17 -> list("休憩曲提升至 d12", "解锁 9 环法术");
            case 18 -> list("魔法秘辛（再选 2 个）");
            case 20 -> list("先发激励");
            default -> list();
        };
    }

    private static boolean isChoiceLevel(Character_Class job, int level) {
        if (isAsiLevel(job, level)) return true;
        if (job instanceof Fighter_Class) return level == 1 || level == 3 || level == 7 || level == 10 || level == 15;
        if (job instanceof Wizard_Class) return level == 1 || level == 2 || level == 18 || level == 20;
        if (job instanceof Sorcerer_Class) return level == 1 || level == 3 || level == 10 || level == 17;
        if (job instanceof Warlock_Class) return level == 1 || level == 2 || level == 3 || level == 5 || level == 7
                || level == 9 || level == 11 || level == 12 || level == 13 || level == 15 || level == 17 || level == 18;
        if (job instanceof Paladin_Class) return level == 2 || level == 3;
        if (job instanceof Bard_Class) return level == 3 || level == 10 || level == 14 || level == 18;
        return false;
    }

    private static boolean isAsiLevel(Character_Class job, int level) {
        if (job instanceof Fighter_Class) return contains(new int[]{4, 6, 8, 12, 14, 16, 19}, level);
        return contains(new int[]{4, 8, 12, 16, 19}, level);
    }

    private static boolean contains(int[] values, int value) {
        return Arrays.stream(values).anyMatch(item -> item == value);
    }

    private static String chosenOrGeneric(String chosen, String generic) {
        return chosen == null || chosen.isBlank() || "未选择".equals(chosen) ? generic : chosen + "能力";
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
