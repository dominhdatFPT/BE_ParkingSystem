# System, Notifications, Support and Payments API

Tài liệu các endpoint còn lại ngoài tám module nghiệp vụ chính.

## Notifications

| Method | Endpoint | Auth |
|--------|----------|------|
| `GET` | `/api/v1/notifications` | Public |
| `GET` | `/api/v1/notifications/{id}` | Public |
| `POST` | `/api/v1/notifications/register-token` | Authenticated |
| `GET` | `/api/v1/admin/notifications` | `ADMIN`, `STAFF` |
| `GET` | `/api/v1/admin/notifications/{id}` | `ADMIN`, `STAFF` |
| `POST` | `/api/v1/admin/notifications` | `ADMIN`, `STAFF` |
| `PUT` | `/api/v1/admin/notifications/{id}` | `ADMIN`, `STAFF` |
| `PATCH` | `/api/v1/admin/notifications/{id}/send` | `ADMIN`, `STAFF` |
| `DELETE` | `/api/v1/admin/notifications/{id}` | `ADMIN`, `STAFF` |
| `GET` | `/api/customer/notifications` | Authenticated |
| `PATCH` | `/api/customer/notifications/{id}/read` | Authenticated |
| `PATCH` | `/api/customer/notifications/read-all` | Authenticated |

## Support, incidents and system data

| Method | Endpoint | Auth |
|--------|----------|------|
| `POST` | `/api/customer/support` | `USER` |
| `GET` | `/api/customer/support/my` | `USER` |
| `GET` | `/api/v1/system-configuration` | `ADMIN`, `STAFF` |
| `PUT` | `/api/v1/system-configuration` | `ADMIN`, `STAFF` |
| `GET` | `/api/v1/incidents` | `ADMIN`, `STAFF` |
| `POST` | `/api/v1/incidents` | `ADMIN`, `STAFF` |
| `PATCH` | `/api/v1/incidents/{id}/reply` | `ADMIN`, `STAFF` |
| `PATCH` | `/api/v1/incidents/{id}/close` | `ADMIN`, `STAFF` |
| `GET` | `/api/v1/audit-logs` | `ADMIN`, `STAFF` |

## MoMo order APIs

| Method | Endpoint | Auth |
|--------|----------|------|
| `GET` | `/api/payments/momo-orders/{orderId}/status` | Authenticated |
| `GET` | `/api/payments/momo-orders/my` | Authenticated |
| `GET` | `/api/payments/momo-orders/pending` | Authenticated |
| `POST` | `/api/payments/momo-orders/{orderId}/admin-confirm` | Authenticated |
| `POST` | `/api/payments/momo-orders/{orderId}/cancel` | Authenticated |

> `SecurityConfig` hiện chưa giới hạn riêng hai endpoint MoMo dành cho admin.

## Các prefix khác

- `POST /api/ai/chat`: AI chat, cần JWT.
- `/api/subscriptions/**`: flow subscription/MoMo thứ hai, cần JWT.
- `GET /api/v1/vehicle-types`: public.
