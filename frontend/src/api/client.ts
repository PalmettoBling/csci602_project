const API_BASE_URL = 'http://localhost:5001'

const TOKEN_STORAGE_KEY = 'authToken'

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function setToken(token: string): void {
    localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearToken(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export class ApiError extends Error {
    status: number

    constructor(status: number, message: string) {
        super(message)
        this.status = status
    }
}

async function request<T>(path: string, options: RequestInit = {}, auth = false): Promise<T> {
    const headers = new Headers(options.headers)
    headers.set('Content-Type', 'application/json')

    if (auth) {
        const token = getToken()
        if (token) {
            headers.set('Authorization', `Bearer ${token}`)
        }
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers,
    })

    const text = await response.text()
    const data = text ? JSON.parse(text) : null

    if (!response.ok) {
        const message = data && data.error ? data.error : `Request failed with status ${response.status}`
        throw new ApiError(response.status, message)
    }

    return data as T
}

export interface AuthResponse {
    token: string
    username: string
}

export function register(username: string, password: string, email: string): Promise<AuthResponse> {
    return request<AuthResponse>('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, password, email }),
    })
}

export function login(username: string, password: string): Promise<AuthResponse> {
    return request<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
    })
}

export interface ApiUser {
    id: number
    name: string
    email: string
}

export function getUsers(): Promise<ApiUser[]> {
    return request<ApiUser[]>('/users', { method: 'GET' }, true)
}
