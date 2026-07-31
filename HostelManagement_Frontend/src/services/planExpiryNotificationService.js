import apiClient from './apiClient'

/**
 * Service for Plan Expiry Notification API interactions
 * Covers 6.4 Plan Expiry Notification UI
 */
const planExpiryNotificationService = {
  // ── Tenant endpoints ──────────────────────────────────────────────────────

  /** Get notifications for a tenant, optionally filtered by status */
  getTenantNotifications: (tenantId, { status, notificationType, fromDate, toDate } = {}) => {
    const params = new URLSearchParams()
    if (status) params.append('status', status)
    if (notificationType) params.append('notificationType', notificationType)
    if (fromDate) params.append('fromDate', fromDate)
    if (toDate) params.append('toDate', toDate)
    const query = params.toString() ? `?${params.toString()}` : ''
    return apiClient.get(`/api/v1/notifications/plan-expiry/${tenantId}${query}`)
  },

  /** Get notifications for a specific agreement */
  getAgreementNotifications: (agreementId) =>
    apiClient.get(`/api/v1/notifications/plan-expiry/agreement/${agreementId}`),

  // ── Owner / Admin endpoints ───────────────────────────────────────────────

  /** Schedule notifications for an agreement with custom lead times */
  scheduleNotifications: (agreementId, leadTimes) =>
    apiClient.post('/api/v1/notifications/plan-expiry/schedule', { agreementId, leadTimes }),

  /** Resend a failed or undelivered notification */
  resendNotification: (notificationId) =>
    apiClient.put(`/api/v1/notifications/plan-expiry/${notificationId}/resend`),

  /** Cancel a pending notification */
  cancelNotification: (notificationId) =>
    apiClient.put(`/api/v1/notifications/plan-expiry/${notificationId}/cancel`),

  /** Cancel all notifications for an agreement */
  cancelAgreementNotifications: (agreementId) =>
    apiClient.put(`/api/v1/notifications/plan-expiry/agreement/${agreementId}/cancel`),

  // ── Admin endpoints ───────────────────────────────────────────────────────

  /** Get delivery statistics (owner/admin) */
  getDeliveryStatistics: () =>
    apiClient.get('/api/v1/notifications/plan-expiry/admin/statistics'),

  /** Get pending notifications summary */
  getPendingNotificationsSummary: () =>
    apiClient.get('/api/v1/notifications/plan-expiry/admin/pending-summary'),

  /** Manually trigger notification processing (admin) */
  triggerProcessing: () =>
    apiClient.post('/api/v1/notifications/plan-expiry/admin/process'),

  /** Get notification configuration */
  getConfiguration: () =>
    apiClient.get('/api/v1/notifications/plan-expiry/admin/config'),

  /** Update notification configuration */
  updateConfiguration: (config) =>
    apiClient.put('/api/v1/notifications/plan-expiry/admin/config', config),

  /** Send urgent notification for an allotment */
  sendUrgentNotification: (allotmentId, notificationType) =>
    apiClient.post('/api/v1/notifications/plan-expiry/schedule/urgent', {
      allotmentId,
      notificationType,
    }),
}

export default planExpiryNotificationService
