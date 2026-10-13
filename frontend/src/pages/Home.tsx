import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetchText } from '../api/client'
import { useSession } from '../auth/session'

type LoadState =
  | { readonly status: 'loading' }
  | { readonly status: 'ready'; readonly code: string }
  | { readonly status: 'error'; readonly message: string }

export default function Home() {
  const session = useSession()
  const [state, setState] = useState<LoadState>({ status: 'loading' })

  useEffect(() => {
    let active = true

    // The verification endpoint is public: it has to answer before anyone has
    // logged in, so it is on the allowlist in SecurityConfig.
    apiFetchText('/verification', { authenticated: false })
      .then((code) => {
        if (active) {
          setState({ status: 'ready', code })
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error', message: 'Could not connect to backend' })
        }
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <div>
      <h1>Semester Project</h1>
      <p>Welcome to our CSCI 602 semester project.</p>

      <h2>Backend Connection</h2>
      <p>
        Verification Code:{' '}
        {state.status === 'loading' && <span className="muted">Loading...</span>}
        {state.status === 'error' && <span className="error-text">{state.message}</span>}
        {state.status === 'ready' && <code>{state.code}</code>}
      </p>

      <p className="spaced">
        {session === null ? (
          <>
            <Link to="/login">Log in</Link> or <Link to="/register">create an account</Link> to
            see your activities.
          </>
        ) : (
          <Link to="/activities">View your activities</Link>
        )}
      </p>
    </div>
  )
}
