// Apex Bank Frontend UI Controller
document.addEventListener('DOMContentLoaded', async () => {
  const api = window.bankApi;

  // 1. Initial health check
  const isOnline = await api.checkHealth();
  updateApiBadge(isOnline);

  // 2. Setup Navigation
  const navBtns = document.querySelectorAll('.nav-btn');
  navBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      navBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const viewId = btn.getAttribute('data-view');
      switchView(viewId);
    });
  });

  // 3. Setup Modals
  setupModals();

  // 4. Initial load
  await refreshAll();

  // Polling backend status every 10 seconds
  setInterval(async () => {
    const online = await api.checkHealth();
    updateApiBadge(online);
  }, 10000);
});

function updateApiBadge(isOnline) {
  const badge = document.getElementById('apiStatusBadge');
  if (!badge) return;
  if (isOnline) {
    badge.className = 'api-status-badge api-online';
    badge.innerHTML = '<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#10b981;"></span> Connected to Spring Boot';
  } else {
    badge.className = 'api-status-badge api-mock';
    badge.innerHTML = '<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#f59e0b;"></span> Standalone Offline Mode';
  }
}

function switchView(viewId) {
  const views = document.querySelectorAll('.view-panel');
  views.forEach(v => v.style.display = 'none');
  const target = document.getElementById(viewId);
  if (target) {
    target.style.display = 'block';
  }
}

async function refreshAll() {
  await renderDashboard();
  await renderCustomers();
  await renderAccounts();
  await renderTransactions();
}

// Format currency
function formatMoney(amount) {
  return '$' + Number(amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Toast notification
function showToast(message, isError = false) {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = 'toast' + (isError ? ' error' : '');
  toast.innerHTML = `<span style="font-weight:bold;">${isError ? 'Error: ' : 'Success: '}</span> ${message}`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Render Dashboard
async function renderDashboard() {
  const stats = await window.bankApi.getStats();
  document.getElementById('statTotalCustomers').innerText = stats.totalCustomers;
  document.getElementById('statTotalAccounts').innerText = stats.totalAccounts;
  document.getElementById('statTotalBalance').innerText = formatMoney(stats.totalBalance);
  document.getElementById('statTodayVolume').innerText = formatMoney(stats.todayTransactionVolume);

  // Recent activity list
  const txns = await window.bankApi.getTransactions();
  const recent = txns.slice(0, 5);
  const tbody = document.getElementById('recentTxnBody');
  if (!tbody) return;

  if (recent.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#94a3b8;padding:2rem;">No recent transactions</td></tr>';
    return;
  }

  tbody.innerHTML = recent.map(tx => `
    <tr>
      <td style="font-weight:600;color:var(--primary);">${tx.referenceNo}</td>
      <td>${new Date(tx.dateTime).toLocaleString()}</td>
      <td><strong>${tx.accountNumber}</strong></td>
      <td><span class="badge ${getBadgeClass(tx.type)}">${tx.type}</span></td>
      <td style="font-weight:700;">${formatMoney(tx.amount)}</td>
      <td style="color:#64748b;">${formatMoney(tx.balanceAfter)}</td>
    </tr>
  `).join('');
}

function getBadgeClass(type) {
  if (type === 'DEPOSIT') return 'badge-deposit';
  if (type === 'WITHDRAWAL') return 'badge-withdrawal';
  if (type.includes('TRANSFER')) return 'badge-transfer';
  return 'badge-type';
}

// Render Customers
async function renderCustomers() {
  const customers = await window.bankApi.getCustomers();
  const tbody = document.getElementById('customersTableBody');
  if (!tbody) return;

  if (customers.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#94a3b8;padding:2rem;">No customers found</td></tr>';
    return;
  }

  tbody.innerHTML = customers.map(c => `
    <tr>
      <td style="color:#64748b;font-weight:600;">#${c.customerId}</td>
      <td style="font-weight:700;">${c.name}</td>
      <td>${c.email}</td>
      <td>${c.phone}</td>
      <td>${c.username ? `<span class="badge" style="background:#e0f2fe;color:#0369a1;">${c.username}</span>` : '<span style="color:#94a3b8;">None</span>'}</td>
      <td>
        <button class="btn btn-outline" style="padding:0.25rem 0.6rem;font-size:0.8rem;" onclick="openNewAccountForCustomer('${c.customerId}')">
          + Open Account
        </button>
      </td>
    </tr>
  `).join('');

  // Update customer options in account creation modal
  const select = document.getElementById('newAccountCustomerSelect');
  if (select) {
    select.innerHTML = customers.map(c => `<option value="${c.customerId}">${c.name} (${c.email})</option>`).join('');
  }
}

// Render Accounts
async function renderAccounts() {
  const accounts = await window.bankApi.getAccounts();
  const grid = document.getElementById('accountsGrid');
  if (!grid) return;

  if (accounts.length === 0) {
    grid.innerHTML = '<div style="grid-column: 1/-1; text-align:center; color:#94a3b8; padding:3rem;">No accounts on record.</div>';
    return;
  }

  grid.innerHTML = accounts.map(a => `
    <div class="account-card ${a.status === 'FROZEN' ? 'frozen' : (a.status === 'CLOSED' ? 'closed' : '')}">
      <div class="account-top">
        <div>
          <span class="badge badge-type" style="margin-bottom:0.35rem;">${a.accountType}</span>
          <div class="account-num">${a.accountNumber}</div>
          <small style="color:#64748b;">${a.customerName || 'Customer'}</small>
        </div>
        <span class="badge ${a.status === 'ACTIVE' ? 'badge-active' : (a.status === 'FROZEN' ? 'badge-frozen' : 'badge-closed')}">${a.status}</span>
      </div>

      <div class="account-balance-box">
        <div class="label">Available Balance</div>
        <div class="amount">${formatMoney(a.balance)}</div>
      </div>

      <div class="account-actions">
        <button class="btn btn-outline" style="flex:1;padding:0.4rem;font-size:0.82rem;" onclick="openDepositModal('${a.accountNumber}')">Deposit</button>
        <button class="btn btn-outline" style="flex:1;padding:0.4rem;font-size:0.82rem;" onclick="openWithdrawModal('${a.accountNumber}')">Withdraw</button>
        <button class="btn btn-primary" style="flex:1;padding:0.4rem;font-size:0.82rem;" onclick="openTransferModal('${a.accountNumber}')">Transfer</button>
      </div>
    </div>
  `).join('');

  // Populate source dropdowns in modals
  const depositSelect = document.getElementById('depositAccountSelect');
  const withdrawSelect = document.getElementById('withdrawAccountSelect');
  const transferFromSelect = document.getElementById('transferFromSelect');

  const optionsHtml = accounts.map(a => `<option value="${a.accountNumber}">${a.accountNumber} (${a.accountType} - ${formatMoney(a.balance)})</option>`).join('');

  if (depositSelect) depositSelect.innerHTML = optionsHtml;
  if (withdrawSelect) withdrawSelect.innerHTML = optionsHtml;
  if (transferFromSelect) transferFromSelect.innerHTML = optionsHtml;
}

// Render Transactions
async function renderTransactions() {
  const txns = await window.bankApi.getTransactions();
  const tbody = document.getElementById('txnsTableBody');
  if (!tbody) return;

  if (txns.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#94a3b8;padding:2rem;">No transactions recorded</td></tr>';
    return;
  }

  tbody.innerHTML = txns.map(tx => `
    <tr>
      <td style="font-weight:600;color:var(--primary);">${tx.referenceNo}</td>
      <td>${new Date(tx.dateTime).toLocaleString()}</td>
      <td><strong>${tx.accountNumber}</strong></td>
      <td><span class="badge ${getBadgeClass(tx.type)}">${tx.type}</span></td>
      <td style="color:#64748b;">${tx.description || '-'}</td>
      <td style="font-weight:700;">${formatMoney(tx.amount)}</td>
      <td style="color:#475569;">${formatMoney(tx.balanceAfter)}</td>
    </tr>
  `).join('');
}

// Setup Modals and Form Handlers
function setupModals() {
  // Modal openers
  document.querySelectorAll('[data-open-modal]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.getAttribute('data-open-modal');
      openModal(modalId);
    });
  });

  // Modal closers
  document.querySelectorAll('.modal-close, [data-close-modal]').forEach(btn => {
    btn.addEventListener('click', () => {
      btn.closest('.modal-overlay').classList.remove('active');
    });
  });

  // Close on outside click
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) overlay.classList.remove('active');
    });
  });

  // Form: Create Customer
  const formCustomer = document.getElementById('formNewCustomer');
  if (formCustomer) {
    formCustomer.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const data = {
          name: document.getElementById('custName').value,
          email: document.getElementById('custEmail').value,
          phone: document.getElementById('custPhone').value,
          address: document.getElementById('custAddress').value,
          username: document.getElementById('custUsername').value
        };
        await window.bankApi.createCustomer(data);
        showToast('Customer created successfully!');
        closeModal('modalNewCustomer');
        formCustomer.reset();
        await refreshAll();
      } catch (err) {
        showToast(err.message, true);
      }
    });
  }

  // Form: Open Account
  const formAccount = document.getElementById('formNewAccount');
  if (formAccount) {
    formAccount.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const data = {
          customerId: document.getElementById('newAccountCustomerSelect').value,
          accountType: document.getElementById('newAccountType').value,
          initialDeposit: parseFloat(document.getElementById('newAccountDeposit').value) || 0,
          status: 'ACTIVE'
        };
        const acc = await window.bankApi.openAccount(data);
        showToast(`Account ${acc.accountNumber} opened successfully!`);
        closeModal('modalNewAccount');
        formAccount.reset();
        await refreshAll();
      } catch (err) {
        showToast(err.message, true);
      }
    });
  }

  // Form: Deposit
  const formDeposit = document.getElementById('formDeposit');
  if (formDeposit) {
    formDeposit.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const data = {
          accountNumber: document.getElementById('depositAccountSelect').value,
          amount: parseFloat(document.getElementById('depositAmount').value),
          description: document.getElementById('depositNote').value
        };
        const res = await window.bankApi.deposit(data);
        showToast(`Deposited ${formatMoney(res.amount)} successfully!`);
        closeModal('modalDeposit');
        formDeposit.reset();
        await refreshAll();
      } catch (err) {
        showToast(err.message, true);
      }
    });
  }

  // Form: Withdraw
  const formWithdraw = document.getElementById('formWithdraw');
  if (formWithdraw) {
    formWithdraw.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const data = {
          accountNumber: document.getElementById('withdrawAccountSelect').value,
          amount: parseFloat(document.getElementById('withdrawAmount').value),
          description: document.getElementById('withdrawNote').value
        };
        const res = await window.bankApi.withdraw(data);
        showToast(`Withdrew ${formatMoney(res.amount)} successfully!`);
        closeModal('modalWithdraw');
        formWithdraw.reset();
        await refreshAll();
      } catch (err) {
        showToast(err.message, true);
      }
    });
  }

  // Form: Transfer
  const formTransfer = document.getElementById('formTransfer');
  if (formTransfer) {
    formTransfer.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const data = {
          fromAccountNumber: document.getElementById('transferFromSelect').value,
          toAccountNumber: document.getElementById('transferToInput').value.trim(),
          amount: parseFloat(document.getElementById('transferAmount').value),
          description: document.getElementById('transferNote').value
        };
        const res = await window.bankApi.transfer(data);
        showToast(`Transferred ${formatMoney(res.amount)}. Ref: ${res.referenceNo}`);
        closeModal('modalTransfer');
        formTransfer.reset();
        await refreshAll();
      } catch (err) {
        showToast(err.message, true);
      }
    });
  }
}

function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('active');
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('active');
}

// Global quick actions
window.openDepositModal = function(accountNumber) {
  const select = document.getElementById('depositAccountSelect');
  if (select && accountNumber) select.value = accountNumber;
  openModal('modalDeposit');
};

window.openWithdrawModal = function(accountNumber) {
  const select = document.getElementById('withdrawAccountSelect');
  if (select && accountNumber) select.value = accountNumber;
  openModal('modalWithdraw');
};

window.openTransferModal = function(accountNumber) {
  const select = document.getElementById('transferFromSelect');
  if (select && accountNumber) select.value = accountNumber;
  openModal('modalTransfer');
};

window.openNewAccountForCustomer = function(customerId) {
  const select = document.getElementById('newAccountCustomerSelect');
  if (select && customerId) select.value = customerId;
  openModal('modalNewAccount');
};
