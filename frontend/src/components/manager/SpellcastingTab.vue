<script setup lang="ts">
import { ref, computed } from 'vue'
import { CharacterAPI, SpellAPI } from '../../api'

const props = defineProps<{ character: any }>()
const emit = defineEmits(['refresh'])

const isSpellcaster = computed(() => {
  return props.character.job && (
    props.character.job.class_name.includes('法师') ||
    props.character.job.class_name.includes('术士') ||
    props.character.job.class_name.includes('邪术士') ||
    props.character.job.class_name.includes('圣武士') ||
    props.character.job.class_name.includes('吟游诗人')
  )
})

const spellMap = computed(() => props.character.spell_map || {})
const isWizard = computed(() => props.character.job?.class_name?.includes('法师'))
const isPaladin = computed(() => props.character.job?.class_name?.includes('圣武士'))

const resolveSpellName = (key: string) => {
  return spellMap.value[key] || key
}

const getSpellSlotLevel = (idx: number) => {
  const levels = ['一', '二', '三', '四', '五', '六', '七', '八', '九']
  return levels[idx]
}

// Modal state
const showModal = ref(false)
const modalMode = ref<'cantrip' | 'spell' | 'prepared'>('cantrip')
const modalTitle = ref('')
const availableSpells = ref<any[]>([])
const loadingSpells = ref(false)

const currentKnownCantrips = computed(() => props.character.job?.known_cantrip_keys || [])
const currentKnownSpells = computed(() => isWizard.value
  ? (props.character.job?.spellbook_spell_keys || [])
  : (props.character.job?.known_spell_keys || []))
const currentPreparedSpells = computed(() => props.character.job?.prepared_spell_keys || [])
const selectionLimit = computed(() => {
  if (modalMode.value === 'cantrip') return props.character.max_cantrips || 0
  if (modalMode.value === 'spell') return props.character.max_known_spells || 0
  return props.character.max_prepared_spells || 0
})
const selectionCount = computed(() => {
  if (modalMode.value === 'cantrip') return currentKnownCantrips.value.length
  if (modalMode.value === 'spell') return currentKnownSpells.value.length
  return currentPreparedSpells.value.length
})

const openModal = async (mode: 'cantrip' | 'spell' | 'prepared') => {
  modalMode.value = mode
  showModal.value = true
  loadingSpells.value = true

  const characterId = props.character.database_id

  try {
    if (mode === 'cantrip') {
      modalTitle.value = '管理戏法 (Cantrips)'
      availableSpells.value = await SpellAPI.getAvailableSpells(characterId, true)
    } else if (mode === 'spell') {
      modalTitle.value = isWizard.value ? '管理法术书 (Spellbook)' : '管理已知法术 (Known Spells)'
      availableSpells.value = await SpellAPI.getAvailableSpells(characterId, false)
    } else {
      modalTitle.value = '管理准备法术 (Prepared Spells)'
      availableSpells.value = isPaladin.value
        ? await SpellAPI.getAvailableSpells(characterId, false)
        : (currentKnownSpells.value || []).map((key: string) => ({
            key,
            display_name: resolveSpellName(key),
            level: null,
            school_or_theme: ''
          }))
    }
  } catch (error) {
    console.error(error)
    availableSpells.value = []
    alert(error instanceof Error ? `加载法术失败：${error.message}` : '加载法术失败')
  } finally {
    loadingSpells.value = false
  }
}

const isSelected = (spellKey: string) => {
  if (modalMode.value === 'cantrip') return currentKnownCantrips.value.includes(spellKey)
  if (modalMode.value === 'spell') return currentKnownSpells.value.includes(spellKey)
  if (modalMode.value === 'prepared') return currentPreparedSpells.value.includes(spellKey)
  return false
}

const toggleSpell = async (spellKey: string) => {
  const selected = isSelected(spellKey)
  if (!selected && selectionCount.value >= selectionLimit.value) {
    alert(`已达到数量上限（${selectionLimit.value}）`)
    return
  }
  let action = ''
  if (modalMode.value === 'cantrip') action = selected ? 'REMOVE_CANTRIP' : 'ADD_CANTRIP'
  else if (modalMode.value === 'spell') action = selected ? 'REMOVE_SPELL' : 'ADD_SPELL'
  else action = selected ? 'UNPREPARE' : 'PREPARE'

  try {
    await CharacterAPI.manageSpells(props.character.database_id, action, spellKey)
    emit('refresh')
  } catch (e) {
    alert('操作失败')
  }
}

const closeModal = () => {
  showModal.value = false
}
</script>

<template>
  <div class="spellcasting-tab">
    <div v-if="!isSpellcaster" class="not-spellcaster">
      <div class="icon">✨</div>
      <div class="text">你的职业不具备施法能力。</div>
      <div class="subtext">（战士只有选择"奥法骑士"子职业才能施法）</div>
    </div>

    <div v-else class="panel-grid">
      <!-- Spellcasting Info Column -->
      <div class="column">
        <div class="info-card">
          <h3>施法属性 (Spellcasting Base)</h3>
          
          <div class="combat-stats-grid mb-4">
            <div class="stat-box">
              <div class="stat-label">施法主属性</div>
              <div class="stat-val text-primary">{{ character.spellcasting_ability_name || '—' }}</div>
            </div>
            <div class="stat-box">
              <div class="stat-label">法术攻击加值</div>
              <div class="stat-val text-success">{{ character.spell_attack_bonus != null ? ((character.spell_attack_bonus > 0 ? '+' : '') + character.spell_attack_bonus) : '—' }}</div>
            </div>
            <div class="stat-box">
              <div class="stat-label">法术豁免 DC</div>
              <div class="stat-val text-danger">{{ character.spell_save_dc || '—' }}</div>
            </div>
          </div>

          <h3>法术位 (Spell Slots)</h3>
          <div class="slots-grid">
            <template v-if="character.job.max_spell_slots">
              <template v-for="(max, idx) in character.job.max_spell_slots" :key="idx">
                <div class="slot-box" v-if="max > 0">
                  <div class="slot-level">{{ getSpellSlotLevel(idx) }}环</div>
                  <div class="slot-count">{{ character.job.current_spell_slots ? character.job.current_spell_slots[idx] : 0 }} / {{ max }}</div>
                </div>
              </template>
            </template>
          </div>
          <div v-if="!character.job.max_spell_slots || character.job.max_spell_slots.every((m: number) => m === 0)" class="text-dim text-sm italic mt-2">
            当前等级没有可用的法术位。
          </div>
        </div>
      </div>

      <!-- Spells Action Column -->
      <div class="column flex-2">
        <div class="info-card">
          <h3>法术书与配置 (Spellbook & Preparation)</h3>
          
          <div class="action-btn-group">
            <button v-if="character.max_cantrips > 0" class="btn-primary highlight-btn" @click="openModal('cantrip')">
              管理戏法 (Cantrips)
            </button>
            <button v-if="!isPaladin" class="btn-primary highlight-btn" @click="openModal('spell')">
              {{ isWizard ? '管理法术书 (Spellbook)' : '管理已知法术 (Known Spells)' }}
            </button>
            <button v-if="isWizard || isPaladin" class="btn-primary highlight-btn" @click="openModal('prepared')">
              管理准备法术 (Prepared Spells)
            </button>
          </div>

          <div class="mt-4">
            <h4>当前已知戏法</h4>
            <div class="spell-list">
              <div v-for="key in currentKnownCantrips" :key="key" class="spell-item cantrip-item">
                🔮 {{ resolveSpellName(key) }}
              </div>
              <div v-if="currentKnownCantrips.length === 0" class="text-dim text-sm italic">
                尚未掌握任何戏法。
              </div>
            </div>
          </div>

          <div class="mt-4">
            <h4>当前已知/准备的法术</h4>
            <div class="spell-list">
              <div v-for="key in currentKnownSpells" :key="'known-'+key" class="spell-item"
                   :class="{ prepared: currentPreparedSpells.includes(key) }">
                {{ currentPreparedSpells.includes(key) ? '🌟' : '📖' }} {{ resolveSpellName(key) }}
              </div>
              <div v-if="currentKnownSpells.length === 0 && currentPreparedSpells.length === 0" class="text-dim text-sm italic">
                尚未掌握或准备任何法术。
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Spell Selection Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-content">
          <div class="modal-header">
            <h3>{{ modalTitle }}</h3>
            <button class="modal-close" @click="closeModal">✕</button>
          </div>
          <div class="modal-body">
            <div v-if="loadingSpells" class="text-dim text-center">加载中...</div>
            <div v-else class="spell-select-grid">
              <div v-for="spell in availableSpells" :key="spell.key" 
                   class="spell-select-item"
                   :class="{ active: isSelected(spell.key) }"
                   @click="toggleSpell(spell.key)">
                <div class="spell-select-name">
                  <span class="spell-check">{{ isSelected(spell.key) ? '✅' : '⬜' }}</span>
                  {{ spell.display_name }}
                </div>
                <div class="spell-select-meta" v-if="spell.level != null">
                  <span class="spell-level-tag">{{ spell.level === 0 ? '戏法' : spell.level + '环' }}</span>
                  <span class="spell-school" v-if="spell.school_or_theme">{{ spell.school_or_theme }}</span>
                </div>
                <div class="spell-select-desc" v-if="spell.short_description">{{ spell.short_description }}</div>
              </div>
              <div v-if="availableSpells.length === 0" class="text-dim text-sm italic">没有可选法术。</div>
            </div>
          </div>
          <div class="modal-footer">
            <span class="text-dim">已选择 {{ selectionCount }} / {{ selectionLimit }}</span>
            <button class="btn-primary" @click="closeModal">完成</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.spellcasting-tab { height: 100%; display: flex; flex-direction: column; }
.not-spellcaster { flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; color: var(--text-dim); }
.not-spellcaster .icon { font-size: 4rem; margin-bottom: 16px; opacity: 0.5; }
.not-spellcaster .text { font-size: 1.5rem; font-family: var(--font-heading); color: var(--text-secondary); margin-bottom: 8px; }
.not-spellcaster .subtext { font-size: 0.9rem; font-style: italic; }
.panel-grid { display: flex; gap: var(--space-lg); height: 100%; }
.column { display: flex; flex-direction: column; gap: var(--space-md); flex: 1; }
.flex-2 { flex: 2; }
.info-card { background: var(--surface-card); padding: var(--space-md); border-radius: var(--radius-md); height: 100%; display: flex; flex-direction: column; }
h3 { color: var(--antique-gold); font-family: var(--font-heading); margin-bottom: var(--space-sm); padding-bottom: 4px; }
h4 { color: var(--text-primary); font-size: 1rem; margin-bottom: 12px; }
.combat-stats-grid { display: flex; gap: 8px; }
.stat-box { flex: 1; background: var(--surface-inset); border-radius: var(--radius-sm); text-align: center; padding: var(--space-sm) 0; }
.stat-label { font-size: 0.8rem; color: var(--text-dim); }
.stat-val { font-size: 1.4rem; font-family: var(--font-display); font-weight: bold; margin-top: 4px; }
.text-primary { color: var(--text-primary); }
.slots-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.slot-box { background: var(--surface-inset); border-radius: 4px; text-align: center; padding: 8px; }
.slot-level { font-size: 0.85rem; color: var(--text-secondary); }
.slot-count { font-size: 1.2rem; font-family: var(--font-display); color: var(--antique-gold); font-weight: bold; }
.action-btn-group { display: flex; gap: 12px; flex-wrap: wrap; }
.highlight-btn { flex: 1; font-size: 0.95rem; padding: 12px; min-width: 160px; }
.spell-list { display: flex; flex-wrap: wrap; gap: 8px; max-height: 200px; overflow-y: auto; padding-right: 4px; }
.spell-item { background: var(--surface-inset); padding: 6px 12px; border-radius: 16px; font-size: 0.9rem; color: var(--text-primary); }
.spell-item.prepared { background: var(--surface-selected); color: var(--antique-gold); }
.mt-2 { margin-top: 8px; }
.mt-4 { margin-top: 16px; }
.mb-4 { margin-bottom: 16px; }
.text-sm { font-size: 0.85rem; }
.text-dim { color: var(--text-dim); }
.text-center { text-align: center; }
.italic { font-style: italic; }
/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: var(--scrim); z-index: 1000; display: flex; justify-content: center; align-items: center; }
.modal-content { background: var(--surface-overlay); border-radius: 12px; width: 700px; max-width: 90vw; max-height: 80vh; display: flex; flex-direction: column; box-shadow: var(--overlay-shadow); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; }
.modal-header h3 { margin: 0; padding: 0; }
.modal-close { color: var(--text-dim); font-size: 1.4rem; cursor: pointer; padding: 4px 8px; --control-bg: transparent; }
.modal-close:hover { color: var(--text-primary); }
.modal-body { flex: 1; overflow-y: auto; padding: 16px 20px; }
.modal-footer { padding: 12px 20px; display: flex; justify-content: flex-end; }
.spell-select-grid { display: flex; flex-direction: column; gap: 8px; }
.spell-select-item { background: var(--surface-card); border-radius: 8px; padding: 12px 16px; cursor: pointer; transition: background-color var(--transition-fast); }
.spell-select-item:hover { background: var(--surface-hover); }
.spell-select-item.active { background: var(--surface-selected); }
.spell-select-name { font-size: 1rem; color: var(--text-primary); font-weight: 600; display: flex; align-items: center; gap: 8px; }
.spell-check { font-size: 0.9rem; }
.spell-select-meta { display: flex; gap: 8px; margin-top: 4px; }
.spell-level-tag { font-size: 0.75rem; background: var(--surface-inset); color: var(--antique-gold); padding: 2px 8px; border-radius: 10px; }
.spell-school { font-size: 0.75rem; color: var(--text-dim); }
.spell-select-desc { font-size: 0.8rem; color: var(--text-dim); margin-top: 4px; line-height: 1.3; }
</style>
