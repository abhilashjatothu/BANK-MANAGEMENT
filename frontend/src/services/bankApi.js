// Apex Bank API service: talks to the Spring Boot REST API when it is running,
// and transparently falls back to a localStorage-backed mock dataset otherwise.
import { mockData } from '../data/mockData.js';

export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';
const STATE_KEY = 'apex_bank_state';

// An HTTP-level error from the live backend (as opposed to a network failure).
class ApiError extends Error {}

const num = (v) => Number(v) || 0;
const uid = () => Date.now() + Math.floor(Math.random() * 1000);
const refNo = (prefix) => `${prefix}-${Date.now().toString().slice(-8)}`;

// The Java entities nest related objects (account.customer, transaction.account),
// while the UI works with flat fields. Normalize both shapes here.
const normCustomer = (c) => ({ ...c, username: c.username ?? c.user?.username ?? '' });
const normAccount = (a) => ({
  ...a,
  customerId: a.customerId ?? a.customer?.customerId,
  customerName: a.customerName ?? a.customer?.name,
  balance: num(a.balance),
});
const normTxn = (t) => ({
  ...t,
  accountNumber: t.accountNumber ?? t.account?.accountNumber,
  amount: num(t.amount),
  balanceAfter: num(t.balanceAfter),
});

class BankApi {
  constructor() {
    this.isLive = false;
    this.state = this.loadState();
  }

  // ---------- local (offline) state ----------
  loadState() {
    try {
      const saved = localStorage.getItem(STATE_KEY);
      if (saved) return JSON.parse(saved);
    } catch (e) {
      console.error('Could not parse saved state', e);
    }
    const initial = JSON.parse(JSON.stringify(mockData));
    initial.users = initial.users || [
      { userId: 1, username: 'admin', role: 'ADMIN' },
      { userId: 2, username: 'emp1', role: 'EMPLOYEE' },
      ...initial.customers
        .filter((c) => c.username)
        .map((c, i) => ({ userId: 10 + i, username: c.username, role: 'CUSTOMER', customerId: c.customerId })),
    ];
    this.saveState(initial);
    return initial;
  }

  saveState(state = this.state) {
    this.state = state;
    localStorage.setItem(STATE_KEY, JSON.stringify(state));
  }

  resetOfflineData() {
    localStorage.removeItem(STATE_KEY);
    this.state = this.loadState();
  }

  // ---------- live backend ----------
  async checkHealth() {
    try {
      const res = await fetch(`${API_BASE}/stats`, { mode: 'cors' });
      this.isLive = res.ok;
    } catch {
      this.isLive = false;
    }
    return this.isLive;
  }

  async live(path, options = {}) {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      throw new ApiError(data?.error || data?.message || `Request failed (${res.status})`);
    }
    return data;
  }

  // Run `liveFn` against the backend if connected; on network failure (not an
  // HTTP error) drop to offline mode and run `offlineFn` instead.
  async run(liveFn, offlineFn) {
    if (this.isLive) {
      try {
        return await liveFn();
      } catch (e) {
        if (e instanceof ApiError) throw e;
        this.isLive = false;
      }
    }
    return offlineFn();
  }

  post(path, body) {
    return this.live(path, { method: 'POST', body: JSON.stringify(body) });
  }

  // ---------- reads ----------
  getStats() {
    return this.run(
      () => this.live('/stats'),
      () => this.computeStats(this.state.accounts, this.state.transactions, this.state.customers.length)
    );
  }

  // Shared by offline mode and by the customer-scoped dashboard.
  computeStats(accounts = [], transactions = [], totalCustomers = 0) {
    const today = new Date().toDateString();
    const todays = transactions.filter((t) => new Date(t.dateTime).toDateString() === today);
    const count = (fn) => accounts.filter(fn).length;
    return {
      totalCustomers,
      totalAccounts: accounts.length,
      activeAccounts: count((a) => a.status === 'ACTIVE'),
      frozenAccounts: count((a) => a.status === 'FROZEN'),
      closedAccounts: count((a) => a.status === 'CLOSED'),
      totalBalance: accounts.reduce((s, a) => s + num(a.balance), 0),
      todayTransactionCount: todays.length,
      todayTransactionVolume: todays.reduce((s, t) => s + num(t.amount), 0),
      savingsAccountsCount: count((a) => a.accountType === 'SAVINGS'),
      currentAccountsCount: count((a) => a.accountType === 'CURRENT'),
    };
  }

  async getCustomers() {
    const list = await this.run(
      () => this.live('/customers'),
      () => this.state.customers || []
    );
    return list.map(normCustomer);
  }

  async getAccounts() {
    const list = await this.run(
      () => this.live('/accounts'),
      () => this.state.accounts || []
    );
    return list.map(normAccount);
  }

  async getTransactions() {
    const list = await this.run(
      () => this.live('/transactions'),
      () => this.state.transactions || []
    );
    return list.map(normTxn);
  }

  // ---------- writes ----------
  createCustomer(data) {
    return this.run(
      () => this.post('/customers', data),
      () => {
        const customer = {
          customerId: uid(),
          name: data.name,
          email: data.email,
          phone: data.phone,
          address: data.address || '',
          username: data.username || '',
          createdAt: new Date().toISOString(),
        };
        this.state.customers.unshift(customer);
        this.saveState();
        return customer;
      }
    );
  }

  openAccount(data) {
    return this.run(
      () => this.post('/accounts', { ...data, customerId: Number(data.customerId) }),
      () => {
        const customer = this.state.customers.find((c) => String(c.customerId) === String(data.customerId));
        if (!customer) throw new Error('Customer not found');
        const accountNumber = '10' + Math.floor(1000000000 + Math.random() * 9000000000);
        const balance = num(data.initialDeposit);
        const account = {
          accountId: uid(),
          accountNumber,
          customerId: customer.customerId,
          customerName: customer.name,
          accountType: data.accountType,
          balance,
          status: data.status || 'ACTIVE',
          createdAt: new Date().toISOString(),
        };
        this.state.accounts.unshift(account);
        if (balance > 0) {
          this.state.transactions.unshift({
            transactionId: uid(),
            accountNumber,
            type: 'DEPOSIT',
            amount: balance,
            balanceAfter: balance,
            dateTime: new Date().toISOString(),
            status: 'SUCCESS',
            referenceNo: refNo('TXN'),
            description: 'Initial account opening deposit',
          });
        }
        this.saveState();
        return account;
      }
    );
  }

  deposit(data) {
    return this.run(
      () => this.post('/transactions/deposit', data),
      () => this.offlineSingle('DEPOSIT', data)
    );
  }

  withdraw(data) {
    return this.run(
      () => this.post('/transactions/withdraw', data),
      () => this.offlineSingle('WITHDRAWAL', data)
    );
  }

  offlineSingle(type, data) {
    const isDeposit = type === 'DEPOSIT';
    const verb = isDeposit ? 'deposit' : 'withdrawal';
    const account = this.state.accounts.find((a) => a.accountNumber === data.accountNumber);
    if (!account) throw new Error('Account not found: ' + data.accountNumber);
    if (account.status !== 'ACTIVE') throw new Error(`Cannot process ${verb} on a ${account.status} account`);

    const amount = num(data.amount);
    if (amount <= 0) throw new Error(`Invalid ${verb} amount`);
    if (!isDeposit && account.balance < amount) {
      throw new Error('Insufficient funds. Available: $' + account.balance.toFixed(2));
    }

    account.balance += isDeposit ? amount : -amount;
    const txn = {
      transactionId: uid(),
      accountNumber: account.accountNumber,
      type,
      amount,
      balanceAfter: account.balance,
      dateTime: new Date().toISOString(),
      status: 'SUCCESS',
      referenceNo: refNo('TXN'),
      description: data.description || (isDeposit ? 'Cash Deposit' : 'Cash Withdrawal'),
    };
    this.state.transactions.unshift(txn);
    this.saveState();
    return txn;
  }

  transfer(data) {
    return this.run(
      () => this.post('/transactions/transfer', data),
      () => {
        if (data.fromAccountNumber === data.toAccountNumber) {
          throw new Error('Source and destination accounts cannot be identical');
        }
        const from = this.state.accounts.find((a) => a.accountNumber === data.fromAccountNumber);
        const to = this.state.accounts.find((a) => a.accountNumber === data.toAccountNumber);
        if (!from) throw new Error('Source account not found');
        if (!to) throw new Error('Destination account not found');
        if (from.status !== 'ACTIVE') throw new Error('Source account is not active');
        if (to.status !== 'ACTIVE') throw new Error('Destination account is not active');

        const amount = num(data.amount);
        if (amount <= 0) throw new Error('Invalid transfer amount');
        if (from.balance < amount) throw new Error('Insufficient balance in source account');

        from.balance -= amount;
        to.balance += amount;

        const referenceNo = refNo('TRF');
        const dateTime = new Date().toISOString();
        const note = data.description ? ' - ' + data.description : '';
        const base = { amount, dateTime, status: 'SUCCESS', referenceNo };
        this.state.transactions.unshift(
          { ...base, transactionId: uid(), accountNumber: from.accountNumber, type: 'TRANSFER_DEBIT', balanceAfter: from.balance, description: `Transfer to ${to.accountNumber}${note}` },
          { ...base, transactionId: uid(), accountNumber: to.accountNumber, type: 'TRANSFER_CREDIT', balanceAfter: to.balance, description: `Transfer from ${from.accountNumber}${note}` }
        );
        this.saveState();
        return { referenceNo, amount };
      }
    );
  }

  // ---------- auth ----------
  login(username, password) {
    return this.run(
      () => this.post('/auth/login', { username, password }),
      () => {
        const users = this.state.users || [];
        const found = users.find((u) => u.username.toLowerCase() === username.toLowerCase());
        if (found) {
          const customer = this.state.customers.find((c) => c.customerId === found.customerId);
          return { ...found, customerName: customer?.name };
        }
        // Offline demo convenience: any unknown username signs in, role guessed from its name.
        if (username && password) {
          const role = username.includes('admin') ? 'ADMIN' : username.includes('emp') ? 'EMPLOYEE' : 'CUSTOMER';
          return { userId: uid(), username, role };
        }
        throw new Error('Invalid username or password');
      }
    );
  }

  register(data) {
    return this.run(
      () => this.post('/auth/register', data),
      () => {
        const users = this.state.users || (this.state.users = []);
        if (users.some((u) => u.username.toLowerCase() === data.username.toLowerCase())) {
          throw new Error('Username already taken');
        }
        const customerId = uid();
        const accountNumber = '10' + Math.floor(1000000000 + Math.random() * 9000000000);
        const now = new Date().toISOString();
        this.state.customers.push({
          customerId, name: data.name, email: data.email, phone: data.phone,
          address: data.address || '', username: data.username, createdAt: now,
        });
        this.state.accounts.push({
          accountId: uid(), accountNumber, customerId, customerName: data.name,
          accountType: data.accountType || 'SAVINGS', balance: num(data.initialDeposit),
          status: 'ACTIVE', createdAt: now,
        });
        users.push({ userId: uid(), username: data.username, role: 'CUSTOMER', customerId });
        this.saveState();
        return { message: 'Account registered successfully', username: data.username, accountNumber };
      }
    );
  }

  forgotPassword(data) {
    return this.run(
      () => this.post('/auth/forgot-password', data),
      () => ({ message: 'Password has been successfully reset.' })
    );
  }
}

export const bankApi = new BankApi();
