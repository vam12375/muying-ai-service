import type { Message } from '../types';

interface Props {
  message: Message;
  isStreaming?: boolean;
}

const STATUS_LABELS = {
  pending: '等待首个事件',
  streaming: 'SSE 生成中',
  done: 'SSE 已完成',
  error: 'SSE 异常',
} as const;

const STATUS_STYLES = {
  pending: {
    color: '#c4b5fd',
    background: 'rgba(124,58,237,0.15)',
    border: '1px solid rgba(124,58,237,0.28)',
  },
  streaming: {
    color: '#67e8f9',
    background: 'rgba(6,182,212,0.14)',
    border: '1px solid rgba(6,182,212,0.28)',
  },
  done: {
    color: '#86efac',
    background: 'rgba(34,197,94,0.14)',
    border: '1px solid rgba(34,197,94,0.24)',
  },
  error: {
    color: '#fca5a5',
    background: 'rgba(239,68,68,0.14)',
    border: '1px solid rgba(239,68,68,0.28)',
  },
} as const;

export function MessageBubble({ message, isStreaming }: Props) {
  const isUser = message.role === 'user';
  const isError = !isUser && message.status === 'error';
  const isEmpty = message.content === '' && (isStreaming || message.status === 'pending');
  const sessionId = message.metadata?.sessionId;
  const code = message.metadata?.code;
  const statusLabel = !isUser && message.status ? STATUS_LABELS[message.status] : null;
  const statusStyle = !isUser && message.status ? STATUS_STYLES[message.status] : null;

  return (
    <div className={`flex items-end gap-2.5 mb-5 msg-enter ${isUser ? 'flex-row-reverse' : ''}`}>
      <div
        className="w-8 h-8 rounded-xl flex items-center justify-center text-white text-xs font-bold flex-shrink-0"
        style={
          isUser
            ? { background: 'linear-gradient(135deg,#7c3aed,#db2777)' }
            : isError
              ? { background: 'linear-gradient(135deg,#ef4444,#f97316)' }
              : { background: 'linear-gradient(135deg,#06b6d4,#6366f1)' }
        }
      >
        {isUser ? '我' : '萌'}
      </div>

      <div
        className={`relative max-w-[72%] px-4 py-3 text-sm leading-relaxed ${isUser ? 'rounded-2xl rounded-br-sm' : 'rounded-2xl rounded-bl-sm'}`}
        style={
          isUser
            ? { background: 'linear-gradient(135deg,#7c3aed,#db2777)', color: '#fff' }
            : isError
              ? {
                  background: 'rgba(127,29,29,0.34)',
                  border: '1px solid rgba(239,68,68,0.24)',
                  color: '#fee2e2',
                  backdropFilter: 'blur(8px)',
                }
              : {
                  background: 'rgba(255,255,255,0.06)',
                  border: '1px solid rgba(255,255,255,0.08)',
                  color: '#e2e8f0',
                  backdropFilter: 'blur(8px)',
                }
        }
      >
        {!isUser && (statusLabel || sessionId || code) && (
          <div className="mb-2 flex flex-wrap items-center gap-2 text-[11px]">
            {statusLabel && statusStyle && (
              <span className="px-2 py-0.5 rounded-full font-medium" style={statusStyle}>
                {statusLabel}
              </span>
            )}
            {sessionId && <span style={{ color: 'rgba(148,163,184,0.8)' }}>sessionId：{sessionId}</span>}
            {code && <span style={{ color: 'rgba(248,113,113,0.9)' }}>code：{code}</span>}
          </div>
        )}

        {isEmpty ? (
          <span className="flex gap-1 items-center h-4">
            <span className="typing-dot" />
            <span className="typing-dot" />
            <span className="typing-dot" />
          </span>
        ) : (
          <span
            className={`whitespace-pre-wrap break-words ${isStreaming && !isUser ? 'streaming-cursor' : ''}`}
          >
            {message.content}
          </span>
        )}
      </div>
    </div>
  );
}
