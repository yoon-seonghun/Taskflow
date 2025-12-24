작성일 : 2025년 12월 18일 
작성자 : 윤성훈 
작성시간 : 22:09
```
  문서 구성

  | 섹션                  | 내용                                                                         |
  |-----------------------|------------------------------------------------------------------------------|
  | 1. 목적               | 요청 기능 개요, 상세 요구사항, 기대 효과                                     |
  | 2. 현재 시스템 상태   | 관련 컴포넌트, 저장 흐름, 코드 분석                                          |
  | 3. 기술적 문제점 분석 | 무한 루프, 신규/수정 구분, 메타데이터 오염 등 5개 이슈                       |
  | 4. 구현 가능성 평가   | 방식별 평가표, 결론                                                          |
  | 5. 대안 제시          | 대안 A/B/C 상세 (DB 스키마, Mapper, Service, Controller, Frontend 코드 포함) |
  | 6. UI 구현 방식 옵션  | 4가지 UI 옵션 상세 설명                                                      |
  | 7. 이력 저장 정책     | 저장 시점, 조건, 보관 정책                                                   |
  | 8. 구현 체크리스트    | 백엔드/프론트엔드/테스트 항목                                                |
  | 9. 참고 자료          | 관련 파일, 유사 기능 참조                                                    |
  | 10. 결론 및 권장사항  | 최종 결론, 우선순위, 예상 작업량                                             |
---
  대안 B (별도 이력 테이블) 구현 시 필요한 모든 코드 (SQL, Java, TypeScript, Vue)가 문서에 포함되어 있어 바로 개발에 착수할 수 있습니다.
```
# 기술 검토 문서

| 항목     | 내용                                         |
| -------- | -------------------------------------------- |
| 문서번호 | TECH_REVIEW_001                              |
| 제목     | 마크다운 에디터 작성자/수정자 자동 추적 기능 |
| 작성일   | 2024-12-18                                   |
| 상태     | 검토 완료 (구현 보류)                        |
| 검토자   | Claude Code                                  |

---

## 1. 목적

### 1.1 요청 기능 개요

마크다운 에디터에서 내용 작성/수정 시 작성자 정보를 자동으로 삽입하여 협업 시 누가 어떤 내용을 작성/수정했는지 추적할 수 있도록 함.

### 1.2 상세 요구사항

#### 신규 내용 추가 시

```
추가된 내용 바로 윗줄에 자동 삽입:
[작성자 : {이름}, 작성날짜 : {Date}, 작성시간 : {Time}]
```

#### 기존 내용 수정 시

```
변경된 문장 끝에 (줄바꿈 감지 시) 자동 삽입:
[수정자 : {이름}, 수정날짜 : {Date}, 수정시간 : {Time}]
```

### 1.3 기대 효과

- 협업 시 작성자 명확화
- 변경 이력 추적 용이
- 책임 소재 명확화

---

## 2. 현재 시스템 상태

### 2.1 관련 컴포넌트

| 파일                  | 역할            | 주요 기능                              |
| --------------------- | --------------- | -------------------------------------- |
| `MarkdownEditor.vue`  | 마크다운 에디터 | 디바운스 자동 저장 (1000ms), blur 저장 |
| `ItemDetailPanel.vue` | 슬라이드 패널   | `handleContentSave()` 호출             |
| `stores/auth.ts`      | 인증 스토어     | `currentUserName` getter 제공          |
| `stores/item.ts`      | 아이템 스토어   | `updateItem()` API 호출                |

### 2.2 현재 저장 흐름

```
사용자 입력
    ↓
handleInput() [MarkdownEditor.vue:57-69]
    ↓
디바운스 타이머 (1000ms)
    ↓
emit('save', localValue)
    ↓
handleContentSave() [ItemDetailPanel.vue:193-214]
    ↓
itemStore.updateItem({ description })
    ↓
PUT /api/boards/{boardId}/items/{itemId}
```

### 2.3 자동 저장 코드 (MarkdownEditor.vue)

```typescript
// 입력 처리 (디바운스 자동 저장)
function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement;
  localValue.value = target.value;
  emit("update:modelValue", target.value);

  // 디바운스 저장
  if (saveTimer.value) {
    clearTimeout(saveTimer.value);
  }
  saveTimer.value = setTimeout(() => {
    emit("save", localValue.value);
  }, props.autoSaveDelay); // 기본값: 1000ms
}

// blur 시 즉시 저장
function handleBlur() {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value);
    saveTimer.value = null;
  }
  emit("save", localValue.value);
}
```

---

## 3. 기술적 문제점 분석

### 3.1 무한 루프 문제 (Critical)

**위험도: 치명적**

현재 자동 저장 방식에서 메타데이터 삽입 시 무한 루프 발생:

```
정상 흐름:
타이핑 → 1초 후 저장 → 다음 타이핑 → 1초 후 저장...

메타데이터 삽입 시:
타이핑
  → 메타데이터 삽입 (텍스트 변경)
    → 변경 감지
      → 다시 메타데이터 삽입
        → 변경 감지
          → 무한 반복...
```

**영향**: 브라우저 정지, 무한 API 호출, 서버 부하

### 3.2 신규/수정 구분 불가 (Critical)

**위험도: 치명적**

줄 단위로 "추가"와 "수정"을 구분하려면:

```
필요 요소:
1. 원본 텍스트 보관 (어느 시점 기준?)
2. 줄 단위 diff 알고리즘
3. 실시간 변경 추적

예시 문제:
원본: "오늘 회의 진행"
변경: "오늘 중요한 회의 진행 예정"

질문:
- "중요한"은 추가? 수정?
- " 예정"은 추가? 수정?
- 어디까지가 한 "수정"인가?
```

**구현 난이도**: diff 알고리즘 복잡도 O(n\*m), 정확도 보장 어려움

### 3.3 자동 저장 빈도 문제 (Major)

**위험도: 높음**

```
"안녕하세요" 입력 시나리오:

00:00.000 - "안" 입력
00:01.000 - 저장 + 메타데이터 삽입
00:01.500 - "녕" 입력
00:02.500 - 저장 + 메타데이터 삽입
...

결과: 한 문장에 메타데이터 5개 이상 중복
```

### 3.4 메타데이터 오염 (Major)

**위험도: 높음**

```
1차 저장 후:
[작성자: 홍길동, 2024-12-18]
안녕하세요

2차 수정 시:
- "[작성자: 홍길동...]" 문장 자체가 "내용"으로 인식
- 메타데이터에도 수정 메타데이터 추가
- 결과: 메타데이터 위에 메타데이터 누적

최종 상태:
[수정자: 김철수, 2024-12-18]
[작성자: 홍길동, 2024-12-18]
[수정자: 김철수, 2024-12-18]  ← 중복
안녕하세요
```

### 3.5 커서 위치 혼란 (Minor)

**위험도: 보통**

```
타이핑 중:
"오늘 회의|" (| = 커서)

메타데이터 삽입 후:
"[작성자: 홍길동...]
오늘 회의|"

사용자 경험: 갑자기 커서 위 텍스트 변경 → 혼란
```

---

## 4. 구현 가능성 평가

### 4.1 방식별 평가표

| 방식                         | 구현 가능성 |  위험도   | 권장 |
| ---------------------------- | :---------: | :-------: | :--: |
| 자동 저장 + 자동 메타데이터  |   ❌ 불가   |  치명적   |  ❌  |
| 수동 저장 + 자동 메타데이터  |  ⚠️ 어려움  |   높음    |  ❌  |
| 저장 시 전체 메타데이터 갱신 |   ✅ 가능   |   낮음    |  △   |
| 별도 이력 테이블 관리        |   ✅ 가능   | 매우 낮음 |  ✅  |

### 4.2 결론

**원본 요청 방식(줄 단위 자동 삽입)은 기술적으로 구현 불가**

주요 사유:

1. 자동 저장과의 충돌로 무한 루프 발생
2. 줄 단위 추가/수정 구분 알고리즘 복잡도 높음
3. 메타데이터가 내용을 오염시켜 가독성 저하
4. 오류 가능성 높음

---

## 5. 대안 제시

### 5.1 대안 A: 마지막 수정자 표시 (간단)

#### 개요

저장 시 description 맨 위에 메타데이터 한 줄만 유지 (갱신 방식)

#### 표시 형식

```markdown
> 최종 수정: 홍길동 | 2024-12-18 21:00

---

(본문 내용)
```

#### 구현 방식

```typescript
function addMetadata(content: string, userName: string): string {
  const now = new Date();
  const dateStr = now.toLocaleDateString("ko-KR");
  const timeStr = now.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });

  const metadata = `> 최종 수정: ${userName} | ${dateStr} ${timeStr}\n---\n\n`;

  // 기존 메타데이터 제거 후 새로 추가
  const cleanContent = content.replace(/^> 최종 수정:.*\n---\n\n/m, "");
  return metadata + cleanContent;
}
```

#### 장단점

| 장점                | 단점                  |
| ------------------- | --------------------- |
| 구현 간단           | 이전 수정자 정보 유실 |
| 무한 루프 방지 가능 | 상세 이력 추적 불가   |
| 기존 코드 최소 수정 | -                     |

---

### 5.2 대안 B: 별도 이력 테이블 (권장)

#### 개요

수정 이력을 별도 테이블에 저장하고, 에디터 내용은 순수하게 유지

#### 5.2.1 데이터베이스 설계

```sql
-- 내용 수정 이력 테이블
CREATE TABLE TB_ITEM_CONTENT_HISTORY (
    HISTORY_ID      BIGINT          PRIMARY KEY AUTO_INCREMENT,
    ITEM_ID         BIGINT          NOT NULL,
    BOARD_ID        BIGINT          NOT NULL,
    EDITOR_ID       BIGINT          NOT NULL,      -- 수정자 ID
    EDITED_AT       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHANGE_TYPE     VARCHAR(10)     NOT NULL,      -- 'CREATE', 'UPDATE'
    CONTENT_BEFORE  TEXT            NULL,          -- 수정 전 전체 내용
    CONTENT_AFTER   TEXT            NOT NULL,      -- 수정 후 전체 내용
    DIFF_SUMMARY    TEXT            NULL,          -- diff 요약 (선택)

    -- 인덱스
    INDEX IDX_CONTENT_HISTORY_ITEM (ITEM_ID, EDITED_AT DESC),
    INDEX IDX_CONTENT_HISTORY_EDITOR (EDITOR_ID, EDITED_AT DESC),
    INDEX IDX_CONTENT_HISTORY_BOARD (BOARD_ID, EDITED_AT DESC),

    -- 외래키
    CONSTRAINT FK_CONTENT_HISTORY_ITEM
        FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID) ON DELETE CASCADE,
    CONSTRAINT FK_CONTENT_HISTORY_EDITOR
        FOREIGN KEY (EDITOR_ID) REFERENCES TB_USER(USER_ID),
    CONSTRAINT FK_CONTENT_HISTORY_BOARD
        FOREIGN KEY (BOARD_ID) REFERENCES TB_BOARD(BOARD_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='아이템 내용 수정 이력';
```

#### 5.2.2 Domain 클래스

```java
// domain/ItemContentHistory.java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemContentHistory {
    private Long historyId;
    private Long itemId;
    private Long boardId;
    private Long editorId;
    private LocalDateTime editedAt;
    private String changeType;      // CREATE, UPDATE
    private String contentBefore;
    private String contentAfter;
    private String diffSummary;

    // 조회용 추가 필드
    private String editorName;
}
```

#### 5.2.3 Mapper 인터페이스

```java
// mapper/ItemContentHistoryMapper.java
@Mapper
public interface ItemContentHistoryMapper {

    // 이력 저장
    int insert(ItemContentHistory history);

    // 아이템별 이력 조회
    List<ItemContentHistory> findByItemId(
        @Param("itemId") Long itemId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    // 보드별 이력 조회 (관리자용)
    List<ItemContentHistory> findByBoardId(
        @Param("boardId") Long boardId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    // 사용자별 이력 조회
    List<ItemContentHistory> findByEditorId(
        @Param("editorId") Long editorId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    // 이력 개수
    int countByItemId(@Param("itemId") Long itemId);

    // 가장 최근 이력 조회
    ItemContentHistory findLatestByItemId(@Param("itemId") Long itemId);
}
```

#### 5.2.4 Mapper XML

```xml
<!-- mapper/ItemContentHistoryMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.taskflow.mapper.ItemContentHistoryMapper">

    <resultMap id="historyResultMap" type="com.taskflow.domain.ItemContentHistory">
        <id property="historyId" column="HISTORY_ID"/>
        <result property="itemId" column="ITEM_ID"/>
        <result property="boardId" column="BOARD_ID"/>
        <result property="editorId" column="EDITOR_ID"/>
        <result property="editedAt" column="EDITED_AT"/>
        <result property="changeType" column="CHANGE_TYPE"/>
        <result property="contentBefore" column="CONTENT_BEFORE"/>
        <result property="contentAfter" column="CONTENT_AFTER"/>
        <result property="diffSummary" column="DIFF_SUMMARY"/>
        <result property="editorName" column="EDITOR_NAME"/>
    </resultMap>

    <insert id="insert" useGeneratedKeys="true" keyProperty="historyId">
        INSERT INTO TB_ITEM_CONTENT_HISTORY (
            ITEM_ID, BOARD_ID, EDITOR_ID, CHANGE_TYPE,
            CONTENT_BEFORE, CONTENT_AFTER, DIFF_SUMMARY
        ) VALUES (
            #{itemId}, #{boardId}, #{editorId}, #{changeType},
            #{contentBefore}, #{contentAfter}, #{diffSummary}
        )
    </insert>

    <select id="findByItemId" resultMap="historyResultMap">
        SELECT
            h.HISTORY_ID,
            h.ITEM_ID,
            h.BOARD_ID,
            h.EDITOR_ID,
            h.EDITED_AT,
            h.CHANGE_TYPE,
            h.CONTENT_BEFORE,
            h.CONTENT_AFTER,
            h.DIFF_SUMMARY,
            u.NAME AS EDITOR_NAME
        FROM TB_ITEM_CONTENT_HISTORY h
        LEFT JOIN TB_USER u ON h.EDITOR_ID = u.USER_ID
        WHERE h.ITEM_ID = #{itemId}
        ORDER BY h.EDITED_AT DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <select id="findByBoardId" resultMap="historyResultMap">
        SELECT
            h.HISTORY_ID,
            h.ITEM_ID,
            h.BOARD_ID,
            h.EDITOR_ID,
            h.EDITED_AT,
            h.CHANGE_TYPE,
            h.CONTENT_BEFORE,
            h.CONTENT_AFTER,
            h.DIFF_SUMMARY,
            u.NAME AS EDITOR_NAME
        FROM TB_ITEM_CONTENT_HISTORY h
        LEFT JOIN TB_USER u ON h.EDITOR_ID = u.USER_ID
        WHERE h.BOARD_ID = #{boardId}
        <if test="startDate != null">
            AND DATE(h.EDITED_AT) >= #{startDate}
        </if>
        <if test="endDate != null">
            AND DATE(h.EDITED_AT) <= #{endDate}
        </if>
        ORDER BY h.EDITED_AT DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <select id="countByItemId" resultType="int">
        SELECT COUNT(*) FROM TB_ITEM_CONTENT_HISTORY
        WHERE ITEM_ID = #{itemId}
    </select>

    <select id="findLatestByItemId" resultMap="historyResultMap">
        SELECT
            h.HISTORY_ID,
            h.ITEM_ID,
            h.BOARD_ID,
            h.EDITOR_ID,
            h.EDITED_AT,
            h.CHANGE_TYPE,
            h.CONTENT_BEFORE,
            h.CONTENT_AFTER,
            h.DIFF_SUMMARY,
            u.NAME AS EDITOR_NAME
        FROM TB_ITEM_CONTENT_HISTORY h
        LEFT JOIN TB_USER u ON h.EDITOR_ID = u.USER_ID
        WHERE h.ITEM_ID = #{itemId}
        ORDER BY h.EDITED_AT DESC
        LIMIT 1
    </select>

</mapper>
```

#### 5.2.5 Service 계층

```java
// service/ItemContentHistoryService.java
public interface ItemContentHistoryService {
    void saveHistory(Long itemId, Long boardId, Long editorId,
                     String contentBefore, String contentAfter);
    PageResponse<ItemContentHistoryResponse> getHistoryByItemId(
        Long itemId, int page, int size);
    PageResponse<ItemContentHistoryResponse> getHistoryByBoardId(
        Long boardId, LocalDate startDate, LocalDate endDate, int page, int size);
}

// service/impl/ItemContentHistoryServiceImpl.java
@Service
@RequiredArgsConstructor
public class ItemContentHistoryServiceImpl implements ItemContentHistoryService {

    private final ItemContentHistoryMapper historyMapper;

    @Override
    @Transactional
    public void saveHistory(Long itemId, Long boardId, Long editorId,
                           String contentBefore, String contentAfter) {
        // 내용이 실제로 변경된 경우만 저장
        if (Objects.equals(contentBefore, contentAfter)) {
            return;
        }

        String changeType = (contentBefore == null || contentBefore.isEmpty())
            ? "CREATE" : "UPDATE";

        // diff 요약 생성 (선택적)
        String diffSummary = generateDiffSummary(contentBefore, contentAfter);

        ItemContentHistory history = ItemContentHistory.builder()
            .itemId(itemId)
            .boardId(boardId)
            .editorId(editorId)
            .changeType(changeType)
            .contentBefore(contentBefore)
            .contentAfter(contentAfter)
            .diffSummary(diffSummary)
            .build();

        historyMapper.insert(history);
    }

    private String generateDiffSummary(String before, String after) {
        if (before == null || before.isEmpty()) {
            return "최초 작성";
        }

        // 간단한 통계 기반 요약
        String[] beforeLines = before.split("\n");
        String[] afterLines = after.split("\n");

        int addedLines = Math.max(0, afterLines.length - beforeLines.length);
        int removedLines = Math.max(0, beforeLines.length - afterLines.length);

        StringBuilder summary = new StringBuilder();
        if (addedLines > 0) {
            summary.append("+").append(addedLines).append("줄 ");
        }
        if (removedLines > 0) {
            summary.append("-").append(removedLines).append("줄 ");
        }
        if (addedLines == 0 && removedLines == 0) {
            summary.append("내용 수정");
        }

        return summary.toString().trim();
    }

    @Override
    public PageResponse<ItemContentHistoryResponse> getHistoryByItemId(
            Long itemId, int page, int size) {
        int offset = page * size;
        List<ItemContentHistory> histories =
            historyMapper.findByItemId(itemId, offset, size);
        int total = historyMapper.countByItemId(itemId);

        List<ItemContentHistoryResponse> content = histories.stream()
            .map(ItemContentHistoryResponse::from)
            .collect(Collectors.toList());

        return PageResponse.<ItemContentHistoryResponse>builder()
            .content(content)
            .page(page)
            .size(size)
            .totalElements(total)
            .totalPages((int) Math.ceil((double) total / size))
            .build();
    }
}
```

#### 5.2.6 Controller

```java
// controller/ItemContentHistoryController.java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemContentHistoryController {

    private final ItemContentHistoryService historyService;

    // 아이템별 내용 수정 이력
    @GetMapping("/items/{itemId}/content-history")
    public ApiResponse<PageResponse<ItemContentHistoryResponse>> getItemHistory(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(historyService.getHistoryByItemId(itemId, page, size));
    }

    // 보드별 내용 수정 이력 (관리자용)
    @GetMapping("/boards/{boardId}/content-history")
    public ApiResponse<PageResponse<ItemContentHistoryResponse>> getBoardHistory(
            @PathVariable Long boardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
            historyService.getHistoryByBoardId(boardId, startDate, endDate, page, size));
    }
}
```

#### 5.2.7 ItemService 수정

```java
// ItemServiceImpl.java 수정 부분

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemContentHistoryService historyService;
    // ... 기존 의존성

    @Override
    @Transactional
    public Item updateItem(Long boardId, Long itemId, ItemUpdateRequest request, Long userId) {
        Item item = itemMapper.findById(itemId)
            .orElseThrow(() -> new BusinessException("아이템을 찾을 수 없습니다."));

        // description 변경 시 이력 저장
        if (request.getDescription() != null) {
            String contentBefore = item.getDescription();
            String contentAfter = request.getDescription();

            // 이력 저장 (비동기 또는 동기)
            historyService.saveHistory(itemId, boardId, userId, contentBefore, contentAfter);
        }

        // 기존 업데이트 로직...
    }
}
```

#### 5.2.8 프론트엔드 API

```typescript
// api/contentHistory.ts
import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";

export interface ContentHistory {
  historyId: number;
  itemId: number;
  boardId: number;
  editorId: number;
  editorName: string;
  editedAt: string;
  changeType: "CREATE" | "UPDATE";
  contentBefore: string | null;
  contentAfter: string;
  diffSummary: string | null;
}

export const contentHistoryApi = {
  // 아이템별 이력 조회
  getByItemId(itemId: number, page = 0, size = 10) {
    return apiClient.get<ApiResponse<PageResponse<ContentHistory>>>(
      `/items/${itemId}/content-history`,
      { params: { page, size } }
    );
  },

  // 보드별 이력 조회
  getByBoardId(
    boardId: number,
    params?: {
      startDate?: string;
      endDate?: string;
      page?: number;
      size?: number;
    }
  ) {
    return apiClient.get<ApiResponse<PageResponse<ContentHistory>>>(
      `/boards/${boardId}/content-history`,
      { params }
    );
  },
};
```

#### 5.2.9 프론트엔드 이력 컴포넌트

```vue
<!-- components/item/ContentHistoryPanel.vue -->
<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { contentHistoryApi, type ContentHistory } from "@/api/contentHistory";
import { Spinner, EmptyState, Pagination } from "@/components/common";

interface Props {
  itemId: number;
}

const props = defineProps<Props>();

const loading = ref(false);
const histories = ref<ContentHistory[]>([]);
const pagination = ref({
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
});
const expandedId = ref<number | null>(null);

async function loadHistory(page = 0) {
  loading.value = true;
  try {
    const response = await contentHistoryApi.getByItemId(
      props.itemId,
      page,
      pagination.value.size
    );
    if (response.data.success && response.data.data) {
      histories.value = response.data.data.content;
      pagination.value = {
        page: response.data.data.page,
        size: response.data.data.size,
        totalElements: response.data.data.totalElements,
        totalPages: response.data.data.totalPages,
      };
    }
  } finally {
    loading.value = false;
  }
}

function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function toggleExpand(historyId: number) {
  expandedId.value = expandedId.value === historyId ? null : historyId;
}

watch(
  () => props.itemId,
  () => loadHistory(0)
);
onMounted(() => loadHistory());
</script>

<template>
  <div class="h-full flex flex-col">
    <!-- 로딩 -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <Spinner />
    </div>

    <!-- 빈 상태 -->
    <template v-else-if="histories.length === 0">
      <EmptyState
        title="수정 이력이 없습니다"
        description="내용이 수정되면 이력이 기록됩니다."
        icon="document"
        class="flex-1"
      />
    </template>

    <!-- 이력 목록 -->
    <template v-else>
      <div class="flex-1 overflow-auto space-y-3 p-2">
        <div
          v-for="history in histories"
          :key="history.historyId"
          class="bg-white border border-gray-200 rounded-lg overflow-hidden"
        >
          <!-- 헤더 -->
          <div
            class="flex items-center justify-between px-3 py-2 bg-gray-50 cursor-pointer hover:bg-gray-100"
            @click="toggleExpand(history.historyId)"
          >
            <div class="flex items-center gap-2">
              <span
                class="px-1.5 py-0.5 text-[11px] rounded"
                :class="
                  history.changeType === 'CREATE'
                    ? 'bg-green-100 text-green-700'
                    : 'bg-blue-100 text-blue-700'
                "
              >
                {{ history.changeType === "CREATE" ? "작성" : "수정" }}
              </span>
              <span class="text-[13px] font-medium text-gray-900">
                {{ history.editorName }}
              </span>
              <span class="text-[12px] text-gray-500">
                {{ formatDateTime(history.editedAt) }}
              </span>
            </div>
            <div class="flex items-center gap-2">
              <span
                v-if="history.diffSummary"
                class="text-[11px] text-gray-500"
              >
                {{ history.diffSummary }}
              </span>
              <svg
                class="w-4 h-4 text-gray-400 transition-transform"
                :class="{ 'rotate-180': expandedId === history.historyId }"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </div>
          </div>

          <!-- 상세 (확장 시) -->
          <div
            v-if="expandedId === history.historyId"
            class="border-t border-gray-200"
          >
            <!-- 수정 전 -->
            <div
              v-if="history.contentBefore"
              class="p-3 border-b border-gray-100"
            >
              <div class="text-[11px] text-red-600 font-medium mb-1">
                수정 전
              </div>
              <pre
                class="text-[12px] text-gray-600 whitespace-pre-wrap bg-red-50 p-2 rounded"
                >{{ history.contentBefore }}</pre
              >
            </div>
            <!-- 수정 후 -->
            <div class="p-3">
              <div class="text-[11px] text-green-600 font-medium mb-1">
                {{ history.changeType === "CREATE" ? "작성 내용" : "수정 후" }}
              </div>
              <pre
                class="text-[12px] text-gray-600 whitespace-pre-wrap bg-green-50 p-2 rounded"
                >{{ history.contentAfter }}</pre
              >
            </div>
          </div>
        </div>
      </div>

      <!-- 페이지네이션 -->
      <div
        v-if="pagination.totalPages > 1"
        class="flex-shrink-0 border-t border-gray-200 p-2"
      >
        <Pagination
          :current-page="pagination.page"
          :total-pages="pagination.totalPages"
          :total-elements="pagination.totalElements"
          size="sm"
          @page-change="loadHistory"
        />
      </div>
    </template>
  </div>
</template>
```

---

### 5.3 대안 C: 댓글 시스템 활용

#### 개요

내용 수정 시 자동으로 시스템 댓글 생성

#### 구현 방식

```typescript
// 내용 저장 시
async function handleContentSave(description: string) {
  // 기존 저장 로직...

  // 자동 댓글 생성
  await commentApi.createComment(item.value.itemId, {
    content: `[시스템] ${currentUserName}님이 내용을 수정했습니다.`,
    isSystem: true,
  });
}
```

#### 장단점

| 장점                    | 단점                           |
| ----------------------- | ------------------------------ |
| 기존 댓글 시스템 재활용 | 댓글 목록이 길어짐             |
| 구현 간단               | 시스템 댓글과 사용자 댓글 혼재 |
| 알림 연동 용이          | 상세 diff 보기 어려움          |

---

## 6. UI 구현 방식 옵션

### 6.1 옵션 A: 슬라이드 패널 탭 추가

```
┌─────────────────────────────────────────────────┐
│  업무 상세                                 [X]  │
├─────────────────────────────────────────────────┤
│  [속성]  [내용]  [댓글]  [이력] ← 새 탭        │
├─────────────────────────────────────────────────┤
│                                                 │
│  (이력 목록 표시)                               │
│                                                 │
└─────────────────────────────────────────────────┘
```

**적합한 경우**: 개별 업무의 이력을 바로 확인하고 싶을 때

### 6.2 옵션 B: 이력관리 메뉴 탭 추가

```
┌─────────────────────────────────────────────────┐
│  이력관리                                       │
├─────────────────────────────────────────────────┤
│  [작업 처리 이력] | [작업 등록 이력] | [내용 수정 이력] │
├─────────────────────────────────────────────────┤
│                                                 │
│  (전체 수정 이력 목록)                          │
│                                                 │
└─────────────────────────────────────────────────┘
```

**적합한 경우**: 관리자가 전체 수정 내역을 모니터링할 때

### 6.3 옵션 C: 복합 방식 (권장)

```
개별 업무 확인: 슬라이드 패널 → [이력] 탭
전체 모니터링: 이력관리 메뉴 → [내용 수정 이력] 탭
```

**적합한 경우**: 완전한 이력 관리 시스템이 필요할 때

### 6.4 옵션 D: 에디터 내 드롭다운

```
┌─────────────────────────────────────────────────┐
│  [편집] [미리보기]              [📜 이력 (3)]   │
├─────────────────────────────────────────────────┤
│                                 ┌─────────────┐ │
│  에디터 영역                    │ 이력 목록   │ │
│                                 └─────────────┘ │
└─────────────────────────────────────────────────┘
```

**적합한 경우**: 에디터와 밀접하게 연결된 빠른 이력 확인

---

## 7. 이력 저장 정책

### 7.1 저장 시점

| 이벤트                   | 저장 여부 | 이유                          |
| ------------------------ | :-------: | ----------------------------- |
| 자동 저장 (1초 디바운스) |    ❌     | 너무 빈번, 불필요한 이력 생성 |
| blur 저장                |    ✅     | 에디터 포커스 잃을 때 저장    |
| 패널 닫기                |    ✅     | 최종 저장 보장                |
| 수동 저장 버튼 (선택)    |    ✅     | 명시적 저장                   |

### 7.2 저장 조건

```java
// 이력 저장 조건
boolean shouldSaveHistory =
    request.getDescription() != null &&                    // description 필드 존재
    !Objects.equals(contentBefore, contentAfter) &&        // 내용 변경됨
    (contentAfter != null && !contentAfter.trim().isEmpty()); // 빈 내용 아님
```

### 7.3 이력 보관 정책 (선택)

```sql
-- 90일 이상 된 이력 자동 삭제 (선택적 구현)
DELETE FROM TB_ITEM_CONTENT_HISTORY
WHERE EDITED_AT < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

---

## 8. 구현 체크리스트

### 8.1 백엔드

- [ ] TB_ITEM_CONTENT_HISTORY 테이블 생성 (01_schema.sql)
- [ ] ItemContentHistory 도메인 클래스 생성
- [ ] ItemContentHistoryMapper 인터페이스 생성
- [ ] ItemContentHistoryMapper.xml 생성
- [ ] ItemContentHistoryService 인터페이스/구현체 생성
- [ ] ItemContentHistoryController 생성
- [ ] ItemServiceImpl에 이력 저장 로직 추가
- [ ] DTO (Request/Response) 생성

### 8.2 프론트엔드

- [ ] contentHistory.ts API 모듈 생성
- [ ] ContentHistory 타입 정의
- [ ] ContentHistoryPanel.vue 컴포넌트 생성
- [ ] ItemDetailPanel.vue에 [이력] 탭 추가
- [ ] (선택) HistoryView.vue에 [내용 수정 이력] 탭 추가

### 8.3 테스트

- [ ] 이력 저장 테스트 (CREATE, UPDATE)
- [ ] 이력 조회 테스트 (아이템별, 보드별)
- [ ] 페이지네이션 테스트
- [ ] 동시 수정 시 이력 충돌 테스트

---

## 9. 참고 자료

### 9.1 관련 파일

| 파일                                                                   | 용도             |
| ---------------------------------------------------------------------- | ---------------- |
| `frontend/src/components/common/MarkdownEditor.vue`                    | 마크다운 에디터  |
| `frontend/src/components/item/ItemDetailPanel.vue`                     | 슬라이드 패널    |
| `frontend/src/stores/auth.ts`                                          | 사용자 정보 접근 |
| `frontend/src/stores/item.ts`                                          | 아이템 상태 관리 |
| `backend/src/main/java/com/taskflow/service/impl/ItemServiceImpl.java` | 아이템 서비스    |

### 9.2 유사 기능 참조

- Google Docs: 버전 기록
- Notion: 페이지 이력
- Confluence: 페이지 버전 비교

---

## 10. 결론 및 권장사항

### 10.1 최종 결론

| 항목        | 내용                                        |
| ----------- | ------------------------------------------- |
| 원본 요청   | 줄 단위 자동 작성자/수정자 삽입             |
| 구현 가능성 | ❌ 불가 (무한 루프, 복잡도)                 |
| 권장 대안   | 대안 B: 별도 이력 테이블                    |
| UI 권장     | 옵션 C: 복합 방식 (패널 탭 + 이력관리 메뉴) |

### 10.2 구현 우선순위

1. **1단계**: TB_ITEM_CONTENT_HISTORY 테이블 + 백엔드 API
2. **2단계**: 슬라이드 패널 [이력] 탭
3. **3단계**: 이력관리 메뉴 [내용 수정 이력] 탭 (선택)

### 10.3 예상 개발 기간

| 단계  | 범위          | 예상 작업량 |
| ----- | ------------- | ----------- |
| 1단계 | DB + Backend  | 중간        |
| 2단계 | Frontend 패널 | 중간        |
| 3단계 | Frontend 메뉴 | 작음        |

---

**문서 끝**
