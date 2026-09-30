import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'

function Home() {
    const [verification, setVerification] = useState('Loading...')

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

            <Link to="/about">About</Link>
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
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/about" element={<About />} />
            </Routes>
        </BrowserRouter>
    )
}

export default App