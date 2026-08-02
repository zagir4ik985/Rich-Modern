export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    const allowedOrigins = (env.ALLOWED_ORIGINS || '').split(',').map(s => s.trim()).filter(Boolean);
    const origin = request.headers.get('Origin') || '';
    const corsHeaders = {
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };
    if (origin && allowedOrigins.length > 0 && allowedOrigins.includes(origin)) {
      corsHeaders['Access-Control-Allow-Origin'] = origin;
    } else if (allowedOrigins.length === 0 && origin) {
      corsHeaders['Access-Control-Allow-Origin'] = origin;
    }

    if (method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      if (path === '/api/login' && method === 'POST') {
        return await handleLogin(request, env, corsHeaders);
      }
      if (path === '/api/register' && method === 'POST') {
        return await handleRegister(request, env, corsHeaders);
      }
      if (path === '/api/user/info' && method === 'GET') {
        return await handleUserInfo(request, env, corsHeaders);
      }
      if (path === '/api/admin/login' && method === 'POST') {
        return await handleAdminLogin(request, env, corsHeaders);
      }
      if (path === '/api/admin/users' && method === 'GET') {
        return await handleAdminGetUsers(request, env, corsHeaders);
      }
      if (path === '/api/admin/users/add' && method === 'POST') {
        return await handleAdminAddUser(request, env, corsHeaders);
      }
      if (path === '/api/admin/users/delete' && method === 'POST') {
        return await handleAdminDeleteUser(request, env, corsHeaders);
      }
      if (path === '/api/admin/users/toggle' && method === 'POST') {
        return await handleAdminToggleUser(request, env, corsHeaders);
      }
      if (path === '/api/admin/users/reset-hwid' && method === 'POST') {
        return await handleAdminResetHwid(request, env, corsHeaders);
      }
      if (path === '/api/admin/migrate-uids' && method === 'POST') {
        return await handleAdminMigrateUids(request, env, corsHeaders);
      }
      if (path === '/api/admin/stats' && method === 'GET') {
        return await handleAdminStats(request, env, corsHeaders);
      }
      if (path === '/api/key' && method === 'GET') {
        return await handleGetKey(request, env, corsHeaders);
      }
      if (path === '/api/mod/version' && method === 'GET') {
        return await handleModVersion(request, env, corsHeaders);
      }
      if (path === '/api/logout' && method === 'POST') {
        return await handleLogout(request, env, corsHeaders);
      }

      return new Response(JSON.stringify({ error: 'Not found' }), {
        status: 404,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    } catch (e) {
      return new Response(JSON.stringify({ error: 'Internal server error' }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }
  },
};

// --- Helper functions ---

async function hmacSign(data, secret) {
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(data));
  return btoa(String.fromCharCode(...new Uint8Array(sig))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function hashPassword(password, salt) {
  const data = new TextEncoder().encode(salt + ':' + password);
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    data,
    { name: 'PBKDF2' },
    false,
    ['deriveBits']
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: new TextEncoder().encode(salt),
      iterations: 100000,
      hash: 'SHA-256',
    },
    keyMaterial,
    256
  );
  return Array.from(new Uint8Array(derivedBits)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function verifyPassword(password, storedHash, salt) {
  const newHash = await hashPassword(password, salt);
  return timingSafeStringEqual(newHash, storedHash);
}

function timingSafeStringEqual(a, b) {
  const enc = new TextEncoder();
  const bufA = enc.encode(a);
  const bufB = enc.encode(b);
  if (bufA.length !== bufB.length) {
    return false;
  }
  let result = 0;
  for (let i = 0; i < bufA.length; i++) {
    result |= bufA[i] ^ bufB[i];
  }
  return result === 0;
}

async function generateToken(login, env) {
  let jwtSecret = null;
  try { jwtSecret = env.JWT_SECRET; } catch(e) {}
  if (!jwtSecret) throw new Error('JWT_SECRET not configured');

  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({
    login,
    exp: Date.now() + 24 * 60 * 60 * 1000,
    iat: Date.now(),
  }));
  const signature = await hmacSign(`${header}.${payload}`, jwtSecret);
  return `${header}.${payload}.${signature}`;
}

function base64UrlDecode(str) {
  let base64 = str.replace(/-/g, '+').replace(/_/g, '/');
  while (base64.length % 4) base64 += '=';
  return atob(base64);
}

async function parseToken(token, env) {
  try {
    let jwtSecret = null;
    try { jwtSecret = env.JWT_SECRET; } catch(e) {}
    if (!jwtSecret) return null;

    const parts = token.split('.');
    if (parts.length !== 3) return null;

    const expectedSig = await hmacSign(`${parts[0]}.${parts[1]}`, jwtSecret);
    if (!timingSafeStringEqual(parts[2], expectedSig)) return null;

    const payload = JSON.parse(base64UrlDecode(parts[1]));
    if (typeof payload.login !== 'string') return null;
    if (payload.exp < Date.now()) return null;

    const revoked = await env.SESSIONS.get(`revoked:${token}`);
    if (revoked) return null;

    return payload;
  } catch {
    return null;
  }
}

function getBearerToken(request) {
  const auth = request.headers.get('Authorization');
  if (!auth || !auth.startsWith('Bearer ')) return null;
  return auth.slice(7);
}

async function checkRateLimit(ip, env, endpoint) {
  const key = `ratelimit:${endpoint}:${ip}`;
  const max = parseInt(env.RATE_LIMIT_MAX || '5');
  const window = parseInt(env.RATE_LIMIT_WINDOW || '60');
  const current = await env.SESSIONS.get(key);
  if (current && parseInt(current) >= max) {
    return false;
  }
  const count = current ? parseInt(current) + 1 : 1;
  await env.SESSIONS.put(key, count.toString(), { expirationTtl: window });
  return true;
}

async function getNextUid(env) {
  for (let attempt = 0; attempt < 20; attempt++) {
    const current = await env.USERS.get('config:next_uid');
    const next = current ? parseInt(current) + 1 : 1;
    await env.USERS.put('config:next_uid', next.toString());
    const verify = await env.USERS.get('config:next_uid');
    if (parseInt(verify) === next) return next;
    await new Promise(r => setTimeout(r, 50 * (attempt + 1)));
  }
  throw new Error('Failed to assign unique UID');
}

function formatUid(uid) {
  return uid.toString().padStart(5, '0');
}

async function deriveUserKey(masterKey, login) {
  const data = new TextEncoder().encode(masterKey + ':' + login + ':zagaDLC');
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    data,
    { name: 'PBKDF2' },
    false,
    ['deriveBits']
  );
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: new TextEncoder().encode('zagaDLC-user-key'),
      iterations: 100000,
      hash: 'SHA-256',
    },
    keyMaterial,
    256
  );
  return btoa(String.fromCharCode(...new Uint8Array(derivedBits))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function isValidDate(dateStr) {
  if (!dateStr || typeof dateStr !== 'string') return false;
  const d = new Date(dateStr);
  return !isNaN(d.getTime());
}

async function listAllUsers(env) {
  const users = [];
  let cursor = undefined;
  do {
    const opts = { prefix: 'user:' };
    if (cursor) opts.cursor = cursor;
    const list = await env.USERS.list(opts);
    for (const key of list.keys) {
      const data = await env.USERS.get(key.name);
      if (data) {
        const user = JSON.parse(data);
        users.push({
          login: user.login,
          uid: user.uid ? formatUid(user.uid) : null,
          hwid: user.hwid || null,
          created_at: user.created_at,
          expires_at: user.expires_at,
          banned: user.banned,
        });
      }
    }
    cursor = list.cursor;
  } while (cursor);
  return users;
}

// --- Auth handlers ---

async function handleLogin(request, env, corsHeaders) {
  const body = await request.json();
  const login = typeof body.login === 'string' ? body.login : '';
  const password = typeof body.password === 'string' ? body.password : '';
  const hwid = typeof body.hwid === 'string' ? body.hwid : '';

  if (!login || !password) {
    return new Response(JSON.stringify({ error: 'Login and password required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
  if (!(await checkRateLimit(ip, env, 'login'))) {
    return new Response(JSON.stringify({ error: 'Too many attempts. Try again later.' }), {
      status: 429,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const userData = await env.USERS.get(`user:${login}`);
  if (!userData) {
    return new Response(JSON.stringify({ error: 'Invalid credentials' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const user = JSON.parse(userData);

  if (user.banned) {
    return new Response(JSON.stringify({ error: 'Account is banned' }), {
      status: 403,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (user.expires_at && isValidDate(user.expires_at) && new Date(user.expires_at) < new Date()) {
    return new Response(JSON.stringify({ error: 'License expired' }), {
      status: 403,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const valid = await verifyPassword(password, user.password_hash, user.salt);
  if (!valid) {
    return new Response(JSON.stringify({ error: 'Invalid credentials' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (hwid) {
    if (user.hwid && user.hwid !== hwid) {
      return new Response(JSON.stringify({ error: 'License bound to another machine' }), {
        status: 403,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }
    if (!user.hwid) {
      user.hwid = hwid;
      await env.USERS.put(`user:${login}`, JSON.stringify(user));
    }
  }

  const token = await generateToken(login, env);

  return new Response(JSON.stringify({
    success: true,
    token,
    user: {
      login: user.login,
      uid: user.uid ? formatUid(user.uid) : null,
      hwid: user.hwid || null,
      expires_at: user.expires_at || null,
    },
  }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleRegister(request, env, corsHeaders) {
  const config = await env.USERS.get('config:registration');
  const configData = config ? JSON.parse(config) : { enabled: false };

  if (!configData.enabled) {
    return new Response(JSON.stringify({ error: 'Registration is disabled' }), {
      status: 403,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
  if (!(await checkRateLimit(ip, env, 'register'))) {
    return new Response(JSON.stringify({ error: 'Too many attempts. Try again later.' }), {
      status: 429,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const body = await request.json();
  const login = typeof body.login === 'string' ? body.login : '';
  const password = typeof body.password === 'string' ? body.password : '';
  const hwid = typeof body.hwid === 'string' ? body.hwid : null;

  if (!login || !password) {
    return new Response(JSON.stringify({ error: 'Login and password required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (login.length < 3 || login.length > 16) {
    return new Response(JSON.stringify({ error: 'Login must be 3-16 characters' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (!/^[a-zA-Z0-9_]+$/.test(login)) {
    return new Response(JSON.stringify({ error: 'Login must contain only letters, numbers, and underscores' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (password.length < 6 || password.length > 128) {
    return new Response(JSON.stringify({ error: 'Password must be 6-128 characters' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const existing = await env.USERS.get(`user:${login}`);
  if (existing) {
    return new Response(JSON.stringify({ error: 'Login already taken' }), {
      status: 409,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const salt = crypto.randomUUID();
  const password_hash = await hashPassword(password, salt);
  const uid = await getNextUid(env);

  await env.USERS.put(`user:${login}`, JSON.stringify({
    login,
    uid,
    password_hash,
    salt,
    hwid: hwid || null,
    created_at: new Date().toISOString(),
    expires_at: null,
    banned: false,
  }));

  return new Response(JSON.stringify({ success: true, uid: formatUid(uid) }), {
    status: 201,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleUserInfo(request, env, corsHeaders) {
  const token = getBearerToken(request);
  if (!token) {
    return new Response(JSON.stringify({ error: 'No token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const payload = await parseToken(token, env);
  if (!payload) {
    return new Response(JSON.stringify({ error: 'Invalid token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const userData = await env.USERS.get(`user:${payload.login}`);
  if (!userData) {
    return new Response(JSON.stringify({ error: 'User not found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const user = JSON.parse(userData);

  return new Response(JSON.stringify({
    login: user.login,
    uid: user.uid ? formatUid(user.uid) : null,
    hwid: user.hwid || null,
    expires_at: user.expires_at || null,
    banned: user.banned,
  }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

// --- Admin handlers ---

async function checkAdminAuth(request, env) {
  const token = getBearerToken(request);
  if (!token) return false;
  const payload = await parseToken(token, env);
  return payload && payload.login === '__admin__';
}

async function handleAdminLogin(request, env, corsHeaders) {
  try {
    const body = await request.json();
    const password = typeof body.password === 'string' ? body.password : '';

    const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
    if (!(await checkRateLimit(ip, env, 'admin-login'))) {
      return new Response(JSON.stringify({ error: 'Too many attempts. Try again later.' }), {
        status: 429,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }

    let adminPw = null;
    try { adminPw = env.ADMIN_PASSWORD; } catch(e) {}
    if (!password || !adminPw || !timingSafeStringEqual(password.trim(), adminPw.trim())) {
      return new Response(JSON.stringify({ error: 'Invalid admin password' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }

    const token = await generateToken('__admin__', env);

    return new Response(JSON.stringify({ success: true, token }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  } catch(e) {
    return new Response(JSON.stringify({ error: 'Admin login error: ' + e.message }), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }
}

async function handleAdminGetUsers(request, env, corsHeaders) {
  if (!await checkAdminAuth(request, env)) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const users = await listAllUsers(env);
  users.sort((a, b) => {
    if (a.uid && b.uid) return a.uid.localeCompare(b.uid);
    if (a.uid) return -1;
    if (b.uid) return 1;
    return (a.login || '').localeCompare(b.login || '');
  });

  return new Response(JSON.stringify({ users }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleAdminAddUser(request, env, corsHeaders) {
  if (!await checkAdminAuth(request, env)) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const body = await request.json();
  const login = typeof body.login === 'string' ? body.login : '';
  const password = typeof body.password === 'string' ? body.password : '';
  const expires_at = typeof body.expires_at === 'string' ? body.expires_at : null;

  if (!login || !password) {
    return new Response(JSON.stringify({ error: 'Login and password required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (login.length < 3 || login.length > 16) {
    return new Response(JSON.stringify({ error: 'Login must be 3-16 characters' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (!/^[a-zA-Z0-9_]+$/.test(login)) {
    return new Response(JSON.stringify({ error: 'Login must contain only letters, numbers, and underscores' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (password.length < 6 || password.length > 128) {
    return new Response(JSON.stringify({ error: 'Password must be 6-128 characters' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  if (expires_at && !isValidDate(expires_at)) {
    return new Response(JSON.stringify({ error: 'Invalid expires_at date format (use YYYY-MM-DD)' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const existing = await env.USERS.get(`user:${login}`);
  if (existing) {
    return new Response(JSON.stringify({ error: 'Login already exists' }), {
      status: 409,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const salt = crypto.randomUUID();
  const password_hash = await hashPassword(password, salt);
  const uid = await getNextUid(env);

  await env.USERS.put(`user:${login}`, JSON.stringify({
    login,
    uid,
    password_hash,
    salt,
    hwid: null,
    created_at: new Date().toISOString(),
    expires_at: expires_at || null,
    banned: false,
  }));

  return new Response(JSON.stringify({ success: true, uid: formatUid(uid) }), {
    status: 201,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleAdminDeleteUser(request, env, corsHeaders) {
  if (!await checkAdminAuth(request, env)) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const body = await request.json();
  const login = typeof body.login === 'string' ? body.login : '';

  if (!login) {
    return new Response(JSON.stringify({ error: 'Login required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const existing = await env.USERS.get(`user:${login}`);
  if (!existing) {
    return new Response(JSON.stringify({ error: 'User not found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  await env.USERS.delete(`user:${login}`);

  return new Response(JSON.stringify({ success: true }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleAdminToggleUser(request, env, corsHeaders) {
  if (!await checkAdminAuth(request, env)) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const body = await request.json();
  const login = typeof body.login === 'string' ? body.login : '';

  if (!login) {
    return new Response(JSON.stringify({ error: 'Login required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const userData = await env.USERS.get(`user:${login}`);
  if (!userData) {
    return new Response(JSON.stringify({ error: 'User not found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const user = JSON.parse(userData);
  user.banned = !user.banned;
  await env.USERS.put(`user:${login}`, JSON.stringify(user));

  return new Response(JSON.stringify({ success: true, banned: user.banned }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleAdminMigrateUids(request, env, corsHeaders) {
  if (!await checkAdminAuth(request, env)) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const users = await listAllUsers(env);
  const usersWithoutUid = [];

  for (const u of users) {
    if (!u.uid) {
      usersWithoutUid.push(u);
    }
  }

  usersWithoutUid.sort((a, b) => {
    const dateA = a.created_at || '';
    const dateB = b.created_at || '';
    return dateA.localeCompare(dateB);
  });

  const currentMax = await env.USERS.get('config:next_uid');
  let counter = currentMax ? parseInt(currentMax) : 0;
  let migrated = 0;

  for (const u of usersWithoutUid) {
    counter++;
    const userData = await env.USERS.get(`user:${u.login}`);
    if (userData) {
      const user = JSON.parse(userData);
      user.uid = counter;
      await env.USERS.put(`user:${u.login}`, JSON.stringify(user));
      migrated++;
    }
  }

  await env.USERS.put('config:next_uid', counter.toString());

  return new Response(JSON.stringify({
    success: true,
    migrated,
    nextUid: counter,
  }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleAdminStats(request, env, corsHeaders) {
  if (!await checkAdminAuth(request, env)) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const users = await listAllUsers(env);
  let total = users.length;
  let banned = 0;
  let active = 0;

  for (const u of users) {
    if (u.banned) banned++;
    else active++;
  }

  return new Response(JSON.stringify({ total, active, banned }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleGetKey(request, env, corsHeaders) {
  const token = getBearerToken(request);
  if (!token) {
    return new Response(JSON.stringify({ error: 'No token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const payload = await parseToken(token, env);
  if (!payload) {
    return new Response(JSON.stringify({ error: 'Invalid token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
  if (!(await checkRateLimit(ip, env, 'key'))) {
    return new Response(JSON.stringify({ error: 'Too many attempts. Try again later.' }), {
      status: 429,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  let key = null;
  try { key = env.JAR_ENCRYPTION_KEY; } catch(e) {}
  if (!key) {
    return new Response(JSON.stringify({ error: 'Key not configured' }), {
      status: 500,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const userKey = await deriveUserKey(key, payload.login);
  return new Response(JSON.stringify({ key: userKey }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleAdminResetHwid(request, env, corsHeaders) {
  const adminToken = getBearerToken(request);
  if (!adminToken) {
    return new Response(JSON.stringify({ error: 'No token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }
  const adminPayload = await parseToken(adminToken, env);
  if (!adminPayload || adminPayload.login !== '__admin__') {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const body = await request.json();
  const login = typeof body.login === 'string' ? body.login : '';
  if (!login) {
    return new Response(JSON.stringify({ error: 'Login required' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const userData = await env.USERS.get(`user:${login}`);
  if (!userData) {
    return new Response(JSON.stringify({ error: 'User not found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  const user = JSON.parse(userData);
  user.hwid = null;
  await env.USERS.put(`user:${login}`, JSON.stringify(user));

  return new Response(JSON.stringify({ success: true, login }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

// --- Version endpoint ---

async function handleVersion(request, env, corsHeaders) {
  try {
    const versionObj = await env.LOADER_BUCKET.get('version.json');
    if (!versionObj) {
      return new Response(JSON.stringify({ error: 'No version available' }), {
        status: 404,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }
    const versionData = JSON.parse(await versionObj.text());
    return new Response(JSON.stringify(versionData), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: 'Version check failed: ' + e.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }
}

async function handleModVersion(request, env, corsHeaders) {
  const token = getBearerToken(request);
  if (!token) {
    return new Response(JSON.stringify({ error: 'No token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }
  const payload = await parseToken(token, env);
  if (!payload) {
    return new Response(JSON.stringify({ error: 'Invalid token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }
  return new Response(JSON.stringify({ version: '', url: '' }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}

async function handleLogout(request, env, corsHeaders) {
  const token = getBearerToken(request);
  if (!token) {
    return new Response(JSON.stringify({ error: 'No token' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json', ...corsHeaders },
    });
  }

  try {
    await env.SESSIONS.put(`revoked:${token}`, '1', { expirationTtl: 86400 });
  } catch (e) {
    // KV write failed, but that's ok
  }

  return new Response(JSON.stringify({ success: true }), {
    status: 200,
    headers: { 'Content-Type': 'application/json', ...corsHeaders },
  });
}
