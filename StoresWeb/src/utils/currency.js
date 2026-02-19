const SYMBOLS = {
  USD: '$',
  EUR: '€',
  GBP: '£',
  JOD: 'JOD',
  SAR: 'SAR',
  AED: 'AED',
  EGP: 'EGP'
}

export function formatPrice(amount, currencyCode = 'USD', symbolPosition = 'before') {
  const n = Number(amount)
  const value = typeof n === 'number' && !Number.isNaN(n) ? n.toFixed(2) : '0.00'
  const symbol = SYMBOLS[currencyCode] || currencyCode || 'USD'
  return symbolPosition === 'after' ? `${value} ${symbol}` : `${symbol} ${value}`
}
