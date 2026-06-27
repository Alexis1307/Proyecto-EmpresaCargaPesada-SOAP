package cibertec.pe.sunat;

import cibertec.pe.empresa.Empresa;
import cibertec.pe.factura.Factura;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    /**
     * Comprime el XML firmado en un archivo ZIP completamente en memoria.
     * * @param xmlFirmadoContenido El array de bytes del XML que devolvió el FirmaDigitalService.
     * @param empresa Entidad de la empresa emisora para extraer el RUC.
     * @param factura Entidad de la factura para extraer el número correlativo.
     * @return byte[] El archivo .zip listo para ser enviado por SOAP a la SUNAT.
     */
    public byte[] comprimirXmlEnMemoria(byte[] xmlFirmadoContenido, Empresa empresa, Factura factura) {
        // 1. Construir el nombre oficial estandarizado por SUNAT
        // Estructura: RUC-TIPO-NUMERO (Tipo '01' significa Factura)
        String nombreBase = empresa.getRuc() + "-01-" + factura.getNumeroFactura();
        String nombreXmlInterno = nombreBase + ".xml";

        // 2. Procesar la compresión usando streams en memoria
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // Crear una entrada (un archivo) dentro del ZIP con el nombre correcto
            ZipEntry entry = new ZipEntry(nombreXmlInterno);
            zos.putNextEntry(entry);

            // Escribir los bytes del XML firmado dentro de la entrada
            zos.write(xmlFirmadoContenido);
            zos.closeEntry();

            // Asegurar que todo se termine de escribir en el stream
            zos.finish();

            // Retornar los bytes puros del archivo .zip resultante
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error crítico al empaquetar el XML firmado en formato ZIP: " + e.getMessage(), e);
        }
    }
}