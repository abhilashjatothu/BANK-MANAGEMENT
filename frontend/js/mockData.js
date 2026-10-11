// Apex Bank Standalone Mock Dataset
window.ApexMockData = {
  systemStats: {
    totalCustomers: 4,
    totalAccounts: 6,
    activeAccounts: 5,
    frozenAccounts: 1,
    closedAccounts: 0,
    totalBalance: 49921.25,
    todayTransactionCount: 11,
    todayTransactionVolume: 63018.00,
    savingsAccountsCount: 4,
    currentAccountsCount: 2
  },
  customers: [
    {
      customerId: 1,
      name: "J. Abhilash",
      email: "abhilash@bankms.com",
      phone: "+1-555-0199",
      address: "100 Financial Way, Tech Hub",
      username: "cust1",
      createdAt: "2026-09-01T09:00:00"
    },
    {
      customerId: 2,
      name: "Sarah Jenkins",
      email: "sarah.j@example.com",
      phone: "+1-555-0144",
      address: "742 Evergreen Terrace, Springfield",
      username: "cust2",
      createdAt: "2026-09-10T10:30:00"
    },
    {
      customerId: 3,
      name: "Michael Chen",
      email: "michael.c@example.com",
      phone: "+1-555-0178",
      address: "450 Innovation Parkway, Boston",
      username: "cust3",
      createdAt: "2026-09-15T11:15:00"
    },
    {
      customerId: 4,
      name: "Priya Sharma",
      email: "priya.s@example.com",
      phone: "+1-555-0211",
      address: "88 Sunset Boulevard, Los Angeles",
      username: "cust4",
      createdAt: "2026-10-01T14:00:00"
    }
  ],
  accounts: [
    {
      accountId: 1,
      accountNumber: "101020304050",
      customerId: 1,
      customerName: "J. Abhilash",
      accountType: "SAVINGS",
      balance: 5000.00,
      status: "ACTIVE",
      createdAt: "2026-09-01T09:15:00"
    },
    {
      accountId: 2,
      accountNumber: "101090807060",
      customerId: 1,
      customerName: "J. Abhilash",
      accountType: "CURRENT",
      balance: 12500.00,
      status: "ACTIVE",
      createdAt: "2026-09-01T09:30:00"
    },
    {
      accountId: 3,
      accountNumber: "102030405060",
      customerId: 2,
      customerName: "Sarah Jenkins",
      accountType: "SAVINGS",
      balance: 8450.50,
      status: "ACTIVE",
      createdAt: "2026-09-10T11:00:00"
    },
    {
      accountId: 4,
      accountNumber: "102070809010",
      customerId: 2,
      customerName: "Sarah Jenkins",
      accountType: "CURRENT",
      balance: 1520.00,
      status: "ACTIVE",
      createdAt: "2026-09-10T11:20:00"
    },
    {
      accountId: 5,
      accountNumber: "103040506070",
      customerId: 3,
      customerName: "Michael Chen",
      accountType: "SAVINGS",
      balance: 22100.75,
      status: "ACTIVE",
      createdAt: "2026-09-15T11:30:00"
    },
    {
      accountId: 6,
      accountNumber: "104050607080",
      customerId: 4,
      customerName: "Priya Sharma",
      accountType: "SAVINGS",
      balance: 350.00,
      status: "FROZEN",
      createdAt: "2026-10-01T14:15:00"
    }
  ],
  transactions: [
    {
      transactionId: 11,
      accountNumber: "104050607080",
      type: "DEPOSIT",
      amount: 350.00,
      balanceAfter: 350.00,
      dateTime: "2026-10-01T14:20:00",
      status: "SUCCESS",
      referenceNo: "TXN-20261001142000-1009",
      description: "Welcome opening credit"
    },
    {
      transactionId: 10,
      accountNumber: "102030405060",
      type: "WITHDRAWAL",
      amount: 1948.75,
      balanceAfter: 8450.50,
      dateTime: "2026-09-28T15:30:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260928153000-1008",
      description: "Branch teller withdrawal"
    },
    {
      transactionId: 8,
      accountNumber: "103040506070",
      type: "TRANSFER_DEBIT",
      amount: 2899.25,
      balanceAfter: 22100.75,
      dateTime: "2026-09-20T10:10:00",
      status: "SUCCESS",
      referenceNo: "TRF-20260920101000-8801",
      description: "Wire transfer to 102030405060"
    },
    {
      transactionId: 9,
      accountNumber: "102030405060",
      type: "TRANSFER_CREDIT",
      amount: 2899.25,
      balanceAfter: 10399.25,
      dateTime: "2026-09-20T10:10:00",
      status: "SUCCESS",
      referenceNo: "TRF-20260920101000-8801",
      description: "Wire transfer from 103040506070"
    },
    {
      transactionId: 7,
      accountNumber: "103040506070",
      type: "DEPOSIT",
      amount: 25000.00,
      balanceAfter: 25000.00,
      dateTime: "2026-09-15T11:35:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260915113500-1007",
      description: "Investment liquidation deposit"
    },
    {
      transactionId: 6,
      accountNumber: "102070809010",
      type: "WITHDRAWAL",
      amount: 480.00,
      balanceAfter: 1520.00,
      dateTime: "2026-09-12T16:45:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260912164500-1006",
      description: "Online utility bills payment"
    },
    {
      transactionId: 5,
      accountNumber: "102070809010",
      type: "DEPOSIT",
      amount: 2000.00,
      balanceAfter: 2000.00,
      dateTime: "2026-09-10T11:25:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260910112500-1005",
      description: "Direct payroll deposit"
    },
    {
      transactionId: 4,
      accountNumber: "102030405060",
      type: "DEPOSIT",
      amount: 7500.00,
      balanceAfter: 7500.00,
      dateTime: "2026-09-10T11:05:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260910110500-1004",
      description: "Initial opening deposit"
    },
    {
      transactionId: 3,
      accountNumber: "101090807060",
      type: "WITHDRAWAL",
      amount: 2500.00,
      balanceAfter: 12500.00,
      dateTime: "2026-09-05T14:20:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260905142000-1003",
      description: "ATM cash withdrawal"
    },
    {
      transactionId: 2,
      accountNumber: "101090807060",
      type: "DEPOSIT",
      amount: 15000.00,
      balanceAfter: 15000.00,
      dateTime: "2026-09-01T09:35:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260901093500-1002",
      description: "Corporate business fund deposit"
    },
    {
      transactionId: 1,
      accountNumber: "101020304050",
      type: "DEPOSIT",
      amount: 5000.00,
      balanceAfter: 5000.00,
      dateTime: "2026-09-01T09:20:00",
      status: "SUCCESS",
      referenceNo: "TXN-20260901092000-1001",
      description: "Initial account opening deposit"
    }
  ]
};
