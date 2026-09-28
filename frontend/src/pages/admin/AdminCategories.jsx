import { useEffect, useState } from 'react'
import { api } from '../../api/client'

const emptyForm = { name: '', slug: '', parentId: '', displayOrder: 0 }

export default function AdminCategories() {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    api.listCategories()
      .then(setCategories)
      .catch((err) => setMessage(err.message))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const startEdit = (c) => {
    setEditingId(c.id)
    setForm({ name: c.name, slug: c.slug, parentId: c.parentId || '', displayOrder: c.displayOrder })
    setMessage('')
  }

  const cancelEdit = () => {
    setEditingId(null)
    setForm(emptyForm)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    try {
      const payload = {
        name: form.name,
        slug: form.slug || undefined,
        parentId: form.parentId || null,
        displayOrder: Number(form.displayOrder) || 0,
      }
      if (editingId) {
        await api.adminUpdateCategory(editingId, payload)
        setMessage('Category updated.')
      } else {
        await api.adminCreateCategory(payload)
        setMessage('Category created.')
      }
      cancelEdit()
      load()
    } catch (err) {
      setMessage(err.message)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Remove this category? Products in it will keep their category reference but it will no longer be shown on the storefront.')) return
    try {
      await api.adminDeleteCategory(id)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <div>
      <h2>Categories</h2>

      <div className="admin-card">
        <table>
          <thead><tr><th>Name</th><th>Slug</th><th>Parent</th><th>Order</th><th></th></tr></thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5}>Loading…</td></tr>
            ) : categories.length === 0 ? (
              <tr><td colSpan={5}>No categories yet. Create one below.</td></tr>
            ) : categories.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>{c.slug}</td>
                <td>{categories.find((p) => p.id === c.parentId)?.name || '—'}</td>
                <td>{c.displayOrder}</td>
                <td style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-sm btn-outline" onClick={() => startEdit(c)}>Edit</button>
                  <button className="btn btn-sm btn-danger" onClick={() => handleDelete(c.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="admin-card" style={{ maxWidth: 480 }}>
        <h3 style={{ marginBottom: 16 }}>{editingId ? 'Edit category' : 'Create category'}</h3>
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label>Name</label>
            <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. Kurtis" />
          </div>
          <div className="field">
            <label>Slug (optional — auto-generated if blank)</label>
            <input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder="e.g. kurtis" />
          </div>
          <div className="field">
            <label>Parent category (optional)</label>
            <select value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })}>
              <option value="">— none (top level) —</option>
              {categories.filter((c) => c.id !== editingId).map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Display order</label>
            <input type="number" value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: e.target.value })} />
          </div>

          {message && <p style={{ marginBottom: 16 }}>{message}</p>}
          <div style={{ display: 'flex', gap: 10 }}>
            <button className="btn">{editingId ? 'Save changes' : 'Create category'}</button>
            {editingId && <button type="button" className="btn btn-outline" onClick={cancelEdit}>Cancel</button>}
          </div>
        </form>
      </div>
    </div>
  )
}
