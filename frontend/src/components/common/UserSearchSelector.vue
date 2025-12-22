<script setup lang="ts">
/**
 * 사용자 검색/선택 통합 컴포넌트
 *
 * 검색 방법:
 * 1. 부서에서 찾기 - 부서 트리 → 부서 선택 → 사용자 목록 → 사용자 선택
 * 2. 이름으로 찾기 - 이름 검색 → 이름, 부서, Email 표시 → 사용자 선택
 *
 * 선택된 사용자 표시: 사용자 이름(부서명, Email)
 */
import { ref, computed, watch, onMounted } from 'vue'
import type { User } from '@/types/user'
import type { Department } from '@/types/department'
import { userApi } from '@/api/user'
import { departmentApi } from '@/api/department'
import DepartmentTree from './DepartmentTree.vue'

type SearchTab = 'department' | 'name'

interface Props {
  modelValue?: number | null
  excludeUserIds?: number[]
  placeholder?: string
  disabled?: boolean
  label?: string
  required?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  excludeUserIds: () => [],
  placeholder: '사용자를 선택하세요',
  disabled: false,
  required: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', userId: number | null): void
  (e: 'select', user: User | null): void
}>()

// 탭 상태
const activeTab = ref<SearchTab>('department')

// 데이터
const departments = ref<Department[]>([])
const allUsers = ref<User[]>([])
const departmentUsers = ref<User[]>([])
const loading = ref(false)
const searchQuery = ref('')

// 선택 상태
const selectedDepartmentId = ref<number | null>(null)
const selectedUser = ref<User | null>(null)

// 필터링된 사용자 목록 (이름 검색)
const filteredUsers = computed(() => {
  if (!searchQuery.value) {
    return []
  }

  const query = searchQuery.value.toLowerCase()
  return allUsers.value
    .filter(user => {
      // 제외 대상 필터
      if (props.excludeUserIds.includes(user.userId)) {
        return false
      }
      // 이름, 부서명, 이메일로 검색
      const name = (user.name || user.userName || '').toLowerCase()
      const deptName = (user.departmentName || '').toLowerCase()
      const email = (user.email || '').toLowerCase()
      return name.includes(query) || deptName.includes(query) || email.includes(query)
    })
    .slice(0, 20) // 최대 20개만 표시
})

// 부서별 사용자 목록 (제외 대상 필터 적용)
const displayDepartmentUsers = computed(() => {
  return departmentUsers.value.filter(user =>
    !props.excludeUserIds.includes(user.userId)
  )
})

// 선택된 사용자 표시 텍스트
const selectedUserDisplay = computed(() => {
  if (!selectedUser.value) return ''
  const name = selectedUser.value.name || selectedUser.value.userName || ''
  const dept = selectedUser.value.departmentName || ''
  const email = selectedUser.value.email || ''

  const parts = [name]
  const subParts = []
  if (dept) subParts.push(dept)
  if (email) subParts.push(email)

  if (subParts.length > 0) {
    parts.push(`(${subParts.join(', ')})`)
  }

  return parts.join(' ')
})

// 초기 데이터 로드
async function loadInitialData() {
  loading.value = true
  try {
    // 부서 목록 로드
    const deptResponse = await departmentApi.getDepartments({ useYn: 'Y' })
    departments.value = deptResponse.data || []

    // 전체 사용자 목록 로드 (이름 검색용)
    const userResponse = await userApi.getUsers({ useYn: 'Y' })
    allUsers.value = userResponse.data || []

    // 초기 선택값이 있으면 해당 사용자 찾기
    if (props.modelValue) {
      const found = allUsers.value.find(u => u.userId === props.modelValue)
      if (found) {
        selectedUser.value = found
      }
    }
  } catch (error) {
    console.error('Failed to load initial data:', error)
  } finally {
    loading.value = false
  }
}

// 부서 선택 시 사용자 목록 로드
async function handleDepartmentSelect(department: Department) {
  selectedDepartmentId.value = department.departmentId
  loading.value = true
  try {
    const response = await departmentApi.getDepartmentUsers(department.departmentId)
    departmentUsers.value = response.data || []
  } catch (error) {
    console.error('Failed to load department users:', error)
    departmentUsers.value = []
  } finally {
    loading.value = false
  }
}

// 사용자 선택
function handleUserSelect(user: User) {
  selectedUser.value = user
  emit('update:modelValue', user.userId)
  emit('select', user)
}

// 선택 해제
function clearSelection() {
  selectedUser.value = null
  emit('update:modelValue', null)
  emit('select', null)
}

// 탭 변경
function changeTab(tab: SearchTab) {
  activeTab.value = tab
  searchQuery.value = ''
  selectedDepartmentId.value = null
  departmentUsers.value = []
}

// props.modelValue 변경 감지
watch(() => props.modelValue, (newValue) => {
  if (newValue === null) {
    selectedUser.value = null
  } else if (newValue !== selectedUser.value?.userId) {
    const found = allUsers.value.find(u => u.userId === newValue)
    if (found) {
      selectedUser.value = found
    }
  }
})

onMounted(() => {
  loadInitialData()
})
</script>

<template>
  <div class="user-search-selector">
    <!-- Label -->
    <label v-if="label" class="block text-[13px] font-medium text-gray-700 mb-2">
      {{ label }}
      <span v-if="required" class="text-red-500 ml-0.5">*</span>
    </label>

    <!-- 탭 전환 -->
    <div class="flex border-b border-gray-200 mb-3">
      <button
        type="button"
        class="px-4 py-2 text-sm font-medium border-b-2 transition-colors"
        :class="activeTab === 'department'
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
        :disabled="disabled"
        @click="changeTab('department')"
      >
        부서에서 찾기
      </button>
      <button
        type="button"
        class="px-4 py-2 text-sm font-medium border-b-2 transition-colors"
        :class="activeTab === 'name'
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
        :disabled="disabled"
        @click="changeTab('name')"
      >
        이름으로 찾기
      </button>
    </div>

    <!-- 부서에서 찾기 탭 -->
    <div v-if="activeTab === 'department'" class="department-search">
      <div class="flex gap-3 h-64">
        <!-- 부서 트리 -->
        <div class="w-1/2 border rounded-lg overflow-hidden">
          <div class="bg-gray-50 px-3 py-2 text-xs font-medium text-gray-600 border-b">
            부서 선택
          </div>
          <div class="overflow-y-auto h-[calc(100%-32px)]">
            <DepartmentTree
              :departments="departments"
              :selected-department-id="selectedDepartmentId"
              @select="handleDepartmentSelect"
            />
          </div>
        </div>

        <!-- 사용자 목록 -->
        <div class="w-1/2 border rounded-lg overflow-hidden">
          <div class="bg-gray-50 px-3 py-2 text-xs font-medium text-gray-600 border-b">
            사용자 목록
            <span v-if="selectedDepartmentId && displayDepartmentUsers.length > 0" class="text-gray-400">
              ({{ displayDepartmentUsers.length }}명)
            </span>
          </div>
          <div class="overflow-y-auto h-[calc(100%-32px)]">
            <!-- 로딩 -->
            <div v-if="loading" class="flex items-center justify-center h-full">
              <svg class="animate-spin h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </div>

            <!-- 부서 미선택 -->
            <div v-else-if="!selectedDepartmentId" class="flex items-center justify-center h-full text-gray-400 text-sm">
              부서를 선택하세요
            </div>

            <!-- 사용자 없음 -->
            <div v-else-if="displayDepartmentUsers.length === 0" class="flex items-center justify-center h-full text-gray-400 text-sm">
              사용자가 없습니다
            </div>

            <!-- 사용자 목록 -->
            <div v-else class="p-1">
              <div
                v-for="user in displayDepartmentUsers"
                :key="user.userId"
                class="flex items-center gap-2 px-3 py-2 rounded cursor-pointer hover:bg-gray-50 transition-colors"
                :class="selectedUser?.userId === user.userId ? 'bg-primary-50' : ''"
                @click="handleUserSelect(user)"
              >
                <!-- 라디오 표시 -->
                <div class="w-4 h-4 rounded-full border-2 flex items-center justify-center flex-shrink-0"
                  :class="selectedUser?.userId === user.userId
                    ? 'border-primary-500 bg-primary-500'
                    : 'border-gray-300'"
                >
                  <div v-if="selectedUser?.userId === user.userId" class="w-1.5 h-1.5 rounded-full bg-white"></div>
                </div>

                <!-- 사용자 정보 -->
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium text-gray-700 truncate">
                    {{ user.name || user.userName }}
                  </div>
                  <div class="text-xs text-gray-400 truncate">
                    {{ user.departmentName || '-' }}{{ user.email ? `, ${user.email}` : '' }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 이름으로 찾기 탭 -->
    <div v-else class="name-search">
      <!-- 검색 입력 -->
      <div class="mb-3">
        <input
          v-model="searchQuery"
          type="text"
          class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
          placeholder="이름, 부서, 이메일로 검색..."
          :disabled="disabled"
        />
      </div>

      <!-- 검색 결과 -->
      <div class="border rounded-lg overflow-hidden h-56">
        <div class="overflow-y-auto h-full">
          <!-- 검색어 없음 -->
          <div v-if="!searchQuery" class="flex items-center justify-center h-full text-gray-400 text-sm">
            검색어를 입력하세요
          </div>

          <!-- 검색 결과 없음 -->
          <div v-else-if="filteredUsers.length === 0" class="flex items-center justify-center h-full text-gray-400 text-sm">
            검색 결과가 없습니다
          </div>

          <!-- 검색 결과 목록 -->
          <div v-else class="p-1">
            <div
              v-for="user in filteredUsers"
              :key="user.userId"
              class="flex items-center gap-2 px-3 py-2 rounded cursor-pointer hover:bg-gray-50 transition-colors"
              :class="selectedUser?.userId === user.userId ? 'bg-primary-50' : ''"
              @click="handleUserSelect(user)"
            >
              <!-- 라디오 표시 -->
              <div class="w-4 h-4 rounded-full border-2 flex items-center justify-center flex-shrink-0"
                :class="selectedUser?.userId === user.userId
                  ? 'border-primary-500 bg-primary-500'
                  : 'border-gray-300'"
              >
                <div v-if="selectedUser?.userId === user.userId" class="w-1.5 h-1.5 rounded-full bg-white"></div>
              </div>

              <!-- 사용자 정보 -->
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-gray-700 truncate">
                  {{ user.name || user.userName }}
                  <span class="font-normal text-gray-500">
                    ({{ user.departmentName || '-' }}{{ user.email ? `, ${user.email}` : '' }})
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 선택된 사용자 표시 -->
    <div class="mt-3 px-3 py-2 bg-gray-50 rounded-lg border" v-if="selectedUser">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="text-xs text-gray-500">선택됨:</span>
          <span class="text-sm font-medium text-gray-700">{{ selectedUserDisplay }}</span>
        </div>
        <button
          type="button"
          class="text-gray-400 hover:text-gray-600"
          :disabled="disabled"
          @click="clearSelection"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 미선택 상태 -->
    <div class="mt-3 px-3 py-2 bg-gray-50 rounded-lg border text-gray-400 text-sm" v-else>
      {{ placeholder }}
    </div>
  </div>
</template>
