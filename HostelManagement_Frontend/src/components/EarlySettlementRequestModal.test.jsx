import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import EarlySettlementRequestModal from './EarlySettlementRequestModal';
import settlementService from '../services/settlementService';

// Mock the settlement service
vi.mock('../services/settlementService');

// Mock the success popup hook
vi.mock('../hooks/useSuccessPopup', () => ({
  useSuccessPopup: () => ({
    showSuccess: vi.fn()
  })
}));

describe('EarlySettlementRequestModal', () => {
  const mockAgreement = {
    id: 'agreement-123',
    roomNumber: 'A-101',
    endDate: '2024-06-30',
    status: 'ACTIVE'
  };

  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    agreement: mockAgreement,
    onSuccess: vi.fn()
  };

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
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('renders modal when open', () => {
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    expect(screen.getByText('Request Early Settlement')).toBeInTheDocument();
    expect(screen.getByText('Agreement Details')).toBeInTheDocument();
    expect(screen.getByText('Room: A-101')).toBeInTheDocument();
  });

  test('does not render when closed', () => {
    render(<EarlySettlementRequestModal {...defaultProps} isOpen={false} />);
    
    expect(screen.queryByText('Request Early Settlement')).not.toBeInTheDocument();
  });

  test('displays form fields correctly', () => {
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    expect(screen.getByLabelText(/requested move-out date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/reason for early exit/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/additional notes/i)).toBeInTheDocument();
  });

  test('shows early settlement warning', () => {
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    expect(screen.getByText('Early Settlement Notice')).toBeInTheDocument();
    expect(screen.getByText(/early exit may incur additional charges/i)).toBeInTheDocument();
  });

  test('validates required fields', async () => {
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    const submitButton = screen.getByRole('button', { name: /submit early settlement request/i });
    fireEvent.click(submitButton);
    
    // Form should not submit without required fields
    expect(settlementService.requestEarlySettlement).not.toHaveBeenCalled();
  });

  test('submits form with valid data', async () => {
    const mockResponse = {
      settlementId: 'settlement-456',
      status: 'SETTLEMENT_REQUESTED',
      message: 'Early settlement request submitted successfully'
    };
    
    settlementService.requestEarlySettlement.mockResolvedValue(mockResponse);
    
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    // Fill out the form
    const dateInput = screen.getByLabelText(/requested move-out date/i);
    const reasonInput = screen.getByLabelText(/reason for early exit/i);
    const notesInput = screen.getByLabelText(/additional notes/i);
    
    fireEvent.change(dateInput, { target: { value: '2024-05-15' } });
    fireEvent.change(reasonInput, { target: { value: 'Job relocation' } });
    fireEvent.change(notesInput, { target: { value: 'Moving to another city for new job' } });
    
    const submitButton = screen.getByRole('button', { name: /submit early settlement request/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(settlementService.requestEarlySettlement).toHaveBeenCalledWith({
        agreementId: 'agreement-123',
        requestedEndDate: '2024-05-15',
        reason: 'Job relocation',
        tenantNotes: 'Moving to another city for new job'
      });
    });
    
    expect(defaultProps.onSuccess).toHaveBeenCalled();
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  test('handles form submission error', async () => {
    const mockError = {
      response: {
        status: 400,
        data: {
          message: 'Early exit not allowed for this agreement type'
        }
      }
    };
    
    settlementService.requestEarlySettlement.mockRejectedValue(mockError);
    
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    // Fill and submit form
    const dateInput = screen.getByLabelText(/requested move-out date/i);
    const reasonInput = screen.getByLabelText(/reason for early exit/i);
    
    fireEvent.change(dateInput, { target: { value: '2024-05-15' } });
    fireEvent.change(reasonInput, { target: { value: 'Job relocation' } });
    
    const submitButton = screen.getByRole('button', { name: /submit early settlement request/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Early exit not allowed for this agreement type')).toBeInTheDocument();
    });
  });

  test('closes modal when close button is clicked', () => {
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    const closeButton = screen.getByLabelText('Close modal');
    fireEvent.click(closeButton);
    
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  test('sets date validation correctly', () => {
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    const dateInput = screen.getByLabelText(/requested move-out date/i);
    
    // Should have min date as today
    const today = new Date().toISOString().split('T')[0];
    expect(dateInput.getAttribute('min')).toBe(today);
    
    // Should have max date as agreement end date
    const agreementEndDate = '2024-06-30';
    expect(dateInput.getAttribute('max')).toBe(agreementEndDate);
  });

  test('handles missing authentication token', async () => {
    // Mock localStorage to return null token
    window.localStorage.getItem.mockReturnValue(null);
    
    render(<EarlySettlementRequestModal {...defaultProps} />);
    
    const dateInput = screen.getByLabelText(/requested move-out date/i);
    const reasonInput = screen.getByLabelText(/reason for early exit/i);
    
    fireEvent.change(dateInput, { target: { value: '2024-05-15' } });
    fireEvent.change(reasonInput, { target: { value: 'Job relocation' } });
    
    const submitButton = screen.getByRole('button', { name: /submit early settlement request/i });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Authentication token not found. Please log in again.')).toBeInTheDocument();
    });
  });
});