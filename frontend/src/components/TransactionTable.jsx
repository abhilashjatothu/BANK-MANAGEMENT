import { formatDateTime, formatMoney, txnBadgeClass } from '../utils.js';

// `compact` is the dashboard variant (no description column).
export default function TransactionTable({ transactions, compact = false, emptyText = 'No transactions recorded' }) {
  const cols = compact ? 6 : 7;
  return (
    <div className="table-responsive">
      <table>
        <thead>
          <tr>
            <th>Reference</th>
            <th>Date &amp; Time</th>
            <th>Account Number</th>
            <th>{compact ? 'Operation Type' : 'Type'}</th>
            {!compact && <th>Description</th>}
            <th>Amount</th>
            <th>Balance After</th>
          </tr>
        </thead>
        <tbody>
          {transactions.length === 0 ? (
            <tr>
              <td colSpan={cols} style={{ textAlign: 'center', color: '#94a3b8', padding: '2rem' }}>{emptyText}</td>
            </tr>
          ) : (
            transactions.map((tx) => (
              <tr key={`${tx.transactionId}-${tx.type}`}>
                <td style={{ fontWeight: 600, color: 'var(--primary)' }}>{tx.referenceNo}</td>
                <td>{formatDateTime(tx.dateTime)}</td>
                <td><strong>{tx.accountNumber}</strong></td>
                <td><span className={`badge ${txnBadgeClass(tx.type)}`}>{tx.type}</span></td>
                {!compact && <td style={{ color: '#64748b' }}>{tx.description || '-'}</td>}
                <td style={{ fontWeight: 700 }}>{formatMoney(tx.amount)}</td>
                <td style={{ color: '#64748b' }}>{formatMoney(tx.balanceAfter)}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
