import { useEffect } from 'react'

export default function ConfirmationModal({
  isOpen,
  onClose,
  onConfirm,
  title = "localhost:3000 says",
  message,
  confirmText = "OK",
  cancelText = "Cancel",
  variant = "default" // "default", "danger", "warning"
}) {
  // Handle escape key
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    if (isOpen) {
      document.addEventListener('keydown', handleEscape)
      // Prevent body scroll when modal is open
      document.body.style.overflow = 'hidden'
    }

    return () => {
      document.removeEventListener('keydown', handleEscape)
      document.body.style.overflow = 'unset'
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose()
    }
  }

  const getVariantStyles = () => {
    switch (variant) {
      case 'danger':
        return {
          confirmButton: 'bg-red-600 hover:bg-red-700 text-white',
          title: 'text-red-600'
        }
      case 'warning':
        return {
          confirmButton: 'bg-amber-600 hover:bg-amber-700 text-white',
          title: 'text-amber-600'
        }
      default:
        return {
          confirmButton: 'bg-blue-600 hover:bg-blue-700 text-white',
          title: 'text-slate-200'
        }
    }
  }

  const styles = getVariantStyles()

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={handleBackdropClick}
    >
      <div 
        className="relative w-full max-w-md mx-4 bg-slate-800 rounded-2xl shadow-2xl border border-slate-700"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Content */}
        <div className="p-6">
          {/* Title */}
          <h3 className={`text-sm font-medium mb-4 ${styles.title}`}>
            {title}
          </h3>
          
          {/* Message */}
          <p className="text-slate-300 text-sm leading-relaxed mb-8">
            {message}
          </p>
          
          {/* Buttons */}
          <div className="flex justify-end gap-3">
            <button
              onClick={onClose}
              className="px-6 py-2.5 text-sm font-medium text-slate-300 bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-slate-500 focus:ring-offset-2 focus:ring-offset-slate-800"
            >
              {cancelText}
            </button>
            <button
              onClick={() => {
                onConfirm()
                onClose()
              }}
              className={`px-6 py-2.5 text-sm font-medium rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 focus:ring-offset-slate-800 ${styles.confirmButton}`}
            >
              {confirmText}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}