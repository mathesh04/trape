import { NavLink, Outlet } from 'react-router-dom'

export default function AdminLayout() {
  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <NavLink to="/admin/products" className={({ isActive }) => isActive ? 'active' : ''}>Products</NavLink>
        <NavLink to="/admin/categories" className={({ isActive }) => isActive ? 'active' : ''}>Categories</NavLink>
        <NavLink to="/admin/orders" className={({ isActive }) => isActive ? 'active' : ''}>Orders</NavLink>
        <NavLink to="/admin/coupons" className={({ isActive }) => isActive ? 'active' : ''}>Coupons</NavLink>
        <NavLink to="/admin/inventory" className={({ isActive }) => isActive ? 'active' : ''}>Inventory</NavLink>
      </aside>
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  )
}
