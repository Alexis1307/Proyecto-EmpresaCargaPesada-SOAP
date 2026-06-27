package cibertec.pe.sunat;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.sunat.ws.BillService;
import pe.sunat.ws.BillService_Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio que envía el comprobante electrónico (ZIP firmado) a SUNAT vía SOAP.
 *
 * CORRECCIONES:
 * 1. El endpoint URL se configura explícitamente en el BindingProvider.
 *    Sin esto, JAX-WS usa la URL del WSDL (que puede apuntar a producción
 *    o a una URL inválida) ignorando la propiedad sunat.endpoint del .properties.
 * 2. Se agrega el prefijo "https:" si el endpoint viene sin protocolo
 *    (como está definido en tu application.properties actual).
 */
@Service
public class SunatSoapService {

    private final SunatHeaderHandler sunatHeaderHandler;

    @Value("${sunat.endpoint}")
    private String sunatEndpoint;

    public SunatSoapService(SunatHeaderHandler sunatHeaderHandler) {
        this.sunatHeaderHandler = sunatHeaderHandler;
    }

    /**
     * Envía el archivo ZIP firmado a SUNAT y retorna el CDR (Constancia de Recepción).
     *
     * @param nombreArchivo Nombre oficial del ZIP según estándar SUNAT.
     *                      Formato: RUC-TIPO-SERIE-CORRELATIVO.zip
     *                      Ejemplo: "20601234567-01-F001-00000001.zip"
     * @param zipBytes      Contenido binario del ZIP generado por ZipService.
     * @return byte[] con el CDR en formato ZIP devuelto por SUNAT.
     */
    public byte[] sendBill(String nombreArchivo, byte[] zipBytes) {
        try {
            // 1. Instanciar el cliente SOAP generado desde el WSDL de SUNAT
            BillService_Service service = new BillService_Service();
            BillService port = service.getBillServicePort();

            // 2. CORRECCIÓN CRÍTICA: Sobreescribir la URL del endpoint
            //    para que apunte al servidor correcto (Beta o Producción).
            //    Sin este paso, la clase autogenerada usa su URL interna del WSDL.
            BindingProvider bindingProvider = (BindingProvider) port;

            // Normalizar el endpoint: agregar "https:" si viene como "//e-beta.sunat.gob.pe/..."
            String urlFinal = sunatEndpoint.startsWith("//")
                ? "https:" + sunatEndpoint
                : sunatEndpoint;

            bindingProvider.getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                urlFinal
            );

            // 3. Inyectar el Handler de credenciales WS-Security
            List<jakarta.xml.ws.handler.Handler> handlerChain = bindingProvider.getBinding().getHandlerChain();
            if (handlerChain == null) {
                handlerChain = new ArrayList<>();
            }
            handlerChain.add(sunatHeaderHandler);
            bindingProvider.getBinding().setHandlerChain(handlerChain);

            // 4. MTOM: SUNAT exige el ZIP como DataHandler, no como byte[] directo
            ByteArrayDataSource dataSource = new ByteArrayDataSource(zipBytes, "application/zip");
            DataHandler dataHandlerEnvio = new DataHandler(dataSource);

            // 5. Invocar el método remoto sendBill de SUNAT
            byte[] cdrResponseBytes = port.sendBill(nombreArchivo, dataHandlerEnvio, null);

            if (cdrResponseBytes == null || cdrResponseBytes.length == 0) {
                throw new RuntimeException("SUNAT respondió pero el CDR retornó vacío. Verifica el nombre del archivo enviado.");
            }

            return cdrResponseBytes;

        } catch (jakarta.xml.ws.soap.SOAPFaultException sfe) {
            // SUNAT devuelve SOAPFault cuando el comprobante tiene errores formales
            throw new RuntimeException("SUNAT rechazó el comprobante (SOAP Fault): " + sfe.getFault().getFaultString(), sfe);
        } catch (Exception e) {
            throw new RuntimeException("Error de conectividad al enviar comprobante a SUNAT: " + e.getMessage(), e);
        }
    }
}
