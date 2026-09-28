import { useEffect, useRef, useState } from 'react'
import { api, resolveMediaUrl } from '../../api/client'

const emptyForm = {
  name: '', slug: '', description: '', brand: 'Trape', basePrice: '', categoryId: '', status: 'ACTIVE',
  imageUrls: [],
  variants: [{ sku: '', size: '', color: '', price: '', mrp: '', initialStock: 0 }],
}

export default function AdminProducts() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [message, setMessage] = useState('')
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef(null)

  const load = () => {
    api.listProducts({ pageSize: 50 }).then((data) => setProducts(data.content)).catch(console.error)
    api.listCategories().then(setCategories).catch(console.error)
  }
  useEffect(load, [])

  const updateVariant = (i, field, value) => {
    const variants = [...form.variants]
    variants[i] = { ...variants[i], [field]: value }
    setForm({ ...form, variants })
  }

  const addVariantRow = () => setForm({ ...form, variants: [...form.variants, { sku: '', size: '', color: '', price: '', mrp: '', initialStock: 0 }] })

  // --- Image upload from device (multiple files in one go) --------------
  const handleFilesSelected = async (e) => {
    const files = Array.from(e.target.files || [])
    if (files.length === 0) return
    setMessage('')
    setUploading(true)
    try {
      const uploaded = await api.adminUploadImages(files)
      setForm((prev) => ({ ...prev, imageUrls: [...prev.imageUrls, ...uploaded.map((u) => u.url)] }))
    } catch (err) {
      setMessage(err.message)
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const removeImage = (url) => {
    setForm((prev) => ({ ...prev, imageUrls: prev.imageUrls.filter((u) => u !== url) }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    try {
      const payload = {
        ...form,
        basePrice: Number(form.basePrice),
        imageUrls: form.imageUrls,
        variants: form.variants.map((v) => ({
          ...v,
          price: Number(v.price),
          mrp: v.mrp ? Number(v.mrp) : null,
          initialStock: Number(v.initialStock) || 0,
        })),
      }
      await api.adminCreateProduct(payload)
      setMessage('Product created.')
      setForm(emptyForm)
      load()
    } catch (err) {
      setMessage(err.message)
    }
  }

  const handleArchive = async (id) => {
    if (!confirm('Archive this product?')) return
    await api.adminArchiveProduct(id)
    load()
  }

  return (
    <div>
      <h2>Products</h2>
      <div className="admin-card">
        <table style={{ marginBottom: 0 }}>
          <thead><tr><th>Name</th><th>Price</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>₹{p.basePrice}</td>
                <td>Active</td>
                <td><button className="btn btn-sm btn-outline" onClick={() => handleArchive(p.id)}>Archive</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="admin-card" style={{ maxWidth: 620 }}>
        <h3 style={{ marginBottom: 16 }}>Create product</h3>
        <form onSubmit={handleSubmit}>
          <div className="field"><label>Name</label><input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
          <div className="field"><label>Slug (optional — auto-generated if blank)</label><input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} /></div>
          <div className="field"><label>Description</label><textarea rows={3} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
          <div className="field"><label>Base price (₹)</label><input required type="number" value={form.basePrice} onChange={(e) => setForm({ ...form, basePrice: e.target.value })} /></div>
          <div className="field">
            <label>Category</label>
            <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">— none —</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>

          <div className="field">
            <label>Product images</label>
            <label className="image-upload-box">
              <input ref={fileInputRef} type="file" accept="image/*" multiple onChange={handleFilesSelected} />
              {uploading ? 'Uploading…' : '📷 Click to choose photos from your device (JPEG, PNG, WEBP, GIF — up to 5MB each)'}
            </label>
            {form.imageUrls.length > 0 && (
              <div className="image-preview-grid">
                {form.imageUrls.map((url) => (
                  <div key={url} className="image-preview-item">
                    <img src={resolveMediaUrl(url)} alt="" />
                    <button type="button" className="image-preview-remove" onClick={() => removeImage(url)}>×</button>
                  </div>
                ))}
              </div>
            )}
            <p className="upload-progress-text">{form.imageUrls.length} image{form.imageUrls.length === 1 ? '' : 's'} attached. The first one becomes the main listing photo.</p>
          </div>

          <p style={{ fontWeight: 600, marginTop: 20, marginBottom: 8 }}>Variants</p>
          {form.variants.map((v, i) => (
            <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
              <input placeholder="SKU" required value={v.sku} onChange={(e) => updateVariant(i, 'sku', e.target.value)} style={{ width: 100 }} />
              <input placeholder="Size" value={v.size} onChange={(e) => updateVariant(i, 'size', e.target.value)} style={{ width: 60 }} />
              <input placeholder="Color" value={v.color} onChange={(e) => updateVariant(i, 'color', e.target.value)} style={{ width: 80 }} />
              <input placeholder="Price" required type="number" value={v.price} onChange={(e) => updateVariant(i, 'price', e.target.value)} style={{ width: 80 }} />
              <input placeholder="MRP" type="number" value={v.mrp} onChange={(e) => updateVariant(i, 'mrp', e.target.value)} style={{ width: 80 }} />
              <input placeholder="Stock" type="number" value={v.initialStock} onChange={(e) => updateVariant(i, 'initialStock', e.target.value)} style={{ width: 70 }} />
            </div>
          ))}
          <button type="button" className="btn btn-sm btn-outline" onClick={addVariantRow}>+ Add variant</button>

          {message && <p style={{ marginTop: 16 }}>{message}</p>}
          <div>
            <button className="btn" style={{ marginTop: 20 }} disabled={uploading}>Create product</button>
          </div>
        </form>
      </div>
    </div>
  )
}
