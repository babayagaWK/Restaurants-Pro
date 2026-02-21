// State
let allMenuItems = [];
let cart = [];

// DOM Elements
const menuGrid = document.getElementById('menu-grid');
const categoryList = document.getElementById('category-list');
const cartDrawer = document.getElementById('cart-drawer');
const cartBackdrop = document.getElementById('cart-backdrop');
const cartItemsContainer = document.getElementById('cart-items');
const sidebarTotal = document.getElementById('sidebar-total');

// Floating Dock Elements
const cartDock = document.getElementById('cart-dock');
const dockItemCount = document.getElementById('dock-item-count');
const dockTotal = document.getElementById('dock-total');
const dockBadge = document.getElementById('dock-badge');

// Table Modal Elements
const tableModal = document.getElementById('table-modal');
const modalTableInput = document.getElementById('modal-table-number');
const tableModalError = document.getElementById('table-modal-error');
const tableDisplay = document.getElementById('table-display');
const modalCloseBtn = document.getElementById('modal-close-btn');

// Item Modal Elements
const itemModal = document.getElementById('item-modal');
const itemModalTitle = document.getElementById('item-modal-title');
const itemModalDesc = document.getElementById('item-modal-desc');
const itemModalImageContainer = document.getElementById('item-modal-image-container');
const itemModalOptionsContainer = document.getElementById('item-modal-options-container');
const itemModalQtyEl = document.getElementById('item-modal-qty');
const itemModalTotalPrice = document.getElementById('item-modal-total-price');

// State
let currentTableNumber = null;
let currentModalItem = null;
let currentModalQty = 1;
let currentSelectedOptions = {}; // { groupName: optionObject }
let trackingOrderId = null;
let trackingInterval = null;
let lastTrackedStatus = null;

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    initTableNumber();
    // Fix "ทั้งหมด" category pill
    const allPill = document.querySelector('.category-pill[data-id="all"]');
    if (allPill) {
        allPill.onclick = () => filterMenu('all', allPill);
    }
    // Restore tracking if page refreshed mid-tracking
    const savedOrderId = sessionStorage.getItem('trackingOrderId');
    if (savedOrderId) {
        startOrderTracking(parseInt(savedOrderId));
    }
});

function initTableNumber() {
    // 1. Check URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const tableParam = urlParams.get('table');

    if (tableParam) {
        // Automatically save and use table from QR
        saveTableSelection(tableParam);
        return;
    }

    // 2. Check Local Storage
    const savedTable = localStorage.getItem('restaurantTableNumber');
    if (savedTable) {
        saveTableSelection(savedTable);
    } else {
        // 3. No table selected -> Force Modal
        openTableModal(false);
    }
}

function openTableModal(canClose = true) {
    tableModal.classList.add('active');
    modalTableInput.value = currentTableNumber || '';
    modalTableInput.focus();
    tableModalError.style.display = 'none';

    if (canClose && currentTableNumber) {
        modalCloseBtn.style.display = 'flex';
    } else {
        modalCloseBtn.style.display = 'none';
    }
}

function closeTableModal() {
    tableModal.classList.remove('active');
}

function confirmTableSelection() {
    const tableInput = modalTableInput.value.trim();
    if (!tableInput || isNaN(tableInput) || parseInt(tableInput) < 1) {
        tableModalError.style.display = 'block';
        tableModalError.textContent = "กรุณาระบุหมายเลขโต๊ะให้ถูกต้อง";
        return;
    }

    saveTableSelection(tableInput);
    closeTableModal();
}

function saveTableSelection(tableNum) {
    currentTableNumber = parseInt(tableNum);
    localStorage.setItem('restaurantTableNumber', currentTableNumber);
    if (tableDisplay) {
        tableDisplay.textContent = currentTableNumber;
    }

    // Clean up URL if it has ?table= to avoid confusion on refresh
    const url = new URL(window.location);
    if (url.searchParams.has('table')) {
        url.searchParams.delete('table');
        window.history.replaceState({}, document.title, url.pathname + url.search);
    }

    // Only fetch data after we know the table
    if (allMenuItems.length === 0) {
        fetchCategories();
        fetchMenuItems();
    }
}

// API Calls
async function fetchCategories() {
    try {
        const response = await fetch('/api/api/categories/');
        const categories = await response.json();
        renderCategories(categories);
    } catch (error) {
        console.error('Error fetching categories:', error);
    }
}

async function fetchMenuItems() {
    try {
        const response = await fetch('/api/api/menu-items/');
        allMenuItems = await response.json();
        renderMenu(allMenuItems);
    } catch (error) {
        console.error('Error fetching menu:', error);
        menuGrid.innerHTML = '<p style="text-align:center;width:100%;color:var(--text-secondary);">ไม่สามารถโหลดเมนูได้</p>';
    }
}

// Rendering
function renderCategories(categories) {
    categories.forEach(cat => {
        const btn = document.createElement('button');
        btn.className = 'category-pill';
        btn.innerHTML = `<i class="fa-solid fa-tag" style="font-size:0.8em; opacity:0.7"></i> ${cat.name}`;
        btn.onclick = () => filterMenu(cat.id, btn);
        categoryList.appendChild(btn);
    });
}

function renderMenu(items) {
    if (items.length === 0) {
        menuGrid.innerHTML = '<p style="text-align:center;width:100%;color:var(--text-secondary);">ไม่พบเมนูอาหาร</p>';
        return;
    }

    menuGrid.innerHTML = '';
    items.forEach((item, index) => {
        if (!item.is_available) return;

        // Calculate animation delay for cascading entrance
        const delay = (index % 12) * 0.05;

        const imageHtml = item.image
            ? `<div class="card-img-wrapper"><img src="${item.image}" alt="${item.name}" loading="lazy"></div>`
            : `<div class="card-img-wrapper"><div class="image-placeholder"><i class="fa-solid fa-utensils"></i></div></div>`;

        const card = document.createElement('div');
        card.className = 'menu-card';
        card.style.animationDelay = `${delay}s`;
        card.style.cursor = 'pointer';
        card.onclick = () => openItemModal(item.id);

        card.innerHTML = `
            ${imageHtml}
            <div class="card-content">
                <h3 class="card-title">${item.name}</h3>
                <p class="card-desc">${item.description || "สูตรพิเศษจากทางร้าน อร่อยกลมกล่อม"}</p>
                <div class="card-footer">
                    <span class="price">฿${parseFloat(item.price).toFixed(2)}</span>
                    <button class="btn-add" onclick="event.stopPropagation(); addToCartDirectly(${item.id})">
                        <i class="fa-solid fa-plus"></i>
                    </button>
                </div>
            </div>
        `;
        menuGrid.appendChild(card);
    });
}

// Logic
function filterMenu(categoryId, btnElement) {
    document.querySelectorAll('.category-pill').forEach(b => b.classList.remove('active'));
    btnElement.classList.add('active');

    // Add brief fade out effect
    menuGrid.style.opacity = '0';

    setTimeout(() => {
        if (categoryId === 'all') {
            renderMenu(allMenuItems);
        } else {
            const filtered = allMenuItems.filter(item => item.category === categoryId);
            renderMenu(filtered);
        }
        menuGrid.style.opacity = '1';
    }, 200);
}

// Modal Logic
function openItemModal(itemId) {
    if (navigator.vibrate) navigator.vibrate(30);

    currentModalItem = allMenuItems.find(i => i.id === itemId);
    if (!currentModalItem) return;

    currentModalQty = 1;
    currentSelectedOptions = {}; // Reset options

    // Populate Modal Info
    itemModalTitle.textContent = currentModalItem.name;
    itemModalDesc.textContent = currentModalItem.description || "สูตรพิเศษจากทางร้าน อร่อยกลมกล่อม";

    itemModalImageContainer.innerHTML = currentModalItem.image
        ? `<img src="${currentModalItem.image}" alt="${currentModalItem.name}">`
        : `<div class="image-placeholder" style="height:100%"><i class="fa-solid fa-utensils"></i></div>`;

    // Render Options
    renderItemOptions(currentModalItem.options || []);

    // Show Modal
    updateModalPriceUI();
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
    itemModal.classList.add('active');
}

function closeItemModal() {
    itemModal.classList.remove('active');
    document.body.style.overflow = '';
}

function renderItemOptions(options) {
    itemModalOptionsContainer.innerHTML = '';

    if (!options || options.length === 0) return;

    // Group options by group_name
    const groupedOptions = options.reduce((acc, opt) => {
        if (!acc[opt.group_name]) acc[opt.group_name] = [];
        acc[opt.group_name].push(opt);
        return acc;
    }, {});

    for (const [groupName, opts] of Object.entries(groupedOptions)) {
        const isRequired = opts.some(o => o.is_required);

        // Auto-select first option if required
        if (isRequired && opts.length > 0) {
            currentSelectedOptions[groupName] = opts[0];
        }

        const groupDiv = document.createElement('div');
        groupDiv.className = 'option-group';

        let headerHtml = `<div class="option-group-title">${groupName}`;
        if (isRequired) headerHtml += `<span class="required-badge">บังคับเลือก</span>`;
        headerHtml += `</div>`;

        let listHtml = `<div class="option-list">`;
        opts.forEach(opt => {
            const isSelected = currentSelectedOptions[groupName]?.id === opt.id;
            const priceText = parseFloat(opt.additional_price) > 0 ? `+฿${parseFloat(opt.additional_price).toFixed(2)}` : 'ฟรี';

            listHtml += `
                <div class="option-item ${isSelected ? 'selected' : ''}" 
                     onclick="selectOption('${groupName}', ${opt.id}, ${parseFloat(opt.additional_price)})"
                     id="opt-${opt.id}">
                    <div class="option-label">
                        <div class="option-radio"></div>
                        <span>${opt.name}</span>
                    </div>
                    <span class="option-price">${priceText}</span>
                </div>
            `;
        });
        listHtml += `</div>`;

        groupDiv.innerHTML = headerHtml + listHtml;
        itemModalOptionsContainer.appendChild(groupDiv);
    }
}

function selectOption(groupName, optionId, price) {
    if (navigator.vibrate) navigator.vibrate(20);

    const optData = currentModalItem.options.find(o => o.id === optionId);
    currentSelectedOptions[groupName] = optData;

    // Update UI
    const groupNodes = itemModalOptionsContainer.querySelectorAll('.option-group');
    groupNodes.forEach(group => {
        const title = group.querySelector('.option-group-title').textContent;
        if (title.includes(groupName)) {
            group.querySelectorAll('.option-item').forEach(item => {
                item.classList.remove('selected');
                if (item.id === `opt-${optionId}`) {
                    item.classList.add('selected');
                }
            });
        }
    });

    updateModalPriceUI();
}

function updateModalQty(change) {
    if (navigator.vibrate) navigator.vibrate(20);
    const newQty = currentModalQty + change;
    if (newQty >= 1 && newQty <= 99) {
        currentModalQty = newQty;
        updateModalPriceUI();
    }
}

function updateModalPriceUI() {
    let unitPrice = parseFloat(currentModalItem.price);

    // Add selected options prices
    for (const opt of Object.values(currentSelectedOptions)) {
        unitPrice += parseFloat(opt.additional_price);
    }

    const total = unitPrice * currentModalQty;
    itemModalQtyEl.textContent = currentModalQty;
    itemModalTotalPrice.textContent = `฿${total.toFixed(2)}`;
}

// Cart Logic
function addModalItemToCart() {
    if (navigator.vibrate) navigator.vibrate(50);

    // Validate required options
    if (currentModalItem.options && currentModalItem.options.length > 0) {
        const requiredGroups = new Set(
            currentModalItem.options.filter(o => o.is_required).map(o => o.group_name)
        );
        for (const group of requiredGroups) {
            if (!currentSelectedOptions[group]) {
                showToast(`กรุณาเลือก <strong>${group}</strong>`, 3000);
                return;
            }
        }
    }

    let unitPrice = parseFloat(currentModalItem.price);
    let notesArr = [];

    for (const [group, opt] of Object.entries(currentSelectedOptions)) {
        unitPrice += parseFloat(opt.additional_price);
        notesArr.push(`${group}: ${opt.name}`);
    }

    const notesStr = notesArr.join(', ');

    // Prevent clumping identical items if notes differ. 
    // We create a unique temporary ID for this specific configured item.
    const tempId = Date.now().toString() + Math.random().toString(36).substr(2, 5);

    const configuredItem = {
        ...currentModalItem,
        tempId: tempId,
        quantity: currentModalQty,
        configuredPrice: unitPrice,
        notes: notesStr
    };

    cart.push(configuredItem);

    updateCartUI();
    closeItemModal();
    showToast(`เพิ่ม <strong>${currentModalItem.name}</strong> ลงตะกร้าแล้ว`);
}

function addToCartDirectly(itemId) {
    if (navigator.vibrate) navigator.vibrate(50);

    const item = allMenuItems.find(i => i.id === itemId);

    // Check if item has required options
    if (item.options && item.options.some(o => o.is_required)) {
        // If it has required options, force open the modal instead
        openItemModal(itemId);
        return;
    }

    // Otherwise, fast-add without options
    const existing = cart.find(i => i.id === itemId && !i.notes); // Find exact match with no options

    if (existing) {
        existing.quantity += 1;
    } else {
        const tempId = Date.now().toString() + Math.random().toString(36).substr(2, 5);
        cart.push({
            ...item,
            tempId: tempId,
            quantity: 1,
            configuredPrice: parseFloat(item.price),
            notes: ""
        });
    }

    updateCartUI();
    showToast(`เพิ่ม <strong>${item.name}</strong> ลงตะกร้าแล้ว`);
}

function updateCartUI() {
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const total = cart.reduce((sum, item) => sum + (item.configuredPrice * item.quantity), 0);

    // Update Dock
    if (count > 0) {
        cartDock.classList.add('visible');
        dockItemCount.textContent = `${count} รายการ`;
        dockTotal.textContent = `฿${total.toFixed(2)}`;
        dockBadge.textContent = count;

        // Add subtle pop animation to dock
        cartDock.style.transform = 'translateX(-50%) translateY(0) scale(1.02)';
        setTimeout(() => {
            cartDock.style.transform = 'translateX(-50%) translateY(0) scale(1)';
        }, 150);
    } else {
        cartDock.classList.remove('visible');
    }

    // Update Sidebar
    if (sidebarTotal) sidebarTotal.textContent = `฿${total.toFixed(2)}`;

    renderCartItems();
}

function renderCartItems() {
    cartItemsContainer.innerHTML = '';
    if (cart.length === 0) {
        cartItemsContainer.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-basket-shopping empty-icon"></i>
                <p>ยังไม่มีรายการอาหารสั่งออเดอร์</p>
            </div>
        `;
        return;
    }

    cart.forEach((item, index) => {
        const itemImageHtml = item.image
            ? `<img src="${item.image}" class="cart-item-img">`
            : `<div class="cart-placeholder"><i class="fa-solid fa-utensils" style="color:#555"></i></div>`;

        const notesHtml = item.notes ? `<div style="font-size:0.8rem; color:var(--brand-primary); margin-bottom:0.3rem;">${item.notes}</div>` : '';

        const el = document.createElement('div');
        el.className = 'cart-item';
        el.style.animationDelay = `${index * 0.05}s`;
        el.innerHTML = `
            ${itemImageHtml}
            <div class="cart-item-details">
                <div class="cart-item-title">${item.name}</div>
                ${notesHtml}
                <div class="cart-item-price">฿${(item.configuredPrice * item.quantity).toFixed(2)}</div>
            </div>
            <div class="qty-controls">
                <button class="qty-btn" onclick="updateQty('${item.tempId}', -1)"><i class="fa-solid fa-minus"></i></button>
                <span class="qty-val">${item.quantity}</span>
                <button class="qty-btn" onclick="updateQty('${item.tempId}', 1)"><i class="fa-solid fa-plus"></i></button>
                <button class="btn-delete-item" onclick="removeItem('${item.tempId}')"><i class="fa-solid fa-trash-can"></i></button>
            </div>
        `;
        cartItemsContainer.appendChild(el);
    });
}

function updateQty(tempId, change) {
    if (navigator.vibrate) navigator.vibrate(30);

    const item = cart.find(i => i.tempId === tempId);
    if (!item) return;

    item.quantity += change;
    if (item.quantity <= 0) {
        removeItem(tempId);
        return;
    }
    updateCartUI();
}

function removeItem(tempId) {
    if (navigator.vibrate) navigator.vibrate(30);
    cart = cart.filter(i => i.tempId !== tempId);
    updateCartUI();
}

function toggleCart() {
    cartDrawer.classList.toggle('open');
    cartBackdrop.classList.toggle('open');
    if (cartDrawer.classList.contains('open')) {
        renderCartItems(); // Re-trigger animations
    }
}

async function placeOrder() {
    if (cart.length === 0) return;

    // Optional UI loading state
    const btn = document.querySelector('.btn-checkout');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> กำลังส่งออเดอร์...';
    btn.disabled = true;

    const orderData = {
        table_number: currentTableNumber,
        items: cart.map(item => ({
            menu_item: item.id,
            quantity: item.quantity,
            price: item.configuredPrice, // Ensure serializer is updated to accept this
            notes: item.notes || ""
        }))
    };

    try {
        const response = await fetch('/api/api/orders/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRFToken': getCookie('csrftoken')
            },
            body: JSON.stringify(orderData)
        });

        if (response.ok) {
            const orderData2 = await response.json();
            cart = [];
            updateCartUI();
            toggleCart();
            showToast('🎉 ส่งออเดอร์เรียบร้อยแล้ว รอรับความอร่อยได้เลย!', 4000);
            // Start tracking the order
            if (orderData2 && orderData2.id) {
                startOrderTracking(orderData2.id);
            }
        } else {
            alert('ส่งออเดอร์ไม่สำเร็จ กรุณาลองใหม่');
        }
    } catch (error) {
        console.error('Order error:', error);
        alert('เกิดข้อผิดพลาดในการเชื่อมต่อ');
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

// Premium Toast Notification System
function showToast(msg, duration = 3000) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'premium-toast';
    toast.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${msg}</span>`;

    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('fade-out');
        toast.addEventListener('animationend', () => {
            toast.remove();
        });
    }, duration);
}

// CSRF Utility
function getCookie(name) {
    let cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

// === Order Tracking System ===

function startOrderTracking(orderId) {
    trackingOrderId = orderId;
    lastTrackedStatus = 'pending';
    sessionStorage.setItem('trackingOrderId', orderId);

    // Show tracker bar
    const tracker = document.getElementById('order-tracker');
    tracker.classList.add('visible');
    updateTrackerUI('pending');

    // Request browser notification permission
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }

    // Start polling
    if (trackingInterval) clearInterval(trackingInterval);
    trackingInterval = setInterval(() => pollOrderStatus(orderId), 10000);
}

async function pollOrderStatus(orderId) {
    try {
        const response = await fetch(`/api/api/orders/${orderId}/`);
        if (!response.ok) return;
        const order = await response.json();
        const status = order.status;

        if (status !== lastTrackedStatus) {
            lastTrackedStatus = status;
            updateTrackerUI(status);

            if (status === 'ready') {
                showReadyNotification();
                stopOrderTracking();
            } else if (status === 'completed' || status === 'cancelled') {
                stopOrderTracking();
                setTimeout(() => closeOrderTracker(), 5000);
            }
        }
    } catch (error) {
        console.error('Tracking poll error:', error);
    }
}

function updateTrackerUI(status) {
    const steps = ['pending', 'cooking', 'ready'];
    const currentIndex = steps.indexOf(status);

    // Update step states
    document.getElementById('step-pending').className = 'tracker-step' + (currentIndex >= 0 ? ' active' : '');
    document.getElementById('step-cooking').className = 'tracker-step' + (currentIndex >= 1 ? ' active' : '');
    document.getElementById('step-ready').className = 'tracker-step' + (currentIndex >= 2 ? ' active' : '');

    // Update lines
    document.getElementById('line-1').className = 'tracker-line' + (currentIndex >= 1 ? ' active' : '');
    document.getElementById('line-2').className = 'tracker-line' + (currentIndex >= 2 ? ' active' : '');
}

function showReadyNotification() {
    // Show modal
    const modal = document.getElementById('ready-modal');
    modal.classList.add('active');

    // Play notification sound
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const playTone = (freq, startTime, duration) => {
            const osc = audioContext.createOscillator();
            const gain = audioContext.createGain();
            osc.connect(gain);
            gain.connect(audioContext.destination);
            osc.frequency.value = freq;
            osc.type = 'sine';
            gain.gain.setValueAtTime(0.3, startTime);
            gain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);
            osc.start(startTime);
            osc.stop(startTime + duration);
        };
        const now = audioContext.currentTime;
        playTone(523, now, 0.2);      // C5
        playTone(659, now + 0.2, 0.2); // E5
        playTone(784, now + 0.4, 0.4); // G5
    } catch (e) {
        console.log('Audio not available');
    }

    // Browser notification
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('🎉 อาหารพร้อมแล้ว!', {
            body: 'กรุณามารับอาหารที่เคาน์เตอร์',
            icon: '🍽️',
            requireInteraction: true
        });
    }

    showToast('🎉 อาหารของคุณพร้อมเสิร์ฟแล้ว!', 8000);
}

function dismissReadyModal() {
    const modal = document.getElementById('ready-modal');
    modal.classList.remove('active');
    closeOrderTracker();
}

function closeOrderTracker() {
    const tracker = document.getElementById('order-tracker');
    tracker.classList.remove('visible');
    stopOrderTracking();
}

function stopOrderTracking() {
    if (trackingInterval) {
        clearInterval(trackingInterval);
        trackingInterval = null;
    }
    trackingOrderId = null;
    sessionStorage.removeItem('trackingOrderId');
}
