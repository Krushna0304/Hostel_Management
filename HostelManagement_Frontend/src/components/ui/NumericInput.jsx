import { useRef, useEffect } from 'react'

const NumericInput = ({ 
  className, 
  value, 
  onChange, 
  placeholder, 
  required = false,
  min,
  max,
  step = "1",
  disabled = false,
  ...props 
}) => {
  const inputRef = useRef(null)

  useEffect(() => {
    const input = inputRef.current
    if (!input) return

    // Disable scroll wheel behavior on number inputs
    const handleWheel = (e) => {
      // Only prevent if the input is focused
      if (document.activeElement === input) {
        e.preventDefault()
      }
    }

    // Add event listener
    input.addEventListener('wheel', handleWheel, { passive: false })

    // Cleanup
    return () => {
      input.removeEventListener('wheel', handleWheel)
    }
  }, [])

  // Handle focus to prevent accidental scroll changes
  const handleFocus = (e) => {
    // Optionally blur on scroll to prevent accidental changes
    const handleDocumentWheel = (wheelEvent) => {
      if (document.activeElement === e.target) {
        e.target.blur()
      }
    }

    document.addEventListener('wheel', handleDocumentWheel, { once: true })
  }

  return (
    <input
      ref={inputRef}
      type="number"
      className={className}
      value={value}
      onChange={onChange}
      onFocus={handleFocus}
      placeholder={placeholder}
      required={required}
      min={min}
      max={max}
      step={step}
      disabled={disabled}
      {...props}
    />
  )
}

export default NumericInput