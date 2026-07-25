'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/Sidebar';
import Navbar from '@/components/Navbar';
import AuthGuard from '@/components/AuthGuard';
import { apiFetch } from '@/lib/api';
import { Instagram, Plus, CheckCircle2, AlertCircle, Trash2 } from 'lucide-react';

interface Account {
  id: string;
  igUserId: string;
  igUsername: string;
  isConnected: boolean;
  tokenExpiresAt: string;
}

export default function InstagramAccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);

  const loadAccounts = async () => {
    try {
      const data = await apiFetch<{ accounts: Account[] }>('/instagram/accounts');
      setAccounts(data.accounts || []);
    } catch (err) {
      console.error('Failed to load IG accounts', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
  }, []);

  const handleConnect = async () => {
    try {
      const data = await apiFetch<{ url: string }>('/instagram/connect');
      window.location.href = data.url;
    } catch (err: any) {
      alert(err.message || 'Failed to initiate Meta OAuth');
    }
  };

  const handleDisconnect = async (id: string) => {
    if (!confirm('Are you sure you want to disconnect this Instagram account?')) return;
    try {
      await apiFetch(`/instagram/accounts/${id}`, { method: 'DELETE' });
      loadAccounts();
    } catch (err: any) {
      alert(err.message || 'Failed to disconnect account');
    }
  };

  return (
    <AuthGuard>
    <div className="flex min-h-screen bg-[#090d16]">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar />

        <main className="p-6 space-y-6 flex-1 overflow-y-auto">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
            <div>
              <h1 className="text-2xl font-bold text-slate-100">Instagram Accounts</h1>
              <p className="text-xs text-slate-400">Connect Instagram Business or Creator accounts via Meta Graph API</p>
            </div>

            <button
              onClick={handleConnect}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-pink-500 via-purple-500 to-indigo-500 text-white text-xs font-semibold hover:opacity-95 transition-opacity shadow-lg shadow-pink-500/20"
            >
              <Plus className="w-4 h-4" />
              Connect Instagram Account
            </button>
          </div>

          {loading ? (
            <div className="py-12 text-center text-xs text-slate-500">Loading connected accounts...</div>
          ) : accounts.length === 0 ? (
            <div className="glass-card p-12 rounded-2xl text-center space-y-3 max-w-lg mx-auto mt-8 border border-white/10">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-pink-500 to-indigo-500 flex items-center justify-center mx-auto text-white shadow-xl shadow-pink-500/30">
                <Instagram className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-100">No Instagram Account Connected</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Connect your Instagram Business account to start automatically responding to post & reel comments with personalized DMs.
              </p>
              <button
                onClick={handleConnect}
                className="mt-4 px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors shadow-lg shadow-indigo-600/30"
              >
                Connect via Facebook OAuth
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {accounts.map((acc) => (
                <div key={acc.id} className="glass-card glass-card-hover p-5 rounded-2xl flex items-center justify-between border border-white/10">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-pink-500 to-purple-600 flex items-center justify-center text-white font-bold text-lg shadow-md">
                      <Instagram className="w-6 h-6" />
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-100 text-sm">@{acc.igUsername || 'Instagram Account'}</h4>
                      <span className="text-[11px] text-slate-400 block mt-0.5">IG User ID: {acc.igUserId}</span>
                      <div className="flex items-center gap-1.5 mt-2">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="text-[10px] text-emerald-400 font-semibold uppercase tracking-wider">Connected & Active</span>
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => handleDisconnect(acc.id)}
                    className="p-2 text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 rounded-xl transition-colors"
                    title="Disconnect Account"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </main>
      </div>
    </div>
    </AuthGuard>
  );
}
