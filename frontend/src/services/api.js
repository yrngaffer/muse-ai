// In production (Vercel), set VITE_API_URL to the deployed backend (Render) origin, e.g.
// https://muse-backend-xxxx.onrender.com. In dev it's empty, so '/api' uses the Vite proxy
// to localhost:3001 (see vite.config.js).
const API_BASE = `${(import.meta.env.VITE_API_URL || '').replace(/\/+$/, '')}/api`;

async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);
    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'Request failed' }));
      throw new Error(error.error || `HTTP ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    if (error.message === 'Failed to fetch') {
      throw new Error('Backend is not running. Start it with: cd backend && npm start');
    }
    throw error;
  }
}

export const api = {
  // Chat
  chat: {
    send: (message, sessionId) =>
      request('/chat', {
        method: 'POST',
        body: JSON.stringify({ message, sessionId }),
      }),

    getHistory: (sessionId) =>
      request(`/chat/history/${sessionId}`),

    getSessions: () =>
      request('/chat/sessions'),

    deleteSession: (sessionId) =>
      request(`/chat/history/${sessionId}`, { method: 'DELETE' }),
  },

  // Memories
  memory: {
    getAll: () =>
      request('/memory'),

    search: (query) =>
      request(`/memory/search?q=${encodeURIComponent(query)}`),

    getStats: () =>
      request('/memory/stats'),

    delete: (id) =>
      request(`/memory/${id}`, { method: 'DELETE' }),
  },

  // Personality
  personality: {
    get: () =>
      request('/personality'),

    update: (data) =>
      request('/personality', {
        method: 'PUT',
        body: JSON.stringify(data),
      }),

    addNickname: (nickname) =>
      request('/personality/nickname', {
        method: 'POST',
        body: JSON.stringify({ nickname }),
      }),

    reset: () =>
      request('/personality/reset', { method: 'DELETE' }),
  },

  // Status
  status: () =>
    request('/status'),
};

export default api;
