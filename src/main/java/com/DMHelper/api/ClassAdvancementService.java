package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Character_Class;
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
import com.DMHelper.basic.spell.Spell_Library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ClassAdvancementService {
    private static final Set<String> ABILITIES = Set.of("STR", "DEX", "CON", "INT", "WIS", "CHA");
    private static final Set<String> DRAGON_ANCESTRIES = Set.of(
            "黑龙 (Black Dragon) - 强酸", "蓝龙 (Blue Dragon) - 闪电", "黄铜龙 (Brass Dragon) - 火焰",
            "青铜龙 (Bronze Dragon) - 闪电", "赤铜龙 (Copper Dragon) - 强酸", "金龙 (Gold Dragon) - 火焰",
            "绿龙 (Green Dragon) - 毒素", "红龙 (Red Dragon) - 火焰", "银龙 (Silver Dragon) - 寒冷",
            "白龙 (White Dragon) - 寒冷"
    );

    private ClassAdvancementService() {}

    static void apply(Character_Sheet character, String type, String value, List<String> values) {
        requireValue(type, "缺少升级选项类型");
        if (character.job instanceof Fighter_Class) {
            FighterAdvancementService.apply(character, type, value, values);
            return;
        }

        switch (type) {
            case "ASI_PLUS_TWO" -> applyAbilityIncrease(character, java.util.Arrays.asList(value, value), false);
            case "ASI_PLUS_ONE_TWO" -> applyAbilityIncrease(character, values, true);
            case "FEAT" -> chooseFeat(character, value);
            default -> applyClassChoice(character, type, value, values);
        }
        character.recalculate_derived_stats();
    }

    private static void applyClassChoice(Character_Sheet character, String type, String value, List<String> values) {
        if (character.job instanceof Wizard_Class wizard) applyWizard(character, wizard, type, value, values);
        else if (character.job instanceof Sorcerer_Class sorcerer) applySorcerer(character, sorcerer, type, value, values);
        else if (character.job instanceof Paladin_Class paladin) applyPaladin(character, paladin, type, value);
        else if (character.job instanceof Warlock_Class warlock) applyWarlock(character, warlock, type, value, values);
        else if (character.job instanceof Bard_Class bard) applyBard(character, bard, type, value, values);
        else throw new IllegalArgumentException("当前职业暂不支持该升级选项");
    }

    private static void applyWizard(Character_Sheet character, Wizard_Class wizard, String type, String value, List<String> values) {
        switch (type) {
            case "WIZARD_SUBCLASS" -> {
                if (wizard.current_level < 2 || wizard.wizard_subclass != Wizard_Subclass.NONE) throw new IllegalArgumentException("当前不能选择奥术传承");
                wizard.wizard_subclass = enumValue(Wizard_Subclass.class, value, Wizard_Subclass.NONE, "请选择有效的奥术传承");
                character.record_advancement("选择奥术传承：" + wizard.get_subclass_name());
            }
            case "WIZARD_CANTRIPS" -> {
                int required = wizard.cantrips_known - wizard.known_cantrip_keys.size();
                List<String> selected = validatedSelection(values, required, wizard.get_available_cantrip_options(), "法师戏法");
                List<String> combined = new ArrayList<>(wizard.known_cantrip_keys); combined.addAll(selected); wizard.set_known_cantrips(combined);
                recordSpells(character, "学习法师戏法", selected);
            }
            case "WIZARD_SPELLBOOK" -> {
                int required = wizard.spells_in_spellbook - wizard.spellbook_spell_keys.size();
                List<String> selected = validatedSelection(values, required, wizard.get_available_spellbook_options(), "法术书法术");
                wizard.learn_spells(selected); recordSpells(character, "抄录法术", selected);
            }
            default -> throw new IllegalArgumentException("不支持的法师升级选项");
        }
    }

    private static void applySorcerer(Character_Sheet character, Sorcerer_Class sorcerer, String type, String value, List<String> values) {
        switch (type) {
            case "SORCERER_ORIGIN" -> {
                if (sorcerer.sorcerous_origin != Sorcerous_Origin.NONE) throw new IllegalArgumentException("术法起源已经选择");
                sorcerer.sorcerous_origin = enumValue(Sorcerous_Origin.class, value, Sorcerous_Origin.NONE, "请选择有效的术法起源");
                character.record_advancement("选择术法起源：" + sorcerer.get_subclass_name());
            }
            case "DRAGON_ANCESTRY" -> {
                if (sorcerer.sorcerous_origin != Sorcerous_Origin.DRACONIC_BLOODLINE || !isBlank(sorcerer.dragon_ancestry)) throw new IllegalArgumentException("当前不能选择龙族先祖");
                if (!DRAGON_ANCESTRIES.contains(value)) throw new IllegalArgumentException("请选择有效的龙族先祖");
                sorcerer.dragon_ancestry = value; character.record_advancement("选择龙族先祖：" + value);
            }
            case "SORCERER_METAMAGIC" -> {
                int required = sorcerer.get_expected_metamagic_count() - sorcerer.metamagic_keys.size();
                List<String> selected = validatedSelection(values, required, sorcerer.get_available_metamagic_options(), "超魔技巧");
                sorcerer.metamagic_keys.addAll(selected);
                character.record_advancement("学习超魔技巧：" + String.join("、", selected.stream().map(sorcerer::get_metamagic_label).toList()));
            }
            case "SORCERER_CANTRIPS" -> addKnownSpells(character, values, sorcerer.cantrips_known - sorcerer.known_cantrip_keys.size(),
                    Spell_Library.get_sorcerer_cantrip_keys(), sorcerer.known_cantrip_keys, sorcerer::set_known_cantrips, "学习术士戏法");
            case "SORCERER_SPELLS" -> addKnownSpells(character, values, sorcerer.spells_known_count - sorcerer.known_spell_keys.size(),
                    sorcerer.get_available_spell_options(), sorcerer.known_spell_keys, sorcerer::set_known_spells, "学习术士法术");
            default -> throw new IllegalArgumentException("不支持的术士升级选项");
        }
    }

    private static void applyPaladin(Character_Sheet character, Paladin_Class paladin, String type, String value) {
        switch (type) {
            case "PALADIN_STYLE" -> {
                if (paladin.current_level < 2 || !isBlank(paladin.fighting_style_name) || !paladin.get_available_fighting_styles().contains(value)) throw new IllegalArgumentException("请选择有效的圣武士战斗风格");
                paladin.fighting_style_name = value; character.record_advancement("选择圣武士战斗风格：" + paladin.get_fighting_style_label(value));
            }
            case "PALADIN_OATH" -> {
                if (paladin.current_level < 3 || paladin.sacred_oath != Paladin_Oath.NONE) throw new IllegalArgumentException("当前不能选择圣武誓言");
                paladin.sacred_oath = enumValue(Paladin_Oath.class, value, Paladin_Oath.NONE, "请选择有效的圣武誓言");
                character.record_advancement("立下圣武誓言：" + paladin.get_subclass_name());
            }
            default -> throw new IllegalArgumentException("不支持的圣武士升级选项");
        }
    }

    private static void applyWarlock(Character_Sheet character, Warlock_Class warlock, String type, String value, List<String> values) {
        switch (type) {
            case "WARLOCK_PATRON" -> {
                if (warlock.patron != Warlock_Patron.NONE) throw new IllegalArgumentException("异界恩主已经选择");
                warlock.patron = enumValue(Warlock_Patron.class, value, Warlock_Patron.NONE, "请选择有效的异界恩主");
                character.record_advancement("缔结异界恩主契约：" + warlock.get_subclass_name());
            }
            case "WARLOCK_PACT" -> {
                if (warlock.current_level < 3 || warlock.pact_boon != Warlock_Pact.NONE) throw new IllegalArgumentException("当前不能选择契约恩赐");
                warlock.pact_boon = enumValue(Warlock_Pact.class, value, Warlock_Pact.NONE, "请选择有效的契约恩赐");
                character.record_advancement("获得契约恩赐：" + value);
            }
            case "WARLOCK_INVOCATIONS" -> {
                int required = warlock.get_expected_invocation_count() - warlock.invocation_keys.size();
                List<String> selected = validatedSelection(values, required, warlock.get_available_invocation_options(), "邪术祈请");
                warlock.invocation_keys.addAll(selected);
                character.record_advancement("学习邪术祈请：" + String.join("、", selected.stream().map(warlock::get_invocation_label).toList()));
            }
            case "WARLOCK_CANTRIPS" -> addKnownSpells(character, values, warlock.cantrips_known - warlock.known_cantrip_keys.size(),
                    Spell_Library.get_warlock_cantrip_keys(), warlock.known_cantrip_keys, warlock::set_known_cantrips, "学习邪术士戏法");
            case "WARLOCK_SPELLS" -> addKnownSpells(character, values, warlock.spells_known_count - warlock.known_spell_keys.size(),
                    withoutKnown(warlock.get_available_spell_options(), warlock.known_spell_keys), warlock.known_spell_keys, warlock::set_known_spells, "学习邪术士法术");
            default -> throw new IllegalArgumentException("不支持的邪术士升级选项");
        }
    }

    private static void applyBard(Character_Sheet character, Bard_Class bard, String type, String value, List<String> values) {
        switch (type) {
            case "BARD_COLLEGE" -> {
                if (bard.current_level < 3 || bard.bard_college != Bard_College.NONE) throw new IllegalArgumentException("当前不能选择吟游学院");
                bard.bard_college = enumValue(Bard_College.class, value, Bard_College.NONE, "请选择有效的吟游学院");
                character.record_advancement("加入吟游学院：" + bard.get_subclass_name());
            }
            case "BARD_BONUS_SKILLS" -> {
                List<String> selected = validatedSelection(values, bard.pending_bonus_skill_choices, bard.get_available_bonus_skill_options(), "额外技能");
                bard.add_lore_bonus_skills(selected); character.record_advancement("获得学院额外技能：" + String.join("、", selected));
            }
            case "BARD_EXPERTISE" -> {
                List<String> selected = validatedSelection(values, bard.pending_expertise_choices, bard.get_available_expertise_options(), "专精技能");
                bard.add_expertise_skills(selected); character.record_advancement("获得技能专精：" + String.join("、", selected));
            }
            case "BARD_MAGICAL_SECRETS" -> {
                List<String> selected = validatedSelection(values, bard.pending_magical_secret_choices, bard.get_available_magical_secret_options(), "魔法秘辛");
                bard.add_magical_secret_spells(selected); recordSpells(character, "获得魔法秘辛", selected);
            }
            case "BARD_CANTRIPS" -> addKnownSpells(character, values, bard.cantrips_known - bard.known_cantrip_keys.size(),
                    bard.get_available_cantrip_options(), bard.known_cantrip_keys, bard::set_known_cantrips, "学习吟游诗人戏法");
            case "BARD_SPELLS" -> addKnownSpells(character, values, bard.base_spells_known_count - bard.known_spell_keys.size(),
                    bard.get_available_spell_options(), bard.known_spell_keys, bard::set_known_spells, "学习吟游诗人法术");
            default -> throw new IllegalArgumentException("不支持的吟游诗人升级选项");
        }
    }

    private static void addKnownSpells(Character_Sheet character, List<String> values, int required, List<String> available,
                                       List<String> current, java.util.function.Consumer<List<String>> setter, String logLabel) {
        List<String> selected = validatedSelection(values, required, withoutKnown(available, current), logLabel);
        List<String> combined = new ArrayList<>(current); combined.addAll(selected); setter.accept(combined); recordSpells(character, logLabel, selected);
    }

    private static List<String> withoutKnown(List<String> available, List<String> known) {
        List<String> result = new ArrayList<>(available); result.removeAll(known); return result;
    }

    private static List<String> validatedSelection(List<String> values, int required, List<String> available, String label) {
        if (required <= 0) throw new IllegalArgumentException("当前没有待选择的" + label);
        if (values == null || values.size() != required || new HashSet<>(values).size() != required) throw new IllegalArgumentException("需要选择 " + required + " 个不同的" + label);
        if (!available.containsAll(values)) throw new IllegalArgumentException("包含无效或已经选择的" + label);
        return values;
    }

    private static void recordSpells(Character_Sheet character, String label, List<String> keys) {
        character.record_advancement(label + "：" + String.join("、", keys.stream().map(key -> {
            var spell = Spell_Library.get_spell(key); return spell == null ? key : spell.display_name;
        }).toList()));
    }

    private static void applyAbilityIncrease(Character_Sheet character, List<String> abilities, boolean requireDifferent) {
        requirePendingAsi(character.job);
        if (abilities == null || abilities.size() != 2) throw new IllegalArgumentException("属性提升分配不完整");
        String first = normalizeAbility(abilities.get(0)), second = normalizeAbility(abilities.get(1));
        if (requireDifferent && first.equals(second)) throw new IllegalArgumentException("请选择两项不同的属性");
        if (getAbility(character, first) >= 20 || getAbility(character, second) >= 20 || (first.equals(second) && getAbility(character, first) > 18)) throw new IllegalArgumentException("属性值不能超过 20");
        increaseAbility(character, first); increaseAbility(character, second); character.job.used_asi_choices++;
        character.record_advancement(first.equals(second) ? "属性值提升：" + first + " +2" : "属性值提升：" + first + " +1，" + second + " +1");
    }

    private static void chooseFeat(Character_Sheet character, String feat) {
        requirePendingAsi(character.job); requireValue(feat, "请选择专长");
        if (!Feat_Library.get_available_feat_keys(character).contains(feat)) throw new IllegalArgumentException("该专长不可用或已经选择");
        character.job.feat_names.add(feat); character.job.used_asi_choices++;
        character.record_advancement("获得专长：" + Feat_Library.get_summary_line(feat));
    }

    private static void requirePendingAsi(Character_Class job) {
        int pending = job instanceof Wizard_Class c ? c.pending_asi_count : job instanceof Sorcerer_Class c ? c.pending_asi_count
                : job instanceof Paladin_Class c ? c.pending_asi_count : job instanceof Warlock_Class c ? c.pending_asi_count
                : job instanceof Bard_Class c ? c.pending_asi_count : 0;
        if (pending <= 0) throw new IllegalArgumentException("当前没有待处理的属性提升或专长");
    }

    private static String normalizeAbility(String ability) {
        String normalized = ability == null ? "" : ability.trim().toUpperCase();
        if (!ABILITIES.contains(normalized)) throw new IllegalArgumentException("无效的属性类型");
        return normalized;
    }

    private static int getAbility(Character_Sheet c, String a) { return switch (a) { case "STR" -> c.stats.str; case "DEX" -> c.stats.dex; case "CON" -> c.stats.con; case "INT" -> c.stats.intel; case "WIS" -> c.stats.wis; case "CHA" -> c.stats.cha; default -> 0; }; }
    private static void increaseAbility(Character_Sheet c, String a) { switch (a) { case "STR" -> c.stats.str++; case "DEX" -> c.stats.dex++; case "CON" -> c.stats.con++; case "INT" -> c.stats.intel++; case "WIS" -> c.stats.wis++; case "CHA" -> c.stats.cha++; } }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E none, String message) {
        try { E selected = Enum.valueOf(type, value); if (selected == none) throw new IllegalArgumentException(); return selected; }
        catch (RuntimeException error) { throw new IllegalArgumentException(message); }
    }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static void requireValue(String value, String message) { if (isBlank(value)) throw new IllegalArgumentException(message); }
}
