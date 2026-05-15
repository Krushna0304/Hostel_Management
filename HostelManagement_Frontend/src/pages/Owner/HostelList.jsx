import Button from '../../components/Button'
import { ArrowLeftIcon, BuildingIcon, LayersIcon } from '../../components/icons/AppIcons'
import { Badge, Card, CardContent, CardHeader } from '../../components/ui'

const HostelList = ({ hostels, onHostelClick, onCreateHostel }) => (
  <Card>
    <CardHeader
      title="Property portfolio"
      description="A clean view of every hostel in your workspace with quick access to floors and room inventory."
      action={onCreateHostel ? <Button label="New hostel" onClick={onCreateHostel} /> : null}
    />
    <CardContent>
      <div className="grid gap-4 lg:grid-cols-2">
        {hostels.map((hostel, index) => (
          <button
            key={hostel.hostelId}
            type="button"
            onClick={() => onHostelClick(hostel)}
            className="group rounded-3xl border border-slate-200 bg-slate-50/90 p-5 text-left transition hover:-translate-y-0.5 hover:border-sky-200 hover:bg-white hover:shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/50"
          >
            <div className="flex items-start justify-between gap-4">
              <div className="rounded-2xl bg-white p-3 text-slate-700 shadow-sm">
                <BuildingIcon className="h-5 w-5" />
              </div>
              <Badge variant="info">Hostel {index + 1}</Badge>
            </div>

            <div className="mt-5 space-y-2">
              <h3 className="text-lg font-semibold text-slate-950">{hostel.hostelName}</h3>
              <p className="text-sm leading-6 text-slate-500">{hostel.hostelAddress}</p>
            </div>

            <div className="mt-5 flex items-center justify-between border-t border-slate-200 pt-4">
              <div className="flex items-center gap-2 text-sm text-slate-500">
                <LayersIcon className="h-4 w-4" />
                <span>Open floors and room inventory</span>
              </div>
              <span className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                View
                <ArrowLeftIcon className="h-4 w-4 rotate-180 transition group-hover:translate-x-0.5" />
              </span>
            </div>
          </button>
        ))}
      </div>
    </CardContent>
  </Card>
)

export default HostelList
