# BFF (Backend For Frontend) - Ecosistema Cordillera

Este microservicio actúa como el **Orquestador Central** de la arquitectura. Su propósito exclusivo es servir como puente optimizado entre la interfaz de usuario (Frontend en React) y los microservicios core subyacentes.

A diferencia de los microservicios de dominio (Productos, Categorías, Stock), el BFF **no posee base de datos propia**. Su trabajo es consumir, agregar y transformar los datos de la red interna para entregar respuestas a medida mediante una única llamada HTTP.

---

## 🚀 Arquitectura y Patrones Implementados

Este servicio es la pieza clave para el rendimiento del ecosistema, aplicando los siguientes patrones de diseño:

*   **Patrón BFF (Backend For Frontend):** Evita el problema de "chatty network" (múltiples llamadas desde el frontend). El BFF realiza las peticiones internamente (con latencia de milisegundos) y ensambla un único objeto (`CatalogoDashboardDTO`) optimizado para la pantalla de React.
*   **Tolerancia a Fallos (Resiliencia):** Implementa estrategias de "Fallback". Si un servicio no crítico (como Categorías) se cae o demora en responder, el BFF captura la excepción y provee un valor por defecto ("Categoría no disponible") en lugar de propagar un Error 500 al cliente.
*   **Patrón DTO (Data Transfer Object):** Transforma las respuestas crudas de los microservicios internos en objetos limpios y exactos que la interfaz necesita, ahorrando ancho de banda y ocultando la estructura real de las bases de datos.

---

## 📋 Requisitos Previos

*   [Java Development Kit (JDK) 17 o superior](https://adoptium.net/)
*   [Apache Maven](https://maven.apache.org/) (o el Wrapper `./mvnw`)
*   IDE recomendado: IntelliJ IDEA, VS Code o Eclipse.

---

## ⚙️ Configuración del Entorno

El BFF está configurado para ejecutarse en el puerto **8084**.

⚠️ **Dependencias Críticas:** Para que el BFF pueda orquestar la información correctamente, los siguientes microservicios internos deben estar en ejecución:
*   **Microservicio de Productos** (Puerto 8081)
*   **Microservicio de Categorías** (Puerto 8082)
*   **Microservicio de Stock** (Puerto 8083)

*Nota de Seguridad: El Frontend (React) no debe apuntar nunca directamente a este puerto (8084). Todo el tráfico debe ingresar a través del API Gateway (Puerto 8090).*

---

## 🛠️ Cómo Ejecutar el Proyecto

Tienes varias opciones para levantar este microservicio en tu entorno local:

### Opción 1: Usando tu IDE (Desarrollo)
1. Localiza la clase principal de la aplicación (ej. `BffApplication.java`).
2. Haz clic en el botón de **Run** (▶) o **Debug** (🐛).

### Opción 2: Usando la terminal (Maven)
Abre la terminal en la raíz del proyecto y ejecuta:
```bash
mvn spring-boot:run

GET https://localhost:8088/bff/catalogo/{productoId}

🧪 Pruebas Unitarias (Testing)
Dado que este servicio es un orquestador sin base de datos, la suite de pruebas (JUnit 5 + Mockito) se centra exclusivamente en la lógica de agregación y resiliencia.

Las pruebas verifican:

El Camino Feliz: Unión correcta de datos cuando los clientes Feign responden exitosamente.

Manejo de Errores: Activación de fallbacks cuando un servicio dependiente falla.

Lógica Matemática: Correcta reducción y suma del stock proveniente de múltiples sucursales.

Para ejecutar la suite de pruebas completa:

Bash
mvn test
🛡️ Solución de Problemas Frecuentes
Error FeignException$NotFound (404): El BFF intentó buscar un dato (ej. un producto) pero el ID no existe en el microservicio origen.

Error Connection refused (500): El microservicio destino (Productos, Categorías o Stock) está apagado o corriendo en un puerto distinto al esperado por el cliente Feign. Revisa los logs para ver cuál servicio falló.

El campo de Categoría dice "Categoría no disponible": Esto no es un bug del BFF, es el mecanismo de resiliencia actuando. Significa que el microservicio de Categorías está caído o inaccesible, pero el BFF protegió la respuesta general.
Abre la terminal en la raíz del proyecto y ejecuta:
```bash
mvn spring-boot:run
