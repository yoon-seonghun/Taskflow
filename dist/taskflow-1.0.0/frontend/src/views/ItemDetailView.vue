<script setup lang="ts">
/**
 * 아이템 상세 페이지 (모바일 전체 화면용)
 * - 768px 미만 화면에서 아이템 클릭 시 이 페이지로 라우팅
 * - ItemDetailPanel을 전체 화면으로 표시
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ItemDetailPanel from '@/components/item/ItemDetailPanel.vue'

const route = useRoute()
const router = useRouter()

const boardId = computed(() => Number(route.params.boardId))
const itemId = computed(() => Number(route.params.itemId))

function handleClose() {
  // 이전 페이지로 돌아가기
  router.back()
}

function handleUpdated() {
  // 업데이트 후에도 패널 유지
}

function handleDeleted() {
  // 삭제 후 목록으로 돌아가기
  router.push({ name: 'Tasks' })
}
</script>

<template>
  <div class="item-detail-view h-full">
    <ItemDetailPanel
      v-if="boardId && itemId"
      :board-id="boardId"
      :item-id="itemId"
      @close="handleClose"
      @updated="handleUpdated"
      @deleted="handleDeleted"
    />
    <div v-else class="h-full flex items-center justify-center">
      <p class="text-gray-500">잘못된 접근입니다.</p>
    </div>
  </div>
</template>
