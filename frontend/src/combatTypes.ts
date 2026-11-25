export interface Cell { x:number; y:number }
export type Terrain = 'WALL'|'DIFFICULT'|'HALF_COVER'|'THREE_QUARTERS_COVER'
export type MapMode = 'inspect'|'move'|'attack'|'place'|'obstacle'
export interface GridState {
  width:number; height:number; feetPerCell:number
  obstacles:Cell[]; terrain:Array<Cell & {type:Terrain}>; moves:Array<Cell & {costFt:number}>
}
export interface BattleToken {
  id:string; name:string; side:'PLAYER'|'ENEMY'; armorClass:number; currentHp:number; maxHp:number
  initiativeRoll:number; initiativeModifier:number; initiative:number; alive:boolean; active:boolean; order:number
  status:string; x:number; y:number; size:number; speedFt:number; remainingMovementFt:number
  reactionAvailable:boolean; assetKey:string
}
export interface TargetPreview { id:string; name:string; hitChance:number; distanceFt:number; cover:string }
export interface CombatAction {
  index:number; itemKey:string; name:string; description:string; category:'ATTACK'|'SPELL'|'THROW'
  resolution:string; attackBonus:number; saveDc:number; saveAbility:string; effectRange:string; effectType:string
  rangeFt:number; maxRangeFt:number; ranged:boolean; economy:'ACTION'|'BONUS_ACTION'|'REACTION'
  areaShape:'NONE'|'CIRCLE'|'CONE'|'LINE'|'CUBE'; areaSizeFt:number; areaFromSelf:boolean
  available:boolean; unavailableReason:string; targets:TargetPreview[]
}
export interface AreaPreview { aimX:number; aimY:number; cells:Cell[]; targets:Array<{id:string;name:string;friendly:boolean;cover:string}> }
export interface CombatState {
  savedAt:string
  adventureSessionId:number|null; locationId:number|null
  sessionId:string; started:boolean; actionSpent:boolean; bonusActionSpent:boolean; disengaged:boolean
  canUndoMove:boolean; finished:boolean; victory:boolean; round:number; latestLog:string
  combatants:BattleToken[]; actions:CombatAction[]; logs:string[]; grid:GridState
  reactions:Array<{id:string;name:string;reactorName:string;targetName:string}>
  settlement:{xpPerCharacter:number;loot:Array<{key:string;name:string}>;receivers:Array<{id:number;name:string}>}
}
