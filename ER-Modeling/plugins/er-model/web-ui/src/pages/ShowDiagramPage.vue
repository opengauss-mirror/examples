<template>
  <div class="er-page column q-px-md">
    <!-- 🔧 工具栏 -->
    <div class="toolbar row items-center q-gutter-sm q-mb-sm">
      <!-- 选择数据库实例 -->
      <q-select
        dense
        outlined
        v-model="selectedInstance"
        :options="managedInstances"
        option-label="displayName"
        label="选择数据库实例"
        :loading="loadingInstances"
        class="toolbar-item"
        clearable
        @update:model-value="onInstanceSelect"
      >
        <template #append>
          <q-btn
            dense
            flat
            round
            size="sm"
            icon="refresh"
            color="red"
            @click.stop="fetchInstances"
            :loading="loadingInstances"
          />
        </template>
        <template #no-option>
          <q-item>
            <q-item-section class="text-grey">暂无可用实例</q-item-section>
          </q-item>
        </template>
      </q-select>

      <!-- Database -->
      <q-input
        dense
        outlined
        label="Database"
        v-model="databaseName"
        class="toolbar-item"
        :disable="!selectedInstance"
      />

      <!-- Schema -->
      <q-select
        dense
        outlined
        label="选择 Schema"
        v-model="selectedSchema"
        :options="availableSchemas"
        :loading="loadingSchemas"
        :disable="!selectedInstance"
        clearable
        class="toolbar-item"
      >
        <template #no-option>
          <q-item>
            <q-item-section class="text-grey">
              {{ selectedInstance ? '无可选 Schema' : '请先选择实例' }}
            </q-item-section>
          </q-item>
        </template>
      </q-select>

      <!-- 按钮 -->
      <div class="row items-center q-gutter-sm q-ml-sm">
        <q-btn
          padding="xs sm"
          size="md"
          unelevated
          class="base-ops-btn"
          label="加载 ER 图"
          :disable="!selectedInstance || !databaseName || !selectedSchema"
          :loading="loadingGraph"
          @click="loadSchemaGraph"
        />
        <q-btn
          padding="xs sm"
          size="md"
          unelevated
          class="base-ops-btn"
          icon="auto_awesome_mosaic"
          label="自动布局"
          @click="applyAutoLayout"
        />
        <q-btn
          padding="xs sm"
          size="md"
          unelevated
          class="base-ops-btn"
          icon="vertical_align_center"
          label="竖向布局"
          @click="applyVerticalLayout"
        />
      </div>
    </div>

    <!-- ER图区域 -->
    <div class="graph-container">
      <q-inner-loading :showing="loadingGraph">
        <q-spinner color="primary" size="42px" />
      </q-inner-loading>

      <dbml-graph v-if="schemaObject" :schema="schemaObject" class="fit" />
      <div v-else class="empty-state text-grey-6">
        请选择实例和 schema 后点击“加载 ER 图”。
      </div>
    </div>
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue'
  import { Notify } from 'quasar'
  import { Parser } from '@dbml/core'
  import DbmlGraph from 'components/DbmlGraph.vue'
  import { api } from 'src/boot/axios'
  import { useChartStore } from 'src/store/chart'

  const managedInstances = ref([])
  const loadingInstances = ref(false)
  const selectedInstance = ref(null)

  const availableSchemas = ref([])
  const loadingSchemas = ref(false)
  const selectedSchema = ref(null)

  const databaseName = ref('')
  const loadingGraph = ref(false)
  const schemaObject = ref(null)
  const chart = useChartStore()

  onMounted(() => fetchInstances())

  async function fetchInstances() {
    loadingInstances.value = true
    try {
      const response = await api.get('/execute/list-instances')
      managedInstances.value = response.data.instances || []
    } catch (error) {
      console.error('获取实例列表失败:', error)
      Notify.create({ type: 'negative', message: '获取实例列表失败' })
    } finally {
      loadingInstances.value = false
    }
  }

  async function onInstanceSelect(instance) {
    availableSchemas.value = []
    selectedSchema.value = null
    schemaObject.value = null
    chart.$reset()
    if (!instance) {
      databaseName.value = ''
      return
    }

    databaseName.value = instance.databaseName || ''

    loadingSchemas.value = true
    try {
      const response = await api.get('/execute/list-schemas', {
        params: {
          clusterId: instance.clusterId,
          nodeId: instance.nodeId,
          databaseName: instance.databaseName
        }
      })
      availableSchemas.value = response.data.schemas || []
      if (availableSchemas.value.length === 1) {
        selectedSchema.value = availableSchemas.value[0]
      }
    } catch (error) {
      console.error('获取 Schema 列表失败:', error)
      Notify.create({ type: 'negative', message: '获取 Schema 列表失败' })
    } finally {
      loadingSchemas.value = false
    }
  }

  async function loadSchemaGraph() {
    const instance = selectedInstance.value
    const schema = selectedSchema.value
    const dbName = (databaseName.value || '').trim()

    if (!instance || !schema || !dbName) {
      Notify.create({ type: 'warning', message: '请完整选择实例、数据库和 Schema' })
      return
    }

    loadingGraph.value = true
    try {
      const { data } = await api.get('/diagram/get-dbml-schema', {
        params: {
          clusterId: instance.clusterId,
          nodeId: instance.nodeId,
          databaseName: dbName,
          schemaName: schema
        }
      })

      const dbml = data?.dbmlString
      if (!dbml) {
        schemaObject.value = null
        Notify.create({ type: 'warning', message: '未获取到 DBML 数据' })
        return
      }

      const parsed = Parser.parse(dbml, 'dbml')
      parsed.normalize()
      chart.$reset()
      chart.loadDatabase(parsed)
      schemaObject.value = parsed.schemas?.[0] || { tableGroups: [], tables: [], refs: [] }
    } catch (error) {
      console.error('加载 ER 图失败:', error)
      Notify.create({ type: 'negative', message: error?.message || '加载 ER 图失败' })
    } finally {
      loadingGraph.value = false
    }
  }

  function applyAutoLayout() {
    chart.autoLayoutGrid()
  }
  function applyVerticalLayout() {
    chart.autoLayoutVertical()
  }
</script>

<style scoped>
  .er-page {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  /* === 工具栏样式（统一 with 设计ER图页） === */
  .toolbar {
    background-color: #fff;
    border-radius: 6px;
    padding: 6px 10px;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
    align-items: center;
  }

  .toolbar-item {
    min-width: 200px;
  }

  .base-ops-btn {
    background-color: #f23030 !important;
    border: 1px solid #f23030 !important;
    color: #ffffff !important;
    border-radius: 6px;
    box-shadow: none !important;
  }
  .base-ops-btn:hover {
    filter: brightness(1.05);
  }

  /* === 图区域 === */
  .graph-container {
    flex: 1;
    min-height: 0;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
    padding: 0.75rem;
    display: flex;
    align-items: stretch;
    justify-content: center;
    position: relative;
  }

  .empty-state {
    align-self: center;
    text-align: center;
    font-size: 16px;
  }
</style>
