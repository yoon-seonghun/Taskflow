/**
 * 전역 드롭다운 상태 관리 Composable
 * - 현재 열린 드롭다운 ID 추적
 * - 새 드롭다운 열릴 때 기존 드롭다운 닫기
 * - 외부 클릭 시 드롭다운 닫기
 * - 드롭다운/캘린더 겹침 방지
 */
import { ref, onMounted, onUnmounted, type Ref } from 'vue'

// 전역 상태 (모듈 스코프)
const registeredDropdowns = new Map<string, {
  close: () => void
  containerRef: Ref<HTMLElement | null>
}>()
let activeDropdownId = ref<string | null>(null)
let idCounter = 0
let globalClickHandlerRegistered = false

/**
 * 전역 클릭 핸들러 - 드롭다운 외부 클릭 시 닫기
 */
function handleGlobalClick(event: MouseEvent) {
  const target = event.target as Node

  // 열린 드롭다운이 없으면 무시
  if (!activeDropdownId.value) return

  // 현재 열린 드롭다운 찾기
  const activeDropdown = registeredDropdowns.get(activeDropdownId.value)
  if (!activeDropdown) return

  // 클릭이 드롭다운 컨테이너 외부인지 확인
  const container = activeDropdown.containerRef.value
  if (container && !container.contains(target)) {
    activeDropdown.close()
    activeDropdownId.value = null
  }
}

/**
 * 전역 클릭 핸들러 등록 (최초 1회만)
 */
function ensureGlobalClickHandler() {
  if (!globalClickHandlerRegistered) {
    document.addEventListener('click', handleGlobalClick, true)
    globalClickHandlerRegistered = true
  }
}

/**
 * 드롭다운 매니저 사용 (컴포넌트에서 호출)
 * - 고유 ID 자동 생성
 * - 자동 등록/해제
 * - 열기/닫기 함수 제공
 * - 외부 클릭 감지
 */
export function useDropdownManager(containerRef: Ref<HTMLElement | null>) {
  // 고유 ID 생성
  const dropdownId = `dropdown-${++idCounter}`

  // 로컬 상태
  const isOpen = ref(false)

  // 닫기 함수
  const closeDropdown = () => {
    isOpen.value = false
    if (activeDropdownId.value === dropdownId) {
      activeDropdownId.value = null
    }
  }

  // 등록
  registeredDropdowns.set(dropdownId, {
    close: closeDropdown,
    containerRef
  })

  // 전역 클릭 핸들러 등록
  onMounted(() => {
    ensureGlobalClickHandler()
  })

  // 컴포넌트 해제 시 등록 해제
  onUnmounted(() => {
    registeredDropdowns.delete(dropdownId)
    if (activeDropdownId.value === dropdownId) {
      activeDropdownId.value = null
    }
  })

  // 열기 함수 (다른 드롭다운 닫기)
  const openDropdown = () => {
    // 다른 모든 드롭다운 닫기
    registeredDropdowns.forEach((dropdown, id) => {
      if (id !== dropdownId) {
        dropdown.close()
      }
    })
    activeDropdownId.value = dropdownId
    isOpen.value = true
  }

  // 토글 함수
  const toggleDropdown = () => {
    if (isOpen.value) {
      closeDropdown()
    } else {
      openDropdown()
    }
  }

  return {
    dropdownId,
    isOpen,
    openDropdown,
    closeDropdown,
    toggleDropdown
  }
}
