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

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Property Management"
        title="Your Hostels"
        description="Manage all your hostel properties from one place. Add new hostels, view existing ones, and navigate to floors and rooms."
        action={
          <Button 
            label="Add hostel" 
            icon={<BuildingIcon className="h-4 w-4" />}
            onClick={() => navigate('/owner/hostels/create-hostel')} 
          />
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
        ) : hostels.length > 0 ? (
          <HostelList
            hostels={hostels}
            onHostelClick={handleHostelClick}
            onCreateHostel={handleCreateHostel}
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