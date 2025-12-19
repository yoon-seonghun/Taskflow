/**
 * 전역 에러 타입 정의
 */

// HTTP 상태 코드 상수
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504
} as const

export type HttpStatusCode = (typeof HTTP_STATUS)[keyof typeof HTTP_STATUS]

// 에러 코드 정의
export enum ErrorCode {
  // 네트워크 에러
  NETWORK_ERROR = 'NETWORK_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',
  CONNECTION_REFUSED = 'CONNECTION_REFUSED',

  // 인증 에러
  UNAUTHORIZED = 'UNAUTHORIZED',
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  INVALID_TOKEN = 'INVALID_TOKEN',
  SESSION_EXPIRED = 'SESSION_EXPIRED',

  // 권한 에러
  FORBIDDEN = 'FORBIDDEN',
  ACCESS_DENIED = 'ACCESS_DENIED',

  // 리소스 에러
  NOT_FOUND = 'NOT_FOUND',
  RESOURCE_NOT_FOUND = 'RESOURCE_NOT_FOUND',

  // 검증 에러
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  INVALID_INPUT = 'INVALID_INPUT',
  DUPLICATE_ERROR = 'DUPLICATE_ERROR',

  // 충돌 에러
  CONFLICT = 'CONFLICT',
  CONCURRENT_MODIFICATION = 'CONCURRENT_MODIFICATION',

  // 서버 에러
  INTERNAL_ERROR = 'INTERNAL_ERROR',
  SERVICE_UNAVAILABLE = 'SERVICE_UNAVAILABLE',

  // 기타
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

// 에러 메시지 매핑
export const ERROR_MESSAGES: Record<ErrorCode, string> = {
  [ErrorCode.NETWORK_ERROR]: '네트워크 연결을 확인해주세요.',
  [ErrorCode.TIMEOUT_ERROR]: '요청 시간이 초과되었습니다. 다시 시도해주세요.',
  [ErrorCode.CONNECTION_REFUSED]: '서버에 연결할 수 없습니다.',

  [ErrorCode.UNAUTHORIZED]: '로그인이 필요합니다.',
  [ErrorCode.TOKEN_EXPIRED]: '로그인이 만료되었습니다. 다시 로그인해주세요.',
  [ErrorCode.INVALID_TOKEN]: '인증 정보가 올바르지 않습니다.',
  [ErrorCode.SESSION_EXPIRED]: '세션이 만료되었습니다. 다시 로그인해주세요.',

  [ErrorCode.FORBIDDEN]: '접근 권한이 없습니다.',
  [ErrorCode.ACCESS_DENIED]: '해당 작업을 수행할 권한이 없습니다.',

  [ErrorCode.NOT_FOUND]: '요청한 리소스를 찾을 수 없습니다.',
  [ErrorCode.RESOURCE_NOT_FOUND]: '해당 데이터를 찾을 수 없습니다.',

  [ErrorCode.VALIDATION_ERROR]: '입력값이 올바르지 않습니다.',
  [ErrorCode.INVALID_INPUT]: '잘못된 입력입니다.',
  [ErrorCode.DUPLICATE_ERROR]: '이미 존재하는 데이터입니다.',

  [ErrorCode.CONFLICT]: '다른 사용자가 수정 중입니다. 새로고침 후 다시 시도해주세요.',
  [ErrorCode.CONCURRENT_MODIFICATION]: '동시 수정이 감지되었습니다.',

  [ErrorCode.INTERNAL_ERROR]: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
  [ErrorCode.SERVICE_UNAVAILABLE]: '서비스를 일시적으로 사용할 수 없습니다.',

  [ErrorCode.UNKNOWN_ERROR]: '알 수 없는 오류가 발생했습니다.'
}

// HTTP 상태 코드 → 에러 코드 매핑
export const HTTP_STATUS_TO_ERROR_CODE: Record<number, ErrorCode> = {
  [HTTP_STATUS.BAD_REQUEST]: ErrorCode.VALIDATION_ERROR,
  [HTTP_STATUS.UNAUTHORIZED]: ErrorCode.UNAUTHORIZED,
  [HTTP_STATUS.FORBIDDEN]: ErrorCode.FORBIDDEN,
  [HTTP_STATUS.NOT_FOUND]: ErrorCode.NOT_FOUND,
  [HTTP_STATUS.CONFLICT]: ErrorCode.CONFLICT,
  [HTTP_STATUS.UNPROCESSABLE_ENTITY]: ErrorCode.VALIDATION_ERROR,
  [HTTP_STATUS.INTERNAL_SERVER_ERROR]: ErrorCode.INTERNAL_ERROR,
  [HTTP_STATUS.BAD_GATEWAY]: ErrorCode.SERVICE_UNAVAILABLE,
  [HTTP_STATUS.SERVICE_UNAVAILABLE]: ErrorCode.SERVICE_UNAVAILABLE,
  [HTTP_STATUS.GATEWAY_TIMEOUT]: ErrorCode.TIMEOUT_ERROR
}

/**
 * 기본 앱 에러 클래스
 */
export class AppError extends Error {
  public readonly code: ErrorCode
  public readonly statusCode?: number
  public readonly context?: string
  public readonly originalError?: Error
  public readonly timestamp: Date

  constructor(
    message: string,
    code: ErrorCode = ErrorCode.UNKNOWN_ERROR,
    options?: {
      statusCode?: number
      context?: string
      originalError?: Error
    }
  ) {
    super(message)
    this.name = 'AppError'
    this.code = code
    this.statusCode = options?.statusCode
    this.context = options?.context
    this.originalError = options?.originalError
    this.timestamp = new Date()

    // Error 상속 시 프로토타입 체인 유지
    Object.setPrototypeOf(this, AppError.prototype)
  }

  toJSON() {
    return {
      name: this.name,
      message: this.message,
      code: this.code,
      statusCode: this.statusCode,
      context: this.context,
      timestamp: this.timestamp.toISOString()
    }
  }
}

/**
 * API 에러 클래스
 */
export class ApiError extends AppError {
  public readonly url?: string
  public readonly method?: string
  public readonly responseData?: unknown

  constructor(
    message: string,
    statusCode: number,
    options?: {
      code?: ErrorCode
      url?: string
      method?: string
      responseData?: unknown
      context?: string
      originalError?: Error
    }
  ) {
    const errorCode = options?.code || HTTP_STATUS_TO_ERROR_CODE[statusCode] || ErrorCode.UNKNOWN_ERROR
    super(message, errorCode, {
      statusCode,
      context: options?.context,
      originalError: options?.originalError
    })
    this.name = 'ApiError'
    this.url = options?.url
    this.method = options?.method
    this.responseData = options?.responseData

    Object.setPrototypeOf(this, ApiError.prototype)
  }

  static fromResponse(
    response: { status: number; statusText?: string; data?: { message?: string } },
    options?: { url?: string; method?: string; context?: string }
  ): ApiError {
    const message = response.data?.message || response.statusText || '요청 처리 중 오류가 발생했습니다.'
    return new ApiError(message, response.status, {
      url: options?.url,
      method: options?.method,
      responseData: response.data,
      context: options?.context
    })
  }
}

/**
 * 네트워크 에러 클래스
 */
export class NetworkError extends AppError {
  public readonly isTimeout: boolean
  public readonly isOffline: boolean

  constructor(
    message: string = '네트워크 연결을 확인해주세요.',
    options?: {
      isTimeout?: boolean
      isOffline?: boolean
      context?: string
      originalError?: Error
    }
  ) {
    const code = options?.isTimeout ? ErrorCode.TIMEOUT_ERROR : ErrorCode.NETWORK_ERROR
    super(message, code, {
      context: options?.context,
      originalError: options?.originalError
    })
    this.name = 'NetworkError'
    this.isTimeout = options?.isTimeout || false
    this.isOffline = options?.isOffline || false

    Object.setPrototypeOf(this, NetworkError.prototype)
  }
}

/**
 * 검증 에러 클래스
 */
export class ValidationError extends AppError {
  public readonly field?: string
  public readonly errors?: Record<string, string[]>

  constructor(
    message: string,
    options?: {
      field?: string
      errors?: Record<string, string[]>
      context?: string
    }
  ) {
    super(message, ErrorCode.VALIDATION_ERROR, { context: options?.context })
    this.name = 'ValidationError'
    this.field = options?.field
    this.errors = options?.errors

    Object.setPrototypeOf(this, ValidationError.prototype)
  }

  getFieldErrors(field: string): string[] {
    return this.errors?.[field] || []
  }
}

/**
 * 인증 에러 클래스
 */
export class AuthError extends AppError {
  public readonly shouldRedirect: boolean
  public readonly redirectTo?: string

  constructor(
    message: string,
    code: ErrorCode = ErrorCode.UNAUTHORIZED,
    options?: {
      shouldRedirect?: boolean
      redirectTo?: string
      context?: string
      originalError?: Error
    }
  ) {
    super(message, code, {
      statusCode: HTTP_STATUS.UNAUTHORIZED,
      context: options?.context,
      originalError: options?.originalError
    })
    this.name = 'AuthError'
    this.shouldRedirect = options?.shouldRedirect ?? true
    this.redirectTo = options?.redirectTo || '/login'

    Object.setPrototypeOf(this, AuthError.prototype)
  }
}

/**
 * 에러 타입 가드 함수들
 */
export function isAppError(error: unknown): error is AppError {
  return error instanceof AppError
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

export function isNetworkError(error: unknown): error is NetworkError {
  return error instanceof NetworkError
}

export function isValidationError(error: unknown): error is ValidationError {
  return error instanceof ValidationError
}

export function isAuthError(error: unknown): error is AuthError {
  return error instanceof AuthError
}

/**
 * 에러를 AppError로 변환
 */
export function toAppError(error: unknown, context?: string): AppError {
  if (isAppError(error)) {
    return error
  }

  if (error instanceof Error) {
    return new AppError(error.message, ErrorCode.UNKNOWN_ERROR, {
      context,
      originalError: error
    })
  }

  if (typeof error === 'string') {
    return new AppError(error, ErrorCode.UNKNOWN_ERROR, { context })
  }

  return new AppError('알 수 없는 오류가 발생했습니다.', ErrorCode.UNKNOWN_ERROR, { context })
}
