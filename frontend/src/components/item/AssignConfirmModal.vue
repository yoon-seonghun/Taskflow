<script setup lang="ts">
/**
 * 업무 담당자 배정 확인 모달
 * - 권한 수준 선택 (조회/편집/전체)
 * - 이메일/알림 발송 옵션
 */
import { ref, computed, watch } from 'vue'
import { Modal, Button } from '@/components/common'
import { assignmentApi } from '@/api/assignment'
import { permissionLevelOptions, type PermissionLevel, type AssignmentRequest } from '@/types/assignment'

interface Props {
  show: boolean
  boardId: number
  itemId: number
  itemTitle: string
  assigneeUsername: string
  assigneeName: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'assigned', response: any): void
}>()

// State
const loading = ref(false)
const selectedPermission = ref<PermissionLevel>('EDIT')
const sendEmail = ref(true)
const sendNotification = ref(true)
const errorMessage = ref('')

// 권한 옵션
const permissionOptions = permissionLevelOptions

// 선택된 권한 설명
const selectedPermissionDescription = computed(() => {
  const option = permissionOptions.find(o => o.value === selectedPermission.value)
  return option?.description ?? ''
})

// 배정 처리
async function handleAssign() {
  loading.value = true
  errorMessage.value = ''

  try {
    const request: AssignmentRequest = {
      assigneeUsername: props.assigneeUsername,
      permissionLevel: selectedPermission.value,
      sendEmail: sendEmail.value,
      sendNotification: sendNotification.value
    }

    const response = await assignmentApi.assignItem(props.boardId, props.itemId, request)

    if (response.success) {
      emit('assigned', response.data)
      emit('close')
    } else {
      errorMessage.value = response.message || '배정에 실패했습니다'
    }
  } catch (error: any) {
    errorMessage.value = error.message || '배정 중 오류가 발생했습니다'
  } finally {
    loading.value = false
  }
}

// 모달 열릴 때 초기화
watch(() => props.show, (newVal) => {
  if (newVal) {
    selectedPermission.value = 'EDIT'
    sendEmail.value = true
    sendNotification.value = true
    errorMessage.value = ''
  }
})
</script>

<template>
  <Modal :model-value="show" title="업무 배정" size="md" @close="emit('close')">
    <div class="p-4 space-y-5">
      <!-- 배정 대상 정보 -->
      <div class="text-center">
        <div class="inline-flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-lg">
          <div class="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
            <svg class="w-4 h-4 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
          <span class="font-medium text-gray-900">{{ assigneeName }}</span>
        </div>
        <p class="mt-2 text-sm text-gray-600">님에게 업무를 배정하시겠습니까?</p>
      </div>

      <!-- 업무 정보 -->
      <div class="px-3 py-2 bg-gray-50 rounded-lg">
        <div class="text-xs text-gray-500 mb-1">업무</div>
        <div class="text-sm font-medium text-gray-900 truncate">{{ itemTitle }}</div>
      </div>

      <!-- 권한 수준 선택 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">권한 수준</label>
        <div class="space-y-2">
          <label
            v-for="option in permissionOptions"
            :key="option.value"
            class="flex items-start gap-3 p-3 border rounded-lg cursor-pointer transition-colors"
            :class="[
              selectedPermission === option.value
                ? 'border-primary-500 bg-primary-50'
                : 'border-gray-200 hover:bg-gray-50'
            ]"
          >
            <input
              type="radio"
              v-model="selectedPermission"
              :value="option.value"
              class="mt-0.5 text-primary-600 focus:ring-primary-500"
            />
            <div>
              <div class="font-medium text-sm text-gray-900">
                {{ option.label }}
                <span v-if="option.value === 'EDIT'" class="ml-1 text-xs text-primary-600">(권장)</span>
              </div>
              <div class="text-xs text-gray-500 mt-0.5">{{ option.description }}</div>
            </div>
          </label>
        </div>
      </div>

      <!-- 알림 옵션 -->
      <div class="space-y-3">
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            v-model="sendEmail"
            class="rounded text-primary-600 focus:ring-primary-500"
          />
          <span class="text-sm text-gray-700">이메일로 알림 발송</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            v-model="sendNotification"
            class="rounded text-primary-600 focus:ring-primary-500"
          />
          <span class="text-sm text-gray-700">앱 내 알림 발송</span>
        </label>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="errorMessage" class="px-3 py-2 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-sm text-red-600">{{ errorMessage }}</p>
      </div>
    </div>

    <!-- 액션 버튼 -->
    <template #footer>
      <div class="flex justify-end gap-2">
        <Button variant="secondary" @click="emit('close')" :disabled="loading">
          취소
        </Button>
        <Button variant="primary" @click="handleAssign" :loading="loading">
          배정하기
        </Button>
      </div>
    </template>
  </Modal>
</template>
