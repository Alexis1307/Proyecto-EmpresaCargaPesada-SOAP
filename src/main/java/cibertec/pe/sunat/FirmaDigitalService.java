package cibertec.pe.sunat;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Servicio que firma digitalmente el XML UBL con el certificado PFX de la empresa.
 *
 * CORRECCIÓN CRÍTICA: El certificado se carga desde el classpath
 * (src/main/resources/) usando getResourceAsStream(), no con Files.newInputStream().
 * La ruta con Files.get() solo funciona en el IDE pero falla al ejecutar el .jar
 * empaquetado porque src/main/resources/ no existe como directorio en producción.
 */
@Service
public class FirmaDigitalService {

    /**
     * Firma el XML UBL 2.1 con la llave privada del certificado PFX.
     *
     * @param xmlString           El XML en texto plano generado por UblBuilder.
     * @param nombreCertificado   Nombre del archivo .pfx dentro de resources/ (ej: "certificado.pfx")
     * @param contrasena          Contraseña del archivo .pfx
     * @return byte[] con el XML firmado digitalmente (incluye el bloque <ds:Signature>)
     */
    public byte[] firmarDocumentoXml(String xmlString, String nombreCertificado, String contrasena) {
        try {
            // CORRECCIÓN: Cargar el certificado desde el classpath del proyecto
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(nombreCertificado)) {
                if (is == null) {
                    throw new RuntimeException(
                        "No se encontró el certificado '" + nombreCertificado + "' en src/main/resources/. " +
                        "Verifica que el archivo .pfx esté en esa carpeta."
                    );
                }
                keyStore.load(is, contrasena.toCharArray());
            }

            // Buscar el alias de la clave privada dentro del keystore
            String alias = null;
            var aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String current = aliases.nextElement();
                if (keyStore.isKeyEntry(current)) {
                    alias = current;
                    break;
                }
            }

            if (alias == null) {
                throw new RuntimeException("No se encontró ninguna clave privada válida dentro del archivo PFX.");
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, contrasena.toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            // Parsear el XML a DOM (necesario para la firma XML)
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            Document doc = dbf.newDocumentBuilder().parse(
                new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))
            );

            // Ubicar el nodo ext:ExtensionContent donde se insertará la firma
            String extNs = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
            NodeList extensionContents = doc.getElementsByTagNameNS(extNs, "ExtensionContent");
            if (extensionContents.getLength() == 0) {
                throw new RuntimeException(
                    "El XML no contiene el bloque ext:ExtensionContent requerido por UBL 2.1 para la firma. " +
                    "Revisa que UblBuilder lo esté generando correctamente."
                );
            }

            var parentNode = extensionContents.item(0);

            // Configurar la firma XML con SHA-256 + RSA (estándar SUNAT)
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            Reference ref = fac.newReference(
                "",
                fac.newDigestMethod(DigestMethod.SHA256, null),
                Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                null
            );

            SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                Collections.singletonList(ref)
            );

            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data x509Data = kif.newX509Data(Collections.singletonList(certificate));
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

            DOMSignContext dsc = new DOMSignContext(privateKey, parentNode);
            dsc.setDefaultNamespacePrefix("ds");

            XMLSignature signature = fac.newXMLSignature(si, ki);
            signature.sign(dsc);

            // Serializar el Document DOM de vuelta a bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.xml.transform.Transformer tf = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
            tf.transform(new javax.xml.transform.dom.DOMSource(doc), new javax.xml.transform.stream.StreamResult(baos));

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error durante la firma digital del XML: " + e.getMessage(), e);
        }
    }
}
