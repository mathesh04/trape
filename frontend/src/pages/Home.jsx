import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import ProductCard from '../components/ProductCard'

export default function Home() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.listProducts({ sort: 'newest', pageSize: 12 })
      .then((data) => setProducts(data.content))
      .catch(console.error)
      .finally(() => setLoading(false))
    api.listCategories().then(setCategories).catch(console.error)
  }, [])

  return (
    <div>
      <section className="hero">
        <h1>Trendy Fashion, Unbeatable Prices</h1>
        <p>Shop the latest styles for women, men &amp; kids — new drops every week.</p>
        <Link to="/products" className="btn">Shop Now</Link>
      </section>

      <div className="container">
        {categories.length > 0 && (
          <>
            <h2 className="section-title">Shop by category</h2>
            <p className="section-subtitle">Curated collections, refreshed daily</p>
            <div className="category-tiles">
              {categories.map((c, i) => (
                <Link key={c.id} to={`/products?category=${c.slug}`} className="category-tile">
                  <div className="category-tile-icon">{c.name.charAt(0)}</div>
                  {c.name}
                </Link>
              ))}
            </div>
          </>
        )}

        <h2 className="section-title">New arrivals</h2>
        <p className="section-subtitle">Fresh picks handpicked for you</p>
        {loading ? (
          <div className="spinner-wrap">Loading…</div>
        ) : products.length === 0 ? (
          <div className="empty-state">
            <p>No products yet — check back soon, or head to the admin panel to add your first item.</p>
          </div>
        ) : (
          <div className="product-grid">
            {products.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </div>
    </div>
  )
}
