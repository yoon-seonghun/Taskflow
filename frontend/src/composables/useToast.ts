/**
 * Toast 알림 composable
 */
import { useUiStore } from '@/stores/ui'

export function useToast() {
  const uiStore = useUiStore()

  function success(message: string, duration?: number) {
    return uiStore.showSuccess(message, duration)
  }

  function error(message: string, duration?: number) {
    return uiStore.showError(message, duration)
  }

  function warning(message: string, duration?: number) {
    return uiStore.showWarning(message, duration)
  }

  function info(message: string, duration?: number) {
    return uiStore.showInfo(message, duration)
  }

  function remove(id: string) {
    uiStore.removeToast(id)
  }

  function clear() {
    uiStore.clearToasts()
  }

  return {
    success,
    error,
    warning,
    info,
    remove,
    clear
  }
}
