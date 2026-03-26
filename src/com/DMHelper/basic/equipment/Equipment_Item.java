package com.DMHelper.basic.equipment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Equipment_Item {
    public final String key;
    public final String display_name;
    public final Equipment_Slot slot;
    public final String description;
    public final String armor_type;
    public final int base_ac;
    public final int shield_bonus;
    public final int attack_dice_count;
    public final int attack_die_size;
    public final int attack_bonus;
    public final String damage_type;
    public final boolean finesse;
    public final boolean ranged;

    public Equipment_Item(String key,
                          String display_name,
                          Equipment_Slot slot,
                          String description,
                          String armor_type,
                          int base_ac,
                          int shield_bonus,
                          int attack_dice_count,
                          int attack_die_size,
                          int attack_bonus,
                          String damage_type,
                          boolean finesse,
                          boolean ranged) {
        this.key = key;
        this.display_name = display_name;
        this.slot = slot;
        this.description = description;
        this.armor_type = armor_type;
        this.base_ac = base_ac;
        this.shield_bonus = shield_bonus;
        this.attack_dice_count = attack_dice_count;
        this.attack_die_size = attack_die_size;
        this.attack_bonus = attack_bonus;
        this.damage_type = damage_type;
        this.finesse = finesse;
        this.ranged = ranged;
    }

    public String to_inventory_line() {
        StringBuilder sb = new StringBuilder(this.display_name);
        if (this.key != null && this.key.startsWith("custom_")) {
            sb.append(" [自定义]");
        }
        sb.append(" | 槽位: ").append(get_slot_label());
        if (this.slot == Equipment_Slot.ARMOR) {
            sb.append(" | AC ").append(this.base_ac);
            if (!"None".equals(this.armor_type)) {
                sb.append(" | ").append(get_armor_type_label());
            }
        } else if (this.slot == Equipment_Slot.MAIN_HAND) {
            sb.append(" | 伤害 ").append(this.attack_dice_count).append("d").append(this.attack_die_size);
            if (this.attack_bonus != 0) {
                sb.append(this.attack_bonus > 0 ? "+" : "").append(this.attack_bonus);
            }
            if (this.damage_type != null && !this.damage_type.isEmpty()) {
                sb.append(" ").append(this.damage_type);
            }
        } else if (this.slot == Equipment_Slot.OFF_HAND && this.shield_bonus > 0) {
            sb.append(" | AC +").append(this.shield_bonus);
        } else if (this.slot == Equipment_Slot.BACKPACK) {
            sb.append(" | 背包物品");
        }
        sb.append(" | ").append(this.description);
        return sb.toString();
    }

    public String get_slot_label() {
        if (this.slot == Equipment_Slot.ARMOR) return "护甲";
        if (this.slot == Equipment_Slot.MAIN_HAND) return "主手武器";
        if (this.slot == Equipment_Slot.OFF_HAND) return "副手/盾牌";
        if (this.slot == Equipment_Slot.CLOAK) return "披风";
        if (this.slot == Equipment_Slot.ACCESSORY) return "护符";
        if (this.slot == Equipment_Slot.BACKPACK) return "背包杂物";
        return this.slot.name();
    }

    public String get_armor_type_label() {
        if ("Light".equals(this.armor_type)) return "轻甲";
        if ("Medium".equals(this.armor_type)) return "中甲";
        if ("Heavy".equals(this.armor_type)) return "重甲";
        return "无甲";
    }

    public boolean is_usable_inventory_item() {
        return this.slot == Equipment_Slot.BACKPACK;
    }

    public boolean is_healing_item() {
        return get_healing_dice_count() > 0 || get_flat_healing_amount() > 0;
    }

    public int get_healing_dice_count() {
        if ("healing_potion".equals(this.key)) return 2;
        if ("greater_healing_potion".equals(this.key)) return 4;
        return 0;
    }

    public int get_healing_die_size() {
        if ("healing_potion".equals(this.key) || "greater_healing_potion".equals(this.key)) return 4;
        return 0;
    }

    public int get_healing_bonus() {
        if ("healing_potion".equals(this.key)) return 2;
        if ("greater_healing_potion".equals(this.key)) return 4;
        return 0;
    }

    public int get_flat_healing_amount() {
        Matcher matcher = Pattern.compile("恢复\\s*(\\d+)\\s*点?\\s*生命").matcher(this.description == null ? "" : this.description);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        matcher = Pattern.compile("恢复\\s*(\\d+)\\s*HP", Pattern.CASE_INSENSITIVE).matcher(this.description == null ? "" : this.description);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public String get_use_hint() {
        if (is_healing_item()) {
            if (get_healing_dice_count() > 0) {
                return "饮用后恢复 " + get_healing_dice_count() + "d" + get_healing_die_size()
                        + (get_healing_bonus() > 0 ? "+" + get_healing_bonus() : "") + " 点生命值。";
            }
            return "使用后恢复 " + get_flat_healing_amount() + " 点生命值。";
        }
        if (is_scroll_item()) {
            return "阅读后触发卷轴效果，并通常会消耗该卷轴。";
        }
        if (is_bomb_item()) {
            return "投掷后造成一次爆炸伤害，并消耗该炸弹。";
        }
        if (is_key_item()) {
            return "用于解锁、开启或触发特定机关，不会自动消耗。";
        }
        if (is_quest_item()) {
            return "可用于调查、提交或推进任务，一般不会自动消耗。";
        }
        return "当前没有自动结算效果，可手动标记为已使用。";
    }

    public boolean is_stackable() {
        return this.slot == Equipment_Slot.BACKPACK;
    }

    public String get_inventory_category() {
        if (this.slot != Equipment_Slot.BACKPACK) {
            return "装备";
        }
        String text = ((this.display_name == null ? "" : this.display_name) + " " + (this.description == null ? "" : this.description)).toLowerCase();
        if (text.contains("药水") || text.contains("卷轴") || text.contains("恢复") || text.contains("饮用")
                || text.contains("炸弹") || text.contains("bomb") || text.contains("燃烧瓶")) {
            return "消耗品";
        }
        if (text.contains("钥匙") || text.contains("地图") || text.contains("工具") || text.contains("徽记") || text.contains("纹章")) {
            return "工具/任务";
        }
        if (this.key != null && this.key.startsWith("custom_")) {
            return "自定义";
        }
        return "材料/战利品";
    }

    public boolean is_scroll_item() {
        String text = ((this.display_name == null ? "" : this.display_name) + " " + (this.description == null ? "" : this.description)).toLowerCase();
        return text.contains("卷轴");
    }

    public boolean is_bomb_item() {
        String text = ((this.display_name == null ? "" : this.display_name) + " " + (this.description == null ? "" : this.description)).toLowerCase();
        return text.contains("炸弹") || text.contains("bomb") || text.contains("燃烧瓶");
    }

    public boolean is_key_item() {
        String text = ((this.display_name == null ? "" : this.display_name) + " " + (this.description == null ? "" : this.description)).toLowerCase();
        return text.contains("钥匙");
    }

    public boolean is_quest_item() {
        String text = ((this.display_name == null ? "" : this.display_name) + " " + (this.description == null ? "" : this.description)).toLowerCase();
        return text.contains("地图") || text.contains("徽记") || text.contains("纹章") || text.contains("圣物") || text.contains("信物");
    }

    public int get_bomb_dice_count() {
        if ("fire_bomb".equals(this.key)) return 2;
        if ("thunder_bomb".equals(this.key)) return 2;
        return 0;
    }

    public int get_bomb_die_size() {
        if ("fire_bomb".equals(this.key)) return 6;
        if ("thunder_bomb".equals(this.key)) return 8;
        return 0;
    }

    public int get_bomb_bonus() {
        return 0;
    }

    public String get_bomb_damage_type() {
        if ("fire_bomb".equals(this.key)) return "火焰";
        if ("thunder_bomb".equals(this.key)) return "雷鸣";
        return this.damage_type == null || this.damage_type.trim().isEmpty() ? "伤害" : this.damage_type;
    }
}
