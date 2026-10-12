import { useMemo, useState } from 'react';
import { useBank } from '../context/BankContext.jsx';
import TransactionTable from '../components/TransactionTable.jsx';

const TYPES = ['ALL', 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER_DEBIT', 'TRANSFER_CREDIT'];

export default function TransactionsPage() {
  const { transactions, openModal, loading } = useBank();
  const [query, setQuery] = useState('');
  const [type, setType] = useState('ALL');

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return transactions.filter((t) => {
      if (type !== 'ALL' && t.type !== type) return false;
      if (!q) return true;
      return [t.referenceNo, t.accountNumber, t.description].some((v) => String(v || '').toLowerCase().includes(q));
    });
  }, [transactions, query, type]);

  return (
    <section>
      <div className="page-header">
        <div className="page-title">
          <h1>Transaction Statements &amp; Audit Logs</h1>
          <p>Complete ledger of deposits, withdrawals, and wire transfers</p>
        </div>
        <div className="actions-bar" style={{ marginBottom: 0 }}>
          <button className="btn btn-success" onClick={() => openModal('deposit')}><i className="bi bi-arrow-down-circle" /> Deposit</button>
          <button className="btn btn-danger" onClick={() => openModal('withdraw')}><i className="bi bi-arrow-up-circle" /> Withdraw</button>
          <button className="btn btn-primary" onClick={() => openModal('transfer')}><i className="bi bi-send" /> Transfer</button>
        </div>
      </div>

      <div className="filter-bar">
        <input
          className="form-control"
          placeholder="Search by reference, account or description…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select className="form-select" value={type} onChange={(e) => setType(e.target.value)}>
          {TYPES.map((t) => <option key={t} value={t}>{t === 'ALL' ? 'All types' : t}</option>)}
        </select>
      </div>

      <div className="card">
        <div className="card-body" style={{ padding: 0 }}>
          <TransactionTable
            transactions={filtered}
            emptyText={loading ? 'Loading…' : 'No transactions match your filters'}
          />
        </div>
      </div>
    </section>
  );
}
