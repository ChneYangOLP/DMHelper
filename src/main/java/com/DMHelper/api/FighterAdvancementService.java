package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class FighterAdvancementService {
    private static final Set<String> ABILITIES = Set.of("STR", "DEX", "CON", "INT", "WIS", "CHA");

    private FighterAdvancementService() {
    }

    static void apply(Character_Sheet character, String type, String value, List<String> values) {
        if (!(character.job instanceof Fighter_Class fighter)) {
            throw new IllegalArgumentException("该角色不是战士");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少升级选项类型");
        }

        switch (type) {
            case "FIGHTING_STYLE" -> chooseFightingStyle(character, fighter, value, false);
            case "SUBCLASS" -> chooseSubclass(character, fighter, value);
            case "MANEUVERS" -> chooseManeuvers(character, fighter, values);
            case "EXTRA_FIGHTING_STYLE" -> chooseFightingStyle(character, fighter, value, true);
            case "ASI_PLUS_TWO" -> applyPlusTwo(character, fighter, value);
            case "ASI_PLUS_ONE_TWO" -> applyAbilityIncrease(character, fighter, values, true);
            case "FEAT" -> chooseFeat(character, fighter, value);
            default -> throw new IllegalArgumentException("不支持的战士升级选项");
        }

        character.recalculate_derived_stats();
    }

    private static void chooseFightingStyle(Character_Sheet character, Fighter_Class fighter, String style, boolean extra) {
        requireValue(style, "请选择战斗风格");
        if (extra) {
            if (fighter.current_level < 10 || fighter.fighter_subclass != Fighter_Subclass.CHAMPION) {
                throw new IllegalArgumentException("当前等级或子职业不能选择额外战斗风格");
            }
            if (fighter.extra_fighting_style_name != null && !fighter.extra_fighting_style_name.isBlank()) {
                throw new IllegalArgumentException("额外战斗风格已经选择");
            }
        } else if (fighter.fighting_style_name != null && !fighter.fighting_style_name.isBlank()) {
            throw new IllegalArgumentException("战斗风格已经选择");
        }

        if (!fighter.get_available_fighting_styles(extra).contains(style)) {
            throw new IllegalArgumentException("无效的战斗风格");
        }
        if (extra) {
            fighter.extra_fighting_style_name = style;
            character.record_advancement("冠军勇士获得额外战斗风格：" + fighter.get_fighting_style_label(style));
        } else {
            fighter.fighting_style_name = style;
            character.record_advancement("选择战斗风格：" + fighter.get_fighting_style_label(style));
        }
    }

    private static void chooseSubclass(Character_Sheet character, Fighter_Class fighter, String subclass) {
        if (fighter.current_level < 3) {
            throw new IllegalArgumentException("战士达到 3 级后才能选择子职业");
        }
        if (fighter.fighter_subclass != Fighter_Subclass.NONE) {
            throw new IllegalArgumentException("战士子职业已经选择");
        }
        try {
            Fighter_Subclass selected = Fighter_Subclass.valueOf(subclass);
            if (selected == Fighter_Subclass.NONE) {
                throw new IllegalArgumentException("请选择有效的战士子职业");
            }
            fighter.fighter_subclass = selected;
            character.record_advancement("选择战士子职业：" + fighter.get_subclass_name());
        } catch (NullPointerException | IllegalArgumentException error) {
            throw new IllegalArgumentException("请选择有效的战士子职业");
        }
    }

    private static void chooseManeuvers(Character_Sheet character, Fighter_Class fighter, List<String> maneuvers) {
        if (fighter.fighter_subclass != Fighter_Subclass.BATTLE_MASTER) {
            throw new IllegalArgumentException("只有战斗大师可以选择战技");
        }
        int required = fighter.get_expected_maneuver_count() - fighter.maneuver_names.size();
        if (required <= 0) {
            throw new IllegalArgumentException("当前没有待选择的战技");
        }
        if (maneuvers == null || maneuvers.size() != required || new HashSet<>(maneuvers).size() != required) {
            throw new IllegalArgumentException("需要选择 " + required + " 个不同的战技");
        }
        List<String> available = fighter.get_available_maneuvers();
        if (!available.containsAll(maneuvers)) {
            throw new IllegalArgumentException("包含无效或已学会的战技");
        }
        fighter.maneuver_names.addAll(maneuvers);
        character.record_advancement("学习战技：" + String.join("、", maneuvers.stream().map(fighter::get_maneuver_label).toList()));
    }

    private static void applyAbilityIncrease(Character_Sheet character, Fighter_Class fighter, List<String> abilities) {
        applyAbilityIncrease(character, fighter, abilities, false);
    }

    private static void applyAbilityIncrease(Character_Sheet character, Fighter_Class fighter, List<String> abilities, boolean requireDifferent) {
        requirePendingAsi(fighter);
        if (abilities == null || abilities.size() != 2) {
            throw new IllegalArgumentException("属性提升分配不完整");
        }
        String first = normalizeAbility(abilities.get(0));
        String second = normalizeAbility(abilities.get(1));
        if (requireDifferent && first.equals(second)) {
            throw new IllegalArgumentException("请选择两项不同的属性");
        }
        boolean split = !first.equals(second);
        if (getAbility(character, first) >= 20 || getAbility(character, second) >= 20) {
            throw new IllegalArgumentException("属性值不能超过 20");
        }
        if (!split && getAbility(character, first) > 18) {
            throw new IllegalArgumentException("该属性无法再提升 2 点");
        }

        increaseAbility(character, first, 1);
        increaseAbility(character, second, 1);
        fighter.used_asi_choices++;
        character.record_advancement(split
                ? "属性值提升：" + first + " +1，" + second + " +1"
                : "属性值提升：" + first + " +2");
    }

    private static void applyPlusTwo(Character_Sheet character, Fighter_Class fighter, String ability) {
        requireValue(ability, "请选择要提升的属性");
        applyAbilityIncrease(character, fighter, List.of(ability, ability));
    }

    private static void chooseFeat(Character_Sheet character, Fighter_Class fighter, String feat) {
        requirePendingAsi(fighter);
        requireValue(feat, "请选择专长");
        if (!Feat_Library.get_available_feat_keys(character).contains(feat)) {
            throw new IllegalArgumentException("该专长不可用或已经选择");
        }
        fighter.feat_names.add(feat);
        fighter.used_asi_choices++;
        character.record_advancement("获得专长：" + Feat_Library.get_summary_line(feat));
    }

    private static void requirePendingAsi(Fighter_Class fighter) {
        if (fighter.pending_asi_count <= 0) {
            throw new IllegalArgumentException("当前没有待处理的属性提升或专长");
        }
    }

    private static String normalizeAbility(String ability) {
        String normalized = ability == null ? "" : ability.trim().toUpperCase();
        if (!ABILITIES.contains(normalized)) {
            throw new IllegalArgumentException("无效的属性类型");
        }
        return normalized;
    }

    private static int getAbility(Character_Sheet character, String ability) {
        return switch (ability) {
            case "STR" -> character.stats.str;
            case "DEX" -> character.stats.dex;
            case "CON" -> character.stats.con;
            case "INT" -> character.stats.intel;
            case "WIS" -> character.stats.wis;
            case "CHA" -> character.stats.cha;
            default -> throw new IllegalArgumentException("无效的属性类型");
        };
    }

    private static void increaseAbility(Character_Sheet character, String ability, int amount) {
        switch (ability) {
            case "STR" -> character.stats.str += amount;
            case "DEX" -> character.stats.dex += amount;
            case "CON" -> character.stats.con += amount;
            case "INT" -> character.stats.intel += amount;
            case "WIS" -> character.stats.wis += amount;
            case "CHA" -> character.stats.cha += amount;
            default -> throw new IllegalArgumentException("无效的属性类型");
        }
    }

    private static void requireValue(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
