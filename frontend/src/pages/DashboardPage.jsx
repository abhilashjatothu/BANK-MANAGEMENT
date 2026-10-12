import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useBank } from '../context/BankContext.jsx';
import StatCard from '../components/StatCard.jsx';
import TransactionTable from '../components/TransactionTable.jsx';
import { formatMoney } from '../utils.js';

export default function DashboardPage() {
  const { user } = useAuth();
  const { stats, transactions, openModal, loading } = useBank();
  const isCustomer = user.role === 'CUSTOMER';
  const s = stats || {};

  return (
    <section>
      <div className="page-header">
        <div className="page-title">
          <h1>{isCustomer ? 'My Dashboard' : 'Financial Dashboard'}</h1>
          <p>Real-time capital reserves, operational metrics, and banking workflows</p>
        </div>
      </div>

      <div className="actions-bar">
        <button className="btn btn-primary" onClick={() => openModal('account')}><i className="bi bi-plus-circle" /> Open Account</button>
        {!isCustomer && (
          <button className="btn btn-outline" onClick={() => openModal('customer')}><i className="bi bi-person-plus" /> New Customer</button>
        )}
        <button className="btn btn-success" onClick={() => openModal('deposit')}><i className="bi bi-arrow-down-circle" /> Deposit Cash</button>
        <button className="btn btn-danger" onClick={() => openModal('withdraw')}><i className="bi bi-arrow-up-circle" /> Withdraw Cash</button>
        <button className="btn btn-primary" onClick={() => openModal('transfer')}><i className="bi bi-send" /> Inter-Account Transfer</button>
      </div>

      <div className="stats-grid">
        {!isCustomer && (
          <StatCard label="Total Customers" value={s.totalCustomers ?? 0} icon="bi-people-fill" bg="#eff6ff" color="#2563eb" />
        )}
        <StatCard label={isCustomer ? 'My Accounts' : 'Total Accounts'} value={s.totalAccounts ?? 0} icon="bi-credit-card-2-front-fill" bg="#ecfdf5" color="#059669" />
        <StatCard label={isCustomer ? 'My Balance' : 'Total Vault Reserves'} value={formatMoney(s.totalBalance)} icon="bi-cash-stack" bg="#f0fdf4" color="#16a34a" />
        <StatCard label="Today's Transactions" value={formatMoney(s.todayTransactionVolume)} icon="bi-activity" bg="#fef3c7" color="#d97706" />
      </div>

      <div className="card">
        <div className="card-header">
          <h3><i className="bi bi-clock-history" style={{ color: 'var(--primary)' }} /> Recent Transaction Stream</h3>
          <Link to="/transactions" className="btn btn-outline" style={{ padding: '0.35rem 0.75rem', fontSize: '0.85rem' }}>View All</Link>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <TransactionTable compact transactions={transactions.slice(0, 5)} emptyText={loading ? 'Loading…' : 'No recent transactions'} />
        </div>
      </div>
    </section>
  );
}
