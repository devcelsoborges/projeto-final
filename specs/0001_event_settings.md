# 0001 — Event Settings

## Overview

Add a `settings` table (one row per event, root or subevent) to store display-related URLs used in listings, event pages, and printed/shown tickets.

## Schema

```sql
CREATE TABLE IF NOT EXISTS settings (
    id              TEXT PRIMARY KEY,
    event_id        TEXT NOT NULL UNIQUE REFERENCES events(id) ON DELETE CASCADE,
    event_card_url  TEXT,
    event_logo_url  TEXT,
    ticket_card_url TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_settings_event_id ON settings (event_id);
```

### Fields

| Column | Type | Description |
|--------|------|-------------|
| `id` | TEXT | UUID primary key |
| `event_id` | TEXT | FK → `events.id`, unique (one settings row per event) |
| `event_card_url` | TEXT (nullable) | Card image shown in event listings |
| `event_logo_url` | TEXT (nullable) | Logo displayed on the event detail page |
| `ticket_card_url` | TEXT (nullable) | Image printed/shown on tickets |
| `created_at` | TIMESTAMPTZ | Row creation timestamp |
| `updated_at` | TIMESTAMPTZ | Last update timestamp |

## API Contract

Both endpoints require a valid Bearer JWT. Only the event owner may access them.

### GET /v1/events/{eventId}/settings

Returns the settings for the given event. If no settings row exists yet, returns a zero-value object (empty strings for all URL fields).

**Response 200:**
```json
{
  "id": "...",
  "event_id": "...",
  "event_card_url": "https://...",
  "event_logo_url": "https://...",
  "ticket_card_url": "https://...",
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-01T00:00:00Z"
}
```

Fields with empty values are omitted from the JSON response (`omitempty`).

### PUT /v1/events/{eventId}/settings

Creates or updates (upsert) the settings row for the event. All fields are optional; omitting a field leaves the stored value unchanged. Setting a field to `null` clears the URL.

**Request body:**
```json
{
  "event_card_url": "https://...",
  "event_logo_url": "https://...",
  "ticket_card_url": "https://..."
}
```

**Response 200:** same shape as GET.

## Error Responses

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid JWT |
| 403 | Authenticated user is not the event owner |
| 404 | `eventId` does not exist |

## Implementation Notes

- Migration: `backend/migrations/0008_event_settings.up.sql`
- Repository interface: `backend/internal/repository/ticketing.go` (`SettingsRepository`)
- Postgres implementation: `backend/internal/repository/postgres/settings.go`
- Service: `backend/internal/service/settings.go`
- Handler: `backend/internal/httpapi/handler/settings.go`
- Routes registered in `backend/internal/httpapi/router.go`
- Wired in `backend/internal/app/app.go`
