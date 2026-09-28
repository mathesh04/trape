import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Account() {
  const { user } = useAuth()
  const [addresses, setAddresses] = useState([])
  const [showAddForm, setShowAddForm] = useState(false)
  const [form, setForm] = useState({ label: 'Home', line1: '', line2: '', city: '', state: '', pincode: '', phone: '', isDefault: false })

  const load = () => api.listAddresses().then(setAddresses).catch(console.error)
  useEffect(() => { load() }, [])

  const handleAdd = async (e) => {
    e.preventDefault()
    await api.addAddress(form)
    setForm({ label: 'Home', line1: '', line2: '', city: '', state: '', pincode: '', phone: '', isDefault: false })
    setShowAddForm(false)
    load()
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this address?')) return
    await api.deleteAddress(id)
    load()
  }

  return (
    <div className="container" style={{ maxWidth: 640, padding: '48px 24px' }}>
      <h1 className="section-title" style={{ marginTop: 0 }}>My account</h1>
      <div style={{ marginBottom: 32 }}>
        <p><strong>{user?.fullName}</strong></p>
        <p style={{ color: 'var(--color-ink-soft)' }}>{user?.email}</p>
      </div>

      <h3 style={{ marginBottom: 12 }}>Saved addresses</h3>
      {addresses.map((addr) => (
        <div key={addr.id} className="address-card">
          <p style={{ fontWeight: 600 }}>{addr.label} {addr.isDefault && '(Default)'}</p>
          <p style={{ fontSize: 14, color: 'var(--color-ink-soft)' }}>{addr.line1}, {addr.city}, {addr.state} {addr.pincode}</p>
          <button onClick={() => handleDelete(addr.id)} style={{ background: 'none', border: 'none', color: 'var(--color-danger)', fontSize: 12, marginTop: 8 }}>Delete</button>
        </div>
      ))}

      {!showAddForm ? (
        <button className="btn btn-outline btn-sm" onClick={() => setShowAddForm(true)}>+ Add address</button>
      ) : (
        <form onSubmit={handleAdd} style={{ border: '1px solid var(--color-border)', padding: 20, marginTop: 12 }}>
          <div className="field"><label>Label</label><input value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} /></div>
          <div className="field"><label>Address line 1</label><input required value={form.line1} onChange={(e) => setForm({ ...form, line1: e.target.value })} /></div>
          <div className="field"><label>City</label><input required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} /></div>
          <div className="field"><label>State</label><input required value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} /></div>
          <div className="field"><label>Pincode</label><input required value={form.pincode} onChange={(e) => setForm({ ...form, pincode: e.target.value })} /></div>
          <div className="field"><label>Phone</label><input required value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
          <button className="btn btn-sm">Save</button>
        </form>
      )}
    </div>
  )
}
