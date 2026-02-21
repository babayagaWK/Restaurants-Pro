from django.contrib import admin
from django.utils.html import format_html
from .models import Category, MenuItem, MenuItemOption, Order, OrderItem, SiteSettings

admin.site.site_header = "ระบบจัดการร้านอาหาร FoodPOS"
admin.site.site_title = "FoodPOS Admin"
admin.site.index_title = "หน้าควบคุมหลัก"

class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0
    readonly_fields = ('menu_item', 'quantity', 'price', 'notes')
    verbose_name = "รายการอาหาร"
    verbose_name_plural = "รายการอาหารในออเดอร์"

    def has_add_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False


@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ('id', 'table_number', 'colored_status', 'total_items', 'order_total', 'created_at')
    list_filter = ('status', 'created_at')
    list_editable = ()
    search_fields = ('id', 'table_number')
    inlines = [OrderItemInline]
    date_hierarchy = 'created_at'
    list_per_page = 50
    readonly_fields = ('created_at', 'updated_at')
    actions = ['mark_cooking', 'mark_ready', 'mark_completed', 'mark_cancelled']
    ordering = ['-created_at']

    def colored_status(self, obj):
        colors = {
            'pending': '#E74C3C',
            'cooking': '#3498DB',
            'ready': '#27AE60',
            'completed': '#95A5A6',
            'cancelled': '#7F8C8D',
        }
        labels = {
            'pending': '⏳ รอรับ',
            'cooking': '🔥 กำลังทำ',
            'ready': '✅ พร้อมเสิร์ฟ',
            'completed': '☑️ เสร็จสิ้น',
            'cancelled': '❌ ยกเลิก',
        }
        color = colors.get(obj.status, '#999')
        label = labels.get(obj.status, obj.status)
        return format_html(
            '<span style="color: {}; font-weight: bold; padding: 3px 8px; '
            'background: {}22; border-radius: 4px;">{}</span>',
            color, color, label
        )
    colored_status.short_description = "สถานะ"
    colored_status.admin_order_field = 'status'

    def total_items(self, obj):
        return obj.items.count()
    total_items.short_description = "จำนวนรายการ"

    def order_total(self, obj):
        total = sum(item.price * item.quantity for item in obj.items.all())
        if total > 0:
            return format_html('<span style="font-weight: bold;">฿{:.2f}</span>', total)
        return '-'
    order_total.short_description = "ยอดรวม"

    @admin.action(description="🔥 เปลี่ยนเป็น กำลังทำ")
    def mark_cooking(self, request, queryset):
        updated = queryset.filter(status='pending').update(status='cooking')
        self.message_user(request, f"เปลี่ยนสถานะ {updated} ออเดอร์เป็น กำลังทำ")

    @admin.action(description="✅ เปลี่ยนเป็น พร้อมเสิร์ฟ")
    def mark_ready(self, request, queryset):
        updated = queryset.filter(status='cooking').update(status='ready')
        self.message_user(request, f"เปลี่ยนสถานะ {updated} ออเดอร์เป็น พร้อมเสิร์ฟ")

    @admin.action(description="☑️ เปลี่ยนเป็น เสร็จสิ้น")
    def mark_completed(self, request, queryset):
        updated = queryset.filter(status__in=['ready', 'cooking']).update(status='completed')
        self.message_user(request, f"เปลี่ยนสถานะ {updated} ออเดอร์เป็น เสร็จสิ้น")

    @admin.action(description="❌ ยกเลิกออเดอร์")
    def mark_cancelled(self, request, queryset):
        updated = queryset.exclude(status='cancelled').update(status='cancelled')
        self.message_user(request, f"ยกเลิก {updated} ออเดอร์")


@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'is_active', 'item_count')
    list_display_links = ('id',)
    list_editable = ('name', 'is_active')
    search_fields = ('name',)
    list_per_page = 50

    def item_count(self, obj):
        return obj.items.count()
    item_count.short_description = "จำนวนเมนู"


class MenuItemOptionInline(admin.TabularInline):
    model = MenuItemOption
    extra = 1
    verbose_name = "ตัวเลือกเสริม"
    verbose_name_plural = "ตัวเลือกเสริมสำหรับเมนูนี้"


@admin.register(MenuItem)
class MenuItemAdmin(admin.ModelAdmin):
    list_display = ('menu_thumbnail', 'name', 'category', 'price', 'is_available')
    list_filter = ('category', 'is_available')
    search_fields = ('name', 'description')
    list_editable = ('price', 'is_available')
    inlines = [MenuItemOptionInline]
    list_per_page = 50
    list_display_links = ('name',)

    def menu_thumbnail(self, obj):
        if obj.image:
            return format_html(
                '<img src="{}" style="width: 50px; height: 50px; object-fit: cover; '
                'border-radius: 6px; border: 1px solid #ddd;" />',
                obj.image.url
            )
        return format_html(
            '<div style="width: 50px; height: 50px; background: #f0f0f0; '
            'border-radius: 6px; display: flex; align-items: center; '
            'justify-content: center; color: #ccc; font-size: 20px;">🍽</div>'
        )
    menu_thumbnail.short_description = "รูป"


@admin.register(SiteSettings)
class SiteSettingsAdmin(admin.ModelAdmin):
    def has_add_permission(self, request):
        # Allow adding only if no settings exist
        return not SiteSettings.objects.exists()
