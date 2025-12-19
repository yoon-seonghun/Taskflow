<script setup lang="ts">
/**
 * 모달 다이얼로그 컴포넌트
 * Compact UI: font 13px
 */
import { watch, onMounted, onUnmounted } from 'vue'
import Button from './Button.vue'

export type ModalSize = 'sm' | 'md' | 'lg' | 'xl'

interface Props {
  modelValue: boolean
  title?: string
  size?: ModalSize
  closable?: boolean
  closeOnBackdrop?: boolean
  closeOnEscape?: boolean
  showFooter?: boolean
  confirmText?: string
  cancelText?: string
  confirmVariant?: 'primary' | 'danger'
  confirmLoading?: boolean
  confirmDisabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  size: 'md',
  closable: true,
  closeOnBackdrop: true,
  closeOnEscape: true,
  showFooter: true,
  confirmText: '확인',
  cancelText: '취소',
  confirmVariant: 'primary',
  confirmLoading: false,
  confirmDisabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
  (e: 'cancel'): void
  (e: 'close'): void
}>()

// 모달 크기 클래스
const sizeClasses: Record<ModalSize, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl'
}

function close() {
  emit('update:modelValue', false)
  emit('close')
}

function handleBackdropClick() {
  if (props.closeOnBackdrop) {
    close()
  }
}

function handleConfirm() {
  emit('confirm')
}

function handleCancel() {
  emit('cancel')
  close()
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.closeOnEscape && props.modelValue) {
    close()
  }
}

// 모달 열릴 때 스크롤 방지
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modelValue"
        class="fixed inset-0 z-modal flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-black bg-opacity-50"
          @click="handleBackdropClick"
        />

        <!-- Modal Content -->
        <Transition
          enter-active-class="transition duration-200 ease-out"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition duration-150 ease-in"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="modelValue"
            class="relative bg-white rounded-lg shadow-xl w-full"
            :class="sizeClasses[size]"
            @click.stop
          >
            <!-- Header -->
            <div
              v-if="title || closable"
              class="flex items-center justify-between px-4 py-3 border-b border-gray-200"
            >
              <h3 class="text-[14px] font-semibold text-gray-900">
                {{ title }}
              </h3>
              <button
                v-if="closable"
                type="button"
                class="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition-colors"
                @click="close"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-4 py-4 text-[13px] text-gray-700">
              <slot />
            </div>

            <!-- Footer -->
            <div
              v-if="showFooter"
              class="flex items-center justify-end gap-2 px-4 py-3 border-t border-gray-200 bg-gray-50 rounded-b-lg"
            >
              <slot name="footer">
                <Button
                  variant="secondary"
                  size="md"
                  @click="handleCancel"
                >
                  {{ cancelText }}
                </Button>
                <Button
                  :variant="confirmVariant"
                  size="md"
                  :loading="confirmLoading"
                  :disabled="confirmDisabled"
                  @click="handleConfirm"
                >
                  {{ confirmText }}
                </Button>
              </slot>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
