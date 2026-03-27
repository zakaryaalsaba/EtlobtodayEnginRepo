/**
 * Latin digits for monetary amounts (independent of UI locale / Arabic numerals).
 */
export function formatAmountLatin(amount) {
  return Number(amount ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

/**
 * Display token for currency. JOD is always the code "JOD", never localized symbols.
 */
export function getCurrencyDisplaySymbol(currencyCode) {
  const c = String(currencyCode || 'USD').toUpperCase();
  if (c === 'JOD') return 'JOD';
  const map = {
    USD: '$',
    EUR: '€',
    GBP: '£',
    SAR: 'SAR',
    AED: 'AED',
    EGP: 'EGP'
  };
  return map[c] || c;
}

/**
 * @param {string} symbolPosition - 'before' | 'after'
 */
export function formatRestaurantMoney(amount, currencyCode, symbolPosition = 'before') {
  const num = formatAmountLatin(amount);
  const sym = getCurrencyDisplaySymbol(currencyCode);
  const after = String(symbolPosition || 'before').toLowerCase() === 'after';
  const multi = sym.length > 1;
  if (after) {
    return multi ? `${num} ${sym}` : `${num}${sym}`;
  }
  return multi ? `${sym} ${num}` : `${sym}${num}`;
}
