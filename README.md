<p align="center">
  <img src="https://github.com/izipay-pe/Imagenes/blob/main/logos_izipay/logo-izipay-banner-1140x100.png?raw=true" alt="Formulario" width=100%/>
</p>

# Server-PaymentForm-Springboot

## Índice

➡️ [1. Introducción](#-1-introducci%C3%B3n)  
🔑 [2. Requisitos previos](#-2-requisitos-previos)  
🚀 [3. Ejecutar ejemplo](#-3-ejecutar-ejemplo)  
🔗 [4. APIs](#4-APIs)  
💻 [4.1. FormToken](#41-formtoken)  
💳 [4.2. Validación de firma](#42-validaci%C3%B3n-de-firma)  
📡 [4.3. IPN](#43-ipn)  
📮 [5. Probar desde POSTMAN](#-5-probar-desde-postman)  
📚 [6. Consideraciones](#-6-consideraciones)

## ➡️ 1. Introducción

En este manual podrás encontrar una guía paso a paso para configurar un servidor API REST (Backend) en **[Springboot]** para la pasarela de pagos de IZIPAY. **El actual proyecto no incluye una interfaz de usuario (Frontend)** y debe integrarse con un proyecto de Front. Te proporcionaremos instrucciones detalladas y credenciales de prueba para la instalación y configuración del proyecto, permitiéndote trabajar y experimentar de manera segura en tu propio entorno local.
Este manual está diseñado para ayudarte a comprender el flujo de la integración de la pasarela para ayudarte a aprovechar al máximo tu proyecto y facilitar tu experiencia de desarrollo.

<p align="center">
  <img src="https://i.postimg.cc/KYpyqYPn/imagen-2025-01-28-082121144.png" alt="Formulario"/>
</p>

## 🔑 2. Requisitos Previos

- Comprender el flujo de comunicación de la pasarela. [Información Aquí](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/javascript/guide/start.html)
- Extraer credenciales del Back Office Vendedor. [Guía Aquí](https://github.com/izipay-pe/obtener-credenciales-de-conexion)
- Para este proyecto utilizamos Spring Boot v3.2.1.
- Apache Maven 3.9.9
- Java 17 o superior
  
> [!NOTE]
> Tener en cuenta que, para que el desarrollo de tu proyecto, eres libre de emplear tus herramientas preferidas.

## 🚀 3. Ejecutar ejemplo


### Clonar el proyecto
```sh
git clone https://github.com/izipay-pe/Server-PaymentForm-Springboot.git
``` 

### Datos de conexión 

Reemplace **[CHANGE_ME]** con sus credenciales de `API REST` extraídas desde el Back Office Vendedor, revisar [Requisitos previos](#-2-requisitos-previos).

- Editar el archivo `src/main/resources/config.properties` en la ruta raiz del proyecto:
```java
# Archivo para la configuración de las crendeciales de comercio
#
# Identificador de la tienda
USERNAME=CHANGE_ME_USER_ID

# Clave de Test o Producción
PASSWORD=CHANGE_ME_PASSWORD

# Clave Pública de Test o Producción
PUBLIC_KEY=CHANGE_ME_PUBLIC_KEY

# Clave HMAC-SHA-256 de Test o Producción
HMAC_SHA256=CHANGE_ME_HMAC_SHA_256
```

### Ejecutar proyecto

1. Ejecutar el proyecto directamente usando Maven

```sh
mvn spring-boot:run
``` 

2.  Abre un navegador web y navega a la siguiente URL:

```
http://127.0.0.1:8081
```

## 🔗4. APIs
- 💻 **FormToken:** Generación de formToken y envío de la llave publicKey necesarios para desplegar la pasarela.
- 💳  **Validacion de firma:** Se encarga de verificar la autenticidad de los datos.
- 📩 ️ **IPN:** Comunicación de servidor a servidor. Envío de los datos del pago al servidor.

## 💻4.1. FormToken
Para configurar la pasarela se necesita generar un formtoken. Se realizará una solicitud API REST a la api de creación de pagos:  `https://api.micuentaweb.pe/api-payment/V4/Charge/CreatePayment` con los datos de la compra para generar el formtoken. El servidor devuelve el formToken generado junto a la llave `publicKey` necesaria para desplegar la pasarela

Podrás encontrarlo en el archivo `src/main/java/com/example/serverpaymentform/controller/McwSpringboot.java`.

```java
@PostMapping("/formtoken")
public ResponseEntity<?> createFormToken(@RequestBody Map<String, String> parameters) {
        try {
            // Obtener PublicKey
            String PUBLIC_KEY = properties.getProperty("PUBLIC_KEY");
            
            // Obtenemos el FormToken generado
            String formToken = mcwController.generateFormToken(parameters);
            
            // Crear respuesta
            Map<String, String> response = new HashMap<>();
            response.put("formToken", formToken);
            response.put("publicKey", PUBLIC_KEY);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

```
Podrás acceder a esta API a través:
```bash
localhost:8081/formToken
```
ℹ️ Para más información: [Formtoken](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/javascript/guide/embedded/formToken.html)

## 💳4.2. Validación de firma
Se configura la función `checkHash` que realizará la validación de los datos recibidos por el servidor luego de realizar el pago mediante el parámetro `kr-answer` utilizando una clave de encriptación definida en `key`. Podrás encontrarlo en el archivo `src/main/java/com/example/serverpaymentform/controller/McwController.java`.

```java
 public boolean checkHash(String krHash, String key, String krAnswer){
	
	String calculatedHash = HmacSha256(krAnswer, key);
	return calculatedHash.equals(krHash);

}
```

Se valida que la firma recibida es correcta. Para la validación de los datos recibidos a través de la pasarela de pagos (front) se utiliza la clave `HMACSHA256`.

```java
    @PostMapping("/validate")
    public ResponseEntity<?> processResult(@RequestBody Map<String, String> resultParameters) {
        try {
            String HMAC_SHA256 = properties.getProperty("HMAC_SHA256");
            
            // Asignando los valores de la respuesta de Izipay
            String krHash = resultParameters.get("kr-hash");
            String krAnswer = resultParameters.get("kr-answer");
            
            // Válida que la respuesta sea íntegra
            if (!mcwController.checkHash(krHash, HMAC_SHA256, krAnswer)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid hash"));
            }

            // Retornarmos la respuesta
            return ResponseEntity.ok("true");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
```
El servidor devuelve un valor `true` verificando si los datos de la transacción coinciden con la firma recibida. Se confirma que los datos son enviados desde el servidor de Izipay.

Podrás acceder a esta API a través:
```bash
localhost:8081/validate
```

ℹ️ Para más información: [Analizar resultado del pago](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/kb/payment_done.html)

## 📩4.3. IPN
La IPN es una notificación de servidor a servidor (servidor de Izipay hacia el servidor del comercio) que facilita información en tiempo real y de manera automática cuando se produce un evento, por ejemplo, al registrar una transacción.

Se realiza la verificación de la firma utilizando la función `checkHash`. Para la validación de los datos recibidos a través de la IPN (back) se utiliza la clave `PASSWORD`. Se devuelve al servidor de izipay un mensaje confirmando el estado del pago.

Se recomienda verificar el parámetro `orderStatus` para determinar si su valor es `PAID` o `UNPAID`. De esta manera verificar si el pago se ha realizado con éxito.

Podrás encontrarlo en el archivo `src/main/java/com/example/serverpaymentform/controller/McwSpringboot.java`.

```java
    @PostMapping("/ipn")
    public ResponseEntity<?> processIpn(@RequestParam Map<String, String> ipnParameters) {
        try {
            String PASSWORD = properties.getProperty("PASSWORD");
            
            // Asignando los valores de la respuesta IPN
            String krHash = ipnParameters.get("kr-hash");
            String krAnswer = ipnParameters.get("kr-answer");
            
            // Válida que la respuesta sea íntegra
            if (!mcwController.checkHash(krHash, PASSWORD, krAnswer)) {
                return ResponseEntity.badRequest().body(Map.of("status", "No valid IPN"));
            }
            
            // Verifica el orderStatus PAID
            String orderStatus = jsonResponse.getString("orderStatus");
	          return ResponseEntity.ok("OK! Order Status: " + orderStatus);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
```
Podrás acceder a esta API a través:
```bash
localhost:8081/ipn
```

La ruta o enlace de la IPN debe ir configurada en el Backoffice Vendedor, en `Configuración -> Reglas de notificación -> URL de notificación al final del pago`

<p align="center">
  <img src="https://i.postimg.cc/XNGt9tyt/ipn.png" alt="Formulario" width=80%/>
</p>

ℹ️ Para más información: [Analizar IPN](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/api/kb/ipn_usage.html)

## 📡4.3.Pase a producción

Reemplace **[CHANGE_ME]** con sus credenciales de PRODUCCIÓN de `API REST` extraídas desde el Back Office Vendedor, revisar [Requisitos Previos](#-2-requisitos-previos).

- Editar el archivo `src/main/resources/config.properties` en la ruta raiz del proyecto:
```java
# Archivo para la configuración de las crendeciales de comercio
#
# Identificador de la tienda
USERNAME=CHANGE_ME_USER_ID

# Clave de Test o Producción
PASSWORD=CHANGE_ME_PASSWORD

# Clave Pública de Test o Producción
PUBLIC_KEY=CHANGE_ME_PUBLIC_KEY

# Clave HMAC-SHA-256 de Test o Producción
HMAC_SHA256=CHANGE_ME_HMAC_SHA_256
```

## 📮 5. Probar desde POSTMAN
* Puedes probar la generación del formToken desde POSTMAN. Coloca la URL con el metodo POST con la ruta `/formToken`.
  
 ```bash
localhost:8081/formToken
```

* Datos a enviar en formato JSON raw:
 ```node
{
    "amount": 1000,
    "currency": "PEN", //USD
    "orderId": "ORDER12345",
    "email": "cliente@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "123456789",
    "identityType": "DNI",
    "identityCode": "ABC123456",
    "address": "Calle principal 123",
    "country": "PE",
    "city": "Lima",
    "state": "Lima",
    "zipCode": "10001"
}
```

## 📚 6. Consideraciones

Para obtener más información, echa un vistazo a:

- [Formulario incrustado: prueba rápida](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/javascript/quick_start_js.html)
- [Primeros pasos: pago simple](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/javascript/guide/start.html)
- [Servicios web - referencia de la API REST](https://secure.micuentaweb.pe/doc/es-PE/rest/V4.0/api/reference.html)
