import { useState } from 'react'
import UserSelectStep from './UserSelectStep'
import AgreementTypeStep from './AgreementTypeStep'
import AgreementFormStep from './AgreementFormStep'
import AgreementReviewStep from './AgreementReviewStep'
import { Card, CardContent } from '../../../components/ui'

const steps = ['Type', 'User', 'Details', 'Review']

export default function AgreementStepper() {
  const [step, setStep] = useState(0)
  const [formData, setFormData] = useState({})

  const nextStep = () => setStep((current) => current + 1)
  const prevStep = () => setStep((current) => current - 1)

  return (
    <Card className="mx-auto max-w-4xl">
      <CardContent className="space-y-8 p-6 sm:p-8">
        <div className="grid gap-3 sm:grid-cols-4">
          {steps.map((label, index) => {
            const isActive = step === index
            const isComplete = step > index

            return (
              <div
                key={label}
                className={`rounded-2xl border px-4 py-3 ${
                  isActive
                    ? 'border-sky-200 bg-sky-50'
                    : isComplete
                      ? 'border-emerald-200 bg-emerald-50'
                      : 'border-slate-200 bg-slate-50'
                }`}
              >
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Step {index + 1}</p>
                <p className={`mt-2 text-sm font-semibold ${isActive || isComplete ? 'text-slate-950' : 'text-slate-500'}`}>{label}</p>
              </div>
            )
          })}
        </div>

        {step === 0 ? <AgreementTypeStep nextStep={nextStep} prevStep={prevStep} setFormData={setFormData} /> : null}
        {step === 1 ? <UserSelectStep nextStep={nextStep} prevStep={prevStep} formData={formData} setFormData={setFormData} /> : null}
        {step === 2 ? <AgreementFormStep nextStep={nextStep} prevStep={prevStep} formData={formData} setFormData={setFormData} /> : null}
        {step === 3 ? <AgreementReviewStep prevStep={prevStep} formData={formData} /> : null}
      </CardContent>
    </Card>
  )
}
