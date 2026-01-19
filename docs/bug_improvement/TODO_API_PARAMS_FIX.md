# Todo API Params 래핑 버그 수정 기록

## 수정일: 2025-01-19

---

## 1. 문제 개요

### 1.1 버그 설명
`todo.ts`에서 API 호출 시 params를 `{ params }` 형태로 한 번 더 감싸서 전달하는 문제

### 1.2 영향 범위
- `getTodos()` - 필터 파라미터 사용 시 동작 불가
- `getCompletedTodos()` - 기간 필터 사용 시 동작 불가
- `getTodoCount()` - userId 파라미터 전달 실패

### 1.3 현재 동작하는 이유
- params 없이 호출하고 있어 `{ params: undefined }`가 전달됨
- axios는 undefined 값을 쿼리스트링에서 자동 제외
- 따라서 현재 기능은 정상 동작하나, 필터 기능 사용 시 문제 발생

---

## 2. 기술적 분석

### 2.1 client.ts의 get 함수
```typescript
export async function get<T>(url: string, params?: object): Promise<ApiResponse<T>> {
  const response = await client.get<ApiResponse<T>>(url, { params })  // 여기서 { params } 감쌈
  return response.data
}
```

### 2.2 문제 코드 (todo.ts)
```typescript
// Line 24
getTodos(params?: {...}) {
  return get<Todo[]>('/todos', { params })  // ❌ 중복 래핑
}

// Line 97
getCompletedTodos(params?: {...}) {
  return get<Todo[]>('/todos/completed', { params })  // ❌ 중복 래핑
}

// Line 118
getTodoCount(userId?: number) {
  return get<TodoCountResponse>('/todos/count', { params: { userId } })  // ❌ 중복 래핑
}
```

### 2.3 결과
```
// getTodos({ includeCompleted: true }) 호출 시
실제 전달: { params: { params: { includeCompleted: true } } }
쿼리스트링: ?params[includeCompleted]=true  (❌ 잘못됨)
기대 결과: ?includeCompleted=true  (✅ 정상)
```

---

## 3. 수정 내역

### 3.1 todo.ts 수정

| 라인 | 변경 전 | 변경 후 |
|------|---------|---------|
| 24 | `return get<Todo[]>('/todos', { params })` | `return get<Todo[]>('/todos', params)` |
| 97 | `return get<Todo[]>('/todos/completed', { params })` | `return get<Todo[]>('/todos/completed', params)` |
| 118 | `return get<TodoCountResponse>('/todos/count', { params: { userId } })` | `return get<TodoCountResponse>('/todos/count', userId !== undefined ? { userId } : undefined)` |

---

## 4. 테스트 체크리스트

### 4.1 기본 기능 테스트
- [ ] Todo 목록 조회 (params 없이)
- [ ] 오늘 마감 Todo 조회
- [ ] 지연 Todo 조회
- [ ] 완료된 Todo 조회 (params 없이)
- [ ] 공유받은 Todo 조회
- [ ] 이관받은 Todo 조회

### 4.2 필터 기능 테스트
- [ ] Todo 목록 - includeCompleted=true 필터
- [ ] Todo 목록 - dueDateFrom/dueDateTo 필터
- [ ] 완료된 Todo - completedDateFrom/completedDateTo 필터
- [ ] Todo 수 조회 - userId 파라미터

### 4.3 CRUD 테스트
- [ ] Todo 생성
- [ ] Todo 수정
- [ ] Todo 완료 토글
- [ ] Todo 삭제

---

## 5. 관련 파일

- `frontend/src/api/todo.ts`
- `frontend/src/api/client.ts`
- `frontend/src/stores/todo.ts`
- `frontend/src/views/TodoView.vue`

---

## 6. 향후 주의사항

다른 API 파일에서 동일한 패턴 사용 여부 점검 필요:
- `calendar.ts` - ✅ 확인됨 (정상)
- `item.ts` - 점검 필요
- `board.ts` - 점검 필요
- `user.ts` - 점검 필요
