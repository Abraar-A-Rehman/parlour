# Parlour Booking Website

Customers book appointments through a calendar or by phone. The owner and manager log in to see all bookings.
Only the owner can edit the website. Staff login and customer bookings are protected by an SMS one-time password (OTP).

| Part | Technology |
|---|---|
| Frontend | React 18 + TypeScript (strict mode, `any` banned by ESLint) + Vite |
| Backend | Spring Boot 3.3, Java 21, Maven, Spring Security |
| Database | MySQL 8.4 in a Docker container |
| ORM | JPA + Hibernate |
| Auth | JWT access token (15 min) + rotating refresh token (7 days) + OTP |

**Documentation:** [API reference](docs/API.md) · [Architecture and folder layout](docs/ARCHITECTURE.md) · [Contributing, Git and GitHub](CONTRIBUTING.md)

---

## 1. Install the tools (once, Windows)

PowerShell **as Administrator**:

```powershell
winget install -e --id Git.Git
winget install -e --id Microsoft.OpenJDK.21
winget install -e --id OpenJS.NodeJS.LTS
winget install -e --id Docker.DockerDesktop

# Maven (official zip from Apache)
New-Item -ItemType Directory -Force C:\tools | Out-Null
Invoke-WebRequest "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile "$env:TEMP\maven.zip"
Expand-Archive "$env:TEMP\maven.zip" -DestinationPath C:\tools -Force
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\tools\apache-maven-3.9.9\bin", "User")

Set-ExecutionPolicy -Scope CurrentUser RemoteSigned -Force
```

Restart the PC, open Docker Desktop and wait for "Engine running". In a **new** PowerShell:

```powershell
git --version; java -version; mvn -v; node -v; npm -v; docker --version
```

## 2. Run on localhost

```powershell
cd C:\path\to\parlour
.\start.ps1
```

This starts the database container, then opens one window for the backend (first start downloads libraries, 3 to 6 minutes) and one for the frontend.
When the backend window shows `Started ParlourApplication`, open **http://localhost:5173**.

Manual alternative, three windows:

```powershell
docker compose up -d                       # 1) database
cd backend;  mvn spring-boot:run           # 2) API      http://localhost:8080
cd frontend; npm install; npm run dev      # 3) website  http://localhost:5173
```

Everything in containers instead (database + API + website):

```powershell
docker compose --profile full up -d --build     # website on http://localhost:5173
docker compose --profile full down              # stop
```

### Demo logins (created on the first start)

| Role | Email | Password |
|---|---|---|
| Owner | owner@parlour.local | Owner@12345 |
| Manager | manager@parlour.local | Manager@12345 |

Login is two steps: password, then a 6-digit OTP. A laptop has no SMS gateway, so in development the code is shown on the screen and printed in the backend window (`[SMS to +880...]`). Customers get the same OTP step when they book.

### What to try
1. Home page: services, prices, click-to-call, FAQ, hours, map link.
2. **Book appointment**: service, date on the calendar, time, name and phone, OTP, confirm.
3. Log in as **manager**: bookings table, confirm or cancel, enter a booking taken by phone. No editing tabs.
4. Log in as **owner**: the same, plus Services (add, edit, delete, upload image) and Website content.

## 3. Keywords explained

| Keyword | Meaning | Where |
|---|---|---|
| **Access token** | Short-lived (15 min) signed JWT sent with every staff request. Proves identity and role. | `service/JwtService`, `config/JwtAuthFilter` |
| **Refresh token** | Long-lived (7 days) random token that only gets a new access token. Rotates on every use; reusing an old one ends the session. Stored hashed. | `service/AuthService`, `entity/RefreshToken`, `frontend/src/api/client.ts` |
| **ORM** | Work with Java objects, the library writes the SQL. | `entity/` |
| **JPA** | The Java standard (`@Entity`, `@ManyToOne` ...) for ORM. | `entity/`, `repository/` |
| **Hibernate** | The library that implements JPA in Spring Boot; creates the tables (`ddl-auto=update`). | `application.properties` |
| **Dynamic page** | Content comes from the database through the API, not typed into the HTML. The owner edits it live. | `frontend/src/pages/Home.tsx` |
| **Docker / container** | A ready-made isolated package of software. Runs MySQL (and optionally the whole app) with no installation. | `docker-compose.yml`, `*/Dockerfile` |

Backend paths above are under `backend/src/main/java/com/parlour/api/`.

## 4. Security summary

- Passwords: BCrypt. OTP: hashed, 5 minutes, 3 tries, single use, 30 s resend cooldown.
- 5 wrong passwords lock that email for 15 minutes. Login errors are generic, so accounts cannot be enumerated.
- Roles are enforced on the server in `config/SecurityConfig.java`; a manager gets `403` on every edit endpoint.
- Customer phone numbers are visible only to owner and manager; the public booking response contains no personal data.
- Uploads: real JPG/PNG/WEBP/GIF only (content checked), random file names, max 5 MB.

## 5. Real SMS

1. Implement `SmsSender` (`service/SmsSender.java`) for your gateway (SSL Wireless, BulkSMSBD, Twilio ...), and move `@Component` from `ConsoleSmsSender` to it.
2. Set `app.otp.dev-echo=false` in `application.properties` so codes are never returned by the API.
3. Copy `.env.example` to `.env`, set a long random `JWT_SECRET`, and change the seeded passwords.

## 6. Checks

```powershell
cd backend;  mvn test                                    # login, OTP, roles, lockout, booking flow (in-memory DB)
cd frontend; npm run lint; npm run typecheck; npm run build
```

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| `mvn` / `node` / `docker` not recognized | Close PowerShell and open a new one. |
| `npm.ps1 cannot be loaded` | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned -Force` |
| Backend: `Communications link failure` | Database not ready. `docker ps`, wait for `healthy`, start the backend again. |
| `port 3306 is already allocated` | Another MySQL is running. Stop it, or map `"3307:3306"` in `docker-compose.yml` and set `$env:DB_URL="jdbc:mysql://localhost:3307/parlour?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"` before `mvn spring-boot:run`. |
| `port 8080 is already in use` | Close the other app, or set `server.port=8081` and update the two proxy lines in `frontend/vite.config.ts`. |
| Docker Desktop will not start | Enable virtualization in BIOS, run `wsl --install` as Administrator, restart. |
| Start with empty data | `docker compose down -v` |
