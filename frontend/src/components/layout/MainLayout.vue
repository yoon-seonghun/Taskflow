<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import { Toast, ConfirmDialog, ConflictDialog } from '@/components/common'
import SlideOverPanel from '@/components/ui/SlideOverPanel.vue'

// 반응형 브레이크포인트 (md = 768px)
const MD_BREAKPOINT = 768

const sidebarOpen = ref(window.innerWidth >= MD_BREAKPOINT)
const isMobile = ref(window.innerWidth < MD_BREAKPOINT)

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

function closeSidebar() {
  // 모바일에서만 사이드바 닫기
  if (isMobile.value) {
    sidebarOpen.value = false
  }
}

// 화면 크기 변경 감지
function handleResize() {
  const newIsMobile = window.innerWidth < MD_BREAKPOINT

  // 모바일 <-> 데스크톱 전환 시 사이드바 상태 자동 변경
  if (newIsMobile !== isMobile.value) {
    isMobile.value = newIsMobile
    sidebarOpen.value = !newIsMobile  // 데스크톱: 열림, 모바일: 닫힘
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <Header :is-mobile="isMobile" @toggle-sidebar="toggleSidebar" />
    <div class="flex pt-14">
      <Sidebar :open="sidebarOpen" :is-mobile="isMobile" @close="closeSidebar" />
      <main
        class="flex-1 transition-all duration-300 ml-0 md:ml-60"
        :class="{ 'md:ml-0': !sidebarOpen }"
      >
        <div class="p-4 h-[calc(100vh-3.5rem)]">
          <RouterView />
        </div>
      </main>
    </div>

    <!-- 슬라이드오버 패널 (리사이즈 가능) -->
    <SlideOverPanel />

    <!-- 토스트 알림 -->
    <Toast />

    <!-- 확인 다이얼로그 -->
    <ConfirmDialog />

    <!-- 충돌 해결 다이얼로그 -->
    <ConflictDialog />
  </div>
</template>
