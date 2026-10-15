import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './App.css'
import RequireAuth from './auth/RequireAuth'
import Layout from './components/Layout'
import About from './pages/About'
import Activities from './pages/Activities'
import Home from './pages/Home'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import Register from './pages/Register'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />
          <Route path="/about" element={<About />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Everything nested here needs a valid session. */}
          <Route element={<RequireAuth />}>
            <Route path="/activities" element={<Activities />} />
          </Route>

          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
