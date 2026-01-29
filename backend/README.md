# CartShare

CartShare is a **Spring Boot–based backend service** designed to act as the **single source of truth** for a mobile-first product catalog.  
It powers an Android application with **offline-first search**, **intelligent autocomplete**, and **crowd-sourced product contributions**, all backed by **Google Firestore**.

The backend is intentionally lightweight and optimized for **data synchronization and search**, while presentation, grouping, and UX logic live entirely on the mobile client.

---

## ✨ Core Capabilities

- **Mobile-First Data Server**
  - Optimized for Android consumption
  - Flat data model for fast sync and caching

- **Offline-Ready Search**
  - Full dataset synced to device (Room / SQLite)
  - Instant autocomplete without network dependency

- **User-Contributed Products**
  - Users can add missing products directly from the app
  - Automatic keyword generation and indexing

- **Smart Autocomplete Engine**
  - Accent normalization (`pão → pao`)
  - Typo tolerance using Levenshtein distance
  - Priority ranking (official products first)

- **Firestore-Powered**
  - No server-side relational DB
  - Scales naturally with usage
  - Free tier–friendly

---

## 🧱 Project Structure

```text
project-root/
├── backend/                    # Spring Boot API
│   ├── controller/             # REST endpoints
│   ├── service/                # Firestore + search logic
│   ├── model/                  # Product, Keyword, Category
│   └── ...
│
├── frontend/                   # React Web Dashboard (optional)
│   └── Firestore management UI
│
└── android/                    # Android Mobile App (Kotlin)
    ├── data/                   # Room + Firestore API
    ├── domain/                 # Business models & use cases
    └── presentation/           # UI + ViewModels
```

---

## 🔥 Backend Overview

The backend acts as a **synchronization and contribution service**, not a traditional CRUD API.

### Responsibilities

- Serve the **initial data sync**
- Validate and accept **user-contributed products**
- Maintain **keyword and autocomplete indexes**
- Expose **health, stats, and sync endpoints**

### Non-Responsibilities

- UI grouping (supermarkets, aisles, etc.)
- Presentation logic
- Client-side filtering

This keeps the backend **fast, simple, and cheap to operate**.

---

## ☁️ Firestore Usage

Firestore is used as:

- **Primary data store**
- **Search index source**
- **Collaboration backbone**

Why Firestore?
- Generous free tier
- No schema migrations
- Excellent fit for document-based product catalogs

---

## 🔐 Infrastructure & Security

- **Spring Boot JAR** running on a Linux VPS
- **Systemd-managed service**
- **SSH key–only access**
- **UFW firewall**
- Optional **Tailscale mesh** for private access

### Example systemd service

```ini
[Unit]
Description=CartShare API
After=network.target

[Service]
User=mike
ExecStart=/usr/bin/java -Xmx2048m -jar /home/mike/cartshare/backend.jar
Environment=GOOGLE_APPLICATION_CREDENTIALS=/home/mike/cartshare/google-key-vps.json
Restart=always

[Install]
WantedBy=multi-user.target
```

---

## 🔄 User-Driven Data Flow

### 1. User Searches for a Product
- Local database queried first
- Instant results if found

### 2. Product Not Found
- App checks backend for existence
- User can contribute a new product

### 3. Contribution Flow
- Backend validates uniqueness
- Category is optional (defaults to `OUTROS`)
- Keywords auto-generated
- Autocomplete index updated

### 4. Global Sync
- New product becomes available to all users on next sync

---

## 🧠 Data Model

### Product
```json
{
  "id": "uuid",
  "productName": "iPhone 15 Pro Max",
  "categoryId": "electronics",
  "isOfficial": false,
  "searchKeywords": ["iphone", "15", "pro", "max"]
}
```

### Keyword
```json
{
  "id": "uuid",
  "keyword": "iphone",
  "categoryId": "electronics"
}
```

### Category
```json
{
  "id": "electronics",
  "name": "Electronics",
  "priority": 1
}
```

---

## 🌱 Official vs User-Contributed Data

| Type | Source | Flag |
|----|----|----|
| Official Products | Excel Seed | `isOfficial = true` |
| Community Products | Mobile App | `isOfficial = false` |

This enables:
- Transparent data provenance
- Search prioritization
- Community growth tracking

---

## 📡 API Highlights

| Endpoint | Method | Purpose |
|--------|-------|--------|
| `/api/sync/initial` | GET | Initial full sync |
| `/api/sync/product/exists` | GET | Check product existence |
| `/api/sync/categories/suggestions` | GET | Category suggestions |
| `/api/sync/contribute/product` | POST | Add new product |
| `/api/sync/stats` | GET | Data statistics |
| `/api/sync/health` | GET | Health check |

---

## 🧪 Testing & Quality

- JaCoCo coverage target: **85%**
- Emphasis on service-layer testing

```bash
./gradlew clean test jacocoTestReport
```

---

## 🚀 Why This Architecture Works

- **Flat data model** → faster syncs
- **Local-first search** → instant UX
- **Crowdsourced growth** → zero CMS overhead
- **Firestore-native** → minimal ops cost

This setup is **production-ready**, scalable, and perfectly aligned with a mobile-first product.

---

## 📌 Future Enhancements

- Incremental sync (delta-based)
- Duplicate detection (fuzzy matching)
- User reputation & contribution badges
- Optional admin review dashboard

---

**CartShare is designed to grow organically — powered by users, optimized for mobile, and simple to operate.** 🚀
