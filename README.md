# Budgment

Aplicación móvil Android para control de gastos y presupuestos personales. Los datos viven localmente en el dispositivo (SQLite) y pueden respaldarse en Google Drive, sin depender de un backend centralizado.

## ✨ Características

### Implementadas / en desarrollo actual
- [ ] Bloqueo de la app con PIN
- [ ] Gestión de cuentas (efectivo, débito, crédito, ahorro, inversión)
- [ ] Categorías de gasto/ingreso con subcategorías
- [ ] Registro, edición y borrado de transacciones

### Planeadas (ver [Roadmap](#-roadmap))
- [ ] Presupuestos con seguimiento de progreso y alertas
- [ ] Reportes y gráficas por categoría/periodo
- [ ] Transferencias entre cuentas propias
- [ ] Transacciones recurrentes automáticas
- [ ] Metas de ahorro
- [ ] Etiquetas y adjuntos (fotos de recibos)
- [ ] Soporte multi-moneda
- [ ] Respaldo y restauración cifrados vía Google Drive
- [ ] Bloqueo con biometría (huella/rostro)

## 🛠️ Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Arquitectura:** MVVM
- **Inyección de dependencias:** Hilt
- **Persistencia:** Room (sobre SQLite)
- **Navegación:** Navigation Compose
- **Tareas en segundo plano:** WorkManager
- **Respaldo en la nube:** Google Drive REST API v3 (appDataFolder)

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

> Estado actual: fase 1-2 (fundamentos y modelo de datos), proyecto iniciado en Android Studio.

## 🚀 Cómo correr el proyecto

### Requisitos
- Android Studio (versión estable más reciente)
- JDK 17
- SDK de Android — minSdk 24 / targetSdk según `build.gradle`
- Cuenta de Google (para probar la integración de respaldo en Drive, cuando esté implementada)

### Pasos
1. Clona el repositorio:
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   ```
2. Ábrelo en Android Studio y espera a que sincronice Gradle.
3. Ejecuta la app en un emulador o dispositivo físico con el botón **Run**.

## 📁 Estructura del proyecto

```
app/
 ├─ data/          # Entities, DAOs, Database (Room)
 ├─ domain/        # Modelos de dominio y casos de uso
 ├─ ui/            # Pantallas y componentes de Compose
 └─ di/            # Módulos de Hilt
docs/
 └─ budgment_schema.dbml
```

*(Ajusta esta sección conforme evolucione la estructura real del proyecto.)*

## 🤝 Contribuciones

Por ahora este es un proyecto personal en desarrollo activo. Si en el futuro se abre a contribuciones externas, esta sección se actualizará con la guía correspondiente.

## 📄 Licencia

Licencia por definir.

## 👤 Autor

Fabian