<script setup lang="ts">
/**
 * 로그인 페이지
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import Button from '@/components/common/Button.vue'
import Input from '@/components/common/Input.vue'

const route = useRoute()
const { login, loginError, isLoggingIn } = useAuth()

// Form State
const username = ref('')
const password = ref('')
const rememberMe = ref(false)

// Validation
const usernameError = ref('')
const passwordError = ref('')

// 세션 만료 메시지
const sessionExpired = computed(() => route.query.expired === 'true')

// Form Validation
function validateForm(): boolean {
  let isValid = true

  // Username validation
  if (!username.value.trim()) {
    usernameError.value = '아이디를 입력해주세요.'
    isValid = false
  } else if (username.value.length < 4) {
    usernameError.value = '아이디는 4자 이상이어야 합니다.'
    isValid = false
  } else {
    usernameError.value = ''
  }

  // Password validation
  if (!password.value) {
    passwordError.value = '비밀번호를 입력해주세요.'
    isValid = false
  } else if (password.value.length < 8) {
    passwordError.value = '비밀번호는 8자 이상이어야 합니다.'
    isValid = false
  } else {
    passwordError.value = ''
  }

  return isValid
}

// Submit Handler
async function handleSubmit() {
  if (!validateForm()) return

  // Remember username if checked
  if (rememberMe.value) {
    localStorage.setItem('savedUsername', username.value.trim())
  } else {
    localStorage.removeItem('savedUsername')
  }

  await login({
    username: username.value.trim(),
    password: password.value
  })
}

// Enter Key Handler
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    handleSubmit()
  }
}

// Load saved username
onMounted(() => {
  const savedUsername = localStorage.getItem('savedUsername')
  if (savedUsername) {
    username.value = savedUsername
    rememberMe.value = true
  }
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100 px-4">
    <div class="w-full max-w-sm">
      <!-- Logo & Title -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-primary-600 rounded-xl mb-4">
          <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">TaskFlow</h1>
        <p class="text-sm text-gray-500 mt-1">업무 관리 시스템</p>
      </div>

      <!-- Login Form -->
      <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <!-- Session Expired Message -->
        <div
          v-if="sessionExpired"
          class="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-700 text-sm"
        >
          세션이 만료되었습니다. 다시 로그인해주세요.
        </div>

        <!-- Error Message -->
        <div
          v-if="loginError"
          class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm"
        >
          {{ loginError }}
        </div>

        <form @submit.prevent="handleSubmit">
          <!-- Username -->
          <div class="mb-4">
            <Input
              v-model="username"
              type="text"
              label="아이디"
              placeholder="아이디를 입력하세요"
              :error="!!usernameError"
              :error-message="usernameError"
              :disabled="isLoggingIn"
              autofocus
              @keydown="handleKeydown"
            />
          </div>

          <!-- Password -->
          <div class="mb-4">
            <Input
              v-model="password"
              type="password"
              label="비밀번호"
              placeholder="비밀번호를 입력하세요"
              :error="!!passwordError"
              :error-message="passwordError"
              :disabled="isLoggingIn"
              @keydown="handleKeydown"
            />
          </div>

          <!-- Remember Me -->
          <div class="mb-6">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="rememberMe"
                type="checkbox"
                class="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
              />
              <span class="text-sm text-gray-600">아이디 저장</span>
            </label>
          </div>

          <!-- Submit Button -->
          <Button
            type="submit"
            variant="primary"
            size="lg"
            block
            :loading="isLoggingIn"
            :disabled="isLoggingIn"
          >
            {{ isLoggingIn ? '로그인 중...' : '로그인' }}
          </Button>
        </form>
      </div>

      <!-- Footer -->
      <p class="text-center text-xs text-gray-400 mt-6">
        &copy; {{ new Date().getFullYear() }} TaskFlow. All rights reserved.
      </p>
    </div>
  </div>
</template>
