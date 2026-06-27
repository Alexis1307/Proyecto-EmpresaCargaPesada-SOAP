package cibertec.pe.sunat;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CdrService {

    /**
     * Descomprime el CDR enviado por SUNAT en memoria y extrae el código y mensaje de respuesta.
     * * @param cdrZipBytes El array de bytes del ZIP que responde el Web Service de SUNAT.
     * @return Map con las llaves "codigo" y "mensaje".
     */
    public Map<String, String> procesarCdr(byte[] cdrZipBytes) {
        Map<String, String> resultado = new HashMap<>();
        resultado.put("codigo", "-1");
        resultado.put("mensaje", "No se pudo procesar la respuesta de SUNAT.");

        if (cdrZipBytes == null || cdrZipBytes.length == 0) {
            return resultado;
        }

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(cdrZipBytes))) {
            ZipEntry entry;

            // 1. Recorrer el archivo ZIP en memoria buscando el XML de la constancia
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".xml")) {

                    // 2. Parsear el XML interno sin guardarlo en disco
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    // Leemos el contenido directamente del ZipInputStream
                    Document doc = builder.parse(zis);

                    // 3. Extraer los datos obligatorios del estándar del CDR de SUNAT
                    NodeList codeNodes = doc.getElementsByTagNameNS("*", "ResponseCode");
                    NodeList descriptionNodes = doc.getElementsByTagNameNS("*", "Description");

                    String codigo = (codeNodes.getLength() > 0) ? codeNodes.item(0).getTextContent() : "99";
                    String mensaje = (descriptionNodes.getLength() > 0) ? descriptionNodes.item(0).getTextContent() : "Sin descripción";

                    resultado.put("codigo", codigo);
                    resultado.put("mensaje", mensaje);

                    zis.closeEntry();
                    break; // Ya encontramos el archivo XML, salimos del bucle
                }
                zis.closeEntry();
            }
            return resultado;

        } catch (Exception e) {
            throw new RuntimeException("Error crítico al procesar el archivo CDR de SUNAT: " + e.getMessage(), e);
        }
    }
}
