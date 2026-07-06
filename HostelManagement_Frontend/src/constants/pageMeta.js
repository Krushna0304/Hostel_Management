import { ownerNavigation, tenantNavigation } from './navigation'

/** More specific routes first — matched before sidebar nav fallback. */
const nestedRouteMeta = [
  {
    test: (path) => path === '/owner/hostels/create-hostel',
    title: 'Create Hostel',
    description: 'Set up a new property with a clear, structured intake flow.',
  },
  {
    test: (path) => /\/owner\/hostels\/[^/]+\/add-floor$/.test(path),
    title: 'Add Floor',
    description: 'Add a floor to structure rooms and bed inventory.',
  },
  {
    test: (path) => /\/owner\/hostels\/[^/]+\/floors\/[^/]+\/add-room$/.test(path),
    title: 'Add Room',
    description: 'Create a room record with beds, type, and availability.',
  },
  {
    test: (path) => /\/owner\/hostels\/[^/]+\/floors$/.test(path),
    title: 'Hostels',
    description: 'Manage floors and rooms for this property.',
  },
  {
    test: (path) => path.startsWith('/owner/agreements/create'),
    title: 'Create Agreement',
    description: 'Set up a new tenant onboarding agreement.',
  },
]

export function resolvePageMeta(pathname, userRole = 'OWNER') {
  const nested = nestedRouteMeta.find((route) => route.test(pathname))
  if (nested) {
    return { title: nested.title, description: nested.description }
  }

  const navigation = userRole === 'TENANT' ? tenantNavigation : ownerNavigation
  const match = navigation
    .filter(({ to }) => pathname === to || pathname.startsWith(`${to}/`))
    .sort((a, b) => b.to.length - a.to.length)[0]

  if (match) {
    return {
      title: match.label,
      description: match.description,
    }
  }

  return {
    title: 'Workspace',
    description: 'Manage your hostel operations.',
  }
}

export function getWorkspaceEyebrow(userRole) {
  return userRole === 'TENANT' ? 'Tenant portal' : 'Owner workspace'
}
