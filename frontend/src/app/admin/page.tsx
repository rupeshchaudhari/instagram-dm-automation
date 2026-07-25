'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/Sidebar';
import Navbar from '@/components/Navbar';
import { apiFetch } from '@/lib/api';
import { Users, Shield, Cpu, Activity, Zap, CheckCircle2, AlertTriangle, Layers, ArrowUpRight } from 'lucide-react';

interface PlatformStats {
  totalUsers: number;
  totalConnectedAccounts: number;
  totalActiveRules: number;
  totalDmsSentPlatformWide: number;
  rateLimit: {
    callCountPercent: number;
    cpuTimePercent: number;
    totalTimePercent: number;
    isThrottled: boolean;
    throttledUntil: string | null;
  };
}

interface UserItem {
  id: string;
  email: string;
  name: string;
  planTier: string;
  isActive: boolean;
  accountCount: number;
  createdAt: string;
}

export default function AdminPortalPage() {
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [users, setUsers] = useState<UserItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');

  const loadAdminData = async () => {
    try {
      const statsData = await apiFetch<PlatformStats>('/admin/stats');
      setStats(statsData);

      const usersData = await apiFetch<{ users: UserItem[] }>('/admin/users');
      setUsers(usersData.users || []);
    } catch (err) {
      console.error('Failed to load admin data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, []);

  const handlePlanChange = async (userId: string, newPlan: string) => {
    try {
      await apiFetch(`/admin/users/${userId}/plan`, {
        method: 'PATCH',
        body: JSON.stringify({ planTier: newPlan }),
      });
      loadAdminData();
    } catch (err: any) {
      alert(err.message || 'Failed to update plan');
    }
  };

  const handleToggleUserStatus = async (userId: string) => {
    try {
      await apiFetch(`/admin/users/${userId}/toggle`, { method: 'PATCH' });
      loadAdminData();
    } catch (err: any) {
      alert(err.message || 'Failed to toggle user status');
    }
  };

  const filteredUsers = users.filter(
    (u) =>
      u.email.toLowerCase().includes(filter.toLowerCase()) ||
      u.name.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="flex min-h-screen bg-[#090d16]">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar />

        <main className="p-6 space-y-6 flex-1 overflow-y-auto">
          {/* Admin Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
            <div>
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-rose-500/20 text-rose-400 border border-rose-500/30">
                  Super Admin
                </span>
                <h1 className="text-2xl font-bold text-slate-100">Platform Control Portal</h1>
              </div>
              <p className="text-xs text-slate-400 mt-1">
                Global SaaS multi-tenant metrics, user plan management, and Meta API rate limit health
              </p>
            </div>
          </div>

          {/* Platform KPI Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Total Registered Users</span>
                <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                  <Users className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.totalUsers ?? 0}</p>
              <span className="text-[10px] text-slate-400 font-medium mt-1 block">SaaS tenants</span>
            </div>

            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Connected IG Accounts</span>
                <div className="p-2 rounded-xl bg-purple-500/10 text-purple-400">
                  <Layers className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.totalConnectedAccounts ?? 0}</p>
              <span className="text-[10px] text-purple-400 font-medium mt-1 block">OAuth active tokens</span>
            </div>

            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Active Rules System-Wide</span>
                <div className="p-2 rounded-xl bg-pink-500/10 text-pink-400">
                  <Zap className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.totalActiveRules ?? 0}</p>
              <span className="text-[10px] text-slate-400 font-medium mt-1 block">Live campaigns</span>
            </div>

            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Platform-Wide DMs Sent</span>
                <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                  <Activity className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.totalDmsSentPlatformWide ?? 0}</p>
              <span className="text-[10px] text-emerald-400 font-medium mt-1 block">All-time volume</span>
            </div>
          </div>

          {/* Meta API Rate Limit Meter Card */}
          <div className="glass-card p-5 rounded-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Cpu className="w-4 h-4 text-indigo-400" />
                <h3 className="text-sm font-semibold text-slate-200">Meta Graph API Rate Limit Health (`x-app-usage`)</h3>
              </div>

              {stats?.rateLimit?.isThrottled ? (
                <span className="px-2.5 py-1 rounded text-[10px] font-bold uppercase tracking-wider bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center gap-1">
                  <AlertTriangle className="w-3 h-3" /> Throttled by Meta
                </span>
              ) : (
                <span className="px-2.5 py-1 rounded text-[10px] font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Rate Limits Normal
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
              <div className="bg-slate-900/60 p-4 rounded-xl border border-white/5 space-y-2">
                <div className="flex justify-between text-xs text-slate-400 font-medium">
                  <span>Call Count Usage</span>
                  <span className="text-slate-200 font-bold">{stats?.rateLimit?.callCountPercent ?? 0}%</span>
                </div>
                <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                  <div
                    className="bg-indigo-500 h-full transition-all duration-300"
                    style={{ width: `${Math.min(stats?.rateLimit?.callCountPercent ?? 0, 100)}%` }}
                  />
                </div>
              </div>

              <div className="bg-slate-900/60 p-4 rounded-xl border border-white/5 space-y-2">
                <div className="flex justify-between text-xs text-slate-400 font-medium">
                  <span>CPU Time Usage</span>
                  <span className="text-slate-200 font-bold">{stats?.rateLimit?.cpuTimePercent ?? 0}%</span>
                </div>
                <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                  <div
                    className="bg-purple-500 h-full transition-all duration-300"
                    style={{ width: `${Math.min(stats?.rateLimit?.cpuTimePercent ?? 0, 100)}%` }}
                  />
                </div>
              </div>

              <div className="bg-slate-900/60 p-4 rounded-xl border border-white/5 space-y-2">
                <div className="flex justify-between text-xs text-slate-400 font-medium">
                  <span>Total Time Window</span>
                  <span className="text-slate-200 font-bold">{stats?.rateLimit?.totalTimePercent ?? 0}%</span>
                </div>
                <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                  <div
                    className="bg-pink-500 h-full transition-all duration-300"
                    style={{ width: `${Math.min(stats?.rateLimit?.totalTimePercent ?? 0, 100)}%` }}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* User Management Section */}
          <div className="glass-card p-5 rounded-2xl space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-semibold text-slate-200">SaaS User Directory & Subscription Management</h3>
                <p className="text-xs text-slate-400">Upgrade tenant plan tiers and manage account access</p>
              </div>

              <input
                type="text"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Search user email or name..."
                className="bg-slate-900/60 border border-white/10 rounded-xl px-3.5 py-1.5 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-indigo-500"
              />
            </div>

            {loading ? (
              <div className="py-8 text-center text-xs text-slate-500">Loading user directory...</div>
            ) : filteredUsers.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-400">No users found.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="text-slate-400 border-b border-white/5 uppercase text-[10px] tracking-wider">
                    <tr>
                      <th className="pb-3 font-semibold">User</th>
                      <th className="pb-3 font-semibold">Plan Tier</th>
                      <th className="pb-3 font-semibold">IG Accounts</th>
                      <th className="pb-3 font-semibold">Account Status</th>
                      <th className="pb-3 font-semibold">Registered Date</th>
                      <th className="pb-3 font-semibold text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 text-slate-300">
                    {filteredUsers.map((u) => (
                      <tr key={u.id} className="hover:bg-white/[0.02]">
                        <td className="py-3">
                          <div className="font-semibold text-slate-200">{u.name}</div>
                          <div className="text-slate-400 text-[11px] font-mono">{u.email}</div>
                        </td>
                        <td className="py-3">
                          <select
                            value={u.planTier}
                            onChange={(e) => handlePlanChange(u.id, e.target.value)}
                            className="bg-slate-900 border border-white/10 rounded-lg px-2.5 py-1 text-xs text-purple-300 font-medium focus:outline-none focus:border-indigo-500"
                          >
                            <option value="free">Free Tier</option>
                            <option value="pro">Pro Plan</option>
                            <option value="enterprise">Enterprise</option>
                          </select>
                        </td>
                        <td className="py-3 font-medium text-slate-300">{u.accountCount} connected</td>
                        <td className="py-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                            u.isActive ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                          }`}>
                            {u.isActive ? 'Active' : 'Suspended'}
                          </span>
                        </td>
                        <td className="py-3 text-slate-400">
                          {new Date(u.createdAt).toLocaleDateString()}
                        </td>
                        <td className="py-3 text-right">
                          <button
                            onClick={() => handleToggleUserStatus(u.id)}
                            className={`px-3 py-1 rounded-lg text-xs font-semibold transition-colors ${
                              u.isActive
                                ? 'bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 border border-rose-500/30'
                                : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 border border-emerald-500/30'
                            }`}
                          >
                            {u.isActive ? 'Suspend User' : 'Reactivate'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
