package cibertec.pe.sunat;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import org.springframework.stereotype.Service;
import pe.sunat.ws.BillService;
import pe.sunat.ws.BillService_Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

@Service
public class SunatSoapService {

    private final SunatHeaderHandler sunatHeaderHandler;

    public SunatSoapService(SunatHeaderHandler sunatHeaderHandler) {
        this.sunatHeaderHandler = sunatHeaderHandler;
    }

    /**
     * Envía el archivo ZIP de la factura a los servidores de SUNAT.
     * * @param nombreArchivo Nombre oficial del archivo (Ej: "20123456789-01-F001-00000001.zip")
     * @param zipBytes Contenido binario del archivo ZIP generado por ZipService
     * @return byte[] La Constancia de Recepción (CDR) devuelta por SUNAT en formato ZIP
     */
    public byte[] sendBill(String nombreArchivo, byte[] zipBytes) {
        try {
            // 1. Instanciar el Servicio Oficial autogenerado por JAX-WS
            BillService_Service service = new BillService_Service();
            BillService port = service.getBillServicePort();

            // 2. Configurar el BindingProvider para inyectar las cabeceras de seguridad
            BindingProvider bindingProvider = (BindingProvider) port;

// CORRECCIÓN: Acceder a través de getBinding()
            List<jakarta.xml.ws.handler.Handler> handlerChain = bindingProvider.getBinding().getHandlerChain();
            if (handlerChain == null) {
                handlerChain = new ArrayList<>();
            }

            // Agregamos tu handler a la lista existente
            handlerChain.add(sunatHeaderHandler);

            // Guardamos la lista actualizada de regreso en el binding
            bindingProvider.getBinding().setHandlerChain(handlerChain);

            // 3. Corrección Obligatoria MTOM: Convertir byte[] a DataHandler tal como lo exige el WSDL
            ByteArrayDataSource dataSource = new ByteArrayDataSource(zipBytes, "application/zip");
            DataHandler dataHandlerEnvio = new DataHandler(dataSource);

            // 4. Invocar al método remoto "sendBill" de la SUNAT
            // Pasamos el DataHandler en lugar del byte[] directo
            byte[] cdrResponseBytes = port.sendBill(nombreArchivo, dataHandlerEnvio, null);

            if (cdrResponseBytes == null || cdrResponseBytes.length == 0) {
                throw new RuntimeException("SUNAT respondió correctamente pero el archivo CDR retornó vacío.");
            }

            return cdrResponseBytes;

        } catch (jakarta.xml.ws.soap.SOAPFaultException sfe) {
            throw new RuntimeException("Error del Servidor SUNAT (SOAP Fault): " + sfe.getFault().getFaultString(), sfe);
        } catch (Exception e) {
            throw new RuntimeException("Error de conectividad al enviar el comprobante a SUNAT: " + e.getMessage(), e);
        }
    }
}