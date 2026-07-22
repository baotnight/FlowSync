# FlowSync 前端

Vue 3 + Element Plus + Axios

## 目录结构

```
frontend/
├── package.json               # npm 依赖
├── vue.config.js              # devServer 代理 + LAN 配置
├── public/
│   ├── index.html
│   ├── favicon.ico
│   ├── login-bg.mp4           # 登录/主界面视频背景
│   └── login-bg.jpg           # 登录背景图片（备用）
└── src/
    ├── main.js                # 入口（Vue3 + ElementPlus + Router）
    ├── App.vue                # 根组件（BackToTop + 自定义字体 @font-face）
    ├── router/index.js        # SPA 路由 → HomeView
    ├── api/index.js           # Axios + JWT Bearer 拦截器 + 401 处理 + 全部 API
    ├── assets/
    │   └── fonts/             # 自定义字体（Inter, Zen Maru Gothic, JetBrains Mono, Great Vibes）
    ├── store/
    │   ├── avatarStore.js     # 头像跨组件响应式同步
    │   └── uploadQueue.js     # 上传队列全局状态
    ├── views/
    │   └── HomeView.vue       # 主容器：3D翻转登录 + 顶部导航 + 面板切换 + 视频背景
    └── components/
        ├── DashboardPanel.vue      # 控制台 — Hero区域 + 4指标卡片悬浮预览 + 上传队列 + 审核
        ├── ProjectPanel.vue        # 项目管理 — 表格 + 小组 + CRUD 弹窗 + 多选批量删除
        ├── AiTaskPlanPanel.vue     # AI 任务拆解 — 选项目→填目标→AI拆解→调整→导入
        ├── TaskPanel.vue           # 任务管理 — 表格/看板切换 + 搜索筛选 + 拖拽 + 子任务进度
        ├── TaskDetailDrawer.vue    # 任务详情抽屉 — 子任务checklist + 评论区 + 自动进度同步
        ├── TaskLogPanel.vue        # 进度更新 — 进度条 + 时间线 + 子任务自动记录
        ├── SummaryPanel.vue        # 总结管理 — 列表 + 新增弹窗
        ├── AdminPanel.vue          # 成员管理 — 邀请码 + 角色升降级 + 项目转让弹窗
        ├── GitHubPanel.vue         # GitHub 仓库 — 授权 + 绑定 + 文件树 + 源码浏览 + Issue/PR
        ├── ProfilePanel.vue        # 个人信息 — 抽屉面板 + 头像/GitHub/密码编辑
        └── TaskCard.vue            # 看板卡片组件
```

## 核心依赖

- `vue` 3.x + `vue-router` 4.x
- `element-plus` 2.4.x — UI 组件库
- `axios` — HTTP 客户端（Bearer Token 拦截器）

## 关键配置

`vue.config.js`：

```js
devServer: {
  host: '0.0.0.0',           // LAN 访问
  port: 8081,
  allowedHosts: 'all',
  client: {
    webSocketURL: 'auto://0.0.0.0:0/ws'
  },
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

## 认证流程

```
登录/注册 → 后端返回 {token, user}
  → sessionStorage 存 token + user
  → Axios 拦截器自动附加 Authorization: Bearer <token>
  → 后端 JwtInterceptor 解析 → 注入 userId + role
  → 401 自动清除存储 + 跳转登录页
```

## 页面组织

| 面板 | 组件 | 访问方式 |
|------|------|----------|
| 控制台 | DashboardPanel | 顶栏导航 + 默认首页 |
| 项目管理 | ProjectPanel | 控制台卡片点击 |
| AI 任务拆解 | AiTaskPlanPanel | 顶栏导航（负责人/管理员） |
| 任务管理 | TaskPanel | 控制台卡片点击，表格/看板切换 |
| 进度更新 | TaskLogPanel | 顶栏导航 |
| 总结管理 | SummaryPanel | 控制台卡片点击 |
| 成员管理 | AdminPanel | 控制台卡片点击 |
| GitHub 仓库 | GitHubPanel | 顶栏导航（负责人/管理员） |
| 个人信息 | ProfilePanel | 点击头像 → 左侧抽屉 |

## 权限控制

| 角色 | 可见功能 |
|------|----------|
| 管理员 | 全部面板 + 全部 CRUD 按钮 + 系统管理 |
| 负责人 | 全部业务面板 + AI + 自己的项目 CRUD |
| 组员 | 控制台/任务/进度/总结，仅操作自己的任务 |

## UI 特性

| 特性 | 说明 |
|------|------|
| 登录页 | 3D 翻转卡片 + 滑块切换 + 视频背景 + 标签式表单 + radio-button 角色选择 |
| 主界面 | 暖色调暗色主题 + 视频背景 + 毛玻璃卡片 + 自定义字体（Inter/Zen Maru Gothic） |
| 导航 | module-nav 按钮网格 + 顶栏紧凑布局 + 左右等宽居中 |
| 看板 | 三列拖拽 + 表格/看板切换 + 卡片悬浮预览 |
| 子任务 | 抽屉内 checklist + 进度条 + 自动同步任务状态和进度记录 |

## 启动

```powershell
cd frontend
npm install
npm run serve
# → http://localhost:8081
```
