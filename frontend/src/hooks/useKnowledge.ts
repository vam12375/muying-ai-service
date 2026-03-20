import { useState, useCallback, useEffect } from 'react';
import type { ErrorState, KnowledgeDocument } from '../types';

const KNOWLEDGE_API_KEY_STORAGE = 'knowledge-api-key';

function getKnowledgeHeaders(): HeadersInit | undefined {
  const apiKey = window.localStorage.getItem(KNOWLEDGE_API_KEY_STORAGE)?.trim();
  return apiKey ? { 'X-API-Key': apiKey } : undefined;
}

function buildKnowledgeError(action: string, error: unknown): ErrorState {
  if (error instanceof Error && error.message.startsWith('HTTP ')) {
    const status = Number(error.message.slice(5));
    if (status === 401) {
      return {
        type: 'auth',
        message: `${action}失败：缺少或错误的知识库管理密钥，请先在页面中填写 API Key。`,
        retryable: true,
      };
    }
    if (status >= 500) {
      return {
        type: 'server',
        message: `${action}失败：服务暂时不可用，请稍后重试。`,
        retryable: true,
      };
    }
  }

  return {
    type: 'network',
    message: `${action}失败：网络异常，请检查连接后重试。`,
    retryable: true,
  };
}

export function useKnowledge() {
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<ErrorState | null>(null);
  const [apiKey, setApiKeyState] = useState(() => window.localStorage.getItem(KNOWLEDGE_API_KEY_STORAGE) ?? '');

  const setApiKey = useCallback((value: string) => {
    setApiKeyState(value);
    window.localStorage.setItem(KNOWLEDGE_API_KEY_STORAGE, value.trim());
  }, []);

  const fetchDocuments = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await fetch('/api/knowledge/list');
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data: KnowledgeDocument[] = await res.json();
      setDocuments(data);
    } catch (e) {
      setError(buildKnowledgeError('获取文档列表', e));
      console.error(e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const uploadDocument = useCallback(async (file: File, category: string) => {
    setIsUploading(true);
    setError(null);
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('category', category);
      const res = await fetch(`/api/knowledge/upload`, { method: 'POST', body: form, headers: getKnowledgeHeaders() });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      await fetchDocuments();
    } catch (e) {
      setError(buildKnowledgeError('上传文档', e));
      console.error(e);
    } finally {
      setIsUploading(false);
    }
  }, [fetchDocuments]);

  const deleteDocument = useCallback(async (id: string) => {
    setError(null);
    try {
      const res = await fetch(`/api/knowledge/${id}`, { method: 'DELETE', headers: getKnowledgeHeaders() });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setDocuments(prev => prev.filter(d => d.id !== id));
    } catch (e) {
      setError(buildKnowledgeError('删除文档', e));
      console.error(e);
    }
  }, []);

  useEffect(() => { fetchDocuments(); }, [fetchDocuments]);

  return {
    documents,
    isLoading,
    isUploading,
    error,
    apiKey,
    setApiKey,
    uploadDocument,
    deleteDocument,
    refetch: fetchDocuments,
  };
}
