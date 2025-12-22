/**
 * 공통 컴포넌트 통합 내보내기
 */

// 기본 컴포넌트
export { default as Button } from './Button.vue'
export { default as Input } from './Input.vue'
export { default as Select } from './Select.vue'
export { default as UserSelect } from './UserSelect.vue'
export { default as DatePicker } from './DatePicker.vue'
export { default as Autocomplete } from './Autocomplete.vue'

// 피드백 컴포넌트
export { default as Modal } from './Modal.vue'
export { default as Toast } from './Toast.vue'
export { default as ConfirmDialog } from './ConfirmDialog.vue'
export { default as ConflictDialog } from './ConflictDialog.vue'
export { default as ErrorBoundary } from './ErrorBoundary.vue'
export { default as Spinner } from './Spinner.vue'

// UI 컴포넌트
export { default as Badge } from './Badge.vue'
export { default as ContextMenu } from './ContextMenu.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as InlineEditor } from './InlineEditor.vue'
export { default as Pagination } from './Pagination.vue'
export { default as MarkdownEditor } from './MarkdownEditor.vue'
export { default as DepartmentTree } from './DepartmentTree.vue'
export { default as UserSearchSelector } from './UserSearchSelector.vue'

// 타입 내보내기
export type { ButtonVariant, ButtonSize } from './Button.vue'
export type { InputSize, InputType } from './Input.vue'
export type { SelectOption, SelectSize } from './Select.vue'
export type { UserOption, DepartmentOption } from './UserSelect.vue'
export type { DatePickerSize, DatePickerMode } from './DatePicker.vue'
export type { ModalSize } from './Modal.vue'
export type { MenuItem } from './ContextMenu.vue'
export type { EditorType } from './InlineEditor.vue'
export type { BadgeVariant, BadgeSize } from './Badge.vue'
export type { SpinnerSize } from './Spinner.vue'
export type { AutocompleteOption, AutocompleteSize } from './Autocomplete.vue'
