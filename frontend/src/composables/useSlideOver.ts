/**
 * Slide-over 패널 관리 composable
 * - PC (768px 이상): 슬라이드오버 패널 사용
 * - 모바일 (768px 미만): 전체 화면 페이지로 라우팅
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUiStore } from '@/stores/ui'

// 반응형 브레이크포인트 (md = 768px)
const MD_BREAKPOINT = 768

function isMobile(): boolean {
  return window.innerWidth < MD_BREAKPOINT
}

export function useSlideOver() {
  const uiStore = useUiStore()
  const router = useRouter()

  const isOpen = computed(() => uiStore.slideOverPanel.visible)
  const currentComponent = computed(() => uiStore.slideOverPanel.component)
  const currentProps = computed(() => uiStore.slideOverPanel.props)

  function open(component: string, props: Record<string, unknown> = {}) {
    uiStore.openSlideOver(component, props)
  }

  function close() {
    uiStore.closeSlideOver()
  }

  // 아이템 상세 패널 열기
  function openItemDetail(itemId: number, boardId: number) {
    if (isMobile()) {
      // 모바일: 전체 화면 페이지로 라우팅
      router.push({
        name: 'ItemDetail',
        params: { boardId: String(boardId), itemId: String(itemId) }
      })
    } else {
      // PC: 슬라이드오버 패널 사용
      open('ItemDetailPanel', { itemId, boardId })
    }
  }

  // 아이템 편집 패널 열기
  function openItemEdit(itemId: number, boardId: number) {
    if (isMobile()) {
      // 모바일: 전체 화면 페이지로 라우팅
      router.push({
        name: 'ItemDetail',
        params: { boardId: String(boardId), itemId: String(itemId) }
      })
    } else {
      // PC: 슬라이드오버 패널 사용
      open('ItemDetailPanel', { itemId, boardId })
    }
  }

  // 신규 아이템 패널 열기
  function openItemCreate(boardId: number, groupId?: number) {
    // 신규 생성은 항상 슬라이드오버 사용 (모바일에서도)
    open('ItemCreate', { boardId, groupId })
  }

  return {
    isOpen,
    currentComponent,
    currentProps,
    open,
    close,
    openItemDetail,
    openItemEdit,
    openItemCreate
  }
}
