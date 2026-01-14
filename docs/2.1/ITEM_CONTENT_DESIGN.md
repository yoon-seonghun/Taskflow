# 업무 내용 분리 및 리치 텍스트 에디터 설계서

## 1. 개요

### 1.1 배경
현재 업무(Item)의 상세 내용은 `TB_ITEM.DESCRIPTION` 컬럼(TEXT)에 단순 텍스트로 저장됩니다.
멀티라인 텍스트로 많은 내용을 담거나 서식이 필요한 경우를 대비하여 구조 개선이 필요합니다.

### 1.2 목표
- 업무 **설명(description)**과 **내용(content)** 분리
- 리치 텍스트 에디터(TipTap) 도입으로 서식 지원
- 목록 조회 성능 최적화 (대용량 컨텐츠 분리)

### 1.3 변경 요약

| 항목 | 용도 | 저장 위치 | 에디터 |
|------|------|-----------|--------|
| **설명 (description)** | 업무 요약/간단 설명 | TB_ITEM.DESCRIPTION (기존) | 단일 라인 텍스트 |
| **내용 (content)** | 상세 내용/문서 | TB_ITEM_CONTENT (신규) | TipTap 리치 텍스트 에디터 |

---

## 2. 데이터베이스 설계

### 2.1 ERD

```
┌─────────────────┐         ┌─────────────────────┐
│    TB_ITEM      │         │   TB_ITEM_CONTENT   │
├─────────────────┤         ├─────────────────────┤
│ ITEM_ID (PK)    │────1:1──│ CONTENT_ID (PK)     │
│ TITLE           │         │ ITEM_ID (FK, UK)    │
│ DESCRIPTION     │         │ CONTENT_TYPE        │
│ STATUS          │         │ CONTENT             │
│ PRIORITY        │         │ PLAIN_TEXT          │
│ ...             │         │ VERSION             │
└─────────────────┘         │ CREATED_AT/BY       │
                            │ UPDATED_AT/BY       │
                            └─────────────────────┘
```

### 2.2 신규 테이블: TB_ITEM_CONTENT

```sql
-- =============================================
-- TB_ITEM_CONTENT: 업무 상세 내용
-- =============================================
CREATE TABLE TB_ITEM_CONTENT (
    CONTENT_ID      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '컨텐츠 ID',
    ITEM_ID         BIGINT          NOT NULL COMMENT '업무 ID (FK → TB_ITEM)',
    CONTENT_TYPE    VARCHAR(20)     NOT NULL DEFAULT 'HTML' COMMENT '컨텐츠 타입 (HTML, JSON, MARKDOWN)',
    CONTENT         MEDIUMTEXT      NULL COMMENT '컨텐츠 본문 (리치 텍스트)',
    PLAIN_TEXT      TEXT            NULL COMMENT '검색용 평문 텍스트',
    VERSION         INT             NOT NULL DEFAULT 1 COMMENT '버전 (동시 편집 충돌 방지)',
    CREATED_AT      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY      VARCHAR(50)     NOT NULL COMMENT '생성자 USERNAME',
    UPDATED_AT      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY      VARCHAR(50)     NULL COMMENT '수정자 USERNAME',
    PRIMARY KEY (CONTENT_ID),
    UNIQUE KEY UK_ITEM_CONTENT (ITEM_ID),
    CONSTRAINT FK_ITEM_CONTENT_ITEM
        FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='업무 상세 내용';

-- 검색용 인덱스
CREATE FULLTEXT INDEX FT_ITEM_CONTENT_PLAIN ON TB_ITEM_CONTENT(PLAIN_TEXT);
```

### 2.3 컬럼 상세 정의

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|------|------|------|--------|------|
| CONTENT_ID | BIGINT | NO | AUTO_INCREMENT | PK |
| ITEM_ID | BIGINT | NO | - | FK → TB_ITEM, UNIQUE |
| CONTENT_TYPE | VARCHAR(20) | NO | 'HTML' | HTML, JSON, MARKDOWN |
| CONTENT | MEDIUMTEXT | YES | NULL | 리치 텍스트 본문 (~16MB) |
| PLAIN_TEXT | TEXT | YES | NULL | 검색/미리보기용 평문 (~64KB) |
| VERSION | INT | NO | 1 | 동시 편집 충돌 방지용 |
| CREATED_AT | DATETIME | NO | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | VARCHAR(50) | NO | - | 생성자 USERNAME |
| UPDATED_AT | DATETIME | YES | ON UPDATE | 수정일시 |
| UPDATED_BY | VARCHAR(50) | YES | - | 수정자 USERNAME |

### 2.4 기존 테이블 변경: TB_ITEM

```sql
-- DESCRIPTION 컬럼 용도 명확화 (변경 없음, 주석만 수정)
-- 필요시 VARCHAR(500)으로 제한하여 요약용임을 명시
ALTER TABLE TB_ITEM
    MODIFY COLUMN DESCRIPTION VARCHAR(500) NULL COMMENT '업무 설명 (요약)';
```

### 2.5 CONTENT_TYPE 값 정의

| 값 | 설명 | 저장 형식 |
|----|------|-----------|
| HTML | TipTap HTML 출력 | `<p>내용</p><ul><li>항목</li></ul>` |
| JSON | TipTap JSON 출력 | `{"type":"doc","content":[...]}` |
| MARKDOWN | 마크다운 형식 | `# 제목\n- 항목` |

> **권장**: HTML 형식 사용 (렌더링 용이, 마이그레이션 편의)

---

## 3. API 설계

### 3.1 엔드포인트 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/boards/{boardId}/items/{itemId}/content` | 업무 내용 조회 |
| PUT | `/api/boards/{boardId}/items/{itemId}/content` | 업무 내용 저장/수정 |

### 3.2 업무 내용 조회

**GET** `/api/boards/{boardId}/items/{itemId}/content`

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "contentId": 1,
    "itemId": 100,
    "contentType": "HTML",
    "content": "<p>상세 내용입니다.</p><ul><li>항목 1</li><li>항목 2</li></ul>",
    "plainText": "상세 내용입니다. 항목 1 항목 2",
    "version": 1,
    "createdAt": "2024-01-15T09:00:00",
    "createdBy": "admin",
    "updatedAt": "2024-01-15T10:30:00",
    "updatedBy": "admin"
  }
}
```

**Response (404 Not Found)** - 내용이 없는 경우
```json
{
  "success": true,
  "data": null
}
```

### 3.3 업무 내용 저장/수정

**PUT** `/api/boards/{boardId}/items/{itemId}/content`

**Request Body**
```json
{
  "contentType": "HTML",
  "content": "<p>수정된 내용입니다.</p>",
  "version": 1
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| contentType | String | O | HTML, JSON, MARKDOWN |
| content | String | O | 리치 텍스트 본문 |
| version | Integer | O | 현재 버전 (충돌 감지용) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "contentId": 1,
    "itemId": 100,
    "contentType": "HTML",
    "content": "<p>수정된 내용입니다.</p>",
    "plainText": "수정된 내용입니다.",
    "version": 2,
    "updatedAt": "2024-01-15T11:00:00",
    "updatedBy": "admin"
  },
  "message": "저장되었습니다."
}
```

**Response (409 Conflict)** - 버전 충돌
```json
{
  "success": false,
  "data": {
    "currentVersion": 3,
    "requestedVersion": 1
  },
  "message": "다른 사용자가 수정했습니다. 새로고침 후 다시 시도해주세요."
}
```

### 3.4 기존 아이템 조회 API 변경

**GET** `/api/boards/{boardId}/items/{itemId}`

기존 응답에 `hasContent` 필드 추가:

```json
{
  "success": true,
  "data": {
    "itemId": 100,
    "title": "업무 제목",
    "description": "간단한 설명",
    "hasContent": true,
    ...
  }
}
```

---

## 4. TipTap 리치 텍스트 에디터 설계

### 4.1 에디터 선정: TipTap

| 항목 | 내용 |
|------|------|
| 라이브러리 | TipTap v2.x |
| 라이선스 | MIT (무료) |
| Vue 지원 | @tiptap/vue-3 (네이티브) |
| 특징 | Headless UI, 확장성, ProseMirror 기반 |

### 4.2 사용할 확장 기능 (모두 무료)

#### 텍스트 서식

| 확장 | 패키지 | 기능 |
|------|--------|------|
| StarterKit | @tiptap/starter-kit | 기본 확장 번들 |
| Bold | (StarterKit 포함) | **굵게** |
| Italic | (StarterKit 포함) | *기울임* |
| Strike | (StarterKit 포함) | ~~취소선~~ |
| Underline | @tiptap/extension-underline | <u>밑줄</u> |
| Highlight | @tiptap/extension-highlight | ==하이라이트== |
| Code | (StarterKit 포함) | `인라인 코드` |

#### 블록 요소

| 확장 | 패키지 | 기능 |
|------|--------|------|
| Heading | (StarterKit 포함) | H1 ~ H6 제목 |
| Paragraph | (StarterKit 포함) | 문단 |
| Blockquote | (StarterKit 포함) | 인용문 |
| CodeBlock | (StarterKit 포함) | 코드 블록 |
| HorizontalRule | (StarterKit 포함) | 수평선 |

#### 목록

| 확장 | 패키지 | 기능 |
|------|--------|------|
| BulletList | (StarterKit 포함) | 불릿 목록 |
| OrderedList | (StarterKit 포함) | 번호 목록 |
| TaskList | @tiptap/extension-task-list | 체크리스트 |
| TaskItem | @tiptap/extension-task-item | 체크리스트 항목 |

#### 테이블

| 확장 | 패키지 | 기능 |
|------|--------|------|
| Table | @tiptap/extension-table | 테이블 |
| TableRow | @tiptap/extension-table-row | 테이블 행 |
| TableCell | @tiptap/extension-table-cell | 테이블 셀 |
| TableHeader | @tiptap/extension-table-header | 테이블 헤더 |

#### 미디어/링크

| 확장 | 패키지 | 기능 |
|------|--------|------|
| Link | @tiptap/extension-link | 하이퍼링크 |
| Image | @tiptap/extension-image | 이미지 삽입 |

#### 정렬/스타일

| 확장 | 패키지 | 기능 |
|------|--------|------|
| TextAlign | @tiptap/extension-text-align | 텍스트 정렬 |
| Color | @tiptap/extension-color | 글자 색상 |
| TextStyle | @tiptap/extension-text-style | 텍스트 스타일 기반 |

#### 기능

| 확장 | 패키지 | 기능 |
|------|--------|------|
| Placeholder | @tiptap/extension-placeholder | 플레이스홀더 |
| CharacterCount | @tiptap/extension-character-count | 글자 수 표시 |
| History | (StarterKit 포함) | Undo/Redo |

### 4.3 설치할 패키지

```bash
npm install @tiptap/vue-3 @tiptap/pm @tiptap/starter-kit \
  @tiptap/extension-underline \
  @tiptap/extension-highlight \
  @tiptap/extension-task-list \
  @tiptap/extension-task-item \
  @tiptap/extension-table \
  @tiptap/extension-table-row \
  @tiptap/extension-table-cell \
  @tiptap/extension-table-header \
  @tiptap/extension-link \
  @tiptap/extension-image \
  @tiptap/extension-text-align \
  @tiptap/extension-color \
  @tiptap/extension-text-style \
  @tiptap/extension-placeholder \
  @tiptap/extension-character-count
```

### 4.4 에디터 설정 예시

```typescript
import { useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Highlight from '@tiptap/extension-highlight'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import TextAlign from '@tiptap/extension-text-align'
import Color from '@tiptap/extension-color'
import TextStyle from '@tiptap/extension-text-style'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'

const editor = useEditor({
  extensions: [
    StarterKit,
    Underline,
    Highlight.configure({ multicolor: true }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
    Link.configure({
      openOnClick: false,
      HTMLAttributes: { class: 'text-primary-600 underline' }
    }),
    Image,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    Color,
    TextStyle,
    Placeholder.configure({ placeholder: '내용을 입력하세요...' }),
    CharacterCount.configure({ limit: 100000 })
  ],
  content: '',
  editorProps: {
    attributes: {
      class: 'prose prose-sm max-w-none focus:outline-none min-h-[200px] p-4'
    }
  }
})
```

---

## 5. 프론트엔드 컴포넌트 설계

### 5.1 컴포넌트 구조

```
src/components/
├── editor/
│   ├── RichTextEditor.vue        # TipTap 에디터 래퍼
│   ├── EditorToolbar.vue         # 툴바 컴포넌트
│   ├── EditorBubbleMenu.vue      # 선택 시 팝업 메뉴
│   ├── EditorLinkModal.vue       # 링크 추가 모달
│   └── EditorImageModal.vue      # 이미지 추가 모달
├── item/
│   └── ItemDetailPanel.vue       # (수정) 설명 + 내용 분리
└── ...
```

### 5.2 RichTextEditor.vue

```vue
<script setup lang="ts">
/**
 * TipTap 리치 텍스트 에디터 컴포넌트
 * - 다양한 서식 지원
 * - 자동 저장 (debounce)
 * - 읽기 전용 모드 지원
 */
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import EditorToolbar from './EditorToolbar.vue'
import EditorBubbleMenu from './EditorBubbleMenu.vue'
// ... extensions import

interface Props {
  modelValue: string
  contentType?: 'HTML' | 'JSON' | 'MARKDOWN'
  placeholder?: string
  readonly?: boolean
  minHeight?: string
  maxHeight?: string
  autofocus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  contentType: 'HTML',
  placeholder: '내용을 입력하세요...',
  readonly: false,
  minHeight: '200px',
  maxHeight: '600px',
  autofocus: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string, plainText: string): void
}>()

// 에디터 초기화
const editor = useEditor({
  extensions: [...],
  content: props.modelValue,
  editable: !props.readonly,
  autofocus: props.autofocus,
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    const plainText = editor.getText()
    emit('update:modelValue', html)
    emit('change', html, plainText)
  }
})

// 외부 값 변경 시 동기화
watch(() => props.modelValue, (newValue) => {
  if (editor.value && newValue !== editor.value.getHTML()) {
    editor.value.commands.setContent(newValue, false)
  }
})

// 클린업
onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="rich-text-editor border border-gray-200 rounded-lg overflow-hidden">
    <!-- 툴바 -->
    <EditorToolbar v-if="!readonly && editor" :editor="editor" />

    <!-- 버블 메뉴 (선택 시) -->
    <EditorBubbleMenu v-if="!readonly && editor" :editor="editor" />

    <!-- 에디터 본문 -->
    <EditorContent
      :editor="editor"
      :class="[
        'editor-content',
        readonly ? 'bg-gray-50' : 'bg-white'
      ]"
      :style="{
        minHeight: minHeight,
        maxHeight: maxHeight,
        overflowY: 'auto'
      }"
    />

    <!-- 글자 수 표시 -->
    <div v-if="editor" class="px-3 py-1.5 border-t border-gray-100 bg-gray-50 text-[12px] text-gray-500">
      {{ editor.storage.characterCount.characters() }}자
    </div>
  </div>
</template>
```

### 5.3 EditorToolbar.vue

```vue
<script setup lang="ts">
/**
 * 에디터 툴바 컴포넌트
 * - 서식 버튼 그룹
 * - 활성 상태 표시
 */
import type { Editor } from '@tiptap/vue-3'

interface Props {
  editor: Editor
}

const props = defineProps<Props>()

// 툴바 버튼 그룹 정의
const toolbarGroups = [
  // 텍스트 서식
  {
    name: 'format',
    buttons: [
      { icon: 'bold', command: 'toggleBold', isActive: 'bold', title: '굵게' },
      { icon: 'italic', command: 'toggleItalic', isActive: 'italic', title: '기울임' },
      { icon: 'underline', command: 'toggleUnderline', isActive: 'underline', title: '밑줄' },
      { icon: 'strikethrough', command: 'toggleStrike', isActive: 'strike', title: '취소선' },
      { icon: 'highlight', command: 'toggleHighlight', isActive: 'highlight', title: '하이라이트' },
      { icon: 'code', command: 'toggleCode', isActive: 'code', title: '인라인 코드' }
    ]
  },
  // 제목
  {
    name: 'heading',
    buttons: [
      { icon: 'h1', command: () => props.editor.chain().focus().toggleHeading({ level: 1 }).run(), isActive: () => props.editor.isActive('heading', { level: 1 }), title: '제목 1' },
      { icon: 'h2', command: () => props.editor.chain().focus().toggleHeading({ level: 2 }).run(), isActive: () => props.editor.isActive('heading', { level: 2 }), title: '제목 2' },
      { icon: 'h3', command: () => props.editor.chain().focus().toggleHeading({ level: 3 }).run(), isActive: () => props.editor.isActive('heading', { level: 3 }), title: '제목 3' }
    ]
  },
  // 목록
  {
    name: 'list',
    buttons: [
      { icon: 'list-ul', command: 'toggleBulletList', isActive: 'bulletList', title: '불릿 목록' },
      { icon: 'list-ol', command: 'toggleOrderedList', isActive: 'orderedList', title: '번호 목록' },
      { icon: 'list-check', command: 'toggleTaskList', isActive: 'taskList', title: '체크리스트' }
    ]
  },
  // 정렬
  {
    name: 'align',
    buttons: [
      { icon: 'align-left', command: () => props.editor.chain().focus().setTextAlign('left').run(), isActive: () => props.editor.isActive({ textAlign: 'left' }), title: '왼쪽 정렬' },
      { icon: 'align-center', command: () => props.editor.chain().focus().setTextAlign('center').run(), isActive: () => props.editor.isActive({ textAlign: 'center' }), title: '가운데 정렬' },
      { icon: 'align-right', command: () => props.editor.chain().focus().setTextAlign('right').run(), isActive: () => props.editor.isActive({ textAlign: 'right' }), title: '오른쪽 정렬' }
    ]
  },
  // 삽입
  {
    name: 'insert',
    buttons: [
      { icon: 'link', command: 'openLinkModal', title: '링크' },
      { icon: 'image', command: 'openImageModal', title: '이미지' },
      { icon: 'table', command: () => props.editor.chain().focus().insertTable({ rows: 3, cols: 3 }).run(), title: '테이블' },
      { icon: 'code-block', command: 'toggleCodeBlock', isActive: 'codeBlock', title: '코드 블록' },
      { icon: 'quote', command: 'toggleBlockquote', isActive: 'blockquote', title: '인용문' },
      { icon: 'hr', command: 'setHorizontalRule', title: '수평선' }
    ]
  },
  // 실행 취소
  {
    name: 'history',
    buttons: [
      { icon: 'undo', command: 'undo', disabled: () => !props.editor.can().undo(), title: '실행 취소' },
      { icon: 'redo', command: 'redo', disabled: () => !props.editor.can().redo(), title: '다시 실행' }
    ]
  }
]
</script>

<template>
  <div class="flex flex-wrap items-center gap-1 px-2 py-1.5 border-b border-gray-200 bg-gray-50">
    <template v-for="(group, groupIndex) in toolbarGroups" :key="group.name">
      <!-- 구분선 -->
      <div v-if="groupIndex > 0" class="w-px h-5 bg-gray-300 mx-1" />

      <!-- 버튼 그룹 -->
      <div class="flex items-center gap-0.5">
        <button
          v-for="button in group.buttons"
          :key="button.icon"
          :class="[
            'p-1.5 rounded hover:bg-gray-200 transition-colors',
            (typeof button.isActive === 'function' ? button.isActive() : editor.isActive(button.isActive))
              ? 'bg-gray-200 text-primary-600'
              : 'text-gray-600'
          ]"
          :disabled="button.disabled?.()"
          :title="button.title"
          @click="typeof button.command === 'function' ? button.command() : editor.chain().focus()[button.command]().run()"
        >
          <!-- 아이콘 (실제 구현 시 SVG 아이콘 사용) -->
          <span class="w-4 h-4 flex items-center justify-center text-[12px] font-bold">
            {{ button.icon.charAt(0).toUpperCase() }}
          </span>
        </button>
      </div>
    </template>
  </div>
</template>
```

### 5.4 ItemDetailPanel.vue 수정

```
┌─────────────────────────────────────────────────────┐
│ [제목 입력 필드]                                     │
├─────────────────────────────────────────────────────┤
│ 설명                                                 │
│ ┌─────────────────────────────────────────────────┐ │
│ │ 간단한 업무 요약 (단일 라인)                      │ │
│ └─────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────┤
│ 내용                                                 │
│ ┌─────────────────────────────────────────────────┐ │
│ │ B I U S │ H1 H2 │ • 1. ☑ │ ≡ ≡ ≡ │ 🔗 📷 ▦ │ │  ← 툴바
│ ├─────────────────────────────────────────────────┤ │
│ │                                                 │ │
│ │  리치 텍스트 에디터 영역                         │ │
│ │  (굵기, 목록, 링크, 이미지, 테이블 등)           │ │
│ │                                                 │ │
│ │                                                 │ │
│ └─────────────────────────────────────────────────┘ │
│ 1,234자                                             │
├─────────────────────────────────────────────────────┤
│ [속성 영역]                                          │
│ 상태: 진행중  우선순위: 높음  담당자: 홍길동          │
│ 요청일: 2024-01-15  마감일: 2024-01-20              │
├─────────────────────────────────────────────────────┤
│ [댓글 영역]                                          │
└─────────────────────────────────────────────────────┘
```

---

## 6. 백엔드 구현 설계

### 6.1 패키지 구조

```
com.taskflow
├── domain/
│   └── ItemContent.java           # 엔티티
├── dto/
│   └── content/
│       ├── ItemContentResponse.java
│       └── ItemContentUpdateRequest.java
├── mapper/
│   └── ItemContentMapper.java     # MyBatis Mapper
├── service/
│   ├── ItemContentService.java    # 인터페이스
│   └── impl/
│       └── ItemContentServiceImpl.java
└── controller/
    └── ItemContentController.java
```

### 6.2 Domain: ItemContent.java

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemContent {
    private Long contentId;
    private Long itemId;
    private String contentType;  // HTML, JSON, MARKDOWN
    private String content;
    private String plainText;
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
```

### 6.3 DTO

```java
// ItemContentResponse.java
@Data
@Builder
public class ItemContentResponse {
    private Long contentId;
    private Long itemId;
    private String contentType;
    private String content;
    private String plainText;
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}

// ItemContentUpdateRequest.java
@Data
public class ItemContentUpdateRequest {
    @NotBlank
    private String contentType;

    @NotNull
    private String content;

    @NotNull
    private Integer version;
}
```

### 6.4 Mapper XML

```xml
<!-- ItemContentMapper.xml -->
<mapper namespace="com.taskflow.mapper.ItemContentMapper">

    <resultMap id="ItemContentResultMap" type="com.taskflow.domain.ItemContent">
        <id property="contentId" column="CONTENT_ID"/>
        <result property="itemId" column="ITEM_ID"/>
        <result property="contentType" column="CONTENT_TYPE"/>
        <result property="content" column="CONTENT"/>
        <result property="plainText" column="PLAIN_TEXT"/>
        <result property="version" column="VERSION"/>
        <result property="createdAt" column="CREATED_AT"/>
        <result property="createdBy" column="CREATED_BY"/>
        <result property="updatedAt" column="UPDATED_AT"/>
        <result property="updatedBy" column="UPDATED_BY"/>
    </resultMap>

    <!-- 업무 ID로 내용 조회 -->
    <select id="findByItemId" resultMap="ItemContentResultMap">
        SELECT * FROM TB_ITEM_CONTENT WHERE ITEM_ID = #{itemId}
    </select>

    <!-- 내용 존재 여부 확인 -->
    <select id="existsByItemId" resultType="boolean">
        SELECT EXISTS(SELECT 1 FROM TB_ITEM_CONTENT WHERE ITEM_ID = #{itemId})
    </select>

    <!-- 내용 등록 -->
    <insert id="insert" parameterType="com.taskflow.domain.ItemContent"
            useGeneratedKeys="true" keyProperty="contentId">
        INSERT INTO TB_ITEM_CONTENT (
            ITEM_ID, CONTENT_TYPE, CONTENT, PLAIN_TEXT, VERSION, CREATED_BY
        ) VALUES (
            #{itemId}, #{contentType}, #{content}, #{plainText}, 1, #{createdBy}
        )
    </insert>

    <!-- 내용 수정 (버전 충돌 체크) -->
    <update id="update">
        UPDATE TB_ITEM_CONTENT
        SET CONTENT_TYPE = #{contentType},
            CONTENT = #{content},
            PLAIN_TEXT = #{plainText},
            VERSION = VERSION + 1,
            UPDATED_BY = #{updatedBy}
        WHERE ITEM_ID = #{itemId}
          AND VERSION = #{version}
    </update>

    <!-- 내용 삭제 -->
    <delete id="deleteByItemId">
        DELETE FROM TB_ITEM_CONTENT WHERE ITEM_ID = #{itemId}
    </delete>

</mapper>
```

### 6.5 Controller

```java
@RestController
@RequestMapping("/api/boards/{boardId}/items/{itemId}/content")
@RequiredArgsConstructor
public class ItemContentController {

    private final ItemContentService itemContentService;

    @GetMapping
    public ResponseEntity<ApiResponse<ItemContentResponse>> getContent(
            @PathVariable Long boardId,
            @PathVariable Long itemId) {
        ItemContentResponse content = itemContentService.getContent(boardId, itemId);
        return ResponseEntity.ok(ApiResponse.success(content));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ItemContentResponse>> updateContent(
            @PathVariable Long boardId,
            @PathVariable Long itemId,
            @Valid @RequestBody ItemContentUpdateRequest request) {
        ItemContentResponse content = itemContentService.updateContent(boardId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(content, "저장되었습니다."));
    }
}
```

---

## 7. 자동 저장 및 동시 편집 처리

### 7.1 자동 저장 (Debounce)

```typescript
// composables/useAutoSave.ts
import { ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'

export function useAutoSave(
  content: Ref<string>,
  saveFn: (content: string, plainText: string) => Promise<void>,
  options = { debounceMs: 2000 }
) {
  const isSaving = ref(false)
  const lastSavedAt = ref<Date | null>(null)
  const hasUnsavedChanges = ref(false)

  const debouncedSave = useDebounceFn(async (html: string, text: string) => {
    isSaving.value = true
    try {
      await saveFn(html, text)
      lastSavedAt.value = new Date()
      hasUnsavedChanges.value = false
    } finally {
      isSaving.value = false
    }
  }, options.debounceMs)

  // 내용 변경 감지
  watch(content, (newContent) => {
    hasUnsavedChanges.value = true
    // plainText는 에디터에서 추출
  })

  return {
    isSaving,
    lastSavedAt,
    hasUnsavedChanges,
    save: debouncedSave
  }
}
```

### 7.2 버전 충돌 처리

```typescript
// 저장 시 버전 충돌 처리
async function saveContent(content: string, plainText: string) {
  try {
    const response = await itemContentApi.update(boardId, itemId, {
      contentType: 'HTML',
      content,
      version: currentVersion.value
    })

    // 버전 업데이트
    currentVersion.value = response.data.version

  } catch (error) {
    if (error.response?.status === 409) {
      // 버전 충돌
      const { currentVersion: serverVersion } = error.response.data.data

      const confirmed = await confirm.show({
        title: '편집 충돌',
        message: '다른 사용자가 수정했습니다. 새로고침하시겠습니까?',
        confirmText: '새로고침',
        cancelText: '내 변경사항 유지'
      })

      if (confirmed) {
        await loadContent()
      }
    }
  }
}
```

---

## 8. 구현 순서

| 단계 | 작업 내용 | 예상 파일 |
|------|-----------|-----------|
| 1 | DB 스키마 생성 | docker/mysql/init/11_item_content.sql |
| 2 | Backend Domain/DTO | ItemContent.java, ItemContentResponse.java, ItemContentUpdateRequest.java |
| 3 | Backend Mapper | ItemContentMapper.java, ItemContentMapper.xml |
| 4 | Backend Service | ItemContentService.java, ItemContentServiceImpl.java |
| 5 | Backend Controller | ItemContentController.java |
| 6 | Frontend TipTap 설치 | package.json |
| 7 | Frontend 에디터 컴포넌트 | RichTextEditor.vue, EditorToolbar.vue |
| 8 | Frontend API 모듈 | itemContent.ts |
| 9 | Frontend Store | useItemContentStore.ts (선택) |
| 10 | ItemDetailPanel 수정 | ItemDetailPanel.vue |
| 11 | 자동 저장 구현 | useAutoSave.ts |
| 12 | 테스트 및 검증 | - |

---

## 9. 참고 자료

- [TipTap 공식 문서](https://tiptap.dev/docs)
- [TipTap Vue 3 가이드](https://tiptap.dev/docs/editor/getting-started/install/vue3)
- [TipTap Extensions 목록](https://tiptap.dev/docs/editor/extensions/overview)

---

**문서 버전**

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2024-01-15 | - | 최초 작성 |
