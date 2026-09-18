import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as XLSX from 'xlsx'
import agreementService from '../../../services/agreementService'
import { floorService, hostelService } from '../../../services/hostelService'
import { Alert, Button, Card, CardContent } from '../../../components/ui'

const emptyRow = () => ({ name: '', phoneNumber: '', roomNumber: '', floorId: '', startDate: '', endDate: '' })
const dateAfterDuration = (startDate, plan) => {
  if (!startDate || !plan?.duration) return ''
  const date = new Date(`${startDate}T00:00:00`)
  const duration = plan.duration.durationType === 'NOT_FIXED'
    ? (plan.duration.minimumStayMonths || 1) : (plan.duration.value || 0)
  if (String(plan.duration.unit).toUpperCase() === 'YEAR' && plan.duration.durationType !== 'NOT_FIXED') date.setFullYear(date.getFullYear() + duration)
  else date.setMonth(date.getMonth() + duration)
  return date.toISOString().slice(0, 10)
}

export default function ExistingTenantOnboarding() {
  const navigate = useNavigate()
  const inputRef = useRef(null)
  const [step, setStep] = useState(1)
  const [hostels, setHostels] = useState([])
  const [plans, setPlans] = useState([])
  const [floors, setFloors] = useState([])
  const [common, setCommon] = useState({ hostelId: '', planId: '', agreementType: 'PG_ROOM', defaultFloorId: '' })
  const [rows, setRows] = useState([emptyRow()])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const selectedPlan = useMemo(() => plans.find((plan) => plan.id === common.planId), [plans, common.planId])

  useEffect(() => { (async () => {
    try {
      const [hostelRes, planRes] = await Promise.all([hostelService.getAllHostels(), agreementService.getActivePlans('PG_ROOM')])
      setHostels(hostelRes.data || []); setPlans(planRes.data || [])
    } catch { setError('Could not load hostels and plans.') }
  })() }, [])
  useEffect(() => { if (common.hostelId) floorService.getFloorsByHostel(common.hostelId).then((r) => setFloors(r.data || [])).catch(() => setFloors([])) }, [common.hostelId])

  const updateRow = (index, key, value) => setRows((old) => old.map((row, i) => {
    if (i !== index) return row
    const next = { ...row, [key]: value }
    if (key === 'startDate') next.endDate = dateAfterDuration(value, selectedPlan)
    return next
  }))
  const updatePlan = (planId) => {
    const plan = plans.find((item) => item.id === planId)
    setCommon((value) => ({ ...value, planId }))
    setRows((old) => old.map((row) => ({ ...row, endDate: dateAfterDuration(row.startDate, plan) || row.endDate })))
  }
  const localValidate = () => {
    if (!common.hostelId || !common.planId) return 'Hostel and plan are required.'
    const phones = new Set()
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i], number = row.phoneNumber.replace(/\D/g, '')
      if (!row.name.trim() || !row.roomNumber.trim() || !row.startDate || !row.endDate) return `Row ${i + 1}: complete all required fields.`
      if (!/^\d{10,15}$/.test(number)) return `Row ${i + 1}: enter a valid phone number.`
      if (phones.has(number)) return `Row ${i + 1}: duplicate phone number in this batch.`
      phones.add(number)
      if (row.endDate <= row.startDate) return `Row ${i + 1}: end date must be after start date.`
    }
    return ''
  }
  const continueToReview = () => { const message = localValidate(); setError(message); if (!message) setStep(3) }
  const downloadTemplate = () => {
    const sheet = XLSX.utils.json_to_sheet([{ Name: 'Rahul Patil', 'Phone Number': '9876543210', 'Room Number': '205', Floor: '', 'Start Date': '2026-09-17', 'End Date': '2027-09-17' }])
    const book = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(book, sheet, 'Existing tenants'); XLSX.writeFile(book, 'existing-tenant-onboarding-template.xlsx')
  }
  const importSheet = async (event) => {
    const file = event.target.files?.[0]; if (!file) return
    try {
      const book = XLSX.read(await file.arrayBuffer(), { type: 'array', cellDates: true })
      const imported = XLSX.utils.sheet_to_json(book.Sheets[book.SheetNames[0]], { defval: '' }).map((item) => ({
        name: String(item.Name ?? item.name ?? '').trim(), phoneNumber: String(item['Phone Number'] ?? item.phoneNumber ?? '').trim(),
        roomNumber: String(item['Room Number'] ?? item.roomNumber ?? '').trim(), floorId: String(item.Floor ?? item.floorId ?? '').trim(),
        startDate: toDate(item['Start Date'] ?? item.startDate), endDate: toDate(item['End Date'] ?? item.endDate),
      }))
      if (!imported.length) throw new Error('The file has no tenant rows.')
      // Floor cells may contain either an ID or a displayed floor number.
      setRows(imported.map((row) => ({ ...row, floorId: floors.find((floor) => String(floor.floorNumber) === row.floorId)?.floorId || row.floorId })))
      setError('')
    } catch (err) { setError(err.message || 'Could not read the Excel file.') }
    finally { event.target.value = '' }
  }
  const submit = async () => {
    const message = localValidate(); if (message) { setError(message); setStep(2); return }
    setLoading(true); setError('')
    try {
      const response = await agreementService.onboardExistingTenants({ ...common, defaultFloorId: common.defaultFloorId || null, tenants: rows.map((row) => ({ ...row, floorId: row.floorId || null })) })
      setResult(response.data); setStep(4)
    } catch (err) { setError(err.response?.data?.message || err.response?.data || 'Onboarding failed. No tenants were created.'); setStep(2) }
    finally { setLoading(false) }
  }

  if (step === 4) return <Card className="mx-auto max-w-5xl"><CardContent className="space-y-5 p-6"><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-2xl font-semibold text-emerald-700">{result?.onboardedCount} tenants onboarded successfully.</h2><Button label="Back to Agreements" variant="secondary" onClick={() => navigate('/owner/agreements')} /></div><p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">Save these temporary passwords now. They are shown only once and are stored securely as hashes.</p><div className="overflow-x-auto"><table className="w-full text-sm"><thead><tr className="border-b text-left"><th className="p-2">Name</th><th>Username</th><th>Temporary password</th><th>Room</th><th>Agreement</th><th>Payment plan</th></tr></thead><tbody>{result?.tenants?.map((tenant) => <tr key={tenant.username} className="border-b"><td className="p-2">{tenant.name}</td><td>{tenant.username}</td><td className="font-mono">{tenant.temporaryPassword}</td><td>{tenant.roomNumber}</td><td>{tenant.agreementStatus}</td><td>{tenant.paymentPlanStatus}</td></tr>)}</tbody></table></div><Button label="Onboard another batch" onClick={() => { setRows([emptyRow()]); setResult(null); setStep(1) }} /></CardContent></Card>

  return <Card className="mx-auto max-w-6xl"><CardContent className="space-y-6 p-5 sm:p-7">
    <div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="text-xl font-semibold">Onboard Existing Tenants</h2><p className="mt-1 text-sm text-slate-500">Existing tenants are activated directly; their first scheduled payment is not assumed to be collected.</p></div><Button label="Back to Agreements" variant="secondary" onClick={() => navigate('/owner/agreements')} /></div>
    <div className="grid gap-2 sm:grid-cols-3">{['Common information', 'Tenant data', 'Review'].map((label, index) => <div key={label} className={`rounded-xl border px-3 py-2 text-sm ${step === index + 1 ? 'border-sky-300 bg-sky-50 font-semibold' : 'border-slate-200 text-slate-500'}`}>{index + 1}. {label}</div>)}</div>
    {error && <Alert tone="error">{String(error)}</Alert>}
    {step === 1 && <div className="grid gap-4 md:grid-cols-2"><Select label="Hostel" value={common.hostelId} onChange={(v) => setCommon((x) => ({ ...x, hostelId: v, defaultFloorId: '' }))} options={hostels.map((h) => [h.hostelId, h.hostelName])} required /><Select label="Plan" value={common.planId} onChange={updatePlan} options={plans.map((p) => [p.id, p.planName])} required /><Select label="Agreement type" value={common.agreementType} onChange={(v) => setCommon((x) => ({ ...x, agreementType: v }))} options={[[ 'PG_ROOM', 'Room Agreement' ]]} /><Select label="Default floor (optional)" value={common.defaultFloorId} onChange={(v) => setCommon((x) => ({ ...x, defaultFloorId: v }))} options={floors.map((f) => [f.floorId, `Floor ${f.floorNumber}`])} /><div className="md:col-span-2"><Button label="Continue to tenant data" onClick={() => { const m = localValidate(); if (!common.hostelId || !common.planId) setError('Hostel and plan are required.'); else { setError(''); setStep(2) } }} /></div></div>}
    {step === 2 && <><div className="flex flex-wrap gap-2"><Button label="Download Excel template" variant="secondary" onClick={downloadTemplate} /><Button label="Upload Excel" variant="secondary" onClick={() => inputRef.current?.click()} /><input ref={inputRef} type="file" accept=".xlsx,.xls,.csv" className="hidden" onChange={importSheet} /><Button label="Add row" onClick={() => setRows((old) => [...old, emptyRow()])} /></div><div className="overflow-x-auto"><table className="w-full min-w-[900px] text-sm"><thead><tr className="border-b text-left text-slate-500"><th className="p-2">Name</th><th>Phone number</th><th>Room number</th><th>Floor</th><th>Start date</th><th>End date</th><th /></tr></thead><tbody>{rows.map((row, index) => <tr className="border-b" key={index}>{[['name','text'],['phoneNumber','tel'],['roomNumber','text']].map(([key,type]) => <td className="p-2" key={key}><input className="w-full rounded border p-2" type={type} value={row[key]} onChange={(e) => updateRow(index,key,e.target.value)} /></td>)}<td><select className="w-full rounded border p-2" value={row.floorId || common.defaultFloorId} onChange={(e) => updateRow(index,'floorId',e.target.value)}><option value="">Auto / choose if ambiguous</option>{floors.map((f) => <option value={f.floorId} key={f.floorId}>Floor {f.floorNumber}</option>)}</select></td><td><input className="w-full rounded border p-2" type="date" value={row.startDate} onChange={(e) => updateRow(index,'startDate',e.target.value)} /></td><td><input className="w-full rounded border p-2" type="date" value={row.endDate} onChange={(e) => updateRow(index,'endDate',e.target.value)} /></td><td className="p-2"><button className="text-red-600 disabled:text-slate-300" disabled={rows.length === 1} onClick={() => setRows((old) => old.filter((_, i) => i !== index))}>Remove</button></td></tr>)}</tbody></table></div><div className="flex gap-2"><Button label="Back" variant="secondary" onClick={() => setStep(1)} /><Button label="Review batch" onClick={continueToReview} /></div></>}
    {step === 3 && <><div className="rounded-xl bg-slate-50 p-4 text-sm"><p><b>Hostel:</b> {hostels.find((h) => h.hostelId === common.hostelId)?.hostelName}</p><p><b>Plan:</b> {selectedPlan?.planName}</p><p><b>Agreement type:</b> Room Agreement</p><p><b>To create:</b> {rows.length} accounts, {rows.length} agreements, and {rows.length} payment plans.</p></div><div className="overflow-x-auto"><table className="w-full text-sm"><thead><tr className="border-b text-left"><th className="p-2">Name</th><th>Phone</th><th>Room</th><th>Floor</th><th>Dates</th></tr></thead><tbody>{rows.map((r,i) => <tr className="border-b" key={i}><td className="p-2">{r.name}</td><td>{r.phoneNumber}</td><td>{r.roomNumber}</td><td>{floors.find((f) => f.floorId === (r.floorId || common.defaultFloorId))?.floorNumber || 'Auto'}</td><td>{r.startDate} — {r.endDate}</td></tr>)}</tbody></table></div><div className="flex gap-2"><Button label="Back" variant="secondary" onClick={() => setStep(2)} /><Button label={`Confirm & Onboard ${rows.length} Tenants`} disabled={loading} onClick={submit} /></div></>}
  </CardContent></Card>
}
function Select({ label, value, onChange, options, required }) { return <label className="space-y-1 text-sm font-medium text-slate-700"><span>{label}{required ? ' *' : ''}</span><select className="block w-full rounded-lg border p-2 font-normal" value={value} onChange={(e) => onChange(e.target.value)}><option value="">Select {label.toLowerCase()}</option>{options.map(([id, name]) => <option value={id} key={id}>{name}</option>)}</select></label> }
function toDate(value) { if (!value) return ''; if (value instanceof Date) return value.toISOString().slice(0, 10); const parsed = new Date(value); return Number.isNaN(parsed.valueOf()) ? String(value).slice(0, 10) : parsed.toISOString().slice(0, 10) }
