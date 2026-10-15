import { apiFetch } from './client'

/** Shape of a successful POST /auth/login. */
export type LoginResult = {
  readonly token: string
  readonly tokenType: string
  readonly expiresInSeconds: number
  readonly userId: number
  readonly username: string
}

/** Shape of a successful POST /auth/register. Never includes a password hash. */
export type RegisteredUser = {
  readonly id: number
  readonly name: string
  readonly username: string
  readonly email: string
}

export type RegistrationInput = {
  readonly name: string
  readonly username: string
  readonly email: string
  readonly password: string
}

export function login(username: string, password: string): Promise<LoginResult> {
  return apiFetch<LoginResult>('/auth/login', {
    method: 'POST',
    body: { username, password },
    // No token to send, and a wrong password must not read as a dead session.
    authenticated: false,
    clearSessionOnUnauthorized: false,
  })
}

export function register(input: RegistrationInput): Promise<RegisteredUser> {
  return apiFetch<RegisteredUser>('/auth/register', {
    method: 'POST',
    body: input,
    authenticated: false,
  })
}
