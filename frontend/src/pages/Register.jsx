import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(form)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="form-card">
      <h2 style={{ marginBottom: 28 }}>Create an account</h2>
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>Full name</label>
          <input required value={form.fullName} onChange={update('fullName')} />
        </div>
        <div className="field">
          <label>Email</label>
          <input type="email" required value={form.email} onChange={update('email')} />
        </div>
        <div className="field">
          <label>Phone (optional)</label>
          <input value={form.phone} onChange={update('phone')} />
        </div>
        <div className="field">
          <label>Password (min 8 characters)</label>
          <input type="password" required minLength={8} value={form.password} onChange={update('password')} />
        </div>
        {error && <p className="error-text">{error}</p>}
        <button className="btn btn-block" disabled={loading}>{loading ? 'Creating…' : 'Create account'}</button>
      </form>
      <p style={{ marginTop: 20, fontSize: 14, textAlign: 'center' }}>
        Already have an account? <Link to="/login" style={{ fontWeight: 600 }}>Sign in</Link>
      </p>
    </div>
  )
}
