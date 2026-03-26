package com.DMHelper.basic.feat;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.playerclass.Character_Class;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Feat_Library {
    private static final Map<String, Feat_Definition> FEATS = new LinkedHashMap<>();

    static {
        add("Actor", "表演者 (Actor)", "魅力相关伪装与模仿能力更强，表演或欺瞒伪装成他人时更有优势。", "");
        add("Alert", "警觉 (Alert)", "先攻 +5，不会因未察觉敌人而措手不及，隐藏敌人也难以偷袭你。", "");
        add("Athlete", "运动健将 (Athlete)", "增强攀爬、起身与跳跃机动性，并提升力量或敏捷。", "");
        add("Charger", "冲锋者 (Charger)", "冲刺后可用附赠动作突击，获得额外伤害或推离效果。", "");
        add("Crossbow Expert", "弩手专家 (Crossbow Expert)", "强化弩类使用，忽略装填限制并允许近距离稳定射击。", "");
        add("Defensive Duelist", "防御决斗家 (Defensive Duelist)", "持灵巧武器时可用反应提高 AC，偏向单挑防守。", "敏捷 13+");
        add("Dual Wielder", "双武器大师 (Dual Wielder)", "更擅长双持战斗，可双持非轻型武器并获得 AC 加值。", "");
        add("Dungeon Delver", "地下城探索者 (Dungeon Delver)", "更擅长发现陷阱、承受陷阱伤害并调查隐藏机关。", "");
        add("Durable", "耐久体魄 (Durable)", "提升体质，并让你花生命骰恢复生命时更稳定。", "");
        add("Elemental Adept", "元素专精 (Elemental Adept)", "选择一种元素伤害，让相关法术更稳定并部分无视抗性。", "");
        add("Grappler", "擒抱专家 (Grappler)", "更专精于近身擒抱与压制。", "力量 13+");
        add("Great Weapon Master", "巨武器大师 (Great Weapon Master)", "重击或击倒后可追加攻击，也可用 -5 命中换 +10 伤害。", "");
        add("Healer", "治疗者 (Healer)", "能高效使用治疗工具包，在战斗与休整中提供额外治疗。", "");
        add("Heavily Armored", "重甲训练 (Heavily Armored)", "提升力量，并获得重甲熟练。", "具备中甲熟练");
        add("Heavy Armor Master", "重甲大师 (Heavy Armor Master)", "提升力量，穿重甲时可削减部分非魔法钝击/穿刺/挥砍伤害。", "具备重甲熟练");
        add("Keen Mind", "敏锐心智 (Keen Mind)", "提升智力，强化方向感、时间感与记忆能力。", "");
        add("Lightly Armored", "轻甲训练 (Lightly Armored)", "提升力量或敏捷，并获得轻甲熟练。", "");
        add("Linguist", "语言学家 (Linguist)", "提升智力，学会额外语言并可设计密码。", "");
        add("Lucky", "幸运 (Lucky)", "每天获得 3 点幸运点，可重掷攻击、检定或豁免。", "");
        add("Mage Slayer", "法师杀手 (Mage Slayer)", "压制近身施法者，对其施法有更强反制与追击能力。", "");
        add("Magic Initiate", "魔法学徒 (Magic Initiate)", "从一个职业法表学会 2 个戏法和 1 个 1 环法术。", "");
        add("Martial Adept", "武艺老手 (Martial Adept)", "学习 2 个战技并获得 1 颗卓越骰。", "");
        add("Medium Armor Master", "中甲大师 (Medium Armor Master)", "穿中甲时更好发挥敏捷，并减轻潜行负担。", "具备中甲熟练");
        add("Mobile", "机动专家 (Mobile)", "速度更快，冲刺更灵活，近战后更容易脱离。", "");
        add("Moderately Armored", "中甲训练 (Moderately Armored)", "提升力量或敏捷，并获得中甲与盾牌熟练。", "具备轻甲熟练");
        add("Mounted Combatant", "骑乘战斗者 (Mounted Combatant)", "更擅长骑乘作战，保护坐骑并对较小目标更有优势。", "");
        add("Observant", "观察入微 (Observant)", "提升智力或感知，并显著提高被动察觉与读唇能力。", "");
        add("Polearm Master", "长柄武器大师 (Polearm Master)", "长柄武器获得附赠动作打击与进场借机攻击。", "");
        add("Resilient", "坚韧不拔 (Resilient)", "提升一项属性，并获得对应豁免熟练。", "");
        add("Ritual Caster", "仪式施法者 (Ritual Caster)", "获得一本仪式书，可学习并施放仪式法术。", "智力/感知/魅力 13+");
        add("Savage Attacker", "野蛮攻击者 (Savage Attacker)", "近战武器伤害每回合可重掷一次并取较好结果。", "");
        add("Sentinel", "哨兵 (Sentinel)", "强化借机攻击与控场，能有效锁住敌人。", "");
        add("Sharpshooter", "神射手 (Sharpshooter)", "远程攻击可无视远距离劣势与部分掩体，也可 -5 命中换 +10 伤害。", "");
        add("Shield Master", "盾牌大师 (Shield Master)", "强化持盾作战，可用盾牌推人并增强敏捷豁免防护。", "");
        add("Skilled", "多才多艺 (Skilled)", "获得任意 3 项技能或工具熟练。", "");
        add("Skulker", "潜伏者 (Skulker)", "更擅长昏暗环境潜行，远程潜袭后更不易暴露。", "");
        add("Spell Sniper", "法术狙击手 (Spell Sniper)", "攻击型法术射程翻倍并更容易穿透掩体，还可额外学 1 个攻击戏法。", "能施放至少一个法术");
        add("Tavern Brawler", "酒馆斗士 (Tavern Brawler)", "强化徒手与 improvised weapon 战斗，并可顺势擒抱。", "");
        add("Tough", "强韧 (Tough)", "每级额外获得 2 点最大生命值。", "");
        add("War Caster", "战斗施法者 (War Caster)", "更擅长维持专注，并可在借机攻击时施放法术。", "能施放至少一个法术");
        add("Weapon Master", "武器大师 (Weapon Master)", "提升力量或敏捷，并获得 4 项武器熟练。", "");
    }

    private static void add(String key, String label, String description, String prerequisite) {
        FEATS.put(key, new Feat_Definition(key, label, description, prerequisite));
    }

    public static Feat_Definition get_feat(String key) {
        return FEATS.get(key);
    }

    public static List<String> get_all_feat_keys() {
        return new ArrayList<>(FEATS.keySet());
    }

    public static String get_label(String key) {
        Feat_Definition feat = FEATS.get(key);
        return feat == null ? key : feat.label;
    }

    public static String get_summary_line(String key) {
        Feat_Definition feat = FEATS.get(key);
        return feat == null ? key : feat.to_summary_line();
    }

    public static String get_prompt_line(String key) {
        Feat_Definition feat = FEATS.get(key);
        return feat == null ? key : feat.to_prompt_line();
    }

    public static List<String> get_available_feat_keys(Character_Sheet character) {
        List<String> keys = new ArrayList<>();
        for (String key : FEATS.keySet()) {
            if (!character.job.feat_names.contains(key) && meets_prerequisite(character, key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    public static boolean meets_prerequisite(Character_Sheet character, String key) {
        switch (key) {
            case "Defensive Duelist":
                return character.stats.dex >= 13;
            case "Grappler":
                return character.stats.str >= 13;
            case "Heavily Armored":
                return has_armor_proficiency(character.job, "Medium");
            case "Heavy Armor Master":
                return has_armor_proficiency(character.job, "Heavy");
            case "Medium Armor Master":
                return has_armor_proficiency(character.job, "Medium");
            case "Moderately Armored":
                return has_armor_proficiency(character.job, "Light");
            case "Ritual Caster":
                return character.stats.intel >= 13 || character.stats.wis >= 13 || character.stats.cha >= 13;
            case "Spell Sniper":
            case "War Caster":
                return can_cast_spell(character);
            default:
                return true;
        }
    }

    private static boolean has_armor_proficiency(Character_Class job, String armorType) {
        for (String proficiency : job.equipment_proficiencies) {
            if (proficiency.contains("所有护甲")) {
                return true;
            }
            if ("Light".equals(armorType) && proficiency.contains("轻甲")) {
                return true;
            }
            if ("Medium".equals(armorType) && proficiency.contains("中甲")) {
                return true;
            }
            if ("Heavy".equals(armorType) && proficiency.contains("重甲")) {
                return true;
            }
        }
        return false;
    }

    private static boolean can_cast_spell(Character_Sheet character) {
        if ("WIZARD".equals(character.job.class_key) || "SORCERER".equals(character.job.class_key)) {
            return true;
        }
        return "PALADIN".equals(character.job.class_key) && character.job.current_level >= 2;
    }
}
