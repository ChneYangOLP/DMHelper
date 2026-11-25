<script setup lang="ts">
import { computed, ref } from 'vue'
import type { BattleToken, Cell, GridState, MapMode } from '../combatTypes'
const props = defineProps<{
 grid:GridState; combatants:BattleToken[]; started:boolean; finished:boolean; busy:boolean
 mode:MapMode; selectedId:string; targetIds:string[]; rangeFt:number; areaMode:boolean; areaCells:Cell[]
 background:string; icons:Record<string,string>; gridOpacity:number
}>()
const emit=defineEmits<{select:[id:string];move:[x:number,y:number];place:[x:number,y:number];obstacle:[x:number,y:number];aim:[x:number,y:number]}>()
const hovered=ref<Cell|null>(null)
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
const hoverText=computed(()=>hovered.value?label(hovered.value)+(props.mode==='move'&&moves.value.has(key(hovered.value))?` · 移动消耗 ${moves.value.get(key(hovered.value))} 尺`:''):props.areaMode?'点击地图选择范围落点或施法方向。':'蓝色角色、红色敌人；悬停查看距离和移动消耗。')
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
  <div class="battle-map" role="group" aria-label="战场方格地图" :class="{artwork:background}" :style="{gridTemplateColumns:`repeat(${grid.width}, minmax(36px, 1fr))`,backgroundImage:background?`url(${background})`:'none','--grid-opacity':gridOpacity}">
   <button v-for="cell in cells" :key="key(cell)" type="button" :aria-label="label(cell)" :title="label(cell)" :disabled="busy||finished"
    :class="['cell',terrain.get(key(cell))?.toLowerCase(),{reachable:started&&mode==='move'&&moves.has(key(cell)),range:mode==='attack'&&!areaMode&&(distance(cell)??Infinity)<=rangeFt,area:area.has(key(cell))}]"
    @mouseenter="hovered=cell" @focus="hovered=cell" @click="click(cell)">
    <span v-if="terrain.has(key(cell))" class="terrain-mark">{{terrain.get(key(cell))==='WALL'?'▧':terrain.get(key(cell))==='DIFFICULT'?'≋':terrain.get(key(cell))==='HALF_COVER'?'½':'¾'}}</span>
    <small v-else-if="started&&mode==='move'&&moves.has(key(cell))" class="cost">{{moves.get(key(cell))}}</small>
    <small v-if="cell.y===0" class="column-label">{{cell.x+1}}</small><small v-if="cell.x===0" class="row-label">{{cell.y+1}}</small>
   </button>
   <div class="tokens-layer" :style="{gridTemplateColumns:`repeat(${grid.width},1fr)`,gridTemplateRows:`repeat(${grid.height},1fr)`}">
    <button v-for="c in living" :key="c.id" type="button" :title="c.name" :aria-label="`棋子 ${c.name}`" :disabled="busy||finished"
     :class="['token',c.side.toLowerCase(),{active:c.active,selected:c.id===selectedId,target:mode==='attack'&&targetIds.includes(c.id)}]"
     :style="{gridColumn:`${c.x+1} / span ${c.size}`,gridRow:`${c.y+1} / span ${c.size}`}" @click="click({x:c.x,y:c.y})" @mouseenter="hovered={x:c.x,y:c.y}">
     <img v-if="icons[c.assetKey]" :src="icons[c.assetKey]" :alt="c.name"><b v-else>{{c.name.slice(0,2)}}</b>
     <small>{{c.id.replace('combatant-','')}}</small><i :style="{width:Math.max(0,c.currentHp/c.maxHp*100)+'%'}"></i>
    </button>
   </div>
  </div>
 </div>
 <p class="map-hover" aria-live="polite">{{hoverText}}</p>
</template>
<style scoped>
.map-scroll{overflow:auto;padding:3px}.battle-map{position:relative;display:grid;min-width:600px;gap:2px;border:1px solid #393b3b;background-color:#121618;background-size:100% 100%;background-position:center;padding:4px;border-radius:8px}.cell{position:relative;aspect-ratio:1;border:1px solid rgba(255,255,255,var(--grid-opacity));border-radius:2px;background:rgba(32,39,41,.8);color:#d9ddda;cursor:pointer;padding:3px;min-width:0}.artwork .cell{background:rgba(12,18,20,.2)}.cell:hover,.cell:focus-visible{outline:2px solid #ead28a;z-index:1}.cell:disabled{cursor:default}.cell.reachable{background:rgba(58,130,98,.35);border-color:#63a786}.cell.range{background:rgba(175,137,53,.26);border-color:#978046}.cell.area{background:rgba(206,106,32,.5);box-shadow:inset 0 0 0 1px #ef9c50}.cell.wall{background:repeating-linear-gradient(135deg,#353637dd,#353637dd 5px,#292b2dee 5px,#292b2dee 10px)}.cell.difficult{background:#77623b80}.cell.half_cover{background:#577a8790}.cell.three_quarters_cover{background:#47606ddd}.terrain-mark{font-size:1.2rem;color:#d0d7d5}.tokens-layer{position:absolute;inset:4px;display:grid;gap:2px;pointer-events:none}.token{pointer-events:auto;position:relative;display:flex;align-items:center;justify-content:center;align-self:stretch;justify-self:stretch;margin:2px;padding:0;color:#fff;border-radius:50%;background:linear-gradient(140deg,#426a87,#203d56);border:1px solid #74a7c4;box-shadow:0 2px 5px #0008;overflow:hidden;cursor:pointer}.token.enemy{background:linear-gradient(140deg,#92584b,#522924);border-color:#ce8b73}.token.active{box-shadow:0 0 0 2px #efd684,0 0 12px #d4ae5980}.token.selected{outline:2px solid #f7e7b2}.token.target{outline:2px solid #da9353}.token img{width:100%;height:100%;object-fit:cover;position:absolute;inset:0}.token b{font-size:clamp(.65rem,1.1vw,1.1rem);white-space:nowrap}.token small{position:absolute;right:3px;top:2px;font-size:.6rem;color:#fff;background:#0008;border-radius:3px;padding:1px 3px}.token i{position:absolute;bottom:0;left:0;height:4px;background:#72c799}.token.enemy i{background:#f09b72}.cost{color:#a8d5b9;font-size:.65rem;text-shadow:0 1px 3px #000}.column-label,.row-label{position:absolute;color:#bbb;font-size:.5rem;pointer-events:none;text-shadow:0 1px 2px #000}.column-label{top:0;left:2px}.row-label{bottom:0;left:1px}.map-hover{min-height:1.5em;margin:10px 0 0;color:var(--text-secondary);font-size:.76rem}
</style>
