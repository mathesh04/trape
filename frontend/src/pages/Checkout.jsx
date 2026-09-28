import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

export default function Checkout() {
  const { user } = useAuth()
  const { cart, refresh } = useCart()
  const navigate = useNavigate()
  const [addresses, setAddresses] = useState([])
  const [selectedAddressId, setSelectedAddressId] = useState(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [form, setForm] = useState({ label: 'Home', line1: '', line2: '', city: '', state: '', pincode: '', phone: '', isDefault: false })
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    api.listAddresses().then((data) => {
      setAddresses(data)
      const def = data.find((a) => a.isDefault) || data[0]
      if (def) setSelectedAddressId(def.id)
      if (data.length === 0) setShowAddForm(true)
    }).catch(console.error)
  }, [])

  if (!cart || cart.items.length === 0) {
    navigate('/cart')
    return null
  }

  const handleAddAddress = async (e) => {
    e.preventDefault()
    try {
      const newAddress = await api.addAddress(form)
      setAddresses([newAddress, ...addresses])
      setSelectedAddressId(newAddress.id)
      setShowAddForm(false)
    } catch (err) {
      setError(err.message)
    }
  }

  const handlePlaceOrder = async () => {
    if (!selectedAddressId) {
      setError('Please select a shipping address.')
      return
    }
    setError('')
    setPlacing(true)
    try {
      const checkoutData = await api.checkout(selectedAddressId)
      const { razorpayOrderId, razorpayKeyId, amountInPaise, currency, order } = checkoutData

      const options = {
        key: razorpayKeyId,
        amount: amountInPaise,
        currency,
        name: 'Trape',
        description: `Order ${order.orderNumber}`,
        order_id: razorpayOrderId,
        prefill: { name: user?.fullName, email: user?.email, contact: user?.phone },
        theme: { color: '#1a1a1a' },
        handler: async (response) => {
          try {
            await api.verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })
            await refresh()
            navigate(`/account/orders/${order.id}`, { state: { justPlaced: true } })
          } catch (err) {
            setError('Payment verification failed: ' + err.message)
          }
        },
        modal: {
          ondismiss: () => setPlacing(false),
        },
      }

      if (!window.Razorpay) {
        setError('Payment gateway failed to load. Please refresh and try again.')
        setPlacing(false)
        return
      }
      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', (resp) => {
        setError('Payment failed: ' + resp.error.description)
        setPlacing(false)
      })
      rzp.open()
    } catch (err) {
      setError(err.message)
      setPlacing(false)
    }
  }

  return (
    <div className="container" style={{ maxWidth: 720, padding: '48px 24px' }}>
      <h1 className="section-title" style={{ marginTop: 0 }}>Checkout</h1>

      <h3 style={{ marginTop: 32, marginBottom: 12 }}>Shipping address</h3>
      {addresses.map((addr) => (
        <div key={addr.id} className={`address-card ${selectedAddressId === addr.id ? 'selected' : ''}`} onClick={() => setSelectedAddressId(addr.id)}>
          <p style={{ fontWeight: 600 }}>{addr.label} {addr.isDefault && '(Default)'}</p>
          <p style={{ fontSize: 14, color: 'var(--color-ink-soft)' }}>{addr.line1}, {addr.line2 ? addr.line2 + ', ' : ''}{addr.city}, {addr.state} {addr.pincode}</p>
          <p style={{ fontSize: 13, color: 'var(--color-ink-soft)' }}>{addr.phone}</p>
        </div>
      ))}

      {!showAddForm ? (
        <button className="btn btn-outline btn-sm" onClick={() => setShowAddForm(true)}>+ Add new address</button>
      ) : (
        <form onSubmit={handleAddAddress} style={{ border: '1px solid var(--color-border)', padding: 20, marginTop: 12 }}>
          <div className="field"><label>Label</label><input value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} /></div>
          <div className="field"><label>Address line 1</label><input required value={form.line1} onChange={(e) => setForm({ ...form, line1: e.target.value })} /></div>
          <div className="field"><label>Address line 2</label><input value={form.line2} onChange={(e) => setForm({ ...form, line2: e.target.value })} /></div>
          <div className="field"><label>City</label><input required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} /></div>
          <div className="field"><label>State</label><input required value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} /></div>
          <div className="field"><label>Pincode</label><input required value={form.pincode} onChange={(e) => setForm({ ...form, pincode: e.target.value })} /></div>
          <div className="field"><label>Phone</label><input required value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></div>
          <button className="btn btn-sm">Save address</button>
        </form>
      )}

      <h3 style={{ marginTop: 32, marginBottom: 12 }}>Order summary</h3>
      <div className="cart-summary" style={{ padding: 20 }}>
        <div className="summary-row"><span>Subtotal</span><span>₹{Number(cart.subtotal).toLocaleString('en-IN')}</span></div>
        {cart.discount > 0 && <div className="summary-row"><span>Discount</span><span>−₹{Number(cart.discount).toLocaleString('en-IN')}</span></div>}
        <div className="summary-row total"><span>Total</span><span>₹{Number(cart.total).toLocaleString('en-IN')}</span></div>
      </div>

      {error && <p className="error-text" style={{ marginTop: 16 }}>{error}</p>}

      <button className="btn btn-block" style={{ marginTop: 24 }} disabled={placing} onClick={handlePlaceOrder}>
        {placing ? 'Processing…' : 'Pay with Razorpay'}
      </button>
    </div>
  )
}
