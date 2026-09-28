import { useEffect, useState, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { api, resolveMediaUrl } from '../api/client'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'

export default function ProductDetail() {
  const { slug } = useParams()
  const { addItem } = useCart()
  const { isAuthenticated } = useAuth()
  const [product, setProduct] = useState(null)
  const [reviews, setReviews] = useState({ content: [] })
  const [activeImage, setActiveImage] = useState(0)
  const [selectedSize, setSelectedSize] = useState(null)
  const [selectedColor, setSelectedColor] = useState(null)
  const [message, setMessage] = useState('')
  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: '' })

  useEffect(() => {
    setMessage('')
    api.getProduct(slug).then((data) => {
      setProduct(data)
      const firstAvailable = data.variants.find((v) => v.inStock) || data.variants[0]
      if (firstAvailable) {
        setSelectedSize(firstAvailable.size)
        setSelectedColor(firstAvailable.color)
      }
      api.listReviews(data.id).then(setReviews).catch(console.error)
    }).catch(console.error)
  }, [slug])

  const sizes = useMemo(() => [...new Set((product?.variants || []).map((v) => v.size).filter(Boolean))], [product])
  const selectedVariant = useMemo(() => {
    if (!product) return null
    return product.variants.find((v) => v.size === selectedSize && (v.color === selectedColor || !v.color))
  }, [product, selectedSize, selectedColor])

  if (!product) return <div className="spinner-wrap">Loading…</div>

  const handleAddToCart = async () => {
    if (!selectedVariant) return
    try {
      await addItem(selectedVariant.id, 1)
      setMessage('Added to cart.')
    } catch (err) {
      setMessage(err.message)
    }
  }

  const handleReviewSubmit = async (e) => {
    e.preventDefault()
    try {
      await api.addReview(product.id, reviewForm)
      const updated = await api.listReviews(product.id)
      setReviews(updated)
      setReviewForm({ rating: 5, comment: '' })
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <div className="container">
      <div className="pdp">
        <div>
          <div className="pdp-gallery-main">
            {product.images[activeImage] ? (
              <img src={resolveMediaUrl(product.images[activeImage].url)} alt={product.name} />
            ) : (
              <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#aaa' }}>No image</div>
            )}
          </div>
          <div className="pdp-thumbs">
            {product.images.map((img, i) => (
              <img key={img.id} src={resolveMediaUrl(img.url)} alt="" onClick={() => setActiveImage(i)} style={{ borderColor: i === activeImage ? 'var(--color-primary)' : undefined }} />
            ))}
          </div>
        </div>

        <div>
          <p style={{ color: 'var(--color-ink-soft)', textTransform: 'uppercase', fontSize: 12, letterSpacing: 1 }}>{product.brand}</p>
          <h1 style={{ fontSize: 30 }}>{product.name}</h1>
          {product.avgRating != null && (
            <div className="rating-stars" style={{ marginTop: 8 }}>
              {'★'.repeat(Math.round(product.avgRating))}{'☆'.repeat(5 - Math.round(product.avgRating))} ({product.reviewCount} reviews)
            </div>
          )}

          <div className="pdp-price">
            ₹{Number(selectedVariant?.price ?? product.basePrice).toLocaleString('en-IN')}
            {selectedVariant?.mrp && selectedVariant.mrp > selectedVariant.price && (
              <span className="pdp-mrp">₹{Number(selectedVariant.mrp).toLocaleString('en-IN')}</span>
            )}
          </div>

          <p style={{ color: 'var(--color-ink-soft)', fontSize: 14 }}>{product.description}</p>

          {sizes.length > 0 && (
            <>
              <p style={{ marginTop: 20, fontWeight: 600, fontSize: 13 }}>Size</p>
              <div className="variant-options">
                {sizes.map((size) => {
                  const variantForSize = product.variants.find((v) => v.size === size);
                  return (
                    <button
                      key={size}
                      className={`variant-chip ${selectedSize === size ? 'selected' : ''}`}
                      disabled={!variantForSize?.inStock}
                      onClick={() => setSelectedSize(size)}
                    >
                      {size}
                    </button>
                  )
                })}
              </div>
            </>
          )}

          <button className="btn btn-block" disabled={!selectedVariant || !selectedVariant.inStock} onClick={handleAddToCart}>
            {selectedVariant?.inStock ? 'Add to cart' : 'Out of stock'}
          </button>
          {message && <p style={{ marginTop: 12, fontSize: 13 }}>{message}</p>}
          {selectedVariant && (
            <p style={{ marginTop: 10, fontSize: 12, color: 'var(--color-ink-soft)' }}>SKU: {selectedVariant.sku} · {selectedVariant.availableQuantity} in stock</p>
          )}
        </div>
      </div>

      <section style={{ padding: '32px 0 64px', borderTop: '1px solid var(--color-border)' }}>
        <h2 className="section-title" style={{ fontSize: 22, marginTop: 24 }}>Reviews</h2>
        {reviews.content.length === 0 && <p style={{ color: 'var(--color-ink-soft)' }}>No reviews yet.</p>}
        {reviews.content.map((r) => (
          <div key={r.id} style={{ borderBottom: '1px solid var(--color-border)', padding: '16px 0' }}>
            <div className="rating-stars">{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</div>
            <p style={{ fontWeight: 600, fontSize: 14, marginTop: 4 }}>{r.userName}</p>
            <p style={{ fontSize: 14, color: 'var(--color-ink-soft)' }}>{r.comment}</p>
          </div>
        ))}

        {isAuthenticated && (
          <form onSubmit={handleReviewSubmit} style={{ marginTop: 24, maxWidth: 420 }}>
            <p style={{ fontWeight: 600, marginBottom: 10 }}>Write a review</p>
            <div className="field">
              <label>Rating</label>
              <select value={reviewForm.rating} onChange={(e) => setReviewForm({ ...reviewForm, rating: Number(e.target.value) })}>
                {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} star{n > 1 ? 's' : ''}</option>)}
              </select>
            </div>
            <div className="field">
              <label>Comment</label>
              <textarea rows={3} value={reviewForm.comment} onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })} />
            </div>
            <button className="btn btn-sm">Submit review</button>
            <p style={{ fontSize: 12, color: 'var(--color-ink-soft)', marginTop: 8 }}>Only verified purchasers (delivered orders) can review.</p>
          </form>
        )}
      </section>
    </div>
  )
}
