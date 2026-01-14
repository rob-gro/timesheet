let allUsers = [];
const API_BASE_URL = '/api/v1/users';

function logout() {
    fetch('/logout', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': getCsrfToken()
        }
    })
        .then(() => {
            window.location.href = '/login';
        })
        .catch(error => {
            console.error('Logout error:', error);
            window.location.href = '/login';
        });
}

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

document.addEventListener('DOMContentLoaded', function () {
    loadUsers();

    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    document.getElementById('savePasswordBtn').addEventListener('click', changePassword);
    document.getElementById('saveRolesBtn').addEventListener('click', saveUserRoles);
});

function loadUsers() {
    console.log("Loading users...");
    fetch(API_BASE_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load users');
            }
            return response.json();
        })
        .then(data => {
            allUsers = data;
            renderUsersTable(data);
        })
        .catch(error => {
            console.error('Error loading users:', error);
            alert('Failed to load users. Please try again later.');
        });
}

function createActionButton(label, className, onClick) {
    const btn = document.createElement('a');
    btn.href = 'javascript:void(0)';
    btn.textContent = label;
    btn.className = `action-button ${className}`;
    btn.addEventListener('click', onClick);
    return btn;
}

function appendTextCell(row, text) {
    const cell = document.createElement('td');
    cell.textContent = text;
    row.appendChild(cell);
}

function renderUsersTable(users) {
    console.log("Rendering users table:", users);
    const tableBody = document.getElementById('userTableBody');
    tableBody.innerHTML = '';

    users.forEach(user => {
        const row = document.createElement('tr');

        appendTextCell(row, user.username);
        appendTextCell(row, user.email || '-');
        appendTextCell(row, user.roles ? Array.from(user.roles).join(', ').replace(/ROLE_/g, '') : '-');

        const actionsCell = document.createElement('td');
        actionsCell.className = 'text-center';

        const buttons = [
            {text: 'Edit', class: 'save-button', onClick: () => showEditUserForm(user.id)},
            {text: 'Password', class: 'save-button', onClick: () => showPasswordForm(user.id)},
            {text: 'Email Reset', class: 'save-button', onClick: () => resetPassword(user.id)},
            {text: 'Roles', class: 'save-button', onClick: () => showRolesForm(user.id)},
            {
                text: user.active ? 'Deactivate' : 'Activate',
                class: user.active ? 'del-button' : 'save-button',
                onClick: () => toggleUserActive(user.id, !user.active)
            }
        ];

        buttons.forEach(button => {
            const btn = document.createElement('button');
            btn.textContent = button.text;
            btn.className = button.class;
            btn.style.margin = '0 3px';
            btn.addEventListener('click', button.onClick);
            actionsCell.appendChild(btn);
        });

        row.appendChild(actionsCell);
        tableBody.appendChild(row);
    });
}

function showAddUserForm() {

    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('passwordSection').style.display = 'block';

    document.getElementById('userFormTitle').textContent = 'Add New User';

    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'block';
    document.getElementById('passwordFormSection').style.display = 'none';
    document.getElementById('rolesFormSection').style.display = 'none';
}

function showEditUserForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('userId').value = user.id;
    document.getElementById('username').value = user.username;
    document.getElementById('email').value = user.email || '';
    document.getElementById('active').value = user.active.toString();

    document.getElementById('passwordSection').style.display = 'none';

    document.getElementById('userFormTitle').textContent = 'Edit User';

    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'block';
    document.getElementById('passwordFormSection').style.display = 'none';
    document.getElementById('rolesFormSection').style.display = 'none';
}

function showPasswordForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('passwordForm').reset();
    document.getElementById('passwordUserId').value = user.id;

    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'none';
    document.getElementById('passwordFormSection').style.display = 'block';
    document.getElementById('rolesFormSection').style.display = 'none';
}

function showRolesForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    document.getElementById('rolesUserId').value = user.id;
    document.getElementById('rolesUsername').textContent = user.username;

    document.getElementById('roleAdmin').checked = false;
    document.getElementById('roleUser').checked = false;
    document.getElementById('roleGuest').checked = false;

    if (user.roles) {
        if (user.roles.includes('ROLE_ADMIN')) document.getElementById('roleAdmin').checked = true;
        if (user.roles.includes('ROLE_USER')) document.getElementById('roleUser').checked = true;
        if (user.roles.includes('ROLE_GUEST')) document.getElementById('roleGuest').checked = true;
    }

    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'none';
    document.getElementById('passwordFormSection').style.display = 'none';
    document.getElementById('rolesFormSection').style.display = 'block';
}

function cancelUserForm() {
    document.getElementById('userListSection').style.display = 'block';
    document.getElementById('userFormSection').style.display = 'none';
}

function cancelPasswordForm() {
    document.getElementById('userListSection').style.display = 'block';
    document.getElementById('passwordFormSection').style.display = 'none';
}

function cancelRolesForm() {
    document.getElementById('userListSection').style.display = 'block';
    document.getElementById('rolesFormSection').style.display = 'none';
}

function saveUser() {
    const userId = document.getElementById('userId').value;
    const isNewUser = !userId;

    const userData = {
        id: isNewUser ? null : parseInt(userId),
        username: document.getElementById('username').value,
        email: document.getElementById('email').value,
        active: document.getElementById('active').value === 'true'
    };

    if (isNewUser) {
        userData.password = document.getElementById('password').value;
    }

    const url = isNewUser ? API_BASE_URL : `${API_BASE_URL}/${userId}`;
    const method = isNewUser ? 'POST' : 'PUT';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to ${isNewUser ? 'create' : 'update'} user`);
            }
            return response.json();
        })
        .then(() => {
            alert(`User successfully ${isNewUser ? 'created' : 'updated'}`);
            cancelUserForm();
            loadUsers();
        })
        .catch(error => {
            console.error('Error saving user:', error);
            alert(`Failed to ${isNewUser ? 'create' : 'update'} user. Please try again.`);
        });
}

function changePassword() {
    const userId = document.getElementById('passwordUserId').value;

    const passwordData = {
        currentPassword: document.getElementById('currentPassword').value,
        newPassword: document.getElementById('newPassword').value
    };

    fetch(`${API_BASE_URL}/${userId}/password`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(passwordData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to change password');
            }
            alert('Password changed successfully');
            cancelPasswordForm();
        })
        .catch(error => {
            console.error('Error changing password:', error);
            alert('Failed to change password. Ensure the current password is correct.');
        });
}

function saveUserRoles() {
    const userId = document.getElementById('rolesUserId').value;

    const roles = [];
    if (document.getElementById('roleAdmin').checked) roles.push('ROLE_ADMIN');
    if (document.getElementById('roleUser').checked) roles.push('ROLE_USER');
    if (document.getElementById('roleGuest').checked) roles.push('ROLE_GUEST');

    fetch(`${API_BASE_URL}/${userId}/roles`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({roleNames: roles})
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to update roles');
            }
            return response.json();
        })
        .then(() => {
            alert('User roles updated successfully');
            cancelRolesForm();
            loadUsers();
        })
        .catch(error => {
            console.error('Error updating roles:', error);
            alert('Failed to update user roles. Please try again.');
        });
}

function toggleUserActive(userId, active) {
    const url = `${API_BASE_URL}/${userId}/${active ? 'activate' : 'deactivate'}`;

    fetch(url, {
        method: 'PUT'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to ${active ? 'activate' : 'deactivate'} user`);
            }
            return response.json();
        })
        .then(() => {
            alert(`User ${active ? 'activated' : 'deactivated'} successfully`);
            loadUsers();
        })
        .catch(error => {
            console.error('Error toggling user status:', error);
            alert(`Failed to ${active ? 'activate' : 'deactivate'} user. Please try again.`);
        });
}

/**
 * Send password reset email (new flow - Step 10)
 * Uses token-based reset link instead of temporary password
 */
function resetPassword(userId) {
    if (confirm('Send password reset email to this user?')) {
        const csrfToken = getCsrfToken();
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

        fetch(`/api/admin/users/${userId}/reset-password`, {
            method: 'POST',
            headers: { [csrfHeader]: csrfToken }
        })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert('Error: ' + data.error);
            } else {
                alert('✓ ' + data.message);
            }
            loadUsers();
        })
        .catch(error => {
            console.error('Reset password error:', error);
            alert('Failed to send reset email. Check console.');
        });
    }
}

/**
 * Reset user password with temporary password (legacy flow)
 * DEPRECATED: Use resetPassword() for new token-based flow
 */
function resetUserPassword(userId) {
    if (confirm('Are you sure you want to reset this user\'s password? The temporary password will be displayed only once.')) {
        fetch(`${API_BASE_URL}/${userId}/reset-password`, {
            method: 'PUT'
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to reset password');
                }
                return response.json();
            })
            .then(data => {
                const tempPassword = data.tempPassword;

                const modal = document.createElement('div');
                modal.className = 'modal';

                modal.innerHTML = `
                    <h3>Temporary Password for ${data.username}</h3>
                    <p>The new password is:</p>
                    <div class="password-container">${tempPassword}</div>
                    <p class="warning">
                        Please copy this password now. It will not be shown again.<br>
                        <span>SECURITY WARNING: Instruct the user to change this password immediately after first login!</span>
                    </p>
                    <button id="closeModal">Close</button>
                `;

                document.body.appendChild(modal);

                document.getElementById('closeModal').addEventListener('click', () => {
                    document.body.removeChild(modal);
                });
            })
            .catch(error => {
                console.error('Error resetting password:', error);
                alert('Failed to reset password. Please try again.');
            });
    }
}
