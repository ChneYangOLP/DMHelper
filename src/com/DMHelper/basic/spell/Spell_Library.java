package com.DMHelper.basic.spell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Spell_Library {

    private static final Map<String, Spell_Definition> SPELLS = new LinkedHashMap<>();
    private static final List<String> WIZARD_SPELL_KEYS = new ArrayList<>();
    private static final List<String> SORCERER_SPELL_KEYS = new ArrayList<>();
    private static final List<String> WARLOCK_SPELL_KEYS = new ArrayList<>();
    private static final List<String> PALADIN_SPELL_KEYS = new ArrayList<>();

    static {
        addWizard("fire_bolt", "火焰箭 (Fire Bolt)", 0, "塑能", "发射火焰远程攻击，命中造成火焰伤害。");
        addWizard("ray_of_frost", "寒霜射线 (Ray of Frost)", 0, "塑能", "命中造成寒冷伤害并降低目标速度。");
        addWizard("light", "光亮术 (Light)", 0, "塑能", "让物体发出稳定光源。");
        addWizard("mage_hand", "法师之手 (Mage Hand)", 0, "咒法", "创造远距离操控的小型幽灵之手。");
        addWizard("minor_illusion", "次级幻影 (Minor Illusion)", 0, "幻术", "制造一个小型影像或声音幻觉。");
        addWizard("prestidigitation", "魔法伎俩 (Prestidigitation)", 0, "变化", "进行各种细小而实用的戏法效果。");
        addWizard("acid_splash", "酸液飞溅 (Acid Splash)", 0, "塑能", "对相邻目标泼溅酸液。");
        addWizard("shocking_grasp", "电爪 (Shocking Grasp)", 0, "塑能", "近战电击并让目标失去反应。");
        addWizard("chill_touch", "寒触术 (Chill Touch)", 0, "死灵", "远程死灵攻击，阻止目标恢复生命。");
        addWizard("friends", "交友术 (Friends)", 0, "惑控", "短暂提高与人交流时的魅力优势。");
        addWizard("eldritch_blast", "魔能爆 (Eldritch Blast)", 0, "塑能", "发射奥术能量束进行远程法术攻击。");

        addWizard("magic_missile", "魔法飞弹 (Magic Missile)", 1, "塑能", "自动命中的力场飞弹。");
        addWizard("shield", "护盾术 (Shield)", 1, "防护", "用反应令 AC 短暂提高并挡下魔法飞弹。");
        addWizard("mage_armor", "法师护甲 (Mage Armor)", 1, "防护", "让无甲目标获得更好的基础 AC。");
        addWizard("detect_magic", "侦测魔法 (Detect Magic)", 1, "预言", "感知附近的魔法灵光。");
        addWizard("identify", "鉴定术 (Identify)", 1, "预言", "解析物品或法术效果的魔法属性。");
        addWizard("sleep", "睡眠术 (Sleep)", 1, "惑控", "使一批低生命值生物陷入睡眠。");
        addWizard("burning_hands", "燃烧之手 (Burning Hands)", 1, "塑能", "锥形喷吐火焰。");
        addWizard("find_familiar", "寻获魔宠 (Find Familiar)", 1, "咒法", "召来一个可长期陪伴的魔法使魔。");
        addWizard("chromatic_orb", "七彩法球 (Chromatic Orb)", 1, "塑能", "投射可切换伤害类型的法球。");
        addWizard("thunderwave", "雷鸣波 (Thunderwave)", 1, "塑能", "近距离爆发雷鸣并击退敌人。");
        addWizard("chaos_bolt", "混沌箭 (Chaos Bolt)", 1, "塑能", "混乱能量射线随机改变伤害类型。");
        addWizard("charm_person", "魅惑人类 (Charm Person)", 1, "惑控", "短时间魅惑一个类人生物。");
        addWizard("hex", "灾祸术 (Hex)", 1, "惑控", "诅咒目标，持续追加死灵伤害并妨碍检定。");
        addWizard("armor_of_agathys", "阿迦西斯之甲 (Armor of Agathys)", 1, "防护", "提供临时生命值，并反伤近战攻击者。");
        addWizard("hellish_rebuke", "地狱斥责 (Hellish Rebuke)", 1, "塑能", "用反应以地狱火焰回击伤害你的人。");
        addWizard("arms_of_hadar", "哈达之臂 (Arms of Hadar)", 1, "咒法", "黑暗触手在身边爆发，伤害并阻止反应。");
        addWizard("witch_bolt", "巫术箭 (Witch Bolt)", 1, "塑能", "闪电束持续连接并电击目标。");

        addWizard("mirror_image", "镜影术 (Mirror Image)", 2, "幻术", "制造分身来干扰敌人的攻击。");
        addWizard("misty_step", "迷踪步 (Misty Step)", 2, "咒法", "迅速瞬移到短距离内可见位置。");
        addWizard("scorching_ray", "灼热射线 (Scorching Ray)", 2, "塑能", "发射多束火焰射线。");
        addWizard("invisibility", "隐形术 (Invisibility)", 2, "幻术", "让目标暂时隐形。");
        addWizard("web", "蛛网术 (Web)", 2, "咒法", "制造大片黏网束缚敌人。");
        addWizard("suggestion", "暗示术 (Suggestion)", 2, "惑控", "向目标植入看似合理的建议。");
        addWizard("hold_person", "人类定身术 (Hold Person)", 2, "惑控", "麻痹一个类人生物。");
        addWizard("levitate", "浮空术 (Levitate)", 2, "变化", "让目标升空悬浮。");
        addWizard("darkness", "黑暗术 (Darkness)", 2, "塑能", "制造魔法黑暗区域。");
        addWizard("blur", "朦胧术 (Blur)", 2, "幻术", "让施法者轮廓模糊，敌人更难命中。");

        addWizard("fireball", "火球术 (Fireball)", 3, "塑能", "经典大范围爆炸火焰法术。");
        addWizard("counterspell", "反制法术 (Counterspell)", 3, "防护", "用反应阻断他人的施法。");
        addWizard("dispel_magic", "解除魔法 (Dispel Magic)", 3, "防护", "压制或终止现有魔法效果。");
        addWizard("fly", "飞行术 (Fly)", 3, "变化", "让目标获得飞行速度。");
        addWizard("lightning_bolt", "闪电束 (Lightning Bolt)", 3, "塑能", "直线闪电重创路径上的敌人。");
        addWizard("hypnotic_pattern", "催眠图纹 (Hypnotic Pattern)", 3, "幻术", "大范围迷惑并瘫住敌人。");
        addWizard("tiny_hut", "李欧蒙小屋 (Tiny Hut)", 3, "塑能", "制造安全庇护结界。");
        addWizard("fear", "恐惧术 (Fear)", 3, "幻术", "在锥形区域制造压倒性恐惧。");
        addWizard("haste", "加速术 (Haste)", 3, "变化", "大幅强化目标的速度、AC 与动作效率。");
        addWizard("slow", "缓慢术 (Slow)", 3, "变化", "迟滞多个目标的动作与反应。");
        addWizard("vampiric_touch", "吸血之触 (Vampiric Touch)", 3, "死灵", "以近战法术攻击吸取生命。");

        addWizard("greater_invisibility", "高等隐形术 (Greater Invisibility)", 4, "幻术", "战斗中保持隐形。");
        addWizard("dimension_door", "任意门 (Dimension Door)", 4, "咒法", "带着同伴进行中距离瞬移。");
        addWizard("ice_storm", "冰风暴 (Ice Storm)", 4, "塑能", "范围冰雹与碎石打击。");
        addWizard("polymorph", "变形术 (Polymorph)", 4, "变化", "把目标变成新的生物形态。");
        addWizard("stoneskin", "石肤术 (Stoneskin)", 4, "防护", "让目标获得对非魔法武器的抗性。");
        addWizard("blight", "枯萎术 (Blight)", 4, "死灵", "以枯萎死灵能量重创单体。");
        addWizard("shadow_of_moil", "幽影缠身 (Shadow of Moil)", 4, "死灵", "以阴影包裹自身并灼伤攻击者。");

        addWizard("cone_of_cold", "寒冰锥 (Cone of Cold)", 5, "塑能", "大范围寒冷冲击。");
        addWizard("telekinesis", "念动术 (Telekinesis)", 5, "变化", "以心灵操纵生物或物体。");
        addWizard("wall_of_force", "力场墙 (Wall of Force)", 5, "塑能", "制造极难突破的透明墙壁。");
        addWizard("scrying", "探知术 (Scrying)", 5, "预言", "远距离窥视目标。");
        addWizard("animate_objects", "活化物体 (Animate Objects)", 5, "变化", "让物品自己行动并攻击。");
        addWizard("hold_monster", "怪物定身术 (Hold Monster)", 5, "惑控", "麻痹任意类型的生物。");
        addWizard("synaptic_static", "灵能静滞 (Synaptic Static)", 5, "惑控", "精神爆炸伤害并削弱一群敌人。");

        addWizard("disintegrate", "解离术 (Disintegrate)", 6, "变化", "高额单体伤害，能把目标化为灰烬。");
        addWizard("globe_of_invulnerability", "法术无效结界 (Globe of Invulnerability)", 6, "防护", "球形结界内可压制低环法术。");
        addWizard("chain_lightning", "连锁闪电 (Chain Lightning)", 6, "塑能", "闪电在多个目标间跳跃。");
        addWizard("finger_of_death", "死亡一指 (Finger of Death)", 7, "死灵", "对单体造成极高死灵伤害。");
        addWizard("teleport", "传送术 (Teleport)", 7, "咒法", "远距离瞬间传送队伍。");
        addWizard("maze", "迷宫术 (Maze)", 8, "咒法", "将目标暂时困进异空间迷宫。");
        addWizard("power_word_stun", "强效定身言 (Power Word Stun)", 8, "惑控", "以言语令受伤目标立刻昏迷。");
        addWizard("meteor_swarm", "流星爆 (Meteor Swarm)", 9, "塑能", "终局级超大范围爆炸。");
        addWizard("power_word_kill", "强效死亡言 (Power Word Kill)", 9, "惑控", "直接击杀生命值不足的目标。");
        addWizard("wish", "祈愿术 (Wish)", 9, "咒法", "最强大的万能法术，可模拟多数低环法术。");

        addSorcerer("fire_bolt");
        addSorcerer("ray_of_frost");
        addSorcerer("light");
        addSorcerer("mage_hand");
        addSorcerer("minor_illusion");
        addSorcerer("prestidigitation");
        addSorcerer("shocking_grasp");
        addSorcerer("chill_touch");
        addSorcerer("friends");
        addSorcerer("magic_missile");
        addSorcerer("shield");
        addSorcerer("sleep");
        addSorcerer("burning_hands");
        addSorcerer("chromatic_orb");
        addSorcerer("chaos_bolt");
        addSorcerer("charm_person");
        addSorcerer("thunderwave");
        addSorcerer("mirror_image");
        addSorcerer("misty_step");
        addSorcerer("scorching_ray");
        addSorcerer("invisibility");
        addSorcerer("suggestion");
        addSorcerer("hold_person");
        addSorcerer("darkness");
        addSorcerer("blur");
        addSorcerer("fireball");
        addSorcerer("counterspell");
        addSorcerer("fear");
        addSorcerer("fly");
        addSorcerer("haste");
        addSorcerer("slow");
        addSorcerer("lightning_bolt");
        addSorcerer("hypnotic_pattern");
        addSorcerer("greater_invisibility");
        addSorcerer("dimension_door");
        addSorcerer("ice_storm");
        addSorcerer("polymorph");
        addSorcerer("blight");
        addSorcerer("cone_of_cold");
        addSorcerer("telekinesis");
        addSorcerer("chain_lightning");
        addSorcerer("disintegrate");
        addSorcerer("teleport");
        addSorcerer("power_word_stun");
        addSorcerer("meteor_swarm");
        addSorcerer("wish");

        addWarlock("eldritch_blast");
        addWarlock("chill_touch");
        addWarlock("friends");
        addWarlock("mage_hand");
        addWarlock("minor_illusion");
        addWarlock("prestidigitation");
        addWarlock("true_strike", "识破先机 (True Strike)", 0, "预言", "短暂洞悉目标防御，为下一击做准备。");
        addWarlock("armor_of_agathys");
        addWarlock("arms_of_hadar");
        addWarlock("charm_person");
        addWarlock("hex");
        addWarlock("hellish_rebuke");
        addWarlock("witch_bolt");
        addWarlock("darkness");
        addWarlock("hold_person");
        addWarlock("invisibility");
        addWarlock("misty_step");
        addWarlock("mirror_image");
        addWarlock("counterspell");
        addWarlock("fear");
        addWarlock("fly");
        addWarlock("hypnotic_pattern");
        addWarlock("vampiric_touch");
        addWarlock("banishment");
        addWarlock("blight");
        addWarlock("dimension_door");
        addWarlock("shadow_of_moil");
        addWarlock("hold_monster");
        addWarlock("synaptic_static");

        addPaladin("bless", "祝福术 (Bless)", 1, "增益", "让队友攻击与豁免获得额外 d4。");
        addPaladin("command", "命令术 (Command)", 1, "控制", "用神圣命令强迫目标执行短暂动作。");
        addPaladin("compelled_duel", "强迫决斗 (Compelled Duel)", 1, "控制", "迫使目标专注与你对决。");
        addPaladin("cure_wounds", "疗伤术 (Cure Wounds)", 1, "治疗", "接触治疗盟友。");
        addPaladin("divine_favor", "神恩术 (Divine Favor)", 1, "增益", "武器攻击额外附带光耀伤害。");
        addPaladin("heroism", "英雄气概 (Heroism)", 1, "增益", "让目标免疫恐慌并持续获得临时生命值。");
        addPaladin("shield_of_faith", "信仰护盾 (Shield of Faith)", 1, "防护", "让目标 AC 提高。");
        addPaladin("wrathful_smite", "愤怒斩击 (Wrathful Smite)", 1, "惩击", "下一次命中附加心灵伤害并可能恐吓敌人。");
        addPaladin("find_steed", "寻获坐骑 (Find Steed)", 2, "召唤", "召来忠诚的神圣坐骑。");
        addPaladin("lesser_restoration", "次级复原术 (Lesser Restoration)", 2, "治疗", "解除常见异常状态。");
        addPaladin("magic_weapon", "魔化武器 (Magic Weapon)", 2, "增益", "让武器临时变为魔法武器。");
        addPaladin("protection_from_poison", "防护毒素 (Protection from Poison)", 2, "防护", "中和毒素并提高抗毒能力。");
        addPaladin("branding_smite", "炽印斩击 (Branding Smite)", 2, "惩击", "命中后附加光耀伤害并暴露隐形敌人。");
        addPaladin("blinding_smite", "致盲斩击 (Blinding Smite)", 3, "惩击", "下一次命中造成高额光耀伤害并可能致盲。");
        addPaladin("crusaders_mantle", "十字军披风 (Crusader's Mantle)", 3, "增益", "附近盟友武器攻击附带额外光耀伤害。");
        addPaladin("daylight", "昼明术 (Daylight)", 3, "光耀", "制造强力日光区域。");
        addPaladin("dispel_magic", "解除魔法 (Dispel Magic)", 3, "防护", "终止现有魔法效果。");
        addPaladin("revivify", "回生术 (Revivify)", 3, "复苏", "让刚死去的目标复活。");
        addPaladin("banishment", "放逐术 (Banishment)", 4, "控制", "把生物暂时逐出当前位面。");
        addPaladin("death_ward", "防死结界 (Death Ward)", 4, "防护", "防止目标遭受致命一击。");
        addPaladin("staggering_smite", "震慑斩击 (Staggering Smite)", 4, "惩击", "重创敌人的心智与战斗能力。");
        addPaladin("circle_of_power", "威能法阵 (Circle of Power)", 5, "防护", "强化队伍对法术的抗性。");
        addPaladin("destructive_wave", "毁灭波 (Destructive Wave)", 5, "打击", "神圣冲击波席卷周围敌人。");
        addPaladin("holy_weapon", "圣武器 (Holy Weapon)", 5, "增益", "显著强化武器并附带光耀爆发。");
        addPaladin("raise_dead", "死者复活 (Raise Dead)", 5, "复苏", "将死者带回人间。");
    }

    private static void addWizard(String key, String name, int level, String school, String desc) {
        SPELLS.put(key, new Spell_Definition(key, name, level, school, desc));
        WIZARD_SPELL_KEYS.add(key);
    }

    private static void addSorcerer(String key) {
        SORCERER_SPELL_KEYS.add(key);
    }

    private static void addWarlock(String key) {
        WARLOCK_SPELL_KEYS.add(key);
    }

    private static void addWarlock(String key, String name, int level, String school, String desc) {
        SPELLS.put(key, new Spell_Definition(key, name, level, school, desc));
        WARLOCK_SPELL_KEYS.add(key);
    }

    private static void addPaladin(String key, String name, int level, String school, String desc) {
        SPELLS.put(key, new Spell_Definition(key, name, level, school, desc));
        PALADIN_SPELL_KEYS.add(key);
    }

    public static Spell_Definition get_spell(String key) {
        return SPELLS.get(key);
    }

    public static List<String> get_wizard_spell_keys_up_to_level(int max_spell_level) {
        return filter_by_level(WIZARD_SPELL_KEYS, 1, max_spell_level);
    }

    public static List<String> get_wizard_cantrip_keys() {
        return filter_by_level(WIZARD_SPELL_KEYS, 0, 0);
    }

    public static List<String> get_sorcerer_spell_keys_up_to_level(int max_spell_level) {
        return filter_by_level(SORCERER_SPELL_KEYS, 1, max_spell_level);
    }

    public static List<String> get_sorcerer_cantrip_keys() {
        return filter_by_level(SORCERER_SPELL_KEYS, 0, 0);
    }

    public static List<String> get_warlock_spell_keys_up_to_level(int max_spell_level) {
        return filter_by_level(WARLOCK_SPELL_KEYS, 1, max_spell_level);
    }

    public static List<String> get_warlock_cantrip_keys() {
        return filter_by_level(WARLOCK_SPELL_KEYS, 0, 0);
    }

    public static List<String> get_paladin_spell_keys_up_to_level(int max_spell_level) {
        return filter_by_level(PALADIN_SPELL_KEYS, 1, max_spell_level);
    }

    private static List<String> filter_by_level(List<String> keys, int min_spell_level, int max_spell_level) {
        List<String> filtered = new ArrayList<>();
        for (String key : keys) {
            Spell_Definition spell = SPELLS.get(key);
            if (spell != null && spell.level >= min_spell_level && spell.level <= max_spell_level) {
                filtered.add(key);
            }
        }
        return filtered;
    }
}
