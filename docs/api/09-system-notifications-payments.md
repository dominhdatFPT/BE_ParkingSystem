# System, Notifications, Support and Payments API

Tai lieu cac endpoint con lai ngoai tam module nghiep vu chinh.

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

## Support, Incidents And System Data

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

## Stripe Order APIs

| Method | Endpoint | Auth |
|--------|----------|------|
| `POST` | `/api/payments/stripe/orders/{paymentIntentId}/confirm` | Authenticated |
| `GET` | `/api/payments/stripe/orders/{paymentIntentId}/status` | Authenticated |
| `POST` | `/api/payments/stripe/webhook` | Stripe webhook |

## Visitor Checkout Payments

| Method | Endpoint | Auth |
|--------|----------|------|
| `POST` | `/api/v1/visitor-checkout/lookup` | Public |
| `POST` | `/api/v1/visitor-checkout/stripe` | Public |
| `POST` | `/api/v1/visitor-checkout/stripe/{paymentIntentId}/confirm` | Public |

## Other Prefixes

- `POST /api/ai/chat`: AI chat, requires JWT.
- `/api/subscriptions/**`: subscription flow, requires JWT.
- `GET /api/v1/vehicle-types`: public.
