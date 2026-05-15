# Tracking Service

A real-time order tracking microservice for QuickBite that provides live delivery updates to customers.

## Features

- **Real-time Location Tracking**: Track delivery agent location with latitude and longitude
- **Order Status Updates**: Monitor order status from confirmation to delivery
- **ETA Calculation**: Automatic estimated delivery time based on order status
- **WebSocket Support**: Real-time updates via WebSocket connections
- **REST API**: Standard REST endpoints for tracking queries
- **Eureka Integration**: Service discovery with Eureka

## API Endpoints

### Update Location
```
POST /api/v1/tracking/update
Content-Type: application/json

{
  "orderId": 1,
  "latitude": 23.1815,
  "longitude": 79.9864,
  "status": "OUT_FOR_DELIVERY"
}
```

### Get Order Tracking
```
GET /api/v1/tracking/orders/{orderId}
Authorization: Bearer <token>

Response:
{
  "orderId": 1,
  "latitude": 23.1815,
  "longitude": 79.9864,
  "status": "OUT_FOR_DELIVERY",
  "estimatedDeliveryTime": "8 mins",
  "updatedAt": "2026-04-11T10:30:00"
}
```

### Complete Tracking
```
POST /api/v1/tracking/orders/{orderId}/complete
Authorization: Bearer <token>
```

## Order Status States

- `CONFIRMED` - Order confirmed, estimated 20 mins
- `PREPARING` - Being prepared, estimated 15 mins
- `OUT_FOR_DELIVERY` - On the way, estimated 8 mins
- `DELIVERED` - Delivery completed, estimated 0 mins

## WebSocket

Connect to WebSocket endpoint for real-time updates:
```
ws://localhost:8089/ws/tracking
```

## Configuration

Port: `8089`
Service Name: `tracking-service`

## Running the Service

```bash
cd tracking-service
mvn spring-boot:run
```

The service will automatically register with Eureka at `http://localhost:8761/eureka/`

