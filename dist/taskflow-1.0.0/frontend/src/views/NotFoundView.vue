<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-gray-300">404</h1>
      <p class="mt-4 text-lg text-gray-600">페이지를 찾을 수 없습니다</p>
      <button
        class="mt-6 btn btn-primary"
        @click="router.push({ name: 'Tasks' })"
      >
        홈으로 돌아가기
      </button>
    </div>
  </div>
</template>
