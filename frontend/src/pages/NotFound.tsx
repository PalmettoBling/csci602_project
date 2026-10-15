import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div>
      <h1>404</h1>
      <p>That page does not exist.</p>
      <p className="spaced">
        <Link to="/">Back to home</Link>
      </p>
    </div>
  )
}
