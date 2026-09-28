import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { api } from '../api/client'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(false)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const data = await api.getCart()
      setCart(data)
    } catch (e) {
      console.error('Failed to load cart', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const addItem = useCallback(async (variantId, quantity = 1) => {
    const data = await api.addToCart({ variantId, quantity })
    setCart(data)
    return data
  }, [])

  const updateItem = useCallback(async (itemId, quantity) => {
    const data = await api.updateCartItem(itemId, { quantity })
    setCart(data)
    return data
  }, [])

  const removeItem = useCallback(async (itemId) => {
    const data = await api.removeCartItem(itemId)
    setCart(data)
    return data
  }, [])

  const applyCoupon = useCallback(async (code) => {
    const data = await api.applyCoupon(code)
    setCart(data)
    return data
  }, [])

  const removeCoupon = useCallback(async () => {
    const data = await api.removeCoupon()
    setCart(data)
    return data
  }, [])

  const itemCount = cart?.items?.reduce((sum, i) => sum + i.quantity, 0) || 0

  return (
    <CartContext.Provider value={{ cart, loading, itemCount, refresh, addItem, updateItem, removeItem, applyCoupon, removeCoupon }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
