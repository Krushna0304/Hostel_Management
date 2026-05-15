/**
 * Utility to dynamically load Razorpay script only when needed
 * This prevents preloading warnings and improves performance
 */

let razorpayLoaded = false
let razorpayPromise = null

export const loadRazorpay = () => {
  // Return existing promise if already loading/loaded
  if (razorpayPromise) {
    return razorpayPromise
  }

  // If already loaded, return resolved promise
  if (razorpayLoaded && window.Razorpay) {
    return Promise.resolve(window.Razorpay)
  }

  // Create new loading promise
  razorpayPromise = new Promise((resolve, reject) => {
    // Check if script already exists
    const existingScript = document.querySelector('script[src*="checkout.razorpay.com"]')
    if (existingScript) {
      if (window.Razorpay) {
        razorpayLoaded = true
        resolve(window.Razorpay)
        return
      }
    }

    // Create and load script
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.async = true
    
    script.onload = () => {
      if (window.Razorpay) {
        razorpayLoaded = true
        resolve(window.Razorpay)
      } else {
        reject(new Error('Razorpay failed to load'))
      }
    }
    
    script.onerror = () => {
      reject(new Error('Failed to load Razorpay script'))
    }
    
    document.head.appendChild(script)
  })

  return razorpayPromise
}

/**
 * Check if Razorpay is available without loading it
 */
export const isRazorpayLoaded = () => {
  return razorpayLoaded && window.Razorpay
}

/**
 * Preload Razorpay script (optional - use when you know payment will be needed soon)
 */
export const preloadRazorpay = () => {
  // Only preload if not already loaded/loading
  if (!razorpayLoaded && !razorpayPromise) {
    loadRazorpay().catch(console.error)
  }
}