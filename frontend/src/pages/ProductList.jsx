import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import ProductCard from '../components/ProductCard'

export default function ProductList() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [result, setResult] = useState({ content: [], totalPages: 0, page: 0 })
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)

  const categorySlug = searchParams.get('category') || ''
  const sort = searchParams.get('sort') || 'newest'
  const q = searchParams.get('q') || ''

  useEffect(() => {
    api.listCategories().then(setCategories).catch(console.error)
  }, [])

  useEffect(() => {
    setLoading(true)
    const category = categories.find((c) => c.slug === categorySlug)
    api.listProducts({ category: category?.id, sort, q, pageSize: 24 })
      .then(setResult)
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [categorySlug, sort, q, categories])

  const updateParam = (key, value) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value); else next.delete(key)
    setSearchParams(next)
  }

  return (
    <div className="container">
      <h1 className="section-title">Shop</h1>
      <div className="filters-bar">
        <select value={categorySlug} onChange={(e) => updateParam('category', e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.slug}>{c.name}</option>)}
        </select>
        <select value={sort} onChange={(e) => updateParam('sort', e.target.value)}>
          <option value="newest">Newest</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
        </select>
        <input placeholder="Search products…" defaultValue={q} onKeyDown={(e) => { if (e.key === 'Enter') updateParam('q', e.target.value) }} />
      </div>

      {loading ? (
        <div className="spinner-wrap">Loading…</div>
      ) : result.content.length === 0 ? (
        <div className="empty-state">No products found.</div>
      ) : (
        <div className="product-grid">
          {result.content.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      )}
    </div>
  )
}
