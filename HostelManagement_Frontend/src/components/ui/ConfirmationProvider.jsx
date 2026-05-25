import { createContext, useContext, useState } from 'react'
import ConfirmationModal from './ConfirmationModal'

const ConfirmationContext = createContext()

export const useConfirm = () => {
  const context = useContext(ConfirmationContext)
  if (!context) {
    throw new Error('useConfirm must be used within a ConfirmationProvider')
  }
  return context
}

export const ConfirmationProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [config, setConfig] = useState({})

  const confirm = (message, options = {}) => {
    return new Promise((resolve) => {
      setConfig({
        title: options.title || 'localhost:3000 says',
        message,
        confirmText: options.confirmText || 'OK',
        cancelText: options.cancelText || 'Cancel',
        variant: options.variant || 'default',
        onConfirm: () => resolve(true),
        onCancel: () => resolve(false)
      })
      setIsOpen(true)
    })
  }

  const handleClose = () => {
    setIsOpen(false)
    if (config.onCancel) {
      config.onCancel()
    }
  }

  const handleConfirm = () => {
    setIsOpen(false)
    if (config.onConfirm) {
      config.onConfirm()
    }
  }

  return (
    <ConfirmationContext.Provider value={{ confirm }}>
      {children}
      <ConfirmationModal
        isOpen={isOpen}
        onClose={handleClose}
        onConfirm={handleConfirm}
        title={config.title}
        message={config.message}
        confirmText={config.confirmText}
        cancelText={config.cancelText}
        variant={config.variant}
      />
    </ConfirmationContext.Provider>
  )
}