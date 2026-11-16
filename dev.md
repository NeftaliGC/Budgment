# Budgment — Notas de Desarrollo

> Este archivo es para el desarrollador interno. Contiene estado del proyecto, decisiones técnicas, pendientes y notas que no deben verse en el README público.

---

## 📌 Estado del repositorio

### Commits en `main` (12 — SHAs anteriores a `b8956d0` reescritos por filter-branch)

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
| `9cfc705` | actualizar dev.md: schemas eliminados del historial |
| `f0b73a0` | feat(ui): accesibilidad, borrado seguro (deshacer/confirmación), formularios, `AmountParser` + tests |
| `5d5e54f` | feat(ui): rediseño de Inicio, pantalla de Configuración, `BudgetOverviewSource`, un solo presupuesto global |

> ⚠️ **`app/schemas/` eliminado permanentemente del historial** mediante `git filter-branch` + `reflog expire` + `gc --prune=now`. Los archivos `1.json` y `2.json` ya no existen en ningún commit ni en disco. Refs originales de filter-branch limpiadas.

### Cambios no commiteados

Solo `.idea/deploymentTargetSelector.xml` (archivo del IDE). Todo el código está commiteado hasta `5d5e54f` (2026-11-15).

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
| `TransferEntity` | `TransferDao` | `TransferRepository` | `TransferFormViewModel` (+ `TransactionsViewModel`, `HomeViewModel`) | TransferFormScreen (+ filas en Movimientos e Inicio) | ✅ Completo |

### Entidades secundarias (DAO existe, sin Repo/ViewModel/Screen)

| Entity | DAO | Repo | ViewModel | Screen | Estado |
|--------|-----|------|-----------|--------|--------|
| `RecurringTransactionEntity` | `RecurringTransactionDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `GoalEntity` | `GoalDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `TagEntity` | `TagDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `AttachmentEntity` | `AttachmentDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
| `BudgetCategoryEntity` | `BudgetCategoryDao` | ✅ (dentro de `BudgetRepositoryImpl`) | ✅ | ✅ | ✅ Completo |
| `TransactionTagEntity` | `TransactionTagDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |

### Repositorios existentes (8)

| Repositorio | Archivo | Estado |
|-------------|---------|--------|
| `AccountRepository` | `data/repository/AccountRepository.kt` | ✅ Completo |
| `AuthRepository` | `data/repository/AuthRepository.kt` | ✅ Completo |
| `CategoryRepository` | `data/repository/CategoryRepository.kt` | ✅ Completo |
| `TransactionRepository` | `data/repository/TransactionRepository.kt` | ✅ Completo |
| `BudgetRepository` | `data/repository/BudgetRepository.kt` | ✅ Completo |
| `UserRepository` | `data/repository/UserRepository.kt` | ✅ Completo |
| `ExchangeRateRepository` | `data/repository/ExchangeRateRepository.kt` | ✅ Completo |
| `TransferRepository` | `data/repository/TransferRepository.kt` | ✅ Completo |

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
- `PeriodUnit`: DAY, WEEK, MONTH, YEAR (el formulario ofrece semanal, mensual y anual; intervalo 1)
- **Periodos renovables**: `startDate` es el ancla (inicio del primer periodo, alineado al calendario: lunes, día 1 o 1 de enero). `BudgetPeriod.current()` / `BudgetEntity.currentPeriod()` calculan el periodo vigente al leer, sin trabajo en segundo plano. `endDate` ya no se usa (se guarda `null`)
- `BudgetOverviewSource` se recalcula a medianoche (`dayTicks()`), así el presupuesto se renueva con la app abierta
- El resumen usa una sola periodicidad: la del global, o sin global la más usada entre los de categoría (empate → mensual); los demás se marcan con `hasOtherPeriods`

### TransactionEntity
```kotlin
id, userId, accountId, categoryId, amount: Long, // signado: negativo=expense, positivo=income
currency, description, date, createdAt, updatedAt, deletedAt
```
- `categoryId` es **nullable** (BD v3): `null` = pata de una transferencia. Así no cuenta en presupuestos ni en conteos de categorías

### TransferEntity
```kotlin
id, userId, fromAccountId, toAccountId,
fromTransactionId, toTransactionId, createdAt
```
- Dos transacciones asociadas (una por cuenta): salida negativa en origen, entrada positiva en destino, ambas sin categoría
- Unique constraint en fromTransactionId y toTransactionId
- `TransferRepository` crea/edita/borra/restaura ambas patas + `balanceCache` dentro de `database.withTransaction`
- Monedas distintas: el formulario sugiere el monto que llega con `ExchangeRateRepository.convert` y el usuario puede ajustarlo
- En listas (`buildTransactionDetails`) cada transferencia es **una sola fila** (la salida) con `TransferDetails`; color neutro, sin signo, ícono `ic_transfer`
- Acceso: acción en la barra de Cuentas (con ≥ 2 cuentas) y de Movimientos; tocar la fila abre `transfer_form?transferId=`

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
- **RepositoryModule**: Binds de los 8 repositorios
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
- **Version:** 3 (v3: `transactions.categoryId` nullable para transferencias)
- **Migration strategy:** `fallbackToDestructiveMigration(dropAllTables = true)` ⚠️
- **13 entities** registradas en `@Database`
- **13 DAOs** provistos via `DatabaseModule`
- `exportSchema = true`, schema guardado en `schemas/`

### Navegación
- `BudgmentNavHost` con `NavHost` y `rememberNavController`
- BottomBar con 4 pestañas: Home, Accounts, Transactions, Budgets
- Estado conservado entre pestañas (`saveState = true`, `restoreState = true`)
- Rutas: login, register, home, settings (desde el menú de perfil), accounts, account_form, categories, category_form, transactions, transaction_form, transfer_form, budgets, budget_form
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
- Selector de periodicidad (semanal/mensual/anual); cada tarjeta muestra «Mensual · se renueva el 1 dic»

---

## 🗺️ Mapa de lo que falta (actualizado 2026-11-15, tras `5d5e54f`)

Leyenda: ✅ hecho · 🟡 parcial · ❌ falta

### A. Funciones con Entity/DAO, sin Repo/ViewModel/Screen

| Feature | Entity / DAO | Repo | VM | Screen | Depende de | Notas |
|---------|--------------|------|----|--------|------------|-------|
| Recurrentes | ✅ `RecurringTransactionEntity`, `RecurringTransactionDao` (`getDueRecurrences`) | ❌ | ❌ | ❌ | WorkManager | Worker periódico que materializa ocurrencias vencidas y avanza `nextOccurrence` |
| Metas de ahorro | ✅ `GoalEntity`, `GoalDao` | ❌ | ❌ | ❌ | (Transferencias, opcional) | Aportes manuales o vinculados a una cuenta de ahorro |
| Etiquetas | ✅ `TagEntity`, `TagDao`, `TransactionTagDao` | ❌ | ❌ | ❌ | — | Selector en `TransactionFormScreen` + filtro en Movimientos |
| Adjuntos | ✅ `AttachmentEntity`, `AttachmentDao` | ❌ | ❌ | ❌ | Etiquetas no | Foto de recibo; almacenamiento en `filesDir`, incluir en respaldo |

### B. Funciones sin ningún código

| Feature | Qué hace falta | Depende de |
|---------|----------------|------------|
| Estadísticas / reportes | Pantalla de gráficos (gasto por categoría, ingreso vs gasto por mes, evolución de saldo) + pestaña o acceso. Íconos `ic_estadisticas`, `ic_up_stats`, `ic_down_stats` sin usar | — |
| PIN / biometría | `BiometricPrompt` + PIN de respaldo, bloqueo al volver de segundo plano, opción en Configuración | Configuración |
| Respaldo en Google Drive | Exportar/importar BD (+ adjuntos), Drive REST API, Worker de respaldo automático | WorkManager, migraciones reales |
| Notificaciones | Alertas de presupuesto (80 %/100 %) y de recurrentes; canal + permiso `POST_NOTIFICATIONS` | WorkManager |

### C. Funciones parciales

| Feature | Estado | Falta |
|---------|--------|-------|
| Subcategorías | 🟡 `parentCategoryId` en modelo | UI no lo usa: selector de padre en `CategoryFormScreen`, agrupar en lista, sumar hijas en presupuestos |
| Configuración | 🟡 Solo moneda por defecto | Cambio de contraseña, borrar usuario, tema, seguridad (PIN), respaldo, exportar CSV |
| Movimientos | 🟡 Lista agrupada por día | Búsqueda y filtros (cuenta, categoría, rango de fechas, etiqueta) |

### D. Deuda técnica

| Tarea | Ubicación | Prioridad |
|-------|-----------|-----------|
| ✅ ~~Typo `imzpl` → `impl`~~ | `di/RepositoryModule.kt:33` | Hecho |
| Quitar `fallbackToDestructiveMigration` y escribir migraciones | `di/DatabaseModule.kt:44` (BD versión 3) | Bloquea publicación y respaldo |
| Capa `domain/` (casos de uso) | No existe, aunque el README la menciona | Opcional; o quitarla del README |
| Tests | Solo `AmountParserTest`, `BuildTransactionDetailsTest` y `BudgetPeriodTest` | Repos (balance, presupuestos), `BudgetOverviewSource`, DAOs (instrumentados) |
| `backup_rules.xml` / `data_extraction_rules.xml` | `res/xml/` | Configurar o excluir la BD |
| Actualizar README (fase actual, características) | `README.md` | Al cerrar cada fase |

### Orden sugerido

1. ✅ ~~Deuda trivial: typo + este documento al día.~~
2. ✅ ~~**Transferencias**~~.
3. ✅ ~~**Renovación de periodos de presupuesto**~~ (calculada al leer, sin WorkManager). **WorkManager** como base → **recurrentes**.
4. **Estadísticas**.
5. **Notificaciones** (reusa WorkManager).
6. **Metas**, **etiquetas**, **subcategorías en UI**, filtros de movimientos.
7. **Migraciones reales** → **respaldo en Drive** → **PIN/biometría**.
8. **Adjuntos**.
9. Tests, pulido, README, publicación.

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

### WorkManager
- Dependencia declarada en `build.gradle.kts` (`libs.work.runtime.ktx`)
- **Sin usar en ningún archivo.** Planeado para transacciones recurrentes y notificaciones.

### Drawables sin uso
- `ic_estadisticas`, `ic_up_stats`, `ic_down_stats` — reservados para Estadísticas
- (`logo_mark.png` ya se usa en Login y en la barra superior; los strings sin uso se eliminaron en `5d5e54f`)

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
- `AmountParserTest`, `BuildTransactionDetailsTest` y `BudgetPeriodTest` (unitarios). Sin tests de repos, ViewModels ni DAOs.

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

Ver **🗺️ Mapa de lo que falta → Orden sugerido** más arriba.

---

## 👤 Autor

Fabian
