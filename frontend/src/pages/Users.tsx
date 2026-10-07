import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getUsers, ApiError } from '../api/client'
import type { ApiUser } from '../api/client'

export function Users() {
    const [users, setUsers] = useState<ApiUser[]>([])
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const { username, logout } = useAuth()
    const navigate = useNavigate()

    useEffect(() => {
        getUsers()
            .then(setUsers)
            .catch((err) => {
                if (err instanceof ApiError && err.status === 401) {
                    logout()
                    navigate('/login')
                    return
                }
                setError(err instanceof ApiError ? err.message : 'Unable to load users')
            })
            .finally(() => setLoading(false))
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        <div>
            <h1>Users</h1>
            <p>Logged in as {username ?? 'unknown'}</p>
            <button type="button" onClick={handleLogout}>
                Log Out
            </button>

            {loading && <p>Loading users...</p>}
            {error && <p role="alert">{error}</p>}

            {!loading && !error && (
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map((user) => (
                            <tr key={user.id}>
                                <td>{user.id}</td>
                                <td>{user.name}</td>
                                <td>{user.email}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            <Link to="/">Home</Link>
        </div>
    )
}
