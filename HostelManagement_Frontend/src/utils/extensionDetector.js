/**
 * Utility to detect potential browser extension conflicts
 * that might cause message channel errors
 */

export const detectPotentialConflicts = () => {
  const conflicts = []

  // Check for common extension indicators
  if (window.chrome && window.chrome.runtime) {
    conflicts.push('Chrome extension runtime detected')
  }

  // Check for modified DOM elements that extensions might add
  const suspiciousElements = document.querySelectorAll('[data-extension], [class*="extension"], [id*="extension"]')
  if (suspiciousElements.length > 0) {
    conflicts.push(`${suspiciousElements.length} potential extension elements found`)
  }

  // Check for modified input elements
  const inputs = document.querySelectorAll('input')
  let modifiedInputs = 0
  inputs.forEach(input => {
    if (input.hasAttribute('data-lpignore') || 
        input.hasAttribute('data-form-type') ||
        input.classList.contains('extension-input')) {
      modifiedInputs++
    }
  })
  
  if (modifiedInputs > 0) {
    conflicts.push(`${modifiedInputs} inputs modified by extensions`)
  }

  return conflicts
}

export const showExtensionWarning = () => {
  const conflicts = detectPotentialConflicts()
  
  if (conflicts.length > 0) {
    console.warn('⚠️ Potential browser extension conflicts detected:', conflicts)
    console.warn('💡 If you experience form submission issues, try:')
    console.warn('   1. Disable browser extensions temporarily')
    console.warn('   2. Use incognito/private browsing mode')
    console.warn('   3. Clear browser cache and cookies')
    
    return true
  }
  
  return false
}

export const suppressExtensionErrors = () => {
  // Suppress common extension-related errors
  const originalError = window.console.error
  
  window.console.error = (...args) => {
    const message = args.join(' ')
    
    // Suppress known extension-related errors
    if (message.includes('message channel closed') ||
        message.includes('listener indicated an asynchronous response') ||
        message.includes('Extension context invalidated') ||
        message.includes('chrome-extension://')) {
      return // Suppress these errors
    }
    
    // Log other errors normally
    originalError.apply(console, args)
  }
}