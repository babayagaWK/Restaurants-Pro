// State
let allMenuItems = [];
let cart = [];

// DOM Elements
const menuGrid = document.getElementById('menu-grid');
const categoryList = document.getElementById('category-list');
const cartCount = document.getElementById('cart-count');
const cartTotal = document.getElementById('cart-total');
const cartItemsContainer = document.getElementById('cart-items');
const cartSidebar = document.getElementById('cart-sidebar');
const cartOverlay = document.getElementById('cart-overlay');
const toast = document.getElementById('toast');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    fetchCategories();
    fetchMenuItems();
});

// API Calls
async function fetchCategories() {
    try {
        const response = await fetch('/api/categories/');
        const categories = await response.json();
        renderCategories(categories);
    } catch (error) {
        console.error('Error fetching categories:', error);
    }
}

async function fetchMenuItems() {
    try {
        const response = await fetch('/api/menu-items/');
        allMenuItems = await response.json();
        renderMenu(allMenuItems);
    } catch (error) {
        console.error('Error fetching menu:', error);
    }
}

// Rendering
function renderCategories(categories) {
    categories.forEach(cat => {
        const btn = document.createElement('button');
        btn.className = 'category-pill';
        btn.textContent = cat.name;
        btn.onclick = () => filterMenu(cat.id, btn);
        categoryList.appendChild(btn);
    });
}

function renderMenu(items) {
    menuGrid.innerHTML = '';
    items.forEach(item => {
        if (!item.is_available) return;

        const card = document.createElement('div');
        card.className = 'menu-card';
        card.innerHTML = `
            <div class="image-placeholder"></div>
            <div class="card-content">
                <div class="item-info">
                    <h3>${item.name}</h3>
                    <p class="item-desc">${item.description || "Delicious dish prepared for you."}</p>
                </div>
                <div class="item-footer">
                    <span class="price">฿${parseFloat(item.price).toFixed(2)}</span>
                    <button class="add-btn" onclick="addToCart(${item.id})">+</button>
                </div>
            </div>
        `;
        menuGrid.appendChild(card);
    });
}

// Logic
function filterMenu(categoryId, btnElement) {
    // Active class logic
    document.querySelectorAll('.category-pill').forEach(b => b.classList.remove('active'));
    btnElement.classList.add('active');

    if (categoryId === 'all') {
        renderMenu(allMenuItems);
    } else {
        const filtered = allMenuItems.filter(item => item.category === categoryId);
        renderMenu(filtered);
    }
}

function addToCart(itemId) {
    const item = allMenuItems.find(i => i.id === itemId);
    const existing = cart.find(i => i.id === itemId);

    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ ...item, quantity: 1 });
    }
    updateCartUI();
    showToast(`Added ${item.name}`);
}

function updateCartUI() {
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

    cartCount.textContent = count;
    cartTotal.textContent = `฿${total.toFixed(2)}`;

    // Render cart items
    cartItemsContainer.innerHTML = '';
    if (cart.length === 0) {
        cartItemsContainer.innerHTML = '<div class="empty-cart-msg">Your cart is empty</div>';
        return;
    }

    cart.forEach(item => {
        const el = document.createElement('div');
        el.className = 'cart-item';
        el.innerHTML = `
            <div class="cart-item-info">
                <h4>${item.name}</h4>
                <div class="cart-item-price">฿${(item.price * item.quantity).toFixed(2)}</div>
            </div>
            <div class="cart-controls">
                <button class="qty-btn" onclick="updateQty(${item.id}, -1)">-</button>
                <span>${item.quantity}</span>
                <button class="qty-btn" onclick="updateQty(${item.id}, 1)">+</button>
            </div>
        `;
        cartItemsContainer.appendChild(el);
    });
}

function updateQty(itemId, change) {
    const item = cart.find(i => i.id === itemId);
    if (!item) return;

    item.quantity += change;
    if (item.quantity <= 0) {
        cart = cart.filter(i => i.id !== itemId);
    }
    updateCartUI();
}

function toggleCart() {
    cartSidebar.classList.toggle('open');
    cartOverlay.classList.toggle('open');
}

async function placeOrder() {
    if (cart.length === 0) return;

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
        const response = await fetch('/api/orders/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRFToken': getCookie('csrftoken') // Function to get CSRF if needed
            },
            body: JSON.stringify(orderData)
        });

        if (response.ok) {
            cart = [];
            updateCartUI();
            toggleCart();
            showToast('Order Sent Successfully!', 3000);
        } else {
            alert('Failed to place order');
        }
    } catch (error) {
        console.error('Order error:', error);
        alert('Network error');
    }
}

function showToast(msg, duration = 2000) {
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, duration);
}

// Helper for Django CSRF
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
