import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'

export default function Orders() {
  const [orders, setOrders] = useState(null)

  useEffect(() => {
    api.listOrders().then((data) => setOrders(data.content)).catch(console.error)
  }, [])

  if (!orders) return <div className="spinner-wrap">Loading…</div>

  if (orders.length === 0) {
    return (
      <div className="empty-state">
        <h2>No orders yet</h2>
        <Link to="/products" className="btn" style={{ marginTop: 20 }}>Start shopping</Link>
      </div>
    )
  }

  return (
    <div className="container" style={{ padding: '48px 24px' }}>
      <h1 className="section-title" style={{ marginTop: 0 }}>Your orders</h1>
      {orders.map((order) => (
        <Link to={`/account/orders/${order.id}`} key={order.id} className="order-card" style={{ display: 'block' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <p style={{ fontWeight: 600 }}>{order.orderNumber}</p>
              <p style={{ fontSize: 13, color: 'var(--color-ink-soft)' }}>{new Date(order.createdAt).toLocaleDateString()} · {order.items.length} item(s)</p>
            </div>
            <div style={{ textAlign: 'right' }}>
              <span className={`status-badge ${order.status}`}>{order.status.replace('_', ' ')}</span>
              <p style={{ fontWeight: 600, marginTop: 8 }}>₹{Number(order.total).toLocaleString('en-IN')}</p>
            </div>
          </div>
        </Link>
      ))}
    </div>
  )
}
