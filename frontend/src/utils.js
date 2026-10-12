export function formatMoney(amount) {
  return '$' + Number(amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function formatDateTime(value) {
  const d = new Date(value);
  return isNaN(d) ? '-' : d.toLocaleString();
}

export function txnBadgeClass(type = '') {
  if (type === 'DEPOSIT') return 'badge-deposit';
  if (type === 'WITHDRAWAL') return 'badge-withdrawal';
  if (type.includes('TRANSFER')) return 'badge-transfer';
  return 'badge-type';
}

export function statusBadgeClass(status) {
  if (status === 'ACTIVE') return 'badge-active';
  if (status === 'FROZEN') return 'badge-frozen';
  return 'badge-closed';
}

export const ROLE_LABELS = {
  ADMIN: 'Administrator',
  EMPLOYEE: 'Branch Teller',
  CUSTOMER: 'Customer',
};
