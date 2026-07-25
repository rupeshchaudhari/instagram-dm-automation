'use client';

import { useAuth } from '@/lib/auth';
import { Zap } from 'lucide-react';

/**
 * AuthGuard — wraps protected pages with authentication check.
 * Shows a branded loading skeleton while validating JWT.
 * Redirects to /auth/login if unauthenticated.
 */
export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const { isLoading, isAuthenticated } = useAuth();

  if (isLoading || !isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#090d16]">
        <div className="flex flex-col items-center gap-4 animate-pulse">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-pink-500 flex items-center justify-center shadow-xl shadow-indigo-500/30">
            <Zap className="w-6 h-6 text-white" />
          </div>
          <div className="space-y-2 text-center">
            <div className="h-3 w-32 bg-slate-800 rounded-full mx-auto" />
            <div className="h-2 w-24 bg-slate-800/60 rounded-full mx-auto" />
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
