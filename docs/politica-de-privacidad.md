# Política de privacidad de Budgment

Última actualización: 16 de noviembre de 2026

Budgment es una aplicación para Android de control de gastos y presupuestos personales. Esta política explica qué información maneja la app, dónde se guarda y qué sale de tu teléfono.

## En resumen

- Tus datos se guardan **en tu teléfono**. Budgment no tiene servidores propios y su desarrollador no recibe ni puede ver tu información.
- No hay publicidad, ni herramientas de analítica o seguimiento, ni venta o cesión de datos.
- Lo único que la app envía por internet es una consulta de tipos de cambio, que no contiene ningún dato tuyo.
- Las copias de seguridad las controlas tú o tu cuenta de Google, según se explica abajo.

## Responsable

**Fabián Neftaly Guía Cruz**
Contacto: **nintech1912@gmail.com**

## Qué información guarda la app

Todo lo siguiente lo escribes tú y se almacena únicamente en el almacenamiento privado de la app en tu teléfono:

- **Datos de tu usuario:** nombre, nombre de usuario y, si decides darlo, correo electrónico. Tu contraseña no se guarda: se guarda un valor derivado de ella (hash PBKDF2 con sal) que no permite recuperarla.
- **Información financiera que registras:** cuentas y sus saldos, categorías, movimientos, transferencias, movimientos recurrentes, presupuestos y metas de ahorro, con sus notas y fechas.
- **Fotos de recibos**, si decides tomarlas. Se guardan reducidas dentro de la app y se borran solas pasado el plazo que elijas en Configuración.
- **Preferencias:** moneda, tema, ajustes de notificaciones y, si activas el bloqueo, un hash de tu PIN.

El usuario y la contraseña de Budgment son locales: solo sirven para separar a las personas que usan la app en un mismo teléfono. No se crea ninguna cuenta en internet.

Budgment no se conecta con tu banco ni accede a tus cuentas bancarias reales, y no te pide credenciales bancarias.

## Qué sale de tu teléfono

### Tipos de cambio

Para convertir entre monedas, la app consulta el servicio público [Frankfurter](https://frankfurter.dev), que publica los tipos de cambio de referencia del Banco Central Europeo. La consulta solo incluye los códigos de las monedas (por ejemplo, USD, MXN, EUR). No incluye tu nombre, tus saldos, tus movimientos ni ningún identificador tuyo.

Como en cualquier conexión a internet, ese servicio puede ver la dirección IP desde la que se hace la consulta. El tratamiento que haga de ella depende de su propia política.

### Copia de seguridad de Android

Si tienes activada la copia de seguridad de Android en tu teléfono, el sistema puede guardar en **tu cuenta de Google** una copia de los datos de Budgment (la información que registraste y tus preferencias; las fotos de recibos no se incluyen). Esto sirve para recuperar tus datos al cambiar de teléfono o reinstalar la app.

- Esa copia la hace y la almacena Google, no el desarrollador de Budgment, y se rige por la política de privacidad de Google.
- Budgment solo permite la copia en la nube cuando el teléfono puede cifrarla de extremo a extremo, lo que requiere que tenga un bloqueo de pantalla.
- Puedes desactivarla en los ajustes de copia de seguridad de tu teléfono.

### Respaldos y exportaciones que tú creas

- **Respaldo a archivo:** desde Configuración puedes crear un archivo con todos los datos de la app, cifrado con una contraseña que tú eliges. El archivo se guarda donde tú indiques (tu teléfono, un servicio en la nube, etc.). Incluye la información de todos los usuarios registrados en ese teléfono.
- **Exportación a CSV:** puedes exportar tus movimientos a un archivo para hojas de cálculo. Este archivo **no está cifrado**.

En ambos casos el archivo solo se crea cuando tú lo pides y queda bajo tu control: Budgment no lo envía a ningún sitio. Lo que ocurra con él depende de dónde lo guardes y con quién lo compartas.

## Permisos

- **Internet:** únicamente para la consulta de tipos de cambio descrita arriba.
- **Notificaciones:** para avisarte de movimientos recurrentes por registrar y de presupuestos cerca de su límite. Las notificaciones se generan en tu teléfono.

Budgment **no** pide permiso de cámara: para la foto de un recibo abre la app de cámara de tu teléfono y recibe solo la foto que tomas en ese momento. Tampoco accede a tu galería, tus contactos ni tu ubicación.

Si usas huella o rostro para desbloquear la app, el reconocimiento lo hace el sistema Android. Budgment solo recibe la respuesta de si fue correcto y nunca tiene acceso a tus datos biométricos.

## Seguridad

- Los datos están en el almacenamiento privado de la app, protegido por el aislamiento entre aplicaciones y el cifrado del almacenamiento de Android.
- Puedes añadir un bloqueo con PIN, huella o rostro para abrir la app.
- Los respaldos a archivo van cifrados (AES-256) con tu contraseña. Si la olvidas, ese respaldo no se puede recuperar.

Ninguna medida es infalible: una persona con acceso completo a tu teléfono desbloqueado podría llegar a tus datos. Mantén tu teléfono protegido con un bloqueo de pantalla.

## Conservación y eliminación

Tú decides cuánto tiempo se conservan tus datos:

- **Eliminar tu cuenta:** en Configuración → Cuenta → «Eliminar mi cuenta» se borran de forma definitiva tu usuario y todo lo que registraste, incluidas las fotos.
- **Desinstalar la app** borra todos sus datos del teléfono.
- **Fotos de recibos:** se borran automáticamente pasado el plazo elegido (3 meses por defecto).
- **Copia de seguridad de Android:** se gestiona desde tu cuenta de Google.
- **Archivos de respaldo o CSV** que hayas creado: son tuyos; bórralos donde los hayas guardado.

Como el desarrollador no tiene copia de tus datos, no puede consultarlos, corregirlos ni borrarlos por ti: todo ello se hace desde la propia app.

## Menores de edad

Budgment no está dirigida a menores de 13 años y no recopila información de nadie, sea cual sea su edad, fuera de lo que cada persona escribe en su propio teléfono.

## Cambios en esta política

Si la app cambia de forma que afecte a lo descrito aquí (por ejemplo, si se añade una función que envíe datos a un servicio externo), esta política se actualizará antes de publicar ese cambio y se indicará la nueva fecha al inicio.

## Contacto

Para cualquier duda sobre esta política: **nintech1912@gmail.com**
