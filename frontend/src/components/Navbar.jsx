import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useBank } from '../context/BankContext.jsx';
import { ROLE_LABELS } from '../utils.js';

export default function Navbar() {
  const { user, logout } = useAuth();
  const { isLive } = useBank();
  const navigate = useNavigate();
  const isCustomer = user?.role === 'CUSTOMER';

  const links = [
    { to: '/', label: 'Dashboard', icon: 'bi-speedometer2', end: true },
    ...(isCustomer ? [] : [{ to: '/customers', label: 'Customers', icon: 'bi-people' }]),
    { to: '/accounts', label: 'Accounts', icon: 'bi-wallet2' },
    { to: '/transactions', label: 'Statements', icon: 'bi-clock-history' },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <nav className="navbar">
      <NavLink to="/" className="nav-brand">
        <i className="bi bi-bank2" />
        <span>Apex Bank</span>
      </NavLink>

      <ul className="nav-links">
        {links.map((l) => (
          <li key={l.to}>
            <NavLink to={l.to} end={l.end} className={({ isActive }) => `nav-btn${isActive ? ' active' : ''}`}>
              <i className={`bi ${l.icon}`} /> {l.label}
            </NavLink>
          </li>
        ))}
      </ul>

      <div className="nav-right">
        <span className={`api-status-badge ${isLive ? 'api-online' : 'api-mock'}`}>
          <span
            style={{
              display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
              background: isLive ? '#10b981' : '#f59e0b',
            }}
          />
          {isLive ? 'Connected to Spring Boot' : 'Standalone Offline Mode'}
        </span>
        <span className="role-selector" title={user?.username}>
          <i className="bi bi-person-circle" /> {user?.customerName || user?.username} · {ROLE_LABELS[user?.role] || user?.role}
        </span>
        <button className="btn btn-outline" style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }} onClick={handleLogout}>
          <i className="bi bi-box-arrow-right" /> Sign Out
        </button>
      </div>
    </nav>
  );
}
