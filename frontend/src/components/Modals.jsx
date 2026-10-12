import { useEffect, useState } from 'react';
import { bankApi } from '../services/bankApi.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useBank } from '../context/BankContext.jsx';
import { formatMoney } from '../utils.js';

function ModalShell({ title, icon, color, children, onClose, onSubmit, submitLabel, submitClass = 'btn-primary', busy }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="modal-overlay active" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-content" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h3 style={color ? { color } : undefined}>
            <i className={`bi ${icon}`} /> {title}
          </h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close">&times;</button>
        </div>
        <form onSubmit={onSubmit}>
          <div className="modal-body">{children}</div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={onClose}>Cancel</button>
            <button type="submit" className={`btn ${submitClass}`} disabled={busy}>
              {busy ? 'Please wait…' : submitLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div className="form-group">
      <label>{label}</label>
      {children}
    </div>
  );
}

// Shared submit handler: guards double-submits and delegates to BankContext.perform.
function useSubmit(action, successMessage) {
  const { perform } = useBank();
  const [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    await perform(action, successMessage);
    setBusy(false);
  };
  return [busy, submit];
}

/* ---------------- New Customer ---------------- */
function CustomerModal({ onClose }) {
  const [f, setF] = useState({ name: '', email: '', phone: '', address: '', username: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const [busy, submit] = useSubmit(() => bankApi.createCustomer(f), 'Customer created successfully!');

  return (
    <ModalShell title="Register New Customer" icon="bi-person-plus" onClose={onClose} onSubmit={submit} submitLabel="Create Customer" busy={busy}>
      <Field label="Full Name *"><input className="form-control" value={f.name} onChange={set('name')} placeholder="e.g. Robert Johnson" required /></Field>
      <Field label="Email Address *"><input type="email" className="form-control" value={f.email} onChange={set('email')} placeholder="e.g. robert.j@example.com" required /></Field>
      <Field label="Phone Number *"><input className="form-control" value={f.phone} onChange={set('phone')} placeholder="e.g. +1-555-0811" required /></Field>
      <Field label="Physical Address"><input className="form-control" value={f.address} onChange={set('address')} placeholder="Street, City, State" /></Field>
      <Field label="Web Portal Username (Optional)"><input className="form-control" value={f.username} onChange={set('username')} placeholder="e.g. rjohnson" /></Field>
    </ModalShell>
  );
}

/* ---------------- Open Account ---------------- */
function AccountModal({ onClose, customerId }) {
  const { user } = useAuth();
  const { customers } = useBank();
  const options = user.role === 'CUSTOMER'
    ? customers.filter((c) => String(c.customerId) === String(user.customerId))
    : customers;

  const [f, setF] = useState({
    customerId: String(customerId ?? options[0]?.customerId ?? ''),
    accountType: 'SAVINGS',
    initialDeposit: '0.00',
  });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const [busy, submit] = useSubmit(
    () => bankApi.openAccount({ ...f, initialDeposit: parseFloat(f.initialDeposit) || 0, status: 'ACTIVE' }),
    (acc) => `Account ${acc.accountNumber} opened successfully!`
  );

  return (
    <ModalShell title="Open New Bank Account" icon="bi-plus-circle" onClose={onClose} onSubmit={submit} submitLabel="Open Account" busy={busy}>
      <Field label="Select Customer Account Holder *">
        <select className="form-select" value={f.customerId} onChange={set('customerId')} required>
          {options.length === 0 && <option value="">No customers available</option>}
          {options.map((c) => <option key={c.customerId} value={c.customerId}>{c.name} ({c.email})</option>)}
        </select>
      </Field>
      <Field label="Account Type *">
        <select className="form-select" value={f.accountType} onChange={set('accountType')}>
          <option value="SAVINGS">SAVINGS Account</option>
          <option value="CURRENT">CURRENT Account</option>
        </select>
      </Field>
      <Field label="Initial Opening Deposit ($)">
        <input type="number" step="0.01" min="0" className="form-control" value={f.initialDeposit} onChange={set('initialDeposit')} />
      </Field>
    </ModalShell>
  );
}

/* ---------------- Deposit / Withdraw ---------------- */
function CashModal({ onClose, accountNumber, kind }) {
  const { accounts } = useBank();
  const isDeposit = kind === 'deposit';
  const [f, setF] = useState({ accountNumber: accountNumber || accounts[0]?.accountNumber || '', amount: '', note: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const [busy, submit] = useSubmit(
    () => {
      const payload = { accountNumber: f.accountNumber, amount: parseFloat(f.amount), description: f.note };
      return isDeposit ? bankApi.deposit(payload) : bankApi.withdraw(payload);
    },
    (res) => `${isDeposit ? 'Deposited' : 'Withdrew'} ${formatMoney(res.amount)} successfully!`
  );

  return (
    <ModalShell
      title={isDeposit ? 'Cash Deposit' : 'Cash Withdrawal'}
      icon={isDeposit ? 'bi-arrow-down-circle' : 'bi-arrow-up-circle'}
      color={isDeposit ? 'var(--accent)' : 'var(--danger)'}
      onClose={onClose}
      onSubmit={submit}
      submitLabel={isDeposit ? 'Confirm Deposit' : 'Confirm Withdrawal'}
      submitClass={isDeposit ? 'btn-success' : 'btn-danger'}
      busy={busy}
    >
      <Field label={isDeposit ? 'Target Account *' : 'Source Account *'}>
        <AccountSelect accounts={accounts} value={f.accountNumber} onChange={set('accountNumber')} />
      </Field>
      <Field label={`${isDeposit ? 'Deposit' : 'Withdrawal'} Amount ($) *`}>
        <input type="number" step="0.01" min="0.01" className="form-control" value={f.amount} onChange={set('amount')} placeholder="0.00" required />
      </Field>
      <Field label="Transaction Memo / Note">
        <input className="form-control" value={f.note} onChange={set('note')} placeholder={isDeposit ? 'Branch cash deposit' : 'ATM / Teller withdrawal'} />
      </Field>
    </ModalShell>
  );
}

function AccountSelect({ accounts, value, onChange }) {
  return (
    <select className="form-select" value={value} onChange={onChange} required>
      {accounts.length === 0 && <option value="">No accounts available</option>}
      {accounts.map((a) => (
        <option key={a.accountNumber} value={a.accountNumber}>
          {a.accountNumber} ({a.accountType} - {formatMoney(a.balance)})
        </option>
      ))}
    </select>
  );
}

/* ---------------- Transfer ---------------- */
function TransferModal({ onClose, accountNumber }) {
  const { accounts } = useBank();
  const [f, setF] = useState({ from: accountNumber || accounts[0]?.accountNumber || '', to: '', amount: '', note: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const [busy, submit] = useSubmit(
    () => bankApi.transfer({
      fromAccountNumber: f.from,
      toAccountNumber: f.to.trim(),
      amount: parseFloat(f.amount),
      description: f.note,
    }),
    (res) => `Transferred ${formatMoney(res.amount)}. Ref: ${res.referenceNo}`
  );

  return (
    <ModalShell title="Inter-Account Transfer" icon="bi-send" color="var(--primary)" onClose={onClose} onSubmit={submit} submitLabel="Execute Transfer" busy={busy}>
      <Field label="Source Account *"><AccountSelect accounts={accounts} value={f.from} onChange={set('from')} /></Field>
      <Field label="Destination 12-Digit Account Number *">
        <input className="form-control" value={f.to} onChange={set('to')} placeholder="102030405060" required />
      </Field>
      <Field label="Transfer Amount ($) *">
        <input type="number" step="0.01" min="0.01" className="form-control" value={f.amount} onChange={set('amount')} placeholder="0.00" required />
      </Field>
      <Field label="Transfer Memo">
        <input className="form-control" value={f.note} onChange={set('note')} placeholder="Wire transfer / Bill payment" />
      </Field>
    </ModalShell>
  );
}

/* ---------------- Root: renders whichever modal is open ---------------- */
export default function ModalRoot() {
  const { modal, closeModal } = useBank();
  if (!modal) return null;
  // `key` remounts the form so state (and default account selection) resets per open.
  const key = `${modal.name}-${modal.accountNumber ?? ''}-${modal.customerId ?? ''}`;
  switch (modal.name) {
    case 'customer': return <CustomerModal key={key} onClose={closeModal} />;
    case 'account': return <AccountModal key={key} onClose={closeModal} customerId={modal.customerId} />;
    case 'deposit': return <CashModal key={key} kind="deposit" onClose={closeModal} accountNumber={modal.accountNumber} />;
    case 'withdraw': return <CashModal key={key} kind="withdraw" onClose={closeModal} accountNumber={modal.accountNumber} />;
    case 'transfer': return <TransferModal key={key} onClose={closeModal} accountNumber={modal.accountNumber} />;
    default: return null;
  }
}
