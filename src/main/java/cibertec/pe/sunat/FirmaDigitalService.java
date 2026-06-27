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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Service
public class FirmaDigitalService {

    public byte[] firmarDocumentoXml(String xmlString, String rutaCertificado, String contrasenaCertificado) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = Files.newInputStream(Paths.get(rutaCertificado))) {
                keyStore.load(is, contrasenaCertificado.toCharArray());
            }

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
                throw new RuntimeException("No se encontró una clave válida en el PFX.");
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, contrasenaCertificado.toCharArray());
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            Document doc = dbf.newDocumentBuilder().parse(
                    new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))
            );

            NodeList extensionContents = doc.getElementsByTagNameNS("urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2", "ExtensionContent");
            if (extensionContents.getLength() == 0) {
                throw new RuntimeException("No se encontró ext:ExtensionContent.");
            }

            var parentNode = extensionContents.item(0);

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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.xml.transform.Transformer tf = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
            tf.transform(new javax.xml.transform.dom.DOMSource(doc), new javax.xml.transform.stream.StreamResult(baos));

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error crítico durante la firma digital: " + e.getMessage(), e);
        }
    }
}