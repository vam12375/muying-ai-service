# 前端子应用说明

这里是 `muying-ai-service` 的前端演示应用，基于 React 19 + TypeScript + Vite 构建，主要提供两个页面：

- 聊天页面：演示多模型切换、SSE 流式回复、会话上下文展示。
- 知识库页面：演示文档上传、列表查询、删除、API Key 管理。

完整项目说明、后端架构、接口文档和启动步骤请优先查看上一级 [`../README.md`](../README.md)。

## 技术栈

- React 19
- TypeScript
- Vite 7
- Tailwind CSS 4
- lucide-react

## 本地启动

```bash
npm install
npm run dev
```

默认端口：`5173`

开发代理配置位于 `vite.config.ts`，会自动把 `/api` 请求转发到：

```text
http://localhost:8090
```

## 可用脚本

```bash
npm run dev
npm run build
npm run lint
npm run preview
```

## 页面结构

```text
src/
├─ App.tsx                 # 页面容器，切换 chat / knowledge 两个页签
├─ components/
│  ├─ ChatPage.tsx         # 聊天页
│  ├─ KnowledgePage.tsx    # 知识库页
│  ├─ MessageBubble.tsx    # 消息气泡
│  ├─ MessageInput.tsx     # 输入框
│  └─ Sidebar.tsx          # 左侧导航
├─ hooks/
│  ├─ useChat.ts           # SSE 对话逻辑
│  └─ useKnowledge.ts      # 知识库管理逻辑
└─ types.ts                # 前端类型定义
```

## 本地存储约定

前端会通过 `localStorage` 保存聊天和知识库上下文：

| Key | 作用 |
| --- | --- |
| `ai-chat-user-id` | 当前聊天用户 ID |
| `ai-chat-login-status` | 登录状态，影响工具调用权限 |
| `ai-chat-scenario` | 业务场景，用于订单 / 物流 / 优惠券工具放行 |
| `knowledge-api-key` | 知识库上传 / 删除接口使用的 API Key |

如果你希望本地验证需要登录上下文的工具调用，可以先在浏览器控制台执行：

```javascript
localStorage.setItem('ai-chat-user-id', 'user-10001')
localStorage.setItem('ai-chat-login-status', 'LOGGED_IN')
localStorage.setItem('ai-chat-scenario', 'ORDER_SERVICE')
```

## 交互特点

- 聊天页会展示 `start`、`error`、`done` 三类 SSE 生命周期提示。
- 知识库页会对鉴权失败、服务异常、网络异常做区分提示。
- API Key 仅保存在浏览器本地，不会写入仓库文件。
