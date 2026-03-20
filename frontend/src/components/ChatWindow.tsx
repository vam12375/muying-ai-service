import { useEffect, useRef } from 'react';
import { useChat } from '../hooks/useChat';
import { MessageBubble } from './MessageBubble';
import { MessageInput } from './MessageInput';
import { ModelSelector } from './ModelSelector';

export function ChatWindow() {
  const {
    messages,
    isLoading,
    model,
    setModel,
    sendMessage,
    stopGeneration,
    clearMessages,
  } = useChat();

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* 顶部导航栏 */}
      <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-indigo-500 flex items-center justify-center text-white font-bold text-sm">
            萌
          </div>
          <div>
            <h1 className="text-base font-semibold text-gray-800">萌宝助手</h1>
            <p className="text-xs text-gray-400">母婴商城智能客服</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <ModelSelector model={model} onChange={setModel} />
          <button
            onClick={clearMessages}
            className="text-sm text-gray-500 hover:text-red-500 transition-colors px-3 py-1.5 rounded-lg hover:bg-gray-100"
          >
            清空对话
          </button>
        </div>
      </header>

      {/* 消息区域 */}
      <div className="flex-1 overflow-y-auto chat-messages px-6 py-4">
        <div className="max-w-3xl mx-auto">
          {messages.length === 0 ? (
            <WelcomeScreen onSend={sendMessage} />
          ) : (
            messages.map((msg, i) => (
              <MessageBubble
                key={msg.id}
                message={msg}
                isStreaming={
                  isLoading &&
                  i === messages.length - 1 &&
                  msg.role === 'assistant'
                }
              />
            ))
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* 输入区域 */}
      <MessageInput
        onSend={sendMessage}
        onStop={stopGeneration}
        isLoading={isLoading}
      />
    </div>
  );
}

/** 欢迎页 — 引导用户开始对话 */
function WelcomeScreen({ onSend }: { onSend: (msg: string) => void }) {
  const suggestions = [
    '宝宝奶粉怎么冲泡比较好？',
    '帮我查一下订单 ORD20240101001 的状态',
    '有什么优惠活动吗？',
    '新生儿洗澡需要注意什么？',
  ];

  return (
    <div className="flex flex-col items-center justify-center h-full text-center">
      <div className="w-16 h-16 rounded-full bg-indigo-100 flex items-center justify-center mb-4">
        <span className="text-3xl">👶</span>
      </div>
      <h2 className="text-xl font-semibold text-gray-700 mb-2">
        你好！我是萌宝助手
      </h2>
      <p className="text-sm text-gray-400 mb-6">
        我可以帮你解答母婴问题、查询订单、推荐商品
      </p>
      <div className="grid grid-cols-2 gap-3 max-w-lg">
        {suggestions.map(s => (
          <button
            key={s}
            onClick={() => onSend(s)}
            className="text-left text-sm text-gray-600 bg-white border border-gray-200 rounded-xl px-4 py-3 hover:border-indigo-400 hover:text-indigo-600 transition-colors"
          >
            {s}
          </button>
        ))}
      </div>
    </div>
  );
}
