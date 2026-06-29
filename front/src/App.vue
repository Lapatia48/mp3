<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import PlayerBar from '@/components/PlayerBar.vue'
import ToastHost from '@/components/ToastHost.vue'

const route = useRoute()
const showShell = computed(() => route.name !== 'login')
</script>

<template>
  <ToastHost />

  <div v-if="showShell" class="app">
    <div class="app-body">
      <AppSidebar />
      <main class="content">
        <RouterView v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
    <PlayerBar />
  </div>

  <RouterView v-else />
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.app-body {
  display: flex;
  flex: 1;
  min-height: 0;
}
.content {
  flex: 1;
  overflow-y: auto;
  padding: 34px 40px 48px;
}
@media (max-width: 820px) {
  .content { padding: 22px 18px 34px; }
}
</style>
