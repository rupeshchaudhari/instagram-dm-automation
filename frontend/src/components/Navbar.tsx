'use client';

import { useEffect, useState } from 'react';
import { User as UserIcon, Bell } from 'lucide-react';

export default function Navbar() {
  const [user, setUser] = useState<{ email?: string; name?: string; planTier?: string } | null>(null);

  useEffect(() => {
    const cachedUser = localStorage.getItem('igdm_user');
    if (cachedUser) {
      try {
        setUser(JSON.parse(cachedUser));
      } catch (e) {}
    }
  }, []);

  return (
    <header className="h-16 border-b border-white/10 bg-slate-900/40 backdrop-blur-md px-6 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
        <span className="text-xs text-slate-400 font-medium">Meta Graph API connected</span>
      </div>

      <div className="flex items-center gap-4">
        {/* Notification Bell */}
        <button className="p-2 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-white/5 transition-colors relative">
          <Bell className="w-4 h-4" />
        </button>

        {/* Profile Badge */}
        <div className="flex items-center gap-3 pl-3 border-l border-white/10">
          <div className="w-8 h-8 rounded-full bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center text-indigo-400 font-bold text-xs">
            {user?.name?.[0]?.toUpperCase() || user?.email?.[0]?.toUpperCase() || 'U'}
          </div>
          <div className="hidden sm:block text-left">
            <p className="text-xs font-semibold text-slate-200">{user?.name || user?.email || 'User'}</p>
            <span className="inline-block px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider bg-purple-500/20 text-purple-300 border border-purple-500/30 rounded">
              {user?.planTier || 'Pro'} Plan
            </span>
          </div>
        </div>
      </div>
    </header>
  );
}
