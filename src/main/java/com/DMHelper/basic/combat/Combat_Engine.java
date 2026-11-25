package com.DMHelper.basic.combat;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

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

    // ---- 新增：战斗日志、专注管理、回合与统计 ----
    private final List<Combat_Log_Entry> combat_log;
    private final Concentration_Manager concentration_manager;
    private int current_round;
    private int total_damage_dealt_by_players;
    private int total_damage_dealt_by_enemies;
    private int total_healing_done;
    private Battle_Grid battle_grid;
    private boolean action_spent;
    private boolean bonus_action_spent, bonus_spell_cast, other_spell_cast, disengaged;
    private final List<Reaction_Offer> reaction_offers = new ArrayList<>();
    private final Set<Combatant> remaining_in_round = new HashSet<>();
    private java.util.function.Consumer<Character_Sheet> character_persistence = Character_DAO::update_character;
    public void defer_character_persistence() { character_persistence = character -> {}; }

    public static class SavedState {
        public List<Combatant> combatants;
        public List<Integer> characterIds, order, remaining;
        public List<Monster_Definition> monsters;
        public List<String> loot;
        public boolean finished, victorious, actionSpent, bonusSpent, bonusSpellCast, otherSpellCast, disengaged;
        public int totalXp, distributedXp, round, playerDamage, enemyDamage, healing;
        public String pendingLog;
        public List<Combat_Log_Entry> combatLog;
        public List<SavedReaction> reactions;
        public List<SavedConcentration> concentrations;
    }
    public record SavedReaction(String id, int reactor, int aggressor, Attack_Option option) {}
    public record SavedConcentration(int caster, Concentration_Manager.ConcentrationInfo info) {}
    public record Restored(Combat_Engine engine, List<Combatant> combatants, Battle_Grid grid) {}

    public SavedState snapshot(List<Combatant> all) {
        SavedState state = new SavedState();
        state.combatants = all;
        state.characterIds = all.stream().map(c -> c.linked_character == null ? -1 : c.linked_character.database_id).toList();
        state.monsters = all.stream().map(c -> c.linked_monster).toList();
        state.order = initiative_order.stream().map(all::indexOf).toList();
        state.remaining = remaining_in_round.stream().map(all::indexOf).sorted().toList();
        state.loot = new ArrayList<>(pending_loot_keys);
        state.finished=combat_finished; state.victorious=players_victorious; state.totalXp=total_encounter_xp;
        state.distributedXp=distributed_xp; state.round=current_round; state.pendingLog=pending_system_log;
        state.actionSpent=action_spent; state.bonusSpent=bonus_action_spent; state.bonusSpellCast=bonus_spell_cast;
        state.otherSpellCast=other_spell_cast; state.disengaged=disengaged;
        state.playerDamage=total_damage_dealt_by_players; state.enemyDamage=total_damage_dealt_by_enemies; state.healing=total_healing_done;
        state.combatLog = new ArrayList<>(combat_log);
        state.reactions = reaction_offers.stream().map(o -> new SavedReaction(o.id,all.indexOf(o.reactor),all.indexOf(o.aggressor),o.option)).toList();
        state.concentrations = new ArrayList<>();
        for(int i=0;i<all.size();i++) {
            var info=concentration_manager.get_concentration(all.get(i));
            if(info!=null)state.concentrations.add(new SavedConcentration(i,info));
        }
        return state;
    }

    private Combat_Engine(SavedState state, List<Combatant> all) {
        initiative_order = new ArrayList<>(state.order.stream().map(all::get).toList());
        player_combatants = new ArrayList<>(all.stream().filter(c -> c.side==Combatant.Side.PLAYER).toList());
        participating_characters = new ArrayList<>(player_combatants.stream().map(c -> c.linked_character).toList());
        encounter_monsters = new ArrayList<>(all.stream().filter(c -> c.linked_monster!=null).map(c -> c.linked_monster).toList());
        total_encounter_xp=state.totalXp; pending_loot_keys=new ArrayList<>(state.loot);
        combat_finished=state.finished; players_victorious=state.victorious; distributed_xp=state.distributedXp;
        current_round=state.round; pending_system_log=state.pendingLog;
        action_spent=state.actionSpent; bonus_action_spent=state.bonusSpent; bonus_spell_cast=state.bonusSpellCast;
        other_spell_cast=state.otherSpellCast; disengaged=state.disengaged;
        total_damage_dealt_by_players=state.playerDamage; total_damage_dealt_by_enemies=state.enemyDamage; total_healing_done=state.healing;
        combat_log=new ArrayList<>(state.combatLog); concentration_manager=new Concentration_Manager();
        for(var saved:state.concentrations) {
            Combatant caster=all.get(saved.caster); var info=saved.info;
            concentration_manager.start_concentration(caster,info.spell_name,info.spell_level);
            concentration_manager.get_concentration(caster).damage_taken=info.damage_taken;
        }
        for(var saved:state.reactions) reaction_offers.add(new Reaction_Offer(saved.id,all.get(saved.reactor),all.get(saved.aggressor),saved.option));
        state.remaining.forEach(index -> remaining_in_round.add(all.get(index)));
        defer_character_persistence();
    }
    public static Restored restore(SavedState state, Battle_Grid.SavedState gridState, List<Character_Sheet> roster) {
        if(state.combatants==null || state.combatants.size()<2 || state.combatants.size()>96
                || state.characterIds.size()!=state.combatants.size() || state.monsters.size()!=state.combatants.size())
            throw new IllegalArgumentException("战斗者存档不完整");
        List<Combatant> all=new ArrayList<>();
        for(int i=0;i<state.combatants.size();i++) {
            Combatant saved=state.combatants.get(i); int characterId=state.characterIds.get(i);
            Character_Sheet character=characterId<0?null:roster.stream().filter(c -> c.database_id==characterId).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("参战角色已被删除，无法恢复战斗"));
            if(saved.side==Combatant.Side.PLAYER && character==null)throw new IllegalArgumentException("参战角色存档缺失");
            all.add(Combatant.restore(saved,character,state.monsters.get(i)));
        }
        Combat_Engine engine=new Combat_Engine(state,all);
        Battle_Grid grid=Battle_Grid.restore(all,gridState);
        engine.battle_grid=grid; // Do not enable_grid(): it would reset the active turn and reactions.
        return new Restored(engine,all,grid);
    }

    public void enable_grid(Battle_Grid grid) {
        this.battle_grid = grid;
        this.action_spent = false;
        remaining_in_round.addAll(get_initiative_order());
        reset_grid_turn(get_active_combatant());
    }

    public boolean is_action_spent() { return action_spent; }

    public boolean is_bonus_action_spent() { return bonus_action_spent; }
    public boolean is_disengaged() { return disengaged; }
    public List<Reaction_Offer> get_reaction_offers() { return List.copyOf(reaction_offers); }
    public record Reaction_Offer(String id, Combatant reactor, Combatant aggressor, Attack_Option option) {}

    public String action_error(Attack_Option option, Combatant target) {
        String error = ability_error(option);
        if (error != null) return error;
        Combatant actor = get_active_combatant();
        if (target == null || !target.is_alive()) return "无效的目标。";
        if (!is_target_mode_valid(actor, target, option.target_mode)) return "当前技能的目标类型不匹配。";
        if (battle_grid != null && !battle_grid.in_range(actor, target, option)) return "目标超出射程、触及距离或被全掩护遮挡。";
        return null;
    }

    public void spend_action() { action_spent = true; if (battle_grid != null) battle_grid.lock_undo(); }
    public boolean can_undo_move() {
        return battle_grid != null && !combat_finished && !action_spent && !bonus_action_spent && reaction_offers.isEmpty() && battle_grid.can_undo(get_active_combatant());
    }
    public String undo_move() {
        if (!can_undo_move()) throw new IllegalArgumentException("本回合已执行动作、附赠动作或触发反应，或没有可撤销的移动。");
        battle_grid.undo_move(get_active_combatant()); return "已撤销上一次移动并恢复移动距离。";
    }
    public String tactical_action(String action) {
        Combatant actor = get_active_combatant();
        if (battle_grid == null || combat_finished || actor == null || actor.is_turn_blocked() || !reaction_offers.isEmpty()) throw new IllegalArgumentException("当前不能执行战术动作。");
        if ("STAND".equals(action)) {
            battle_grid.stand(actor); return actor.display_name + " 起身，消耗一半速度。";
        }
        if (action_spent) throw new IllegalArgumentException("本回合动作已使用。");
        if (!List.of("DISENGAGE", "DASH").contains(action)) throw new IllegalArgumentException("无效的战术动作。");
        spend_action();
        if ("DISENGAGE".equals(action)) { disengaged=true; return actor.display_name + " 使用撤离，本回合移动不触发借机攻击。"; }
        battle_grid.dash(actor); return actor.display_name + " 使用疾走，增加等于速度的移动距离。";
    }

    private void reset_grid_turn(Combatant actor) {
        action_spent=false; bonus_action_spent=false; bonus_spell_cast=false; other_spell_cast=false; disengaged=false;
        if (actor != null) actor.reaction_available=true;
        battle_grid.begin_turn(actor);
    }

    private Attack_Option melee_option(Combatant actor) {
        return actor.attack_options.stream().filter(a -> a.resolution_type == Attack_Option.Resolution_Type.ATTACK_ROLL && !a.ranged_attack && !a.spell
                && a.spell_slot_cost_level==0 && a.pact_slot_cost==0 && a.superiority_die_cost==0 && a.area_shape==Attack_Option.Area_Shape.NONE)
                .max(Comparator.comparingDouble(a -> a.damage_dice_count * (a.damage_dice_size + 1) / 2.0 + a.damage_bonus))
                .orElse(Attack_Option.attack_roll("徒手打击", "", Combatant.get_modifier(actor.strength), 1, 1, Combatant.get_modifier(actor.strength), 1, "钝击"));
    }

    public String move_active(Battle_Grid.Cell destination) {
        Combatant actor=get_active_combatant();
        if (battle_grid==null || actor==null || combat_finished || !reaction_offers.isEmpty()) throw new IllegalArgumentException("当前不能移动，请先处理反应。");
        List<Battle_Grid.Cell> path=battle_grid.path(actor,destination);
        battle_grid.record_move(actor); StringBuilder log=new StringBuilder(); int cost=0;
        for (Battle_Grid.Cell next:path) {
            if (!disengaged && !actor.status_effects.stream().anyMatch(e->e.type==Combat_Status_Type.INVISIBLE)) {
                for (Combatant enemy:List.copyOf(initiative_order)) {
                    Attack_Option melee=melee_option(enemy);
                    if (enemy.side==actor.side || !enemy.is_alive() || enemy.is_turn_blocked() || !enemy.reaction_available
                            || battle_grid.distance(enemy,actor)>melee.range_ft || battle_grid.distance_at(actor,next,enemy)<=melee.range_ft
                            || battle_grid.cover(enemy,actor)==Cover_Type.FULL) continue;
                    enemy.reaction_available=false; battle_grid.lock_undo();
                    log.append("[借机攻击] ").append(enemy.display_name).append(" -> ").append(actor.display_name).append("\n");
                    resolve_target(enemy,melee.opportunity_attack(),actor,battle_grid.cover(enemy,actor),null,log);
                    if (!actor.is_alive() || battle_grid.speed(actor)==0) break;
                }
            }
            if (!actor.is_alive() || battle_grid.speed(actor)==0 || !reaction_offers.isEmpty()) break;
            cost+=battle_grid.step(actor,next);
        }
        log.append(actor.display_name).append(" 移动消耗 ").append(cost).append(" 尺，剩余 ").append(battle_grid.remaining(actor)).append(" 尺。");
        finish_resolution(log,actor); sync_player_states(); return log.toString();
    }

    private void offer_damage_reaction(Combatant reactor, Combatant aggressor) {
        if (!reactor.is_alive() || !reactor.reaction_available || reactor.is_turn_blocked() || reaction_offers.stream().anyMatch(o->o.reactor==reactor)) return;
        for (Attack_Option option:reactor.attack_options) if (option.reacts_to_damage && can_pay_cost(reactor,option)==null
                && !(reactor==get_active_combatant() && bonus_spell_cast && option.spell) && battle_grid.in_range(reactor,aggressor,option)) {
            reaction_offers.add(new Reaction_Offer(java.util.UUID.randomUUID().toString(),reactor,aggressor,option)); return;
        }
    }

    public void offer_external_damage_reaction(Combatant reactor, Combatant aggressor, int damage) {
        if (battle_grid != null && !combat_finished && damage > 0) offer_damage_reaction(reactor, aggressor);
    }

    public String resolve_reaction(String id, boolean accept) {
        Reaction_Offer offer=reaction_offers.stream().filter(o->o.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("反应已失效。"));
        if (!accept) { reaction_offers.remove(offer); return offer.reactor.display_name+" 放弃反应。"; }
        Combatant reactor=offer.reactor;
        if (!reactor.is_alive() || !reactor.reaction_available || reactor.is_turn_blocked() || !offer.aggressor.is_alive()
                || !battle_grid.in_range(reactor,offer.aggressor,offer.option) || can_pay_cost(reactor,offer.option)!=null
                || reactor==get_active_combatant() && bonus_spell_cast && offer.option.spell) throw new IllegalArgumentException("当前无法使用此反应。");
        reaction_offers.remove(offer); reactor.reaction_available=false; battle_grid.lock_undo();
        if (reactor==get_active_combatant() && offer.option.spell) other_spell_cast=true;
        StringBuilder log=new StringBuilder("[反应] "+reactor.display_name+" 使用 "+offer.option.name+"\n");
        consume_cost(reactor,offer.option,log);
        Combatant previous=get_active_combatant();
        resolve_target(reactor,offer.option,offer.aggressor,battle_grid.cover(reactor,offer.aggressor),null,log);
        finish_resolution(log,previous);sync_player_states();return log.toString();
    }

    public Combat_Engine(List<Character_Sheet> characters, List<Monster_Definition> monsters) {
        this.initiative_order = new ArrayList<>();
        this.player_combatants = new ArrayList<>();
        this.participating_characters = new ArrayList<>(characters);
        this.encounter_monsters = new ArrayList<>(monsters);
        this.pending_loot_keys = new ArrayList<>();
        // 新增字段初始化
        this.combat_log = new ArrayList<>();
        this.concentration_manager = new Concentration_Manager();
        this.current_round = 0;
        this.total_damage_dealt_by_players = 0;
        this.total_damage_dealt_by_enemies = 0;
        this.total_healing_done = 0;
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
                .thenComparing(Comparator.comparingInt((Combatant combatant) -> combatant.dexterity).reversed())
                .thenComparing(combatant -> combatant.display_name));

        // 记录战斗开始日志
        StringBuilder startDesc = new StringBuilder("战斗开始！参战方：");
        if (!characters.isEmpty()) {
            startDesc.append("玩家方（").append(characters.size()).append("人）");
        }
        if (!monsters.isEmpty()) {
            if (!characters.isEmpty()) startDesc.append(" vs ");
            startDesc.append("敌方（").append(monsters.size()).append("只）");
        }
        add_log(Combat_Log_Entry.Log_Type.COMBAT_START, "系统", "",
                startDesc.toString(), 0, "总遭遇 XP: " + this.total_encounter_xp);
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
        return get_valid_targets(null);
    }

    public List<Combatant> get_valid_targets(Attack_Option attackOption) {
        Combatant active = get_active_combatant();
        List<Combatant> targets = new ArrayList<>();
        if (active == null) {
            return targets;
        }
        Attack_Option.Target_Mode targetMode = attackOption == null ? Attack_Option.Target_Mode.HOSTILE : attackOption.target_mode;
        if (targetMode == Attack_Option.Target_Mode.SELF) {
            targets.add(active);
            return targets;
        }
        for (Combatant combatant : this.initiative_order) {
            if (!combatant.is_alive()) {
                continue;
            }
            if (targetMode == Attack_Option.Target_Mode.HOSTILE && combatant.side != active.side) {
                targets.add(combatant);
            } else if (targetMode == Attack_Option.Target_Mode.FRIENDLY && combatant.side == active.side) {
                targets.add(combatant);
            } else if (targetMode == Attack_Option.Target_Mode.FRIENDLY_OTHER
                    && combatant.side == active.side
                    && combatant != active) {
                targets.add(combatant);
            }
        }
        if (battle_grid != null && attackOption != null) {
            targets.removeIf(target -> !battle_grid.in_range(active, target, attackOption));
        }
        return targets;
    }

    public String execute_attack(Attack_Option option, Combatant target) {
        String error = action_error(option, target);
        if (error != null) return error;
        if (battle_grid != null && option.area_shape != Attack_Option.Area_Shape.NONE) return "范围技能需要选择落点或方向。";
        return execute_targets(get_active_combatant(), option, List.of(target), null);
    }

    public String ability_error(Attack_Option option) {
        Combatant actor = get_active_combatant();
        if (combat_finished || actor == null || option == null) return "当前没有可执行的行动。";
        if (actor.is_turn_blocked()) return "当前无法行动。";
        if (battle_grid != null) {
            if (option.action_economy == Attack_Option.Action_Economy.REACTION) return "该技能需要反应触发，不能作为普通行动。";
            if (option.action_economy == Attack_Option.Action_Economy.BONUS_ACTION ? bonus_action_spent : action_spent) return "本回合对应的动作或附赠动作已使用。";
            if (option.spell && (bonus_spell_cast && !(option.action_economy == Attack_Option.Action_Economy.ACTION && option.spell_level == 0)
                    || option.action_economy == Attack_Option.Action_Economy.BONUS_ACTION && other_spell_cast)) return "本回合施放附赠动作法术时，其他法术只能是用动作施放的戏法。";
            if (!reaction_offers.isEmpty()) return "请先处理待决反应。";
        }
        return can_pay_cost(actor, option);
    }

    public String execute_area(Attack_Option option, Battle_Grid.Cell aim) {
        String error = ability_error(option);
        if (error != null) return error;
        if (battle_grid == null || option.area_shape == Attack_Option.Area_Shape.NONE) return "当前技能不是范围技能。";
        List<Combatant> targets = battle_grid.area_targets(get_active_combatant(), aim, option);
        if (targets.isEmpty()) return "范围内没有受影响的单位，未消耗行动或资源。";
        return execute_targets(get_active_combatant(), option, targets, aim);
    }

    private String execute_targets(Combatant actor, Attack_Option option, List<Combatant> targets, Battle_Grid.Cell aim) {
        StringBuilder log = new StringBuilder(actor.display_name + " 使用 " + option.name + "\n");
        consume_cost(actor, option, log);
        if (battle_grid != null) {
            if (option.action_economy == Attack_Option.Action_Economy.BONUS_ACTION) bonus_action_spent = true;
            else action_spent = true;
            if (option.spell) {
                if (option.action_economy == Attack_Option.Action_Economy.BONUS_ACTION) bonus_spell_cast = true;
                else if (option.spell_level > 0) other_spell_cast = true;
            }
            battle_grid.lock_undo();
        }
        Integer damage = aim == null ? null : Math.max(0, option.roll_damage(false));
        boolean concentration = false;
        for (Combatant target : targets) {
            Cover_Type cover = battle_grid == null ? Cover_Type.NONE : aim == null ? battle_grid.cover(actor,target) : battle_grid.area_cover(actor,aim,target,option);
            log.append("目标：").append(target.display_name).append("（").append(cover.label).append("）\n");
            concentration |= resolve_target(actor, option, target, cover, damage, log);
        }
        if (option.requires_concentration && concentration) {
            concentration_manager.start_concentration(actor, option.name, option.spell_slot_cost_level > 0 ? option.spell_slot_cost_level : actor.pact_slot_level);
            log.append(actor.display_name).append(" 开始专注【").append(option.name).append("】\n");
        }
        finish_resolution(log, actor);
        if (!combat_finished && battle_grid == null) advance_turn();
        sync_player_states();
        return log.toString().trim();
    }

    private boolean resolve_target(Combatant attacker, Attack_Option attackOption, Combatant target, Cover_Type cover, Integer sharedDamage, StringBuilder log) {
        boolean concentrationEffectApplied = false;
        int initialHp = target.current_hp;

        // 计算优势/劣势（从攻击者状态效果获取）
        boolean hasAdvantage = Advantage_Disadvantage.has_advantage_from_status(attacker.status_effects);
        boolean hasDisadvantage = Advantage_Disadvantage.has_disadvantage_from_status(attacker.status_effects);
        if (battle_grid != null && battle_grid.ranged_disadvantage(attacker, target, attackOption)) {
            hasDisadvantage = true;
            log.append("远程攻击因长射程或邻近敌人而具有劣势。\n");
        }

        if (attackOption.healing_dice_count > 0) {
            int healing = Math.max(0, attackOption.roll_healing());
            int beforeHp = target.current_hp;
            target.current_hp = Math.min(target.max_hp, target.current_hp + healing);
            int actualHealing = target.current_hp - beforeHp;
            log.append("恢复 ").append(actualHealing).append(" 点生命值，目标当前 HP ")
                    .append(target.current_hp).append("/").append(target.max_hp).append("\n");
            apply_status_if_needed(attackOption, target, log, true);
            // 累计治疗统计
            this.total_healing_done += actualHealing;
            add_log(Combat_Log_Entry.Log_Type.HEALING_DONE, attacker.display_name, target.display_name,
                    attacker.display_name + " 使用 " + attackOption.name + " 恢复了 " + actualHealing + " 点生命值",
                    actualHealing, "目标当前 HP: " + target.current_hp + "/" + target.max_hp);
        } else if (attackOption.resolution_type == Attack_Option.Resolution_Type.ATTACK_ROLL) {
            for (int i = 1; i <= attackOption.attack_count; i++) {
                // 使用优势/劣势系统掷骰
                int d20 = Advantage_Disadvantage.resolve_d20(hasAdvantage, hasDisadvantage);
                String rollLabel = Advantage_Disadvantage.resolve_d20_label(hasAdvantage, hasDisadvantage);
                int totalAttack = d20 + attackOption.attack_bonus + attacker.get_effective_attack_modifier();
                boolean critical = d20 == 20;
                boolean hit = critical || (d20 != 1 && totalAttack >= (target.get_effective_armor_class() + cover.ac_bonus));

                // 记录攻击掷骰日志
                Combat_Log_Entry.Log_Type rollLogType = hasAdvantage && !hasDisadvantage
                        ? Combat_Log_Entry.Log_Type.ADVANTAGE_ROLL
                        : (hasDisadvantage && !hasAdvantage
                                ? Combat_Log_Entry.Log_Type.DISADVANTAGE_ROLL
                                : Combat_Log_Entry.Log_Type.ATTACK_ROLL);
                add_log(rollLogType, attacker.display_name, target.display_name,
                        "第 " + i + " 次攻击：" + rollLabel + " + "
                                + (attackOption.attack_bonus + attacker.get_effective_attack_modifier())
                                + " = " + totalAttack + (hit ? "，命中" : "，未命中"),
                        totalAttack, hit ? "命中" : "未命中");

                log.append("第 ").append(i).append(" 次攻击：").append(rollLabel)
                        .append(" + ").append(attackOption.attack_bonus + attacker.get_effective_attack_modifier())
                        .append(" = ").append(totalAttack)
                        .append(hit ? "，命中" : "，未命中")
                        .append("\n");
                if (hit) {
                    concentrationEffectApplied = true;
                    int damage = Math.max(0, attackOption.roll_damage(critical));
                    target.current_hp = Math.max(0, target.current_hp - damage);
                    log.append("造成 ").append(damage).append(" 点").append(attackOption.damage_type)
                            .append(critical ? "（重击）" : "")
                            .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");
                    apply_status_if_needed(attackOption, target, log, true);

                    // 累计伤害统计
                    if (attacker.side == Combatant.Side.PLAYER) {
                        this.total_damage_dealt_by_players += damage;
                    } else {
                        this.total_damage_dealt_by_enemies += damage;
                    }
                    add_log(Combat_Log_Entry.Log_Type.DAMAGE_DEALT, attacker.display_name, target.display_name,
                            attacker.display_name + " 造成 " + damage + " 点" + attackOption.damage_type
                                    + (critical ? "（重击）" : "") + "伤害",
                            damage, "目标剩余 HP: " + target.current_hp + "/" + target.max_hp);

                    // 受到伤害时检查专注
                    if (damage > 0 && concentration_manager.has_concentration(target)) {
                        String concResult = concentration_manager.check_concentration(target, damage);
                        log.append(concResult).append("\n");
                        add_log(Combat_Log_Entry.Log_Type.CONCENTRATION_CHECK, target.display_name, "",
                                concResult, damage, "受到伤害触发专注检定");
                    }
                }
                if (!target.is_alive()) {
                    log.append(target.display_name).append(" 倒下了。\n");
                    add_log(Combat_Log_Entry.Log_Type.DEATH, attacker.display_name, target.display_name,
                            target.display_name + " 被 " + attacker.display_name + " 击倒", 0,
                            "使用 " + attackOption.name);
                    break;
                }
            }
        } else if (attackOption.resolution_type == Attack_Option.Resolution_Type.SAVE_DC) {
            int saveRoll = Dice_Util.roll_d20();
            int saveBonus = target.get_saving_throw_bonus(attackOption.save_ability) + ("Dexterity".equals(attackOption.save_ability) ? cover.dexterity_save_bonus : 0);
            int saveTotal = saveRoll + saveBonus;
            int damage = sharedDamage == null ? Math.max(0, attackOption.roll_damage(false)) : sharedDamage;
            boolean success = saveTotal >= attackOption.save_dc;
            int appliedDamage = success && attackOption.half_damage_on_save ? damage / 2 : (success ? 0 : damage);
            target.current_hp = Math.max(0, target.current_hp - appliedDamage);
            log.append(target.display_name).append(" 进行 ").append(attackOption.save_ability).append(" 豁免：d20=")
                    .append(saveRoll).append(" + ").append(saveBonus).append(" = ").append(saveTotal)
                    .append(success ? "，成功" : "，失败").append("\n");
            log.append("造成 ").append(appliedDamage).append(" 点").append(attackOption.damage_type)
                    .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");

            // 记录豁免日志
            add_log(Combat_Log_Entry.Log_Type.SAVE_THROW, target.display_name, attacker.display_name,
                    target.display_name + " 进行 " + attackOption.save_ability + " 豁免：d20=" + saveRoll
                            + " + " + saveBonus + " = " + saveTotal + (success ? "，成功" : "，失败"),
                    saveTotal, success ? "豁免成功" : "豁免失败");

            if (appliedDamage > 0) {
                if (attacker.side == Combatant.Side.PLAYER) {
                    this.total_damage_dealt_by_players += appliedDamage;
                } else {
                    this.total_damage_dealt_by_enemies += appliedDamage;
                }
                add_log(Combat_Log_Entry.Log_Type.DAMAGE_DEALT, attacker.display_name, target.display_name,
                        attacker.display_name + " 造成 " + appliedDamage + " 点" + attackOption.damage_type + "伤害",
                        appliedDamage, "目标剩余 HP: " + target.current_hp + "/" + target.max_hp);

                // 受到伤害时检查专注
                if (appliedDamage > 0 && concentration_manager.has_concentration(target)) {
                    String concResult = concentration_manager.check_concentration(target, appliedDamage);
                    log.append(concResult).append("\n");
                    add_log(Combat_Log_Entry.Log_Type.CONCENTRATION_CHECK, target.display_name, "",
                            concResult, appliedDamage, "受到伤害触发专注检定");
                }
            }

            if (!success) {
                concentrationEffectApplied = true;
                apply_status_if_needed(attackOption, target, log, true);
            }
            if (!target.is_alive()) {
                log.append(target.display_name).append(" 倒下了。\n");
                add_log(Combat_Log_Entry.Log_Type.DEATH, attacker.display_name, target.display_name,
                        target.display_name + " 被 " + attacker.display_name + " 击倒", 0,
                        "使用 " + attackOption.name);
            }
        } else {
            concentrationEffectApplied = true;
            int damage = sharedDamage == null ? Math.max(0, attackOption.roll_damage(false)) : sharedDamage;
            if (damage > 0) {
                target.current_hp = Math.max(0, target.current_hp - damage);
                log.append("自动命中，造成 ").append(damage).append(" 点").append(attackOption.damage_type)
                        .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");

                // 累计伤害统计
                if (attacker.side == Combatant.Side.PLAYER) {
                    this.total_damage_dealt_by_players += damage;
                } else {
                    this.total_damage_dealt_by_enemies += damage;
                }
                add_log(Combat_Log_Entry.Log_Type.DAMAGE_DEALT, attacker.display_name, target.display_name,
                        attacker.display_name + " 造成 " + damage + " 点" + attackOption.damage_type + "伤害（自动命中）",
                        damage, "目标剩余 HP: " + target.current_hp + "/" + target.max_hp);

                // 受到伤害时检查专注
                if (concentration_manager.has_concentration(target)) {
                    String concResult = concentration_manager.check_concentration(target, damage);
                    log.append(concResult).append("\n");
                    add_log(Combat_Log_Entry.Log_Type.CONCENTRATION_CHECK, target.display_name, "",
                            concResult, damage, "受到伤害触发专注检定");
                }
            } else {
                log.append("效果直接生效。\n");
            }
            apply_status_if_needed(attackOption, target, log, true);
            if (!target.is_alive()) {
                log.append(target.display_name).append(" 倒下了。\n");
                add_log(Combat_Log_Entry.Log_Type.DEATH, attacker.display_name, target.display_name,
                        target.display_name + " 被 " + attacker.display_name + " 击倒", 0,
                        "使用 " + attackOption.name);
            }
        }

        if (battle_grid != null && target.current_hp < initialHp) offer_damage_reaction(target, attacker);
        return concentrationEffectApplied;
    }

    private void finish_resolution(StringBuilder log, Combatant previousActor) {
        cleanup_dead();
        if (is_side_defeated(Combatant.Side.ENEMY) && !is_side_defeated(Combatant.Side.PLAYER)) {
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
            add_log(Combat_Log_Entry.Log_Type.XP_DISTRIBUTED, "系统", "",
                    "战斗胜利！每位参战角色获得 " + this.distributed_xp + " 经验值",
                    this.distributed_xp, "总遭遇 XP: " + this.total_encounter_xp);
            add_log(Combat_Log_Entry.Log_Type.COMBAT_END, "系统", "",
                    "战斗结束 — 玩家胜利", 0, "总回合数: " + this.current_round);
        } else if (is_side_defeated(Combatant.Side.PLAYER)) {
            this.combat_finished = true;
            log.append("战斗失败，所有参战角色都倒下了。\n");
            add_log(Combat_Log_Entry.Log_Type.COMBAT_END, "系统", "",
                    "战斗结束 — 玩家失败", 0, "总回合数: " + this.current_round);
        }

        reaction_offers.removeIf(offer -> !offer.reactor.is_alive() || !offer.aggressor.is_alive());
        if (battle_grid != null && !combat_finished && previousActor != null && !previousActor.is_alive()) {
            complete_grid_turn(previousActor);
            begin_grid_turn();
        }
    }

    public String skip_turn() {
        if (!reaction_offers.isEmpty()) return "请先处理待决反应。";
        Combatant active = get_active_combatant();
        if (active == null) {
            return "当前没有可跳过的回合。";
        }
        String message = active.display_name + (active.is_turn_blocked() ? " 因状态影响无法行动，回合结束。" : " 结束了本回合。");
        // 添加跳过回合日志
        add_log(Combat_Log_Entry.Log_Type.TURN_SKIPPED, active.display_name, "",
                message, 0, active.is_turn_blocked() ? "因状态效果无法行动" : "主动跳过");
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

        if (battle_grid == null) {
            this.current_round++;
            add_log(Combat_Log_Entry.Log_Type.ROUND_START, "系统", "", "第 " + this.current_round + " 回合开始", this.current_round,
                    "剩余战斗者: " + this.initiative_order.size() + " 人");
            cleanup_dead();
            Combatant next=get_active_combatant();
            if(next!=null)this.pending_system_log=process_start_of_turn(next);
        } else {
            complete_grid_turn(first);
            begin_grid_turn();
        }
    }

    private void complete_grid_turn(Combatant actor) {
        remaining_in_round.remove(actor);
        remaining_in_round.retainAll(this.initiative_order);
        if (!this.initiative_order.isEmpty() && remaining_in_round.isEmpty()) {
            this.current_round++;
            remaining_in_round.addAll(this.initiative_order);
            add_log(Combat_Log_Entry.Log_Type.ROUND_START,"系统","","第 "+(this.current_round+1)+" 轮开始",this.current_round+1,"");
        }
    }

    private void begin_grid_turn() {
        StringBuilder log=new StringBuilder();
        while(!combat_finished) {
            Combatant actor=get_active_combatant();
            if(actor==null)break;
            reset_grid_turn(actor);
            String effects=process_start_of_turn(actor);
            if(effects!=null)log.append(effects).append("\n");
            handle_post_external_resolution(log);
            if(actor.is_alive())break;
            complete_grid_turn(actor);
        }
        if(log.length()>0)this.pending_system_log=(this.pending_system_log==null?"":this.pending_system_log+"\n")+log.toString().trim();
    }

    private void cleanup_dead() {
        for (Combatant combatant : this.initiative_order) {
            if (!combatant.is_alive()) {
                concentration_manager.end_concentration(combatant);
            }
        }
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
            character_persistence.accept(character);
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
            character_persistence.accept(receiver);
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
        String source = sourceLabel == null || sourceLabel.trim().isEmpty() ? "外部效果" : sourceLabel;
        log.append(source).append(" -> ").append(target.display_name).append("\n");
        target.current_hp = Math.max(0, target.current_hp - damage);
        log.append("造成 ").append(damage).append(" 点").append(damageType == null || damageType.trim().isEmpty() ? "伤害" : damageType)
                .append("伤害，目标剩余 HP ").append(target.current_hp).append("/").append(target.max_hp).append("\n");
        if (note != null && !note.trim().isEmpty()) {
            log.append("备注：").append(note.trim()).append("\n");
        }

        // 添加外部伤害日志
        add_log(Combat_Log_Entry.Log_Type.EXTERNAL_EFFECT, source, target.display_name,
                source + " 造成 " + damage + " 点" + (damageType == null || damageType.trim().isEmpty() ? "伤害" : damageType) + "伤害",
                damage, "目标剩余 HP: " + target.current_hp + "/" + target.max_hp);

        // 外部伤害也要检查专注
        if (damage > 0 && concentration_manager.has_concentration(target)) {
            String concResult = concentration_manager.check_concentration(target, damage);
            log.append(concResult).append("\n");
            add_log(Combat_Log_Entry.Log_Type.CONCENTRATION_CHECK, target.display_name, "",
                    concResult, damage, "外部伤害触发专注检定");
        }

        if (!target.is_alive()) {
            log.append(target.display_name).append(" 倒下了。\n");
            add_log(Combat_Log_Entry.Log_Type.DEATH, source, target.display_name,
                    target.display_name + " 因外部效果倒下", 0, source + " 造成 " + damage + " 点伤害");
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
        String source = sourceLabel == null || sourceLabel.trim().isEmpty() ? "外部治疗" : sourceLabel;
        log.append(source).append(" -> ").append(target.display_name).append("\n");
        int beforeHp = target.current_hp;
        target.current_hp = Math.min(target.max_hp, target.current_hp + amount);
        int healed = target.current_hp - beforeHp;
        log.append("恢复 ").append(healed).append(" 点生命值，目标当前 HP ")
                .append(target.current_hp).append("/").append(target.max_hp).append("\n");
        if (note != null && !note.trim().isEmpty()) {
            log.append("备注：").append(note.trim()).append("\n");
        }

        // 累计治疗统计
        this.total_healing_done += healed;
        // 添加外部治疗日志
        add_log(Combat_Log_Entry.Log_Type.HEALING_DONE, source, target.display_name,
                source + " 恢复了 " + healed + " 点生命值",
                healed, "目标当前 HP: " + target.current_hp + "/" + target.max_hp);

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
            character_persistence.accept(combatant.linked_character);
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
        if (option.bardic_inspiration_cost > 0 && attacker.bardic_inspiration_remaining < option.bardic_inspiration_cost) {
            return attacker.display_name + " 没有足够的吟游激励次数。";
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
        if (option.bardic_inspiration_cost > 0) {
            attacker.bardic_inspiration_remaining -= option.bardic_inspiration_cost;
            log.append("消耗吟游激励 ").append(option.bardic_inspiration_cost).append(" 次。\n");
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

    private boolean is_target_mode_valid(Combatant attacker, Combatant target, Attack_Option.Target_Mode targetMode) {
        if (attacker == null || target == null) {
            return false;
        }
        if (targetMode == Attack_Option.Target_Mode.SELF) {
            return attacker == target;
        }
        if (targetMode == Attack_Option.Target_Mode.FRIENDLY) {
            return attacker.side == target.side;
        }
        if (targetMode == Attack_Option.Target_Mode.FRIENDLY_OTHER) {
            return attacker.side == target.side && attacker != target;
        }
        return attacker.side != target.side;
    }

    // ------------------------------------------------------------------ //
    //  新增：战斗日志、专注管理、统计查询方法
    // ------------------------------------------------------------------ //

    /**
     * 返回完整战斗日志（只读副本）。
     */
    public List<Combat_Log_Entry> get_combat_log() {
        return new ArrayList<>(this.combat_log);
    }

    /**
     * 返回当前回合数。
     */
    public int get_current_round() {
        return this.current_round;
    }

    /**
     * 返回专注管理器实例。
     */
    public Concentration_Manager get_concentration_manager() {
        return this.concentration_manager;
    }

    /**
     * 返回玩家方累计造成的总伤害。
     */
    public int get_total_damage_by_players() {
        return this.total_damage_dealt_by_players;
    }

    /**
     * 返回敌方累计造成的总伤害。
     */
    public int get_total_damage_by_enemies() {
        return this.total_damage_dealt_by_enemies;
    }

    /**
     * 返回累计总治疗量。
     */
    public int get_total_healing() {
        return this.total_healing_done;
    }

    /**
     * 添加战斗日志条目的辅助方法。
     *
     * @param type        日志类型
     * @param actor       行动者名称
     * @param target      目标名称
     * @param description 描述文本
     * @param numericValue 数值（伤害、治疗、掷骰结果等）
     * @param detail      额外详细信息
     */
    private void add_log(Combat_Log_Entry.Log_Type type, String actor, String target,
                         String description, int numericValue, String detail) {
        this.combat_log.add(new Combat_Log_Entry(
                type, this.current_round, actor, target,
                description, numericValue, detail, null));
    }
}
