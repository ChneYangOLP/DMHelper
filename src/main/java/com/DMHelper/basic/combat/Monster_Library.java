package com.DMHelper.basic.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Monster_Library {
    private static final List<Monster_Definition> MONSTERS = new ArrayList<>();

    static {
        MONSTERS.add(new Monster_Definition(
                "goblin",
                "地精",
                "Goblin",
                "1",
                50,
                15,
                2,
                6,
                0,
                8, 14, 10, 10, 8, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("弯刀 (Scimitar)", "近战攻击。", 4, 1, 6, 2, 1, "挥砍"),
                        Attack_Option.attack_roll("短弓 (Shortbow)", "远程攻击。", 4, 1, 6, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "skeleton",
                "不死生物",
                "骷髅",
                "Skeleton",
                "1",
                50,
                13,
                2,
                8,
                4,
                10, 14, 15, 6, 8, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("短剑 (Shortsword)", "近战攻击。", 4, 1, 6, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("短弓 (Shortbow)", "远程攻击。", 4, 1, 6, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "wolf",
                "野兽",
                "狼",
                "Wolf",
                "1",
                50,
                13,
                2,
                8,
                2,
                12, 15, 12, 3, 12, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 4, 2, 4, 2, 1, "穿刺")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "orc",
                "兽人",
                "Orc",
                "1-2",
                100,
                13,
                2,
                8,
                6,
                16, 12, 16, 7, 11, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("巨斧 (Greataxe)", "近战攻击。", 5, 1, 12, 3, 1, "挥砍"),
                        Attack_Option.attack_roll("标枪 (Javelin)", "投掷攻击。", 5, 1, 6, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "hobgoblin",
                "大地精",
                "Hobgoblin",
                "2",
                100,
                18,
                2,
                8,
                2,
                13, 12, 12, 10, 10, 9,
                Arrays.asList(
                        Attack_Option.attack_roll("长剑 (Longsword)", "近战攻击。", 3, 1, 8, 1, 1, "挥砍"),
                        Attack_Option.attack_roll("长弓 (Longbow)", "远程攻击。", 3, 1, 8, 1, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "bugbear",
                "熊地精",
                "Bugbear",
                "2",
                200,
                16,
                5,
                8,
                5,
                15, 14, 13, 8, 11, 9,
                Arrays.asList(
                        Attack_Option.attack_roll("晨星锤 (Morningstar)", "近战攻击。", 4, 2, 8, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("标枪 (Javelin)", "投掷攻击。", 4, 2, 6, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ghoul",
                "不死生物",
                "食尸鬼",
                "Ghoul",
                "2",
                200,
                12,
                5,
                8,
                0,
                13, 15, 10, 7, 10, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("爪击 (Claws)", "近战攻击。", 4, 2, 4, 2, 1, "挥砍")
                                .with_status(Combat_Status_Type.PARALYZED, 1, "Constitution", 10),
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 2, 2, 6, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "bandit_captain",
                "强盗队长",
                "Bandit Captain",
                "3",
                450,
                15,
                9,
                8,
                18,
                15, 16, 14, 14, 11, 14,
                Arrays.asList(
                        Attack_Option.attack_roll("弯刀连击 (Scimitar)", "多重近战攻击。", 5, 1, 6, 3, 3, "挥砍"),
                        Attack_Option.attack_roll("轻弩 (Light Crossbow)", "远程攻击。", 5, 1, 8, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ogre",
                "食人魔",
                "Ogre",
                "3-4",
                450,
                11,
                7,
                10,
                21,
                19, 8, 16, 5, 7, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("大木棒 (Greatclub)", "近战攻击。", 6, 2, 8, 4, 1, "钝击"),
                        Attack_Option.attack_roll("标枪 (Javelin)", "远程攻击。", 6, 2, 6, 4, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "owlbear",
                "枭熊",
                "Owlbear",
                "4-5",
                700,
                13,
                7,
                10,
                21,
                20, 12, 17, 3, 12, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("喙击 (Beak)", "近战攻击。", 7, 1, 10, 5, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 7, 2, 8, 5, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "young_green_dragon",
                "青年绿龙",
                "Young Green Dragon",
                "8-10",
                3900,
                18,
                15,
                10,
                45,
                19, 12, 17, 16, 13, 15,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "近战攻击。", 7, 2, 10, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "近战攻击。", 7, 2, 6, 4, 2, "挥砍"),
                        Attack_Option.save_dc("毒息 (Poison Breath)", "锥形吐息。", 14, "Constitution", 12, 6, 0, true, "毒素")
                                .with_status(Combat_Status_Type.POISONED, 3, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "cultist",
                "邪教徒",
                "Cultist",
                "1",
                25,
                12,
                2,
                8,
                0,
                11, 12, 10, 10, 11, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("弯刀 (Scimitar)", "近战攻击。", 3, 1, 6, 1, 1, "挥砍"),
                        Attack_Option.attack_roll("轻弩 (Light Crossbow)", "远程攻击。", 3, 1, 8, 1, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "zombie",
                "不死生物",
                "僵尸",
                "Zombie",
                "1-2",
                50,
                8,
                3,
                8,
                9,
                13, 6, 16, 3, 6, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("猛击 (Slam)", "近战攻击。", 3, 1, 6, 1, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_spider",
                "野兽",
                "巨型蜘蛛",
                "Giant Spider",
                "2-3",
                200,
                14,
                4,
                10,
                0,
                14, 16, 12, 2, 11, 4,
                Arrays.asList(
                        Attack_Option.attack_roll("毒牙 (Bite)", "近战攻击。", 5, 1, 8, 3, 1, "穿刺")
                                .with_status(Combat_Status_Type.POISONED, 3, "Constitution", 11),
                        Attack_Option.save_dc("蛛网喷射 (Web)", "远程蛛网束缚。", 12, "Dexterity", 0, 1, 0, false, "束缚")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "wight",
                "不死生物",
                "尸妖",
                "Wight",
                "4",
                700,
                14,
                6,
                8,
                18,
                15, 14, 16, 10, 13, 15,
                Arrays.asList(
                        Attack_Option.attack_roll("长剑 (Longsword)", "近战攻击。", 4, 1, 8, 2, 1, "挥砍"),
                        Attack_Option.attack_roll("长弓 (Longbow)", "远程攻击。", 4, 1, 8, 2, 2, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "werewolf",
                "狼人",
                "Werewolf",
                "4-5",
                700,
                11,
                9,
                8,
                18,
                15, 13, 14, 10, 11, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 4, 2, 4, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 4, 2, 4, 2, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "troll",
                "巨魔",
                "Troll",
                "5-6",
                1800,
                15,
                8,
                10,
                40,
                18, 13, 20, 7, 9, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 7, 1, 6, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "近战攻击。", 7, 2, 6, 4, 2, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "mummy",
                "不死生物",
                "木乃伊",
                "Mummy",
                "5-6",
                700,
                11,
                8,
                8,
                16,
                16, 8, 15, 6, 10, 12,
                Arrays.asList(
                        Attack_Option.attack_roll("腐朽之拳 (Rotting Fist)", "近战攻击。", 5, 2, 6, 3, 1, "钝击")
                                .with_status(Combat_Status_Type.FRIGHTENED, 2, "Wisdom", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "wyvern",
                "飞龙",
                "Wyvern",
                "7-8",
                2300,
                13,
                13,
                10,
                26,
                19, 10, 16, 5, 12, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 7, 2, 6, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("毒刺 (Stinger)", "近战攻击。", 7, 2, 6, 4, 1, "穿刺")
                                .with_status(Combat_Status_Type.POISONED, 3, "Constitution", 15)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "crawling_claw",
                "不死生物",
                "爬行断手",
                "Crawling Claw",
                "1",
                25,
                12,
                1,
                4,
                0,
                13, 14, 11, 5, 10, 4,
                Arrays.asList(
                        Attack_Option.attack_roll("抓挠 (Claw)", "小型近战攻击。", 3, 1, 4, 1, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "shadow",
                "不死生物",
                "暗影",
                "Shadow",
                "2",
                100,
                12,
                3,
                8,
                6,
                6, 14, 13, 6, 10, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("暗影触碰 (Strength Drain)", "阴影般的死灵近战攻击。", 4, 2, 6, 2, 1, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 2, "Strength", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "specter",
                "不死生物",
                "幽魂",
                "Specter",
                "2-3",
                200,
                12,
                5,
                8,
                0,
                1, 14, 11, 10, 10, 11,
                Arrays.asList(
                        Attack_Option.attack_roll("生命虹吸 (Life Drain)", "幽魂死灵近战攻击。", 4, 3, 6, 0, 1, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 2, "Constitution", 10)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "minotaur_skeleton",
                "不死生物",
                "牛头怪骷髅",
                "Minotaur Skeleton",
                "3-4",
                450,
                12,
                9,
                10,
                18,
                18, 11, 15, 6, 8, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("巨斧 (Greataxe)", "沉重近战攻击。", 6, 2, 12, 4, 1, "挥砍"),
                        Attack_Option.attack_roll("撞角冲锋 (Gore)", "冲锋近战攻击。", 6, 2, 8, 4, 1, "穿刺")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 14)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "flameskull",
                "不死生物",
                "焰颅",
                "Flameskull",
                "4-5",
                1100,
                13,
                6,
                4,
                6,
                1, 17, 14, 16, 10, 11,
                Arrays.asList(
                        Attack_Option.save_dc("火焰射线 (Fire Ray)", "炽热火焰射线。", 13, "Dexterity", 3, 6, 0, false, "火焰")
                                .with_status(Combat_Status_Type.BURNING, 2, "", 0),
                        Attack_Option.save_dc("火球术 (Fireball)", "焰颅释放火球。", 13, "Dexterity", 8, 6, 0, true, "火焰")
                                .with_status(Combat_Status_Type.BURNING, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ghost",
                "不死生物",
                "幽灵",
                "Ghost",
                "4-5",
                1100,
                11,
                10,
                8,
                10,
                7, 13, 10, 10, 12, 17,
                Arrays.asList(
                        Attack_Option.attack_roll("衰朽之触 (Withering Touch)", "冰冷的死灵触碰。", 5, 4, 6, 3, 1, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 2, "Constitution", 13),
                        Attack_Option.save_dc("恐怖显现 (Horrifying Visage)", "骇人的幽灵姿态。", 13, "Wisdom", 0, 1, 0, false, "恐惧")
                                .with_status(Combat_Status_Type.FRIGHTENED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "banshee",
                "不死生物",
                "女妖",
                "Banshee",
                "5-6",
                1100,
                12,
                13,
                8,
                13,
                1, 14, 10, 12, 11, 17,
                Arrays.asList(
                        Attack_Option.attack_roll("腐蚀之触 (Corrupting Touch)", "带着哀嚎的死灵触碰。", 4, 3, 6, 2, 1, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 2, "Constitution", 13),
                        Attack_Option.save_dc("骇人哀号 (Horrifying Wail)", "尖啸震慑周围敌人。", 13, "Wisdom", 0, 1, 0, false, "恐惧")
                                .with_status(Combat_Status_Type.FRIGHTENED, 3, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "wraith",
                "不死生物",
                "怨魂",
                "Wraith",
                "6-7",
                1800,
                13,
                9,
                8,
                18,
                6, 16, 16, 12, 14, 15,
                Arrays.asList(
                        Attack_Option.attack_roll("生命汲取 (Life Drain)", "可怕的死灵近战攻击。", 6, 4, 8, 3, 1, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 3, "Constitution", 14)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "revenant",
                "不死生物",
                "亡命复仇者",
                "Revenant",
                "7-8",
                1800,
                13,
                16,
                8,
                48,
                18, 14, 18, 13, 16, 18,
                Arrays.asList(
                        Attack_Option.attack_roll("拳击 (Fist)", "带着复仇执念的重击。", 7, 2, 6, 4, 2, "钝击"),
                        Attack_Option.attack_roll("复仇凝视 (Vengeful Glare)", "以死亡目光逼迫目标。", 7, 4, 6, 2, 1, "心灵")
                                .with_status(Combat_Status_Type.PARALYZED, 1, "Wisdom", 15)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "bone_naga",
                "不死生物",
                "骨蛇妖",
                "Bone Naga",
                "7-8",
                2300,
                15,
                11,
                10,
                33,
                15, 16, 12, 15, 15, 16,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "带毒的骸骨咬击。", 5, 1, 8, 3, 1, "穿刺")
                                .with_status(Combat_Status_Type.POISONED, 2, "Constitution", 13),
                        Attack_Option.save_dc("定身法术 (Hold Person)", "骨蛇妖释放控制法术。", 14, "Wisdom", 0, 1, 0, false, "控制")
                                .with_status(Combat_Status_Type.PARALYZED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "deathlock",
                "不死生物",
                "亡契法师",
                "Deathlock",
                "8-9",
                3900,
                12,
                10,
                8,
                10,
                10, 15, 12, 14, 12, 16,
                Arrays.asList(
                        Attack_Option.attack_roll("魔能爆 (Eldritch Blast)", "远程力场攻击。", 7, 3, 10, 3, 2, "力场"),
                        Attack_Option.save_dc("枯萎术 (Blight)", "腐朽死灵法术。", 15, "Constitution", 8, 8, 0, false, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "vampire_spawn",
                "不死生物",
                "吸血鬼衍体",
                "Vampire Spawn",
                "8-9",
                1800,
                15,
                11,
                8,
                33,
                16, 16, 16, 11, 10, 12,
                Arrays.asList(
                        Attack_Option.attack_roll("利爪 (Claws)", "迅猛近战攻击。", 6, 2, 4, 3, 2, "挥砍"),
                        Attack_Option.attack_roll("撕咬 (Bite)", "吸血近战攻击。", 6, 2, 6, 3, 1, "穿刺")
                                .with_status(Combat_Status_Type.CURSED, 2, "Constitution", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "lich",
                "不死生物",
                "巫妖",
                "Lich",
                "15-20",
                33000,
                17,
                18,
                8,
                54,
                11, 16, 16, 20, 14, 16,
                Arrays.asList(
                        Attack_Option.save_dc("解离术 (Disintegrate)", "可怕的高阶奥术光线。", 18, "Dexterity", 10, 6, 40, false, "力场"),
                        Attack_Option.save_dc("死亡一指 (Finger of Death)", "毁灭性的死灵法术。", 18, "Constitution", 7, 8, 30, false, "死灵")
                                .with_status(Combat_Status_Type.CURSED, 3, "", 0),
                        Attack_Option.save_dc("支配凝视 (Dominating Gaze)", "巫妖可怕的支配凝视。", 18, "Wisdom", 0, 1, 0, false, "精神")
                                .with_status(Combat_Status_Type.CHARMED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "kobold",
                "狗头人",
                "Kobold",
                "1",
                25,
                12,
                2,
                6,
                2,
                7, 15, 9, 8, 7, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("匕首 (Dagger)", "近战攻击。", 4, 1, 4, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("投石索 (Sling)", "远程攻击。", 4, 1, 4, 2, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "mastiff",
                "野兽",
                "獒犬",
                "Mastiff",
                "1",
                25,
                12,
                1,
                8,
                2,
                13, 14, 12, 3, 12, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 3, 1, 6, 1, 1, "穿刺")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "panther",
                "野兽",
                "黑豹",
                "Panther",
                "1",
                25,
                12,
                3,
                8,
                0,
                14, 15, 10, 3, 14, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("利爪 (Claw)", "敏捷迅猛的近战攻击。", 4, 1, 6, 2, 1, "挥砍")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 12)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "crocodile",
                "野兽",
                "鳄鱼",
                "Crocodile",
                "1",
                50,
                12,
                3,
                10,
                5,
                15, 10, 13, 2, 10, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "咬住并拖拽目标。", 4, 1, 10, 2, 1, "穿刺")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 12)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_badger",
                "野兽",
                "巨獾",
                "Giant Badger",
                "1-2",
                50,
                10,
                2,
                8,
                4,
                13, 10, 15, 2, 12, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 3, 1, 6, 1, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 3, 2, 4, 1, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ape",
                "野兽",
                "猿猴",
                "Ape",
                "1-2",
                100,
                12,
                3,
                8,
                6,
                16, 14, 14, 6, 12, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("拳击 (Fist)", "近战攻击。", 5, 1, 6, 3, 2, "钝击"),
                        Attack_Option.attack_roll("石块 (Rock)", "远程投掷攻击。", 5, 1, 6, 3, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "boar",
                "野兽",
                "野猪",
                "Boar",
                "1-2",
                50,
                11,
                2,
                8,
                2,
                13, 11, 12, 2, 9, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("獠牙 (Tusk)", "近战冲撞攻击。", 3, 1, 6, 1, 1, "挥砍")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_crab",
                "野兽",
                "巨蟹",
                "Giant Crab",
                "1-2",
                50,
                15,
                3,
                8,
                3,
                13, 15, 11, 1, 9, 3,
                Arrays.asList(
                        Attack_Option.attack_roll("蟹钳 (Claw)", "夹住目标的近战攻击。", 3, 1, 6, 1, 1, "钝击")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "constrictor_snake",
                "野兽",
                "蟒蛇",
                "Constrictor Snake",
                "1-2",
                50,
                12,
                2,
                8,
                2,
                15, 14, 12, 1, 10, 3,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 4, 1, 6, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("缠绕 (Constrict)", "缠住目标。", 4, 1, 8, 2, 1, "钝击")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "Strength", 12)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_poisonous_snake",
                "野兽",
                "巨毒蛇",
                "Giant Poisonous Snake",
                "2",
                200,
                14,
                2,
                8,
                2,
                10, 18, 13, 2, 10, 3,
                Arrays.asList(
                        Attack_Option.attack_roll("毒牙 (Bite)", "带毒近战攻击。", 6, 1, 4, 4, 1, "穿刺")
                                .with_status(Combat_Status_Type.POISONED, 3, "Constitution", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "reef_shark",
                "野兽",
                "礁鲨",
                "Reef Shark",
                "2",
                100,
                12,
                3,
                8,
                6,
                14, 13, 13, 1, 10, 4,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "水中近战攻击。", 4, 1, 8, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "bandit",
                "强盗",
                "Bandit",
                "1",
                25,
                12,
                2,
                8,
                2,
                11, 12, 12, 10, 10, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("弯刀 (Scimitar)", "近战攻击。", 3, 1, 6, 1, 1, "挥砍"),
                        Attack_Option.attack_roll("轻弩 (Light Crossbow)", "远程攻击。", 3, 1, 8, 1, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "guard",
                "卫兵",
                "Guard",
                "1",
                25,
                16,
                2,
                8,
                2,
                13, 12, 12, 10, 11, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("长矛 (Spear)", "近战攻击。", 3, 1, 6, 1, 1, "穿刺"),
                        Attack_Option.attack_roll("长矛投掷 (Thrown Spear)", "远程攻击。", 3, 1, 6, 1, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "acolyte",
                "侍僧",
                "Acolyte",
                "1",
                50,
                10,
                2,
                8,
                0,
                10, 10, 10, 10, 14, 11,
                Arrays.asList(
                        Attack_Option.attack_roll("木杖 (Club)", "近战攻击。", 2, 1, 4, 0, 1, "钝击"),
                        Attack_Option.save_dc("圣焰 (Sacred Flame)", "神圣火焰灼烧目标。", 12, "Dexterity", 1, 8, 0, false, "光耀")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_rat",
                "野兽",
                "巨鼠",
                "Giant Rat",
                "1",
                25,
                12,
                2,
                6,
                0,
                7, 15, 11, 2, 10, 4,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "近战攻击。", 4, 1, 4, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "stirge",
                "野兽",
                "针嘴怪",
                "Stirge",
                "1",
                25,
                14,
                1,
                4,
                0,
                4, 16, 11, 2, 8, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("刺吸 (Blood Drain)", "近战吸血攻击。", 5, 1, 4, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_wolf_spider",
                "野兽",
                "巨狼蛛",
                "Giant Wolf Spider",
                "1-2",
                50,
                13,
                2,
                8,
                2,
                12, 16, 13, 3, 12, 4,
                Arrays.asList(
                        Attack_Option.attack_roll("毒牙 (Bite)", "近战攻击。", 3, 1, 6, 1, 1, "穿刺")
                                .with_status(Combat_Status_Type.POISONED, 2, "Constitution", 11)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "thug",
                "打手",
                "Thug",
                "2",
                100,
                11,
                5,
                8,
                10,
                15, 11, 14, 10, 10, 11,
                Arrays.asList(
                        Attack_Option.attack_roll("晨星锤 (Mace)", "连续近战攻击。", 4, 1, 6, 2, 2, "钝击"),
                        Attack_Option.attack_roll("轻弩 (Light Crossbow)", "远程攻击。", 2, 1, 8, 0, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "scout",
                "斥候",
                "Scout",
                "2",
                100,
                13,
                3,
                8,
                3,
                11, 14, 12, 11, 13, 11,
                Arrays.asList(
                        Attack_Option.attack_roll("短剑 (Shortsword)", "近战攻击。", 4, 1, 6, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("长弓 (Longbow)", "双发远程攻击。", 4, 1, 8, 2, 2, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_boar",
                "野兽",
                "巨野猪",
                "Giant Boar",
                "2-3",
                200,
                12,
                5,
                10,
                15,
                17, 10, 16, 2, 7, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("獠牙冲撞 (Tusk)", "强力冲锋近战攻击。", 5, 2, 6, 3, 1, "挥砍")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "lion",
                "野兽",
                "狮子",
                "Lion",
                "2-3",
                200,
                12,
                4,
                10,
                4,
                17, 15, 13, 3, 12, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 5, 1, 8, 3, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "扑击后的近战攻击。", 5, 1, 6, 3, 1, "挥砍")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "tiger",
                "野兽",
                "老虎",
                "Tiger",
                "2-3",
                200,
                12,
                5,
                10,
                5,
                17, 15, 14, 3, 12, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 5, 1, 10, 3, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "扑击后的近战攻击。", 5, 1, 8, 3, 1, "挥砍")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_goat",
                "野兽",
                "巨山羊",
                "Giant Goat",
                "2-3",
                100,
                11,
                4,
                10,
                8,
                17, 11, 12, 3, 12, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("撞角 (Ram)", "冲撞型近战攻击。", 5, 2, 4, 3, 1, "钝击")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_hyena",
                "野兽",
                "巨鬣狗",
                "Giant Hyena",
                "2-3",
                200,
                12,
                6,
                10,
                12,
                16, 14, 14, 2, 12, 9,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "凶狠的近战攻击。", 5, 2, 6, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "animated_armor",
                "活化护甲",
                "Animated Armor",
                "2-3",
                200,
                18,
                6,
                8,
                6,
                14, 11, 13, 1, 3, 1,
                Arrays.asList(
                        Attack_Option.attack_roll("重拳 (Slam)", "近战重击。", 4, 1, 6, 2, 2, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "brown_bear",
                "野兽",
                "棕熊",
                "Brown Bear",
                "2-3",
                200,
                11,
                4,
                10,
                12,
                19, 10, 16, 2, 13, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 5, 1, 8, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 5, 2, 6, 4, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_eagle",
                "野兽",
                "巨鹰",
                "Giant Eagle",
                "3",
                200,
                13,
                4,
                10,
                4,
                16, 17, 13, 8, 14, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("喙击 (Beak)", "俯冲啄击。", 5, 1, 6, 3, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Talons)", "俯冲利爪。", 5, 2, 6, 3, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_vulture",
                "野兽",
                "巨秃鹫",
                "Giant Vulture",
                "3",
                200,
                10,
                3,
                10,
                6,
                15, 10, 15, 6, 12, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("喙击 (Beak)", "近战攻击。", 4, 1, 6, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Talons)", "近战攻击。", 4, 2, 4, 2, 1, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_toad",
                "野兽",
                "巨蟾蜍",
                "Giant Toad",
                "3-4",
                200,
                11,
                4,
                10,
                8,
                15, 13, 13, 2, 10, 3,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "吞咬近战攻击。", 4, 1, 10, 2, 1, "穿刺")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ghast",
                "不死生物",
                "食尸妖",
                "Ghast",
                "3",
                450,
                13,
                8,
                8,
                16,
                16, 17, 10, 11, 10, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("爪击 (Claws)", "近战攻击。", 5, 2, 6, 3, 1, "挥砍")
                                .with_status(Combat_Status_Type.PARALYZED, 1, "Constitution", 10),
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 3, 2, 8, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "cult_fanatic",
                "邪教狂热者",
                "Cult Fanatic",
                "3",
                450,
                13,
                6,
                8,
                6,
                11, 14, 12, 10, 13, 14,
                Arrays.asList(
                        Attack_Option.attack_roll("匕首 (Dagger)", "近战攻击。", 4, 1, 4, 2, 1, "穿刺"),
                        Attack_Option.save_dc("命令术 (Command)", "强迫目标短暂失去行动节奏。", 11, "Wisdom", 0, 1, 0, false, "控制")
                                .with_status(Combat_Status_Type.FRIGHTENED, 1, "", 0),
                        Attack_Option.save_dc("灵体守卫 (Spiritual Assault)", "神圣或邪异能量重创目标。", 11, "Wisdom", 3, 8, 0, true, "死灵")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "dire_wolf",
                "野兽",
                "恐狼",
                "Dire Wolf",
                "3-4",
                200,
                14,
                5,
                10,
                10,
                17, 15, 15, 3, 12, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "强力近战攻击。", 5, 2, 6, 3, 1, "穿刺")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_constrictor_snake",
                "野兽",
                "巨蟒",
                "Giant Constrictor Snake",
                "4",
                450,
                12,
                8,
                12,
                16,
                19, 14, 12, 1, 10, 3,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 6, 2, 6, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("缠绕 (Constrict)", "缠住目标。", 6, 2, 8, 4, 1, "钝击")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "Strength", 14)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_octopus",
                "野兽",
                "巨章鱼",
                "Giant Octopus",
                "4-5",
                450,
                11,
                8,
                10,
                8,
                17, 13, 13, 4, 10, 4,
                Arrays.asList(
                        Attack_Option.attack_roll("触腕 (Tentacles)", "缠绕并挤压目标。", 5, 2, 6, 3, 1, "钝击")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "Strength", 13),
                        Attack_Option.save_dc("墨汁喷射 (Ink Cloud)", "制造遮蔽并扰乱目标。", 12, "Constitution", 0, 1, 0, false, "干扰")
                                .with_status(Combat_Status_Type.CURSED, 1, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "gargoyle",
                "石像鬼",
                "Gargoyle",
                "4",
                450,
                15,
                7,
                8,
                14,
                15, 11, 16, 6, 11, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "近战攻击。", 4, 1, 6, 2, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 4, 1, 6, 2, 2, "挥砍")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "harpy",
                "鹰身女妖",
                "Harpy",
                "4",
                200,
                11,
                5,
                8,
                5,
                12, 13, 12, 7, 10, 13,
                Arrays.asList(
                        Attack_Option.save_dc("惑魂歌声 (Luring Song)", "歌声迷惑目标。", 11, "Wisdom", 0, 1, 0, false, "惑控")
                                .with_status(Combat_Status_Type.CHARMED, 2, "", 0),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 3, 2, 4, 1, 1, "挥砍"),
                        Attack_Option.attack_roll("木棒 (Club)", "近战攻击。", 3, 1, 4, 1, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "mimic",
                "拟形怪",
                "Mimic",
                "4",
                450,
                12,
                9,
                8,
                18,
                17, 12, 15, 5, 13, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("伪足 (Pseudopod)", "黏性近战攻击。", 5, 1, 8, 3, 1, "钝击")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 13),
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 5, 1, 8, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_crocodile",
                "野兽",
                "巨鳄",
                "Giant Crocodile",
                "5-6",
                1800,
                14,
                11,
                12,
                33,
                21, 9, 17, 2, 10, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "重型咬击并拖拽目标。", 8, 3, 10, 5, 1, "穿刺")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 15),
                        Attack_Option.attack_roll("尾扫 (Tail)", "横扫近战攻击。", 8, 2, 8, 5, 1, "钝击")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 15)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_ape",
                "野兽",
                "巨猿",
                "Giant Ape",
                "8-10",
                2900,
                12,
                15,
                12,
                45,
                23, 14, 18, 7, 12, 7,
                Arrays.asList(
                        Attack_Option.attack_roll("重拳 (Fist)", "沉重近战连击。", 9, 3, 10, 6, 2, "钝击"),
                        Attack_Option.attack_roll("投掷巨石 (Rock)", "远程重型投掷。", 9, 7, 6, 6, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "minotaur",
                "牛头怪",
                "Minotaur",
                "4-5",
                700,
                14,
                9,
                10,
                27,
                18, 11, 16, 6, 16, 9,
                Arrays.asList(
                        Attack_Option.attack_roll("巨斧 (Greataxe)", "近战攻击。", 6, 2, 12, 4, 1, "挥砍"),
                        Attack_Option.attack_roll("冲锋撞角 (Gore)", "冲锋近战攻击。", 6, 2, 8, 4, 1, "穿刺")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 14)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "spectator",
                "观众魔",
                "Spectator",
                "4-5",
                700,
                14,
                13,
                8,
                13,
                8, 14, 14, 13, 14, 11,
                Arrays.asList(
                        Attack_Option.attack_roll("咬击 (Bite)", "近战攻击。", 1, 2, 6, -1, 1, "穿刺"),
                        Attack_Option.save_dc("麻痹眼线 (Paralyzing Ray)", "眼线令目标麻痹。", 13, "Constitution", 0, 1, 0, false, "控制")
                                .with_status(Combat_Status_Type.PARALYZED, 1, "", 0),
                        Attack_Option.save_dc("恐惧眼线 (Fear Ray)", "眼线令目标恐慌。", 13, "Wisdom", 0, 1, 0, false, "精神")
                                .with_status(Combat_Status_Type.FRIGHTENED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "gelatinous_cube",
                "胶质方块",
                "Gelatinous Cube",
                "4-5",
                450,
                6,
                10,
                10,
                40,
                14, 3, 20, 1, 6, 1,
                Arrays.asList(
                        Attack_Option.attack_roll("包覆 (Engulf)", "把目标吞入体内。", 3, 3, 6, 0, 1, "强酸")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "Dexterity", 12),
                        Attack_Option.attack_roll("伪足 (Pseudopod)", "近战攻击。", 3, 3, 6, 0, 1, "强酸")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 12)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "basilisk",
                "蛇怪",
                "Basilisk",
                "5",
                700,
                15,
                8,
                8,
                16,
                16, 8, 15, 2, 8, 7,
                Arrays.asList(
                        Attack_Option.save_dc("石化凝视 (Petrifying Gaze)", "恐怖凝视令目标逐渐石化。", 12, "Constitution", 0, 1, 0, false, "石化")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "", 0),
                        Attack_Option.attack_roll("撕咬 (Bite)", "近战攻击。", 5, 2, 6, 3, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "manticore",
                "蝎尾狮",
                "Manticore",
                "5",
                700,
                14,
                8,
                10,
                24,
                17, 16, 17, 7, 12, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "近战攻击。", 5, 1, 8, 3, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claws)", "近战攻击。", 5, 1, 6, 3, 2, "挥砍"),
                        Attack_Option.attack_roll("尾刺 (Tail Spike)", "远程尾刺攻击。", 5, 1, 8, 3, 3, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "veteran",
                "老兵",
                "Veteran",
                "5",
                700,
                17,
                9,
                8,
                18,
                16, 13, 14, 10, 11, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("长剑 (Longsword)", "多重近战攻击。", 5, 1, 8, 3, 2, "挥砍"),
                        Attack_Option.attack_roll("短剑 (Shortsword)", "副手近战攻击。", 5, 1, 6, 3, 1, "穿刺"),
                        Attack_Option.attack_roll("轻弩 (Heavy Crossbow)", "远程攻击。", 3, 1, 10, 1, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "saber_toothed_tiger",
                "野兽",
                "刃齿虎",
                "Saber-Toothed Tiger",
                "6",
                700,
                12,
                8,
                10,
                16,
                18, 14, 15, 3, 12, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "猛扑后的撕咬。", 6, 1, 10, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "猛扑利爪。", 6, 2, 6, 4, 1, "挥砍")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 14)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "giant_elk",
                "野兽",
                "巨麋鹿",
                "Giant Elk",
                "6-7",
                700,
                14,
                5,
                12,
                20,
                19, 16, 14, 7, 14, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("撞角 (Ram)", "高速冲撞。", 6, 2, 6, 4, 1, "钝击")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 14),
                        Attack_Option.attack_roll("践踏 (Hooves)", "目标倒地后更危险。", 6, 4, 8, 4, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "knight",
                "骑士",
                "Knight",
                "5-6",
                700,
                18,
                8,
                8,
                24,
                16, 11, 14, 11, 11, 15,
                Arrays.asList(
                        Attack_Option.attack_roll("巨剑 (Greatsword)", "多重近战攻击。", 5, 2, 6, 3, 2, "挥砍"),
                        Attack_Option.attack_roll("重弩 (Heavy Crossbow)", "远程攻击。", 2, 1, 10, 0, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ettin",
                "双头巨人",
                "Ettin",
                "6-7",
                1100,
                12,
                10,
                10,
                30,
                21, 8, 17, 6, 10, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("战斧 (Battleaxe)", "双重近战攻击。", 7, 2, 8, 5, 2, "挥砍"),
                        Attack_Option.attack_roll("流星锤 (Morningstar)", "双重近战攻击。", 7, 2, 8, 5, 2, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "hill_giant",
                "丘陵巨人",
                "Hill Giant",
                "7-8",
                1800,
                13,
                12,
                12,
                48,
                21, 8, 19, 5, 9, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("巨棒 (Greatclub)", "近战重击。", 8, 3, 8, 5, 1, "钝击"),
                        Attack_Option.attack_roll("巨石 (Rock)", "远程巨石投掷。", 8, 3, 10, 5, 1, "钝击")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "chimera",
                "奇美拉",
                "Chimera",
                "7-8",
                2300,
                14,
                12,
                10,
                36,
                19, 11, 19, 3, 14, 10,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "多重近战攻击。", 7, 2, 6, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("撞角 (Horns)", "多重近战攻击。", 7, 1, 12, 4, 1, "钝击"),
                        Attack_Option.attack_roll("利爪 (Claws)", "多重近战攻击。", 7, 2, 6, 4, 1, "挥砍"),
                        Attack_Option.save_dc("火焰吐息 (Fire Breath)", "锥形火焰吐息。", 15, "Dexterity", 7, 8, 0, true, "火焰")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "worg",
                "野兽",
                "座狼",
                "Worg",
                "3",
                200,
                13,
                4,
                10,
                8,
                16, 13, 13, 7, 11, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "凶狠的近战撕咬。", 5, 2, 6, 3, 1, "穿刺")
                                .with_status(Combat_Status_Type.PRONE, 1, "Strength", 13)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ankheg",
                "怪虫",
                "蚁狮虫",
                "Ankheg",
                "4-5",
                450,
                14,
                6,
                10,
                12,
                17, 11, 13, 1, 13, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("巨颚 (Bite)", "强力近战啃咬。", 5, 2, 6, 3, 1, "挥砍")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 13),
                        Attack_Option.save_dc("酸液喷吐 (Acid Spray)", "喷吐腐蚀性酸液。", 13, "Dexterity", 4, 6, 0, true, "强酸")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "ettercap",
                "怪物",
                "蛛化怪",
                "Ettercap",
                "4",
                450,
                13,
                8,
                8,
                16,
                14, 15, 13, 7, 12, 8,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "带毒的近战啃咬。", 5, 1, 8, 2, 1, "穿刺")
                                .with_status(Combat_Status_Type.POISONED, 2, "Constitution", 11),
                        Attack_Option.save_dc("蛛网 (Web)", "黏稠蛛网困住目标。", 12, "Dexterity", 0, 1, 0, false, "束缚")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "hell_hound",
                "邪魔兽",
                "地狱猎犬",
                "Hell Hound",
                "5",
                700,
                15,
                7,
                10,
                14,
                17, 12, 14, 6, 13, 6,
                Arrays.asList(
                        Attack_Option.attack_roll("撕咬 (Bite)", "燃烧獠牙的近战攻击。", 5, 2, 6, 3, 1, "穿刺")
                                .with_status(Combat_Status_Type.BURNING, 2, "", 0),
                        Attack_Option.save_dc("火焰吐息 (Fire Breath)", "锥形火焰吐息。", 12, "Dexterity", 6, 6, 0, true, "火焰")
                                .with_status(Combat_Status_Type.BURNING, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "shambling_mound",
                "植物",
                "活化藤丘",
                "Shambling Mound",
                "6-7",
                1800,
                15,
                16,
                10,
                32,
                18, 8, 16, 5, 10, 5,
                Arrays.asList(
                        Attack_Option.attack_roll("猛击 (Slam)", "缠着藤蔓的重击。", 7, 2, 8, 4, 2, "钝击")
                                .with_status(Combat_Status_Type.RESTRAINED, 1, "Strength", 14)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "medusa",
                "怪物",
                "美杜莎",
                "Medusa",
                "7-8",
                2300,
                15,
                17,
                8,
                34,
                10, 15, 16, 12, 13, 15,
                Arrays.asList(
                        Attack_Option.save_dc("石化凝视 (Petrifying Gaze)", "凝视令目标逐渐僵硬石化。", 14, "Constitution", 0, 1, 0, false, "石化")
                                .with_status(Combat_Status_Type.RESTRAINED, 2, "", 0),
                        Attack_Option.attack_roll("蛇发短弓 (Longbow)", "精准的远程攻击。", 5, 1, 8, 2, 2, "穿刺"),
                        Attack_Option.attack_roll("蛇发短剑 (Shortsword)", "近战攻击。", 5, 1, 6, 2, 1, "穿刺")
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "young_black_dragon",
                "青年黑龙",
                "Young Black Dragon",
                "8-10",
                2900,
                18,
                15,
                10,
                45,
                19, 14, 17, 12, 11, 15,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "近战攻击。", 7, 2, 10, 4, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "近战攻击。", 7, 2, 6, 4, 2, "挥砍"),
                        Attack_Option.save_dc("强酸吐息 (Acid Breath)", "直线强酸吐息。", 14, "Dexterity", 11, 8, 0, true, "强酸")
                                .with_status(Combat_Status_Type.BURNING, 2, "", 0)
                )
        ));
        MONSTERS.add(new Monster_Definition(
                "young_red_dragon",
                "青年红龙",
                "Young Red Dragon",
                "10-12",
                5900,
                18,
                19,
                10,
                76,
                23, 10, 21, 14, 11, 19,
                Arrays.asList(
                        Attack_Option.attack_roll("啃咬 (Bite)", "近战攻击。", 10, 2, 10, 6, 1, "穿刺"),
                        Attack_Option.attack_roll("利爪 (Claw)", "近战攻击。", 10, 2, 6, 6, 2, "挥砍"),
                        Attack_Option.save_dc("火焰吐息 (Fire Breath)", "大范围火焰吐息。", 17, "Dexterity", 16, 6, 0, true, "火焰")
                                .with_status(Combat_Status_Type.BURNING, 3, "", 0)
                )
        ));
    }

    private Monster_Library() {
    }

    public static List<Monster_Definition> get_all_monsters() {
        return new ArrayList<>(MONSTERS);
    }

    public static List<Monster_Definition> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return get_all_monsters();
        }

        List<Monster_Definition> result = new ArrayList<>();
        for (Monster_Definition monster : MONSTERS) {
            String haystack = (monster.chinese_name + " " + monster.english_name + " " + monster.monster_type + " " + monster.recommended_level).toLowerCase();
            if (haystack.contains(normalized)) {
                result.add(monster);
            }
        }
        return result;
    }
}
