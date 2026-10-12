import { Outlet } from 'react-router-dom';
import Navbar from './Navbar.jsx';
import Toasts from './Toasts.jsx';
import ModalRoot from './Modals.jsx';

export default function Layout() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Outlet />
      </main>
      <footer>
        <div>&copy; 2026 Apex Bank Management System. All rights reserved.</div>
        <div>
          <i className="bi bi-shield-check" style={{ color: 'var(--accent)' }} /> Dual Mode: Spring Boot REST API &amp; Standalone Offline Storage
        </div>
      </footer>
      <ModalRoot />
      <Toasts />
    </>
  );
}
