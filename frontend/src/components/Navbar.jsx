import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { api } from '../api/client'

export default function Navbar() {
  const { user, isAuthenticated, logout, isAdmin } = useAuth()
  const { itemCount } = useCart()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [categories, setCategories] = useState([])

  useEffect(() => {
    api.listCategories().then(setCategories).catch(console.error)
  }, [])

  const handleSearch = (e) => {
    e.preventDefault()
    navigate(query ? `/products?q=${encodeURIComponent(query)}` : '/products')
  }

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="brand" aria-label="TRAPE Home">
          <img src="/logo.png" alt="TRAPE" className="brand-logo" />
        </Link>

        <form className="navbar-search" onSubmit={handleSearch}>
          <input
            placeholder="Search for kurtis, sarees, shirts, dresses..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button type="submit" aria-label="Search">⌕</button>
        </form>

        <nav className="nav-links">
          <Link to="/products">Shop</Link>
          {isAdmin && <Link to="/admin">Admin</Link>}
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {isAuthenticated ? (
            <>
              <button className="nav-icon-btn" onClick={() => navigate('/account/orders')}>Orders</button>
              <button className="nav-icon-btn" onClick={() => { logout(); navigate('/') }}>Logout</button>
            </>
          ) : (
            <button className="nav-icon-btn" onClick={() => navigate('/login')}>Login</button>
          )}
          <button className="nav-icon-btn" onClick={() => navigate('/cart')}>
            🛒 Cart
            {itemCount > 0 && <span className="cart-badge">{itemCount}</span>}
          </button>
        </div>
      </div>

      {categories.length > 0 && (
        <div className="category-strip">
          <div className="category-strip-inner">
            {categories.map((c) => (
              <Link key={c.id} to={`/products?category=${c.slug}`} className="category-pill">{c.name}</Link>
            ))}
          </div>
        </div>
      )}
    </header>
  )
}
