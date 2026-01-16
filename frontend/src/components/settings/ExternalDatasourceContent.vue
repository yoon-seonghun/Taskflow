<script setup lang="ts">
/**
 * 외부 DB 관리 콘텐츠 컴포넌트 (설정 페이지 내 탭용)
 * - 데이터소스 CRUD
 * - 연결 테스트
 * - ADMIN 전용
 */
import { ref, computed, onMounted } from 'vue'
import Badge from '@/components/common/Badge.vue'
import ExternalDatasourceModal from './ExternalDatasourceModal.vue'
import type { ExternalDatasource } from '@/types/externalDatasource'
import { DB_TYPE_OPTIONS } from '@/types/externalDatasource'
import { useExternalDatasourceStore } from '@/stores/externalDatasource'
import { useUiStore } from '@/stores/ui'

const datasourceStore = useExternalDatasourceStore()
const uiStore = useUiStore()

// 상태
const loading = computed(() => datasourceStore.loading)
const datasources = computed(() => datasourceStore.datasources)

// 필터
const filterDbType = ref<string>('')
const filterKeyword = ref<string>('')

// 모달 상태
const showModal = ref(false)
const editingDatasource = ref<ExternalDatasource | null>(null)

// 연결 테스트 상태
const testingId = ref<number | null>(null)

// 필터링된 목록
const filteredDatasources = computed(() => {
  let result = datasources.value

  if (filterDbType.value) {
    result = result.filter(d => d.dbType === filterDbType.value)
  }

  if (filterKeyword.value) {
    const keyword = filterKeyword.value.toLowerCase()
    result = result.filter(d =>
      d.datasourceName.toLowerCase().includes(keyword) ||
      d.datasourceCode.toLowerCase().includes(keyword) ||
      d.host.toLowerCase().includes(keyword)
    )
  }

  return result
})

// 초기 로드
async function loadData() {
  try {
    await datasourceStore.fetchDatasources()
  } catch (error) {
    console.error('Failed to fetch datasources:', error)
    uiStore.showError('데이터소스 목록 조회에 실패했습니다.')
  }
}

// 새 데이터소스 추가
function handleAdd() {
  editingDatasource.value = null
  showModal.value = true
}

// 데이터소스 수정
function handleEdit(datasource: ExternalDatasource) {
  editingDatasource.value = datasource
  showModal.value = true
}

// 데이터소스 삭제
async function handleDelete(datasource: ExternalDatasource) {
  const confirmed = await uiStore.confirm({
    title: '데이터소스 삭제',
    message: `'${datasource.datasourceName}' 데이터소스를 삭제하시겠습니까?\n이 데이터소스를 사용하는 쿼리가 있으면 함께 영향을 받습니다.`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    await datasourceStore.deleteDatasource(datasource.datasourceId)
    uiStore.showSuccess('데이터소스가 삭제되었습니다.')
  } catch (error) {
    console.error('Failed to delete datasource:', error)
    uiStore.showError('데이터소스 삭제에 실패했습니다.')
  }
}

// 연결 테스트
async function handleTestConnection(datasource: ExternalDatasource) {
  testingId.value = datasource.datasourceId
  try {
    const result = await datasourceStore.testConnection(datasource.datasourceId)
    if (result.success) {
      uiStore.showSuccess(`연결 성공! (${result.responseTimeMs}ms)`)
    } else {
      uiStore.showError(`연결 실패: ${result.errorMessage}`)
    }
  } catch (error) {
    console.error('Connection test failed:', error)
    uiStore.showError('연결 테스트에 실패했습니다.')
  } finally {
    testingId.value = null
  }
}

// 모달 닫기
function handleModalClose() {
  showModal.value = false
  editingDatasource.value = null
}

// 모달 저장 완료
function handleModalSaved() {
  showModal.value = false
  editingDatasource.value = null
}

// DB 타입 라벨 가져오기
function getDbTypeLabel(dbType: string): string {
  const option = DB_TYPE_OPTIONS.find(o => o.value === dbType)
  return option?.label || dbType
}

// DB 타입 색상 가져오기
function getDbTypeColor(dbType: string): string {
  switch (dbType) {
    case 'MYSQL': return 'primary'
    case 'ORACLE': return 'warning'
    case 'MSSQL': return 'info'
    case 'TIBERO': return 'success'
    default: return 'default'
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="datasource-content">
    <!-- 설명 -->
    <div class="mb-4">
      <p class="text-sm text-gray-500">
        외부 데이터베이스 연결을 설정하고 관리합니다. 등록된 데이터소스는 외부 쿼리에서 사용할 수 있습니다.
      </p>
    </div>

    <!-- 상단 필터/검색 바 -->
    <div class="flex items-center justify-between gap-4 mb-4">
      <div class="flex items-center gap-3">
        <!-- DB 타입 필터 -->
        <select
          v-model="filterDbType"
          class="form-select"
        >
          <option value="">전체 DB 타입</option>
          <option
            v-for="option in DB_TYPE_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>

        <!-- 검색 -->
        <div class="search-box">
          <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            v-model="filterKeyword"
            type="text"
            class="search-input"
            placeholder="이름, 코드, 호스트 검색..."
          />
        </div>
      </div>

      <!-- 추가 버튼 -->
      <button type="button" class="btn-primary" @click="handleAdd">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        데이터소스 추가
      </button>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-state">
      <svg class="animate-spin h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">데이터소스 목록을 불러오는 중...</p>
    </div>

    <!-- 데이터소스 목록 -->
    <div v-else-if="filteredDatasources.length > 0" class="datasource-list">
      <div
        v-for="datasource in filteredDatasources"
        :key="datasource.datasourceId"
        class="datasource-card"
      >
        <div class="card-header">
          <div class="flex items-center gap-3">
            <!-- DB 타입 아이콘 -->
            <div class="db-icon" :class="`db-${datasource.dbType.toLowerCase()}`">
              <span class="text-xs font-bold">{{ datasource.dbType.substring(0, 2) }}</span>
            </div>
            <div>
              <h3 class="card-title">{{ datasource.datasourceName }}</h3>
              <p class="card-code">{{ datasource.datasourceCode }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <Badge :variant="getDbTypeColor(datasource.dbType)" size="sm">
              {{ getDbTypeLabel(datasource.dbType) }}
            </Badge>
            <Badge :variant="datasource.useYn === 'Y' ? 'success' : 'default'" size="sm">
              {{ datasource.useYn === 'Y' ? '활성' : '비활성' }}
            </Badge>
          </div>
        </div>

        <div class="card-body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">호스트</span>
              <span class="info-value">{{ datasource.host }}:{{ datasource.port }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">데이터베이스</span>
              <span class="info-value">{{ datasource.databaseName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">사용자</span>
              <span class="info-value">{{ datasource.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">풀 크기</span>
              <span class="info-value">{{ datasource.maxPoolSize }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <div class="footer-meta">
            <span v-if="datasource.createdByName" class="text-xs text-gray-400">
              등록: {{ datasource.createdByName }}
            </span>
          </div>
          <div class="footer-actions">
            <button
              type="button"
              class="action-btn test"
              :disabled="testingId === datasource.datasourceId"
              @click="handleTestConnection(datasource)"
            >
              <svg v-if="testingId === datasource.datasourceId" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              테스트
            </button>
            <button type="button" class="action-btn edit" @click="handleEdit(datasource)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              수정
            </button>
            <button type="button" class="action-btn delete" @click="handleDelete(datasource)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              삭제
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="empty-state">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
          d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">
        {{ filterDbType || filterKeyword ? '검색 결과가 없습니다.' : '등록된 데이터소스가 없습니다.' }}
      </p>
      <button
        v-if="!filterDbType && !filterKeyword"
        type="button"
        class="mt-3 text-sm text-primary-600 hover:text-primary-700"
        @click="handleAdd"
      >
        + 첫 번째 데이터소스 추가하기
      </button>
    </div>

    <!-- 데이터소스 추가/수정 모달 -->
    <ExternalDatasourceModal
      v-if="showModal"
      :datasource="editingDatasource"
      @close="handleModalClose"
      @saved="handleModalSaved"
    />
  </div>
</template>

<style scoped>
.datasource-content {
  @apply w-full;
}

.form-select {
  @apply px-3 py-2 text-sm border border-gray-300 rounded-lg bg-white
         focus:ring-primary-500 focus:border-primary-500
         dark:bg-gray-700 dark:border-gray-600 dark:text-white;
}

.search-box {
  @apply relative;
}

.search-icon {
  @apply absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400;
}

.search-input {
  @apply pl-9 pr-4 py-2 text-sm border border-gray-300 rounded-lg w-64
         focus:ring-primary-500 focus:border-primary-500
         dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400;
}

.btn-primary {
  @apply inline-flex items-center gap-2 px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150
         dark:focus:ring-offset-gray-900;
}

.loading-state {
  @apply py-12 text-center;
}

.datasource-list {
  @apply space-y-4;
}

.datasource-card {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden
         hover:shadow-md transition-shadow
         dark:bg-gray-800 dark:border-gray-700;
}

.card-header {
  @apply flex items-center justify-between p-4 border-b border-gray-100
         dark:border-gray-700;
}

.db-icon {
  @apply w-10 h-10 rounded-lg flex items-center justify-center text-white;
}

.db-mysql {
  @apply bg-blue-600;
}

.db-oracle {
  @apply bg-red-600;
}

.db-mssql {
  @apply bg-gray-700 dark:bg-gray-500;
}

.db-tibero {
  @apply bg-green-600;
}

.card-title {
  @apply text-base font-medium text-gray-900 dark:text-white;
}

.card-code {
  @apply text-xs text-gray-500 font-mono dark:text-gray-400;
}

.card-body {
  @apply p-4;
}

.info-grid {
  @apply grid grid-cols-2 md:grid-cols-4 gap-4;
}

.info-item {
  @apply space-y-1;
}

.info-label {
  @apply block text-xs text-gray-500 dark:text-gray-400;
}

.info-value {
  @apply block text-sm text-gray-900 font-medium truncate dark:text-white;
}

.card-footer {
  @apply flex items-center justify-between px-4 py-3 bg-gray-50 border-t border-gray-100
         dark:bg-gray-700/50 dark:border-gray-700;
}

.footer-meta {
  @apply text-sm text-gray-500 dark:text-gray-400;
}

.footer-actions {
  @apply flex items-center gap-2;
}

.action-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md
         transition-colors duration-150;
}

.action-btn.test {
  @apply text-primary-600 bg-primary-50 hover:bg-primary-100
         dark:text-primary-400 dark:bg-primary-900/50 dark:hover:bg-primary-900/70;
}

.action-btn.edit {
  @apply text-gray-600 bg-gray-100 hover:bg-gray-200
         dark:text-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600;
}

.action-btn.delete {
  @apply text-red-600 bg-red-50 hover:bg-red-100
         dark:text-red-400 dark:bg-red-900/50 dark:hover:bg-red-900/70;
}

.action-btn:disabled {
  @apply opacity-50 cursor-not-allowed;
}

.empty-state {
  @apply text-center py-12 bg-white rounded-lg border border-gray-200
         dark:bg-gray-800 dark:border-gray-700;
}
</style>
