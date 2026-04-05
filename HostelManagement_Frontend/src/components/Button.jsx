const Button = ({ type = 'button', label, onClick, disabled = false, variant = 'primary', className = '', fullWidth = false }) => {
  const baseClasses = 'px-4 py-2 rounded-lg font-medium transition-colors focus:outline-none'
  
  const variants = {
    primary: 'bg-blue-500 text-white hover:bg-blue-600 disabled:bg-gray-400',
    secondary: 'bg-gray-200 text-gray-800 hover:bg-gray-300 disabled:bg-gray-100',
    danger: 'bg-red-500 text-white hover:bg-red-600 disabled:bg-gray-400',
    success: 'bg-green-500 text-white hover:bg-green-600 disabled:bg-gray-400',
  }

  const widthClass = fullWidth ? 'w-full' : ''

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`${baseClasses} ${variants[variant]} ${widthClass} ${className}`}
    >
      {label}
    </button>
  )
}

export default Button
