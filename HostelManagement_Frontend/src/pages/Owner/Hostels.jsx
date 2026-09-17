import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import HostelList from './HostelList'
import { hostelService } from '../../services/hostelService'
import { BuildingIcon, SparkIcon } from '../../components/icons/AppIcons'
import {
  Alert,
  Button,
  EmptyState,
  PageHeader,
  Skeleton,
} from '../../components/ui'

const Hostels = () => {
  const navigate = useNavigate()
  const [hostels, setHostels] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortOrder, setSortOrder] = useState('RECENT')

  useEffect(() => {
    fetchHostels()
  }, [])

  const fetchHostels = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await hostelService.getAllHostels()
      const hostelsData = (response.data ?? []).map((hostel) => ({
        ...hostel,
        hostelId: hostel.hostelId || hostel.id || hostel._id,
      }))
      setHostels(hostelsData)
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to load hostels. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleHostelClick = (hostel) => {
    navigate(`/owner/hostels/${hostel.hostelId}/floors`, {
      state: {
        hostelId: hostel.hostelId,
        hostelName: hostel.hostelName,
        hostelAddress: hostel.hostelAddress,
      },
    })
  }

  const handleCreateHostel = (action) => {
    if (action === '__refresh__') {
      // Refresh the hostel list after successful creation
      fetchHostels()
    } else {
      // Navigate to create hostel page (fallback)
      navigate('/owner/hostels/create-hostel')
    }
  }

  const visibleHostels = hostels
    .filter((hostel) => {
      const query = searchQuery.trim().toLowerCase()
      return !query || hostel.hostelName?.toLowerCase().includes(query) || hostel.hostelAddress?.toLowerCase().includes(query)
    })
    .sort((a, b) => sortOrder === 'AZ'
      ? (a.hostelName || '').localeCompare(b.hostelName || '')
      : 0)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Hostels"
        description="Manage all your hostel properties from one place. Add new hostels, view existing ones, and navigate to floors and rooms."
        toolbar={
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <input
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search hostels..."
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 sm:w-56"
            />
            <select
              value={sortOrder}
              onChange={(event) => setSortOrder(event.target.value)}
              aria-label="Filter hostels"
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100"
            >
              <option value="RECENT">Filter</option>
              <option value="AZ">Name: A–Z</option>
            </select>
            <Button label="Add hostel" icon={<BuildingIcon className="h-4 w-4" />} onClick={() => navigate('/owner/hostels/create-hostel')} />
          </div>
        }
      />

      {error ? (
        <Alert tone="error" title="We couldn't load your hostels.">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span>{error}</span>
            <Button label="Try again" variant="secondary" onClick={fetchHostels} />
          </div>
        </Alert>
      ) : null}

      <div>
        {loading ? (
          <div className="grid gap-4 lg:grid-cols-2">
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} className="h-64 rounded-3xl" />
            ))}
          </div>
        ) : hostels.length > 0 && visibleHostels.length > 0 ? (
          <HostelList
            hostels={visibleHostels}
            onHostelClick={handleHostelClick}
            onCreateHostel={handleCreateHostel}
          />
        ) : hostels.length > 0 ? (
          <EmptyState
            title="No matching hostels"
            description="Try a different search term or filter to find a property."
          />
        ) : (
          <EmptyState
            icon={<SparkIcon className="h-5 w-5" />}
            title="No hostels yet"
            description="Start by creating your first hostel. From there you can add floors, define room inventory, and move into agreement creation."
            actionLabel="Create your first hostel"
            onAction={() => navigate('/owner/hostels/create-hostel')}
          />
        )}
      </div>
    </div>
  )
}

export default Hostels
