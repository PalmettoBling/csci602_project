import { useState } from 'react'
import type { FormEvent } from 'react'
import { apiFetch } from '../api/client'

export type Weather = {
  readonly latitude: number
  readonly longitude: number
  readonly localTime: string
  readonly utcOffset: string
  readonly sampledAt: string
  readonly temperatureC: number
  readonly precipitationMm: number
  readonly windSpeedKmh: number
  readonly weatherCode: number
  readonly fetchedAt: string
}

function conditions(code: number): string {
  if (code === 0) return 'Clear'
  if ([1, 2, 3].includes(code)) return 'Partly cloudy / overcast'
  if ([45, 48].includes(code)) return 'Fog'
  if ([51, 53, 55, 56, 57].includes(code)) return 'Drizzle'
  if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return 'Rain'
  if ([71, 73, 75, 77, 85, 86].includes(code)) return 'Snow'
  if ([95, 96, 99].includes(code)) return 'Thunderstorm'
  return `Weather code ${code}`
}

type Props = {
  readonly userId: number
  readonly activityId: number
  readonly activityDate: string
  readonly weather: Weather | null
  readonly onSaved: (weather: Weather) => void
}

export default function ActivityWeather({ userId, activityId, activityDate, weather, onSaved }: Props) {
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [time, setTime] = useState(weather?.localTime.slice(11, 16) ?? '')
  const [offset, setOffset] = useState(weather?.utcOffset === 'Z' ? '+00:00' : weather?.utcOffset ?? '')
  const [latitude, setLatitude] = useState(weather?.latitude.toString() ?? '')
  const [longitude, setLongitude] = useState(weather?.longitude.toString() ?? '')

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const result = await apiFetch<Weather>(`/users/${userId}/activities/${activityId}/weather`, {
        method: 'POST',
        body: {
          latitude: Number(latitude),
          longitude: Number(longitude),
          occurredAt: `${activityDate}T${time}:00${offset}`,
        },
      })
      onSaved(result)
      setEditing(false)
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : 'Could not attach historical weather.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      {weather ? (
        <div>
          <strong>{conditions(weather.weatherCode)}</strong>
          <div>{weather.temperatureC} °C; rain/snow {weather.precipitationMm} mm; wind {weather.windSpeedKmh} km/h</div>
          <small>
            Activity: {weather.localTime.replace('T', ' ')} (UTC{weather.utcOffset === 'Z' ? '+00:00' : weather.utcOffset})
            <br />
            Location: {weather.latitude}, {weather.longitude}
            <br />
            Hourly estimate: {weather.sampledAt} (UTC)
          </small>
        </div>
      ) : <span className="muted">No weather attached. </span>}
      <button type="button" onClick={() => setEditing(!editing)} disabled={busy}>
        {editing ? 'Cancel' : weather ? 'Change weather details' : 'Attach historical weather'}
      </button>
      {editing && (
        <form className="form" onSubmit={save}>
          <p className="muted">
            Enter the time and UTC offset at the activity location on {activityDate}.
            Only completed activities are supported. Recent weather may take several days to become available.
          </p>
          <label>
            Local activity time
            <input aria-label="Local activity time" type="time" required value={time}
              onChange={(event) => setTime(event.target.value)} disabled={busy} />
          </label>
          <label>
            UTC offset at that date/location (e.g. -04:00)
            <input aria-label="UTC offset" placeholder="-04:00" required pattern="[+-](0[0-9]|1[0-7]):[0-5][0-9]|[+-]18:00"
              value={offset} onChange={(event) => setOffset(event.target.value)} disabled={busy} />
          </label>
          <label>
            Latitude (-90 to 90)
            <input aria-label="Latitude" type="number" min="-90" max="90" step="any" required value={latitude}
              onChange={(event) => setLatitude(event.target.value)} disabled={busy} />
          </label>
          <label>
            Longitude (-180 to 180)
            <input aria-label="Longitude" type="number" min="-180" max="180" step="any" required value={longitude}
              onChange={(event) => setLongitude(event.target.value)} disabled={busy} />
          </label>
          <button type="submit" disabled={busy}>{busy ? 'Loading weather...' : 'Save weather'}</button>
        </form>
      )}
      {error && <p className="banner banner-error" role="alert">{error}</p>}
      {weather && <small>Historical estimates by <a href="https://open-meteo.com/" target="_blank" rel="noreferrer">Open-Meteo</a> (CC BY 4.0), not exact on-site observations.</small>}
    </div>
  )
}
