import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, CardContent, CardHeader, EmptyState, LoadingScreen } from './ui'
import electricityBillService from '../services/electricityBillService'
import { hostelService } from '../services/hostelService'

const ElectricityAccountManagement = () => {
  const [accounts, setAccounts] = useState([])
  const [hostels, setHostels] = useState([])
  const [selectedHostelId, setSelectedHostelId] = useState('')
  const [rooms, setRooms] = useState([])
  const [accountNumbers, setAccountNumbers] = useState({})
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [isEditMode, setIsEditMode] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingRooms, setLoadingRooms] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true)
        const [accountData, hostelResponse] = await Promise.all([
          electricityBillService.getOwnerAccounts(),
          hostelService.getOwnerHostels(),
        ])
        const ownerHostels = hostelResponse.data || []
        setAccounts(accountData || [])
        setHostels(ownerHostels)
        if (ownerHostels.length > 0) setSelectedHostelId(ownerHostels[0].hostelId)
      } catch (err) {
        setError(err?.response?.data?.message || 'Failed to load electricity account data.')
      } finally {
        setLoading(false)
      }
    }
    loadInitialData()
  }, [])

  useEffect(() => {
    const loadHostelRooms = async () => {
      if (!selectedHostelId) {
        setRooms([])
        setAccountNumbers({})
        return
      }

      try {
        setLoadingRooms(true)
        setError('')
        const response = await hostelService.getHostelRooms(selectedHostelId)
        const hostelRooms = response.data || []
        const accountsByRoomId = new Map(accounts.map((account) => [account.roomId, account]))
        setRooms(hostelRooms)
        setAccountNumbers(Object.fromEntries(hostelRooms.map((room) => [
          room.roomId,
          accountsByRoomId.get(room.roomId)?.accountNumber || '',
        ])))
      } catch (err) {
        setRooms([])
        setAccountNumbers({})
        setError(err?.response?.data?.message || 'Failed to load rooms for this hostel.')
      } finally {
        setLoadingRooms(false)
      }
    }
    loadHostelRooms()
  }, [selectedHostelId, accounts])

  const accountsByRoomId = useMemo(
    () => new Map(accounts.map((account) => [account.roomId, account])),
    [accounts],
  )

  const visibleRooms = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()
    return rooms.filter((room) => {
      const existingAccount = accountsByRoomId.get(room.roomId)
      const accountNumber = accountNumbers[room.roomId] || ''
      const matchesStatus = statusFilter === 'all'
        || (statusFilter === 'created' && Boolean(existingAccount))
        || (statusFilter === 'pending' && !existingAccount)
      const matchesSearch = !query
        || room.roomNumber?.toLowerCase().includes(query)
        || accountNumber.toLowerCase().includes(query)
      return matchesStatus && matchesSearch
    })
  }, [rooms, accountsByRoomId, accountNumbers, searchQuery, statusFilter])

  const roomPairs = useMemo(() => Array.from(
    { length: Math.ceil(visibleRooms.length / 2) },
    (_, index) => visibleRooms.slice(index * 2, index * 2 + 2),
  ), [visibleRooms])

  const handleHostelChange = (event) => {
    setSelectedHostelId(event.target.value)
    setSearchQuery('')
    setStatusFilter('all')
    setSuccess('')
  }

  const updateAccountNumber = (roomId, value) => {
    setAccountNumbers((current) => ({ ...current, [roomId]: value }))
    setSuccess('')
  }

  const handleSave = async () => {
    setError('')
    setSuccess('')

    const rowsToSave = rooms
      .map((room) => {
        const account = accountsByRoomId.get(room.roomId)
        const accountNumber = (accountNumbers[room.roomId] || '').trim()
        return { room, account, accountNumber }
      })
      .filter(({ account, accountNumber }) => accountNumber && (!account || account.accountNumber !== accountNumber))

    if (rowsToSave.length === 0) {
      setError('Enter an account number for a pending room or change an existing account number before saving.')
      return
    }

    const duplicateAccount = rowsToSave.find(({ accountNumber }, index) => (
      rowsToSave.findIndex((row) => row.accountNumber === accountNumber) !== index
    ))
    if (duplicateAccount) {
      setError(`Account number ${duplicateAccount.accountNumber} is repeated. Each room needs a unique account number.`)
      return
    }

    setSaving(true)
    const savedRoomNumbers = []
    try {
      for (const { room, account, accountNumber } of rowsToSave) {
        if (account) {
          await electricityBillService.updateElectricityAccount(account.accountId, {
            roomId: room.roomId,
            accountNumber,
          })
        } else {
          await electricityBillService.createElectricityAccount({ roomId: room.roomId, accountNumber })
        }
        savedRoomNumbers.push(room.roomNumber)
      }

      const refreshedAccounts = await electricityBillService.getOwnerAccounts()
      setAccounts(refreshedAccounts || [])
      setSuccess(`${savedRoomNumbers.length} electricity account mapping${savedRoomNumbers.length === 1 ? '' : 's'} saved successfully.`)
    } catch (err) {
      const failureMessage = err?.response?.data?.message || 'Failed to save electricity account mappings.'
      setError(savedRoomNumbers.length > 0
        ? `Saved rooms ${savedRoomNumbers.join(', ')}, but remaining changes were not saved. ${failureMessage}`
        : failureMessage)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingScreen />

  return (
    <div className="w-full space-y-4 sm:space-y-6">
      <div className="min-w-0">
        <h1 className="text-xl font-bold leading-tight text-slate-950 sm:text-2xl">
          Electricity Account Management
        </h1>
        <p className="mt-1 text-xs leading-5 text-slate-600 sm:text-sm">
          Map electricity account numbers to every room in a hostel.
        </p>
      </div>

      {error ? <Alert tone="error">{error}</Alert> : null}
      {success ? <Alert tone="success">{success}</Alert> : null}

      {hostels.length === 0 ? (
        <Alert tone="info">
          Create a hostel and rooms before managing electricity accounts.
        </Alert>
      ) : (
        <Card>
          <CardHeader
            title="Room account mappings"
            description={isEditMode
              ? 'Edit account numbers, then save all changes together.'
              : 'View existing account numbers. Switch to edit mode to add or change mappings.'}
          />

          <CardContent className="w-full space-y-4 p-4 sm:space-y-5 sm:p-6">
            {/* Filters */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(190px,0.8fr)_minmax(240px,1fr)_minmax(120px,0.35fr)_auto] xl:items-end">
              <label className="block min-w-0 space-y-1.5 sm:space-y-2">
                <span className="text-xs font-medium text-slate-700 sm:text-sm">
                  Select hostel
                </span>
                <select
                  value={selectedHostelId}
                  onChange={handleHostelChange}
                  className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100 sm:h-11"
                >
                  {hostels.map((hostel) => (
                    <option key={hostel.hostelId} value={hostel.hostelId}>
                      {hostel.hostelName}
                    </option>
                  ))}
                </select>
              </label>

              <label className="block min-w-0 space-y-1.5 sm:space-y-2">
                <span className="text-xs font-medium text-slate-700 sm:text-sm">
                  Search rooms or account numbers
                </span>
                <input
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="e.g. A-101 or 123456"
                  className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100 sm:h-11"
                />
              </label>

              <label className="block min-w-0 space-y-1.5 sm:space-y-2">
                <span className="text-xs font-medium text-slate-700 sm:text-sm">
                  Filter
                </span>
                <select
                  value={statusFilter}
                  onChange={(event) => setStatusFilter(event.target.value)}
                  className="h-10 w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100 sm:h-11"
                >
                  <option value="all">All</option>
                  <option value="created">Created</option>
                  <option value="pending">Pending</option>
                </select>
              </label>

              <div className="flex min-w-0 flex-col space-y-1.5 sm:space-y-2">
                <span className="text-xs font-medium text-slate-700 sm:text-sm">Mode</span>
                <Button
                  label={isEditMode ? 'View mode' : 'Edit mode'}
                  variant="secondary"
                  onClick={() => setIsEditMode((current) => !current)}
                  className="whitespace-nowrap"
                />
              </div>
            </div>

            {loadingRooms ? (
              <div className="py-10 text-center text-sm text-slate-500">
                Loading rooms…
              </div>
            ) : rooms.length === 0 ? (
              <EmptyState
                title="No rooms found"
                description="Add rooms to this hostel before creating electricity account mappings."
              />
            ) : (
              <>
                {visibleRooms.length === 0 ? (
                  <p className="rounded-2xl border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500">
                    No rooms match this search or filter.
                  </p>
                ) : (
                  <>
                    {/* Mobile: stacked cards */}
                    <div
                      className="grid grid-cols-1 gap-3 md:hidden"
                      role="region"
                      aria-label="Electricity account mappings"
                    >
                      {visibleRooms.map((room) => {
                        const hasAccount = accountsByRoomId.has(room.roomId)

                        return (
                          <div
                            key={room.roomId}
                            className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
                          >
                            <div className="flex items-start justify-between gap-3">
                              <div className="min-w-0">
                                <p className="text-sm font-semibold text-slate-900">
                                  {room.roomNumber}
                                </p>
                                <span
                                  className={`mt-1 inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                                    hasAccount
                                      ? 'bg-emerald-100 text-emerald-700'
                                      : 'bg-amber-100 text-amber-700'
                                  }`}
                                >
                                  {hasAccount ? 'Created' : 'Pending'}
                                </span>
                              </div>
                            </div>

                            <label className="mt-3 block">
                              <span className="mb-1.5 block text-xs font-medium text-slate-600">
                                Account number
                              </span>
                              <input
                                value={accountNumbers[room.roomId] || ''}
                                onChange={(event) =>
                                  updateAccountNumber(room.roomId, event.target.value)
                                }
                                placeholder="Enter account number"
                                readOnly={!isEditMode}
                                aria-readonly={!isEditMode}
                                className={`h-11 w-full rounded-xl border border-slate-200 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 ${
                                  isEditMode
                                    ? 'focus:border-sky-400 focus:ring-4 focus:ring-sky-100'
                                    : 'cursor-default bg-slate-50 text-slate-600'
                                }`}
                              />
                            </label>
                          </div>
                        )
                      })}
                    </div>

                    {/* Desktop/tablet: two-room table */}
                    <div
                      className="hidden w-full overflow-hidden rounded-2xl border border-slate-200 md:block"
                      role="region"
                      aria-label="Electricity account mappings"
                    >
                      <table className="w-full table-fixed text-left text-sm">
                        <colgroup>
                          <col style={{ width: '20%' }} />
                          <col style={{ width: '30%' }} />
                          <col style={{ width: '20%' }} />
                          <col style={{ width: '30%' }} />
                        </colgroup>

                        <thead className="bg-slate-50 text-xs uppercase tracking-[0.12em] text-slate-500">
                          <tr>
                            <th className="px-3 py-3 font-semibold lg:px-4">Room number</th>
                            <th className="px-3 py-3 font-semibold lg:px-4">Account number</th>
                            <th className="px-3 py-3 font-semibold lg:px-4">Room number</th>
                            <th className="px-3 py-3 font-semibold lg:px-4">Account number</th>
                          </tr>
                        </thead>

                        <tbody className="divide-y divide-slate-100 bg-white">
                          {roomPairs.map(([leftRoom, rightRoom]) => {
                            const renderRoom = (room) => {
                              if (!room) {
                                return (
                                  <>
                                    <td className="px-3 py-3 lg:px-4" />
                                    <td className="px-3 py-3 lg:px-4" />
                                  </>
                                )
                              }

                              const hasAccount = accountsByRoomId.has(room.roomId)

                              return (
                                <>
                                  <td className="px-3 py-3 align-top lg:px-4">
                                    <div className="font-medium text-slate-900">
                                      {room.roomNumber}
                                    </div>
                                    <span
                                      className={`mt-1 inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                                        hasAccount
                                          ? 'bg-emerald-100 text-emerald-700'
                                          : 'bg-amber-100 text-amber-700'
                                      }`}
                                    >
                                      {hasAccount ? 'Created' : 'Pending'}
                                    </span>
                                  </td>

                                  <td className="px-3 py-3 align-top lg:px-4">
                                    <input
                                      value={accountNumbers[room.roomId] || ''}
                                      onChange={(event) =>
                                        updateAccountNumber(
                                          room.roomId,
                                          event.target.value,
                                        )
                                      }
                                      placeholder="Enter account number"
                                      readOnly={!isEditMode}
                                      aria-readonly={!isEditMode}
                                      className={`h-11 w-full min-w-0 rounded-xl border border-slate-200 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 ${
                                        isEditMode
                                          ? 'focus:border-sky-400 focus:ring-4 focus:ring-sky-100'
                                          : 'cursor-default bg-slate-50 text-slate-600'
                                      }`}
                                    />
                                  </td>
                                </>
                              )
                            }

                            return (
                              <tr key={leftRoom.roomId}>
                                {renderRoom(leftRoom)}
                                {renderRoom(rightRoom)}
                              </tr>
                            )
                          })}
                        </tbody>
                      </table>
                    </div>
                  </>
                )}

                {isEditMode ? (
                  <div className="flex w-full justify-stretch sm:justify-end">
                    <Button
                      label="Save account mappings"
                      loading={saving}
                      onClick={handleSave}
                    />
                  </div>
                ) : null}
              </>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
export default ElectricityAccountManagement
