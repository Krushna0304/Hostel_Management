import { Button, useConfirm } from './index'

// Demo component showing different confirmation modal variants
export default function ConfirmationDemo() {
  const confirm = useConfirm()

  const handleDefaultConfirm = async () => {
    const result = await confirm('This is a default confirmation message.')
    console.log('Default confirmation result:', result)
  }

  const handleWarningConfirm = async () => {
    const result = await confirm(
      'This action will deactivate the item. Are you sure you want to continue?',
      {
        variant: 'warning',
        confirmText: 'Deactivate',
        cancelText: 'Cancel'
      }
    )
    console.log('Warning confirmation result:', result)
  }

  const handleDangerConfirm = async () => {
    const result = await confirm(
      'This action will permanently delete the item and cannot be undone.',
      {
        variant: 'danger',
        confirmText: 'Delete',
        cancelText: 'Cancel'
      }
    )
    console.log('Danger confirmation result:', result)
  }

  const handleCustomConfirm = async () => {
    const result = await confirm(
      'Would you like to save your changes before continuing?',
      {
        title: 'Unsaved Changes',
        confirmText: 'Save',
        cancelText: 'Discard',
        variant: 'default'
      }
    )
    console.log('Custom confirmation result:', result)
  }

  return (
    <div className="space-y-4 p-6">
      <h2 className="text-xl font-bold">Confirmation Modal Demo</h2>
      <div className="flex gap-4">
        <Button label="Default Confirm" onClick={handleDefaultConfirm} />
        <Button label="Warning Confirm" variant="warning" onClick={handleWarningConfirm} />
        <Button label="Danger Confirm" variant="danger" onClick={handleDangerConfirm} />
        <Button label="Custom Confirm" variant="secondary" onClick={handleCustomConfirm} />
      </div>
    </div>
  )
}