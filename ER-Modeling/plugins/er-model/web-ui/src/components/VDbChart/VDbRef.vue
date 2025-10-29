<template>
  <g
    ref="root"
    :id="`ref-${id}`"
    :class="{
      'db-ref': true,
      'db-ref__highlight': highlight,
      'db-ref__active': isActive
    }"
    @mouseenter.passive="onMouseEnter"
    @mouseleave.passive="onMouseLeave"
  >
    <!-- 连线路径 -->
    <path class="db-ref__hitbox" :d="path" />
    <path class="db-ref__path" :d="path" />
    <path v-if="isActive" class="db-ref__path-active" :d="path" />

    <!-- 两端圆点 -->
    <circle class="db-ref__endpoint" :cx="start.x" :cy="start.y" r="5" />
    <path
      v-if="endRelation==='*'"
      class="db-ref__crowfoot"
      :d="endCrowfootPath"
    />
    <path
      v-else
      class="db-ref__one"
      :d="endOnePath"
    />

    <!-- Cardinality 标签，仅悬停或高亮时显示 -->
    <g class="db-ref__labels">
      <text
        class="db-ref__label start"
        :x="startLabel.x"
        :y="startLabel.y"
        :text-anchor="startLabel.anchor"
      >{{ startLabel.text }}</text>
      <text
        class="db-ref__label end"
        :x="endLabel.x"
        :y="endLabel.y"
        :text-anchor="endLabel.anchor"
      >{{ endLabel.text }}</text>
    </g>
  </g>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useChartStore } from '../../store/chart'

const props = defineProps({
  id: Number,
  name: String,
  endpoints: Array,
  onUpdate: [String, Object, undefined],
  onDelete: [String, Object, undefined],
  schema: Object,
  dbState: Object,
  database: Object,
  token: Object,
  containerRef: Object
})

const store = useChartStore()
store.getRef(props.id)

const highlight = ref(false)

const getPositionAnchors = (endpoint) => {
  const s = store.getTable(endpoint.fields[0].table.id)
  const fieldIndex = endpoint.fields[0].table.fields.findIndex(f => f.id === endpoint.fields[0].id)

  return [
    {
      x: s.x,
      y: s.y + 35 + (30 * fieldIndex) + (30 / 2.0)
    },
    {
      x: s.x + s.width,
      y: s.y + 35 + (30 * fieldIndex) + (30 / 2.0)
    }
  ]
}

const getClosestAnchor = (point, anchors) => {
  const withDistances = anchors.map(a => ({
      distanceXY: {
        x: (a.x - point.x),
        y: (a.y - point.y)
      },
      distance: Math.sqrt(
        ((a.x - point.x) * (a.x - point.x))
        + ((a.y - point.y) * (a.y - point.y))
      ),
      anchor: a
    })
  )

  let smallest = withDistances[0]
  for (const withDistance of withDistances) {
    if (withDistance.distance < smallest.distance) {
      smallest = withDistance
    }
  }

  return smallest.anchor
}

const startAnchors = computed(() => getPositionAnchors(props.endpoints[0]))
const endAnchors = computed(() => getPositionAnchors(props.endpoints[1]))

const startTableState = computed(() => {
  const tableId = props.endpoints?.[0]?.fields?.[0]?.table?.id
  return tableId != null ? store.getTable(tableId) : { x: 0, y: 0, width: 0, height: 0 }
})

const endTableState = computed(() => {
  const tableId = props.endpoints?.[1]?.fields?.[0]?.table?.id
  return tableId != null ? store.getTable(tableId) : { x: 0, y: 0, width: 0, height: 0 }
})

const getSide = (anchor, table) => {
  if (!anchor || !table) return 'right'
  const leftDist = Math.abs(anchor.x - table.x)
  const rightDist = Math.abs(anchor.x - (table.x + table.width))
  return rightDist < leftDist ? 'right' : 'left'
}

const start = computed(() => {
  const anchors = startAnchors.value || []
  if (anchors.length < 2) return { x: 0, y: 0 }
  const startState = startTableState.value
  const endState = endTableState.value
  const preferRight = !endState || (endState.x >= startState.x)
  return preferRight ? anchors[1] : anchors[0]
})

const end = computed(() => {
  const anchors = endAnchors.value || []
  if (anchors.length < 2) return { x: 0, y: 0 }
  const startState = startTableState.value
  const endState = endTableState.value
  const preferLeft = !startState || (startState.x <= endState.x)
  return preferLeft ? anchors[0] : anchors[1]
})

const startSide = computed(() => getSide(start.value, startTableState.value))
const endSide = computed(() => getSide(end.value, endTableState.value))

const endRelation = computed(() => props.endpoints?.[1]?.relation || '1')
const startRelation = computed(() => props.endpoints?.[0]?.relation || '1')

const path = computed(() => {
  const sAnchor = start.value
  const eAnchor = end.value
  if (!sAnchor || !eAnchor) return ''
  const horizontalGap = Math.max(60, Math.abs(eAnchor.x - sAnchor.x) / 2)
  const midX = sAnchor.x <= eAnchor.x ? sAnchor.x + horizontalGap : sAnchor.x - horizontalGap
  return `M ${sAnchor.x},${sAnchor.y} C ${midX},${sAnchor.y} ${midX},${eAnchor.y} ${eAnchor.x},${eAnchor.y}`
})

const isActive = computed(() => highlight.value || (store.highlightedRefs || []).includes(props.id))

const onMouseEnter = () => { highlight.value = true }
const onMouseLeave = () => { highlight.value = false }

const startLabel = computed(() => ({
  x: startSide.value === 'right' ? start.value.x + 16 : start.value.x - 16,
  y: start.value.y - 6,
  anchor: startSide.value === 'right' ? 'start' : 'end',
  text: (startRelation.value === '*' ? '*' : (props.endpoints?.[0]?.fields?.[0]?.not_null ? '1' : '0..1'))
}))
const endLabel = computed(() => ({
  x: endSide.value === 'right' ? end.value.x - 8 : end.value.x + 8,
  y: end.value.y - 6,
  anchor: endSide.value === 'right' ? 'end' : 'start',
  text: (endRelation.value === '*' ? '*' : (props.endpoints?.[1]?.fields?.[0]?.not_null ? '1' : '0..1'))
}))

const buildOnePath = (anchor, side) => {
  if (!anchor) return ''
  const dir = side === 'right' ? -1 : 1
  return `M ${anchor.x + dir * 8},${anchor.y} L ${anchor.x},${anchor.y}`
}

const buildCrowfootPath = (anchor, side) => {
  if (!anchor) return ''
  const dir = side === 'right' ? -1 : 1
  const offset = dir * 8
  return [
    `M ${anchor.x + offset},${anchor.y - 6}`,
    `L ${anchor.x},${anchor.y}`,
    `L ${anchor.x + offset},${anchor.y + 6}`
  ].join(' ')
}

const endOnePath = computed(() => buildOnePath(end.value, endSide.value))
const endCrowfootPath = computed(() => buildCrowfootPath(end.value, endSide.value))

watch(() => props.id, () => {
  store.getRef(props.id)
})
</script>

<style scoped>
.db-ref__labels text {
  pointer-events: none;
  paint-order: stroke;
  stroke: #ffffff;
  stroke-width: 3;
}

/* 默认隐藏 cardinality */
.db-ref__label {
  opacity: 0;
  transition: opacity 0.2s ease;
  font-size: 11px;
  font-weight: 600;
  fill: #475569;
}

/* 悬停或高亮时显示 */
.db-ref:hover .db-ref__label,
.db-ref__highlight .db-ref__label,
.db-ref__active .db-ref__label {
  opacity: 1;
}
</style>
