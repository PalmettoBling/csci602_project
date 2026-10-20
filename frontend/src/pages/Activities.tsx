import { useEffect, useState } from 'react'
import { apiFetch } from '../api/client'
import { useSession } from '../auth/session'
import ActivityWeather from '../components/ActivityWeather'
import type { Weather } from '../components/ActivityWeather'

/**
 * An activity as returned by GET /users/{userId}/activities. The response also
 * carries the owning user, but the endpoint is already scoped to one user so
 * there is nothing here that needs to read it.
 */
type Activity = {
  readonly id: number
  readonly activityType: string
  readonly durationMinutes: number
  readonly activityDate: string
  readonly weather: Weather | null
}

type LoadState =
  | { readonly status: 'loading' }
  | { readonly status: 'ready'; readonly activities: readonly Activity[] }
  | { readonly status: 'error'; readonly message: string }

export default function Activities() {
  const session = useSession()
  const [state, setState] = useState<LoadState>({ status: 'loading' })

  // RequireAuth guarantees a session before this renders, but the effect below
  // depends on the id, so read it defensively rather than asserting.
  const userId = session?.userId

  useEffect(() => {
    if (userId === undefined) {
      return
    }

    let active = true

    /*
     * The user-scoped endpoint, served by UserActivityEndpoints. The server
     * returns only this user's rows and the security rule on /users/{userId}/**
     * rejects a token whose subject is a different id with a 403, so there is no
     * client-side filtering to do -- and none that could be trusted anyway.
     */
    apiFetch<Activity[]>(`/users/${userId}/activities`)
      .then((activities) => {
        if (!active) {
          return
        }
        setState({ status: 'ready', activities })
      })
      .catch((caught: unknown) => {
        if (!active) {
          return
        }
        // A 401 has already cleared the session inside the fetch helper, which
        // sends RequireAuth to /login; there is nothing useful to show here.
        setState({
          status: 'error',
          message:
            caught instanceof Error ? caught.message : 'Could not load your activities.',
        })
      })

    return () => {
      active = false
    }
  }, [userId])

  return (
    <div>
      <h1>Your Activities</h1>

      {state.status === 'loading' && <p className="muted">Loading your activities...</p>}

      {state.status === 'error' && (
        <p className="banner banner-error" role="alert">
          {state.message}
        </p>
      )}

      {state.status === 'ready' && state.activities.length === 0 && (
        <p className="muted">You have no recorded activities yet.</p>
      )}

      {state.status === 'ready' && state.activities.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Activity</th>
              <th>Duration</th>
              <th>Weather</th>
            </tr>
          </thead>
          <tbody>
            {state.activities.map((activity) => (
              <tr key={activity.id}>
                <td>{activity.activityDate}</td>
                <td>{activity.activityType}</td>
                <td>{activity.durationMinutes} min</td>
                <td>
                  {userId !== undefined && (
                    <ActivityWeather
                      userId={userId}
                      activityId={activity.id}
                      activityDate={activity.activityDate}
                      weather={activity.weather}
                      onSaved={(weather) => setState((current) => current.status === 'ready' ? {
                        ...current,
                        activities: current.activities.map((item) => item.id === activity.id ? { ...item, weather } : item),
                      } : current)}
                    />
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
