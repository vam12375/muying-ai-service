import { useState, useRef, type DragEvent } from 'react';
import { Upload, Trash2, FileText, RefreshCw, AlertCircle } from 'lucide-react';
import { useKnowledge } from '../hooks/useKnowledge';
import { KNOWLEDGE_CATEGORIES } from '../types';

const CATEGORY_STYLES: Record<string, { bg: string; text: string; border: string }> = {
  faq:       { bg: 'rgba(139,92,246,0.12)', text: '#a78bfa', border: 'rgba(139,92,246,0.3)' },
  policy:    { bg: 'rgba(59,130,246,0.12)',  text: '#60a5fa', border: 'rgba(59,130,246,0.3)'  },
  parenting: { bg: 'rgba(16,185,129,0.12)',  text: '#34d399', border: 'rgba(16,185,129,0.3)'  },
  promotion: { bg: 'rgba(236,72,153,0.12)',  text: '#f472b6', border: 'rgba(236,72,153,0.3)'  },
};

export function KnowledgePage() {
  const { documents, isLoading, isUploading, error, uploadDocument, deleteDocument, refetch } = useKnowledge();
  const [category, setCategory] = useState('faq');
  const [dragOver, setDragOver]   = useState(false);
  const [deleting, setDeleting]   = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFiles = (files: FileList | null) => {
    if (!files?.length) return;
    Array.from(files).forEach(f => uploadDocument(f, category));
  };

  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    handleFiles(e.dataTransfer.files);
  };

  const handleDelete = async (id: string) => {
    setDeleting(id);
    await deleteDocument(id);
    setDeleting(null);
  };

  const fmt = (ts: number) =>
    new Date(ts).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });

  return (
    <div
      className="flex flex-col h-screen overflow-y-auto"
      style={{ background: 'linear-gradient(135deg,#0f0a1e 0%,#120d24 50%,#0a0f1e 100%)' }}
    >
      {/* 顶栏 */}
      <header
        className="flex items-center justify-between px-8 py-5 flex-shrink-0"
        style={{ borderBottom: '1px solid rgba(139,92,246,0.12)' }}
      >
        <div>
          <h1 className="text-lg font-semibold" style={{ color: '#e2e8f0' }}>知识库管理</h1>
          <p className="text-xs mt-0.5" style={{ color: 'rgba(148,163,184,0.5)' }}>
            上传文档后自动向量化入库，供 RAG 检索使用
          </p>
        </div>
        <button
          onClick={refetch}
          disabled={isLoading}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs transition-all"
          style={{ color: 'rgba(167,139,250,0.7)', border: '1px solid rgba(139,92,246,0.2)' }}
        >
          <RefreshCw size={12} className={isLoading ? 'animate-spin' : ''} />
          刷新
        </button>
      </header>

      <div className="flex-1 px-8 py-6 max-w-4xl w-full mx-auto space-y-6">
        {/* 错误提示 */}
        {error && (
          <div
            className="flex items-center gap-2 px-4 py-3 rounded-xl text-sm"
            style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)', color: '#fca5a5' }}
          >
            <AlertCircle size={14} />
            {error}
          </div>
        )}

        {/* 上传区 */}
        <div
          className={`drop-zone rounded-2xl p-8 text-center cursor-pointer transition-all ${dragOver ? 'drag-over' : ''}`}
          style={{ background: 'rgba(255,255,255,0.02)' }}
          onDragOver={e => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileRef.current?.click()}
        >
          <input
            ref={fileRef}
            type="file"
            accept=".txt,.md"
            multiple
            className="hidden"
            onChange={e => handleFiles(e.target.files)}
          />
          <div
            className="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-4"
            style={{ background: 'rgba(139,92,246,0.15)', border: '1px solid rgba(139,92,246,0.3)' }}
          >
            <Upload size={22} style={{ color: '#a78bfa' }} />
          </div>
          {isUploading ? (
            <p className="text-sm font-medium" style={{ color: '#a78bfa' }}>正在处理并向量化...</p>
          ) : (
            <>
              <p className="text-sm font-medium mb-1" style={{ color: '#e2e8f0' }}>
                点击或拖拽文件到此上传
              </p>
              <p className="text-xs" style={{ color: 'rgba(148,163,184,0.4)' }}>
                支持 TXT、MD 格式 · 自动切片 + Embedding 入库
              </p>
            </>
          )}
        </div>

        {/* 分类选择 */}
        <div className="flex items-center gap-3">
          <span className="text-xs font-medium" style={{ color: 'rgba(148,163,184,0.5)' }}>上传分类</span>
          <div className="flex gap-2 flex-wrap">
            {KNOWLEDGE_CATEGORIES.map(c => {
              const s = CATEGORY_STYLES[c.value] ?? CATEGORY_STYLES.faq;
              return (
                <button
                  key={c.value}
                  onClick={() => setCategory(c.value)}
                  className="px-3 py-1 rounded-lg text-xs font-medium transition-all"
                  style={
                    category === c.value
                      ? { background: s.bg, color: s.text, border: `1px solid ${s.border}` }
                      : { background: 'transparent', color: 'rgba(148,163,184,0.45)', border: '1px solid rgba(255,255,255,0.06)' }
                  }
                >
                  {c.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* 文档列表 */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-medium" style={{ color: '#e2e8f0' }}>
              已入库文档
              <span className="ml-2 px-1.5 py-0.5 rounded-md text-xs" style={{ background: 'rgba(139,92,246,0.15)', color: '#a78bfa' }}>
                {documents.length}
              </span>
            </h2>
          </div>

          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3].map(i => (
                <div
                  key={i}
                  className="h-16 rounded-xl"
                  style={{ background: 'rgba(255,255,255,0.03)', animation: 'pulse 1.5s ease-in-out infinite' }}
                />
              ))}
            </div>
          ) : documents.length === 0 ? (
            <div
              className="flex flex-col items-center justify-center py-16 rounded-2xl"
              style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' }}
            >
              <FileText size={32} style={{ color: 'rgba(148,163,184,0.2)' }} className="mb-3" />
              <p className="text-sm" style={{ color: 'rgba(148,163,184,0.4)' }}>暂无文档，上传后即可用于 AI 检索</p>
            </div>
          ) : (
            <div className="space-y-2">
              {documents.map(doc => {
                const s = CATEGORY_STYLES[doc.category] ?? CATEGORY_STYLES.faq;
                const catLabel = KNOWLEDGE_CATEGORIES.find(c => c.value === doc.category)?.label ?? doc.category;
                return (
                  <div
                    key={doc.id}
                    className="flex items-center gap-4 px-5 py-4 rounded-xl card-hover"
                    style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}
                  >
                    <div
                      className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0"
                      style={{ background: s.bg, border: `1px solid ${s.border}` }}
                    >
                      <FileText size={15} style={{ color: s.text }} />
                    </div>

                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate" style={{ color: '#e2e8f0' }}>{doc.fileName}</p>
                      <p className="text-xs mt-0.5" style={{ color: 'rgba(148,163,184,0.45)' }}>
                        {doc.segmentCount} 段 · {fmt(doc.createdAt)}
                      </p>
                    </div>

                    <span
                      className="px-2 py-0.5 rounded-lg text-xs font-medium flex-shrink-0"
                      style={{ background: s.bg, color: s.text, border: `1px solid ${s.border}` }}
                    >
                      {catLabel}
                    </span>

                    <button
                      onClick={() => handleDelete(doc.id)}
                      disabled={deleting === doc.id}
                      className="flex-shrink-0 w-7 h-7 rounded-lg flex items-center justify-center transition-all disabled:opacity-40"
                      style={{ color: 'rgba(148,163,184,0.35)', border: '1px solid transparent' }}
                      onMouseEnter={e => { e.currentTarget.style.color = '#fca5a5'; e.currentTarget.style.borderColor = 'rgba(239,68,68,0.3)'; e.currentTarget.style.background = 'rgba(239,68,68,0.1)'; }}
                      onMouseLeave={e => { e.currentTarget.style.color = 'rgba(148,163,184,0.35)'; e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.background = 'transparent'; }}
                    >
                      {deleting === doc.id
                        ? <RefreshCw size={12} className="animate-spin" />
                        : <Trash2 size={12} />
                      }
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
