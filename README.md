# 🧠 Muse — AI Companion

Your personalized AI companion with memory, personality, and long-term context. Runs free locally using Ollama.

---

## 📱 Native Android app (new)

A native, local-first Android build of Muse is under active development in [`android/`](android/), rebuilt around a **zero-trust security core** (permission broker, encrypted memory, auditable tools) so it can run entirely on your phone with an on-device model.

- **Build & install (free, no local toolchain):** [`docs/BUILD-AND-INSTALL.md`](docs/BUILD-AND-INSTALL.md) — GitHub Actions builds the APK for you to download.
- **What works today vs. what doesn't (honest):** [`docs/09-feature-status.md`](docs/09-feature-status.md)
- **Architecture, security, model selection, roadmap:** [`docs/`](docs/)

The web stack below is the original Muse and is unchanged.

---

## Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React + Vite + TailwindCSS |
| Backend | Node.js + Express |
| Database | MongoDB (local or Atlas) |
| Semantic Memory | ChromaDB |
| AI Model | Ollama (Llama 3 / DeepSeek / Gemma) |
| Android | Capacitor (future) |

## Quick Start

### 1. Install Ollama & pull a model

```bash
# Install Ollama from https://ollama.com
ollama run llama3
```

### 2. Start MongoDB

```bash
# Local MongoDB or connection string in backend/.env
mongod
```

### 3. Start Backend

```bash
cd backend
cp .env.example .env
npm install
npm start
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

### 5. Open Muse

```
http://localhost:5173
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/chat` | Send a message |
| GET | `/api/chat/sessions` | List sessions |
| GET | `/api/chat/history/:id` | Get session history |
| DELETE | `/api/chat/history/:id` | Delete session |
| GET | `/api/memory` | Get all memories |
| GET | `/api/memory/stats` | Memory statistics |
| DELETE | `/api/memory/:id` | Delete memory |
| GET | `/api/personality` | Get personality settings |
| PUT | `/api/personality` | Update personality |
| GET | `/api/status` | Service health check |

## Features

### AI Agent Capabilities

Muse can **act on your device** — not just chat:

| Mode | Tools Available |
|------|----------------|
| 🟢 **Online** | All local tools + web search, browse URLs, API calls |
| 🟡 **Offline** | Apps, files, keyboard input, system commands, media |

### Offline Tools (always available)
- **Open/close apps** — Launch anything (Chrome, Notepad, Spotify, etc.)
- **Type text** — Simulate keyboard input via PowerShell
- **File operations** — Read, write, search filesystem
- **System commands** — Run shell commands with safety guardrails
- **Play media** — Play music/videos with default player
- **System info** — CPU, memory, processes, network status

### Online Tools (when connected)
- **Web search** — DuckDuckGo integration (no API key needed)
- **Read URLs** — Fetch and extract content from web pages
- **API requests** — Make HTTP requests to any API

### Core Features
- **Persistent Memory** — Remembers preferences, emotions, facts, goals
- **Personality Engine** — 6 adjustable traits + 5 communication styles
- **Semantic Search** — ChromaDB-powered meaning-based memory retrieval
- **Dark Mode UI** — Glassmorphism design with purple gradient accents
- **Chat History** — Persistent session management
- **Local AI** — Free, private, no API bills

## Architecture

```
USER MESSAGE
     ↓
FRONTEND (React + Vite)
     ↓
BACKEND (Node.js + Express)
     ├─ Agent Service (tool dispatch)
     ├─ Memory Engine (ChromaDB + MongoDB)
     ├─ Connectivity Monitor (online/offline)
     └─ Ollama (local AI)
           ↓
TOOL EXECUTION
     ├─ Offline: Apps, Files, System, Input, Media
     └─ Online: Web Search, Browse, API Calls
           ↓
RESPONSE → Save Memory → Display to User
```

## Project Structure

```
backend/
 ├── routes/        # API routes (chat, memory, personality)
 ├── models/        # Mongoose schemas
 ├── services/      # Business logic
 │   ├── tools/     # Agent tools (app, system, file, input, media, browser)
 │   ├── toolRegistry.js  # Tool registration & dispatch
 │   ├── agentService.js  # AI agent loop (tool selection + execution)
 │   ├── connectivityMonitor.js  # Online/offline detection
 │   ├── ollamaService.js  # Local AI integration
 │   └── contextBuilder.js  # Prompt assembly
 ├── memory/        # Memory engine + ChromaDB
 ├── prompts/       # AI system prompts
 └── server.js      # Express entry point

frontend/
 ├── src/
 │   ├── components/
 │   │   ├── Chat/     # Chat UI (container, messages, input)
 │   │   ├── Memory/   # Memory viewer
 │   │   ├── Settings/ # Personality settings
 │   │   ├── Agent/    # Agent panel (status, tool calls)
 │   │   └── Layout/   # Sidebar
 │   ├── hooks/        # React hooks (useChat, useMemories)
 │   └── services/     # API client
 └── vite.config.js
```
