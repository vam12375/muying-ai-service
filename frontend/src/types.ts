/** 聊天消息角色 */
export type MessageRole = 'user' | 'assistant';

/** 聊天消息状态 */
export type MessageStatus = 'pending' | 'streaming' | 'done' | 'error';

/** SSE 事件类型 */
export type StreamEventType = 'start' | 'message' | 'error' | 'done';

/** 聊天消息 */
export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: number;
  status?: MessageStatus;
  eventType?: StreamEventType;
  metadata?: Record<string, string>;
}

/** 聊天请求 */
export interface ChatRequest {
  message: string;
  sessionId: string;
  model?: string;
  userId?: string;
  loginStatus?: 'ANONYMOUS' | 'LOGGED_IN';
  scenario?: string;
}

/** 当前聊天上下文 */
export interface ChatContext {
  userId: string;
  loginStatus: 'ANONYMOUS' | 'LOGGED_IN';
  scenario: string;
}

/** SSE 生命周期事件 */
export interface StreamLifecycleEvent {
  type: Exclude<StreamEventType, 'message'>;
  message: string;
  timestamp: number;
  metadata?: Record<string, string>;
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
