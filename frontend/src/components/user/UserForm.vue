<script setup lang="ts">
/**
 * 사용자 등록/수정 폼 컴포넌트
 * - 사용자 이름, 아이디, 비밀번호 입력
 * - 비밀번호 정책 검증
 * - 부서 선택 (계층 구조)
 * - 그룹 다중 선택 (태그)
 */
import { ref, computed, watch, onMounted } from 'vue'
import Input from '@/components/common/Input.vue'
import type { User, UserCreateRequest, UserUpdateRequest } from '@/types/user'
import type { Department } from '@/types/department'
import type { Group } from '@/types/group'
import { departmentApi } from '@/api/department'
import { groupApi } from '@/api/group'
import { useUiStore } from '@/stores/ui'

interface Props {
  user?: User | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  user: null,
  loading: false
})

const emit = defineEmits<{
  (e: 'submit', data: UserCreateRequest | UserUpdateRequest): void
  (e: 'cancel'): void
}>()

const uiStore = useUiStore()

// Form fields
const userName = ref('')
const email = ref('')
const username = ref('')
const password = ref('')
const passwordConfirm = ref('')
const departmentId = ref<number | null>(null)
const selectedGroupIds = ref<number[]>([])

// Data
const departments = ref<Department[]>([])
const groups = ref<Group[]>([])
const loadingDepartments = ref(false)
const loadingGroups = ref(false)

// Dropdown states
const showDepartmentDropdown = ref(false)
const showGroupDropdown = ref(false)
const departmentSearch = ref('')
const groupSearch = ref('')

// Password visibility
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

// 편집 모드 여부
const isEditMode = computed(() => !!props.user)

// 버튼 텍스트
const submitButtonText = computed(() => isEditMode.value ? '변경' : '등록')

// 부서 옵션 (트리 구조 평면화 + 들여쓰기)
const departmentOptions = computed(() => {
  const options: { id: number; name: string; depth: number }[] = []

  const flatten = (depts: Department[], depth = 0) => {
    for (const dept of depts) {
      if (dept.useYn === 'Y') {
        options.push({
          id: dept.departmentId,
          name: dept.departmentName,
          depth
        })
        if (dept.children?.length) {
          flatten(dept.children, depth + 1)
        }
      }
    }
  }

  flatten(departments.value)
  return options
})

// 필터링된 부서 옵션
const filteredDepartmentOptions = computed(() => {
  if (!departmentSearch.value) return departmentOptions.value
  const search = departmentSearch.value.toLowerCase()
  return departmentOptions.value.filter(opt =>
    opt.name.toLowerCase().includes(search)
  )
})

// 선택된 부서 이름
const selectedDepartmentName = computed(() => {
  if (!departmentId.value) return ''
  const dept = departmentOptions.value.find(d => d.id === departmentId.value)
  return dept?.name || ''
})

// 선택된 그룹 목록
const selectedGroups = computed(() => {
  return groups.value.filter(g => selectedGroupIds.value.includes(g.groupId))
})

// 필터링된 그룹 옵션 (선택되지 않은 것만)
const filteredGroupOptions = computed(() => {
  let filtered = groups.value.filter(g =>
    g.useYn === 'Y' && !selectedGroupIds.value.includes(g.groupId)
  )
  if (groupSearch.value) {
    const search = groupSearch.value.toLowerCase()
    filtered = filtered.filter(g =>
      g.groupName.toLowerCase().includes(search) ||
      g.groupCode.toLowerCase().includes(search)
    )
  }
  return filtered
})

// 비밀번호 정책 검증
interface PasswordValidation {
  minLength: boolean
  hasUppercase: boolean
  hasLowercase: boolean
  hasNumber: boolean
  hasSpecial: boolean
  isValid: boolean
}

const passwordValidation = computed<PasswordValidation>(() => {
  const pwd = password.value
  const minLength = pwd.length >= 8
  const hasUppercase = /[A-Z]/.test(pwd)
  const hasLowercase = /[a-z]/.test(pwd)
  const hasNumber = /[0-9]/.test(pwd)
  const hasSpecial = /[!@#$%^&*]/.test(pwd)

  return {
    minLength,
    hasUppercase,
    hasLowercase,
    hasNumber,
    hasSpecial,
    isValid: minLength && hasUppercase && hasLowercase && hasNumber && hasSpecial
  }
})

// 비밀번호 일치 여부
const passwordsMatch = computed(() => {
  if (!password.value || !passwordConfirm.value) return true
  return password.value === passwordConfirm.value
})

// 아이디 유효성 (영문+숫자, 4~20자)
const usernameValidation = computed(() => {
  const id = username.value
  if (!id) return { isValid: true, message: '' }

  const validLength = id.length >= 4 && id.length <= 20
  const validChars = /^[a-zA-Z0-9]+$/.test(id)

  if (!validChars) {
    return { isValid: false, message: '영문과 숫자만 사용 가능합니다.' }
  }
  if (!validLength) {
    return { isValid: false, message: '4~20자로 입력해주세요.' }
  }
  return { isValid: true, message: '' }
})

// 이메일 유효성
const emailValidation = computed(() => {
  const emailValue = email.value
  if (!emailValue) return { isValid: true, message: '' }

  const validFormat = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(emailValue)

  if (!validFormat) {
    return { isValid: false, message: '올바른 이메일 형식이 아닙니다.' }
  }
  return { isValid: true, message: '' }
})

// 폼 유효성
const isValid = computed(() => {
  // 필수 필드 검증
  if (!userName.value.trim()) return false

  // 이메일 형식 검증 (입력된 경우에만)
  if (email.value && !emailValidation.value.isValid) return false

  if (isEditMode.value) {
    // 편집 모드: 비밀번호 입력시에만 검증
    if (password.value) {
      if (!passwordValidation.value.isValid) return false
      if (!passwordsMatch.value) return false
    }
    return true
  } else {
    // 생성 모드: 모든 필드 필수
    if (!username.value.trim()) return false
    if (!usernameValidation.value.isValid) return false
    if (!password.value) return false
    if (!passwordValidation.value.isValid) return false
    if (!passwordsMatch.value) return false
    return true
  }
})

// 부서 목록 로드 (트리 구조)
async function loadDepartments() {
  loadingDepartments.value = true
  try {
    const response = await departmentApi.getDepartments({ useYn: 'Y' })
    if (response.success && response.data) {
      departments.value = response.data
    }
  } catch (error) {
    console.error('Failed to load departments:', error)
    uiStore.showError('부서 목록을 불러오는데 실패했습니다.')
  } finally {
    loadingDepartments.value = false
  }
}

// 그룹 목록 로드
async function loadGroups() {
  loadingGroups.value = true
  try {
    const response = await groupApi.getGroups({ useYn: 'Y' })
    if (response.success && response.data) {
      groups.value = response.data
    }
  } catch (error) {
    console.error('Failed to load groups:', error)
    uiStore.showError('그룹 목록을 불러오는데 실패했습니다.')
  } finally {
    loadingGroups.value = false
  }
}

// 폼 초기화
function resetForm() {
  userName.value = ''
  email.value = ''
  username.value = ''
  password.value = ''
  passwordConfirm.value = ''
  departmentId.value = null
  selectedGroupIds.value = []
  showPassword.value = false
  showPasswordConfirm.value = false
  departmentSearch.value = ''
  groupSearch.value = ''
}

// 사용자 데이터로 폼 채우기
function fillForm(user: User) {
  userName.value = user.userName || user.name || ''
  email.value = user.email || ''
  username.value = user.username
  departmentId.value = user.departmentId ?? null
  // groupIds 우선, 없으면 groups 배열에서 추출
  selectedGroupIds.value = user.groupIds ?? user.groups?.map(g => g.groupId) ?? []
  // 편집 모드에서 비밀번호는 비움
  password.value = ''
  passwordConfirm.value = ''
}

// 부서 선택
function selectDepartment(id: number | null) {
  departmentId.value = id
  showDepartmentDropdown.value = false
  departmentSearch.value = ''
}

// 부서 선택 해제
function clearDepartment() {
  departmentId.value = null
}

// 그룹 추가
function addGroup(groupId: number) {
  if (!selectedGroupIds.value.includes(groupId)) {
    selectedGroupIds.value.push(groupId)
  }
  groupSearch.value = ''
}

// 그룹 제거
function removeGroup(groupId: number) {
  selectedGroupIds.value = selectedGroupIds.value.filter(id => id !== groupId)
}

// 드롭다운 닫기
function closeDepartmentDropdown() {
  setTimeout(() => {
    showDepartmentDropdown.value = false
  }, 200)
}

function closeGroupDropdown() {
  setTimeout(() => {
    showGroupDropdown.value = false
  }, 200)
}

// 제출
function handleSubmit() {
  if (!isValid.value || props.loading) return

  if (isEditMode.value) {
    const data: UserUpdateRequest = {
      userName: userName.value.trim(),
      email: email.value.trim() || undefined,
      departmentId: departmentId.value ?? undefined,
      groupIds: selectedGroupIds.value.length > 0 ? selectedGroupIds.value : undefined
    }
    // 비밀번호가 입력된 경우에만 포함
    if (password.value && passwordValidation.value.isValid && passwordsMatch.value) {
      data.password = password.value
    }
    emit('submit', data)
  } else {
    const data: UserCreateRequest = {
      username: username.value.trim(),
      password: password.value,
      passwordConfirm: passwordConfirm.value,
      userName: userName.value.trim(),
      email: email.value.trim() || undefined,
      departmentId: departmentId.value ?? undefined,
      groupIds: selectedGroupIds.value.length > 0 ? selectedGroupIds.value : undefined
    }
    emit('submit', data)
  }
}

// 취소
function handleCancel() {
  resetForm()
  emit('cancel')
}

// 사용자 변경 감지
watch(() => props.user, (newUser) => {
  if (newUser) {
    fillForm(newUser)
  } else {
    resetForm()
  }
}, { immediate: true })

onMounted(() => {
  loadDepartments()
  loadGroups()
})

// 외부에서 폼 초기화 가능하도록 expose
defineExpose({ resetForm })
</script>

<template>
  <div class="user-form">
    <div class="space-y-4">
      <!-- 사용자 이름 -->
      <Input
        v-model="userName"
        label="사용자 이름"
        placeholder="이름을 입력하세요"
        :required="true"
        :maxlength="50"
        :disabled="loading"
      />

      <!-- 이메일 -->
      <Input
        v-model="email"
        label="이메일"
        type="email"
        placeholder="example@email.com"
        :maxlength="100"
        :disabled="loading"
        :error="!emailValidation.isValid && email.length > 0"
        :error-message="emailValidation.message"
      />

      <!-- 아이디 -->
      <div>
        <Input
          v-model="username"
          label="아이디"
          placeholder="영문+숫자 4~20자"
          :required="!isEditMode"
          :maxlength="20"
          :disabled="loading || isEditMode"
          :error="!usernameValidation.isValid && username.length > 0"
          :error-message="usernameValidation.message"
        />
        <p v-if="isEditMode" class="mt-1 text-xs text-gray-500">
          아이디는 변경할 수 없습니다.
        </p>
      </div>

      <!-- 비밀번호 -->
      <div>
        <div class="flex items-center justify-between mb-1">
          <label class="block text-[13px] font-medium text-gray-700">
            비밀번호
            <span v-if="!isEditMode" class="text-red-500 ml-0.5">*</span>
          </label>
        </div>
        <div class="relative">
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="isEditMode ? '변경 시에만 입력' : '비밀번호를 입력하세요'"
            class="input-field pr-10"
            :class="{ 'border-red-500': password && !passwordValidation.isValid }"
            :disabled="loading"
          />
          <button
            type="button"
            class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            @click="showPassword = !showPassword"
          >
            <svg v-if="showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
          </button>
        </div>

        <!-- 비밀번호 정책 표시 -->
        <div v-if="password" class="mt-2 space-y-1">
          <div class="flex items-center gap-2 text-xs"
            :class="passwordValidation.minLength ? 'text-green-600' : 'text-gray-500'">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="passwordValidation.minLength" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            최소 8자 이상
          </div>
          <div class="flex items-center gap-2 text-xs"
            :class="passwordValidation.hasUppercase ? 'text-green-600' : 'text-gray-500'">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="passwordValidation.hasUppercase" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            영문 대문자 포함
          </div>
          <div class="flex items-center gap-2 text-xs"
            :class="passwordValidation.hasLowercase ? 'text-green-600' : 'text-gray-500'">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="passwordValidation.hasLowercase" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            영문 소문자 포함
          </div>
          <div class="flex items-center gap-2 text-xs"
            :class="passwordValidation.hasNumber ? 'text-green-600' : 'text-gray-500'">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="passwordValidation.hasNumber" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            숫자 포함
          </div>
          <div class="flex items-center gap-2 text-xs"
            :class="passwordValidation.hasSpecial ? 'text-green-600' : 'text-gray-500'">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="passwordValidation.hasSpecial" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
            특수문자 포함 (!@#$%^&*)
          </div>
        </div>
      </div>

      <!-- 비밀번호 확인 -->
      <div v-if="password || !isEditMode">
        <label class="block text-[13px] font-medium text-gray-700 mb-1">
          비밀번호 확인
          <span v-if="!isEditMode" class="text-red-500 ml-0.5">*</span>
        </label>
        <div class="relative">
          <input
            v-model="passwordConfirm"
            :type="showPasswordConfirm ? 'text' : 'password'"
            placeholder="비밀번호를 다시 입력하세요"
            class="input-field pr-10"
            :class="{ 'border-red-500': passwordConfirm && !passwordsMatch }"
            :disabled="loading"
          />
          <button
            type="button"
            class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            @click="showPasswordConfirm = !showPasswordConfirm"
          >
            <svg v-if="showPasswordConfirm" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
          </button>
        </div>
        <p v-if="passwordConfirm && !passwordsMatch" class="mt-1 text-xs text-red-500">
          비밀번호가 일치하지 않습니다.
        </p>
      </div>

      <!-- 소속 부서 -->
      <div class="form-field">
        <label class="block text-[13px] font-medium text-gray-700 mb-1">소속 부서</label>
        <div class="relative">
          <div
            class="select-trigger"
            :class="{ 'disabled': loading || loadingDepartments }"
            @click="!loading && !loadingDepartments && (showDepartmentDropdown = !showDepartmentDropdown)"
          >
            <span v-if="selectedDepartmentName" class="text-gray-900">{{ selectedDepartmentName }}</span>
            <span v-else class="text-gray-400">부서 선택</span>
            <div class="flex items-center gap-1">
              <button
                v-if="departmentId"
                type="button"
                class="clear-btn"
                @click.stop="clearDepartment"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
              <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </div>
          </div>

          <!-- 부서 드롭다운 -->
          <div v-if="showDepartmentDropdown" class="dropdown">
            <div class="dropdown-search">
              <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                v-model="departmentSearch"
                type="text"
                class="dropdown-search-input"
                placeholder="부서 검색..."
                @blur="closeDepartmentDropdown"
              />
            </div>
            <div class="dropdown-list">
              <button
                type="button"
                class="dropdown-item"
                @mousedown.prevent="selectDepartment(null)"
              >
                <span class="text-gray-400">(선택 안함)</span>
              </button>
              <button
                v-for="dept in filteredDepartmentOptions"
                :key="dept.id"
                type="button"
                class="dropdown-item"
                :class="{ 'selected': departmentId === dept.id }"
                :style="{ paddingLeft: `${dept.depth * 16 + 12}px` }"
                @mousedown.prevent="selectDepartment(dept.id)"
              >
                <span v-if="dept.depth > 0" class="tree-indent">└</span>
                {{ dept.name }}
                <svg v-if="departmentId === dept.id" class="w-4 h-4 ml-auto text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 소속 그룹 -->
      <div class="form-field">
        <label class="block text-[13px] font-medium text-gray-700 mb-1">소속 그룹</label>

        <!-- 선택된 그룹 태그 -->
        <div v-if="selectedGroups.length > 0" class="tags-container">
          <span
            v-for="group in selectedGroups"
            :key="group.groupId"
            class="tag"
            :style="{
              backgroundColor: group.groupColor ? `${group.groupColor}20` : '#E5E7EB',
              borderColor: group.groupColor || '#9CA3AF'
            }"
          >
            <span
              class="tag-dot"
              :style="{ backgroundColor: group.groupColor || '#9CA3AF' }"
            />
            {{ group.groupName }}
            <button
              type="button"
              class="tag-remove"
              @click="removeGroup(group.groupId)"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </span>
        </div>

        <!-- 그룹 선택 입력 -->
        <div class="relative">
          <div
            class="select-trigger"
            :class="{ 'disabled': loading || loadingGroups }"
            @click="!loading && !loadingGroups && (showGroupDropdown = !showGroupDropdown)"
          >
            <span class="text-gray-400">그룹 추가...</span>
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
          </div>

          <!-- 그룹 드롭다운 -->
          <div v-if="showGroupDropdown" class="dropdown">
            <div class="dropdown-search">
              <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                v-model="groupSearch"
                type="text"
                class="dropdown-search-input"
                placeholder="그룹 검색..."
                @blur="closeGroupDropdown"
              />
            </div>
            <div class="dropdown-list">
              <div v-if="filteredGroupOptions.length === 0" class="dropdown-empty">
                {{ groupSearch ? '검색 결과가 없습니다.' : '추가할 그룹이 없습니다.' }}
              </div>
              <button
                v-for="group in filteredGroupOptions"
                :key="group.groupId"
                type="button"
                class="dropdown-item group-item"
                @mousedown.prevent="addGroup(group.groupId)"
              >
                <span
                  class="group-color"
                  :style="{ backgroundColor: group.groupColor || '#9CA3AF' }"
                />
                <span class="group-name">{{ group.groupName }}</span>
                <span class="group-code">{{ group.groupCode }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 버튼 영역 -->
      <div class="flex items-center gap-2 pt-2">
        <button
          type="button"
          class="btn-primary flex-1"
          :disabled="!isValid || loading"
          @click="handleSubmit"
        >
          <span v-if="loading" class="flex items-center justify-center gap-2">
            <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            처리중...
          </span>
          <span v-else>{{ submitButtonText }}</span>
        </button>

        <button
          v-if="isEditMode"
          type="button"
          class="btn-secondary"
          :disabled="loading"
          @click="handleCancel"
        >
          취소
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-form {
  @apply p-4 bg-white rounded-lg border border-gray-200;
}

.form-field {
  @apply space-y-1;
}

.input-field {
  @apply w-full px-3 py-1.5 text-[13px] h-8 rounded border border-gray-300
         focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
         placeholder:text-gray-400 transition-all duration-150;
}

.input-field:disabled {
  @apply bg-gray-100 cursor-not-allowed text-gray-500;
}

.select-trigger {
  @apply w-full h-8 px-3 flex items-center justify-between
         text-[13px] rounded border border-gray-300 bg-white cursor-pointer
         hover:border-gray-400 transition-colors;
}

.select-trigger.disabled {
  @apply bg-gray-100 cursor-not-allowed text-gray-500;
}

.clear-btn {
  @apply p-0.5 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-600;
}

.dropdown {
  @apply absolute z-20 mt-1 w-full bg-white rounded-lg shadow-lg border border-gray-200;
}

.dropdown-search {
  @apply flex items-center gap-2 px-3 py-2 border-b border-gray-100;
}

.dropdown-search-input {
  @apply flex-1 text-sm outline-none placeholder:text-gray-400;
}

.dropdown-list {
  @apply max-h-48 overflow-y-auto py-1;
}

.dropdown-item {
  @apply w-full flex items-center gap-2 px-3 py-2 text-sm text-left
         hover:bg-gray-50 transition-colors;
}

.dropdown-item.selected {
  @apply bg-primary-50 text-primary-700;
}

.dropdown-empty {
  @apply px-3 py-4 text-sm text-gray-500 text-center;
}

.tree-indent {
  @apply text-gray-300 mr-1;
}

.group-item {
  @apply gap-2;
}

.group-color {
  @apply w-3 h-3 rounded-full flex-shrink-0;
}

.group-name {
  @apply flex-1 truncate;
}

.group-code {
  @apply text-xs text-gray-400;
}

.tags-container {
  @apply flex flex-wrap gap-1.5 mb-2;
}

.tag {
  @apply inline-flex items-center gap-1.5 px-2 py-1 text-xs font-medium
         rounded-md border;
}

.tag-dot {
  @apply w-2 h-2 rounded-full;
}

.tag-remove {
  @apply p-0.5 rounded hover:bg-black/10 text-gray-500 hover:text-gray-700
         transition-colors;
}

.btn-primary {
  @apply px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}

.btn-secondary {
  @apply px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150;
}
</style>
