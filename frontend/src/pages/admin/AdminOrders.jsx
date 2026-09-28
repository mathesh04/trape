import { useEffect, useState } from 'react'
import { api } from '../../api/client'

const STATUSES = ['CREATED', 'PAYMENT_PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED', 'FAILED']

export default function AdminOrders() {
  const [orders, setOrders] = useState([])
  const [filter, setFilter] = useState('')

  const load = () => api.adminListOrders(filter).then((data) => setOrders(data.content)).catch(console.error)
  useEffect(load, [filter])

  const handleStatusChange = async (id, status) => {
    try {
      await api.adminUpdateOrderStatus(id, { status, note: `Updated to ${status} by admin` })
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <div>
      <h2>Orders</h2>
      <select value={filter} onChange={(e) => setFilter(e.target.value)} style={{ marginBottom: 20, padding: 8 }}>
        <option value="">All statuses</option>
        {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
      </select>
      <table>
        <thead><tr><th>Order #</th><th>Total</th><th>Status</th><th>Update</th></tr></thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td>{o.orderNumber}</td>
              <td>₹{o.total}</td>
              <td><span className={`status-badge ${o.status}`}>{o.status}</span></td>
              <td>
                <select defaultValue="" onChange={(e) => e.target.value && handleStatusChange(o.id, e.target.value)}>
                  <option value="">Change to…</option>
                  {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
