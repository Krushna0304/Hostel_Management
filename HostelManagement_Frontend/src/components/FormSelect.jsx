import { useState } from 'react'

const FormSelect = ({ label, name, value, onChange, options, placeholder, required = false, error = '' }) => {
  const [isFocused, setIsFocused] = useState(false)

  return (
    <div className="mb-4">
      <label className="block text-sm font-medium text-gray-700 mb-1">
        {label}
        {required && <span className="text-red-500">*</span>}
      </label>
      <select
        name={name}
        value={value}
        onChange={onChange}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        className={`w-full px-4 py-2 border rounded-lg focus:outline-none transition-colors ${
          isFocused ? 'border-blue-500 ring-1 ring-blue-200' : 'border-gray-300'
        } ${error ? 'border-red-500 ring-1 ring-red-200' : ''}`}
      >
        <option value="">{placeholder || 'Select an option'}</option>
        {options?.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
    </div>
  )
}

export default FormSelect
