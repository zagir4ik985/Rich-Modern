const API_URL = 'https://zagadlc.zagir4ik985.workers.dev';

let adminToken = null;
let isLoading = false;

async function apiCall(path, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (adminToken) headers['Authorization'] = `Bearer ${adminToken}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(`${API_URL}${path}`, opts);
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Request failed');
    return data;
}

function setLoading(loading) {
    isLoading = loading;
    document.querySelectorAll('button').forEach(btn => {
        if (loading) {
            btn.dataset.prevDisabled = btn.disabled;
            btn.disabled = true;
        } else {
            btn.disabled = btn.dataset.prevDisabled === 'true';
        }
    });
}

function showScreen(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(id).classList.add('active');
}

async function adminLogin() {
    if (isLoading) return;
    const password = document.getElementById('admin-password').value;
    const errorEl = document.getElementById('login-error');
    errorEl.textContent = '';

    if (!password) {
        errorEl.textContent = 'Password required';
        return;
    }

    setLoading(true);
    try {
        const data = await apiCall('/api/admin/login', 'POST', { password });
        adminToken = data.token;
        showScreen('dashboard-screen');
        loadDashboard();
    } catch (e) {
        errorEl.textContent = e.message;
    } finally {
        setLoading(false);
    }
}

function logout() {
    adminToken = null;
    showScreen('login-screen');
    document.getElementById('admin-password').value = '';
    document.getElementById('login-error').textContent = '';
}

async function loadDashboard() {
    try {
        const stats = await apiCall('/api/admin/stats');
        document.getElementById('stat-total').textContent = stats.total;
        document.getElementById('stat-active').textContent = stats.active;
        document.getElementById('stat-banned').textContent = stats.banned;
        document.getElementById('stats-text').textContent = `${stats.active} active / ${stats.total} total`;
    } catch (e) {
        console.error('Stats error:', e);
    }

    await loadUsers();
}

async function migrateUids() {
    if (!confirm('Assign UIDs to all users without one?')) return;
    setLoading(true);
    try {
        const data = await apiCall('/api/admin/migrate-uids', 'POST');
        alert(`Done! Migrated ${data.migrated} users. Next UID: ${data.nextUid}`);
        loadDashboard();
    } catch (e) {
        alert(e.message);
    } finally {
        setLoading(false);
    }
}

async function loadUsers() {
    try {
        const data = await apiCall('/api/admin/users');
        const tbody = document.getElementById('users-body');
        const noUsers = document.getElementById('no-users');

        if (data.users.length === 0) {
            tbody.innerHTML = '';
            noUsers.style.display = 'block';
            return;
        }

        noUsers.style.display = 'none';
        tbody.innerHTML = data.users.map(u => `
            <tr>
                <td><span class="uid-badge">${u.uid || '—'}</span></td>
                <td><strong>${escapeHtml(u.login)}</strong></td>
                <td style="color: var(--text-dim); font-size: 12px; font-family: monospace;">${u.hwid ? escapeHtml(u.hwid.substring(0, 24)) + '...' : '—'}</td>
                <td style="color: var(--text-dim);">${u.created_at ? new Date(u.created_at).toLocaleDateString() : '—'}</td>
                <td style="color: var(--text-dim);">${u.expires_at ? new Date(u.expires_at).toLocaleDateString() : '∞'}</td>
                <td><span class="status-badge ${u.banned ? 'status-banned' : 'status-active'}">${u.banned ? 'Banned' : 'Active'}</span></td>
                <td>
                    <button class="btn-sm btn-warning" onclick="toggleUser('${escapeHtml(u.login)}', this)">${u.banned ? 'Unban' : 'Ban'}</button>
                    <button class="btn-sm btn-info" onclick="resetHwid('${escapeHtml(u.login)}')">Reset HWID</button>
                    <button class="btn-sm btn-danger" onclick="deleteUser('${escapeHtml(u.login)}')">Delete</button>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Users error:', e);
    }
}

function showAddUser() {
    document.getElementById('add-user-modal').classList.add('active');
    document.getElementById('new-login').value = '';
    document.getElementById('new-password').value = '';
    document.getElementById('new-expires').value = '';
    document.getElementById('add-error').textContent = '';
}

function closeModal() {
    document.getElementById('add-user-modal').classList.remove('active');
}

async function addUser() {
    if (isLoading) return;
    const login = document.getElementById('new-login').value.trim();
    const password = document.getElementById('new-password').value;
    const expires = document.getElementById('new-expires').value.trim();
    const errorEl = document.getElementById('add-error');
    errorEl.textContent = '';

    if (!login || !password) {
        errorEl.textContent = 'Login and password required';
        return;
    }

    setLoading(true);
    try {
        const body = { login, password };
        if (expires) body.expires_at = expires;
        await apiCall('/api/admin/users/add', 'POST', body);
        closeModal();
        loadDashboard();
    } catch (e) {
        errorEl.textContent = e.message;
    } finally {
        setLoading(false);
    }
}

async function deleteUser(login) {
    if (!confirm(`Delete user "${login}"? This cannot be undone.`)) return;
    setLoading(true);
    try {
        await apiCall('/api/admin/users/delete', 'POST', { login });
        loadDashboard();
    } catch (e) {
        alert(e.message);
    } finally {
        setLoading(false);
    }
}

async function toggleUser(login, btn) {
    const action = btn.textContent === 'Ban' ? 'ban' : 'unban';
    if (!confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} user "${login}"?`)) return;
    setLoading(true);
    try {
        await apiCall('/api/admin/users/toggle', 'POST', { login });
        loadDashboard();
    } catch (e) {
        alert(e.message);
    } finally {
        setLoading(false);
    }
}

async function resetHwid(login) {
    if (!confirm(`Reset HWID for ${login}? They will need to re-authenticate from their machine.`)) return;
    setLoading(true);
    try {
        await apiCall('/api/admin/users/reset-hwid', 'POST', { login });
        loadDashboard();
    } catch (e) {
        alert(e.message);
    } finally {
        setLoading(false);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML.replace(/'/g, '&#39;');
}

document.getElementById('admin-password').addEventListener('keydown', e => {
    if (e.key === 'Enter') adminLogin();
});

document.getElementById('add-user-modal').addEventListener('click', e => {
    if (e.target === e.currentTarget) closeModal();
});
