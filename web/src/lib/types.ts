// Mirrors the Java records in src/main/java/.../dto. Dates arrive as strings:
// LocalDateTime without offset (UTC on the server), Instant with a trailing Z.

export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED'

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export type UserResponse = {
  id: number
  name: string
  email: string
  createdAt: string
}

export type LoginResponse = {
  token: string
  type: string
  expiresIn: number
}

export type CategoryResponse = {
  id: number
  name: string
  color: string
}

export type TagResponse = {
  id: number
  name: string
}

export type TaskResponse = {
  id: number
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
  category: CategoryResponse | null
  tags: TagResponse[]
  parentTaskId: number | null
}

export type ErrorResponse = {
  timestamp: string
  status: number
  error: string
  message: string
}

export type ValidationErrorResponse = {
  timestamp: string
  status: number
  error: string
  fields: Record<string, string>
}
