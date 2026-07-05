# BRJOBS.md

Guidance for Claude Code and developers working on the brjobs project.

## Repository Overview

**brjobs** is a job board platform for freelancers and service providers. This repository contains:

| Submodule | Stack | Responsibility |
|-----------|-------|-----------------|
| `brjobs-angular/` | Angular 20, TypeScript 5.9, Tailwind CSS | SPA frontend: job catalog, authentication, user profiles, search, contact |
| `brjobs-java/` | Spring Boot 3.3.5, Java 17, PostgreSQL | REST API: job management, user auth, profile, search, messaging |

## ✅ Regra obrigatória: revisar a responsividade antes de finalizar (Definition of Done)

**Toda entrega que toque a UI — nova tela, feature, correção de bug, ajuste de
layout/estilo — SÓ é considerada concluída depois de a responsividade ter sido
revisada.** Não finalize nem reporte como "pronto" sem passar por este checklist.

Como revisar (de preferência rodando o app e inspecionando o DOM, não só lendo o CSS):

- [ ] Testar em **mobile (≤375px)**, **tablet (~768px)** e **desktop (≥1280px)**.
- [ ] **Nenhum scroll horizontal**: `document.documentElement.scrollWidth` não pode
      ultrapassar `window.innerWidth` em nenhum breakpoint.
- [ ] Elementos que abrem/flutuam (dropdowns, popovers, modais, menus, tooltips)
      **não estouram a borda da viewport** — atenção especial a itens ancorados a
      gatilhos posicionados à direita.
- [ ] Textos não são cortados/truncados de forma indesejada; botões e alvos de
      toque continuam clicáveis (mín. ~40px) e não ficam espremidos.
- [ ] Imagens/mídia respeitam `max-width: 100%`; grids/flex reempilham quando falta
      largura.
- [ ] Inputs continuam utilizáveis no mobile (evitar largura fixa que estoura;
      considerar zoom do iOS com `font-size` < 16px).

Ao concluir, diga explicitamente em quais larguras a responsividade foi validada.

## Running the Full Stack

```bash
# Backend (Spring Boot + PostgreSQL)
cd brjobs-java
mvn spring-boot:run

# Frontend (Angular dev server)
cd brjobs-angular
npm install
npm run dev  # http://localhost:4200

# Backend runs on: http://localhost:8080
# Database: PostgreSQL (localhost:5432, user: postgres)
```

---

## Backend (Java/Spring Boot)

### Setup

```bash
cd brjobs-java

# Install dependencies
mvn install

# Run locally
mvn spring-boot:run

# Build JAR
mvn clean package

# Run tests
mvn test

# Run database migrations
mvn flyway:migrate
```

### Configuration

Key environment variables (see `brjobs-java/src/main/resources/application.properties`):
- `spring.datasource.url` — PostgreSQL connection
- `spring.datasource.username`, `spring.datasource.password` — DB credentials
- `spring.jpa.hibernate.ddl-auto` — Schema generation (usually `update` or `validate`)
- `app.jwt.secret` — JWT signing key
- `app.jwt.expiration` — JWT token TTL (milliseconds)
- `app.jwt.refresh-expiration` — Refresh token TTL
- `app.cors.allowed-origins` — CORS whitelist

### Architecture

**Strict layered architecture:** Controller → Service → Repository → Database

- `src/main/java/ads/uninassau/brjobs/`
  - `BrjobsApplication.java` — Spring Boot entry point
  - `controller/` — REST endpoints (one file per domain)
  - `service/` — business logic (interfaces + implementations)
  - `repository/` — Spring Data JPA interfaces
  - `model/` — JPA `@Entity` classes
  - `dto/` — Data Transfer Objects (request/response)
  - `config/` — Spring beans, security config, authentication filters
  - `security/` — JWT provider, auth filters, role-based access
  - `validator/` — custom validation logic
  - `exception/` — custom exception classes

### Coding Style & Conventions

- **Package naming:** `ads.uninassau.brjobs.*` (reverse DNS + domain)
- **Classes:**
  - Controllers: `*Controller` suffix (e.g., `JobController`)
  - Services: `*Service` suffix (e.g., `JobService`)
  - Repositories: `*Repository` interface, no suffix needed
  - Entities: `*Model` suffix or bare name (e.g., `JobModel` or `Job`)
  - DTOs: `*DTO` suffix (e.g., `JobDTO`, `CreateJobDTO`)
  - Exceptions: `*Exception` suffix
- **Methods:** camelCase, lowercase (Java convention)
- **Format:** Run `mvn spotless:apply` (if configured) or ensure consistent spacing
- **Nullability:** use `@NotNull`, `@Nullable` from javax.validation or annotations package
- **Logging:** use SLF4J with `@Slf4j` Lombok annotation

### Testing

Tests live next to the code as `*Test.java` or in `src/test/java/` mirroring the main structure:
- Unit tests for services, validators, utils
- Integration tests for repositories, controllers using `@SpringBootTest`
- Mock external APIs with Mockito
- Use H2 in-memory database for integration tests (avoid touching real DB)

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JobServiceTest
```

### API Documentation

Swagger/OpenAPI is enabled via SpringDoc (`springdoc-openapi-starter-webmvc-ui`).
Accessible at: `http://localhost:8080/swagger-ui.html`

---

## Frontend (Angular/TypeScript)

### Setup

```bash
cd brjobs-angular

# Install dependencies
npm install

# Run dev server
npm run dev      # http://localhost:4200

# Build for production
npm run build

# Run tests
npm test

# Lint TypeScript
npm run lint

# Type-check
npm run typecheck
```

### Architecture

- `src/app/` — Application root (configured in `app.ts`)
- `src/app/components/` — Feature components:
  - `auth/` — login, register, password reset
  - `home/` — landing page, catalog list
  - `search/` — job search, filters
  - `profile/` — user profile, settings
  - `header/`, `footer/` — layout components
  - `contact/`, `about/`, `accessibility/` — static pages
- `src/app/service/` — HTTP services:
  - `auth.service.ts` — login/logout, token refresh
  - `register.service.ts` — user registration
  - `job.service.ts` — job listing, search (to be added)
  - `user.service.ts` — profile management (to be added)
- `src/app/app.routes.ts` — route definitions (Angular Router config)

### Component Pattern

Each feature component follows this structure:
```
component-name/
├── component-name.component.ts       # Class
├── component-name.component.html     # Template
├── component-name.component.css      # Styles
└── component-name.component.spec.ts  # Tests
```

**Naming:**
- Component class: `ComponentNameComponent`
- Selector: `app-component-name` (lowercase, hyphens)
- File: `component-name.component.ts` (lowercase, hyphens)

### Coding Style & Conventions

- **Language:** TypeScript with `strict: true` mode
- **Framework:** Angular 20 App Router (new Control Flow syntax: `@if`, `@for`, `@switch`)
- **Life cycle:** use `OnInit`, `OnDestroy` for cleanup
- **Naming:**
  - Classes: PascalCase
  - Methods, properties: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Templates:**
  - Two-way binding: `[(ngModel)]` (import `FormsModule`)
  - Event binding: `(click)="method()"`
  - Property binding: `[property]="value"`
  - String interpolation: `{{ property }}`
- **Services:** inject via constructor, use `providedIn: 'root'` for singleton
- **Styling:**
  - Tailwind CSS v4 (already configured)
  - Global styles in `styles.css`
  - Component scoped styles in `*.component.css`
- **Code formatting:**
  - Prettier config enforces: 100 printWidth, single quotes, 2 spaces
  - Run `npm run lint` to check

### Authentication Flow

1. User logs in via `LoginComponent` → calls `AuthService.login(credentials)`
2. `AuthService` posts to backend `/v1/auth/login`, stores JWT in `localStorage`
3. `JwtInterceptor` automatically adds `Authorization: Bearer <token>` to all HTTP requests
4. On 401 (token expired), interceptor calls `AuthService.refresh()` to renew token
5. Protected routes guard via `AuthGuard` (to be implemented) checking token presence

### HTTP Client Pattern

Services use Angular's `HttpClient`. Always wrap API calls in error handling:

```typescript
// Example job service (to create)
@Injectable({ providedIn: 'root' })
export class JobService {
  constructor(private http: HttpClient) {}

  getJobs(): Observable<JobDTO[]> {
    return this.http.get<JobDTO[]>('/v1/jobs');
  }

  createJob(job: CreateJobDTO): Observable<JobDTO> {
    return this.http.post<JobDTO>('/v1/jobs', job);
  }
}
```

### Testing

Tests use Jasmine + Karma. Create `*.spec.ts` files alongside components:

```bash
# Run tests with watch mode
npm test

# Run tests once (CI mode)
npm run test -- --watch=false
```

Example test:
```typescript
describe('JobComponent', () => {
  let component: JobComponent;
  let fixture: ComponentFixture<JobComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobComponent],
      providers: [JobService]
    }).compileComponents();
    fixture = TestBed.createComponent(JobComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
```

---

## API Routes

Base endpoint: `http://localhost:8080/api/v1`

### Public Routes (no auth required)
- `POST /auth/login` — login with email/password
- `POST /auth/register` — register new user
- `POST /auth/refresh` — refresh expired JWT token
- `GET /jobs` — list all jobs (public catalog)
- `GET /jobs/{id}` — get job details

### Protected Routes (Bearer JWT required)
- `GET /users/me` — get current user profile
- `PUT /users/{id}` — update user profile
- `POST /jobs` — create new job (service provider only)
- `PUT /jobs/{id}` — edit job
- `DELETE /jobs/{id}` — delete job
- `POST /applications` — apply for a job (freelancer)
- `GET /applications/my` — list my job applications

Error format:
```json
{
  "timestamp": "2024-04-08T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "must be a valid email" }
  ]
}
```

---

## Database

### Connection

PostgreSQL running on `localhost:5432`:
- Username: `postgres`
- Password: `postgres`
- Database: `brjobs` (to be created)

### Schema

Current entities (see `brjobs-java/src/main/java/ads/uninassau/brjobs/model/`):
- `UserModel` — users (freelancers, service providers)
- `JobModel` — job postings
- `ApplicationModel` — job applications (freelancer → job)
- `MessageModel` — messaging (future)

Migrations:
- Manual SQL files: `brjobs-java/migrations/` (if using Flyway or Liquibase)
- Or auto-generated via JPA `ddl-auto: update` (development only)

---

## Build & Deploy

### Local Build

```bash
# Backend JAR
cd brjobs-java
mvn clean package
# Output: target/brjobs-0.0.1-SNAPSHOT.jar

# Frontend static build
cd brjobs-angular
npm run build
# Output: dist/brjobs-angular/
```

### Docker (if needed)

Create `Dockerfile` in `brjobs-java/`:
```dockerfile
FROM openjdk:17-slim
COPY target/brjobs-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Frontend is static (runs via web server, Cloudflare Pages, or S3+CloudFront).

---

## Adding a New Feature

### Full-Stack Feature (Angular + Java)

1. **Define the spec** — use `/lf-discovery` to create a discovery document
2. **Backend (Java):**
   - Create entity in `model/`
   - Create repository interface in `repository/`
   - Create service in `service/`
   - Create DTOs in `dto/`
   - Create controller in `controller/` with REST endpoints
   - Add integration tests in `src/test/`
3. **Frontend (Angular):**
   - Create service in `service/` that calls backend endpoints
   - Create component(s) in `components/feature/`
   - Add routes in `app.routes.ts`
   - Add tests (`.spec.ts`)
4. **Test end-to-end:**
   - Backend: `mvn test`
   - Frontend: `npm test`
   - Integration: start both servers, test via UI
5. **Commit:**
   - Backend changes in `brjobs-java/`
   - Frontend changes in `brjobs-angular/`
   - Update this file if adding new patterns

---

## Coding Standards & Linting

### Backend
- Java: Follow Spring Boot conventions (PascalCase for classes, camelCase for methods)
- Format: `mvn spotless:apply` (if added to POM)
- Tests: unit + integration, minimum 70% coverage on new code

### Frontend
- TypeScript: `strict: true` mode enforced
- ESLint: run `npm run lint` before committing
- Prettier: auto-formats on save (if configured in editor)
- Components: feature-based organization, each component in its own folder
- Tests: every component/service should have `.spec.ts`

---

## Version Updates

Current versions (as of 2024-04):
- Angular: 20.3.0
- Spring Boot: 3.3.5
- Java: 17
- TypeScript: 5.9.2
- PostgreSQL: 14+ (recommended)

To update:
- Angular: `ng update @angular/cli @angular/core --allow-dirty`
- Spring Boot: update `pom.xml` version
- Dependencies: `npm audit fix` (frontend), `mvn dependency:update-check` (backend)

---

## Security

### JWT Secrets & Environment Variables

**NEVER commit secrets.** Use environment variables:

```bash
# brjobs-java/.env (or export in terminal)
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=3600000
DATABASE_URL=jdbc:postgresql://localhost:5432/brjobs
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
```

### CORS & Frontend URL

Backend must whitelist frontend origin:
```properties
# application.properties
app.cors.allowed-origins=http://localhost:4200,https://brjobs.com
```

### Password Storage

- Use Spring Security's `PasswordEncoder` (bcrypt)
- Never store plain-text passwords
- Minimum 8 characters, complexity rules enforced on registration

### API Rate Limiting

- Consider adding rate limiting for auth endpoints (login/register)
- Use Redis or database-backed approach to prevent brute force

---

## Troubleshooting

### "Connection refused" on Backend

```bash
# Ensure PostgreSQL is running
psql -U postgres -d postgres -c "SELECT 1"  

# Or start container if using Docker
docker run -d -p 5432:5432 --name postgres -e POSTGRES_PASSWORD=postgres postgres:14
```

### Angular Build Errors

```bash
npm install
npm run lint --fix
npm run typecheck
```

### Spring Boot Startup Issues

```bash
mvn clean
mvn install
mvn spring-boot:run
```

---

## Contact & Documentation

- **Slack/Docs:** (to be defined)
- **Jira/Issues:** (to be defined)
- **Architecture Decisions:** Document in ADR format in `docs/adr/` (to be created)

---

## Next Steps

1. Complete documentation for: database schema, detailed API contracts, deployment pipeline
2. Add Docker Compose for one-command full-stack startup
3. Add pre-commit hooks (eslint, prettier, gofmt equivalents)
4. Establish CI/CD pipeline (GitHub Actions, GitLab CI, etc.)
