<script setup lang="ts">
/**
 * 외부 쿼리 추가/수정 모달
 * - SQL 입력
 * - 컬럼 매핑
 * - 캐시 설정
 */
import { ref, computed, watch, onMounted } from 'vue'
import type {
  ExternalQuery,
  ExternalQueryCreateRequest,
  ExternalQueryUpdateRequest,
  QueryOwnerType
} from '@/types/externalQuery'
import { useExternalQueryStore } from '@/stores/externalQuery'
import { useExternalDatasourceStore } from '@/stores/externalDatasource'
import { useUiStore } from '@/stores/ui'

const props = defineProps<{
  query: ExternalQuery | null
  ownerType: QueryOwnerType
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saved'): void
}>()

const queryStore = useExternalQueryStore()
const datasourceStore = useExternalDatasourceStore()
const uiStore = useUiStore()

// 편집 모드 여부
const isEditMode = computed(() => !!props.query)

// 활성 데이터소스 목록
const activeDatasources = computed(() => datasourceStore.activeDatasources)

// 폼 데이터
const formData = ref({
  datasourceId: 0,
  queryCode: '',
  queryName: '',
  description: '',
  querySql: '',
  valueColumn: 'value',
  labelColumn: 'label',
  colorColumn: '',
  cacheEnabledYn: 'N',
  cacheTtlSeconds: 300,
  sortOrder: 0,
  useYn: 'Y'
})

// 저장 로딩
const saving = ref(false)

// Owner Type 라벨
const ownerTypeLabel = computed(() => {
  switch (props.ownerType) {
    case 'GLOBAL': return '글로벌'
    case 'MANAGER': return '매니저'
    case 'USER': return '개인'
    default: return ''
  }
})

// 폼 유효성 검사
const isValid = computed(() => {
  const f = formData.value
  return (
    f.datasourceId > 0 &&
    f.queryCode.trim() &&
    f.queryName.trim() &&
    f.querySql.trim() &&
    f.valueColumn.trim() &&
    f.labelColumn.trim()
  )
})

// 폼 초기화
function initForm() {
  if (props.query) {
    formData.value = {
      datasourceId: props.query.datasourceId,
      queryCode: props.query.queryCode,
      queryName: props.query.queryName,
      description: props.query.description || '',
      querySql: props.query.querySql,
      valueColumn: props.query.valueColumn,
      labelColumn: props.query.labelColumn,
      colorColumn: props.query.colorColumn || '',
      cacheEnabledYn: props.query.cacheEnabledYn,
      cacheTtlSeconds: props.query.cacheTtlSeconds || 300,
      sortOrder: props.query.sortOrder,
      useYn: props.query.useYn
    }
  } else {
    // 기본 데이터소스 선택
    const defaultDs = activeDatasources.value[0]
    formData.value = {
      datasourceId: defaultDs?.datasourceId || 0,
      queryCode: '',
      queryName: '',
      description: '',
      querySql: 'SELECT \n  id AS value,\n  name AS label\nFROM table_name\nWHERE use_yn = \'Y\'\nORDER BY sort_order',
      valueColumn: 'value',
      labelColumn: 'label',
      colorColumn: '',
      cacheEnabledYn: 'N',
      cacheTtlSeconds: 300,
      sortOrder: 0,
      useYn: 'Y'
    }
  }
}

// 저장
async function handleSave() {
  if (!isValid.value) {
    uiStore.showWarning('필수 항목을 모두 입력해주세요.')
    return
  }

  saving.value = true

  try {
    if (isEditMode.value && props.query) {
      // 수정
      const updateData: ExternalQueryUpdateRequest = {
        datasourceId: formData.value.datasourceId,
        queryName: formData.value.queryName,
        description: formData.value.description || undefined,
        querySql: formData.value.querySql,
        valueColumn: formData.value.valueColumn,
        labelColumn: formData.value.labelColumn,
        colorColumn: formData.value.colorColumn || undefined,
        cacheEnabledYn: formData.value.cacheEnabledYn,
        cacheTtlSeconds: formData.value.cacheEnabledYn === 'Y' ? formData.value.cacheTtlSeconds : undefined,
        sortOrder: formData.value.sortOrder,
        useYn: formData.value.useYn
      }

      await queryStore.updateQuery(props.query.queryId, updateData)
      uiStore.showSuccess('쿼리가 수정되었습니다.')
    } else {
      // 생성
      const createData: ExternalQueryCreateRequest = {
        datasourceId: formData.value.datasourceId,
        queryCode: formData.value.queryCode,
        queryName: formData.value.queryName,
        description: formData.value.description || undefined,
        querySql: formData.value.querySql,
        valueColumn: formData.value.valueColumn,
        labelColumn: formData.value.labelColumn,
        colorColumn: formData.value.colorColumn || undefined,
        cacheEnabledYn: formData.value.cacheEnabledYn,
        cacheTtlSeconds: formData.value.cacheEnabledYn === 'Y' ? formData.value.cacheTtlSeconds : undefined,
        sortOrder: formData.value.sortOrder
      }

      // Owner Type에 따라 다른 API 호출
      switch (props.ownerType) {
        case 'GLOBAL':
          await queryStore.createGlobalQuery(createData)
          break
        case 'MANAGER':
          await queryStore.createManagerQuery(createData)
          break
        case 'USER':
          await queryStore.createUserQuery(createData)
          break
      }
      uiStore.showSuccess('쿼리가 등록되었습니다.')
    }

    emit('saved')
  } catch (error) {
    console.error('Failed to save query:', error)
    uiStore.showError(isEditMode.value ? '쿼리 수정에 실패했습니다.' : '쿼리 등록에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 닫기
function handleClose() {
  emit('close')
}

// SQL 샘플 삽입
function insertSample(type: string) {
  switch (type) {
    case 'basic':
      formData.value.querySql = `SELECT
  id AS value,
  name AS label
FROM table_name
WHERE use_yn = 'Y'
ORDER BY sort_order`
      break
    case 'with-color':
      formData.value.querySql = `SELECT
  code AS value,
  name AS label,
  color_code AS color
FROM code_table
WHERE use_yn = 'Y'
ORDER BY sort_order`
      break
    case 'join':
      formData.value.querySql = `SELECT
  u.user_id AS value,
  CONCAT(u.user_name, ' (', d.dept_name, ')') AS label
FROM users u
JOIN departments d ON u.dept_code = d.dept_code
WHERE u.status = 'ACTIVE'
ORDER BY u.user_name`
      break
  }
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
          <h2 class="modal-title">
            {{ isEditMode ? '쿼리 수정' : '쿼리 추가' }}
          </h2>
          <p class="modal-subtitle">{{ ownerTypeLabel }} 쿼리</p>
        </div>
        <button type="button" class="close-btn" @click="handleClose">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 본문 -->
      <div class="modal-body">
        <!-- 기본 정보 -->
        <div class="form-section">
          <h3 class="section-title">기본 정보</h3>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label required">데이터소스</label>
              <select v-model.number="formData.datasourceId" class="form-input">
                <option :value="0" disabled>데이터소스 선택</option>
                <option
                  v-for="ds in activeDatasources"
                  :key="ds.datasourceId"
                  :value="ds.datasourceId"
                >
                  {{ ds.datasourceName }} ({{ ds.dbType }})
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label required">쿼리 코드</label>
              <input
                v-model="formData.queryCode"
                type="text"
                class="form-input"
                :class="{ 'readonly': isEditMode }"
                :readonly="isEditMode"
                placeholder="예: DEPT_LIST"
              />
              <p v-if="!isEditMode" class="form-hint">영문 대문자, 숫자, 언더스코어만 사용</p>
            </div>
            <div class="form-group col-span-2">
              <label class="form-label required">쿼리 이름</label>
              <input
                v-model="formData.queryName"
                type="text"
                class="form-input"
                placeholder="예: 부서 목록"
              />
            </div>
            <div class="form-group col-span-2">
              <label class="form-label">설명</label>
              <input
                v-model="formData.description"
                type="text"
                class="form-input"
                placeholder="쿼리에 대한 설명 (선택)"
              />
            </div>
          </div>
        </div>

        <!-- SQL 입력 -->
        <div class="form-section">
          <div class="flex items-center justify-between mb-2">
            <h3 class="section-title mb-0">SQL 쿼리</h3>
            <div class="flex gap-2">
              <button type="button" class="sample-btn" @click="insertSample('basic')">
                기본 샘플
              </button>
              <button type="button" class="sample-btn" @click="insertSample('with-color')">
                색상 포함
              </button>
              <button type="button" class="sample-btn" @click="insertSample('join')">
                조인 샘플
              </button>
            </div>
          </div>
          <div class="form-group">
            <textarea
              v-model="formData.querySql"
              class="sql-input"
              rows="8"
              placeholder="SELECT 쿼리를 입력하세요..."
            ></textarea>
            <p class="form-hint">SELECT 문만 허용됩니다. value, label 컬럼을 포함해야 합니다.</p>
          </div>
        </div>

        <!-- 컬럼 매핑 -->
        <div class="form-section">
          <div class="flex items-center justify-between">
            <h3 class="section-title mb-0">컬럼 매핑</h3>
          </div>
          <!-- 안내 문구 -->
          <div class="column-mapping-guide">
            <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>SQL 쿼리에서 사용한 컬럼명 또는 별칭(AS)을 입력하세요. 등록 후 "테스트" 기능에서 컬럼을 선택할 수 있습니다.</span>
          </div>
          <!-- 대문자 변환 안내 -->
          <div class="column-mapping-notice">
            <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span>컬럼명은 저장 시 자동으로 <strong>대문자</strong>로 변환됩니다. (예: userid → USERID)</span>
          </div>
          <div class="form-grid cols-3">
            <div class="form-group">
              <label class="form-label required">
                Value 컬럼
                <span class="tooltip-container">
                  <svg class="w-3.5 h-3.5 text-gray-400 hover:text-gray-600 cursor-help dark:hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span class="tooltip-text">DB에 저장되는 실제 값 (예: 사원번호, 코드)</span>
                </span>
              </label>
              <input
                v-model="formData.valueColumn"
                type="text"
                class="form-input"
                placeholder="value"
              />
              <p class="form-hint">실제 저장될 값 (예: id, code, user_id)</p>
            </div>
            <div class="form-group">
              <label class="form-label required">
                Label 컬럼
                <span class="tooltip-container">
                  <svg class="w-3.5 h-3.5 text-gray-400 hover:text-gray-600 cursor-help dark:hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span class="tooltip-text">화면에 표시되는 이름 (예: 사원명, 부서명)</span>
                </span>
              </label>
              <input
                v-model="formData.labelColumn"
                type="text"
                class="form-input"
                placeholder="label"
              />
              <p class="form-hint">화면 표시명 (예: name, title, user_name)</p>
            </div>
            <div class="form-group">
              <label class="form-label">
                Color 컬럼
                <span class="tooltip-container">
                  <svg class="w-3.5 h-3.5 text-gray-400 hover:text-gray-600 cursor-help dark:hover:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span class="tooltip-text">옵션별 색상 코드 (예: #FF5733) - 드롭다운에 색상 배지 표시</span>
                </span>
              </label>
              <input
                v-model="formData.colorColumn"
                type="text"
                class="form-input"
                placeholder="(선택) color"
              />
              <p class="form-hint">선택사항 - 비워두면 색상 미적용</p>
            </div>
          </div>
        </div>

        <!-- 캐시 설정 -->
        <div class="form-section">
          <h3 class="section-title">캐시 설정</h3>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">캐시 사용</label>
              <div class="flex items-center gap-4 mt-2">
                <label class="radio-label">
                  <input
                    v-model="formData.cacheEnabledYn"
                    type="radio"
                    value="N"
                    class="radio-input"
                  />
                  <span>사용 안 함</span>
                </label>
                <label class="radio-label">
                  <input
                    v-model="formData.cacheEnabledYn"
                    type="radio"
                    value="Y"
                    class="radio-input"
                  />
                  <span>사용</span>
                </label>
              </div>
            </div>
            <div v-if="formData.cacheEnabledYn === 'Y'" class="form-group">
              <label class="form-label">캐시 유지 시간 (초)</label>
              <input
                v-model.number="formData.cacheTtlSeconds"
                type="number"
                min="60"
                max="86400"
                class="form-input"
              />
              <p class="form-hint">60초 ~ 86400초 (1일)</p>
            </div>
          </div>
        </div>

        <!-- 기타 설정 -->
        <div class="form-section">
          <h3 class="section-title">기타 설정</h3>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">정렬 순서</label>
              <input
                v-model.number="formData.sortOrder"
                type="number"
                min="0"
                class="form-input"
              />
            </div>
            <div v-if="isEditMode" class="form-group">
              <label class="form-label">사용 여부</label>
              <select v-model="formData.useYn" class="form-input">
                <option value="Y">활성</option>
                <option value="N">비활성</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <div class="flex gap-2">
          <button type="button" class="btn-cancel" @click="handleClose">
            취소
          </button>
          <button
            type="button"
            class="btn-primary"
            :disabled="!isValid || saving"
            @click="handleSave"
          >
            <svg v-if="saving" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            {{ isEditMode ? '수정' : '등록' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 bg-black/50 flex items-center justify-center z-50;
}

.modal-container {
  @apply bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col
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
  @apply flex-1 overflow-y-auto px-6 py-4 space-y-6;
}

.modal-footer {
  @apply flex items-center justify-end px-6 py-4 border-t border-gray-200 bg-gray-50
         dark:border-gray-700 dark:bg-gray-900;
}

.form-section {
  @apply space-y-4;
}

.section-title {
  @apply text-sm font-medium text-gray-700 border-b border-gray-200 pb-2
         dark:text-gray-300 dark:border-gray-700;
}

.form-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.form-grid.cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 767px) {
  .form-grid.cols-3 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

.form-group {
  @apply space-y-1;
}

.form-group.col-span-2 {
  grid-column: span 2 / span 2;
}

@media (max-width: 767px) {
  .form-group.col-span-2 {
    grid-column: span 1 / span 1;
  }
}

.form-label {
  @apply block text-sm font-medium text-gray-700
         dark:text-gray-300;
}

.form-label.required::after {
  content: ' *';
  @apply text-red-500;
}

.form-input {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-lg
         focus:ring-2 focus:ring-primary-500 focus:border-primary-500
         dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 dark:placeholder-gray-500;
}

.form-input.readonly {
  @apply bg-gray-100 cursor-not-allowed
         dark:bg-gray-900;
}

.form-hint {
  @apply text-xs text-gray-500
         dark:text-gray-400;
}

.sql-input {
  @apply w-full px-3 py-2 text-sm font-mono border border-gray-300 rounded-lg
         focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none
         dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 dark:placeholder-gray-500;
}

.sample-btn {
  @apply px-2 py-1 text-xs text-gray-500 bg-gray-100 rounded hover:bg-gray-200 transition-colors
         dark:text-gray-400 dark:bg-gray-700 dark:hover:bg-gray-600;
}

.radio-label {
  @apply flex items-center gap-2 text-sm text-gray-700 cursor-pointer
         dark:text-gray-300;
}

.radio-input {
  @apply w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500
         dark:border-gray-600 dark:bg-gray-700;
}

.btn-cancel {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors
         dark:bg-gray-700 dark:border-gray-600 dark:text-gray-200 dark:hover:bg-gray-600;
}

.btn-primary {
  @apply inline-flex items-center gap-2 justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed transition-colors;
}

/* 툴팁 스타일 */
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

/* 컬럼 매핑 안내 */
.column-mapping-guide {
  @apply flex items-start gap-2 p-3 mb-2 bg-blue-50 text-blue-700 rounded-lg text-sm
         dark:bg-blue-900/30 dark:text-blue-300;
}

/* 대문자 변환 안내 */
.column-mapping-notice {
  @apply flex items-start gap-2 p-3 mb-3 bg-amber-50 text-amber-700 rounded-lg text-sm
         dark:bg-amber-900/30 dark:text-amber-300;
}
</style>
