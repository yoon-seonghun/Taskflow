<script setup lang="ts">
/**
 * 확인 다이얼로그 컴포넌트
 * UI Store와 연동하여 전역에서 사용
 */
import { computed } from 'vue'
import { useUiStore } from '@/stores/ui'
import Modal from './Modal.vue'

const uiStore = useUiStore()

const isOpen = computed(() => uiStore.confirmDialog.visible)
const options = computed(() => uiStore.confirmDialog.options)

function handleConfirm() {
  uiStore.resolveConfirm(true)
}

function handleCancel() {
  uiStore.resolveConfirm(false)
}
</script>

<template>
  <Modal
    :model-value="isOpen"
    :title="options?.title || '확인'"
    size="sm"
    :close-on-backdrop="false"
    :confirm-text="options?.confirmText || '확인'"
    :cancel-text="options?.cancelText || '취소'"
    :confirm-variant="options?.confirmType === 'danger' ? 'danger' : 'primary'"
    @update:model-value="handleCancel"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <p class="text-gray-700">
      {{ options?.message }}
    </p>
  </Modal>
</template>
