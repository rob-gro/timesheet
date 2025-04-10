// Global variables
let allUsers = [];
const API_BASE_URL = '/api/v1/users';

// Logout function
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

// Get CSRF token from meta tag
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

// Load users when page is ready
document.addEventListener('DOMContentLoaded', function () {
    loadUsers();

    // Add event listeners to buttons
    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    document.getElementById('savePasswordBtn').addEventListener('click', changePassword);
    document.getElementById('saveRolesBtn').addEventListener('click', saveUserRoles);
});

// Load all users from API
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

// Helper function to create action buttons
function createActionButton(label, className, onClick) {
    const btn = document.createElement('a');
    btn.href = 'javascript:void(0)';
    btn.textContent = label;
    btn.className = `action-button ${className}`;
    btn.addEventListener('click', onClick);
    return btn;
}

// Helper function to append text cell to row
function appendTextCell(row, text) {
    const cell = document.createElement('td');
    cell.textContent = text;
    row.appendChild(cell);
}

// Render users table
function renderUsersTable(users) {
    console.log("Rendering users table:", users);
    const tableBody = document.getElementById('userTableBody');
    tableBody.innerHTML = '';

    users.forEach(user => {
        const row = document.createElement('tr');

        // Add data cells
        appendTextCell(row, user.username);
        appendTextCell(row, user.email || '-');
        appendTextCell(row, user.roles ? Array.from(user.roles).join(', ').replace(/ROLE_/g, '') : '-');

        // Create actions cell
        const actionsCell = document.createElement('td');
        actionsCell.className = 'text-center';

        const buttons = [
            {text: 'Edit', class: 'save-button', onClick: () => showEditUserForm(user.id)},
            {text: 'Password', class: 'save-button', onClick: () => showPasswordForm(user.id)},
            {text: 'Reset Pass', class: 'save-button', onClick: () => resetUserPassword(user.id)},
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

// Show form to add a new user
function showAddUserForm() {
    // Reset form
    document.getElementById('userForm').reset();
    document.getElementById('userId').value = '';
    document.getElementById('passwordSection').style.display = 'block';

    // Update form title
    document.getElementById('userFormTitle').textContent = 'Add New User';

    // Show form section and hide others
    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'block';
    document.getElementById('passwordFormSection').style.display = 'none';
    document.getElementById('rolesFormSection').style.display = 'none';
}

// Show form to edit a user
function showEditUserForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    // Populate form
    document.getElementById('userId').value = user.id;
    document.getElementById('username').value = user.username;
    document.getElementById('email').value = user.email || '';
    document.getElementById('active').value = user.active.toString();

    // Hide password field for edit
    document.getElementById('passwordSection').style.display = 'none';

    // Update form title
    document.getElementById('userFormTitle').textContent = 'Edit User';

    // Show form section and hide others
    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'block';
    document.getElementById('passwordFormSection').style.display = 'none';
    document.getElementById('rolesFormSection').style.display = 'none';
}

// Show form to change password
function showPasswordForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    // Reset and prepare form
    document.getElementById('passwordForm').reset();
    document.getElementById('passwordUserId').value = user.id;

    // Show form section and hide others
    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'none';
    document.getElementById('passwordFormSection').style.display = 'block';
    document.getElementById('rolesFormSection').style.display = 'none';
}

// Show form to manage user roles
function showRolesForm(userId) {
    const user = allUsers.find(u => u.id === userId);
    if (!user) return;

    // Prepare form
    document.getElementById('rolesUserId').value = user.id;
    document.getElementById('rolesUsername').textContent = user.username;

    // Reset checkboxes
    document.getElementById('roleAdmin').checked = false;
    document.getElementById('roleUser').checked = false;
    document.getElementById('roleGuest').checked = false;

    // Check appropriate role checkboxes
    if (user.roles) {
        if (user.roles.includes('ROLE_ADMIN')) document.getElementById('roleAdmin').checked = true;
        if (user.roles.includes('ROLE_USER')) document.getElementById('roleUser').checked = true;
        if (user.roles.includes('ROLE_GUEST')) document.getElementById('roleGuest').checked = true;
    }

    // Show form section and hide others
    document.getElementById('userListSection').style.display = 'none';
    document.getElementById('userFormSection').style.display = 'none';
    document.getElementById('passwordFormSection').style.display = 'none';
    document.getElementById('rolesFormSection').style.display = 'block';
}

// Cancel form and go back to user list
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

// Save user (create or update)
function saveUser() {
    const userId = document.getElementById('userId').value;
    const isNewUser = !userId;

    // Collect form data
    const userData = {
        id: isNewUser ? null : parseInt(userId),
        username: document.getElementById('username').value,
        email: document.getElementById('email').value,
        active: document.getElementById('active').value === 'true'
    };

    // Add password for new users
    if (isNewUser) {
        userData.password = document.getElementById('password').value;
    }

    // API call
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

// Change user password
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

// Save user roles
function saveUserRoles() {
    const userId = document.getElementById('rolesUserId').value;

    // Collect selected roles
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

// Toggle user active status
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

// Reset user password
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
                modal.style.position = 'fixed';
                modal.style.top = '50%';
                modal.style.left = '50%';
                modal.style.transform = 'translate(-50%, -50%)';
                modal.style.backgroundColor = '#161b22';
                modal.style.border = '2px solid #ffc008';
                modal.style.padding = '20px';
                modal.style.zIndex = '1000';
                modal.style.borderRadius = '10px';
                modal.style.boxShadow = '0 0 20px rgba(0,0,0,0.5)';

                modal.innerHTML = `
                    <h3 style="color: #ffc008; margin-bottom: 20px;">Temporary Password for ${data.username}</h3>
                    <p style="color: white; margin-bottom: 15px;">The new password is:</p>
                    <div style="background-color: #233333; padding: 15px; margin-bottom: 20px; border-radius: 5px; font-family: monospace; font-size: 16px; color: #ffc008;">${tempPassword}</div>
                    <p style="color: white; font-weight: bold; margin-bottom: 20px;">
                        Please copy this password now. It will not be shown again.<br>
                        <span style="color: #ff5555;">SECURITY WARNING: Instruct the user to change this password immediately after first login!</span>
                    </p>
                    <button id="closeModal" style="background-color: #233333; color: #ffc008; border: 1px solid #ffc008; padding: 10px 20px; border-radius: 5px; cursor: pointer;">Close</button>
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
