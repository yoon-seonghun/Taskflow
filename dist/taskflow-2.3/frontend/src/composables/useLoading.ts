/**
 * 로딩 상태 관리 composable
 */
import { ref } from 'vue'
import { useUiStore } from '@/stores/ui'

export function useLoading() {
  const uiStore = useUiStore()
  const localLoading = ref(false)

  function startLoading() {
    localLoading.value = true
  }

  function stopLoading() {
    localLoading.value = false
  }

  function setGlobalLoading(loading: boolean, message?: string) {
    uiStore.setGlobalLoading(loading, message)
  }

  async function withLoading<T>(
    fn: () => Promise<T>,
    options?: { global?: boolean; message?: string }
  ): Promise<T> {
    const { global = false, message } = options || {}

    if (global) {
      setGlobalLoading(true, message)
    } else {
      startLoading()
    }

    try {
      return await fn()
    } finally {
      if (global) {
        setGlobalLoading(false)
      } else {
        stopLoading()
      }
    }
  }

  return {
    loading: localLoading,
    globalLoading: uiStore.globalLoading,
    loadingMessage: uiStore.loadingMessage,
    startLoading,
    stopLoading,
    setGlobalLoading,
    withLoading
  }
}
