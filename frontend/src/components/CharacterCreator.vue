<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { CharacterAPI } from '../api'

const router = useRouter()
const step = ref(1)
const isSubmitting = ref(false)

const form = ref({
  name: '', age: 20, gender: '男',
  race: '人类 (Human)', subrace: '',
  halfElfStat1: 'STR', halfElfStat2: 'DEX',
  job: '战士 (Fighter)',
  stats: { str: 10, dex: 10, con: 10, intel: 10, wis: 10, cha: 10 },
  background_story: '', personality_traits: '', ideals: '', bonds: '', flaws: '',
  skills: [] as string[],
  fightingStyle: '',
  sorcererOrigin: '',
  dragonAncestry: '',
  warlockPatron: ''
})

const races = [
  '人类 (Human)', '精灵 (Elf)', '矮人 (Dwarf)', '半身人 (Halfling)',
  '龙裔 (Dragonborn)', '侏儒 (Gnome)', '半精灵 (Half-Elf)',
  '半兽人 (Half-Orc)', '提夫林 (Tiefling)'
]
const classes = [
  '战士 (Fighter)', '法师 (Wizard)', '术士 (Sorcerer)',
  '邪术士 (Warlock)', '圣武士 (Paladin)', '吟游诗人 (Bard)'
]
const statNames = [
  { k: 'STR', n: '力量' }, { k: 'DEX', n: '敏捷' }, { k: 'CON', n: '体质' },
  { k: 'INT', n: '智力' }, { k: 'WIS', n: '感知' }, { k: 'CHA', n: '魅力' }
]

const subraceOptions = computed(() => {
  switch (form.value.race) {
    case '精灵 (Elf)': return [{ v: 'HIGH', l: '高等精灵' }, { v: 'WOOD', l: '木精灵' }, { v: 'DROW', l: '卓尔精灵' }]
    case '矮人 (Dwarf)': return [{ v: 'HILL', l: '丘陵矮人' }, { v: 'MOUNTAIN', l: '山地矮人' }]
    case '半身人 (Halfling)': return [{ v: 'LIGHTFOOT', l: '轻足半身人' }, { v: 'STOUT', l: '健壮半身人' }]
    case '龙裔 (Dragonborn)': return [
      { v: 'BLACK', l: '黑龙 (强酸)' }, { v: 'BLUE', l: '蓝龙 (闪电)' }, { v: 'BRASS', l: '黄铜龙 (火焰)' },
      { v: 'BRONZE', l: '青铜龙 (闪电)' }, { v: 'COPPER', l: '赤铜龙 (强酸)' }, { v: 'GOLD', l: '金龙 (火焰)' },
      { v: 'GREEN', l: '绿龙 (毒素)' }, { v: 'RED', l: '红龙 (火焰)' }, { v: 'SILVER', l: '银龙 (寒冷)' }, { v: 'WHITE', l: '白龙 (寒冷)' }
    ]
    case '侏儒 (Gnome)': return [{ v: 'FOREST', l: '森林侏儒' }, { v: 'ROCK', l: '岩侏儒' }]
    default: return []
  }
})

const handleRaceChange = () => {
  const opts = subraceOptions.value
  if (opts.length > 0) form.value.subrace = opts[0].v
  else form.value.subrace = ''
}

const classSkillData: Record<string, { count: number, options: string[] }> = {
  '战士 (Fighter)': { count: 2, options: ['Acrobatics (杂技)', 'Animal Handling (驯兽)', 'Athletics (运动)', 'History (历史)', 'Insight (洞悉)', 'Intimidation (威吓)', 'Perception (察觉)', 'Survival (生存)'] },
  '法师 (Wizard)': { count: 2, options: ['Arcana (奥秘)', 'History (历史)', 'Insight (洞悉)', 'Investigation (调查)', 'Medicine (医药)', 'Religion (宗教)'] },
  '术士 (Sorcerer)': { count: 2, options: ['Arcana (奥秘)', 'Deception (欺瞒)', 'Insight (洞悉)', 'Intimidation (威吓)', 'Persuasion (游说)', 'Religion (宗教)'] },
  '邪术士 (Warlock)': { count: 2, options: ['Arcana (奥秘)', 'Deception (欺瞒)', 'History (历史)', 'Intimidation (威吓)', 'Investigation (调查)', 'Nature (自然)', 'Religion (宗教)'] },
  '圣武士 (Paladin)': { count: 2, options: ['Athletics (运动)', 'Insight (洞悉)', 'Intimidation (威吓)', 'Medicine (医药)', 'Persuasion (游说)', 'Religion (宗教)'] },
  '吟游诗人 (Bard)': { count: 3, options: ['Acrobatics (杂技)', 'Animal Handling (驯兽)', 'Arcana (奥秘)', 'Athletics (运动)', 'Deception (欺瞒)', 'History (历史)', 'Insight (洞悉)', 'Intimidation (威吓)', 'Investigation (调查)', 'Medicine (医药)', 'Nature (自然)', 'Perception (察觉)', 'Performance (表演)', 'Persuasion (游说)', 'Religion (宗教)', 'Sleight of Hand (巧手)', 'Stealth (隐匿)', 'Survival (生存)'] }
}

const currentClassSkills = computed(() => classSkillData[form.value.job] || { count: 0, options: [] })

const handleClassChange = () => {
  form.value.skills = []
  form.value.fightingStyle = ''
  form.value.sorcererOrigin = ''
  form.value.dragonAncestry = ''
  form.value.warlockPatron = ''
}

const toggleSkill = (skill: string) => {
  const limit = currentClassSkills.value.count
  const idx = form.value.skills.indexOf(skill)
  if (idx !== -1) {
    form.value.skills.splice(idx, 1)
  } else if (form.value.skills.length < limit) {
    form.value.skills.push(skill)
  }
}

const nextStep = () => { if (step.value < 5) step.value++ }
const prevStep = () => { if (step.value > 1) step.value-- }
const finish = async () => {
  isSubmitting.value = true
  try {
    await CharacterAPI.create(form.value)
    router.push('/roster')
  } catch (e) {
    console.error(e)
    alert('创建失败，请查看控制台')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="creator-container">
    <button class="back-btn" @click="router.push('/roster')">← 返回</button>
    <h2>创建新角色 (Character Creator)</h2>
    
    <div class="stepper">
      <div :class="['step', { active: step >= 1 }]">1. 基础信息</div>
      <div :class="['step', { active: step >= 2 }]">2. 种族职业</div>
      <div :class="['step', { active: step >= 3 }]">3. 职业特性</div>
      <div :class="['step', { active: step >= 4 }]">4. 核心属性</div>
      <div :class="['step', { active: step >= 5 }]">5. 确认</div>
    </div>

    <div class="card glass-panel form-panel">
      <!-- Step 1 -->
      <div v-if="step === 1" class="step-content fade-in">
        <h3>冒险者身份</h3>
        <label>姓名 Name</label>
        <input type="text" v-model="form.name" class="glass-input" placeholder="输入角色名字..." />
        
        <div style="display: flex; gap: 16px;">
          <div style="flex: 1;">
            <label>年龄 Age</label>
            <input type="number" v-model="form.age" class="glass-input" />
          </div>
          <div style="flex: 1;">
            <label>性别 Gender</label>
            <select v-model="form.gender" class="glass-input">
              <option>男</option><option>女</option><option>无性别</option><option>其他</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Step 2 -->
      <div v-if="step === 2" class="step-content fade-in">
        <h3>血统与道途</h3>
        <label>种族 Race</label>
        <select v-model="form.race" class="glass-input" @change="handleRaceChange">
          <option v-for="r in races" :key="r" :value="r">{{ r }}</option>
        </select>
        
        <div v-if="subraceOptions.length > 0" class="sub-panel">
          <label>子种族 / 血脉 Subrace</label>
          <select v-model="form.subrace" class="glass-input">
            <option v-for="opt in subraceOptions" :key="opt.v" :value="opt.v">{{ opt.l }}</option>
          </select>
        </div>

        <div v-if="form.race === '半精灵 (Half-Elf)'" class="sub-panel">
          <label>半精灵属性加值 (任选两项不含魅力)</label>
          <div style="display: flex; gap: 16px;">
            <select v-model="form.halfElfStat1" class="glass-input">
              <option v-for="s in statNames.filter(x=>x.k!=='CHA')" :key="s.k" :value="s.k">{{ s.n }}</option>
            </select>
            <select v-model="form.halfElfStat2" class="glass-input">
              <option v-for="s in statNames.filter(x=>x.k!=='CHA')" :key="s.k" :value="s.k">{{ s.n }}</option>
            </select>
          </div>
        </div>
        
        <label>职业 Class</label>
        <select v-model="form.job" class="glass-input" @change="handleClassChange">
          <option v-for="c in classes" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>

      <!-- Step 3: Class Features & Skills -->
      <div v-if="step === 3" class="step-content fade-in">
        <h3>技能与特性 ({{ form.job }})</h3>
        
        <div class="sub-panel">
          <label>选择 {{ currentClassSkills.count }} 项技能熟练</label>
          <div class="skill-grid">
            <div v-for="skill in currentClassSkills.options" :key="skill" 
                 :class="['skill-tag', { active: form.skills.includes(skill), disabled: form.skills.length >= currentClassSkills.count && !form.skills.includes(skill) }]"
                 @click="toggleSkill(skill)">
              {{ skill }}
            </div>
          </div>
        </div>

        <div v-if="form.job === '战士 (Fighter)'" class="sub-panel">
          <label>选择 1 级战斗风格</label>
          <select v-model="form.fightingStyle" class="glass-input">
            <option value="Archery">箭术 (Archery)</option>
            <option value="Defense">防御 (Defense)</option>
            <option value="Dueling">对决 (Dueling)</option>
            <option value="Great Weapon Fighting">巨武器战斗 (Great Weapon Fighting)</option>
            <option value="Protection">保护 (Protection)</option>
            <option value="Two-Weapon Fighting">双武器战斗 (Two-Weapon Fighting)</option>
          </select>
        </div>

        <div v-if="form.job === '术士 (Sorcerer)'" class="sub-panel">
          <label>选择术法起源</label>
          <select v-model="form.sorcererOrigin" class="glass-input">
            <option value="龙脉术士 (Draconic Bloodline)">龙脉术士 (Draconic Bloodline)</option>
            <option value="狂野魔法术士 (Wild Magic)">狂野魔法术士 (Wild Magic)</option>
          </select>
          
          <div v-if="form.sorcererOrigin === '龙脉术士 (Draconic Bloodline)'" style="margin-top: 16px;">
            <label>龙脉先祖</label>
            <select v-model="form.dragonAncestry" class="glass-input">
              <option value="黑龙 (Black Dragon) - 强酸">黑龙 (强酸)</option>
              <option value="蓝龙 (Blue Dragon) - 闪电">蓝龙 (闪电)</option>
              <option value="黄铜龙 (Brass Dragon) - 火焰">黄铜龙 (火焰)</option>
              <option value="青铜龙 (Bronze Dragon) - 闪电">青铜龙 (闪电)</option>
              <option value="赤铜龙 (Copper Dragon) - 强酸">赤铜龙 (强酸)</option>
              <option value="金龙 (Gold Dragon) - 火焰">金龙 (火焰)</option>
              <option value="绿龙 (Green Dragon) - 毒素">绿龙 (毒素)</option>
              <option value="红龙 (Red Dragon) - 火焰">红龙 (火焰)</option>
              <option value="银龙 (Silver Dragon) - 寒冷">银龙 (寒冷)</option>
              <option value="白龙 (White Dragon) - 寒冷">白龙 (寒冷)</option>
            </select>
          </div>
        </div>

        <div v-if="form.job === '邪术士 (Warlock)'" class="sub-panel">
          <label>选择异界恩主</label>
          <select v-model="form.warlockPatron" class="glass-input">
            <option value="邪魔恩主 (The Fiend)">邪魔恩主 (The Fiend)</option>
            <option value="妖精恩主 (The Archfey)">妖精恩主 (The Archfey)</option>
            <option value="旧日支配者 (The Great Old One)">旧日支配者 (The Great Old One)</option>
          </select>
        </div>

      </div>

      <!-- Step 4 -->
      <div v-if="step === 4" class="step-content fade-in">
        <div style="display: flex; gap: 32px;">
          <div style="flex: 1;">
            <h3>核心属性 (Stats)</h3>
            <div class="stats-grid">
              <div class="stat-box" v-for="s in statNames" :key="s.k">
                <label>{{ s.n }}</label>
                <input type="number" v-model="(form.stats as any)[s.k.toLowerCase()]" class="glass-input stat-input" min="1" max="20" />
              </div>
            </div>
          </div>
          <div style="flex: 1;">
            <h3>角色背景与性格</h3>
            <input type="text" v-model="form.background_story" class="glass-input" placeholder="背景故事" />
            <input type="text" v-model="form.personality_traits" class="glass-input" placeholder="性格特点" />
            <input type="text" v-model="form.ideals" class="glass-input" placeholder="理想信念" />
            <input type="text" v-model="form.bonds" class="glass-input" placeholder="羁绊关系" />
            <input type="text" v-model="form.flaws" class="glass-input" placeholder="缺陷弱点" />
          </div>
        </div>
      </div>

      <!-- Step 5 -->
      <div v-if="step === 5" class="step-content fade-in">
        <h3>确认命运</h3>
        <div class="summary">
          <p><strong>姓名：</strong> {{ form.name || '未命名' }}</p>
          <p><strong>身份：</strong> {{ form.age }}岁 {{ form.gender }} {{ form.race }} {{ form.job }}</p>
          <p v-if="form.skills.length > 0"><strong>熟练技能：</strong> {{ form.skills.join('、') }}</p>
          <div class="gold-divider"></div>
          <p class="text-dim" v-if="!form.name">【警告】姓名不能为空，请返回第一步填写！</p>
          <p class="text-dim" v-if="form.skills.length < currentClassSkills.count">【警告】请在第三步选满 {{ currentClassSkills.count }} 项技能！</p>
          <p v-else class="text-success">角色已准备就绪，点击完成将其写入档案库。</p>
        </div>
      </div>

      <div class="actions">
        <button v-if="step > 1" @click="prevStep" class="btn-secondary" :disabled="isSubmitting">上一步</button>
        <button v-if="step < 5" @click="nextStep" class="btn-primary">下一步</button>
        <button v-if="step === 5" @click="finish" class="btn-primary" :disabled="!form.name || form.skills.length < currentClassSkills.count || isSubmitting">
          {{ isSubmitting ? '铭刻中...' : '完成创建' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.creator-container { padding: var(--space-lg); max-width: 800px; margin: 0 auto; }
.back-btn { background: transparent; border: none; color: var(--antique-gold); cursor: pointer; margin-bottom: var(--space-md); font-family: var(--font-heading); }
.stepper { display: flex; justify-content: space-between; margin-bottom: var(--space-lg); border-bottom: 1px solid var(--glass-border); padding-bottom: var(--space-md); font-size: 0.9rem; }
.step { color: var(--text-dim); transition: color var(--transition-fast); }
.step.active { color: var(--antique-gold); text-shadow: 0 0 8px rgba(201, 168, 76, 0.4); }

.form-panel { padding: var(--space-xl); min-height: 400px; display: flex; flex-direction: column; }
.step-content { flex: 1; }
h3 { color: var(--text-primary); margin-bottom: var(--space-md); font-family: var(--font-heading); }
label { display: block; color: var(--text-secondary); margin-bottom: var(--space-xs); font-size: 0.9rem; }
.glass-input { width: 100%; padding: var(--space-sm); margin-bottom: var(--space-md); background: rgba(0, 0, 0, 0.2); border: 1px solid var(--glass-border); color: var(--text-primary); border-radius: var(--radius-sm); font-family: var(--font-body); }
.glass-input:focus { border-color: var(--antique-gold); outline: none; box-shadow: 0 0 8px rgba(201, 168, 76, 0.2); }

.sub-panel { background: rgba(0,0,0,0.1); padding: var(--space-md); margin-bottom: var(--space-md); border-radius: var(--radius-sm); border: 1px solid var(--glass-border); }

.skill-grid { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.skill-tag { padding: 4px 12px; border: 1px solid var(--text-dim); border-radius: 16px; font-size: 0.85rem; cursor: pointer; color: var(--text-dim); transition: all var(--transition-fast); user-select: none; }
.skill-tag:hover:not(.disabled) { border-color: var(--text-primary); color: var(--text-primary); }
.skill-tag.active { background: rgba(201, 168, 76, 0.2); border-color: var(--antique-gold); color: var(--antique-gold); text-shadow: 0 0 4px rgba(201, 168, 76, 0.5); }
.skill-tag.disabled { opacity: 0.3; cursor: not-allowed; }

.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--space-sm); }
.stat-box { text-align: center; }
.stat-input { text-align: center; font-size: 1.2rem; font-family: var(--font-display); }

.summary { background: rgba(0,0,0,0.2); padding: var(--space-md); border-radius: var(--radius-sm); border: 1px solid var(--glass-border); line-height: 1.8; }
.actions { display: flex; justify-content: flex-end; gap: var(--space-md); margin-top: var(--space-lg); padding-top: var(--space-md); border-top: 1px solid var(--glass-border); }

.btn-primary { background: linear-gradient(180deg, var(--parchment-mid), var(--parchment-dark)); border: 1px solid var(--antique-gold); color: var(--antique-gold-bright); padding: var(--space-sm) var(--space-lg); border-radius: var(--radius-sm); cursor: pointer; transition: all var(--transition-fast); }
.btn-primary:hover:not(:disabled) { background: var(--parchment-light); box-shadow: var(--shadow-gold); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { background: transparent; border: 1px solid var(--text-dim); color: var(--text-secondary); padding: var(--space-sm) var(--space-lg); border-radius: var(--radius-sm); cursor: pointer; }
.btn-secondary:hover:not(:disabled) { color: var(--text-primary); border-color: var(--text-primary); }
</style>
