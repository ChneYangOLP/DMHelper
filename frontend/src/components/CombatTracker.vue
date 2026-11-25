<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError, CombatAPI } from '../api'
import BattleMap from './BattleMap.vue'
import type { AreaPreview, BattleToken, CombatAction, CombatState, MapMode, Terrain } from '../combatTypes'
import { loadArtwork, readArtwork, saveArtwork } from '../combatAssets'
import { notifyAdventureHistoryChanged } from '../aiPanel'
import type { AdventureSession, CombatArchiveEntry } from '../adventureTypes'
import type { AdventureLocation } from '../atlasTypes'

type ActionCategory = 'ATTACK' | 'SPELL' | 'THROW'
interface SetupCharacter { id:number; name:string; className:string; level:number; currentHp:number; maxHp:number; armorClass:number; initiativeModifier:number; available:boolean }
interface SetupMonster { key:string; fullName:string; monster_type:string; recommended_level:string; xp_reward:number; armor_class:number; hpRange:string }
const loading=ref(true), busy=ref(false), error=ref(''), monsterSearch=ref(''), selectedTargetId=ref('')
const characters=ref<SetupCharacter[]>([]), monsters=ref<SetupMonster[]>([])
const selectedCharacterIds=ref<number[]>([]), selectedMonsterKeys=ref<string[]>([])
const adventureSessions=ref<AdventureSession[]>([]), locations=ref<AdventureLocation[]>([])
const adventureSessionId=ref<number|null>(null), combatLocationId=ref<number|null>(null)
const archiveOpen=ref(false), archiveDraft=ref<CombatArchiveEntry|null>(null), archivedEntryId=ref<number|null>(null)
const state=ref<CombatState|null>(null), category=ref<ActionCategory|null>(null), selectedOptionIndex=ref<number|null>(null), receiverId=ref<number|null>(null)
const recoveryFailed=ref(false), restored=ref(false)
const savedTime=computed(()=>state.value?.savedAt?new Date(state.value.savedAt).toLocaleString('zh-CN'):'')
const categoryLabels:Record<ActionCategory,string>={ATTACK:'攻击',SPELL:'法术',THROW:'物品'}
const turnMenuOpen = ref(false)
const turnPopover = ref<HTMLElement | null>(null)
const battlefield = ref<HTMLElement | null>(null)
const mapMode=ref<MapMode>('place'), deploymentId=ref(''), deploymentSpeed=ref(30), deploymentSize=ref(1)
const terrainType=ref<Terrain>('WALL'), areaPreview=ref<AreaPreview|null>(null)
const artwork=ref<Record<string,string>>({}), artworkBusy=ref(false), avatarId=ref(''), gridOpacity=ref(.18)
const avatarToken=computed(()=>state.value?.combatants.find(c=>c.id===avatarId.value))
const canExecute=computed(()=>!!selectedAction.value?.available&&(!state.value?.reactions.length)&&(selectedAction.value.areaShape==='NONE'?!!selectedTarget.value:!!areaPreview.value?.targets.length))
const economyLabel=(value:string)=>(({ACTION:'动作',BONUS_ACTION:'附赠动作',REACTION:'反应'} as Record<string,string>)[value]||value)
const shapeLabel=(value:string)=>(({CIRCLE:'圆形',CONE:'锥形',LINE:'直线',CUBE:'方形',NONE:'单目标'} as Record<string,string>)[value]||value)
const deploymentCombatant=computed(()=>state.value?.combatants.find(c=>c.id===deploymentId.value))
const rangeLabel=(a:CombatAction)=>a.rangeFt===a.maxRangeFt?`${a.rangeFt} 尺`:`${a.rangeFt}/${a.maxRangeFt} 尺`

const filteredMonsters=computed(()=>{const q=monsterSearch.value.trim().toLowerCase();return q?monsters.value.filter(m=>`${m.fullName} ${m.monster_type} ${m.recommended_level}`.toLowerCase().includes(q)):monsters.value})
const monsterCounts=computed(()=>selectedMonsterKeys.value.reduce<Record<string,number>>((all,key)=>{all[key]=(all[key]||0)+1;return all},{}))
const orderedCombatants=computed(()=>[...(state.value?.combatants||[])].sort((a,b)=>a.alive!==b.alive?(a.alive?-1:1):(a.order>=0&&b.order>=0?a.order-b.order:b.initiative-a.initiative)))
const activeCombatant=computed(()=>state.value?.combatants.find(c=>c.active)||null)
const visibleActions = computed(() => state.value?.actions.filter(action => {
 const actionCategory = action.category === 'THROW' && !action.itemKey ? 'ATTACK' : action.category
 return actionCategory === category.value
}) ?? [])
const selectedAction=computed(()=>state.value?.actions.find(a=>a.index===selectedOptionIndex.value)||null)
const selectedTarget=computed(()=>selectedAction.value?.targets.find(t=>t.id===selectedTargetId.value)||null)
const formatBonus=(v:number)=>v>=0?`+${v}`:`${v}`
const hpPercent=(c:BattleToken)=>Math.max(0,Math.min(100,c.currentHp/c.maxHp*100))
const apiMessage=(e:unknown)=>e instanceof ApiError?e.message:e instanceof Error?e.message:'操作失败'

watch(turnPopover, panel => {
 if (panel && !panel.matches(':popover-open')) panel.showPopover()
})
watch(() => activeCombatant.value?.id, () => { turnMenuOpen.value = false })

function closeTurnMenu(restoreFocus = false) {
 turnMenuOpen.value = false
 if (restoreFocus) battlefield.value?.querySelector<HTMLElement>('.token.active')?.focus({ preventScroll: true })
}

function chooseMovement(restoreFocus = true) {
 resetActionSelection()
 mapMode.value = 'move'
 if (restoreFocus) closeTurnMenu(true)
}

function applySetup(data:{characters:SetupCharacter[];monsters:SetupMonster[];adventureSessions?:AdventureSession[];locations?:AdventureLocation[]}){
 characters.value=data.characters;monsters.value=data.monsters;adventureSessions.value=data.adventureSessions??[];locations.value=data.locations??[]
 adventureSessionId.value=adventureSessions.value.find(item=>item.status==='ACTIVE')?.id??null
 combatLocationId.value=locations.value.find(item=>item.is_current)?.id??null
}
async function loadSetup(){loading.value=true;error.value='';try{applySetup(await CombatAPI.getSetup())}catch(e){error.value=apiMessage(e)}finally{loading.value=false}}
function applyRestored(current:CombatState|null){
 state.value=current;resetActionSelection();areaPreview.value=null
 mapMode.value=current?.started?'inspect':'place'
 deploymentId.value=current?.combatants[0]?.id||'';avatarId.value=deploymentId.value
 deploymentSpeed.value=deploymentCombatant.value?.speedFt??30;deploymentSize.value=deploymentCombatant.value?.size??1
 receiverId.value=current?.settlement.receivers[0]?.id||null
}
async function restoreCombat(){
 loading.value=true;error.value='';recoveryFailed.value=false
 try{const [data,current]=await Promise.all([CombatAPI.getSetup(),CombatAPI.getCurrent()]);applySetup(data);applyRestored(current);restored.value=!!current}
 catch(e){error.value=apiMessage(e);recoveryFailed.value=true}
 finally{loading.value=false}
}
function addMonster(key:string){selectedMonsterKeys.value.push(key)}
function removeMonster(key:string){const i=selectedMonsterKeys.value.lastIndexOf(key);if(i>=0)selectedMonsterKeys.value.splice(i,1)}
function resetActionSelection(){category.value=null;selectedOptionIndex.value=null;selectedTargetId.value='';areaPreview.value=null;turnMenuOpen.value=false}
async function startCombat(){if(!selectedCharacterIds.value.length||!selectedMonsterKeys.value.length){error.value='请至少选择一名可战斗角色和一个敌人。';return}busy.value=true;error.value='';try{state.value=await CombatAPI.start(selectedCharacterIds.value,selectedMonsterKeys.value,adventureSessionId.value,combatLocationId.value);resetActionSelection();mapMode.value='place';selectMapToken(state.value!.combatants[0]?.id||'');avatarId.value=deploymentId.value;receiverId.value=state.value?.settlement.receivers[0]?.id||null}catch(e){error.value=apiMessage(e)}finally{busy.value=false}}
function chooseCategory(next:ActionCategory|null){category.value=next;selectedOptionIndex.value=null;selectedTargetId.value='';areaPreview.value=null;mapMode.value='inspect'}
function chooseAction(action:CombatAction){
 selectedOptionIndex.value=action.index
 selectedTargetId.value=action.targets[0]?.id||''
 areaPreview.value=null
 mapMode.value='attack'
 if (action.areaShape !== 'NONE') closeTurnMenu(true)
}
async function executeAction(){
 if(!state.value||!selectedAction.value||!canExecute.value)return;busy.value=true;error.value=''
 try{const preview=areaPreview.value;state.value=await CombatAPI.act(state.value.sessionId,selectedAction.value.index,selectedTargetId.value,selectedAction.value.itemKey,preview?{x:preview.aimX,y:preview.aimY}:undefined,activeCombatant.value?.id);resetActionSelection();mapMode.value='inspect'}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function skipTurn(){if(!state.value)return;busy.value=true;error.value='';try{state.value=await CombatAPI.skip(state.value.sessionId,activeCombatant.value?.id);resetActionSelection();mapMode.value='inspect'}catch(e){error.value=apiMessage(e)}finally{busy.value=false}}
async function assignLoot(){if(!state.value||receiverId.value==null)return;busy.value=true;error.value='';try{state.value=await CombatAPI.assignLoot(state.value.sessionId,receiverId.value,state.value.settlement.loot.map(i=>i.key))}catch(e){error.value=apiMessage(e)}finally{busy.value=false}}
async function newEncounter(){
 if(!window.confirm('清除当前遭遇存档并新建？尚未归档的战斗记录将随遭遇清除，已归档日志会保留。角色已保存的生命、资源、经验和物品也会保留。'))return
 busy.value=true;error.value=''
 try{await CombatAPI.clearCurrent();applyRestored(null);selectedMonsterKeys.value=[];archiveOpen.value=false;archiveDraft.value=null;archivedEntryId.value=null;recoveryFailed.value=false;restored.value=false;await loadSetup()}
 catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function openArchive(){
 if(!state.value?.finished||busy.value)return
 busy.value=true;error.value=''
 try{const preview=await CombatAPI.archivePreview(state.value.sessionId);archiveDraft.value=preview.entry;archivedEntryId.value=preview.alreadyArchived?preview.entry.id:null;archiveOpen.value=true}
 catch(cause){error.value=apiMessage(cause)}finally{busy.value=false}
}
async function saveArchive(){
 if(!state.value||!archiveDraft.value||busy.value||archivedEntryId.value)return
 busy.value=true;error.value=''
 try{const entry=await CombatAPI.archive(state.value.sessionId,{...archiveDraft.value});archiveDraft.value=entry;archivedEntryId.value=entry.id;notifyAdventureHistoryChanged()}
 catch(cause){error.value=apiMessage(cause)}finally{busy.value=false}
}
function selectMapToken(id:string){
 if(!state.value?.started){deploymentId.value=id;deploymentSpeed.value=deploymentCombatant.value?.speedFt??30;deploymentSize.value=deploymentCombatant.value?.size??1}
 else if(id===activeCombatant.value?.id){turnMenuOpen.value=!turnMenuOpen.value}
 else if(mapMode.value==='attack'&&selectedAction.value?.targets.some(t=>t.id===id)){selectedTargetId.value=id;turnMenuOpen.value=true}
}
function deployDraggedToken(id: string, x: number, y: number) {
 if (!state.value || state.value.started || busy.value || mapMode.value !== 'place') return
 void deploy(x, y, false, id)
}
async function deploy(x: number, y: number, obstacle = false, draggedId?: string) {
 if (!state.value || busy.value) return
 const id = draggedId ?? deploymentId.value
 const useDraft = !obstacle && id === deploymentId.value
 busy.value = true
 error.value = ''
 try {
   state.value = await CombatAPI.deploy(
     state.value.sessionId, id, x, y,
     useDraft ? deploymentSpeed.value : undefined, obstacle,
     useDraft ? deploymentSize.value : undefined, obstacle ? terrainType.value : undefined
   )
   if (draggedId && draggedId === deploymentId.value) selectMapToken('')
 } catch (e) {
   error.value = apiMessage(e)
 } finally {
   busy.value = false
 }
}
async function beginBattle(){
 if(!state.value)return;busy.value=true;error.value=''
 try{state.value=await CombatAPI.begin(state.value.sessionId);mapMode.value='inspect';resetActionSelection()}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function moveToken(x:number,y:number){
 if(!state.value||!activeCombatant.value)return;busy.value=true;error.value=''
 try{state.value=await CombatAPI.move(state.value.sessionId,activeCombatant.value.id,x,y);areaPreview.value=null;const action=selectedAction.value;if(action&&!action.targets.some(t=>t.id===selectedTargetId.value))selectedTargetId.value=action.targets[0]?.id||''}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function aimArea(x:number,y:number){
 if(!state.value||!selectedAction.value||!activeCombatant.value||busy.value)return
 busy.value=true;error.value='';areaPreview.value=null
 const index=selectedAction.value.index,actorId=activeCombatant.value.id
 try{const preview=await CombatAPI.previewArea(state.value.sessionId,index,x,y,actorId);if(selectedAction.value?.index===index&&activeCombatant.value?.id===actorId){areaPreview.value=preview;turnMenuOpen.value=true}}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function tactical(action:string){
 if(!state.value||!activeCombatant.value)return;busy.value=true;error.value=''
 try{state.value=await CombatAPI.tactical(state.value.sessionId,action,activeCombatant.value.id);resetActionSelection();mapMode.value='move'}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function undoMove(){
 if(!state.value||!activeCombatant.value)return;busy.value=true;error.value=''
 try{state.value=await CombatAPI.undoMove(state.value.sessionId,activeCombatant.value.id);areaPreview.value=null;mapMode.value='move'}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function reactToDamage(id:string,accept:boolean){
 if(!state.value)return;busy.value=true;error.value=''
 try{state.value=await CombatAPI.reaction(state.value.sessionId,id,accept);resetActionSelection();mapMode.value='move'}catch(e){error.value=apiMessage(e)}finally{busy.value=false}
}
async function importImage(event:Event,icon:boolean){
 const input=event.target as HTMLInputElement,file=input.files?.[0];if(!file)return
 const assetKey=icon?avatarToken.value?.assetKey:'background'
 if(!assetKey){error.value='请先选择要导入图标的棋子。';input.value='';return}
 artworkBusy.value=true;error.value=''
 try{const image=await readArtwork(file,icon);await saveArtwork(assetKey,image);artwork.value={...artwork.value,[assetKey]:image}}catch(e){error.value=apiMessage(e)}finally{artworkBusy.value=false;input.value=''}
}
async function removeImage(key:string){
 artworkBusy.value=true;error.value=''
 try{await saveArtwork(key,null);const next={...artwork.value};delete next[key];artwork.value=next}catch(e){error.value=apiMessage(e)}finally{artworkBusy.value=false}
}
onMounted(()=>{restoreCombat();loadArtwork().then(images=>{artwork.value=images}).catch(e=>{error.value=apiMessage(e)})})
</script>

<template>
 <section class="workspace-page">
  <header class="workspace-heading"><h1>战斗控制台</h1><div v-if="state" class="save-controls"><button class="btn-secondary" :disabled="busy||loading" @click="restoreCombat">重新载入</button><button class="btn-secondary" :disabled="busy||loading" @click="newEncounter">新建遭遇</button></div></header>
  <p v-if="state&&!loading" class="save-status" role="status">{{restored?'已恢复上次遭遇 · ':''}}每次操作自动保存至本机<span v-if="savedTime"> · 最近保存：{{savedTime}}</span></p>
  <div v-if="error" class="error-banner">{{error}}</div><div v-if="loading" class="loading-card">正在读取角色与怪物图鉴…</div>
  <section v-else-if="recoveryFailed" class="panel recovery-panel"><h2>战斗存档尚未载入</h2><p>原存档会保留，暂时不能开始新的遭遇。请重试，或清除存档后重新开始。</p><button class="btn-secondary" :disabled="busy" @click="restoreCombat">重新载入</button><button class="btn-secondary" :disabled="busy" @click="newEncounter">清除存档并新建</button></section>
  <section v-else-if="!state" class="setup-grid">
   <article class="panel setup-panel"><div class="panel-title"><div><h2>选择战斗角色</h2></div><span>{{selectedCharacterIds.length}} 人</span></div>
    <label v-for="c in characters" :key="c.id" :class="['selection-card',{selected:selectedCharacterIds.includes(c.id),disabled:!c.available}]"><input v-model="selectedCharacterIds" type="checkbox" :value="c.id" :disabled="!c.available"><div class="selection-main"><strong>{{c.name}}</strong><span>{{c.className}} · Lv.{{c.level}}</span></div><div class="selection-stats"><span>HP {{c.currentHp}}/{{c.maxHp}}</span><span>AC {{c.armorClass}}</span><span>先攻 {{formatBonus(c.initiativeModifier)}}</span></div><span v-if="!c.available" class="down-tag">已倒下</span></label>
    <p v-if="!characters.length" class="empty">还没有角色存档，请先创建角色。</p>
   </article>
   <article class="panel setup-panel"><div class="panel-title"><div><h2>选择敌人</h2></div><span>{{selectedMonsterKeys.length}} 只</span></div><input v-model="monsterSearch" class="search" aria-label="搜索怪物" placeholder="搜索名称、类型或推荐等级…">
    <div class="monster-list"><div v-for="m in filteredMonsters" :key="m.key" class="monster-card"><div class="selection-main"><strong>{{m.fullName}}</strong><span>{{m.monster_type}} · 推荐 Lv.{{m.recommended_level}} · {{m.xp_reward}} XP</span></div><div class="selection-stats"><span>AC {{m.armor_class}}</span><span>HP {{m.hpRange}}</span></div><div class="counter"><button :aria-label="`减少${m.fullName}`" :disabled="!monsterCounts[m.key]" @click="removeMonster(m.key)">−</button><b>{{monsterCounts[m.key]||0}}</b><button :aria-label="`增加${m.fullName}`" @click="addMonster(m.key)">＋</button></div></div></div>
   </article>
   <div class="encounter-context"><label>关联场次<select v-model="adventureSessionId"><option :value="null">未关联场次</option><option v-for="session in adventureSessions" :key="session.id" :value="session.id">{{session.title}}{{session.status === 'ACTIVE' ? ' · 进行中' : ''}}</option></select></label><label>战斗地点<select v-model="combatLocationId"><option :value="null">未指定地点</option><option v-for="location in locations" :key="location.id" :value="location.id">{{location.name}}</option></select></label></div><div class="start-bar"><div><strong>遭遇配置</strong><span>{{selectedCharacterIds.length}} 名角色 vs {{selectedMonsterKeys.length}} 名敌人</span></div><button class="btn-primary" :disabled="busy||!selectedCharacterIds.length||!selectedMonsterKeys.length" @click="startCombat">{{busy?'正在计算…':'进入战场摆位'}}</button></div>
  </section>
  <template v-else>
   <section class="battle-layout">
    <aside class="panel initiative-panel"><div class="panel-title compact"><div><h2>行动顺序</h2></div><span>第 {{state.round}} 轮</span></div>
     <div v-for="c in orderedCombatants" :key="c.id" :class="['combatant',c.side.toLowerCase(),{active:c.active,dead:!c.alive}]"><div class="initiative-score"><img v-if="artwork[c.assetKey]" :src="artwork[c.assetKey]" :alt="c.name"><span>{{c.initiative}}</span></div><div class="combatant-body"><div class="combatant-name"><strong>{{c.name}}</strong><span>{{c.side==='PLAYER'?'角色':'敌人'}}</span></div><div class="hp-track"><i :style="{width:hpPercent(c)+'%'}"></i></div><div class="combatant-meta"><span>HP {{c.currentHp}}/{{c.maxHp}}</span><span>AC {{c.armorClass}}</span><span>{{c.size}}×{{c.size}} 格 · {{c.reactionAvailable?'反应可用':'反应已用'}}</span><span>d20 {{c.initiativeRoll}} {{formatBonus(c.initiativeModifier)}}</span></div><small v-if="!c.alive">已倒下</small><small v-else-if="c.status!=='无'" :class="{ 'text-danger': c.status.includes('倒地') }">{{c.status}}</small></div></div>
    </aside>
    <section class="battle-main">
     <article ref="battlefield" class="panel map-panel">
      <div class="panel-title compact map-title">
       <h2>{{state.started?'战场':'战前摆位'}}</h2>
       <span>{{state.grid.width}} × {{state.grid.height}} · 每格 {{state.grid.feetPerCell}} 尺</span>
       <button v-if="!state.started" class="btn-danger begin-battle" :disabled="busy" @click="beginBattle">完成摆位，开始战斗</button>
      </div>
      <template v-if="!state.started">
       <div class="map-toolbar"><button :class="['btn-secondary',{chosen:mapMode==='place'}]" :disabled="busy" @click="mapMode='place'">摆放棋子</button><button :class="['btn-secondary',{chosen:mapMode==='obstacle'}]" :disabled="busy" @click="mapMode='obstacle'">绘制地形</button><select v-model="terrainType" aria-label="地形类型"><option value="WALL">墙壁 / 全掩护</option><option value="DIFFICULT">困难地形</option><option value="HALF_COVER">半掩体</option><option value="THREE_QUARTERS_COVER">四分之三掩体</option></select></div>
       <div class="deployment-controls">
        <label>当前棋子<select v-model="deploymentId" :disabled="busy" @change="selectMapToken(deploymentId)">
         <option value="">选择棋子</option>
         <option v-for="c in state.combatants" :key="c.id" :value="c.id">{{c.id.replace('combatant-','#')}} {{c.name}}</option>
        </select></label>
        <label>速度（尺）<input v-model.number="deploymentSpeed" :disabled="busy||!deploymentCombatant" type="number" min="0" max="120" step="5"></label>
        <label>占格边长<select v-model.number="deploymentSize" :disabled="busy||!deploymentCombatant">
         <option v-for="size in [1,2,3,4]" :key="size" :value="size">{{size}}×{{size}}</option>
        </select></label>
        <button class="btn-secondary" :disabled="busy||!deploymentCombatant" @click="deploymentCombatant&&deploy(deploymentCombatant.x,deploymentCombatant.y)">应用速度 / 体型</button>
       </div>
       <p class="map-note">按住棋子拖到目标格并松手，或先选中棋子再点击空格。绘制地形时再次点击同类地形可移除。</p>
      </template>
      <div v-else-if="!state.finished" class="turn-summary">
       <p role="status">当前行动：<strong>{{activeCombatant?.name}}</strong> · 剩余 {{activeCombatant?.remainingMovementFt??0}} 尺 · {{state.actionSpent?'动作已用':'动作可用'}} · {{state.bonusActionSpent?'附赠已用':'附赠可用'}}{{state.disengaged?' · 已撤离':''}}</p>
       <p class="map-note">{{ mapMode === 'move' ? '点击高亮格或拖动当前棋子移动。' : mapMode === 'attack' ? '点击战场选择目标或落点。' : '点击金边棋子打开行动菜单，或直接拖动它移动。' }}</p>
       <div v-if="state.reactions.length" class="reaction-panel">
        <div v-for="offer in state.reactions" :key="offer.id">
         <strong>{{offer.reactorName}} 可对 {{offer.targetName}} 使用 {{offer.name}}</strong>
         <button class="btn-primary" :disabled="busy" @click="reactToDamage(offer.id,true)">使用反应</button>
         <button class="btn-secondary" :disabled="busy" @click="reactToDamage(offer.id,false)">放弃反应</button>
        </div>
       </div>
      </div>
      <div class="artwork-controls">
       <label class="btn-secondary file-btn">导入地图图片<input type="file" accept="image/png,image/jpeg,image/webp" aria-label="导入地图图片" :disabled="artworkBusy" @change="importImage($event,false)"></label>
       <button class="btn-secondary" :disabled="artworkBusy||!artwork.background" @click="removeImage('background')">移除地图图片</button>
       <label>网格透明度<input v-model.number="gridOpacity" type="range" min="0" max=".8" step=".05"></label>
       <select v-model="avatarId" aria-label="导入图标的棋子"><option v-for="c in state.combatants" :key="c.id" :value="c.id">{{c.name}}</option></select>
       <label class="btn-secondary file-btn">导入角色图标<input type="file" accept="image/png,image/jpeg,image/webp" aria-label="导入角色图标" :disabled="artworkBusy||!avatarToken" @change="importImage($event,true)"></label>
       <button class="btn-secondary" :disabled="artworkBusy||!avatarToken||!artwork[avatarToken.assetKey]" @click="avatarToken&&removeImage(avatarToken.assetKey)">移除图标</button>
      </div>
      <BattleMap
       :grid="state.grid" :combatants="state.combatants" :started="state.started" :finished="state.finished"
       :busy="busy||!!state.reactions.length" :mode="mapMode" :deployment-size="deploymentSize" :action-menu-open="turnMenuOpen"
       :selected-id="!state.started?deploymentId:mapMode==='attack'?selectedTargetId:''"
       :target-ids="areaPreview?.targets.map(t=>t.id)||selectedAction?.targets.map(t=>t.id)||[]" :range-ft="selectedAction?.maxRangeFt??0"
       :area-mode="mapMode==='attack'&&!!selectedAction&&selectedAction.areaShape!=='NONE'" :area-cells="areaPreview?.cells||[]"
       :background="artwork.background||''" :icons="artwork" :grid-opacity="gridOpacity"
       @select="selectMapToken" @place="(x,y)=>deploy(x,y)" @deploy="deployDraggedToken" @obstacle="(x,y)=>deploy(x,y,true)" @move-intent="chooseMovement(false)" @move="moveToken" @aim="aimArea"
      >
       <section v-if="turnMenuOpen && activeCombatant && !state.finished && !state.reactions.length"
        ref="turnPopover" class="turn-popover" popover="manual" role="dialog" aria-label="棋子行动" @keydown.esc.stop="closeTurnMenu(true)">
        <header class="turn-heading">
         <div><h2>{{activeCombatant.name}}</h2><small>剩余移动 {{activeCombatant.remainingMovementFt}} 尺</small></div>
         <button type="button" class="close-turn" aria-label="收起行动菜单" autofocus @click="closeTurnMenu(true)">×</button>
        </header>
        <p v-if="error" class="error-banner" role="alert">{{error}}</p>
        <div class="category-tabs">
         <button :disabled="busy" :aria-pressed="mapMode==='move'" @click="chooseMovement()">移动</button>
         <button v-for="(label,key) in categoryLabels" :key="key" :disabled="busy"
          :aria-pressed="category===key" @click="chooseCategory(key)">{{label}}</button>
        </div>
        <div v-if="category && !selectedAction" class="action-grid">
         <button v-for="a in visibleActions" :key="a.index" :disabled="busy||!a.available" :title="a.unavailableReason"
          class="action-card" @click="chooseAction(a)">
          <strong>{{a.name}}</strong><span>{{a.description}}</span>
          <small>{{economyLabel(a.economy)}} · 射程 {{rangeLabel(a)}}{{a.areaShape!=='NONE' ? ' · ' + shapeLabel(a.areaShape) + ' ' + a.areaSizeFt + ' 尺' : ''}}</small>
          <small v-if="!a.available">{{a.unavailableReason}}</small>
         </button>
         <p v-if="!visibleActions.length" class="empty">{{ category === 'THROW' ? '背包中没有可投掷的物品。' : '当前行动者没有此类行动。' }}</p>
        </div>
        <div v-else-if="selectedAction" class="action-detail">
         <strong>{{selectedAction.name}}</strong>
         <p class="map-note">{{selectedAction.resolution==='ATTACK_ROLL' ? '命中 ' + formatBonus(selectedAction.attackBonus) + ' · ' : selectedAction.resolution==='SAVE_DC' ? selectedAction.saveAbility + ' DC ' + selectedAction.saveDc + ' · ' : ''}}{{selectedAction.effectType}} {{selectedAction.effectRange}} · {{economyLabel(selectedAction.economy)}}</p>
         <template v-if="selectedAction.areaShape==='NONE'">
          <label>选择目标<select v-model="selectedTargetId" :disabled="busy || !selectedAction.targets.length">
           <option v-for="target in selectedAction.targets" :key="target.id" :value="target.id">{{target.name}} · {{target.distanceFt}} 尺 · {{target.cover}}</option>
          </select></label>
          <p v-if="selectedTarget" class="map-note">命中 / 生效概率 {{selectedTarget.hitChance}}%</p>
          <p v-else class="map-note">射程内没有有效目标。</p>
          <div class="turn-buttons">
           <button :disabled="busy || !selectedAction.targets.length" @click="closeTurnMenu(true)">在地图选目标</button>
           <button class="btn-primary" :disabled="busy||!canExecute" @click="executeAction">执行行动</button>
          </div>
         </template>
         <template v-else>
          <p class="map-note">{{shapeLabel(selectedAction.areaShape)}} {{selectedAction.areaSizeFt}} 尺 · 点击地图选择{{selectedAction.areaFromSelf?'方向':selectedAction.areaShape==='CUBE'?'方形左上角':'落点'}}</p>
          <ul v-if="areaPreview" class="area-targets">
           <li v-for="target in areaPreview.targets" :key="target.id" :class="{ 'text-danger': target.friendly }">{{target.name}} · {{target.friendly?'友军（可能误伤）':'敌人'}} · {{target.cover}}</li>
           <li v-if="!areaPreview.targets.length">范围内没有有效目标。</li>
          </ul>
          <div class="turn-buttons">
           <button :disabled="busy" @click="closeTurnMenu(true)">在地图选落点</button>
           <button v-if="!selectedAction.areaFromSelf && selectedAction.areaShape==='CIRCLE'" :disabled="busy" @click="aimArea(activeCombatant.x,activeCombatant.y)">以自身为落点</button>
           <button class="btn-primary" :disabled="busy||!canExecute" @click="executeAction">执行范围行动</button>
          </div>
         </template>
        </div>
        <div class="turn-buttons">
         <button class="btn-secondary" :disabled="busy||state.actionSpent" @click="tactical('DISENGAGE')">撤离</button>
         <button class="btn-secondary" :disabled="busy||state.actionSpent" @click="tactical('DASH')">疾走</button>
         <button v-if="activeCombatant.status.includes('倒地')" class="btn-secondary" :disabled="busy" @click="tactical('STAND')">起身</button>
         <button v-if="state.canUndoMove" class="btn-secondary" :disabled="busy" @click="undoMove">撤销移动</button>
         <button class="btn-danger end-turn" :disabled="busy" @click="skipTurn">结束当前回合</button>
        </div>
       </section>
      </BattleMap>
      <p class="map-note">绿色：可移动；金色：射程；橙色：受影响范围。墙壁阻挡视线，掩体增加 AC 与敏捷豁免；困难地形增加移动消耗。</p>
      <details class="map-note"><summary>第二版规则与图片说明</summary><p>动作、附赠动作分别计数；借机攻击自动消耗反应，受伤反应由 DM 选择。动作、附赠动作、起身或触发反应后不能撤销移动。范围按格子中心判定，包含友军；大型单位任一占格被覆盖即可受影响且只结算一次。图片支持 PNG/JPG/WebP（10 MB 内），保存在本机，地图图片铺满棋盘；原图中的墙壁不会自动识别，请手动绘制。角色图标按角色存档保存，怪物图标按种类共用。背包投掷物暂用 20 尺，自定义远程武器暂用 60/120 尺。</p></details>
     </article>
     <article v-if="state.finished" class="panel settlement"><div :class="['result-seal',{defeat:!state.victory}]">{{state.victory?'胜利':'败北'}}</div><h2>战斗结算</h2><button class="btn-primary" :disabled="busy" @click="openArchive">归档到冒险日志</button><p v-if="state.victory">每名参战角色获得 <strong>{{state.settlement.xpPerCharacter}} XP</strong>，经验已写入角色存档。</p><p v-else>队伍全员倒下，本次遭遇没有经验与掉落结算。</p><template v-if="state.victory"><h3>敌人掉落物</h3><div v-if="state.settlement.loot.length" class="loot-list"><span v-for="(item,i) in state.settlement.loot" :key="item.key+i">◆ {{item.name}}</span></div><p v-else class="empty">没有待分配的掉落物。</p><div v-if="state.settlement.loot.length" class="loot-assign"><select v-model="receiverId"><option v-for="r in state.settlement.receivers" :key="r.id" :value="r.id">{{r.name}}</option></select><button class="btn-primary" :disabled="busy||receiverId==null" @click="assignLoot">全部分配</button></div></template><button class="btn-secondary" @click="newEncounter">返回遭遇配置</button></article>
     <article class="panel log-panel"><div class="panel-title compact"><div><h2>战斗记录</h2></div></div><div class="logs"><p v-if="!state.logs.length" class="empty">先攻已完成，等待第一个行动。</p><div v-for="(log,i) in [...state.logs].reverse()" :key="i"><b>#{{state.logs.length-i}}</b><pre>{{log}}</pre></div></div></article>
    </section>
   </section>
  </template>
  <div v-if="archiveOpen && archiveDraft" class="archive-backdrop" role="dialog" aria-label="战斗归档" @mousedown.self="!busy && (archiveOpen=false)">
   <form class="archive-editor" @submit.prevent="saveArchive">
    <p v-if="error" class="error-banner" role="alert">{{error}}</p>
    <h2>{{archivedEntryId?'战斗已归档':'归档战斗记录'}}</h2>
    <p>确认记录后保存为一条见闻。归档不会再次结算经验或分配掉落。</p>
    <fieldset :disabled="busy || !!archivedEntryId">
     <label>标题<input v-model="archiveDraft.title" maxlength="120" required></label>
     <label>关联场次<select v-model="archiveDraft.adventure_session_id"><option :value="null">未关联场次</option><option v-for="session in adventureSessions" :key="session.id" :value="session.id">{{session.title}}</option></select></label>
     <label>关联地点<select v-model="archiveDraft.location_id"><option :value="null">未指定地点</option><option v-for="location in locations" :key="location.id" :value="location.id">{{location.name}}</option></select></label>
     <label>归档内容<textarea v-model="archiveDraft.content" rows="12" maxlength="200000" required></textarea></label>
    </fieldset>
    <div class="archive-actions"><button type="button" class="btn-secondary" :disabled="busy" @click="archiveOpen=false">关闭</button><router-link v-if="archivedEntryId" :to="{path:'/journal',query:{entryId:archivedEntryId}}">查看 / 编辑已归档日志 ↗</router-link><button v-else class="btn-primary" :disabled="busy || !archiveDraft.title.trim() || !archiveDraft.content.trim()">{{busy?'保存中…':'确认归档'}}</button></div>
   </form>
  </div>
 </section>
</template>

<style scoped>
.encounter-context { grid-column: 1/-1; display: flex; gap: 15px; padding: 14px 18px; border-radius: 8px; background: var(--surface-panel); flex-wrap: wrap; }
.encounter-context label { font-size: 12px; color: var(--text-secondary); }
.encounter-context select { display: block; margin-top: 7px; padding: 8px; border-radius: 5px; }
.archive-backdrop { position: fixed; inset: 0; z-index: 130; display: grid; place-items: center; padding: 24px; background: var(--scrim); }
.archive-editor { width: min(800px,100%); max-height: calc(100vh - 70px); overflow: auto; padding: 24px; background: var(--surface-overlay); color: var(--text-primary); border-radius: 10px; }
.archive-editor h2 { margin-bottom: 10px; }
.archive-editor p { font-size: 12px; color: var(--text-secondary); line-height: 1.8; }
.archive-editor label { display: block; font-size: 12px; margin-top: 15px; }
.archive-editor input, .archive-editor select, .archive-editor textarea { display: block; width: 100%; margin-top: 6px; padding: 10px; border-radius: 6px; font: inherit; }
.archive-editor textarea { resize: vertical; line-height: 1.8; }
.archive-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 18px; }
.archive-actions a { color: var(--antique-gold); font-size: 12px; }
.save-controls { display: flex; gap: 8px; flex-wrap: wrap; }
.save-status { margin: 0 0 16px; padding: 10px 14px; color: var(--text-success); background: var(--surface-success); border-radius: 7px; font-size: 12px; }
.recovery-panel p { color: var(--text-secondary); line-height: 1.8; }
.recovery-panel button { margin: 10px 10px 0 0; }
.artwork-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin: 12px 0; }
.artwork-controls select { width: 180px; }
.artwork-controls label { font-size: .75rem; color: var(--text-secondary); }
.artwork-controls label input[type=range] { display: block; width: 110px; }
.file-btn { position: relative; overflow: hidden; cursor: pointer; font-weight: 700; }
.file-btn input[type=file] { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; }
.reaction-panel { padding: 12px 16px; margin: 14px 0; background: var(--surface-hover); border-radius: 8px; color: var(--text-secondary); font-size: .82rem; }
.reaction-panel>div { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.map-toolbar select { width: 180px; }
.initiative-score img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.initiative-score span { position: relative; z-index: 1; }
.initiative-score:has(img) span { align-self: end; background: var(--scrim); padding: 0 3px; border-radius: 4px; font-size: .75rem; }
.map-panel { padding: 18px; min-width: 0; }
.map-title { justify-content: flex-start; gap: 10px; flex-wrap: wrap; }
.map-title span { font-size: .8rem; }
.begin-battle { margin-left: auto; }
.map-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin: 12px 0; }
.btn-secondary.chosen { color: var(--antique-gold); --control-bg: var(--surface-selected); }
.map-note { font-size: .76rem; line-height: 1.6; color: var(--text-secondary); margin: 10px 0; }
.map-note summary { cursor: pointer; }
.deployment-controls { display: flex; gap: 10px; align-items: end; flex-wrap: wrap; }
.deployment-controls label { font-size: .75rem; color: var(--text-secondary); }
.deployment-controls label:first-child { flex: 1; min-width: 170px; }
.deployment-controls input { display: block; box-sizing: border-box; width: 90px; padding: 10px; color: var(--text-primary); border-radius: 7px; }
.action-card:disabled { opacity: .45; cursor: default; }
.panel { background: var(--surface-panel); border-radius: 14px; }
.error-banner, .loading-card { padding: 14px 18px; margin-bottom: 18px; border-radius: 8px; }
.error-banner { color: var(--text-danger); background: var(--surface-danger); }
.loading-card { color: var(--text-secondary); background: var(--surface-panel); }
.setup-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.setup-panel { padding: 20px; min-width: 0; }
.panel-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; color: var(--text-secondary); }
.panel-title h2 { margin: 0; color: var(--antique-gold); font: 600 1.15rem var(--font-heading); }
.selection-card, .monster-card { position: relative; display: flex; align-items: center; gap: 14px; padding: 13px; margin: 8px 0; border-radius: 9px; background: var(--surface-card); transition: background-color var(--transition-fast); }
.selection-card { cursor: pointer; }
.selection-card.disabled { background: var(--surface-danger); cursor: not-allowed; }
.selection-card input { accent-color: var(--antique-gold); }
.selection-main { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 4px; }
.selection-main strong { color: var(--text-primary); }
.selection-main span, .selection-stats { color: var(--text-secondary); font-size: .78rem; }
.selection-stats { display: flex; gap: 10px; flex-wrap: wrap; }
.down-tag { color: var(--text-danger); font-size: .72rem; }
.search, select { box-sizing: border-box; width: 100%; padding: 10px 12px; color: var(--text-primary); border-radius: 7px; }
.monster-list { max-height: 410px; overflow: auto; padding-right: 4px; }
.counter { display: flex; align-items: center; gap: 8px; }
.counter button { width: 28px; height: 28px; border-radius: 5px; cursor: pointer; padding: 0; }
.counter button:disabled { opacity: .3; }
.start-bar { grid-column: 1/-1; display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: var(--surface-panel); border-radius: 12px; gap: 12px; }
.start-bar div { display: flex; flex-direction: column; gap: 4px; }
.start-bar span { color: var(--text-secondary); font-size: .82rem; }
.btn-primary, .btn-secondary, .btn-danger { padding: 10px 18px; }
.battle-layout { display: grid; grid-template-columns: minmax(240px,25%) minmax(0,1fr); gap: 18px; align-items: start; }
.initiative-panel, .settlement, .log-panel { padding: 18px; }
.compact { margin-bottom: 10px; }
.combatant { display: flex; gap: 12px; padding: 12px; margin: 7px 0; background: var(--surface-card); border-radius: 6px; }
.combatant.active { background: var(--surface-selected); }
.combatant.dead { background: var(--surface-danger); }
.initiative-score { position: relative; overflow: hidden; display: grid; place-items: center; width: 44px; height: 44px; flex: 0 0 44px; border-radius: 50%; background: var(--surface-inset); color: var(--antique-gold); font: 700 1.2rem var(--font-heading); }
.combatant-body { flex: 1; min-width: 0; }
.combatant-name { display: flex; justify-content: space-between; gap: 8px; }
.combatant-name strong { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.combatant-name span { color: var(--text-dim); font-size: .7rem; }
.hp-track { height: 5px; margin: 8px 0; overflow: hidden; border-radius: 5px; background: var(--surface-inset); }
.hp-track i { display: block; height: 100%; background: var(--hp-enemy); transition: width .3s; }
.player .hp-track i { background: var(--hp-party); }
.combatant-meta { display: flex; flex-wrap: wrap; gap: 10px; color: var(--text-secondary); font-size: .72rem; }
.combatant small { display: block; margin-top: 6px; color: var(--antique-gold); }
.battle-main { display: grid; gap: 18px; min-width: 0; }
.turn-summary { padding: 10px 12px; border-radius: var(--radius-sm); background: var(--surface-card); }
.turn-summary strong { color: var(--antique-gold); }
.turn-summary .map-note { margin: 4px 0 0; }
.turn-popover {
  position: fixed;
  position-anchor: --active-battle-token;
  position-area: right span-bottom;
  position-try-fallbacks: left span-bottom, right span-top, left span-top, bottom span-all, top span-all;
  width: min(340px, calc(100vw - 24px));
  max-height: min(480px, 65vh);
  overflow: auto;
  margin: 8px;
  padding: 14px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--surface-overlay);
  color: var(--text-primary);
  box-shadow: var(--overlay-shadow);
}
.turn-heading { display: flex; justify-content: space-between; align-items: start; gap: 12px; }
.turn-heading small { color: var(--text-secondary); }
.close-turn { padding: 2px 9px; font-size: 1rem; }
.category-tabs { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 6px; margin: 12px 0; }
.category-tabs button { padding: 8px 4px; }
.category-tabs button[aria-pressed="true"] { color: var(--antique-gold); --control-bg: var(--surface-selected); }
.action-grid { display: grid; gap: 6px; margin-bottom: 12px; }
.action-card { display: grid; gap: 4px; padding: 10px; text-align: left; --control-bg: var(--surface-card); --control-hover: var(--surface-hover); }
.action-card span, .action-card small { color: var(--text-secondary); font-size: .75rem; font-weight: 400; }
.action-detail { padding: 10px; margin-bottom: 12px; background: var(--surface-inset); border-radius: var(--radius-sm); }
.action-detail label { color: var(--text-secondary); font-size: .8rem; }
.action-detail select { display: block; margin-top: 4px; }
.area-targets { padding-left: 18px; margin-bottom: 10px; font-size: .8rem; }
.turn-buttons { display: flex; flex-wrap: wrap; gap: 6px; }
.turn-buttons button { padding: 7px 10px; font-size: .8rem; }
.end-turn { margin-left: auto; }
.settlement { text-align: center; }
.result-seal { display: grid; place-items: center; width: 84px; height: 84px; margin: 4px auto 14px; border-radius: var(--radius-md); color: var(--antique-gold); font: 800 1.35rem var(--font-heading); background: var(--surface-selected); }
.result-seal.defeat { color: var(--text-danger); background: var(--surface-danger); }
.settlement h2 { margin: 0; }
.settlement p { color: var(--text-secondary); }
.settlement p strong { color: var(--antique-gold); }
.settlement h3 { margin: 22px 0 10px; color: var(--antique-gold); }
.loot-list { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }
.loot-list span { padding: 7px 10px; color: var(--text-primary); background: var(--surface-card); border-radius: 5px; }
.loot-assign { display: flex; gap: 8px; max-width: 430px; margin: 16px auto; }
.settlement>.btn-secondary { margin-top: 16px; }
.logs { max-height: 280px; overflow: auto; display: grid; gap: 4px; }
.logs>div { display: grid; grid-template-columns: 36px 1fr; gap: 8px; padding: 8px; background: var(--surface-card); border-radius: var(--radius-sm); }
.logs b { color: var(--antique-gold); font-size: .72rem; }
.logs pre { margin: 0; white-space: pre-wrap; color: var(--text-secondary); font: .8rem/1.55 var(--font-body); }
.empty { color: var(--text-dim); font-style: italic; }
.selection-card:not(.disabled):hover { background: var(--surface-hover); }
.selection-card.selected { background: var(--surface-selected); }
.combatant.dead small, .combatant small.text-danger { color: var(--text-danger); }
.file-btn:hover { background: var(--button-hover); }
.file-btn:focus-within { outline: 1px solid var(--focus-gold); outline-offset: 1px; }
.recovery-panel { padding: var(--space-lg); }
@media (max-width:960px) {
  .setup-grid, .battle-layout { grid-template-columns: 1fr; }
  .start-bar { grid-column: 1; }
}
@media (max-width:600px) {
  .selection-card, .monster-card { flex-wrap: wrap; }
  .start-bar { align-items: stretch; flex-direction: column; }
  .turn-popover { position-area: bottom span-all; position-try-fallbacks: top span-all; }
}
</style>
