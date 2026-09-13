# Acceso del frontend a MS3

El backend permite CORS exclusivamente en `/api/carritos/**` para los métodos
GET, POST, PATCH, DELETE y OPTIONS y las cabeceras Content-Type y Accept.
Por defecto permite `http://localhost:5173` y `http://127.0.0.1:5173`.

Para otro puerto o el frontend desplegado, configura en el entorno **del backend**:

```dotenv
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://tu-frontend.example
```

Reemplaza el dominio de ejemplo por el origen real, sin ruta ni barra final.
La variable reemplaza la lista predeterminada. No se necesitan cookies ni
credenciales de MongoDB en el frontend para estas llamadas.

La corrección local requiere recompilar y desplegar MS3 para que afecte a AWS.
API Gateway debe enviar OPTIONS al backend cuando este gestione CORS. Si API
Gateway administra CORS, su configuración debe permitir los mismos orígenes,
métodos y cabeceras, incluidas las respuestas de error. No se modificó AWS.

## Diagnóstico comprobado el 12/09/2026

- GET `/api/carritos/cliente/CLI001` en AWS devolvió 404 con
  `{"mensaje":"No existe carrito activo para el cliente: CLI001"}` y sin
  Access-Control-Allow-Origin para localhost.
- OPTIONS `/api/carritos`, con origen `http://localhost:5173`, método solicitado
  POST y cabecera content-type, devolvió 403 `Invalid CORS request`.
- El navegador no puede leer ese 404 y Axios lo presenta como un fallo de red.
  El preflight bloquea el POST antes de ejecutar la creación.
- El servicio local no consulta un registro de clientes: exige un idCliente no
  vacío y busca un carrito ACTIVO. Un 404 de esta consulta significa que no hay
  carrito activo; no prueba que el cliente no exista ni que un carrito esté dañado.

## Verificación

`./mvnw -Dtest=CarritoCorsTests,CarritoServiceTests test`

Las pruebas CORS usan el controlador real con el servicio simulado, sin MongoDB.
Comprueban preflight, respuestas 201/200, errores 400/404 legibles por navegador
y rechazo de un origen no configurado.

Después de desplegar, repetir desde el frontend `/carrito` con los botones
Consultar carrito y Crear carrito. Confirmar en Network que OPTIONS responde
correctamente y que GET/POST incluyen Access-Control-Allow-Origin para el origen
real del frontend, también en 400/404.
