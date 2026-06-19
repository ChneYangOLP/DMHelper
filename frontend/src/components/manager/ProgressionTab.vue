<script setup lang="ts">
import { ref, computed } from 'vue'
import { CharacterAPI } from '../../api'

const props = defineProps<{ character: any }>()
const emit = defineEmits(['refresh'])

const isResting = ref(false)

// XP thresholds matching Dnd5e_Progression.java
const XP_TABLE = [0, 300, 900, 2700, 6500, 14000, 23000, 34000, 48000, 64000, 85000, 100000, 120000, 140000, 165000, 195000, 225000, 265000, 305000, 355000]

const currentLevel = computed(() => props.character.job?.current_level || 1)
const isMaxLevel = computed(() => currentLevel.value >= 20)

const currentLevelXp = computed(() => {
  const lv = currentLevel.value
  if (lv <= 1) return 0
  if (lv > 20) return XP_TABLE[19]
  return XP_TABLE[lv - 1]
})

const nextLevelXp = computed(() => {
  if (isMaxLevel.value) return -1
  return XP_TABLE[currentLevel.value]
})

const xpBarPercent = computed(() => {
  if (isMaxLevel.value) return 100
  const current = props.character.experience_points || 0
  const start = currentLevelXp.value
  const end = nextLevelXp.value
  if (end <= start) return 100
  return Math.min(100, Math.max(0, ((current - start) / (end - start)) * 100))
})

const canLevelUp = computed(() => {
  return props.character.can_level_up === true
})

const addXp = async () => {
  const amt = prompt('请输入要增加的经验值数量:', '100')
  if (!amt) return
  try {
    await CharacterAPI.addXp(props.character.database_id, parseInt(amt), '手动添加')
    alert('经验增加成功！')
    emit('refresh')
  } catch(e) {
    alert('经验增加失败')
  }
}

const performRest = async (type: string) => {
  if (!confirm(`确定要进行${type === 'LONG' ? '长休' : '短休'}吗？`)) return
  isResting.value = true
  try {
    await CharacterAPI.rest(props.character.database_id, type)
    alert('休息完成，状态已恢复')
    emit('refresh')
  } catch(e) {
    alert('操作失败')
  } finally {
    isResting.value = false
  }
}

const useSecondWind = async () => {
  try {
    await CharacterAPI.rest(props.character.database_id, 'SECOND_WIND')
    alert('已使用复苏之风！')
    emit('refresh')
  } catch(e) {
    alert('操作失败')
  }
}

const handleLevelUp = async () => {
  if (!confirm('确定要进行升级吗？')) return
  try {
    await CharacterAPI.levelUp(props.character.database_id)
    alert('升级成功！')
    emit('refresh')
  } catch(e) {
    alert('升级失败：可能经验值不足')
  }
}
</script>

<template>
  <div class="progression-tab">
    <div class="panel-grid">
      <!-- Info Column -->
      <div class="column flex-2">
        <div class="info-card">
          <div class="flex-between">
            <h3>角色等级 (Level)</h3>
            <div class="level-badge">LV. {{ currentLevel }}</div>
          </div>
          
          <div class="xp-bar-container mt-4">
            <div class="flex-between text-sm mb-2 text-dim">
              <span>当前 XP: {{ character.experience_points }}</span>
              <span v-if="!isMaxLevel">下一级需要: {{ nextLevelXp }}</span>
              <span v-else>已达满级</span>
            </div>
            <div class="xp-bar-bg">
              <div class="xp-bar-fill" :style="{ width: xpBarPercent + '%' }"></div>
            </div>
            <div v-if="!isMaxLevel" class="text-sm text-dim mt-1" style="text-align:right;">
              还需 {{ Math.max(0, nextLevelXp - character.experience_points) }} XP
            </div>
          </div>

          <div class="log-area mt-4">
            <h4>冒险历程日志</h4>
            <div class="logs">
              <div v-for="(log, idx) in character.advancement_history" :key="idx" class="log-entry">
                <span class="log-bullet">•</span> {{ log }}
              </div>
              <div v-if="!character.advancement_history || character.advancement_history.length === 0" class="text-dim text-sm italic">
                角色刚刚踏上旅途...
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions Column -->
      <div class="column">
        <div class="info-card actions-card">
          <h3>行动与指令 (Actions)</h3>
          
          <div class="action-btn-group mt-4">
            <button class="btn-primary w-full highlight-btn mb-4" @click="handleLevelUp" :disabled="!canLevelUp">
              {{ canLevelUp ? '⬆️ 执行升级！' : (isMaxLevel ? '🏆 已达满级' : '经验不足，暂不可升级') }}
            </button>
            
            <button class="btn-secondary w-full" @click="addXp">添加经验值 (Add XP)</button>
            <hr class="divider" />
            <button class="btn-secondary w-full" @click="performRest('SHORT')" :disabled="isResting">进行短休 (Short Rest)</button>
            <button class="btn-secondary w-full" @click="performRest('LONG')" :disabled="isResting">进行长休 (Long Rest)</button>
            
            <!-- Class specific actions -->
            <template v-if="character.job.class_name && character.job.class_name.includes('战士')">
              <hr class="divider" />
              <button class="btn-secondary w-full" @click="useSecondWind" 
                      :disabled="character.job.current_second_wind_uses <= 0">
                使用复苏之风 (剩余: {{ character.job.current_second_wind_uses || 0 }})
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.progression-tab { height: 100%; display: flex; flex-direction: column; }
.panel-grid { display: flex; gap: var(--space-lg); height: 100%; }
.column { display: flex; flex-direction: column; gap: var(--space-md); flex: 1; }
.flex-2 { flex: 2; }

.info-card { background: rgba(0,0,0,0.2); padding: var(--space-md); border-radius: var(--radius-md); border: 1px solid var(--glass-border); height: 100%; display: flex; flex-direction: column; }
h3 { color: var(--antique-gold); font-family: var(--font-heading); margin-bottom: var(--space-sm); border-bottom: 1px solid var(--glass-border); padding-bottom: 4px; }
h4 { color: var(--text-primary); font-size: 1rem; margin-bottom: 12px; }

.flex-between { display: flex; justify-content: space-between; align-items: center; }
.text-sm { font-size: 0.85rem; }
.text-dim { color: var(--text-dim); }
.italic { font-style: italic; }
.mt-1 { margin-top: 4px; }
.mt-4 { margin-top: 16px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.w-full { width: 100%; }

.level-badge { font-family: var(--font-display); font-size: 1.5rem; font-weight: bold; color: var(--antique-gold-bright); text-shadow: 0 0 10px rgba(201, 168, 76, 0.5); }

.xp-bar-container { background: rgba(0,0,0,0.2); padding: 16px; border-radius: 8px; border: 1px dashed var(--glass-border); }
.xp-bar-bg { width: 100%; height: 12px; background: rgba(0,0,0,0.5); border-radius: 6px; overflow: hidden; border: 1px solid var(--glass-border); }
.xp-bar-fill { height: 100%; background: linear-gradient(90deg, var(--parchment-mid), var(--antique-gold-bright)); transition: width 0.5s ease; box-shadow: 0 0 10px rgba(201,168,76,0.6); }

.log-area { flex: 1; border: 1px solid var(--glass-border); border-radius: 4px; padding: 12px; background: rgba(0,0,0,0.1); overflow-y: auto; display: flex; flex-direction: column; }
.logs { display: flex; flex-direction: column; gap: 8px; }
.log-entry { font-size: 0.9rem; color: var(--text-secondary); line-height: 1.4; }
.log-bullet { color: var(--antique-gold); margin-right: 6px; }

.action-btn-group { display: flex; flex-direction: column; gap: 12px; }
.highlight-btn { font-size: 1.1rem; padding: 12px; }
.divider { border: none; border-top: 1px solid rgba(255,255,255,0.1); margin: 8px 0; }

.btn-primary { background: linear-gradient(180deg, var(--parchment-mid), var(--parchment-dark)); border: 1px solid var(--antique-gold); color: var(--antique-gold-bright); padding: 8px 16px; border-radius: 4px; cursor: pointer; transition: all var(--transition-fast); }
.btn-primary:hover:not(:disabled) { background: var(--parchment-light); box-shadow: var(--shadow-gold); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: rgba(0,0,0,0.3); border: 1px solid var(--text-dim); color: var(--text-secondary); padding: 10px 16px; border-radius: 4px; cursor: pointer; transition: all 0.2s; }
.btn-secondary:hover:not(:disabled) { color: var(--text-primary); border-color: var(--text-primary); background: rgba(255,255,255,0.05); }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
