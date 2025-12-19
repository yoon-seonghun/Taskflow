<script setup lang="ts">
/**
 * 에러 바운더리 컴포넌트
 * 자식 컴포넌트에서 발생한 에러를 캡처하고 폴백 UI를 표시
 */
import { ref, computed, onErrorCaptured, provide, type InjectionKey } from 'vue'
import { useRouter } from 'vue-router'
import { useErrorHandler } from '@/composables/useErrorHandler'
import { AppError, toAppError } from '@/utils/errorTypes'

interface Props {
  // 폴백 메시지
  fallbackMessage?: string
  // 재시도 버튼 표시 여부
  showRetry?: boolean
  // 에러 상세 표시 여부 (개발용)
  showDetails?: boolean
  // 에러 발생 시 콜백
  onError?: (error: AppError) => void
  // 재시도 시 콜백
  onRetry?: () => void
  // 커스텀 폴백 컴포넌트 표시 여부 (slot 사용)
  useCustomFallback?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fallbackMessage: '문제가 발생했습니다.',
  showRetry: true,
  showDetails: undefined,
  useCustomFallback: false
})

// showDetails가 undefined면 개발 환경 여부로 결정
const shouldShowDetails = computed(() => props.showDetails ?? import.meta.env.DEV)

const emit = defineEmits<{
  (e: 'error', error: AppError): void
  (e: 'retry'): void
  (e: 'reset'): void
}>()

const router = useRouter()
const { handleError, logError } = useErrorHandler()

// 에러 상태
const error = ref<AppError | null>(null)
const errorCount = ref(0)

// 홈으로 이동
function goHome() {
  try {
    router.push('/')
  } catch (e) {
    console.error('Navigation error:', e)
  }
}

// 에러 캡처
onErrorCaptured((err, instance, info) => {
  const appError = toAppError(err, info)
  error.value = appError
  errorCount.value++

  // 로깅
  logError(appError, `ErrorBoundary (${info})`)

  // 에러 핸들러 호출
  handleError(err, info)

  // 콜백 호출
  props.onError?.(appError)
  emit('error', appError)

  // 에러 전파 방지
  return false
})

// 재시도
function retry() {
  error.value = null
  props.onRetry?.()
  emit('retry')
}

// 리셋
function reset() {
  error.value = null
  errorCount.value = 0
  emit('reset')
}

// 에러 상태 제공 (자식 컴포넌트에서 접근 가능)
const ERROR_BOUNDARY_KEY: InjectionKey<{
  hasError: () => boolean
  getError: () => AppError | null
  reset: () => void
}> = Symbol('errorBoundary')

provide(ERROR_BOUNDARY_KEY, {
  hasError: () => error.value !== null,
  getError: () => error.value,
  reset
})

// 외부 노출
defineExpose({
  error,
  errorCount,
  retry,
  reset
})
</script>

<template>
  <!-- 에러 발생 시 폴백 UI -->
  <template v-if="error">
    <!-- 커스텀 폴백 슬롯 (useCustomFallback이 true일 때) -->
    <slot
      v-if="useCustomFallback"
      name="fallback"
      :error="error"
      :retry="retry"
      :reset="reset"
      :go-home="goHome"
    />

    <!-- 기본 폴백 UI -->
    <div v-else class="error-fallback">
      <div class="error-icon">
        <svg class="w-12 h-12 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>

      <h3 class="error-title">{{ fallbackMessage }}</h3>

      <p v-if="error.message && error.message !== fallbackMessage" class="error-message">
        {{ error.message }}
      </p>

      <!-- 에러 상세 (개발용) -->
      <details v-if="shouldShowDetails" class="error-details">
        <summary class="details-summary">상세 정보</summary>
        <div class="details-content">
          <div class="detail-row">
            <span class="detail-label">코드:</span>
            <span class="detail-value">{{ error.code }}</span>
          </div>
          <div v-if="error.statusCode" class="detail-row">
            <span class="detail-label">상태:</span>
            <span class="detail-value">{{ error.statusCode }}</span>
          </div>
          <div v-if="error.context" class="detail-row">
            <span class="detail-label">컨텍스트:</span>
            <span class="detail-value">{{ error.context }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">시간:</span>
            <span class="detail-value">{{ error.timestamp?.toLocaleString() }}</span>
          </div>
          <div v-if="error.stack" class="detail-row stack">
            <span class="detail-label">스택:</span>
            <pre class="detail-stack">{{ error.stack }}</pre>
          </div>
        </div>
      </details>

      <!-- 버튼 영역 -->
      <div class="error-actions">
        <button
          v-if="showRetry"
          type="button"
          class="btn-retry"
          aria-label="요청 재시도"
          @click="retry"
        >
          <svg class="w-4 h-4" aria-hidden="true" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          다시 시도
        </button>

        <button
          type="button"
          class="btn-home"
          aria-label="홈으로 이동"
          @click="goHome"
        >
          <svg class="w-4 h-4" aria-hidden="true" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          홈으로
        </button>
      </div>

      <!-- 에러 횟수 표시 (개발용) -->
      <p v-if="shouldShowDetails && errorCount > 1" class="error-count">
        이 세션에서 {{ errorCount }}번의 에러가 발생했습니다.
      </p>
    </div>
  </template>

  <!-- 정상 상태: 자식 컴포넌트 렌더링 -->
  <slot v-else />
</template>

<style scoped>
.error-fallback {
  @apply flex flex-col items-center justify-center min-h-[300px] p-8
         bg-white rounded-lg border border-gray-200;
}

.error-icon {
  @apply mb-4;
}

.error-title {
  @apply text-lg font-semibold text-gray-900 mb-2 text-center;
}

.error-message {
  @apply text-sm text-gray-600 mb-4 text-center max-w-md;
}

.error-details {
  @apply w-full max-w-lg mb-4;
}

.details-summary {
  @apply cursor-pointer text-sm text-gray-500 hover:text-gray-700
         py-2 px-3 bg-gray-50 rounded-lg;
}

.details-content {
  @apply mt-2 p-3 bg-gray-50 rounded-lg text-xs font-mono;
}

.detail-row {
  @apply flex gap-2 py-1;
}

.detail-row.stack {
  @apply flex-col;
}

.detail-label {
  @apply text-gray-500 min-w-[80px];
}

.detail-value {
  @apply text-gray-700;
}

.detail-stack {
  @apply mt-1 p-2 bg-gray-100 rounded text-xs overflow-x-auto whitespace-pre-wrap
         max-h-40 overflow-y-auto;
}

.error-actions {
  @apply flex items-center gap-3;
}

.btn-retry {
  @apply inline-flex items-center gap-2 px-4 py-2
         text-sm font-medium text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors;
}

.btn-home {
  @apply inline-flex items-center gap-2 px-4 py-2
         text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors;
}

.error-count {
  @apply mt-4 text-xs text-gray-400;
}
</style>
