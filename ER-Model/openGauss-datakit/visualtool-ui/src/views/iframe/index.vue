<template>
  <div class="app-container">
    <div class="main-bd">
      <div class="iframe-con">
        <WujieVue class="iframe" :class="[appStore.menuCollapse && 'iframe-clp']" :name="route.fullPath" :url="pluginUrl"
          :props="props" :plugins="plugins" :sync="false"></WujieVue>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeMount } from 'vue'
import { createPluginApp } from '@/utils/pluginApp'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/store'
import { DocElementRectPlugin } from "wujie-polyfill";

const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const prefix = process.env.NODE_ENV === 'development' ? '//localhost:8081' : ''
const pluginUrl = ref(prefix + route.path)
const props = ref<any>({})
const methods = ref<any>({
  jump(location: any) {
    router.push(location)
  }
})
const plugins = ref<any>([])

watch(() => appStore.menuCollapse, (val) => {
  if (val) {
    plugins.value = [
      DocElementRectPlugin(),
      {
        cssBeforeLoaders: [
          { content: 'html{padding-top: 113px !important;padding-left: 64px !important;height: 100%;view-transition-name: none;}' }
        ]
      }
    ]
  } else {
    plugins.value = [
      DocElementRectPlugin(),
      {
        cssBeforeLoaders: [
          { content: 'html{padding-top: 113px !important;padding-left: 228px !important;height: 100%;view-transition-name: none;}' }
        ]
      }
    ]
  }
}, {
  immediate: true
})

props.value = {
  data: route.query,
  methods: methods.value
}

onBeforeMount(() => {
  if (route.fullPath) {
    createPluginApp(route.fullPath as string)
  }
})
</script>

<style lang="less" scoped>
.app-container {
  .main-bd {
    height: calc(100vh - 114px);
    display: flex;
    flex-direction: column;

    .iframe-con {
      flex: 1 1 auto;
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .iframe {
        flex: 1 1 auto;
        display: block;
        width: calc(100vw - 16px);
        height: 100%;
        min-height: calc(100vh - 114px);
        margin-top: -113px;
        margin-left: -228px;

        &.iframe-clp {
          margin-left: -64px;
        }
      }
    }
  }
}</style>
