<script setup lang="ts">
/**
 * 쿼리 테스트 모달
 * - 쿼리 실행 테스트
 * - 결과 미리보기
 * - 옵션 매핑 확인
 */
import { ref, computed, onMounted } from 'vue'
import Badge from '@/components/common/Badge.vue'
import type { ExternalQuery, QueryTestResponse } from '@/types/externalQuery'
import { useExternalQueryStore } from '@/stores/externalQuery'
import { useExternalDatasourceStore } from '@/stores/externalDatasource'

const props = defineProps<{
  query: ExternalQuery
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const queryStore = useExternalQueryStore()
const datasourceStore = useExternalDatasourceStore()

// 테스트 상태
const testing = ref(false)
const testResult = ref<QueryTestResponse | null>(null)
const errorMessage = ref<string | null>(null)

// 도움말 표시 상태
const showHelp = ref(false)

// 수정 가능한 SQL
const editableSql = ref('')

// 컬럼 매핑
const valueColumn = ref('')
const labelColumn = ref('')
const colorColumn = ref('')

// 최대 행 수
const maxRows = ref(100)

// 데이터소스 정보
const datasource = computed(() =>
  datasourceStore.getDatasourceById(props.query.datasourceId)
)

// 컬럼 매핑 불일치 감지
const columnMismatch = computed(() => {
  if (!testResult.value?.success || !testResult.value?.columns) {
    return { hasError: false, missingColumns: [] as string[] }
  }

  const columns = testResult.value.columns.map(c => c.toUpperCase())
  const missingColumns: string[] = []

  if (valueColumn.value && !columns.includes(valueColumn.value.toUpperCase())) {
    missingColumns.push(`Value 컬럼 "${valueColumn.value}"`)
  }
  if (labelColumn.value && !columns.includes(labelColumn.value.toUpperCase())) {
    missingColumns.push(`Label 컬럼 "${labelColumn.value}"`)
  }
  if (colorColumn.value && !columns.includes(colorColumn.value.toUpperCase())) {
    missingColumns.push(`Color 컬럼 "${colorColumn.value}"`)
  }

  return {
    hasError: missingColumns.length > 0,
    missingColumns
  }
})

// 개별 컬럼 일치 여부
const isValueColumnValid = computed(() => {
  if (!testResult.value?.columns || !valueColumn.value) return true
  return testResult.value.columns.map(c => c.toUpperCase()).includes(valueColumn.value.toUpperCase())
})

const isLabelColumnValid = computed(() => {
  if (!testResult.value?.columns || !labelColumn.value) return true
  return testResult.value.columns.map(c => c.toUpperCase()).includes(labelColumn.value.toUpperCase())
})

const isColorColumnValid = computed(() => {
  if (!testResult.value?.columns || !colorColumn.value) return true
  return testResult.value.columns.map(c => c.toUpperCase()).includes(colorColumn.value.toUpperCase())
})

// 테스트 결과에서 사용 가능한 컬럼 목록
const availableColumns = computed(() => {
  if (!testResult.value?.success || !testResult.value?.columns) {
    return []
  }
  return testResult.value.columns
})

// 초기화
function initForm() {
  editableSql.value = props.query.querySql
  valueColumn.value = props.query.valueColumn
  labelColumn.value = props.query.labelColumn
  colorColumn.value = props.query.colorColumn || ''
}

// 테스트 실행
async function handleTest() {
  testing.value = true
  testResult.value = null
  errorMessage.value = null

  try {
    const result = await queryStore.testQuery({
      datasourceId: props.query.datasourceId,
      querySql: editableSql.value,
      valueColumn: valueColumn.value,
      labelColumn: labelColumn.value,
      colorColumn: colorColumn.value || undefined,
      maxRows: maxRows.value
    })

    testResult.value = result
    if (!result.success) {
      errorMessage.value = result.message
    }
  } catch (error: any) {
    console.error('Query test failed:', error)
    errorMessage.value = error.message || '쿼리 테스트에 실패했습니다.'
  } finally {
    testing.value = false
  }
}

// 닫기
function handleClose() {
  emit('close')
}

// 초기화
onMounted(() => {
  initForm()
})
</script>

<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-container">
      <!-- 헤더 -->
      <div class="modal-header">
        <div>
          <h2 class="modal-title">쿼리 테스트</h2>
          <p class="modal-subtitle">
            {{ query.queryName }}
            <span class="text-gray-400 ml-2">{{ query.queryCode }}</span>
          </p>
        </div>
        <button type="button" class="close-btn" @click="handleClose">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 본문 -->
      <div class="modal-body">
        <!-- 사용법 도움말 -->
        <div class="help-section">
          <button type="button" class="help-toggle" @click="showHelp = !showHelp">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>사용법 안내</span>
            <svg :class="['w-4 h-4 transition-transform', showHelp && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <div v-if="showHelp" class="help-content">
            <div class="help-item">
              <h5>Value 컬럼</h5>
              <p>실제 DB에 저장되는 값입니다. 예: 사원번호 'EMP001', 부서코드 'DEV'</p>
            </div>
            <div class="help-item">
              <h5>Label 컬럼</h5>
              <p>사용자에게 보여지는 표시명입니다. 예: 사원명 '홍길동', 부서명 '개발팀'</p>
            </div>
            <div class="help-item">
              <h5>Color 컬럼 (선택)</h5>
              <p>옵션별 색상 코드입니다. 예: '#FF5733', 'blue' (미지정 시 기본 색상 사용)</p>
            </div>
            <div class="help-example">
              <span class="example-label">예시 쿼리:</span>
              <code>SELECT USER_ID AS value, USER_NAME AS label FROM TB_USER</code>
            </div>
          </div>
        </div>

        <!-- 데이터소스 정보 -->
        <div class="datasource-info">
          <span class="info-label">데이터소스:</span>
          <Badge :variant="datasource?.dbType === 'MYSQL' ? 'primary' : 'default'" size="sm">
            {{ datasource?.dbType }}
          </Badge>
          <span class="info-value">{{ datasource?.datasourceName }}</span>
          <span class="info-host">({{ datasource?.host }}:{{ datasource?.port }})</span>
        </div>

        <!-- SQL 편집 -->
        <div class="sql-section">
          <div class="flex items-center justify-between mb-2">
            <label class="section-label">SQL 쿼리</label>
            <div class="flex items-center gap-2">
              <label class="text-xs text-gray-500">
                최대 행:
                <select v-model.number="maxRows" class="max-rows-select">
                  <option :value="10">10</option>
                  <option :value="50">50</option>
                  <option :value="100">100</option>
                  <option :value="500">500</option>
                  <option :value="1000">1000</option>
                </select>
              </label>
            </div>
          </div>
          <textarea
            v-model="editableSql"
            class="sql-textarea"
            rows="6"
            placeholder="SELECT 쿼리..."
          ></textarea>
        </div>

        <!-- 컬럼 매핑 -->
        <div class="column-mapping">
          <div class="mapping-item">
            <label class="mapping-label">
              Value 컬럼
              <span class="tooltip-container">
                <svg class="w-3.5 h-3.5 text-gray-400 hover:text-gray-600 cursor-help dark:hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span class="tooltip-text">DB에 저장되는 실제 값 (예: 사원번호, 코드)</span>
              </span>
            </label>
            <div class="input-wrapper">
              <!-- 테스트 성공 시 드롭다운, 아니면 입력 -->
              <select
                v-if="availableColumns.length > 0"
                v-model="valueColumn"
                :class="['mapping-select', !isValueColumnValid && 'input-error']"
              >
                <option value="">-- 컬럼 선택 --</option>
                <option v-for="col in availableColumns" :key="col" :value="col">
                  {{ col }}
                </option>
              </select>
              <input
                v-else
                v-model="valueColumn"
                type="text"
                class="mapping-input"
                placeholder="value"
              />
              <span v-if="testResult?.success && !isValueColumnValid" class="input-status error">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </span>
              <span v-else-if="testResult?.success && valueColumn && isValueColumnValid" class="input-status success">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </span>
            </div>
          </div>
          <div class="mapping-item">
            <label class="mapping-label">
              Label 컬럼
              <span class="tooltip-container">
                <svg class="w-3.5 h-3.5 text-gray-400 hover:text-gray-600 cursor-help dark:hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span class="tooltip-text">화면에 표시되는 이름 (예: 사원명, 부서명)</span>
              </span>
            </label>
            <div class="input-wrapper">
              <select
                v-if="availableColumns.length > 0"
                v-model="labelColumn"
                :class="['mapping-select', !isLabelColumnValid && 'input-error']"
              >
                <option value="">-- 컬럼 선택 --</option>
                <option v-for="col in availableColumns" :key="col" :value="col">
                  {{ col }}
                </option>
              </select>
              <input
                v-else
                v-model="labelColumn"
                type="text"
                class="mapping-input"
                placeholder="label"
              />
              <span v-if="testResult?.success && !isLabelColumnValid" class="input-status error">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </span>
              <span v-else-if="testResult?.success && labelColumn && isLabelColumnValid" class="input-status success">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </span>
            </div>
          </div>
          <div class="mapping-item">
            <label class="mapping-label">
              Color 컬럼
              <span class="tooltip-container">
                <svg class="w-3.5 h-3.5 text-gray-400 hover:text-gray-600 cursor-help dark:hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span class="tooltip-text">옵션별 색상 코드 (예: #FF5733) - 선택사항</span>
              </span>
            </label>
            <div class="input-wrapper">
              <select
                v-if="availableColumns.length > 0"
                v-model="colorColumn"
                :class="['mapping-select', colorColumn && !isColorColumnValid && 'input-error']"
              >
                <option value="">(사용 안 함)</option>
                <option v-for="col in availableColumns" :key="col" :value="col">
                  {{ col }}
                </option>
              </select>
              <input
                v-else
                v-model="colorColumn"
                type="text"
                class="mapping-input"
                placeholder="color (선택)"
              />
              <span v-if="testResult?.success && colorColumn && !isColorColumnValid" class="input-status error">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </span>
              <span v-else-if="testResult?.success && colorColumn && isColorColumnValid" class="input-status success">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </span>
            </div>
          </div>
          <div class="mapping-item">
            <button
              type="button"
              class="test-btn"
              :disabled="testing"
              @click="handleTest"
            >
              <svg v-if="testing" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              테스트 실행
            </button>
          </div>
        </div>

        <!-- 컬럼 선택 안내 (테스트 전) -->
        <div v-if="availableColumns.length === 0 && !testResult" class="column-hint">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>테스트 실행 후 쿼리 결과의 컬럼을 드롭다운에서 선택할 수 있습니다.</span>
        </div>

        <!-- 컬럼 매핑 불일치 경고 -->
        <div v-if="columnMismatch.hasError" class="column-warning">
          <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div class="warning-content">
            <span class="warning-title">컬럼 매핑 불일치</span>
            <span class="warning-text">
              쿼리 결과에 다음 컬럼이 없습니다: {{ columnMismatch.missingColumns.join(', ') }}
            </span>
            <span class="warning-hint">
              쿼리 결과 컬럼: {{ testResult?.columns?.join(', ') }}
            </span>
          </div>
        </div>

        <!-- 에러 메시지 -->
        <div v-if="errorMessage" class="error-message">
          <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- 테스트 결과 -->
        <div v-if="testResult && testResult.success" class="result-section">
          <!-- 실행 정보 -->
          <div class="result-header">
            <div class="result-stats">
              <span class="stat-item">
                <svg class="w-4 h-4 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
                실행 성공
              </span>
              <span class="stat-item">
                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {{ testResult.executionTimeMs }}ms
              </span>
              <span class="stat-item">
                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 6h16M4 10h16M4 14h16M4 18h16" />
                </svg>
                {{ testResult.rowCount }}건
              </span>
            </div>
            <div v-if="testResult.columns" class="result-columns">
              컬럼: {{ testResult.columns.join(', ') }}
            </div>
          </div>

          <!-- 옵션 미리보기 -->
          <div v-if="testResult.options && testResult.options.length > 0" class="options-preview">
            <h4 class="preview-title">
              옵션 미리보기 (상위 {{ testResult.options.length }}개)
              <span v-if="columnMismatch.hasError" class="preview-warning">(매핑 오류로 일부 값이 표시되지 않음)</span>
            </h4>
            <div class="options-list">
              <div
                v-for="(option, index) in testResult.options"
                :key="index"
                class="option-item"
              >
                <span
                  v-if="option.color"
                  class="option-color"
                  :style="{ backgroundColor: option.color }"
                  :title="'색상: ' + option.color"
                ></span>
                <span v-else-if="colorColumn" class="option-color-empty" title="색상 없음"></span>
                <span :class="['option-label', !option.label && 'option-empty']">
                  {{ option.label || '(라벨 없음)' }}
                </span>
                <span :class="['option-value', !option.value && 'option-empty']">
                  {{ option.value || '(값 없음)' }}
                </span>
              </div>
            </div>
          </div>

          <!-- 원시 데이터 -->
          <div v-if="testResult.data && testResult.data.length > 0" class="raw-data">
            <details>
              <summary class="data-summary">원시 데이터 보기</summary>
              <div class="data-table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th v-for="col in testResult.columns" :key="col">{{ col }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, idx) in testResult.data" :key="idx">
                      <td v-for="col in testResult.columns" :key="col">
                        {{ row[col] }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </details>
          </div>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <button type="button" class="btn-close" @click="handleClose">
          닫기
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 bg-black/50 flex items-center justify-center z-50;
}

.modal-container {
  @apply bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col
         dark:bg-gray-800;
}

.modal-header {
  @apply flex items-start justify-between px-6 py-4 border-b border-gray-200
         dark:border-gray-700;
}

.modal-title {
  @apply text-lg font-semibold text-gray-900
         dark:text-white;
}

.modal-subtitle {
  @apply text-sm text-gray-500 mt-0.5
         dark:text-gray-400;
}

.close-btn {
  @apply p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors
         dark:text-gray-500 dark:hover:text-gray-300 dark:hover:bg-gray-700;
}

.modal-body {
  @apply flex-1 overflow-y-auto px-6 py-4 space-y-4;
}

.modal-footer {
  @apply flex items-center justify-end px-6 py-4 border-t border-gray-200 bg-gray-50
         dark:border-gray-700 dark:bg-gray-900;
}

.datasource-info {
  @apply flex items-center gap-2 p-3 bg-gray-50 rounded-lg text-sm
         dark:bg-gray-700;
}

.info-label {
  @apply text-gray-500
         dark:text-gray-400;
}

.info-value {
  @apply font-medium text-gray-900
         dark:text-white;
}

.info-host {
  @apply text-gray-400 text-xs
         dark:text-gray-500;
}

.sql-section {
  @apply space-y-2;
}

.section-label {
  @apply block text-sm font-medium text-gray-700
         dark:text-gray-300;
}

.max-rows-select {
  @apply ml-1 px-2 py-0.5 text-xs border border-gray-300 rounded
         dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200;
}

.sql-textarea {
  @apply w-full px-3 py-2 text-sm font-mono border border-gray-300 rounded-lg
         focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none
         dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 dark:placeholder-gray-500;
}

.column-mapping {
  @apply flex items-end gap-3 p-3 bg-gray-50 rounded-lg
         dark:bg-gray-700;
}

.mapping-item {
  @apply flex-1 space-y-1;
}

.mapping-label {
  @apply block text-xs font-medium text-gray-500
         dark:text-gray-400;
}

.mapping-input {
  @apply w-full px-2 py-1.5 text-sm border border-gray-300 rounded
         focus:ring-primary-500 focus:border-primary-500
         dark:bg-gray-800 dark:border-gray-600 dark:text-gray-200 dark:placeholder-gray-500;
}

.mapping-select {
  @apply w-full px-2 py-1.5 text-sm border border-gray-300 rounded
         focus:ring-primary-500 focus:border-primary-500
         bg-white cursor-pointer
         dark:bg-gray-800 dark:border-gray-600 dark:text-gray-200;
}

.column-hint {
  @apply flex items-center gap-2 p-3 bg-blue-50 text-blue-700 rounded-lg text-sm
         dark:bg-blue-900/30 dark:text-blue-300;
}

.test-btn {
  @apply inline-flex items-center gap-2 px-4 py-1.5 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500
         disabled:opacity-50 disabled:cursor-not-allowed transition-colors;
}

.error-message {
  @apply flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-lg text-sm
         dark:bg-red-900/30 dark:text-red-400;
}

.result-section {
  @apply space-y-4 p-4 bg-green-50 rounded-lg
         dark:bg-green-900/30;
}

.result-header {
  @apply space-y-2;
}

.result-stats {
  @apply flex items-center gap-4 text-sm;
}

.stat-item {
  @apply flex items-center gap-1 text-gray-600
         dark:text-gray-300;
}

.result-columns {
  @apply text-xs text-gray-500 font-mono
         dark:text-gray-400;
}

.options-preview {
  @apply space-y-2;
}

.preview-title {
  @apply text-sm font-medium text-gray-700
         dark:text-gray-300;
}

.options-list {
  @apply grid grid-cols-2 md:grid-cols-3 gap-2 max-h-48 overflow-y-auto;
}

.option-item {
  @apply flex items-center gap-2 px-3 py-2 bg-white rounded border border-gray-200 text-sm
         dark:bg-gray-800 dark:border-gray-700;
}

.option-color {
  @apply w-3 h-3 rounded-full flex-shrink-0;
}

.option-label {
  @apply flex-1 truncate text-gray-900
         dark:text-white;
}

.option-value {
  @apply text-xs text-gray-400 font-mono
         dark:text-gray-500;
}

.raw-data {
  @apply mt-2;
}

.data-summary {
  @apply text-sm text-gray-600 cursor-pointer hover:text-gray-900
         dark:text-gray-400 dark:hover:text-gray-200;
}

.data-table-wrapper {
  @apply mt-2 max-h-60 overflow-auto border border-gray-200 rounded-lg
         dark:border-gray-700;
}

.data-table {
  @apply w-full text-xs;
}

.data-table th {
  @apply px-3 py-2 bg-gray-100 text-left font-medium text-gray-600 sticky top-0
         dark:bg-gray-700 dark:text-gray-300;
}

.data-table td {
  @apply px-3 py-2 border-t border-gray-100 text-gray-700
         dark:border-gray-700 dark:text-gray-300;
}

.btn-close {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors
         dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 dark:hover:bg-gray-600;
}

/* 도움말 섹션 */
.help-section {
  @apply mb-4;
}

.help-toggle {
  @apply flex items-center gap-2 w-full px-3 py-2 text-sm text-gray-600
         bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors
         dark:bg-blue-900/30 dark:text-blue-300 dark:hover:bg-blue-900/50;
}

.help-content {
  @apply mt-2 p-4 bg-blue-50 rounded-lg space-y-3 text-sm
         dark:bg-blue-900/20;
}

.help-item h5 {
  @apply font-medium text-gray-800 dark:text-gray-200;
}

.help-item p {
  @apply text-gray-600 mt-0.5 dark:text-gray-400;
}

.help-example {
  @apply mt-3 pt-3 border-t border-blue-200 dark:border-blue-800;
}

.help-example .example-label {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

.help-example code {
  @apply block mt-1 px-2 py-1 bg-white rounded text-xs font-mono text-gray-700
         dark:bg-gray-800 dark:text-gray-300;
}

/* 툴팁 */
.tooltip-container {
  @apply relative inline-flex ml-1;
}

.tooltip-text {
  @apply invisible absolute z-50 px-2 py-1 text-xs text-white bg-gray-900 rounded
         whitespace-nowrap opacity-0 transition-opacity
         bottom-full left-1/2 -translate-x-1/2 mb-1
         dark:bg-gray-600;
}

.tooltip-container:hover .tooltip-text {
  @apply visible opacity-100;
}

/* 인풋 래퍼 */
.input-wrapper {
  @apply relative;
}

.input-status {
  @apply absolute right-2 top-1/2 -translate-y-1/2;
}

.input-status.error {
  @apply text-red-500;
}

.input-status.success {
  @apply text-green-500;
}

.mapping-input.input-error,
.mapping-select.input-error {
  @apply border-red-500 focus:border-red-500 focus:ring-red-500
         dark:border-red-500;
}

/* 컬럼 매핑 경고 */
.column-warning {
  @apply flex items-start gap-3 p-3 bg-amber-50 text-amber-800 rounded-lg
         dark:bg-amber-900/30 dark:text-amber-300;
}

.warning-content {
  @apply flex flex-col gap-0.5;
}

.warning-title {
  @apply font-medium text-sm;
}

.warning-text {
  @apply text-sm;
}

.warning-hint {
  @apply text-xs text-amber-600 dark:text-amber-400;
}

/* 옵션 미리보기 개선 */
.preview-warning {
  @apply text-xs font-normal text-amber-600 ml-2
         dark:text-amber-400;
}

.option-color-empty {
  @apply w-3 h-3 rounded-full flex-shrink-0 border-2 border-dashed border-gray-300
         dark:border-gray-600;
}

.option-empty {
  @apply text-gray-400 italic dark:text-gray-500;
}
</style>
