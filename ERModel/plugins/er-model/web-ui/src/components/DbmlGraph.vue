<template>
  <div class="dbml-graph-wrapper">
    <v-db-chart
      class="dbml-graph"
      :schema="schema"
      :table-groups="schema.tableGroups"
      :tables="schema.tables"
      :refs="schema.refs"
      @dblclick:table="locateInEditor"
      @dblclick:ref="locateInEditor"
      @dblclick:table-group="locateInEditor"
      @dblclick:field="locateInEditor"
    />

    <div class="dbml-structure-wrapper" v-if="false">
      <q-card class="shadow-6">
        <v-db-structure v-if="editor.database.schemas"
                        :database="editor.database"
        />
      </q-card>
    </div>

  </div>
</template>

<script setup>
  import {useEditorStore} from '../store/editor'
  import VDbChart from './VDbChart/VDbChart'
  import VDbStructure from './VDbStructure'

  const emit = defineEmits([
    'update:positions',
  ])
  const editor = useEditorStore()
  const props = defineProps({
    schema: {
      type: Object,
      required: true
    }
  })

  const locateInEditor = (e, thing) => {
    console.log("locateInEditor", e, thing);
    if (thing) {
      const token = thing.token
      editor.updateSelectionMarker(token.start, token.end)
    }
  }

</script>

<style scoped lang="scss">
  .dbml-graph-wrapper {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    position: relative;
  }

  .dbml-graph, .db-chart {
    height: 100% !important;
    width: 100% !important;
    flex: 1 1 auto;
    min-height: 0;
  }

  .dbml-structure-wrapper {
    width: 400px;
    max-height: 300px;
    height: 300px;
    align-self: start;
    position: absolute;
    bottom: 1rem;
    left: 1rem;
    margin-right: auto;

    > .q-card {
      max-height: 300px;
      overflow: auto;
    }
  }
</style>
