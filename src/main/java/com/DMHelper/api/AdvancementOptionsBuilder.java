package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.bard.Bard_College;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Oath;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerous_Origin;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Pact;
import com.DMHelper.basic.playerclass.warlock.Warlock_Patron;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Subclass;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class AdvancementOptionsBuilder {
    private AdvancementOptionsBuilder() {}

    static JsonObject build(Character_Sheet character) {
        JsonObject result = new JsonObject();
        JsonArray groups = new JsonArray();
        int pendingAsi = 0;
        String classLabel = character.job.class_name;

        if (character.job instanceof Fighter_Class fighter) {
            pendingAsi = fighter.pending_asi_count;
            if (isBlank(fighter.fighting_style_name)) groups.add(group("FIGHTING_STYLE", "选择战斗风格", 1, styleOptions(fighter, false)));
            if (fighter.current_level >= 3 && fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.NONE)
                groups.add(group("SUBCLASS", "选择 3 级战士子职业", 1, options(new String[][]{{"CHAMPION","冠军勇士","稳定强化重击、生存与运动能力。"},{"BATTLE_MASTER","战斗大师","运用卓越骰和战技控制战场。"}})));
            int maneuverCount = fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.BATTLE_MASTER ? Math.max(0, fighter.get_expected_maneuver_count() - fighter.maneuver_names.size()) : 0;
            if (maneuverCount > 0) groups.add(group("MANEUVERS", "选择战技", maneuverCount, labeled(fighter.get_available_maneuvers(), fighter::get_maneuver_label, fighter::get_maneuver_description)));
            if (fighter.current_level >= 10 && fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.CHAMPION && isBlank(fighter.extra_fighting_style_name))
                groups.add(group("EXTRA_FIGHTING_STYLE", "选择冠军勇士额外战斗风格", 1, styleOptions(fighter, true)));
        } else if (character.job instanceof Wizard_Class wizard) {
            pendingAsi = wizard.pending_asi_count;
            if (wizard.current_level >= 2 && wizard.wizard_subclass == Wizard_Subclass.NONE)
                groups.add(group("WIZARD_SUBCLASS", "选择奥术传承", 1, wizardSubclasses()));
            addSpellGroup(groups, "WIZARD_CANTRIPS", "选择法师戏法", wizard.cantrips_known - wizard.known_cantrip_keys.size(), wizard.get_available_cantrip_options());
            addSpellGroup(groups, "WIZARD_SPELLBOOK", "抄录升级获得的法术", wizard.spells_in_spellbook - wizard.spellbook_spell_keys.size(), wizard.get_available_spellbook_options());
        } else if (character.job instanceof Sorcerer_Class sorcerer) {
            pendingAsi = sorcerer.pending_asi_count;
            if (sorcerer.sorcerous_origin == Sorcerous_Origin.NONE)
                groups.add(group("SORCERER_ORIGIN", "选择术法起源", 1, options(new String[][]{{"DRACONIC_BLOODLINE","龙族血脉","获得龙鳞坚韧、元素亲和与龙翼。"},{"WILD_MAGIC","狂野魔法","驾驭魔法涌动、混沌潮汐与扭转幸运。"}})));
            if (sorcerer.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE && isBlank(sorcerer.dragon_ancestry))
                groups.add(group("DRAGON_ANCESTRY", "选择龙族先祖", 1, dragonAncestries()));
            addSpellGroup(groups, "SORCERER_CANTRIPS", "选择术士戏法", sorcerer.cantrips_known - sorcerer.known_cantrip_keys.size(), withoutKnown(Spell_Library.get_sorcerer_cantrip_keys(), sorcerer.known_cantrip_keys));
            addSpellGroup(groups, "SORCERER_SPELLS", "选择术士法术", sorcerer.spells_known_count - sorcerer.known_spell_keys.size(), sorcerer.get_available_spell_options());
            if (sorcerer.pending_metamagic_choices > 0)
                groups.add(group("SORCERER_METAMAGIC", "选择超魔技巧", sorcerer.pending_metamagic_choices, labeled(sorcerer.get_available_metamagic_options(), sorcerer::get_metamagic_label, sorcerer::get_metamagic_description)));
        } else if (character.job instanceof Paladin_Class paladin) {
            pendingAsi = paladin.pending_asi_count;
            if (paladin.current_level >= 2 && isBlank(paladin.fighting_style_name))
                groups.add(group("PALADIN_STYLE", "选择圣武士战斗风格", 1, labeled(paladin.get_available_fighting_styles(), paladin::get_fighting_style_label, paladin::get_fighting_style_description)));
            if (paladin.current_level >= 3 && paladin.sacred_oath == Paladin_Oath.NONE)
                groups.add(group("PALADIN_OATH", "选择圣武誓言", 1, options(new String[][]{{"DEVOTION","奉献誓言","以诚实、勇气与慈悲守护正义。"},{"ANCIENTS","古贤誓言","守护生命、光明与自然之美。"},{"VENGEANCE","复仇誓言","不惜代价追猎最危险的邪恶。"}})));
        } else if (character.job instanceof Warlock_Class warlock) {
            pendingAsi = warlock.pending_asi_count;
            if (warlock.patron == Warlock_Patron.NONE)
                groups.add(group("WARLOCK_PATRON", "选择异界恩主", 1, options(new String[][]{{"FIEND","邪魔","以黑暗祝福与炼狱力量换取契约。"},{"ARCHFEY","妖精","获得魅惑、幻术与遁形力量。"},{"GREAT_OLD_ONE","旧日支配者","以心灵感应和异界知识扭曲现实。"}})));
            if (warlock.current_level >= 3 && warlock.pact_boon == Warlock_Pact.NONE)
                groups.add(group("WARLOCK_PACT", "选择契约恩赐", 1, options(new String[][]{{"BLADE","刃之契约","召唤并驾驭独特的契约武器。"},{"CHAIN","链之契约","获得能力远胜寻常魔宠的契约使魔。"},{"TOME","书之契约","获得影之书与额外戏法。"}})));
            addSpellGroup(groups, "WARLOCK_CANTRIPS", "选择邪术士戏法", warlock.cantrips_known - warlock.known_cantrip_keys.size(), withoutKnown(Spell_Library.get_warlock_cantrip_keys(), warlock.known_cantrip_keys));
            addSpellGroup(groups, "WARLOCK_SPELLS", "选择邪术士法术", warlock.spells_known_count - warlock.known_spell_keys.size(), withoutKnown(warlock.get_available_spell_options(), warlock.known_spell_keys));
            if (warlock.pending_invocation_choices > 0)
                groups.add(group("WARLOCK_INVOCATIONS", "选择邪术祈请", warlock.pending_invocation_choices, labeled(warlock.get_available_invocation_options(), warlock::get_invocation_label, warlock::get_invocation_description)));
        } else if (character.job instanceof Bard_Class bard) {
            pendingAsi = bard.pending_asi_count;
            if (bard.current_level >= 3 && bard.bard_college == Bard_College.NONE)
                groups.add(group("BARD_COLLEGE", "选择吟游学院", 1, options(new String[][]{{"LORE","逸闻学院","精通更多技能、秘闻与反制技巧。"},{"VALOR","勇气学院","强化武器、护甲与战斗激励。"}})));
            if (bard.pending_bonus_skill_choices > 0) groups.add(group("BARD_BONUS_SKILLS", "选择学院额外技能", bard.pending_bonus_skill_choices, plainOptions(bard.get_available_bonus_skill_options())));
            addSpellGroup(groups, "BARD_CANTRIPS", "选择吟游诗人戏法", bard.cantrips_known - bard.known_cantrip_keys.size(), bard.get_available_cantrip_options());
            addSpellGroup(groups, "BARD_SPELLS", "选择吟游诗人法术", bard.base_spells_known_count - bard.known_spell_keys.size(), bard.get_available_spell_options());
            if (bard.pending_expertise_choices > 0) groups.add(group("BARD_EXPERTISE", "选择专精技能", bard.pending_expertise_choices, plainOptions(bard.get_available_expertise_options())));
            if (bard.pending_magical_secret_choices > 0) addSpellGroup(groups, "BARD_MAGICAL_SECRETS", "选择魔法秘辛", bard.pending_magical_secret_choices, bard.get_available_magical_secret_options());
        }

        result.addProperty("class_label", classLabel);
        result.add("groups", groups);
        result.add("level_tree", ClassProgressionTreeBuilder.build(character));
        result.addProperty("pending_asi_count", pendingAsi);
        JsonArray feats = new JsonArray();
        for (String key : Feat_Library.get_available_feat_keys(character)) feats.add(option(key, Feat_Library.get_label(key), Feat_Library.get_summary_line(key)));
        result.add("feats", feats);
        return result;
    }

    private static void addSpellGroup(JsonArray groups, String type, String title, int required, List<String> keys) {
        if (required > 0) groups.add(group(type, title, required, spellOptions(keys)));
    }
    private static JsonObject group(String type, String title, int required, JsonArray options) {
        JsonObject group = new JsonObject(); group.addProperty("type", type); group.addProperty("title", title);
        group.addProperty("required", required); group.addProperty("multiple", required > 1); group.add("options", options); return group;
    }
    private static JsonArray spellOptions(List<String> keys) {
        JsonArray result = new JsonArray(); for (String key : keys) { Spell_Definition spell = Spell_Library.get_spell(key); if (spell != null) result.add(option(key, spell.to_brief_label(), spell.school_or_theme + " · " + spell.short_description)); } return result;
    }
    private static JsonArray plainOptions(List<String> keys) { JsonArray result = new JsonArray(); for (String key : keys) result.add(option(key, key, "")); return result; }
    private static JsonArray labeled(List<String> keys, java.util.function.Function<String,String> label, java.util.function.Function<String,String> description) { JsonArray result = new JsonArray(); for (String key : keys) result.add(option(key, label.apply(key), description.apply(key))); return result; }
    private static JsonArray styleOptions(Fighter_Class fighter, boolean extra) { return labeled(fighter.get_available_fighting_styles(extra), fighter::get_fighting_style_label, fighter::get_fighting_style_description); }
    private static JsonObject option(String key, String label, String description) { JsonObject item = new JsonObject(); item.addProperty("key", key); item.addProperty("label", label); item.addProperty("description", description); return item; }
    private static JsonArray options(String[][] values) { JsonArray result = new JsonArray(); for (String[] value : values) result.add(option(value[0], value[1], value[2])); return result; }
    private static List<String> withoutKnown(List<String> source, List<String> known) { List<String> result = new ArrayList<>(source); result.removeAll(known); return result; }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static JsonArray wizardSubclasses() { return options(new String[][]{{"ABJURATION","防护学派","创造奥术结界并强化反制。"},{"CONJURATION","咒法学派","强化召唤与短距传送。"},{"DIVINATION","预言学派","以预兆骰改变关键结果。"},{"ENCHANTMENT","惑控学派","专精魅惑与精神控制。"},{"EVOCATION","塑能学派","精确塑造高伤害元素法术。"},{"ILLUSION","幻术学派","制造更真实、更灵活的幻象。"},{"NECROMANCY","死灵学派","操纵生命能量与不死仆从。"},{"TRANSMUTATION","变化学派","改变物质、生物与自身形态。"}}); }
    private static JsonArray dragonAncestries() { return options(new String[][]{{"黑龙 (Black Dragon) - 强酸","黑龙 · 强酸","强化强酸元素血脉。"},{"蓝龙 (Blue Dragon) - 闪电","蓝龙 · 闪电","强化闪电元素血脉。"},{"黄铜龙 (Brass Dragon) - 火焰","黄铜龙 · 火焰","强化火焰元素血脉。"},{"青铜龙 (Bronze Dragon) - 闪电","青铜龙 · 闪电","强化闪电元素血脉。"},{"赤铜龙 (Copper Dragon) - 强酸","赤铜龙 · 强酸","强化强酸元素血脉。"},{"金龙 (Gold Dragon) - 火焰","金龙 · 火焰","强化火焰元素血脉。"},{"绿龙 (Green Dragon) - 毒素","绿龙 · 毒素","强化毒素元素血脉。"},{"红龙 (Red Dragon) - 火焰","红龙 · 火焰","强化火焰元素血脉。"},{"银龙 (Silver Dragon) - 寒冷","银龙 · 寒冷","强化寒冷元素血脉。"},{"白龙 (White Dragon) - 寒冷","白龙 · 寒冷","强化寒冷元素血脉。"}}); }
}
