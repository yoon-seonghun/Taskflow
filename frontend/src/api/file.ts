import client, { get, put, del } from './client'
import type { ApiResponse } from '@/types/api'
import type { FileUploadResponse, FileInfoResponse, FileUploadOptions } from '@/types/file'

export const fileApi = {
  /**
   * 파일 업로드
   * FormData를 사용하여 multipart/form-data로 전송
   */
  async upload(file: File, options?: FileUploadOptions): Promise<ApiResponse<FileUploadResponse>> {
    const formData = new FormData()
    formData.append('file', file)

    if (options?.relatedType) {
      formData.append('relatedType', options.relatedType)
    }
    if (options?.relatedId) {
      formData.append('relatedId', String(options.relatedId))
    }

    // FormData 사용 시 Content-Type 헤더를 설정하지 않아야 함
    // Axios가 자동으로 multipart/form-data와 boundary를 설정
    const response = await client.post<ApiResponse<FileUploadResponse>>('/files/upload', formData, {
      onUploadProgress: (progressEvent) => {
        if (options?.onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          options.onProgress(progress)
        }
      }
    })

    return response.data
  },

  /**
   * 파일 URL 조회 (다운로드/표시용)
   */
  getFileUrl(fileId: number, download = false): string {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
    return `${baseUrl}/files/${fileId}${download ? '?download=true' : ''}`
  },

  /**
   * 파일 정보 조회
   */
  getFileInfo(fileId: number) {
    return get<FileInfoResponse>(`/files/${fileId}/info`)
  },

  /**
   * 파일 삭제
   */
  deleteFile(fileId: number) {
    return del<void>(`/files/${fileId}`)
  },

  /**
   * 연관 엔티티의 파일 목록 조회
   */
  getRelatedFiles(relatedType: string, relatedId: number) {
    return get<FileInfoResponse[]>(`/files/related/${relatedType}/${relatedId}`)
  },

  /**
   * 파일 연관 정보 업데이트
   */
  async updateRelated(fileId: number, relatedType: string, relatedId: number): Promise<ApiResponse<void>> {
    const response = await client.put<ApiResponse<void>>(
      `/files/${fileId}/related?relatedType=${relatedType}&relatedId=${relatedId}`
    )
    return response.data
  },

  /**
   * 이미지 파일 여부 확인
   */
  isImage(fileId: number) {
    return get<boolean>(`/files/${fileId}/is-image`)
  }
}
