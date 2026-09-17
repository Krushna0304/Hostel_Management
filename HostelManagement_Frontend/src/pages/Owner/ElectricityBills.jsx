import { useState, lazy, Suspense } from 'react'
import { LoadingScreen } from '../../components/ui'

// Lazy load components to avoid potential circular dependency issues
const ElectricityAccountManagement = lazy(() => import('../../components/ElectricityAccountManagement'))
const ElectricityBillCreation = lazy(() => import('../../components/ElectricityBillCreation'))
const ElectricityBillCards = lazy(() => import('../../components/ElectricityBillCards'))

export default function ElectricityBills() {
  const [activeTab, setActiveTab] = useState('bills')

  const tabs = [
    { id: 'bills', label: 'Bills Overview', icon: '⚡' },
    { id: 'create', label: 'Create Bills', icon: '➕' },
    { id: 'accounts', label: 'Manage Accounts', icon: '⚙️' }
  ]

  const renderContent = () => {
    const ComponentToRender = () => {
      switch (activeTab) {
        case 'accounts':
          return <ElectricityAccountManagement />
        case 'create':
          return <ElectricityBillCreation />
        case 'bills':
        default:
          return <ElectricityBillCards />
      }
    }

    return (
      <Suspense fallback={<LoadingScreen />}>
        <ComponentToRender />
      </Suspense>
    )
  }

  return (
    <div className="space-y-4">
      {/* Page Header */}
      {/* <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-950">Electricity Bill Management</h1>
          <p className="text-slate-600 mt-1">Manage electricity accounts, create bills, and track payments</p>
        </div>
      </div> */}

      {/* Tab Navigation */}
      <div className="border-b border-slate-200">
        <nav className="grid grid-cols-3 gap-1 sm:flex sm:gap-6" aria-label="Electricity bill sections">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              aria-current={activeTab === tab.id ? 'page' : undefined}
              className={`flex min-w-0 items-center justify-center gap-1 border-b-2 px-1 py-2.5 text-xs font-medium leading-4 transition-colors sm:w-auto sm:gap-2 sm:px-2 sm:text-sm ${
                activeTab === tab.id
                  ? 'border-slate-950 text-slate-950'
                  : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
              }`}
            >
              <span className="shrink-0 text-base sm:text-lg">{tab.icon}</span>
              <span className="min-w-0 text-center">{tab.label}</span>
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="min-h-[400px]">
        {renderContent()}
      </div>
    </div>
  )
}
