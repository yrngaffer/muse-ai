# Deploy the Muse web app (free)

Frontend → **Vercel**, backend → **Render**, database → **MongoDB Atlas**, AI → **OpenRouter**.
All free tiers, no card required. The code on branch `main` of `yrngaffer/muse-ai` is already wired for this.

Do the parts in order. You create the accounts and paste the keys into each dashboard — none of the
secrets go into git. Total time ~15 min.

---

## 1. MongoDB Atlas — the database (free M0)

1. Sign up: https://www.mongodb.com/cloud/atlas/register
2. Create a **free M0** cluster (any provider/region; the defaults are fine).
3. **Database Access** → Add New Database User → set a username + password (save them).
4. **Network Access** → Add IP Address → **Allow access from anywhere** (`0.0.0.0/0`).
   (Render's IP isn't fixed, so this is needed.)
5. **Connect** → **Drivers** → copy the connection string. Replace `<username>`/`<password>` and add
   the db name `muse` before the `?`:
   ```
   mongodb+srv://USER:PASS@cluster0.xxxxx.mongodb.net/muse?retryWrites=true&w=majority
   ```
   → This is your **`MONGODB_URI`**.

## 2. OpenRouter — the AI key (free)

1. Sign up: https://openrouter.ai
2. https://openrouter.ai/keys → **Create Key** → copy it (starts with `sk-or-...`).
   → This is your **`OPENROUTER_API_KEY`**. Free models allow ~50 requests/day.

## 3. Render — the backend

1. https://dashboard.render.com → sign in with GitHub.
2. **New → Web Service** → connect **`yrngaffer/muse-ai`**.
3. Settings:
   - **Branch:** `main`
   - **Root Directory:** `backend`
   - **Build Command:** `npm install`
   - **Start Command:** `node server.js`
   - **Instance Type:** Free
4. **Environment Variables** (Advanced):
   | Key | Value |
   |-----|-------|
   | `MONGODB_URI` | *(from step 1)* |
   | `OPENROUTER_API_KEY` | *(from step 2)* |
   | `OPENROUTER_MODEL` | `meta-llama/llama-3.1-8b-instruct:free` *(optional)* |
   | `AI_PROVIDER` | `openrouter` *(optional — Render is auto-detected anyway)* |
5. **Create Web Service**, wait ~2–3 min.
6. Copy the URL, e.g. `https://muse-backend-xxxx.onrender.com`. Test it:
   `https://muse-backend-xxxx.onrender.com/api/status` → JSON showing `mongodb: connected`, `ai: connected`.
   → This is your **backend URL**.

> ⚠️ Render free sleeps after ~15 min idle; the first request after that takes ~50s to wake.

## 4. Vercel — the frontend (your public link)

1. https://vercel.com → sign in with GitHub.
2. **Add New → Project** → import **`yrngaffer/muse-ai`**.
3. Configure:
   - **Root Directory:** `frontend`
   - **Framework Preset:** Vite *(auto-detected)* — build `npm run build`, output `dist` (defaults)
4. **Environment Variables:**
   | Key | Value |
   |-----|-------|
   | `VITE_API_URL` | your backend URL from step 3 — **no trailing slash** |
5. **Deploy**, wait ~1 min.
6. Your app is live at `https://muse-ai-xxxx.vercel.app` — **that's your link.**

## 5. Verify

- Open the Vercel URL. The top badge should flip from **Offline** to **Online**, and chatting should
  get a reply (the first one is slow: Render waking + model cold start).
- CORS is already handled — the backend allows any `*.vercel.app` origin. If you ever use a custom
  domain, add `FRONTEND_URL=https://yourdomain` to Render's env and redeploy.

---

## What you get / limits (honest)

- **Works:** chat (OpenRouter free model), chat history + structured memory + personality (MongoDB).
- **Off:** semantic memory (ChromaDB) — there's no Chroma server, and the app degrades cleanly without it.
- **Free-tier limits:** Render sleeps when idle (~50s cold start); OpenRouter free ≈ 50 messages/day;
  Atlas M0 is 512 MB.
- **Secrets** live only in Render/Vercel env vars, never in the repo.
