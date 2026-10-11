// Apex Bank API Service with Smart Backend Detection & Fallback
const API_BASE = 'http://localhost:8080/api';

class BankApi {
  constructor() {
    this.isLive = false;
    this.localState = this.loadLocalState();
  }

  loadLocalState() {
    const saved = localStorage.getItem('apex_bank_state');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing local state', e);
      }
    }
    const initial = JSON.parse(JSON.stringify(window.ApexMockData || {}));
    this.saveLocalState(initial);
    return initial;
  }

  saveLocalState(state) {
    localStorage.setItem('apex_bank_state', JSON.stringify(state));
    this.localState = state;
  }

  async checkHealth() {
    try {
      const res = await fetch(`${API_BASE}/stats`, { method: 'GET', mode: 'cors' });
      if (res.ok) {
        this.isLive = true;
        return true;
      }
    } catch (err) {
      this.isLive = false;
    }
    return false;
  }

  async getStats() {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/stats`);
        if (res.ok) return await res.json();
      } catch (e) {
        this.isLive = false;
      }
    }
    // Calculate live totals from local state
    const accounts = this.localState.accounts || [];
    const active = accounts.filter(a => a.status === 'ACTIVE').length;
    const frozen = accounts.filter(a => a.status === 'FROZEN').length;
    const closed = accounts.filter(a => a.status === 'CLOSED').length;
    const totalBalance = accounts.reduce((acc, cur) => acc + (parseFloat(cur.balance) || 0), 0);
    const savings = accounts.filter(a => a.accountType === 'SAVINGS').length;
    const current = accounts.filter(a => a.accountType === 'CURRENT').length;

    return {
      totalCustomers: (this.localState.customers || []).length,
      totalAccounts: accounts.length,
      activeAccounts: active,
      frozenAccounts: frozen,
      closedAccounts: closed,
      totalBalance: totalBalance,
      todayTransactionCount: (this.localState.transactions || []).length,
      todayTransactionVolume: totalBalance,
      savingsAccountsCount: savings,
      currentAccountsCount: current
    };
  }

  async getCustomers() {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/customers`);
        if (res.ok) return await res.json();
      } catch (e) {
        this.isLive = false;
      }
    }
    return this.localState.customers || [];
  }

  async createCustomer(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/customers`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) return await res.json();
        const err = await res.json();
        throw new Error(err.error || 'Failed to create customer');
      } catch (e) {
        if (this.isLive) throw e;
      }
    }
    const id = Date.now();
    const customer = {
      customerId: id,
      name: data.name,
      email: data.email,
      phone: data.phone,
      address: data.address || '',
      username: data.username || '',
      createdAt: new Date().toISOString()
    };
    this.localState.customers.unshift(customer);
    this.saveLocalState(this.localState);
    return customer;
  }

  async getAccounts() {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/accounts`);
        if (res.ok) return await res.json();
      } catch (e) {
        this.isLive = false;
      }
    }
    return this.localState.accounts || [];
  }

  async openAccount(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/accounts`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) return await res.json();
        const err = await res.json();
        throw new Error(err.error || 'Failed to open account');
      } catch (e) {
        if (this.isLive) throw e;
      }
    }

    const customer = (this.localState.customers || []).find(c => String(c.customerId) === String(data.customerId));
    const randomNum = '10' + Math.floor(1000000000 + Math.random() * 9000000000);
    const balance = parseFloat(data.initialDeposit) || 0;

    const account = {
      accountId: Date.now(),
      accountNumber: randomNum,
      customerId: data.customerId,
      customerName: customer ? customer.name : 'Customer',
      accountType: data.accountType,
      balance: balance,
      status: data.status || 'ACTIVE',
      createdAt: new Date().toISOString()
    };

    this.localState.accounts.unshift(account);

    if (balance > 0) {
      this.localState.transactions.unshift({
        transactionId: Date.now() + 1,
        accountNumber: randomNum,
        type: 'DEPOSIT',
        amount: balance,
        balanceAfter: balance,
        dateTime: new Date().toISOString(),
        status: 'SUCCESS',
        referenceNo: 'TXN-' + Date.now().toString().slice(-8),
        description: 'Initial account opening deposit'
      });
    }

    this.saveLocalState(this.localState);
    return account;
  }

  async deposit(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/transactions/deposit`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) return await res.json();
        const err = await res.json();
        throw new Error(err.error || 'Deposit failed');
      } catch (e) {
        if (this.isLive) throw e;
      }
    }

    const account = this.localState.accounts.find(a => a.accountNumber === data.accountNumber);
    if (!account) throw new Error('Account not found: ' + data.accountNumber);
    if (account.status !== 'ACTIVE') throw new Error('Cannot deposit into ' + account.status + ' account');

    const amount = parseFloat(data.amount);
    if (isNaN(amount) || amount <= 0) throw new Error('Invalid deposit amount');

    account.balance += amount;
    const txn = {
      transactionId: Date.now(),
      accountNumber: account.accountNumber,
      type: 'DEPOSIT',
      amount: amount,
      balanceAfter: account.balance,
      dateTime: new Date().toISOString(),
      status: 'SUCCESS',
      referenceNo: 'TXN-' + Date.now().toString().slice(-8),
      description: data.description || 'Cash Deposit'
    };

    this.localState.transactions.unshift(txn);
    this.saveLocalState(this.localState);
    return txn;
  }

  async withdraw(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/transactions/withdraw`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) return await res.json();
        const err = await res.json();
        throw new Error(err.error || 'Withdrawal failed');
      } catch (e) {
        if (this.isLive) throw e;
      }
    }

    const account = this.localState.accounts.find(a => a.accountNumber === data.accountNumber);
    if (!account) throw new Error('Account not found: ' + data.accountNumber);
    if (account.status !== 'ACTIVE') throw new Error('Cannot withdraw from ' + account.status + ' account');

    const amount = parseFloat(data.amount);
    if (isNaN(amount) || amount <= 0) throw new Error('Invalid withdrawal amount');
    if (account.balance < amount) throw new Error('Insufficient funds. Available: $' + account.balance.toFixed(2));

    account.balance -= amount;
    const txn = {
      transactionId: Date.now(),
      accountNumber: account.accountNumber,
      type: 'WITHDRAWAL',
      amount: amount,
      balanceAfter: account.balance,
      dateTime: new Date().toISOString(),
      status: 'SUCCESS',
      referenceNo: 'TXN-' + Date.now().toString().slice(-8),
      description: data.description || 'Cash Withdrawal'
    };

    this.localState.transactions.unshift(txn);
    this.saveLocalState(this.localState);
    return txn;
  }

  async transfer(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/transactions/transfer`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) return await res.json();
        const err = await res.json();
        throw new Error(err.error || 'Transfer failed');
      } catch (e) {
        if (this.isLive) throw e;
      }
    }

    if (data.fromAccountNumber === data.toAccountNumber) {
      throw new Error('Source and destination accounts cannot be identical');
    }

    const fromAcc = this.localState.accounts.find(a => a.accountNumber === data.fromAccountNumber);
    const toAcc = this.localState.accounts.find(a => a.accountNumber === data.toAccountNumber);

    if (!fromAcc) throw new Error('Source account not found');
    if (!toAcc) throw new Error('Destination account not found');
    if (fromAcc.status !== 'ACTIVE') throw new Error('Source account is not active');
    if (toAcc.status !== 'ACTIVE') throw new Error('Destination account is not active');

    const amount = parseFloat(data.amount);
    if (isNaN(amount) || amount <= 0) throw new Error('Invalid transfer amount');
    if (fromAcc.balance < amount) throw new Error('Insufficient balance in source account');

    fromAcc.balance -= amount;
    toAcc.balance += amount;

    const ref = 'TRF-' + Date.now().toString().slice(-8);
    const now = new Date().toISOString();

    this.localState.transactions.unshift({
      transactionId: Date.now(),
      accountNumber: fromAcc.accountNumber,
      type: 'TRANSFER_DEBIT',
      amount: amount,
      balanceAfter: fromAcc.balance,
      dateTime: now,
      status: 'SUCCESS',
      referenceNo: ref,
      description: 'Transfer to ' + toAcc.accountNumber + (data.description ? ' - ' + data.description : '')
    });

    this.localState.transactions.unshift({
      transactionId: Date.now() + 1,
      accountNumber: toAcc.accountNumber,
      type: 'TRANSFER_CREDIT',
      amount: amount,
      balanceAfter: toAcc.balance,
      dateTime: now,
      status: 'SUCCESS',
      referenceNo: ref,
      description: 'Transfer from ' + fromAcc.accountNumber + (data.description ? ' - ' + data.description : '')
    });

    this.saveLocalState(this.localState);
    return { referenceNo: ref, amount: amount };
  }

  async getTransactions(filter = {}) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/transactions`);
        if (res.ok) return await res.json();
      } catch (e) {
        this.isLive = false;
      }
    }
    let list = this.localState.transactions || [];
    if (filter.accountNumber) {
      list = list.filter(t => t.accountNumber === filter.accountNumber);
    }
    if (filter.type) {
      list = list.filter(t => t.type === filter.type);
    }
    return list;
  }

  // --- Auth APIs ---
  async login(username, password) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password })
        });
        if (res.ok) {
          return await res.json();
        } else {
          const errData = await res.json().catch(() => ({}));
          throw new Error(errData.error || 'Invalid credentials');
        }
      } catch (err) {
        if (this.isLive) throw err;
      }
    }

    // Offline mock authentication fallback
    const users = (this.localState && this.localState.users) || (window.ApexMockData && window.ApexMockData.users) || [];
    const found = users.find(u => u.username.toLowerCase() === username.toLowerCase());
    if (found) {
      return {
        userId: found.userId,
        username: found.username,
        role: found.role,
        customerId: found.customerId
      };
    }
    // If entered credentials match standard test patterns
    if (username && password) {
      const role = username.includes('admin') ? 'ADMIN' : (username.includes('emp') ? 'EMPLOYEE' : 'CUSTOMER');
      return {
        userId: Date.now(),
        username: username,
        role: role
      };
    }
    throw new Error('Invalid username or password');
  }

  async register(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) {
          return await res.json();
        } else {
          const errData = await res.json().catch(() => ({}));
          throw new Error(errData.error || 'Registration failed');
        }
      } catch (err) {
        if (this.isLive) throw err;
      }
    }

    // Offline mock registration fallback
    const custId = Date.now();
    const accNum = '10' + Math.floor(1000000000 + Math.random() * 9000000000);
    const newCustomer = {
      customerId: custId,
      name: data.name,
      email: data.email,
      phone: data.phone,
      address: data.address || '',
      createdAt: new Date().toISOString()
    };
    const newAccount = {
      accountId: Date.now() + 1,
      accountNumber: accNum,
      customerId: custId,
      customerName: data.name,
      accountType: data.accountType || 'SAVINGS',
      balance: parseFloat(data.initialDeposit) || 0,
      status: 'ACTIVE',
      createdAt: new Date().toISOString()
    };

    if (!this.localState.customers) this.localState.customers = [];
    if (!this.localState.accounts) this.localState.accounts = [];
    if (!this.localState.users) this.localState.users = [];

    this.localState.customers.push(newCustomer);
    this.localState.accounts.push(newAccount);
    this.localState.users.push({
      userId: Date.now() + 2,
      username: data.username,
      role: 'CUSTOMER',
      customerId: custId
    });

    this.saveLocalState(this.localState);
    return {
      message: 'Account registered successfully',
      username: data.username,
      accountNumber: accNum
    };
  }

  async forgotPassword(data) {
    if (this.isLive) {
      try {
        const res = await fetch(`${API_BASE}/auth/forgot-password`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });
        if (res.ok) {
          return await res.json();
        } else {
          const errData = await res.json().catch(() => ({}));
          throw new Error(errData.error || 'Failed to reset password');
        }
      } catch (err) {
        if (this.isLive) throw err;
      }
    }

    // Offline mock reset fallback
    return { message: 'Password has been successfully reset.' };
  }
}

window.bankApi = new BankApi();
