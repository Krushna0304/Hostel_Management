import { useState, lazy, Suspense } from 'react'
import { Button, LoadingScreen } from '../../components/ui'

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
        <nav className="flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 py-2.5 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === tab.id
                  ? 'border-slate-950 text-slate-950'
                  : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
              }`}
            >
              <span className="text-lg">{tab.icon}</span>
              {tab.label}
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