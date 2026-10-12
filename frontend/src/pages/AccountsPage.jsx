import { useBank } from '../context/BankContext.jsx';
import { formatMoney, statusBadgeClass } from '../utils.js';

export default function AccountsPage() {
  const { accounts, openModal, loading } = useBank();
  const small = { flex: 1, padding: '0.4rem', fontSize: '0.82rem' };

  return (
    <section>
      <div className="page-header">
        <div className="page-title">
          <h1>Bank Accounts Directory</h1>
          <p>Active deposit and current accounts across customers</p>
        </div>
        <button className="btn btn-primary" onClick={() => openModal('account')}>
          <i className="bi bi-plus-circle" /> Open New Account
        </button>
      </div>

      <div className="accounts-grid">
        {accounts.length === 0 && (
          <div style={{ gridColumn: '1/-1', textAlign: 'center', color: '#94a3b8', padding: '3rem' }}>
            {loading ? 'Loading…' : 'No accounts on record.'}
          </div>
        )}
        {accounts.map((a) => (
          <div key={a.accountNumber} className={`account-card ${a.status === 'FROZEN' ? 'frozen' : a.status === 'CLOSED' ? 'closed' : ''}`}>
            <div className="account-top">
              <div>
                <span className="badge badge-type" style={{ marginBottom: '0.35rem' }}>{a.accountType}</span>
                <div className="account-num">{a.accountNumber}</div>
                <small style={{ color: '#64748b' }}>{a.customerName || 'Customer'}</small>
              </div>
              <span className={`badge ${statusBadgeClass(a.status)}`}>{a.status}</span>
            </div>

            <div className="account-balance-box">
              <div className="label">Available Balance</div>
              <div className="amount">{formatMoney(a.balance)}</div>
            </div>

            <div className="account-actions">
              <button className="btn btn-outline" style={small} onClick={() => openModal('deposit', { accountNumber: a.accountNumber })}>Deposit</button>
              <button className="btn btn-outline" style={small} onClick={() => openModal('withdraw', { accountNumber: a.accountNumber })}>Withdraw</button>
              <button className="btn btn-primary" style={small} onClick={() => openModal('transfer', { accountNumber: a.accountNumber })}>Transfer</button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
