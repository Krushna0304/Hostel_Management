# Hostel Management System - Frontend

A mobile-first React + Vite + Tailwind CSS frontend for the Hostel Management System.

## 📁 Project Structure

```
src/
├── components/              # Reusable UI components
│   ├── ProtectedRoute.jsx  # Role-based route protection
│   ├── FormInput.jsx       # Reusable input field
│   ├── FormSelect.jsx      # Reusable select dropdown
│   └── Button.jsx          # Reusable button component
├── pages/
│   ├── Auth/
│   │   ├── Login.jsx       # Login screen (mobile-first)
│   │   └── Register.jsx    # User registration screen
│   └── Owner/
│       ├── Dashboard.jsx   # Owner dashboard - lists hostels
│       ├── CreateHostel.jsx # Create new hostel
│       ├── AddFloor.jsx    # Add floor to hostel
│       └── AddRoom.jsx     # Add room to floor
├── services/
│   ├── apiClient.js        # Axios instance with JWT interceptors
│   ├── authService.js      # Authentication API calls
│   └── hostelService.js    # Hostel/Floor/Room API calls
├── App.jsx                 # Main app with routing
├── main.jsx                # React entry point
└── index.css               # Global styles + Tailwind
```

## 🚀 Setup & Installation

### Prerequisites
- Node.js 16+ and npm/yarn

### Steps

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Set environment variables** (optional)
   Create a `.env` file:
   ```
   REACT_APP_API_URL=http://localhost:8080/api
   ```

3. **Start development server**
   ```bash
   npm run dev
   ```
   Opens at `http://localhost:3000`

4. **Build for production**
   ```bash
   npm run build
   ```

## 🎨 Mobile-First Design

All components are designed mobile-first with responsive Tailwind classes:
- Base styles for mobile (320px+)
- `sm:` breakpoint for tablets (640px+)
- Optimal for touch interactions

## 🔐 Authentication

- JWT-based authentication
- Token stored in `localStorage` with key `authToken`
- Role stored in `localStorage` with key `userRole`
- Protected routes redirect to `/login` if unauthenticated
- Auto-logout on 401 response from API

## 📡 API Integration

All API calls use a centralized `apiClient` that:
- Automatically adds JWT token to request headers
- Handles 401 responses (unauthorized)
- Points to `http://localhost:8080/api` (configurable)

### API Endpoints Expected from Backend

#### Authentication
- `POST /auth/login` → `{ token, role }`
- `POST /auth/register` → Register user

#### Hostels
- `GET /hostels` → List all hostels
- `POST /hostels` → Create hostel
- `GET /hostels/:id` → Get hostel details
- `PUT /hostels/:id` → Update hostel
- `DELETE /hostels/:id` → Delete hostel

#### Floors
- `GET /hostels/:hostelId/floors` → List floors
- `POST /hostels/:hostelId/floors` → Create floor
- `GET /hostels/:hostelId/floors/:floorId` → Get floor details
- `PUT /hostels/:hostelId/floors/:floorId` → Update floor
- `DELETE /hostels/:hostelId/floors/:floorId` → Delete floor

#### Rooms
- `GET /hostels/:hostelId/floors/:floorId/rooms` → List rooms
- `POST /hostels/:hostelId/floors/:floorId/rooms` → Create room
- `GET /hostels/:hostelId/rooms/available` → Get available rooms

## 📋 TODOs & Backend Integration

Look for `TODO:` comments in the code for:
1. **Endpoint verification** - Confirm actual backend endpoint paths
2. **Response structure validation** - Check data format from API
3. **Field mapping** - Match frontend field names to backend models
4. **Error handling** - Customize error messages
5. **Feature additions**:
   - Floor listing and details view
   - Room listing and management
   - Tenant allocation to rooms
   - Agreement creation and signing
   - Payment tracking and transactions
   - OTP/QR verification for cash payments
   - Announcements system
   - Cleaner and tenant dashboards

## 🔄 Routing

- `/login` → Login screen
- `/register` → Registration screen
- `/owner/dashboard` → Owner dashboard (protected, OWNER role)
- `/owner/hostels/create-hostel` → Create hostel form (protected)
- `/owner/add-floor` → Add floor form (protected)
- `/owner/add-room` → Add room form (protected)

## 🎯 Roles

- **OWNER** - Full access to hostel, floor, room, and tenant management
- **TENANT** - View agreement, payments, announcements (Coming soon)
- **CLEANER** - View assignments and payments (Coming soon)

## 🛠️ Development

### Adding a New Page
1. Create a new folder in `src/pages/[Role]/`
2. Create component file
3. Add route in `App.jsx`
4. Import and use `ProtectedRoute` if needed

### Adding a New Component
1. Create file in `src/components/`
2. Export as default
3. Import in pages as needed

### Adding API Service
1. Create function in `src/services/hostelService.js` or similar
2. Use `apiClient` for requests
3. Add `TODO:` comments for endpoint verification

## 📦 Dependencies

- **react** - UI library
- **react-router-dom** - Client-side routing
- **axios** - HTTP client
- **tailwindcss** - Utility-first CSS framework
- **vite** - Build tool (fast HMR)

## 🚨 Important

1. Update `API_BASE_URL` in `apiClient.js` to match your backend URL
2. Verify all API endpoints with backend team before deployment
3. Ensure backend returns JWT token and role in login response
4. Test on mobile devices (use DevTools device emulation)
5. All form validations are client-side; add server-side validation too

## 📱 Testing Mobile UI

Use Chrome DevTools:
1. Press `F12` → Toggle Device Toolbar (Ctrl+Shift+M)
2. Select iPhone or Android preset
3. Test touch interactions

---

**Backend Location**: `../HostelManagment_Backend/` (Spring Boot)
**Ready to connect**: Yes, just verify API endpoints
