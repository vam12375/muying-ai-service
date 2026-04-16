import { useState, useRef, useCallback } from 'react';
import type {
  ChatContext,
  ChatRequest,
  Message,
  StreamEventType,
  StreamLifecycleEvent,
} from '../types';

const CHAT_USER_ID_STORAGE = 'ai-chat-user-id';
const CHAT_LOGIN_STATUS_STORAGE = 'ai-chat-login-status';
const CHAT_SCENARIO_STORAGE = 'ai-chat-scenario';
const DEFAULT_SCENARIO = 'GENERAL_ASSISTANT';
const DEFAULT_ERROR_MESSAGE = '抱歉，服务暂时不可用，请稍后重试。';

const uid = () => Math.random().toString(36).slice(2, 10);

function getChatContext(): ChatContext {
  if (typeof window === 'undefined') {
    return { userId: '', loginStatus: 'ANONYMOUS', scenario: DEFAULT_SCENARIO };
  }

  const userId = window.localStorage.getItem(CHAT_USER_ID_STORAGE)?.trim() ?? '';
  const storedLoginStatus = window.localStorage.getItem(CHAT_LOGIN_STATUS_STORAGE)?.trim().toUpperCase();
  const scenario = window.localStorage.getItem(CHAT_SCENARIO_STORAGE)?.trim().toUpperCase() || DEFAULT_SCENARIO;

  return {
    userId,
    loginStatus: userId && storedLoginStatus === 'LOGGED_IN' ? 'LOGGED_IN' : 'ANONYMOUS',
    scenario,
  };
}

function parseEventMetadata(data: string): Record<string, string> {
  if (!data) {
    return {};
  }

  try {
    const parsed = JSON.parse(data) as Record<string, unknown>;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {};
    }

    return Object.entries(parsed).reduce<Record<string, string>>((acc, [key, value]) => {
      acc[key] = value == null ? '' : String(value);
      return acc;
    }, {});
  } catch {
    return {};
  }
}

function buildLifecycleMessage(
  eventType: Exclude<StreamEventType, 'message'>,
  metadata: Record<string, string>,
): string {
  if (eventType === 'start') {
    return metadata.sessionId ? `SSE 会话已建立，sessionId：${metadata.sessionId}` : 'SSE 会话已建立';
  }
  if (eventType === 'error') {
    return metadata.message || DEFAULT_ERROR_MESSAGE;
  }
  return metadata.sessionId ? `本轮回答已完成，sessionId：${metadata.sessionId}` : '本轮回答已完成';
}

export function useChat() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [model, setModel] = useState('deepseek');
  const [streamEvent, setStreamEvent] = useState<StreamLifecycleEvent | null>(null);
  const sessionIdRef = useRef(uid());
  const abortRef = useRef<AbortController | null>(null);
  const chatContext = getChatContext();

  const sendMessage = useCallback(async (content: string) => {
    if (!content.trim() || isLoading) return;

    const context = getChatContext();
    const userMsg: Message = {
      id: uid(),
      role: 'user',
      content: content.trim(),
      timestamp: Date.now(),
      status: 'done',
    };
    const asstMsg: Message = {
      id: uid(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      status: 'pending',
      eventType: 'start',
      metadata: { sessionId: sessionIdRef.current },
    };
    const asstId = asstMsg.id;

    const updateAssistant = (updater: (message: Message) => Message) => {
      setMessages(prev => prev.map(message => (message.id === asstId ? updater(message) : message)));
    };

    setMessages(prev => [...prev, userMsg, asstMsg]);
    setStreamEvent(null);
    setIsLoading(true);

    const abort = new AbortController();
    abortRef.current = abort;

    let buffer = '';
    let accumulated = '';
    let completedByDone = false;
    let hasStreamError = false;
    let wasAborted = false;

    const processEventBlock = (block: string) => {
      if (!block.trim()) {
        return false;
      }

      let eventType: StreamEventType = 'message';
      const dataLines: string[] = [];

      for (const rawLine of block.split(/\r?\n/)) {
        const line = rawLine.trimEnd();
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim() as StreamEventType;
        }
        if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim());
        }
      }

      const data = dataLines.join('\n');
      const metadata = eventType === 'message' ? {} : parseEventMetadata(data);

      if (eventType === 'start') {
        setStreamEvent({
          type: 'start',
          message: buildLifecycleMessage('start', metadata),
          timestamp: Date.now(),
          metadata,
        });
        updateAssistant(message => ({
          ...message,
          status: 'streaming',
          eventType: 'start',
          metadata: { ...(message.metadata ?? {}), ...metadata },
        }));
        return false;
      }

      if (eventType === 'message') {
        if (data) {
          accumulated += data;
        }
        updateAssistant(message => ({
          ...message,
          content: accumulated,
          status: 'streaming',
          eventType: 'message',
        }));
        return false;
      }

      if (eventType === 'error') {
        hasStreamError = true;
        const errorMessage = metadata.message || DEFAULT_ERROR_MESSAGE;
        setStreamEvent({
          type: 'error',
          message: buildLifecycleMessage('error', metadata),
          timestamp: Date.now(),
          metadata,
        });
        updateAssistant(message => ({
          ...message,
          content: accumulated || errorMessage,
          status: 'error',
          eventType: 'error',
          metadata: { ...(message.metadata ?? {}), ...metadata },
        }));
        return false;
      }

      completedByDone = true;
      if (!hasStreamError) {
        setStreamEvent({
          type: 'done',
          message: buildLifecycleMessage('done', metadata),
          timestamp: Date.now(),
          metadata,
        });
      }
      updateAssistant(message => ({
        ...message,
        status: message.status === 'error' ? 'error' : 'done',
        eventType: message.status === 'error' ? 'error' : 'done',
        metadata: { ...(message.metadata ?? {}), ...metadata },
      }));
      return true;
    };

    try {
      const requestBody: ChatRequest = {
        message: content.trim(),
        sessionId: sessionIdRef.current,
        model,
        userId: context.userId,
        loginStatus: context.loginStatus,
        scenario: context.scenario,
      };

      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody),
        signal: abort.signal,
      });

      if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`);

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      outer: while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const parts = buffer.split(/\r?\n\r?\n/);
        buffer = parts.pop() ?? '';

        for (const part of parts) {
          if (processEventBlock(part)) break outer;
        }
      }

      const tail = buffer.trim();
      if (tail) {
        processEventBlock(tail);
      }
    } catch (err: unknown) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        wasAborted = true;
        return;
      }

      hasStreamError = true;
      setStreamEvent({
        type: 'error',
        message: DEFAULT_ERROR_MESSAGE,
        timestamp: Date.now(),
        metadata: { code: 'NETWORK_ERROR', sessionId: sessionIdRef.current },
      });
      updateAssistant(message => ({
        ...message,
        content: accumulated || DEFAULT_ERROR_MESSAGE,
        status: 'error',
        eventType: 'error',
        metadata: { ...(message.metadata ?? {}), code: 'NETWORK_ERROR', sessionId: sessionIdRef.current },
      }));
    } finally {
      if (!wasAborted && !hasStreamError && !completedByDone) {
        updateAssistant(message => ({
          ...message,
          status: message.status === 'pending' ? 'done' : message.status,
          eventType: message.eventType === 'start' ? 'done' : message.eventType,
        }));
      }
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
    setStreamEvent(null);
    sessionIdRef.current = uid();
  }, []);

  return {
    messages,
    isLoading,
    model,
    setModel,
    sendMessage,
    stopGeneration,
    clearMessages,
    streamEvent,
    chatContext,
  };
}
