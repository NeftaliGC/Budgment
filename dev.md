# Budgment — Notas de Desarrollo

> Este archivo es para el desarrollador interno. Contiene estado del proyecto, decisiones técnicas, pendientes y notas que no deben verse en el README público.

---

## 📌 Estado del repositorio

### Commits en `main` (10 — SHAs reescritos por filter-branch)

| Commit | Descripción |
|--------|-------------|
| `32e457a` | first commit |
| `663d155` | readme and docs init |
| `80f2d1e` | modelos iniciales |
| `f3caaac` | daos y bd |
| `8d946ee` | bd completa ⚠️ schemas eliminados por filter-branch |
| `693a71c` | pantallas de login y register |
| `dd1aec7` | pantallas de categorias y movimientos |
| `2af6b7f` | presupuestos, rediseno home y categorias, system bars y tasas de cambio |
| `b8956d0` | exclude app/schemas/ from tracking |
| `b013327` | work in progress: UI improvements, AccountIcons, dev.md, README update |

> ⚠️ **`app/schemas/` eliminado permanentemente del historial** mediante `git filter-branch` + `reflog expire` + `gc --prune=now`. Los archivos `1.json` y `2.json` ya no existen en ningún commit ni en disco. Refs originales de filter-branch limpiadas.

### Cambios no commiteados

**Ninguno.** Working directory limpio. Todo el trabajo actual está commiteado en `b013327`.

---

## 📊 Matriz de entidades (Entity → DAO → Repo → ViewModel → Screen)

### Entidades principales (FULLY IMPLEMENTADAS)

| Entity | DAO | Repo | ViewModel | Screen | Estado |
|--------|-----|------|-----------|--------|--------|
| `UserEntity` | `UserDao` | `UserRepository` | `SessionViewModel` | Login, Register | ✅ Completo |
| `AccountEntity` | `AccountDao` | `AccountRepository` | `AccountsViewModel`, `AccountFormViewModel` | AccountsListScreen, AccountFormScreen | ✅ Completo (en desarrollo UI) |
| `CategoryEntity` | `CategoryDao` | `CategoryRepository` | `CategoryFormViewModel` | CategoriesListScreen, CategoryFormScreen | ✅ Completo |
| `TransactionEntity` | `TransactionDao` | `TransactionRepository` | `TransactionsViewModel`, `TransactionFormViewModel` | TransactionsListScreen, TransactionFormScreen | ✅ Completo |
| `BudgetEntity` | `BudgetDao` | `BudgetRepository` | `BudgetsViewModel`, `BudgetFormViewModel` | BudgetsListScreen, BudgetFormScreen | ✅ Completo |
| `ExchangeRateEntity` | `ExchangeRateDao` | `ExchangeRateRepository` | — | (integrado en HomeViewModel, AccountsViewModel, BudgetsViewModel) | ✅ Completo |

### Entidades secundarias (DAO existe, sin Repo/ViewModel/Screen)

| Entity | DAO | Repo | ViewModel | Screen | Estado |
|--------|-----|------|-----------|--------|--------|
| `RecurringTransactionEntity` | `RecurringTransactionDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `TransferEntity` | `TransferDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `GoalEntity` | `GoalDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `TagEntity` | `TagDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `AttachmentEntity` | `AttachmentDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `BudgetCategoryEntity` | `BudgetCategoryDao` | ✅ (dentro de `BudgetRepositoryImpl`) | ✅ | ✅ | ✅ Completo |
| `TransactionTagEntity` | `TransactionTagDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |

### Repositorios existentes (7)

| Repositorio | Archivo | Estado |
|-------------|---------|--------|
| `AccountRepository` | `data/repository/AccountRepository.kt` | ✅ Completo |
| `AuthRepository` | `data/repository/AuthRepository.kt` | ✅ Completo |
| `CategoryRepository` | `data/repository/CategoryRepository.kt` | ✅ Completo |
| `TransactionRepository` | `data/repository/TransactionRepository.kt` | ✅ Completo |
| `BudgetRepository` | `data/repository/BudgetRepository.kt` | ✅ Completo |
| `UserRepository` | `data/repository/UserRepository.kt` | ✅ Completo |
| `ExchangeRateRepository` | `data/repository/ExchangeRateRepository.kt` | ✅ Completo |

**Nota:** `BudgetCategoryDao` está integrado dentro de `BudgetRepositoryImpl` (no tiene repositorio separado). Los DAOs de `TransactionTagDao` tampoco tienen repositorio separado (están pensados para ser gestionados junto con Tag en una futura implementación).

---

## 🧱 Modelos de datos clave

### AccountEntity
```kotlin
id, userId, name, accountType: AccountType, currency, icon, color,
balanceCache: Long, // en centavos
creditLimit: Long?, // crédito
statementDay: Int?, // día de corte
paymentDueDay: Int?, // día límite de pago
createdAt, updatedAt, deletedAt
```
- `AccountType`: CASH, DEBIT, CREDIT, SAVINGS, INVESTMENT
- `balanceCache`: saldo calculado en centavos, negativo para crédito
- `defaultColorHex()` definido en `AccountIcon.kt`: CASH=#00897B, DEBIT=#1976D2, CREDIT=#E64A19, SAVINGS=#7B1FA2, INVESTMENT=#388E3C

### CategoryEntity
```kotlin
id, userId, parentCategoryId: String?, // null = categoría raíz
name, type: CategoryType, icon, color, createdAt, updatedAt, deletedAt
```
- Soporta subcategorías via `parentCategoryId` (FK a sí misma)
- `CategoryType`: EXPENSE, INCOME

### BudgetEntity
```kotlin
id, userId, name, amountLimit: Long, // centavos
scope: BudgetScope, periodInterval, periodUnit: PeriodUnit,
startDate, endDate, createdAt, updatedAt, deletedAt
```
- `BudgetScope`: GLOBAL, CATEGORY
- `PeriodUnit`: DAY, WEEK, MONTH, YEAR

### TransactionEntity
```kotlin
id, userId, accountId, categoryId, amount: Long, // signado: negativo=expense, positivo=income
currency, description, date, createdAt, updatedAt, deletedAt
```

### TransferEntity
```kotlin
id, userId, fromAccountId, toAccountId,
fromTransactionId, toTransactionId, createdAt
```
- Dos transacciones asociadas (una por cuenta)
- Unique constraint en fromTransactionId y toTransactionId

### RecurringTransactionEntity
```kotlin
id, userId, template fields, nextOccurrence, active,
deletedAt, updatedAt
```
- `getDueRecurrences(upToDate)` en DAO para WorkManager

### GoalEntity
```kotlin
id, userId, name, targetAmount, currentAmount, targetDate,
completed, deletedAt
```

### TagEntity
```kotlin
id, userId, name, color, createdAt
```

### ExchangeRateEntity
```kotlin
id, fromCurrency, toCurrency, rateMicros: Long, date
```
- Tasas almacenadas en micros (1/1,000,000) para precisión

---

## 🏗️ Arquitectura y decisiones técnicas

### Hilt DI
- **DatabaseModule**: Crea `BudgmentDatabase` singleton con `fallbackToDestructiveMigration(dropAllTables = true)`
- **RepositoryModule**: Binds de los 7 repositorios. **Nota: hay un typo `imzpl` en `bindAuthRepository`** (línea 33)
- **NetworkModule**: Retrofit + OkHttpClient → `ExchangeRateApi` (Frankfurter API, base URL: `https://api.frankfurter.app/`)
- **SessionModule**: DataStore (`budgment_session`) para user_id y display_currency

### SessionManager
- `SessionManagerImpl` con DataStore + `MutableStateFlow` en memoria
- Login con `rememberMe`: persiste en DataStore; sin rememberMe: solo en memoria
- `currentUserId: Flow<String?>`, `displayCurrency: Flow<String?>`
- `login(userId, rememberMe)`, `setDisplayCurrency(code)`, `logout()`

### PasswordHasher
- PBKDF2 con HmacSHA256, 120,000 iteraciones, clave de 256 bits
- Formato almacenado: `iterations:saltBase64:digestBase64`
- Usado solo en `AuthRepositoryImpl`

### ExchangeRateRepository
- Frankfurter API v2 (`https://api.frankfurter.app/latest?from=USD&symbols=...`)
- Caché local con TTL de 6 horas
- Mutex para evitar refrescos concurrentes
- Conversión por pivote USD si no existe par directo
- `convert(cents, from, to)` → `Long?` (null si tasa no disponible)
- Tasas almacenadas en micros para evitar punto flotante

### Room Database
- **Version:** 2
- **Migration strategy:** `fallbackToDestructiveMigration(dropAllTables = true)` ⚠️
- **13 entities** registradas en `@Database`
- **13 DAOs** provistos via `DatabaseModule`
- `exportSchema = true`, schema guardado en `schemas/`

### Navegación
- `BudgmentNavHost` con `NavHost` y `rememberNavController`
- BottomBar con 4 pestañas: Home, Accounts, Transactions, Budgets
- Estado conservado entre pestañas (`saveState = true`, `restoreState = true`)
- Rutas: login, register, home, accounts, account_form, categories, category_form, transactions, transaction_form, budgets, budget_form
- `BudgmentDestination` como sealed class con `createRoute()` helpers

### UI Pattern
- Cada screen tiene dos composables: `XxxScreen` (con ViewModel) y `XxxScreenContent` (puro UI)
- ViewModel con `@HiltViewModel`, StateFlow para UI state
- `LocalInspectionMode.current` para previews sin Hilt
- `BudgmentScaffold` con topBar, bottomBar, FAB
- `BudgmentTopBar` con userName, settings, logout
- `BudgmentBottomBar` con NavigationBarItem

---

## 📱 Pantallas UI implementadas

### LoginScreen / RegisterScreen
- Formularios con validación
- `AuthRepository` con `AuthResult` sellado (Success/Error)
- `AuthError` enum: USERNAME_TAKEN, INVALID_CREDENTIALS, EMPTY_FIELDS, PASSWORDS_DO_NOT_MATCH

### HomeScreen
- Saldo total convertido a moneda seleccionada
- Selector de moneda (FilterChip con SupportedCurrency.ALL)
- `missingRateAccounts` — cuenta las cuentas sin tasa de cambio
- Acciones rápidas: nuevo movimiento, cuenta, categoría, presupuesto
- Lista de cuentas (hasta 4, en cards 2x2)
- Chips de categorías

### AccountsListScreen
- `TotalSummaryHeader` con saldo total, conteo, currency selector
- Accounts agrupadas por tipo (AccountType.entries)
- `AccountCard` con icono, nombre, tipo, saldo, delete button
- `AccountFormScreen` con selector de iconos (7 opciones), paleta de colores, dropdown de tipo, dropdown de currency, campos crédito condicionales

### CategoriesListScreen / CategoryFormScreen
- Chips de categorías con icono + color
- Formulario con name, type, icon picker, color picker
- Soporte para subcategorías (modelo `parentCategoryId`)

### TransactionsListScreen / TransactionFormScreen
- Lista ordenada por fecha descendente
- Filas con categoryName, accountName, date, amount
- Delete button por transacción
- Formulario: account/category dropdowns, amount, description
- `TransactionFormError`: MISSING_FIELDS, INVALID_AMOUNT

### BudgetsListScreen / BudgetFormScreen
- `BudgetSummaryHeader`: allocated, spent, remaining, progress bar
- Secciones: GLOBAL y CATEGORY
- `BudgetWithProgress` con spent calculado y progreso
- `BudgetCard` con linkedCategories, delete button
- Formulario: name, amountLimit, scope (GLOBAL/CATEGORY), category selector
- `currentMonthRange()` helper para fechas

---

## ❌ Lo que falta por implementar

### Por entidad

| Feature | Entity/DAO existe | Repo falta | ViewModel falta | Screen falta |
|---------|-------------------|------------|-----------------|--------------|
| Transferencias | ✅ TransferEntity, TransferDao | ✗ | ✗ | ✗ |
| Recurrentes | ✅ RecurringTransactionEntity, RecurringTransactionDao | ✗ | ✗ | ✗ |
| Metas de ahorro | ✅ GoalEntity, GoalDao | ✗ | ✗ | ✗ |
| Tags | ✅ TagEntity, TagDao, TransactionTagDao | ✗ | ✗ | ✗ |
| Adjuntos | ✅ AttachmentEntity, AttachmentDao | ✗ | ✗ | ✗ |
| Estadísticas | ❌ Ninguna | ✗ | ✗ | ✗ (icons existen) |
| PIN/Biometría | ❌ Ninguna | ✗ | ✗ | ✗ |

### Lista priorizada de tareas pendientes

1. **Capa de dominio** (`domain/`) — Crear casos de uso, actualizar arquitectura a Clean Architecture
2. **Transferencias** — Crear `TransferRepository`, `TransferViewModel`, pantalla de transferencias
3. **Recurrentes** — Crear `RecurringTransactionRepository`, `RecurringTransactionViewModel`, pantalla + integración con WorkManager
4. **Metas** — Crear `GoalRepository`, `GoalViewModel`, pantalla
5. **Tags** — Crear `TagRepository`, `TagViewModel`, pantalla de gestión
6. **Adjuntos** — Crear `AttachmentRepository`, pantalla de adjuntos
7. **Estadísticas/Reportes** — Crear pantalla de gráficos (icons ya existen en `res/drawable/`)
8. **PIN/Biometría** — Pantalla de bloqueo con BiometricPrompt o PIN dialog
9. **Google Drive backup** — Integrar Drive REST API, WorkManager para backup automático
10. **Configuración** — Pantalla de settings
11. **Notificaciones** — WorkManager + notifications para alertas de presupuesto y recurrencias

---

## ⚠️ Notas y problemas conocidos

### `app/schemas/` eliminado permanentemente (2026-09-27)
- Los archivos `app/schemas/com.nintech.budgment.data.local.BudgmentDatabase/1.json` y `2.json` fueron **eliminados permanentemente del historial** con `git filter-branch` + `reflog expire` + `gc --prune=now`
- Los archivos no existen en ningún commit ni en disco
- `app/schemas/` fue agregado a `.gitignore` para evitar reintroducirlos accidentalmente
- Los SHAs originales de los commits cambiaron tras filter-branch (referencias anteriores rotas)
- Los refs originales de backup fueron limpiados con `git for-each-ref --format='delete %(refname)' refs/original/`

### `fallbackToDestructiveMigration`
- En `DatabaseModule.kt` línea 44: `fallbackToDestructiveMigration(dropAllTables = true)`
- **Eliminar antes de publicar.** Borra y recrea la BD ante cualquier cambio de esquema.

### Tipo en RepositoryModule
- `RepositoryModule.kt` línea 33: `bindAuthRepository(imzpl: AuthRepositoryImpl)` — typo `imzpl` debería ser `impl`
- Funciona pero es un error ortográfico

### WorkManager
- Dependencia declarada en `build.gradle.kts` (`libs.work.runtime.ktx`)
- **Sin usar en ningún archivo.** Planeado para transacciones recurrentes y notificaciones.

### Strings sin usar
- `bottom_nav_stats` en `strings.xml` — no hay pantalla Stats ni pestaña en BottomBar
- `ic_estadisticas`, `ic_up_stats`, `ic_down_stats` en `res/drawable` — sin uso
- `home_action_coming_soon` — string "Próximamente" sin uso en UI actual

### Drawables con recursos no utilizados
- `logo_mark.png` — no usado en código visible
- `ic_transfer.xml` — sin uso (transferencias no implementadas)

### backup_rules.xml / data_extraction_rules.xml
- Existen en `res/xml/` pero no configurados para nada funcional

### Cálculo de balance
- `AccountRepository.adjustBalance()` ajusta `balanceCache` directamente
- `TransactionRepository` llama a `adjustBalance` al crear/actualizar/eliminar
- El balance se calcula solo para cuentas, no para presupuestos o metas

### Category subcategorías
- `CategoryEntity` tiene `parentCategoryId: String?` con FK self-referencing
- `CategoryDao` tiene query para `observeCategories(userId)`
- **UI no expone** la creación de subcategorías

### Currency conversion edge cases
- `ExchangeRateRepository.convert()` retorna `null` si no hay tasa → se muestra error en UI
- `HomeViewModel` cuenta `missingRateAccounts`
- Conversión sin red: usa caché local, fallback por pivote USD

---

## 📦 Configuración build

### `build.gradle.kts` (app)
```kotlin
// Compiles
compileSdk = 37, minSdk = 24, targetSdk = 37
versionCode = 1, versionName = "1.0"

// Plugins
android.application, kotlin.compose, hilt, ksp

// Dependencias clave
- androidx.core.ktx, lifecycle.runtime.ktx, activity.compose
- compose BOM, compose.ui, compose.material3
- hilt.android, hilt.navigation.compose, hilt.compiler (ksp)
- room.runtime, room.ktx, room.compiler (ksp)
- navigation.compose
- work.runtime.ktx
- datastore.preferences
- retrofit, retrofit.converter.gson (Frankfurter API)
- compose.icons.core, compose.icons.extended
- errorprone.annotations
```

### Kotlin Version
- Consultar `libs.versions.toml` para versiones exactas

### Testing
- `testImplementation libs.junit`
- `androidTestImplementation libs.androidx.junit`
- `androidTestImplementation libs.androidx.espresso.core`
- **Sin tests escritos.** Las dependencias están declaradas.

### KSP
- `ksp` plugin aplicado
- `room.schemaLocation = "$projectDir/schemas"` en `ksp {}` block
- Schema files generados en `app/schemas/`

---

## 🔧 Stack y dependencias

| Componente | Librería | Versión ref |
|-----------|----------|-------------|
| DI | Hilt | `libs.hilt.android` |
| Persistencia | Room | `libs.room.runtime` |
| Navegación | Navigation Compose | `libs.navigation.compose` |
| Background | WorkManager | `libs.work.runtime.ktx` |
| API | Retrofit + Gson | `libs.retrofit` |
| DataStore | Preferences | `libs.datastore.preferences` |
| Icons | Compose Icons Core + Extended | `libs.compose.icons.core` |
| Red | OkHttp | Built-in en NetworkModule |

---

## 📝 Orden de migración recomendado

1. `RepositoryModule.kt` typo fix (`imzpl` → `impl`)
2. Crear `domain/` con casos de uso básicos
3. Crear `TransferRepository` + ViewModel + Screen
4. Crear `RecurringTransactionRepository` + ViewModel + Screen + WorkManager integration
5. Crear `GoalRepository` + ViewModel + Screen
6. Crear `TagRepository` + ViewModel + Screen
7. Crear `AttachmentRepository` + Screen
8. Crear Stats screen + BottomBar tab
9. Implementar PIN/Biometría
10. Google Drive backup + WorkManager
11. Quitar `fallbackToDestructiveMigration`
12. Escribir tests
13. Publicar

---

## 👤 Autor

Fabian
