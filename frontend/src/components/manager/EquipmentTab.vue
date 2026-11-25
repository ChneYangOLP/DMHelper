<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { CharacterAPI, ItemAPI } from '../../api'

const props = defineProps<{ character: any }>()
const emit = defineEmits(['refresh'])

const armor = ref(props.character.equipped_armor_key || '')
const mainHand = ref(props.character.equipped_main_hand_key || '')
const offHand = ref(props.character.equipped_off_hand_key || '')
const cloak = ref(props.character.equipped_cloak_key || '')
const accessory = ref(props.character.equipped_accessory_key || '')

watch(() => props.character, (character) => {
  armor.value = character.equipped_armor_key || ''
  mainHand.value = character.equipped_main_hand_key || ''
  offHand.value = character.equipped_off_hand_key || ''
  cloak.value = character.equipped_cloak_key || ''
  accessory.value = character.equipped_accessory_key || ''
  if (selectedItemKey.value && !(character.inventory || []).some((item: any) => item.key === selectedItemKey.value)) {
    selectedItemKey.value = ''
  }
})

const selectedCategory = ref('全部')
const categories = ['全部', '消耗品', '材料/战利品', '工具/任务', '自定义']
const selectedItemKey = ref('')

const isApplying = ref(false)
const showShop = ref(false)
const shopQuery = ref('')
const shopResults = ref<any[]>([])

const allOwnedItems = computed(() => {
  return props.character.inventory || []
})

const getItemsForSlot = (slot: string) => {
  return allOwnedItems.value.filter((i: any) => i.slot === slot || i.slot === 'ANY')
}

const filteredInventory = computed(() => {
  if (selectedCategory.value === '全部') return allOwnedItems.value
  return allOwnedItems.value.filter((i: any) => i.inventory_category === selectedCategory.value)
})

const selectedItem = computed(() => {
  return allOwnedItems.value.find((i: any) => i.key === selectedItemKey.value)
})

const formatCp = (cp: number) => {
  const safeCp = Math.max(0, Math.floor(cp))
  const parts: string[] = []
  const gp = Math.floor(safeCp / 100)
  const sp = Math.floor((safeCp % 100) / 10)
  const copper = safeCp % 10
  if (gp > 0) parts.push(`${gp} gp`)
  if (sp > 0) parts.push(`${sp} sp`)
  if (copper > 0 || parts.length === 0) parts.push(`${copper} cp`)
  return parts.join(' ')
}

const applyEquipment = async () => {
  isApplying.value = true
  try {
    const id = props.character.database_id
    await CharacterAPI.equipItem(id, 'ARMOR', armor.value)
    await CharacterAPI.equipItem(id, 'MAIN_HAND', mainHand.value)
    await CharacterAPI.equipItem(id, 'OFF_HAND', offHand.value)
    await CharacterAPI.equipItem(id, 'CLOAK', cloak.value)
    await CharacterAPI.equipItem(id, 'ACCESSORY', accessory.value)
    emit('refresh')
  } catch (error) {
    console.error(error)
    alert(error instanceof Error ? `应用失败：${error.message}` : '应用失败')
  } finally {
    isApplying.value = false
  }
}

const handleUse = async () => {
  if (!selectedItemKey.value) return
  try {
    await CharacterAPI.manageInventory(props.character.database_id, 'USE', selectedItemKey.value, 1)
    emit('refresh')
  } catch (error) {
    console.error(error)
    alert(error instanceof Error ? `使用失败：${error.message}` : '使用失败')
  }
}

const handleSell = async () => {
  if (!selectedItemKey.value) return
  const qty = prompt('出售数量:', '1')
  if (!qty) return
  const quantity = Number(qty)
  if (!Number.isInteger(quantity) || quantity <= 0) {
    alert('请输入大于 0 的整数数量')
    return
  }
  if (quantity > (selectedItem.value?.count || 1)) {
    alert('出售数量不能超过持有数量')
    return
  }
  try {
    await CharacterAPI.manageInventory(props.character.database_id, 'SELL', selectedItemKey.value, quantity)
    emit('refresh')
  } catch (error) {
    console.error(error)
    alert(error instanceof Error ? `出售失败：${error.message}` : '出售失败')
  }
}

const searchShop = async () => {
  try {
    shopResults.value = await ItemAPI.search(shopQuery.value)
  } catch (error) {
    console.error(error)
    shopResults.value = []
    alert(error instanceof Error ? `搜索失败：${error.message}` : '搜索失败')
  }
}

const buyItem = async (item: any) => {
  const isStackable = item.slot === 'BACKPACK'
  const qty = isStackable
    ? prompt(`购买 ${item.display_name} 的数量 (单价: ${formatCp(item.value_in_cp)}):`, '1')
    : '1'
  if (qty === null || qty.trim() === '') return
  const quantity = Number(qty)
  if (!Number.isInteger(quantity) || quantity <= 0) {
    alert('请输入大于 0 的整数数量')
    return
  }
  try {
    await CharacterAPI.manageInventory(props.character.database_id, 'BUY', item.key, quantity)
    alert('购买成功')
    emit('refresh')
  } catch(error) {
    console.error(error)
    alert(error instanceof Error ? `购买失败：${error.message}` : '购买失败')
  }
}
</script>

<template>
  <div class="equipment-tab">
    <div class="panel-grid">
      <!-- Equipment Slots -->
      <div class="column">
        <div class="info-card">
          <h3>装备槽位 (Equipment Slots)</h3>
          <div class="slot-group">
            <label>护甲 (Armor)</label>
            <select v-model="armor" class="glass-input">
              <option value="">[空置]</option>
              <option v-for="i in getItemsForSlot('ARMOR')" :key="i.key" :value="i.key">{{ i.display_name }}</option>
            </select>
          </div>
          <div class="slot-group">
            <label>主手 (Main Hand)</label>
            <select v-model="mainHand" class="glass-input">
              <option value="">[空置]</option>
              <option v-for="i in getItemsForSlot('MAIN_HAND')" :key="i.key" :value="i.key">{{ i.display_name }}</option>
            </select>
          </div>
          <div class="slot-group">
            <label>副手/盾牌 (Off Hand)</label>
            <select v-model="offHand" class="glass-input">
              <option value="">[空置]</option>
              <option v-for="i in getItemsForSlot('OFF_HAND')" :key="i.key" :value="i.key">{{ i.display_name }}</option>
            </select>
          </div>
          <div class="slot-group">
            <label>披风 (Cloak)</label>
            <select v-model="cloak" class="glass-input">
              <option value="">[空置]</option>
              <option v-for="i in getItemsForSlot('CLOAK')" :key="i.key" :value="i.key">{{ i.display_name }}</option>
            </select>
          </div>
          <div class="slot-group">
            <label>护符 (Accessory)</label>
            <select v-model="accessory" class="glass-input">
              <option value="">[空置]</option>
              <option v-for="i in getItemsForSlot('ACCESSORY')" :key="i.key" :value="i.key">{{ i.display_name }}</option>
            </select>
          </div>
          <button class="btn-primary w-full mt-4" @click="applyEquipment" :disabled="isApplying">
            {{ isApplying ? '应用中...' : '应用当前装备' }}
          </button>
        </div>
        
        <div class="info-card wallet-card">
          <h3>钱包 (Wallet)</h3>
          <div class="coins">
            <div class="coin gp"><span>{{ character.currency_gp || character.gold_pieces || 0 }}</span> GP</div>
            <div class="coin sp"><span>{{ character.currency_sp || character.silver_pieces || 0 }}</span> SP</div>
            <div class="coin cp"><span>{{ character.currency_cp || character.copper_pieces || 0 }}</span> CP</div>
          </div>
          <div class="total-value">总值: {{ formatCp((character.currency_gp || character.gold_pieces || 0) * 100 + (character.currency_sp || character.silver_pieces || 0) * 10 + (character.currency_cp || character.copper_pieces || 0)) }}</div>
        </div>
      </div>

      <!-- Backpack & Actions -->
      <div class="column flex-2">
        <div class="info-card backpack-card">
          <div class="flex-between">
            <h3>背包物品 (Inventory)</h3>
            <div class="actions-mini">
              <button class="btn-secondary" @click="showShop = true">商店/添加</button>
            </div>
          </div>
          
          <div class="filter-bar">
            <select v-model="selectedCategory" class="glass-input slim">
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>

          <div class="inventory-list">
            <div v-for="item in filteredInventory" :key="item.key" 
                 :class="['inv-item', { active: selectedItemKey === item.key }]"
                 @click="selectedItemKey = item.key">
              <div class="item-name">{{ item.display_name }} <span class="item-qty">x{{ item.count || 1 }}</span></div>
              <div class="item-cat">{{ item.inventory_category }}</div>
            </div>
            <div v-if="filteredInventory.length === 0" class="empty-state">背包空空如也</div>
          </div>

          <div class="item-detail-panel" v-if="selectedItem">
            <h4>{{ selectedItem.display_name }}</h4>
            <div class="detail-row"><span class="text-dim">分类:</span> {{ selectedItem.inventory_category }}</div>
            <div class="detail-row"><span class="text-dim">描述:</span> {{ selectedItem.description }}</div>
            <div class="detail-row"><span class="text-dim">参考价:</span> {{ formatCp(selectedItem.value_in_cp) }} (出售价: {{ formatCp(Math.floor(selectedItem.value_in_cp / 2)) }})</div>
            
            <div class="action-buttons mt-4">
              <button v-if="selectedItem.auto_usable" class="btn-primary" @click="handleUse">使用</button>
              <button class="btn-secondary" @click="handleSell">出售</button>
            </div>
          </div>
          <div class="item-detail-panel empty" v-else>
            请选择一个物品查看详情
          </div>
        </div>
      </div>
    </div>

    <!-- Shop Modal -->
    <div v-if="showShop" class="modal-overlay" @click.self="showShop = false">
      <div class="modal-content">
        <div class="flex-between mb-4">
          <h3>搜寻物品</h3>
          <button class="close-btn" @click="showShop = false">×</button>
        </div>
        <div class="flex gap-2 mb-4">
          <input type="text" v-model="shopQuery" class="glass-input flex-1" placeholder="搜索物品库..." @keyup.enter="searchShop" />
          <button class="btn-primary" @click="searchShop">搜索</button>
        </div>
        <div class="shop-results">
          <div v-for="res in shopResults" :key="res.key" class="shop-item">
            <div class="flex-1">
              <div class="fw-bold">{{ res.display_name }}</div>
              <div class="text-dim text-sm">{{ res.description }}</div>
            </div>
            <div class="price">{{ formatCp(res.value_in_cp) }}</div>
            <button class="btn-secondary slim" @click="buyItem(res)">购买</button>
          </div>
          <div v-if="shopResults.length === 0" class="text-center text-dim mt-4">未找到物品，请尝试搜索</div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.equipment-tab { height: 100%; display: flex; flex-direction: column; }
.panel-grid { display: flex; gap: var(--space-lg); height: 100%; }
.column { display: flex; flex-direction: column; gap: var(--space-md); flex: 1; }
.flex-2 { flex: 2; }
.info-card { background: var(--surface-card); padding: var(--space-md); border-radius: var(--radius-md); }
h3 { color: var(--antique-gold); font-family: var(--font-heading); margin-bottom: var(--space-sm); padding-bottom: 4px; }
.slot-group { margin-bottom: 12px; }
.slot-group label { display: block; font-size: 0.85rem; color: var(--text-dim); margin-bottom: 4px; }
.glass-input { width: 100%; color: var(--text-primary); padding: 8px; border-radius: 4px; background: var(--surface-inset); }
.glass-input.slim { padding: 4px 8px; }
.wallet-card .coins { display: flex; gap: 16px; margin-top: 12px; }
.coin { font-family: var(--font-display); background: var(--surface-inset); padding: 8px 16px; border-radius: 8px; flex: 1; text-align: center; }
.coin span { font-size: 1.4rem; font-weight: bold; }
.gp { color: var(--antique-gold); }
.sp { color: var(--text-primary); }
.cp { color: var(--text-success); }
.total-value { text-align: right; margin-top: 8px; color: var(--text-dim); font-size: 0.9rem; }
.backpack-card { flex: 1; display: flex; flex-direction: column; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 12px; }
.inventory-list { flex: 1; border-radius: 4px; overflow-y: auto; max-height: 250px; display: grid; gap: 4px; align-content: start; }
.inv-item { display: flex; justify-content: space-between; padding: 8px 12px; cursor: pointer; transition: background 0.2s; background: var(--surface-inset); border-radius: var(--radius-sm); }
.inv-item:hover { background: var(--surface-hover); }
.inv-item.active { background: var(--surface-selected); }
.item-name { font-weight: bold; color: var(--text-primary); }
.item-qty { color: var(--antique-gold); font-size: 0.9rem; margin-left: 8px; }
.item-cat { color: var(--text-dim); font-size: 0.85rem; }
.empty-state { padding: 20px; text-align: center; color: var(--text-dim); font-style: italic; }
.item-detail-panel { margin-top: 16px; padding: 16px; border-radius: 8px; background: var(--surface-card); }
.item-detail-panel.empty { text-align: center; color: var(--text-dim); }
.item-detail-panel h4 { color: var(--antique-gold); margin-bottom: 12px; font-size: 1.1rem; }
.detail-row { margin-bottom: 6px; font-size: 0.95rem; }
.action-buttons { display: flex; gap: 8px; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: var(--scrim); display: flex; justify-content: center; align-items: center; z-index: 100; }
.modal-content { background: var(--surface-overlay); padding: 24px; border-radius: 8px; width: 600px; max-width: 90vw; }
.close-btn { color: var(--text-dim); font-size: 1.5rem; cursor: pointer; --control-bg: transparent; }
.close-btn:hover { color: var(--text-primary); }
.shop-results { max-height: 400px; overflow-y: auto; border-radius: 4px; padding: 8px; display: grid; gap: 4px; }
.shop-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: var(--surface-card); border-radius: var(--radius-sm); flex-wrap: wrap; }
.shop-item .price { color: var(--antique-gold); font-family: var(--font-display); width: 80px; text-align: right; }
.w-full { width: 100%; }
.mt-4 { margin-top: 16px; }
.mb-4 { margin-bottom: 16px; }
.flex { display: flex; }
.gap-2 { gap: 8px; }
.flex-1 { flex: 1; }
.text-dim { color: var(--text-dim); }
.text-sm { font-size: 0.85rem; }
.fw-bold { font-weight: bold; }
.text-center { text-align: center; }
.slim { padding: 4px 12px; }
</style>
