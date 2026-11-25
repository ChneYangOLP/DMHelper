<script setup lang="ts">
import { ref, computed } from 'vue'
import { CharacterAPI } from '../../api'

const props = defineProps<{ character: any }>()
const emit = defineEmits<{
  refresh: [updatedCharacter?: any]
}>()

const isResting = ref(false)
const shortRestHitDice = ref(0)
const xpAmount = ref('')
const xpError = ref('')
const isAddingXp = ref(false)
const isLevelingUp = ref(false)
const operationNotice = ref('')
const operationFailed = ref(false)
const isSavingChoice = ref(false)
const selectedStyle = ref('')
const selectedSubclass = ref('')
const selectedManeuvers = ref<string[]>([])
const asiMode = ref('ASI_PLUS_TWO')
const primaryAbility = ref('STR')
const secondaryAbility = ref('DEX')
const selectedFeat = ref('')
const groupSelections = ref<Record<string, string[]>>({})

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
const availableHitDice = computed(() => Math.max(0, props.character.available_hit_dice || 0))
const hitDieSize = computed(() => Math.max(1, props.character.job?.hp_dice || 1))

const fighterOptions = computed(() => props.character.fighter_advancement || null)
const advancementOptions = computed(() => props.character.advancement_options || null)
const levelTree = computed(() => advancementOptions.value?.level_tree || [])
const pendingChoices = computed<string[]>(() => props.character.pending_advancement_choices || [])
const abilityOptions = [
  { key: 'STR', label: '力量 (STR)' },
  { key: 'DEX', label: '敏捷 (DEX)' },
  { key: 'CON', label: '体质 (CON)' },
  { key: 'INT', label: '智力 (INT)' },
  { key: 'WIS', label: '感知 (WIS)' },
  { key: 'CHA', label: '魅力 (CHA)' }
]

const handleXpInput = (event: Event) => {
  const input = event.target as HTMLInputElement
  const normalizedValue = input.value
    .replace(/\D/g, '')
    .replace(/^0+(?=\d)/, '')

  xpAmount.value = normalizedValue
  input.value = normalizedValue
  xpError.value = ''
}

const addXp = async () => {
  const amount = Number(xpAmount.value)
  if (!Number.isSafeInteger(amount) || amount <= 0 || amount > 2147483647) {
    xpError.value = '请输入 1 到 2147483647 之间的整数经验值'
    return
  }
  isAddingXp.value = true
  xpError.value = ''
  try {
    const updatedCharacter = await CharacterAPI.addXp(props.character.database_id, amount, '手动添加')
    operationFailed.value = false
    operationNotice.value = `已增加 ${amount} 点经验值`
    xpAmount.value = ''
    emit('refresh', updatedCharacter)
  } catch(error) {
    console.error(error)
    xpError.value = error instanceof Error ? `经验增加失败：${error.message}` : '经验增加失败'
  } finally {
    isAddingXp.value = false
  }
}

const performRest = async (type: string) => {
  const spentHitDice = type === 'SHORT' ? shortRestHitDice.value : 0
  const restLabel = type === 'LONG' ? '长休' : '短休'
  const hitDiceLabel = type === 'SHORT' && spentHitDice > 0 ? `并消耗 ${spentHitDice} 颗生命骰` : ''
  if (!confirm(`确定要进行${restLabel}${hitDiceLabel}吗？`)) return
  isResting.value = true
  try {
    const updatedCharacter = await CharacterAPI.rest(props.character.database_id, type, spentHitDice)
    shortRestHitDice.value = 0
    operationFailed.value = false
    operationNotice.value = `${restLabel}完成：当前 HP ${updatedCharacter.current_hp}/${updatedCharacter.hp}`
    emit('refresh', updatedCharacter)
  } catch(error) {
    console.error(error)
    alert(error instanceof Error ? `操作失败：${error.message}` : '操作失败')
  } finally {
    isResting.value = false
  }
}

const useSecondWind = async () => {
  try {
    const updatedCharacter = await CharacterAPI.rest(props.character.database_id, 'SECOND_WIND')
    alert('已使用复苏之风！')
    emit('refresh', updatedCharacter)
  } catch(error) {
    console.error(error)
    alert(error instanceof Error ? `操作失败：${error.message}` : '操作失败')
  }
}

const handleLevelUp = async () => {
  if (!confirm('确定要进行升级吗？')) return
  isLevelingUp.value = true
  operationNotice.value = ''
  operationFailed.value = false
  try {
    const updatedCharacter = await CharacterAPI.levelUp(props.character.database_id)
    operationNotice.value = `升级成功！当前等级为 ${updatedCharacter.job?.current_level ?? ''} 级`
    emit('refresh', updatedCharacter)
  } catch(error) {
    console.error(error)
    operationFailed.value = true
    operationNotice.value = error instanceof Error ? `升级失败：${error.message}` : '升级失败'
  } finally {
    isLevelingUp.value = false
  }
}

const toggleManeuver = (key: string) => {
  const index = selectedManeuvers.value.indexOf(key)
  if (index >= 0) {
    selectedManeuvers.value.splice(index, 1)
    return
  }
  const limit = fighterOptions.value?.pending_maneuver_count || 0
  if (selectedManeuvers.value.length < limit) selectedManeuvers.value.push(key)
}

const saveChoice = async (type: string, value?: string, values?: string[]) => {
  isSavingChoice.value = true
  operationNotice.value = ''
  operationFailed.value = false
  try {
    const updatedCharacter = await CharacterAPI.applyAdvancementChoice(props.character.database_id, type, value, values)
    operationNotice.value = '升级选项已保存'
    selectedStyle.value = ''
    selectedSubclass.value = ''
    selectedManeuvers.value = []
    selectedFeat.value = ''
    groupSelections.value = {}
    emit('refresh', updatedCharacter)
  } catch (error) {
    console.error(error)
    alert(error instanceof Error ? `升级选项保存失败：${error.message}` : '升级选项保存失败')
  } finally {
    isSavingChoice.value = false
  }
}

const toggleGroupChoice = (group: any, key: string) => {
  const selected = groupSelections.value[group.type] || []
  if (!group.multiple) {
    groupSelections.value[group.type] = [key]
    return
  }
  if (selected.includes(key)) {
    groupSelections.value[group.type] = selected.filter(item => item !== key)
  } else if (selected.length < group.required) {
    groupSelections.value[group.type] = [...selected, key]
  }
}

const saveGroupChoice = (group: any) => {
  const selected = groupSelections.value[group.type] || []
  if (selected.length !== group.required) return
  if (group.required === 1) saveChoice(group.type, selected[0])
  else saveChoice(group.type, undefined, selected)
}

const saveAsiChoice = () => {
  if (asiMode.value === 'FEAT') {
    if (!selectedFeat.value) return
    saveChoice('FEAT', selectedFeat.value)
  } else if (asiMode.value === 'ASI_PLUS_ONE_TWO') {
    saveChoice('ASI_PLUS_ONE_TWO', undefined, [primaryAbility.value, secondaryAbility.value])
  } else {
    saveChoice('ASI_PLUS_TWO', primaryAbility.value)
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

          <div v-if="operationNotice" :class="['operation-notice', { error: operationFailed }]" :role="operationFailed ? 'alert' : 'status'">
            {{ operationNotice }}
          </div>
          <div class="action-btn-group mt-4">
            <button class="btn-primary w-full highlight-btn mb-4" @click="handleLevelUp" :disabled="!canLevelUp || isLevelingUp || isAddingXp">
              {{ isLevelingUp ? '升级中...' : (canLevelUp ? '⬆️ 执行升级！' : (isMaxLevel ? '🏆 已达满级' : '经验不足，暂不可升级')) }}
            </button>
            <form class="xp-entry" @submit.prevent="addXp">
              <label for="xp-amount">添加经验值 (Add XP)</label>
              <div class="xp-entry-controls">
                <input
                  id="xp-amount"
                  :value="xpAmount"
                  class="glass-input"
                  type="text"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  autocomplete="off"
                  placeholder="输入经验值"
                  :disabled="isAddingXp || isLevelingUp"
                  @input="handleXpInput"
                />
                <button type="submit" class="btn-secondary" :disabled="isAddingXp || isLevelingUp || !xpAmount">
                  {{ isAddingXp ? '添加中...' : '添加' }}
                </button>
              </div>
              <div v-if="xpError" class="form-error" role="alert">{{ xpError }}</div>
            </form>
            <div class="short-rest-control">
              <label for="short-rest-hit-dice">短休生命骰</label>
              <select id="short-rest-hit-dice" v-model.number="shortRestHitDice" class="glass-input" :disabled="isResting || availableHitDice === 0">
                <option v-for="count in availableHitDice + 1" :key="count - 1" :value="count - 1">
                  {{ count - 1 }} 颗 d{{ hitDieSize }}
                </option>
              </select>
              <small>可用 {{ availableHitDice }} 颗；每颗额外加入体质调整值。</small>
            </div>
            <button class="btn-secondary w-full" @click="performRest('SHORT')" :disabled="isResting">进行短休 (Short Rest)</button>
            <button class="btn-secondary w-full" @click="performRest('LONG')" :disabled="isResting">进行长休 (Long Rest)</button>
            <!-- Class specific actions -->
            <template v-if="character.job.class_name && character.job.class_name.includes('战士')">
              <button class="btn-secondary w-full" @click="useSecondWind" 
                      :disabled="character.job.current_second_wind_uses <= 0">
                使用复苏之风 (剩余: {{ character.job.current_second_wind_uses || 0 }})
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <section v-if="fighterOptions" class="advancement-panel info-card">
      <div class="flex-between">
        <h3>战士升级选项</h3>
        <span class="pending-count">待处理 {{ pendingChoices.length }} 项</span>
      </div>

      <div v-if="pendingChoices.length === 0" class="no-pending-choice">
        当前等级没有待选择项目；动作如潮、额外攻击等固定职业能力会在升级时自动获得。
      </div>

      <div v-if="fighterOptions.needs_fighting_style" class="choice-card">
        <div class="choice-title">选择战斗风格</div>
        <select v-model="selectedStyle" class="glass-input">
          <option value="">请选择</option>
          <option v-for="option in fighterOptions.fighting_styles" :key="option.key" :value="option.key">
            {{ option.label }} — {{ option.description }}
          </option>
        </select>
        <button class="btn-primary" :disabled="isSavingChoice || !selectedStyle" @click="saveChoice('FIGHTING_STYLE', selectedStyle)">确认选择</button>
      </div>

      <div v-if="fighterOptions.needs_subclass" class="choice-card">
        <div class="choice-title">选择 3 级战士子职业</div>
        <div class="option-grid">
          <button
            v-for="option in fighterOptions.subclasses"
            :key="option.key"
            :class="['select-option', { active: selectedSubclass === option.key }]"
            @click="selectedSubclass = option.key"
          >
            <strong>{{ option.label }}</strong>
            <span>{{ option.description }}</span>
          </button>
        </div>
        <button class="btn-primary" :disabled="isSavingChoice || !selectedSubclass" @click="saveChoice('SUBCLASS', selectedSubclass)">确认子职业</button>
      </div>

      <div v-if="fighterOptions.pending_maneuver_count > 0" class="choice-card">
        <div class="choice-title">
          选择战技（{{ selectedManeuvers.length }} / {{ fighterOptions.pending_maneuver_count }}）
        </div>
        <div class="maneuver-grid">
          <button
            v-for="option in fighterOptions.maneuvers"
            :key="option.key"
            :class="['select-option', { active: selectedManeuvers.includes(option.key) }]"
            @click="toggleManeuver(option.key)"
          >
            <strong>{{ option.label }}</strong>
            <span>{{ option.description }}</span>
          </button>
        </div>
        <button
          class="btn-primary"
          :disabled="isSavingChoice || selectedManeuvers.length !== fighterOptions.pending_maneuver_count"
          @click="saveChoice('MANEUVERS', undefined, selectedManeuvers)"
        >确认战技</button>
      </div>

      <div v-if="fighterOptions.needs_extra_fighting_style" class="choice-card">
        <div class="choice-title">选择冠军勇士额外战斗风格</div>
        <select v-model="selectedStyle" class="glass-input">
          <option value="">请选择</option>
          <option v-for="option in fighterOptions.extra_fighting_styles" :key="option.key" :value="option.key">
            {{ option.label }} — {{ option.description }}
          </option>
        </select>
        <button class="btn-primary" :disabled="isSavingChoice || !selectedStyle" @click="saveChoice('EXTRA_FIGHTING_STYLE', selectedStyle)">确认选择</button>
      </div>

      <div v-if="fighterOptions.pending_asi_count > 0" class="choice-card">
        <div class="choice-title">属性值提升或专长（剩余 {{ fighterOptions.pending_asi_count }} 次）</div>
        <select v-model="asiMode" class="glass-input">
          <option value="ASI_PLUS_TWO">一项属性 +2</option>
          <option value="ASI_PLUS_ONE_TWO">两项不同属性各 +1</option>
          <option value="FEAT">选择一项专长</option>
        </select>
        <div v-if="asiMode !== 'FEAT'" class="ability-choice-row">
          <select v-model="primaryAbility" class="glass-input">
            <option v-for="ability in abilityOptions" :key="ability.key" :value="ability.key">{{ ability.label }}</option>
          </select>
          <select v-if="asiMode === 'ASI_PLUS_ONE_TWO'" v-model="secondaryAbility" class="glass-input">
            <option v-for="ability in abilityOptions" :key="ability.key" :value="ability.key">{{ ability.label }}</option>
          </select>
        </div>
        <select v-else v-model="selectedFeat" class="glass-input">
          <option value="">请选择专长</option>
          <option v-for="option in fighterOptions.feats" :key="option.key" :value="option.key">
            {{ option.label }} — {{ option.description }}
          </option>
        </select>
        <button
          class="btn-primary"
          :disabled="isSavingChoice || (asiMode === 'FEAT' && !selectedFeat) || (asiMode === 'ASI_PLUS_ONE_TWO' && primaryAbility === secondaryAbility)"
          @click="saveAsiChoice"
        >确认成长选择</button>
      </div>
    </section>

    <section v-if="advancementOptions && !fighterOptions" class="advancement-panel info-card">
      <div class="flex-between">
        <h3>{{ advancementOptions.class_label }}升级树</h3>
        <span class="pending-count">待处理 {{ pendingChoices.length }} 项</span>
      </div>

      <div v-if="pendingChoices.length === 0" class="no-pending-choice">
        当前等级没有待选择项目；固定职业能力、资源上限和法术位会随等级自动生效。
      </div>

      <div v-for="group in advancementOptions.groups" :key="group.type" class="choice-card">
        <div class="choice-title">
          {{ group.title }}
          <span v-if="group.required > 1">（{{ (groupSelections[group.type] || []).length }} / {{ group.required }}）</span>
        </div>
        <div class="maneuver-grid">
          <button
            v-for="option in group.options"
            :key="option.key"
            :class="['select-option', { active: (groupSelections[group.type] || []).includes(option.key) }]"
            @click="toggleGroupChoice(group, option.key)"
          >
            <strong>{{ option.label }}</strong>
            <span v-if="option.description">{{ option.description }}</span>
          </button>
        </div>
        <button
          class="btn-primary"
          :disabled="isSavingChoice || (groupSelections[group.type] || []).length !== group.required"
          @click="saveGroupChoice(group)"
        >确认选择</button>
      </div>

      <div v-if="advancementOptions.pending_asi_count > 0" class="choice-card">
        <div class="choice-title">属性值提升或专长（剩余 {{ advancementOptions.pending_asi_count }} 次）</div>
        <select v-model="asiMode" class="glass-input">
          <option value="ASI_PLUS_TWO">一项属性 +2</option>
          <option value="ASI_PLUS_ONE_TWO">两项不同属性各 +1</option>
          <option value="FEAT">选择一项专长</option>
        </select>
        <div v-if="asiMode !== 'FEAT'" class="ability-choice-row">
          <select v-model="primaryAbility" class="glass-input">
            <option v-for="ability in abilityOptions" :key="ability.key" :value="ability.key">{{ ability.label }}</option>
          </select>
          <select v-if="asiMode === 'ASI_PLUS_ONE_TWO'" v-model="secondaryAbility" class="glass-input">
            <option v-for="ability in abilityOptions" :key="ability.key" :value="ability.key">{{ ability.label }}</option>
          </select>
        </div>
        <select v-else v-model="selectedFeat" class="glass-input">
          <option value="">请选择专长</option>
          <option v-for="option in advancementOptions.feats" :key="option.key" :value="option.key">
            {{ option.label }} — {{ option.description }}
          </option>
        </select>
        <button
          class="btn-primary"
          :disabled="isSavingChoice || (asiMode === 'FEAT' && !selectedFeat) || (asiMode === 'ASI_PLUS_ONE_TWO' && primaryAbility === secondaryAbility)"
          @click="saveAsiChoice"
        >确认成长选择</button>
      </div>
    </section>

    <section v-if="levelTree.length" class="level-tree-panel info-card">
      <div class="tree-heading">
        <div>
          <h3>1–20 级职业成长树</h3>
          <p>完整展示 {{ advancementOptions.class_label }} 的能力里程碑；带“需要选择”标记的等级需要在升级后完成对应决策。</p>
        </div>
        <div class="tree-legend">
          <span class="legend completed">已获得</span>
          <span class="legend current">当前</span>
          <span class="legend next">下一级</span>
        </div>
      </div>

      <div class="level-tree">
        <article
          v-for="node in levelTree"
          :key="node.level"
          :class="['level-node', node.status.toLowerCase()]"
        >
          <div class="level-node-head">
            <span class="tree-level">Lv.{{ node.level }}</span>
            <span class="tree-pb">熟练 +{{ node.proficiency_bonus }}</span>
            <span v-if="node.choice_required" class="choice-badge">需要选择</span>
          </div>
          <ul>
            <li v-for="feature in node.features" :key="feature">{{ feature }}</li>
          </ul>
        </article>
      </div>
    </section>

  </div>
</template>

<style scoped>
.progression-tab { min-height: 100%; display: flex; flex-direction: column; gap: var(--space-lg); }
.panel-grid { display: flex; gap: var(--space-lg); }
.column { display: flex; flex-direction: column; gap: var(--space-md); flex: 1; }
.flex-2 { flex: 2; }
.info-card { background: var(--surface-card); padding: var(--space-md); border-radius: var(--radius-md); height: 100%; display: flex; flex-direction: column; }
h3 { color: var(--antique-gold); font-family: var(--font-heading); margin-bottom: var(--space-sm); padding-bottom: 4px; }
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
.level-badge { font-family: var(--font-display); font-size: 1.5rem; font-weight: bold; color: var(--antique-gold); }
.xp-bar-container { background: var(--surface-inset); padding: 16px; border-radius: 8px; }
.xp-bar-bg { width: 100%; height: 12px; background: var(--surface-card); border-radius: 6px; overflow: hidden; }
.xp-bar-fill { height: 100%; background: var(--bronze); transition: width 0.5s ease; }
.log-area { flex: 1; border-radius: 4px; padding: 12px; background: var(--surface-inset); overflow-y: auto; display: flex; flex-direction: column; }
.logs { display: flex; flex-direction: column; gap: 8px; }
.log-entry { font-size: 0.9rem; color: var(--text-secondary); line-height: 1.4; }
.log-bullet { color: var(--antique-gold); margin-right: 6px; }
.action-btn-group { display: flex; flex-direction: column; gap: 12px; }
.short-rest-control { display: flex; flex-direction: column; gap: 6px; }
.short-rest-control label { color: var(--text-secondary); font-size: 0.9rem; }
.short-rest-control small { color: var(--text-dim); line-height: 1.35; }
.operation-notice { margin-top: 12px; padding: 10px 12px; border-radius: 4px; background: var(--surface-success); color: var(--text-success); font-size: 0.9rem; }
.operation-notice.error { background: var(--surface-danger); color: var(--text-danger); }
.highlight-btn { font-size: 1.1rem; padding: 12px; }
.xp-entry { display: flex; flex-direction: column; gap: 8px; -webkit-app-region: no-drag; }
.xp-entry label { color: var(--text-secondary); font-size: 0.9rem; }
.xp-entry-controls { display: flex; gap: 8px; }
.xp-entry-controls .glass-input { flex: 1; min-width: 0; }
.xp-entry-controls .btn-secondary { flex: 0 0 auto; }
.glass-input { width: 100%; box-sizing: border-box; padding: 10px 12px; border-radius: 4px; background: var(--surface-inset); color: var(--text-primary); font: inherit; }
.form-error { margin-top: 10px; color: var(--text-danger); font-size: 0.9rem; }
.advancement-panel { height: auto; gap: 14px; }
.pending-count { color: var(--text-dim); font-size: 0.85rem; }
.no-pending-choice { color: var(--text-dim); padding: 10px 0; }
.choice-card { display: flex; flex-direction: column; gap: 10px; padding: 14px; border-radius: var(--radius-sm); background: var(--surface-card); }
.choice-title { color: var(--antique-gold); font-weight: 700; }
.option-grid,  .maneuver-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.maneuver-grid { max-height: 360px; overflow-y: auto; padding-right: 4px; }
.select-option { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; text-align: left; --control-bg: var(--surface-inset); --control-hover: var(--surface-hover); }
.select-option span { color: var(--text-dim); font-size: 0.8rem; line-height: 1.35; }
.select-option.active { --control-bg: var(--surface-selected); color: var(--antique-gold); }
.ability-choice-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.level-tree-panel { height: auto; }
.tree-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.tree-heading h3 { margin-bottom: 6px; }
.tree-heading p { margin: 0; color: var(--text-dim); font-size: 0.88rem; line-height: 1.45; }
.tree-legend { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 6px; }
.legend { padding: 3px 8px; border-radius: 999px; color: var(--text-dim); font-size: 0.75rem; white-space: nowrap; background: var(--surface-card); }
.legend.completed { color: var(--text-success); }
.legend.current { color: var(--antique-gold); }
.legend.next { color: var(--text-primary); }
.level-tree { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 14px; margin-top: 16px; }
.level-node { min-height: 108px; padding: 12px 14px; border-radius: var(--radius-sm); background: var(--surface-card); }
.level-node.completed { background: var(--surface-success); }
.level-node.current { background: var(--surface-selected); }
.level-node.next { background: var(--surface-hover); }
.level-node-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.tree-level { color: var(--antique-gold); font-family: var(--font-display); font-weight: 800; }
.tree-pb { color: var(--text-dim); font-size: 0.76rem; }
.choice-badge { margin-left: auto; padding: 2px 6px; border-radius: 999px; color: var(--antique-gold); font-size: 0.7rem; background: var(--surface-selected); }
.level-node ul { margin: 0; padding-left: 18px; color: var(--text-secondary); font-size: 0.84rem; line-height: 1.5; }
.level-node li::marker { color: var(--antique-gold); }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
@media (max-width: 820px) {
  .tree-heading { flex-direction: column; }
  .tree-legend { justify-content: flex-start; }
  .level-tree { grid-template-columns: 1fr; }
}
</style>
