import { useState, useCallback } from 'react'

export const useConfirmation = () => {
  const [isOpen, setIsOpen] = useState(false)
  const [config, setConfig] = useState({
    title: 'localhost:3000 says',
    message: '',
    confirmText: 'OK',
    cancelText: 'Cancel',
    variant: 'default',
    onConfirm: null
  })

  const showConfirmation = useCallback(({
    title = 'localhost:3000 says',
    message,
    confirmText = 'OK',
    cancelText = 'Cancel',
    variant = 'default',
    onConfirm
  }) => {
    setConfig({
      title,
      message,
      confirmText,
      cancelText,
      variant,
      onConfirm
    })
    setIsOpen(true)
  }, [])

  const hideConfirmation = useCallback(() => {
    setIsOpen(false)
    setConfig(prev => ({ ...prev, onConfirm: null }))
  }, [])

  const handleConfirm = useCallback(() => {
    if (config.onConfirm) {
      config.onConfirm()
    }
    hideConfirmation()
  }, [config.onConfirm, hideConfirmation])

  return {
    isOpen,
    config,
    showConfirmation,
    hideConfirmation,
    handleConfirm
  }
}