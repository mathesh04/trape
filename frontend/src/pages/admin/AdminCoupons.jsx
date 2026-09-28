import { useEffect, useState } from 'react'
import { api } from '../../api/client'

export default function AdminCoupons() {
  const [coupons, setCoupons] = useState([])
  const [form, setForm] = useState({ code: '', discountType: 'PERCENT', discountValue: '', minCartValue: '', validFrom: '', validUntil: '', usageLimit: '' })
  const [message, setMessage] = useState('')

  const load = () => api.adminListCoupons().then(setCoupons).catch(console.error)
  useEffect(load, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.adminCreateCoupon({
        ...form,
        discountValue: Number(form.discountValue),
        minCartValue: form.minCartValue ? Number(form.minCartValue) : 0,
        usageLimit: form.usageLimit ? Number(form.usageLimit) : null,
        validFrom: new Date(form.validFrom).toISOString(),
        validUntil: new Date(form.validUntil).toISOString(),
      })
      setMessage('Coupon created.')
      load()
    } catch (err) {
      setMessage(err.message)
    }
  }

  return (
    <div>
      <h2>Coupons</h2>
      <table style={{ marginBottom: 32 }}>
        <thead><tr><th>Code</th><th>Discount</th><th>Min cart</th><th>Used</th></tr></thead>
        <tbody>
          {coupons.map((c) => (
            <tr key={c.id}>
              <td>{c.code}</td>
              <td>{c.discountType === 'PERCENT' ? `${c.discountValue}%` : `₹${c.discountValue}`}</td>
              <td>₹{c.minCartValue}</td>
              <td>{c.usageCount}{c.usageLimit ? ` / ${c.usageLimit}` : ''}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Create coupon</h3>
      <form onSubmit={handleSubmit} style={{ maxWidth: 420 }}>
        <div className="field"><label>Code</label><input required value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></div>
        <div className="field">
          <label>Type</label>
          <select value={form.discountType} onChange={(e) => setForm({ ...form, discountType: e.target.value })}>
            <option value="PERCENT">Percent</option>
            <option value="FLAT">Flat</option>
          </select>
        </div>
        <div className="field"><label>Value</label><input required type="number" value={form.discountValue} onChange={(e) => setForm({ ...form, discountValue: e.target.value })} /></div>
        <div className="field"><label>Minimum cart value (₹)</label><input type="number" value={form.minCartValue} onChange={(e) => setForm({ ...form, minCartValue: e.target.value })} /></div>
        <div className="field"><label>Valid from</label><input required type="date" value={form.validFrom} onChange={(e) => setForm({ ...form, validFrom: e.target.value })} /></div>
        <div className="field"><label>Valid until</label><input required type="date" value={form.validUntil} onChange={(e) => setForm({ ...form, validUntil: e.target.value })} /></div>
        <div className="field"><label>Usage limit (optional)</label><input type="number" value={form.usageLimit} onChange={(e) => setForm({ ...form, usageLimit: e.target.value })} /></div>
        {message && <p>{message}</p>}
        <button className="btn">Create coupon</button>
      </form>
    </div>
  )
}
