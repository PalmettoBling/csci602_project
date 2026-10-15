import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import { ApiError } from '../api/client'

export default function Register() {
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Per-field messages straight from the server's 400 body.
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      await register({ name, username, email, password })
      // Registration does not hand back a token, so send the user to log in.
      navigate('/login', {
        replace: true,
        state: { from: '/activities' },
      })
    } catch (caught) {
      if (caught instanceof ApiError) {
        setError(caught.message)
        setFieldErrors(caught.fields ?? {})
      } else {
        setError('Something went wrong. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="narrow">
      <h1>Register</h1>

      {error !== null && (
        <p className="banner banner-error" role="alert">
          {error}
        </p>
      )}

      <form className="form" onSubmit={handleSubmit}>
        <label htmlFor="register-name">Name</label>
        <input
          id="register-name"
          name="name"
          autoComplete="name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          required
        />
        {fieldErrors.name !== undefined && (
          <span className="field-error">{fieldErrors.name}</span>
        )}

        <label htmlFor="register-username">Username</label>
        <input
          id="register-username"
          name="username"
          autoComplete="username"
          maxLength={50}
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          required
        />
        {fieldErrors.username !== undefined && (
          <span className="field-error">{fieldErrors.username}</span>
        )}

        <label htmlFor="register-email">Email</label>
        <input
          id="register-email"
          name="email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />
        {fieldErrors.email !== undefined && (
          <span className="field-error">{fieldErrors.email}</span>
        )}

        <label htmlFor="register-password">Password</label>
        <input
          id="register-password"
          name="password"
          type="password"
          autoComplete="new-password"
          minLength={8}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
        {fieldErrors.password !== undefined && (
          <span className="field-error">{fieldErrors.password}</span>
        )}
        <span className="muted small">At least 8 characters.</span>

        <button className="button" type="submit" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Create account'}
        </button>
      </form>

      <p className="spaced muted">
        Already registered? <Link to="/login">Log in</Link>
      </p>
    </div>
  )
}
