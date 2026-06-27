# Corrección: integración Guía de Remisión con SUNAT (sendBill / error 0160)

## Diagnóstico

El error que recibías:

```
FAULT[soap-env:Client]: 0160
detail: El archivo XML esta vacio - Detalle: ... 'Validation File size error'
```

**no significa que el archivo tenga 0 bytes.** Tu ZIP (2.7 KB) y tu XML (5886 bytes)
existían. SUNAT usa ese mismo código para decir "no encontré un comprobante
electrónico válido dentro del paquete que me enviaste". Decodificando el ZIP
real de tu log (`contentFile` del SOAP request) encontré la causa raíz exacta:

```xml
<ext:ExtensionContent/>            <!-- vacío -->
...
</cac:Shipment>
<Signature xmlns="http://www.w3.org/2000/09/xmldsig#">...</Signature>   <!-- colgando aquí -->
</DespatchAdvice>
```

El manual del programador (sección 3.2.a) exige que la firma digital esté
**dentro** de `ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent`. Tu
firma quedaba **fuera**, como hijo final de `<DespatchAdvice>`, porque
`XmlSigner` firmaba apuntando al nodo raíz del documento
(`doc.getDocumentElement()`) en lugar de apuntar al nodo `ExtensionContent`.
SUNAT no encuentra ninguna firma en el lugar esperado → trata el documento
como inválido/vacío → `0160`.

Encontré dos problemas adicionales (menores, pero reales):

1. **Encoding inconsistente**: el XML declara `encoding="ISO-8859-1"` en su
   cabecera, pero `SunatService` convertía el `String` a `byte[]` con
   `StandardCharsets.UTF_8`. Funcionaba "por casualidad" mientras no hubiera
   tildes/ñ, pero viola la sección 1.4.c del manual y corrompería cualquier
   texto con caracteres especiales.
2. **Nada se guardaba en BD**: `SunatEnvioRepository` existía pero nunca se
   usaba. `SunatService.enviar()` solo devolvía un `String` con la respuesta
   SOAP cruda, sin persistir nada en `tbl_sunat_envio` ni vincularlo a la
   `GuiaTransporte`.

El endpoint que usabas
(`https://e-beta.sunat.gob.pe/ol-ti-itemision-guia-gem-beta/billService`) es
correcto — coincide con el que indica SUNAT para el servicio beta de Guía de
Remisión, así que no había que tocarlo.

## Validación de la corrección

No pude compilar tu proyecto Spring completo en este entorno (no hay Maven ni
red), pero **sí reproduje y verifiqué la lógica crítica de firmado en un
programa Java independiente**, usando un certificado PKCS12 real generado con
`keytool`. Resultado:

- ✅ `<Signature>` queda **dentro** de `<ext:ExtensionContent>`
- ✅ Ya no cuelga después de `</cac:Shipment>`
- ✅ Contiene `DigestValue`, `SignatureValue`, `X509Certificate` reales
- ✅ El XML resultante es bien formado y parseable
- ✅ **La firma se valida criptográficamente** (validé con
  `XMLSignature.validate()`, resultado `true`)
- ✅ El ZIP sigue teniendo la estructura `dummy/` + `<archivo>.xml` que pide
  el manual (sección 1.3)
- ✅ Confirmé con bytes crudos que ISO-8859-1 ahora codifica/decodifica
  consistentemente (antes: 24 bytes para una cadena con tildes en UTF-8 vs.
  16 bytes reales en ISO-8859-1 → corrupción si SUNAT lee respetando la
  cabecera declarada)

El script de prueba (`SignTest.java`) se incluye en este paquete por si
quieres correrlo tú mismo con `java SignTest.java` (Java 11+, no necesita
compilación previa).

## Archivos modificados

```
cibertec/pe/sunat/security/XmlSigner.java       <- fix principal (firma mal ubicada)
cibertec/pe/sunat/service/SunatService.java     <- fix encoding + persistencia en BD
cibertec/pe/sunat/soap/SunatSoapClient.java     <- parseo robusto de éxito/fault
cibertec/pe/sunat/soap/SunatSendResult.java     <- NUEVO: DTO de resultado
cibertec/pe/sunat/SunatTestController.java      <- /enviar ahora devuelve el SunatEnvio guardado
cibertec/pe/guiaTransportista/GuiaTransporte.java <- @JsonIgnore para evitar ciclo al serializar
```

### 1. `XmlSigner.java`

Antes:
```java
DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());
```

Ahora busca el nodo real por namespace URI (no depende del prefijo `ext:`) y
firma ahí dentro:
```java
Element extensionContent = findExtensionContent(doc); // ext:ExtensionContent
DOMSignContext dsc = new DOMSignContext(privateKey, extensionContent);
```

Si el XML que le llega no tiene `<ext:ExtensionContent/>` (por ejemplo si
cambias el builder más adelante y rompes esa estructura), ahora lanza un
`IllegalStateException` con un mensaje claro en vez de fallar en silencio.

### 2. `SunatService.java`

- Usa `Charset.forName("ISO-8859-1")` de forma consistente para codificar el
  XML antes de firmar y para decodificarlo al guardarlo/loguearlo.
- Ya no escribe directamente a `C:/temp` en medio del flujo crítico: ahora es
  un paso "best effort" que no tumba el envío si la carpeta no existe (útil
  si algún día corres esto en Linux/Mac o en un servidor).
- **Guarda un `SunatEnvio`** con: XML firmado, ZIP en base64, hash SHA-256 del
  ZIP, nombre de archivo, fecha de envío, y el resultado (`ENVIADO` /
  `RECHAZADO` / `ERROR`) junto con el CDR en base64 cuando corresponde.
- Vincula ese `SunatEnvio` a la `GuiaTransporte` (`guia.setSunatEnvio(...)`)
  para que puedas saber, consultando la guía, si ya tiene envío a SUNAT.
- Es `@Transactional`: si algo falla a mitad de camino, no quedan registros
  a medio guardar (excepto el caso de excepción de red, donde guardamos el
  intento fallido a propósito antes de relanzar la excepción, para que quede
  constancia en BD de qué pasó).

### 3. `SunatSoapClient.java` + `SunatSendResult.java` (nuevo)

Antes devolvía un `String` con la respuesta cruda o `"FAULT[...]: ..."`.
Ahora devuelve un objeto `SunatSendResult` que separa:

- `exito` (boolean)
- `cdrBase64` — el ZIP del CDR en base64, listo para guardar o descomprimir
- `faultCode` / `faultString` / `mensajeError` — cuando hay fault, incluyendo
  el contenido de `<detail><message>...` (donde SUNAT pone el detalle real,
  como el `ticket: ... error: Validation File size error` que viste)
- `rawResponse` — el SOAP completo, para depurar/loguear

Esto te permite tomar decisiones de negocio (reintentar, marcar como
rechazada, etc.) sin tener que parsear strings con regex.

### 4. `SunatTestController.java`

`/sunat/test/enviar/{id}` ahora devuelve el `SunatEnvio` guardado en BD (no
solo el string de SUNAT), así puedes ver en Bruno/Postman exactamente qué
quedó persistido.

### 5. `GuiaTransporte.java`

Agregué `@JsonIgnore` al campo `sunatEnvio`. Sin esto, al serializar el
`SunatEnvio` devuelto por `/enviar/{id}` (que referencia a `GuiaTransporte`,
que a su vez referencia de vuelta a `SunatEnvio`), Jackson entra en un ciclo
infinito y el endpoint truena con un `StackOverflowError` o un error de
serialización. Esto no estaba relacionado con tu bug original, pero lo
hubieras encontrado en cuanto el envío empezara a funcionar y el controller
intentara serializar la respuesta.

## Cómo probar

1. Reemplaza los 6 archivos en tu proyecto (mismas rutas de paquete).
2. Verifica que tu certificado de prueba (`certificado.pfx` en
   `src/main/resources`) siga siendo válido — el que tienes vence el
   2027-06-25, así que estás bien por ahora.
3. Llama a `GET /sunat/test/xml/{id}` para confirmar que el XML se sigue
   generando igual que antes (no debería cambiar nada aquí, el builder no se
   tocó).
4. Llama a `GET /sunat/test/enviar/{id}`. Revisa la consola: el LOG 1 ahora
   debe mostrar `<Signature>` **dentro** de `<ext:ExtensionContent>`, con
   `DigestValue`, `SignatureValue` y `X509Certificate` reales.
5. La respuesta del endpoint debe ser un JSON con los campos de `SunatEnvio`
   (`estado`, `cdrBase64`, `respuestaSunat`, etc.) en vez de un string plano.
6. Si SUNAT responde `ENVIADO`, decodifica `cdrBase64` (es un ZIP) para ver
   la Constancia de Recepción (CDR) — debería tener `ResponseCode = 0` si fue
   aceptada, o un código de error/observación si no.

## Si después de esto sigue sin funcionar

Con la firma y el encoding corregidos, lo más probable es que cualquier
nuevo rechazo ya sea un código de **negocio** (2000-3999) y no un error
estructural como el 0160. Si ves algo así, el mensaje en
`SunatEnvio.respuestaSunat` (o `cac:Response/cbc:Description` dentro del
CDR) te va a decir exactamente qué campo del UBL está mal — en ese punto es
cuestión de ajustar el `GuiaTransporteSunatXmlBuilder`, no la infraestructura
de firma/envío.
