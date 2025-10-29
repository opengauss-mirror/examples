<template>
  <svg v-if="tooltip.show"
       :x="tooltip.x"
       :y="tooltip.y"
       :width="tooltip.width"
       :height="tooltip.height"
       class="db-tooltip"
  >
    <foreignObject x="0"
                   y="0"
                   :width="tooltip.width"
                   :height="tooltip.height"
                   class="db-tooltip__content">
      <q-card  class="db-tooltip__content-card">
        <component :is="tooltip.component" v-bind="tooltip.binds"/>
      </q-card>
    </foreignObject>
  </svg>
</template>

<script setup>
  import { useChartStore } from '../../store/chart'
  import { computed } from 'vue'

  const store = useChartStore()

  const tooltip = computed(() => store.tooltip)
</script>
