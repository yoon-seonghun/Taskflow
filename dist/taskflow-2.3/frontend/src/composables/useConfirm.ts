/**
 * 확인 다이얼로그 composable
 */
import { useUiStore, type ConfirmOptions } from '@/stores/ui'

export function useConfirm() {
  const uiStore = useUiStore()

  async function confirm(options: ConfirmOptions): Promise<boolean> {
    return uiStore.confirm(options)
  }

  async function confirmDelete(itemName?: string): Promise<boolean> {
    return confirm({
      title: '삭제 확인',
      message: itemName
        ? `"${itemName}"을(를) 삭제하시겠습니까?`
        : '선택한 항목을 삭제하시겠습니까?',
      confirmText: '삭제',
      cancelText: '취소',
      confirmType: 'danger'
    })
  }

  async function confirmSave(message?: string): Promise<boolean> {
    return confirm({
      title: '저장 확인',
      message: message || '변경 사항을 저장하시겠습니까?',
      confirmText: '저장',
      cancelText: '취소',
      confirmType: 'primary'
    })
  }

  async function confirmDiscard(): Promise<boolean> {
    return confirm({
      title: '변경 사항 취소',
      message: '저장하지 않은 변경 사항이 있습니다. 정말 취소하시겠습니까?',
      confirmText: '취소하기',
      cancelText: '계속 편집',
      confirmType: 'danger'
    })
  }

  return {
    confirm,
    show: confirm, // 별칭
    confirmDelete,
    confirmSave,
    confirmDiscard
  }
}
