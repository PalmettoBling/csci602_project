import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiError } from '../api/client'
import {
  createActivity,
  deleteActivity,
  draftFrom,
  emptyDraft,
  listActivities,
  newestFirst,
  updateActivity,
} from '../api/activities'
import type { Activity, ActivityPayload } from '../api/activities'
import ActivityForm from '../components/ActivityForm'
import { useSession } from '../auth/session'
import ActivityWeather from '../components/ActivityWeather'

type LoadState =
  | { readonly status: 'loading' }
  | { readonly status: 'ready'; readonly activities: readonly Activity[] }
  | { readonly status: 'error'; readonly message: string }

export default function Activities() {
  const session = useSession()
  const [state, setState] = useState<LoadState>({ status: 'loading' })

  /** The activity being edited, or null while the form is adding a new one. */
  const [editing, setEditing] = useState<Activity | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  /** The row whose Delete button has been pressed once and awaits confirmation. */
  const [confirmingDeleteId, setConfirmingDeleteId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [rowError, setRowError] = useState<string | null>(null)
  /** Bumped after a successful add, which remounts and so clears the add form. */
  const [addFormGeneration, setAddFormGeneration] = useState(0)

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
    listActivities(userId)
      .then((activities) => {
        if (!active) {
          return
        }
        setState({ status: 'ready', activities: newestFirst(activities) })
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

  /** Replaces the list in place, so no reload is needed after a write. */
  const applyToList = useCallback(
    (change: (activities: readonly Activity[]) => readonly Activity[]) => {
      setState((current) =>
        current.status === 'ready'
          ? { status: 'ready', activities: newestFirst(change(current.activities)) }
          : current,
      )
    },
    [],
  )

  function reportWriteFailure(caught: unknown, fallback: string) {
    if (caught instanceof ApiError) {
      // A 401 already cleared the session; RequireAuth is about to redirect.
      if (caught.status === 401) {
        return
      }
      setFormError(caught.message)
      setFieldErrors(caught.fields ?? {})
      return
    }
    setFormError(fallback)
  }

  async function handleSubmit(payload: ActivityPayload) {
    if (userId === undefined) {
      return
    }

    setSubmitting(true)
    setFormError(null)
    setFieldErrors({})

    try {
      if (editing === null) {
        const created = await createActivity(userId, payload)
        applyToList((activities) => [created, ...activities])
        setAddFormGeneration((generation) => generation + 1)
      } else {
        const updated = await updateActivity(userId, editing, payload)
        applyToList((activities) =>
          activities.map((activity) => (activity.id === updated.id ? updated : activity)),
        )
        setEditing(null)
      }
    } catch (caught) {
      reportWriteFailure(caught, 'Could not save your activity. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(activity: Activity) {
    if (userId === undefined) {
      return
    }

    setDeletingId(activity.id)
    setRowError(null)

    try {
      await deleteActivity(userId, activity.id)
      applyToList((activities) =>
        activities.filter((candidate) => candidate.id !== activity.id),
      )
      if (editing?.id === activity.id) {
        setEditing(null)
      }
      setConfirmingDeleteId(null)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        return
      }
      setRowError(
        caught instanceof Error ? caught.message : 'Could not delete that activity.',
      )
    } finally {
      setDeletingId(null)
    }
  }

  function startEditing(activity: Activity) {
    setEditing(activity)
    setFormError(null)
    setFieldErrors({})
    setConfirmingDeleteId(null)
  }

  function cancelEditing() {
    setEditing(null)
    setFormError(null)
    setFieldErrors({})
  }

  // A stable object, so ActivityForm only resets when the target really changes.
  const initialDraft = useMemo(
    () => (editing === null ? emptyDraft() : draftFrom(editing)),
    [editing],
  )

  return (
    <div>
      <h1>Your Activities</h1>

      <section className="card" aria-labelledby="activity-form-heading">
        <h2 id="activity-form-heading" className="card-heading">
          {editing === null ? 'Add an activity' : 'Edit activity'}
        </h2>

        {formError !== null && (
          <p className="banner banner-error" role="alert">
            {formError}
          </p>
        )}

        <ActivityForm
          // Remounts the form when the target changes, so Edit arrives prefilled
          // and a finished add starts blank -- no state-resetting effect needed.
          key={editing === null ? `add-${addFormGeneration}` : `edit-${editing.id}`}
          initialDraft={initialDraft}
          submitLabel={editing === null ? 'Add activity' : 'Save changes'}
          submittingLabel={editing === null ? 'Adding...' : 'Saving...'}
          submitting={submitting}
          serverFieldErrors={fieldErrors}
          onSubmit={handleSubmit}
          onCancel={editing === null ? undefined : cancelEditing}
        />
      </section>

      {state.status === 'loading' && <p className="muted">Loading your activities...</p>}

      {state.status === 'error' && (
        <p className="banner banner-error" role="alert">
          {state.message}
        </p>
      )}

      {rowError !== null && (
        <p className="banner banner-error" role="alert">
          {rowError}
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
              <th>
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {state.activities.map((activity) => (
              <tr key={activity.id}>
                <td>{activity.activityDate}</td>
                <td>{activity.activityType}</td>
                <td>{activity.durationMinutes} min</td>
                <td>
                  {/* Attaching weather needs the owner id the route is scoped to. */}
                  {userId !== undefined && (
                    <ActivityWeather
                      userId={userId}
                      activityId={activity.id}
                      activityDate={activity.activityDate}
                      weather={activity.weather}
                      onSaved={(weather) =>
                        applyToList((activities) =>
                          activities.map((item) =>
                            item.id === activity.id ? { ...item, weather } : item,
                          ),
                        )
                      }
                    />
                  )}
                </td>
                <td>
                  {confirmingDeleteId === activity.id ? (
                    <div className="row-actions">
                      <span className="small">Delete this activity?</span>
                      <button
                        className="button button-small button-danger"
                        type="button"
                        onClick={() => void handleDelete(activity)}
                        disabled={deletingId === activity.id}
                      >
                        {deletingId === activity.id ? 'Deleting...' : 'Yes, delete'}
                      </button>
                      <button
                        className="button button-small button-secondary"
                        type="button"
                        onClick={() => setConfirmingDeleteId(null)}
                        disabled={deletingId === activity.id}
                      >
                        Keep
                      </button>
                    </div>
                  ) : (
                    <div className="row-actions">
                      <button
                        className="button button-small button-secondary"
                        type="button"
                        onClick={() => startEditing(activity)}
                      >
                        Edit
                      </button>
                      <button
                        className="button button-small button-secondary"
                        type="button"
                        onClick={() => setConfirmingDeleteId(activity.id)}
                      >
                        Delete
                      </button>
                    </div>
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
