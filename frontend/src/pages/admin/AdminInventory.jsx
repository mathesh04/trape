import { useState } from 'react'
import { api } from '../../api/client'

export default function AdminInventory() {
  const [variantId, setVariantId] = useState('')
  const [quantity, setQuantity] = useState('')
  const [message, setMessage] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.adminSetStock({ variantId, quantityAvailable: Number(quantity) })
      setMessage('Stock updated.')
    } catch (err) {
      setMessage(err.message)
    }
  }

  return (
    <div>
      <h2>Inventory</h2>
      <p style={{ color: 'var(--color-ink-soft)', marginBottom: 20 }}>Set absolute stock for a variant by its ID (find variant IDs via the product detail API/Swagger UI).</p>
      <form onSubmit={handleSubmit} style={{ maxWidth: 420 }}>
        <div className="field"><label>Variant ID</label><input required value={variantId} onChange={(e) => setVariantId(e.target.value)} /></div>
        <div className="field"><label>Quantity available</label><input required type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} /></div>
        {message && <p>{message}</p>}
        <button className="btn">Update stock</button>
      </form>
    </div>
  )
}
