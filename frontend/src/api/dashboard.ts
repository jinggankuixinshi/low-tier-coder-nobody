import request from '../utils/request'

export function getDashboardStats() {
  return request.get('/dashboard/stats')
}

export function getRecentTasks() {
  return request.get('/dashboard/recent-tasks')
}

export function getRecentSubmissions() {
  return request.get('/dashboard/recent-submissions')
}
