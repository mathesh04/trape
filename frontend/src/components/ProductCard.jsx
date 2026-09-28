import { Link } from 'react-router-dom'
import { resolveMediaUrl } from '../api/client'

export default function ProductCard({ product }) {
  const price = Number(product.basePrice)
  const mrp = product.mrp != null ? Number(product.mrp) : null
  const hasDiscount = mrp && mrp > price
  const discountPct = hasDiscount ? Math.round(((mrp - price) / mrp) * 100) : 0

  return (
    <Link to={`/products/${product.slug}`} className="product-card">
      <div className="product-card-img">
        {hasDiscount && <span className="discount-badge">{discountPct}% OFF</span>}
        {product.primaryImageUrl ? (
          <img src={resolveMediaUrl(product.primaryImageUrl)} alt={product.name} />
        ) : (
          <div className="no-image">No image</div>
        )}
      </div>
      <div className="product-card-body">
        {product.brand && <div className="product-card-brand">{product.brand}</div>}
        <div className="product-card-name">{product.name}</div>
        <div className="product-card-price-row">
          <span className="product-card-price">₹{price.toLocaleString('en-IN')}</span>
          {hasDiscount && (
            <>
              <span className="product-card-mrp">₹{mrp.toLocaleString('en-IN')}</span>
              <span className="product-card-off">{discountPct}% off</span>
            </>
          )}
        </div>
        {product.avgRating != null && (
          <div className="rating-pill">
            <span>{product.avgRating.toFixed(1)} ★</span>
            <span className="rating-count">({product.reviewCount})</span>
          </div>
        )}
        <div className="free-delivery-tag">Free Delivery</div>
      </div>
    </Link>
  )
}
