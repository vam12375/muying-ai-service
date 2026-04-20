# 🍼 母婴 AI 智能客服服务 (Muying AI Service)

![Java](https://img.shields.io/badge/Java-21-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M1-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue.js-3.x-4FC08D.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

`muying-ai-service` 是一个专为母婴商城场景打造的 AI 智能客服系统。它基于 **Spring AI** 构建，深度整合了大语言模型（LLM）、检索增强生成（RAG）以及函数调用（Function Calling）技术，旨在解决传统客服系统中的痛点。

## 🎯 核心解决的问题

- **知识碎片化**：用户咨询内容分散，传统 FAQ 难以覆盖真实且多变的问题。
- **信息孤岛**：售前、订单、物流、优惠活动等信息分散在不同业务系统里，客服查询效率低。
- **维护成本高**：知识库需要可持续维护，而不是将提示词或规则硬编码在代码中。

本项目将多模型对话、RAG 知识检索、商城工具调用和知识库管理无缝整合，非常适合用作母婴商城客服助手原型、AI 服务中台样板或业务侧接入演示项目。

---

## 📑 目录

- [✨ 亮点能力](#-亮点能力)
- [💡 典型使用场景](#-典型使用场景)
- [🛠️ 技术栈](#️-技术栈)
- [🚀 快速体验](#-快速体验)
  - [1. 环境要求](#1-环境要求)
  - [2. 启动基础设施](#2-启动基础设施)
  - [3. 配置模型密钥](#3-配置模型密钥)
  - [4. 启动后端](#4-启动后端)
  - [5. 启动前端](#5-启动前端)
- [🏗️ 架构概览](#️-架构概览)
- [🔌 核心接口](#-核心接口)
- [⚙️ 关键机制](#️-关键机制)
- [💻 前端说明](#-前端说明)
- [📁 目录结构](#-目录结构)
- [❓ 常见问题](#-常见问题)
- [🔮 后续可扩展方向](#-后续可扩展方向)

---

## ✨ 亮点能力

| 能力 | 说明 | 价值 |
| :--- | :--- | :--- |
| **多模型路由** | 支持 `deepseek`、`qianwen`、`zhipu` 三个模型按名称无缝切换 | 便于横向评估不同模型的效果与成本 |
| **SSE 流式聊天** | `/api/chat/stream` 按 `start / message / error / done` 事件输出 | 前端可实时打字机渲染，交互更接近真实客服 |
| **RAG 检索增强** | 对话前先从 Milvus 检索相关知识片段，再拼接进提示词 | 避免大模型幻觉，基于真实业务知识回答问题 |
| **Function Calling** | 可自动调用订单、物流、商品、优惠券查询工具 | 让客服从“只会说”进化成“能查能办” |
| **Redis 对话记忆** | 默认保留最近 20 条消息上下文 | 支持多轮对话，理解用户连续意图 |
| **知识库管理** | 支持上传 `TXT`、`MD` 文档并自动向量化入库 | 让业务人员可持续维护内容，无需开发介入 |
| **启动预加载** | 自动导入 `resources/knowledge/**` 下的内置知识文档 | 降低冷启动演示成本，开箱即用 |
| **基础安全与限流** | 支持知识库管理接口 API Key 保护和聊天限流 | 便于本地联调与基础生产化改造 |

## 💡 典型使用场景

### 1. 售前咨询 (RAG + Function Calling)
**用户输入：**
> “这款奶粉适合 6 个月宝宝吗？还有没有库存？”

**系统行为：**
- 结合知识库回答基础喂养建议。
- 命中商品关键词，自动调用商品查询工具获取实时库存。

### 2. 订单与物流查询 (Function Calling)
**用户输入：**
> “帮我查一下订单 ORD20240101001 的状态”

**系统行为：**
- 校验 `userId`、`loginStatus=LOGGED_IN`、`scenario` 等前置条件。
- 自动调用订单工具或物流工具，返回精准状态。

### 3. 政策与知识问答 (RAG)
**用户输入：**
> “新生儿洗澡要注意什么？退换货规则是怎样的？”

**系统行为：**
- 从内置或上传的知识库文档中检索相关片段。
- 以业务知识为主、大模型语言能力为辅生成专业回答。

## 🛠️ 技术栈

**后端 (Backend):**
- Java 21
- Spring Boot 3.2+
- Spring AI (大模型接入、RAG、Function Calling)
- Milvus (向量数据库)
- Redis (会话记忆、限流、元数据存储)

**前端 (Frontend):**
- Vue 3 (Composition API)
- Vite
- Tailwind CSS / SCSS

---

## 🚀 快速体验

### 1. 环境要求

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker Desktop

### 2. 启动基础设施

在项目根目录执行以下命令启动依赖中间件：

```bash
docker-compose up -d
```

默认会启动：
- `milvus`：向量数据库，端口 `19530`
- `redis`：会话和知识库元数据存储，端口 `6379`

### 3. 配置模型密钥

后端默认通过 Spring Boot 配置读取模型信息，其中 API Key 通常用环境变量注入，其他项可在 `application.yml`、启动参数或环境变量中覆盖。

**PowerShell 示例：**

```powershell
$env:DEEPSEEK_API_KEY="your-deepseek-key"
$env:QIANWEN_API_KEY="your-qianwen-key"
$env:ZHIPU_API_KEY="your-zhipu-key"
$env:AI_SERVICE_API_KEY="your-admin-key"
```

**Mac/Linux 示例：**

```bash
export DEEPSEEK_API_KEY="your-deepseek-key"
export QIANWEN_API_KEY="your-qianwen-key"
export ZHIPU_API_KEY="your-zhipu-key"
export AI_SERVICE_API_KEY="your-admin-key"
```

**常用配置项说明：**

| 配置项 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `DEEPSEEK_API_KEY` | `sk-placeholder` | DeepSeek 模型密钥 |
| `QIANWEN_API_KEY` | `sk-placeholder` | 通义千问模型与 Embedding 密钥 |
| `ZHIPU_API_KEY` | `sk-placeholder` | 智谱模型密钥 |
| `AI_SERVICE_API_KEY` | `change-me` | 知识库上传 / 删除接口密钥 |
| `mall.api.base-url` | `http://localhost:8080/api` | 母婴商城后端 API 根地址 |
| `web.cors.allowed-origins` | `http://localhost:5173,http://localhost:3000` | 允许跨域的前端地址 |

如果需要开启知识库管理鉴权和聊天限流，可以在 `application.yml` 中覆盖：

```yaml
security:
  api-key:
    enabled: true
    key: ${AI_SERVICE_API_KEY}
  rate-limit:
    enabled: true
    max-requests: 30
    window-seconds: 60
```

### 4. 启动后端

```bash
mvn spring-boot:run
```

- 服务地址：`http://localhost:8090`
- 接口文档 (Swagger/Knife4j)：`http://localhost:8090/doc.html`

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 前端地址：`http://localhost:5173`
- 已内置 `/api -> http://localhost:8090` 的开发代理

---

## 🏗️ 架构概览

```text
浏览器前端
    ├─ ChatPage          -> 发送聊天请求，接收 SSE 流式消息
    └─ KnowledgePage     -> 上传 / 删除 / 查看知识文档

Spring Boot 服务
    ├─ ChatController
    │   └─ ChatService
    │       ├─ ModelRouterService       -> 选择聊天模型
    │       ├─ ChatMemoryAdvisor        -> 读取 / 保存会话记忆
    │       ├─ VectorStore              -> 检索知识库上下文
    │       └─ ToolConfig               -> 注册商城查询工具
    │
    └─ KnowledgeController
        └─ KnowledgeService
            ├─ EmbeddingPipeline        -> 文档解析与切片
            ├─ VectorStore              -> 向量写入 / 删除
            └─ Redis Repository         -> 文档元数据

基础设施
    ├─ Redis    -> 对话记忆 + 文档元数据
    ├─ Milvus   -> 向量索引
    └─ 商城 API -> 订单 / 物流 / 商品 / 优惠券能力来源
```

## 🔌 核心接口

### 聊天接口

#### `POST /api/chat/stream`
SSE 流式对话接口，返回 `text/event-stream`。

**请求体示例：**
```json
{
  "message": "帮我查一下订单 ORD20240101001 的状态",
  "sessionId": "session-demo-001",
  "userId": "user-10001",
  "loginStatus": "LOGGED_IN",
  "scenario": "ORDER_SERVICE",
  "model": "deepseek"
}
```

**事件类型：**
- `start`：会话建立成功
- `message`：逐段返回模型内容
- `error`：流式生成异常
- `done`：本轮回答完成

#### `POST /api/chat`
同步对话接口，一次性返回完整结果。

#### `GET /api/chat/models`
获取当前可用模型名称集合。

### 知识库接口

#### `GET /api/knowledge/list`
获取已入库文档元数据列表。

#### `POST /api/knowledge/upload`
上传知识文档，支持 `TXT`、`MD`，并自动切片和向量化。

#### `DELETE /api/knowledge/{id}`
删除指定文档，同时清理 Milvus 向量和 Redis 元数据。

> **注意**：当 `security.api-key.enabled=true` 时，上传和删除接口需要在 Header 中携带：
> `X-API-Key: your-admin-key`

---

## ⚙️ 关键机制

### RAG 知识检索
- `EmbeddingPipeline` 使用 `TextDocumentParser` 解析文档。
- 文档默认按 `300 token` 切片，并保留 `50 token` 重叠。
- 查询时默认取 `topK = 3`，相似度阈值为 `0.6`。
- 检索结果会被拼接为“参考资料”后再交给模型。
- 当向量检索失败时，服务会自动降级为纯对话模式。

### Function Calling
系统会根据用户输入中的关键词自动选择工具：

| 工具 | 触发关键词示例 | 依赖商城接口 | 权限要求 |
| :--- | :--- | :--- | :--- |
| `orderQuery` | 订单、支付、退款、售后 | `GET /order/{orderId}` | 需要登录、携带 `userId`、场景允许 |
| `logisticsQuery` | 物流、快递、配送、发货 | `GET /logistics/order/{orderId}` | 需要登录、携带 `userId`、场景允许 |
| `productQuery` | 商品、奶粉、纸尿裤、库存、价格 | `GET /product/{productId}` | 无额外限制 |
| `couponQuery` | 优惠券、满减券、红包 | `GET /coupon/available?userId={userId}` | 需要登录、携带 `userId`、场景允许 |

**推荐场景值 (`scenario`)：**
- `GENERAL_ASSISTANT`
- `ORDER_SERVICE`
- `LOGISTICS_SERVICE`
- `AFTER_SALES`
- `PROMOTION_SERVICE`
- `MEMBER_SERVICE`

### 对话记忆
- 基于 Redis 自定义 `ChatMemoryRepository`。
- 默认使用 `MessageWindowChatMemory`。
- 最近 20 条消息参与上下文拼接。

### 内置知识文档
启动时会自动扫描并预加载以下目录中的 Markdown 文档：
```text
src/main/resources/knowledge/
├─ faq/
├─ parenting/
├─ policy/
└─ promotion/
```
当前仓库已经提供常见问题、退换货政策、新生儿护理、会员权益说明等样例文档，可直接用于演示。

---

## 💻 前端说明

前端包含两个核心页面：
- **`ChatPage`**：聊天页，支持模型切换、上下文展示、SSE 生命周期提示、清空会话。
- **`KnowledgePage`**：知识库页，支持 API Key 输入、上传、删除、刷新、错误态提示。

前端通过 `localStorage` 维护上下文：

| Key | 说明 |
| :--- | :--- |
| `ai-chat-user-id` | 当前聊天用户 ID |
| `ai-chat-login-status` | 登录状态 |
| `ai-chat-scenario` | 聊天业务场景 |
| `knowledge-api-key` | 知识库管理 API Key |

如果你希望本地验证需要权限的工具调用，可以在浏览器控制台执行：
```javascript
localStorage.setItem('ai-chat-user-id', 'user-10001')
localStorage.setItem('ai-chat-login-status', 'LOGGED_IN')
localStorage.setItem('ai-chat-scenario', 'ORDER_SERVICE')
```

前端子模块的补充说明见 [`frontend/README.md`](./frontend/README.md)。

---

## 📁 目录结构

```text
muying-ai-service/
├─ docker-compose.yml       # 基础设施编排
├─ pom.xml                  # Maven 依赖
├─ src/
│  ├─ main/
│  │  ├─ java/com/muying/ai/
│  │  │  ├─ config/         # 配置类
│  │  │  ├─ controller/     # API 接口
│  │  │  ├─ model/          # 实体类
│  │  │  ├─ rag/            # RAG 相关实现
│  │  │  ├─ service/        # 业务逻辑
│  │  │  ├─ tool/           # Function Calling 工具
│  │  │  ├─ KnowledgeInitializer.java
│  │  │  ├─ RedisChatMemoryRepository.java
│  │  │  └─ RedisKnowledgeDocumentRepository.java
│  │  └─ resources/
│  │     ├─ application.yml # 配置文件
│  │     └─ knowledge/      # 内置知识库文档
│  └─ test/                 # 单元测试
└─ frontend/                # 前端 Vue 项目
   ├─ src/
   ├─ package.json
   └─ README.md
```

---

## ❓ 常见问题

### 1. 聊天可以返回内容，但没有知识库命中
**排查步骤：**
- 检查 `docker-compose` 中的 `milvus` 是否已经成功启动。
- 检查 `QIANWEN_API_KEY` 是否配置正确且可用于 Embedding 模型。
- 查看后端启动日志，确认是否出现“知识文档预加载成功”的信息。

### 2. 上传知识文档返回 `401 Unauthorized`
通常说明知识库管理接口已经开启 API Key 保护，但前端没有携带正确的 `X-API-Key`。
**请确认：**
- `application.yml` 中 `security.api-key.enabled=true`。
- 环境变量 `AI_SERVICE_API_KEY` 的值与前端页面填写的 API Key 一致。

### 3. 订单或物流问题没有触发工具调用
**常见原因：**
- 消息中没有出现工具匹配的关键词。
- 前端未注入 `userId`。
- `loginStatus` 不是 `LOGGED_IN`。
- `scenario` 不在允许名单中。
- 商城后端接口 `mall.api.base-url` 不可访问或返回异常。

### 4. 前端访问后端出现跨域问题 (CORS)
请检查 `application.yml` 中的 `web.cors.allowed-origins` 是否包含实际前端地址。本地开发默认允许：
- `http://localhost:5173`
- `http://localhost:3000`

---

## 🔮 后续可扩展方向

- [ ] 增加批量导入、重建索引和知识库版本管理能力。
- [ ] 将工具调用结果改造成结构化 DTO，而不是直接透传商城接口字符串，以便前端更好渲染卡片。
- [ ] 增加会话列表、历史记录和客服工作台能力。
- [ ] 增加上传文件类型校验、大小限制提示和后台审计日志。
- [ ] 引入意图识别模型，更精准地路由到不同的处理流程。
