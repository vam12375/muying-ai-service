import { useEffect, useRef } from 'react';
import { Trash2, Zap } from 'lucide-react';
import { useChat } from '../hooks/useChat';
import { MessageBubble } from './MessageBubble';
import { MessageInput } from './MessageInput';
import type { StreamLifecycleEvent } from '../types';

const MODELS = [
  { value: 'deepseek', label: 'DeepSeek', color: '#06b6d4' },
  { value: 'qianwen',  label: '通义千问', color: '#10b981' },
  { value: 'zhipu',    label: '智谱 GLM',  color: '#f59e0b' },
];

const SUGGESTIONS = [
  { icon: '🍼', text: '宝宝奶粉怎么冲泡比较好？' },
  { icon: '📦', text: '帮我查一下订单 ORD20240101001 的状态' },
  { icon: '🎁', text: '最近有什么优惠活动吗？' },
  { icon: '👶', text: '新生儿洗澡需要注意什么？' },
];

export function ChatPage() {
  const {
    messages,
    isLoading,
    model,
    setModel,
    sendMessage,
    stopGeneration,
    clearMessages,
    streamEvent,
    chatContext,
  } = useChat();
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamEvent]);

  return (
    <div className="flex flex-col h-screen" style={{ background: 'linear-gradient(135deg,#0f0a1e 0%,#120d24 50%,#0a0f1e 100%)' }}>
      <header
        className="flex items-center justify-between px-6 py-4 flex-shrink-0"
        style={{ borderBottom: '1px solid rgba(139,92,246,0.12)' }}
      >
        <div className="flex items-center gap-2">
          <Zap size={14} style={{ color: '#a78bfa' }} />
          <span className="text-xs font-medium" style={{ color: 'rgba(167,139,250,0.7)' }}>模型</span>
          <div className="flex gap-1.5 ml-1">
            {MODELS.map(m => (
              <button
                key={m.value}
                onClick={() => setModel(m.value)}
                className="px-3 py-1 rounded-lg text-xs font-medium transition-all duration-150"
                style={
                  model === m.value
                    ? { background: `${m.color}22`, color: m.color, border: `1px solid ${m.color}44` }
                    : { background: 'transparent', color: 'rgba(148,163,184,0.5)', border: '1px solid rgba(255,255,255,0.06)' }
                }
              >
                {m.label}
              </button>
            ))}
          </div>
        </div>

        {messages.length > 0 && (
          <button
            onClick={clearMessages}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs transition-all"
            style={{ color: 'rgba(148,163,184,0.5)', border: '1px solid rgba(255,255,255,0.06)' }}
            onMouseEnter={e => { e.currentTarget.style.color = '#fca5a5'; e.currentTarget.style.borderColor = 'rgba(239,68,68,0.3)'; }}
            onMouseLeave={e => { e.currentTarget.style.color = 'rgba(148,163,184,0.5)'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.06)'; }}
          >
            <Trash2 size={12} />
            清空对话
          </button>
        )}
      </header>

      <div className="px-6 pt-3 flex-shrink-0">
        <div className="max-w-3xl mx-auto flex flex-wrap items-center gap-2">
          <ContextChip
            label={`身份：${chatContext.loginStatus === 'LOGGED_IN' ? '已登录' : '匿名'}`}
            tone={chatContext.loginStatus === 'LOGGED_IN' ? 'green' : 'slate'}
          />
          <ContextChip label={`场景：${chatContext.scenario}`} tone="violet" />
          <ContextChip label={`用户：${chatContext.userId || '未注入'}`} tone={chatContext.userId ? 'cyan' : 'slate'} />
        </div>
        {streamEvent && <StreamEventBanner event={streamEvent} />}
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-4">
        <div className="max-w-3xl mx-auto">
          {messages.length === 0 ? (
            <WelcomeScreen onSend={sendMessage} />
          ) : (
            messages.map(msg => (
              <MessageBubble
                key={msg.id}
                message={msg}
                isStreaming={msg.role === 'assistant' && (msg.status === 'pending' || msg.status === 'streaming')}
              />
            ))
          )}
          <div ref={bottomRef} />
        </div>
      </div>

      <MessageInput onSend={sendMessage} onStop={stopGeneration} isLoading={isLoading} />
    </div>
  );
}

function ContextChip({ label, tone }: { label: string; tone: 'violet' | 'green' | 'cyan' | 'slate' }) {
  const toneStyles = {
    violet: { background: 'rgba(139,92,246,0.12)', color: '#c4b5fd', border: '1px solid rgba(139,92,246,0.26)' },
    green: { background: 'rgba(34,197,94,0.12)', color: '#86efac', border: '1px solid rgba(34,197,94,0.24)' },
    cyan: { background: 'rgba(6,182,212,0.12)', color: '#67e8f9', border: '1px solid rgba(6,182,212,0.24)' },
    slate: { background: 'rgba(148,163,184,0.10)', color: '#cbd5e1', border: '1px solid rgba(148,163,184,0.20)' },
  } as const;

  return (
    <span className="px-3 py-1 rounded-full text-xs font-medium" style={toneStyles[tone]}>
      {label}
    </span>
  );
}

function StreamEventBanner({ event }: { event: StreamLifecycleEvent }) {
  const tone = event.type === 'error'
    ? {
        title: 'SSE 错误事件',
        background: 'rgba(127,29,29,0.34)',
        border: '1px solid rgba(239,68,68,0.22)',
        titleColor: '#fca5a5',
      }
    : event.type === 'done'
      ? {
          title: 'SSE 完成事件',
          background: 'rgba(20,83,45,0.24)',
          border: '1px solid rgba(34,197,94,0.22)',
          titleColor: '#86efac',
        }
      : {
          title: 'SSE 启动事件',
          background: 'rgba(30,41,59,0.28)',
          border: '1px solid rgba(6,182,212,0.20)',
          titleColor: '#67e8f9',
        };

  return (
    <div
      className="mt-3 rounded-2xl px-4 py-3"
      style={{ background: tone.background, border: tone.border, backdropFilter: 'blur(8px)' }}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs font-semibold" style={{ color: tone.titleColor }}>{tone.title}</p>
        <span className="text-[11px]" style={{ color: 'rgba(148,163,184,0.72)' }}>
          {new Date(event.timestamp).toLocaleTimeString('zh-CN', { hour12: false })}
        </span>
      </div>
      <p className="mt-1 text-sm" style={{ color: 'rgba(226,232,240,0.88)' }}>{event.message}</p>
      {(event.metadata?.sessionId || event.metadata?.code) && (
        <div className="mt-2 flex flex-wrap gap-3 text-[11px]" style={{ color: 'rgba(148,163,184,0.78)' }}>
          {event.metadata?.sessionId && <span>sessionId：{event.metadata.sessionId}</span>}
          {event.metadata?.code && <span>code：{event.metadata.code}</span>}
        </div>
      )}
    </div>
  );
}

function WelcomeScreen({ onSend }: { onSend: (s: string) => void }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] text-center">
      <div
        className="w-20 h-20 rounded-3xl flex items-center justify-center text-4xl mb-6 float-anim"
        style={{ background: 'linear-gradient(135deg,rgba(124,58,237,0.2),rgba(219,39,119,0.2))', border: '1px solid rgba(139,92,246,0.3)' }}
      >
        👶
      </div>

      <h2 className="text-2xl font-bold mb-2 gradient-text">萌宝助手，你好！</h2>
      <p className="text-sm mb-10 max-w-sm" style={{ color: 'rgba(148,163,184,0.6)' }}>
        我能回答母婴问题、查询订单物流、介绍优惠活动
      </p>

      <div className="flex gap-2 mb-10 flex-wrap justify-center">
        {['RAG 知识库', 'Function Calling', '多模型路由', '对话记忆'].map(tag => (
          <span
            key={tag}
            className="px-3 py-1 rounded-full text-xs font-medium"
            style={{ background: 'rgba(139,92,246,0.1)', color: '#a78bfa', border: '1px solid rgba(139,92,246,0.2)' }}
          >
            {tag}
          </span>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-3 max-w-xl w-full">
        {SUGGESTIONS.map(s => (
          <button
            key={s.text}
            onClick={() => onSend(s.text)}
            className="flex items-start gap-3 p-4 rounded-2xl text-left text-sm card-hover"
            style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)', color: '#e2e8f0' }}
          >
            <span className="text-xl flex-shrink-0 mt-0.5">{s.icon}</span>
            <span style={{ color: 'rgba(226,232,240,0.8)' }}>{s.text}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
