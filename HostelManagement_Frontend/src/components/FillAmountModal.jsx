import { useState, useEffect, useRef } from 'react'

/**
 * Reusable custom prompt modal that replaces native browser prompt() dialogs.
 * Supports multiple input fields with validation.
 *
 * Props:
 *  - isOpen: boolean
 *  - title: string
 *  - subtitle: string (optional)
 *  - fields: [{ key, label, placeholder, type }]
 *  - onConfirm: (values) => void
 *  - onCancel: () => void
 */
export default function FillAmountModal({ isOpen, title, subtitle, fields = [], onConfirm, onCancel }) {
  const [values, setValues] = useState({})
  const firstInputRef = useRef(null)
  const backdropRef = useRef(null)

  // Reset values when modal opens
  useEffect(() => {
    if (isOpen) {
      const initial = {}
      fields.forEach(f => { initial[f.key] = '' })
      setValues(initial)
      // Focus first input after render
      setTimeout(() => firstInputRef.current?.focus(), 50)
    }
  }, [isOpen, fields])

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return
    const handleKey = (e) => {
      if (e.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [isOpen, onCancel])

  if (!isOpen) return null

  const handleChange = (key, value) => {
    setValues(prev => ({ ...prev, [key]: value }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    // Basic validation — check all fields have values
    const allFilled = fields.every(f => {
      const val = values[f.key]?.trim()
      if (!val) return false
      if (f.type === 'number' && (isNaN(val) || parseFloat(val) <= 0)) return false
      return true
    })
    if (allFilled) {
      onConfirm(values)
    }
  }

  const handleBackdropClick = (e) => {
    if (e.target === backdropRef.current) onCancel()
  }

  return (
    <div
      ref={backdropRef}
      onClick={handleBackdropClick}
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ animation: 'fadeIn 0.2s ease-out' }}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-slate-950/40 backdrop-blur-sm" />

      {/* Modal */}
      <div
        className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden border border-slate-200"
        style={{ animation: 'slideUp 0.25s ease-out' }}
      >
        {/* Header */}
        <div className="px-6 pt-6 pb-2">
          <h3 className="text-lg font-bold text-slate-950">{title}</h3>
          {subtitle && (
            <p className="text-sm text-slate-500 mt-1">{subtitle}</p>
          )}
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="px-6 pb-6">
          <div className="space-y-4 mt-4">
            {fields.map((field, idx) => (
              <div key={field.key}>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  {field.label}
                </label>
                <input
                  ref={idx === 0 ? firstInputRef : undefined}
                  type={field.type || 'text'}
                  step={field.type === 'number' ? '0.01' : undefined}
                  min={field.type === 'number' ? '0' : undefined}
                  value={values[field.key] || ''}
                  onChange={(e) => handleChange(field.key, e.target.value)}
                  placeholder={field.placeholder || ''}
                  className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm 
                    focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 
                    transition-all duration-200 bg-slate-50 hover:bg-white"
                  autoComplete="off"
                />
              </div>
            ))}
          </div>

          {/* Footer Buttons */}
          <div className="flex items-center justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={onCancel}
              className="px-5 py-2 text-sm font-medium text-slate-600 bg-slate-100 
                rounded-xl hover:bg-slate-200 transition-colors duration-200"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 text-sm font-medium text-white bg-slate-900 
                rounded-xl hover:bg-slate-800 transition-colors duration-200 
                shadow-lg shadow-slate-900/20"
            >
              Apply
            </button>
          </div>
        </form>
      </div>

      {/* Inline animations */}
      <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes slideUp {
          from { opacity: 0; transform: translateY(12px) scale(0.97); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
      `}</style>
    </div>
  )
}
