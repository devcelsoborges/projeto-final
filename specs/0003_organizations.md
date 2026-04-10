 Plan: Auth0 Organization Setup for Backoffice Users                                                       │
│                                                                                                           │
│ Context                                                                                                   │
│                                                                                                           │
│ Currently, users in the backoffice have no concept of organization/tenant. Events have a tenant_id that   │
│ is auto-generated as a UUID per event. The goal is to introduce Auth0 Organizations so that each user     │
│ (company/organizer) maps to one Auth0 org, and that org's ID becomes the tenant_id for all their events,  │
│ batches, tickets, orders, and purchases.                                                                  │
│                                                                                                           │
│ Flow: Post-login in the backoffice → if user has no org → prompt for org name → create Auth0 org → add    │
│ user as member → save auth0_org_id in DB → use that ID as tenant_id when creating events.                 │
│                                                                                                           │
│ ---                                                                                                       │
│ Critical Files to Modify                                                                                  │
│                                                                                                           │
│ Backend                                                                                                   │
│                                                                                                           │
│ - backend/internal/platform/auth0/client.go — add org management methods                                  │
│ - backend/internal/service/auth.go — (no change)                                                          │
│ - backend/internal/service/identity.go — new SetupOrganization method                                     │
│ - backend/internal/repository/ticketing.go — add Auth0OrgID to User struct                                │
│ - backend/internal/repository/postgres/user.go — update upsert/scan + new SetAuth0OrgID                   │
│ - backend/internal/httpapi/handler/auth.go — new SetupOrganization endpoint                               │
│ - backend/internal/httpapi/router.go — register new route                                                 │
│ - backend/internal/app/app.go — wire new dependencies                                                     │
│ - backend/migrations/0010_users_auth0_org_id.up.sql — new migration                                       │
│                                                                                                           │
│ Frontend (web/)                                                                                           │
│                                                                                                           │
│ - web/src/types/auth.ts — add auth0_org_id field                                                          │
│ - web/src/services/auth.ts — add setupOrganization() function                                             │
│ - web/src/app/organization/setup/page.tsx — new page                                                      │
│ - web/src/components/feature/auth/organization-setup-screen.tsx — new component                           │
│ - web/src/components/feature/auth/login-screen.tsx — post-login redirect check                            │
│ - web/src/components/feature/auth/auth-callback-screen.tsx — post-OAuth redirect check                    │
│                                                                                                           │
│ ---                                                                                                       │
│ Implementation Steps                                                                                      │
│                                                                                                           │
│ Step 1 — Migration                                                                                        │
│                                                                                                           │
│ Create backend/migrations/0010_users_auth0_org_id.up.sql:                                                 │
│ ALTER TABLE users ADD COLUMN auth0_org_id TEXT;                                                           │
│ Column is nullable — existing users and new users without an org have NULL.                               │
│                                                                                                           │
│ Step 2 — Auth0 Client: Organization methods                                                               │
│                                                                                                           │
│ Add to backend/internal/platform/auth0/client.go:                                                         │
│                                                                                                           │
│ type Auth0OrgInfo struct {                                                                                │
│     ID          string                                                                                    │
│     Name        string  // slug, e.g. "acme-corp"                                                         │
│     DisplayName string  // e.g. "Acme Corp"                                                               │
│ }                                                                                                         │
│                                                                                                           │
│ // CreateOrganization creates a new Auth0 org and returns its info.                                       │
│ // `displayName` is free text; slug is derived (lowercase, spaces→dashes, strip non-alnum).               │
│ func (c *Client) CreateOrganization(ctx context.Context, displayName string) (Auth0OrgInfo, error)        │
│                                                                                                           │
│ // AddOrganizationMember adds an existing Auth0 user (by auth0Subject, e.g. "auth0|abc123") to an org.    │
│ func (c *Client) AddOrganizationMember(ctx context.Context, orgID, auth0Subject string) error             │
│                                                                                                           │
│ Auth0 Management API calls used:                                                                          │
│ - POST /api/v2/organizations with { "name": slug, "display_name": displayName }                           │
│ - POST /api/v2/organizations/{orgId}/members with { "members": [auth0Subject] }                           │
│ Both require the M2M management token (already implemented via fetchManagementToken).                     │
│                                                                                                           │
│ Step 3 — Repository: Update User model                                                                    │
│                                                                                                           │
│ In backend/internal/repository/ticketing.go:                                                              │
│ type User struct {                                                                                        │
│     // ... existing fields ...                                                                            │
│     Auth0OrgID  string    `json:"auth0_org_id,omitempty"`                                                 │
│ }                                                                                                         │
│                                                                                                           │
│ In backend/internal/repository/postgres/user.go:                                                          │
│ - Update UpsertAuth0User query: add auth0_org_id to RETURNING clause (not in INSERT — stays NULL on       │
│ create)                                                                                                   │
│ - Update GetByID query: include auth0_org_id                                                              │
│ - Update scanUser: scan auth0_org_id as sql.NullString                                                    │
│ - Add new method: SetAuth0OrgID(ctx context.Context, userID, orgID string) (User, error)                  │
│                                                                                                           │
│ Add to backend/internal/repository/ticketing.go:                                                          │
│ type UserRepository interface {                                                                           │
│     UpsertAuth0User(ctx context.Context, params UpsertAuth0UserParams) (User, error)                      │
│     GetByID(ctx context.Context, userID string) (User, error)                                             │
│     SetAuth0OrgID(ctx context.Context, userID, orgID string) (User, error)  // NEW                        │
│ }                                                                                                         │
│                                                                                                           │
│ Step 4 — Service: Organization setup                                                                      │
│                                                                                                           │
│ Add new interface to backend/internal/service/identity.go:                                                │
│ type orgProvider interface {                                                                              │
│     CreateOrganization(ctx context.Context, displayName string) (auth0.Auth0OrgInfo, error)               │
│     AddOrganizationMember(ctx context.Context, orgID, auth0Subject string) error                          │
│ }                                                                                                         │
│                                                                                                           │
│ Extend IdentityService:                                                                                   │
│ func (s *IdentityService) SetupOrganization(ctx context.Context, user repository.User, orgName string)    │
│ (repository.User, error) {                                                                                │
│     if user.Auth0OrgID != "" {                                                                            │
│         return user, NewValidationError("user already belongs to an organization")                        │
│     }                                                                                                     │
│     if strings.TrimSpace(orgName) == "" {                                                                 │
│         return repository.User{}, NewValidationError("organization name is required")                     │
│     }                                                                                                     │
│     org, err := s.orgProvider.CreateOrganization(ctx, strings.TrimSpace(orgName))                         │
│     // ... handle error ...                                                                               │
│     if err := s.orgProvider.AddOrganizationMember(ctx, org.ID, user.Auth0Subject); err != nil {           │
│         // ... handle error ...                                                                           │
│     }                                                                                                     │
│     return s.users.SetAuth0OrgID(ctx, user.ID, org.ID)                                                    │
│ }                                                                                                         │
│                                                                                                           │
│ IdentityService gets the orgProvider injected. Auth0 *Client already satisfies both authIdentityProvider  │
│ and will satisfy orgProvider.                                                                             │
│                                                                                                           │
│ Step 5 — Handler: POST /v1/auth/organization/setup                                                        │
│                                                                                                           │
│ Add to backend/internal/httpapi/handler/auth.go:                                                          │
│                                                                                                           │
│ // SetupOrganization handles POST /v1/auth/organization/setup                                             │
│ // Requires auth. Creates Auth0 org for the user.                                                         │
│ func (h *AuthHandler) SetupOrganization(w http.ResponseWriter, r *http.Request) {                         │
│     user, err := authenticatedUser(r)                                                                     │
│     // ...                                                                                                │
│     var body struct { OrgName string `json:"org_name"` }                                                  │
│     // ...                                                                                                │
│     updatedUser, err := h.identityService.SetupOrganization(r.Context(), user, body.OrgName)              │
│     // ...                                                                                                │
│     writeJSON(w, http.StatusCreated, updatedUser)                                                         │
│ }                                                                                                         │
│                                                                                                           │
│ The AuthHandler gains a reference to *IdentityService (add to constructor).                               │
│                                                                                                           │
│ Step 6 — Router: Register new route                                                                       │
│                                                                                                           │
│ In backend/internal/httpapi/router.go, add under protected routes:                                        │
│ POST /v1/auth/organization/setup → authHandler.SetupOrganization                                          │
│                                                                                                           │
│ Step 7 — App wiring                                                                                       │
│                                                                                                           │
│ In backend/internal/app/app.go:                                                                           │
│ - Pass auth0Client to NewIdentityService (add orgProvider param)                                          │
│ - Pass identityService to NewAuthHandler                                                                  │
│                                                                                                           │
│ Step 8 — Event Service: Use org ID as tenant_id                                                           │
│                                                                                                           │
│ In backend/internal/service/events.go, change Create:                                                     │
│ func (s *EventService) Create(ctx context.Context, ownerUserID, tenantID string, input CreateEventInput)  │
│ (repository.Event, error) {                                                                               │
│     return s.create(ctx, ownerUserID, "", tenantID, input)                                                │
│ }                                                                                                         │
│                                                                                                           │
│ Callers (the event handler) will pass user.Auth0OrgID as the tenantID. If Auth0OrgID is empty, return a   │
│ validation error: "organization setup required".                                                          │
│                                                                                                           │
│ Update backend/internal/httpapi/handler/event.go (or wherever event creation lives) to extract tenantID   │
│ from the authenticated user.                                                                              │
│                                                                                                           │
│ ---                                                                                                       │
│ Frontend Steps                                                                                            │
│                                                                                                           │
│ Step 9 — Update AuthUser type                                                                             │
│                                                                                                           │
│ In web/src/types/auth.ts:                                                                                 │
│ type AuthUser = {                                                                                         │
│   // ... existing ...                                                                                     │
│   auth0_org_id?: string;                                                                                  │
│ };                                                                                                        │
│                                                                                                           │
│ Step 10 — Auth service: setupOrganization                                                                 │
│                                                                                                           │
│ In web/src/services/auth.ts, add:                                                                         │
│ export async function setupOrganization(orgName: string): Promise<AuthUser>                               │
│ // POST /v1/auth/organization/setup  (with Bearer token via authorizedFetch)                              │
│ // On success, the returned user has auth0_org_id set                                                     │
│ // Call applyAuthSession or update the auth store with the new user                                       │
│                                                                                                           │
│ After a successful org setup, update the auth store's user field with the new user data.                  │
│                                                                                                           │
│ Step 11 — New page and component                                                                          │
│                                                                                                           │
│ Create web/src/app/organization/setup/page.tsx (static, no SSR).                                          │
│ Create web/src/components/feature/auth/organization-setup-screen.tsx:                                     │
│ - Uses useAuth() to guard: if not authenticated → redirect to /login                                      │
│ - If already has auth0_org_id → redirect to /account                                                      │
│ - Form: single input for organization name + submit button                                                │
│ - On submit: await setupOrganization(name) → on success redirect to /account                              │
│                                                                                                           │
│ Step 12 — Post-login redirect                                                                             │
│                                                                                                           │
│ In web/src/components/feature/auth/login-screen.tsx, change:                                              │
│ // Before: router.push("/account")                                                                        │
│ // After:                                                                                                 │
│ const session = await login(email, password);                                                             │
│ router.push(session.user.auth0_org_id ? "/account" : "/organization/setup");                              │
│                                                                                                           │
│ In web/src/components/feature/auth/auth-callback-screen.tsx, change:                                      │
│ // After refreshSession():                                                                                │
│ router.replace(session.user.auth0_org_id ? "/account" : "/organization/setup");                           │
│                                                                                                           │
│ ---                                                                                                       │
│ Verification                                                                                              │
│                                                                                                           │
│ 1. Run migrations: PGPASSWORD=postgres make migrate in backend/                                           │
│ 2. Signup + login a new user → should be redirected to /organization/setup                                │
│ 3. Submit org name → backend creates Auth0 org + adds member → user row gets auth0_org_id                 │
│ 4. Login again → should skip setup, go to /account                                                        │
│ 5. Create an event → tenant_id on event should equal auth0_org_id of the user                             │
│ 6. Run backend tests: go test ./... 