# Deploy StoresWeb to DigitalOcean App Platform

StoresWeb is a **Vue 3 + Vite** app (stores/customer-facing). Deploy it as a **Static Site** component, like the main frontend.

---

## Prerequisites

- Backend already deployed (e.g. `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app`)
- Same GitHub repo; branch `main` (or your default)

---

## Step 1: Add a Static Site component

1. DigitalOcean → your app → **Components** → **Add Component**
2. Choose **Static Site**
3. **Source:**
   - Repository: your repo (e.g. `zakaryaalsaba/EtlobtodayEnginRepo`)
   - Branch: `main`
   - **Source Directory:** `StoresWeb`

---

## Step 2: Build settings

| Setting | Value |
|--------|--------|
| **Build Command** | `npm install --include=dev && npm run build` |
| **Output Directory** | `dist` |

Use `--include=dev` so Vite and other devDependencies are installed (the Node buildpack prunes them before the custom command).

---

## Step 3: Build-time environment variables

In the component’s **Environment Variables** (build-time), add:

| Variable | Value |
|----------|--------|
| `VITE_API_BASE_URL` | `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app` |

If the app is **not** at the root of your domain, also set:

| Variable | Value |
|----------|--------|
| `VITE_BASE_PATH` | `/stores/` (or the path you use in routing rules) |

Use the **exact** path from **Component routing rules** (e.g. `/stores/` or `/etlobtodayenginrepo-stores/`), with leading and trailing slashes. If StoresWeb is served at the app root, omit `VITE_BASE_PATH` or set it to `/`.

---

## Step 4: Routing (Networking)

In **Networking** → **Component routing rules**:

- Add a rule, e.g. **Route:** `/stores` → **Target:** your StoresWeb component (path trimmed if available).

Then set **Build** env var:

- `VITE_BASE_PATH` = `/stores/`

So the site is reachable at: `https://your-app.ondigitalocean.app/stores/`

---

## Step 5: Lock file

Keep `package-lock.json` in sync so the platform installs the same dependency tree:

```bash
cd StoresWeb
npm install
git add package-lock.json
git commit -m "chore(StoresWeb): sync package-lock.json"
git push origin main
```

---

## Summary

| Item | Value |
|------|--------|
| Source Directory | `StoresWeb` |
| Build Command | `npm install --include=dev && npm run build` |
| Output Directory | `dist` |
| `VITE_API_BASE_URL` | Your backend URL (no trailing slash) |
| `VITE_BASE_PATH` | Subpath if not at root (e.g. `/stores/`) |

After deployment, open the URL from the component or your routing path (e.g. `https://restaurantwebsitebuilder-q58fe.ondigitalocean.app/stores/`).
