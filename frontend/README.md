# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
  ---
  AI 智能客服系统 (muying-ai-service) — 完成                                                                  
                                                                                                                后端（17 个 Java 文件）                                                                                     
                                                                                                              
  ┌──────────────┬─────────────────────────────────────┬─────────────────────────────────────────────────┐      │     模块     │                文件                 │                     技术点                      │      ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤      │ 启动入口     │ MuyingAiApplication.java            │ Spring Boot 3.3.6 + Java 21                     │      ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ 模型配置     │ AiModelConfig.java,                 │ 多模型注册（OpenAI 兼容协议）                   │
  │              │ AiModelProperties.java              │                                                 │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ 模型路由     │ ModelRouterService.java             │ 策略模式，DeepSeek/千问/智谱                    │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ 聊天核心     │ ChatService.java                    │ Spring AI Advisor 链 + 手动 RAG + .toolNames()  │    
  │              │                                     │ Function Calling                                │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ 对话记忆     │ ChatMemoryConfig.java               │ MessageChatMemoryAdvisor +                      │    
  │              │                                     │ MessageWindowChatMemory                         │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ Function     │ ToolConfig.java + 4 个 Tool 类      │ @Bean + @Description                            │    
  │ Calling      │                                     │ 模式，订单/商品/物流/优惠券                     │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ RAG 管道     │ EmbeddingPipeline.java,             │ LangChain4j 解析 → Spring AI VectorStore        │    
  │              │ KnowledgeService.java               │                                                 │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ API 接口     │ ChatController.java,                │ SSE 流式 + REST                                 │    
  │              │ KnowledgeController.java            │                                                 │    
  ├──────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────┤    
  │ 基础设施     │ docker-compose.yml                  │ Milvus 2.4.17 + Redis 7.4                       │    
  └──────────────┴─────────────────────────────────────┴─────────────────────────────────────────────────┘    

  前端（5 个组件 + 1 个 Hook）

  ┌───────────────────┬────────────────────────────────────┐
  │       文件        │                功能                │
  ├───────────────────┼────────────────────────────────────┤
  │ ChatWindow.tsx    │ 主界面：导航栏 + 消息列表 + 欢迎页 │
  ├───────────────────┼────────────────────────────────────┤
  │ MessageBubble.tsx │ 消息气泡（用户/助手双样式）        │
  ├───────────────────┼────────────────────────────────────┤
  │ MessageInput.tsx  │ 输入框 + 发送/停止按钮             │
  ├───────────────────┼────────────────────────────────────┤
  │ ModelSelector.tsx │ 模型切换下拉框                     │
  ├───────────────────┼────────────────────────────────────┤
  │ useChat.ts        │ SSE 流式接收 Hook                  │
  └───────────────────┴────────────────────────────────────┘

  启动方式

  1. docker-compose up -d — 启动 Milvus + Redis
  2. 后端 mvn spring-boot:run — 端口 8090
  3. 前端 cd frontend && npm run dev — 端口 5173（代理 → 8090）