import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  type: ToastType
  message: string
  duration?: number
}

export interface ConfirmOptions {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  confirmType?: 'danger' | 'primary'
}

export const useUiStore = defineStore('ui', () => {
  // Toast 관련
  const toasts = ref<Toast[]>([])

  // Modal/Dialog 관련
  const confirmDialog = ref<{
    visible: boolean
    options: ConfirmOptions | null
    resolve: ((value: boolean) => void) | null
  }>({
    visible: false,
    options: null,
    resolve: null
  })

  // Slide-over Panel 관련
  const slideOverPanel = ref<{
    visible: boolean
    component: string | null
    props: Record<string, unknown>
  }>({
    visible: false,
    component: null,
    props: {}
  })

  // 전역 로딩 상태
  const globalLoading = ref(false)
  const loadingMessage = ref<string | null>(null)

  // 사이드바 상태
  const sidebarCollapsed = ref(false)

  // 모바일 메뉴 상태
  const mobileMenuOpen = ref(false)

  // Toast Functions
  function addToast(toast: Omit<Toast, 'id'>) {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    const newToast: Toast = { ...toast, id }
    toasts.value.push(newToast)

    // 자동 제거
    const duration = toast.duration ?? 3000
    if (duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }

    return id
  }

  function removeToast(id: string) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function clearToasts() {
    toasts.value = []
  }

  // 편의 함수
  function showSuccess(message: string, duration?: number) {
    return addToast({ type: 'success', message, duration })
  }

  function showError(message: string, duration?: number) {
    return addToast({ type: 'error', message, duration: duration ?? 5000 })
  }

  function showWarning(message: string, duration?: number) {
    return addToast({ type: 'warning', message, duration })
  }

  function showInfo(message: string, duration?: number) {
    return addToast({ type: 'info', message, duration })
  }

  // Confirm Dialog Functions
  function confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      confirmDialog.value = {
        visible: true,
        options,
        resolve
      }
    })
  }

  function resolveConfirm(result: boolean) {
    if (confirmDialog.value.resolve) {
      confirmDialog.value.resolve(result)
    }
    confirmDialog.value = {
      visible: false,
      options: null,
      resolve: null
    }
  }

  // Slide-over Panel Functions
  function openSlideOver(component: string, props: Record<string, unknown> = {}) {
    slideOverPanel.value = {
      visible: true,
      component,
      props
    }
  }

  function closeSlideOver() {
    slideOverPanel.value = {
      visible: false,
      component: null,
      props: {}
    }
  }

  // Loading Functions
  function setGlobalLoading(loading: boolean, message?: string) {
    globalLoading.value = loading
    loadingMessage.value = message ?? null
  }

  // Sidebar Functions
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  // Mobile Menu Functions
  function toggleMobileMenu() {
    mobileMenuOpen.value = !mobileMenuOpen.value
  }

  function closeMobileMenu() {
    mobileMenuOpen.value = false
  }

  return {
    // State
    toasts,
    confirmDialog,
    slideOverPanel,
    globalLoading,
    loadingMessage,
    sidebarCollapsed,
    mobileMenuOpen,
    // Toast
    addToast,
    removeToast,
    clearToasts,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    // Confirm
    confirm,
    resolveConfirm,
    // Slide-over
    openSlideOver,
    closeSlideOver,
    // Loading
    setGlobalLoading,
    // Sidebar
    toggleSidebar,
    setSidebarCollapsed,
    // Mobile Menu
    toggleMobileMenu,
    closeMobileMenu
  }
})
