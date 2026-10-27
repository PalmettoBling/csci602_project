import { useState } from 'react'
import type { FormEvent } from 'react'
import {
  ACTIVITY_TYPE_MAX_LENGTH,
  EARLIEST_ACTIVITY_DATE,
  MAX_DURATION_MINUTES,
  MIN_DURATION_MINUTES,
  todayIso,
  validateDraft,
} from '../api/activities'
import type { ActivityDraft, ActivityPayload } from '../api/activities'

type ActivityFormProps = {
  /**
   * Starting values, read once. The caller gives this component a `key` that
   * changes when the target activity changes, so switching between adding and
   * editing remounts the form with fresh state instead of resetting it from an
   * effect.
   */
  readonly initialDraft: ActivityDraft
  readonly submitLabel: string
  readonly submittingLabel: string
  readonly submitting: boolean
  /** Per-field messages from the server's 400 body, shown beneath the inputs. */
  readonly serverFieldErrors: Record<string, string>
  readonly onSubmit: (payload: ActivityPayload) => void
  /** Present only while editing; absent for the add form. */
  readonly onCancel?: () => void
}

/**
 * The add/edit form, shared by both so one set of rules and one layout covers
 * them. Client-side validation mirrors the server's, but the server re-checks
 * everything and its `fields` messages are rendered in the same places.
 */
export default function ActivityForm({
  initialDraft,
  submitLabel,
  submittingLabel,
  submitting,
  serverFieldErrors,
  onSubmit,
  onCancel,
}: ActivityFormProps) {
  const [draft, setDraft] = useState<ActivityDraft>(initialDraft)
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({})
  // Errors stay hidden until the first submit, so a blank form is not shouting.
  const [showErrors, setShowErrors] = useState(false)

  function update(field: keyof ActivityDraft, value: string) {
    const next = { ...draft, [field]: value }
    setDraft(next)
    if (showErrors) {
      setClientErrors(validateDraft(next))
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const errors = validateDraft(draft)
    setClientErrors(errors)
    setShowErrors(true)

    if (Object.keys(errors).length > 0) {
      return
    }

    onSubmit({
      activityType: draft.activityType.trim(),
      durationMinutes: Number(draft.durationMinutes.trim()),
      activityDate: draft.activityDate.trim(),
    })
  }

  /** The client message if there is one, otherwise whatever the server said. */
  function messageFor(field: keyof ActivityDraft): string | undefined {
    if (showErrors && clientErrors[field] !== undefined) {
      return clientErrors[field]
    }
    return serverFieldErrors[field]
  }

  function fieldError(field: keyof ActivityDraft) {
    const message = messageFor(field)
    if (message === undefined) {
      return null
    }
    return <span className="field-error">{message}</span>
  }

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      <label htmlFor="activity-type">Exercise type</label>
      <input
        id="activity-type"
        name="activityType"
        maxLength={ACTIVITY_TYPE_MAX_LENGTH}
        value={draft.activityType}
        onChange={(event) => update('activityType', event.target.value)}
        aria-invalid={messageFor('activityType') !== undefined}
        disabled={submitting}
      />
      {fieldError('activityType')}

      <label htmlFor="activity-duration">Duration (minutes)</label>
      <input
        id="activity-duration"
        name="durationMinutes"
        type="number"
        inputMode="numeric"
        min={MIN_DURATION_MINUTES}
        max={MAX_DURATION_MINUTES}
        step={1}
        value={draft.durationMinutes}
        onChange={(event) => update('durationMinutes', event.target.value)}
        aria-invalid={messageFor('durationMinutes') !== undefined}
        disabled={submitting}
      />
      {fieldError('durationMinutes')}
      <span className="muted small">
        Between {MIN_DURATION_MINUTES} and {MAX_DURATION_MINUTES} minutes.
      </span>

      <label htmlFor="activity-date">Date</label>
      <input
        id="activity-date"
        name="activityDate"
        type="date"
        min={EARLIEST_ACTIVITY_DATE}
        max={todayIso()}
        value={draft.activityDate}
        onChange={(event) => update('activityDate', event.target.value)}
        aria-invalid={messageFor('activityDate') !== undefined}
        disabled={submitting}
      />
      {fieldError('activityDate')}
      <span className="muted small">Today or earlier.</span>

      {/* Any server message whose field this form has no input for. */}
      {Object.entries(serverFieldErrors)
        .filter(([field]) => !(field in draft))
        .map(([field, message]) => (
          <span className="field-error" key={field}>
            {message}
          </span>
        ))}

      <div className="row-actions">
        <button className="button" type="submit" disabled={submitting}>
          {submitting ? submittingLabel : submitLabel}
        </button>
        {onCancel !== undefined && (
          <button
            className="button button-secondary"
            type="button"
            onClick={onCancel}
            disabled={submitting}
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
