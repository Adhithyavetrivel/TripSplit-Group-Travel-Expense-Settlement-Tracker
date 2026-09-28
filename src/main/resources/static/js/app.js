/**
 * TripSplit - Client JavaScript
 * Handles API communication with X-User header, modals, toasts, and dynamic forms
 */

// User Management (Phase 14)
function getUser() {
  const user = localStorage.getItem('tripsplit_user');
  return (user && user.trim().length > 0) ? user.trim() : 'SYSTEM';
}

function setUser(username) {
  const clean = (username && username.trim().length > 0) ? username.trim() : 'SYSTEM';
  localStorage.setItem('tripsplit_user', clean);
  const input = document.getElementById('headerUser');
  if (input) input.value = clean;
}

function initUserHeader() {
  const input = document.getElementById('headerUser');
  if (input) {
    input.value = getUser();
    input.addEventListener('change', () => setUser(input.value));
    input.addEventListener('blur', () => setUser(input.value));
  }
}

function getApiHeaders(extra = {}) {
  return {
    'Content-Type': 'application/json',
    'X-User': getUser(),
    ...extra
  };
}

// Toast Notifications (Phase 17 & 18)
function showToast(message, type = 'success') {
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  const icon = type === 'success' ? '✓' : '⚠';
  toast.innerHTML = `<span style="font-weight: bold; font-size: 16px;">${icon}</span> <span>${escapeHtml(message)}</span>`;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

async function handleApiResponse(response) {
  if (response.status === 204) {
    return null;
  }
  const contentType = response.headers.get('content-type') || '';
  let data = null;
  if (contentType.includes('application/json')) {
    data = await response.json();
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    const errorMsg = (data && data.message) ? data.message : (typeof data === 'string' ? data : 'Operation failed');
    throw new Error(errorMsg);
  }
  return data;
}

// Modal Helpers
function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('open');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('open');
}

// Mobile Sidebar
function toggleSidebar() {
  const sidebar = document.getElementById('appSidebar');
  if (sidebar) sidebar.classList.toggle('open');
}

// Add Participant (Phase 13)
async function submitAddParticipant(event, tripId) {
  event.preventDefault();
  const form = event.target;
  const name = form.name.value.trim();
  const email = form.email.value.trim();

  try {
    const response = await fetch(`/api/trips/${tripId}/participants`, {
      method: 'POST',
      headers: getApiHeaders(),
      body: JSON.stringify({ name, email })
    });
    await handleApiResponse(response);
    showToast('Participant added successfully.');
    closeModal('addParticipantModal');
    setTimeout(() => window.location.reload(), 600);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Generate Settlement (Phase 11)
async function triggerGenerateSettlement(tripId) {
  const btn = document.getElementById('generateSettlementBtn');
  if (btn) btn.disabled = true;

  try {
    const response = await fetch(`/api/trips/${tripId}/settlements/generate`, {
      method: 'POST',
      headers: getApiHeaders()
    });
    await handleApiResponse(response);
    showToast('Settlement generated successfully.');
    setTimeout(() => window.location.reload(), 600);
  } catch (err) {
    showToast(err.message, 'error');
    if (btn) btn.disabled = false;
  }
}

// Expense Management (Phase 7, 8, 9)
function onSplitTypeChange() {
  const isCustom = document.querySelector('input[name="splitType"]:checked')?.value === 'custom';
  const customInputs = document.querySelectorAll('.custom-amount-input');
  customInputs.forEach(el => {
    el.style.display = isCustom ? 'block' : 'none';
  });
}

function openAddExpenseModal() {
  const form = document.getElementById('expenseForm');
  if (form) {
    form.reset();
    document.getElementById('expenseModalTitle').innerText = 'Add Expense';
    document.getElementById('expenseIdField').value = '';
    onSplitTypeChange();
  }
  openModal('expenseModal');
}

function editExpense(btn) {
  const expenseData = JSON.parse(btn.getAttribute('data-expense'));
  const form = document.getElementById('expenseForm');
  if (!form) return;

  document.getElementById('expenseModalTitle').innerText = 'Edit Expense';
  document.getElementById('expenseIdField').value = expenseData.id;
  form.description.value = expenseData.description;
  form.amount.value = expenseData.amount;
  form.payerId.value = expenseData.payerId;
  form.expenseDate.value = expenseData.expenseDate;

  // Determine if custom split was used
  const hasCustom = expenseData.participants && expenseData.participants.some(p => p.owedAmount !== null);
  if (hasCustom) {
    document.getElementById('splitCustomRadio').checked = true;
  } else {
    document.getElementById('splitEqualRadio').checked = true;
  }

  // Set participant checkboxes & amounts
  const pMap = {};
  if (expenseData.participants) {
    expenseData.participants.forEach(p => {
      pMap[p.participantId] = p.owedAmount;
    });
  }

  document.querySelectorAll('.expense-participant-checkbox').forEach(cb => {
    const pId = Number(cb.value);
    const customInput = document.getElementById(`customAmount_${pId}`);
    if (pMap.hasOwnProperty(pId)) {
      cb.checked = true;
      if (customInput) customInput.value = pMap[pId] !== null ? pMap[pId] : '';
    } else {
      cb.checked = false;
      if (customInput) customInput.value = '';
    }
  });

  onSplitTypeChange();
  openModal('expenseModal');
}

async function submitExpenseForm(event, tripId) {
  event.preventDefault();
  const form = event.target;
  const expenseId = document.getElementById('expenseIdField').value;
  const isEditing = expenseId && expenseId.trim().length > 0;

  const description = form.description.value.trim();
  const amount = parseFloat(form.amount.value);
  const payerId = parseInt(form.payerId.value);
  const expenseDate = form.expenseDate.value;
  const isCustom = document.querySelector('input[name="splitType"]:checked')?.value === 'custom';

  const participants = [];
  document.querySelectorAll('.expense-participant-checkbox:checked').forEach(cb => {
    const pId = parseInt(cb.value);
    const item = { participantId: pId };
    if (isCustom) {
      const owedVal = document.getElementById(`customAmount_${pId}`)?.value;
      item.owedAmount = owedVal ? parseFloat(owedVal) : 0;
    }
    participants.push(item);
  });

  if (participants.length === 0) {
    showToast('Please select at least one participant for the expense.', 'error');
    return;
  }

  const payload = {
    description,
    amount,
    payerId,
    expenseDate: expenseDate || null,
    participants
  };

  const url = isEditing
    ? `/api/trips/${tripId}/expenses/${expenseId}`
    : `/api/trips/${tripId}/expenses`;
  const method = isEditing ? 'PUT' : 'POST';

  try {
    const response = await fetch(url, {
      method,
      headers: getApiHeaders(),
      body: JSON.stringify(payload)
    });
    await handleApiResponse(response);
    showToast(isEditing ? 'Expense updated successfully.' : 'Expense created successfully.');
    closeModal('expenseModal');
    setTimeout(() => window.location.reload(), 600);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteExpense(tripId, expenseId) {
  if (!confirm('Are you sure you want to delete this expense? This will recalculate balances and settlements.')) {
    return;
  }

  try {
    const response = await fetch(`/api/trips/${tripId}/expenses/${expenseId}`, {
      method: 'DELETE',
      headers: getApiHeaders()
    });
    await handleApiResponse(response);
    showToast('Expense deleted successfully.');
    setTimeout(() => window.location.reload(), 600);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Dynamic Participants in Trip Creation (Phase 6)
function addParticipantRow() {
  const container = document.getElementById('createParticipantsContainer');
  if (!container) return;

  const count = container.querySelectorAll('.participant-row').length + 1;
  const row = document.createElement('div');
  row.className = 'participant-row form-row';
  row.style.marginBottom = '12px';
  row.style.alignItems = 'flex-end';
  row.innerHTML = `
    <div class="form-group" style="margin-bottom: 0;">
      <label class="form-label">Participant ${count} Name</label>
      <input type="text" class="form-control part-name" placeholder="e.g. Friend Name" required>
    </div>
    <div class="form-group" style="margin-bottom: 0; display: flex; gap: 8px;">
      <div style="flex: 1;">
        <label class="form-label">Email</label>
        <input type="email" class="form-control part-email" placeholder="friend@example.com">
      </div>
      <button type="button" class="btn btn-secondary btn-sm" onclick="this.closest('.participant-row').remove()" style="align-self: flex-end; height: 42px;" title="Remove">✕</button>
    </div>
  `;
  container.appendChild(row);
}

async function submitCreateTrip(event) {
  event.preventDefault();
  const form = event.target;
  const name = form.name.value.trim();
  const description = form.description.value.trim();
  const destination = form.destination.value.trim();
  const startDate = form.startDate.value || null;
  const endDate = form.endDate.value || null;

  const participants = [];
  document.querySelectorAll('.participant-row').forEach(row => {
    const pName = row.querySelector('.part-name')?.value.trim();
    const pEmail = row.querySelector('.part-email')?.value.trim();
    if (pName) {
      participants.push({
        name: pName,
        email: pEmail || null
      });
    }
  });

  const payload = {
    name,
    description: description || null,
    destination: destination || null,
    startDate,
    endDate,
    participants: participants.length > 0 ? participants : []
  };

  try {
    const response = await fetch('/api/trips', {
      method: 'POST',
      headers: getApiHeaders(),
      body: JSON.stringify(payload)
    });
    const createdTrip = await handleApiResponse(response);
    showToast('Trip created successfully.');
    setTimeout(() => {
      window.location.href = `/dashboard?tripId=${createdTrip.id}`;
    }, 600);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Activity Tabs Switcher (Phase 12)
function switchActivityTab(tabName) {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
  });
  document.querySelectorAll('.tab-pane').forEach(pane => {
    pane.classList.toggle('active', pane.id === `${tabName}Pane`);
  });
}

// Initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  initUserHeader();
});
