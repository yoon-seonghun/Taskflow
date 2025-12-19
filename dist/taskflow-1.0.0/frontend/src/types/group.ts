/**
 * 그룹 타입 정의
 */

export interface Group {
  groupId: number
  groupCode: string
  groupName: string
  groupDescription?: string
  groupColor?: string
  sortOrder: number
  useYn: string
  memberCount?: number
  createdAt: string
  createdBy: number
  createdByName?: string
  updatedAt?: string
  updatedBy?: number
}

export interface GroupCreateRequest {
  groupCode: string
  groupName: string
  groupDescription?: string
  groupColor?: string
  sortOrder?: number
}

export interface GroupUpdateRequest {
  groupCode?: string
  groupName?: string
  groupDescription?: string
  groupColor?: string
  sortOrder?: number
  useYn?: string
}

export interface GroupMember {
  userId: number
  userName: string
  departmentName?: string
  joinedAt: string
}

export interface GroupMemberRequest {
  userId: number
}
