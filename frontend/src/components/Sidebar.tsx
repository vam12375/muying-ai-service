import { MessageCircle, BookOpen, Sparkles } from 'lucide-react';
import type { Tab } from '../types';

interface Props {
  activeTab: Tab;
  onTabChange: (tab: Tab) => void;
}

const TABS = [
  { id: 'chat'      as Tab, icon: MessageCircle, label: '智能对话' },
  { id: 'knowledge' as Tab, icon: BookOpen,       label: '知识库'   },
] satisfies { id: Tab; icon: React.ComponentType<{ size?: number; className?: string }>; label: string }[];

export function Sidebar({ activeTab, onTabChange }: Props) {
  return (
    <aside
      className="flex flex-col w-60 flex-shrink-0 h-screen"
      style={{ background: 'linear-gradient(180deg,#120d24 0%,#0d0920 100%)', borderRight: '1px solid rgba(139,92,246,0.12)' }}
    >
      {/* Logo */}
      <div className="px-5 pt-7 pb-6">
        <div className="flex items-center gap-3">
          <div
            className="w-9 h-9 rounded-xl flex items-center justify-center text-white text-base font-bold flex-shrink-0 pulse-glow"
            style={{ background: 'linear-gradient(135deg,#7c3aed,#db2777)' }}
          >
            萌
          </div>
          <div>
            <p className="text-white font-semibold text-sm leading-tight">萌宝助手</p>
            <p className="text-xs leading-tight" style={{ color: 'rgba(167,139,250,0.6)' }}>母婴 AI 客服</p>
          </div>
          <Sparkles size={14} className="ml-auto" style={{ color: '#a78bfa' }} />
        </div>
      </div>

      {/* 导航 */}
      <nav className="flex-1 px-3 space-y-1">
        {TABS.map(({ id, icon: Icon, label }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              onClick={() => onTabChange(id)}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200"
              style={
                active
                  ? { background: 'linear-gradient(135deg,rgba(124,58,237,0.3),rgba(219,39,119,0.2))', color: '#e9d5ff', border: '1px solid rgba(139,92,246,0.3)' }
                  : { color: 'rgba(148,163,184,0.7)', border: '1px solid transparent' }
              }
            >
              <Icon
                size={16}
                style={{ color: active ? '#a78bfa' : 'rgba(148,163,184,0.5)' }}
              />
              {label}
              {active && (
                <span
                  className="ml-auto w-1.5 h-1.5 rounded-full"
                  style={{ background: 'linear-gradient(135deg,#a78bfa,#f472b6)' }}
                />
              )}
            </button>
          );
        })}
      </nav>

      {/* 底部信息 */}
      <div className="px-5 py-5">
        <div
          className="rounded-xl p-3 text-xs"
          style={{ background: 'rgba(139,92,246,0.08)', border: '1px solid rgba(139,92,246,0.15)', color: 'rgba(167,139,250,0.6)' }}
        >
          <p className="font-medium mb-1" style={{ color: '#a78bfa' }}>RAG + Function Calling</p>
          <p>知识库检索增强 · 订单查询 · 物流跟踪 · 优惠推荐</p>
        </div>
      </div>
    </aside>
  );
}
