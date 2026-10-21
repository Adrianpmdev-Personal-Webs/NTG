<div align="center">

# 🖥️ NTG System · Frontend 🖥️

**Single-page application for the NTG platform: sign in, pick a game, play and manage it.**

![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white&style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript&logoColor=white&style=flat-square)
![RxJS](https://img.shields.io/badge/RxJS-7.8-B7178C?logo=reactivex&logoColor=white&style=flat-square)
![SCSS](https://img.shields.io/badge/SCSS-design_tokens-CC6699?logo=sass&logoColor=white&style=flat-square)
![Vercel](https://img.shields.io/badge/Vercel-deployed-000000?logo=vercel&logoColor=white&style=flat-square)

[Overview](#-overview) · [Screens](#-screens) · [Access & routing](#-access-and-routing) · [Session](#-session-handling) · [Folder structure](#-folder-structure) · [Look & feel](#-look-and-feel) · [Conventions](#-conventions) · [Run & deploy](#-run-and-deploy)

↩️ [Back to the project overview](../README.md)

</div>

<br>

---
# ✨ Overview

The frontend is an **Angular 21** SPA that talks to the [NTG backend](../backend/README.md). It shows each user
only the games they have access to, and gives admins the tools to manage users, access and game content.

- ⚡ **Modern Angular only:** standalone components, **zoneless** change detection, **signals**, lazy-loaded routes
  and `OnPush` on every component, with strict TypeScript and strict templates.
- 🪶 **Lean by choice:** no UI library and no state library. The only runtime dependencies are Angular and RxJS.
- 🧩 **One folder per game.** Adding a game means adding a feature folder and one entry in a central list.
- 🔐 **Stays signed in** with silent session renewal, and never stores the access token on disk.
- 🎨 **Two visual worlds:** a terminal look for the platform, and a custom identity for each game.

> The interface is written in Spanish (the audience is a Spanish-speaking group). Code and documentation are in
> English.

<br>

---
# 🧭 Screens

| Area | Screen | What it does |
|---|---|---|
| 🔑 Auth | **Login** | Sign in; restores the session automatically if one exists |
| 🔑 Auth | **Set password** | Landing page of the email link: the user chooses a password and enters directly |
| 🏠 Platform | **Welcome** | Player home: cards for the games they can open |
| 👑 Admin | **Dashboard** | Platform statistics |
| 👑 Admin | **Users** | Create users by email, assign tags, edit, resend the password email, delete |
| 🎮 Pokémon | **Lockes** | Rule sets grouped by game, with a link to each locke's tournaments |
| 🎮 Pokémon | **Tournaments** | Participants, groups, bracket, standings and rewards of each tournament |
| 🎮 Pokémon | **Type calculator** | Pick up to two defending types and see the damage multipliers |
| 🎮 Pokémon | **Pokelike** | A roguelike-style mini-game: starter, map, battles, rewards, trades and badges |
| 🎮 Pokémon (admin) | **Management** | Games, lockes and full tournament administration (participants, groups, bracket, standings, rewards) |

<br>

---
# 🚦 Access and routing

The backend returns the user's **role** and **tags**; the app uses them to decide what to show. The backend
enforces the same rules, the frontend only hides what a user cannot use.

| Route | Who can enter | Layout |
|---|---|---|
| `/login`, `/set-password` | Public | Full-screen terminal |
| `/` | Signed in | Redirects: admin → dashboard, player → welcome |
| `/welcome` | Signed in | Terminal shell |
| `/admin/dashboard`, `/admin/users` | Admin | Terminal shell |
| `/admin/<game>/**` | Admin | Terminal shell (code lives in the game's feature) |
| `/<game>/**` | Signed in **and** has the game's tag (admins always) | The game's own layout |

```mermaid
flowchart LR
    R["Route request"] --> A{"authGuard<br/>session?"}
    A -->|no| L["/login"]
    A -->|yes| G{"gameGuard<br/>tag or admin?"}
    G -->|no| H["Back to home"]
    G -->|yes| P["✅ Game opens"]
```

Guards: `authGuard`, `adminGuard`, `roleHomeGuard` and `gameGuard(gameId)`.

<br>

---
# 🔁 Session handling

```mermaid
sequenceDiagram
    participant U as Browser
    participant App as Angular app
    participant API as Backend
    U->>App: Open the site (or reload)
    App->>API: POST /auth/refresh (HttpOnly cookie)
    API-->>App: Access token + user (role and tags)
    Note over App: Token lives only in memory
    App->>API: Requests with the token
    API-->>App: 401 (token expired)
    App->>API: Renew once, then repeat the request
```

- **No token on disk.** The access token lives in memory; the long session is kept by an **HttpOnly cookie** that
  scripts cannot read.
- **Reloads keep you signed in:** guards restore the session from the cookie before showing a page.
- **Silent renewal:** a timer renews the token shortly before it expires, and an HTTP interceptor renews and
  retries once if the API answers 401. Concurrent renewals share a single request.
- **Logout** closes the session on the backend and clears local state.
- **First-time access:** the invitation email opens `/set-password`; after choosing a password the user is signed
  in without going through the login screen.

<br>

---
# 📁 Folder structure

```text
frontend/
├── public/                     Static assets served by URL (images, game artwork)
├── scripts/                    Build-time environment generator
├── vercel.json                 SPA fallback + /api rewrite to the backend
└── src/
    ├── main.ts · index.html · styles.scss
    ├── styles/                 Design tokens, mixins, reset, typography, terminal theme
    └── app/
        ├── app.config.ts       Zoneless change detection, router, HTTP client + interceptor
        ├── app.routes.ts       Lazy-loaded top-level routes
        │
        ├── core/               🧱 App-wide singletons
        │   ├── config/         API base URL and the list of games
        │   ├── services/       Auth, users, tags, notifications
        │   ├── interceptors/   Token handling and silent session renewal
        │   └── guards/         auth · admin · role-home · game
        │
        ├── shared/             ♻️ Reusable pieces (models, notification toasts)
        ├── layout/             🖼️ Terminal shell: sidebar, header, footer
        │
        └── features/           📦 One folder per area of the site
            ├── auth/           Login and set password
            ├── welcome/        Player home
            ├── admin/          Dashboard and user management (with tags)
            └── pokemon/        🎮 The Pokémon module
                ├── pokemon-layout/       Own shell and theme
                ├── lockes/ · tournaments/    Player views
                ├── type-calculator/ · pokelike/    Game tools
                └── admin/                Games, lockes and tournament management
```

### Adding a game

1. Create `features/<game>/` with its own layout, theme, views, models and services.
2. Add its routes (`<game>.routes.ts`, plus an admin routes file if it needs management).
3. Register them in `app.routes.ts` and add one entry to the games list in `core/config/games.ts`.

The sidebar, the welcome page and the dashboard read from that list, and the game's **id doubles as the tag
key** that unlocks it on the backend.

<br>

---
# 🎨 Look and feel

- **Platform areas** (login, welcome, admin) share a **terminal** aesthetic: monospaced type, neon accents and a
  command-line flavour.
- **Each game defines its own identity** inside its feature (custom properties, fonts and layout). The terminal
  styles are scoped, so they never leak into a game.
- Design tokens are CSS custom properties, and global feedback (success, error, warning) goes through a single
  notification service.

<br>

---
# 📐 Conventions

- **Dependency direction:** features may use `core`, `shared` and `layout`; `shared` never imports from
  `features`; a feature never imports from another feature.
- **Locality first:** a file used by one feature stays inside it and only moves to `core` or `shared` when two
  features need it.
- **Naming:** hyphen-separated file names, one base name per component (`.ts`, `.html`, `.scss`), no type
  suffixes, selector prefix `app-`.
- **Path aliases:** `@core`, `@shared`, `@layout`, `@features`.
- Every file starts with a short English header comment.

<br>

---
# 🚢 Run and deploy

```bash
npm install
npm start          # development server on http://localhost:4200
npm run build      # production build
```

- **Environments:** the API address is generated at build time (`scripts/generate-env.js`). In production it is a
  **relative `/api/v1`**; in local development it points to the backend on `localhost`.
- **Hosting:** deployed on **Vercel**. `vercel.json` rewrites `/api/*` to the backend and falls back to
  `index.html` for client-side routes.
- **Why the rewrite:** the browser only talks to the site's own domain, so the session cookie is **first-party**
  (it also works in browsers that block third-party cookies) and the backend address is never exposed to the app.

<br>

---
# 🗺️ Ideas for next steps

- Automated tests for guards, interceptor and key screens.
- A tag management screen for admins.
- Player names and avatars in tournament views.
- More game modules following the same structure.

<br>

---

↩️ [Back to the project overview](../README.md) · ⚙️ [Backend documentation](../backend/README.md)
