<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ character: any }>()

const calcMod = (score: number) => Math.floor((score - 10) / 2)
const formatMod = (score: number) => {
  const mod = calcMod(score)
  return mod >= 0 ? `+${mod}` : `${mod}`
}

const statsList = computed(() => [
  { key: 'STR', name: '力量', val: props.character.stats.str },
  { key: 'DEX', name: '敏捷', val: props.character.stats.dex },
  { key: 'CON', name: '体质', val: props.character.stats.con },
  { key: 'INT', name: '智力', val: props.character.stats.intel },
  { key: 'WIS', name: '感知', val: props.character.stats.wis },
  { key: 'CHA', name: '魅力', val: props.character.stats.cha }
])

const savingThrowBonus = (statKey: string) => {
  const isProficient = props.character.job.saving_throws.includes(statKey)
  const pb = Math.floor((props.character.job.current_level - 1) / 4) + 2
  const statVal = props.character.stats[statKey.toLowerCase() === 'intelligence' ? 'intel' : statKey.toLowerCase().substring(0, 3)] || 10
  const total = calcMod(statVal) + (isProficient ? pb : 0)
  return total >= 0 ? `+${total}` : `${total}`
}
</script>

<template>
  <div class="stats-tab">
    <div class="panel-grid">
      <!-- Left Column -->
      <div class="column">
        <div class="info-card">
          <h3>基础属性 (Base)</h3>
          <div class="info-row">
            <span class="label">种族:</span> <span class="val">{{ character.race.race_name }}</span>
          </div>
          <div class="info-row" v-if="character.race.subrace_name">
            <span class="label">血统:</span> <span class="val">{{ character.race.subrace_name }}</span>
          </div>
          <div class="info-row">
            <span class="label">职业:</span> <span class="val">{{ character.job.class_name }} (Lv.{{ character.job.current_level }})</span>
          </div>
          <div class="info-row">
            <span class="label">经验:</span> <span class="val">{{ character.experience_points }} XP</span>
          </div>
          <div class="info-row">
            <span class="label">年龄/性别:</span> <span class="val">{{ character.age }} / {{ character.gender }}</span>
          </div>
        </div>

        <div class="combat-stats-grid">
          <div class="stat-box hp-box">
            <div class="stat-label">HP</div>
            <div class="stat-val text-success">{{ character.current_hp }} / {{ character.max_hp || character.hp }}</div>
          </div>
          <div class="stat-box ac-box">
            <div class="stat-label">AC</div>
            <div class="stat-val">{{ character.ac }}</div>
          </div>
          <div class="stat-box">
            <div class="stat-label">速度</div>
            <div class="stat-val">{{ character.race.base_speed }}尺</div>
          </div>
          <div class="stat-box">
            <div class="stat-label">先攻</div>
            <div class="stat-val">{{ formatMod(character.stats.dex) }}</div>
          </div>
        </div>

        <div class="ability-scores">
          <div class="score-box" v-for="s in statsList" :key="s.key">
            <div class="score-name">{{ s.name }}</div>
            <div class="score-val">{{ s.val }}</div>
            <div class="score-mod">{{ formatMod(s.val) }}</div>
          </div>
        </div>

        <div class="info-card">
          <h3>豁免检定 (Saving Throws)</h3>
          <div class="saving-throws">
            <div class="st-item"><span class="label">力量:</span> <span class="val">{{ savingThrowBonus('Strength') }}</span></div>
            <div class="st-item"><span class="label">敏捷:</span> <span class="val">{{ savingThrowBonus('Dexterity') }}</span></div>
            <div class="st-item"><span class="label">体质:</span> <span class="val">{{ savingThrowBonus('Constitution') }}</span></div>
            <div class="st-item"><span class="label">智力:</span> <span class="val">{{ savingThrowBonus('Intelligence') }}</span></div>
            <div class="st-item"><span class="label">感知:</span> <span class="val">{{ savingThrowBonus('Wisdom') }}</span></div>
            <div class="st-item"><span class="label">魅力:</span> <span class="val">{{ savingThrowBonus('Charisma') }}</span></div>
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="column">
        <div class="info-card editable-card">
          <h3>角色设定 (Profile)</h3>
          <label>背景故事</label>
          <textarea v-model="character.background_story" class="glass-textarea" rows="3"></textarea>
          <label>性格特点</label>
          <textarea v-model="character.personality_traits" class="glass-textarea" rows="2"></textarea>
          <label>理想信念</label>
          <textarea v-model="character.ideals" class="glass-textarea" rows="2"></textarea>
          <label>羁绊关系</label>
          <textarea v-model="character.bonds" class="glass-textarea" rows="2"></textarea>
          <label>缺陷弱点</label>
          <textarea v-model="character.flaws" class="glass-textarea" rows="2"></textarea>
          <div class="hint-text">* 修改后请点击顶部"保存更改"按钮。</div>
        </div>

        <div class="info-card list-card">
          <h3>种族特性 (Racial Traits)</h3>
          <ul v-if="character.race.racial_traits && character.race.racial_traits.length > 0">
            <li v-for="(trait, idx) in character.race.racial_traits" :key="idx">
              {{ trait }}
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-tab { display: flex; flex-direction: column; height: 100%; }
.panel-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-lg); }
.column { display: flex; flex-direction: column; gap: var(--space-md); }

.info-card { background: rgba(0,0,0,0.2); padding: var(--space-md); border-radius: var(--radius-md); border: 1px solid var(--glass-border); }
.info-card h3 { color: var(--antique-gold); font-family: var(--font-heading); margin-bottom: var(--space-sm); border-bottom: 1px solid var(--glass-border); padding-bottom: 4px; }
.info-row { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px dashed rgba(255,255,255,0.1); }
.info-row .label { color: var(--text-secondary); }
.info-row .val { color: var(--text-primary); font-weight: bold; }

.combat-stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.stat-box { background: rgba(0,0,0,0.3); border: 1px solid var(--glass-border); border-radius: var(--radius-sm); text-align: center; padding: var(--space-sm) 0; }
.hp-box { border-color: rgba(76, 175, 80, 0.5); }
.ac-box { border-color: rgba(33, 150, 243, 0.5); }
.stat-label { font-size: 0.8rem; color: var(--text-dim); text-transform: uppercase; }
.stat-val { font-size: 1.4rem; font-family: var(--font-display); font-weight: bold; color: var(--text-primary); }

.ability-scores { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
.score-box { background: rgba(0,0,0,0.2); border: 1px solid var(--glass-border); border-radius: var(--radius-sm); text-align: center; padding: 8px 0; }
.score-name { font-size: 0.8rem; color: var(--text-secondary); }
.score-val { font-size: 1.4rem; font-family: var(--font-display); font-weight: bold; color: var(--text-primary); margin: 4px 0; }
.score-mod { font-size: 0.9rem; color: var(--antique-gold); background: rgba(0,0,0,0.3); border-radius: 12px; margin: 0 10px; }

.saving-throws { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.st-item { display: flex; justify-content: space-between; padding: 4px 8px; background: rgba(255,255,255,0.03); border-radius: 4px; }
.st-item .val { font-family: var(--font-display); color: var(--antique-gold); }

.editable-card label { display: block; margin-top: 8px; color: var(--text-secondary); font-size: 0.9rem; }
.glass-textarea { width: 100%; background: rgba(0,0,0,0.15); border: 1px solid var(--glass-border); color: var(--text-primary); padding: 8px; border-radius: 4px; margin-top: 4px; font-family: var(--font-body); resize: vertical; }
.glass-textarea:focus { border-color: var(--antique-gold); outline: none; }
.hint-text { font-size: 0.8rem; color: var(--text-dim); margin-top: 8px; font-style: italic; }

.list-card ul { margin: 0; padding-left: 20px; color: var(--text-secondary); line-height: 1.6; }
.list-card strong { color: var(--text-primary); }
</style>
