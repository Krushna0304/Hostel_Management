# Enhanced SettlementSummary Component

## Overview

The enhanced `SettlementSummary` component provides a comprehensive display for settlement information in the Enhanced Settlement and Allotment System. It supports both legacy settlement data and new enhanced settlement transaction data with improved status handling and transaction details.

## Features

### Enhanced Settlement Support
- **Transaction Data Display**: Shows detailed transaction information when available
- **Early Exit Indicators**: Visual indicators for early settlement requests
- **Enhanced Status Handling**: Support for new settlement statuses from the enhanced system
- **Transaction Metadata**: Displays transaction IDs, creation timestamps, and creator information

### Settlement Transaction Details
- **Comprehensive Breakdown**: Shows security deposit, outstanding amounts, penalties, and deductions
- **Early Exit Penalties**: Special handling for early exit scenarios with penalty amounts and days
- **Round-Trip Data Support**: Handles JSON transaction data parsing and display
- **Backward Compatibility**: Falls back to legacy display for older settlement records

### Visual Enhancements
- **Status Badges**: Enhanced status badge system with new settlement statuses
- **Settlement Type Indicators**: Clear visual indicators for owner vs tenant payable amounts
- **Early Exit Badges**: Special badges for early settlement requests
- **Transaction Cards**: Dedicated sections for transaction metadata display

## Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `settlement` | Object | Required | Settlement data object |
| `showActions` | Boolean | `false` | Whether to show action buttons |
| `onAction` | Function | - | Callback function for rendering actions |
| `showTransactionDetails` | Boolean | `false` | Whether to show detailed transaction breakdown |

## Settlement Data Structure

### Enhanced Settlement Object
```javascript
{
  settlementId: 'settlement-123',
  tenantName: 'John Doe',
  roomNumber: 'A-101',
  status: 'SETTLEMENT_TRANSACTION_CREATED', // Enhanced status
  settlementType: 'OWNER_PAYABLE' | 'TENANT_PAYABLE',
  finalSettlementAmount: 2500,
  earlySettlementRequested: true, // Early exit flag
  requestedEndDate: '2024-02-01', // For early settlements
  settlementTransactionData: '{"transactionId": "tx-456", ...}', // JSON string
  // ... other settlement fields
}
```

### Transaction Data Structure
```javascript
{
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
  notes: 'Transaction notes',
  createdBy: 'owner-123',
  createdAt: '2024-01-15T10:30:00Z'
}
```

## Supported Settlement Statuses

### Original Statuses
- `PENDING_OWNER_REVIEW`
- `CALCULATION_IN_PROGRESS`
- `PENDING_TENANT_PAYMENT`
- `PENDING_OWNER_PAYMENT`
- `PAYMENT_IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`
- `REJECTED`

### Enhanced Statuses
- `SETTLEMENT_REQUESTED` - Settlement has been requested by tenant
- `SETTLEMENT_TRANSACTION_CREATED` - Settlement transaction created by owner
- `SETTLEMENT_APPROVED` - Settlement approved by owner
- `SETTLEMENT_DONE` - Settlement transaction completed

## Usage Examples

### Basic Usage
```jsx
<SettlementSummary settlement={settlementData} />
```

### With Actions
```jsx
<SettlementSummary 
  settlement={settlementData}
  showActions={true}
  onAction={(settlement) => <Button>Review</Button>}
/>
```

### With Transaction Details
```jsx
<SettlementSummary 
  settlement={settlementData}
  showTransactionDetails={true}
/>
```

### Enhanced Settlement with All Features
```jsx
<SettlementSummary 
  settlement={enhancedSettlementData}
  showActions={true}
  showTransactionDetails={true}
  onAction={(settlement) => renderActionButtons(settlement)}
/>
```

## Integration Notes

### With Enhanced Settlement System
- The component automatically detects enhanced settlement data via `settlementTransactionData`
- Early exit settlements are identified by `earlySettlementRequested` or transaction data flags
- Transaction details are parsed from JSON and displayed in organized sections

### With Settlement Status System
- Uses enhanced `SettlementStatusBadge` for status display
- Supports both original and enhanced status transitions
- Provides contextual information based on settlement state

### With Settlement Actions
- Action rendering is delegated to parent components via `onAction` callback
- Different actions based on settlement status and type
- Support for enhanced status-specific actions

## Styling

The component uses Tailwind CSS classes and follows the design system:
- **Cards**: Uses `Card` component for main container
- **Badges**: Uses `Badge` component for status and type indicators  
- **Colors**: Semantic colors (green for positive, red for negative, blue for neutral)
- **Layout**: Responsive grid layouts for financial breakdowns
- **Typography**: Consistent font weights and sizes

## Error Handling

- **JSON Parsing**: Gracefully handles invalid transaction data JSON
- **Missing Data**: Provides fallbacks for undefined or null values
- **Backward Compatibility**: Falls back to legacy display for older settlements
- **Null Safety**: Handles missing transaction data without errors

## Testing

The component includes comprehensive unit tests covering:
- Basic settlement information display
- Enhanced status badge rendering
- Transaction details display
- Early exit indicators
- Settlement type badges
- Action button integration
- Error handling scenarios

Run tests with:
```bash
npm test SettlementSummary.test.jsx
```