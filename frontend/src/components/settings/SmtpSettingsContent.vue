<script setup lang="ts">
/**
 * SMTP 설정 컴포넌트
 * - SMTP 서버 설정
 * - 연결 테스트
 * - 테스트 메일 발송
 */
import { ref, onMounted, computed } from 'vue'
import { configApi, type SmtpConfigItem } from '@/api/config'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

// 로딩 상태
const loading = ref(false)
const saving = ref(false)
const testingConnection = ref(false)
const sendingTestEmail = ref(false)

// SMTP 설정
const smtpConfigs = ref<SmtpConfigItem[]>([])
const editedValues = ref<Record<string, string>>({})
const password = ref('')
const showPassword = ref(false)

// 테스트 메일 수신자
const testEmailRecipient = ref('')

// 설정 표시 순서 및 라벨
const configLabels: Record<string, { label: string; placeholder: string; type?: string }> = {
  HOST: { label: 'SMTP 서버', placeholder: 'mail.example.com' },
  PORT: { label: '포트', placeholder: '587' },
  SECURITY_TYPE: { label: '보안 유형', placeholder: 'STARTTLS' },
  USERNAME: { label: '계정', placeholder: 'user@example.com' },
  PASSWORD: { label: '비밀번호', placeholder: '비밀번호 입력', type: 'password' },
  FROM_ADDRESS: { label: '발신자 주소', placeholder: 'TaskFlow <noreply@example.com>' },
  ADMIN_EMAIL: { label: '관리자 이메일', placeholder: 'admin@example.com' }
}

const configOrder = ['HOST', 'PORT', 'SECURITY_TYPE', 'USERNAME', 'PASSWORD', 'FROM_ADDRESS', 'ADMIN_EMAIL']

// 정렬된 설정 목록
const sortedConfigs = computed(() => {
  const configs: (SmtpConfigItem & { order: number })[] = []
  for (const config of smtpConfigs.value) {
    const order = configOrder.indexOf(config.key)
    configs.push({ ...config, order: order >= 0 ? order : 999 })
  }
  return configs.sort((a, b) => a.order - b.order)
})

// 비밀번호 설정 여부 확인
const isPasswordSet = computed(() => {
  const passwordConfig = smtpConfigs.value.find(c => c.key === 'PASSWORD')
  // 비밀번호가 설정되어 있으면 마스킹된 값(********)이 표시됨
  return passwordConfig?.value === '********'
})

// 보안 유형 옵션
const securityOptions = [
  { value: 'STARTTLS', label: 'STARTTLS (권장)' },
  { value: 'TLS', label: 'TLS/SSL' },
  { value: 'NONE', label: '없음' }
]

// 설정 로드
async function loadConfig() {
  loading.value = true
  try {
    const response = await configApi.getSmtpConfig()
    smtpConfigs.value = response.data?.configs || []

    // 편집 값 초기화
    editedValues.value = {}
    for (const config of smtpConfigs.value) {
      if (!config.encrypted) {
        editedValues.value[config.key] = config.value || ''
      }
    }
  } catch (error) {
    console.error('Failed to load SMTP config:', error)
    uiStore.showError('SMTP 설정을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 설정 저장
async function saveConfig() {
  saving.value = true
  try {
    // 일반 설정 저장
    const configs = Object.entries(editedValues.value)
      .filter(([key]) => key !== 'PASSWORD')
      .map(([key, value]) => ({ key, value }))

    if (configs.length > 0) {
      await configApi.updateSmtpConfig({ configs })
    }

    // 비밀번호 저장 (입력된 경우만)
    if (password.value) {
      await configApi.updateSmtpPassword(password.value)
      password.value = ''
    }

    uiStore.showSuccess('SMTP 설정이 저장되었습니다.')
    await loadConfig()
  } catch (error) {
    console.error('Failed to save SMTP config:', error)
    uiStore.showError('SMTP 설정 저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 연결 테스트
async function testConnection() {
  testingConnection.value = true
  try {
    const result = await configApi.testSmtpConnection()
    if (result.data?.success) {
      uiStore.showSuccess('SMTP 서버 연결 성공!')
    } else {
      uiStore.showError(result.data?.message || 'SMTP 서버 연결 실패')
    }
  } catch (error) {
    console.error('SMTP connection test failed:', error)
    uiStore.showError('SMTP 연결 테스트에 실패했습니다.')
  } finally {
    testingConnection.value = false
  }
}

// 테스트 메일 발송
async function sendTestEmail() {
  if (!testEmailRecipient.value.trim()) {
    uiStore.showWarning('수신자 이메일을 입력해주세요.')
    return
  }

  sendingTestEmail.value = true
  try {
    const result = await configApi.sendTestEmail(testEmailRecipient.value.trim())
    if (result.data?.success) {
      uiStore.showSuccess('테스트 메일이 발송되었습니다.')
    } else {
      uiStore.showError(result.data?.message || '테스트 메일 발송 실패')
    }
  } catch (error) {
    console.error('Test email send failed:', error)
    uiStore.showError('테스트 메일 발송에 실패했습니다.')
  } finally {
    sendingTestEmail.value = false
  }
}

// 마운트 시 설정 로드
onMounted(() => {
  loadConfig()
})
</script>

<template>
  <div class="smtp-settings">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500">SMTP 설정을 불러오는 중...</p>
    </div>

    <div v-else class="space-y-6">
      <!-- 비밀번호 미설정 경고 -->
      <div v-if="!isPasswordSet" class="bg-amber-50 border border-amber-300 rounded-lg p-4">
        <div class="flex gap-3">
          <svg class="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div class="text-sm text-amber-800">
            <p class="font-medium">SMTP 비밀번호가 설정되지 않았습니다</p>
            <p class="mt-1 text-amber-700">
              이메일 발송을 위해 비밀번호를 입력하고 <strong>설정 저장</strong> 버튼을 클릭한 후 연결 테스트를 진행해주세요.
            </p>
          </div>
        </div>
      </div>

      <!-- SMTP 서버 설정 카드 -->
      <div class="settings-card">
        <div class="settings-card-title">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            <span>SMTP 서버 설정</span>
          </div>
        </div>
        <div class="settings-card-content">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <template v-for="config in sortedConfigs" :key="config.key">
              <!-- 비밀번호 필드 -->
              <div v-if="config.key === 'PASSWORD'" class="form-group">
                <label class="form-label">
                  {{ configLabels[config.key]?.label || config.key }}
                  <span v-if="config.encrypted" class="ml-1 text-xs text-amber-600">(암호화됨)</span>
                </label>
                <div class="relative">
                  <input
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    class="form-input pr-10"
                    :placeholder="config.value ? '변경하려면 새 비밀번호 입력' : configLabels[config.key]?.placeholder"
                  />
                  <button
                    type="button"
                    class="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
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
              </div>

              <!-- 보안 유형 선택 -->
              <div v-else-if="config.key === 'SECURITY_TYPE'" class="form-group">
                <label class="form-label">{{ configLabels[config.key]?.label || config.key }}</label>
                <select
                  v-model="editedValues[config.key]"
                  class="form-input"
                >
                  <option v-for="option in securityOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>

              <!-- 일반 텍스트 필드 -->
              <div v-else class="form-group">
                <label class="form-label">{{ configLabels[config.key]?.label || config.key }}</label>
                <input
                  v-model="editedValues[config.key]"
                  type="text"
                  class="form-input"
                  :placeholder="configLabels[config.key]?.placeholder"
                />
              </div>
            </template>
          </div>

          <!-- 저장 버튼 -->
          <div class="mt-6 flex justify-end gap-3">
            <button
              type="button"
              class="btn-secondary"
              :disabled="testingConnection"
              @click="testConnection"
            >
              <svg v-if="testingConnection" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <svg v-else class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              {{ testingConnection ? '테스트 중...' : '연결 테스트' }}
            </button>
            <button
              type="button"
              class="btn-primary"
              :disabled="saving"
              @click="saveConfig"
            >
              <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              {{ saving ? '저장 중...' : '설정 저장' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 테스트 메일 발송 카드 -->
      <div class="settings-card">
        <div class="settings-card-title">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
            <span>테스트 메일 발송</span>
          </div>
        </div>
        <div class="settings-card-content">
          <p class="text-sm text-gray-600 mb-4">
            SMTP 설정이 올바른지 확인하기 위해 테스트 메일을 발송합니다.
          </p>
          <div class="flex gap-3">
            <div class="flex-1">
              <input
                v-model="testEmailRecipient"
                type="email"
                class="form-input"
                placeholder="수신자 이메일 주소"
              />
            </div>
            <button
              type="button"
              class="btn-primary flex-shrink-0"
              :disabled="sendingTestEmail || !testEmailRecipient.trim()"
              @click="sendTestEmail"
            >
              <svg v-if="sendingTestEmail" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <svg v-else class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
              </svg>
              {{ sendingTestEmail ? '발송 중...' : '테스트 메일 발송' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 안내 사항 -->
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div class="flex gap-3">
          <svg class="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div class="text-sm text-blue-700">
            <p class="font-medium mb-1">SMTP 설정 안내</p>
            <ul class="list-disc list-inside space-y-1 text-blue-600">
              <li>STARTTLS는 포트 587에서 주로 사용됩니다.</li>
              <li>TLS/SSL은 포트 465에서 주로 사용됩니다.</li>
              <li>비밀번호는 암호화되어 저장됩니다.</li>
              <li>설정 변경 후 연결 테스트를 권장합니다.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.smtp-settings {
  @apply max-w-3xl;
}

.settings-card {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden
         dark:bg-gray-800 dark:border-gray-700;
}

.settings-card-title {
  @apply px-4 py-3 text-base font-medium text-gray-900 bg-gray-50 border-b border-gray-200
         dark:text-white dark:bg-gray-700 dark:border-gray-600;
}

.settings-card-content {
  @apply p-4;
}

.form-group {
  @apply space-y-1;
}

.form-label {
  @apply block text-sm font-medium text-gray-700 dark:text-gray-300;
}

.form-input {
  @apply w-full px-3 py-2 text-sm border border-gray-300 rounded-lg
         focus:ring-primary-500 focus:border-primary-500 transition-colors
         dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150
         dark:focus:ring-offset-gray-900;
}

.btn-secondary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         disabled:opacity-50 disabled:cursor-not-allowed
         transition-colors duration-150
         dark:text-gray-200 dark:bg-gray-700 dark:border-gray-600 dark:hover:bg-gray-600
         dark:focus:ring-offset-gray-900;
}
</style>
