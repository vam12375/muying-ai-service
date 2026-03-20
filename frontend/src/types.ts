/** 聊天消息 */
export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

/** 聊天请求 */
export interface ChatRequest {
  message: string;
  sessionId: string;
  model?: string;
}

export interface ErrorState {
  type: 'network' | 'server' | 'auth';
  message: string;
  retryable: boolean;
}

/** 知识库文档 */
export interface KnowledgeDocument {
  id: string;
  fileName: string;
  category: string;
  segmentCount: number;
  createdAt: number;
}

/** 导航页签 */
export type Tab = 'chat' | 'knowledge';

/** 知识文档分类 */
export const KNOWLEDGE_CATEGORIES = [
  { value: 'faq',       label: 'FAQ 常见问题',   color: 'violet' },
  { value: 'policy',    label: '政策条款',     color: 'blue'   },
  { value: 'parenting', label: '育儿知识',     color: 'green'  },
  { value: 'promotion', label: '营销优惠',     color: 'pink'   },
] as const;
