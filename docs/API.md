# API reference

Single index of every backend endpoint. Base URL in development: `http://localhost:8080`
(the frontend dev server forwards `/api` and `/uploads` there, so the browser only uses `http://localhost:5173`).

**Where is the code?** Controllers live in `backend/src/main/java/com/parlour/api/controller/`
(staff-only ones in `controller/admin/`). Access rules are all in one place: `config/SecurityConfig.java`.

**Access levels**

| Level | Meaning |
|---|---|
| Public | No login needed |
| Staff | Logged-in `OWNER` or `MANAGER` (`Authorization: Bearer <access token>`) |
| Owner | Logged-in `OWNER` only. A manager gets `403` |

---

## Authentication (`AuthController`)

| Method | Path | Access | Body | Result |
|---|---|---|---|---|
| POST | `/api/auth/login` | Public | `{ email, password }` | Password is checked, an OTP is texted to the staff phone. `{ otpRequired, maskedPhone, devOtp }` (`devOtp` only when `app.otp.dev-echo=true`) |
| POST | `/api/auth/verify-otp` | Public | `{ email, code }` | `{ accessToken, refreshToken, role, email, fullName }` |
| POST | `/api/auth/refresh` | Public | `{ refreshToken }` | New token pair. The old refresh token stops working (rotation) |
| POST | `/api/auth/logout` | Public | `{ refreshToken }` | `204`. Revokes that refresh token |

Tokens: access token = JWT, valid 15 minutes. Refresh token = random string, valid 7 days, stored only as a SHA-256 hash.

## Services (`ServiceController`)

| Method | Path | Access | Body | Result |
|---|---|---|---|---|
| GET | `/api/services` | Public | none | Active services, sorted by category then name |
| POST | `/api/services` | Owner | `ServiceRequest` | Created service |
| PUT | `/api/services/{id}` | Owner | `ServiceRequest` | Updated service |
| DELETE | `/api/services/{id}` | Owner | none | `204`. A service that already has bookings is hidden instead of deleted |
| GET | `/api/admin/services` | Staff | none | All services including hidden ones (`AdminServiceController`) |

`ServiceRequest`: `{ name, category, description, price, durationMin (5-480), imageUrl, active }`

## Website content (`SiteSettingsController`)

| Method | Path | Access | Body | Result |
|---|---|---|---|---|
| GET | `/api/settings` | Public | none | Name, tagline, about, phone, email, address, opening hours, offer banner, hero image |
| PUT | `/api/settings` | Owner | `SettingsDto` | Saved settings |

## Image upload (`UploadController`)

| Method | Path | Access | Body | Result |
|---|---|---|---|---|
| POST | `/api/uploads` | Owner | `multipart/form-data`, field `file` (JPG, PNG, WEBP, GIF, max 5 MB) | `{ url }` such as `/uploads/3f2a...png` |
| GET | `/uploads/{file}` | Public | none | The image |

## Customer booking (`BookingController`)

| Method | Path | Access | Body / params | Result |
|---|---|---|---|---|
| GET | `/api/bookings/availability` | Public | `?date=YYYY-MM-DD&serviceId=` | Free start times, e.g. `["10:00","10:30"]` |
| POST | `/api/bookings/request-otp` | Public | `{ phone }` | OTP texted to the customer. `{ sent, devOtp }` |
| POST | `/api/bookings` | Public | `{ customerName, customerPhone, serviceId, startTime, otp }` | `{ id, serviceName, startTime, endTime, status }` (no personal data returned) |

## Staff bookings dashboard (`AdminBookingController`)

| Method | Path | Access | Body / params | Result |
|---|---|---|---|---|
| GET | `/api/admin/bookings` | Staff | optional `?date=YYYY-MM-DD` | Bookings with customer name and phone |
| POST | `/api/admin/bookings` | Staff | `{ customerName, customerPhone, serviceId, startTime, notes }` | Booking taken by phone call (`bookedVia = CALL`, status `CONFIRMED`) |
| PATCH | `/api/admin/bookings/{id}/status` | Staff | `{ status }` = `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED` | Updated booking. A cancelled or completed booking cannot be changed |

---

## Error format

Every error is JSON: `{ "message": "human readable text" }`

| Status | When |
|---|---|
| 400 | Invalid input, wrong or expired OTP, booking outside opening hours |
| 401 | Missing or invalid token, wrong email or password, bad refresh token |
| 403 | Logged in but not allowed (for example a manager editing services) |
| 404 | Service or booking not found |
| 409 | Time slot was just taken (all chairs busy) |
| 413 | Upload larger than 5 MB |
| 429 | Too many attempts: OTP requested again too soon, too many wrong OTP attempts, or 5 wrong passwords (account locked 15 minutes) |

## Business rules

- Opening hours, slot length and number of chairs are in `application.properties` (`app.booking.*`).
- A slot is free while fewer bookings than chairs overlap it. Cancelled bookings free the slot.
- Customers can book up to 90 days ahead, only at slot boundaries inside opening hours.
