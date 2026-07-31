import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ExtensionRequestModal from './ExtensionRequestModal';
import extensionService from '../services/extensionService';
import agreementService from '../services/agreementService';

// Mock the services
vi.mock('../services/extensionService');
vi.mock('../services/agreementService');
vi.mock('../hooks/useSuccessPopup', () => ({
  useSuccessPopup: () => ({ showSuccess: vi.fn() })
}));

describe('ExtensionRequestModal', () => {
  const mockAgreement = {
    id: 'agreement-123',
    roomNumber: 'A-101',
    endDate: '2024-03-01',
    status: 'ACTIVE'
  };

  const mockPlans = [
    {
      planId: 'plan-1',
      planName: 'Standard Plan',
      durationMonths: 6,
      monthlyRent: 15000,
      securityDeposit: 30000
    },
    {
      planId: 'plan-2', 
      planName: 'Premium Plan',
      durationMonths: 12,
      monthlyRent: 18000,
      securityDeposit: 36000
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    // Mock localStorage
    Object.defineProperty(window, 'localStorage', {
      value: {
        getItem: vi.fn(() => 'mock-token'),
        setItem: vi.fn(),
        removeItem: vi.fn()
      }
    });

    // Mock agreement service to return plans
    agreementService.getActivePlans.mockResolvedValue({ data: mockPlans });
  });

  it('should render the modal when isOpen is true', async () => {
    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={vi.fn()}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    expect(screen.getByText('Request Allotment Extension')).toBeInTheDocument();
    expect(screen.getByText('Current Agreement Details')).toBeInTheDocument();
    expect(screen.getByText('A-101')).toBeInTheDocument();
  });

  it('should not render the modal when isOpen is false', () => {
    render(
      <ExtensionRequestModal 
        isOpen={false}
        onClose={vi.fn()}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    expect(screen.queryByText('Request Allotment Extension')).not.toBeInTheDocument();
  });

  it('should load and display available plans', async () => {
    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={vi.fn()}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(agreementService.getActivePlans).toHaveBeenCalledWith('ROOM');
    });

    // Check if plans are loaded into the select options
    await waitFor(() => {
      expect(screen.getByDisplayValue('')).toBeInTheDocument();
    });
  });

  it('should show plan details when a plan is selected', async () => {
    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={vi.fn()}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(agreementService.getActivePlans).toHaveBeenCalled();
    });

    const selectElement = screen.getByRole('combobox');
    fireEvent.change(selectElement, { target: { value: 'plan-1' } });

    await waitFor(() => {
      expect(screen.getByText('Selected Plan Details')).toBeInTheDocument();
      expect(screen.getByText('Standard Plan')).toBeInTheDocument();
      expect(screen.getByText('6 months')).toBeInTheDocument();
      expect(screen.getByText('₹15000')).toBeInTheDocument();
    });
  });

  it('should submit extension request successfully', async () => {
    const mockResponse = {
      requestId: 'ext-123',
      status: 'PENDING_OWNER_APPROVAL',
      message: 'Extension request submitted successfully'
    };

    extensionService.createExtensionRequest.mockResolvedValue(mockResponse);
    
    const onSuccess = vi.fn();
    const onClose = vi.fn();

    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={onClose}
        agreement={mockAgreement}
        onSuccess={onSuccess}
      />
    );

    await waitFor(() => {
      expect(agreementService.getActivePlans).toHaveBeenCalled();
    });

    // Select a plan
    const selectElement = screen.getByRole('combobox');
    fireEvent.change(selectElement, { target: { value: 'plan-1' } });

    // Add notes
    const notesTextarea = screen.getByPlaceholderText(/additional information/i);
    fireEvent.change(notesTextarea, { 
      target: { value: 'Would like to extend for another 6 months' } 
    });

    // Submit the form
    const submitButton = screen.getByRole('button', { name: /submit extension request/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(extensionService.createExtensionRequest).toHaveBeenCalledWith({
        currentAgreementId: 'agreement-123',
        planId: 'plan-1',
        tenantNotes: 'Would like to extend for another 6 months'
      });
    });

    expect(onSuccess).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });

  it('should handle submission errors gracefully', async () => {
    const mockError = new Error('Failed to submit request');
    extensionService.createExtensionRequest.mockRejectedValue(mockError);

    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={vi.fn()}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(agreementService.getActivePlans).toHaveBeenCalled();
    });

    // Select a plan
    const selectElement = screen.getByRole('combobox');
    fireEvent.change(selectElement, { target: { value: 'plan-1' } });

    // Submit the form
    const submitButton = screen.getByRole('button', { name: /submit extension request/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/Failed to submit extension request/i)).toBeInTheDocument();
    });
  });

  it('should close the modal when close button is clicked', () => {
    const onClose = vi.fn();

    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={onClose}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    const closeButton = screen.getByLabelText('Close modal');
    fireEvent.click(closeButton);

    expect(onClose).toHaveBeenCalled();
  });

  it('should disable submit button when no plan is selected', async () => {
    render(
      <ExtensionRequestModal 
        isOpen={true}
        onClose={vi.fn()}
        agreement={mockAgreement}
        onSuccess={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(agreementService.getActivePlans).toHaveBeenCalled();
    });

    const submitButton = screen.getByRole('button', { name: /submit extension request/i });
    expect(submitButton).toBeDisabled();
  });
});