import { useBank } from '../context/BankContext.jsx';

export default function Toasts() {
  const { toasts } = useBank();
  return (
    <div className="toast-container">
      {toasts.map((t) => (
        <div key={t.id} className={`toast${t.isError ? ' error' : ''}`}>
          <span style={{ fontWeight: 'bold' }}>{t.isError ? 'Error:' : 'Success:'}</span> {t.message}
        </div>
      ))}
    </div>
  );
}
