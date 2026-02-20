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

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    initTableNumber();
    fetchCategories();
    fetchMenuItems();
});

function initTableNumber() {
    const urlParams = new URLSearchParams(window.location.search);
    const tableParam = urlParams.get('table');
    if (tableParam) {
        const tableInput = document.getElementById('table-number');
        if (tableInput) {
            tableInput.value = tableParam;
            // tableInput.readOnly = true;
        }
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
        card.innerHTML = `
            ${imageHtml}
            <div class="card-content">
                <h3 class="card-title">${item.name}</h3>
                <p class="card-desc">${item.description || "สูตรพิเศษจากทางร้าน อร่อยกลมกล่อม"}</p>
                <div class="card-footer">
                    <span class="price">฿${parseFloat(item.price).toFixed(2)}</span>
                    <button class="btn-add" onclick="addToCart(${item.id})">
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

function addToCart(itemId) {
    // Add satisfying haptic feedback if supported (mobile)
    if (navigator.vibrate) navigator.vibrate(50);

    const item = allMenuItems.find(i => i.id === itemId);
    const existing = cart.find(i => i.id === itemId);

    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ ...item, quantity: 1 });
    }

    updateCartUI();
    showToast(`เพิ่ม <strong>${item.name}</strong> ลงตะกร้าแล้ว`);
}

function updateCartUI() {
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

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

        const el = document.createElement('div');
        el.className = 'cart-item';
        el.style.animationDelay = `${index * 0.05}s`;
        el.innerHTML = `
            ${itemImageHtml}
            <div class="cart-item-details">
                <div class="cart-item-title">${item.name}</div>
                <div class="cart-item-price">฿${(item.price * item.quantity).toFixed(2)}</div>
            </div>
            <div class="qty-controls">
                <button class="qty-btn" onclick="updateQty(${item.id}, -1)"><i class="fa-solid fa-minus"></i></button>
                <span class="qty-val">${item.quantity}</span>
                <button class="qty-btn" onclick="updateQty(${item.id}, 1)"><i class="fa-solid fa-plus"></i></button>
            </div>
        `;
        cartItemsContainer.appendChild(el);
    });
}

function updateQty(itemId, change) {
    if (navigator.vibrate) navigator.vibrate(30);

    const item = cart.find(i => i.id === itemId);
    if (!item) return;

    item.quantity += change;
    if (item.quantity <= 0) {
        cart = cart.filter(i => i.id !== itemId);
    }
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

    const tableNumber = document.getElementById('table-number').value;
    const orderData = {
        table_number: parseInt(tableNumber),
        items: cart.map(item => ({
            menu_item: item.id,
            quantity: item.quantity,
            notes: ""
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
            cart = [];
            updateCartUI();
            toggleCart();
            showToast('🎉 ส่งออเดอร์เรียบร้อยแล้ว รอรับความอร่อยได้เลย!', 4000);
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
