import { Routes, Route, useNavigate } from "react-router-dom";
import AgreementStepper from "./AgreementStepper";
import AgreementList from "./AgreementList";
import Button from "../../../components/Button";

export default function AgreementsLayout() {
  const navigate = useNavigate();

  return (
    <div className="p-4 min-h-screen bg-gray-50">
      <div className="mb-6 flex gap-2">
        <Button
          label="View All Agreements"
          onClick={() => navigate("/owner/agreements")}
          variant="secondary"
        />
      </div>

      <Routes>
        <Route path="/" element={<AgreementList />} />
        <Route path="/create" element={<AgreementStepper />} />
      </Routes>
    </div>
  );
}
