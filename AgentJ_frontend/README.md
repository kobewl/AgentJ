# AgentJ Frontend

AgentJ 前端项目，基于 Vue 3 + Element Plus 构建。

## 快速启动

### 环境要求

- Node.js 18+
- npm 或 pnpm

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:5173

### 构建生产版本

```bash
npm run build
```

## 项目结构

```
src/
├── api/                    # API 接口封装
│   ├── http.ts            # HTTP 客户端配置
│   └── codegen.ts         # 代码生成 API
├── components/             # 可复用组件
│   ├── MarkdownRenderer.vue  # Markdown 渲染 ⭐
│   └── ...
├── layouts/                # 布局组件
│   └── AppLayout.vue
├── pages/                  # 页面组件
│   ├── CodeGen/           # 代码生成模块 ⭐
│   │   ├── CodeGenList.vue
│   │   └── CodeGenEditor.vue
│   ├── ChatDialog.vue      # LLM 对话
│   ├── Dashboard.vue       # 总览仪表盘
│   ├── Login.vue           # 登录页
│   └── ...
├── router/                 # 路由配置
│   └── index.ts
├── stores/                 # Pinia 状态管理
│   └── loginUser.ts
├── styles/                 # 全局样式
└── utils/                  # 工具函数
    └── visualEditor.ts    # 可视化编辑器 ⭐
```

## 代码生成模块 ⭐

### 功能特性

- **SSE 流式响应**: EventSource 实时接收 AI 生成内容
- **Markdown 渲染**: 使用 markdown-it + highlight.js 渲染 AI 响应
- **可视化编辑**: iframe 内嵌 + postMessage 通信
- **对话记忆**: 加载和展示对话历史
- **代码下载**: 支持下载生成的代码（ZIP 格式）

### 页面组件

#### CodeGenList.vue
- 应用列表展示
- 创建新应用
- 编辑/删除/预览/下载

#### CodeGenEditor.vue
- 左侧对话面板（流式消息显示）
- 右侧预览面板（iframe 加载）
- 可视化编辑模式切换
- 元素选中信息展示

### 工具类

#### visualEditor.ts
- iframe 脚本注入
- 元素选择器生成
- postMessage 通信处理

### API 封装

```typescript
// 代码生成 API
export function createApp(data: CreateAppRequest)
export function getApp(id: number)
export function getUserApps()
export function updateApp(id: number, data: UpdateAppRequest)
export function deleteApp(id: number)
export function deployApp(id: number)
export function getChatHistory(appId: number)
export function getPreviewUrl(deployKey: string)
export function getDownloadUrl(appId: number)
```

## 配置

### 环境变量

创建 `.env.local` 文件：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### Vite 配置

主要配置在 `vite.config.ts`:

- 代理配置 - 开发环境 API 代理
- 别名配置 - @ 指向 src 目录
- 构建优化

## 依赖说明

### 核心依赖

```json
{
  "vue": "^3.4.37",
  "vue-router": "^4.3.2",
  "element-plus": "^2.9.2",
  "axios": "^1.7.7",
  "pinia": "^2.1.7"
}
```

### 代码生成相关依赖

```json
{
  "markdown-it": "^14.1.0",
  "highlight.js": "^11.11.1"
}
```

### 工作流相关

```json
{
  "@vue-flow/core": "^1.48.1",
  "@vueflow/background": "^1.4.1",
  "@vueflow/controls": "^1.4.1"
}
```

## 样式定制

### Element Plus 主题定制

在 `src/styles/` 目录下创建主题文件，使用 CSS 变量定制主题。

### 全局样式

- `styles/global.css` - 全局通用样式
- `styles/variables.css` - CSS 变量定义

## 开发指南

### 添加新页面

1. 在 `src/pages/` 下创建页面组件
2. 在 `src/router/index.ts` 中添加路由配置
3. 在侧边栏菜单中添加入口

### 添加 API

1. 在 `src/api/` 下创建 API 文件
2. 使用 `http` 实例发送请求
3. 处理响应数据

### 状态管理

使用 Pinia 进行状态管理，现有 Store：

- `loginUser` - 登录用户信息

## 构建和部署

### 构建

```bash
npm run build
```

### 预览

```bash
npm run preview
```

### Docker 部署

```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```

## 许可证

Apache License 2.0
