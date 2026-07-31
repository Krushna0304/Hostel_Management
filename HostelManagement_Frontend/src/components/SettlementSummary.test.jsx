import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import SettlementSummary from './SettlementSummary';

// Mock the ui components
vi.mock('./ui/Card', () => ({
  Card: ({ children, className }) => <div className={className}>{children}</div>
}));

vi.mock('./ui/Badge', () => ({
  Badge: ({ children, variant, className }) => (
    <span className={`badge ${variant} ${className}`}>{children}</span>
  )
}));

vi.mock('./SettlementStatusBadge', () => ({
  default: ({ status }) => <span data-testid="status-badge">{status}</span>
}));

describe('Enhanced SettlementSummary', () => {
  const mockSettlement = {
    settlementId: 'settlement-123',
    tenantName: 'John Doe',
    roomNumber: 'A-101',
    status: 'SETTLEMENT_TRANSACTION_CREATED',
    settlementType: 'OWNER_PAYABLE',
    finalSettlementAmount: 2500,
    securityDeposit: 5000,
    outstandingRent: 1200,
    outstandingCharges: 300,
    createdAt: '2024-01-15T10:30:00Z',
    settlementTransactionData: JSON.stringify({
      transactionId: 'tx-456',
      calculationDate: '2024-01-15',
      securityDeposit: 5000,
      outstandingRent: 1200,
      outstandingCharges: 300,
      damageCharges: 0,
      cleaningCharges: 100,
      earlyExitPenalty: 500,
      earlyExitDays: 10,
      totalDeductions: 2100,
      finalSettlementAmount: 2900,
      settlementType: 'OWNER_PAYABLE',
      isEarlyExit: true,
      notes: 'Test transaction',
      createdBy: 'owner-123',
      createdAt: '2024-01-15T10:30:00Z'
    })
  };

  it('renders basic settlement information', () => {
    render(<SettlementSummary settlement={mockSettlement} />);
    
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('Room: A-101')).toBeInTheDocument();
    expect(screen.getByTestId('status-badge')).toHaveTextContent('SETTLEMENT_TRANSACTION_CREATED');
  });

  it('displays enhanced status badge for new settlement statuses', () => {
    render(<SettlementSummary settlement={mockSettlement} />);
    
    const statusBadge = screen.getByTestId('status-badge');
    expect(statusBadge).toBeInTheDocument();
    expect(statusBadge).toHaveTextContent('SETTLEMENT_TRANSACTION_CREATED');
  });

  it('shows early exit badge when settlement is early exit', () => {
    const earlyExitSettlement = {
      ...mockSettlement,
      earlySettlementRequested: true
    };
    
    render(<SettlementSummary settlement={earlyExitSettlement} />);
    
    expect(screen.getByText('Early Exit')).toBeInTheDocument();
  });

  it('displays transaction details when showTransactionDetails is true', () => {
    render(<SettlementSummary settlement={mockSettlement} showTransactionDetails={true} />);
    
    expect(screen.getByText('Transaction Details')).toBeInTheDocument();
    expect(screen.getByText('tx-456...')).toBeInTheDocument();
    expect(screen.getByText('Early Exit Penalty')).toBeInTheDocument();
  });

  it('shows settlement type badge with correct variant', () => {
    render(<SettlementSummary settlement={mockSettlement} />);
    
    const ownerPayableBadge = screen.getByText(/Owner Pays:/);
    expect(ownerPayableBadge).toBeInTheDocument();
    expect(ownerPayableBadge).toHaveClass('success');
  });

  it('displays final settlement amount correctly', () => {
    render(<SettlementSummary settlement={mockSettlement} />);
    
    expect(screen.getByText('Final Settlement Amount')).toBeInTheDocument();
    expect(screen.getByText('₹2,500')).toBeInTheDocument();
    expect(screen.getByText('Owner will refund this amount')).toBeInTheDocument();
  });

  it('shows early settlement information when applicable', () => {
    const earlySettlement = {
      ...mockSettlement,
      requestedEndDate: '2024-02-01',
      earlySettlementRequested: true
    };
    
    render(<SettlementSummary settlement={earlySettlement} showTransactionDetails={true} />);
    
    expect(screen.getByText('Early Settlement Request:')).toBeInTheDocument();
    expect(screen.getByText(/Requested end date:/)).toBeInTheDocument();
  });

  it('handles settlement without transaction data gracefully', () => {
    const basicSettlement = {
      ...mockSettlement,
      settlementTransactionData: null
    };
    
    render(<SettlementSummary settlement={basicSettlement} showTransactionDetails={true} />);
    
    // Should fall back to basic financial breakdown
    expect(screen.getByText('Security Deposit')).toBeInTheDocument();
    expect(screen.getByText('Outstanding Rent')).toBeInTheDocument();
    expect(screen.queryByText('Transaction Details')).not.toBeInTheDocument();
  });

  it('displays transaction metadata when available', () => {
    render(<SettlementSummary settlement={mockSettlement} showTransactionDetails={true} />);
    
    expect(screen.getByText('Transaction Created:')).toBeInTheDocument();
    expect(screen.getByText('Created By:')).toBeInTheDocument();
    expect(screen.getByText('owner-123')).toBeInTheDocument();
  });

  it('shows SETTLEMENT_DONE status correctly', () => {
    const completedSettlement = {
      ...mockSettlement,
      status: 'SETTLEMENT_DONE'
    };
    
    render(<SettlementSummary settlement={completedSettlement} />);
    
    expect(screen.getByText('✓ Settlement Transaction Completed')).toBeInTheDocument();
  });

  it('renders actions when showActions is true', () => {
    const mockOnAction = vi.fn(() => <button>Test Action</button>);
    
    render(
      <SettlementSummary 
        settlement={mockSettlement} 
        showActions={true}
        onAction={mockOnAction}
      />
    );
    
    expect(mockOnAction).toHaveBeenCalledWith(mockSettlement);
    expect(screen.getByText('Test Action')).toBeInTheDocument();
  });
});