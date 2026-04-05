// Generates a valid password according to backend rules:
// - 8-20 characters
// - At least one uppercase letter
// - At least one lowercase letter
// - At least one digit
// - At least one special character (@#$%^&+=!)

export function generateValidPassword(length = 12) {
  const upper = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const lower = 'abcdefghijklmnopqrstuvwxyz';
  const digits = '0123456789';
  const special = '@#$%^&+=!';
  const all = upper + lower + digits + special;

  if (length < 8) length = 8;
  if (length > 20) length = 20;

  // Ensure at least one of each required type
  let password = [
    upper[Math.floor(Math.random() * upper.length)],
    lower[Math.floor(Math.random() * lower.length)],
    digits[Math.floor(Math.random() * digits.length)],
    special[Math.floor(Math.random() * special.length)],
  ];

  // Fill the rest
  for (let i = password.length; i < length; i++) {
    password.push(all[Math.floor(Math.random() * all.length)]);
  }

  // Shuffle
  for (let i = password.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [password[i], password[j]] = [password[j], password[i]];
  }

  return password.join('');
}
