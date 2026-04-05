import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import agreementService from "../../../services/agreementService";
import Button from "../../../components/Button";

export default function AgreementList() {
  const navigate = useNavigate();
  const [agreements, setAgreements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchAgreements();
  }, []);

  const fetchAgreements = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await agreementService.getAllAgreements();
      setAgreements(res.data || []);
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || "Failed to load agreements");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "ACTIVE":
        return "bg-green-100 text-green-800";
      case "PENDING_TENANT_ACTION":
        return "bg-yellow-100 text-yellow-800";
      case "REJECTED":
        return "bg-red-100 text-red-800";
      case "CLOSED":
        return "bg-gray-100 text-gray-800";
      default:
        return "bg-blue-100 text-blue-800";
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return <div className="text-center py-8">Loading agreements...</div>;
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Agreements</h2>
        <Button
          label="+ Create New Agreement"
          onClick={() => navigate("/owner/agreements/create")}
          variant="primary"
        />
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 p-3 rounded">
          {error}
        </div>
      )}

      {agreements.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          No agreements found. Create your first agreement to get started.
        </div>
      ) : (
        <div className="grid gap-4">
          {agreements.map((agreement) => (
            <div
              key={agreement.id}
              className="bg-white rounded-lg shadow p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex justify-between items-start mb-3">
                <div>
                  <h3 className="font-semibold text-lg">
                    {agreement.type} Agreement
                  </h3>
                  <p className="text-sm text-gray-600">ID: {agreement.id}</p>
                </div>
                <span
                  className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(
                    agreement.status
                  )}`}
                >
                  {agreement.status.replace("_", " ")}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-sm mb-3">
                <div>
                  <span className="font-semibold">Rent:</span> ₹{agreement.rent}
                </div>
                {agreement.deposit && (
                  <div>
                    <span className="font-semibold">Deposit:</span> ₹
                    {agreement.deposit}
                  </div>
                )}
                <div>
                  <span className="font-semibold">Start Date:</span>{" "}
                  {formatDate(agreement.startDate)}
                </div>
                <div>
                  <span className="font-semibold">Created:</span>{" "}
                  {formatDate(agreement.createdAt)}
                </div>
                {agreement.activatedAt && (
                  <div>
                    <span className="font-semibold">Activated:</span>{" "}
                    {formatDate(agreement.activatedAt)}
                  </div>
                )}
              </div>

              {agreement.status === "PENDING_TENANT_ACTION" && (
                <div className="mt-3 pt-3 border-t">
                  <p className="text-xs text-gray-600 mb-2">
                    QR Token: {agreement.qrToken}
                  </p>
                  <p className="text-xs text-gray-500">
                    Expires: {formatDate(agreement.qrExpiry)}
                  </p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

