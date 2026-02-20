from django.db import models

class Category(models.Model):
    name = models.CharField(max_length=100, verbose_name="ชื่อหมวดหมู่")
    is_active = models.BooleanField(default=True, verbose_name="เปิดใช้งาน")

    def __str__(self):
        return self.name

    class Meta:
        verbose_name = "หมวดหมู่สินค้า"
        verbose_name_plural = "หมวดหมู่สินค้า"

class MenuItem(models.Model):
    category = models.ForeignKey(Category, related_name='items', on_delete=models.CASCADE, verbose_name="หมวดหมู่")
    name = models.CharField(max_length=100, verbose_name="ชื่อเมนู")
    description = models.TextField(blank=True, verbose_name="รายละเอียด")
    price = models.DecimalField(max_digits=10, decimal_places=2, verbose_name="ราคา")
    is_available = models.BooleanField(default=True, verbose_name="มีจำหน่าย")
    image = models.ImageField(upload_to='menu_items/', blank=True, null=True, verbose_name="รูปอาหาร")

    def __str__(self):
        return self.name

    class Meta:
        verbose_name = "รายการอาหาร"
        verbose_name_plural = "รายการอาหาร"

class MenuItemOption(models.Model):
    menu_item = models.ForeignKey(MenuItem, related_name='options', on_delete=models.CASCADE, verbose_name="เมนู")
    group_name = models.CharField(max_length=100, verbose_name="กลุ่มตัวเลือก", help_text="เช่น ความหวาน, ท็อปปิ้ง")
    name = models.CharField(max_length=100, verbose_name="ชื่อตัวเลือก", help_text="เช่น ปกติ, หวานน้อย, ไข่มุก")
    additional_price = models.DecimalField(max_digits=10, decimal_places=2, default=0, verbose_name="ราคาบวกเพิ่ม")
    is_required = models.BooleanField(default=False, verbose_name="บังคับเลือก") # Useful for future frontend features
    
    def __str__(self):
        return f"{self.group_name}: {self.name} (+฿{self.additional_price})"

    class Meta:
        verbose_name = "ตัวเลือกเสริม"
        verbose_name_plural = "ตัวเลือกเสริม"

class Order(models.Model):
    STATUS_CHOICES = (
        ('pending', 'รอรับออเดอร์'),
        ('cooking', 'กำลังปรุง'),
        ('ready', 'พร้อมเสิร์ฟ'),
        ('completed', 'เสร็จสิ้น'),
        ('cancelled', 'ยกเลิก'),
    )

    table_number = models.IntegerField(verbose_name="เลขโต๊ะ")
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='pending', verbose_name="สถานะ")
    created_at = models.DateTimeField(auto_now_add=True, verbose_name="เวลาสั่ง")
    updated_at = models.DateTimeField(auto_now=True, verbose_name="อัปเดตล่าสุด")

    def __str__(self):
        return f"ออเดอร์ #{self.id} - โต๊ะ {self.table_number}"

    class Meta:
        verbose_name = "ออเดอร์"
        verbose_name_plural = "รายการออเดอร์"

class OrderItem(models.Model):
    order = models.ForeignKey(Order, related_name='items', on_delete=models.CASCADE)
    menu_item = models.ForeignKey(MenuItem, on_delete=models.CASCADE, verbose_name="เมนู")
    quantity = models.PositiveIntegerField(default=1, verbose_name="จำนวน")
    price = models.DecimalField(max_digits=10, decimal_places=2, default=0, verbose_name="ราคารวมตัวเลือก ณ ตอนสั่ง")
    notes = models.TextField(blank=True, verbose_name="ตัวเลือก/หมายเหตุ")

    def __str__(self):
        return f"{self.quantity}x {self.menu_item.name}"

    class Meta:
        verbose_name = "รายการในออเดอร์"
        verbose_name_plural = "รายการในออเดอร์"

class SiteSettings(models.Model):
    restaurant_name = models.CharField(max_length=255, default="FoodPOS", verbose_name="ชื่อร้านอาหาร")
    background_image = models.ImageField(upload_to='backgrounds/', blank=True, null=True, verbose_name="รูปพื้นหลัง")
    blur_amount = models.IntegerField(default=10, verbose_name="ความเบลอ (px)")
    
    def __str__(self):
        return "ตั้งค่าเว็บไซต์"

    class Meta:
        verbose_name = "ตั้งค่าเว็บไซต์"
        verbose_name_plural = "ตั้งค่าเว็บไซต์"
