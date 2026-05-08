# Campus Assistant — Full Stack

This documentation describes two related projects:

| Part | Location | Role |
|------|----------|------|
| **Backend** | This repository (`web-app`) | Spring Boot REST API, MyBatis, JWT auth, mail, AI (DashScope), Qdrant, Meilisearch |
| **Frontend** | Separate checkout: `login-page` (Vue 3 + Vite) | Web UI: login/register, competitions, messages, AI chat, **application review**, etc. |

The frontend calls the backend over HTTP. API base URL is configured in the frontend (default `http://localhost:8080`).

---

## Prerequisites

### Backend

- **JDK 17**
- **Apache Maven 3.8+**
- **MySQL 5.7** (schema expected by the application)
- Optional / feature-dependent:
  - **Redis** — dependency is present; some Redis usage may be optional depending on your branch
  - **Qdrant** (gRPC) — competition vectors (`qdrant.*` in config)
  - **Meilisearch** — competition search (`meilisearch.*` in config)
  - **Alibaba DashScope** — embeddings + chat for AI features (`dashscope.*` in config)
  - **SMTP** — mail (`spring.mail.*` in config)

### Frontend

- **Node.js 18+** (LTS recommended)
- **npm** (or compatible package manager)

---

## Backend (`web-app`)

### Tech stack

- Spring Boot **3.4.x**
- MyBatis + PageHelper
- MySQL
- JWT (`java-jwt`)
- OkHttp (DashScope HTTP)
- Qdrant Java client + gRPC
- Meilisearch Java SDK

### Configuration

Main file: `src/main/resources/application.yml`.

Configure at least:

- `spring.datasource.*` — MySQL URL, user, password  
- `spring.mail.*` — if you use verification / mail features  
- `dashscope.*` — API key and models for AI  
- `qdrant.*` — host, gRPC port, collection, vector size (must match embedding dim)  
- `meilisearch.*` — host, API key, index name  

**Do not commit real secrets.** Prefer environment-specific files, Spring profiles, or environment variables, and keep secrets out of version control.

### Run the API

From this directory:

```bash
mvn spring-boot:run
```

Default HTTP port: **8080** (`server.port` in `application.yml`).

### Tests

```bash
mvn test
```

### CORS & auth

- CORS is enabled broadly in `WebConfig` for browser access during development.  
- Most routes go through `LoginInterceptor`; paths such as `/user/login` and `/user/register` are excluded (see `WebConfig` for the full exclude list).  
- Protected calls from the frontend should send the `Authorization` header as implemented in your JWT utilities.

---

## Frontend (`login-page`)

The UI lives in a **separate** repository/folder (e.g. `d:\svnProject\web1\login-page`). The `package.json` name is `register-page`; the app is Vue **3** with **Vite 6**, **Vue Router**, **Vuex**, and **Axios**.

### Point the UI at your API

Edit `src/api/config.js`:

```js
export const BASE_URL = 'http://localhost:8080';
```

Change host/port when the backend is not local.

### Install and dev server

```bash
cd /path/to/login-page
npm install
npm run dev
```

Vite’s dev server URL is printed in the terminal (commonly `http://localhost:5173`).

### Production build

```bash
npm run build
```

Output: `dist/`. Serve `dist` with any static file server or reverse proxy, and ensure the API base URL matches your deployment.

---

## Typical local workflow

1. Start MySQL (and Qdrant / Meilisearch / external services if you use those features).  
2. Apply DB migrations or schema as required by your team (not shipped in this README).  
3. Run **`mvn spring-boot:run`** in `web-app`.  
4. In `login-page`, set **`BASE_URL`**, then **`npm run dev`**.  
5. Open the Vite URL in the browser and exercise login → authenticated features.

---

## Project map (quick)

**Backend** (under `src/main/java/com/yuki/webapp/`):

- `contoller/` — REST controllers (auth, user, competitions, messages, AI chat, application review, search, etc.)  
- `config/` — Spring configuration (web, Qdrant, etc.)  
- `service/`, `mapper/` — business logic and MyBatis mappers  

**Frontend** (under `login-page/src/`):

- `api/` — Axios instance and `BASE_URL`  
- `views/`, `components/` — pages including review UI (`review/`, `ReviewList.vue`, etc.)  
- `router/`, `store/` — routing and Vuex state  

---

## License / ownership

Internal or team-specific; add your license file if you publish the code.
