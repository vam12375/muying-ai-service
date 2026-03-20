import { useState, useRef, useEffect, type KeyboardEvent } from 'react';
import { Send, Square } from 'lucide-react';

interface Props {
  onSend: (message: string) => void;
  onStop: () => void;
  isLoading: boolean;
}

export function MessageInput({ onSend, onStop, isLoading }: Props) {
  const [input, setInput] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 160) + 'px';
  }, [input]);

  const handleSend = () => {
    if (input.trim() && !isLoading) {
      onSend(input);
      setInput('');
    }
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div
      className="px-6 pb-6 pt-3"
      style={{ background: 'linear-gradient(0deg, rgba(15,10,30,1) 0%, rgba(15,10,30,0) 100%)' }}
    >
      <div
        className="flex items-end gap-3 max-w-3xl mx-auto rounded-2xl px-4 py-3"
        style={{ background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(139,92,246,0.2)', backdropFilter: 'blur(16px)' }}
      >
        <textarea
          ref={textareaRef}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入问题，如：宝宝奶粉怎么冲泡？（Enter 发送，Shift+Enter 换行）"
          rows={1}
          disabled={isLoading}
          className="flex-1 resize-none bg-transparent text-sm outline-none py-1"
          style={{ color: '#e2e8f0', caretColor: '#a78bfa', maxHeight: '160px' }}
        />

        {isLoading ? (
          <button
            onClick={onStop}
            className="flex-shrink-0 w-9 h-9 rounded-xl flex items-center justify-center transition-all"
            style={{ background: 'rgba(239,68,68,0.2)', border: '1px solid rgba(239,68,68,0.4)', color: '#fca5a5' }}
          >
            <Square size={14} fill="currentColor" />
          </button>
        ) : (
          <button
            onClick={handleSend}
            disabled={!input.trim()}
            className="flex-shrink-0 w-9 h-9 rounded-xl flex items-center justify-center btn-primary text-white disabled:opacity-30 disabled:cursor-not-allowed disabled:transform-none"
          >
            <Send size={14} />
          </button>
        )}
      </div>
      <p className="text-center mt-2 text-xs" style={{ color: 'rgba(148,163,184,0.3)' }}>
        AI 生成内容仅供参考，涉及健康问题请咨询专业医生
      </p>
    </div>
  );
}
