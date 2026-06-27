package cibertec.pe.sunat;

import jakarta.xml.soap.*;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Set;

    @Component
    public class SunatHeaderHandler implements SOAPHandler<SOAPMessageContext> {

        // Credenciales Beta por defecto de la SUNAT para pruebas
        // En producción, estos valores deberían venir desde tu application.properties
        private static final String RUC_EMPRESA = "20123456789";
        private static final String USUARIO_SOL = "MODDATOS";
        private static final String CLAVE_SOL = "MODDATOS";

        @Override
        public Set<QName> getHeaders() {
            return Collections.emptySet();
        }

        @Override
        public boolean handleMessage(SOAPMessageContext context) {
            // Verificar si es una salida (petición saliente del cliente hacia SUNAT)
            Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            if (outboundProperty) {
                try {
                    SOAPMessage message = context.getMessage();
                    SOAPPart soapPart = message.getSOAPPart();
                    SOAPEnvelope envelope = soapPart.getEnvelope();
                    SOAPHeader header = envelope.getHeader();

                    // Si no existe cabecera SOAP, la creamos
                    if (header == null) {
                        header = envelope.addHeader();
                    }

                    // 1. Definir los Namespaces necesarios para WS-Security
                    String wsseNamespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

                    // 2. Crear el bloque <wsse:Security>
                    SOAPElement securityElement = header.addChildElement("Security", "wsse", wsseNamespace);

                    // 3. Crear el bloque <wsse:UsernameToken>
                    SOAPElement usernameTokenElement = securityElement.addChildElement("UsernameToken", "wsse");

                    // 4. Concatenar obligatoriamente RUC + Usuario SOL tal como lo exige SUNAT
                    String usuarioCompletoSunat = RUC_EMPRESA + USUARIO_SOL;

                    // 5. Inyectar los valores de usuario y clave dentro del Token
                    SOAPElement usernameElement = usernameTokenElement.addChildElement("Username", "wsse");
                    usernameElement.addTextNode(usuarioCompletoSunat);

                    SOAPElement passwordElement = usernameTokenElement.addChildElement("Password", "wsse");
                    passwordElement.addTextNode(CLAVE_SOL);

                    // Guardar los cambios estructurales en el mensaje de salida
                    message.saveChanges();

                } catch (SOAPException e) {
                    throw new RuntimeException("Error crítico al construir las cabeceras WS-Security para SUNAT: " + e.getMessage(), e);
                }
            }
            return true; // Permitir que el mensaje continúe su flujo en la tubería SOAP
        }

        @Override
        public boolean handleFault(SOAPMessageContext context) {
            return true;
        }

        @Override
        public void close(MessageContext context) {
            // Limpieza de recursos si fuese necesario
        }
    }
