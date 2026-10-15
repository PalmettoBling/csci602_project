import { clearSession, getSession } from '../auth/session'

/*
 * Where the API lives.
 *
 * Empty by default, so every request is same-origin and the dev proxy in
 * vite.config.ts forwards it to the Spring Boot app on port 5001. That keeps the
 * dev setup free of CORS entirely. Set VITE_API_BASE_URL (for example
 * https://csci-602-api.onrender.com) when the API is served from another host,
 * such as the Render deployment -- the backend already allows the configured
 * browser origin via security.cors.allowed-origins.
 *
 * Either way there is no hardcoded localhost URL anywhere in src/.
 */
const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''

/** An API response that was not a 2xx. */
export class ApiError extends Error {
  readonly status: number
  /** Per-field validation messages, when the server sent any (a 400). */
  readonly fields: Record<string, string> | undefined

  constructor(status: number, message: string, fields?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fields = fields
  }
}

export type RequestOptions = {
  readonly method?: string
  readonly body?: unknown
  /**
   * Attach the bearer token. On by default; the register and login calls turn
   * it off since they are what produce a token in the first place.
   */
  readonly authenticated?: boolean
  /**
   * Drop the stored session when the server answers 401. On by default, and
   * turned off for login so that a simple wrong password does not look like an
   * expired session.
   */
  readonly clearSessionOnUnauthorized?: boolean
}

/**
 * The single place a request leaves this app, and the single place an
 * Authorization header is attached.
 */
async function request(path: string, options: RequestOptions): Promise<Response> {
  const {
    method = 'GET',
    body,
    authenticated = true,
    clearSessionOnUnauthorized = true,
  } = options

  const headers: Record<string, string> = { Accept: 'application/json, text/plain' }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (authenticated) {
    const session = getSession()
    if (session !== null) {
      headers.Authorization = `Bearer ${session.token}`
    }
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    // A DNS failure, a refused connection, a dev server with no backend behind
    // it: there is no status code to report, so say something actionable.
    throw new ApiError(0, 'Could not reach the server. Is the API running?')
  }

  if (response.status === 401 && clearSessionOnUnauthorized) {
    /*
     * The token is missing, expired, or was rejected. Drop it. RequireAuth
     * subscribes to the session store, so clearing it immediately re-renders
     * the protected route and sends the user to /login -- the redirect falls
     * out of the state change rather than being pushed from here.
     */
    clearSession()
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  return response
}

/** Builds an ApiError from the server's JSON error envelope where possible. */
async function toApiError(response: Response): Promise<ApiError> {
  let message = `Request failed with status ${response.status}.`
  let fields: Record<string, string> | undefined

  try {
    const text = await response.text()
    if (text.length > 0) {
      const parsed: unknown = JSON.parse(text)
      if (typeof parsed === 'object' && parsed !== null) {
        const envelope = parsed as { message?: unknown; fields?: unknown }
        if (typeof envelope.message === 'string' && envelope.message.length > 0) {
          message = envelope.message
        }
        if (typeof envelope.fields === 'object' && envelope.fields !== null) {
          fields = envelope.fields as Record<string, string>
        }
      }
    }
  } catch {
    // Not JSON, or an empty body. The generic message above still applies.
  }

  return new ApiError(response.status, message, fields)
}

/** Calls an endpoint that answers with JSON. */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await request(path, options)

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

/** Calls an endpoint that answers with a bare string, such as /verification. */
export async function apiFetchText(
  path: string,
  options: RequestOptions = {},
): Promise<string> {
  const response = await request(path, options)
  return await response.text()
}
