import { apiFetch } from './client'

/**
 * An activity as the API returns it.
 *
 * <p>The write routes (POST and PUT /users/{userId}/activities) answer with
 * exactly these four fields. The read routes currently serialize the entity and
 * so also carry the owning user, which this app has no use for -- the endpoint
 * is already scoped to one user -- so it is deliberately not modelled here.
 */
export type Activity = {
  readonly id: number
  readonly activityType: string
  readonly durationMinutes: number
  readonly activityDate: string
}

/** The body the write routes accept. There is no owner field: the path decides. */
export type ActivityPayload = {
  readonly activityType: string
  readonly durationMinutes: number
  readonly activityDate: string
}

/** What the form holds while the user is typing: strings, not numbers. */
export type ActivityDraft = {
  readonly activityType: string
  readonly durationMinutes: string
  readonly activityDate: string
}

/*
 * These mirror the server's Bean Validation rules in ActivityWriteRequest.
 * Client-side checks are a convenience that saves a round trip and gives
 * immediate feedback; the server re-validates everything regardless, so these
 * constants being out of date would be a UX bug, never a security hole.
 */
export const ACTIVITY_TYPE_MAX_LENGTH = 100
export const MIN_DURATION_MINUTES = 1
export const MAX_DURATION_MINUTES = 1440
export const EARLIEST_ACTIVITY_DATE = '1900-01-01'

/** Today in the yyyy-MM-dd form both the date input and the API use. */
export function todayIso(): string {
  const now = new Date()
  // Built from local date parts rather than toISOString(), which converts to UTC
  // and can land on yesterday or tomorrow depending on the user's offset.
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function emptyDraft(): ActivityDraft {
  return { activityType: '', durationMinutes: '', activityDate: todayIso() }
}

export function draftFrom(activity: Activity): ActivityDraft {
  return {
    activityType: activity.activityType,
    durationMinutes: String(activity.durationMinutes),
    activityDate: activity.activityDate,
  }
}

/**
 * Validates a draft the same way the server does, keyed by field name so the
 * messages can be rendered in the same places as the server's `fields` map.
 * An empty result means the draft is submittable.
 */
export function validateDraft(draft: ActivityDraft): Record<string, string> {
  const errors: Record<string, string> = {}

  const activityType = draft.activityType.trim()
  if (activityType.length === 0) {
    errors.activityType = 'activityType is required'
  } else if (activityType.length > ACTIVITY_TYPE_MAX_LENGTH) {
    errors.activityType = `activityType must be at most ${ACTIVITY_TYPE_MAX_LENGTH} characters`
  }

  const duration = draft.durationMinutes.trim()
  if (duration.length === 0) {
    errors.durationMinutes = 'durationMinutes is required'
  } else if (!/^-?\d+$/.test(duration)) {
    errors.durationMinutes = 'durationMinutes must be a whole number of minutes'
  } else {
    const minutes = Number(duration)
    if (minutes < MIN_DURATION_MINUTES) {
      errors.durationMinutes = `durationMinutes must be at least ${MIN_DURATION_MINUTES}`
    } else if (minutes > MAX_DURATION_MINUTES) {
      errors.durationMinutes = `durationMinutes must be at most ${MAX_DURATION_MINUTES}`
    }
  }

  const date = draft.activityDate.trim()
  if (date.length === 0) {
    errors.activityDate = 'activityDate is required'
  } else if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    errors.activityDate = 'activityDate must be a yyyy-MM-dd date'
  } else if (date > todayIso()) {
    // String comparison is safe for a zero-padded ISO date and avoids timezone
    // surprises that parsing into a Date would introduce.
    errors.activityDate = 'activityDate must not be in the future'
  } else if (date < EARLIEST_ACTIVITY_DATE) {
    errors.activityDate = `activityDate must be on or after ${EARLIEST_ACTIVITY_DATE}`
  }

  return errors
}

/** Converts a validated draft into the request body. */
export function toPayload(draft: ActivityDraft): ActivityPayload {
  return {
    activityType: draft.activityType.trim(),
    durationMinutes: Number(draft.durationMinutes.trim()),
    activityDate: draft.activityDate.trim(),
  }
}

/*
 * The four calls below are the only places this app names an activity route.
 * Every one is scoped to /users/{userId}/..., which the server checks against
 * the token subject, and apiFetch attaches that token and clears the session on
 * a 401.
 */

export function listActivities(userId: number): Promise<Activity[]> {
  return apiFetch<Activity[]>(`/users/${userId}/activities`)
}

export function createActivity(
  userId: number,
  payload: ActivityPayload,
): Promise<Activity> {
  return apiFetch<Activity>(`/users/${userId}/activities`, {
    method: 'POST',
    body: payload,
  })
}

export function updateActivity(
  userId: number,
  activityId: number,
  payload: ActivityPayload,
): Promise<Activity> {
  return apiFetch<Activity>(`/users/${userId}/activities/${activityId}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteActivity(userId: number, activityId: number): Promise<void> {
  return apiFetch<void>(`/users/${userId}/activities/${activityId}`, {
    method: 'DELETE',
  })
}

/** Newest activity date first, with the higher id winning a tie -- the server's order. */
export function newestFirst(activities: readonly Activity[]): Activity[] {
  return [...activities].sort((left, right) => {
    if (left.activityDate !== right.activityDate) {
      return left.activityDate < right.activityDate ? 1 : -1
    }
    return right.id - left.id
  })
}
