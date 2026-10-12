import { useBank } from '../context/BankContext.jsx';

export default function CustomersPage() {
  const { customers, openModal, loading } = useBank();

  return (
    <section>
      <div className="page-header">
        <div className="page-title">
          <h1>Customer Management</h1>
          <p>Onboard customers and manage contact profiles</p>
        </div>
        <button className="btn btn-primary" onClick={() => openModal('customer')}>
          <i className="bi bi-person-plus" /> Add New Customer
        </button>
      </div>

      <div className="card">
        <div className="card-body" style={{ padding: 0 }}>
          <div className="table-responsive">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>Customer Name</th><th>Email</th><th>Phone Number</th><th>Web Portal User</th><th>Action</th>
                </tr>
              </thead>
              <tbody>
                {customers.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', color: '#94a3b8', padding: '2rem' }}>
                      {loading ? 'Loading…' : 'No customers found'}
                    </td>
                  </tr>
                ) : (
                  customers.map((c) => (
                    <tr key={c.customerId}>
                      <td style={{ color: '#64748b', fontWeight: 600 }}>#{c.customerId}</td>
                      <td style={{ fontWeight: 700 }}>{c.name}</td>
                      <td>{c.email}</td>
                      <td>{c.phone}</td>
                      <td>
                        {c.username
                          ? <span className="badge" style={{ background: '#e0f2fe', color: '#0369a1' }}>{c.username}</span>
                          : <span style={{ color: '#94a3b8' }}>None</span>}
                      </td>
                      <td>
                        <button
                          className="btn btn-outline"
                          style={{ padding: '0.25rem 0.6rem', fontSize: '0.8rem' }}
                          onClick={() => openModal('account', { customerId: c.customerId })}
                        >
                          + Open Account
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  );
}
