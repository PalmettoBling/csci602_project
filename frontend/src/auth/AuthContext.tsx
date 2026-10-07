import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as api from '../api/client'

interface AuthContextValue {
    token: string | null
    username: string | null
    isAuthenticated: boolean
    login: (username: string, password: string) => Promise<void>
    register: (username: string, password: string, email: string) => Promise<void>
    logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [token, setTokenState] = useState<string | null>(api.getToken())
    const [username, setUsername] = useState<string | null>(null)

    const handleAuthResponse = (response: api.AuthResponse) => {
        api.setToken(response.token)
        setTokenState(response.token)
        setUsername(response.username)
    }

    const value = useMemo<AuthContextValue>(() => ({
        token,
        username,
        isAuthenticated: token !== null,
        login: async (user: string, password: string) => {
            const response = await api.login(user, password)
            handleAuthResponse(response)
        },
        register: async (user: string, password: string, email: string) => {
            const response = await api.register(user, password, email)
            handleAuthResponse(response)
        },
        logout: () => {
            api.clearToken()
            setTokenState(null)
            setUsername(null)
        },
    }), [token, username])

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext)
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider')
    }
    return context
}
