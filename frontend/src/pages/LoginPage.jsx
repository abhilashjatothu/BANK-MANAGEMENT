import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { bankApi } from '../services/bankApi.js';

function Alert({ alert }) {
  if (!alert) return null;
  const icon = alert.type === 'error' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill';
  return (
    <div className={`alert-banner ${alert.type}`} style={{ display: 'flex' }}>
      <i className={`bi ${icon}`} /> <span>{alert.message}</span>
    </div>
  );
}

const fullWidthBtn = { width: '100%', padding: '0.75rem', justifyContent: 'center' };

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [view, setView] = useState('signin'); // signin | signup | forgot
  const [alert, setAlert] = useState(null);
  const [busy, setBusy] = useState(false);

  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [reg, setReg] = useState({
    name: '', email: '', phone: '', address: '', username: '',
    accountType: 'SAVINGS', initialDeposit: '100.00', password: '', confirmPassword: '',
  });
  const [reset, setReset] = useState({ username: '', email: '', newPassword: '', confirmNewPassword: '' });

  if (user) return <Navigate to="/" replace />;

  const go = (next) => {
    setView(next);
    setAlert(null);
  };
  const bind = (state, setState) => (k) => ({
    value: state[k],
    onChange: (e) => setState({ ...state, [k]: e.target.value }),
  });
  const L = bind(loginForm, setLoginForm);
  const R = bind(reg, setReg);
  const P = bind(reset, setReset);

  // Runs `fn`, showing errors in the alert banner and disabling buttons meanwhile.
  const guarded = (fn) => async (e) => {
    e.preventDefault();
    setAlert(null);
    setBusy(true);
    try {
      await fn();
    } catch (err) {
      setAlert({ type: 'error', message: err.message || 'Something went wrong' });
    } finally {
      setBusy(false);
    }
  };

  const onSignIn = guarded(async () => {
    await login(loginForm.username.trim(), loginForm.password);
    navigate('/', { replace: true });
  });

  const onSignUp = guarded(async () => {
    if (reg.password !== reg.confirmPassword) throw new Error('Passwords do not match');
    await bankApi.checkHealth();
    await bankApi.register({
      ...reg,
      name: reg.name.trim(),
      email: reg.email.trim(),
      phone: reg.phone.trim(),
      address: reg.address.trim(),
      username: reg.username.trim(),
      initialDeposit: parseFloat(reg.initialDeposit) || 0,
    });
    setView('signin');
    setAlert({ type: 'success', message: 'Account created successfully! Please sign in.' });
  });

  const onReset = guarded(async () => {
    if (reset.newPassword !== reset.confirmNewPassword) throw new Error('New passwords do not match');
    await bankApi.checkHealth();
    await bankApi.forgotPassword({ ...reset, username: reset.username.trim(), email: reset.email.trim() });
    setView('signin');
    setAlert({ type: 'success', message: 'Password has been reset! Please sign in with your new password.' });
  });

  return (
    <div className="auth-page">
      <div className={`auth-container${view === 'signup' ? ' wide' : ''}`}>
        {view === 'signin' && (
          <div>
            <div className="auth-brand">
              <i className="bi bi-bank2" />
              <h2>Apex Bank</h2>
              <p>Enter your credentials to access the banking portal</p>
            </div>
            <Alert alert={alert} />
            <form onSubmit={onSignIn}>
              <div className="form-group">
                <label>Username</label>
                <input className="form-control" placeholder="Enter your username" required autoFocus {...L('username')} />
              </div>
              <div className="form-group">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                  <label style={{ margin: 0 }}>Password</label>
                  <a className="auth-link" style={{ fontSize: '0.85rem' }} onClick={() => go('forgot')}>Forgot Password?</a>
                </div>
                <input type="password" className="form-control" placeholder="Enter your password" required {...L('password')} />
              </div>
              <button type="submit" className="btn btn-primary" style={{ ...fullWidthBtn, marginTop: '1rem' }} disabled={busy}>
                <i className="bi bi-box-arrow-in-right" /> {busy ? 'Signing in…' : 'Sign In to Portal'}
              </button>
            </form>
            <div className="auth-footer">
              Don&apos;t have an account? <a className="auth-link" onClick={() => go('signup')}>Sign Up</a>
            </div>
          </div>
        )}

        {view === 'signup' && (
          <div>
            <div className="auth-brand">
              <i className="bi bi-person-plus-fill" />
              <h2>Create an Account</h2>
              <p>Register your profile and open an account</p>
            </div>
            <Alert alert={alert} />
            <form onSubmit={onSignUp}>
              <div className="form-grid">
                <div className="form-group col-span-2">
                  <label>Full Name *</label>
                  <input className="form-control" placeholder="e.g. John Doe" required {...R('name')} />
                </div>
                <div className="form-group">
                  <label>Email Address *</label>
                  <input type="email" className="form-control" placeholder="name@example.com" required {...R('email')} />
                </div>
                <div className="form-group">
                  <label>Phone Number *</label>
                  <input type="tel" className="form-control" placeholder="+1 555-0199" required {...R('phone')} />
                </div>
                <div className="form-group col-span-2">
                  <label>Residential Address</label>
                  <input className="form-control" placeholder="123 Financial Way, Suite 100" {...R('address')} />
                </div>
                <div className="form-group">
                  <label>Username *</label>
                  <input className="form-control" placeholder="Choose username" required {...R('username')} />
                </div>
                <div className="form-group">
                  <label>Account Type *</label>
                  <select className="form-select" required {...R('accountType')}>
                    <option value="SAVINGS">Savings Account</option>
                    <option value="CURRENT">Current Account</option>
                  </select>
                </div>
                <div className="form-group col-span-2">
                  <label>Initial Opening Deposit ($) *</label>
                  <input type="number" step="0.01" min="0" className="form-control" required {...R('initialDeposit')} />
                </div>
                <div className="form-group">
                  <label>Password *</label>
                  <input type="password" className="form-control" placeholder="Min 6 characters" minLength={6} required {...R('password')} />
                </div>
                <div className="form-group">
                  <label>Confirm Password *</label>
                  <input type="password" className="form-control" placeholder="Re-type password" required {...R('confirmPassword')} />
                </div>
              </div>
              <button type="submit" className="btn btn-primary" style={{ ...fullWidthBtn, marginTop: '1.25rem' }} disabled={busy}>
                <i className="bi bi-person-check" /> {busy ? 'Creating account…' : 'Register & Open Account'}
              </button>
            </form>
            <div className="auth-footer">
              Already have an account? <a className="auth-link" onClick={() => go('signin')}>Sign In</a>
            </div>
          </div>
        )}

        {view === 'forgot' && (
          <div>
            <div className="auth-brand">
              <i className="bi bi-shield-lock-fill" style={{ color: '#d97706' }} />
              <h2>Reset Password</h2>
              <p>Verify your details to set a new password</p>
            </div>
            <Alert alert={alert} />
            <form onSubmit={onReset}>
              <div className="form-group">
                <label>Username *</label>
                <input className="form-control" placeholder="Enter username" required {...P('username')} />
              </div>
              <div className="form-group">
                <label>Registered Email Address *</label>
                <input type="email" className="form-control" placeholder="name@example.com" required {...P('email')} />
              </div>
              <div className="form-group">
                <label>New Password *</label>
                <input type="password" className="form-control" placeholder="Min 6 characters" minLength={6} required {...P('newPassword')} />
              </div>
              <div className="form-group">
                <label>Confirm New Password *</label>
                <input type="password" className="form-control" placeholder="Re-type new password" required {...P('confirmNewPassword')} />
              </div>
              <button type="submit" className="btn btn-primary" style={{ ...fullWidthBtn, marginTop: '1rem' }} disabled={busy}>
                <i className="bi bi-key" /> {busy ? 'Resetting…' : 'Reset Password'}
              </button>
            </form>
            <div className="auth-footer">
              Remember your password? <a className="auth-link" onClick={() => go('signin')}>Back to Sign In</a>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
