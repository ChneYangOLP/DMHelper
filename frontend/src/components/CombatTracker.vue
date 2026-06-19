<script setup lang="ts">
import { ref, computed} from 'vue'

/* =============================================
   Interfaces
   ============================================= */
interface StatusEffect {
  name: string
  icon: string
  color: string
}

interface Combatant {
  id: number
  name: string
  initiative: number
  currentHp: number
  maxHp: number
  armorClass: number
  type: 'player' | 'enemy' | 'ally'
  isActive: boolean
  statusEffects: StatusEffect[]
  portraitEmoji: string
}

/* =============================================
   Reactive State
   ============================================= */
const combatants = ref<Combatant[]>([
  {
    id: 1,
    name: '阿拉贡 · 银剑',
    initiative: 22,
    currentHp: 45,
    maxHp: 52,
    armorClass: 18,
    type: 'player',
    isActive: true,
    statusEffects: [{ name: '祝福术', icon: '✨', color: '#c9a84c' }],
    portraitEmoji: '⚔️'
  },
  {
    id: 2,
    name: '莱戈拉斯',
    initiative: 19,
    currentHp: 38,
    maxHp: 38,
    armorClass: 16,
    type: 'player',
    isActive: false,
    statusEffects: [],
    portraitEmoji: '🏹'
  },
  {
    id: 3,
    name: '甘道夫',
    initiative: 17,
    currentHp: 28,
    maxHp: 40,
    armorClass: 15,
    type: 'player',
    isActive: false,
    statusEffects: [{ name: '专注：护盾术', icon: '🛡️', color: '#4a8caa' }],
    portraitEmoji: '🧙'
  },
  {
    id: 4,
    name: '兽人狂战士',
    initiative: 15,
    currentHp: 30,
    maxHp: 30,
    armorClass: 13,
    type: 'enemy',
    isActive: false,
    statusEffects: [],
    portraitEmoji: '👹'
  },
  {
    id: 5,
    name: '食人魔',
    initiative: 12,
    currentHp: 14,
    maxHp: 59,
    armorClass: 11,
    type: 'enemy',
    isActive: false,
    statusEffects: [{ name: '中毒', icon: '☠️', color: '#6b4c8a' }],
    portraitEmoji: '👾'
  },
  {
    id: 6,
    name: '骷髅弓箭手',
    initiative: 8,
    currentHp: 4,
    maxHp: 13,
    armorClass: 13,
    type: 'enemy',
    isActive: false,
    statusEffects: [],
    portraitEmoji: '💀'
  }
])

const currentRound = ref(3)
const currentTurn = ref(1)
const d20Result = ref<number | null>(null)
const isRolling = ref(false)
const showHpModal = ref(false)
const selectedCombatant = ref<Combatant | null>(null)
const hpChangeAmount = ref(0)
const rollingDisplay = ref(0)
const flashingId = ref<number | null>(null)
const flashType = ref<'damage' | 'heal' | null>(null)

/* =============================================
   Computed
   ============================================= */
const sortedCombatants = computed(() => {
  return [...combatants.value].sort((a, b) => b.initiative - a.initiative)
})

const activeCombatant = computed(() => {
  return sortedCombatants.value[currentTurn.value - 1] || null
})

/* =============================================
   Methods
   ============================================= */
function getHpPercentage(c: Combatant): number {
  return (c.currentHp / c.maxHp) * 100
}

function getHpColor(c: Combatant): string {
  const pct = getHpPercentage(c)
  if (pct > 75) return '#4a8c5c'
  if (pct > 50) return '#7aaa3c'
  if (pct > 25) return '#b8862d'
  return '#c43232'
}

function rollD20() {
  if (isRolling.value) return
  isRolling.value = true
  d20Result.value = null

  const interval = setInterval(() => {
    rollingDisplay.value = Math.floor(Math.random() * 20) + 1
  }, 60)

  setTimeout(() => {
    clearInterval(interval)
    const result = Math.floor(Math.random() * 20) + 1
    d20Result.value = result
    rollingDisplay.value = result
    isRolling.value = false
  }, 800)
}

function nextTurn() {
  // Clear current active state
  sortedCombatants.value.forEach(c => (c.isActive = false))

  if (currentTurn.value >= sortedCombatants.value.length) {
    currentTurn.value = 1
    currentRound.value++
  } else {
    currentTurn.value++
  }

  // Set new active
  const active = sortedCombatants.value[currentTurn.value - 1]
  if (active) active.isActive = true
}

function prevTurn() {
  // Clear current active state
  sortedCombatants.value.forEach(c => (c.isActive = false))

  if (currentTurn.value <= 1) {
    if (currentRound.value > 1) {
      currentRound.value--
      currentTurn.value = sortedCombatants.value.length
    }
  } else {
    currentTurn.value--
  }

  // Set new active
  const active = sortedCombatants.value[currentTurn.value - 1]
  if (active) active.isActive = true
}

function openHpModal(combatant: Combatant) {
  selectedCombatant.value = combatant
  hpChangeAmount.value = 0
  showHpModal.value = true
}

function closeHpModal() {
  showHpModal.value = false
  selectedCombatant.value = null
  hpChangeAmount.value = 0
}

function applyDamage() {
  if (!selectedCombatant.value || hpChangeAmount.value <= 0) return
  const target = combatants.value.find(c => c.id === selectedCombatant.value!.id)
  if (target) {
    target.currentHp = Math.max(0, target.currentHp - hpChangeAmount.value)
    triggerFlash(target.id, 'damage')
  }
  closeHpModal()
}

function applyHeal() {
  if (!selectedCombatant.value || hpChangeAmount.value <= 0) return
  const target = combatants.value.find(c => c.id === selectedCombatant.value!.id)
  if (target) {
    target.currentHp = Math.min(target.maxHp, target.currentHp + hpChangeAmount.value)
    triggerFlash(target.id, 'heal')
  }
  closeHpModal()
}

function triggerFlash(id: number, type: 'damage' | 'heal') {
  flashingId.value = id
  flashType.value = type
  setTimeout(() => {
    flashingId.value = null
    flashType.value = null
  }, 600)
}

function getD20Class(): string {
  if (isRolling.value) return 'rolling'
  if (d20Result.value === 20) return 'nat20'
  if (d20Result.value === 1) return 'nat1'
  return ''
}
</script>

<template>
  <div class="combat-tracker">
    <!-- ====== LEFT COLUMN: Initiative List ====== -->
    <div class="initiative-panel">
      <!-- Header -->
      <div class="initiative-header">
        <div class="initiative-header__title-row">
          <h1 class="initiative-header__title">⚔ 战斗追踪器</h1>
          <div class="initiative-header__badges">
            <span class="round-badge">
              <span class="round-badge__label">第</span>
              <span class="round-badge__number">{{ currentRound }}</span>
              <span class="round-badge__label">轮</span>
            </span>
            <span class="turn-badge">
              回合 {{ currentTurn }} / {{ sortedCombatants.length }}
            </span>
          </div>
        </div>

        <!-- Action Bar -->
        <div class="action-bar">
          <button class="action-btn action-btn--nav" @click="prevTurn">
            <span class="action-btn__icon">◀</span>
            上一回合
          </button>
          <button class="action-btn action-btn--nav action-btn--primary" @click="nextTurn">
            下一回合
            <span class="action-btn__icon">▶</span>
          </button>
        </div>
      </div>

      <div class="gold-divider"></div>

      <!-- Combatant List -->
      <div class="combatant-list">
        <div
          v-for="(combatant, index) in sortedCombatants"
          :key="combatant.id"
          class="combatant-card"
          :class="{
            'combatant-card--active': combatant.isActive,
            'combatant-card--player': combatant.type === 'player',
            'combatant-card--enemy': combatant.type === 'enemy',
            'combatant-card--dead': combatant.currentHp <= 0,
            'combatant-card--damage-flash': flashingId === combatant.id && flashType === 'damage',
            'combatant-card--heal-flash': flashingId === combatant.id && flashType === 'heal',
          }"
          :style="{ '--i': index }"
        >
          <!-- Portrait -->
          <div
            class="combatant-portrait"
            :class="{
              'combatant-portrait--player': combatant.type === 'player',
              'combatant-portrait--enemy': combatant.type === 'enemy',
            }"
          >
            <span class="combatant-portrait__emoji">{{ combatant.portraitEmoji }}</span>
          </div>

          <!-- Info -->
          <div class="combatant-info">
            <div class="combatant-info__header">
              <span
                class="combatant-info__name"
                :class="{
                  'combatant-info__name--player': combatant.type === 'player',
                  'combatant-info__name--enemy': combatant.type === 'enemy',
                }"
              >
                {{ combatant.name }}
              </span>
              <span v-if="combatant.currentHp <= 0" class="combatant-info__dead-tag">💀 倒地</span>
            </div>

            <!-- HP Bar -->
            <div class="hp-bar-container" @click="openHpModal(combatant)">
              <div class="hp-bar">
                <div
                  class="hp-bar__fill"
                  :style="{
                    width: getHpPercentage(combatant) + '%',
                    backgroundColor: getHpColor(combatant),
                  }"
                ></div>
                <div class="hp-bar__shine"></div>
              </div>
              <span class="hp-bar__text">
                {{ combatant.currentHp }} / {{ combatant.maxHp }}
              </span>
            </div>

            <!-- Status Effects -->
            <div v-if="combatant.statusEffects.length > 0" class="status-effects">
              <span
                v-for="effect in combatant.statusEffects"
                :key="effect.name"
                class="status-badge"
                :style="{ borderColor: effect.color, color: effect.color }"
              >
                <span class="status-badge__icon">{{ effect.icon }}</span>
                {{ effect.name }}
              </span>
            </div>
          </div>

          <!-- Stats -->
          <div class="combatant-stats">
            <div class="initiative-value">
              <span class="initiative-value__number">{{ combatant.initiative }}</span>
              <span class="initiative-value__label">先攻</span>
            </div>
            <div class="ac-value">
              <span class="ac-value__shield">🛡️</span>
              <span class="ac-value__number">{{ combatant.armorClass }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== RIGHT COLUMN: D20 Roller ====== -->
    <div class="d20-panel">
      <div class="d20-panel__inner">
        <h2 class="d20-panel__title">🎲 骰子塔</h2>
        <div class="gold-divider"></div>

        <!-- D20 Display -->
        <div class="d20-display">
          <div
            class="d20-orb"
            :class="getD20Class()"
          >
            <span class="d20-orb__value">
              {{ isRolling ? rollingDisplay : (d20Result ?? '?') }}
            </span>
          </div>
          <div v-if="d20Result === 20 && !isRolling" class="nat20-banner">
            ✦ 大成功！ ✦
          </div>
          <div v-if="d20Result === 1 && !isRolling" class="nat1-banner">
            ✦ 大失败！ ✦
          </div>
        </div>

        <!-- Roll Button -->
        <button
          class="roll-btn"
          :class="{ 'roll-btn--rolling': isRolling }"
          :disabled="isRolling"
          @click="rollD20"
        >
          <span class="roll-btn__icon">🎲</span>
          <span class="roll-btn__text">{{ isRolling ? '投掷中...' : '投掷 D20' }}</span>
        </button>

        <div class="gold-divider"></div>

        <!-- Quick Stats -->
        <div v-if="activeCombatant" class="quick-stats">
          <h3 class="quick-stats__title">当前回合</h3>
          <div class="quick-stats__combatant">
            <span class="quick-stats__emoji">{{ activeCombatant.portraitEmoji }}</span>
            <span class="quick-stats__name">{{ activeCombatant.name }}</span>
          </div>
          <div class="quick-stats__grid">
            <div class="stat-cell">
              <span class="stat-cell__label">HP</span>
              <span class="stat-cell__value" :style="{ color: getHpColor(activeCombatant) }">
                {{ activeCombatant.currentHp }}/{{ activeCombatant.maxHp }}
              </span>
            </div>
            <div class="stat-cell">
              <span class="stat-cell__label">AC</span>
              <span class="stat-cell__value">{{ activeCombatant.armorClass }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-cell__label">先攻</span>
              <span class="stat-cell__value">{{ activeCombatant.initiative }}</span>
            </div>
          </div>
          <div v-if="activeCombatant.statusEffects.length > 0" class="quick-stats__effects">
            <span
              v-for="effect in activeCombatant.statusEffects"
              :key="effect.name"
              class="status-badge"
              :style="{ borderColor: effect.color, color: effect.color }"
            >
              {{ effect.icon }} {{ effect.name }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== HP MODAL ====== -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showHpModal" class="hp-modal-overlay" @click.self="closeHpModal">
          <div class="hp-modal">
            <button class="hp-modal__close" @click="closeHpModal">✕</button>
            <h2 class="hp-modal__title">
              {{ selectedCombatant?.portraitEmoji }} {{ selectedCombatant?.name }}
            </h2>
            <p class="hp-modal__subtitle">
              当前 HP：
              <strong :style="{ color: selectedCombatant ? getHpColor(selectedCombatant) : '' }">
                {{ selectedCombatant?.currentHp }}
              </strong>
              / {{ selectedCombatant?.maxHp }}
            </p>

            <div class="hp-modal__input-group">
              <label class="hp-modal__label">数值</label>
              <input
                v-model.number="hpChangeAmount"
                type="number"
                class="hp-modal__input"
                min="0"
                placeholder="输入数值..."
                @keyup.enter="applyDamage"
              />
            </div>

            <div class="hp-modal__actions">
              <button class="hp-modal__btn hp-modal__btn--damage" @click="applyDamage">
                ⚔ 造成伤害
              </button>
              <button class="hp-modal__btn hp-modal__btn--heal" @click="applyHeal">
                💚 治疗
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* =============================================
   Layout
   ============================================= */
.combat-tracker {
  display: flex;
  gap: var(--space-lg);
  height: 100%;
  min-height: 0;
}

/* =============================================
   Initiative Panel (Left Column)
   ============================================= */
.initiative-panel {
  flex: 7;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.initiative-header {
  flex-shrink: 0;
}

.initiative-header__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.initiative-header__title {
  font-family: var(--font-display);
  font-size: clamp(1.25rem, 2.5vw, 1.75rem);
  color: var(--antique-gold);
  text-shadow:
    0 2px 4px rgba(0, 0, 0, 0.6),
    0 0 20px rgba(201, 168, 76, 0.15);
  letter-spacing: 0.04em;
}

.initiative-header__badges {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.round-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--parchment-mid), var(--parchment-light));
  border: 1px solid var(--antique-gold-dim);
  font-family: var(--font-heading);
  font-size: 0.85rem;
  color: var(--antique-gold);
  box-shadow: var(--shadow-sm);
}

.round-badge__number {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--antique-gold-bright);
}

.turn-badge {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--parchment-mid);
  border: 1px solid var(--glass-border);
  font-size: 0.8rem;
  color: var(--text-secondary);
}

/* =============================================
   Action Bar
   ============================================= */
.action-bar {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  color: var(--text-secondary);
  font-family: var(--font-heading);
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition:
    all var(--transition-base);
}

.action-btn:hover {
  border-color: var(--antique-gold-dim);
  color: var(--text-primary);
  background: var(--parchment-mid);
  box-shadow: var(--shadow-sm);
  animation: none;
}

.action-btn--primary {
  border-color: var(--antique-gold-dim);
  color: var(--antique-gold);
}

.action-btn--primary:hover {
  border-color: var(--antique-gold);
  color: var(--antique-gold-bright);
  box-shadow: var(--shadow-gold);
}

.action-btn__icon {
  font-size: 0.65rem;
}

/* =============================================
   Combatant List
   ============================================= */
.combatant-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-sm) 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* =============================================
   Combatant Card
   ============================================= */
.combatant-card {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  box-shadow: var(--shadow-sm);
  transition:
    all var(--transition-base);
  animation: fadeSlideIn 0.4s ease both;
  animation-delay: calc(var(--i) * 80ms);
  position: relative;
  overflow: hidden;
}

.combatant-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: transparent;
  transition: background var(--transition-base);
}

.combatant-card:hover {
  border-color: rgba(201, 168, 76, 0.25);
  box-shadow: var(--shadow-md);
  transform: translateX(2px);
}

/* Active state */
.combatant-card--active {
  border-color: rgba(201, 168, 76, 0.4);
  animation: fadeSlideIn 0.4s ease both, breathGlow 3s ease-in-out infinite;
  animation-delay: calc(var(--i) * 80ms), 0s;
  background: rgba(26, 20, 16, 0.85);
}

.combatant-card--active::before {
  background: linear-gradient(
    180deg,
    var(--antique-gold) 0%,
    var(--antique-gold-dim) 100%
  );
}

/* Dead state */
.combatant-card--dead {
  opacity: 0.5;
  filter: saturate(0.3);
}

/* Flash animations */
.combatant-card--damage-flash {
  animation: damageFlash 0.6s ease !important;
}

.combatant-card--heal-flash {
  animation: healGlow 0.6s ease !important;
}

/* =============================================
   Portrait
   ============================================= */
.combatant-portrait {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--glass-border);
  background: var(--parchment-mid);
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base);
}

.combatant-portrait--player {
  border-color: var(--antique-gold-dim);
  box-shadow: 0 0 8px rgba(201, 168, 76, 0.1);
}

.combatant-portrait--enemy {
  border-color: var(--dragon-blood-dim);
  box-shadow: 0 0 8px rgba(139, 26, 26, 0.15);
}

.combatant-card--active .combatant-portrait--player {
  border-color: var(--antique-gold);
  box-shadow: 0 0 12px rgba(201, 168, 76, 0.3);
}

.combatant-portrait__emoji {
  font-size: 1.35rem;
  line-height: 1;
}

/* =============================================
   Combatant Info
   ============================================= */
.combatant-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.combatant-info__header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.combatant-info__name {
  font-family: var(--font-heading);
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.combatant-info__name--player {
  color: var(--antique-gold);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.combatant-info__name--enemy {
  color: var(--dragon-blood-glow);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.combatant-info__dead-tag {
  font-size: 0.7rem;
  color: var(--text-danger);
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(196, 50, 50, 0.15);
  border: 1px solid rgba(196, 50, 50, 0.3);
  white-space: nowrap;
}

/* =============================================
   HP Bar
   ============================================= */
.hp-bar-container {
  position: relative;
  cursor: pointer;
  padding: 2px 0;
}

.hp-bar-container:hover .hp-bar {
  box-shadow: 0 0 6px rgba(201, 168, 76, 0.15);
}

.hp-bar {
  position: relative;
  height: 14px;
  border-radius: 7px;
  background: var(--parchment-darkest);
  border: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
  transition: box-shadow var(--transition-base);
}

.hp-bar__fill {
  height: 100%;
  border-radius: 6px;
  transition:
    width 0.6s cubic-bezier(0.4, 0, 0.2, 1),
    background-color 0.6s ease;
  position: relative;
}

.hp-bar__shine {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.12) 0%,
    transparent 100%
  );
  pointer-events: none;
}

.hp-bar__text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 0.65rem;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--text-primary);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
  pointer-events: none;
  white-space: nowrap;
  letter-spacing: 0.05em;
}

/* =============================================
   Status Effects
   ============================================= */
.status-effects {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 0.65rem;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid;
  white-space: nowrap;
  line-height: 1.5;
}

.status-badge__icon {
  font-size: 0.7rem;
}

/* =============================================
   Stats (Right side of card)
   ============================================= */
.combatant-stats {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xs);
}

.initiative-value {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  background: var(--parchment-mid);
  border: 1px solid var(--glass-border);
  min-width: 44px;
}

.initiative-value__number {
  font-family: var(--font-heading);
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.initiative-value__label {
  font-size: 0.55rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-dim);
  margin-top: 1px;
}

.ac-value {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.ac-value__shield {
  font-size: 0.7rem;
}

.ac-value__number {
  font-family: var(--font-heading);
  font-weight: 700;
}

/* =============================================
   D20 Panel (Right Column)
   ============================================= */
.d20-panel {
  flex: 3;
  min-width: 240px;
  max-width: 340px;
}

.d20-panel__inner {
  position: sticky;
  top: 0;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.d20-panel__title {
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: var(--antique-gold);
  text-align: center;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

/* =============================================
   D20 Display
   ============================================= */
.d20-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md) 0;
}

.d20-orb {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 35% 35%, rgba(94, 77, 58, 0.6) 0%, transparent 60%),
    radial-gradient(circle at 65% 65%, rgba(15, 12, 8, 0.8) 0%, transparent 60%),
    linear-gradient(135deg, var(--parchment-light) 0%, var(--parchment-mid) 50%, var(--parchment-dark) 100%);
  border: 2px solid var(--glass-border);
  box-shadow:
    var(--shadow-md),
    inset 0 2px 8px rgba(0, 0, 0, 0.4),
    inset 0 -2px 8px rgba(201, 168, 76, 0.05);
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base);
  perspective: 600px;
}

.d20-orb__value {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
  transition: all var(--transition-fast);
}

/* Rolling state */
.d20-orb.rolling {
  animation: d20Spin 0.8s linear infinite;
  border-color: var(--antique-gold-dim);
  box-shadow:
    var(--shadow-md),
    0 0 20px rgba(201, 168, 76, 0.2);
}

.d20-orb.rolling .d20-orb__value {
  font-size: 1.6rem;
  color: var(--antique-gold);
}

/* Natural 20 */
.d20-orb.nat20 {
  border-color: var(--antique-gold-bright);
  animation: borderGlow 1.5s ease-in-out infinite;
  background:
    radial-gradient(circle at 35% 35%, rgba(201, 168, 76, 0.3) 0%, transparent 60%),
    radial-gradient(circle at 65% 65%, rgba(15, 12, 8, 0.8) 0%, transparent 60%),
    linear-gradient(135deg, var(--parchment-light) 0%, var(--parchment-mid) 50%, var(--parchment-dark) 100%);
}

.d20-orb.nat20 .d20-orb__value {
  color: var(--antique-gold-bright);
  animation: resultPop 0.4s ease both;
  text-shadow:
    0 0 10px rgba(232, 200, 76, 0.5),
    0 2px 4px rgba(0, 0, 0, 0.6);
}

/* Natural 1 */
.d20-orb.nat1 {
  border-color: var(--dragon-blood-glow);
  box-shadow:
    var(--shadow-dragon),
    inset 0 0 20px rgba(139, 26, 26, 0.2);
  animation: shake 0.5s ease both;
}

.d20-orb.nat1 .d20-orb__value {
  color: var(--dragon-blood-glow);
  animation: resultPop 0.4s ease both;
}

.nat20-banner {
  font-family: var(--font-display);
  font-size: 0.85rem;
  color: var(--antique-gold-bright);
  text-shadow: 0 0 10px rgba(232, 200, 76, 0.4);
  animation: resultPop 0.5s ease both;
  letter-spacing: 0.08em;
}

.nat1-banner {
  font-family: var(--font-display);
  font-size: 0.85rem;
  color: var(--dragon-blood-glow);
  text-shadow: 0 0 10px rgba(196, 50, 50, 0.4);
  animation: resultPop 0.5s ease both;
  letter-spacing: 0.08em;
}

/* =============================================
   Roll Button
   ============================================= */
.roll-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  width: 100%;
  padding: var(--space-md) var(--space-lg);
  border: 1px solid var(--antique-gold-dim);
  border-radius: var(--radius-md);
  background:
    linear-gradient(135deg, #3d3024, #4d3e2e, #5e4d3a, #4d3e2e, #3d3024);
  background-size: 200% auto;
  color: var(--antique-gold);
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition:
    all var(--transition-base);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.roll-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(201, 168, 76, 0.08) 40%,
    rgba(201, 168, 76, 0.15) 50%,
    rgba(201, 168, 76, 0.08) 60%,
    transparent 100%
  );
  transition: left 0.5s ease;
}

.roll-btn:hover:not(:disabled) {
  animation: goldShimmer 2s linear infinite;
  border-color: var(--antique-gold);
  box-shadow: var(--shadow-gold);
  color: var(--antique-gold-bright);
}

.roll-btn:hover:not(:disabled)::after {
  left: 100%;
}

.roll-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.roll-btn--rolling {
  opacity: 0.8;
  cursor: wait;
}

.roll-btn__icon {
  font-size: 1.2rem;
  animation: subtleFloat 2s ease-in-out infinite;
}

.roll-btn__text {
  position: relative;
  z-index: 1;
}

/* =============================================
   Quick Stats
   ============================================= */
.quick-stats {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.quick-stats__title {
  font-family: var(--font-heading);
  font-size: 0.8rem;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.quick-stats__combatant {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.quick-stats__emoji {
  font-size: 1.2rem;
}

.quick-stats__name {
  font-family: var(--font-heading);
  font-size: 0.9rem;
  color: var(--antique-gold);
  font-weight: 700;
}

.quick-stats__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
}

.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-sm);
  border-radius: var(--radius-sm);
  background: var(--parchment-mid);
  border: 1px solid var(--glass-border);
}

.stat-cell__label {
  font-size: 0.6rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-dim);
}

.stat-cell__value {
  font-family: var(--font-heading);
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.quick-stats__effects {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* =============================================
   HP Modal
   ============================================= */
.hp-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.hp-modal {
  position: relative;
  width: 90%;
  max-width: 400px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.hp-modal__close {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-dim);
  font-size: 0.85rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  padding: 0;
}

.hp-modal__close:hover {
  background: var(--parchment-light);
  color: var(--text-primary);
  animation: none;
}

.hp-modal__title {
  font-family: var(--font-heading);
  font-size: 1.2rem;
  color: var(--antique-gold);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.hp-modal__subtitle {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.hp-modal__input-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.hp-modal__label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.hp-modal__input {
  padding: var(--space-sm) var(--space-md);
  font-size: 1.2rem;
  font-family: var(--font-mono);
  text-align: center;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--parchment-darkest);
  color: var(--text-primary);
}

.hp-modal__input:focus {
  border-color: var(--antique-gold-dim);
  box-shadow: 0 0 0 3px rgba(201, 168, 76, 0.1);
  outline: none;
}

.hp-modal__actions {
  display: flex;
  gap: var(--space-sm);
}

.hp-modal__btn {
  flex: 1;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-family: var(--font-heading);
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-base);
  letter-spacing: 0.02em;
}

.hp-modal__btn--damage {
  background: linear-gradient(135deg, var(--dragon-blood-dim), var(--dragon-blood));
  border: 1px solid var(--dragon-blood-glow);
  color: #ffd4d4;
}

.hp-modal__btn--damage:hover {
  background: linear-gradient(135deg, var(--dragon-blood), var(--dragon-blood-bright));
  box-shadow: var(--shadow-dragon);
  animation: none;
}

.hp-modal__btn--heal {
  background: linear-gradient(135deg, #1a3d2a, #2a5e3c);
  border: 1px solid var(--hp-full);
  color: #b8f0c8;
}

.hp-modal__btn--heal:hover {
  background: linear-gradient(135deg, #2a5e3c, #3a7e4c);
  box-shadow: 0 0 15px rgba(74, 140, 92, 0.3);
  animation: none;
}

/* =============================================
   Modal Transitions
   ============================================= */
.modal-enter-active {
  animation: fadeIn 0.25s ease;
}

.modal-enter-active .hp-modal {
  animation: scaleIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-leave-to {
  opacity: 0;
}

.modal-leave-active .hp-modal {
  transition: all 0.2s ease;
  transform: scale(0.95);
  opacity: 0;
}

/* =============================================
   Responsive
   ============================================= */
@media (max-width: 768px) {
  .combat-tracker {
    flex-direction: column;
  }

  .d20-panel {
    max-width: none;
  }

  .d20-panel__inner {
    position: static;
  }
}
</style>
