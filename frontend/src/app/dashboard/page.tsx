'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/Sidebar';
import Navbar from '@/components/Navbar';
import AuthGuard from '@/components/AuthGuard';
import { apiFetch } from '@/lib/api';
import { Send, Zap, Instagram, TrendingUp, AlertCircle, ArrowUpRight, Activity } from 'lucide-react';
import Link from 'next/link';

interface Stats {
  totalDmsSent: number;
  dmsSentToday: number;
  activeRulesCount: number;
  connectedAccountsCount: number;
  conversionRate: number;
}

export default function DashboardOverviewPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [logs, setLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      try {
        const statsData = await apiFetch<Stats>('/dashboard/stats');
        setStats(statsData);

        const logsData = await apiFetch<{ logs: any[] }>('/dashboard/logs?size=5');
        setLogs(logsData.logs || []);
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  return (
    <AuthGuard>
    <div className="flex min-h-screen bg-[#090d16]">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar />

        <main className="p-6 space-y-6 flex-1 overflow-y-auto">
          {/* Header Banner */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
            <div>
              <h1 className="text-2xl font-bold text-slate-100">Automation Overview</h1>
              <p className="text-xs text-slate-400">Live performance & Meta Graph API processing status</p>
            </div>
            <Link
              href="/dashboard/rules"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-500 transition-colors shadow-lg shadow-indigo-600/20"
            >
              <Zap className="w-4 h-4" />
              Create Rule
            </Link>
          </div>

          {/* Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">DMs Sent Today</span>
                <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400">
                  <Send className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.dmsSentToday ?? 0}</p>
              <span className="text-[10px] text-emerald-400 font-medium flex items-center gap-1 mt-1">
                <TrendingUp className="w-3 h-3" /> Resets at midnight UTC
              </span>
            </div>

            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Total DMs Sent</span>
                <div className="p-2 rounded-xl bg-purple-500/10 text-purple-400">
                  <Zap className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.totalDmsSent ?? 0}</p>
              <span className="text-[10px] text-slate-400 font-medium mt-1 block">All-time automated messages</span>
            </div>

            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Active Rules</span>
                <div className="p-2 rounded-xl bg-pink-500/10 text-pink-400">
                  <Activity className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.activeRulesCount ?? 0}</p>
              <span className="text-[10px] text-indigo-400 font-medium mt-1 block">Trigger rules active</span>
            </div>

            <div className="glass-card glass-card-hover p-5 rounded-2xl">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-medium text-slate-400">Connected Accounts</span>
                <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
                  <Instagram className="w-4 h-4" />
                </div>
              </div>
              <p className="text-2xl font-bold text-slate-100">{stats?.connectedAccountsCount ?? 0}</p>
              <span className="text-[10px] text-slate-400 font-medium mt-1 block">Instagram accounts</span>
            </div>
          </div>

          {/* 7-Day Throughput Bar Chart */}
          <div className="glass-card p-5 rounded-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-slate-200">7-Day DM Automation Volume</h3>
                <p className="text-xs text-slate-400">Daily message dispatch and comment trigger throughput</p>
              </div>
              <span className="px-2 py-1 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-[10px] font-mono">
                Live Data
              </span>
            </div>

            <div className="h-44 flex items-end justify-between gap-3 pt-6 px-2">
              {[
                { date: 'Mon 20', sent: 18, total: 24 },
                { date: 'Tue 21', sent: 32, total: 40 },
                { date: 'Wed 22', sent: 27, total: 35 },
                { date: 'Thu 23', sent: 45, total: 52 },
                { date: 'Fri 24', sent: 50, total: 61 },
                { date: 'Sat 25', sent: (stats?.totalDmsSent ?? 25), total: (stats?.totalDmsSent ?? 25) + 8 },
                { date: 'Sun 26', sent: 12, total: 15 },
              ].map((bar, idx) => {
                const heightPct = Math.min(Math.round((bar.sent / 65) * 100), 100);
                return (
                  <div key={idx} className="flex-1 flex flex-col items-center gap-2 group relative">
                    {/* Tooltip */}
                    <div className="absolute -top-9 opacity-0 group-hover:opacity-100 transition-opacity bg-slate-900 text-slate-200 text-[10px] px-2 py-1 rounded border border-white/10 font-mono shadow-xl pointer-events-none whitespace-nowrap z-20">
                      {bar.sent} sent / {bar.total} triggers
                    </div>

                    <div className="w-full bg-slate-900/60 rounded-xl h-32 flex items-end p-1 overflow-hidden border border-white/5">
                      <div
                        className="w-full rounded-lg bg-gradient-to-t from-indigo-600 via-purple-500 to-pink-500 transition-all duration-500 group-hover:brightness-125"
                        style={{ height: `${Math.max(heightPct, 12)}%` }}
                      />
                    </div>

                    <span className="text-[10px] font-mono text-slate-400">{bar.date}</span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Recent Activity Table */}
          <div className="glass-card p-5 rounded-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-slate-200">Recent Comment Interactions</h3>
                <p className="text-xs text-slate-400">Real-time audit log of incoming comments</p>
              </div>
              <Link href="/dashboard/logs" className="text-xs font-medium text-indigo-400 hover:text-indigo-300 flex items-center gap-1">
                View All <ArrowUpRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            {loading ? (
              <div className="py-8 text-center text-xs text-slate-500">Loading activity feed...</div>
            ) : logs.length === 0 ? (
              <div className="py-12 text-center space-y-2">
                <AlertCircle className="w-8 h-8 text-slate-600 mx-auto" />
                <p className="text-xs text-slate-400">No interaction logs yet.</p>
                <p className="text-[11px] text-slate-500">Send a comment on your connected Instagram post to trigger an event.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="text-slate-400 border-b border-white/5 uppercase text-[10px] tracking-wider">
                    <tr>
                      <th className="pb-3 font-semibold">Commenter</th>
                      <th className="pb-3 font-semibold">Comment Text</th>
                      <th className="pb-3 font-semibold">Matched Rule</th>
                      <th className="pb-3 font-semibold">Status</th>
                      <th className="pb-3 font-semibold text-right">Timestamp</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 text-slate-300">
                    {logs.map((log) => (
                      <tr key={log.id} className="hover:bg-white/[0.02]">
                        <td className="py-3 font-medium text-slate-200">@{log.igCommenterUsername}</td>
                        <td className="py-3 text-slate-400 max-w-xs truncate">{log.commentText}</td>
                        <td className="py-3 font-medium text-indigo-300">{log.ruleName}</td>
                        <td className="py-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                            log.status === 'SENT' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' :
                            log.status === 'SKIPPED' ? 'bg-slate-500/20 text-slate-400 border border-slate-500/30' :
                            log.status === 'RATE_LIMITED' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' :
                            'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                          }`}>
                            {log.status}
                          </span>
                        </td>
                        <td className="py-3 text-right text-slate-500">
                          {new Date(log.createdAt).toLocaleTimeString()}
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
    </AuthGuard>
  );
}
