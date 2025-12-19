import { get, post, put, del } from './client'
import type {
  Group,
  GroupCreateRequest,
  GroupUpdateRequest,
  GroupMember,
  GroupMemberRequest
} from '@/types/group'

export const groupApi = {
  getGroups(params?: { useYn?: string }) {
    return get<Group[]>('/groups', params)
  },

  getGroup(groupId: number) {
    return get<Group>(`/groups/${groupId}`)
  },

  createGroup(data: GroupCreateRequest) {
    return post<Group>('/groups', data)
  },

  updateGroup(groupId: number, data: GroupUpdateRequest) {
    return put<Group>(`/groups/${groupId}`, data)
  },

  deleteGroup(groupId: number) {
    return del<void>(`/groups/${groupId}`)
  },

  updateGroupOrder(groupId: number, sortOrder: number) {
    return put<void>(`/groups/${groupId}/order`, { sortOrder })
  },

  // 그룹 멤버
  getGroupMembers(groupId: number) {
    return get<GroupMember[]>(`/groups/${groupId}/members`)
  },

  addGroupMember(groupId: number, data: GroupMemberRequest) {
    return post<GroupMember>(`/groups/${groupId}/members`, data)
  },

  removeGroupMember(groupId: number, userId: number) {
    return del<void>(`/groups/${groupId}/members/${userId}`)
  }
}
