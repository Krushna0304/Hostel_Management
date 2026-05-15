import apiClient from './apiClient'

const reminderService = {
  /** Get current subscription (includes smsRemindersEnabled flag) */
  getSubscription: () => apiClient.get('/owner/subscription'),

  /** Toggle SMS reminders on or off */
  toggleSmsReminders: (enabled) =>
    apiClient.post('/owner/subscription/toggle-sms-reminders', { enabled }),

  /** Get all SMS templates (custom + defaults) */
  getAllTemplates: () => apiClient.get('/owner/sms-templates/all'),

  /** Get only custom templates saved by this owner */
  getCustomTemplates: () => apiClient.get('/owner/sms-templates'),

  /** Create or update a custom SMS template */
  saveTemplate: (reminderType, templateContent) =>
    apiClient.post('/owner/sms-templates', { reminderType, templateContent }),

  /** Delete a custom template by ID */
  deleteTemplate: (templateId) =>
    apiClient.delete(`/owner/sms-templates/${templateId}`),
}

export default reminderService
