import { createApp } from 'vue'
import { pinia } from './plugins/pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

/**
 * 앱 마운트 전 테마 초기화
 * - localStorage에서 설정 로드
 * - <html>에 dark 클래스 즉시 적용
 * - 다크모드 전환 시 깜빡임 방지
 */
function initializeTheme() {
  try {
    const saved = localStorage.getItem('taskflow_settings')
    if (saved) {
      const settings = JSON.parse(saved)
      const theme = settings.theme || 'light'

      if (theme === 'dark') {
        document.documentElement.classList.add('dark')
      } else if (theme === 'system') {
        // 시스템 다크모드 설정 확인
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
        if (prefersDark) {
          document.documentElement.classList.add('dark')
        }
      }
    }
  } catch (error) {
    console.error('Failed to initialize theme:', error)
  }
}

// 앱 마운트 전 테마 적용
initializeTheme()

const app = createApp(App)

app.use(pinia)
app.use(router)

app.mount('#app')
