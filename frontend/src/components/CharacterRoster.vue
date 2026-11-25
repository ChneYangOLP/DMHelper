<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CharacterAPI } from '../api'

const characters = ref<any[]>([])
const loadError = ref('')

const loadCharacters = async () => {
  loadError.value = ''
  try {
    const data = await CharacterAPI.getAll()
    characters.value = data
  } catch (error) {
    console.error(error)
    characters.value = []
    loadError.value = error instanceof Error ? error.message : '无法连接角色服务'
  }
}

onMounted(loadCharacters)
</script>

<template>
  <section class="workspace-page">
    <header class="workspace-heading"><h1>角色一览</h1></header>
    <div v-if="loadError" class="load-error">
      <span>加载失败：{{ loadError }}</span>
      <button class="btn-secondary" @click="loadCharacters">重试</button>
    </div>
    <div class="card-grid">
      <router-link v-for="char in characters" :key="char.database_id" class="char-card" :to="`/manage/${char.database_id}`">
        <div class="char-header">
          <div class="char-avatar" aria-hidden="true">{{ Array.from(char.name)[0] }}</div>
          <div class="char-info">
            <h3>{{ char.name }}</h3>
            <div class="text-dim">Lv.{{ char.job.current_level }} {{ char.race.race_name }} {{ char.job.class_name }}</div>
          </div>
        </div>
        <div class="char-stats">
          <span>HP: {{ char.current_hp }}/{{ char.hp }}</span>
          <span>AC: {{ char.ac }}</span>
        </div>
      </router-link>
      <router-link class="char-card add-card" to="/create">
        <span class="add-icon" aria-hidden="true">+</span>
        <div>创建新角色</div>
      </router-link>
    </div>
  </section>
</template>

<style scoped>
.load-error {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  color: var(--text-danger);
  border-radius: var(--radius-sm);
  background: var(--surface-danger);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr));
  gap: var(--space-md);
}

.char-card {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-primary);
  transition: background-color var(--transition-fast);
}

.char-card:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.char-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.char-avatar {
  flex: 0 0 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  background: var(--surface-inset);
  color: var(--antique-gold);
  font-family: var(--font-heading);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.char-info h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--text-primary);
}

.char-stats {
  display: flex;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  min-height: 100px;
  color: var(--text-secondary);
}

.add-icon {
  font-size: 1.5rem;
  color: var(--antique-gold);
}
</style>
