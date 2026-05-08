# 校园助手 — 全栈说明（前端 + 后端）

本文档同时说明 **后端 API 服务** 与 **独立前端工程**，便于本地联调与部署。

| 部分 | 路径 / 仓库 | 说明 |
|------|-------------|------|
| **后端** | 当前仓库 `web-app` | Spring Boot REST、MyBatis、JWT、邮件、通义千问、Qdrant、Meilisearch 等 |
| **前端** | 独立目录 `login-page`（Vue 3 + Vite） | 登录注册、竞赛、消息、AI 对话、**报名审核** 等页面 |

前端通过 HTTP 调用后端；接口根地址在前端 `src/api/config.js` 中配置（默认 `http://localhost:8080`）。

---

## 环境要求

### 后端

- **JDK 17**
- **Maven 3.8+**
- **MySQL 5.7**（需有应用期望的库表结构）
- 按功能可选：
  - **Redis**（项目已引入依赖；是否强依赖以当前分支代码为准）
  - **Qdrant**（gRPC，配置项 `qdrant.*`）
  - **Meilisearch**（配置项 `meilisearch.*`）
  - **阿里云通义千问 DashScope**（配置项 `dashscope.*`）
  - **SMTP 邮件**（`spring.mail.*`）

### 前端

- **Node.js 18+**（建议 LTS）
- **npm**

---

## 后端（`web-app`）

### 技术栈

- Spring Boot **3.4.x**
- MyBatis + PageHelper
- MySQL
- JWT（`java-jwt`）
- OkHttp（调用 DashScope）
- Qdrant 官方 Java 客户端 + gRPC
- Meilisearch Java SDK

### 配置说明

主配置：`src/main/resources/application.yml`。

至少需要配置或确认：

- `spring.datasource.*` — 数据库连接  
- `spring.mail.*` — 若使用验证码/发信  
- `dashscope.*` — AI 接口密钥与模型  
- `qdrant.*` — 向量库地址、gRPC 端口、集合名、向量维度（需与 embedding 维度一致）  
- `meilisearch.*` — 检索服务地址与索引名  

**请勿将真实密钥提交到仓库**；建议使用 Spring Profile、本地覆盖配置或环境变量，并避免在文档中粘贴生产密码。

### 启动后端

在本仓库根目录执行：

```bash
mvn spring-boot:run
```

默认端口：**8080**（可在 `application.yml` 的 `server.port` 修改）。

### 运行测试

```bash
mvn test
```

### 跨域与鉴权

- `WebConfig` 中为浏览器访问配置了 CORS。  
- 多数接口经 `LoginInterceptor` 校验；登录、注册等路径已排除（详见 `WebConfig` 中的 `excludePathPatterns`）。  
- 前端请求受保护接口时，需按项目约定携带 `Authorization`（JWT）。

---

## 前端（`login-page`）

前端在**另一目录/仓库**中（例如 `d:\svnProject\web1\login-page`）。`package.json` 中项目名为 `register-page`，技术栈为 **Vue 3**、**Vite 6**、**Vue Router**、**Vuex**、**Axios**。

### 配置后端地址

编辑 `src/api/config.js`：

```js
export const BASE_URL = 'http://localhost:8080';
```

部署或联调时改为实际 API 地址即可。

### 安装依赖与开发模式

```bash
cd /path/to/login-page
npm install
npm run dev
```

终端会打印本地访问地址（常见为 `http://localhost:5173`）。

### 生产构建

```bash
npm run build
```

产物在 `dist/`。用任意静态资源服务器或网关托管 `dist`，并保证 `BASE_URL` 指向线上 API。

---

## 本地联调建议顺序

1. 启动 MySQL（若使用向量检索/搜索，再启动 Qdrant、Meilisearch 等）。  
2. 按团队规范初始化或迁移数据库（本 README 不包含 SQL 脚本说明）。  
3. 在 `web-app` 目录执行 **`mvn spring-boot:run`**。  
4. 在 `login-page` 中确认 **`BASE_URL`** 后执行 **`npm run dev`**。  
5. 浏览器打开 Vite 地址，完成登录后再测竞赛、消息、审核等业务。

---

## 目录速览

**后端**（`src/main/java/com/yuki/webapp/`）：

- `contoller/` — 各 REST 控制器（用户、竞赛、消息、AI、报名审核、检索等）  
- `config/` — Web、Qdrant 等 Spring 配置  
- `service/`、`mapper/` — 业务与 MyBatis 映射  

**前端**（`login-page/src/`）：

- `api/` — Axios 封装与 `BASE_URL`  
- `views/`、`components/` — 页面与组件（含 `review/`、`ReviewList.vue` 等审核相关 UI）  
- `router/`、`store/` — 路由与 Vuex  

---

## 许可证 / 归属

若对仓库有开源或对内规范，请自行补充 `LICENSE` 或内部说明文档。
