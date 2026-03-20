import { useState, useCallback, useEffect } from 'react';
import type { KnowledgeDocument } from '../types';

export function useKnowledge() {
  const [documents, setDocuments]   = useState<KnowledgeDocument[]>([]);
  const [isLoading, setIsLoading]   = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError]           = useState<string | null>(null);

  const fetchDocuments = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await fetch('/api/knowledge/list');
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data: KnowledgeDocument[] = await res.json();
      setDocuments(data);
    } catch (e) {
      setError('获取文档列表失败');
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
      const res = await fetch(`/api/knowledge/upload`, { method: 'POST', body: form });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      await fetchDocuments();
    } catch (e) {
      setError('上传失败，请重试');
      console.error(e);
    } finally {
      setIsUploading(false);
    }
  }, [fetchDocuments]);

  const deleteDocument = useCallback(async (id: string) => {
    try {
      const res = await fetch(`/api/knowledge/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setDocuments(prev => prev.filter(d => d.id !== id));
    } catch (e) {
      setError('删除失败，请重试');
      console.error(e);
    }
  }, []);

  useEffect(() => { fetchDocuments(); }, [fetchDocuments]);

  return { documents, isLoading, isUploading, error, uploadDocument, deleteDocument, refetch: fetchDocuments };
}
