import { Link, NavLink, Outlet } from 'react-router-dom'
import { clearSession, useSession } from '../auth/session'

/** Shared chrome: the nav bar, the session controls, and the routed page. */
export default function Layout() {
  const session = useSession()

  return (
    <div className="layout">
      <nav className="nav">
        <div className="nav-links">
          <NavLink to="/" end>
            Home
          </NavLink>
          <NavLink to="/about">About</NavLink>
          {session !== null && <NavLink to="/activities">Activities</NavLink>}
        </div>

        <div className="nav-session">
          {session === null ? (
            <>
              <Link to="/login">Log in</Link>
              <Link className="button button-small" to="/register">
                Register
              </Link>
            </>
          ) : (
            <>
              <span className="nav-user">
                Signed in as <strong>{session.username}</strong>
              </span>
              {/*
                * Clearing the session is all that is needed. RequireAuth is
                * subscribed to it, so if the current page is protected it
                * redirects to /login on its own.
                */}
              <button className="button button-small" type="button" onClick={clearSession}>
                Log out
              </button>
            </>
          )}
        </div>
      </nav>

      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}
