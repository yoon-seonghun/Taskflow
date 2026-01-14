import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { externalQueryApi } from '@/api/externalQuery'
import type {
  ExternalQuery,
  ExternalQueryCreateRequest,
  ExternalQueryUpdateRequest,
  QueryTestRequest,
  QueryTestResponse,
  QueryExecuteResponse
} from '@/types/externalQuery'

export const useExternalQueryStore = defineStore('externalQuery', () => {
  // ==================== State ====================
  const queries = ref<ExternalQuery[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const cachedResults = ref<Map<number, QueryExecuteResponse>>(new Map())

  // ==================== Getters ====================
  const activeQueries = computed(() =>
    queries.value.filter(q => q.useYn === 'Y')
  )

  const getQueryById = computed(() => {
    return (id: number) => queries.value.find(q => q.queryId === id)
  })

  const getQueryByCode = computed(() => {
    return (code: string) => queries.value.find(q => q.queryCode === code)
  })

  const globalQueries = computed(() =>
    queries.value.filter(q => q.ownerType === 'GLOBAL')
  )

  const managerQueries = computed(() =>
    queries.value.filter(q => q.ownerType === 'MANAGER')
  )

  const userQueries = computed(() =>
    queries.value.filter(q => q.ownerType === 'USER')
  )

  const queriesByDatasource = computed(() => {
    const grouped: Record<number, ExternalQuery[]> = {}
    queries.value.forEach(q => {
      if (!grouped[q.datasourceId]) {
        grouped[q.datasourceId] = []
      }
      grouped[q.datasourceId].push(q)
    })
    return grouped
  })

  // ==================== Actions ====================

  // 글로벌 쿼리 조회
  async function fetchGlobalQueries() {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.getGlobalQueries()
      // 기존 데이터 병합 또는 교체
      const newQueries = response.data || []
      newQueries.forEach(nq => {
        const index = queries.value.findIndex(q => q.queryId === nq.queryId)
        if (index !== -1) {
          queries.value[index] = nq
        } else {
          queries.value.push(nq)
        }
      })
    } catch (e) {
      error.value = e instanceof Error ? e.message : '글로벌 쿼리 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 매니저 쿼리 조회
  async function fetchManagerQueries() {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.getManagerQueries()
      const newQueries = response.data || []
      newQueries.forEach(nq => {
        const index = queries.value.findIndex(q => q.queryId === nq.queryId)
        if (index !== -1) {
          queries.value[index] = nq
        } else {
          queries.value.push(nq)
        }
      })
    } catch (e) {
      error.value = e instanceof Error ? e.message : '매니저 쿼리 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 사용자 쿼리 조회
  async function fetchUserQueries() {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.getUserQueries()
      const newQueries = response.data || []
      newQueries.forEach(nq => {
        const index = queries.value.findIndex(q => q.queryId === nq.queryId)
        if (index !== -1) {
          queries.value[index] = nq
        } else {
          queries.value.push(nq)
        }
      })
    } catch (e) {
      error.value = e instanceof Error ? e.message : '사용자 쿼리 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 접근 가능한 전체 쿼리 조회
  async function fetchAccessibleQueries() {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.getAccessibleQueries()
      queries.value = response.data || []
    } catch (e) {
      error.value = e instanceof Error ? e.message : '쿼리 목록 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 글로벌 쿼리 생성
  async function createGlobalQuery(data: ExternalQueryCreateRequest): Promise<number> {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.createGlobalQuery(data)
      await fetchAccessibleQueries()
      return response.data as number
    } catch (e) {
      error.value = e instanceof Error ? e.message : '글로벌 쿼리 생성 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 매니저 쿼리 생성
  async function createManagerQuery(data: ExternalQueryCreateRequest): Promise<number> {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.createManagerQuery(data)
      await fetchAccessibleQueries()
      return response.data as number
    } catch (e) {
      error.value = e instanceof Error ? e.message : '매니저 쿼리 생성 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 사용자 쿼리 생성
  async function createUserQuery(data: ExternalQueryCreateRequest): Promise<number> {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.createUserQuery(data)
      await fetchAccessibleQueries()
      return response.data as number
    } catch (e) {
      error.value = e instanceof Error ? e.message : '사용자 쿼리 생성 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 쿼리 수정
  async function updateQuery(id: number, data: ExternalQueryUpdateRequest) {
    loading.value = true
    error.value = null
    try {
      await externalQueryApi.updateQuery(id, data)
      const index = queries.value.findIndex(q => q.queryId === id)
      if (index !== -1) {
        queries.value[index] = { ...queries.value[index], ...data }
      }
      // 캐시 무효화
      cachedResults.value.delete(id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '쿼리 수정 실패'
      await fetchAccessibleQueries()
      throw e
    } finally {
      loading.value = false
    }
  }

  // 쿼리 삭제
  async function deleteQuery(id: number) {
    loading.value = true
    error.value = null
    try {
      await externalQueryApi.deleteQuery(id)
      queries.value = queries.value.filter(q => q.queryId !== id)
      cachedResults.value.delete(id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '쿼리 삭제 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 쿼리 테스트
  async function testQuery(data: QueryTestRequest): Promise<QueryTestResponse> {
    loading.value = true
    error.value = null
    try {
      const response = await externalQueryApi.testQuery(data)
      return response.data as QueryTestResponse
    } catch (e) {
      error.value = e instanceof Error ? e.message : '쿼리 테스트 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 쿼리 실행
  async function executeQuery(id: number, useCache = true): Promise<QueryExecuteResponse> {
    // 캐시 확인
    if (useCache && cachedResults.value.has(id)) {
      return cachedResults.value.get(id)!
    }

    loading.value = true
    error.value = null
    try {
      const response = useCache
        ? await externalQueryApi.executeQuery(id)
        : await externalQueryApi.executeQueryNoCache(id)

      const result = response.data as QueryExecuteResponse
      if (result.success) {
        cachedResults.value.set(id, result)
      }
      return result
    } catch (e) {
      error.value = e instanceof Error ? e.message : '쿼리 실행 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  // 캐시 무효화
  async function evictCache(id: number) {
    try {
      await externalQueryApi.evictCache(id)
      cachedResults.value.delete(id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '캐시 무효화 실패'
      throw e
    }
  }

  function reset() {
    queries.value = []
    loading.value = false
    error.value = null
    cachedResults.value.clear()
  }

  return {
    // State
    queries,
    loading,
    error,
    cachedResults,
    // Getters
    activeQueries,
    getQueryById,
    getQueryByCode,
    globalQueries,
    managerQueries,
    userQueries,
    queriesByDatasource,
    // Actions
    fetchGlobalQueries,
    fetchManagerQueries,
    fetchUserQueries,
    fetchAccessibleQueries,
    createGlobalQuery,
    createManagerQuery,
    createUserQuery,
    updateQuery,
    deleteQuery,
    testQuery,
    executeQuery,
    evictCache,
    reset
  }
})
