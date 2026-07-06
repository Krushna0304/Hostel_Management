import { useEffect } from 'react'
import { createPortal } from 'react-dom'

/**
 * Viewport-centered modal rendered via portal (avoids overflow/transform parent issues).
 */
export default function CenteredModal({
  open,
  onClose,
  children,
  maxWidth = 'max-w-2xl',
  overlayClassName = 'bg-slate-950/45',
}) {
  useEffect(() => {
    if (!open) return undefined

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    const handleEscape = (event) => {
      if (event.key === 'Escape') onClose?.()
    }
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleEscape)
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div
      className={`fixed inset-0 z-[100] flex items-center justify-center p-4 ${overlayClassName}`}
      onClick={onClose}
      role="presentation"
    >
      <div
        className={`relative z-10 w-full ${maxWidth}`}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        {children}
      </div>
    </div>,
    document.body,
  )
}
