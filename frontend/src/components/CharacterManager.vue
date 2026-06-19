<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { CharacterAPI } from '../api'
import StatsTab from './manager/StatsTab.vue'
import EquipmentTab from './manager/EquipmentTab.vue'
import SpellcastingTab from './manager/SpellcastingTab.vue'
import ProgressionTab from './manager/ProgressionTab.vue'

const props = defineProps<{ id: string }>()
const router = useRouter()
const character = ref<any>(null)
const activeTab = ref('stats')

const fetchCharacter = async () => {
  try {
    const res = await CharacterAPI.getById(parseInt(props.id))
    character.value = res
  } catch (e) {
    console.error(e)
    alert('无法加载角色信息')
    router.push('/roster')
  }
}

const saveCharacter = async () => {
  try {
    await CharacterAPI.update(parseInt(props.id), {
      background_story: character.value.background_story,
      personality_traits: character.value.personality_traits,
      ideals: character.value.ideals,
      bonds: character.value.bonds,
      flaws: character.value.flaws
    })
    alert('角色信息保存成功')
  } catch (e) {
    console.error(e)
    alert('保存失败')
  }
}

onMounted(() => {
  fetchCharacter()
})
</script>

<template>
  <div class="manager-container" v-if="character">
    <div class="top-bar">
      <div class="title-area">
        <button class="back-btn" @click="router.push('/roster')">← 返回酒馆</button>
        <h2>角色档案室: {{ character.name }}</h2>
      </div>
      <div class="actions">
        <button class="btn-secondary" @click="saveCharacter">保存更改</button>
        <button class="btn-secondary">导出 PDF</button>
      </div>
    </div>

    <div class="tabs">
      <button :class="['tab-btn', { active: activeTab === 'stats' }]" @click="activeTab = 'stats'">基础与属性</button>
      <button :class="['tab-btn', { active: activeTab === 'equipment' }]" @click="activeTab = 'equipment'">装备与物品</button>
      <button :class="['tab-btn', { active: activeTab === 'spells' }]" @click="activeTab = 'spells'">施法与法术</button>
      <button :class="['tab-btn', { active: activeTab === 'progression' }]" @click="activeTab = 'progression'">成长与升级</button>
    </div>

    <div class="tab-content glass-panel">
      <StatsTab v-if="activeTab === 'stats'" :character="character" />
      <EquipmentTab v-if="activeTab === 'equipment'" :character="character" @refresh="fetchCharacter" />
      <SpellcastingTab v-if="activeTab === 'spells'" :character="character" @refresh="fetchCharacter" />
      <ProgressionTab v-if="activeTab === 'progression'" :character="character" @refresh="fetchCharacter" />
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<style scoped>
.manager-container { padding: var(--space-md); max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; height: 100vh; }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-md); padding-bottom: var(--space-sm); border-bottom: 1px solid var(--glass-border); }
.title-area { display: flex; align-items: center; gap: var(--space-md); }
.back-btn { background: transparent; border: none; color: var(--antique-gold); cursor: pointer; font-family: var(--font-heading); font-size: 1rem; }
h2 { color: var(--text-primary); font-family: var(--font-heading); margin: 0; }
.actions { display: flex; gap: var(--space-sm); }
.btn-secondary { background: rgba(0,0,0,0.3); border: 1px solid var(--glass-border); color: var(--text-secondary); padding: 6px 12px; border-radius: var(--radius-sm); cursor: pointer; }
.btn-secondary:hover { color: var(--text-primary); border-color: var(--text-primary); }

.tabs { display: flex; gap: 4px; margin-bottom: -1px; z-index: 1; position: relative; }
.tab-btn { background: rgba(0,0,0,0.4); border: 1px solid var(--glass-border); border-bottom: none; color: var(--text-dim); padding: 10px 20px; border-radius: 8px 8px 0 0; cursor: pointer; font-family: var(--font-heading); font-size: 1.1rem; transition: all 0.2s; }
.tab-btn:hover { color: var(--text-primary); background: rgba(0,0,0,0.6); }
.tab-btn.active { background: rgba(20,20,20,0.8); color: var(--antique-gold); border-color: var(--antique-gold); text-shadow: 0 0 8px rgba(201, 168, 76, 0.4); }

.tab-content { flex: 1; padding: var(--space-lg); overflow-y: auto; border-top-left-radius: 0; border: 1px solid var(--antique-gold); }
.loading { display: flex; justify-content: center; align-items: center; height: 100vh; color: var(--antique-gold); font-size: 1.5rem; font-family: var(--font-heading); }
</style>
