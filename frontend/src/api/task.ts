import request from '../utils/request'
import type { PageData } from '@/types/api'
import type { Task, TaskFormData, TaskSearchParams } from '@/types/task'

export function getTaskList(params: { page?: number; size?: number }) {
  return request.get('/tasks', { params }) as Promise<PageData<Task>>
}

export function getTaskDetail(id: number) {
  return request.get(`/tasks/${id}`) as Promise<Task>
}

export function createTask(data: TaskFormData) {
  return request.post('/tasks', data) as Promise<Task>
}

export function updateTask(id: number, data: Partial<Task>) {
  return request.put(`/tasks/${id}`, data) as Promise<Task>
}

export function deleteTask(id: number) {
  return request.delete(`/tasks/${id}`)
}

export function toggleFavorite(id: number) {
  return request.post(`/tasks/${id}/favorite`)
}

export function getFavoriteIds() {
  return request.get('/tasks/favorites') as Promise<number[]>
}

export function getFavoriteTasks() {
  return request.get('/tasks/favorite-list') as Promise<Task[]>
}
