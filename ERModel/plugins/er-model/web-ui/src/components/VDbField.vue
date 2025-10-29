<template>
  <svg
    ref="root"
    :id="`field-${id}`"
    :class="{
      'db-field':true,
      'db-field__highlight': highlight,
      'db-field__dragging': dragging,
      'db-field__pk': pk,
      'db-field__unique': unique,
      'db-field__not_null': not_null,
      'db-field__increment': increment,
      'db-field__ref': endpoints.length > 0
    }"
    :x="position.x"
    :y="position.y"
    :width="size.width"
    :height="size.height"
    @mousedown.passive="onMouseDown"
    @mouseup.passive="onMouseUp"
    @mouseenter.passive="onMouseEnter"
    @mouseleave.passive="onMouseLeave"
  >
    <rect
      :height="size.height"
      :width="size.width"
    />
    <!-- 主键图标 -->
    <text ref="nameEl" class="db-field__name"
          :y="size.height/2"
          :x="nameStartX">
      {{ displayName }}
    </text>
    <!-- 图标放在列名右侧；若是主键则只显示🔑，非主键且有关联显示🔗 -->
    <text v-if="pk" class="db-field__pk-icon" :x="iconX" :y="size.height/2">🔑</text>
    <text v-else-if="showFk" class="db-field__fk-icon" :x="iconX" :y="size.height/2">🔗</text>
    <text class="db-field__type"
          :x="typeX"
          :y="size.height/2">
      {{ type.type_name }}
    </text>
    <!-- NN 灰色圆角标签 -->
    <g v-if="not_null" class="db-field__nn-group">
      <rect class="db-field__nn-rect"
            :x="nnRectX" :y="nnRectY" :rx="4" :ry="4"
            :width="nnWidth" :height="nnHeight" />
      <text class="db-field__nn-text"
            :x="nnRectX + nnWidth/2"
            :y="size.height/2">NN</text>
    </g>

    <!-- 新增：Tooltip -->
    <foreignObject
      v-if="showTooltip"
      :x="tooltipPosition.x"
      :y="tooltipPosition.y"
      width="150"
      height="100"
    >
      <div class="db-field__tooltip">
        <p><strong>{{ name }}</strong></p>
        <p>Type: {{ type.type_name }}</p>
        <p v-if="note">Note: {{ note }}</p>
        <p v-if="dbdefault">Default: {{ dbdefault.value }}</p>
        <p v-if="unique">Unique: Yes</p>
      </div>
    </foreignObject>
  </svg>
</template>

<script setup>
  import { computed, onMounted, ref, reactive, watch } from 'vue'

  const props = defineProps({
    id: Number,
    selection: String,
    token: Object,
    name: String,
    type: Object,
    unique: Boolean,
    pk: Boolean,
    dbState: Object,
    not_null: Boolean,
    note: String,
    dbdefault: Object,
    increment: Boolean,
    width: Number,
    table: Object,
    endpoints: Array,
    _enum: Object
  })
  const root = ref(null)
  const nameEl = ref(null)
  const typeWidth = ref(0)
  const rightPadding = 12
  const nnWidth = 26
  const nnHeight = 16
  const typeToNnSpacing = 8
  const leftPadding = 12
  const iconWidth = 12
  const iconGap = 6

  const size = computed(() => ({
    width: props.width,
    height: 30
  }))

  const position = computed(() => ({
    x: 0,
    y: 35 + (props.table.fields.findIndex(f => f.id === props.id) * 30)
  }))

  const mounted = onMounted(() => {
    updateMeasures()
  })

  const updateMeasures = () => {
    try {
      const svg = root.value?.ownerSVGElement || root.value
      if (svg) {
        const t = document.createElementNS('http://www.w3.org/2000/svg','text')
        t.setAttribute('class','db-field__type')
        t.textContent = props.type?.type_name || ''
        t.setAttribute('visibility','hidden')
        svg.appendChild(t)
        if (typeof t.getComputedTextLength === 'function') typeWidth.value = t.getComputedTextLength()
        svg.removeChild(t)
      }
    } catch (e) {}
  }

  watch(() => [props.type?.type_name, props.width, props.not_null, props.name], () => updateMeasures())

  const nameStartX = computed(() => leftPadding)
  const showFk = computed(() => ((props.endpoints?.length || 0) > 0) && !props.pk)
  const typeX = computed(() => props.width - rightPadding - (props.not_null ? (nnWidth + typeToNnSpacing) : 0))
  const nnRectX = computed(() => props.width - rightPadding - nnWidth)
  const nnRectY = computed(() => (size.value.height - nnHeight) / 2)

  const displayName = computed(() => {
    const iconReserve = (props.pk || showFk.value) ? (iconGap + iconWidth) : 0
    const rightReserved = rightPadding + (props.not_null ? (nnWidth + typeToNnSpacing) : 0) + typeWidth.value
    const available = Math.max(20, props.width - rightReserved - nameStartX.value - iconReserve)
    const text = props.name || ''
    if (!nameEl.value || typeof nameEl.value.getComputedTextLength !== 'function') return text
    nameEl.value.textContent = text
    if (nameEl.value.getComputedTextLength() <= available) return text
    let left = 0, right = text.length, res = 0
    while (left <= right) {
      const mid = Math.floor((left + right) / 2)
      const candidate = text.slice(0, mid) + '…'
      nameEl.value.textContent = candidate
      if (nameEl.value.getComputedTextLength() <= available) {
        res = mid
        left = mid + 1
      } else {
        right = mid - 1
      }
    }
    const finalText = text.slice(0, res) + '…'
    nameEl.value.textContent = finalText
    return finalText
  })

  const nameActualWidth = computed(() => {
    if (!nameEl.value || typeof nameEl.value.getComputedTextLength !== 'function') return 0
    nameEl.value.textContent = displayName.value
    return nameEl.value.getComputedTextLength()
  })
  const iconX = computed(() => nameStartX.value + nameActualWidth.value + iconGap)

  const highlight = ref(false)
  const dragging = ref(false)

  const showTooltip = ref(false);
  const tooltipPosition = reactive({ x: 0, y: 0 });

  const onMouseEnter = (e) => {
    highlight.value = true;
    showTooltip.value = true;
    tooltipPosition.x = e.offsetX + 10; // 调整 tooltip 的位置
    tooltipPosition.y = e.offsetY + 10;
  }

  const onMouseLeave = (e) => {
    highlight.value = false;
    showTooltip.value = false;
  }
  const onMouseUp = (e) => {
    dragging.value = false
  }
  const onMouseDown = (e) => {
    dragging.value = true
  }

</script>
