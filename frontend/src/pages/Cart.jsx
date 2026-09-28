import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { resolveMediaUrl } from '../api/client'

export default function Cart() {
  const { cart, updateItem, removeItem, applyCoupon, removeCoupon, loading } = useCart()
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [couponInput, setCouponInput] = useState('')
  const [couponError, setCouponError] = useState('')

  if (loading && !cart) return <div className="spinner-wrap">Loading…</div>

  if (!cart || cart.items.length === 0) {
    return (
      <div className="empty-state">
        <h2>Your cart is empty</h2>
        <p style={{ margin: '12px 0 24px' }}>Browse the collection and find something you like.</p>
        <Link to="/products" className="btn">Shop now</Link>
      </div>
    )
  }

  const handleApplyCoupon = async () => {
    setCouponError('')
    try {
      await applyCoupon(couponInput)
      setCouponInput('')
    } catch (err) {
      setCouponError(err.message)
    }
  }

  const handleCheckout = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: '/checkout' } })
      return
    }
    navigate('/checkout')
  }

  return (
    <div className="container cart-page">
      <div>
        <h1 className="section-title" style={{ marginTop: 24 }}>Your cart</h1>
        {cart.items.map((item) => (
          <div key={item.id} className="cart-line">
            <img src={resolveMediaUrl(item.imageUrl)} alt={item.productName} />
            <div style={{ flex: 1 }}>
              <p style={{ fontWeight: 600 }}>{item.productName}</p>
              <p style={{ fontSize: 13, color: 'var(--color-ink-soft)' }}>{[item.size, item.color].filter(Boolean).join(' / ')}</p>
              <p style={{ fontSize: 14, margin: '8px 0' }}>₹{Number(item.unitPrice).toLocaleString('en-IN')}</p>
              {!item.inStock && <p className="error-text">Only {item.availableQuantity} left in stock</p>}
              <div className="qty-control">
                <button onClick={() => item.quantity > 1 ? updateItem(item.id, item.quantity - 1) : removeItem(item.id)}>−</button>
                <span>{item.quantity}</span>
                <button onClick={() => updateItem(item.id, item.quantity + 1)}>+</button>
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <p style={{ fontWeight: 600 }}>₹{Number(item.lineTotal).toLocaleString('en-IN')}</p>
              <button onClick={() => removeItem(item.id)} style={{ background: 'none', border: 'none', color: 'var(--color-danger)', fontSize: 12, marginTop: 8 }}>Remove</button>
            </div>
          </div>
        ))}
      </div>

      <div className="cart-summary">
        <h3 style={{ marginBottom: 20 }}>Order summary</h3>
        <div className="summary-row"><span>Subtotal</span><span>₹{Number(cart.subtotal).toLocaleString('en-IN')}</span></div>
        {cart.discount > 0 && (
          <div className="summary-row"><span>Discount {cart.couponCode ? `(${cart.couponCode})` : ''}</span><span>−₹{Number(cart.discount).toLocaleString('en-IN')}</span></div>
        )}
        <div className="summary-row total"><span>Total</span><span>₹{Number(cart.total).toLocaleString('en-IN')}</span></div>

        {cart.couponCode ? (
          <button className="btn btn-outline btn-sm" style={{ marginTop: 8 }} onClick={removeCoupon}>Remove coupon</button>
        ) : (
          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <input placeholder="Coupon code" value={couponInput} onChange={(e) => setCouponInput(e.target.value)} style={{ flex: 1, padding: '10px 12px', border: '1px solid var(--color-border)' }} />
            <button className="btn btn-sm" onClick={handleApplyCoupon}>Apply</button>
          </div>
        )}
        {couponError && <p className="error-text">{couponError}</p>}

        <button className="btn btn-block" style={{ marginTop: 20 }} onClick={handleCheckout}>Checkout</button>
      </div>
    </div>
  )
}
