import { useEffect, useState } from 'react'
import { apiFetch } from '../api/client'
import { useSession } from '../auth/session'

/** An activity as returned by the API. */
type Activity = {
  readonly id: number
  readonly activityType: string
  readonly durationMinutes: number
  readonly activityDate: string
  readonly user: { readonly id: number } | null
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
     * TODO (after feature/exercise-endpoints merges): replace this with
     *
     *     apiFetch<Activity[]>(`/users/${userId}/activities`)
     *
     * and drop the client-side filter below. That endpoint is being built on
     * another branch, so building it here would conflict. The server already
     * enforces that a token may only read its own /users/{userId}/... path, so
     * the swap needs no other change.
     *
     * Until then this reads the collection endpoint and narrows to the signed-in
     * user here. Note the difference that makes: the filtering is cosmetic, not
     * a security boundary -- the response really does contain every user's rows.
     */
    apiFetch<Activity[]>('/activities')
      .then((activities) => {
        if (!active) {
          return
        }
        setState({
          status: 'ready',
          activities: activities.filter((activity) => activity.user?.id === userId),
        })
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
        <p className="muted">
          You have no recorded activities yet. Once the exercise endpoints are
          merged you will be able to add some here.
        </p>
      )}

      {state.status === 'ready' && state.activities.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Activity</th>
              <th>Duration</th>
            </tr>
          </thead>
          <tbody>
            {state.activities.map((activity) => (
              <tr key={activity.id}>
                <td>{activity.activityDate}</td>
                <td>{activity.activityType}</td>
                <td>{activity.durationMinutes} min</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
