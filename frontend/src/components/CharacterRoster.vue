<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CharacterAPI } from '../api'

const characters = ref<any[]>([])

onMounted(async () => {
  const data = await CharacterAPI.getAll()
  if (data) {
    characters.value = data
  }
})
</script>

<template>
  <div class="roster-container">
    <h2>角色一览 (Character Roster)</h2>
    <div class="card-grid">
      <div v-for="char in characters" :key="char.database_id" class="card char-card" @click="$router.push(`/manage/${char.database_id}`)">
        <div class="char-header">
          <div class="char-avatar">🧙</div>
          <div class="char-info">
            <h3>{{ char.name }}</h3>
            <div class="text-dim">Lv.{{ char.job.current_level }} {{ char.race.race_name }} {{ char.job.class_name }}</div>
          </div>
        </div>
        <div class="gold-divider"></div>
        <div class="char-stats">
          <span>HP: {{ char.current_hp }}/{{ char.hp }}</span>
          <span>AC: {{ char.ac }}</span>
        </div>
      </div>
      <div class="card char-card add-card" @click="$router.push('/create')">
        <div class="add-icon">+</div>
        <div>创建新角色</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.roster-container {
  padding: var(--space-lg);
}

h2 {
  color: var(--antique-gold);
  margin-bottom: var(--space-lg);
  font-family: var(--font-display);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-lg);
}

.char-card {
  padding: var(--space-md);
  cursor: pointer;
  transition: all var(--transition-base);
}

.char-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-gold);
  border-color: rgba(201, 168, 76, 0.4);
}

.char-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.char-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--parchment-mid);
  border: 1px solid var(--antique-gold);
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

.gold-divider {
  margin: var(--space-sm) 0;
}

.char-stats {
  display: flex;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.add-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  color: var(--text-dim);
  border-style: dashed;
}

.add-card:hover {
  color: var(--antique-gold);
}

.add-icon {
  font-size: 3rem;
  font-weight: 300;
}
</style>
