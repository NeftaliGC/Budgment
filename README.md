# Budgment

Aplicación móvil Android para control de gastos y presupuestos personales. Los datos viven localmente en el dispositivo (SQLite) y pueden respaldarse en Google Drive, sin depender de un backend centralizado.

## ✨ Características

- **Autenticación** — Registro e inicio de sesión con contraseña hasheada (PBKDF2)
- **Gestión de cuentas** — Efectivo, débito, crédito, ahorro e inversión con campos específicos (límite de crédito, días de corte/pago)
- **Categorías** — Gasto e ingreso con iconos, colores y subcategorías
- **Transacciones** — Registro, edición y eliminación de movimientos
- **Presupuestos** — Seguimiento de progreso con alcance global o por categoría
- **Tasas de cambio** — Conversión automática entre 7 monedas (MXN, USD, EUR, CAD, GBP, JPY, CHF)
- **Multi-moneda** — Selector de moneda de visualización en toda la app
- **Navegación intuitiva** — BottomBar con pestañas y conservación de estado

## 🌐 Web

Sitio del proyecto y política de privacidad: <https://neftaligc.github.io/Budgment/> (publicado con GitHub Pages desde la carpeta [`docs/`](docs/)).

## 🛠️ Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** MVVM
- **Inyección de dependencias:** Hilt
- **Persistencia:** Room (sobre SQLite)
- **Navegación:** Navigation Compose
- **Tasas de cambio:** Frankfurter API (Retrofit)
- **Tareas en segundo plano:** WorkManager

## 📐 Modelo de datos

El esquema completo de la base de datos está documentado en [`docs/budgment_schema.dbml`](docs/budgment_schema.dbml) (formato [DBML](https://dbml.dbdiagram.io/)). Incluye: usuarios, cuentas, categorías, transacciones, transferencias, presupuestos, transacciones recurrentes, metas, etiquetas, adjuntos e historial de tasas de cambio.

## 🗺️ Roadmap

1. Fundamentos técnicos y arquitectura
2. Modelo de datos y capa de persistencia (Room)
3. MVP: usuario, cuentas, categorías y transacciones
4. Presupuestos y reportes
5. Transferencias, recurrencias y metas
6. Seguridad y respaldo en Google Drive
7. Extras: tags, adjuntos y multi-moneda
8. Pulido, pruebas y publicación

> Estado actual: fase 3-4 (MVP completado, presupuestos implementados).

## 🚀 Cómo correr el proyecto

### Requisitos
- Android Studio (versión estable más reciente)
- JDK 17
- SDK de Android — minSdk 24 / targetSdk 37

### Pasos
1. Clona el repositorio:
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   ```
2. Ábrelo en Android Studio y espera a que sincronice Gradle.
3. Ejecuta la app en un emulador o dispositivo físico con el botón **Run**.

> **Nota:** La base de datos usa `fallbackToDestructiveMigration(dropAllTables = true)` durante el desarrollo. Al cambiar el esquema se borrará y recreará la BD. Quitar antes de publicar.

## 📁 Estructura del proyecto

```
app/
  ├─ data/          # Entities, DAOs, Database (Room), repositorios, seguridad
  ├─ di/            # Módulos de Hilt (Database, Repository, Network, Session)
  ├─ domain/        # Modelos de dominio y casos de uso
  ├─ ui/            # Pantallas y componentes de Compose
  └─ MainActivity.kt, BudgmentApp.kt
docs/                          # Sitio de GitHub Pages (Jekyll)
  ├─ index.html                # Portada
  ├─ politica-de-privacidad.md # Política de privacidad → /privacidad/
  └─ budgment_schema.dbml
```

## 🤝 Contribuciones

Por ahora este es un proyecto personal en desarrollo activo. Si en el futuro se abre a contribuciones externas, esta sección se actualizará con la guía correspondiente.

## 📄 Licencia

Licencia por definir.

## 👤 Autor

Fabian
