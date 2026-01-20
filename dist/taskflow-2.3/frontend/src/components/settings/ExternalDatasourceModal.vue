<script setup lang="ts">
/**
 * 외부 데이터소스 추가/수정 모달
 * - 연결 정보 입력
 * - 연결 테스트
 * - 저장 전 검증
 */
import { ref, computed, watch, onMounted } from 'vue'
import type {
  ExternalDatasource,
  ExternalDatasourceCreateRequest,
  ExternalDatasourceUpdateRequest
} from '@/types/externalDatasource'
import { DB_TYPE_OPTIONS, CONNECTION_TYPE_OPTIONS } from '@/types/externalDatasource'
import { useExternalDatasourceStore } from '@/stores/externalDatasource'
import { useUiStore } from '@/stores/ui'

const props = defineProps<{
  datasource: ExternalDatasource | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'saved'): void
}>()

const datasourceStore = useExternalDatasourceStore()
const uiStore = useUiStore()

// 편집 모드 여부
const isEditMode = computed(() => !!props.datasource)

// 폼 데이터
const formData = ref({
  datasourceCode: '',
  datasourceName: '',
  dbType: 'MYSQL',
  connectionType: 'SERVICE_NAME',
  host: '',
  port: 3306,
  databaseName: '',
  schemaName: '',
  username: '',
  password: '',
  connectionTimeout: 5000,
  queryTimeout: 30000,
  maxPoolSize: 3,
  useYn: 'Y'
})

// 연결 테스트 상태
const testing = ref(false)
const testResult = ref<{ success: boolean; message: string } | null>(null)

// 저장 로딩
const saving = ref(false)

// DB 타입별 기본 포트
const defaultPorts: Record<string, number> = {
  MYSQL: 3306,
  ORACLE: 1521,
  MSSQL: 1433,
  TIBERO6: 8629,
  TIBERO7: 8629
}

// Oracle인지 확인
const isOracle = computed(() => formData.value.dbType === 'ORACLE')

// DB 타입 변경 시 기본 포트 설정
watch(() => formData.value.dbType, (newType) => {
  if (!isEditMode.value) {
    formData.value.port = defaultPorts[newType] || 3306
  }
  // connectionType은 Oracle만 사용
  if (newType !== 'ORACLE') {
    formData.value.connectionType = 'SERVICE_NAME'
  }
})

// 폼 유효성 검사
const isValid = computed(() => {
  const f = formData.value
  return (
    f.datasourceCode.trim() &&
    f.datasourceName.trim() &&
    f.dbType &&
    f.host.trim() &&
    f.port > 0 &&
    f.databaseName.trim() &&
    f.username.trim() &&
    (isEditMode.value || f.password.trim())
  )
})

// 폼 초기화
function initForm() {
  if (props.datasource) {
    formData.value = {
      datasourceCode: props.datasource.datasourceCode,
      datasourceName: props.datasource.datasourceName,
      dbType: props.datasource.dbType,
      connectionType: props.datasource.connectionType || 'SERVICE_NAME',
      host: props.datasource.host,
      port: props.datasource.port,
      databaseName: props.datasource.databaseName,
      schemaName: props.datasource.schemaName || '',
      username: props.datasource.username,
      password: '', // 비밀번호는 보안상 표시하지 않음
      connectionTimeout: props.datasource.connectionTimeout,
      queryTimeout: props.datasource.queryTimeout,
      maxPoolSize: props.datasource.maxPoolSize,
      useYn: props.datasource.useYn
    }
  } else {
    formData.value = {
      datasourceCode: '',
      datasourceName: '',
      dbType: 'MYSQL',
      connectionType: 'SERVICE_NAME',
      host: '',
      port: 3306,
      databaseName: '',
      schemaName: '',
      username: '',
      password: '',
      connectionTimeout: 5000,
      queryTimeout: 30000,
      maxPoolSize: 3,
      useYn: 'Y'
    }
  }
  testResult.value = null
}

// 연결 테스트
async function handleTest() {
  testing.value = true
  testResult.value = null

  try {
    const testData: ExternalDatasourceCreateRequest = {
      datasourceCode: formData.value.datasourceCode || 'TEST',
      datasourceName: formData.value.datasourceName || 'Test Connection',
      dbType: formData.value.dbType,
      connectionType: formData.value.connectionType,
      host: formData.value.host,
      port: formData.value.port,
      databaseName: formData.value.databaseName,
      schemaName: formData.value.schemaName || undefined,
      username: formData.value.username,
      password: formData.value.password,
      connectionTimeout: formData.value.connectionTimeout,
      queryTimeout: formData.value.queryTimeout,
      maxPoolSize: formData.value.maxPoolSize
    }

    const result = await datasourceStore.testNewConnection(testData)
    testResult.value = {
      success: result.success,
      message: result.success
        ? `연결 성공! (${result.responseTimeMs}ms) - ${result.dbVersion || ''}`
        : result.errorMessage || '연결 실패'
    }
  } catch (error) {
    console.error('Connection test failed:', error)
    testResult.value = {
      success: false,
      message: '연결 테스트에 실패했습니다.'
    }
  } finally {
    testing.value = false
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
    if (isEditMode.value && props.datasource) {
      // 수정
      const updateData: ExternalDatasourceUpdateRequest = {
        datasourceName: formData.value.datasourceName,
        dbType: formData.value.dbType,
        connectionType: formData.value.connectionType,
        host: formData.value.host,
        port: formData.value.port,
        databaseName: formData.value.databaseName,
        schemaName: formData.value.schemaName || undefined,
        username: formData.value.username,
        password: formData.value.password || undefined, // 비어있으면 변경하지 않음
        connectionTimeout: formData.value.connectionTimeout,
        queryTimeout: formData.value.queryTimeout,
        maxPoolSize: formData.value.maxPoolSize,
        useYn: formData.value.useYn
      }

      await datasourceStore.updateDatasource(props.datasource.datasourceId, updateData)
      uiStore.showSuccess('데이터소스가 수정되었습니다.')
    } else {
      // 생성
      const createData: ExternalDatasourceCreateRequest = {
        datasourceCode: formData.value.datasourceCode,
        datasourceName: formData.value.datasourceName,
        dbType: formData.value.dbType,
        connectionType: formData.value.connectionType,
        host: formData.value.host,
        port: formData.value.port,
        databaseName: formData.value.databaseName,
        schemaName: formData.value.schemaName || undefined,
        username: formData.value.username,
        password: formData.value.password,
        connectionTimeout: formData.value.connectionTimeout,
        queryTimeout: formData.value.queryTimeout,
        maxPoolSize: formData.value.maxPoolSize
      }

      await datasourceStore.createDatasource(createData)
      uiStore.showSuccess('데이터소스가 등록되었습니다.')
    }

    emit('saved')
  } catch (error) {
    console.error('Failed to save datasource:', error)
    uiStore.showError(isEditMode.value ? '데이터소스 수정에 실패했습니다.' : '데이터소스 등록에 실패했습니다.')
  } finally {
    saving.value = false
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
        <h2 class="modal-title">
          {{ isEditMode ? '데이터소스 수정' : '데이터소스 추가' }}
        </h2>
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
              <label class="form-label required">데이터소스 코드</label>
              <input
                v-model="formData.datasourceCode"
                type="text"
                class="form-input"
                :class="{ 'readonly': isEditMode }"
                :readonly="isEditMode"
                placeholder="예: ERP_MYSQL"
              />
              <p v-if="!isEditMode" class="form-hint">영문 대문자, 숫자, 언더스코어만 사용</p>
            </div>
            <div class="form-group">
              <label class="form-label required">데이터소스 이름</label>
              <input
                v-model="formData.datasourceName"
                type="text"
                class="form-input"
                placeholder="예: ERP MySQL 데이터베이스"
              />
            </div>
            <div class="form-group">
              <label class="form-label required">DB 타입</label>
              <select v-model="formData.dbType" class="form-input">
                <option
                  v-for="option in DB_TYPE_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>
            <div v-if="isOracle" class="form-group">
              <label class="form-label">연결 방식</label>
              <select v-model="formData.connectionType" class="form-input">
                <option
                  v-for="option in CONNECTION_TYPE_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <!-- 연결 정보 -->
        <div class="form-section">
          <h3 class="section-title">연결 정보</h3>
          <div class="form-grid">
            <div class="form-group col-span-2">
              <label class="form-label required">호스트</label>
              <input
                v-model="formData.host"
                type="text"
                class="form-input"
                placeholder="예: db.example.com"
              />
            </div>
            <div class="form-group">
              <label class="form-label required">포트</label>
              <input
                v-model.number="formData.port"
                type="number"
                min="1"
                max="65535"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label required">{{ isOracle && formData.connectionType === 'SID' ? 'SID' : '데이터베이스명' }}</label>
              <input
                v-model="formData.databaseName"
                type="text"
                class="form-input"
                :placeholder="isOracle && formData.connectionType === 'SID' ? '예: ORCL' : '예: mydb'"
              />
            </div>
            <div class="form-group">
              <label class="form-label">스키마명 (선택)</label>
              <input
                v-model="formData.schemaName"
                type="text"
                class="form-input"
                placeholder="기본 스키마 (선택사항)"
              />
              <p class="form-hint">비워두면 쿼리에서 직접 지정</p>
            </div>
          </div>
        </div>

        <!-- 인증 정보 -->
        <div class="form-section">
          <h3 class="section-title">인증 정보</h3>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label required">사용자명</label>
              <input
                v-model="formData.username"
                type="text"
                class="form-input"
                placeholder="DB 접속 계정"
              />
            </div>
            <div class="form-group">
              <label class="form-label" :class="{ 'required': !isEditMode }">비밀번호</label>
              <input
                v-model="formData.password"
                type="password"
                class="form-input"
                :placeholder="isEditMode ? '변경 시에만 입력' : '비밀번호 입력'"
              />
              <p v-if="isEditMode" class="form-hint">비워두면 기존 비밀번호 유지</p>
            </div>
          </div>
        </div>

        <!-- 고급 설정 -->
        <div class="form-section">
          <h3 class="section-title">고급 설정</h3>
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">연결 타임아웃 (ms)</label>
              <input
                v-model.number="formData.connectionTimeout"
                type="number"
                min="1000"
                max="60000"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">쿼리 타임아웃 (ms)</label>
              <input
                v-model.number="formData.queryTimeout"
                type="number"
                min="1000"
                max="300000"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">최대 풀 크기</label>
              <input
                v-model.number="formData.maxPoolSize"
                type="number"
                min="1"
                max="20"
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

        <!-- 연결 테스트 결과 -->
        <div v-if="testResult" class="test-result" :class="testResult.success ? 'success' : 'error'">
          <svg v-if="testResult.success" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
          <span>{{ testResult.message }}</span>
        </div>
      </div>

      <!-- 푸터 -->
      <div class="modal-footer">
        <button
          type="button"
          class="btn-test"
          :disabled="testing"
          @click="handleTest"
        >
          <svg v-if="testing" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          연결 테스트
        </button>
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
  @apply bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col
         dark:bg-gray-800;
}

.modal-header {
  @apply flex items-center justify-between px-6 py-4 border-b border-gray-200
         dark:border-gray-700;
}

.modal-title {
  @apply text-lg font-semibold text-gray-900
         dark:text-white;
}

.close-btn {
  @apply p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors
         dark:text-gray-500 dark:hover:text-gray-300 dark:hover:bg-gray-700;
}

.modal-body {
  @apply flex-1 overflow-y-auto px-6 py-4 space-y-6;
}

.modal-footer {
  @apply flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-gray-50
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

.test-result {
  @apply flex items-center gap-2 px-4 py-3 rounded-lg text-sm;
}

.test-result.success {
  @apply bg-green-50 text-green-700
         dark:bg-green-900/30 dark:text-green-400;
}

.test-result.error {
  @apply bg-red-50 text-red-700
         dark:bg-red-900/30 dark:text-red-400;
}

.btn-test {
  @apply inline-flex items-center gap-2 px-4 py-2 text-sm font-medium
         text-primary-600 bg-primary-50 rounded-lg
         hover:bg-primary-100 focus:outline-none focus:ring-2 focus:ring-primary-500
         disabled:opacity-50 disabled:cursor-not-allowed transition-colors
         dark:text-primary-400 dark:bg-primary-900/30 dark:hover:bg-primary-900/50;
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
</style>
