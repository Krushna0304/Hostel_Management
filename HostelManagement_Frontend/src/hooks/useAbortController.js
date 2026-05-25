import { useEffect, useRef } from 'react'

/**
 * Custom hook to manage AbortController for API requests
 * Helps prevent duplicate requests and memory leaks
 */
export const useAbortController = () => {
  const abortControllerRef = useRef(null)

  useEffect(() => {
    // Create new AbortController on mount
    abortControllerRef.current = new AbortController()

    // Cleanup on unmount
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort()
      }
    }
  }, [])

  const getSignal = () => {
    return abortControllerRef.current?.signal
  }

  const abort = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = new AbortController()
    }
  }

  return { getSignal, abort }
}