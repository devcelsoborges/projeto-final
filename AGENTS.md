# Repository Guidelines

## Project Structure & Module Organization

This repository is a multi-app platform with three Git submodules: `backend/` (Go API), `web/` (customer Next.js app), and `backoffice/` (admin Next.js app). Use the root `docker-compose.yml` to run the stack together. Discovery artifacts belong under `ai/specs/`.

In `backend/`, entry points live in `cmd/`, business code in `internal/`, and schema changes in `migrations/`. In `web/src/` and `backoffice/src/`, routes live in `app/`, reusable UI in `components/`, API clients in `services/`, and shared helpers/types in `lib/` and `types/`.

## Build, Test, and Development Commands

- `docker compose up --build`: starts Postgres, migrations, backend, web, and backoffice.
- `cd backend && ./build-push.sh`: build and push the backend image to AWS ECR. **Always use this script** — never run `docker build` without `--platform linux/amd64`, as the project runs on Apple Silicon and ECS Fargate requires `linux/amd64`.
- `cd backend && make run|build|test`: run, build, or test the API.
- `cd backend && PGPASSWORD=$DB_PASSWORD make migrate`: apply DB migrations.
- `cd web && npm install && npm run dev`: start the customer app.
- `cd web && npm run build && npm run lint && npm run typecheck`: verify frontend changes.
- `cd backoffice && npm install && npm run dev`: start the admin app; use the same `build`, `lint`, and `typecheck` scripts as `web`.

## Coding Style & Naming Conventions

Keep backend changes within the existing layered flow: handlers -> services -> repositories -> storage/platform. Run `gofmt` on Go code. Frontend code uses TypeScript, ESLint, 2-space indentation, double quotes, and semicolons. Keep filenames in English and feature code under `src/components/feature/`.

## Testing Guidelines

Backend tests live next to the code as `*_test.go`; extend them when changing handlers, services, repositories, or config. Frontend and backoffice currently rely on static checks, so every UI change should pass `npm run lint`, `npm run typecheck`, and a quick manual smoke test.

## Commit & Pull Request Guidelines

Recent history uses short, imperative, lowercase subjects such as `add redis and backoffice edits`. Keep commits narrowly scoped. For submodule changes, commit inside the submodule first, then commit the updated pointer in the root repo. Pull requests should summarize impact by app, note env or migration changes, link the issue, and include screenshots for UI changes.

## Security & Configuration Tips

Keep secrets out of Git. Backend runtime config lives in `backend/.env`; frontend apps use variables such as `NEXT_PUBLIC_API_URL`. When changing Auth0, CORS, callback URLs, or DB settings, update the docs and call out rollout steps in the PR.
