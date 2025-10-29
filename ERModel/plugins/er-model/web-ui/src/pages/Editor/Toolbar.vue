<template>
  <div class="q-py-sm flex items-center">
    <!-- 导出 -->
    <q-btn-dropdown
      padding="xs sm"
      size="md"
      class="q-mx-xs red-btn"
    >
      <template #label>
        <q-icon class="q-mr-sm" size="xs" name="file_download" />
        {{ $t('export') }}
      </template>

      <q-list dense>
        <q-item
          v-for="exportOption in exportOptions"
          :key="exportOption.id"
          clickable
          dense
          @click="onClickExport(exportOption.id)"
        >
          <q-item-section>
            <q-item-label>{{ exportOption.label }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>

    <!-- 导入 -->
    <q-btn-dropdown
      padding="xs sm"
      size="md"
      class="q-mx-xs red-btn"
    >
      <template #label>
        <q-icon class="q-mr-sm" size="xs" name="file_upload" />
        {{ $t('import') }}
      </template>

      <q-list dense>
        <q-item clickable dense @click="onImportSql('POSTGRESQL')">
          <q-item-section>从 SQL 文件导入（PostgreSQL）</q-item-section>
        </q-item>
        <q-item clickable dense @click="onImportSql('MYSQL')">
          <q-item-section>从 SQL 文件导入（MySQL）</q-item-section>
        </q-item>
        <q-item clickable dense @click="onImportSql('OPENGAUSS')">
          <q-item-section>从 SQL 文件导入（openGauss）</q-item-section>
        </q-item>
      </q-list>
    </q-btn-dropdown>

    <!-- 自动布局 -->
    <q-btn
      padding="xs sm"
      size="md"
      class="q-mx-xs red-btn"
      icon="auto_awesome_mosaic"
      label="自动布局"
      @click="runAutoLayout('LR')"
    />

    <!-- 竖向布局 -->
    <q-btn
      padding="xs sm"
      size="md"
      class="q-mx-xs red-btn"
      icon="vertical_align_center"
      label="竖向布局"
      @click="runAutoLayout('TB')"
    />

    <!-- 实例选择 -->
    <div class="q-mx-xs">
      <q-select
        :model-value="localInstance"
        @update:model-value="onChangeInstance"
        :options="managedInstances"
        option-label="displayName"
        label="选择数据库实例"
        style="min-width: 250px;"
        class="bg-white"
        :loading="loadingInstances"
        dense
        outlined
        clearable
      >
        <template #append>
          <q-btn
            round
            dense
            flat
            size="sm"
            icon="refresh"
            @click.stop="onRefreshInstances"
          >
            <q-tooltip>刷新实例列表</q-tooltip>
          </q-btn>
        </template>

        <template #no-option>
          <q-item>
            <q-item-section class="text-grey">
              无可用实例
            </q-item-section>
          </q-item>
        </template>
      </q-select>
    </div>

    <!-- Schema 选择 -->
    <div class="q-mx-xs">
      <q-select
        ref="schemaSelect"
        :model-value="localSchema"
        @update:model-value="onChangeSchema"
        :options="availableSchemas"
        label="选择Schema"
        style="min-width: 180px;"
        class="bg-white"
        :disable="!localInstance"
        :loading="loadingSchemas"
        dense
        outlined
        clearable
        use-input
        input-debounce="0"
      >
        <!-- schema 创建区 -->
        <template #before-options>
          <div class="q-pa-sm">
            <q-input
              dense
              outlined
              v-model="localNewSchemaName"
              placeholder="新建 schema 名称"
              @keyup.enter="createSchemaAndSelect"
            />
            <div class="row items-center q-mt-sm">
              <q-btn
                size="sm"
                label="创建并选择"
                class="red-btn"
                :disable="creatingSchema || !isValidSchema(localNewSchemaName) || !localInstance"
                :loading="creatingSchema"
                @click="createSchemaAndSelect"
              />
            </div>
            <q-separator class="q-my-sm" />
          </div>
        </template>

        <template #no-option>
          <q-item>
            <q-item-section class="text-grey">
              {{ localInstance ? '无匹配项' : '请先选择实例' }}
            </q-item-section>
          </q-item>
        </template>
      </q-select>
    </div>

    <!-- 执行 -->
    <q-btn
      padding="6px 12px"
      size="md"
      class="q-mx-xs red-btn"
      icon="play_arrow"
      label="执行"
      :disable="!localSchema"
      @click="onExecute"
    />

    <!-- 测试连接（空心红色按钮） -->
    <q-btn
      padding="6px 12px"
      size="md"
      outline
      class="q-mx-xs red-btn"
      icon="science"
      label="测试连接"
      :disable="!localInstance"
      @click="onTestConn"
    />
  </div>

  <q-space />
</template>

<script setup>
  import { computed, onMounted, ref } from 'vue'
  import { Notify } from 'quasar'
  import { useEditorStore } from 'src/store/editor'
  import { useChartStore } from 'src/store/chart'
  import { api } from 'src/boot/axios'
  import { exportVisualFile } from 'src/utils/exportVisualFile'
  import { exportSqlFromDbml } from 'src/utils/exportSqlFile'

  const editor = useEditorStore()
  const chart = useChartStore()

  const exportOptions = [
    { id: 'SQL_OPENGAUSS', label: '导出 SQL（openGauss）' },
    { id: 'SQL_POSTGRESQL', label: '导出 SQL（PostgreSQL）' },
    { id: 'SQL_MYSQL', label: '导出 SQL（MySQL）' },
    { id: 'PNG', label: '导出 PNG 图片' },
    { id: 'PDF', label: '导出 PDF 文档' },
    { id: 'PLANTUML_DBML', label: '导出 PlantUML（当前 DBML）' },
    { id: 'PLANTUML_DB', label: '导出 PlantUML（数据库实例）' }
  ]

  const managedInstances = ref([])
  const loadingInstances = ref(false)
  const localInstance = ref(null)

  const availableSchemas = ref([])
  const loadingSchemas = ref(false)
  const localSchema = ref(null)
  const localNewSchemaName = ref('')
  const creatingSchema = ref(false)

  const schemaSelect = ref(null)

  const hasTables = computed(() => (chart.schemaDefinition?.tables?.length ?? 0) > 0)

  const DIALECT_MAP = Object.freeze({
    opengauss: 'OPENGAUSS',
    'open-gauss': 'OPENGAUSS',
    postgres: 'POSTGRESQL',
    postgresql: 'POSTGRESQL',
    mysql: 'MYSQL',
    mssql: 'MSSQL',
    sqlserver: 'MSSQL'
  })

  const isValidSchema = (name) => {
    const trimmed = (name || '').trim()
    return /^[A-Za-z_][A-Za-z0-9_$]*$/.test(trimmed)
  }

  function resolveDialect(instance) {
    const type = (instance?.dbType || '').toLowerCase()
    return DIALECT_MAP[type] || 'OPENGAUSS'
  }

  function buildFileName(suffix) {
    const base = localInstance.value?.displayName || 'er-diagram'
    return `${base}-${suffix}`
  }

  async function fetchInstances() {
    loadingInstances.value = true
    try {
      const response = await api.get('/execute/list-instances')
      const instances = response.data?.instances || []
      managedInstances.value = instances

      const previous = localInstance.value
      if (previous) {
        const matched = instances.find(
          inst => inst.clusterId === previous.clusterId && inst.nodeId === previous.nodeId
        )
        if (matched) {
          localInstance.value = matched
          await fetchSchemasForInstance(matched)
          return
        }
      }

      if (instances.length) {
        localInstance.value = instances[0]
        await fetchSchemasForInstance(localInstance.value)
      } else {
        localInstance.value = null
        availableSchemas.value = []
        localSchema.value = null
      }
    } catch (err) {
      console.error('获取实例列表失败:', err)
      managedInstances.value = []
      Notify.create({ type: 'negative', message: '获取实例列表失败' })
    } finally {
      loadingInstances.value = false
    }
  }

  async function fetchSchemasForInstance(instance) {
    availableSchemas.value = []
    localSchema.value = null
    if (!instance) return

    loadingSchemas.value = true
    try {
      const response = await api.get('/execute/list-schemas', {
        params: {
          clusterId: instance.clusterId,
          nodeId: instance.nodeId,
          databaseName: instance.databaseName
        }
      })
      availableSchemas.value = response.data?.schemas || []
      if (availableSchemas.value.length) {
        localSchema.value = availableSchemas.value[0]
      }
    } catch (err) {
      console.error('获取 Schema 列表失败:', err)
      Notify.create({ type: 'negative', message: '获取 Schema 列表失败' })
    } finally {
      loadingSchemas.value = false
    }
  }

  function onRefreshInstances() {
    fetchInstances()
  }

  async function onChangeInstance(val) {
    localInstance.value = val || null
    await fetchSchemasForInstance(localInstance.value)
  }

  function onChangeSchema(val) {
    localSchema.value = val || null
  }

  async function createSchemaAndSelect() {
    const name = (localNewSchemaName.value || '').trim()
    if (!localInstance.value || !isValidSchema(name)) {
      return
    }
    if (!localInstance.value?.databaseName) {
      Notify.create({ type: 'warning', message: '请选择数据库实例后再创建 schema' })
      return
    }
    creatingSchema.value = true
    try {
      await api.post('/execute/create-schema', {
        clusterId: localInstance.value.clusterId,
        nodeId: localInstance.value.nodeId,
        databaseName: localInstance.value.databaseName,
        schemaName: name
      })
      Notify.create({ type: 'positive', message: `Schema ${name} 创建成功` })
      await fetchSchemasForInstance(localInstance.value)
      localSchema.value = name
      localNewSchemaName.value = ''
      schemaSelect.value?.hidePopup?.()
    } catch (err) {
      console.error('创建 Schema 失败:', err)
      Notify.create({ type: 'negative', message: err?.message || '创建 Schema 失败' })
    } finally {
      creatingSchema.value = false
    }
  }

  async function importSqlFromFile(dialect) {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.sql,.txt'
    input.onchange = async () => {
      const file = input.files?.[0]
      if (!file) return

      try {
        const text = await file.text()
        const response = await api.post('/import/sql-to-dbml', {
          sqlText: text,
          dialect,
          skipData: true
        })
        const dbmlString = response.data?.dbmlString
        if (!dbmlString) {
          Notify.create({ type: 'warning', message: '未解析出有效的 DBML 内容' })
          return
        }
        editor.updateSourceText(dbmlString)
        Notify.create({ type: 'positive', message: 'SQL 文件导入成功' })
      } catch (err) {
        console.error('导入 SQL 失败:', err)
        Notify.create({ type: 'negative', message: err?.message || '导入 SQL 失败' })
      } finally {
        input.value = ''
      }
    }
    input.click()
  }

  async function executeSqlForInstance() {
    if (!localInstance.value || !localSchema.value) {
      Notify.create({ type: 'warning', message: '请先选择实例与 Schema' })
      return
    }

    const dbmlText = editor.getSourceText || ''
    if (!dbmlText.trim()) {
      Notify.create({ type: 'warning', message: 'DBML 内容为空，无法执行' })
      return
    }

    try {
      await api.post('/execute/from-dbml', {
        clusterId: localInstance.value.clusterId,
        nodeId: localInstance.value.nodeId,
        dialect: resolveDialect(localInstance.value),
        targetSchema: localSchema.value,
        dbmlText
      })
      Notify.create({ type: 'positive', message: 'SQL 执行成功' })
    } catch (err) {
      console.error('执行 SQL 失败:', err)
      Notify.create({ type: 'negative', message: err?.message || '执行 SQL 失败' })
    }
  }

  async function testConnection() {
    if (!localInstance.value) {
      Notify.create({ type: 'warning', message: '请先选择实例' })
      return
    }
    try {
      const res = await api.post('/execute/test-connection', {
        clusterId: localInstance.value.clusterId,
        nodeId: localInstance.value.nodeId
      })
      const message = res.data?.message || '连接测试成功'
      Notify.create({ type: 'positive', message })
    } catch (err) {
      console.error('测试连接失败:', err)
      Notify.create({ type: 'negative', message: err?.message || '测试连接失败' })
    }
  }

  async function handleExport(optionId) {
    const dbmlText = editor.getSourceText || ''

    if (!dbmlText.trim() && optionId.startsWith('SQL')) {
      Notify.create({ type: 'warning', message: '当前 DBML 内容为空，无法导出 SQL' })
      return
    }

    try {
      switch (optionId) {
        case 'SQL_OPENGAUSS':
          await exportSqlFromDbml(dbmlText, 'opengauss')
          Notify.create({ type: 'positive', message: '已导出 openGauss SQL' })
          break
        case 'SQL_POSTGRESQL':
          await exportSqlFromDbml(dbmlText, 'postgresql')
          Notify.create({ type: 'positive', message: '已导出 PostgreSQL SQL' })
          break
        case 'SQL_MYSQL':
          await exportSqlFromDbml(dbmlText, 'mysql')
          Notify.create({ type: 'positive', message: '已导出 MySQL SQL' })
          break
        case 'PNG':
          await exportVisualFile({
            format: 'png',
            fileName: buildFileName('diagram')
          })
          Notify.create({ type: 'positive', message: 'PNG 导出完成' })
          break
        case 'PDF':
          await exportVisualFile({
            format: 'pdf',
            fileName: buildFileName('diagram')
          })
          Notify.create({ type: 'positive', message: 'PDF 导出完成' })
          break
        case 'PLANTUML_DBML':
          if (!dbmlText.trim()) {
            Notify.create({ type: 'warning', message: '当前 DBML 内容为空，无法导出 PlantUML' })
            return
          }
          await exportVisualFile({
            format: 'plantuml',
            fileName: buildFileName('plantuml'),
            plantumlSource: 'dbml',
            dbmlText
          })
          Notify.create({ type: 'positive', message: 'PlantUML（DBML）导出完成' })
          break
        case 'PLANTUML_DB':
          if (!localInstance.value || !localSchema.value) {
            Notify.create({ type: 'warning', message: '请先选择实例与 Schema' })
            return
          }
          await exportVisualFile({
            format: 'plantuml',
            fileName: buildFileName('plantuml'),
            plantumlSource: 'db',
            dbParams: {
              clusterId: localInstance.value.clusterId,
              nodeId: localInstance.value.nodeId,
              databaseName: localInstance.value.databaseName,
              schemaName: localSchema.value
            }
          })
          Notify.create({ type: 'positive', message: 'PlantUML（数据库）导出完成' })
          break
        default:
          Notify.create({ type: 'warning', message: '暂不支持的导出选项' })
      }
    } catch (err) {
      console.error('导出失败:', err)
      Notify.create({ type: 'negative', message: err?.message || '导出失败' })
    }
  }

  function runAutoLayout(direction) {
    if (!hasTables.value) {
      Notify.create({
        type: 'warning',
        message: '请先加载 ER 图后再进行布局操作'
      })
      return
    }
    try {
      if (direction === 'LR') {
        chart.autoLayoutGrid()
      } else if (direction === 'TB') {
        chart.autoLayoutVertical()
      }
      Notify.create({
        type: 'positive',
        message: direction === 'LR' ? '自动布局完成' : '竖向布局完成'
      })
    } catch (err) {
      console.error('布局失败:', err)
      Notify.create({
        type: 'negative',
        message: '布局失败，请检查控制台日志'
      })
    }
  }

  function onClickExport(id) {
    handleExport(id)
  }

  function onImportSql(dialect) {
    importSqlFromFile(dialect)
  }

  function onExecute() {
    executeSqlForInstance()
  }

  function onTestConn() {
    testConnection()
  }

  onMounted(() => {
    fetchInstances()
  })
</script>

<style scoped>
  /* 统一红色按钮样式 */
  .red-btn {
    background-color: #f23030 !important;
    border: 1px solid #f23030 !important;
    color: #ffffff !important;
    box-shadow: none !important;
    border-radius: 6px;
  }

  .red-btn.q-btn--outline {
    background-color: transparent !important;
    color: #f23030 !important;
    border: 1px solid #f23030 !important;
  }

  .red-btn:hover {
    filter: brightness(1.05);
  }

  /* 让导出、导入、实例选择等按钮之间的横向间距一致 */
  .q-mx-xs {
    margin-left: 6px !important;
    margin-right: 6px !important;
  }
</style>
