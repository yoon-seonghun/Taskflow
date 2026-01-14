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
            <label class="mapping-label">Value 컬럼</label>
            <input
              v-model="valueColumn"
              type="text"
              class="mapping-input"
              placeholder="value"
            />
          </div>
          <div class="mapping-item">
            <label class="mapping-label">Label 컬럼</label>
            <input
              v-model="labelColumn"
              type="text"
              class="mapping-input"
              placeholder="label"
            />
          </div>
          <div class="mapping-item">
            <label class="mapping-label">Color 컬럼</label>
            <input
              v-model="colorColumn"
              type="text"
              class="mapping-input"
              placeholder="color (선택)"
            />
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
            <h4 class="preview-title">옵션 미리보기 (상위 {{ testResult.options.length }}개)</h4>
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
                ></span>
                <span class="option-label">{{ option.label }}</span>
                <span class="option-value">{{ option.value }}</span>
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
  @apply bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col;
}

.modal-header {
  @apply flex items-start justify-between px-6 py-4 border-b border-gray-200;
}

.modal-title {
  @apply text-lg font-semibold text-gray-900;
}

.modal-subtitle {
  @apply text-sm text-gray-500 mt-0.5;
}

.close-btn {
  @apply p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors;
}

.modal-body {
  @apply flex-1 overflow-y-auto px-6 py-4 space-y-4;
}

.modal-footer {
  @apply flex items-center justify-end px-6 py-4 border-t border-gray-200 bg-gray-50;
}

.datasource-info {
  @apply flex items-center gap-2 p-3 bg-gray-50 rounded-lg text-sm;
}

.info-label {
  @apply text-gray-500;
}

.info-value {
  @apply font-medium text-gray-900;
}

.info-host {
  @apply text-gray-400 text-xs;
}

.sql-section {
  @apply space-y-2;
}

.section-label {
  @apply block text-sm font-medium text-gray-700;
}

.max-rows-select {
  @apply ml-1 px-2 py-0.5 text-xs border border-gray-300 rounded;
}

.sql-textarea {
  @apply w-full px-3 py-2 text-sm font-mono border border-gray-300 rounded-lg
         focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none;
}

.column-mapping {
  @apply flex items-end gap-3 p-3 bg-gray-50 rounded-lg;
}

.mapping-item {
  @apply flex-1 space-y-1;
}

.mapping-label {
  @apply block text-xs font-medium text-gray-500;
}

.mapping-input {
  @apply w-full px-2 py-1.5 text-sm border border-gray-300 rounded
         focus:ring-primary-500 focus:border-primary-500;
}

.test-btn {
  @apply inline-flex items-center gap-2 px-4 py-1.5 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500
         disabled:opacity-50 disabled:cursor-not-allowed transition-colors;
}

.error-message {
  @apply flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-lg text-sm;
}

.result-section {
  @apply space-y-4 p-4 bg-green-50 rounded-lg;
}

.result-header {
  @apply space-y-2;
}

.result-stats {
  @apply flex items-center gap-4 text-sm;
}

.stat-item {
  @apply flex items-center gap-1 text-gray-600;
}

.result-columns {
  @apply text-xs text-gray-500 font-mono;
}

.options-preview {
  @apply space-y-2;
}

.preview-title {
  @apply text-sm font-medium text-gray-700;
}

.options-list {
  @apply grid grid-cols-2 md:grid-cols-3 gap-2 max-h-48 overflow-y-auto;
}

.option-item {
  @apply flex items-center gap-2 px-3 py-2 bg-white rounded border border-gray-200 text-sm;
}

.option-color {
  @apply w-3 h-3 rounded-full flex-shrink-0;
}

.option-label {
  @apply flex-1 truncate text-gray-900;
}

.option-value {
  @apply text-xs text-gray-400 font-mono;
}

.raw-data {
  @apply mt-2;
}

.data-summary {
  @apply text-sm text-gray-600 cursor-pointer hover:text-gray-900;
}

.data-table-wrapper {
  @apply mt-2 max-h-60 overflow-auto border border-gray-200 rounded-lg;
}

.data-table {
  @apply w-full text-xs;
}

.data-table th {
  @apply px-3 py-2 bg-gray-100 text-left font-medium text-gray-600 sticky top-0;
}

.data-table td {
  @apply px-3 py-2 border-t border-gray-100 text-gray-700;
}

.btn-close {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors;
}
</style>
