import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { bankApi } from '../services/bankApi.js';
import { useAuth } from './AuthContext.jsx';

const BankContext = createContext(null);

export function BankProvider({ children }) {
  const { user } = useAuth();
  const isCustomer = user?.role === 'CUSTOMER';

  const [customers, setCustomers] = useState([]);
  const [allAccounts, setAllAccounts] = useState([]);
  const [allTransactions, setAllTransactions] = useState([]);
  const [apiStats, setApiStats] = useState(null);
  const [isLive, setIsLive] = useState(false);
  const [loading, setLoading] = useState(true);
  const [toasts, setToasts] = useState([]);
  const [modal, setModal] = useState(null); // { name, accountNumber?, customerId? }
  const toastId = useRef(0);

  // ---- toasts ----
  const showToast = useCallback((message, isError = false) => {
    const id = ++toastId.current;
    setToasts((t) => [...t, { id, message, isError }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4000);
  }, []);

  // ---- modals ----
  const openModal = useCallback((name, payload = {}) => setModal({ name, ...payload }), []);
  const closeModal = useCallback(() => setModal(null), []);

  // ---- data ----
  const refresh = useCallback(async () => {
    try {
      const [c, a, t, s] = await Promise.all([
        bankApi.getCustomers(),
        bankApi.getAccounts(),
        bankApi.getTransactions(),
        bankApi.getStats(),
      ]);
      setCustomers(c);
      setAllAccounts(a);
      setAllTransactions(t);
      setApiStats(s);
      setIsLive(bankApi.isLive);
    } catch (err) {
      showToast(err.message || 'Failed to load data', true);
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    if (!user) return undefined;
    let cancelled = false;
    (async () => {
      await bankApi.checkHealth();
      if (!cancelled) await refresh();
    })();
    // Re-check backend availability every 10 seconds
    const timer = setInterval(async () => {
      const live = await bankApi.checkHealth();
      if (!cancelled) setIsLive(live);
    }, 10000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [user, refresh]);

  // Customers only ever see their own accounts and the transactions on them.
  const accounts = useMemo(
    () => (isCustomer ? allAccounts.filter((a) => String(a.customerId) === String(user.customerId)) : allAccounts),
    [allAccounts, isCustomer, user]
  );

  const transactions = useMemo(() => {
    if (!isCustomer) return allTransactions;
    const owned = new Set(accounts.map((a) => a.accountNumber));
    return allTransactions.filter((t) => owned.has(t.accountNumber));
  }, [allTransactions, accounts, isCustomer]);

  const stats = useMemo(
    () => (isCustomer ? bankApi.computeStats(accounts, transactions, 1) : apiStats),
    [isCustomer, accounts, transactions, apiStats]
  );

  // Run an API action, toast the outcome, refresh data, and close the modal on success.
  const perform = useCallback(
    async (action, successMessage) => {
      try {
        const result = await action();
        showToast(typeof successMessage === 'function' ? successMessage(result) : successMessage);
        setModal(null);
        await refresh();
        return result;
      } catch (err) {
        showToast(err.message || 'Something went wrong', true);
        return null;
      }
    },
    [refresh, showToast]
  );

  const value = {
    customers, accounts, transactions, stats, isLive, loading,
    toasts, modal, showToast, openModal, closeModal, refresh, perform,
  };
  return <BankContext.Provider value={value}>{children}</BankContext.Provider>;
}

export function useBank() {
  const ctx = useContext(BankContext);
  if (!ctx) throw new Error('useBank must be used inside <BankProvider>');
  return ctx;
}
