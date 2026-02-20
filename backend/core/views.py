from rest_framework import viewsets
from django.shortcuts import render
from .models import Category, MenuItem, Order, SiteSettings
from .serializers import CategorySerializer, MenuItemSerializer, OrderSerializer

def index(request):
    settings = SiteSettings.objects.first()
    return render(request, 'index.html', {'settings': settings})

def qr_generator(request):
    return render(request, 'qr_generator.html')

class CategoryViewSet(viewsets.ModelViewSet):
    queryset = Category.objects.filter(is_active=True)
    serializer_class = CategorySerializer

class MenuItemViewSet(viewsets.ModelViewSet):
    queryset = MenuItem.objects.all()
    serializer_class = MenuItemSerializer

class OrderViewSet(viewsets.ModelViewSet):
    queryset = Order.objects.all().order_by('-created_at')
    serializer_class = OrderSerializer

    def get_queryset(self):
        """
        Optionally restricts the returned orders to a given status,
        by filtering against a `status` query parameter in the URL.
        """
        queryset = Order.objects.all().order_by('-created_at')
        status = self.request.query_params.get('status')
        if status is not None:
            statuses = status.split(',')
            queryset = queryset.filter(status__in=statuses)
        return queryset
