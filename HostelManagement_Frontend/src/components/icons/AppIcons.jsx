const baseProps = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  viewBox: '0 0 24 24',
}

function Icon({ children, className = 'h-5 w-5' }) {
  return (
    <svg {...baseProps} className={className} aria-hidden="true">
      {children}
    </svg>
  )
}

export function HomeIcon(props) {
  return (
    <Icon {...props}>
      <path d="M3 10.75 12 3l9 7.75" />
      <path d="M5.75 9.5V21h12.5V9.5" />
      <path d="M9.5 21v-6.25h5V21" />
    </Icon>
  )
}

export function BuildingIcon(props) {
  return (
    <Icon {...props}>
      <path d="M4 21V5.5A1.5 1.5 0 0 1 5.5 4H14v17" />
      <path d="M14 8.5h4.5A1.5 1.5 0 0 1 20 10v11" />
      <path d="M8 8h2" />
      <path d="M8 12h2" />
      <path d="M8 16h2" />
      <path d="M14 21H2" />
      <path d="M16.5 13.5h1.5" />
      <path d="M16.5 17h1.5" />
    </Icon>
  )
}

export function ClipboardIcon(props) {
  return (
    <Icon {...props}>
      <rect x="6" y="4" width="12" height="17" rx="2" />
      <path d="M9 4.5h6a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1Z" />
      <path d="M9 10h6" />
      <path d="M9 14h6" />
    </Icon>
  )
}

export function LayersIcon(props) {
  return (
    <Icon {...props}>
      <path d="m12 3 9 4.5-9 4.5L3 7.5 12 3Z" />
      <path d="m3 12.5 9 4.5 9-4.5" />
      <path d="m3 17 9 4.5 9-4.5" />
    </Icon>
  )
}

export function DoorIcon(props) {
  return (
    <Icon {...props}>
      <path d="M6 21V5.5L16.5 3v18" />
      <path d="M6 21h12" />
      <path d="M12.75 12.5h.01" />
    </Icon>
  )
}

export function PlusIcon(props) {
  return (
    <Icon {...props}>
      <path d="M12 5v14" />
      <path d="M5 12h14" />
    </Icon>
  )
}

export function MenuIcon(props) {
  return (
    <Icon {...props}>
      <path d="M4 7h16" />
      <path d="M4 12h16" />
      <path d="M4 17h16" />
    </Icon>
  )
}

export function CloseIcon(props) {
  return (
    <Icon {...props}>
      <path d="m6 6 12 12" />
      <path d="m18 6-12 12" />
    </Icon>
  )
}

export function ArrowLeftIcon(props) {
  return (
    <Icon {...props}>
      <path d="M19 12H5" />
      <path d="m11 18-6-6 6-6" />
    </Icon>
  )
}

export function LogOutIcon(props) {
  return (
    <Icon {...props}>
      <path d="M10 21H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="m14 17 5-5-5-5" />
      <path d="M19 12h-9" />
    </Icon>
  )
}

export function SparkIcon(props) {
  return (
    <Icon {...props}>
      <path d="m12 3 1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9L12 3Z" />
      <path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8L19 16Z" />
      <path d="m5 14 .6 1.4L7 16l-1.4.6L5 18l-.6-1.4L3 16l1.4-.6L5 14Z" />
    </Icon>
  )
}

export function UsersIcon(props) {
  return (
    <Icon {...props}>
      <path d="M16 21v-1.5a3.5 3.5 0 0 0-3.5-3.5h-1A3.5 3.5 0 0 0 8 19.5V21" />
      <path d="M12 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
      <path d="M18.5 21v-1a3 3 0 0 0-2-2.82" />
      <path d="M17 6.2a2.75 2.75 0 0 1 0 5.6" />
      <path d="M5.5 21v-1a3 3 0 0 1 2-2.82" />
      <path d="M7 6.2a2.75 2.75 0 0 0 0 5.6" />
    </Icon>
  )
}

export function SpinnerIcon(props) {
  return (
    <svg className={props.className || 'h-4 w-4 animate-spin'} viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" className="opacity-20" fill="none" stroke="currentColor" strokeWidth="4" />
      <path d="M21 12a9 9 0 0 0-9-9" fill="none" stroke="currentColor" strokeWidth="4" strokeLinecap="round" />
    </svg>
  )
}

export function DocumentIcon(props) {
  return (
    <Icon {...props}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </Icon>
  )
}

export function CreditCardIcon(props) {
  return (
    <Icon {...props}>
      <rect x="3" y="6" width="18" height="12" rx="2" />
      <path d="M3 10h18" />
      <path d="M7 15h.01" />
      <path d="M11 15h2" />
    </Icon>
  )
}

export function ReceiptIcon(props) {
  return (
    <Icon {...props}>
      <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1-2-1Z" />
      <path d="M8 7h8" />
      <path d="M8 11h8" />
      <path d="M8 15h5" />
    </Icon>
  )
}

export function ProfileIcon(props) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
    </Icon>
  )
}

export function HistoryIcon(props) {
  return (
    <Icon {...props}>
      <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
      <path d="M3 3v5h5" />
      <path d="M12 7v5l4 2" />
    </Icon>
  )
}

export function BellIcon(props) {
  return (
    <Icon {...props}>
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </Icon>
  )
}
