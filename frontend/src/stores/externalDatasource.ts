import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { externalDatasourceApi } from '@/api/externalDatasource'
import type {
  ExternalDatasource,
  ExternalDatasourceCreateRequest,
  ExternalDatasourceUpdateRequest,
  ConnectionTestResponse
} from '@/types/externalDatasource'

export const useExternalDatasourceStore = defineStore('externalDatasource', () => {
  // ==================== State ====================
  const datasources = ref<ExternalDatasource[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // ==================== Getters ====================
  const activeDatasources = computed(() =>
    datasources.value.filter(d => d.useYn === 'Y')
  )

  const getDatasourceById = computed(() => {
    return (id: number) => datasources.value.find(d => d.datasourceId === id)
  })

  const getDatasourceByCode = computed(() => {
    return (code: string) => datasources.value.find(d => d.datasourceCode === code)
  })

  const datasourcesByDbType = computed(() => {
    const grouped: Record<string, ExternalDatasource[]> = {}
    datasources.value.forEach(d => {
      if (!grouped[d.dbType]) {
        grouped[d.dbType] = []
      }
      grouped[d.dbType].push(d)
    })
    return grouped
  })

  // ==================== Actions ====================
  async function fetchDatasources(params?: { dbType?: string; keyword?: string }) {
    loading.value = true
    error.value = null
    try {
      const response = await externalDatasourceApi.getDatasources(params)
      datasources.value = response.data || []
    } catch (e) {
      error.value = e instanceof Error ? e.message : '데이터소스 목록 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchActiveDatasources() {
    loading.value = true
    error.value = null
    try {
      const response = await externalDatasourceApi.getActiveDatasources()
      datasources.value = response.data || []
    } catch (e) {
      error.value = e instanceof Error ? e.message : '활성 데이터소스 목록 조회 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createDatasource(data: ExternalDatasourceCreateRequest): Promise<number> {
    loading.value = true
    error.value = null
    try {
      const response = await externalDatasourceApi.createDatasource(data)
      await fetchDatasources()
      return response.data as number
    } catch (e) {
      error.value = e instanceof Error ? e.message : '데이터소스 생성 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateDatasource(id: number, data: ExternalDatasourceUpdateRequest) {
    loading.value = true
    error.value = null
    try {
      await externalDatasourceApi.updateDatasource(id, data)
      // Optimistic update
      const index = datasources.value.findIndex(d => d.datasourceId === id)
      if (index !== -1) {
        datasources.value[index] = { ...datasources.value[index], ...data }
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : '데이터소스 수정 실패'
      await fetchDatasources() // Rollback
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteDatasource(id: number) {
    loading.value = true
    error.value = null
    try {
      await externalDatasourceApi.deleteDatasource(id)
      datasources.value = datasources.value.filter(d => d.datasourceId !== id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '데이터소스 삭제 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function testConnection(id: number): Promise<ConnectionTestResponse> {
    loading.value = true
    error.value = null
    try {
      const response = await externalDatasourceApi.testConnection(id)
      return response.data as ConnectionTestResponse
    } catch (e) {
      error.value = e instanceof Error ? e.message : '연결 테스트 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function testNewConnection(data: ExternalDatasourceCreateRequest): Promise<ConnectionTestResponse> {
    loading.value = true
    error.value = null
    try {
      const response = await externalDatasourceApi.testNewConnection(data)
      return response.data as ConnectionTestResponse
    } catch (e) {
      error.value = e instanceof Error ? e.message : '연결 테스트 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function reinitializePool(id: number) {
    loading.value = true
    error.value = null
    try {
      await externalDatasourceApi.reinitializePool(id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '풀 재초기화 실패'
      throw e
    } finally {
      loading.value = false
    }
  }

  function reset() {
    datasources.value = []
    loading.value = false
    error.value = null
  }

  return {
    // State
    datasources,
    loading,
    error,
    // Getters
    activeDatasources,
    getDatasourceById,
    getDatasourceByCode,
    datasourcesByDbType,
    // Actions
    fetchDatasources,
    fetchActiveDatasources,
    createDatasource,
    updateDatasource,
    deleteDatasource,
    testConnection,
    testNewConnection,
    reinitializePool,
    reset
  }
})
