# CartShareApp 🛒

A modern Android application for sharing and managing shopping products with real-time synchronization and local caching.

## 🚀 Tech Stack
* **UI**: Jetpack Compose with Material 3
* **Language**: Kotlin
* **Database**: Room (with KSP)
* **Networking**: Retrofit & Gson
* **Dependency Injection**: Hilt
* **Authentication**: Firebase Auth & Google Credential Manager
* **Architecture**: Clean Architecture (Domain, Data, Presentation)

---

## 🏗 Project Structure
To keep the project organized according to **Clean Architecture**, the following package structure is implemented in `java/com.example.cartshareapp`:

* **`data/local/`**: Local persistence logic (Entities, DAOs, Room Database).
* **`data/remote/`**: Network communication (API interfaces, Response models).
* **`data/repository/`**: Implementation of data logic (Sync, Auth, and Contribution).
* **`domain/model/`**: Pure Kotlin data classes used across the app.
* **`domain/usecase/`**: Business logic units (e.g., `SearchUseCase`).
* **`di/`**: Hilt Modules for dependency injection.
* **`presentation/`**: UI layer (ViewModels and Compose Screens).



---

## 🛠 Setup & Implementation

### 1. Database & API Implementation
Place the core data components into their respective files:
1. **Entities**: Place `CategoryEntity`, `ProductEntity`, and `KeywordEntity` in `data/local/entity`.
2. **DAOs**: Place the three DAO interfaces in `data/local/dao`.
3. **Database**: Create the `AppDatabase` class in `data/local/database`. *Note: Ensure `StringListConverter` is registered to handle list serialization.*

### 2. Initialization (Hilt & Firebase)
Create a custom Application class to initialize Hilt.

```kotlin
@HiltAndroidApp
class MainApplication : Application()