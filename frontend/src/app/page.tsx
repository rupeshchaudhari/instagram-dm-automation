'use client';

import Link from 'next/link';
import { Zap, ArrowRight, ShieldCheck, Cpu, MessageSquare, BarChart3, Layers, CheckCircle2 } from 'lucide-react';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-[#090d16] text-slate-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      {/* Header / Navbar */}
      <header className="border-b border-white/5 backdrop-blur-xl bg-slate-950/40 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg shadow-indigo-500/25">
              <Zap className="w-6 h-6 text-white" />
            </div>
            <div>
              <span className="font-bold text-xl tracking-tight gradient-text">InstaAuto DM</span>
              <span className="hidden sm:inline-block text-[10px] ml-2 px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-mono">v1.0 SaaS</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <Link
              href="/auth/login"
              className="text-sm font-medium text-slate-300 hover:text-white transition-colors px-4 py-2"
            >
              Sign In
            </Link>
            <Link
              href="/auth/register"
              className="text-sm font-semibold text-white px-5 py-2.5 rounded-xl bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 hover:opacity-95 transition-opacity shadow-lg shadow-indigo-500/20 flex items-center gap-2"
            >
              Get Started Free
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1">
        {/* Hero Section */}
        <section className="relative pt-20 pb-24 overflow-hidden border-b border-white/5">
          {/* Background Glow Orbs */}
          <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-indigo-600/15 rounded-full blur-[140px] pointer-events-none" />
          <div className="absolute top-1/3 right-1/4 w-[400px] h-[250px] bg-pink-600/10 rounded-full blur-[120px] pointer-events-none" />

          <div className="max-w-5xl mx-auto px-6 text-center space-y-8 relative z-10">
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/5 border border-white/10 backdrop-blur-md">
              <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
              <span className="text-xs font-medium text-slate-300">Official Meta Graph API Integration</span>
            </div>

            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-slate-100 leading-tight">
              Turn Instagram Comments Into <br />
              <span className="gradient-text">Instant DM Conversions</span>
            </h1>

            <p className="text-base sm:text-xl text-slate-400 max-w-3xl mx-auto font-normal leading-relaxed">
              Automate direct messages when users comment targeted keywords on your posts, reels, or ads. 
              Enforce rate limits, respect Meta&apos;s 24-hour window, and double your sales leads.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
              <Link
                href="/auth/register"
                className="w-full sm:w-auto px-8 py-4 rounded-xl bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 text-white font-semibold text-base shadow-xl shadow-indigo-500/25 hover:opacity-95 transition-opacity flex items-center justify-center gap-2"
              >
                Launch Automation Free
                <ArrowRight className="w-5 h-5" />
              </Link>
              <Link
                href="/dashboard"
                className="w-full sm:w-auto px-8 py-4 rounded-xl bg-slate-900/80 hover:bg-slate-900 border border-white/10 text-slate-300 font-semibold text-base transition-colors flex items-center justify-center gap-2"
              >
                View Live Demo Dashboard
              </Link>
            </div>

            {/* Key Feature Badges */}
            <div className="pt-8 flex flex-wrap items-center justify-center gap-6 text-xs text-slate-400 border-t border-white/5 max-w-3xl mx-auto">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>&lt; 2s Webhook Response</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-indigo-400" />
                <span>AES-256 Encrypted Tokens</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-purple-400" />
                <span>RabbitMQ Backoff Retries</span>
              </div>
            </div>
          </div>
        </section>

        {/* Feature Grid */}
        <section className="py-20 max-w-7xl mx-auto px-6 space-y-12">
          <div className="text-center space-y-3">
            <h2 className="text-2xl sm:text-3xl font-bold text-slate-100">Built for Enterprise Scale & Speed</h2>
            <p className="text-sm text-slate-400 max-w-xl mx-auto">
              Engineered with asynchronous microservice architecture to process high-volume comment campaigns smoothly.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="glass-card p-6 rounded-2xl space-y-4 hover:border-indigo-500/40 transition-colors">
              <div className="w-12 h-12 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center">
                <MessageSquare className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-200">Keyword Trigger Builder</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Define custom keyword rules (&quot;link&quot;, &quot;discount&quot;, &quot;free&quot;) with personalized message templates and variable tags like <code className="text-indigo-300 bg-slate-900 px-1 py-0.5 rounded font-mono">{"{{username}}"}</code>.
              </p>
            </div>

            <div className="glass-card p-6 rounded-2xl space-y-4 hover:border-purple-500/40 transition-colors">
              <div className="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-200">Rate Limit Protection</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Automatic parsing of Meta&apos;s <code className="text-purple-300 bg-slate-900 px-1 py-0.5 rounded font-mono">x-app-usage</code> headers. Proactively throttles sending at 80% capacity to safeguard your accounts.
              </p>
            </div>

            <div className="glass-card p-6 rounded-2xl space-y-4 hover:border-pink-500/40 transition-colors">
              <div className="w-12 h-12 rounded-xl bg-pink-500/10 text-pink-400 flex items-center justify-center">
                <BarChart3 className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-200">Audit & Analytics Feed</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Track every message sent, skipped, or rate-limited in real time. Monitor daily caps, conversion metrics, and multi-tenant user accounts.
              </p>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-white/5 py-8 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Zap className="w-4 h-4 text-indigo-400" />
            <span className="font-semibold text-slate-400">InstaAuto DM SaaS</span>
          </div>
          <p>© 2026 InstaAuto DM. All rights reserved. Meta Graph API Compliant.</p>
        </div>
      </footer>
    </div>
  );
}
