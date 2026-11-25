package com.DMHelper.basic.combat;

import com.DMHelper.basic.Encumbrance_System;
import com.DMHelper.basic.race.Creature_Size;
import java.util.*;

/** Square battlefield. Coordinates are top-left anchors for multi-square tokens. */
public final class Battle_Grid {
    public static final int FEET_PER_CELL = 5, WIDTH = 16, HEIGHT = 12;
    public record Cell(int x, int y) {}
    public record Move(Cell cell, int costFt) {}
    public enum Terrain { WALL, DIFFICULT, HALF_COVER, THREE_QUARTERS_COVER }
    private record Snapshot(Combatant actor, Cell position, int moved) {}
    private record Search(Map<Cell,Integer> costs, Map<Cell,Cell> parents) {}
    private final List<Combatant> combatants;
    private final Map<Combatant,Cell> positions = new IdentityHashMap<>();
    private final Map<Combatant,Integer> speeds = new IdentityHashMap<>(), sizes = new IdentityHashMap<>(), moved = new IdentityHashMap<>();
    private final Map<Cell,Terrain> terrain = new HashMap<>();
    private Snapshot undo;
    private boolean undo_locked;
    private int extra_movement;

    public record SavedToken(Cell position, Integer speed, int size, int moved) {}
    public record SavedTerrain(Cell cell, Terrain type) {}
    public record SavedState(List<SavedToken> tokens, List<SavedTerrain> terrain, Integer undoActor,
                             Cell undoPosition, int undoMoved, boolean undoLocked, int extraMovement) {}
    public SavedState snapshot() {
        List<SavedToken> tokens = new ArrayList<>();
        for (Combatant actor : combatants) tokens.add(new SavedToken(position(actor), speeds.get(actor), size(actor), moved.getOrDefault(actor,0)));
        List<SavedTerrain> cells = new ArrayList<>();
        terrain.forEach((cell,type) -> cells.add(new SavedTerrain(cell,type)));
        return new SavedState(tokens,cells,undo == null ? null : combatants.indexOf(undo.actor),
                undo == null ? null : undo.position,undo == null ? 0 : undo.moved,undo_locked,extra_movement);
    }
    private Battle_Grid(List<Combatant> actors, SavedState state) {
        combatants = new ArrayList<>(actors);
        if (state.tokens.size() != actors.size()) throw new IllegalArgumentException("棋子存档不完整");
        for (int i=0;i<actors.size();i++) {
            SavedToken token = state.tokens.get(i);
            if (token.size<1 || token.size>4 || !in_bounds(token.position) || token.position.x+token.size>WIDTH
                    || token.position.y+token.size>HEIGHT || token.moved<0) throw new IllegalArgumentException("棋子存档坐标无效");
            positions.put(actors.get(i), token.position); sizes.put(actors.get(i), token.size);
            moved.put(actors.get(i),token.moved);
            if (token.speed != null) speeds.put(actors.get(i),token.speed);
        }
        for (SavedTerrain cell : state.terrain) {
            if (!in_bounds(cell.cell) || cell.type==null) throw new IllegalArgumentException("地形存档无效");
            terrain.put(cell.cell,cell.type);
        }
        undo_locked=state.undoLocked; extra_movement=state.extraMovement;
        if (state.undoActor != null) undo = new Snapshot(actors.get(state.undoActor),state.undoPosition,state.undoMoved);
    }
    public static Battle_Grid restore(List<Combatant> actors, SavedState state) { return new Battle_Grid(actors,state); }

    public Battle_Grid(List<Combatant> combatants) {
        this.combatants = new ArrayList<>(combatants);
        for(Combatant.Side side:Combatant.Side.values())if(combatants.stream().filter(c->c.side==side).count()>48)throw new IllegalArgumentException("每个阵营最多 48 个棋子。");
        for (Combatant c : combatants) {
            int size = c.linked_monster != null ? c.linked_monster.footprint
                    : c.linked_character != null && c.linked_character.race != null && c.linked_character.race.creature_size == Creature_Size.LARGE ? 2 : 1;
            sizes.put(c,size);
            boolean placed = false;
            int start = c.side == Combatant.Side.PLAYER ? 1 : WIDTH / 2;
            int end = c.side == Combatant.Side.PLAYER ? WIDTH / 2 : WIDTH;
            for (int pass=0; pass<2 && !placed; pass++) {
                for (int y=0; y<HEIGHT && !placed; y++) for (int x=pass==0?start:0; x<(pass==0?end:WIDTH) && !placed; x++) {
                    Cell cell=new Cell(x,y);
                    if (can_occupy(c,cell,size)) { positions.put(c,cell); placed=true; }
                }
            }
            if (!placed) throw new IllegalArgumentException("地图没有足够空间放置这些体型的棋子，请减少参战数量。");
        }
    }
    public Cell position(Combatant c) { return positions.get(c); }
    public int size(Combatant c) { return sizes.getOrDefault(c,1); }
    public Map<Cell,Terrain> terrain() { return Map.copyOf(terrain); }
    public Set<Cell> obstacles() {
        Set<Cell> walls=new HashSet<>(); terrain.forEach((cell,type)->{if(type==Terrain.WALL)walls.add(cell);}); return walls;
    }
    public boolean in_bounds(Cell cell) { return cell!=null && cell.x>=0 && cell.x<WIDTH && cell.y>=0 && cell.y<HEIGHT; }
    public List<Cell> footprint(Combatant c) { return footprint(position(c),size(c)); }
    public List<Cell> footprint(Cell anchor,int size) {
        List<Cell> result=new ArrayList<>();
        if(anchor!=null)for(int y=0;y<size;y++)for(int x=0;x<size;x++)result.add(new Cell(anchor.x+x,anchor.y+y));
        return result;
    }
    public Combatant occupant(Cell cell) {
        for(Combatant c:combatants)if(c.is_alive() && footprint(c).contains(cell))return c;
        return null;
    }
    private boolean solid(Cell cell) { return terrain.containsKey(cell) && terrain.get(cell)!=Terrain.DIFFICULT; }
    private boolean can_occupy(Combatant c,Cell anchor,int size) {
        for(Cell cell:footprint(anchor,size)) {
            if(!in_bounds(cell)||solid(cell))return false;
            Combatant other=occupant(cell);
            if(other!=null&&other!=c)return false;
        }
        return true;
    }
    public void place(Combatant c,Cell cell,Integer speedFt) { place(c,cell,speedFt,null); }
    public void place(Combatant c,Cell cell,Integer speedFt,Integer footprintSize) {
        int newSize=footprintSize==null?size(c):footprintSize;
        if(!positions.containsKey(c)||newSize<1||newSize>4||!in_bounds(cell))throw new IllegalArgumentException("无效的棋子、位置或体型（1–4 格）。");
        if(!can_occupy(c,cell,newSize))throw new IllegalArgumentException("棋子占格超出地图或与障碍、其他棋子重叠。");
        if(speedFt!=null&&(speedFt<0||speedFt>120||speedFt%5!=0))throw new IllegalArgumentException("速度须为 0–120 尺之间的 5 的倍数。");
        positions.put(c,cell); sizes.put(c,newSize); if(speedFt!=null)speeds.put(c,speedFt);
    }
    public void toggle_obstacle(Cell cell) { toggle_terrain(cell,Terrain.WALL); }
    public void toggle_terrain(Cell cell,Terrain type) {
        if(!in_bounds(cell)||type==null)throw new IllegalArgumentException("无效的地形位置或类型。");
        if(type!=Terrain.DIFFICULT&&occupant(cell)!=null)throw new IllegalArgumentException("不能在棋子占格上设置障碍或掩体。");
        if(terrain.get(cell)==type)terrain.remove(cell); else terrain.put(cell,type);
    }
    private boolean has_status(Combatant c,Combat_Status_Type type) { return c.status_effects.stream().anyMatch(e->e.type==type); }
    public int speed(Combatant c) {
        if(c==null||!c.is_alive()||c.is_turn_blocked()||has_status(c,Combat_Status_Type.RESTRAINED))return 0;
        int speed=speeds.containsKey(c)?speeds.get(c):base_speed(c);
        if(has_status(c,Combat_Status_Type.SLOWED))speed/=2;
        return Math.max(0,speed/5*5);
    }
    private int base_speed(Combatant c) {
        if(c.linked_character==null)return 30;
        int racial=c.linked_character.race==null?30:c.linked_character.race.base_speed;
        return Math.max(0,racial-(30-Encumbrance_System.get_effective_speed(c.linked_character)));
    }
    public int remaining(Combatant c) { return Math.max(0,speed(c)+extra_movement-moved.getOrDefault(c,0)); }
    public void begin_turn(Combatant c) { moved.put(c,0); extra_movement=0; undo=null; undo_locked=false; }
    public void dash(Combatant c) { extra_movement+=speed(c); lock_undo(); }
    public void stand(Combatant c) {
        if(!has_status(c,Combat_Status_Type.PRONE))throw new IllegalArgumentException("当前没有倒地。");
        int cost=speed(c)/2;
        if(speed(c)==0||remaining(c)<cost)throw new IllegalArgumentException("移动距离不足以起身。");
        moved.merge(c,cost,Integer::sum);c.status_effects.removeIf(e->e.type==Combat_Status_Type.PRONE);lock_undo();
    }
    public void lock_undo() { undo=null; undo_locked=true; }
    public boolean can_undo(Combatant c) { return !undo_locked&&undo!=null&&undo.actor==c&&c.is_alive(); }
    public void record_move(Combatant c) { if(!undo_locked)undo=new Snapshot(c,position(c),moved.getOrDefault(c,0)); }
    public void undo_move(Combatant c) {
        if(!can_undo(c))throw new IllegalArgumentException("没有可撤销的移动，或本回合已执行动作、附赠动作或触发反应。");
        if(!can_occupy(c,undo.position,size(c)))throw new IllegalArgumentException("原位置已被占据，不能撤销。");
        positions.put(c,undo.position); moved.put(c,undo.moved); undo=null;
    }
    public int distance(Cell a,Cell b) { return Math.max(Math.abs(a.x-b.x),Math.abs(a.y-b.y))*5; }
    public int distance(Combatant a,Combatant b) { return distance_at(a,position(a),b); }
    public int distance_at(Combatant a,Cell at,Combatant b) {
        int best=Integer.MAX_VALUE;
        for(Cell from:footprint(at,size(a)))for(Cell to:footprint(b))best=Math.min(best,distance(from,to));
        return best;
    }
    public int distance_to(Combatant a,Cell to) {
        return footprint(a).stream().mapToInt(from->distance(from,to)).min().orElse(Integer.MAX_VALUE);
    }
    private int step_cost(Combatant c,Cell next) {
        boolean difficult=footprint(next,size(c)).stream().anyMatch(cell->terrain.get(cell)==Terrain.DIFFICULT);
        return 5+(difficult?5:0)+(has_status(c,Combat_Status_Type.PRONE)?5:0);
    }
    private Search search(Combatant c) {
        Map<Cell,Integer> costs=new HashMap<>(); Map<Cell,Cell> parents=new HashMap<>();
        Cell start=position(c); if(start==null)return new Search(costs,parents);
        PriorityQueue<Move> queue=new PriorityQueue<>(Comparator.comparingInt(Move::costFt));
        costs.put(start,0);queue.add(new Move(start,0));
        int budget=remaining(c);
        while(!queue.isEmpty()) {
            Move current=queue.remove(); if(current.costFt!=costs.get(current.cell))continue;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                if(dx==0&&dy==0)continue;
                Cell next=new Cell(current.cell.x+dx,current.cell.y+dy);
                if(!can_occupy(c,next,size(c)))continue;
                if(dx!=0&&dy!=0&&(!can_occupy(c,new Cell(current.cell.x+dx,current.cell.y),size(c))||!can_occupy(c,new Cell(current.cell.x,current.cell.y+dy),size(c))))continue;
                int cost=current.costFt+step_cost(c,next);
                if(cost>budget||cost>=costs.getOrDefault(next,Integer.MAX_VALUE))continue;
                costs.put(next,cost);parents.put(next,current.cell);queue.add(new Move(next,cost));
            }
        }
        return new Search(costs,parents);
    }
    public List<Move> available_moves(Combatant c) {
        List<Move> result=new ArrayList<>(); if(remaining(c)<=0)return result;
        search(c).costs.forEach((cell,cost)->{if(cost>0)result.add(new Move(cell,cost));});return result;
    }
    public List<Cell> path(Combatant c,Cell to) {
        Search search=search(c);
        if(!search.costs.containsKey(to)||to.equals(position(c)))throw new IllegalArgumentException("目的地不可到达、被占据或超出移动距离。");
        LinkedList<Cell> path=new LinkedList<>();
        for(Cell cell=to;!cell.equals(position(c));cell=search.parents.get(cell))path.addFirst(cell);
        return path;
    }
    public int step(Combatant c,Cell to) {
        if(distance(position(c),to)!=5||!can_occupy(c,to,size(c)))throw new IllegalArgumentException("无效的移动路径。");
        int cost=step_cost(c,to);if(cost>remaining(c))throw new IllegalArgumentException("移动距离不足。");
        positions.put(c,to);moved.merge(c,cost,Integer::sum);return cost;
    }
    public int move(Combatant c,Cell to) { List<Cell> path=path(c,to);record_move(c);int cost=0;for(Cell step:path)cost+=step(c,step);return cost; }

    // Sample rays to a target's footprint. Walls block sight; low cover and creatures grant cover.
    private Cover_Type ray(double ax,double ay,double bx,double by,Combatant actor,Combatant target) {
        int samples=(int)(Math.max(Math.abs(bx-ax),Math.abs(by-ay))*16)+1;
        Cover_Type cover=Cover_Type.NONE;
        Set<Cell> source=actor==null?Set.of(new Cell((int)ax,(int)ay)):new HashSet<>(footprint(actor));
        Set<Cell> end=target==null?Set.of(new Cell((int)bx,(int)by)):new HashSet<>(footprint(target));
        for(int i=1;i<samples;i++) {
            double t=(double)i/samples;Cell cell=new Cell((int)Math.floor(ax+(bx-ax)*t),(int)Math.floor(ay+(by-ay)*t));
            if(source.contains(cell)||end.contains(cell))continue;
            Terrain type=terrain.get(cell);
            if(type==Terrain.WALL)return Cover_Type.FULL;
            if(type==Terrain.THREE_QUARTERS_COVER)cover=Cover_Type.THREE_QUARTERS;
            else if((type==Terrain.HALF_COVER||occupant(cell)!=null)&&cover==Cover_Type.NONE)cover=Cover_Type.HALF;
        }
        return cover;
    }
    public Cover_Type cover(Combatant actor,Combatant target) {
        List<Cell> origins=footprint(actor);List<Cell> targets=footprint(target);
        if(origins.isEmpty()||targets.isEmpty())return Cover_Type.FULL;
        int blocked=0,total=0,low=0;
        for(Cell to:targets)for(double ox:new double[]{.25,.75})for(double oy:new double[]{.25,.75}) {
            int best=3;
            for(Cell from:origins)best=Math.min(best,ray(from.x+.5,from.y+.5,to.x+ox,to.y+oy,actor,target).ordinal());
            total++;if(best==3)blocked++;else low=Math.max(low,best);
        }
        if(blocked==total)return Cover_Type.FULL;
        int rating=blocked*4>=total*3?2:blocked*2>=total?1:0;
        return Cover_Type.values()[Math.max(rating,low)];
    }
    public boolean visible(Combatant actor,Cell to) {
        return footprint(actor).stream().anyMatch(from->ray(from.x+.5,from.y+.5,to.x+.5,to.y+.5,actor,null)!=Cover_Type.FULL);
    }
    public boolean in_range(Combatant actor,Combatant target,Attack_Option option) { return distance(actor,target)<=option.max_range_ft&&cover(actor,target)!=Cover_Type.FULL; }
    public boolean ranged_disadvantage(Combatant actor,Combatant target,Attack_Option option) {
        if(!option.ranged_attack)return false;
        if(distance(actor,target)>option.range_ft)return true;
        return combatants.stream().anyMatch(e->e.side!=actor.side&&e.is_alive()&&!e.is_turn_blocked()&&!has_status(e,Combat_Status_Type.INVISIBLE)&&distance(actor,e)<=5&&cover(e,actor)!=Cover_Type.FULL);
    }

    private Cell area_origin(Combatant actor,Cell aim,Attack_Option option) {
        if(!option.area_from_self)return aim;
        return footprint(actor).stream().min(Comparator.comparingInt(c->distance(c,aim))).orElse(position(actor));
    }
    public List<Cell> area_cells(Combatant actor,Cell aim,Attack_Option option) {
        if(option.area_shape==Attack_Option.Area_Shape.NONE||!in_bounds(aim))throw new IllegalArgumentException("无效的范围技能或落点。");
        if(!option.area_from_self&&(distance_to(actor,aim)>option.max_range_ft||!visible(actor,aim)||solid(aim)))throw new IllegalArgumentException("落点超出射程、被墙遮挡或位于障碍中。");
        Cell origin=area_origin(actor,aim,option);
        double vx=aim.x-origin.x,vy=aim.y-origin.y,len=Math.hypot(vx,vy);
        if(option.area_from_self&&len==0&&option.area_shape!=Attack_Option.Area_Shape.CIRCLE)throw new IllegalArgumentException("请选择施法方向，不能指向自身占格。");
        double ux=len==0?1:vx/len,uy=len==0?0:vy/len;
        double extent=option.area_size_ft/5.0,width=option.area_width_ft/5.0;
        List<Cell> result=new ArrayList<>();
        for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
            Cell cell=new Cell(x,y);double dx=x-origin.x,dy=y-origin.y;
            double forward=dx*ux+dy*uy,side=Math.abs(dx*uy-dy*ux);boolean inside=false;
            switch(option.area_shape) {
                case CIRCLE -> inside=Math.hypot(dx,dy)<=extent;
                case CONE -> inside=forward>0&&forward<=extent&&side<=forward/2+.0001;
                case LINE -> inside=forward>0&&forward<=extent&&side<width/2+.0001;
                case CUBE -> inside=option.area_from_self?forward>0&&forward<=extent&&side<extent/2:dx>=0&&dx<extent&&dy>=0&&dy<extent;
                default -> { }
            }
            if(!inside||terrain.get(cell)==Terrain.WALL||option.area_from_self&&footprint(actor).contains(cell))continue;
            if(ray(origin.x+.5,origin.y+.5,x+.5,y+.5,option.area_from_self?actor:null,null)!=Cover_Type.FULL)result.add(cell);
        }
        // Effects such as Fireball spread around corners, but never through a sealed wall.
        if(option.area_wraps_corners) {
            Set<Cell> geometric=new HashSet<>();
            for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) {
                Cell c=new Cell(x,y);if(Math.hypot(x-origin.x,y-origin.y)<=extent&&terrain.get(c)!=Terrain.WALL)geometric.add(c);
            }
            result.clear();Set<Cell> seen=new HashSet<>();ArrayDeque<Cell> queue=new ArrayDeque<>();
            queue.add(origin);seen.add(origin);
            while(!queue.isEmpty()){Cell c=queue.remove();if(geometric.contains(c))result.add(c);for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){Cell n=new Cell(c.x+d[0],c.y+d[1]);if(geometric.contains(n)&&seen.add(n))queue.add(n);}}
        }
        return result;
    }
    public List<Combatant> area_targets(Combatant actor,Cell aim,Attack_Option option) {
        Set<Cell> cells=new HashSet<>(area_cells(actor,aim,option));
        return combatants.stream().filter(c->c.is_alive()&&footprint(c).stream().anyMatch(cells::contains)).toList();
    }
    public Cover_Type area_cover(Combatant actor,Cell aim,Combatant target,Attack_Option option) {
        if(option.area_wraps_corners)return Cover_Type.NONE;
        Cell origin=area_origin(actor,aim,option);int best=3;
        for(Cell c:footprint(target))best=Math.min(best,ray(origin.x+.5,origin.y+.5,c.x+.5,c.y+.5,option.area_from_self?actor:null,target).ordinal());
        return Cover_Type.values()[best];
    }
}
