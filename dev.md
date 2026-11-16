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
| `RecurringTransactionEntity` | `RecurringTransactionDao` | `RecurringRepository` | `TransactionFormViewModel` (modos), `RecurringListViewModel`, `HomeViewModel` | RecurringListScreen, TransactionFormScreen, tarjeta en Inicio | ✅ Completo |
| `TransferEntity` | `TransferDao` | `TransferRepository` | `TransferFormViewModel` (+ `TransactionsViewModel`, `HomeViewModel`) | TransferFormScreen (+ filas en Movimientos e Inicio) | ✅ Completo |

### Entidades secundarias (DAO existe, sin Repo/ViewModel/Screen)

| Entity | DAO | Repo | ViewModel | Screen | Estado |
|--------|-----|------|-----------|--------|--------|
| `GoalEntity`, `GoalContributionEntity` | `GoalDao`, `GoalContributionDao` | ✅ `GoalRepository` | ✅ | ✅ | ✅ Completo |
| `TagEntity` | `TagDao` | ❌ | ❌ | ❌ | ⏸️ Descartado por ahora |
| `AttachmentEntity` | `AttachmentDao` | ✅ `AttachmentRepository` (`data/attachments/`) | ✅ `TransactionFormViewModel`, `PhotosSettingsViewModel` | ✅ En el formulario de movimiento y en Configuración | ✅ Completo |
| `BudgetCategoryEntity` | `BudgetCategoryDao` | ✅ (dentro de `BudgetRepositoryImpl`) | ✅ | ✅ | ✅ Completo |
| `TransactionTagEntity` | `TransactionTagDao` | ❌ | ❌ | ❌ | ⏸️ Descartado por ahora |

### Repositorios existentes (9)

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
| `RecurringRepository` | `data/repository/RecurringRepository.kt` | ✅ Completo |

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
- `parentCategoryId` (FK a sí misma) existe pero no se usa: subcategorías descartadas por ahora
- `CategoryType`: EXPENSE, INCOME

### BudgetEntity
```kotlin
id, userId, name, amountLimit: Long, // centavos
scope: BudgetScope, periodInterval, periodUnit: PeriodUnit,
startDate, endDate, createdAt, updatedAt, deletedAt
```
- `BudgetScope`: GLOBAL, CATEGORY
- `PeriodUnit`: DAY, WEEK, MONTH, YEAR + `periodInterval`. El formulario ofrece las 8 frecuencias de `RecurrenceFrequency` (diario a anual) en una lista desplegable e indica cuándo vuelve a empezar de cero
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
id, userId, accountId, categoryId, baseAmount (con signo), description, currency,
periodInterval, periodUnit, anchorDate, nextOccurrence, endDate, active,
notifyBeforeDays, snoozedUntil, createdAt, updatedAt, deletedAt
```
- **Nunca se registran solos.** Cuando vence `nextOccurrence` queda pendiente: Inicio muestra la tarjeta «Por registrar» con Registrar / Más tarde (`snoozedUntil` = mañana) / Omitir esta vez / Revisar (formulario prellenado, modo `OCCURRENCE`)
- Serie = `anchorDate + k·intervalo` a medianoche local (`Recurrence`), calculada siempre desde el ancla (un cargo del 31 vuelve al 31 tras febrero)
- Al crear con fecha pasada no se generan atrasados (`firstUpcoming`); al reanudar tampoco se piden los que vencieron en pausa
- `registerOccurrence` / `skipOccurrence` son atómicos y comprueban la ocurrencia esperada (un doble toque no registra dos veces)
- Frecuencias (`RecurrenceFrequency`, compartidas con presupuestos): diaria, semanal, cada 2 semanas, mensual, bimestral, trimestral, semestral y anual. En recurrentes se muestran como «Cada 3 meses»; en presupuestos como «Trimestral»
- Se crean desde el formulario de movimiento («Repetir») o desde Recurrentes (ícono en Movimientos); el mismo `TransactionFormViewModel` atiende `NEW`, `EDIT`, `OCCURRENCE`, `RECURRING_NEW`, `RECURRING_EDIT`
- `RecurringReminderWorker` (WorkManager, diario ~9:00, dependencias por `@EntryPoint`) solo **anuncia** con una notificación; requiere sesión recordada. Permiso `POST_NOTIFICATIONS` se pide al elegir una frecuencia
- `getDueRecurrences` del DAO queda sin uso (el aviso usa `getPending`)

### GoalEntity
```kotlin
id, userId, accountId?, name, targetAmount, currentAmount, currency?,
targetDate?, icon?, color?, createdAt, updatedAt?, deletedAt?
```

### GoalContributionEntity
```kotlin
id, goalId, amount (con signo), note?, date, createdAt
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
- **DatabaseModule**: Crea `BudgmentDatabase` singleton con `ALL_MIGRATIONS` (v1→v6), sin fallback destructivo; antes de migrar guarda una copia (`PreMigrationBackup`)
- **RepositoryModule**: Binds de los 8 repositorios
- **NetworkModule**: Retrofit + OkHttpClient → `ExchangeRateApi` (Frankfurter API, base URL: `https://api.frankfurter.app/`)
- **BackupManager** (`data/backup/`): `@Singleton` con `@Inject constructor`, sin módulo propio
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
- **Version:** 6 (v3: `transactions.categoryId` nullable para transferencias; v4: `description` y `snoozedUntil` en recurrentes + índices de FK; v5: tabla `notifications`; v6: `goals.currency` + tabla `goal_contributions`)
- **Migraciones:** cadena completa v1→v2→…→v6 en `data/local/Migrations.kt` (`ALL_MIGRATIONS`). 1→2 crea las tablas que agregó la v2 (la v1 solo tenía users/accounts/categories, reconstruida del historial de git); 2→3 reconstruye `transactions` para que `categoryId` admita null (crear nueva → copiar → borrar → renombrar, compatible con el SQLite de Android 7)
- **Sin fallback destructivo:** si faltara una migración, la app falla al abrir en vez de borrar datos (Room revierte la transacción)
- **Copia previa** (`data/local/PreMigrationBackup.kt`): si la BD en disco tiene versión menor que `DATABASE_VERSION`, se copian `.db`, `-wal` y `-shm` a `noBackupFilesDir/db-backups/budgment-v<N>-<fecha>.db` antes de abrirla con Room; se guardan las 3 más recientes. Aún no hay pantalla para restaurarlas
- **Verificación:** `python3 tools/verify_migrations.py .` crea una BD en cada versión (1…N-1) con datos, aplica el SQL de `Migrations.kt` y compara columnas, tipos, NOT NULL, PK, FKs e índices con el último esquema, además de comprobar que no se pierdan filas. Necesita `app/schemas/` (hoy en `.gitignore`)
- **Al cambiar el esquema:** subir `DATABASE_VERSION` (`BudgmentDatabase.kt`), compilar para exportar `N+1.json`, escribir `MIGRATION_N_N+1` con su SQL, agregarla a `ALL_MIGRATIONS` y correr `tools/verify_migrations.py`
- **13 entities** registradas en `@Database`
- **13 DAOs** provistos via `DatabaseModule`
- `exportSchema = true`, schema guardado en `schemas/`

### Navegación
- `BudgmentNavHost` con `NavHost` y `rememberNavController`
- BottomBar con 4 pestañas: Home, Accounts, Transactions, Budgets
- Estado conservado entre pestañas (`saveState = true`, `restoreState = true`)
- Rutas: login, register, home, settings (desde el menú de perfil), accounts, account_form, categories, category_form, transactions, transaction_form (con `recurringId` y `mode`), transfer_form, recurring, budgets, budget_form
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
- `parentCategoryId` en el modelo, sin uso (subcategorías descartadas por ahora)

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
| ✅ ~~Adjuntos~~ | ✅ | ✅ | ✅ | ✅ | — | Hecho: ver «Fotos de recibos» abajo |

### B. Funciones sin ningún código

| Feature | Qué hace falta | Depende de |
|---------|----------------|------------|
| ✅ ~~Estadísticas~~ | Hecho: `ui/statistics/` (ver «Estadísticas» abajo). Pendiente: exportar reporte, filtrar por cuenta | — |
| ✅ ~~PIN / biometría~~ | Hecho: ver «Bloqueo de la app» abajo | — |
| ⏸️ Cifrar la base de datos | Descartado por ahora: ver «Descartado por ahora: cifrar la base de datos» | — |
| ✅ ~~Respaldo a archivo~~ | Hecho: ver «Respaldo a archivo» abajo. Con el selector del sistema el archivo se puede guardar en Drive sin API ni credenciales | — |
| Respaldo automático en Google Drive | Drive REST API + Worker programado, reutilizando el formato de «Respaldo a archivo». Necesita un proyecto de Google Cloud (cliente OAuth con el SHA-1 de la firma) | Respaldo a archivo ✅ |
| ✅ ~~Notificaciones~~ | Hecho: bandeja, ajustes, recurrentes y alertas de presupuesto (ver «Notificaciones» abajo) | — |

### C. Funciones parciales

| Feature | Estado | Falta |
|---------|--------|-------|
| ✅ ~~Configuración~~ | Moneda por defecto, apariencia, seguridad, fotos de recibos, respaldo, exportar CSV y cuenta (ver «Configuración» abajo) | Editar nombre, usuario o correo |
| Movimientos | 🟡 Lista por día con búsqueda y filtros | Filtro por etiqueta (cuando existan) |

### D. Deuda técnica

| Tarea | Ubicación | Prioridad |
|-------|-----------|-----------|
| ✅ ~~Typo `imzpl` → `impl`~~ | `di/RepositoryModule.kt:33` | Hecho |
| ✅ ~~Quitar `fallbackToDestructiveMigration`~~ | Cadena completa v1→v6 + copia previa + `tools/verify_migrations.py` | Hecho |
| ✅ ~~Versionar `app/schemas/`~~ | Versionados desde `38db1c3` (el 1.json no existe; el verificador lo reconstruye del 2) | Hecho |
| Restaurar una copia previa a la migración o a una restauración | `noBackupFilesDir/db-backups`; sin UI | Pendiente |
| Capa `domain/` (casos de uso) | No existe, aunque el README la menciona | Opcional; o quitarla del README |
| Tests | `AmountParserTest`, `BuildTransactionDetailsTest`, `BudgetPeriodTest`, `RecurrenceTest`, `BuildRecurringItemsTest`, `ComputeStatisticsTest`, `GroupByDayTest`, `BudgetAlertsTest`, `GoalProgressTest`, `BuildGoalItemsTest`, `TransactionFilterTest`, `AppLockPolicyTest`, `BackupCryptoTest`, `AttachmentPolicyTest` y `TransactionsCsvTest`; instrumentado: `BackupRoundTripTest`; falta test de migraciones (`MigrationTestHelper`) | Repos (balance, presupuestos), `BudgetOverviewSource`, DAOs (instrumentados) |
| ✅ ~~`backup_rules.xml` / `data_extraction_rules.xml`~~ | `res/xml/` | Hecho: respaldo automático de Android configurado |
| Actualizar README (fase actual, características) | `README.md` | Al cerrar cada fase |

### Orden sugerido

1. ✅ ~~Deuda trivial: typo + este documento al día.~~
2. ✅ ~~**Transferencias**~~.
3. ✅ ~~**Renovación de periodos de presupuesto**~~ (calculada al leer, sin WorkManager). ✅ ~~**WorkManager** como base → **recurrentes**~~ (con confirmación manual).
4. ✅ ~~**Estadísticas**~~.
5. ✅ ~~**Notificaciones**~~ (bandeja, ajustes, recurrentes y presupuestos).
6. ✅ ~~**Metas**~~, ✅ ~~filtros de movimientos~~. ⏸️ Etiquetas y subcategorías descartadas por ahora (ver «Descartado por ahora»).
7. ✅ ~~**Migraciones reales**~~ → ✅ ~~**PIN/biometría**~~ → ✅ ~~**respaldo a archivo**~~.
8. ✅ ~~**Adjuntos**~~ (fotos de recibos, incluidas en el respaldo a archivo). Opcional: respaldo automático en Drive.
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
- ✅ Quitado por completo: hay migración para cada versión desde la v1, verificada con `tools/verify_migrations.py`
- Cualquier cambio de esquema necesita su `Migration`; sin ella la app falla al abrir en vez de borrar los datos, y la copia previa queda intacta

### ⏸️ Descartado por ahora: etiquetas y subcategorías
- **Etiquetas** (`TagEntity`, `TransactionTagEntity`): su caso principal (agrupar gastos de distintas categorías, p. ej. un viaje) ya lo cubren la nota + la búsqueda con rango de fechas, que además da el total
- **Subcategorías** (`CategoryEntity.parentCategoryId`): mucha complejidad (selector en dos niveles, cómo suman en presupuestos y estadísticas, borrar un padre) para poco beneficio; basta con crear categorías más específicas
- Las tablas y el campo se quedan sin uso: quitarlos exigiría otra migración sin ganar nada. Retomar solo si hay una necesidad concreta

### ⏸️ Descartado por ahora: cifrar la base de datos (2026-11-16)
- **Por qué no:** la clave tendría que poder usarse sin el usuario presente (`RecurringReminderWorker` abre la BD en segundo plano, el PIN es opcional y `users` vive en la misma BD), así que solo podría custodiarla el Android Keystore. Eso protege únicamente si alguien obtiene el archivo sin poder ejecutar nada en el teléfono; contra root o el teléfono desbloqueado no añade nada a lo que ya dan el cifrado del almacenamiento de Android, el aislamiento entre apps y el bloqueo de la app
- **Qué costaría:** una forma nueva de perder todos los datos (clave del Keystore perdida, error al convertir la BD), un `BackupAgent` para que la clave viaje en el respaldo automático (solo se prueba a mano con `bmgr`), ~5 MB más de APK por arquitectura y perder el Database Inspector
- **En su lugar:** se cifra lo que sale del teléfono (ver «Respaldo a archivo»)
- **Retomar si:** la clave puede depender de un secreto del usuario (PIN obligatorio y avisos en segundo plano rediseñados) o hay un requisito externo de «base de datos cifrada». El diseño evaluado: SQLCipher + clave aleatoria envuelta por el Keystore en `noBackupFilesDir`, conversión de la BD con `sqlcipher_export` y clave escrita en claro solo durante el respaldo de Android

### Respaldo a archivo
- En Configuración → Respaldo (`ui/settings/BackupSection.kt`, `BackupViewModel`). «Crear respaldo»: pide una contraseña (mínimo 8 caracteres, dos veces) y abre el selector del sistema (`CreateDocument`) para elegir dónde guardar `Budgment <fecha>.budgment`: teléfono, Drive, USB… «Restaurar un respaldo»: selector (`OpenDocument`), contraseña, confirmación con fecha y cuántas cuentas y movimientos trae, y reinicio
- «Restaurar un respaldo» también está en la pantalla de inicio de sesión (`RestoreBackupButton`), para un teléfono nuevo donde aún no hay ninguna cuenta. Ahí no se espera al desbloqueo: sin sesión no hay pantalla de bloqueo
- **Qué contiene:** la base de datos completa, con **todos** los usuarios del teléfono, y las fotos de recibos si se deja marcada la casilla «Incluir las fotos de recibos (tamaño)» del diálogo (solo aparece si hay fotos). No incluye preferencias (DataStore) ni el PIN
- **Formato** (`data/backup/BackupCrypto.kt`, puro, con tests): cabecera sin cifrar de 48 bytes (`BDGMBKUP`, versión del formato, fecha, versión del esquema, iteraciones, sal, prefijo del nonce) + contenido cifrado con AES-256-GCM en bloques de 64 KiB. El contenido depende de la versión del formato: **1** = BD comprimida con gzip (se sigue pudiendo restaurar); **2** (actual) = ZIP con `budgment.db` y `attachments/<id>.jpg` (las fotos sin recomprimir). Al leer el ZIP el nombre de cada foto se valida entero. Clave: PBKDF2-HMAC-SHA256, 600 000 iteraciones, sal nueva por archivo. El número de bloque y la marca de «último» van en el nonce y la cabecera es dato autenticado de cada bloque: no se puede alterar, reordenar ni cortar sin que falle. Fallo en el primer bloque = contraseña incorrecta; en otro = archivo dañado
- **Copia coherente** (`DatabaseSnapshot.create`): abre una BD nueva, le adjunta la que está en uso y copia tablas e índices en una transacción (Android 7 no tiene `VACUUM INTO`). No depende de cerrar Room ni de vaciar el -wal
- **Restaurar en dos tiempos** (`PendingRestore`): con la app abierta solo se descifra a caché, se valida (`DatabaseSnapshot.inspect`: versión ≤ actual, `integrity_check`, tablas mínimas) y se deja como `budgment.db.restore`, con las fotos en `filesDir/attachments.restore` (la carpeta se crea siempre, aunque el respaldo no traiga fotos: las actuales pertenecen a movimientos que dejan de existir); se cierra la sesión y se reinicia el proceso. El reemplazo ocurre en `DatabaseModule` antes de abrir Room: copia de la BD actual a `db-backups` (`budgment-antes-de-restaurar-<fecha>.db`), borrar -wal/-shm, cambiar la carpeta de fotos y renombrar la BD al final. De las fotos anteriores no se guarda copia. Si el respaldo es de un esquema anterior, las migraciones lo ponen al día
- `BackupManager` usa su propio `CoroutineScope`: cerrar la sesión destruye la pantalla de Configuración antes de terminar
- **Bloqueo de la app:** el selector de archivos manda la app al fondo y al volver puede pedir el PIN; los diálogos del respaldo esperan a que se desbloquee (`BackupViewModel.unlocked`)
- **Olvidar la contraseña = respaldo perdido.** No hay recuperación, y así se le dice al usuario
- `Pbkdf2` (`data/local/security/`): usa el del sistema y, en Android 7 (API 24–25, donde `PBKDF2WithHmacSHA256` no existe), uno propio con el mismo resultado. `PasswordHasher` también lo usa: antes el inicio de sesión fallaba en esas versiones
- **Pendiente:** respaldo automático programado, pantalla para recuperar las copias de `db-backups`
- **Probar:** `./gradlew connectedDebugAndroidTest` corre `BackupRoundTripTest` con bases de nombre propio. ⚠️ Al terminar, Gradle puede desinstalar la app del dispositivo: usar un emulador o un teléfono sin datos reales

### Configuración
- Secciones, en orden: moneda por defecto, apariencia, seguridad, fotos de recibos, respaldo, exportar movimientos y cuenta. Cada una es un composable con su ViewModel (`ui/settings/`); `SettingsScreenContent` las recibe como huecos
- **Apariencia** (`ThemeMode` en `UiPreferences`, del dispositivo): igual que el teléfono, claro u oscuro. `MainActivity` lo lee antes del primer cuadro (`runBlocking`, una lectura corta de DataStore) para que no parpadee, y lo pasa a `BudgmentTheme`. `LocalDarkTheme` dice si la app se muestra en oscuro; `SystemBars.kt` lo usa en vez de `isSystemInDarkTheme()`. La pantalla de arranque del sistema sigue el tema del teléfono
- **Exportar movimientos** (`TransactionsCsv.kt`, puro, con tests; `ExportViewModel`): CSV del usuario actual con los mismos datos que la lista de Movimientos (incluye el historial de cuentas y categorías eliminadas). Columnas: fecha (`yyyy-MM-dd`), tipo, categoría, cuenta, cuenta destino, monto con signo y punto decimal, moneda y nota; una transferencia es una fila. Lleva BOM para Excel, y un texto que empiece como fórmula (`=`, `+`, `-`, `@`) se antecede de un apóstrofo. No está cifrado: la pantalla lo avisa
- **Cambiar contraseña** (`AuthRepository.changePassword`): pide la actual. No hay longitud mínima, igual que en el registro. El PIN no cambia
- **Eliminar mi cuenta** (`AuthRepository.deleteUser`): pide la contraseña y no se puede deshacer. Borra las fotos del usuario y luego `UserDao.deleteWithData`: transferencias, movimientos y recurrentes primero (sus claves foráneas son `RESTRICT` hacia cuentas y categorías) y después el usuario, que arrastra lo demás en cascada. Los otros usuarios no se tocan. Al terminar se cierra la sesión. El PIN y las preferencias del dispositivo se quedan

### Fotos de recibos (adjuntos)
- **Dónde:** en el formulario de movimiento (nuevo, editar y confirmar una ocurrencia; no en la plantilla de un recurrente ni en transferencias), debajo de la nota: miniaturas, «Tomar foto» y visor a pantalla completa con zoom (`ui/attachments/ReceiptPhotos.kt`). En la lista de Movimientos, el menú ⋮ de una fila con fotos ofrece «Ver fotos del recibo» (visor de solo lectura, `ReceiptPhotoViewer`); la fila en sí no lleva ningún indicador. Quitar una foto desde el formulario pide confirmación
- **Solo desde la cámara** (`ActivityResultContracts.TakePicture` + `FileProvider`, autoridad `<applicationId>.files`, `res/xml/file_paths.xml`): la app de cámara del teléfono deja la foto en `cacheDir/captures/`. No se pide el permiso `CAMERA` (no hay que declararlo: si se declarara, habría que pedirlo) y no se puede elegir de la galería
- **Para que ocupen poco** (`AttachmentPolicy`, puro, con tests): lado mayor de 1600 px, JPEG al 80 %, giro EXIF aplicado; máximo **3 por movimiento**
- **Archivos** (`AttachmentStore`): definitivos en `filesDir/attachments/<id>.jpg`; `AttachmentEntity.filePath` guarda solo el nombre, para que un respaldo sirva en otro teléfono
- **Se aplican al guardar:** las fotos nuevas viven en caché y las quitadas no se borran hasta guardar el movimiento (`AttachmentRepository.applyChanges`); salir sin guardar no cambia nada. Para conocer el id de un movimiento nuevo, `createTransaction`, `createTransactionAndRecurring` y `registerOccurrence` aceptan un `transactionId`
- **Si el sistema cierra la app con la cámara abierta:** las fotos ya tomadas se recuperan (`SavedStateHandle`), pero el resto del formulario no se guarda ahí y se pierde, igual que antes
- **Borrado periódico** (`AttachmentRepository.cleanUp`): corre en la revisión diaria (`RecurringReminderWorker`, aunque no haya sesión), al abrir la app y al cambiar el plazo. Borra las fotos que cumplieron el plazo (meses de calendario desde que se tomaron), las de movimientos eliminados hace más de un día, los registros sin archivo y los archivos sin registro. El movimiento nunca se toca
- **Plazo** (Configuración → Fotos de recibos, `AttachmentSettings` en DataStore): 3 meses (por defecto), 6 meses, 1 año o siempre. Si el plazo elegido borraría fotos en el acto, se pide confirmación con el número. Ahí mismo se ve cuántas fotos hay y cuánto ocupan; el formulario avisa del plazo junto a las fotos
- **Respaldos:** van en el respaldo a archivo (opcional). **No** van en el respaldo automático de Android: su límite es de 25 MB y, si se supera, falla el respaldo entero; tras una restauración automática los registros sin archivo se limpian solos

### WorkManager
- `RecurringReminderWorker` (aviso diario de recurrentes pendientes), programado en `BudgmentApp.onCreate` con `KEEP`
- Sin `HiltWorkerFactory`: el worker obtiene dependencias con `EntryPointAccessors`
- También revisa las alertas de presupuesto (`BudgetAlertChecker`)

### Búsqueda y filtros de Movimientos
- Barra fija sobre la lista (`TransactionFilterBar.kt`): campo de búsqueda + chips Tipo, Cuenta, Categoría y Fecha (cada uno abre una hoja; se aplica al momento) + «Limpiar filtros»
- `applyFilter` (puro, `TransactionFilter.kt`): dentro de un grupo suma (cualquiera de las cuentas elegidas), entre grupos se combina. Una transferencia aparece en su cuenta de origen y de destino (`TransferDetails.toAccountId`); filtrar por categoría la oculta
- Búsqueda sin acentos ni mayúsculas, por palabras, en nota, categoría, cuentas y monto («45.50», «25,000», «8000»)
- Fecha: este mes, mes pasado, últimos 30 días, este año o rango propio (`DateRangePicker`, último día incluido)
- Resumen bajo los chips: cuántos y, si todo está en una moneda, ingresos y gastos (`summarize`). Sin resultados → estado vacío con «Limpiar filtros»
- Opciones: cuentas y categorías activas, más las eliminadas que tengan movimientos (marcadas «(eliminada)»)
- El filtro vive en `TransactionsViewModel`: se conserva al cambiar de pestaña, no al cerrar la app

### Metas de ahorro
- Sección desplegable en Inicio, antes de «Últimos movimientos» (`ui/home/GoalsSection.kt`); el estado plegado/desplegado se guarda en `UiPreferences` (DataStore). Botón + para crear; cada tarjeta abre el detalle
- Dos modos, elegidos al crear y cambiables al editar:
  - **En una cuenta** (`accountId`): el avance es el `balanceCache` de la cuenta, en su moneda. Si la cuenta se elimina, se avisa y se pide editar la meta. El detalle ofrece «Transferir a esta cuenta» (ruta `transfer_form?toAccountId=`)
  - **Aportes manuales** (`accountId == null`): `goal_contributions` (aporte +, retiro −) y `GoalEntity.currentAmount` como suma cacheada, actualizada en la misma transacción. No mueven dinero de las cuentas. No se puede retirar más de lo ahorrado; borrar un aporte tiene deshacer. Lo «ya ahorrado» al crear se guarda como primer aporte
- `GoalEntity.currency` (nuevo en v6): moneda de la meta; en las vinculadas, la de la cuenta. La moneda de una meta manual no se puede cambiar si ya tiene aportes
- `goalProgress` (puro, `data/model/GoalProgress.kt`): estado alcanzada / a tiempo / vencida / sin fecha; con fecha, aporte mensual = lo que falta ÷ meses restantes (mes parcial cuenta entero, redondeo hacia arriba)
- `MIGRATION_5_6`: `ALTER TABLE goals ADD COLUMN currency` + tabla `goal_contributions`
- Eliminar una meta: confirmación y borrado lógico (`deletedAt`)

### Notificaciones
- Acceso desde el menú de perfil (junto a Estadísticas y Configuración); ruta `notifications`. Punto en el ícono de perfil y contador en el menú si hay avisos sin leer
- Bandeja persistente: `NotificationEntity` (tabla `notifications`, BD v5, `MIGRATION_4_5`), `NotificationDao`, `NotificationRepository`. Texto fijado al crear; `dedupKey` único por usuario evita duplicados (recurrentes: `recurring:<id>:<ocurrencia>`); se borran solos a los 90 días
- `syncRecurring` llena la bandeja con los recurrentes vencidos no pospuestos; lo llaman el worker diario y `HeaderViewModel` (al cambiar recurrentes y cada día)
- Al abrir la bandeja todo se marca leído, pero lo nuevo conserva su punto mientras se ve. Recurrente aún pendiente → botón Registrar; ya registrado/omitido/pausado → «Ya no está pendiente». Deslizar para borrar con deshacer
- Ajustes (hoja con ⚙): qué tipos llegan como notificación del teléfono (`NotificationSettings` en DataStore, no se borra al cerrar sesión) y hora del recordatorio (6:00–22:00; reprograma el worker con `CANCEL_AND_REENQUEUE`). Aviso si el sistema tiene las notificaciones desactivadas, con botón que pide permiso o abre los ajustes del sistema
- La bandeja registra todo aunque el tipo esté desactivado: los interruptores solo controlan la notificación del teléfono
- Alertas de presupuesto: `budgetAlertLevel` (puro, `data/model/BudgetAlerts.kt`) da el umbral más alto alcanzado: el porcentaje elegido (50–95 %, por defecto 80, uno para todos los presupuestos, en la hoja de ajustes) o el límite (100 %). Si un gasto salta directo al límite, solo llega ese aviso
- `notifications/BudgetAlertChecker.kt`: calcula lo gastado en el periodo vigente en la moneda por defecto (la de los límites); `dedupKey` = `budget:<id>:<inicio del periodo>:<nivel>`, así cada umbral avisa una vez por periodo y vuelve a avisar al renovarse. Lo llaman `HeaderViewModel` (al cambiar movimientos, presupuestos o preferencias, y cada día) y el worker diario
- Canal `budgets`; una notificación por presupuesto (la de límite reemplaza a la previa); ícono `ic_notification_budget`
- `BudgetEntity.notifyThresholdPercent` sigue sin usarse: el porcentaje es global por decisión de producto

### Estadísticas
- Acceso desde el menú de perfil (junto a Configuración); ruta `statistics`
- `StatisticsCalculator.kt` (puro, con tests): `StatsRange` mes/año con desplazamiento, `computeStatistics` → ingresos, gastos, comparación con el periodo anterior, gasto por categoría, flujos por mes (6 meses o los 12 del año) y saldo al cierre de cada mes
- Las transferencias (`categoryId == null`) no cuentan como ingreso ni gasto, pero sí en el saldo
- Saldo histórico = `balanceCache` actual − movimientos posteriores; solo cuentas existentes en esa fecha; tipo de cambio de hoy
- Todo en la moneda de visualización (`SessionManager.displayCurrency`); si falta un tipo de cambio, el monto se omite y se avisa
- Gráficas propias con `Canvas` (`StatisticsCharts.kt`), sin librería externa; cifras también en texto y `contentDescription`

### Drawables sin uso
- (`logo_mark.png` ya se usa en Login y en la barra superior; los strings sin uso se eliminaron en `5d5e54f`)

### Bloqueo de la app (PIN, huella o rostro)
- Opcional, en Configuración → Seguridad. Es del dispositivo: no se borra al cerrar sesión
- **PIN** de 4 a 6 dígitos, guardado como hash PBKDF2 con sal (`PasswordHasher`) en el DataStore de sesión (`AppLockSettings`). Cambiarlo o quitarlo pide el PIN actual
- **Intentos:** 5 libres; después 30 s de espera que se duplica con cada fallo hasta 15 min (`AppLockPolicy`). El contador sobrevive a cerrar la app
- **Huella o rostro** (`androidx.biometric` 1.1.0, clase débil incluida): solo Android 9+, porque antes la librería usa un diálogo AppCompat y el tema no lo es. Se confirma con el sensor al activarlo; se abre sola al volver a la app y hay botón para reintentar
- **Cuándo se bloquea** (`AppLockManager`): al abrir la app en frío y al volver de segundo plano tras el tiempo elegido (al salir, 30 s, 1 min, 5 min), medido con `elapsedRealtime`. Girar la pantalla no cuenta. Tras iniciar sesión con contraseña no se pide el PIN
- **Pantalla:** `LockScreen` se dibuja encima de la navegación (`BudgmentRoot` → `LockGate`), que sigue compuesta debajo para no perder lo que se estaba haciendo; queda oculta a los lectores de pantalla. Atrás manda la app al fondo
- **¿Olvidaste tu PIN?:** con la contraseña de la cuenta se desbloquea y se quita el PIN
- **Apps recientes:** con el bloqueo activo la miniatura no se muestra (`setRecentsScreenshotEnabled`, Android 13+). No se usa `FLAG_SECURE` para no bloquear capturas
- `MainActivity` es `FragmentActivity` (lo exige `BiometricPrompt`)
- **Límite:** protege la interfaz, no cifra la BD. Quien saque el archivo del teléfono (root, ADB con depuración) puede leerlo. Cifrar la BD se evaluó y se descartó por ahora (ver «Descartado por ahora: cifrar la base de datos»)

### Respaldo automático de Android (`backup_rules.xml` / `data_extraction_rules.xml`)
- Se respaldan `database/budgment.db` (con -wal/-shm) y `file/datastore/` (sesión recordada y preferencias). Todo lo demás queda fuera, incluidas las fotos de recibos (`file/attachments/`); `noBackupFilesDir` (WorkManager, copias previas a migrar) y el caché nunca se respaldan
- Nube: solo con cifrado de extremo a extremo (`disableIfNoEncryptionCapabilities` en API 31+, `requireFlags="clientSideEncryption"` en API ≤30). Sin bloqueo de pantalla no se sube nada
- Transferencia directa a un teléfono nuevo: mismas reglas, sin requisito de cifrado
- Cuándo: Android decide (aprox. una vez al día con el teléfono cargando, en wifi e inactivo); límite 25 MB. Restaura al reinstalar o al configurar otro teléfono con la misma cuenta de Google
- Una copia restaurada de una versión anterior del esquema la ponen al día las migraciones al abrir la app
- Probar a mano: `adb shell bmgr backupnow com.nintech.budgment`, desinstalar, reinstalar desde Android Studio y luego `adb shell bmgr restore <token> com.nintech.budgment` (el token sale de `adb shell bmgr list sets`), o reinstalar desde Play Store
- No sustituye el respaldo en Drive desde la app: el usuario no controla ni ve cuándo se hace

### Cálculo de balance
- `AccountRepository.adjustBalance()` ajusta `balanceCache` directamente
- `TransactionRepository` llama a `adjustBalance` al crear/actualizar/eliminar
- El balance se calcula solo para cuentas, no para presupuestos o metas

### Category subcategorías
- `CategoryEntity` tiene `parentCategoryId: String?` con FK self-referencing
- `CategoryDao` tiene query para `observeCategories(userId)`
- **UI no expone** la creación de subcategorías (descartado por ahora; ver «Descartado por ahora»)

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
- `AmountParserTest`, `BuildTransactionDetailsTest`, `BudgetPeriodTest` y `RecurrenceTest` (unitarios). Sin tests de repos, ViewModels ni DAOs.

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
