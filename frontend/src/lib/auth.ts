'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { apiFetch } from '@/lib/api';

interface AuthUser {
  id: string;
  email: string;
  name: string;
  planTier: string;
}

interface AuthState {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
}

/**
 * useAuth() hook — checks localStorage for JWT and validates it against /auth/me.
 * Redirects unauthenticated users to /auth/login.
 * Use on every protected page (dashboard/*, admin).
 */
export function useAuth(): AuthState {
  const router = useRouter();
  const pathname = usePathname();
  const [state, setState] = useState<AuthState>({
    user: null,
    isLoading: true,
    isAuthenticated: false,
  });

  useEffect(() => {
    const token = localStorage.getItem('igdm_jwt');
    const cachedUser = localStorage.getItem('igdm_user');

    if (!token) {
      router.replace(`/auth/login?redirect=${encodeURIComponent(pathname)}`);
      return;
    }

    // Use cached user for instant render, then validate in background
    if (cachedUser) {
      try {
        const parsed = JSON.parse(cachedUser) as AuthUser;
        setState({ user: parsed, isLoading: false, isAuthenticated: true });
      } catch {
        // Invalid cached data, will re-fetch below
      }
    }

    // Validate token against backend
    apiFetch<AuthUser>('/auth/me')
      .then((user) => {
        localStorage.setItem('igdm_user', JSON.stringify(user));
        setState({ user, isLoading: false, isAuthenticated: true });
      })
      .catch(() => {
        localStorage.removeItem('igdm_jwt');
        localStorage.removeItem('igdm_user');
        router.replace(`/auth/login?redirect=${encodeURIComponent(pathname)}`);
      });
  }, [router, pathname]);

  return state;
}
