'use client';

import { useEffect, useState } from 'react';
import Sidebar from '@/components/Sidebar';
import Navbar from '@/components/Navbar';
import { apiFetch } from '@/lib/api';
import { Zap, Plus, ToggleLeft, ToggleRight, Trash2, Edit3, X, HelpCircle } from 'lucide-react';

interface Rule {
  id: string;
  name: string;
  triggerType: string;
  triggerKeywords: string[];
  dmTemplate: string;
  isActive: boolean;
  dailyDmLimit: number;
  dmSentToday: number;
  igUsername: string;
}

export default function RulesPage() {
  const [rules, setRules] = useState<Rule[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [triggerType, setTriggerType] = useState('keyword');
  const [keywordInput, setKeywordInput] = useState('');
  const [keywords, setKeywords] = useState<string[]>([]);
  const [dmTemplate, setDmTemplate] = useState('Hi {{username}}! Thanks for your comment. Here is your link: https://example.com');
  const [dailyDmLimit, setDailyDmLimit] = useState(100);
  const [submitting, setSubmitting] = useState(false);

  const loadRules = async () => {
    try {
      const data = await apiFetch<{ rules: Rule[] }>('/rules');
      setRules(data.rules || []);
    } catch (err) {
      console.error('Failed to load rules', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRules();
  }, []);

  const handleAddKeyword = () => {
    if (keywordInput.trim() && !keywords.includes(keywordInput.trim())) {
      setKeywords([...keywords, keywordInput.trim().toLowerCase()]);
      setKeywordInput('');
    }
  };

  const handleRemoveKeyword = (kw: string) => {
    setKeywords(keywords.filter((k) => k !== kw));
  };

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      await apiFetch('/rules', {
        method: 'POST',
        body: JSON.stringify({
          name,
          triggerType,
          triggerKeywords: keywords,
          dmTemplate,
          dailyDmLimit,
        }),
      });

      setShowModal(false);
      setName('');
      setKeywords([]);
      loadRules();
    } catch (err: any) {
      alert(err.message || 'Failed to create rule');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleRule = async (id: string) => {
    try {
      await apiFetch(`/rules/${id}/toggle`, { method: 'PATCH' });
      loadRules();
    } catch (err: any) {
      alert(err.message || 'Failed to toggle rule');
    }
  };

  const handleDeleteRule = async (id: string) => {
    if (!confirm('Are you sure you want to delete this rule?')) return;
    try {
      await apiFetch(`/rules/${id}`, { method: 'DELETE' });
      loadRules();
    } catch (err: any) {
      alert(err.message || 'Failed to delete rule');
    }
  };

  const insertVariable = (varName: string) => {
    setDmTemplate((prev) => `${prev} {{${varName}}}`);
  };

  return (
    <div className="flex min-h-screen bg-[#090d16]">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar />

        <main className="p-6 space-y-6 flex-1 overflow-y-auto">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
            <div>
              <h1 className="text-2xl font-bold text-slate-100">Automation Rules</h1>
              <p className="text-xs text-slate-400">Define comment keyword triggers and automated DM responses</p>
            </div>

            <button
              onClick={() => setShowModal(true)}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 text-white text-xs font-semibold hover:opacity-95 transition-opacity shadow-lg shadow-indigo-500/25"
            >
              <Plus className="w-4 h-4" />
              New Automation Rule
            </button>
          </div>

          {loading ? (
            <div className="py-12 text-center text-xs text-slate-500">Loading rules...</div>
          ) : rules.length === 0 ? (
            <div className="glass-card p-12 rounded-2xl text-center space-y-3 max-w-lg mx-auto mt-8 border border-white/10">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center mx-auto text-white shadow-xl shadow-indigo-500/30">
                <Zap className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-100">No Automation Rules Configured</h3>
              <p className="text-xs text-slate-400 leading-relaxed">
                Create a trigger rule to match keywords like "link", "discount", or "info" and send instant DMs to users who comment on your posts.
              </p>
              <button
                onClick={() => setShowModal(true)}
                className="mt-4 px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors shadow-lg shadow-indigo-600/30"
              >
                Create Your First Rule
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {rules.map((rule) => (
                <div key={rule.id} className="glass-card glass-card-hover p-5 rounded-2xl flex flex-col justify-between border border-white/10 space-y-4">
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <h4 className="font-bold text-slate-100 text-base">{rule.name}</h4>
                      <button
                        onClick={() => handleToggleRule(rule.id)}
                        className={`text-2xl transition-colors ${rule.isActive ? 'text-emerald-400' : 'text-slate-600'}`}
                      >
                        {rule.isActive ? <ToggleRight className="w-8 h-8" /> : <ToggleLeft className="w-8 h-8" />}
                      </button>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                        {rule.triggerType}
                      </span>
                      <span className="text-xs text-slate-400">@{rule.igUsername || 'connected'}</span>
                    </div>

                    {/* Keywords Tag Cloud */}
                    {rule.triggerKeywords && rule.triggerKeywords.length > 0 && (
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        {rule.triggerKeywords.map((kw) => (
                          <span key={kw} className="px-2 py-0.5 rounded-md bg-white/5 border border-white/10 text-xs text-purple-300 font-mono">
                            #{kw}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* Template Content */}
                    <div className="p-3 rounded-xl bg-slate-900/60 border border-white/5 text-xs text-slate-300 font-mono leading-relaxed mt-2">
                      "{rule.dmTemplate}"
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-white/5 text-xs text-slate-400">
                    <span>
                      Sent Today: <strong className="text-slate-200">{rule.dmSentToday}</strong> / {rule.dailyDmLimit}
                    </span>
                    <button
                      onClick={() => handleDeleteRule(rule.id)}
                      className="p-1.5 text-slate-500 hover:text-rose-400 rounded-lg transition-colors"
                      title="Delete Rule"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Modal */}
          {showModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
              <div className="glass-card w-full max-w-lg p-6 rounded-2xl border border-white/10 shadow-2xl space-y-5">
                <div className="flex items-center justify-between pb-3 border-b border-white/10">
                  <h3 className="text-lg font-bold text-slate-100">Create Automation Rule</h3>
                  <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-white">
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <form onSubmit={handleCreateRule} className="space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">Rule Campaign Name</label>
                    <input
                      type="text"
                      required
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="e.g. Summer Promo Link"
                      className="w-full bg-slate-900/60 border border-white/10 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">Trigger Condition</label>
                    <select
                      value={triggerType}
                      onChange={(e) => setTriggerType(e.target.value)}
                      className="w-full bg-slate-900 border border-white/10 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
                    >
                      <option value="keyword">Contains Trigger Keyword(s)</option>
                      <option value="any_comment">Any Comment</option>
                      <option value="first_comment">First Comment Only</option>
                    </select>
                  </div>

                  {triggerType === 'keyword' && (
                    <div>
                      <label className="block text-xs font-medium text-slate-300 mb-1.5">Trigger Keywords</label>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={keywordInput}
                          onChange={(e) => setKeywordInput(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                              e.preventDefault();
                              handleAddKeyword();
                            }
                          }}
                          placeholder="Type keyword & press Add..."
                          className="flex-1 bg-slate-900/60 border border-white/10 rounded-xl px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
                        />
                        <button
                          type="button"
                          onClick={handleAddKeyword}
                          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold"
                        >
                          Add
                        </button>
                      </div>

                      {keywords.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 mt-2">
                          {keywords.map((kw) => (
                            <span key={kw} className="px-2.5 py-1 rounded-lg bg-indigo-500/20 border border-indigo-500/30 text-xs text-indigo-300 flex items-center gap-1">
                              #{kw}
                              <button type="button" onClick={() => handleRemoveKeyword(kw)} className="text-indigo-400 hover:text-white">
                                <X className="w-3 h-3" />
                              </button>
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  <div>
                    <div className="flex items-center justify-between mb-1.5">
                      <label className="block text-xs font-medium text-slate-300">Automated DM Message Template</label>
                      <div className="flex gap-1">
                        <button
                          type="button"
                          onClick={() => insertVariable('username')}
                          className="px-2 py-0.5 text-[10px] rounded bg-purple-500/20 text-purple-300 border border-purple-500/30 font-mono"
                        >
                          + {"{{username}}"}
                        </button>
                      </div>
                    </div>
                    <textarea
                      required
                      rows={3}
                      value={dmTemplate}
                      onChange={(e) => setDmTemplate(e.target.value)}
                      className="w-full bg-slate-900/60 border border-white/10 rounded-xl p-3 text-xs font-mono text-slate-200 focus:outline-none focus:border-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1.5">Daily DM Limit ({dailyDmLimit})</label>
                    <input
                      type="range"
                      min={10}
                      max={500}
                      value={dailyDmLimit}
                      onChange={(e) => setDailyDmLimit(Number(e.target.value))}
                      className="w-full text-indigo-500"
                    />
                  </div>

                  <div className="pt-3 border-t border-white/10 flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setShowModal(false)}
                      className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={submitting}
                      className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/30"
                    >
                      {submitting ? 'Creating...' : 'Save & Enable Rule'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
