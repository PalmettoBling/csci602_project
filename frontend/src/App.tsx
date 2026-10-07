import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { RequireAuth } from './auth/RequireAuth'
import { Login } from './pages/Login'
import { Register } from './pages/Register'
import { Users } from './pages/Users'

function Home() {
    const [verification, setVerification] = useState('Loading...')
    const { isAuthenticated } = useAuth()

    useEffect(() => {
        fetch('http://localhost:5001/verification')
            .then(response => response.text())
            .then(data => setVerification(data))
            .catch(() => setVerification('Could not connect to backend'))
    }, [])

    return (
        <div>
            <h1>Semester Project</h1>
            <p>Welcome to our CSCI 602 semester project.</p>

            <h2>Backend Connection</h2>
            <p>Verification Code: {verification}</p>

            <nav>
                <Link to="/about">About</Link>
                {' | '}
                {isAuthenticated ? (
                    <Link to="/users">Users</Link>
                ) : (
                    <>
                        <Link to="/login">Log In</Link>
                        {' | '}
                        <Link to="/register">Register</Link>
                    </>
                )}
            </nav>
        </div>
    )
}

function About() {
    return (
        <div>
            <h1>About</h1>
            <p>This application was created for CSCI 602.</p>
            <Link to="/">Home</Link>
        </div>
    )
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/about" element={<About />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route
                        path="/users"
                        element={
                            <RequireAuth>
                                <Users />
                            </RequireAuth>
                        }
                    />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    )
}

export default App