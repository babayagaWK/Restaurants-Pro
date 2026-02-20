from django.contrib import admin
from .models import Category, MenuItem, MenuItemOption, Order, OrderItem, SiteSettings

admin.site.site_header = "ระบบจัดการร้านอาหาร FoodPOS"
admin.site.site_title = "FoodPOS Admin"
admin.site.index_title = "หน้าควบคุมหลัก"

class OrderItemInline(admin.TabularInline):
    model = OrderItem
    extra = 0
    verbose_name = "รายการอาหาร"
    verbose_name_plural = "รายการอาหารในออเดอร์"

@admin.register(Order)
class OrderAdmin(admin.ModelAdmin):
    list_display = ('id', 'table_number', 'status', 'total_items', 'created_at')
    list_filter = ('status', 'created_at')
    search_fields = ('table_number',)
    inlines = [OrderItemInline]
    date_hierarchy = 'created_at'

    def total_items(self, obj):
        return obj.items.count()
    total_items.short_description = "จำนวนรายการ"

@admin.register(Category)
class CategoryAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'is_active', 'item_count')
    list_display_links = ('id',)
    list_editable = ('name', 'is_active')
    search_fields = ('name',)

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
    list_display = ('name', 'category', 'price', 'is_available')
    list_filter = ('category', 'is_available')
    search_fields = ('name', 'description')
    list_editable = ('price', 'is_available')
    inlines = [MenuItemOptionInline]

@admin.register(SiteSettings)
class SiteSettingsAdmin(admin.ModelAdmin):
    def has_add_permission(self, request):
        # Allow adding only if no settings exist
        return not SiteSettings.objects.exists()
