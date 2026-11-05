<div align="center">

# ⚙️ NTG System · Backend ⚙️

### REST API that handles identity, access control and the data of every game module

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-multi--module-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-multi--stage-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

[![Author](https://img.shields.io/badge/Author-adrianpmdev-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/adrianpmdev)

[Overview](#-overview) · [Folder structure](#-folder-structure) · [Security](#-security) · [Main flows](#-main-flows) · [API](#-api-overview) · [Data model](#-data-model) · [Free tiers](#-built-for-free-tiers) · [Errors & logs](#-errors-and-logging) · [Run & deploy](#-run-and-deploy)

↩️ [Back to the project overview](../README.md)

</div>

---

<br>

## ✨ Overview

The backend is a **Spring Boot** application split into Maven modules with one rule: **the platform never names
a game, and a game never depends on the platform**. Both only share a small module of contracts.

| Module | Role |
|---|---|
| 📜 **`platform-api`** | Contracts shared by everyone: who is calling, file storage, picture compression, public pictures, how a game declares itself (`GameModule`), what it shows in the admin dashboard and on the profile, what it does when an account is deleted, and helpers for cacheable JSON. No Spring Security, no Supabase. |
| 🚀 **`core`** | The platform: identity (users, tags), the game catalog, public pictures, background music, the own profile, the admin dashboard, security, the Supabase integration, logging and error handling. It implements the contracts of `platform-api`. |
| 🎮 **`games/*`** | One module per game, with its own domain and REST endpoints. It depends **only on `platform-api`** and contains **no security code**: it declares its API path and tag, and `core` builds the access rules from that. |
| 🧷 **`app`** | Boots `core` and every game together and builds the runnable `ntg.jar`. Adding a game to the application is adding one dependency here. |

```mermaid
flowchart LR
    A["app<br/>boots everything"] --> C["core<br/>identity · security · catalog · pictures"]
    A --> G["games/*<br/>one module per game"]
    C --> P["platform-api<br/>shared contracts"]
    G --> P
    C --> SB[("Supabase<br/>Auth · Storage")]
    C --> DB[("PostgreSQL<br/>schema core")]
    G --> DB2[("PostgreSQL<br/>one schema per game")]
```

`Application` lives in the root package `com.adrianpmdev`, so Spring finds the components, entities and
repositories of every module on the classpath without listing them.

---

<br>

## 📁 Folder structure

The code is **feature-first**: each feature owns everything it needs, split into layers.

```text
backend/
├── pom.xml                         Parent POM: Java 25, versions, plugins, list of modules
├── Dockerfile                      Multi-stage build (Maven + JDK → slim JRE, non-root user)
├── .mvn/jvm.config                 JVM options of Maven
│
├── platform-api/                   📜 Contracts shared by core and the games
│   └── com/adrianpmdev/platform/
│       ├── port/                   CurrentUser, UserDirectory, ObjectStorage
│       ├── module/                 GameModule (API path, tag, player writes), GameHighlightProvider, UserDataCleaner
│       ├── image/                  ImageCompressor, ImageSpec, CompressedImage
│       ├── asset/                  ImageSlot, ImageSlotProvider, PublicAssets
│       ├── status/                 ModuleStatus, ModuleStatusProvider (a block of the admin dashboard)
│       ├── profile/                ProfileBlock, ProfileBlockProvider (a game on the profile of a player)
│       ├── error/                  ApiErrors (404, 409, 400, 403)
│       └── web/                    JsonResponses (JSON with ETag and cache rules)
│
├── core/                           🚀 The platform
│   ├── sql/core.sql                Schema core, public buckets and policies
│   └── com/adrianpmdev/core/
│       ├── config/                 Typed settings, OpenAPI, HTTP client, upload limits
│       ├── security/               🛡️ Who you are and what you may do
│       │   ├── config/             Security chains, CORS, token decoder
│       │   ├── authentication/     Principal, cached role and tags, Swagger admin login
│       │   ├── authorization/      Roles and tag-based authorities
│       │   ├── ratelimit/          Limits on sign-in attempts
│       │   ├── cookie/             Session (refresh token) cookie
│       │   └── filter/             Bearer token filter
│       ├── image/                  Picture compression (JDK only)
│       ├── integration/supabase/   🔌 Supabase Auth and Storage clients
│       ├── web/                    Health endpoint and global error handling
│       ├── logging/                Request id, access log, traffic counters, recent warnings and errors
│       └── features/               auth · users · tags · catalog · assets · music · profile · dashboard
│
├── games/
│   ├── pokemon/                    🎮 Pokémon (+ sql/pokemon.sql)
│   ├── wow-forever/                🎮 WoW Forever (characters, builds and challenges in its schema; data from the game client; sql/wow-forever.sql)
│   └── lol/                        🎮 League of Legends (data from the Riot API, sql/lol.sql)
│
└── app/                            🧷 Application.java, application.yml, ntg.jar
```

### Inside every feature

```text
<feature>/
├── api/                            🌐 What the outside world sees
│   ├── controller/                 REST endpoints
│   ├── dto/request/                Validated input
│   ├── dto/response/               Explicit output (entities are never exposed)
│   └── mapper/                     Entity ⇄ DTO conversion (only where needed)
├── application/service/            🧠 Business rules and orchestration
├── domain/                         🧩 Pure rules without Spring (only where needed, e.g. Pokémon tournaments)
└── infrastructure/persistence/     🗄️ JPA entities and Spring Data repositories
```

**Rules that keep it tidy**

- Controllers never touch repositories: they call services, and services own the business rules.
- Requests are validated at the edge and responses are DTOs, so the database model never leaks into the API.
- A game only knows `platform-api`. When it needs something from the platform (who is calling, storing a file,
  compressing a picture) it uses one of those contracts and `core` provides the implementation.
- A game declares itself with a `GameModule` bean (API path, tag key, write routes open to players) and, if it
  needs public pictures, with an `ImageSlotProvider`. `core` reads those beans; it never has a game's name in it.

---

<br>

## 🛡️ Security

Every request goes through the same pipeline: request id and access log → CORS allow-list → optional bearer token
(verified locally, role and tags from a short cache) → deny-by-default authorization rules → controller. Requests
that are not signed in receive `401` and requests without permission receive `403`.

| 🔐 Concern | How it is handled |
|---|---|
| **Identity** | Delegated to **Supabase Auth**. The backend never stores passwords. |
| **Token verification** | Access tokens (JWT) are verified **locally**: signature, expiry, issuer and audience (`authenticated`, so only session tokens are accepted). No network call per request. |
| **Fresh permissions** | Role and tags are kept in memory for at most 60 seconds and evicted the moment an admin changes them, so a removed tag or a deleted user takes effect **immediately**. |
| **Authorization** | **Deny by default.** Every route must be listed; anything else is rejected. Game rules are built from the `GameModule` each game declares. |
| **Game access** | Reading a game's endpoints needs its **tag** (or admin); writing needs **admin**, except the routes a game opens to players. The tag of a game that is still *coming soon*, or whose module has no entry in the catalog, is ignored for non-admins, so its API stays closed. |
| **Sign-in limits** | Login is limited per email, per client address and site-wide; session renewal and the email link per address; the email to change a password per user. Too many attempts get `429` with `Retry-After`. |
| **Sessions** | Short-lived access token kept in memory by the client, plus a long-lived **refresh token in an HttpOnly cookie** (scoped to the auth path, `Secure` and `SameSite` configurable). The refresh token **rotates** on every use and logout revokes it on Supabase. |
| **Accounts** | **No public sign-up.** Only admins create users, and users choose their own password from a one-time email link. An admin cannot delete their own account. |
| **Passwords** | Policy enforced on the way in (length, at least a letter and a number). A failed login gives a generic message that does not say whether the email exists. |
| **Uploaded songs** | Compressed by the admin's browser to AAC (M4A); the server only accepts an MP4 file of at most 4 MB and 10 minutes, checked by its bytes, and never decodes audio. At most 50 songs per area. |
| **Uploaded pictures** | Validated by their bytes, never by name. The server resizes and re-encodes every picture (which also drops metadata such as GPS), guards against huge images and processes one at a time. Private pictures (Pokémon teams) live in a bucket closed to clients and are shown through short-lived signed URLs. |
| **Database** | Row level security is enabled on every table, so the Supabase Data API can read nothing; the backend connects as the owner. |
| **Errors** | Standard problem-detail responses. Server errors return a generic message plus a **request id**, never stack traces, SQL or class names. |
| **Swagger** | Off by default; when an environment turns it on, it is protected with **Basic Auth** (admin only). |
| **Container** | The Docker image runs as an unprivileged user. |
| **Secrets** | Keys and credentials live **outside the repository** (environment variables or an ignored local file). |
| **Auditability** | Admin actions (users, tags, catalog, pictures…) are written to a dedicated `AUDIT` log. |

The web client adds its own browser protections on Vercel: a strict Content Security Policy, HSTS and the usual
security headers (see the [frontend documentation](../frontend/README.md)).

### Who can call what

| Area | Route | Access |
|---|---|---|
| Session | `POST /auth/login`, `/auth/refresh`, `/auth/logout`, `PUT /auth/set-password` | Public (the last one needs the link token), with attempt limits |
| Session | `GET /auth/me` | Any signed-in user |
| Profile | `GET /profile/me`, `PUT /users/me`, `PUT` · `DELETE /users/me/avatar`, `POST /users/me/password-email` | Any signed-in user (own account only) |
| Catalog | `GET /games`, `GET /games/highlights` | Any signed-in user (each sees the games they can) |
| Catalog | `POST` · `PUT` · `DELETE /games/**` | **Admin** |
| Music | `GET /music` | Any signed-in user (the general playlist and the games they hold the tag for) |
| Music | `/music/**` | **Admin** |
| Dashboard | `GET /dashboard` | **Admin** |
| Users, tags, pictures | `/users/**`, `/tags/**`, `/assets/**` | **Admin** |
| Games | `GET /<game>/**` | Users with the game's **tag**, and admins |
| Games | Other methods on `/<game>/**` | **Admin**, except the routes the game opens to players |
| Health | `GET /keep-alive` | Public |
| Anything else | | ⛔ Denied |

---

<br>

## 🔄 Main flows

### 1. Creating a user (admin → email → password)

- An admin creates the user with an email, a username and tags. The account is created in Supabase without a
  password, the local profile and tags are saved, and Supabase sends an email with a one-time link.
- The user opens the link, chooses a password and is signed in right away.
- If saving the profile fails, the Supabase account is removed again, so no half-created users are left behind.
- If only the email fails (provider limits, for example), the user is still created and the admin is told, so the
  email can be **resent** later.
- Deleting a user asks each game first, through `UserDataCleaner`: what only belongs to that person is deleted (WoW
  Forever characters, builds and challenges, the League of Legends link and games) and what others need for their history stays without the
  link (a Pokémon player and their results). If a game fails, the account is kept and the deletion can be retried. Then
  go the Supabase account, the local profile and the uploaded avatar.

### 2. Staying signed in

- Login returns a short-lived access token (kept in memory by the client) and sets a long-lived refresh token in an
  HttpOnly cookie.
- When the page is reloaded or the access token expires, the client asks for a new one using only the cookie. The
  refresh token rotates on every use.

### 3. Games, tags and release

- Adding a game to the **catalog** also creates its tag. A new game starts as *coming soon*: listed for everyone,
  open only to admins.
- An admin assigns the tag to users and, when the game is ready, unticks *coming soon*. From the next request,
  users with the tag can read the game's API. No deploy is needed.
- A game may declare a `GameHighlightProvider`: a short text about what is happening in it (a tournament being played,
  the build served), shown on its cover on the home page. Only the games the caller can open answer, once per visit to
  that page.
- A game may declare a `ProfileBlockProvider`: a summary and sections made of common pieces (figures, bars and cards)
  about a player. Each player only reads their own profile, with the games they can open, and a new game needs no change
  in the web clients.

### 4. Pictures

- An admin uploads the original picture for a slot that a module declared (a game cover, a game icon, a talent tree…). The
  server checks it, resizes it, re-encodes it as JPEG (PNG when it has transparency) and stores it in the public
  bucket. Its URL changes with every upload, so browsers can cache each version for a year.
- An avatar is an `https` link, kept as it is, or a picture its owner uploads: compressed to 256 px and 150 KB and stored in the
  same bucket.

### 5. Music

- There is one playlist for the general web (`platform`) and one per game module, so a new game module gets its own
  playlist with no change in `core`. Games only listed in the catalog have none until their module exists.
- The admin's browser turns each song into AAC in an M4A file (96, 64 or 48 kbps by length, so up to 10 minutes fit
  in 4 MB) and uploads only that file. The server checks it and stores it in the public bucket `public-music` as
  `<area>/<id>.m4a`; the name never changes, so browsers keep each song for a year.
- `GET /music` gives each user the playlists they can hear. The files are public by URL, but only signed-in users get
  the URLs and nobody can list the bucket.

### 6. Admin dashboard

- `GET /dashboard` gathers everything in one call, made only when an admin opens or refreshes the screen. A source
  that fails leaves its values empty and never hides the others.
- **Server:** the memory of the whole process (`/proc`, against the 512 MB of the plan), CPU, uptime and the deployed
  commit. **Space:** the database and the files per bucket, with two small queries, against the plan limits
  (`app.dashboard.*`).
- **Modules:** each one may declare a `ModuleStatusProvider`. WoW Forever tells its served build, the newest one on
  wago.tools and how the last check went.
- **Accounts:** the last activity is the latest of `core.user.last_seen_at` (written at most once an hour per user)
  and the last sign in of Supabase Auth. The column is plain SQL, outside the `User` entity, so a database that still
  lacks it only loses this value.
- **Traffic and logs:** counters and the last 50 warnings and errors, in memory, since the last start.

### 7. League of Legends

- A player links their Riot ID, or an admin links it; an admin starts the download of the games of the season, which
  runs on one thread, one account at a time, within the limits of a personal Riot key.
- Opening a player or pressing the update button asks Riot for what is new, at most every 5 minutes per player. At dawn
  the players nobody opened in the last 20 hours are updated. History and statistics are always read from the database.
- An admin can download an account again, for what a complete account cannot get otherwise: an older season or a queue
  Riot starts to give. Stored games are skipped.
- Seasons, one per year, are managed from the web. Riot only tells the current rank, so each update keeps a copy: the
  last one is the final rank of that season, and the highest one seen is shown as "Máx. registrado".
- The emblem of each tier is a picture an admin uploads from the Pictures screen; the API sends its URL.

---

<br>

## 🌐 API overview

All routes live under `/api/v1`.

| Module | Resources |
|---|---|
| **Auth** | `auth` (login, refresh, logout, set-password, me) |
| **Users** | `users` (create, list, update, delete, resend email), `users/{id}/tags`, `users/me`, `users/me/avatar`, `users/me/password-email` |
| **Profile** | `profile/me` (the account of the caller and a block per game) |
| **Tags** | `tags` |
| **Catalog** | `games` (the games each user can see, and their management) |
| **Pictures** | `assets` (expected pictures, upload, delete) |
| **Dashboard** | `dashboard` (state of the system, admins) |
| **Music** | `music` (playlists), `music/areas/{area}/tracks`, `music/areas/{area}/order`, `music/tracks/{id}` |
| **Game modules** | One group of resources per game, under `/api/v1/<game>/...` |

Interactive documentation is available through Swagger UI where an environment enables it (admin only).

---

<br>

## 🗄️ Data model

PostgreSQL on Supabase, organised in schemas: **`core`** for the platform (users, tags, game catalog, pictures, music)
plus **one schema per game** that needs tables. Each module keeps its DDL next to its code
(`core/sql/core.sql`, `games/<game>/sql/`), and every file can be run again safely.

- Each user shares its id with its Supabase Auth account and is linked to its tags through a join table.
- A game owns the tables of its own schema and never exposes its entities: the API only returns DTOs.
- League of Legends stores a small summary of each game: history and statistics are read from the database, and Riot is
  only asked for new games when a player is opened, at most every 5 minutes per player, on one thread with its own limit.
- What a player keeps in WoW Forever (characters, talent builds, ticked Legacy challenges) is in the database, on their
  account, so any client on any device gets it through the API. A character only needs its name: the backend reads the
  rest from the public pages of the official armory and checks, one by one, whether it changed when somebody opens the
  screen (at most every 5 minutes) and at dawn if nobody did. What did not change is not written.
- Data that only changes with a release (the Pokémon type chart, the WoW Forever texts and rules) lives in JSON
  files inside the module, not in the database.
- The WoW Forever game data is built by the backend from the game client (through wago.tools) every time a new
  build comes out, checked every hour; the last good copy is kept in a private Storage bucket. After a read whose copy
  was kept, the backend restarts at 05:00 (Madrid) to give back the memory the read leaves; without a new build it does
  not restart.
- Pictures and songs are files in Supabase Storage; the database keeps only their object names.

---

<br>

## 🪶 Built for free tiers

The site runs on the free plans of Render, Vercel and Supabase, so the backend avoids work it does not need:

| Technique | Effect |
|---|---|
| **Role and tags cache** (Caffeine, 60 s) | At most one query per user per minute instead of one per request |
| **Catalog, picture URLs and playlists in memory** | Reloaded only after an admin changes them |
| **Dashboard read on demand** | One call when an admin opens or refreshes it, never on its own; traffic and logs are small counters and a fixed buffer in memory |
| **Last activity once an hour** | At most one write per user per hour, however many requests they make |
| **Songs compressed in the browser** | The server never decodes audio, so music costs no server memory; each song is downloaded once per browser |
| **Riot read on demand** | Only when somebody looks, and once at dawn for who nobody looked at; one thread that sleeps when idle; only a small summary of each game is kept |
| **Gzip** | JSON is compressed (the WoW data drops from about 1.8 MB to about 200 KB) |
| **ETag on static JSON** | A browser that already has the data gets `304` with no body |
| **Daily `select 1`** | The keep-alive call touches the database once a day, so Supabase never pauses the project |
| **Small connection pool** | At most 5 connections to the Supabase pooler; none is held while a picture is compressed or uploaded |
| **JVM tuned for 512 MB** | Heap capped so the whole process fits, light garbage collector, memory given back after peaks, restart on out-of-memory |

---

<br>

## 🧾 Errors and logging

**One consistent error format** (RFC 7807 problem details), with the field errors listed for validation failures
and a `requestId` that matches the logs:

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "username: Username must have between 3 and 50 characters",
  "path": "/api/v1/users",
  "requestId": "a1b2c3d4e5f6"
}
```

**Readable logs on the console**, no log tables in the database. Every line carries the request id:

```text
12:30:01.123 INFO  [a1b2c3d4e5f6] ACCESS  POST /api/v1/auth/login -> 200 (143 ms)
12:31:10.456 INFO  [9f8e7d6c5b4a] AUDIT   actor=<admin> action=USER_CREATED target=<user> tags=[<game>]
```

- `ACCESS`: one line per request.
- `AUDIT`: who did what in the admin area.
- Client errors are warnings without stack traces; server errors are errors with the full trace.

---

<br>

## 🚢 Run and deploy

### Requirements

Java 25 and Maven 3.9. Settings without secrets are versioned in `app/src/main/resources/application.yml`
(compression, connection pool, Swagger off). Secrets come from a local `application.properties` next to it
(ignored by git) or from environment variables, which override the versioned file.

| Setting | Purpose |
|---|---|
| `spring.datasource.*` | PostgreSQL connection (Supabase) |
| `supabase.url`, `supabase.anon-key`, `supabase.service-role-key` | Supabase Auth and Storage access |
| `supabase.jwt.*` | How access tokens are verified (key set URL or shared secret, expected audience) |
| `app.frontend.url` | Where the email links point to |
| `app.cors.allowed-origins` | Origins allowed to call the API |
| `app.auth.*` | Session cookie options (`secure`, `same-site`, lifetime) |
| `app.dashboard.*` | Plan limits the admin dashboard compares against |
| `riot.api.key` | Riot personal API key (`RIOT_API_KEY` on Render); it never leaves the backend |

```bash
mvn clean install                  # builds every module and runs the tests
java -jar app/target/ntg.jar       # starts the API on port 8080
```

From an IDE, run `com.adrianpmdev.Application` in the `app` module.

### Deployment

- A **multi-stage Dockerfile** builds the `app` module with Maven and JDK 25 and ships only `ntg.jar` on a slim
  JRE image, as an unprivileged user.
- Hosted on **Render**; the frontend on Vercel forwards `/api/*` to it.
- `GET /keep-alive` works as a health check and keeps the free instance awake.
- All environment-specific values come from the platform's environment variables.

---

<br>

## 🗺️ Ideas for next steps

- More automated tests for the account flows.
- More game modules following the same structure.

---

<br>

↩️ [Back to the project overview](../README.md) · 🖥️ [Frontend documentation](../frontend/README.md) · 📄 [License](../LICENSE)

---

<br>

<div align="center">

✨ Made in **2026** by [@adrianpmdev](https://github.com/adrianpmdev) ✨

</div>
