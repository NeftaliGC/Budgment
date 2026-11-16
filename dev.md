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
| `AttachmentEntity` | `AttachmentDao` | ❌ | ❌ | ❌ | 🔴 Sin implementar |
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
| Adjuntos | ✅ `AttachmentEntity`, `AttachmentDao` | ❌ | ❌ | ❌ | — | Foto de recibo; almacenamiento en `filesDir`, incluir en respaldo |

### B. Funciones sin ningún código

| Feature | Qué hace falta | Depende de |
|---------|----------------|------------|
| ✅ ~~Estadísticas~~ | Hecho: `ui/statistics/` (ver «Estadísticas» abajo). Pendiente: exportar reporte, filtrar por cuenta | — |
| PIN / biometría | `BiometricPrompt` + PIN de respaldo, bloqueo al volver de segundo plano, opción en Configuración | Configuración |
| Respaldo en Google Drive | Exportar/importar BD (+ adjuntos), Drive REST API, Worker de respaldo automático | WorkManager, migraciones reales |
| ✅ ~~Notificaciones~~ | Hecho: bandeja, ajustes, recurrentes y alertas de presupuesto (ver «Notificaciones» abajo) | — |

### C. Funciones parciales

| Feature | Estado | Falta |
|---------|--------|-------|
| Configuración | 🟡 Solo moneda por defecto | Cambio de contraseña, borrar usuario, tema, seguridad (PIN), respaldo, exportar CSV |
| Movimientos | 🟡 Lista por día con búsqueda y filtros | Filtro por etiqueta (cuando existan) |

### D. Deuda técnica

| Tarea | Ubicación | Prioridad |
|-------|-----------|-----------|
| ✅ ~~Typo `imzpl` → `impl`~~ | `di/RepositoryModule.kt:33` | Hecho |
| ✅ ~~Quitar `fallbackToDestructiveMigration`~~ | Cadena completa v1→v6 + copia previa + `tools/verify_migrations.py` | Hecho |
| Versionar `app/schemas/` | Hoy en `.gitignore`: sin los JSON no se pueden verificar migraciones en otro equipo | Recomendado |
| Restaurar una copia previa a la migración | `noBackupFilesDir/db-backups`; sin UI | Con el respaldo en Drive |
| Capa `domain/` (casos de uso) | No existe, aunque el README la menciona | Opcional; o quitarla del README |
| Tests | `AmountParserTest`, `BuildTransactionDetailsTest`, `BudgetPeriodTest`, `RecurrenceTest`, `BuildRecurringItemsTest`, `ComputeStatisticsTest`, `GroupByDayTest`, `BudgetAlertsTest`, `GoalProgressTest`, `BuildGoalItemsTest` y `TransactionFilterTest`; falta test de migraciones (`MigrationTestHelper`) | Repos (balance, presupuestos), `BudgetOverviewSource`, DAOs (instrumentados) |
| ✅ ~~`backup_rules.xml` / `data_extraction_rules.xml`~~ | `res/xml/` | Hecho: respaldo automático de Android configurado |
| Actualizar README (fase actual, características) | `README.md` | Al cerrar cada fase |

### Orden sugerido

1. ✅ ~~Deuda trivial: typo + este documento al día.~~
2. ✅ ~~**Transferencias**~~.
3. ✅ ~~**Renovación de periodos de presupuesto**~~ (calculada al leer, sin WorkManager). ✅ ~~**WorkManager** como base → **recurrentes**~~ (con confirmación manual).
4. ✅ ~~**Estadísticas**~~.
5. ✅ ~~**Notificaciones**~~ (bandeja, ajustes, recurrentes y presupuestos).
6. ✅ ~~**Metas**~~, ✅ ~~filtros de movimientos~~. ⏸️ Etiquetas y subcategorías descartadas por ahora (ver «Descartado por ahora»).
7. ✅ ~~**Migraciones reales**~~ → **respaldo en Drive** → **PIN/biometría**.
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
- ✅ Quitado por completo: hay migración para cada versión desde la v1, verificada con `tools/verify_migrations.py`
- Cualquier cambio de esquema necesita su `Migration`; sin ella la app falla al abrir en vez de borrar los datos, y la copia previa queda intacta

### ⏸️ Descartado por ahora: etiquetas y subcategorías
- **Etiquetas** (`TagEntity`, `TransactionTagEntity`): su caso principal (agrupar gastos de distintas categorías, p. ej. un viaje) ya lo cubren la nota + la búsqueda con rango de fechas, que además da el total
- **Subcategorías** (`CategoryEntity.parentCategoryId`): mucha complejidad (selector en dos niveles, cómo suman en presupuestos y estadísticas, borrar un padre) para poco beneficio; basta con crear categorías más específicas
- Las tablas y el campo se quedan sin uso: quitarlos exigiría otra migración sin ganar nada. Retomar solo si hay una necesidad concreta

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

### Respaldo automático de Android (`backup_rules.xml` / `data_extraction_rules.xml`)
- Se respaldan `database/budgment.db` (con -wal/-shm) y `file/datastore/` (sesión recordada y preferencias). Todo lo demás queda fuera; `noBackupFilesDir` (WorkManager, copias previas a migrar) y el caché nunca se respaldan
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
