package com.DMHelper.basic.menus;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.feat.Feat_Library;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Oath;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerous_Origin;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Patron;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Subclass;
import com.DMHelper.basic.spell.Spell_Library;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 升级与建角选择流程辅助器。
 * 所有“需要玩家当场做决定”的成长项，例如技能、子职业、专长、法术、战技，都集中在这里处理。
 */
public class Character_Advancement_Helper {

    public static void configure_new_character(Component parent, Character_Sheet character) {
        // 新角色创建完成后，先补齐职业起始选择，再统一刷新角色派生数据。
        choose_starting_skills(parent, character.job);

        if (character.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) character.job;
            String chosenStyle = choose_single_option(
                    parent,
                    "选择战斗风格",
                    "请选择战士 1 级战斗风格：",
                    build_fighting_style_labels(fighter.get_available_fighting_styles(false), fighter)
            );
            fighter.fighting_style_name = to_fighting_style_key(chosenStyle);
            character.record_advancement("1级选择战斗风格：" + fighter.get_fighting_style_label(fighter.fighting_style_name));
        } else if (character.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) character.job;
            choose_sorcerer_origin(parent, character, sorcerer);
        } else if (character.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) character.job;
            choose_warlock_patron(parent, character, warlock);
        }

        resolve_spellcasting_progression(parent, character);
        character.recalculate_derived_stats();
    }

    public static void process_pending_choices(Component parent, Character_Sheet character) {
        boolean processedChoice;

        do {
            // 某些选择会解锁下一层选择，所以这里循环处理，直到没有新的待办项为止。
            processedChoice = false;

            if (character.job instanceof Fighter_Class) {
                Fighter_Class fighter = (Fighter_Class) character.job;

                if (character.job.current_level >= 3 && fighter.fighter_subclass == Fighter_Subclass.NONE) {
                    String pickedSubclass = choose_single_option(
                            parent,
                            "选择战士子职业",
                            build_fighter_subclass_prompt(),
                            Arrays.asList("冠军勇士 (Champion)", "战斗大师 (Battle Master)")
                    );
                    fighter.fighter_subclass = pickedSubclass.contains("冠军勇士")
                            ? Fighter_Subclass.CHAMPION
                            : Fighter_Subclass.BATTLE_MASTER;
                    character.record_advancement("选择战士子职业：" + fighter.get_subclass_name());
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                int pendingManeuvers = Math.max(0, fighter.get_expected_maneuver_count() - fighter.maneuver_names.size());
                if (fighter.fighter_subclass == Fighter_Subclass.BATTLE_MASTER && pendingManeuvers > 0) {
                    List<String> chosenManeuvers = choose_multi_options(
                            parent,
                            "选择战技",
                            build_maneuver_prompt(fighter, pendingManeuvers),
                            build_maneuver_labels(fighter.get_available_maneuvers(), fighter),
                            pendingManeuvers
                    );
                    List<String> maneuverKeys = new ArrayList<>();
                    List<String> maneuverLabels = new ArrayList<>();
                    for (String chosenManeuver : chosenManeuvers) {
                        String key = to_maneuver_key(chosenManeuver);
                        maneuverKeys.add(key);
                        maneuverLabels.add(fighter.get_maneuver_label(key));
                    }
                    fighter.maneuver_names.addAll(maneuverKeys);
                    character.record_advancement("学习战技：" + String.join("、", maneuverLabels));
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                if (fighter.fighter_subclass == Fighter_Subclass.CHAMPION
                        && character.job.current_level >= 10
                        && (fighter.extra_fighting_style_name == null || fighter.extra_fighting_style_name.trim().isEmpty())) {
                    String chosenStyle = choose_single_option(
                            parent,
                            "选择额外战斗风格",
                            "请选择冠军勇士额外战斗风格：",
                            build_fighting_style_labels(fighter.get_available_fighting_styles(true), fighter)
                    );
                    fighter.extra_fighting_style_name = to_fighting_style_key(chosenStyle);
                    character.record_advancement("冠军勇士获得额外战斗风格：" + fighter.get_fighting_style_label(fighter.extra_fighting_style_name));
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            if (character.job instanceof Wizard_Class) {
                Wizard_Class wizard = (Wizard_Class) character.job;
                if (character.job.current_level >= 2 && wizard.wizard_subclass == Wizard_Subclass.NONE) {
                    String pickedSubclass = choose_single_option(
                            parent,
                            "选择奥术传承",
                            build_wizard_subclass_prompt(),
                            Arrays.asList(
                                    "防护学派 (School of Abjuration)",
                                    "咒法学派 (School of Conjuration)",
                                    "预言学派 (School of Divination)",
                                    "惑控学派 (School of Enchantment)",
                                    "塑能学派 (School of Evocation)",
                                    "幻术学派 (School of Illusion)",
                                    "死灵学派 (School of Necromancy)",
                                    "变化学派 (School of Transmutation)")
                    );
                    wizard.wizard_subclass = to_wizard_subclass(pickedSubclass);
                    character.record_advancement("选择奥术传承：" + wizard.get_subclass_name());
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            if (character.job instanceof Sorcerer_Class) {
                Sorcerer_Class sorcerer = (Sorcerer_Class) character.job;
                if (sorcerer.sorcerous_origin == Sorcerous_Origin.NONE) {
                    choose_sorcerer_origin(parent, character, sorcerer);
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                if (sorcerer.pending_metamagic_choices > 0) {
                    List<String> selectedMetamagic = choose_multi_options(
                            parent,
                            "选择超魔技巧",
                            build_metamagic_prompt(sorcerer, sorcerer.pending_metamagic_choices),
                            build_metamagic_labels(sorcerer.get_available_metamagic_options(), sorcerer),
                            sorcerer.pending_metamagic_choices
                    );
                    for (String metamagicLabel : selectedMetamagic) {
                        sorcerer.metamagic_keys.add(to_metamagic_key(metamagicLabel));
                    }
                    character.record_advancement("学习超魔技巧：" + String.join("、", selectedMetamagic));
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            if (character.job instanceof Warlock_Class) {
                Warlock_Class warlock = (Warlock_Class) character.job;
                if (warlock.patron == Warlock_Patron.NONE) {
                    choose_warlock_patron(parent, character, warlock);
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                if (warlock.pending_invocation_choices > 0) {
                    List<String> selectedInvocations = choose_multi_options(
                            parent,
                            "选择邪术祈请",
                            build_warlock_invocation_prompt(warlock, warlock.pending_invocation_choices),
                            build_warlock_invocation_labels(warlock),
                            warlock.pending_invocation_choices
                    );
                    for (String label : selectedInvocations) {
                        warlock.invocation_keys.add(to_warlock_invocation_key(label));
                    }
                    character.record_advancement("学习邪术祈请：" + String.join("、", selectedInvocations));
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            if (character.job instanceof Paladin_Class) {
                Paladin_Class paladin = (Paladin_Class) character.job;
                if (character.job.current_level >= 2 && (paladin.fighting_style_name == null || paladin.fighting_style_name.trim().isEmpty())) {
                    String chosenStyle = choose_single_option(
                            parent,
                            "选择圣武士战斗风格",
                            "请选择圣武士战斗风格：",
                            build_paladin_style_labels(paladin.get_available_fighting_styles(), paladin)
                    );
                    paladin.fighting_style_name = to_paladin_style_key(chosenStyle);
                    character.record_advancement("圣武士选择战斗风格：" + paladin.get_fighting_style_label(paladin.fighting_style_name));
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }

                if (character.job.current_level >= 3 && paladin.sacred_oath == Paladin_Oath.NONE) {
                    String chosenOath = choose_single_option(
                            parent,
                            "选择圣武誓言",
                            build_paladin_oath_prompt(),
                            Arrays.asList("奉献誓言 (Oath of Devotion)", "远古誓言 (Oath of the Ancients)", "复仇誓言 (Oath of Vengeance)")
                    );
                    paladin.sacred_oath = to_paladin_oath(chosenOath);
                    character.record_advancement("选择圣武誓言：" + paladin.get_subclass_name());
                    character.recalculate_derived_stats();
                    processedChoice = true;
                }
            }

            if (resolve_spellcasting_progression(parent, character)) {
                character.recalculate_derived_stats();
                processedChoice = true;
            }

            while (get_pending_asi_count(character.job) > 0) {
                resolve_asi_or_feat(parent, character);
                character.job.used_asi_choices++;
                character.recalculate_derived_stats();
                processedChoice = true;
            }
        } while (processedChoice);
    }

    private static boolean resolve_spellcasting_progression(Component parent, Character_Sheet character) {
        if (character.job instanceof Wizard_Class) {
            return resolve_wizard_spell_choices(parent, character, (Wizard_Class) character.job);
        }
        if (character.job instanceof Sorcerer_Class) {
            return resolve_sorcerer_spell_choices(parent, character, (Sorcerer_Class) character.job);
        }
        if (character.job instanceof Warlock_Class) {
            return resolve_warlock_spell_choices(parent, character, (Warlock_Class) character.job);
        }
        return false;
    }

    private static boolean resolve_wizard_spell_choices(Component parent, Character_Sheet character, Wizard_Class wizard) {
        boolean changed = false;
        int missingCantrips = Math.max(0, wizard.cantrips_known - wizard.known_cantrip_keys.size());
        if (missingCantrips > 0) {
            List<String> selected = Spell_Management_Helper.open_required_selection_dialog(
                    parent,
                    "选择法师戏法",
                    "请选择 " + missingCantrips + " 个新的法师戏法。",
                    wizard.get_available_cantrip_options(),
                    new ArrayList<>(),
                    missingCantrips
            );
            if (selected.size() == missingCantrips) {
                wizard.known_cantrip_keys.addAll(selected);
                character.record_advancement("学习法师戏法：" + join_spell_names(selected));
                changed = true;
            }
        }

        int missingSpellbook = Math.max(0, wizard.spells_in_spellbook - wizard.spellbook_spell_keys.size());
        if (missingSpellbook > 0) {
            List<String> selected = Spell_Management_Helper.open_required_selection_dialog(
                    parent,
                    "抄录法术书",
                    "请选择 " + missingSpellbook + " 个抄录进法术书的新法术。当前最高可学习 " + wizard.get_max_spell_level() + " 环。",
                    wizard.get_available_spellbook_options(),
                    new ArrayList<>(),
                    missingSpellbook
            );
            if (selected.size() == missingSpellbook) {
                wizard.learn_spells(selected);
                character.record_advancement("法术书新增法术：" + join_spell_names(selected));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean resolve_sorcerer_spell_choices(Component parent, Character_Sheet character, Sorcerer_Class sorcerer) {
        boolean changed = false;
        int missingCantrips = Math.max(0, sorcerer.cantrips_known - sorcerer.known_cantrip_keys.size());
        if (missingCantrips > 0) {
            List<String> selected = Spell_Management_Helper.open_required_selection_dialog(
                    parent,
                    "选择术士戏法",
                    "请选择 " + missingCantrips + " 个新的术士戏法。",
                    get_missing_spell_keys(Spell_Library.get_sorcerer_cantrip_keys(), sorcerer.known_cantrip_keys),
                    new ArrayList<>(),
                    missingCantrips
            );
            if (selected.size() == missingCantrips) {
                sorcerer.known_cantrip_keys.addAll(selected);
                character.record_advancement("学习术士戏法：" + join_spell_names(selected));
                changed = true;
            }
        }

        int missingSpells = Math.max(0, sorcerer.spells_known_count - sorcerer.known_spell_keys.size());
        if (missingSpells > 0) {
            List<String> selected = Spell_Management_Helper.open_required_selection_dialog(
                    parent,
                    "选择术士法术",
                    "请选择 " + missingSpells + " 个新的术士法术。当前最高可学 " + sorcerer.get_max_spell_level() + " 环。",
                    sorcerer.get_available_spell_options(),
                    new ArrayList<>(),
                    missingSpells
            );
            if (selected.size() == missingSpells) {
                sorcerer.known_spell_keys.addAll(selected);
                character.record_advancement("学习术士法术：" + join_spell_names(selected));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean resolve_warlock_spell_choices(Component parent, Character_Sheet character, Warlock_Class warlock) {
        boolean changed = false;
        int missingCantrips = Math.max(0, warlock.cantrips_known - warlock.known_cantrip_keys.size());
        if (missingCantrips > 0) {
            List<String> selected = Spell_Management_Helper.open_required_selection_dialog(
                    parent,
                    "选择邪术士戏法",
                    "请选择 " + missingCantrips + " 个新的邪术士戏法。",
                    get_missing_spell_keys(Spell_Library.get_warlock_cantrip_keys(), warlock.known_cantrip_keys),
                    new ArrayList<>(),
                    missingCantrips
            );
            if (selected.size() == missingCantrips) {
                warlock.known_cantrip_keys.addAll(selected);
                character.record_advancement("学习邪术士戏法：" + join_spell_names(selected));
                changed = true;
            }
        }

        int missingSpells = Math.max(0, warlock.spells_known_count - warlock.known_spell_keys.size());
        if (missingSpells > 0) {
            List<String> selected = Spell_Management_Helper.open_required_selection_dialog(
                    parent,
                    "选择邪术士法术",
                    "请选择 " + missingSpells + " 个新的邪术士法术。当前契约法术按 " + warlock.pact_slot_level + " 环施放。",
                    get_missing_spell_keys(warlock.get_available_spell_options(), warlock.known_spell_keys),
                    new ArrayList<>(),
                    missingSpells
            );
            if (selected.size() == missingSpells) {
                warlock.known_spell_keys.addAll(selected);
                character.record_advancement("学习邪术士法术：" + join_spell_names(selected));
                changed = true;
            }
        }
        return changed;
    }

    private static List<String> get_missing_spell_keys(List<String> sourceKeys, List<String> knownKeys) {
        List<String> missing = new ArrayList<>(sourceKeys);
        missing.removeAll(knownKeys);
        return missing;
    }

    private static String join_spell_names(List<String> spellKeys) {
        List<String> names = new ArrayList<>();
        for (String spellKey : spellKeys) {
            if (Spell_Library.get_spell(spellKey) != null) {
                names.add(Spell_Library.get_spell(spellKey).display_name);
            } else {
                names.add(spellKey);
            }
        }
        return String.join("、", names);
    }

    private static void choose_starting_skills(Component parent, Character_Class job) {
        if (job.skill_choose_count <= 0) {
            return;
        }
        if (job.skill_proficiencies.size() == job.skill_choose_count) {
            return;
        }

        List<String> chosenSkills = choose_multi_options(
                parent,
                "选择技能熟练",
                "请选择 " + job.skill_choose_count + " 项技能熟练：",
                job.skill_options,
                job.skill_choose_count
        );
        job.select_skills(chosenSkills);
    }

    private static void resolve_asi_or_feat(Component parent, Character_Sheet character) {
        String decision = choose_single_option(
                parent,
                "属性值提升或专长",
                "本次升级请选择属性值提升还是专长：",
                Arrays.asList("属性值提升", "专长")
        );

        if ("专长".equals(decision)) {
            choose_feat(parent, character);
        } else {
            apply_ability_score_improvement(parent, character);
        }
    }

    private static void choose_feat(Component parent, Character_Sheet character) {
        List<String> availableFeats = Feat_Library.get_available_feat_keys(character);
        if (availableFeats.isEmpty()) {
            apply_ability_score_improvement(parent, character);
            return;
        }

        List<String> featLabels = build_feat_labels(availableFeats);
        String selectedLabel = choose_single_option(
                parent,
                "选择专长",
                build_feat_prompt(availableFeats),
                featLabels
        );
        String featName = to_feat_key(selectedLabel);
        character.job.feat_names.add(featName);
        character.record_advancement("获得专长：" + Feat_Library.get_summary_line(featName));
    }

    private static String build_feat_prompt(List<String> featKeys) {
        StringBuilder sb = new StringBuilder("请选择专长：\n");
        for (String featKey : featKeys) {
            sb.append(Feat_Library.get_prompt_line(featKey)).append("\n");
        }
        return sb.toString();
    }

    private static void apply_ability_score_improvement(Component parent, Character_Sheet character) {
        String mode = choose_single_option(
                parent,
                "分配属性提升",
                "请选择本次属性提升的分配方式：",
                Arrays.asList("+2 到一项属性", "+1 到两项属性")
        );

        List<String> stats = Arrays.asList("力量", "敏捷", "体质", "智力", "感知", "魅力");

        if ("+1 到两项属性".equals(mode)) {
            List<String> chosenStats = choose_multi_options(
                    parent,
                    "选择两项属性",
                    "请选择 2 项不同属性，各提升 +1：",
                    stats,
                    2
            );
            for (String statName : chosenStats) {
                increase_stat(character, statName, 1);
            }
            character.record_advancement("属性值提升：" + chosenStats.get(0) + " +1，" + chosenStats.get(1) + " +1");
        } else {
            String chosenStat = choose_single_option(parent, "选择属性", "请选择提升 +2 的属性：", stats);
            increase_stat(character, chosenStat, 2);
            character.record_advancement("属性值提升：" + chosenStat + " +2");
        }
    }

    private static void increase_stat(Character_Sheet character, String stat_name, int amount) {
        if ("力量".equals(stat_name)) {
            character.stats.str = Math.min(20, character.stats.str + amount);
        } else if ("敏捷".equals(stat_name)) {
            character.stats.dex = Math.min(20, character.stats.dex + amount);
        } else if ("体质".equals(stat_name)) {
            character.stats.con = Math.min(20, character.stats.con + amount);
        } else if ("智力".equals(stat_name)) {
            character.stats.intel = Math.min(20, character.stats.intel + amount);
        } else if ("感知".equals(stat_name)) {
            character.stats.wis = Math.min(20, character.stats.wis + amount);
        } else if ("魅力".equals(stat_name)) {
            character.stats.cha = Math.min(20, character.stats.cha + amount);
        }
    }

    private static int get_pending_asi_count(Character_Class job) {
        if (job instanceof Fighter_Class) return ((Fighter_Class) job).pending_asi_count;
        if (job instanceof Wizard_Class) return ((Wizard_Class) job).pending_asi_count;
        if (job instanceof Sorcerer_Class) return ((Sorcerer_Class) job).pending_asi_count;
        if (job instanceof Warlock_Class) return ((Warlock_Class) job).pending_asi_count;
        if (job instanceof Paladin_Class) return ((Paladin_Class) job).pending_asi_count;
        return 0;
    }

    private static List<String> build_fighting_style_labels(List<String> styleKeys, Fighter_Class fighter) {
        List<String> labels = new ArrayList<>();
        for (String styleKey : styleKeys) {
            labels.add(fighter.get_fighting_style_label(styleKey));
        }
        return labels;
    }

    private static String to_fighting_style_key(String label) {
        if (label.contains("Archery")) return "Archery";
        if (label.contains("Defense")) return "Defense";
        if (label.contains("Dueling")) return "Dueling";
        if (label.contains("Great Weapon Fighting")) return "Great Weapon Fighting";
        if (label.contains("Protection")) return "Protection";
        if (label.contains("Two-Weapon Fighting")) return "Two-Weapon Fighting";
        return label;
    }

    private static List<String> build_maneuver_labels(List<String> maneuverKeys, Fighter_Class fighter) {
        List<String> labels = new ArrayList<>();
        for (String maneuverKey : maneuverKeys) {
            labels.add(fighter.get_maneuver_label(maneuverKey));
        }
        return labels;
    }

    private static String to_maneuver_key(String label) {
        if (label.contains("Disarming Attack")) return "Disarming Attack";
        if (label.contains("Precision Attack")) return "Precision Attack";
        if (label.contains("Riposte")) return "Riposte";
        if (label.contains("Trip Attack")) return "Trip Attack";
        if (label.contains("Menacing Attack")) return "Menacing Attack";
        if (label.contains("Parry")) return "Parry";
        if (label.contains("Pushing Attack")) return "Pushing Attack";
        if (label.contains("Rally")) return "Rally";
        return label;
    }

    private static String build_fighter_subclass_prompt() {
        return "请选择战士子职业：\n"
                + "冠军勇士 (Champion)：偏向稳定输出与简单强力，被动强化重击范围、运动表现与生存能力。\n"
                + "战斗大师 (Battle Master)：通过卓越骰与战技进行控制、反击、支援与战术压制。";
    }

    private static String build_wizard_subclass_prompt() {
        return "请选择法师奥术传承：\n"
                + "防护学派 (School of Abjuration)：结界、防护、反制与抗法。\n"
                + "咒法学派 (School of Conjuration)：召唤、生物与空间转移。\n"
                + "预言学派 (School of Divination)：操纵命运与感知未来。\n"
                + "惑控学派 (School of Enchantment)：魅惑、控制与精神影响。\n"
                + "塑能学派 (School of Evocation)：直接伤害与元素爆发。\n"
                + "幻术学派 (School of Illusion)：制造幻象、欺骗感知。\n"
                + "死灵学派 (School of Necromancy)：汲取生命与操纵亡灵。\n"
                + "变化学派 (School of Transmutation)：改造物质与形态。";
    }

    private static Wizard_Subclass to_wizard_subclass(String label) {
        if (label.contains("防护")) return Wizard_Subclass.ABJURATION;
        if (label.contains("咒法")) return Wizard_Subclass.CONJURATION;
        if (label.contains("预言")) return Wizard_Subclass.DIVINATION;
        if (label.contains("惑控")) return Wizard_Subclass.ENCHANTMENT;
        if (label.contains("塑能")) return Wizard_Subclass.EVOCATION;
        if (label.contains("幻术")) return Wizard_Subclass.ILLUSION;
        if (label.contains("死灵")) return Wizard_Subclass.NECROMANCY;
        if (label.contains("变化")) return Wizard_Subclass.TRANSMUTATION;
        return Wizard_Subclass.EVOCATION;
    }

    private static void choose_sorcerer_origin(Component parent, Character_Sheet character, Sorcerer_Class sorcerer) {
        String selectedOrigin = choose_single_option(
                parent,
                "选择术法起源",
                build_sorcerer_origin_prompt(),
                Arrays.asList("龙脉术士 (Draconic Bloodline)", "狂野魔法术士 (Wild Magic)")
        );
        sorcerer.sorcerous_origin = selectedOrigin.contains("龙脉")
                ? Sorcerous_Origin.DRACONIC_BLOODLINE
                : Sorcerous_Origin.WILD_MAGIC;
        if (sorcerer.sorcerous_origin == Sorcerous_Origin.DRACONIC_BLOODLINE
                && (sorcerer.dragon_ancestry == null || sorcerer.dragon_ancestry.trim().isEmpty())) {
            sorcerer.dragon_ancestry = choose_single_option(
                    parent,
                    "选择龙脉先祖",
                    "请选择龙脉先祖：",
                    Arrays.asList(
                            "黑龙 (Black Dragon) - 强酸",
                            "蓝龙 (Blue Dragon) - 闪电",
                            "黄铜龙 (Brass Dragon) - 火焰",
                            "青铜龙 (Bronze Dragon) - 闪电",
                            "赤铜龙 (Copper Dragon) - 强酸",
                            "金龙 (Gold Dragon) - 火焰",
                            "绿龙 (Green Dragon) - 毒素",
                            "红龙 (Red Dragon) - 火焰",
                            "银龙 (Silver Dragon) - 寒冷",
                            "白龙 (White Dragon) - 寒冷")
            );
        }
        character.record_advancement("选择术法起源：" + sorcerer.get_subclass_name());
    }

    private static String build_sorcerer_origin_prompt() {
        return "请选择术法起源：\n"
                + "龙脉术士 (Draconic Bloodline)：更耐打，强化元素法术，并在高等级获得龙翼。\n"
                + "狂野魔法术士 (Wild Magic)：法术可能触发随机魔涌，拥有极具戏剧性的混沌能力。";
    }

    private static void choose_warlock_patron(Component parent, Character_Sheet character, Warlock_Class warlock) {
        String selectedPatron = choose_single_option(
                parent,
                "选择异界恩主",
                build_warlock_patron_prompt(),
                Arrays.asList("邪魔恩主 (The Fiend)", "妖精恩主 (The Archfey)", "旧日支配者 (The Great Old One)")
        );
        if (selectedPatron.contains("邪魔")) warlock.patron = Warlock_Patron.FIEND;
        else if (selectedPatron.contains("妖精")) warlock.patron = Warlock_Patron.ARCHFEY;
        else warlock.patron = Warlock_Patron.GREAT_OLD_ONE;
        character.record_advancement("选择异界恩主：" + warlock.get_subclass_name());
    }

    private static String build_warlock_patron_prompt() {
        return "请选择邪术士的异界恩主：\n"
                + "邪魔恩主 (The Fiend)：偏向爆发、临时生命值与炼狱力量。\n"
                + "妖精恩主 (The Archfey)：偏向魅惑、恐惧、位移与生存。\n"
                + "旧日支配者 (The Great Old One)：偏向心灵、控制与诡异干扰。";
    }

    private static List<String> build_warlock_invocation_labels(Warlock_Class warlock) {
        List<String> labels = new ArrayList<>();
        for (String key : warlock.get_available_invocation_options()) {
            labels.add(warlock.get_invocation_label(key));
        }
        return labels;
    }

    private static String build_warlock_invocation_prompt(Warlock_Class warlock, int count) {
        StringBuilder sb = new StringBuilder("请选择 ").append(count).append(" 个邪术祈请：\n");
        for (String key : warlock.get_available_invocation_options()) {
            sb.append(warlock.get_invocation_label(key)).append("：").append(warlock.get_invocation_description(key)).append("\n");
        }
        return sb.toString();
    }

    private static String to_warlock_invocation_key(String label) {
        if (label.contains("Agonizing Blast")) return "Agonizing Blast";
        if (label.contains("Armor of Shadows")) return "Armor of Shadows";
        if (label.contains("Devil's Sight")) return "Devil's Sight";
        if (label.contains("Eldritch Sight")) return "Eldritch Sight";
        if (label.contains("Fiendish Vigor")) return "Fiendish Vigor";
        if (label.contains("Mask of Many Faces")) return "Mask of Many Faces";
        if (label.contains("Misty Visions")) return "Misty Visions";
        if (label.contains("Repelling Blast")) return "Repelling Blast";
        return label;
    }

    private static List<String> build_metamagic_labels(List<String> metamagicKeys, Sorcerer_Class sorcerer) {
        List<String> labels = new ArrayList<>();
        for (String key : metamagicKeys) {
            labels.add(sorcerer.get_metamagic_label(key));
        }
        return labels;
    }

    private static String build_metamagic_prompt(Sorcerer_Class sorcerer, int count) {
        StringBuilder sb = new StringBuilder("请选择 ").append(count).append(" 个超魔技巧：\n");
        for (String key : sorcerer.get_available_metamagic_options()) {
            sb.append(sorcerer.get_metamagic_label(key)).append("：").append(sorcerer.get_metamagic_description(key)).append("\n");
        }
        return sb.toString();
    }

    private static String to_metamagic_key(String label) {
        if (label.contains("Careful Spell")) return "Careful Spell";
        if (label.contains("Distant Spell")) return "Distant Spell";
        if (label.contains("Empowered Spell")) return "Empowered Spell";
        if (label.contains("Extended Spell")) return "Extended Spell";
        if (label.contains("Heightened Spell")) return "Heightened Spell";
        if (label.contains("Quickened Spell")) return "Quickened Spell";
        if (label.contains("Subtle Spell")) return "Subtle Spell";
        if (label.contains("Twinned Spell")) return "Twinned Spell";
        return label;
    }

    private static List<String> build_feat_labels(List<String> featKeys) {
        List<String> labels = new ArrayList<>();
        for (String featKey : featKeys) {
            labels.add(Feat_Library.get_label(featKey));
        }
        return labels;
    }

    private static String to_feat_key(String label) {
        for (String featKey : Feat_Library.get_all_feat_keys()) {
            if (label.contains("(" + featKey + ")")) {
                return featKey;
            }
        }
        return label;
    }

    private static List<String> build_paladin_style_labels(List<String> styleKeys, Paladin_Class paladin) {
        List<String> labels = new ArrayList<>();
        for (String key : styleKeys) {
            labels.add(paladin.get_fighting_style_label(key));
        }
        return labels;
    }

    private static String to_paladin_style_key(String label) {
        if (label.contains("Defense")) return "Defense";
        if (label.contains("Dueling")) return "Dueling";
        if (label.contains("Great Weapon Fighting")) return "Great Weapon Fighting";
        if (label.contains("Protection")) return "Protection";
        return label;
    }

    private static String build_paladin_oath_prompt() {
        return "请选择圣武誓言：\n"
                + "奉献誓言 (Oath of Devotion)：标准圣骑士路线，偏保护、净化与圣光打击。\n"
                + "远古誓言 (Oath of the Ancients)：自然与光明守护者，擅长抗法与持久战。\n"
                + "复仇誓言 (Oath of Vengeance)：专精追猎强敌与单体压制。";
    }

    private static Paladin_Oath to_paladin_oath(String label) {
        if (label.contains("奉献")) return Paladin_Oath.DEVOTION;
        if (label.contains("远古")) return Paladin_Oath.ANCIENTS;
        if (label.contains("复仇")) return Paladin_Oath.VENGEANCE;
        return Paladin_Oath.DEVOTION;
    }

    private static String build_maneuver_prompt(Fighter_Class fighter, int pendingManeuvers) {
        StringBuilder sb = new StringBuilder("请选择 ").append(pendingManeuvers).append(" 个战技：\n");
        for (String maneuverKey : fighter.get_available_maneuvers()) {
            sb.append(fighter.get_maneuver_label(maneuverKey))
                    .append("：")
                    .append(fighter.get_maneuver_description(maneuverKey))
                    .append("\n");
        }
        return sb.toString();
    }

    private static String choose_single_option(Component parent, String title, String prompt, List<String> options) {
        String[] optionArray = options.toArray(new String[0]);
        String selection = (String) JOptionPane.showInputDialog(
                parent,
                prompt,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                optionArray,
                optionArray.length > 0 ? optionArray[0] : null
        );

        if (selection == null || selection.trim().isEmpty()) {
            return optionArray.length > 0 ? optionArray[0] : "";
        }
        return selection;
    }

    private static List<String> choose_multi_options(Component parent, String title, String prompt, List<String> options, int requiredCount) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        MultiSelectDialog dialog = new MultiSelectDialog(owner, title, prompt, options, requiredCount);
        dialog.setVisible(true);

        List<String> selected = dialog.getSelectedValues();
        if (selected.size() != requiredCount) {
            return new ArrayList<>(options.subList(0, Math.min(requiredCount, options.size())));
        }
        return selected;
    }

    private static class MultiSelectDialog extends JDialog {
        private final List<String> allOptions;
        private final Set<String> selectedValues;
        private final int requiredCount;
        private final DefaultListModel<String> listModel;
        private final JList<String> optionList;
        private final JLabel selectedLabel;
        private final JButton confirmButton;
        private boolean confirmed;

        MultiSelectDialog(Window owner, String title, String prompt, List<String> options, int requiredCount) {
            super(owner, title, ModalityType.APPLICATION_MODAL);
            this.allOptions = new ArrayList<>(options);
            this.selectedValues = new LinkedHashSet<>();
            this.requiredCount = requiredCount;
            this.listModel = new DefaultListModel<>();
            this.optionList = new JList<>(this.listModel);
            this.selectedLabel = new JLabel();
            this.confirmButton = new JButton("确认选择");

            setSize(460, 420);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));

            JLabel promptLabel = new JLabel("<html>" + prompt.replace("\n", "<br>") + "<br><br>双击条目可切换已选状态。</html>");
            promptLabel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
            add(promptLabel, BorderLayout.NORTH);

            refreshListModel();
            this.optionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.optionList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JLabel label = new JLabel(value);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                label.setBackground(isSelected ? new Color(210, 228, 255) : Color.WHITE);
                if (selectedValues.contains(allOptions.get(index))) {
                    label.setForeground(new Color(20, 120, 40));
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else {
                    label.setForeground(Color.BLACK);
                }
                return label;
            });

            this.optionList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int index = optionList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            toggleSelection(allOptions.get(index));
                        }
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(this.optionList);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            add(scrollPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            this.selectedLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
            bottomPanel.add(this.selectedLabel, BorderLayout.NORTH);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton("取消");
            cancelButton.addActionListener(e -> dispose());
            this.confirmButton.addActionListener(e -> {
                this.confirmed = true;
                dispose();
            });
            buttonPanel.add(cancelButton);
            buttonPanel.add(this.confirmButton);
            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
            add(bottomPanel, BorderLayout.SOUTH);

            updateFooter();
        }

        private void toggleSelection(String value) {
            if (this.selectedValues.contains(value)) {
                this.selectedValues.remove(value);
            } else {
                if (this.selectedValues.size() >= this.requiredCount) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                this.selectedValues.add(value);
            }
            refreshListModel();
            updateFooter();
            this.optionList.repaint();
        }

        private void refreshListModel() {
            this.listModel.clear();
            for (String option : this.allOptions) {
                this.listModel.addElement(this.selectedValues.contains(option) ? "[已选] " + option : option);
            }
        }

        private void updateFooter() {
            List<String> selected = new ArrayList<>(this.selectedValues);
            String text = selected.isEmpty()
                    ? "已选择 0/" + this.requiredCount
                    : "已选择 " + selected.size() + "/" + this.requiredCount + "： " + String.join("、", selected);
            this.selectedLabel.setText(text);
            this.confirmButton.setEnabled(this.selectedValues.size() == this.requiredCount);
        }

        List<String> getSelectedValues() {
            if (!this.confirmed) {
                return new ArrayList<>();
            }
            return new ArrayList<>(this.selectedValues);
        }
    }
}
