<template>
  <div class="editor-page">
    <q-splitter
      class="editor-splitter"
      separator-class="bg-grey-4"
      :limits="[10, 90]"
      v-model="splitRatio"
    >
      <template #before>
        <div class="panel">
          <dbml-editor class="panel-content" :source="sourceText" @update:source="onSourceUpdate" />
        </div>
      </template>
      <template #after>
        <div class="panel">
          <dbml-graph class="panel-content" :schema="activeSchema" />
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import DbmlEditor from 'components/DbmlEditor.vue'
import DbmlGraph from 'components/DbmlGraph.vue'
import { useEditorStore } from 'src/store/editor'

const editor = useEditorStore()

const sourceText = computed({
  get: () => editor.getSourceText,
  set: (val) => editor.updateSourceText(val)
})

const activeSchema = computed(() => {
  const schemas = editor.database?.schemas
  return Array.isArray(schemas) && schemas.length > 0
    ? schemas[0]
    : { tableGroups: [], tables: [], refs: [] }
})

const splitRatio = computed({
  get: () => editor.getSplit,
  set: (val) => editor.updateSplit(val)
})

const onSourceUpdate = (val) => {
  sourceText.value = val
}
</script>

<style scoped>
.editor-page {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.editor-splitter {
  flex: 1 1 auto;
  min-height: 0;
  border: none;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  height: 100%;
}

.panel-content {
  flex: 1 1 auto;
  min-height: 0;
}
</style>
