import { useState, useEffect } from "react";
import authService from "../../../services/authService";
import { generateValidPassword } from "../../../utils/passwordUtils";
import FormInput from "../../../components/FormInput";
import Button from "../../../components/Button";

export default function UserSelectStep({ nextStep, prevStep, formData, setFormData }) {
  const [mode, setMode] = useState(null); // 'new' or 'existing'
  const [searchUsername, setSearchUsername] = useState("");
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  
  // Determine role based on agreement type
  const getRoleFromAgreementType = () => {
    if (formData.agreementType === "ROOM") {
      return "TENANT";
    } else if (formData.agreementType === "WORKER") {
      return "CLEANER";
    }
    return "TENANT"; // Default fallback
  };
  
  // New user form
  const [newUser, setNewUser] = useState({
    displayName: "",
    username: "",
    phoneNumber: "",
    role: getRoleFromAgreementType(),
  });

  // Update role when agreement type changes or when switching to new mode
  useEffect(() => {
    if (mode === "new" && formData.agreementType) {
      const role = getRoleFromAgreementType();
      setNewUser(prev => ({
        ...prev,
        role: role
      }));
    }
  }, [formData.agreementType, mode]);

  const handleSearchUser = async () => {
    if (!searchUsername.trim()) {
      setError("Please enter a username");
      return;
    }
    setLoading(true);
    setError("");
    try {
      // Get role based on agreement type
      const role = getRoleFromAgreementType();
      // Use the new endpoint that searches by username AND role
      const response = await authService.getUserByUsernameAndRole(searchUsername, role);
      setUser(response.data);
      setError("");
    } catch (err) {
      const errorData = err?.response?.data;
      setError(errorData?.message || "User not found with the specified role");
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async () => {
    setLoading(true);
    setError("");
    try {
      // Ensure role is set based on agreement type
      const role = getRoleFromAgreementType();
      const userData = {
        ...newUser,
        role: role, // Set role based on agreement type
        password: generateValidPassword(), // Valid password for backend
        isActive: false, // User is CREATED, not ACTIVE yet
      };
      await authService.register(userData);
      
      // Fetch the created user using the new endpoint with role
      const response = await authService.getUserByUsernameAndRole(newUser.username, role);
      setUser(response.data);
      setError("");
    } catch (err) {
      const errorData = err?.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || "Failed to create user")
      } else {
        // It's a general error message
        setError(errorData?.message || "Failed to create user")
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSelectUser = () => {
    if (user) {
      setFormData((prev) => ({ ...prev, userId: user.userId, userDisplayName: user.displayName }));
      nextStep();
    }
  };

  if (mode === null) {
    const agreementTypeLabel = formData.agreementType === "ROOM" ? "Room Agreement" : "Worker Agreement";
    const roleLabel = formData.agreementType === "ROOM" ? "TENANT" : "CLEANER";
    
    return (
      <div className="space-y-4">
        <h2 className="text-xl font-semibold mb-4">Select User</h2>
        <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
          <p className="text-sm text-gray-700">
            <span className="font-semibold">Agreement Type:</span> {agreementTypeLabel}
          </p>
          <p className="text-sm text-gray-700">
            <span className="font-semibold">User Role:</span> {roleLabel}
          </p>
        </div>
        <Button
          label="Create New User"
          onClick={() => setMode("new")}
          fullWidth
          variant="primary"
        />
        <Button
          label="Use Existing User"
          onClick={() => setMode("existing")}
          fullWidth
          variant="secondary"
        />
        <Button
          label="Back"
          onClick={prevStep}
          fullWidth
          variant="secondary"
        />
      </div>
    );
  }

  if (mode === "new") {
    return (
      <div className="space-y-4">
        <h2 className="text-xl font-semibold mb-4">Create New User</h2>
        {error && <div className="text-red-600 text-sm">{error}</div>}
        
        <FormInput
          label="Display Name"
          name="displayName"
          value={newUser.displayName}
          onChange={(e) => setNewUser({ ...newUser, displayName: e.target.value })}
          required
        />
        
        <FormInput
          label="Username"
          name="username"
          value={newUser.username}
          onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
          required
        />
        
        <FormInput
          label="Phone Number"
          name="phoneNumber"
          type="tel"
          value={newUser.phoneNumber}
          onChange={(e) => setNewUser({ ...newUser, phoneNumber: e.target.value })}
          required
        />
        
        <div className="mb-2 p-2 bg-gray-50 rounded text-sm">
          <p className="text-gray-600">
            Role will be set to: <span className="font-semibold">{getRoleFromAgreementType()}</span>
          </p>
        </div>
        
        <div className="flex gap-2">
          <Button
            label="Create User"
            onClick={handleCreateUser}
            disabled={loading}
            variant="primary"
          />
          <Button
            label="Back"
            onClick={() => setMode(null)}
            variant="secondary"
          />
        </div>
        
        {user && (
          <div className="mt-4 p-4 bg-green-50 rounded">
            <p className="font-semibold">User Created!</p>
            <p>{user.displayName} ({user.username})</p>
            <Button
              label="Continue"
              onClick={handleSelectUser}
              fullWidth
              variant="success"
              className="mt-2"
            />
          </div>
        )}
      </div>
    );
  }

  // Existing user mode
  const roleLabel = getRoleFromAgreementType();
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold mb-4">Search Existing User</h2>
      <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
        <p className="text-sm text-gray-700">
          <span className="font-semibold">Searching for:</span> {roleLabel} role
        </p>
      </div>
      {error && <div className="text-red-600 text-sm">{error}</div>}
      
      <div className="flex gap-2">
        <FormInput
          label="Username"
          name="searchUsername"
          value={searchUsername}
          onChange={(e) => setSearchUsername(e.target.value)}
          placeholder="Enter username"
          className="flex-1"
        />
        <Button
          label="Search"
          onClick={handleSearchUser}
          disabled={loading}
          variant="primary"
        />
      </div>
      
      {user && (
        <div className="mt-4 p-4 bg-blue-50 rounded">
          <p className="font-semibold">User Found:</p>
          <p>{user.displayName} ({user.username})</p>
          <p className="text-sm text-gray-600">Role: {user.role}</p>
          <Button
            label="Select User"
            onClick={handleSelectUser}
            fullWidth
            variant="success"
            className="mt-2"
          />
        </div>
      )}
      
      <Button
        label="Back"
        onClick={() => {
          setMode(null);
          setUser(null);
          setSearchUsername("");
        }}
        variant="secondary"
        fullWidth
      />
    </div>
  );
}
