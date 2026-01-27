## CartShare



### Project Layout

```graph

project-root/
├── backend/                    # Spring Boot - API Server
│   ├── src/
│   │   └── main/java/com/example/
│   │       ├── controller/
│   │       │   ├── FirestoreManagementController.java
│   │       │   └── SyncController.java              # NEW
│   │       ├── service/
│   │       │   ├── AutocompleteService.java
│   │       │   └── FirestoreExcelImporter.java
│   │       ├── model/
│   │       │   ├── Category.java
│   │       │   ├── Keyword.java
│   │       │   └── Product.java
│   │       └── ...
│   ├── pom.xml
│   └── ...
│
├── frontend/                   # React Web Dashboard
│   ├── src/
│   │   ├── components/
│   │   │   └── FirestoreDashboard.jsx
│   │   └── ...
│   ├── package.json
│   └── ...
│
└── android/                    # Android Mobile App
    ├── app/
    │   ├── src/
    │   │   ├── main/
    │   │   │   ├── kotlin/com/example/
    │   │   │   │   ├── data/
    │   │   │   │   │   ├── local/
    │   │   │   │   │   │   ├── dao/
    │   │   │   │   │   │   │   ├── CategoryDao.kt
    │   │   │   │   │   │   │   ├── KeywordDao.kt
    │   │   │   │   │   │   │   └── ProductDao.kt
    │   │   │   │   │   │   └── database/
    │   │   │   │   │   │       └── AppDatabase.kt
    │   │   │   │   │   ├── remote/
    │   │   │   │   │   │   └── FirestoreApi.kt
    │   │   │   │   │   └── repository/
    │   │   │   │   │       ├── CategoryRepository.kt
    │   │   │   │   │       ├── KeywordRepository.kt
    │   │   │   │   │       └── ProductRepository.kt
    │   │   │   │   ├── domain/
    │   │   │   │   │   ├── model/
    │   │   │   │   │   │   ├── Category.kt
    │   │   │   │   │   │   ├── Keyword.kt
    │   │   │   │   │   │   └── Product.kt
    │   │   │   │   │   └── usecase/
    │   │   │   │   │       ├── SyncDataUseCase.kt
    │   │   │   │   │       └── SearchUseCase.kt
    │   │   │   │   ├── presentation/
    │   │   │   │   │   ├── viewmodel/
    │   │   │   │   │   │   └── SearchViewModel.kt
    │   │   │   │   │   └── ui/
    │   │   │   │   │       └── SearchScreen.kt
    │   │   │   │   └── MainActivity.kt
    │   │   │   └── res/
    │   │   └── test/
    │   ├── build.gradle.kts
    │   └── ...
    ├── build.gradle.kts
    └── settings.gradle.kts
    
    


```
### Backend
This is a presentation of the key aspects of the backend



### Firestore

Firestore is used because it is a good free option


##Tailscale

## Do You Need Tailscale on the VPS?

For your specific goal — **GitHub triggering a local Jenkins pipeline on your Mac** — you do **not strictly need** Tailscale installed on your VPS.

However, there *is* a **“Professional Tier”** reason why you might still want it. Here’s the breakdown 👇

---

## 1. Why Tailscale Is **NOT Required** for Your Jenkins Pipeline

The **Tailscale Funnel** only needs to exist **where Jenkins is running**. Since Jenkins is on your **MacBook**:

- GitHub sends the webhook to the **Tailscale Funnel (public URL)**
- The Funnel routes traffic to your **MacBook**
- Your MacBook sends files to the VPS using its **public IPv4** (`77.90.39.240`)

✅ Result: **Your pipeline works perfectly without Tailscale on the VPS**

---

## 2. Why You **SHOULD Install It Anyway** (The Benefits)

Even though the pipeline doesn’t require it, installing Tailscale on a **4GB VPS** is a major quality-of-life upgrade for two reasons:

### 🔐 Security — *“The Private Backdoor”*
- You can configure `ufw` to **block SSH (port 22) from the public internet**
- SSH access would only be possible via the **Tailscale IP**
- This completely eliminates brute-force bot attacks

### ⚙️ Cleaner Jenkins Configuration
- In your `Jenkinsfile`, you can use the **Tailscale IP** (`100.x.x.x`) instead of the public IP
- The Tailscale IP:
    - Is more stable
    - Does not change if you migrate VPS providers

---

## 3. How to Install It (Optional but Recommended)

If you want this extra security layer, run the following commands **as `mike` on your VPS**:

### Install Tailscale
```bash
curl -fsSL https://tailscale.com/install.sh | sh
```

### Github Webhook


### Categories

### Keywords


### Products


#Final thouths


## User-Driven Firestore Data Architecture

### Overview
This is a collaborative, crowd-sourced approach where users contribute new products organically. The system auto-generates keywords and manages categories intelligently.

### Complete User Flow
### Step 1: User Types Product Name
```bash
Mobile App → User inputs "iPhone 15 Pro Max"
           ↓
[Input field with real-time validation]
```

### Step 2: Check if Product Exists
```bash
User finishes typing
     ↓
App calls: GET /api/sync/product/exists?productName="iPhone 15 Pro Max"
     ↓
Backend checks Firestore collection
     ↓
Response:
{
  "productName": "iPhone 15 Pro Max",
  "exists": false  ← ✅ Can be added!
}

```


### Step 3: Load Category Suggestions
If product doesn't exist...
     ↓
App calls: GET /api/sync/categories/suggestions?productName="iPhone 15 Pro Max"
     ↓
Backend returns all available categories
     ↓
UI displays category list:
  • Electronics
  • Devices
  • Apple Products
  • ...
  • OUTROS (default)
     ↓
User selects category OR skips (defaults to OUTROS)
Step 4: User Submits Product
User clicks "Add Product" button
     ↓
App calls: POST /api/sync/contribute/product
{
  "productName": "iPhone 15 Pro Max",
  "categoryId": "electronics"  [optional]
}
     ↓
Backend processes:
  1. ✅ Validates product doesn't exist
  2. ✅ Verifies category exists (or uses OUTROS)
  3. ✅ Auto-generates keywords from name:
     ["iPhone", "15", "Pro", "Max", "iPhone 15", "Pro Max", etc.]
  4. ✅ Creates Product entity (isOfficial=false)
  5. ✅ Creates Keyword entities from generated keywords
  6. ✅ Updates autocomplete index
     ↓
Response:
{
  "message": "Product added successfully",
  "productId": "uuid-123456",
  "productName": "iPhone 15 Pro Max",
  "categoryId": "electronics",
  "generatedKeywords": ["iPhone", "15", "Pro", "Max", ...]
}
     ↓
UI shows success message ✅
Step 5: Data Synced to All Users
Backend updates Firestore:
  • products collection: new product added
  • keywords collection: new keywords added
  • autocomplete index: refreshed
     ↓
Next time other users sync data, they get:
  • New product in their local cache
  • New keywords for autocomplete
  • Better search suggestions

Data Model
Products Collection
json{
  "id": "uuid-123456",
  "productName": "iPhone 15 Pro Max",
  "categoryId": "electronics",
  "isOfficial": false,  // false = user-contributed
  "searchKeywords": ["iPhone", "15", "Pro", "Max", ...]
}
Keywords Collection
json{
  "id": "uuid-789012",
  "keyword": "iPhone",
  "categoryId": "electronics"
}
Categories Collection
json{
  "id": "electronics",
  "name": "Electronics",
  "classification": "TECH",
  "priority": 1
}

Backend Endpoints
EndpointMethodPurpose/api/sync/initial
GET
Download all data (initial sync)/api/sync/product/exists
GET
Check if product exists/api/sync/categories/suggestions
GET
Get category list for new product/api/sync/contribute/product
POST
Submit new product/api/sync/health
GET
Health check/api/sync/statsGETData statistics

Key Features
✅ No CMS Dashboard Needed

Users contribute data naturally through the app
Reduces admin overhead

✅ Auto-Generated Keywords

Keywords automatically created from product names
Improves autocomplete suggestions
No manual keyword management

✅ Smart Category Handling

User selects from existing categories
Defaults to "OUTROS" if not selected
System prevents data inconsistency

✅ User vs Official Products

Official products: isOfficial = true (from initial seed)
User-contributed: isOfficial = false
Track origin of data

✅ Local-First Search

All data cached on device
Instant search without network
Works offline completely

✅ Real-Time Collaboration

When one user adds a product, all others eventually see it
Crowdsourced data becomes richer over time


### Example User Journeys

Journey 1: Product Doesn't Exist

1. User searches "Samsung Galaxy S24"
2. Not found in local cache
3. User clicks "Add new product"
4. App checks: Product doesn't exist ✅
5. Shows category suggestions
6. User selects "Electronics"
7. Submits
8. Server:
   - Creates product
   - Auto-generates: ["Samsung", "Galaxy", "S24", ...]
   - Creates keywords
   - Syncs to all users

Journey 2: Product Already Exists

1. User searches "iPhone 15"
2. Found in local cache
3. Display immediately
4. No need to add anything

Journey 3: User Forgets Category

1. User adds product "Laptop"
2. Doesn't select any category
3. System defaults to "OUTROS"
4. Product still added successfully
5. Later, admin can recategorize if needed

### Future Enhancements

- Incremental Sync:
  - Instead of syncing all data each time, sync only changes since last sync
  - Reduces bandwidth

- Duplicate Detection
  - Fuzzy matching to detect "iPhone 15" vs "iPhone 15 Pro"
  - Ask user for confirmation

    
User Attribution

Track which user contributed what
Reputation/contribution badges

Data Quality Ratings

Community votes on keyword quality
Surface best keywords first

Admin Panel

Optional: Review/approve user contributions
Manually categorize products
Remove spam/duplicates

## Architecture Summary

### Data Flow

```text
Firestore (Backend - Single Source of Truth)
│
├──▶ Web Dashboard (React) - Real-time management
│     └──▶ FirestoreManagementController
│
└──▶ Android App - Initial Sync
      └──▶ SyncController (/api/sync/initial)
            └──▶ Local SQLite Database (Room)
                  └──▶ Instant offline search & autocomplete

```

Excellent! Now the architecture properly distinguishes between:
isOfficial Flag Usage
isOfficial = true (Official Products)

Come from initial Excel seed via FirestoreDataSeeder
Endpoint: GET /api/sync/products/official
Trusted, curated data
Foundation of the catalog

isOfficial = false (User-Contributed Products)

Added by users through the mobile app
Endpoint: GET /api/sync/products/user-contributed
Community-sourced data
Enriches the catalog over time

Statistics Now Show Breakdown
json{
"productsCount": 1500,
"productsBreakdown": {
"official": 1000,
"userContributed": 500
},
"message": "Official products: 1000 | User-contributed: 500"
}
Use Cases for This
You could now:

Show users "1,000 official products" vs "500 community-added"
Filter search results by source
Build trust by showing data provenance
Eventually build a review system for user-contributed products
Create a "Community Contributions" feature/badge
Set different auto-complete weights (maybe official products appear first)

The architecture is now perfectly balanced between:

✅ Curated official data (from your Excel seed)
✅ Community growth (from user contributions)
✅ Complete transparency (you always know the source)

This is production-grade! 🚀

Infrastructure & Security
Component	Configuration	Status
Server Hostname	prod-api-01	✅ Active
Primary User	mike	✅ Sudo Enabled
SSH Key	vps_ml_key	✅ Passwordless
Firewall (UFW)	Ports: 22 (Tailscale), 8080 (Public)	✅ Secured
Private Network	Tailscale Mesh (Internal 100.x.x.x)	✅ Connected
Exportar para Sheets

3. GitHub Webhook Settings
   Your repository is configured to trigger Jenkins via a secure Tailscale Funnel.
* Payload URL: https://your-mac-node.tail-net.ts.net/github-webhook/
* Content Type: application/json
* Events: Just the push event
* Secret: •••••••••••••••• (Configured in Jenkins)

4. Systemd Service Definition
   Located at /etc/systemd/system/myapp.service on your VPS:
   Ini, TOML

[Unit]
Description=Spring Boot API Service
After=network.target

[Service]
User=mike
ExecStart=/usr/bin/java -Xmx2048m -jar /home/mike/cartshare/backend-0.0.1-SNAPSHOT.jar
Environment=GOOGLE_APPLICATION_CREDENTIALS=/home/mike/cartshare/google-key-vps.json
Restart=always

[Install]
WantedBy=multi-user.target


# CartShare Backend

A Spring Boot-based synchronization and search service for the CartShare mobile ecosystem. This service manages a high-performance dictionary of products and keywords using Google Firestore, designed for a mobile-first "Flat Data" architecture.

## 🚀 Key Features
- Flat Data Synchronization: Streamlines the delivery of product and keyword data. Grouping (e.g., by Supermarket) is handled entirely by the mobile client for maximum flexibility.
- Intelligent Autocomplete: A custom AutocompleteService providing:
- Normalization: Strips accents (e.g., "pão" -> "pao") using java.text.Normalizer.
- Fuzzy Matching: Uses the Levenshtein Distance algorithm to find products despite typos.
- Priority Ranking: Ensures "Official" products (from seed data) appear before user-contributed items.
- Universal Product Contribution: Allows users to add new products, which are automatically indexed into search keywords.

## 🛠️ Project Structure

We follow a minimalist Spring Boot structure focused on data delivery:

api.controller: Simplified endpoints for search, sync, and contribution.
service: Core logic for Firestore interactions and fuzzy search algorithms.
util: Utility classes for ClassLoader resource loading and String manipulation.
config: Firestore and Security configurations (currently set to permitAll() for development).

## 🧪 Testing & Quality
We maintain a strict quality gate with a 85% code coverage target using JaCoCo.

## Run Tests & Generate Report

``` bash

./gradlew clean test jacocoTestReport
View the report at: backend/build/reports/jacoco/test/html/index.html

```

## ⚙️ Troubleshooting

1. Firestore & Connectivity
   Credentials: Ensure GOOGLE_APPLICATION_CREDENTIALS points to your service account JSON.

Permissions: The service account requires Cloud Datastore User permissions.

2. Search Performance
   Warm-up: On startup, the AutocompleteWarmup class fetches all records to build the in-memory masterIndex. If search feels slow initially, check logs for >>> Starting Autocomplete Index Warm-up.

Fuzzy Logic: Distance 1 is allowed for terms < 4 chars; Distance 2 for longer terms.

## 📦 Deployment
The application is packaged as a standalone executable JAR:

``` bash

./gradlew bootJar
The artifact is generated in build/libs/.
```

## Why the Flat Model?
By eliminating the Category entity from the backend, the system is now:

- Faster: Fewer Firestore joins and lookups.
- Flexible: The mobile app can group products by Supermarket, Store, or aisle without backend changes.
- Simpler: 30% less code to maintain and test.


