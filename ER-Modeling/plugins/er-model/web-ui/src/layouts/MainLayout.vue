<template>
  <div class="er-layout">
    <header class="er-toolbar shadow-1 bg-white" role="banner">
      <router-view name="toolbar" />
    </header>
    <main class="er-content bg-grey-1">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 这里无需显式逻辑，主布局仅用于承载命名视图
</script>

<style scoped>
.er-layout {
  height: 100%;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.er-toolbar {
  flex: 0 0 auto;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.er-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0.5rem;
}

.er-content > * {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
