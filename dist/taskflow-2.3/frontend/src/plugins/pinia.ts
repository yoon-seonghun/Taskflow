/**
 * Pinia 인스턴스
 * - main.ts와 api/client.ts에서 공유
 * - 컴포넌트 외부에서 store 사용 가능
 */
import { createPinia } from 'pinia'

export const pinia = createPinia()
