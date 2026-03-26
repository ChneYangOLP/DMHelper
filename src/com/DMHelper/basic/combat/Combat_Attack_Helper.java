package com.DMHelper.basic.combat;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;

import java.util.ArrayList;
import java.util.List;

public class Combat_Attack_Helper {
    private Combat_Attack_Helper() {
    }

    public static Combatant build_player_combatant(Character_Sheet character) {
        int dexMod = character.stats.get_mod(character.stats.dex);
        return new Combatant(
                character.name + " - " + character.job.class_name,
                Combatant.Side.PLAYER,
                character.ac,
                character.hp,
                dexMod,
                character.stats.str,
                character.stats.dex,
                character.stats.con,
                character.stats.intel,
                character.stats.wis,
                character.stats.cha,
                character.get_proficiency_bonus(),
                build_attack_options(character),
                character,
                null,
                0
        );
    }

    public static Combatant build_enemy_combatant(Monster_Definition monster, int serialNumber) {
        int hp = Math.max(1, monster.roll_hit_points());
        return new Combatant(
                monster.get_full_name() + " #" + serialNumber,
                Combatant.Side.ENEMY,
                monster.armor_class,
                hp,
                Combatant.get_modifier(monster.dex),
                monster.str,
                monster.dex,
                monster.con,
                monster.intel,
                monster.wis,
                monster.cha,
                2,
                monster.attack_options,
                null,
                monster,
                monster.xp_reward
        );
    }

    private static List<Attack_Option> build_attack_options(Character_Sheet character) {
        List<Attack_Option> options = new ArrayList<>();
        int pb = character.get_proficiency_bonus();
        int strMod = character.stats.get_mod(character.stats.str);
        int dexMod = character.stats.get_mod(character.stats.dex);
        int intMod = character.stats.get_mod(character.stats.intel);
        int chaMod = character.stats.get_mod(character.stats.cha);

        options.add(Attack_Option.attack_roll("徒手打击 (Unarmed Strike)", "基础近战攻击。", pb + strMod, 1, 1, strMod, 1, "钝击"));

        if (character.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) character.job;
            int meleeDamageBonus = strMod + ("Dueling".equals(fighter.fighting_style_name) ? 2 : 0);
            options.add(build_weapon_attack(character, pb, strMod, meleeDamageBonus, fighter.attacks_per_action, "战士近战主攻击。"));
            int rangedBonus = pb + dexMod + ("Archery".equals(fighter.fighting_style_name) ? 2 : 0);
            if ("Archery".equals(fighter.fighting_style_name)) {
                options.add(Attack_Option.attack_roll("长弓射击 (Longbow)", "战士远程攻击。", rangedBonus, 1, 8, dexMod, fighter.attacks_per_action, "穿刺"));
            } else {
                options.add(Attack_Option.attack_roll("标枪投掷 (Javelin)", "战士远程攻击。", pb + strMod, 1, 6, strMod, fighter.attacks_per_action, "穿刺"));
            }
            if (fighter.superiority_dice > 0) {
                options.add(Attack_Option.attack_roll("绊摔攻击 (Trip Attack)", "战技攻击，命中后尝试使目标倒地。", pb + strMod, 1, 8, meleeDamageBonus + fighter.superiority_dice_type / 4, 1, "挥砍")
                        .with_superiority_die_cost(1)
                        .with_status(Combat_Status_Type.PRONE, 1, "Strength", 8 + pb + strMod));
                options.add(Attack_Option.attack_roll("威吓攻击 (Menacing Attack)", "战技攻击，命中后尝试令目标恐慌。", pb + strMod, 1, 8, meleeDamageBonus + fighter.superiority_dice_type / 4, 1, "挥砍")
                        .with_superiority_die_cost(1)
                        .with_status(Combat_Status_Type.FRIGHTENED, 2, "Wisdom", 8 + pb + strMod));
            }
        } else if (character.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) character.job;
            int meleeDamageBonus = strMod + ("Dueling".equals(paladin.fighting_style_name) ? 2 : 0);
            Attack_Option meleeAttack = build_weapon_attack(character, pb, strMod, meleeDamageBonus, paladin.attacks_per_action, "圣武士近战主攻击。");
            options.add(meleeAttack);
            options.add(Attack_Option.attack_roll("神圣惩击 - " + meleeAttack.name, "附带神圣惩击的近战攻击。", meleeAttack.attack_bonus, 3, 8, meleeDamageBonus, 1, "光耀")
                    .with_spell_slot_cost(1));
            options.add(Attack_Option.attack_roll("标枪投掷 (Javelin)", "圣武士远程攻击。", pb + strMod, 1, 6, strMod, 1, "穿刺"));
        } else if (character.job instanceof Wizard_Class) {
            options.add(build_weapon_attack(character, pb, strMod, strMod, 1, "基础近战攻击。"));
            options.addAll(build_wizard_attacks(character, intMod, pb));
        } else if (character.job instanceof Sorcerer_Class) {
            options.add(build_weapon_attack(character, pb, strMod, dexMod, 1, "基础近战攻击。"));
            options.addAll(build_sorcerer_attacks(character, chaMod, pb));
        } else if (character.job instanceof Warlock_Class) {
            options.add(build_weapon_attack(character, pb, strMod, dexMod, 1, "基础近战攻击。"));
            options.addAll(build_warlock_attacks(character, chaMod, pb));
        } else {
            options.add(Attack_Option.attack_roll("简易武器攻击", "通用攻击。", pb + strMod, 1, 6, strMod, 1, "挥砍"));
        }

        return options;
    }

    private static Attack_Option build_weapon_attack(Character_Sheet character,
                                                     int pb,
                                                     int defaultStrMod,
                                                     int finesseOrDamageMod,
                                                     int attacksPerAction,
                                                     String description) {
        Equipment_Item weapon = character.get_equipped_item(Equipment_Slot.MAIN_HAND);
        if (weapon == null || weapon.attack_die_size <= 0) {
            return Attack_Option.attack_roll("徒手打击 (Unarmed Strike)", description, pb + defaultStrMod, 1, 1, defaultStrMod, attacksPerAction, "钝击");
        }
        int dexMod = character.stats.get_mod(character.stats.dex);
        int strMod = character.stats.get_mod(character.stats.str);
        int attackMod = weapon.finesse ? Math.max(strMod, dexMod) : (weapon.ranged ? dexMod : strMod);
        int damageBonus = weapon.finesse ? Math.max(finesseOrDamageMod, strMod) : (weapon.ranged ? dexMod : finesseOrDamageMod);
        return Attack_Option.attack_roll(
                weapon.display_name,
                description,
                pb + attackMod + weapon.attack_bonus,
                weapon.attack_dice_count,
                weapon.attack_die_size,
                damageBonus,
                attacksPerAction,
                weapon.damage_type == null || weapon.damage_type.isEmpty() ? "武器" : weapon.damage_type
        );
    }

    private static List<Attack_Option> build_wizard_attacks(Character_Sheet character, int castingMod, int pb) {
        List<Attack_Option> options = new ArrayList<>();
        Wizard_Class wizardClass = (Wizard_Class) character.job;
        int spellAttack = pb + castingMod;
        add_known_cantrip_attacks(options, wizardClass.known_cantrip_keys, spellAttack, character.job.current_level, 0, false);
        options.addAll(build_arcane_spell_attacks(character, castingMod, pb, true));
        return options;
    }

    private static List<Attack_Option> build_arcane_spell_attacks(Character_Sheet character, int castingMod, int pb, boolean wizard) {
        List<Attack_Option> options = new ArrayList<>();
        int spellAttack = pb + castingMod;
        int spellDc = 8 + pb + castingMod;

        if (wizard) {
            Wizard_Class wizardClass = (Wizard_Class) character.job;
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "magic_missile", Attack_Option.auto_hit("魔法飞弹 (Magic Missile)", "自动命中的力场飞弹。", 3, 4, 3, "力场").with_spell_slot_cost(1));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "chromatic_orb", Attack_Option.attack_roll("七彩法球 (Chromatic Orb)", "远程法术攻击。", spellAttack, 3, 8, 0, 1, "元素").with_spell_slot_cost(1));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "scorching_ray", Attack_Option.attack_roll("灼热射线 (Scorching Ray)", "多重远程法术攻击。", spellAttack, 2, 6, 0, 3, "火焰").with_spell_slot_cost(2));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "hold_person", Attack_Option.save_dc("人类定身术 (Hold Person)", "强迫 humanoid 进行豁免。", spellDc, "Wisdom", 0, 1, 0, false, "控制").with_spell_slot_cost(2).with_status(Combat_Status_Type.PARALYZED, 2, "", 0));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "fireball", Attack_Option.save_dc("火球术 (Fireball)", "范围塑能法术。", spellDc, "Dexterity", 8, 6, 0, true, "火焰").with_spell_slot_cost(3));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "lightning_bolt", Attack_Option.save_dc("闪电束 (Lightning Bolt)", "直线塑能法术。", spellDc, "Dexterity", 8, 6, 0, true, "闪电").with_spell_slot_cost(3));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "fear", Attack_Option.save_dc("恐惧术 (Fear)", "制造恐慌。", spellDc, "Wisdom", 0, 1, 0, false, "精神").with_spell_slot_cost(3).with_status(Combat_Status_Type.FRIGHTENED, 2, "", 0));
            add_spell_if_present(options, wizardClass.prepared_spell_keys, "cone_of_cold", Attack_Option.save_dc("寒冰锥 (Cone of Cold)", "大范围寒冷法术。", spellDc, "Constitution", 8, 8, 0, true, "寒冷").with_spell_slot_cost(5));
        } else {
            options.add(Attack_Option.attack_roll("魔能爆 (Eldritch Blast)", "远程法术攻击。", spellAttack, get_cantrip_dice_count(character.job.current_level), 10, 0, get_eldritch_blast_beams(character.job.current_level), "力场"));
        }

        return options;
    }

    private static List<Attack_Option> build_sorcerer_attacks(Character_Sheet character, int castingMod, int pb) {
        List<Attack_Option> options = new ArrayList<>();
        Sorcerer_Class sorcererClass = (Sorcerer_Class) character.job;
        int spellAttack = pb + castingMod;
        int spellDc = 8 + pb + castingMod;

        add_known_cantrip_attacks(options, sorcererClass.known_cantrip_keys, spellAttack, character.job.current_level, castingMod, false);
        add_spell_if_present(options, sorcererClass.known_spell_keys, "magic_missile", Attack_Option.auto_hit("魔法飞弹 (Magic Missile)", "自动命中的力场飞弹。", 3, 4, 3, "力场").with_spell_slot_cost(1));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "chromatic_orb", Attack_Option.attack_roll("七彩法球 (Chromatic Orb)", "远程法术攻击。", spellAttack, 3, 8, 0, 1, "元素").with_spell_slot_cost(1));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "chaos_bolt", Attack_Option.attack_roll("混沌箭 (Chaos Bolt)", "混沌能量远程攻击。", spellAttack, 2, 8, 1, 1, "混沌").with_spell_slot_cost(1));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "scorching_ray", Attack_Option.attack_roll("灼热射线 (Scorching Ray)", "多重远程法术攻击。", spellAttack, 2, 6, 0, 3, "火焰").with_spell_slot_cost(2));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "hold_person", Attack_Option.save_dc("人类定身术 (Hold Person)", "迫使目标麻痹。", spellDc, "Wisdom", 0, 1, 0, false, "控制").with_spell_slot_cost(2).with_status(Combat_Status_Type.PARALYZED, 2, "", 0));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "fireball", Attack_Option.save_dc("火球术 (Fireball)", "范围塑能法术。", spellDc, "Dexterity", 8, 6, 0, true, "火焰").with_spell_slot_cost(3));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "fear", Attack_Option.save_dc("恐惧术 (Fear)", "制造恐慌。", spellDc, "Wisdom", 0, 1, 0, false, "精神").with_spell_slot_cost(3).with_status(Combat_Status_Type.FRIGHTENED, 2, "", 0));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "lightning_bolt", Attack_Option.save_dc("闪电束 (Lightning Bolt)", "直线塑能法术。", spellDc, "Dexterity", 8, 6, 0, true, "闪电").with_spell_slot_cost(3));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "blight", Attack_Option.save_dc("枯萎术 (Blight)", "死灵能量重创单体。", spellDc, "Constitution", 8, 8, 0, false, "死灵").with_spell_slot_cost(4));
        add_spell_if_present(options, sorcererClass.known_spell_keys, "cone_of_cold", Attack_Option.save_dc("寒冰锥 (Cone of Cold)", "大范围寒冷法术。", spellDc, "Constitution", 8, 8, 0, true, "寒冷").with_spell_slot_cost(5));
        return options;
    }

    private static List<Attack_Option> build_warlock_attacks(Character_Sheet character, int castingMod, int pb) {
        List<Attack_Option> options = new ArrayList<>();
        Warlock_Class warlockClass = (Warlock_Class) character.job;
        int spellAttack = pb + castingMod;
        int spellDc = 8 + pb + castingMod;
        int eldritchBonus = warlockClass.invocation_keys.contains("Agonizing Blast") ? castingMod : 0;

        add_known_cantrip_attacks(options, warlockClass.known_cantrip_keys, spellAttack, character.job.current_level, eldritchBonus, true);
        add_spell_if_present(options, warlockClass.known_spell_keys, "witch_bolt", Attack_Option.attack_roll("巫术箭 (Witch Bolt)", "持续闪电攻击。", spellAttack, 1, 12, 0, 1, "闪电").with_pact_slot_cost());
        add_spell_if_present(options, warlockClass.known_spell_keys, "hellish_rebuke", Attack_Option.save_dc("地狱斥责 (Hellish Rebuke)", "地狱火焰反击。", spellDc, "Dexterity", 2, 10, 0, true, "火焰").with_pact_slot_cost().with_status(Combat_Status_Type.BURNING, 2, "", 0));
        add_spell_if_present(options, warlockClass.known_spell_keys, "arms_of_hadar", Attack_Option.save_dc("哈达之臂 (Arms of Hadar)", "黑暗触手爆发。", spellDc, "Strength", 2, 6, 0, false, "死灵").with_pact_slot_cost().with_status(Combat_Status_Type.RESTRAINED, 1, "", 0));
        add_spell_if_present(options, warlockClass.known_spell_keys, "hex", Attack_Option.auto_hit("灾祸术 (Hex)", "诅咒目标，压低其状态。", 0, 1, 0, "诅咒").with_pact_slot_cost().with_status(Combat_Status_Type.CURSED, 3, "", 0));
        add_spell_if_present(options, warlockClass.known_spell_keys, "hold_person", Attack_Option.save_dc("人类定身术 (Hold Person)", "迫使目标麻痹。", spellDc, "Wisdom", 0, 1, 0, false, "控制").with_pact_slot_cost().with_status(Combat_Status_Type.PARALYZED, 2, "", 0));
        add_spell_if_present(options, warlockClass.known_spell_keys, "fear", Attack_Option.save_dc("恐惧术 (Fear)", "制造恐慌。", spellDc, "Wisdom", 0, 1, 0, false, "精神").with_pact_slot_cost().with_status(Combat_Status_Type.FRIGHTENED, 2, "", 0));
        add_spell_if_present(options, warlockClass.known_spell_keys, "blight", Attack_Option.save_dc("枯萎术 (Blight)", "死灵能量重创单体。", spellDc, "Constitution", 8, 8, 0, false, "死灵").with_pact_slot_cost());
        return options;
    }

    private static void add_known_cantrip_attacks(List<Attack_Option> options,
                                                  List<String> cantripKeys,
                                                  int spellAttack,
                                                  int level,
                                                  int eldritchBonus,
                                                  boolean warlock) {
        int cantripDice = get_cantrip_dice_count(level);
        if (cantripKeys.contains("fire_bolt")) {
            options.add(Attack_Option.attack_roll("火焰箭 (Fire Bolt)", "远程法术攻击。", spellAttack, cantripDice, 10, 0, 1, "火焰"));
        }
        if (cantripKeys.contains("ray_of_frost")) {
            options.add(Attack_Option.attack_roll("寒霜射线 (Ray of Frost)", "远程法术攻击。", spellAttack, cantripDice, 8, 0, 1, "寒冷"));
        }
        if (cantripKeys.contains("shocking_grasp")) {
            options.add(Attack_Option.attack_roll("电爪 (Shocking Grasp)", "近战法术攻击。", spellAttack, cantripDice, 8, 0, 1, "闪电"));
        }
        if (cantripKeys.contains("acid_splash")) {
            options.add(Attack_Option.save_dc("酸液飞溅 (Acid Splash)", "敏捷豁免伤害。", 8 + spellAttack, "Dexterity", cantripDice, 6, 0, false, "强酸"));
        }
        if (cantripKeys.contains("chill_touch")) {
            options.add(Attack_Option.attack_roll("寒触术 (Chill Touch)", "远程死灵法术攻击。", spellAttack, cantripDice, 8, 0, 1, "死灵"));
        }
        if (cantripKeys.contains("eldritch_blast")) {
            options.add(Attack_Option.attack_roll("魔能爆 (Eldritch Blast)", "远程法术攻击。", spellAttack, cantripDice, 10, eldritchBonus, get_eldritch_blast_beams(level), "力场"));
        }
        if (warlock && cantripKeys.contains("true_strike")) {
            options.add(Attack_Option.auto_hit("识破先机 (True Strike)", "短暂洞悉目标破绽，让其陷入诅咒。", 0, 1, 0, "预言").with_status(Combat_Status_Type.CURSED, 1, "", 0));
        }
    }

    private static void add_spell_if_present(List<Attack_Option> options, List<String> spellKeys, String spellKey, Attack_Option option) {
        if (spellKeys != null && spellKeys.contains(spellKey)) {
            options.add(option);
        }
    }

    private static int get_cantrip_dice_count(int level) {
        if (level >= 17) return 4;
        if (level >= 11) return 3;
        if (level >= 5) return 2;
        return 1;
    }

    private static int get_eldritch_blast_beams(int level) {
        if (level >= 17) return 4;
        if (level >= 11) return 3;
        if (level >= 5) return 2;
        return 1;
    }
}
