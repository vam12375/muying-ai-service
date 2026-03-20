import type { Message } from '../types';

interface Props {
  message: Message;
  isStreaming?: boolean;
}

export function MessageBubble({ message, isStreaming }: Props) {
  const isUser = message.role === 'user';
  const isEmpty = message.content === '' && isStreaming;

  return (
    <div className={`flex items-end gap-2.5 mb-5 msg-enter ${isUser ? 'flex-row-reverse' : ''}`}>
      {/* 头像 */}
      <div
        className="w-8 h-8 rounded-xl flex items-center justify-center text-white text-xs font-bold flex-shrink-0"
        style={
          isUser
            ? { background: 'linear-gradient(135deg,#7c3aed,#db2777)' }
            : { background: 'linear-gradient(135deg,#06b6d4,#6366f1)' }
        }
      >
        {isUser ? '我' : '萌'}
      </div>

      {/* 气泡 */}
      <div
        className={`relative max-w-[72%] px-4 py-3 text-sm leading-relaxed ${isUser ? 'rounded-2xl rounded-br-sm' : 'rounded-2xl rounded-bl-sm'}`}
        style={
          isUser
            ? { background: 'linear-gradient(135deg,#7c3aed,#db2777)', color: '#fff' }
            : { background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.08)', color: '#e2e8f0', backdropFilter: 'blur(8px)' }
        }
      >
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
