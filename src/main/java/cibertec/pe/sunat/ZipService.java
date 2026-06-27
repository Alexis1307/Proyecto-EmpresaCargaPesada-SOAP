package cibertec.pe.sunat;

import cibertec.pe.empresa.Empresa;
import cibertec.pe.factura.Factura;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio que empaqueta el XML firmado en un archivo ZIP en memoria.
 *
 * Nombre del XML interno y ZIP según estándar SUNAT:
 * El archivo XML dentro del ZIP debe llamarse exactamente igual que el ZIP,
 * pero con extensión .xml en lugar de .zip.
 * Formato: RUC-SERIE-CORRELATIVO.xml
 * Ejemplo: "20601234567-F001-00000001.xml"
 *
 * ANTES (incorrecto): RUC + "-01-" + numeroFactura → "20601234567-01-F001-00000001.xml"
 * El "-01-" es el tipo de documento (factura), pero SUNAT espera que el nombre sea
 * solo RUC-SERIE-CORRELATIVO, sin el tipo intercalado.
 */
@Service
public class ZipService {

    /**
     * Comprime el XML firmado en un archivo ZIP en memoria.
     *
     * @param xmlFirmadoContenido Bytes del XML firmado por FirmaDigitalService.
     * @param empresa             Empresa emisora (se usa el RUC).
     * @param factura             Factura emitida (se usa el numeroFactura: ej "F001-00000001").
     * @return byte[] con el archivo .zip listo para enviar a SUNAT.
     */
    public byte[] comprimirXmlEnMemoria(byte[] xmlFirmadoContenido, Empresa empresa, Factura factura) {

        // Nombre del XML interno: RUC-TIPO-SERIE-CORRELATIVO.xml
        // Formato oficial SUNAT: RUC + "-" + TipoDocumento + "-" + Serie + "-" + Correlativo
        // Ejemplo: "20601234567-01-F001-00000001.xml"
        // Donde "01" es el tipo CPE (01=Factura). SUNAT valida este campo obligatoriamente.
        // El numeroFactura ya viene en formato "F001-00000001", por eso se intercala "-01-".
        String nombreXmlInterno = empresa.getRuc() + "-01-" + factura.getNumeroFactura() + ".xml";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry = new ZipEntry(nombreXmlInterno);
            zos.putNextEntry(entry);
            zos.write(xmlFirmadoContenido);
            zos.closeEntry();
            zos.finish();

            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error al empaquetar el XML firmado en ZIP: " + e.getMessage(), e);
        }
    }
}