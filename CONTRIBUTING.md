# Contributing

Short rules so that several engineers can work in this repository without stepping on each other.

## 1. First-time Git and GitHub setup

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"

cd C:\path\to\parlour
git init -b main
git add .
git commit -m "chore: initial project"
```

On github.com create an **empty** repository named `parlour` (no README), then:

```powershell
git remote add origin https://github.com/YOUR-USERNAME/parlour.git
git push -u origin main
```

## 2. Daily workflow

Never commit directly to `main`. Work on a branch, open a pull request, get a review.

```powershell
git checkout main
git pull
git checkout -b feat/short-description     # see branch names below

# ...edit files...
git status
git add .
git commit -m "feat(booking): show next free slot"
git push -u origin feat/short-description
```

Then open a **Pull Request** on GitHub. CI must be green and one teammate must approve before merging.

### Branch names

`feat/...` new feature, `fix/...` bug fix, `docs/...` documentation, `refactor/...` restructuring, `chore/...` tooling.

### Commit messages ([Conventional Commits](https://www.conventionalcommits.org))

`type(scope): short summary in the imperative`

| Type | Use for | Example |
|---|---|---|
| feat | new behaviour | `feat(auth): lock account after 5 wrong passwords` |
| fix | bug fix | `fix(booking): free the slot when a booking is cancelled` |
| docs | documentation only | `docs: add API reference` |
| refactor | no behaviour change | `refactor(dto): one record per file` |
| test | tests only | `test(auth): cover refresh token reuse` |
| chore | build, CI, dependencies | `chore(ci): run lint on pull requests` |

Small commits, one idea each. Never commit secrets (`.env`, passwords, API keys); `.gitignore` already excludes `.env`.

## 3. Where does my change go?

| I want to... | Touch these places |
|---|---|
| add or change an API endpoint | `controller/` (HTTP), `service/` (logic), `dto/` (shapes), **`config/SecurityConfig.java`** (who may call it), **`docs/API.md`** (document it), a test in `src/test` |
| add a database column | the class in `entity/`, then the DTO that exposes it |
| add a screen | `frontend/src/pages/`, route in `App.tsx`, API call in `api/api.ts`, types in `types/` |
| add a reusable UI piece | `frontend/src/components/` |
| change styling | the matching file in `frontend/src/styles/` |

Rules: one public class/record per Java file; no `any` in TypeScript (ESLint fails the build); controllers stay thin; user-visible text and prices come from the database, never hard-coded.

## 4. Checks before you push

```powershell
cd backend;  mvn test              # backend tests (in-memory database, no Docker needed)
cd ..\frontend
npm run lint                       # style + bans `any`
npm run typecheck                  # TypeScript
npm run build                      # production build
```

The same checks run automatically on every pull request (`.github/workflows/ci.yml`).
