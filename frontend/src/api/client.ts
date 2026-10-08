import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import type { Session } from './types'

/**
 * HTTP client for the TaskFlow API. The access token lives only in memory; the refresh token is an
 * HttpOnly cookie the browser sends to /api/auth/*. On a 401 the client refreshes once and retries.
 */

let accessToken: string | null = null
let sessionExpiredHandler: (() => void) | null = null
let refreshInFlight: Promise<Session | null> | null = null

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function onSessionExpired(handler: () => void) {
  sessionExpiredHandler = handler
}

/** Error with a user-friendly message; raw technical details are never shown (claude.md §37). */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>
  readonly requestId?: string

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}, requestId?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.requestId = requestId
  }

  get isNetworkError() {
    return this.status === 0
  }
}

interface ProblemBody {
  detail?: string
  code?: string
  fieldErrors?: Record<string, string>
  requestId?: string
}

function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return new ApiError(0, 'NETWORK_ERROR', "Can't reach TaskFlow. Check your connection and try again.")
    }
    const body = (error.response.data ?? {}) as ProblemBody
    const status = error.response.status
    const fallback =
      status >= 500 ? 'Something went wrong on our side. Please try again.' : 'The request could not be completed.'
    return new ApiError(status, body.code ?? 'ERROR', body.detail ?? fallback, body.fieldErrors ?? {}, body.requestId)
  }
  return new ApiError(0, 'UNKNOWN', 'Something unexpected happened. Please try again.')
}

const baseConfig: AxiosRequestConfig = {
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20_000,
}

/** Used for the cookie-based auth calls, so they never recurse through the refresh logic. */
export const authHttp = axios.create(baseConfig)

export const http = axios.create(baseConfig)

/** Exchanges the refresh cookie for a new access token; concurrent callers share one request. */
export function refreshSession(): Promise<Session | null> {
  refreshInFlight ??= authHttp
    .post<Session>('/auth/refresh')
    .then((response) => {
      accessToken = response.data.accessToken
      return response.data
    })
    .catch(() => {
      accessToken = null
      return null
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

http.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined
    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true
      const session = await refreshSession()
      if (session) {
        original.headers.Authorization = `Bearer ${session.accessToken}`
        return http(original)
      }
      sessionExpiredHandler?.()
    }
    return Promise.reject(toApiError(error))
  },
)

authHttp.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(toApiError(error)),
)

/** Seconds until the in-memory access token expires (0 when missing or unreadable). */
export function accessTokenTtlSeconds(): number {
  if (!accessToken) return 0
  try {
    const payload = JSON.parse(atob(accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return Math.max(0, payload.exp - Date.now() / 1000)
  } catch {
    return 0
  }
}
