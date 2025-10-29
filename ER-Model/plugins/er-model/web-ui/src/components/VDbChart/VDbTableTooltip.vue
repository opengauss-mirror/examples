<template>
  <div class="dbtt">
    <q-card-section class="dbtt-header">
      <div class="dbtt-title">
        <span class="dbtt-label">Table:</span>
        <span class="dbtt-name">{{ table.name }}</span>
      </div>
    </q-card-section>
    <q-separator v-if="note" />
    <q-card-section v-if="note" class="dbtt-body">
      <div class="dbtt-note">{{ note }}</div>
    </q-card-section>
  </div>
  
</template>

<script setup>
  import { computed } from 'vue'
  const props = defineProps({
    table: Object
  })
  const note = computed(() => props.table?.note || '')
</script>

<style scoped>
.dbtt { display: flex; flex-direction: column; gap: 4px; font-size: 12px; line-height: 1.35; }
.dbtt-header { padding: 0; }
.dbtt-title { font-weight: 700; color: #f8fafc; letter-spacing: .08em; text-transform: uppercase; }
.dbtt-label { opacity: .85; margin-right: 4px; font-weight: 600; }
.dbtt-name { font-weight: 700; }
.dbtt-body { padding: 0; }
.dbtt-note { color: #e2e8f0; opacity: .95; white-space: pre-wrap; max-width: 18rem; }
.q-separator { background: rgba(248,250,252,.16); height: 1px; margin: 6px 0; }
</style>
