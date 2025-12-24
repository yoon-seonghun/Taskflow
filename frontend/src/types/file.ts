/**
 * 파일 관련 타입 정의
 */

/**
 * 파일 업로드 응답
 */
export interface FileUploadResponse {
  fileId: number
  originalName: string
  storedName: string
  extension: string
  mimeType: string
  fileSize: number
  fileSizeDisplay: string
  storageType: string
  url: string
  markdown: string
  createdAt: string
}

/**
 * 파일 정보 응답
 */
export interface FileInfoResponse {
  fileId: number
  originalName: string
  storedName: string
  extension: string
  mimeType: string
  fileSize: number
  fileSizeDisplay: string
  storageType: string
  relatedType: string | null
  relatedId: number | null
  url: string
  createdAt: string
  createdBy: number
  createdByName: string | null
}

/**
 * 파일 업로드 옵션
 */
export interface FileUploadOptions {
  relatedType?: string
  relatedId?: number
  onProgress?: (progress: number) => void
}

/**
 * 이미지 MIME 타입
 */
export const IMAGE_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'image/svg+xml'
] as const

/**
 * 허용된 파일 확장자
 */
export const ALLOWED_EXTENSIONS = [
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'svg',
  'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'txt', 'zip'
] as const

/**
 * 최대 파일 크기 (10MB)
 */
export const MAX_FILE_SIZE = 10 * 1024 * 1024

/**
 * 허용 파일 형식 안내 텍스트 (간략)
 */
export const ALLOWED_FILES_SHORT = '이미지, 문서, PDF, 압축파일'

/**
 * 허용 파일 형식 안내 텍스트 (상세)
 */
export const ALLOWED_FILES_DETAIL = 'jpg, png, gif, pdf, doc, xls, ppt, txt, zip 등'

/**
 * 최대 파일 크기 안내 텍스트
 */
export const MAX_FILE_SIZE_TEXT = '10MB'

/**
 * 파일 업로드 안내 텍스트 생성
 */
export function getFileUploadGuide(type: 'short' | 'full' = 'short'): string {
  if (type === 'short') {
    return `${ALLOWED_FILES_SHORT} (최대 ${MAX_FILE_SIZE_TEXT})`
  }
  return `허용 형식: ${ALLOWED_FILES_DETAIL}\n최대 크기: ${MAX_FILE_SIZE_TEXT}`
}

/**
 * 이미지 파일 여부 확인
 */
export function isImageFile(mimeType: string): boolean {
  return IMAGE_MIME_TYPES.includes(mimeType as typeof IMAGE_MIME_TYPES[number])
}

/**
 * 파일 확장자 추출
 */
export function getFileExtension(filename: string): string {
  if (!filename || !filename.includes('.')) {
    return ''
  }
  return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
}

/**
 * 파일 크기 포맷팅
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

/**
 * 파일 유효성 검사
 */
export function validateFile(file: File): { valid: boolean; error?: string } {
  // 크기 검사
  if (file.size > MAX_FILE_SIZE) {
    return {
      valid: false,
      error: `파일 크기가 최대 ${MAX_FILE_SIZE_TEXT}를 초과합니다. (현재: ${formatFileSize(file.size)})`
    }
  }

  // 확장자 검사
  const ext = getFileExtension(file.name)
  if (!ALLOWED_EXTENSIONS.includes(ext as typeof ALLOWED_EXTENSIONS[number])) {
    return {
      valid: false,
      error: `허용되지 않는 파일 형식입니다: ${ext}\n업로드 가능: ${ALLOWED_FILES_SHORT} (${ALLOWED_FILES_DETAIL})`
    }
  }

  return { valid: true }
}

/**
 * 파일 아이콘 정보 (SVG path 및 색상)
 */
export interface FileIconInfo {
  pathD: string
  color: string
  bgColor: string
}

/**
 * 확장자별 파일 아이콘 반환
 */
export function getFileIcon(extension: string): FileIconInfo {
  const ext = extension.toLowerCase()

  // 이미지
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'].includes(ext)) {
    return {
      pathD: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z',
      color: 'text-green-600',
      bgColor: 'bg-green-100'
    }
  }

  // PDF
  if (ext === 'pdf') {
    return {
      pathD: 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z',
      color: 'text-red-600',
      bgColor: 'bg-red-100'
    }
  }

  // Word
  if (['doc', 'docx'].includes(ext)) {
    return {
      pathD: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
      color: 'text-blue-600',
      bgColor: 'bg-blue-100'
    }
  }

  // Excel
  if (['xls', 'xlsx', 'csv'].includes(ext)) {
    return {
      pathD: 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
      color: 'text-emerald-600',
      bgColor: 'bg-emerald-100'
    }
  }

  // PowerPoint
  if (['ppt', 'pptx'].includes(ext)) {
    return {
      pathD: 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z',
      color: 'text-orange-600',
      bgColor: 'bg-orange-100'
    }
  }

  // 압축 파일
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) {
    return {
      pathD: 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4',
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-100'
    }
  }

  // 텍스트
  if (['txt', 'md', 'json', 'xml', 'html', 'css', 'js'].includes(ext)) {
    return {
      pathD: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
      color: 'text-gray-600',
      bgColor: 'bg-gray-100'
    }
  }

  // 기본 파일 아이콘
  return {
    pathD: 'M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z',
    color: 'text-gray-500',
    bgColor: 'bg-gray-100'
  }
}
