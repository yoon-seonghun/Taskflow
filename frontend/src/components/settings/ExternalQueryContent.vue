<script setup lang="ts">
/**
 * 외부 쿼리 관리 콘텐츠 컴포넌트 (설정 페이지 내 탭용)
 * - Owner 타입별 탭 (글로벌/매니저/개인)
 * - 쿼리 CRUD
 * - 쿼리 테스트/실행
 */
import { ref, computed, onMounted, watch } from 'vue'
import Badge from '@/components/common/Badge.vue'
import ExternalQueryModal from './ExternalQueryModal.vue'
import QueryTestModal from './QueryTestModal.vue'
import type { ExternalQuery, QueryOwnerType } from '@/types/externalQuery'
import { useExternalQueryStore } from '@/stores/externalQuery'
import { useExternalDatasourceStore } from '@/stores/externalDatasource'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const queryStore = useExternalQueryStore()
const datasourceStore = useExternalDatasourceStore()
const authStore = useAuthStore()
const uiStore = useUiStore()

// 현재 탭 (Owner Type)
type TabType = 'global' | 'manager' | 'user'
const activeTab = ref<TabType>('global')

// 로딩 상태
const loading = computed(() => queryStore.loading)

// 탭별 쿼리 목록
const globalQueries = computed(() => queryStore.globalQueries)
const managerQueries = computed(() => queryStore.managerQueries)
const userQueries = computed(() => queryStore.userQueries)

// 현재 탭의 쿼리 목록
const currentQueries = computed(() => {
  switch (activeTab.value) {
    case 'global': return globalQueries.value
    case 'manager': return managerQueries.value
    case 'user': return userQueries.value
    default: return []
  }
})

// 검색 필터
const filterKeyword = ref('')

// 필터링된 목록
const filteredQueries = computed(() => {
  if (!filterKeyword.value) return currentQueries.value

  const keyword = filterKeyword.value.toLowerCase()
  return currentQueries.value.filter(q =>
    q.queryName.toLowerCase().includes(keyword) ||
    q.queryCode.toLowerCase().includes(keyword) ||
    (q.description?.toLowerCase().includes(keyword))
  )
})

// 모달 상태
const showModal = ref(false)
const editingQuery = ref<ExternalQuery | null>(null)
const modalOwnerType = ref<QueryOwnerType>('GLOBAL')

// 테스트 모달 상태
const showTestModal = ref(false)
const testingQuery = ref<ExternalQuery | null>(null)

// 쿼리 실행 상태
const executingId = ref<number | null>(null)

// 권한 확인
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const isManager = computed(() => authStore.user?.role === 'MANAGER' || isAdmin.value)

// 탭 목록
const tabs = computed(() => [
  { id: 'global' as TabType, label: '글로벌', count: globalQueries.value.length, canCreate: isAdmin.value },
  { id: 'manager' as TabType, label: '매니저', count: managerQueries.value.length, canCreate: isManager.value },
  { id: 'user' as TabType, label: '개인', count: userQueries.value.length, canCreate: true }
])

// 초기 로드
async function loadData() {
  try {
    // 쿼리 목록은 모든 사용자가 조회 가능
    await queryStore.fetchAccessibleQueries()

    // 데이터소스 목록은 ADMIN만 조회 가능 (ADMIN이 아닌 경우 건너뜀)
    if (isAdmin.value) {
      await datasourceStore.fetchActiveDatasources()
    }
  } catch (error) {
    console.error('Failed to fetch data:', error)
    uiStore.showError('데이터 조회에 실패했습니다.')
  }
}

// 탭 변경
function changeTab(tab: TabType) {
  activeTab.value = tab
  filterKeyword.value = ''
}

// 새 쿼리 추가
function handleAdd() {
  editingQuery.value = null
  // 현재 탭에 따라 Owner Type 설정
  switch (activeTab.value) {
    case 'global': modalOwnerType.value = 'GLOBAL'; break
    case 'manager': modalOwnerType.value = 'MANAGER'; break
    case 'user': modalOwnerType.value = 'USER'; break
  }
  showModal.value = true
}

// 쿼리 수정
function handleEdit(query: ExternalQuery) {
  editingQuery.value = query
  modalOwnerType.value = query.ownerType
  showModal.value = true
}

// 쿼리 삭제
async function handleDelete(query: ExternalQuery) {
  const confirmed = await uiStore.confirm({
    title: '쿼리 삭제',
    message: `'${query.queryName}' 쿼리를 삭제하시겠습니까?\n이 쿼리를 사용하는 속성이 있으면 영향을 받습니다.`,
    confirmText: '삭제',
    cancelText: '취소',
    confirmType: 'danger'
  })

  if (!confirmed) return

  try {
    await queryStore.deleteQuery(query.queryId)
    uiStore.showSuccess('쿼리가 삭제되었습니다.')
  } catch (error) {
    console.error('Failed to delete query:', error)
    uiStore.showError('쿼리 삭제에 실패했습니다.')
  }
}

// 쿼리 테스트 모달 열기
function handleTest(query: ExternalQuery) {
  testingQuery.value = query
  showTestModal.value = true
}

// 쿼리 실행 (옵션 미리보기)
async function handleExecute(query: ExternalQuery) {
  executingId.value = query.queryId
  try {
    const result = await queryStore.executeQuery(query.queryId, false) // 캐시 무시
    if (result.success) {
      uiStore.showSuccess(`실행 완료: ${result.resultCount}건 (${result.cached ? '캐시됨' : '신규 조회'})`)
    } else {
      uiStore.showError(`실행 실패: ${result.message}`)
    }
  } catch (error) {
    console.error('Failed to execute query:', error)
    uiStore.showError('쿼리 실행에 실패했습니다.')
  } finally {
    executingId.value = null
  }
}

// 모달 닫기
function handleModalClose() {
  showModal.value = false
  editingQuery.value = null
}

// 모달 저장 완료
function handleModalSaved() {
  showModal.value = false
  editingQuery.value = null
}

// 테스트 모달 닫기
function handleTestModalClose() {
  showTestModal.value = false
  testingQuery.value = null
}

// 데이터소스 이름 가져오기 (쿼리 응답에 포함된 datasourceName 우선 사용)
function getDatasourceName(query: ExternalQuery): string {
  // 쿼리 응답에 포함된 datasourceName 우선 사용
  if (query.datasourceName) {
    return query.datasourceName
  }
  // 폴백: 스토어에서 조회 (ADMIN만 로드됨)
  const ds = datasourceStore.getDatasourceById(query.datasourceId)
  return ds?.datasourceName || `ID: ${query.datasourceId}`
}

// 캐시 상태 텍스트
function getCacheText(query: ExternalQuery): string {
  if (query.cacheEnabledYn !== 'Y') return '캐시 없음'
  const ttl = query.cacheTtlSeconds || 300
  return `캐시: ${ttl >= 60 ? Math.round(ttl / 60) + '분' : ttl + '초'}`
}

// 현재 탭에서 추가 가능 여부
const canCreateInCurrentTab = computed(() => {
  const tab = tabs.value.find(t => t.id === activeTab.value)
  return tab?.canCreate ?? false
})

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="query-content">
    <!-- 설명 -->
    <div class="mb-4">
      <p class="text-sm text-gray-500">
        외부 데이터베이스에서 실행할 쿼리를 정의하고 관리합니다. 쿼리 결과는 속성의 선택 옵션으로 활용할 수 있습니다.
      </p>
    </div>

    <!-- 탭 네비게이션 -->
    <div class="flex items-center justify-between mb-4">
      <div class="tab-nav">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="tab-btn"
          :class="{ 'active': activeTab === tab.id }"
          @click="changeTab(tab.id)"
        >
          {{ tab.label }}
          <span class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <div class="flex items-center gap-3">
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
            placeholder="쿼리 검색..."
          />
        </div>

        <!-- 추가 버튼 -->
        <button
          v-if="canCreateInCurrentTab"
          type="button"
          class="btn-primary"
          @click="handleAdd"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          쿼리 추가
        </button>
      </div>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-state">
      <svg class="animate-spin h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">쿼리 목록을 불러오는 중...</p>
    </div>

    <!-- 쿼리 목록 -->
    <div v-else-if="filteredQueries.length > 0" class="query-list">
      <div
        v-for="query in filteredQueries"
        :key="query.queryId"
        class="query-card"
      >
        <div class="card-header">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="card-title">{{ query.queryName }}</h3>
              <Badge :variant="query.useYn === 'Y' ? 'success' : 'default'" size="xs">
                {{ query.useYn === 'Y' ? '활성' : '비활성' }}
              </Badge>
            </div>
            <div class="card-meta">
              <span class="query-code">{{ query.queryCode }}</span>
              <span class="separator">|</span>
              <span class="datasource-name">{{ getDatasourceName(query) }}</span>
            </div>
          </div>
        </div>

        <div class="card-body">
          <!-- SQL 미리보기 -->
          <div class="sql-preview">
            <code>{{ query.querySql.substring(0, 200) }}{{ query.querySql.length > 200 ? '...' : '' }}</code>
          </div>

          <!-- 컬럼 매핑 정보 -->
          <div class="column-info">
            <span class="info-item">
              <span class="info-label">Value:</span>
              <span class="info-value">{{ query.valueColumn }}</span>
            </span>
            <span class="info-item">
              <span class="info-label">Label:</span>
              <span class="info-value">{{ query.labelColumn }}</span>
            </span>
            <span v-if="query.colorColumn" class="info-item">
              <span class="info-label">Color:</span>
              <span class="info-value">{{ query.colorColumn }}</span>
            </span>
            <span class="info-item">
              <span class="info-label">{{ getCacheText(query) }}</span>
            </span>
          </div>

          <!-- 실행 통계 -->
          <div v-if="query.lastExecutedAt" class="execution-stats">
            <span class="text-xs text-gray-400">
              마지막 실행: {{ new Date(query.lastExecutedAt).toLocaleString('ko-KR') }}
              ({{ query.lastResultCount }}건)
            </span>
          </div>
        </div>

        <div class="card-footer">
          <div class="footer-meta">
            <span v-if="query.ownerName" class="text-xs text-gray-400">
              소유자: {{ query.ownerName }}
            </span>
          </div>
          <div class="footer-actions">
            <button
              v-if="isManager"
              type="button"
              class="action-btn test"
              @click="handleTest(query)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              테스트
            </button>
            <button
              type="button"
              class="action-btn execute"
              :disabled="executingId === query.queryId"
              @click="handleExecute(query)"
            >
              <svg v-if="executingId === query.queryId" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              실행
            </button>
            <button type="button" class="action-btn edit" @click="handleEdit(query)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              수정
            </button>
            <button type="button" class="action-btn delete" @click="handleDelete(query)">
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
          d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">
        {{ filterKeyword ? '검색 결과가 없습니다.' : '등록된 쿼리가 없습니다.' }}
      </p>
      <button
        v-if="!filterKeyword && canCreateInCurrentTab"
        type="button"
        class="mt-3 text-sm text-primary-600 hover:text-primary-700"
        @click="handleAdd"
      >
        + 첫 번째 쿼리 추가하기
      </button>
    </div>

    <!-- 쿼리 추가/수정 모달 -->
    <ExternalQueryModal
      v-if="showModal"
      :query="editingQuery"
      :owner-type="modalOwnerType"
      @close="handleModalClose"
      @saved="handleModalSaved"
    />

    <!-- 쿼리 테스트 모달 -->
    <QueryTestModal
      v-if="showTestModal && testingQuery"
      :query="testingQuery"
      @close="handleTestModalClose"
    />
  </div>
</template>

<style scoped>
.query-content {
  @apply w-full;
}

.tab-nav {
  @apply flex gap-1 bg-gray-100 p-1 rounded-lg;
}

.tab-btn {
  @apply px-4 py-2 text-sm font-medium rounded-md
         text-gray-600 hover:text-gray-900 transition-colors;
}

.tab-btn.active {
  @apply bg-white text-gray-900 shadow-sm;
}

.tab-count {
  @apply ml-1.5 px-1.5 py-0.5 text-xs rounded-full bg-gray-200 text-gray-600;
}

.tab-btn.active .tab-count {
  @apply bg-primary-100 text-primary-600;
}

.search-box {
  @apply relative;
}

.search-icon {
  @apply absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400;
}

.search-input {
  @apply pl-9 pr-4 py-2 text-sm border border-gray-300 rounded-lg w-48
         focus:ring-primary-500 focus:border-primary-500;
}

.btn-primary {
  @apply inline-flex items-center gap-2 px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}

.loading-state {
  @apply py-12 text-center;
}

.query-list {
  @apply space-y-4;
}

.query-card {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden
         hover:shadow-md transition-shadow;
}

.card-header {
  @apply flex items-center justify-between p-4 border-b border-gray-100;
}

.card-title {
  @apply text-base font-medium text-gray-900;
}

.card-meta {
  @apply flex items-center gap-2 mt-1 text-xs text-gray-500;
}

.query-code {
  @apply font-mono;
}

.separator {
  @apply text-gray-300;
}

.datasource-name {
  @apply text-primary-600;
}

.card-body {
  @apply p-4 space-y-3;
}

.sql-preview {
  @apply p-3 bg-gray-50 rounded-lg overflow-x-auto;
}

.sql-preview code {
  @apply text-xs text-gray-600 font-mono whitespace-pre-wrap break-all;
}

.column-info {
  @apply flex flex-wrap gap-4 text-xs;
}

.info-item {
  @apply flex items-center gap-1;
}

.info-label {
  @apply text-gray-500;
}

.info-value {
  @apply font-mono text-gray-700;
}

.execution-stats {
  @apply pt-2 border-t border-gray-100;
}

.card-footer {
  @apply flex items-center justify-between px-4 py-3 bg-gray-50 border-t border-gray-100;
}

.footer-meta {
  @apply text-sm text-gray-500;
}

.footer-actions {
  @apply flex items-center gap-2;
}

.action-btn {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md
         transition-colors duration-150;
}

.action-btn.test {
  @apply text-purple-600 bg-purple-50 hover:bg-purple-100;
}

.action-btn.execute {
  @apply text-primary-600 bg-primary-50 hover:bg-primary-100;
}

.action-btn.edit {
  @apply text-gray-600 bg-gray-100 hover:bg-gray-200;
}

.action-btn.delete {
  @apply text-red-600 bg-red-50 hover:bg-red-100;
}

.action-btn:disabled {
  @apply opacity-50 cursor-not-allowed;
}

.empty-state {
  @apply text-center py-12 bg-white rounded-lg border border-gray-200;
}
</style>
