import { useState, useRef, useCallback } from 'react';
import type { Message } from '../types';

const uid = () => Math.random().toString(36).slice(2, 10);

export function useChat() {
  const [messages, setMessages]   = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [model, setModel]         = useState('deepseek');
  const sessionIdRef              = useRef(uid());
  const abortRef                  = useRef<AbortController | null>(null);

  const sendMessage = useCallback(async (content: string) => {
    if (!content.trim() || isLoading) return;

    const userMsg: Message = { id: uid(), role: 'user',      content: content.trim(), timestamp: Date.now() };
    const asstMsg: Message = { id: uid(), role: 'assistant', content: '',             timestamp: Date.now() };
    const asstId = asstMsg.id;

    setMessages(prev => [...prev, userMsg, asstMsg]);
    setIsLoading(true);

    const abort = new AbortController();
    abortRef.current = abort;

    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: content.trim(), sessionId: sessionIdRef.current, model }),
        signal: abort.signal,
      });

      if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`);

      const reader  = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let accumulated = '';

      outer: while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const parts = buffer.split('\n\n');
        buffer = parts.pop() ?? '';

        for (const part of parts) {
          let eventType = 'message';
          let data = '';
          for (const line of part.split('\n')) {
            if (line.startsWith('event:')) eventType = line.slice(6).trim();
            if (line.startsWith('data:'))  data      = line.slice(5).trim();
          }
          if (eventType === 'done') break outer;
          if (eventType === 'message' && data) {
            accumulated += data;
            setMessages(prev => prev.map(m => m.id === asstId ? { ...m, content: accumulated } : m));
          }
        }
      }
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') return;
      setMessages(prev => prev.map(m =>
        m.id === asstId ? { ...m, content: '抱歉，服务暂时不可用，请稍后重试。' } : m
      ));
    } finally {
      setIsLoading(false);
      abortRef.current = null;
    }
  }, [isLoading, model]);

  const stopGeneration = useCallback(() => {
    abortRef.current?.abort();
    setIsLoading(false);
  }, []);

  const clearMessages = useCallback(() => {
    setMessages([]);
    sessionIdRef.current = uid();
  }, []);

  return { messages, isLoading, model, setModel, sendMessage, stopGeneration, clearMessages };
}
