import { useEffect, useState } from 'react'
import { useParams, useLocation } from 'react-router-dom'
import { api } from '../api/client'

export default function OrderDetail() {
  const { id } = useParams()
  const location = useLocation()
  const [order, setOrder] = useState(null)
  const [error, setError] = useState('')

  const load = () => api.getOrder(id).then(setOrder).catch((e) => setError(e.message))

  useEffect(() => { load() }, [id])

  const handleCancel = async () => {
    if (!confirm('Cancel this order?')) return
    try {
      await api.cancelOrder(id)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  if (error) return <div className="empty-state">{error}</div>
  if (!order) return <div className="spinner-wrap">Loading…</div>

  const canCancel = ['CREATED', 'PAYMENT_PENDING', 'PAID', 'PROCESSING'].includes(order.status)

  return (
    <div className="container" style={{ maxWidth: 720, padding: '48px 24px' }}>
      {location.state?.justPlaced && (
        <div style={{ background: '#e3f3e9', color: 'var(--color-success)', padding: 16, marginBottom: 24, borderRadius: 4 }}>
          Payment confirmed — thank you for your order!
        </div>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 className="section-title" style={{ marginTop: 0 }}>{order.orderNumber}</h1>
        <span className={`status-badge ${order.status}`}>{order.status.replace('_', ' ')}</span>
      </div>

      <div style={{ marginTop: 24 }}>
        {order.items.map((item) => (
          <div key={item.id} className="cart-line">
            <div style={{ flex: 1 }}>
              <p style={{ fontWeight: 600 }}>{item.productName}</p>
              <p style={{ fontSize: 13, color: 'var(--color-ink-soft)' }}>{item.variantLabel} · Qty {item.quantity}</p>
            </div>
            <p style={{ fontWeight: 600 }}>₹{Number(item.lineTotal).toLocaleString('en-IN')}</p>
          </div>
        ))}
      </div>

      <div className="cart-summary" style={{ padding: 20, marginTop: 20 }}>
        <div className="summary-row"><span>Subtotal</span><span>₹{Number(order.subtotal).toLocaleString('en-IN')}</span></div>
        {order.discount > 0 && <div className="summary-row"><span>Discount</span><span>−₹{Number(order.discount).toLocaleString('en-IN')}</span></div>}
        <div className="summary-row"><span>Shipping</span><span>{Number(order.shippingFee) === 0 ? 'Free' : `₹${order.shippingFee}`}</span></div>
        <div className="summary-row total"><span>Total</span><span>₹{Number(order.total).toLocaleString('en-IN')}</span></div>
      </div>

      <h3 style={{ marginTop: 32, marginBottom: 12 }}>Order timeline</h3>
      {order.history.map((h, i) => (
        <div key={i} style={{ display: 'flex', gap: 12, fontSize: 13, padding: '8px 0', borderBottom: '1px solid var(--color-border)' }}>
          <span style={{ fontWeight: 600, minWidth: 140 }}>{h.status.replace('_', ' ')}</span>
          <span style={{ color: 'var(--color-ink-soft)' }}>{h.note}</span>
          <span style={{ color: 'var(--color-ink-soft)', marginLeft: 'auto' }}>{new Date(h.at).toLocaleString()}</span>
        </div>
      ))}

      {canCancel && <button className="btn btn-outline" style={{ marginTop: 24 }} onClick={handleCancel}>Cancel order</button>}
    </div>
  )
}
