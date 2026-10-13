import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useSession } from './session'

/**
 * Layout route that gates everything nested inside it.
 *
 * Because it reads the session through useSession, it reacts to the token being
 * cleared from anywhere -- the logout button, or the shared fetch helper on a
 * 401 -- and redirects without anyone having to call navigate() imperatively.
 * The path being left is remembered so login can send the user back to it.
 */
export default function RequireAuth() {
  const session = useSession()
  const location = useLocation()

  if (session === null) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
