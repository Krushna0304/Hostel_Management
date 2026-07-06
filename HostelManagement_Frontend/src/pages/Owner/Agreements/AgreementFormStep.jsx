import { useState, useEffect, useRef } from "react";
import FormInput from "../../../components/FormInput";
import Button from "../../../components/Button";
import { hostelService, floorService, roomService } from "../../../services/hostelService";
import agreementService from "../../../services/agreementService";

export default function AgreementFormStep({ nextStep, prevStep, formData, setFormData }) {
  const [hostels, setHostels] = useState([]);
  const [floors, setFloors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [plans, setPlans] = useState([]);
  const [selectedHostelId, setSelectedHostelId] = useState(formData.hostelId || "");
  const [selectedFloorId, setSelectedFloorId] = useState(formData.floorId || "");
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});

  // Co-tenant state
  const [coTenantInput, setCoTenantInput] = useState("");
  const [coTenantError, setCoTenantError] = useState("");

  const [agreementData, setAgreementData] = useState({
    roomId: formData.roomId || "",
    planId: formData.planId || "",
    startDate: formData.startDate || "",
    endDate: formData.endDate || "",
    mayExtend: Boolean(formData.mayExtend),
    coTenantNames: formData.coTenantNames || [],
  });

  const isFlat = formData.agreementType === "FLAT";
  const isRoom = formData.agreementType === "ROOM";

  // Track whether the hostel/floor effects are running for the first time (edit-mode restore).
  // On first run we only fetch data without resetting the already-restored selections.
  const hostelInitialized = useRef(false);
  const floorInitialized = useRef(false);

  // Derive the roomType param for fetching
  const getRoomTypeParam = () => {
    if (isFlat) return "FLAT";
    if (isRoom) return "PG_ROOM";
    return undefined;
  };

  // Derive the planType param for fetching
  const getPlanTypeParam = () => {
    if (isFlat) return "FLAT";
    if (isRoom) return "PG_ROOM";
    return undefined;
  };

  useEffect(() => {
    fetchHostels();
    fetchPlans();
  }, []);

  // Re-fetch plans when agreementType changes
  useEffect(() => {
    fetchPlans();
  }, [formData.agreementType]);

  useEffect(() => {
    if (selectedHostelId) {
      if (hostelInitialized.current) {
        // User changed hostel — reset dependent selections
        setFloors([]);
        setRooms([]);
        setSelectedFloorId("");
        setAgreementData(prev => ({ ...prev, roomId: "" }));
      }
      hostelInitialized.current = true;
      fetchFloors(selectedHostelId);
    }
  }, [selectedHostelId]);

  useEffect(() => {
    if (selectedFloorId && agreementData.startDate && agreementData.endDate) {
      if (floorInitialized.current) {
        // User changed floor or dates — reset room selection
        setAgreementData(prev => ({ ...prev, roomId: "" }));
      }
      floorInitialized.current = true;
      fetchRooms(selectedFloorId, agreementData.startDate, agreementData.endDate);
    } else if (floorInitialized.current) {
      setRooms([]);
    }
  }, [selectedFloorId, agreementData.startDate, agreementData.endDate, formData.agreementType]);

  useEffect(() => {
    if (agreementData.planId) {
      const plan = plans.find(p => p.id === agreementData.planId);
      setSelectedPlan(plan || null);

      // For NOT_FIXED plans: auto-compute end date from start + minimumStayMonths
      if (plan?.duration?.durationType === 'NOT_FIXED' && agreementData.startDate) {
        const minStay = plan.duration?.minimumStayMonths || 1;
        const start = new Date(agreementData.startDate);
        start.setMonth(start.getMonth() + minStay);
        const autoEnd = start.toISOString().split('T')[0];
        setAgreementData(prev => ({ ...prev, endDate: autoEnd, mayExtend: true }));
      }
    }
  }, [agreementData.planId, plans]);

  // Also auto-update end date when startDate changes for NOT_FIXED plans
  useEffect(() => {
    if (selectedPlan?.duration?.durationType === 'NOT_FIXED' && agreementData.startDate) {
      const minStay = selectedPlan.duration?.minimumStayMonths || 1;
      const start = new Date(agreementData.startDate);
      start.setMonth(start.getMonth() + minStay);
      const autoEnd = start.toISOString().split('T')[0];
      setAgreementData(prev => ({ ...prev, endDate: autoEnd, mayExtend: true }));
    }
  }, [agreementData.startDate, selectedPlan]);

  const fetchHostels = async () => {
    try {
      const res = await hostelService.getAllHostels();
      setHostels(res.data || []);
    } catch (err) {
      console.error("Failed to fetch hostels", err);
    }
  };

  const fetchFloors = async (hostelId) => {
    try {
      const res = await floorService.getFloorsByHostel(hostelId);
      setFloors(res.data || []);
    } catch (err) {
      console.error("Failed to fetch floors", err);
    }
  };

  const fetchRooms = async (floorId, startDate, endDate) => {
    try {
      const roomType = getRoomTypeParam();
      const res = await roomService.getAvailableRooms(floorId, startDate, endDate, roomType);
      const availableRooms = (res.data || []).map((room) => ({
        ...room,
        roomNumber: room.roomName || room.roomNumber,
      }));
      setRooms(availableRooms);
    } catch (err) {
      console.error("Failed to fetch available rooms", err);
      setRooms([]);
    }
  };

  const fetchPlans = async () => {
    try {
      const planType = getPlanTypeParam();
      const res = await agreementService.getActivePlans(planType);
      setPlans(res.data || []);
    } catch (err) {
      console.error("Failed to fetch plans", err);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // If room is changing, clear co-tenants since the bed limit might be different
    if (name === 'roomId' && isFlat && value !== agreementData.roomId) {
      setAgreementData((prev) => ({
        ...prev,
        [name]: value,
        coTenantNames: [], // Clear co-tenants when room changes
      }));
    } else {
      setAgreementData((prev) => ({
        ...prev,
        [name]: value,
      }));
    }
    
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  // Co-tenant handlers
  const handleAddCoTenant = () => {
    const trimmed = coTenantInput.trim();
    if (!trimmed) return;
    if (trimmed.length > 100) {
      setCoTenantError("Co-tenant name must not exceed 100 characters");
      return;
    }

    // Get the selected room to check available beds
    const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);
    const maxCoTenants = selectedRoom ? selectedRoom.availableBeds - 1 : 5; // Default to 5 if no room selected

    if (agreementData.coTenantNames.length >= maxCoTenants) {
      setCoTenantError(`Maximum ${maxCoTenants} co-tenants allowed for this room (${selectedRoom?.availableBeds} beds available, 1 for primary tenant)`);
      return;
    }

    setAgreementData(prev => ({
      ...prev,
      coTenantNames: [...prev.coTenantNames, trimmed],
    }));
    setCoTenantInput("");
    setCoTenantError("");
  };

  const handleRemoveCoTenant = (index) => {
    setAgreementData(prev => ({
      ...prev,
      coTenantNames: prev.coTenantNames.filter((_, i) => i !== index),
    }));
  };

  const handleCoTenantInputChange = (e) => {
    const val = e.target.value;
    setCoTenantInput(val);
    if (val.length > 100) {
      setCoTenantError("Co-tenant name must not exceed 100 characters");
    } else {
      setCoTenantError("");
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!agreementData.roomId) newErrors.roomId = "Room is required";
    if (!agreementData.planId) newErrors.planId = "Plan is required";
    if (!agreementData.startDate) newErrors.startDate = "Start date is required";
    if (!agreementData.endDate && selectedPlan?.duration?.durationType !== 'NOT_FIXED') {
      newErrors.endDate = "Expected end date is required";
    } else if (agreementData.endDate && agreementData.startDate && selectedPlan) {
      const start = new Date(agreementData.startDate);
      const end = new Date(agreementData.endDate);
      const isNotFixed = selectedPlan.duration?.durationType === 'NOT_FIXED';
      const requiredMonths = isNotFixed
        ? (selectedPlan.duration?.minimumStayMonths || 1)
        : (selectedPlan.duration?.value || 0);
      const minEnd = new Date(start);
      minEnd.setMonth(minEnd.getMonth() + requiredMonths);
      if (end < minEnd) {
        const label = isNotFixed ? 'minimum stay' : 'plan duration';
        newErrors.endDate = `End date must be on or after ${minEnd.toLocaleDateString()} (start date + ${requiredMonths} month(s) ${label})`;
      }
    }
    return newErrors;
  };

  const handleNext = () => {
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    const selectedHostel = hostels.find(h => h.hostelId === selectedHostelId);
    const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);

    const update = {
      ...agreementData,
      hostelId: selectedHostelId,
      floorId: selectedFloorId,
      plan: selectedPlan,
      hostelNumber: selectedHostel ? (selectedHostel.hostelNumber || selectedHostel.hostelName || selectedHostel.name) : undefined,
      roomNumber: selectedRoom ? selectedRoom.roomNumber : undefined,
    };

    if (isFlat) {
      update.coTenantNames = agreementData.coTenantNames;
    }

    setFormData((prev) => ({
      ...prev,
      ...update,
    }));
    nextStep();
  };

  // Empty-state messages
  const plansEmptyMessage = isFlat
    ? "No Flat plans available. Please create a Flat plan first."
    : isRoom
    ? "No PG Room plans available. Please create a PG Room plan first."
    : null;

  const roomsEmptyMessage = isFlat
    ? "No Flat rooms available on this floor."
    : isRoom
    ? "No PG Rooms available on this floor."
    : null;

  const plansLoaded = plans.length === 0 && plansEmptyMessage;
  const roomsLoaded = rooms.length === 0 && roomsEmptyMessage && selectedHostelId && selectedFloorId && agreementData.startDate && agreementData.endDate;

  return (
    <div className="space-y-4 max-h-[600px] overflow-y-auto">
      <h2 className="text-xl font-semibold mb-4">Agreement Details</h2>

      {/* Hostel Selection */}
      <div className="space-y-2">
        <label className="block text-sm font-medium text-gray-700">
          Select Hostel <span className="text-red-500">*</span>
        </label>
        <select
          value={selectedHostelId}
          onChange={(e) => setSelectedHostelId(e.target.value)}
          className="w-full px-4 py-2 border rounded-lg"
        >
          <option value="">Select a hostel</option>
          {hostels.map((hostel) => (
            <option key={hostel.hostelId} value={hostel.hostelId}>
              {hostel.hostelName}
            </option>
          ))}
        </select>
      </div>

      {/* Floor Selection */}
      {selectedHostelId && (
        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">
            Select Floor <span className="text-red-500">*</span>
          </label>
          <select
            value={selectedFloorId}
            onChange={(e) => setSelectedFloorId(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg"
          >
            <option value="">Select a floor</option>
            {floors.map((floor) => (
              <option key={floor.floorId} value={floor.floorId}>
                Floor {floor.floorNumber}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Plan Selection — moved above dates so end date constraint can be derived */}
      <div className="space-y-2">
        <label className="block text-sm font-medium text-gray-700">
          Select Agreement Plan <span className="text-red-500">*</span>
        </label>
        {plansLoaded ? (
          <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            {plansEmptyMessage}
          </p>
        ) : (
          <select
            name="planId"
            value={agreementData.planId}
            onChange={handleChange}
            disabled={plansLoaded}
            className="w-full px-4 py-2 border rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
          >
            <option value="">Select a plan</option>
            {plans.map((plan) => (
              <option key={plan.id} value={plan.id}>
                {plan.planName} - ₹{plan.rentDetails?.monthlyRent}/month
                {plan.duration?.durationType === 'NOT_FIXED' ? ' · Monthly rolling' : plan.duration?.value ? ` · ${plan.duration.value} months` : ''}
              </option>
            ))}
          </select>
        )}
        {errors.planId && <p className="text-red-500 text-sm">{errors.planId}</p>}
      </div>

      <FormInput
        label="Start Date"
        name="startDate"
        type="date"
        value={agreementData.startDate}
        onChange={handleChange}
        min={new Date().toISOString().split('T')[0]}
        required
        error={errors.startDate}
      />

      {/* End Date — constrained by plan duration / minimum stay */}
      {(() => {
        const isNotFixed = selectedPlan?.duration?.durationType === 'NOT_FIXED'
        const requiredMonths = isNotFixed
          ? (selectedPlan?.duration?.minimumStayMonths || 1)
          : (selectedPlan?.duration?.value || 0)
        const minEndDate = (() => {
          if (!agreementData.startDate || !selectedPlan) return new Date().toISOString().split('T')[0]
          const d = new Date(agreementData.startDate)
          d.setMonth(d.getMonth() + requiredMonths)
          return d.toISOString().split('T')[0]
        })()

        if (isNotFixed) {
          return (
            <div className="rounded-lg border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-800">
              <p className="font-medium">Not Fixed Duration Plan</p>
              <p className="mt-1 text-xs text-sky-600">
                End date is auto-set to <strong>{agreementData.endDate || '—'}</strong> (start date + {requiredMonths} month{requiredMonths !== 1 ? 's' : ''} minimum stay). The agreement continues month-to-month beyond that.
              </p>
            </div>
          )
        }

        return (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="block text-sm font-medium text-gray-700">
                Expected End Date <span className="text-red-500">*</span>
                {selectedPlan && requiredMonths > 0 && (
                  <span className="ml-2 text-xs font-normal text-slate-400">
                    (min: start + {requiredMonths} month{requiredMonths !== 1 ? 's' : ''})
                  </span>
                )}
              </label>
              {isRoom && (
                <label className="inline-flex items-center gap-2 text-sm text-gray-700 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    name="mayExtend"
                    checked={Boolean(agreementData.mayExtend)}
                    onChange={(e) => setAgreementData((prev) => ({ ...prev, mayExtend: e.target.checked }))}
                    className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                  />
                  May Extend
                </label>
              )}
            </div>
            <FormInput
              name="endDate"
              type="date"
              value={agreementData.endDate}
              onChange={handleChange}
              min={minEndDate}
              required
              error={errors.endDate}
            />
          </div>
        )
      })()}

      {/* Room Selection — requires floor + start + end date */}
      {selectedHostelId && selectedFloorId && agreementData.startDate && agreementData.endDate && (
        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">
            Select Room <span className="text-red-500">*</span>
          </label>
          {roomsLoaded ? (
            <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              {roomsEmptyMessage}
            </p>
          ) : (
            <select
              name="roomId"
              value={agreementData.roomId}
              onChange={handleChange}
              disabled={roomsLoaded}
              className="w-full px-4 py-2 border rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
            >
              <option value="">Select a room</option>
              {rooms.map((room) => (
                <option key={room.roomId} value={room.roomId}>
                  Room {room.roomNumber} ({room.availableBeds} bed{room.availableBeds === 1 ? '' : 's'} available)
                </option>
              ))}
            </select>
          )}
          {errors.roomId && <p className="text-red-500 text-sm">{errors.roomId}</p>}
        </div>
      )}

      {/* Plan Details Display */}
      {selectedPlan && (
        <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
          <h3 className="font-semibold text-lg mb-3 text-blue-900">{selectedPlan.planName}</h3>

          <div className="space-y-2 text-sm">
            <div>
              <span className="font-medium">Monthly Rent:</span> ₹{selectedPlan.rentDetails?.monthlyRent} {selectedPlan.rentDetails?.currency}
            </div>
            {selectedPlan.charges?.securityDeposit && (
              <div>
                <span className="font-medium">Security Deposit:</span> ₹{selectedPlan.charges.securityDeposit.amount}
                {selectedPlan.charges.securityDeposit.refundable && " (Refundable)"}
              </div>
            )}
            {selectedPlan.duration && (
              <div>
                <span className="font-medium">Duration:</span> {selectedPlan.duration.value} {selectedPlan.duration.unit}(s)
                {selectedPlan.duration.minimumStayMonths && ` (Minimum ${selectedPlan.duration.minimumStayMonths} months)`}
              </div>
            )}

            {selectedPlan.freeFacilities?.facilities && selectedPlan.freeFacilities.facilities.length > 0 && (
              <div className="mt-3">
                <span className="font-medium block mb-2">Free Facilities Included:</span>
                <div className="flex flex-wrap gap-2">
                  {selectedPlan.freeFacilities.facilities.map((facility, index) => (
                    <span
                      key={index}
                      className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs"
                      title={facility.description}
                    >
                      {facility.name}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Co-tenant Names Section — visible only for FLAT agreements */}
      {isFlat && (
        <div className="space-y-3 border border-gray-200 rounded-lg p-4 bg-gray-50">
          <h3 className="text-sm font-semibold text-gray-700">Co-tenants</h3>

          {/* Input row */}
          <div className="flex gap-2 items-start">
            <div className="flex-1">
              <input
                type="text"
                value={coTenantInput}
                onChange={handleCoTenantInputChange}
                placeholder="Enter co-tenant name"
                className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddCoTenant();
                  }
                }}
              />
              {coTenantError && (
                <p className="text-red-500 text-xs mt-1">{coTenantError}</p>
              )}
            </div>
            <button
              type="button"
              onClick={handleAddCoTenant}
              disabled={(() => {
                const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);
                const maxCoTenants = selectedRoom ? selectedRoom.availableBeds - 1 : 5;
                return agreementData.coTenantNames.length >= maxCoTenants || !coTenantInput.trim();
              })()}
              className="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed whitespace-nowrap"
            >
              Add co-tenant
            </button>
          </div>

          {/* Co-tenant list */}
          {agreementData.coTenantNames.length > 0 && (
            <ul className="space-y-1">
              {agreementData.coTenantNames.map((name, index) => (
                <li
                  key={index}
                  className="flex items-center justify-between bg-white border border-gray-200 rounded-lg px-3 py-2 text-sm"
                >
                  <span>{name}</span>
                  <button
                    type="button"
                    onClick={() => handleRemoveCoTenant(index)}
                    className="text-gray-400 hover:text-red-500 ml-2 font-bold leading-none"
                    aria-label={`Remove ${name}`}
                  >
                    ✕
                  </button>
                </li>
              ))}
            </ul>
          )}

          {/* Dynamic help text based on selected room */}
          {(() => {
            const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);
            if (selectedRoom) {
              const maxCoTenants = selectedRoom.availableBeds - 1;
              const currentCount = agreementData.coTenantNames.length;
              return (
                <div className="text-xs">
                  {currentCount === 0 ? (
                    <p className="text-gray-400">
                      No co-tenants added yet. You can add up to {maxCoTenants} co-tenants for this room 
                      ({selectedRoom.availableBeds} beds available, 1 reserved for primary tenant).
                    </p>
                  ) : (
                    <p className="text-gray-600">
                      {currentCount} of {maxCoTenants} co-tenants added 
                      ({selectedRoom.availableBeds} beds available, 1 for primary tenant).
                    </p>
                  )}
                </div>
              );
            } else {
              return (
                <p className="text-xs text-gray-400">
                  Select a room first to see the maximum number of co-tenants allowed.
                </p>
              );
            }
          })()}
        </div>
      )}

      <div className="flex gap-2">
        <Button
          label="Review"
          onClick={handleNext}
          fullWidth
          variant="primary"
          disabled={plansLoaded}
        />
        <Button
          label="Cancel"
          onClick={prevStep}
          fullWidth
          variant="secondary"
        />
      </div>
    </div>
  );
}
