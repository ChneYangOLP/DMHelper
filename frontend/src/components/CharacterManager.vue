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

const handleCharacterRefresh = (updatedCharacter?: any) => {
  if (updatedCharacter && typeof updatedCharacter === 'object') {
    character.value = updatedCharacter
    return
  }
  void fetchCharacter()
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
        <button class="back-btn" @click="router.push('/roster')">← 角色酒馆</button>
        <h2>角色档案 · {{ character.name }}</h2>
      </div>
      <div class="actions">
        <button class="btn-secondary" @click="saveCharacter">保存更改</button>
      </div>
    </div>

    <div class="tabs">
      <button :class="['tab-btn', { active: activeTab === 'stats' }]" :aria-pressed="activeTab === 'stats'" @click="activeTab = 'stats'">基础与属性</button>
      <button :class="['tab-btn', { active: activeTab === 'equipment' }]" :aria-pressed="activeTab === 'equipment'" @click="activeTab = 'equipment'">装备与物品</button>
      <button :class="['tab-btn', { active: activeTab === 'spells' }]" :aria-pressed="activeTab === 'spells'" @click="activeTab = 'spells'">施法与法术</button>
      <button :class="['tab-btn', { active: activeTab === 'progression' }]" :aria-pressed="activeTab === 'progression'" @click="activeTab = 'progression'">成长与升级</button>
    </div>

    <div class="tab-content card">
      <StatsTab v-if="activeTab === 'stats'" :character="character" />
      <EquipmentTab v-if="activeTab === 'equipment'" :character="character" @refresh="fetchCharacter" />
      <SpellcastingTab v-if="activeTab === 'spells'" :character="character" @refresh="fetchCharacter" />
      <ProgressionTab v-if="activeTab === 'progression'" :character="character" @refresh="handleCharacterRefresh" />
    </div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<style scoped>
.manager-container { padding: var(--space-lg) var(--page-gutter) 72px; max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; min-height: 100%; }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-md); padding-bottom: var(--space-sm); flex-wrap: wrap; gap: var(--space-sm); }
.title-area { display: flex; align-items: center; gap: var(--space-md); flex-wrap: wrap; }
.back-btn { color: var(--text-secondary); cursor: pointer; font-family: var(--font-heading); font-size: 1rem; --control-bg: transparent; }
h2 { color: var(--antique-gold); font-family: var(--font-heading); margin: 0; }
.actions { display: flex; gap: var(--space-sm); }
.tabs { display: flex; gap: 4px; margin-bottom: var(--space-sm); z-index: 1; position: relative; flex-wrap: wrap; }
.tab-btn { color: var(--text-secondary); padding: 10px 20px; border-radius: var(--radius-sm); cursor: pointer; font-size: .875rem; --control-bg: var(--surface-card); --control-hover: var(--surface-hover); }
.tab-btn.active { color: var(--antique-gold); --control-bg: var(--surface-selected); }
.tab-content { flex: 1; padding: var(--space-lg); overflow-y: auto; }
.loading { display: flex; justify-content: center; align-items: center; height: 100vh; color: var(--antique-gold); font-size: 1.5rem; font-family: var(--font-heading); }
</style>
