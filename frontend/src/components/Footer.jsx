import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-grid">
          <div className="footer-col">
            <p className="footer-brand">TRAPE</p>
            <p>Trendy, affordable fashion for women, men &amp; kids — delivered to your doorstep.</p>
          </div>
          <div className="footer-col">
            <h4>Shop</h4>
            <Link to="/products">All Products</Link>
            <Link to="/products?sort=newest">New Arrivals</Link>
            <Link to="/products?sort=price_asc">Best Deals</Link>
          </div>
          <div className="footer-col">
            <h4>Customer Policies</h4>
            <p>Returns &amp; Refunds</p>
            <p>Shipping Info</p>
            <p>Track Your Order</p>
            <p>FAQs</p>
          </div>
          <div className="footer-col">
            <h4>Company</h4>
            <p>About Us</p>
            <p>Careers</p>
            <p>Contact Us</p>
          </div>
        </div>
        <div className="footer-bottom">
          © {new Date().getFullYear()} Trape. All rights reserved.
        </div>
      </div>
    </footer>
  )
}
