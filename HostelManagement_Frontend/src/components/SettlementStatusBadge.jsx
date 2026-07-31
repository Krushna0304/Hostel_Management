import { Badge } from './ui/Badge';

const SettlementStatusBadge = ({ status }) => {
  const getStatusConfig = (status) => {
    const statusConfig = {
      // Original statuses
      PENDING_OWNER_REVIEW: { 
        variant: 'warning', 
        label: 'Pending Review',
        description: 'Waiting for owner to review and approve'
      },
      CALCULATION_IN_PROGRESS: { 
        variant: 'secondary', 
        label: 'Calculating',
        description: 'Settlement amount being calculated'
      },
      PENDING_TENANT_PAYMENT: { 
        variant: 'destructive', 
        label: 'Payment Required',
        description: 'Tenant needs to make payment'
      },
      PENDING_OWNER_PAYMENT: { 
        variant: 'warning', 
        label: 'Refund Pending',
        description: 'Owner needs to process refund'
      },
      PAYMENT_IN_PROGRESS: { 
        variant: 'secondary', 
        label: 'Processing',
        description: 'Payment is being processed'
      },
      COMPLETED: { 
        variant: 'success', 
        label: 'Completed',
        description: 'Settlement successfully completed'
      },
      CANCELLED: { 
        variant: 'secondary', 
        label: 'Cancelled',
        description: 'Settlement was cancelled'
      },
      REJECTED: { 
        variant: 'destructive', 
        label: 'Rejected',
        description: 'Settlement request was rejected'
      },
      
      // Enhanced settlement statuses (from Enhanced Settlement System)
      SETTLEMENT_REQUESTED: {
        variant: 'warning',
        label: 'Settlement Requested', 
        description: 'Settlement has been requested by tenant'
      },
      SETTLEMENT_TRANSACTION_CREATED: {
        variant: 'secondary',
        label: 'Transaction Created',
        description: 'Settlement transaction has been created'
      },
      SETTLEMENT_APPROVED: {
        variant: 'success',
        label: 'Approved',
        description: 'Settlement has been approved by owner'
      },
      SETTLEMENT_DONE: {
        variant: 'success',
        label: 'Settlement Complete',
        description: 'Settlement transaction has been completed'
      },
      
      // Room allotment related statuses
      SETTLEMENT_PENDING: {
        variant: 'warning',
        label: 'Settlement Pending',
        description: 'Settlement is pending for this allotment'
      },
      SETTLEMENT_TASK: {
        variant: 'secondary',
        label: 'Settlement Task',
        description: 'Settlement task needs to be completed'
      },
      ON_NOTICE_PERIOD: {
        variant: 'warning',
        label: 'Notice Period',
        description: 'Tenant is in notice period'
      },
      ALLOTMENT_ACTION_PENDING: {
        variant: 'destructive',
        label: 'Action Required',
        description: 'Allotment action is pending'
      }
    };

    return statusConfig[status] || { 
      variant: 'secondary', 
      label: status,
      description: 'Unknown status'
    };
  };

  const config = getStatusConfig(status);
  
  return (
    <Badge 
      variant={config.variant}
      title={config.description}
    >
      {config.label}
    </Badge>
  );
};

export default SettlementStatusBadge;