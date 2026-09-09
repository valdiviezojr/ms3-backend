# MS3 — Carrito de compras

Microservicio universitario con Java 21, Spring Boot 4.1.1, Maven, MongoDB,
Validation, Lombok y springdoc-openapi 3.1.0.

## Estructura y archivos

```text
pom.xml                                      # Dependencias y compilación Java 21
src/main/java/com/example/backendcarrito/
├── BackendCarritoApplication.java            # Punto de entrada existente
├── config/OpenApiConfig.java
├── controller/CarritoController.java
├── dto/
│   ├── CrearCarritoRequest.java
│   ├── AgregarItemRequest.java
│   ├── ActualizarCantidadRequest.java
│   └── CambiarEstadoRequest.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── RecursoNoEncontradoException.java
├── model/
│   ├── Carrito.java
│   ├── ItemCarrito.java
│   ├── ResumenCarrito.java
│   └── EstadoCarrito.java
├── repository/CarritoRepository.java
└── service/CarritoService.java
src/main/resources/application.properties
src/test/java/com/example/backendcarrito/
├── BackendCarritoApplicationTests.java        # Prueba existente de contexto
├── CarritoMongoIntegrationTests.java
└── service/CarritoServiceTests.java
```

Se crearon los archivos de las capas, configuración OpenAPI, las dos nuevas clases
 de pruebas y este README. Se actualizaron `pom.xml` y `application.properties`.

## Ejecutar

1. Tener MongoDB escuchando en `localhost:27017`.
2. Configurar `JAVA_HOME` con la carpeta de un JDK 21 instalado y agregar su `bin` al `PATH`:

   ```bash
   export JAVA_HOME=/ruta/a/tu/jdk-21
   export PATH="$JAVA_HOME/bin:$PATH"
   java -version
   ./mvnw spring-boot:run
   ```

   En IntelliJ, seleccionar ese JDK en Project SDK y en Maven Runner, recargar
   Maven y ejecutar `BackendCarritoApplication`.

3. Abrir http://localhost:8080/swagger-ui.html. El documento OpenAPI está en
   http://localhost:8080/v3/api-docs.

La máquina usada para desarrollar tenía JDK 26.0.2.1; las pruebas se ejecutaron con
ese JDK y Maven compila con `release=21`. No se instaló ni se verificó la ejecución
con un JDK 21 en esta máquina.

Para otra instancia de MongoDB:

```bash
MONGODB_URI='mongodb://localhost:27017/carrito_db' ./mvnw spring-boot:run
```

Se conserva `spring.data.mongodb.uri` solicitada y se enlaza a
`spring.mongodb.uri`, que es la propiedad efectiva en Spring Boot 4:
https://docs.spring.io/spring-boot/reference/data/nosql.html

## Comprobar MongoDB

```bash
mongosh 'mongodb://localhost:27017/carrito_db' --eval 'db.runCommand({ping: 1})'
```

Debe devolver `ok: 1`. En los logs de la aplicación debe aparecer
`Monitor thread successfully connected to server` con `state=CONNECTED`.
El mensaje `Started ...` por sí solo no confirma la conexión.

Después de crear un carrito por HTTP:

```bash
mongosh 'mongodb://localhost:27017/carrito_db' --eval 'db.carritos.find().pretty()'
```

MongoDB crea la base y la colección al guardar el primer documento. El `_id` se
asigna automáticamente al persistir, mediante Spring Data/driver MongoDB, y es
un ObjectId en BSON; se expone como String en HTTP. Los campos MongoDB usan
snake_case, las fechas se almacenan como BSON Date y los importes como Decimal128.
Las peticiones y respuestas HTTP usan camelCase.

## Probar con curl

Crear carrito (201 y encabezado Location):

```bash
curl -i -X POST http://localhost:8080/api/carritos \
  -H 'Content-Type: application/json' \
  -d '{"idCliente":"CLI001"}'
```

Copiar `id` de la respuesta:

```bash
CARRITO_ID='reemplazar-con-el-id-devuelto'
curl "http://localhost:8080/api/carritos/$CARRITO_ID"
curl http://localhost:8080/api/carritos/cliente/CLI001
```

Agregar producto:

```bash
curl -X POST "http://localhost:8080/api/carritos/$CARRITO_ID/items" \
  -H 'Content-Type: application/json' \
  -d '{"idProducto":101,"nombre":"Producto","precioUnitario":50.00,"urlImagen":"https://example.com/imagen.jpg","urlProducto":"https://example.com/productos/101","cantidad":2}'
```

El resumen debe ser `totalArticulos=2`, `subtotal=100.00`, `moneda=PEN`.

```bash
curl -X PATCH "http://localhost:8080/api/carritos/$CARRITO_ID/items/101" \
  -H 'Content-Type: application/json' -d '{"cantidad":3}'
curl -X DELETE "http://localhost:8080/api/carritos/$CARRITO_ID/items/101"
curl -X DELETE "http://localhost:8080/api/carritos/$CARRITO_ID/items"
curl -X PATCH "http://localhost:8080/api/carritos/$CARRITO_ID/estado" \
  -H 'Content-Type: application/json' -d '{"estado":"COMPLETADO"}'
curl -i -X DELETE "http://localhost:8080/api/carritos/$CARRITO_ID"
```

Las modificaciones devuelven el carrito actualizado (200). Eliminar el carrito
completo devuelve 204. Recursos ausentes devuelven 404 y peticiones inválidas 400,
con `mensaje` y, para errores de validación, un mapa `errores` por campo.

## Pruebas y JAR

```bash
./mvnw test
RUN_MONGO_TESTS=true ./mvnw package
java -jar target/backend-carrito-0.0.1-SNAPSHOT.jar
```

La prueba de integración requiere MongoDB local sin autenticación. Crea una base
`carrito_test_<uuid>` y elimina únicamente esa base al terminar. Verifica el CRUD,
validación HTTP, errores 404, Swagger, ObjectId, fechas y Decimal128 reales.
Las pruebas unitarias verifican valores iniciales, cálculos decimales, productos
repetidos, recursos ausentes, estados y eliminación.

## Supuestos de esta versión

- CrearCarritoRequest recibe únicamente idCliente; los items iniciales siempre están vacíos.
- Los DTOs no contienen resumen, fechas ni ID del carrito; el backend determina esos valores.
- Un producto repetido suma cantidad y conserva precio, nombre, URLs y fecha originales.
- Cantidad e idProducto deben ser positivos. Precio puede ser cero y admite hasta
  12 dígitos enteros y 2 decimales. Las URLs son opcionales; si se proporcionan,
  deben tener formato URL válido.
- Solo se admite un carrito activo por cliente en operaciones secuenciales; crear
  o reactivar un segundo produce 400. Esta comprobación es de servicio, sin índice
  único ni garantía frente a peticiones concurrentes.
- Se permiten cambios de items en cualquier estado y transiciones entre todos los
  estados, porque no se especificaron restricciones adicionales.
- Las fechas usan LocalDateTime y la zona horaria del proceso. Se debe usar una
  zona consistente al desplegar posteriormente.
- La versión local utiliza lectura/modificación/guardado del documento; las
  actualizaciones concurrentes del mismo carrito pueden sobrescribirse.
- `CarritoService.crearItem` concentra la obtención de datos del producto para
  incorporar posteriormente ProductoClient sin cambiar los cálculos del carrito.
  Por ahora los datos y el precio llegan en la petición, como se solicitó.

El proyecto genera un JAR ejecutable y recibe la URI por entorno. No incluye
Dockerfile, carga masiva, integración HTTP con MS1 ni configuración AWS.
