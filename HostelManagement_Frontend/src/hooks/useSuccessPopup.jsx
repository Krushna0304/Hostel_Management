import { useState, useCallback } from 'react'
import { BuildingIcon, LayersIcon, DoorIcon, DocumentIcon, ReceiptIcon, ClipboardIcon, PlusIcon } from '../components/icons/AppIcons'

/**
 * useSuccessPopup - Hook for managing success popup state and actions
 * 
 * @returns {Object} - { isOpen, showSuccess, hideSuccess, popupProps }
 */
export function useSuccessPopup() {
  const [isOpen, setIsOpen] = useState(false)
  const [popupConfig, setPopupConfig] = useState({
    title: 'Success!',
    message: 'Operation completed successfully.',
    actions: [],
    primaryAction: 'Continue'
  })

  const showSuccess = useCallback((config = {}) => {
    setPopupConfig(prev => ({
      ...prev,
      ...config
    }))
    setIsOpen(true)
  }, [])

  const hideSuccess = useCallback(() => {
    setIsOpen(false)
  }, [])

  return {
    isOpen,
    showSuccess,
    hideSuccess,
    popupProps: {
      ...popupConfig,
      isOpen,
      onClose: hideSuccess
    }
  }
}

/**
 * Predefined success popup configurations for common operations
 */
export const successConfigs = {
  hostelCreated: (hostelName, hostelId) => ({
    title: 'Hostel Created Successfully!',
    message: `${hostelName} has been added to your portfolio. You can now add floors and rooms.`,
    actions: [
      {
        label: 'Add Floor',
        icon: <BuildingIcon className="h-4 w-4" />,
        onClick: () => window.location.href = `/owner/hostels/${hostelId}/add-floor`
      },
      {
        label: 'View Floors',
        icon: <LayersIcon className="h-4 w-4" />,
        onClick: () => window.location.href = `/owner/hostels/${hostelId}/floors`
      },
      {
        label: 'Create Another Hostel',
        icon: <PlusIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/hostels/create-hostel'
      }
    ]
  }),

  floorCreated: (floorNumber, hostelId, floorId) => ({
    title: 'Floor Created Successfully!',
    message: `Floor ${floorNumber} has been added. You can now add rooms to this floor.`,
    actions: [
      {
        label: 'Add Room',
        icon: <DoorIcon className="h-4 w-4" />,
        onClick: () => window.location.href = `/owner/hostels/${hostelId}/floors/${floorId}/add-room`
      },
      {
        label: 'View Rooms',
        icon: <LayersIcon className="h-4 w-4" />,
        onClick: () => window.location.href = `/owner/hostels/${hostelId}/floors/${floorId}/rooms`
      },
      {
        label: 'Add Another Floor',
        icon: <PlusIcon className="h-4 w-4" />,
        onClick: () => window.location.href = `/owner/hostels/${hostelId}/add-floor`
      }
    ]
  }),

  roomCreated: (roomNumber, hostelId, floorId) => ({
    title: 'Room Created Successfully!',
    message: `Room ${roomNumber} has been added and is ready for tenant assignments.`,
    actions: [
      {
        label: 'Create Plan',
        icon: <DocumentIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/plans'
      },
      {
        label: 'Create Agreement',
        icon: <ClipboardIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/agreements/create'
      },
      {
        label: 'Add Another Room',
        icon: <PlusIcon className="h-4 w-4" />,
        onClick: () => window.location.href = `/owner/hostels/${hostelId}/floors/${floorId}/add-room`
      }
    ]
  }),

  planCreated: (planName) => ({
    title: 'Plan Created Successfully!',
    message: `${planName} is now available for tenant agreements.`,
    actions: [
      {
        label: 'Create Agreement',
        icon: <ClipboardIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/agreements/create'
      },
      {
        label: 'View All Plans',
        icon: <LayersIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/plans'
      },
      {
        label: 'Create Another Plan',
        icon: <PlusIcon className="h-4 w-4" />,
        onClick: () => window.location.reload()
      }
    ]
  }),

  otherChargeCreated: (chargeName) => ({
    title: 'Other Charge Created Successfully!',
    message: `${chargeName} has been added and can now be assigned to tenants.`,
    actions: [
      {
        label: 'View Other Charges',
        icon: <LayersIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/other-charges'
      },
      {
        label: 'Create Another Charge',
        icon: <PlusIcon className="h-4 w-4" />,
        onClick: () => window.location.reload()
      }
    ]
  }),

  agreementCreated: (tenantName) => ({
    title: 'Agreement Created Successfully!',
    message: `Agreement for ${tenantName} has been created and is now active.`,
    actions: [
      {
        label: 'View Agreements',
        icon: <LayersIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/agreements'
      },
      {
        label: 'Create Another Agreement',
        icon: <PlusIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/agreements/create'
      },
      {
        label: 'View Collections',
        icon: <ReceiptIcon className="h-4 w-4" />,
        onClick: () => window.location.href = '/owner/collections'
      }
    ]
  })
}

export default useSuccessPopup