<script setup lang="ts">
/**
 * 부서 트리 컴포넌트
 * - 계층적 부서 구조 표시
 * - 펼침/접힘 기능
 * - 부서 선택 이벤트
 */
import { ref, computed, onMounted } from 'vue'
import type { Department } from '@/types/department'
import { departmentApi } from '@/api/department'

interface Props {
  departments?: Department[]
  selectedDepartmentId?: number | null
  autoLoad?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  departments: () => [],
  selectedDepartmentId: null,
  autoLoad: false
})

const emit = defineEmits<{
  (e: 'select', department: Department): void
}>()

const internalDepartments = ref<Department[]>([])
const expandedIds = ref<Set<number>>(new Set())
const loading = ref(false)

// 표시할 부서 목록 (props 또는 내부 로드)
const displayDepartments = computed(() => {
  return props.departments.length > 0 ? props.departments : internalDepartments.value
})

// 부서 목록 로드
async function loadDepartments() {
  if (!props.autoLoad) return

  loading.value = true
  try {
    const response = await departmentApi.getDepartments({ useYn: 'Y' })
    internalDepartments.value = response.data || []
    // 최상위 부서 자동 펼침
    internalDepartments.value.forEach(dept => {
      if (dept.children && dept.children.length > 0) {
        expandedIds.value.add(dept.departmentId)
      }
    })
  } catch (error) {
    console.error('Failed to load departments:', error)
  } finally {
    loading.value = false
  }
}

// 펼침/접힘 토글
function toggleExpand(departmentId: number) {
  if (expandedIds.value.has(departmentId)) {
    expandedIds.value.delete(departmentId)
  } else {
    expandedIds.value.add(departmentId)
  }
}

// 펼침 여부 확인
function isExpanded(departmentId: number): boolean {
  return expandedIds.value.has(departmentId)
}

// 부서 선택
function selectDepartment(department: Department) {
  emit('select', department)
}

// 모두 펼치기
function expandAll() {
  const addAllIds = (depts: Department[]) => {
    depts.forEach(dept => {
      if (dept.children && dept.children.length > 0) {
        expandedIds.value.add(dept.departmentId)
        addAllIds(dept.children)
      }
    })
  }
  addAllIds(displayDepartments.value)
}

// 모두 접기
function collapseAll() {
  expandedIds.value.clear()
}

onMounted(() => {
  if (props.autoLoad) {
    loadDepartments()
  }
})

// 외부에서 사용 가능하도록 노출
defineExpose({
  expandAll,
  collapseAll,
  loadDepartments
})
</script>

<template>
  <div class="department-tree">
    <!-- 로딩 -->
    <div v-if="loading" class="flex items-center justify-center py-4">
      <svg class="animate-spin h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="displayDepartments.length === 0" class="text-center py-4 text-gray-500 text-sm">
      부서가 없습니다
    </div>

    <!-- 트리 렌더링 -->
    <div v-else class="space-y-0.5">
      <template v-for="department in displayDepartments" :key="department.departmentId">
        <DepartmentTreeNode
          :department="department"
          :depth="0"
          :expanded-ids="expandedIds"
          :selected-id="selectedDepartmentId"
          @toggle="toggleExpand"
          @select="selectDepartment"
        />
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, h, type PropType } from 'vue'

/**
 * 부서 트리 노드 (재귀 컴포넌트)
 */
const DepartmentTreeNode = defineComponent({
  name: 'DepartmentTreeNode',
  props: {
    department: {
      type: Object as PropType<Department>,
      required: true
    },
    depth: {
      type: Number,
      default: 0
    },
    expandedIds: {
      type: Object as PropType<Set<number>>,
      required: true
    },
    selectedId: {
      type: Number as PropType<number | null>,
      default: null
    }
  },
  emits: ['toggle', 'select'],
  setup(props, { emit }) {
    const hasChildren = computed(() =>
      props.department.children && props.department.children.length > 0
    )

    const isExpanded = computed(() =>
      props.expandedIds.has(props.department.departmentId)
    )

    const isSelected = computed(() =>
      props.selectedId === props.department.departmentId
    )

    function handleToggle(e: Event) {
      e.stopPropagation()
      emit('toggle', props.department.departmentId)
    }

    function handleSelect() {
      emit('select', props.department)
    }

    return () => {
      const indent = `${props.depth * 16}px`

      const children = []

      // 현재 노드
      children.push(
        h('div', {
          class: [
            'flex items-center gap-1 px-2 py-1.5 rounded cursor-pointer text-sm',
            'hover:bg-gray-100 transition-colors',
            isSelected.value ? 'bg-primary-50 text-primary-700' : 'text-gray-700'
          ],
          style: { paddingLeft: indent },
          onClick: handleSelect
        }, [
          // 펼침/접힘 아이콘 (자식이 있을 때만)
          h('span', {
            class: 'w-5 h-5 flex items-center justify-center flex-shrink-0',
            onClick: hasChildren.value ? handleToggle : undefined
          }, hasChildren.value ? [
            h('svg', {
              class: [
                'w-4 h-4 text-gray-400 transition-transform',
                isExpanded.value ? 'rotate-90' : ''
              ],
              fill: 'none',
              stroke: 'currentColor',
              viewBox: '0 0 24 24'
            }, [
              h('path', {
                'stroke-linecap': 'round',
                'stroke-linejoin': 'round',
                'stroke-width': '2',
                d: 'M9 5l7 7-7 7'
              })
            ])
          ] : [
            // 빈 공간 (정렬용)
            h('span', { class: 'w-4' })
          ]),
          // 폴더 아이콘
          h('svg', {
            class: 'w-4 h-4 text-gray-400 flex-shrink-0',
            fill: 'none',
            stroke: 'currentColor',
            viewBox: '0 0 24 24'
          }, [
            h('path', {
              'stroke-linecap': 'round',
              'stroke-linejoin': 'round',
              'stroke-width': '2',
              d: isExpanded.value && hasChildren.value
                ? 'M5 19a2 2 0 01-2-2V7a2 2 0 012-2h4l2 2h4a2 2 0 012 2v1M5 19h14a2 2 0 002-2v-5a2 2 0 00-2-2H9a2 2 0 00-2 2v5a2 2 0 01-2 2z'
                : 'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z'
            })
          ]),
          // 부서명
          h('span', { class: 'truncate flex-1' }, props.department.departmentName),
          // 사용자 수 (있을 경우)
          props.department.userCount !== undefined && props.department.userCount > 0 ?
            h('span', { class: 'text-xs text-gray-400' }, `(${props.department.userCount})`) : null
        ])
      )

      // 자식 노드들 (펼쳐진 상태일 때만)
      if (hasChildren.value && isExpanded.value) {
        props.department.children!.forEach(child => {
          children.push(
            h(DepartmentTreeNode, {
              key: child.departmentId,
              department: child,
              depth: props.depth + 1,
              expandedIds: props.expandedIds,
              selectedId: props.selectedId,
              onToggle: (id: number) => emit('toggle', id),
              onSelect: (dept: Department) => emit('select', dept)
            })
          )
        })
      }

      return h('div', { class: 'department-node' }, children)
    }
  }
})

export default {
  components: {
    DepartmentTreeNode
  }
}
</script>
