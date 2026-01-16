<script setup lang="ts">
/**
 * 아이템 상세/편집 패널 컴포넌트
 * - PC: 3컬럼 레이아웃 (속성 | 리치 텍스트 에디터 | 댓글)
 * - 각 컬럼 사이에 리사이즈 핸들로 폭 조절 가능
 * - Mobile: 탭 전환
 * - v2.1: 설명(description)과 내용(content) 분리
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useItemStore } from '@/stores/item'
import { useBoardStore } from '@/stores/board'
import { usePropertyStore } from '@/stores/property'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { useConfirm } from '@/composables/useConfirm'
import { boardApi } from '@/api/board'
import { itemApi } from '@/api/item'
import { itemContentApi, type ItemContentResponse } from '@/api/itemContent'
import { Button, Spinner, EntityEditModal } from '@/components/common'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import { FileAttachment } from '@/components/file'
import ItemForm from './ItemForm.vue'
import ItemTransferModal from './ItemTransferModal.vue'
import ItemShareModal from './ItemShareModal.vue'
import AssignConfirmModal from './AssignConfirmModal.vue'
// v2.2: 하위 업무 관련 컴포넌트
import SubTaskList from './SubTaskList.vue'
import ItemBreadcrumb from './ItemBreadcrumb.vue'
import IncompleteChildrenModal from './IncompleteChildrenModal.vue'
import { useItemPermission } from '@/composables/useItemPermission'
import { userApi } from '@/api/user'
import { CommentList } from '@/components/comment'
import type { Item, ItemUpdateRequest, IncompleteChildrenResponse, AncestorResponse } from '@/types/item'
import type { Category } from '@/types/category'

interface Props {
  itemId: number
  boardId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updated', item: Item): void
  (e: 'deleted', itemId: number): void
  (e: 'navigate', itemId: number): void  // v2.2: Breadcrumb 네비게이션
}>()

const itemStore = useItemStore()
const boardStore = useBoardStore()
const propertyStore = usePropertyStore()
const authStore = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

// v2.1: 업무 권한 관리
const boardIdRef = computed(() => props.boardId)
const itemIdRef = computed(() => props.itemId)
const { canAssign, isOwner, permissionLevel, isAssigned, isShared, getPermissionLabel, fetchAccessInfo } = useItemPermission(boardIdRef, itemIdRef)

// 공유/배당 권한 배지 표시 여부
const showPermissionBadge = computed(() => {
  // 소유자가 아니고, 공유 또는 배당 받은 업무인 경우에만 표시
  return !isOwner.value && (isAssigned.value || isShared.value || item.value?.isSharedToMe || item.value?.isAssignedToMe)
})

// 권한 배지 텍스트
const permissionBadgeText = computed(() => {
  const shareTypeText = isAssigned.value || item.value?.isAssignedToMe ? '배당' : '공유'
  const permText = getPermissionLabel() || '조회'
  return `${shareTypeText} · ${permText}`
})

// 권한 배지 색상 클래스
const permissionBadgeClass = computed(() => {
  const level = permissionLevel.value
  if (level === 'FULL') return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
  if (level === 'EDIT') return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
  return 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400'  // VIEW 또는 기본
})

// 상태
const item = ref<Item | null>(null)
const isLoading = ref(false)
const isSaving = ref(false)
const hasChanges = ref(false)
const activeTab = ref<'detail' | 'content' | 'comments'>('detail')
const commentListRef = ref<InstanceType<typeof CommentList> | null>(null)

// v2.1: 설명(description)과 내용(content) 분리
const descriptionText = ref('')  // TB_ITEM.DESCRIPTION (단일 라인 요약)

// v2.1: 리치 텍스트 에디터 상태 (TB_ITEM_CONTENT)
const richTextContent = ref('')  // 리치 텍스트 HTML 내용
const contentVersion = ref(0)    // 동시 편집 충돌 방지용 버전
const isContentSaving = ref(false)
const contentLastSavedAt = ref<Date | null>(null)
const hasUnsavedContent = ref(false)
let autoSaveTimeout: ReturnType<typeof setTimeout> | null = null

// 모달 상태
const showTransferModal = ref(false)
const showShareModal = ref(false)
const showPropertyModal = ref(false)  // v2.0: 속성 선택 모달
const showAssignModal = ref(false)    // v2.1: 담당자 배정 모달
const showIncompleteChildrenModal = ref(false)  // v2.2: 미완료 하위 업무 모달
const canTransfer = ref(false)
const canShare = ref(false)

// v2.2: 하위 업무 관련 상태
const incompleteChildren = ref<IncompleteChildrenResponse | null>(null)
const subTaskListExpanded = ref(false)

// v2.1: 담당자 배정 관련 상태
const pendingAssignee = ref<{ username: string; name: string } | null>(null)
const originalAssignee = ref<string | undefined>(undefined)

// v2.0: 속성 선택 모달용 상태
const boardCategories = ref<Category[]>([])
const selectedPropertyIds = ref<number[]>([])
const selectedPropertyDefaults = ref<Record<number, string>>({})
const selectedPropertySortOrders = ref<Record<number, number>>({})  // v2.0.1: 속성 정렬 순서

// 컬럼 폭 상태 (리사이즈용)
const PROP_WIDTH_KEY = 'taskflow_prop_width'
const COMMENT_WIDTH_KEY = 'taskflow_comment_width'
const propWidth = ref(240)  // 속성 패널 폭
const commentWidth = ref(280)  // 댓글 패널 폭
const isResizingProp = ref(false)
const isResizingComment = ref(false)
const containerRef = ref<HTMLDivElement | null>(null)

// 저장된 폭 불러오기
function loadColumnWidths() {
  const savedProp = localStorage.getItem(PROP_WIDTH_KEY)
  const savedComment = localStorage.getItem(COMMENT_WIDTH_KEY)
  if (savedProp) propWidth.value = Math.max(180, Math.min(400, parseInt(savedProp, 10)))
  if (savedComment) commentWidth.value = Math.max(200, Math.min(450, parseInt(savedComment, 10)))
}

// 폭 저장
function saveColumnWidths() {
  localStorage.setItem(PROP_WIDTH_KEY, propWidth.value.toString())
  localStorage.setItem(COMMENT_WIDTH_KEY, commentWidth.value.toString())
}

// 속성 패널 리사이즈
function startResizeProp(event: MouseEvent) {
  event.preventDefault()
  isResizingProp.value = true
  document.body.style.cursor = 'ew-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleResizeProp)
  document.addEventListener('mouseup', stopResizeProp)
}

function handleResizeProp(event: MouseEvent) {
  if (!isResizingProp.value || !containerRef.value) return
  const containerRect = containerRef.value.getBoundingClientRect()
  const newWidth = event.clientX - containerRect.left
  propWidth.value = Math.max(180, Math.min(400, newWidth))
}

function stopResizeProp() {
  isResizingProp.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  saveColumnWidths()
  document.removeEventListener('mousemove', handleResizeProp)
  document.removeEventListener('mouseup', stopResizeProp)
}

// 댓글 패널 리사이즈
function startResizeComment(event: MouseEvent) {
  event.preventDefault()
  isResizingComment.value = true
  document.body.style.cursor = 'ew-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleResizeComment)
  document.addEventListener('mouseup', stopResizeComment)
}

function handleResizeComment(event: MouseEvent) {
  if (!isResizingComment.value || !containerRef.value) return
  const containerRect = containerRef.value.getBoundingClientRect()
  const newWidth = containerRect.right - event.clientX
  commentWidth.value = Math.max(200, Math.min(450, newWidth))
}

function stopResizeComment() {
  isResizingComment.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  saveColumnWidths()
  document.removeEventListener('mousemove', handleResizeComment)
  document.removeEventListener('mouseup', stopResizeComment)
}

// 수정 정보
const modifiedInfo = computed(() => {
  if (!item.value) return null
  return {
    name: item.value.updatedByName || item.value.createdByName || '알 수 없음',
    time: formatDateTime(item.value.updatedAt || item.value.createdAt)
  }
})

// 댓글 수
const commentCount = computed(() => {
  return item.value?.commentCount || 0
})

// 아이템 로드
async function loadItem() {
  console.log('[ItemDetailPanel] loadItem called, props:', { itemId: props.itemId, boardId: props.boardId })
  if (!props.itemId || !props.boardId) {
    console.log('[ItemDetailPanel] loadItem early return - missing props')
    return
  }

  isLoading.value = true
  try {
    const result = await itemStore.fetchItem(props.boardId, props.itemId)
    if (result) {
      item.value = result
      descriptionText.value = result.description || ''  // v2.1: 설명 필드
      itemStore.startEditing(props.itemId, result)

      // v2.1: 업무 내용(content) 별도 로드
      await loadItemContent()

      // v2.0: 현재 아이템의 속성 정보 초기화 (모달용)
      initializePropertySelection(result)

      // 권한 확인
      checkPermissions()
    } else {
      toast.error('업무를 불러오는데 실패했습니다.')
      emit('close')
    }
  } catch (error) {
    toast.error('업무를 불러오는데 실패했습니다.')
    emit('close')
  } finally {
    isLoading.value = false
  }
}

// v2.1: 업무 내용(content) 로드
async function loadItemContent() {
  if (!props.boardId || !props.itemId) return

  try {
    const response = await itemContentApi.getContent(props.boardId, props.itemId)
    if (response.success && response.data) {
      richTextContent.value = response.data.content || ''
      contentVersion.value = response.data.version || 0
      contentLastSavedAt.value = response.data.updatedAt ? new Date(response.data.updatedAt) : null
    } else {
      // 내용이 없는 경우
      richTextContent.value = ''
      contentVersion.value = 0
      contentLastSavedAt.value = null
    }
    hasUnsavedContent.value = false
  } catch (error) {
    console.error('[loadItemContent] Failed to load content:', error)
    richTextContent.value = ''
    contentVersion.value = 0
  }
}

// v2.0: 아이템의 현재 속성 정보를 모달 초기값으로 설정
// CONCEPT_NOTE 원칙: 모달 초기값 = TB_ITEM_PROPERTY에 저장된 속성만 (보드 전체 속성 X)
// v2.0.2: sortOrder를 배열 인덱스 기반으로 할당 (DB 순서 유지)
function initializePropertySelection(itemData: Item) {
  // 1. 아이템에 실제 저장된 속성 ID 목록 (TB_ITEM_PROPERTY 기준)
  const itemPropertyIds: number[] = []

  // 2. 아이템에 저장된 속성 값 (TB_ITEM_PROPERTY)
  const defaults: Record<number, string> = {}

  // v2.0.2: 1 기반 sortOrder 사용 (DB 쿼리 결과 순서 반영, 일관성 유지)
  // DB에서 sortOrder가 0이어도, 배열 순서 자체가 실제 표시 순서를 반영
  const sortOrders: Record<number, number> = {}

  // item.properties에서 속성 ID 및 값 추출 (TB_ITEM_PROPERTY 데이터)
  if (itemData.properties && itemData.properties.length > 0) {
    itemData.properties.forEach((prop, index) => {
      // 속성 ID 수집 (값 유무와 관계없이 TB_ITEM_PROPERTY에 레코드가 있으면 "선택됨")
      itemPropertyIds.push(prop.propertyId)
      // 값 추출
      if (prop.value !== null && prop.value !== undefined) {
        defaults[prop.propertyId] = String(prop.value)
      }
      // v2.0.2: 1 기반 sortOrder 사용 (PropertiesContent.vue와 일관성 유지)
      // 이렇게 하면 모달에서 ensureSortOrdersForSelectedProperties가
      // 이미 sortOrder가 있는 속성은 재정렬하지 않음
      sortOrders[prop.propertyId] = index + 1  // 1 기반
    })
  }

  // propertyValues에서도 값 추출 (대안 - properties에 없는 경우)
  if (itemData.propertyValues) {
    let nextIndex = itemPropertyIds.length + 1  // 1 기반: 기존 속성 개수 + 1
    for (const [propId, value] of Object.entries(itemData.propertyValues)) {
      const propertyId = Number(propId)
      // propertyValues에만 있고 properties에 없는 경우 추가
      if (!itemPropertyIds.includes(propertyId)) {
        itemPropertyIds.push(propertyId)
        // propertyValues에서 추가된 속성은 기존 속성 뒤에 배치
        sortOrders[propertyId] = nextIndex++
      }
      if (value !== null && value !== undefined) {
        defaults[propertyId] = String(value)
      }
    }
  }

  console.log('[initializePropertySelection] item.properties 순서:', itemData.properties?.map(p => p.propertyId))
  console.log('[initializePropertySelection] 할당된 sortOrders:', sortOrders)

  // 3. 선택된 속성 ID = 아이템에 실제 저장된 속성만 (TB_ITEM_PROPERTY 기준)
  selectedPropertyIds.value = itemPropertyIds
  selectedPropertyDefaults.value = defaults
  selectedPropertySortOrders.value = sortOrders
}

// 이관/공유 권한 확인
async function checkPermissions() {
  try {
    canTransfer.value = await itemStore.canTransferItem(props.boardId, props.itemId)
    canShare.value = await itemStore.canShareItem(props.boardId, props.itemId)
  } catch {
    canTransfer.value = false
    canShare.value = false
  }
}

// 이관 완료 핸들러
function handleTransferred(transferredItem: Item) {
  emit('deleted', props.itemId) // 현재 보드에서 제거
  emit('close')
}

// 공유 업데이트 핸들러 (v2.2.1: 공유 수 배지 업데이트)
async function handleShareUpdated() {
  // 아이템 새로고침하여 shareCount 업데이트
  await loadItem()
}

// v2.1: 담당자 배정 완료 핸들러
async function handleAssigned(response: any) {
  showAssignModal.value = false
  pendingAssignee.value = null

  // 아이템 새로고침
  await loadItem()
  if (item.value) {
    emit('updated', item.value)
  }
  toast.success('담당자가 배정되었습니다.')
}

// v2.1: 담당자 배정 모달 닫기 (취소)
function handleAssignModalClose() {
  showAssignModal.value = false
  // 원래 담당자로 복원 (모달을 취소한 경우)
  if (item.value && pendingAssignee.value) {
    item.value.assigneeUsername = originalAssignee.value
  }
  pendingAssignee.value = null
}

// v2.1: 담당자 변경 감지 및 배정 모달 표시
async function checkAndShowAssignModal(newAssignee: string | undefined) {
  console.log('[checkAndShowAssignModal] called with:', newAssignee)
  if (!item.value || !newAssignee) {
    console.log('[checkAndShowAssignModal] early return - no item or newAssignee')
    return false
  }

  // 소유자 정보 확인 (현재 로그인 사용자가 소유자인지)
  const currentUser = authStore.user
  console.log('[checkAndShowAssignModal] currentUser:', currentUser?.username)
  if (!currentUser) {
    console.log('[checkAndShowAssignModal] no currentUser')
    return false
  }

  // 자기 자신에게 배정하는 경우는 모달 표시 안함 (기본 속성 변경만)
  if (newAssignee === currentUser.username) {
    console.log('[checkAndShowAssignModal] assigning to self - skip modal')
    return false
  }

  // 배정 권한 확인 (소유자 또는 FULL 권한 공유자만 배정 가능)
  // access-info API 실패 시 createdBy 기반으로 fallback
  const isItemOwner = item.value.createdBy === currentUser.username
  const hasAssignPermission = canAssign.value || isItemOwner
  console.log('[checkAndShowAssignModal] canAssign:', canAssign.value, 'isOwner:', isOwner.value, 'isItemOwner:', isItemOwner, 'hasAssignPermission:', hasAssignPermission)
  if (!hasAssignPermission) {
    console.log('[checkAndShowAssignModal] no assign permission - skip modal')
    return false
  }

  // 담당자 이름 조회
  try {
    console.log('[checkAndShowAssignModal] fetching user info for:', newAssignee)
    const response = await userApi.getUserByUsername(newAssignee)
    console.log('[checkAndShowAssignModal] userApi response:', response)
    if (response.success && response.data) {
      pendingAssignee.value = {
        username: newAssignee,
        name: response.data.name || response.data.userName || newAssignee
      }
      showAssignModal.value = true
      console.log('[checkAndShowAssignModal] showing modal for:', pendingAssignee.value)
      return true
    } else {
      console.log('[checkAndShowAssignModal] API failed or no data')
    }
  } catch (error) {
    console.error('[checkAndShowAssignModal] 사용자 정보 조회 실패:', error)
  }

  return false
}

// v2.0: 보드 카테고리 로드
async function loadBoardCategories() {
  if (!props.boardId) {
    boardCategories.value = []
    return
  }
  try {
    const response = await boardApi.getBoardCategories(props.boardId)
    if (response.success && response.data) {
      // BoardCategory를 Category 형태로 변환
      // 외부 연동 키 정책: ownerId 대신 ownerUsername 사용
      boardCategories.value = response.data.map(bc => ({
        categoryId: bc.categoryId,
        categoryCode: bc.categoryCode || '',
        categoryName: bc.categoryName,
        categoryColor: bc.categoryColor,
        description: bc.description || '',
        ownerType: bc.ownerType || 'USER',
        ownerUsername: bc.ownerUsername || '',
        sortOrder: bc.sortOrder || 0,
        useYn: 'Y'
      }))
    }
  } catch (error) {
    console.error('Failed to load board categories:', error)
  }
}

// v2.0: 속성 선택 모달에서 저장 핸들러
// v2.0.1: propertySortOrders 추가
async function handlePropertySave(data: {
  name: string
  description: string
  color: string
  categoryIds: number[]
  categoryId: number | null
  propertyIds: number[]
  propertyDefaults: Record<number, string>
  propertySortOrders: Record<number, number>
}) {
  if (!item.value || !props.boardId) return

  isSaving.value = true
  try {
    // 1. 기존 아이템에 저장된 속성 ID 목록
    const existingPropertyIds = new Set<number>(
      item.value.properties?.map(p => p.propertyId) || []
    )

    // 2. 새로 선택된 속성 ID 목록
    const newPropertyIds = new Set<number>(data.propertyIds)

    // DEBUG: 데이터 흐름 확인
    console.log('[handlePropertySave] item.value.properties:', item.value.properties)
    console.log('[handlePropertySave] existingPropertyIds:', Array.from(existingPropertyIds))
    console.log('[handlePropertySave] newPropertyIds (from modal):', Array.from(newPropertyIds))
    console.log('[handlePropertySave] propertySortOrders:', data.propertySortOrders)

    // 3. properties 맵 생성
    const properties: Record<number, unknown> = {}

    // 3-1. 선택된 속성: 값이 있으면 해당 값, 없으면 빈 문자열로 INSERT 요청
    // (빈 문자열이라도 TB_ITEM_PROPERTY에 레코드 생성하여 "선택됨" 상태 유지)
    for (const propId of data.propertyIds) {
      if (data.propertyDefaults[propId] !== undefined && data.propertyDefaults[propId] !== '') {
        properties[propId] = data.propertyDefaults[propId]
      } else {
        // 값이 없어도 빈 문자열로 설정하여 DB에 레코드 생성
        properties[propId] = ''
      }
    }

    // 3-2. 선택 해제된 속성: null로 설정하여 삭제 요청
    for (const existingPropId of existingPropertyIds) {
      if (!newPropertyIds.has(existingPropId)) {
        properties[existingPropId] = null
        console.log('[handlePropertySave] 삭제 대상 속성:', existingPropId)
      }
    }

    console.log('[handlePropertySave] 최종 properties 맵:', properties)

    // 제목, 설명, 카테고리 및 속성 업데이트 (sortOrder 포함)
    const updateData: ItemUpdateRequest = {
      title: data.name || undefined,
      description: data.description || undefined,
      categoryId: data.categoryId ?? undefined,
      properties: Object.keys(properties).length > 0 ? properties : undefined,
      propertySortOrders: Object.keys(data.propertySortOrders).length > 0 ? data.propertySortOrders : undefined
    }

    console.log('[handlePropertySave] API 전송 데이터:', updateData)

    const result = await itemStore.updateItem(props.boardId, item.value.itemId, updateData)
    console.log('[handlePropertySave] API 응답 result:', result)
    console.log('[handlePropertySave] result.properties:', result?.properties)
    if (result) {
      // v2.0.1: 속성 정렬 순서 별도 업데이트 (기존 속성에 대해서만)
      // 아이템 업데이트 후 반환된 properties에서 itemPropertyId를 가져와 sortOrder 업데이트
      if (Object.keys(data.propertySortOrders).length > 0 && result.properties && result.properties.length > 0) {
        try {
          // propertyId -> sortOrder 맵에서 itemPropertyId -> sortOrder 맵으로 변환
          const orders: Array<{ itemPropertyId: number; sortOrder: number }> = []
          for (const prop of result.properties) {
            const sortOrder = data.propertySortOrders[prop.propertyId]
            if (sortOrder !== undefined && prop.itemPropertyId) {
              orders.push({
                itemPropertyId: prop.itemPropertyId,
                sortOrder: sortOrder
              })
            }
          }

          if (orders.length > 0) {
            console.log('[handlePropertySave] 속성 순서 업데이트:', orders)
            await itemApi.updatePropertySortOrders(props.boardId, item.value.itemId, { orders })
          }
        } catch (sortError) {
          console.warn('[handlePropertySave] 속성 순서 업데이트 실패:', sortError)
          // 순서 업데이트 실패해도 속성 수정은 성공으로 처리
        }
      }

      // 아이템 다시 로드하여 최신 properties 반영 (API 응답에 완전한 properties 배열 포함)
      await loadItem()
      emit('updated', item.value!)
      toast.success('속성이 수정되었습니다.')
      showPropertyModal.value = false
    } else {
      toast.error('속성 수정에 실패했습니다.')
    }
  } catch (error) {
    toast.error('속성 수정에 실패했습니다.')
  } finally {
    isSaving.value = false
  }
}

// 아이템 업데이트 (속성 변경) - description은 별도 저장 (병합하지 않음)
async function handleUpdate(data: ItemUpdateRequest) {
  if (!item.value || !props.boardId) return

  // v2.1: 담당자 변경 감지 - 모달 표시 여부 결정
  if (data.assigneeUsername !== undefined &&
      data.assigneeUsername !== item.value.assigneeUsername) {
    // 원래 담당자 저장
    originalAssignee.value = item.value.assigneeUsername

    // 새 담당자로 먼저 UI 업데이트 (optimistic update)
    item.value.assigneeUsername = data.assigneeUsername

    // 배정 모달 표시 여부 확인
    const showModal = await checkAndShowAssignModal(data.assigneeUsername)
    if (showModal) {
      // 모달에서 처리하므로 여기서는 API 호출하지 않음
      return
    }
    // 모달을 표시하지 않는 경우 (자기 자신 등) 일반 업데이트 진행
  }

  hasChanges.value = true
  isSaving.value = true

  // description은 별도로 저장하므로 여기서 포함하지 않음
  const updateData = { ...data }
  itemStore.updateEditingData(updateData as Partial<Item>)

  try {
    const result = await itemStore.updateItem(props.boardId, item.value.itemId, updateData)
    if (result) {
      // v2.1: 현재 설명/내용 유지 (서버 응답으로 덮어쓰지 않음)
      const currentDescription = descriptionText.value
      const currentContent = richTextContent.value
      item.value = result
      // 설명이 변경되지 않았으면 현재 값 유지
      if (result.description !== currentDescription) {
        descriptionText.value = result.description || ''
      }
      // 리치 텍스트 내용은 별도 테이블이므로 현재 값 유지
      richTextContent.value = currentContent
      hasChanges.value = false
      emit('updated', result)
    } else {
      toast.error('저장에 실패했습니다.')
    }
  } catch (error) {
    toast.error('저장에 실패했습니다.')
  } finally {
    isSaving.value = false
  }
}

// v2.1: 설명(description) 저장 - 단일 라인 텍스트
async function handleDescriptionSave() {
  if (!item.value || !props.boardId) return
  if (descriptionText.value === item.value.description) return

  hasChanges.value = true
  isSaving.value = true

  try {
    const result = await itemStore.updateItem(props.boardId, item.value.itemId, {
      description: descriptionText.value
    })
    if (result) {
      item.value = result
      hasChanges.value = false
      emit('updated', result)
    } else {
      toast.error('설명 저장에 실패했습니다.')
    }
  } catch (error) {
    toast.error('설명 저장에 실패했습니다.')
  } finally {
    isSaving.value = false
  }
}

// v2.1: 리치 텍스트 내용 변경 핸들러 (자동 저장 트리거)
function handleRichTextChange(html: string, plainText: string) {
  hasUnsavedContent.value = true

  // 기존 타이머 취소
  if (autoSaveTimeout) {
    clearTimeout(autoSaveTimeout)
  }

  // 2초 후 자동 저장
  autoSaveTimeout = setTimeout(() => {
    saveRichTextContent()
  }, 2000)
}

// v2.1: 리치 텍스트 내용 저장
async function saveRichTextContent() {
  if (!item.value || !props.boardId || !hasUnsavedContent.value) return

  isContentSaving.value = true

  try {
    const response = await itemContentApi.updateContent(props.boardId, props.itemId, {
      contentType: 'HTML',
      content: richTextContent.value,
      version: contentVersion.value
    })

    if (response.success && response.data) {
      contentVersion.value = response.data.version
      contentLastSavedAt.value = new Date()
      hasUnsavedContent.value = false
    } else {
      toast.error('내용 저장에 실패했습니다.')
    }
  } catch (error: any) {
    // 버전 충돌 처리
    if (error.response?.status === 409) {
      const confirmed = await confirm.show({
        title: '편집 충돌',
        message: '다른 사용자가 수정했습니다. 새로고침하시겠습니까?',
        confirmText: '새로고침',
        cancelText: '내 변경사항 유지',
        confirmType: 'warning'
      })

      if (confirmed) {
        await loadItemContent()
      }
    } else {
      toast.error('내용 저장에 실패했습니다.')
    }
  } finally {
    isContentSaving.value = false
  }
}

// v2.1: 수동 저장 (버튼 클릭 시)
async function handleManualSave() {
  if (autoSaveTimeout) {
    clearTimeout(autoSaveTimeout)
    autoSaveTimeout = null
  }
  await saveRichTextContent()
}

// 완료 처리 (v2.2: 하위 업무 포함)
async function handleComplete() {
  if (!item.value || !props.boardId) return

  // v2.2: 하위 업무가 있는 경우 completeItemWithChildren 사용
  const hasChildren = item.value.hasChildren || (item.value.childCount ?? 0) > 0

  if (hasChildren) {
    // 하위 업무가 있는 경우 - 먼저 미완료 하위 업무 확인
    const result = await itemStore.completeItemWithChildren(props.boardId, item.value.itemId, false)

    if (result.success) {
      // 성공적으로 완료됨 (미완료 하위 업무 없음)
      toast.success('완료 처리되었습니다.')
      item.value = { ...item.value, status: 'COMPLETED' }
      emit('updated', item.value)
    } else if (result.incompleteChildren) {
      // 미완료 하위 업무가 있음 - 모달 표시
      incompleteChildren.value = result.incompleteChildren
      showIncompleteChildrenModal.value = true
    } else {
      toast.error('완료 처리에 실패했습니다.')
    }
  } else {
    // 하위 업무 없는 경우 - 기존 로직
    const confirmed = await confirm.show({
      title: '완료 처리',
      message: '이 업무를 완료 처리하시겠습니까?',
      confirmText: '완료',
      confirmType: 'primary'
    })

    if (confirmed) {
      const result = await itemStore.completeItem(props.boardId, item.value.itemId)
      if (result) {
        toast.success('완료 처리되었습니다.')
        item.value = { ...item.value, status: 'COMPLETED' }
        emit('updated', item.value)
      } else {
        toast.error('완료 처리에 실패했습니다.')
      }
    }
  }
}

// v2.2: 강제 완료 처리 (미완료 하위 업무 포함)
async function handleForceComplete() {
  if (!item.value || !props.boardId) return

  const result = await itemStore.completeItemWithChildren(props.boardId, item.value.itemId, true)

  showIncompleteChildrenModal.value = false
  incompleteChildren.value = null

  if (result.success) {
    toast.success('모든 하위 업무와 함께 완료 처리되었습니다.')
    item.value = { ...item.value, status: 'COMPLETED' }
    emit('updated', item.value)
  } else {
    toast.error('완료 처리에 실패했습니다.')
  }
}

// v2.2: 미완료 하위 업무 모달 닫기
function handleIncompleteChildrenModalClose() {
  showIncompleteChildrenModal.value = false
  incompleteChildren.value = null
}

// v2.2: Breadcrumb 네비게이션 핸들러
function handleBreadcrumbNavigate(ancestor: AncestorResponse) {
  if (ancestor.itemId === 0) {
    // 루트 목록으로 이동 (패널 닫기)
    emit('close')
  } else {
    // 해당 업무로 이동
    emit('navigate', ancestor.itemId)
  }
}

// v2.2: 하위 업무 선택 핸들러
function handleSubTaskSelect(childItem: Item) {
  emit('navigate', childItem.itemId)
}

// v2.2: 하위 업무 업데이트 핸들러 (추가/완료/삭제 시)
async function handleSubTaskUpdate() {
  // 아이템 새로고침하여 childCount 등 업데이트
  await loadItem()
  if (item.value) {
    emit('updated', item.value)
  }
}

// 삭제 처리
async function handleDelete() {
  if (!item.value || !props.boardId) return

  const confirmed = await confirm.show({
    title: '업무 삭제',
    message: '이 업무를 삭제하시겠습니까?',
    confirmText: '삭제',
    confirmType: 'danger'
  })

  if (confirmed) {
    const success = await itemStore.deleteItem(props.boardId, item.value.itemId)
    if (success) {
      toast.success('삭제되었습니다.')
      emit('deleted', item.value.itemId)
      emit('close')
    } else {
      toast.error('삭제에 실패했습니다.')
    }
  }
}

// 닫기
function handleClose() {
  itemStore.stopEditing()
  emit('close')
}

// 날짜 포맷
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// itemId 변경 시 아이템 재로드
watch(() => props.itemId, (newId, oldId) => {
  console.log('[ItemDetailPanel] watch triggered - itemId changed:', { newId, oldId, boardId: props.boardId })
  loadItem()
}, { immediate: true })

// 모바일 감지
const isMobile = ref(window.innerWidth < 768)

function handleResize() {
  isMobile.value = window.innerWidth < 768
}

// v2.0: boardId 변경 시 카테고리 로드
watch(() => props.boardId, () => {
  loadBoardCategories()
}, { immediate: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadColumnWidths()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  itemStore.stopEditing()

  // v2.1: 자동 저장 타이머 정리
  if (autoSaveTimeout) {
    clearTimeout(autoSaveTimeout)
    autoSaveTimeout = null
  }

  // 저장되지 않은 내용이 있으면 저장 시도
  if (hasUnsavedContent.value) {
    saveRichTextContent()
  }
})
</script>

<template>
  <div class="item-detail-panel h-full flex flex-col bg-white dark:bg-gray-800">
    <!-- 헤더 -->
    <div class="flex-shrink-0 px-4 py-3 border-b border-gray-200 dark:border-gray-700">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <button
            v-if="isMobile"
            class="p-1 -ml-1 text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 rounded"
            @click="handleClose"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h2 class="text-[15px] font-semibold text-gray-900 dark:text-white">업무 상세</h2>
        </div>

        <div class="flex items-center gap-1">
          <span v-if="isSaving" class="text-[12px] text-gray-400 mr-2">저장 중...</span>

          <!-- 이관 버튼 (v2.2: 하위 업무는 이관 불가) -->
          <Button
            v-if="item && canTransfer && item.status !== 'DELETED' && (item.itemDepth ?? 0) === 0"
            variant="ghost"
            size="sm"
            title="업무 이관"
            @click="showTransferModal = true"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
            </svg>
            이관
          </Button>

          <!-- 공유 버튼 (v2.2.1: 공유 수 배지 추가) -->
          <Button
            v-if="item && canShare && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            title="업무 공유"
            class="relative"
            @click="showShareModal = true"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            공유
            <!-- 공유 수 배지 -->
            <span
              v-if="item.shareCount && item.shareCount > 0"
              class="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 flex items-center justify-center text-[10px] font-medium text-white bg-blue-500 rounded-full"
            >
              {{ item.shareCount > 99 ? '99+' : item.shareCount }}
            </span>
          </Button>

          <!-- v2.0: 속성수정 버튼 -->
          <Button
            v-if="item && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            title="속성 수정"
            @click="showPropertyModal = true"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            속성수정
          </Button>

          <Button
            v-if="item && item.status !== 'COMPLETED' && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            @click="handleComplete"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            완료
          </Button>

          <Button
            v-if="item && item.status !== 'DELETED'"
            variant="ghost"
            size="sm"
            class="text-red-600 hover:text-red-700 hover:bg-red-50"
            @click="handleDelete"
          >
            <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            삭제
          </Button>

          <button
            v-if="!isMobile"
            class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:text-gray-500 dark:hover:text-gray-300 dark:hover:bg-gray-700 rounded"
            @click="handleClose"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div v-if="modifiedInfo || showPermissionBadge" class="mt-2 flex items-center gap-2 text-[11px] text-gray-400 dark:text-gray-500">
        <!-- 공유/배당 권한 배지 -->
        <span
          v-if="showPermissionBadge"
          class="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium"
          :class="permissionBadgeClass"
        >
          {{ permissionBadgeText }}
        </span>
        <!-- 수정 정보 -->
        <span v-if="modifiedInfo">{{ modifiedInfo.name }}님이 {{ modifiedInfo.time }}에 수정</span>
      </div>

      <!-- v2.2: Breadcrumb (하위 업무인 경우 표시) -->
      <ItemBreadcrumb
        v-if="item && item.parentItemId"
        :item="item"
        :board-id="boardId"
        class="mt-2"
        @navigate="handleBreadcrumbNavigate"
      />
    </div>

    <!-- 탭 (모바일) -->
    <div v-if="isMobile" class="flex-shrink-0 border-b border-gray-200 dark:border-gray-700">
      <div class="flex">
        <button
          class="flex-1 px-3 py-2.5 text-[13px] font-medium"
          :class="activeTab === 'detail' ? 'text-primary-600 dark:text-primary-400 border-b-2 border-primary-600 dark:border-primary-400' : 'text-gray-500 dark:text-gray-400'"
          @click="activeTab = 'detail'"
        >속성</button>
        <button
          class="flex-1 px-3 py-2.5 text-[13px] font-medium"
          :class="activeTab === 'content' ? 'text-primary-600 dark:text-primary-400 border-b-2 border-primary-600 dark:border-primary-400' : 'text-gray-500 dark:text-gray-400'"
          @click="activeTab = 'content'"
        >내용</button>
        <button
          class="flex-1 px-3 py-2.5 text-[13px] font-medium relative"
          :class="activeTab === 'comments' ? 'text-primary-600 dark:text-primary-400 border-b-2 border-primary-600 dark:border-primary-400' : 'text-gray-500 dark:text-gray-400'"
          @click="activeTab = 'comments'"
        >
          댓글
          <span v-if="commentCount > 0" class="ml-1 px-1.5 py-0.5 text-[10px] rounded-full bg-gray-100 dark:bg-gray-700 dark:text-gray-300">{{ commentCount }}</span>
        </button>
      </div>
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="flex-1 min-h-0 overflow-hidden">
      <!-- 로딩 -->
      <div v-if="isLoading" class="h-full flex items-center justify-center">
        <Spinner size="lg" />
      </div>

      <!-- 아이템 없음 -->
      <div v-else-if="!item" class="h-full flex items-center justify-center">
        <p class="text-[14px] text-gray-500 dark:text-gray-400">업무를 찾을 수 없습니다.</p>
      </div>

      <!-- PC 레이아웃: 3컬럼 + 리사이즈 핸들 -->
      <template v-else-if="!isMobile">
        <div ref="containerRef" class="h-full flex">
          <!-- 속성 패널 -->
          <div
            class="flex-shrink-0 overflow-y-auto p-3 bg-gray-50 dark:bg-gray-900"
            :style="{ width: `${propWidth}px` }"
          >
            <ItemForm
              :item="item"
              :disabled="item.status === 'DELETED'"
              @update="handleUpdate"
            />

            <!-- v2.2: 하위 업무 목록 -->
            <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
              <SubTaskList
                :parent-item="item"
                :board-id="boardId"
                :expanded="subTaskListExpanded"
                :max-depth="2"
                :include-completed="false"
                @select="handleSubTaskSelect"
                @update="handleSubTaskUpdate"
              />
            </div>
          </div>

          <!-- 리사이즈 핸들 (속성-에디터) -->
          <div
            class="w-2 flex-shrink-0 bg-gray-200 hover:bg-primary-300 dark:bg-gray-700 dark:hover:bg-primary-600 cursor-ew-resize flex items-center justify-center group"
            :class="{ 'bg-primary-400 dark:bg-primary-500': isResizingProp }"
            @mousedown="startResizeProp"
          >
            <div class="flex flex-col gap-0.5">
              <div class="w-0.5 h-4 bg-gray-400 group-hover:bg-primary-600 dark:bg-gray-500 dark:group-hover:bg-primary-400 rounded-full" />
            </div>
          </div>

          <!-- 에디터 패널 -->
          <div class="flex-1 min-w-0 overflow-hidden flex flex-col">
            <!-- v2.1: 설명 필드 (단일 라인) -->
            <div class="flex-shrink-0 px-4 pt-4 pb-2">
              <label class="text-[12px] font-medium text-gray-600 dark:text-gray-400 mb-1.5 block">설명</label>
              <input
                v-model="descriptionText"
                type="text"
                class="w-full px-3 py-2 text-[13px] border border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-50 disabled:text-gray-500 dark:disabled:bg-gray-800 dark:disabled:text-gray-500"
                placeholder="업무에 대한 간단한 설명을 입력하세요..."
                :disabled="item.status === 'DELETED'"
                @blur="handleDescriptionSave"
                @keyup.enter="handleDescriptionSave"
              />
            </div>

            <!-- v2.1: 리치 텍스트 에디터 영역 -->
            <div class="flex-1 min-h-[200px] px-4 pb-0 flex flex-col">
              <div class="flex items-center justify-between mb-1.5">
                <label class="text-[12px] font-medium text-gray-600 dark:text-gray-400">내용</label>
                <div class="flex items-center gap-2 text-[11px] text-gray-400 dark:text-gray-500">
                  <span v-if="isContentSaving" class="flex items-center gap-1">
                    <svg class="w-3 h-3 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    저장 중...
                  </span>
                  <span v-else-if="hasUnsavedContent" class="text-amber-500">미저장</span>
                  <span v-else-if="contentLastSavedAt">
                    {{ formatDateTime(contentLastSavedAt.toISOString()) }} 저장됨
                  </span>
                  <button
                    v-if="hasUnsavedContent && !isContentSaving"
                    class="px-2 py-0.5 text-primary-600 hover:bg-primary-50 rounded transition-colors"
                    @click="handleManualSave"
                  >
                    저장
                  </button>
                </div>
              </div>
              <!-- v2.2.1: overflow-hidden + max-height 100%로 스크롤 활성화 -->
            <div class="flex-1 min-h-0 overflow-hidden">
                <RichTextEditor
                  v-model="richTextContent"
                  :readonly="item.status === 'DELETED'"
                  placeholder="상세 내용을 입력하세요..."
                  min-height="100%"
                  max-height="100%"
                  related-type="ITEM"
                  :related-id="item.itemId"
                  @change="handleRichTextChange"
                />
              </div>
            </div>

            <!-- 파일 첨부 영역 -->
            <div class="flex-shrink-0">
              <FileAttachment
                related-type="ITEM"
                :related-id="item.itemId"
                :disabled="item.status === 'DELETED'"
              />
            </div>
          </div>

          <!-- 리사이즈 핸들 (에디터-댓글) -->
          <div
            class="w-2 flex-shrink-0 bg-gray-200 hover:bg-primary-300 dark:bg-gray-700 dark:hover:bg-primary-600 cursor-ew-resize flex items-center justify-center group"
            :class="{ 'bg-primary-400 dark:bg-primary-500': isResizingComment }"
            @mousedown="startResizeComment"
          >
            <div class="flex flex-col gap-0.5">
              <div class="w-0.5 h-4 bg-gray-400 group-hover:bg-primary-600 dark:bg-gray-500 dark:group-hover:bg-primary-400 rounded-full" />
            </div>
          </div>

          <!-- 댓글 패널 -->
          <div
            class="flex-shrink-0 overflow-y-auto p-3 bg-gray-50 dark:bg-gray-900"
            :style="{ width: `${commentWidth}px` }"
          >
            <CommentList ref="commentListRef" :item-id="itemId" />
          </div>
        </div>
      </template>

      <!-- 모바일 레이아웃 -->
      <template v-else>
        <div v-show="activeTab === 'detail'" class="h-full overflow-y-auto p-4 border-t border-gray-200 dark:border-gray-700">
          <ItemForm :item="item" :disabled="item.status === 'DELETED'" @update="handleUpdate" />

          <!-- v2.2: 하위 업무 목록 (모바일) -->
          <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
            <SubTaskList
              :parent-item="item"
              :board-id="boardId"
              :expanded="subTaskListExpanded"
              :max-depth="2"
              :include-completed="false"
              @select="handleSubTaskSelect"
              @update="handleSubTaskUpdate"
            />
          </div>
        </div>
        <div v-show="activeTab === 'content'" class="h-full overflow-y-auto flex flex-col border-t border-gray-200 dark:border-gray-700">
          <!-- v2.1: 설명 필드 (단일 라인) -->
          <div class="flex-shrink-0 px-4 pt-4 pb-2">
            <label class="text-[12px] font-medium text-gray-600 dark:text-gray-400 mb-1.5 block">설명</label>
            <input
              v-model="descriptionText"
              type="text"
              class="w-full px-3 py-2 text-[13px] border border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-50 disabled:text-gray-500 dark:disabled:bg-gray-800 dark:disabled:text-gray-500"
              placeholder="업무에 대한 간단한 설명을 입력하세요..."
              :disabled="item.status === 'DELETED'"
              @blur="handleDescriptionSave"
            />
          </div>

          <!-- v2.1: 리치 텍스트 에디터 -->
          <div class="flex-1 p-4">
            <div class="flex items-center justify-between mb-1.5">
              <label class="text-[12px] font-medium text-gray-600 dark:text-gray-400">내용</label>
              <div class="flex items-center gap-2 text-[11px] text-gray-400 dark:text-gray-500">
                <span v-if="isContentSaving">저장 중...</span>
                <span v-else-if="hasUnsavedContent" class="text-amber-500">미저장</span>
                <button
                  v-if="hasUnsavedContent && !isContentSaving"
                  class="px-2 py-0.5 text-primary-600 hover:bg-primary-50 rounded transition-colors"
                  @click="handleManualSave"
                >
                  저장
                </button>
              </div>
            </div>
            <RichTextEditor
              v-model="richTextContent"
              :readonly="item.status === 'DELETED'"
              placeholder="상세 내용을 입력하세요..."
              min-height="calc(100vh - 400px)"
              max-height="none"
              related-type="ITEM"
              :related-id="item.itemId"
              @change="handleRichTextChange"
            />
          </div>
          <div class="flex-shrink-0">
            <FileAttachment
              related-type="ITEM"
              :related-id="item.itemId"
              :disabled="item.status === 'DELETED'"
            />
          </div>
        </div>
        <div v-show="activeTab === 'comments'" class="h-full overflow-y-auto p-4">
          <CommentList ref="commentListRef" :item-id="itemId" />
        </div>
      </template>
    </div>

    <!-- 하단 버튼 (모바일) -->
    <div
      v-if="isMobile && item && item.status !== 'DELETED'"
      class="flex-shrink-0 px-4 py-3 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800"
    >
      <div class="flex gap-2">
        <Button v-if="item.status !== 'COMPLETED'" variant="primary" class="flex-1" @click="handleComplete">완료 처리</Button>
        <Button variant="outline" class="flex-1" @click="handleClose">닫기</Button>
      </div>
    </div>

    <!-- 이관 모달 -->
    <ItemTransferModal
      v-if="item"
      :show="showTransferModal"
      :item="item"
      :board-id="boardId"
      @close="showTransferModal = false"
      @transferred="handleTransferred"
    />

    <!-- 공유 모달 -->
    <ItemShareModal
      v-if="item"
      :show="showShareModal"
      :item="item"
      :board-id="boardId"
      @close="showShareModal = false"
      @updated="handleShareUpdated"
    />

    <!-- v2.0: 속성 선택 모달 (EntityEditModal 사용) -->
    <EntityEditModal
      v-if="item"
      v-model="showPropertyModal"
      mode="item"
      title="업무 속성 수정"
      :categories="boardCategories"
      :show-color="false"
      name-label="업무명"
      :initial-name="item.title"
      :initial-description="item.description || ''"
      :initial-category-id="item.categoryId"
      :initial-property-ids="selectedPropertyIds"
      :initial-property-defaults="selectedPropertyDefaults"
      :initial-property-sort-orders="selectedPropertySortOrders"
      enable-property-drag-drop
      @save="handlePropertySave"
    />

    <!-- v2.1: 담당자 배정 확인 모달 -->
    <AssignConfirmModal
      v-if="item && pendingAssignee"
      :show="showAssignModal"
      :board-id="boardId"
      :item-id="item.itemId"
      :item-title="item.title"
      :assignee-username="pendingAssignee.username"
      :assignee-name="pendingAssignee.name"
      @close="handleAssignModalClose"
      @assigned="handleAssigned"
    />

    <!-- v2.2: 미완료 하위 업무 확인 모달 -->
    <IncompleteChildrenModal
      :show="showIncompleteChildrenModal"
      :incomplete-children="incompleteChildren"
      :parent-title="item?.title"
      @close="handleIncompleteChildrenModalClose"
      @cancel="handleIncompleteChildrenModalClose"
      @force-complete="handleForceComplete"
    />
  </div>
</template>
