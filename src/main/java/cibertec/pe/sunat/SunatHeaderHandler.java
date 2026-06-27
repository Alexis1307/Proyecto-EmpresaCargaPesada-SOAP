package cibertec.pe.sunat;

import jakarta.xml.soap.*;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Set;

/**
 * Handler SOAP que inyecta las credenciales WS-Security en cada petición a SUNAT.
 *
 * CORRECCIÓN: Las credenciales ya no están hardcodeadas. Se leen desde
 * application.properties usando @Value, así el mismo código funciona
 * tanto para el ambiente Beta (pruebas) como para Producción.
 */
@Component
public class SunatHeaderHandler implements SOAPHandler<SOAPMessageContext> {

    // Leídos desde application.properties
    @Value("${sunat.ruc}")
    private String rucEmpresa;

    @Value("${sunat.usuario-sol}")
    private String usuarioSol;

    @Value("${sunat.clave-sol}")
    private String claveSol;

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (Boolean.TRUE.equals(outbound)) {
            try {
                SOAPMessage message = context.getMessage();
                SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
                SOAPHeader header = envelope.getHeader();

                if (header == null) {
                    header = envelope.addHeader();
                }

                String wsseNs = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

                // <wsse:Security>
                SOAPElement security = header.addChildElement("Security", "wsse", wsseNs);

                // <wsse:UsernameToken>
                SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");

                // SUNAT exige que el username sea: RUC + UsuarioSOL (concatenado, sin separador)
                String usuarioCompleto = rucEmpresa + usuarioSol;

                SOAPElement username = usernameToken.addChildElement("Username", "wsse");
                username.addTextNode(usuarioCompleto);

                SOAPElement password = usernameToken.addChildElement("Password", "wsse");
                password.addTextNode(claveSol);

                message.saveChanges();

            } catch (SOAPException e) {
                throw new RuntimeException("Error al construir cabeceras WS-Security para SUNAT: " + e.getMessage(), e);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }
}
