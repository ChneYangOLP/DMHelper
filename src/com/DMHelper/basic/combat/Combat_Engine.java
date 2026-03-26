package com.DMHelper.basic.combat;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 战斗结算引擎。
 * UI 只负责展示和收集选择，真正的命中、伤害、状态、轮转、经验与掉落都在这里完成。
 */
public class Combat_Engine {
    private final List<Combatant> initiative_order;
    private final List<Combatant> player_combatants;
    private final List<Character_Sheet> participating_characters;
    private final List<Monster_Definition> encounter_monsters;
    private final int total_encounter_xp;
    private final List<String> pending_loot_keys;
    private boolean combat_finished;
    private boolean players_victorious;
    private int distributed_xp;
    private String pending_system_log;

    public Combat_Engine(List<Character_Sheet> characters, List<Monster_Definition> monsters) {
        this.initiative_order = new ArrayList<>();
        this.player_combatants = new ArrayList<>();
        this.participating_characters = new ArrayList<>(characters);
        this.encounter_monsters = new ArrayList<>(monsters);
        this.pending_loot_keys = new ArrayList<>();
        int xpCounter = 0;

        for (Character_Sheet character : characters) {
            Combatant combatant = Combat_Attack_Helper.build_player_combatant(character);
            roll_initiative(combatant);
            this.initiative_order.add(combatant);
            this.player_combatants.add(combatant);
        }

        int serial = 1;
        for (Monster_Definition monster : monsters) {
            Combatant combatant = Combat_Attack_Helper.build_enemy_combatant(monster, serial++);
            roll_initiative(combatant);
            this.initiative_order.add(combatant);
            xpCounter += monster.xp_reward;
        }
        this.total_encounter_xp = xpCounter;

        // 先攻按照：总先攻值 -> 敏捷 -> 名称 排序，保证同值时也有稳定顺序。
        this.initiative_order.sort(Comparator
                .comparingInt((Combatant combatant) -> combatant.initiative_total).reversed()
                .thenComparingInt(combatant -> combatant.dexterity).reversed()
                .thenComparing(combatant -> combatant.display_name));
    }

    private void roll_initiative(Combatant combatant) {
        combatant.initiative_roll = Dice_Util.roll_d20();
        combatant.initiative_total = combatant.initiative_roll + combatant.initiative_modifier;
    }

    public Combatant get_active_combatant() {
        cleanup_dead();
        return this.initiative_order.isEmpty() ? null : this.initiative_order.get(0);
    }

    public List<Combatant> get_initiative_order() {
        cleanup_dead();
        return new ArrayList<>(this.initiative_order);
    }

    public List<Combatant> get_valid_targets() {
        Combatant active = get_active_combatant();
        List<Combatant> targets = new ArrayList<>();
        if (active == null) {
            return targets;
        }
        for (Combatant combatant : this.initiative_order) {
            if (combatant.side != active.side && combatant.is_alive()) {
                targets.add(combatant);
            }
        }
        return targets;
    }

    public String execute_attack(Attack_Option attackOption, Combatant target) {
        Combatant attacker = get_active_combatant();
        if (attacker == null || target == null || attackOption == null) {
            return "当前没有可执行的攻击。";
        }
        if (attacker.is_turn_blocked()) {
            return attacker.display_name + " 当前处于无法行动状态，只能结束回合。";
        }
        String resourceCheck = can_pay_cost(attacker, attackOption);
        if (resourceCheck != null) {
            return resourceCheck;
        }

        StringBuilder log = new StringBuilder();
        log.append(attacker.display_name).append(" 使用 ").append(attackOption.name).append(" -> ").append(target.display_name).append("\n");
        // 先扣资源，再结算攻击；这样战斗日志能准确反映真实消耗。
        consume_cost(attacker, attackOption, log);

        if (attackOption.resolution_type == Attack_Option.Resolution_Type.ATTACK_ROLL) {
            for (int i = 1; i <= attackOption.attack_count; i++) {
                int d20 = Dice_Util.roll_d20();
                int totalAttack = d20 + attackOption.attack_bonus + attacker.get_effective_attack_modifier();
                boolean critical = d20 == 20;
                boolean hit = critical || totalAttack >= target.get_effective_armor_class();
                log.append("第 ").append(i).append(" 次攻击：d20=").append(d20)
                        .append(" + ").append(attackOption.attack_bonus + attacker.get_effective_attack_modifier())
                        .append(" = ").append(totalAttack)
                        .append(hit ? "，命中" : "，未命中")
                        .append("\n");
                if (hit) {
                    int damage = Math.max(0, attackOption.roll_damage(critical));
                    target.current_hp = Math.max(0, target.current_hp - damage);
                    log.append("造成 ").append(damage).append(" 点").append(attackOption.damage_type)
                            .append(critical ? "（重击）" : "")
                            .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");
                    apply_status_if_needed(attackOption, target, log, false);
                }
                if (!target.is_alive()) {
                    log.append(target.display_name).append(" 倒下了。\n");
                    break;
                }
            }
        } else if (attackOption.resolution_type == Attack_Option.Resolution_Type.SAVE_DC) {
            int saveRoll = Dice_Util.roll_d20();
            int saveBonus = target.get_saving_throw_bonus(attackOption.save_ability);
            int saveTotal = saveRoll + saveBonus;
            int damage = Math.max(0, attackOption.roll_damage(false));
            boolean success = saveTotal >= attackOption.save_dc;
            int appliedDamage = success && attackOption.half_damage_on_save ? damage / 2 : (success ? 0 : damage);
            target.current_hp = Math.max(0, target.current_hp - appliedDamage);
            log.append(target.display_name).append(" 进行 ").append(attackOption.save_ability).append(" 豁免：d20=")
                    .append(saveRoll).append(" + ").append(saveBonus).append(" = ").append(saveTotal)
                    .append(success ? "，成功" : "，失败").append("\n");
            log.append("造成 ").append(appliedDamage).append(" 点").append(attackOption.damage_type)
                    .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");
            if (!success) {
                apply_status_if_needed(attackOption, target, log, true);
            }
            if (!target.is_alive()) {
                log.append(target.display_name).append(" 倒下了。\n");
            }
        } else {
            int damage = Math.max(0, attackOption.roll_damage(false));
            target.current_hp = Math.max(0, target.current_hp - damage);
            log.append("自动命中，造成 ").append(damage).append(" 点").append(attackOption.damage_type)
                    .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");
            apply_status_if_needed(attackOption, target, log, true);
            if (!target.is_alive()) {
                log.append(target.display_name).append(" 倒下了。\n");
            }
        }

        cleanup_dead();
        if (is_side_defeated(Combatant.Side.ENEMY)) {
            distribute_experience();
            generate_loot();
            this.players_victorious = true;
            this.combat_finished = true;
            log.append("战斗胜利！每位参战角色获得 ").append(this.distributed_xp).append(" 经验值。\n");
            if (this.pending_loot_keys.isEmpty()) {
                log.append("本次战斗没有额外掉落物。\n");
            } else {
                log.append("本次战斗掉落 ").append(this.pending_loot_keys.size()).append(" 件物品，等待分配。\n");
            }
        } else if (is_side_defeated(Combatant.Side.PLAYER)) {
            this.combat_finished = true;
            log.append("战斗失败，所有参战角色都倒下了。\n");
        }

        if (!this.combat_finished) {
            advance_turn();
        }
        // 每次行动后都把玩家当前 HP/法术位/职业资源回写，避免战斗结束前切回管理界面时数据不一致。
        sync_player_states();
        return log.toString().trim();
    }

    public String skip_turn() {
        Combatant active = get_active_combatant();
        if (active == null) {
            return "当前没有可跳过的回合。";
        }
        String message = active.display_name + (active.is_turn_blocked() ? " 因状态影响无法行动，回合结束。" : " 结束了本回合。");
        advance_turn();
        sync_player_states();
        return message;
    }

    private void advance_turn() {
        cleanup_dead();
        if (this.initiative_order.size() <= 1) {
            return;
        }
        // 把当前行动者挪到队尾，形成“回合制循环队列”。
        Combatant first = this.initiative_order.remove(0);
        decrement_statuses(first);
        this.initiative_order.add(first);
        cleanup_dead();
        Combatant next = get_active_combatant();
        if (next != null) {
            this.pending_system_log = process_start_of_turn(next);
        }
    }

    private void cleanup_dead() {
        this.initiative_order.removeIf(combatant -> !combatant.is_alive());
    }

    private boolean is_side_defeated(Combatant.Side side) {
        for (Combatant combatant : this.initiative_order) {
            if (combatant.side == side && combatant.is_alive()) {
                return false;
            }
        }
        return true;
    }

    private void distribute_experience() {
        if (this.participating_characters.isEmpty()) {
            this.distributed_xp = 0;
            return;
        }

        // 当前实现按参战角色平均分配 XP，方便桌面工具快速结算。
        this.distributed_xp = Math.max(0, this.total_encounter_xp / this.participating_characters.size());
        for (Character_Sheet character : this.participating_characters) {
            character.add_experience(this.distributed_xp);
            character.record_advancement("战斗结算获得经验值：" + this.distributed_xp);
            Character_DAO.update_character(character);
        }
    }

    private void generate_loot() {
        this.pending_loot_keys.clear();
        this.pending_loot_keys.addAll(Monster_Loot_Helper.roll_loot_for_encounter(this.encounter_monsters));
    }

    public boolean is_combat_finished() {
        return this.combat_finished;
    }

    public boolean did_players_win() {
        return this.players_victorious;
    }

    public int get_distributed_xp() {
        return this.distributed_xp;
    }

    public List<Character_Sheet> get_participating_characters() {
        return new ArrayList<>(this.participating_characters);
    }

    public List<String> get_pending_loot_keys() {
        return new ArrayList<>(this.pending_loot_keys);
    }

    public void assign_loot(List<String> lootKeys, Character_Sheet receiver) {
        if (receiver == null || lootKeys == null || lootKeys.isEmpty()) {
            return;
        }

        List<String> assigned = new ArrayList<>();
        for (String lootKey : lootKeys) {
            if (this.pending_loot_keys.remove(lootKey)) {
                receiver.add_item_to_inventory(lootKey);
                assigned.add(lootKey);
            }
        }

        if (!assigned.isEmpty()) {
            List<String> displayNames = new ArrayList<>();
            for (String assignedKey : assigned) {
                Equipment_Item item = Equipment_Library.get_item(assignedKey);
                displayNames.add(item == null ? assignedKey : item.display_name);
            }
            receiver.record_advancement("战斗后获得战利品：" + String.join("、", displayNames));
            Character_DAO.update_character(receiver);
        }
    }

    public String get_and_clear_pending_log() {
        String log = this.pending_system_log;
        this.pending_system_log = null;
        return log;
    }

    public String apply_external_damage(String sourceLabel, Combatant target, int damage, String damageType, String note) {
        if (target == null || damage < 0) {
            return "外部效果未能生效。";
        }
        StringBuilder log = new StringBuilder();
        log.append(sourceLabel == null || sourceLabel.trim().isEmpty() ? "外部效果" : sourceLabel)
                .append(" -> ").append(target.display_name).append("\n");
        target.current_hp = Math.max(0, target.current_hp - damage);
        log.append("造成 ").append(damage).append(" 点").append(damageType == null || damageType.trim().isEmpty() ? "伤害" : damageType)
                .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");
        if (note != null && !note.trim().isEmpty()) {
            log.append("备注：").append(note.trim()).append("\n");
        }
        if (!target.is_alive()) {
            log.append(target.display_name).append(" 倒下了。\n");
        }
        handle_post_external_resolution(log);
        sync_player_states();
        return log.toString().trim();
    }

    public String apply_external_healing(String sourceLabel, Combatant target, int amount, String note) {
        if (target == null || amount <= 0) {
            return "外部治疗未能生效。";
        }
        StringBuilder log = new StringBuilder();
        log.append(sourceLabel == null || sourceLabel.trim().isEmpty() ? "外部治疗" : sourceLabel)
                .append(" -> ").append(target.display_name).append("\n");
        int beforeHp = target.current_hp;
        target.current_hp = Math.min(target.max_hp, target.current_hp + amount);
        int healed = target.current_hp - beforeHp;
        log.append("恢复 ").append(healed).append(" 点生命值，目标当前 HP ")
                .append(target.current_hp).append("/").append(target.max_hp).append("\n");
        if (note != null && !note.trim().isEmpty()) {
            log.append("备注：").append(note.trim()).append("\n");
        }
        sync_player_states();
        return log.toString().trim();
    }

    private void handle_post_external_resolution(StringBuilder log) {
        cleanup_dead();
        if (is_side_defeated(Combatant.Side.ENEMY) && !this.combat_finished) {
            distribute_experience();
            generate_loot();
            this.players_victorious = true;
            this.combat_finished = true;
            log.append("战斗胜利！每位参战角色获得 ").append(this.distributed_xp).append(" 经验值。\n");
            if (this.pending_loot_keys.isEmpty()) {
                log.append("本次战斗没有额外掉落物。\n");
            } else {
                log.append("本次战斗掉落 ").append(this.pending_loot_keys.size()).append(" 件物品，等待分配。\n");
            }
        } else if (is_side_defeated(Combatant.Side.PLAYER) && !this.combat_finished) {
            this.combat_finished = true;
            log.append("战斗失败，所有参战角色都倒下了。\n");
        }
    }

    private void sync_player_states() {
        for (Combatant combatant : this.player_combatants) {
            if (combatant.linked_character == null) {
                continue;
            }
            combatant.linked_character.sync_from_combatant(combatant);
            Character_DAO.update_character(combatant.linked_character);
        }
    }

    private String can_pay_cost(Combatant attacker, Attack_Option option) {
        if (option.spell_slot_cost_level > 0) {
            if (option.spell_slot_cost_level >= attacker.spell_slots_remaining.length || attacker.spell_slots_remaining[option.spell_slot_cost_level] <= 0) {
                return attacker.display_name + " 没有足够的 " + option.spell_slot_cost_level + " 环法术位。";
            }
        }
        if (option.pact_slot_cost > 0 && attacker.pact_slots_remaining < option.pact_slot_cost) {
            return attacker.display_name + " 没有足够的契约法术位。";
        }
        if (option.sorcery_point_cost > 0 && attacker.sorcery_points_remaining < option.sorcery_point_cost) {
            return attacker.display_name + " 没有足够的术法点。";
        }
        if (option.superiority_die_cost > 0 && attacker.superiority_dice_remaining < option.superiority_die_cost) {
            return attacker.display_name + " 没有足够的卓越骰。";
        }
        if (option.lay_on_hands_cost > 0 && attacker.lay_on_hands_remaining < option.lay_on_hands_cost) {
            return attacker.display_name + " 没有足够的圣疗池。";
        }
        return null;
    }

    private void consume_cost(Combatant attacker, Attack_Option option, StringBuilder log) {
        if (option.spell_slot_cost_level > 0) {
            attacker.spell_slots_remaining[option.spell_slot_cost_level]--;
            log.append("消耗 ").append(option.spell_slot_cost_level).append(" 环法术位 1 个。\n");
        }
        if (option.pact_slot_cost > 0) {
            attacker.pact_slots_remaining -= option.pact_slot_cost;
            log.append("消耗契约法术位 1 个。\n");
        }
        if (option.sorcery_point_cost > 0) {
            attacker.sorcery_points_remaining -= option.sorcery_point_cost;
            log.append("消耗术法点 ").append(option.sorcery_point_cost).append(" 点。\n");
        }
        if (option.superiority_die_cost > 0) {
            attacker.superiority_dice_remaining -= option.superiority_die_cost;
            log.append("消耗卓越骰 ").append(option.superiority_die_cost).append(" 颗。\n");
        }
        if (option.lay_on_hands_cost > 0) {
            attacker.lay_on_hands_remaining -= option.lay_on_hands_cost;
            log.append("消耗圣疗池 ").append(option.lay_on_hands_cost).append(" 点。\n");
        }
    }

    private void apply_status_if_needed(Attack_Option option, Combatant target, StringBuilder log, boolean allowApply) {
        if (!allowApply || option.applied_status_type == null) {
            return;
        }

        boolean resisted = false;
        if (option.status_save_dc > 0 && option.status_save_ability != null && !option.status_save_ability.isEmpty()) {
            int saveRoll = Dice_Util.roll_d20();
            int saveBonus = target.get_saving_throw_bonus(option.status_save_ability);
            int saveTotal = saveRoll + saveBonus;
            resisted = saveTotal >= option.status_save_dc;
            log.append(target.display_name).append(" 对状态进行 ").append(option.status_save_ability)
                    .append(" 豁免：d20=").append(saveRoll).append(" + ").append(saveBonus)
                    .append(" = ").append(saveTotal)
                    .append(resisted ? "，成功抵抗。\n" : "，失败。\n");
        }

        if (!resisted) {
            target.apply_status(option.applied_status_type, option.status_duration_rounds);
            log.append(target.display_name).append(" 陷入状态：").append(option.applied_status_type.label)
                    .append("（持续 ").append(option.status_duration_rounds).append(" 轮）。\n");
        }
    }

    private String process_start_of_turn(Combatant combatant) {
        if (combatant == null || combatant.status_effects.isEmpty()) {
            return null;
        }
        StringBuilder log = new StringBuilder();
        for (Combat_Status_Effect effect : combatant.status_effects) {
            if (effect.type.start_turn_damage_dice_count > 0) {
                int damage = Dice_Util.roll_dice(effect.type.start_turn_damage_dice_count, effect.type.start_turn_damage_dice_size);
                combatant.current_hp = Math.max(0, combatant.current_hp - damage);
                log.append(combatant.display_name).append(" 受到状态【").append(effect.type.label).append("】影响，承受 ")
                        .append(damage).append(" 点").append(effect.type.damage_type).append("伤害。\n");
            }
        }
        if (combatant.is_turn_blocked()) {
            log.append(combatant.display_name).append(" 当前无法行动，只能结束回合。\n");
        }
        return log.length() == 0 ? null : log.toString().trim();
    }

    private void decrement_statuses(Combatant combatant) {
        for (int i = combatant.status_effects.size() - 1; i >= 0; i--) {
            Combat_Status_Effect effect = combatant.status_effects.get(i);
            effect.rounds_remaining--;
            if (effect.rounds_remaining <= 0) {
                combatant.status_effects.remove(i);
            }
        }
    }
}
