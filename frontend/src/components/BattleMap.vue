<script setup lang="ts">
import { computed, ref } from 'vue'
import type { BattleToken, Cell, GridState, MapMode } from '../combatTypes'
const props = defineProps<{
 grid:GridState; combatants:BattleToken[]; started:boolean; finished:boolean; busy:boolean
 mode:MapMode; selectedId:string; targetIds:string[]; rangeFt:number; areaMode:boolean; areaCells:Cell[]
 background:string; icons:Record<string,string>; gridOpacity:number
 deploymentSize:number; actionMenuOpen:boolean
}>()
const emit=defineEmits<{select:[id:string];move:[x:number,y:number];'move-intent':[];place:[x:number,y:number];deploy:[id:string,x:number,y:number];obstacle:[x:number,y:number];aim:[x:number,y:number]}>()
const hovered=ref<Cell|null>(null)
interface TokenGesture {
 id: number
 token: BattleToken
 start: Cell
 grab: Cell
}

const board = ref<HTMLDivElement | null>(null)
const dragOffset = ref<Cell | null>(null)
const dropCell = ref<Cell | null>(null)
let gesture: TokenGesture | null = null
let ignorePointerClick = false
const draggedToken = ref<BattleToken | null>(null)
function canDrag(token: BattleToken) {
 if (props.finished || props.busy) return false
 if (!props.started) return props.mode === 'place'
 return active.value?.id === token.id
}
const key=(cell:Cell)=>`${cell.x},${cell.y}`
const living=computed(()=>props.combatants.filter(c=>c.alive))
const tokens=computed(()=>{
 const result=new Map<string,BattleToken>()
 for(const c of living.value)for(let y=0;y<c.size;y++)for(let x=0;x<c.size;x++)result.set(key({x:c.x+x,y:c.y+y}),c)
 return result
})
const terrain=computed(()=>new Map(props.grid.terrain.map(c=>[key(c),c.type])))
const moves=computed(()=>new Map(props.grid.moves.map(c=>[key(c),c.costFt])))
const area=computed(()=>new Set(props.areaCells.map(key)))
const active=computed(()=>living.value.find(c=>c.active))
const cells=computed(()=>Array.from({length:props.grid.width*props.grid.height},(_,i)=>({x:i%props.grid.width,y:Math.floor(i/props.grid.width)})))
const dropSize = computed(() => !props.started && draggedToken.value?.id === props.selectedId ? props.deploymentSize : draggedToken.value?.size ?? 1)
const dropFootprint = computed(() => {
 const anchor = dropCell.value
 const size = dropSize.value
 if (!anchor || !draggedToken.value || !Number.isInteger(size) || size < 1 || size > 4) return []
 return Array.from({length:size * size}, (_,index) => ({x:anchor.x + index % size,y:anchor.y + Math.floor(index / size)}))
})
const dropKeys = computed(() => new Set(dropFootprint.value.map(key)))
const validDrop = computed(() => {
 if (!dropCell.value || !draggedToken.value) return false
 if (props.started) return moves.value.has(key(dropCell.value))
 return dropFootprint.value.length > 0 && dropFootprint.value.every(cell => {
   const feature = terrain.value.get(key(cell))
   const occupant = tokens.value.get(key(cell))
   return cell.x >= 0 && cell.y >= 0 && cell.x < props.grid.width && cell.y < props.grid.height &&
     (!feature || feature === 'DIFFICULT') && (!occupant || occupant.id === draggedToken.value?.id)
 })
})

function cellAtPointer(event: PointerEvent): Cell | undefined {
 const grid = board.value
 if (!grid?.contains(document.elementFromPoint(event.clientX, event.clientY))) return
 const first = grid.querySelector('.cell')?.getBoundingClientRect()
 if (!first) return
 const style = getComputedStyle(grid)
 const gapX = parseFloat(style.columnGap)
 const gapY = parseFloat(style.rowGap)
 const x = Math.floor((event.clientX - first.left + gapX / 2) / (first.width + gapX))
 const y = Math.floor((event.clientY - first.top + gapY / 2) / (first.height + gapY))
 if (x >= 0 && x < props.grid.width && y >= 0 && y < props.grid.height) return {x,y}
}

function startDrag(event: PointerEvent, token: BattleToken) {
 if (event.button !== 0 || !event.isPrimary || gesture) return
 ignorePointerClick = false
 if (!canDrag(token)) return
 const grabbed = cellAtPointer(event)
 gesture = {
   id:event.pointerId,
   token,
   start:{x:event.clientX,y:event.clientY},
   grab:grabbed ? {x:grabbed.x-token.x,y:grabbed.y-token.y} : {x:0,y:0}
 }
 ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
}

function updateDrag(event: PointerEvent) {
 if (!gesture || gesture.id !== event.pointerId || !canDrag(gesture.token)) return
 const offset = {x:event.clientX-gesture.start.x,y:event.clientY-gesture.start.y}
 if (!draggedToken.value) {
   if (Math.hypot(offset.x, offset.y) < 5) return
   if (props.started) emit('move-intent')
 }
 draggedToken.value = gesture.token
 dragOffset.value = offset
 const cell = cellAtPointer(event)
 dropCell.value = cell ? {x:cell.x-gesture.grab.x,y:cell.y-gesture.grab.y} : null
}

function endDrag(event: PointerEvent) {
 if (!gesture || gesture.id !== event.pointerId) return
 if (event.type === 'pointerup') updateDrag(event)
 const token = draggedToken.value
 const destination = dropCell.value
 const commit = event.type === 'pointerup' && token && destination && validDrop.value && canDrag(token)
 ignorePointerClick = !!token
 gesture = null
 draggedToken.value = null
 dragOffset.value = null
 dropCell.value = null
 if (commit && token && destination && (token.x !== destination.x || token.y !== destination.y)) {
   if (props.started) emit('move', destination.x, destination.y)
   else emit('deploy', token.id, destination.x, destination.y)
 }
}

function clickToken(event: MouseEvent, token: BattleToken) {
 if (event.detail && ignorePointerClick) {
   ignorePointerClick = false
   return
 }
 if (props.started && token.active) emit('select', token.id)
 else click(token)
}
const distance=(cell:Cell)=>{
 const c=active.value;if(!c)return null
 const target=tokens.value.get(key(cell)),tx=target?.x??cell.x,ty=target?.y??cell.y,size=target?.size??1
 const dx=Math.max(c.x-(tx+size-1),0,tx-(c.x+c.size-1)),dy=Math.max(c.y-(ty+size-1),0,ty-(c.y+c.size-1))
 return Math.max(dx,dy)*props.grid.feetPerCell
}
const label=(cell:Cell)=>{
 const token=tokens.value.get(key(cell)),type=terrain.value.get(key(cell))
 const terrainNames={WALL:'墙壁',DIFFICULT:'困难地形',HALF_COVER:'半掩体',THREE_QUARTERS_COVER:'四分之三掩体'}
 return `${cell.x+1}, ${cell.y+1}${token?` · ${token.name} · HP ${token.currentHp}/${token.maxHp}`:type?` · ${terrainNames[type]}`:''}${props.started&&distance(cell)!==null?` · 距离 ${distance(cell)} 尺`:''}`
}
const hoverText=computed(()=>hovered.value?label(hovered.value)+(props.mode==='move'&&moves.value.has(key(hovered.value))?` · 移动消耗 ${moves.value.get(key(hovered.value))} 尺`:''):props.areaMode?'点击地图选择范围落点或施法方向。':'棕金色角色、酒红色敌人；悬停查看距离和移动消耗。')
function click(cell:Cell){
 if(props.busy||props.finished)return
 const token=tokens.value.get(key(cell))
 if(!props.started){
  if(props.mode==='obstacle')emit('obstacle',cell.x,cell.y)
  else if(token)emit('select',token.id)
  else if(props.selectedId)emit('place',cell.x,cell.y)
 }else if(props.mode==='attack'&&props.areaMode)emit('aim',cell.x,cell.y)
 else if(token)emit('select',token.id)
 else if(props.mode==='move'&&moves.value.has(key(cell)))emit('move',cell.x,cell.y)
}
</script>
<template>
 <div class="map-scroll">
   <div ref="board" class="battle-map" role="group" aria-label="战场方格地图" :class="{artwork:background, 'is-aiming':areaMode}" :style="{'--grid-columns':grid.width,backgroundImage:background?`url(${background})`:'none','--grid-opacity':gridOpacity}">
   <button v-for="cell in cells" :key="key(cell)" type="button" :aria-label="label(cell)" :title="label(cell)" :disabled="busy||finished"
    :class="['cell',terrain.get(key(cell))?.toLowerCase(),{reachable:started&&(mode==='move'||draggedToken)&&moves.has(key(cell)),range:mode==='attack'&&!areaMode&&(distance(cell)??Infinity)<=rangeFt,area:area.has(key(cell)), 'drop-ok':validDrop&&dropKeys.has(key(cell)), 'drop-blocked':!validDrop&&dropKeys.has(key(cell))}]"
    @mouseenter="hovered=cell" @focus="hovered=cell" @click="click(cell)">
    <span v-if="terrain.has(key(cell))" class="terrain-mark">{{terrain.get(key(cell))==='WALL'?'▧':terrain.get(key(cell))==='DIFFICULT'?'≋':terrain.get(key(cell))==='HALF_COVER'?'½':'¾'}}</span>
    <small v-else-if="started&&(mode==='move'||draggedToken)&&moves.has(key(cell))" class="cost">{{moves.get(key(cell))}}</small>
    <small v-if="cell.y===0" class="column-label">{{cell.x+1}}</small><small v-if="cell.x===0" class="row-label">{{cell.y+1}}</small>
   </button>
   <div class="tokens-layer" :style="{gridTemplateRows:`repeat(${grid.height},1fr)`}">
    <button v-for="c in living" :key="c.id" type="button" :title="started&&c.active&&!finished ? '当前行动 · 点击打开行动菜单' : c.name"
     :aria-label="`棋子 ${c.name}${started&&c.active&&!finished ? ' · 当前行动' : ''}`" :disabled="busy||finished"
     :aria-expanded="started&&c.active&&!finished ? actionMenuOpen : undefined" :aria-haspopup="started&&c.active&&!finished ? 'dialog' : undefined"
     :class="['token',c.side.toLowerCase(),{active:started&&c.active&&!finished,selected:c.id===selectedId,target:mode==='attack'&&targetIds.includes(c.id),draggable:canDrag(c),dragging:draggedToken?.id===c.id}]"
     :style="{gridColumn:`${c.x+1} / span ${c.size}`,gridRow:`${c.y+1} / span ${c.size}`,translate:dragOffset&&draggedToken?.id===c.id ? `${dragOffset.x}px ${dragOffset.y}px` : undefined}"
     @pointerdown="startDrag($event,c)" @pointermove="updateDrag" @pointerup="endDrag"
     @pointercancel="endDrag" @lostpointercapture="endDrag" @dragstart.prevent
     @click="clickToken($event,c)" @mouseenter="hovered={x:c.x,y:c.y}">
     <img v-if="icons[c.assetKey]" :src="icons[c.assetKey]" :alt="c.name" draggable="false"><b v-else>{{c.name.slice(0,2)}}</b>
     <small>{{c.id.replace('combatant-','')}}</small><i :style="{width:Math.max(0,c.currentHp/c.maxHp*100)+'%'}"></i>
    </button>
   </div>
  </div>
 </div>
 <slot />
 <p class="map-hover" aria-live="polite">{{hoverText}}</p>
</template>
<style scoped>
.map-scroll { overflow: auto; padding: 3px; }
.battle-map {
  --cell-min: 36px;
  --grid-gap: 2px;
  --grid-padding: 4px;
  position: relative;
  min-width: max(600px, calc(var(--grid-columns) * (var(--cell-min) + var(--grid-gap)) - var(--grid-gap) + 2 * var(--grid-padding)));
  background-color: var(--surface-inset);
  background-size: 100% 100%;
  background-position: center;
  padding: var(--grid-padding);
  border-radius: 8px;
}
.battle-map,
.tokens-layer {
  display: grid;
  grid-template-columns: repeat(var(--grid-columns), minmax(var(--cell-min), 1fr));
  gap: var(--grid-gap);
}
.cell { position: relative; aspect-ratio: 1; border-radius: 2px; color: var(--text-primary); cursor: pointer; padding: 3px; min-width: 0; border: 1px solid rgba(var(--map-grid-rgb), var(--grid-opacity)); --control-bg: var(--surface-card); --control-hover: var(--surface-hover); }
.artwork .cell { --control-bg: var(--map-grid-artwork); --control-hover: var(--map-grid-hover); }
.cell:hover, .cell:focus-visible { z-index: 1; }
.cell:disabled { cursor: default; }
.is-aiming .cell:not(:disabled) { cursor: crosshair; }
.cell.reachable { --control-bg: var(--map-reachable); }
.cell.range { --control-bg: var(--map-range); }
.cell.area { --control-bg: var(--map-area); }
.cell.wall { --control-bg: var(--map-wall); }
.cell.difficult { --control-bg: var(--map-difficult); }
.cell.half_cover { --control-bg: var(--map-half-cover); }
.cell.three_quarters_cover { --control-bg: var(--map-three-quarters-cover); }
.terrain-mark { font-size: 1.2rem; color: var(--text-primary); }
.tokens-layer { position: absolute; inset: var(--grid-padding); z-index: 2; pointer-events: none; }
.token { pointer-events: auto; position: relative; display: flex; align-items: center; justify-content: center; align-self: stretch; justify-self: stretch; margin: 2px; padding: 0; color: var(--text-primary); border-radius: 50%; overflow: hidden; cursor: pointer; --control-bg: var(--surface-selected); --control-hover: var(--surface-hover); box-shadow: var(--overlay-shadow); isolation: isolate; }
.token.enemy { --control-bg: var(--wine); --control-hover: var(--wine-hover); }
.token.active {
  anchor-name: --active-battle-token;
  outline: 3px solid var(--gold-hover);
  outline-offset: 2px;
  box-shadow: 0 0 0 2px var(--surface-inset);
}
.token.selected { outline: 2px solid var(--antique-gold); }
.token.target { outline: 2px solid var(--text-danger); }
.token.active.selected, .token.active.target { outline: 3px solid var(--gold-hover); }
.token.draggable { cursor: grab; touch-action: none; user-select: none; }
.token.dragging { z-index: 2; cursor: grabbing; opacity: .65; }
.cell.drop-ok { box-shadow: inset 0 0 0 2px var(--antique-gold); background: var(--map-range); }
.cell.drop-blocked { box-shadow: inset 0 0 0 2px var(--text-danger); background: var(--map-area); }
.token img { width: 100%; height: 100%; object-fit: cover; position: absolute; inset: 0; z-index: -1; }
.token b { font-size: clamp(.65rem,1.1vw,1.1rem); white-space: nowrap; }
.token small { position: absolute; right: 3px; top: 2px; font-size: .6rem; color: var(--text-primary); background: var(--scrim); border-radius: 3px; padding: 1px 3px; }
.token i { position: absolute; bottom: 0; left: 0; height: 4px; background: var(--hp-party); }
.token.enemy i { background: var(--hp-enemy); }
.cost { color: var(--text-primary); font-size: .65rem; text-shadow: 0 1px 2px var(--background); }
.column-label, .row-label { position: absolute; color: var(--text-secondary); font-size: .5rem; pointer-events: none; }
.column-label { top: 0; left: 2px; }
.row-label { bottom: 0; left: 1px; }
.map-hover { min-height: 1.5em; margin: 10px 0 0; color: var(--text-secondary); font-size: .76rem; }
.cell:focus-visible { outline: 2px solid var(--text-primary); outline-offset: 0; }
.cell:is(.reachable,.range,.area,.wall,.difficult,.half_cover,.three_quarters_cover) { --control-hover: color-mix(in srgb, var(--control-bg), var(--text-primary) 15%); }
</style>
