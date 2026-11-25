package com.DMHelper.api;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.combat.Attack_Option;
import com.DMHelper.basic.combat.Battle_Grid;
import com.DMHelper.basic.combat.Advantage_Disadvantage;
import com.DMHelper.basic.combat.Combat_Engine;
import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.combat.Dice_Util;
import com.DMHelper.basic.combat.Monster_Definition;
import com.DMHelper.basic.combat.Monster_Library;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Combat_Save_DAO;
import com.DMHelper.basic.database.Adventure_Session_DAO;
import com.DMHelper.basic.database.Adventure_Atlas_DAO;
import com.DMHelper.basic.database.Adventure_Log_DAO;
import com.DMHelper.basic.journal.Adventure_Log_Entry;
import com.DMHelper.basic.combat.Combat_Checkpoint;
import java.sql.SQLException;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** HTTP adapter for the existing desktop combat engine. */
public final class CombatController {
    private static final Map<String, CombatSession> SESSIONS = new ConcurrentHashMap<>();

    private CombatController() {}

    public static class StartRequest {
        public List<Integer> characterIds;
        public List<String> monsterKeys;
        public Integer adventureSessionId;
        public Integer locationId;
    }

    public static class ActionRequest {
        public int optionIndex;
        public String targetId;
        public String itemKey;
        public Integer aimX;
        public Integer aimY;
        public String combatantId;
    }

    public static class LootRequest {
        public int receiverCharacterId;
        public List<String> lootKeys;
    }

    public static class GridRequest {
        public String combatantId;
        public Integer x;
        public Integer y;
        public Integer speedFt;
        public boolean obstacle;
        public String terrain;
        public Integer footprint;
    }

    public static class TacticalRequest { public String action; public String combatantId; }
    public static class ReactionRequest { public String offerId; public boolean accept; }

    public static synchronized void getSetup(ApiExchange ctx) {
        JsonObject result = new JsonObject();
        JsonArray characters = new JsonArray();
        for (Character_Sheet character : Global_Data.character_pool) {
            JsonObject item = new JsonObject();
            item.addProperty("id", character.database_id);
            item.addProperty("name", character.name);
            item.addProperty("className", character.job.class_name);
            item.addProperty("level", character.job.current_level);
            item.addProperty("currentHp", character.current_hp);
            item.addProperty("maxHp", character.hp);
            item.addProperty("armorClass", character.ac);
            item.addProperty("initiativeModifier", character.get_initiative_modifier());
            item.addProperty("available", character.is_alive());
            characters.add(item);
        }
        JsonArray monsters = new JsonArray();
        for (Monster_Definition monster : Monster_Library.get_all_monsters()) {
            JsonObject item = ApiServer.GSON.toJsonTree(monster).getAsJsonObject();
            item.addProperty("fullName", monster.get_full_name());
            int minHp = monster.hit_dice_count + monster.hit_point_bonus;
            int maxHp = monster.hit_dice_count * monster.hit_dice_size + monster.hit_point_bonus;
            item.addProperty("hpRange", Math.max(1, minHp) + "–" + Math.max(1, maxHp));
            monsters.add(item);
        }
        result.add("characters", characters);
        result.add("monsters", monsters);
        try {
            result.add("adventureSessions", ApiServer.GSON.toJsonTree(Adventure_Session_DAO.list()));
            result.add("locations", ApiServer.GSON.toJsonTree(Adventure_Atlas_DAO.locations()));
        } catch (SQLException error) { storageError(ctx); return; }
        respondJson(ctx, result);
    }

    public static synchronized void start(ApiExchange ctx) {
        try {
            if (Combat_Save_DAO.load()!=null) { ctx.status(409).result("已有已保存的遭遇，请继续战斗或先清除当前遭遇。"); return; }
        } catch (SQLException error) { storageError(ctx); return; }
        StartRequest request = ApiServer.GSON.fromJson(ctx.body(), StartRequest.class);
        if (request == null || request.characterIds == null || request.characterIds.isEmpty()
                || request.monsterKeys == null || request.monsterKeys.isEmpty()) {
            ctx.status(400).result("请至少选择一名角色和一个敌人");
            return;
        }

        List<Character_Sheet> characters = new ArrayList<>();
        if (request.characterIds.size() > 48 || request.monsterKeys.size() > 48) {
            ctx.status(400).result("每个阵营最多选择 48 个棋子。");
            return;
        }
        for (Integer id : request.characterIds) {
            Character_Sheet character = Global_Data.character_pool.stream()
                    .filter(candidate -> candidate.database_id == id).findFirst().orElse(null);
            if (character == null || !character.is_alive()) {
                ctx.status(400).result("参战角色不存在或已经倒下");
                return;
            }
            if (!characters.contains(character)) characters.add(character);
        }

        List<Monster_Definition> catalog = Monster_Library.get_all_monsters();
        List<Monster_Definition> monsters = new ArrayList<>();
        for (String key : request.monsterKeys) {
            Monster_Definition monster = catalog.stream().filter(candidate -> candidate.key.equals(key)).findFirst().orElse(null);
            if (monster == null) {
                ctx.status(400).result("敌人不存在：" + key);
                return;
            }
            monsters.add(monster);
        }

        CombatSession session;
        try { session = new CombatSession(characters, monsters); }
        catch (IllegalArgumentException error) { ctx.status(400).result(error.getMessage()); return; }
        try {
            JsonObject body = ApiServer.GSON.fromJson(ctx.body(), JsonObject.class);
            session.adventureSessionId = body.has("adventureSessionId") ? request.adventureSessionId : Adventure_Session_DAO.current_id();
            session.locationId = body.has("locationId") ? request.locationId : Adventure_Atlas_DAO.locations().stream().filter(item -> item.is_current).map(item -> item.id).findFirst().orElse(null);
            if (session.adventureSessionId != null && Adventure_Session_DAO.get(session.adventureSessionId) == null) { ctx.status(400).result("关联场次不存在。"); return; }
            if (session.locationId != null && Adventure_Atlas_DAO.location(session.locationId) == null) { ctx.status(400).result("关联地点不存在。"); return; }
        } catch (SQLException error) { storageError(ctx); return; }
        SESSIONS.put(session.id, session);
        persistResponse(ctx, session, "先攻已生成，请摆放棋子和障碍后开始战斗。");
    }

    public static synchronized void getCurrent(ApiExchange ctx) {
        try {
            var saved=Combat_Save_DAO.load();
            if(saved==null) { ctx.status(204); return; }
            respondJson(ctx, restore(saved).toJson(null));
        } catch (SQLException error) { storageError(ctx); }
        catch (RuntimeException error) { ctx.status(409).result("无法恢复战斗：存档损坏、版本不兼容或参战角色已删除。可清除当前战斗存档后重新开始。"); }
    }
    public static synchronized void clearCurrent(ApiExchange ctx) {
        try {
            var saved=Combat_Save_DAO.load();
            if(saved!=null) Combat_Save_DAO.delete(saved.id());
            SESSIONS.clear();
            ctx.status(204);
        } catch (SQLException error) { storageError(ctx); }
    }

    private static CombatSession restore(Combat_Save_DAO.Saved saved) {
        CombatSession cached=SESSIONS.get(saved.id());
        if(cached!=null)return cached;
        var data=Combat_Checkpoint.decode(saved.snapshot());
        if(!saved.id().equals(data.id()))throw new IllegalArgumentException("存档标识不匹配");
        if(data.engine().characterIds.stream().anyMatch(id -> id>0 && Global_Data.character_pool.stream().noneMatch(c -> c.database_id==id)))
            Character_DAO.load_all_characters();
        CombatSession session=new CombatSession(data,saved.savedAt());
        session.adventureSessionId = saved.adventureSessionId();
        session.locationId = saved.locationId();
        SESSIONS.clear(); SESSIONS.put(session.id,session);
        return session;
    }
    private static void storageError(ApiExchange ctx) { ctx.status(500).result("战斗存档读写失败，请检查本机存储空间或数据库权限。"); }
    private static void persistResponse(ApiExchange ctx, CombatSession session, String log) {
        try {
            JsonObject view=session.toJson(log);
            String snapshot=Combat_Checkpoint.encode(new Combat_Checkpoint.Saved(1,session.id,session.started,
                    session.logs,session.engine.snapshot(session.combatants),session.grid.snapshot()));
            session.savedAt=Combat_Save_DAO.save(session.id,snapshot,session.engine.get_participating_characters(),session.adventureSessionId,session.locationId);
            view.addProperty("savedAt",session.savedAt);
            respondJson(ctx,view);
        } catch (SQLException | RuntimeException error) {
            // The failed request must not leave an unsaved turn or inventory in the live cache.
            SESSIONS.clear(); Character_DAO.load_all_characters();
            ctx.status(500).result("本次操作未能保存，已回退到上次成功存档。请重新载入战斗后重试。");
        }
    }

    public static synchronized void getState(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session != null) synchronized (session) { respondJson(ctx, session.toJson(null)); }
    }

    public static synchronized void deploy(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        GridRequest request = ApiServer.GSON.fromJson(ctx.body(), GridRequest.class);
        synchronized (session) {
            if (session.started) { ctx.status(409).result("战斗已开始，不能再修改摆位或障碍。"); return; }
            if (request == null || request.x == null || request.y == null) { ctx.status(400).result("缺少地图坐标。"); return; }
            try {
                Battle_Grid.Cell cell = new Battle_Grid.Cell(request.x, request.y);
                if (request.obstacle) session.grid.toggle_terrain(cell, request.terrain == null ? Battle_Grid.Terrain.WALL : Battle_Grid.Terrain.valueOf(request.terrain));
                else session.grid.place(session.combatantById(request.combatantId), cell, request.speedFt, request.footprint);
                persistResponse(ctx, session, null);
            } catch (IllegalArgumentException error) { ctx.status(400).result(error.getMessage()); }
        }
    }

    public static synchronized void begin(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        synchronized (session) {
            if (session.started) { ctx.status(409).result("战斗已开始。"); return; }
            session.started = true;
            String log = "摆位完成，战斗开始。";
            session.record(log);
            persistResponse(ctx, session, log);
        }
    }

    public static synchronized void move(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        GridRequest request = ApiServer.GSON.fromJson(ctx.body(), GridRequest.class);
        synchronized (session) {
            if (!session.started || session.engine.is_combat_finished()) { ctx.status(409).result("当前不能移动。"); return; }
            Combatant actor = session.engine.get_active_combatant();
            if (request == null || request.x == null || request.y == null || actor == null
                    || !session.ids.get(actor).equals(request.combatantId)) { ctx.status(400).result("只能移动当前行动者。"); return; }
            try {
                String log = session.engine.move_active(new Battle_Grid.Cell(request.x, request.y));
                session.record(log);
                persistResponse(ctx, session, log);
            } catch (IllegalArgumentException error) { ctx.status(400).result(error.getMessage()); }
        }
    }

    private static boolean canControl(ApiExchange ctx, CombatSession session, String actorId) {
        Combatant actor=session.engine.get_active_combatant();
        if (!session.started || session.engine.is_combat_finished() || actor==null) { ctx.status(409).result("当前不能执行此操作。"); return false; }
        if (actorId!=null && !actorId.equals(session.ids.get(actor))) { ctx.status(409).result("行动者已变化，请刷新战场。"); return false; }
        return true;
    }

    public static synchronized void undoMove(ApiExchange ctx) {
        CombatSession session=find(ctx); if(session==null)return;
        GridRequest request=ApiServer.GSON.fromJson(ctx.body(),GridRequest.class);
        synchronized(session) {
            if (!canControl(ctx,session,request==null?null:request.combatantId))return;
            try { String log=session.engine.undo_move();session.record(log);persistResponse(ctx,session,log); }
            catch(IllegalArgumentException error){ctx.status(400).result(error.getMessage());}
        }
    }
    public static synchronized void tactical(ApiExchange ctx) {
        CombatSession session=find(ctx);if(session==null)return;
        TacticalRequest request=ApiServer.GSON.fromJson(ctx.body(),TacticalRequest.class);
        synchronized(session) {
            if(request==null){ctx.status(400).result("缺少战术动作。");return;}
            if(!canControl(ctx,session,request.combatantId))return;
            try { String log=session.engine.tactical_action(request.action);session.record(log);persistResponse(ctx,session,log); }
            catch(IllegalArgumentException error){ctx.status(400).result(error.getMessage());}
        }
    }
    public static synchronized void reaction(ApiExchange ctx) {
        CombatSession session=find(ctx);if(session==null)return;
        ReactionRequest request=ApiServer.GSON.fromJson(ctx.body(),ReactionRequest.class);
        synchronized(session) {
            if(!session.started||session.engine.is_combat_finished()||request==null){ctx.status(409).result("当前没有待决反应。");return;}
            try { String log=session.engine.resolve_reaction(request.offerId,request.accept);session.record(log);persistResponse(ctx,session,log); }
            catch(IllegalArgumentException error){ctx.status(400).result(error.getMessage());}
        }
    }
    public static synchronized void previewArea(ApiExchange ctx) {
        CombatSession session=find(ctx);if(session==null)return;
        ActionRequest request=ApiServer.GSON.fromJson(ctx.body(),ActionRequest.class);
        synchronized(session) {
            if(!canControl(ctx,session,request==null?null:request.combatantId))return;
            Combatant actor=session.engine.get_active_combatant();
            if(request==null||request.optionIndex<0||request.optionIndex>=actor.attack_options.size()||request.aimX==null||request.aimY==null){ctx.status(400).result("请选择范围技能和落点。");return;}
            try { respondJson(ctx,session.areaPreview(actor.attack_options.get(request.optionIndex),new Battle_Grid.Cell(request.aimX,request.aimY))); }
            catch(IllegalArgumentException error){ctx.status(400).result(error.getMessage());}
        }
    }

    public static synchronized void act(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        ActionRequest request = ApiServer.GSON.fromJson(ctx.body(), ActionRequest.class);
        if (request == null) {
            ctx.status(400).result("缺少行动参数");
            return;
        }
        synchronized (session) {
            if (!session.started) { ctx.status(409).result("请先完成摆位并开始战斗。"); return; }
            if (session.engine.is_combat_finished()) {
                ctx.status(409).result("战斗已经结束");
                return;
            }
            if (!canControl(ctx,session,request.combatantId)) return;
            Combatant actor = session.engine.get_active_combatant();
            if (request.itemKey != null && !request.itemKey.isBlank()) {
                resolveThrownItem(ctx, session, actor, request);
                return;
            }
            if (actor == null || request.optionIndex < 0 || request.optionIndex >= actor.attack_options.size()) {
                ctx.status(400).result("无效的行动");
                return;
            }
            Attack_Option option = actor.attack_options.get(request.optionIndex);
            String abilityError=session.engine.ability_error(option);
            if(abilityError!=null){ctx.status(400).result(abilityError);return;}
            String log;
            try {
                if(option.area_shape!=Attack_Option.Area_Shape.NONE) {
                    if(request.aimX==null||request.aimY==null){ctx.status(400).result("范围技能需要选择落点或方向。");return;}
                    Battle_Grid.Cell aim=new Battle_Grid.Cell(request.aimX,request.aimY);
                    if(session.grid.area_targets(actor,aim,option).isEmpty()){ctx.status(400).result("范围内没有受影响的单位。");return;}
                    log=session.engine.execute_area(option,aim);
                } else {
                    Combatant target=session.combatantById(request.targetId);
                    if(target==null||!session.engine.get_valid_targets(option).contains(target)){ctx.status(400).result("目标超出射程、被遮挡或目标类型不符。");return;}
                    String error=session.engine.action_error(option,target);
                    if(error!=null){ctx.status(400).result(error);return;}
                    log=session.engine.execute_attack(option,target);
                }
            } catch(IllegalArgumentException error){ctx.status(400).result(error.getMessage());return;}
            session.record(log);
            persistResponse(ctx, session, log);
        }
    }

    private static void resolveThrownItem(ApiExchange ctx, CombatSession session, Combatant actor, ActionRequest request) {
        if (!session.engine.get_reaction_offers().isEmpty()) { ctx.status(409).result("请先处理待决反应。"); return; }
        if (session.engine.is_action_spent()) { ctx.status(409).result("本回合行动已使用。"); return; }
        if (actor == null || actor.linked_character == null || actor.is_turn_blocked()) {
            ctx.status(400).result("当前行动者不能投掷背包物品");
            return;
        }
        Equipment_Item item = Equipment_Library.get_item(request.itemKey);
        if (item == null || !item.is_bomb_item() || actor.linked_character.get_item_count(item.key) <= 0) {
            ctx.status(400).result("背包中没有这个可投掷物");
            return;
        }
        Combatant target = session.combatantById(request.targetId);
        if (target == null || !session.engine.get_valid_targets().contains(target) || session.grid.distance(actor, target) > 20 || session.grid.cover(actor,target)==com.DMHelper.basic.combat.Cover_Type.FULL) {
            ctx.status(400).result("无效的投掷目标");
            return;
        }
        int damage = Dice_Util.roll_dice(item.get_bomb_dice_count(), item.get_bomb_die_size()) + item.get_bomb_bonus();
        String log = session.engine.apply_external_damage(item.display_name, target, damage, item.get_bomb_damage_type(), "战斗中投掷");
        actor.linked_character.remove_item_from_inventory(item.key);
        actor.linked_character.record_advancement("战斗中投掷 " + item.display_name + "，对 " + target.display_name
                + " 造成 " + damage + " 点" + item.get_bomb_damage_type() + "伤害");
        session.engine.spend_action();
        session.engine.offer_external_damage_reaction(target, actor, damage);
        session.record(log);
        persistResponse(ctx, session, log);
    }

    public static synchronized void skip(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        GridRequest request=ApiServer.GSON.fromJson(ctx.body(),GridRequest.class);
        synchronized (session) {
            if(!canControl(ctx,session,request==null?null:request.combatantId))return;
            if (!session.started) { ctx.status(409).result("请先完成摆位并开始战斗。"); return; }
            if (session.engine.is_combat_finished()) {
                ctx.status(409).result("战斗已经结束");
                return;
            }
            if (!session.engine.get_reaction_offers().isEmpty()) { ctx.status(409).result("请先处理待决反应。"); return; }
            String log = session.engine.skip_turn();
            session.record(log);
            persistResponse(ctx, session, log);
        }
    }

    public static synchronized void assignLoot(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        LootRequest request = ApiServer.GSON.fromJson(ctx.body(), LootRequest.class);
        if (!session.engine.is_combat_finished() || !session.engine.did_players_win()) {
            ctx.status(409).result("只有战斗胜利后才能分配战利品");
            return;
        }
        Character_Sheet receiver = session.engine.get_participating_characters().stream()
                .filter(character -> character.database_id == (request == null ? -1 : request.receiverCharacterId))
                .findFirst().orElse(null);
        if (receiver == null) {
            ctx.status(400).result("请选择一名参战角色接收战利品");
            return;
        }
        List<String> keys = request == null || request.lootKeys == null
                ? session.engine.get_pending_loot_keys()
                : request.lootKeys;
        session.engine.assign_loot(keys, receiver);
        String log = "战利品已分配给 " + receiver.name + "。";
        session.record(log);
        persistResponse(ctx, session, log);
    }

    private static CombatSession find(ApiExchange ctx) {
        try {
            var saved=Combat_Save_DAO.load();
            if(saved==null || !saved.id().equals(ctx.pathParam("id"))) { ctx.status(404).result("战斗会话不存在或已被新遭遇替换"); return null; }
            return restore(saved);
        } catch (SQLException error) { storageError(ctx); return null; }
        catch (RuntimeException error) { ctx.status(409).result("战斗存档无法恢复，请重新载入或清除存档。"); return null; }
    }

    private static void respondJson(ApiExchange ctx, Object payload) {
        ctx.contentType("application/json").result(ApiServer.GSON.toJson(payload));
    }

    public static synchronized void archivePreview(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        if (!session.engine.is_combat_finished()) { ctx.status(409).result("战斗结束后才能归档。"); return; }
        try {
            Adventure_Log_Entry existing = Adventure_Log_DAO.find_by_combat_source(session.id);
            JsonObject result = new JsonObject();
            result.addProperty("alreadyArchived", existing != null);
            result.add("entry", ApiServer.GSON.toJsonTree(existing == null ? archiveDraft(session) : existing));
            respondJson(ctx, result);
        } catch (SQLException error) { storageError(ctx); }
    }

    public static synchronized void archive(ApiExchange ctx) {
        CombatSession session = find(ctx);
        if (session == null) return;
        if (!session.engine.is_combat_finished()) { ctx.status(409).result("战斗结束后才能归档。"); return; }
        try {
            Adventure_Log_Entry existing = Adventure_Log_DAO.find_by_combat_source(session.id);
            if (existing != null) { respondJson(ctx, existing); return; }
            Adventure_Log_Entry entry = ApiServer.GSON.fromJson(ctx.body(), Adventure_Log_Entry.class);
            if (entry == null || entry.title == null || entry.title.isBlank() || entry.title.trim().length() > 120
                    || entry.content == null || entry.content.isBlank() || entry.content.length() > 200000) {
                ctx.status(400).result("请填写有效的归档标题和内容（标题最多 120 字，内容最多 200000 字）。"); return;
            }
            entry.title = entry.title.trim(); entry.content = entry.content.trim();
            if (entry.adventure_session_id != null && Adventure_Session_DAO.get(entry.adventure_session_id) == null) { ctx.status(400).result("关联场次不存在。"); return; }
            if (entry.location_id != null) {
                var location = Adventure_Atlas_DAO.location(entry.location_id);
                if (location == null) { ctx.status(400).result("关联地点不存在。"); return; }
                entry.location = location.name;
            } else entry.location = "";
            entry.entry_type = "EVENT"; entry.character_id = null; entry.tags = "战斗归档";
            entry.quest_status = ""; entry.event_date = ""; entry.pinned = false;
            ctx.status(201); respondJson(ctx, Adventure_Log_DAO.archive_combat(entry, session.id));
        } catch (com.google.gson.JsonParseException error) { ctx.status(400).result("归档格式无效。"); }
        catch (SQLException error) { storageError(ctx); }
    }

    private static Adventure_Log_Entry archiveDraft(CombatSession session) throws SQLException {
        Adventure_Log_Entry entry = new Adventure_Log_Entry();
        entry.entry_type = "EVENT"; entry.tags = "战斗归档"; entry.quest_status = ""; entry.event_date = "";
        entry.adventure_session_id = session.adventureSessionId; entry.location_id = session.locationId;
        var location = session.locationId == null ? null : Adventure_Atlas_DAO.location(session.locationId);
        entry.location = location == null ? "" : location.name;
        String outcome = session.engine.did_players_win() ? "胜利" : "败北";
        entry.title = (entry.location.isBlank() ? "战斗" : entry.location + "战斗") + " · " + outcome;
        if (entry.title.length() > 120) entry.title = entry.title.substring(0, 120);
        StringBuilder content = new StringBuilder("战斗结果：").append(outcome).append("\n回合：").append(Math.max(1, session.engine.get_current_round() + 1));
        content.append("\n参战角色：").append(String.join("、", session.combatants.stream().filter(c -> c.linked_character != null).map(c -> c.display_name).toList()));
        content.append("\n敌方：").append(String.join("、", session.combatants.stream().filter(c -> c.linked_character == null).map(c -> c.display_name).toList()));
        content.append("\n每名角色已结算经验：").append(session.engine.get_distributed_xp()).append(" XP\n\n结束时的生命状态：\n");
        for (Combatant actor : session.combatants) content.append(actor.display_name).append("：").append(actor.current_hp).append("/").append(actor.max_hp).append("\n");
        content.append("\n待分配掉落：");
        if (session.engine.get_pending_loot_keys().isEmpty()) content.append("无（已分配的物品见下方战斗记录）");
        else content.append(String.join("、", session.engine.get_pending_loot_keys().stream().map(key -> {
            Equipment_Item item = Equipment_Library.get_item(key); return item == null ? key : item.display_name;
        }).toList()));
        content.append("\n战斗记录（含系统结算与分配信息）：\n").append(String.join("\n\n", session.logs));
        if (!session.engine.get_pending_loot_keys().isEmpty()) content.append("\n\n仍有待分配掉落，请在战斗结算中处理。归档本身不会分配掉落或重复发放经验。");
        entry.content = content.length() <= 200000 ? content.toString() : content.substring(0, 199950) + "\n[记录过长，后续部分已截短]";
        return entry;
    }

    private static final class CombatSession {
        final String id;
        final Combat_Engine engine;
        final List<Combatant> combatants;
        final IdentityHashMap<Combatant, String> ids = new IdentityHashMap<>();
        final List<String> logs = new ArrayList<>();
        final Battle_Grid grid;
        boolean started;
        String savedAt;
        Integer adventureSessionId;
        Integer locationId;

        CombatSession(List<Character_Sheet> characters, List<Monster_Definition> monsters) {
            id=UUID.randomUUID().toString();
            engine = new Combat_Engine(characters, monsters);
            engine.defer_character_persistence();
            combatants = engine.get_initiative_order();
            grid = new Battle_Grid(combatants);
            engine.enable_grid(grid);
            for (int i = 0; i < combatants.size(); i++) ids.put(combatants.get(i), "combatant-" + (i + 1));
        }

        CombatSession(Combat_Checkpoint.Saved saved, String savedAt) {
            id=saved.id(); started=saved.started(); this.savedAt=savedAt;
            var restored=Combat_Engine.restore(saved.engine(),saved.grid(),Global_Data.character_pool);
            engine=restored.engine(); combatants=restored.combatants(); grid=restored.grid(); logs.addAll(saved.logs());
            for(int i=0;i<combatants.size();i++)ids.put(combatants.get(i),"combatant-"+(i+1));
        }

        void record(String log) {
            logs.add(log);
            String systemLog=engine.get_and_clear_pending_log();
            if(systemLog!=null&&!systemLog.isBlank())logs.add(systemLog);
        }

        Combatant combatantById(String id) {
            for (Combatant combatant : combatants) if (ids.get(combatant).equals(id)) return combatant;
            return null;
        }

        JsonObject toJson(String latestLog) {
            JsonObject json = new JsonObject();
            json.addProperty("sessionId", id);
            json.addProperty("savedAt", savedAt);
            json.addProperty("adventureSessionId", adventureSessionId);
            json.addProperty("locationId", locationId);
            json.addProperty("started", started);
            json.addProperty("actionSpent", engine.is_action_spent());
            json.addProperty("bonusActionSpent", engine.is_bonus_action_spent());
            json.addProperty("disengaged", engine.is_disengaged());
            json.addProperty("canUndoMove", started && engine.can_undo_move());
            JsonArray reactions=new JsonArray();
            for(var offer:engine.get_reaction_offers()) {
                JsonObject item=new JsonObject();item.addProperty("id",offer.id());item.addProperty("name",offer.option().name);
                item.addProperty("reactorName",offer.reactor().display_name);item.addProperty("targetName",offer.aggressor().display_name);reactions.add(item);
            }
            json.add("reactions",reactions);
            json.addProperty("finished", engine.is_combat_finished());
            json.addProperty("victory", engine.did_players_win());
            json.addProperty("round", Math.max(1, engine.get_current_round() + 1));
            json.addProperty("latestLog", latestLog == null ? "" : latestLog);
            JsonArray order = new JsonArray();
            List<Combatant> livingOrder = engine.get_initiative_order();
            Combatant active = engine.get_active_combatant();
            for (Combatant combatant : combatants) {
                JsonObject item = combatantJson(combatant);
                item.addProperty("active", combatant == active && started && !engine.is_combat_finished());
                item.addProperty("order", livingOrder.indexOf(combatant));
                order.add(item);
            }
            json.add("combatants", order);
            JsonObject battlefield = new JsonObject();
            battlefield.addProperty("width", Battle_Grid.WIDTH);
            battlefield.addProperty("height", Battle_Grid.HEIGHT);
            battlefield.addProperty("feetPerCell", Battle_Grid.FEET_PER_CELL);
            battlefield.add("obstacles", ApiServer.GSON.toJsonTree(grid.obstacles()));
            JsonArray terrain=new JsonArray();
            grid.terrain().forEach((cell,type)->{JsonObject t=new JsonObject();t.addProperty("x",cell.x());t.addProperty("y",cell.y());t.addProperty("type",type.name());terrain.add(t);});
            battlefield.add("terrain",terrain);
            JsonArray moves = new JsonArray();
            if (started && !engine.is_combat_finished() && active != null) {
                for (Battle_Grid.Move move : grid.available_moves(active)) {
                    JsonObject cell = new JsonObject();
                    cell.addProperty("x", move.cell().x());
                    cell.addProperty("y", move.cell().y());
                    cell.addProperty("costFt", move.costFt());
                    moves.add(cell);
                }
            }
            battlefield.add("moves", moves);
            json.add("grid", battlefield);
            json.add("actions", actionJson(active));
            json.add("logs", ApiServer.GSON.toJsonTree(logs));

            JsonObject settlement = new JsonObject();
            settlement.addProperty("xpPerCharacter", engine.get_distributed_xp());
            JsonArray loot = new JsonArray();
            for (String key : engine.get_pending_loot_keys()) {
                JsonObject item = new JsonObject();
                Equipment_Item equipment = Equipment_Library.get_item(key);
                item.addProperty("key", key);
                item.addProperty("name", equipment == null ? key : equipment.display_name);
                loot.add(item);
            }
            settlement.add("loot", loot);
            JsonArray receivers = new JsonArray();
            for (Character_Sheet character : engine.get_participating_characters()) {
                JsonObject receiver = new JsonObject();
                receiver.addProperty("id", character.database_id);
                receiver.addProperty("name", character.name);
                receivers.add(receiver);
            }
            settlement.add("receivers", receivers);
            json.add("settlement", settlement);
            return json;
        }

        JsonObject combatantJson(Combatant combatant) {
            JsonObject item = new JsonObject();
            item.addProperty("id", ids.get(combatant));
            item.addProperty("name", combatant.display_name);
            item.addProperty("side", combatant.side.name());
            item.addProperty("armorClass", combatant.get_effective_armor_class());
            item.addProperty("currentHp", combatant.current_hp);
            item.addProperty("maxHp", combatant.max_hp);
            item.addProperty("initiativeRoll", combatant.initiative_roll);
            item.addProperty("initiativeModifier", combatant.initiative_modifier);
            item.addProperty("initiative", combatant.initiative_total);
            item.addProperty("alive", combatant.is_alive());
            item.addProperty("status", combatant.get_status_summary());
            item.addProperty("x", grid.position(combatant).x());
            item.addProperty("y", grid.position(combatant).y());
            item.addProperty("speedFt", grid.speed(combatant));
            item.addProperty("remainingMovementFt", grid.remaining(combatant));
            item.addProperty("size",grid.size(combatant));item.addProperty("reactionAvailable",combatant.reaction_available);
            item.addProperty("assetKey",combatant.linked_character!=null?"character-"+combatant.linked_character.database_id:combatant.linked_monster!=null?"monster-"+combatant.linked_monster.key:ids.get(combatant));
            return item;
        }

        JsonArray actionJson(Combatant actor) {
            JsonArray result = new JsonArray();
            if (actor == null || !started || engine.is_combat_finished()) return result;
            for (int index = 0; index < actor.attack_options.size(); index++) {
                Attack_Option option = actor.attack_options.get(index);
                JsonObject item = new JsonObject();
                item.addProperty("index", index);
                item.addProperty("itemKey", "");
                item.addProperty("name", option.name);
                item.addProperty("description", option.description);
                item.addProperty("rangeFt", option.range_ft);
                item.addProperty("maxRangeFt", option.max_range_ft);
                item.addProperty("ranged", option.ranged_attack);
                item.addProperty("economy",option.action_economy.name());
                item.addProperty("areaShape",option.area_shape.name());item.addProperty("areaSizeFt",option.area_size_ft);
                item.addProperty("areaFromSelf",option.area_from_self);
                String unavailable=engine.ability_error(option);
                item.addProperty("available",unavailable==null);item.addProperty("unavailableReason",unavailable==null?"":unavailable);
                item.addProperty("category", category(actor, option));
                item.addProperty("resolution", option.resolution_type.name());
                item.addProperty("attackBonus", option.attack_bonus + actor.get_effective_attack_modifier());
                item.addProperty("saveDc", option.save_dc);
                item.addProperty("saveAbility", option.save_ability);
                int min = option.healing_dice_count > 0
                        ? option.healing_dice_count + option.healing_bonus
                        : option.damage_dice_count + option.damage_bonus;
                int max = option.healing_dice_count > 0
                        ? option.healing_dice_count * option.healing_dice_size + option.healing_bonus
                        : option.damage_dice_count * option.damage_dice_size + option.damage_bonus;
                int count = option.healing_dice_count > 0 ? 1 : option.attack_count;
                item.addProperty("effectRange", Math.max(0, min * count) + "–" + Math.max(0, max * count));
                item.addProperty("effectType", option.healing_dice_count > 0 ? "治疗" : option.damage_type);
                JsonArray targets = new JsonArray();
                for (Combatant target : engine.get_valid_targets(option)) {
                    JsonObject preview = new JsonObject();
                    preview.addProperty("id", ids.get(target));
                    preview.addProperty("name", target.display_name);
                    preview.addProperty("hitChance", hitChance(option, actor, target));
                    preview.addProperty("distanceFt", grid.distance(actor, target));
                    preview.addProperty("cover",grid.cover(actor,target).label);
                    targets.add(preview);
                }
                item.add("targets", targets);
                result.add(item);
            }
            if (actor.linked_character != null && !actor.is_turn_blocked()) {
                int thrownItemIndex = -1;
                for (Equipment_Item equipment : actor.linked_character.get_owned_items_for_slot(Equipment_Slot.BACKPACK)) {
                    if (!equipment.is_bomb_item() || actor.linked_character.get_item_count(equipment.key) <= 0) continue;
                    JsonObject item = new JsonObject();
                    item.addProperty("index", thrownItemIndex--);
                    item.addProperty("itemKey", equipment.key);
                    item.addProperty("name", equipment.display_name + " ×" + actor.linked_character.get_item_count(equipment.key));
                    item.addProperty("description", equipment.description);
                    item.addProperty("rangeFt", 20);
                    item.addProperty("maxRangeFt", 20);
                    item.addProperty("ranged", false);item.addProperty("economy","ACTION");item.addProperty("areaShape","NONE");
                    item.addProperty("areaSizeFt",0);item.addProperty("areaFromSelf",false);item.addProperty("available",!engine.is_action_spent()&&engine.get_reaction_offers().isEmpty());item.addProperty("unavailableReason","本回合动作已使用或有待决反应。");
                    item.addProperty("category", "THROW");
                    item.addProperty("resolution", "AUTO_HIT");
                    item.addProperty("attackBonus", 0);
                    item.addProperty("saveDc", 0);
                    item.addProperty("saveAbility", "");
                    int min = equipment.get_bomb_dice_count() + equipment.get_bomb_bonus();
                    int max = equipment.get_bomb_dice_count() * equipment.get_bomb_die_size() + equipment.get_bomb_bonus();
                    item.addProperty("effectRange", Math.max(0, min) + "–" + Math.max(0, max));
                    item.addProperty("effectType", equipment.get_bomb_damage_type());
                    JsonArray targets = new JsonArray();
                    for (Combatant target : engine.get_valid_targets()) {
                        if (grid.distance(actor, target) > 20 || grid.cover(actor,target)==com.DMHelper.basic.combat.Cover_Type.FULL) continue;
                        JsonObject preview = new JsonObject();
                        preview.addProperty("id", ids.get(target));
                        preview.addProperty("name", target.display_name);
                        preview.addProperty("hitChance", 100);
                        preview.addProperty("distanceFt", grid.distance(actor, target));
                        preview.addProperty("cover",grid.cover(actor,target).label);
                        targets.add(preview);
                    }
                    item.add("targets", targets);
                    result.add(item);
                }
            }
            return result;
        }

        JsonObject areaPreview(Attack_Option option, Battle_Grid.Cell aim) {
            Combatant actor=engine.get_active_combatant();
            JsonObject result=new JsonObject();result.addProperty("aimX",aim.x());result.addProperty("aimY",aim.y());
            result.add("cells",ApiServer.GSON.toJsonTree(grid.area_cells(actor,aim,option)));
            JsonArray targets=new JsonArray();
            for(Combatant target:grid.area_targets(actor,aim,option)) {
                JsonObject item=new JsonObject();item.addProperty("id",ids.get(target));item.addProperty("name",target.display_name);
                item.addProperty("friendly",target.side==actor.side);item.addProperty("cover",grid.area_cover(actor,aim,target,option).label);targets.add(item);
            }
            result.add("targets",targets);return result;
        }

        private int hitChance(Attack_Option option, Combatant actor, Combatant target) {
            if (option.resolution_type == Attack_Option.Resolution_Type.AUTO_HIT) return 100;
            if (option.resolution_type == Attack_Option.Resolution_Type.SAVE_DC) {
                int saveBonus = target.get_saving_throw_bonus(option.save_ability) + ("Dexterity".equals(option.save_ability)?grid.cover(actor,target).dexterity_save_bonus:0);
                int successFaces = Math.max(0, Math.min(20, 21 - option.save_dc + saveBonus));
                return 100 - successFaces * 5;
            }
            int bonus = option.attack_bonus + actor.get_effective_attack_modifier();
            int needed = target.get_effective_armor_class() + grid.cover(actor,target).ac_bonus - bonus;
            double chance = Math.max(5, Math.min(95, (21 - needed) * 5)) / 100.0;
            boolean advantage = Advantage_Disadvantage.has_advantage_from_status(actor.status_effects);
            boolean disadvantage = Advantage_Disadvantage.has_disadvantage_from_status(actor.status_effects)
                    || grid.ranged_disadvantage(actor, target, option);
            if (advantage && !disadvantage) chance = 1 - (1 - chance) * (1 - chance);
            if (disadvantage && !advantage) chance *= chance;
            return (int) Math.round(chance * 100);
        }

        private String category(Combatant actor, Attack_Option option) {
            String text = (option.name + " " + option.description).toLowerCase();
            if (text.contains("投掷") || text.contains("飞镖") || text.contains("巨石") || text.contains("javelin") || text.contains("dart") || text.contains("rock")) return "THROW";
            if (actor.linked_character != null && (option.spell_slot_cost_level > 0 || option.pact_slot_cost > 0
                    || text.contains("法术") || text.contains("术 (") || text.contains("魔能") || text.contains("火焰箭")
                    || text.contains("寒霜") || text.contains("电爪") || text.contains("酸液飞溅") || text.contains("寒触")
                    || text.contains("恶言相加") || text.contains("识破先机") || text.contains("吟游激励"))) return "SPELL";
            return "ATTACK";
        }
    }
}
