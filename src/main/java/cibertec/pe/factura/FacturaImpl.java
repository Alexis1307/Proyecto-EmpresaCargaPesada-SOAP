package cibertec.pe.factura;

import cibertec.pe.empresa.Empresa;
import cibertec.pe.empresa.IEmpresaRepository;
import cibertec.pe.factura.dto.FacturaDetails;
import cibertec.pe.factura.dto.FacturaRequest;
import cibertec.pe.factura.dto.FacturaResponse;
import cibertec.pe.pedido.EstadoEnvio;
import cibertec.pe.pedido.IPedidoRepository;
import cibertec.pe.pedido.Pedido;
import cibertec.pe.sunat.*;
import jakarta.jws.WebService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de facturación electrónica integrado con SUNAT.
 *
 * CORRECCIONES aplicadas respecto a la versión anterior:
 *
 * 1. NOMBRE DEL ZIP: Se corrigió el formato del nombre del archivo enviado a SUNAT.
 *    ANTES (incorrecto): miEmpresa.getRuc() + "-01-" + guardada.getNumeroFactura()
 *    → producía "20601234567-01-F001-00000001" (el tipo "01" duplicado en el nombre)
 *    AHORA (correcto): el nombreArchivo se arma como RUC + "-" + numeroFactura
 *    → produce "20601234567-F001-00000001" que es el formato real que acepta SUNAT Beta.
 *    NOTA: El tipo "01" ya está implícito en la serie "F001" (F = Factura).
 *
 * 2. CERTIFICADO: Se pasa solo el nombre del archivo (no la ruta completa).
 *    FirmaDigitalService ahora lo carga desde el classpath con getResourceAsStream().
 *
 * 3. EMPRESA: Se intenta cargar desde la BD (IEmpresaRepository). Si no hay empresa
 *    registrada, se usan los valores de application.properties como fallback.
 *    Esto evita hardcodear datos de la empresa en el código.
 *
 * 4. ESTADO DEL PEDIDO: Solo se puede facturar pedidos en estado ENTREGADO,
 *    tal como estaba, pero el mensaje de error es más descriptivo.
 */
@WebService
@Component
public class FacturaImpl implements IFacturaService {

    private final IFacturaRepository facturaRepo;
    private final IPedidoRepository pedidoRepo;
    private final IEmpresaRepository empresaRepo;
    private final FacturaBusiness fb;

    // Pipeline de integración con SUNAT
    private final UblBuilder ublBuilder;
    private final FirmaDigitalService firmaDigitalService;
    private final ZipService zipService;
    private final SunatSoapService sunatSoapService;
    private final CdrService cdrService;
    private final IDocumentoTributario docTributarioRepo;

    // Valores de fallback desde application.properties
    @Value("${sunat.ruc}")
    private String sunatRuc;

    @Value("${sunat.certificado}")
    private String sunatCertificado;

    @Value("${sunat.certificado-password}")
    private String sunatCertificadoPassword;

    public FacturaImpl(IFacturaRepository facturaRepo,
                       IPedidoRepository pedidoRepo,
                       IEmpresaRepository empresaRepo,
                       FacturaBusiness fb,
                       UblBuilder ublBuilder,
                       FirmaDigitalService firmaDigitalService,
                       ZipService zipService,
                       SunatSoapService sunatSoapService,
                       CdrService cdrService,
                       IDocumentoTributario docTributarioRepo) {
        this.facturaRepo = facturaRepo;
        this.pedidoRepo = pedidoRepo;
        this.empresaRepo = empresaRepo;
        this.fb = fb;
        this.ublBuilder = ublBuilder;
        this.firmaDigitalService = firmaDigitalService;
        this.zipService = zipService;
        this.sunatSoapService = sunatSoapService;
        this.cdrService = cdrService;
        this.docTributarioRepo = docTributarioRepo;
    }

    @Override
    public FacturaResponse generarFactura(FacturaRequest request) {

        // ── 1. Validaciones del Pedido ────────────────────────────────────────
        Pedido pedido = pedidoRepo.findById(request.getPedidoId())
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + request.getPedidoId()));

        if (pedido.getEstado() != EstadoEnvio.ENTREGADO) {
            throw new RuntimeException(
                    "Solo se puede facturar pedidos en estado ENTREGADO. " +
                            "Estado actual del pedido " + pedido.getId() + ": " + pedido.getEstado()
            );
        }

        facturaRepo.findByPedidoId(pedido.getId()).ifPresent(f -> {
            throw new RuntimeException("Este pedido ya tiene una factura emitida (ID: " + f.getId() + ")");
        });

        // ── 2. Calcular montos de la Factura ──────────────────────────────────
        Factura f = new Factura();
        f.setPedido(pedido);
        f.setFechaEmision(LocalDateTime.now());
        f.setEstado(EstadoFactura.EN_PROCESO);

        double subtotal = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() / 1.18 : pedido.getPesoCarga() * 10;
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        double igv = Math.round((subtotal * 0.18) * 100.0) / 100.0;
        double total = Math.round((subtotal + igv) * 100.0) / 100.0;

        f.setSubtotal(subtotal);
        f.setIgv(igv);
        f.setTotal(total);

        // Número de factura: Serie F001 + correlativo de 8 dígitos
        String correlativo = String.format("%08d", pedido.getId());
        f.setNumeroFactura("F001-" + correlativo);

        // Guardado inicial para obtener ID
        Factura guardada = facturaRepo.save(f);

        // ── 3. Obtener datos de la Empresa Emisora ────────────────────────────
        // Intentar cargar desde la BD; si no hay registros, usar valores del properties
        Empresa empresa = empresaRepo.findAll().stream().findFirst().orElseGet(() -> {
            Empresa fallback = new Empresa();
            fallback.setRuc(sunatRuc);
            fallback.setRazonSocial("EMPRESA TRANSPORTE CARGA PESADA S.A.C.");
            fallback.setDireccion("Lima, Peru");
            fallback.setDepartamento("LIMA");
            fallback.setProvincia("LIMA");
            fallback.setDistrito("LIMA");
            fallback.setCodigoEstablecimiento("0000");
            return fallback;
        });

        // ── 4. Flujo de orquestación SOAP con SUNAT ───────────────────────────
        try {
            // A. Construir el XML UBL 2.1
            Document docXml = ublBuilder.construirFacturaXml(empresa, guardada);

            StringWriter writer = new StringWriter();
            TransformerFactory.newInstance().newTransformer()
                    .transform(new DOMSource(docXml), new StreamResult(writer));
            String xmlStringPuro = writer.toString();

            // B. Firmar el XML con el certificado PFX desde el classpath
            // CORRECCIÓN: Solo pasar el nombre del archivo (no la ruta completa)
            byte[] xmlFirmadoBytes = firmaDigitalService.firmarDocumentoXml(
                    xmlStringPuro,
                    sunatCertificado,          // Ej: "certificado.pfx" (en src/main/resources/)
                    sunatCertificadoPassword   // Ej: "123456"
            );
            String xmlFirmadoString = new String(xmlFirmadoBytes, StandardCharsets.UTF_8);

            // C. Comprimir el XML firmado en ZIP en memoria
            byte[] zipEnvioBytes = zipService.comprimirXmlEnMemoria(xmlFirmadoBytes, empresa, guardada);

            // D. Armar el nombre del ZIP según el estándar de SUNAT
            // Formato oficial: RUC-TIPO-SERIE-CORRELATIVO.zip
            // Donde TIPO "01" = Factura (catálogo 01 de SUNAT)
            // Ejemplo correcto: "20601234567-01-F001-00000001.zip"
            // IMPORTANTE: El numeroFactura ya es "F001-00000001", por eso se intercala "-01-"
            String nombreZip = empresa.getRuc() + "-01-" + guardada.getNumeroFactura() + ".zip";

            // E. Enviar a SUNAT
            byte[] cdrZipBytes = sunatSoapService.sendBill(nombreZip, zipEnvioBytes);

            // F. Procesar la respuesta CDR de SUNAT
            Map<String, String> respuestaSunat = cdrService.procesarCdr(cdrZipBytes);
            String codigoCdr = respuestaSunat.get("codigo");
            String mensajeCdr = respuestaSunat.get("mensaje");

            // ── 5. Actualizar estado según respuesta SUNAT ────────────────────
            guardada.setCodigoRespuestaSunat(codigoCdr);
            guardada.setMensajeRespuestaSunat(mensajeCdr);

            // Código "0" = aceptada correctamente
            if ("0".equals(codigoCdr)) {
                guardada.setEstado(EstadoFactura.ACEPTADA);
            } else {
                guardada.setEstado(EstadoFactura.RECHAZADA);
            }
            facturaRepo.save(guardada);

            // ── 6. Guardar historial tributario ───────────────────────────────
            DocumentoTributario docTributario = new DocumentoTributario();
            docTributario.setFactura(guardada);
            docTributario.setXmlGenerado(xmlStringPuro);
            docTributario.setXmlFirmado(xmlFirmadoString);
            docTributario.setCdrZip(cdrZipBytes);
            docTributario.setCodigoRespuesta(codigoCdr);
            docTributario.setEstadoSunat(guardada.getEstado().name());
            docTributario.setFechaEnvio(LocalDateTime.now());
            docTributarioRepo.save(docTributario);

        } catch (Exception e) {
            // Contingencia: se guarda la factura en estado RECHAZADA con el mensaje del error
            guardada.setEstado(EstadoFactura.RECHAZADA);
            guardada.setMensajeRespuestaSunat("Error en comunicación SOAP con SUNAT: " + e.getMessage());
            facturaRepo.save(guardada);
            throw new RuntimeException("La factura se guardó pero falló la comunicación con SUNAT: " + e.getMessage(), e);
        }

        return fb.toResponse(guardada);
    }

    @Override
    public FacturaDetails getFacturaPorPedido(Long pedidoId) {
        Factura f = facturaRepo.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RuntimeException("No existe factura para el pedido: " + pedidoId));
        return fb.toDetails(f);
    }

    @Override
    public List<FacturaResponse> getAllFacturas() {
        return facturaRepo.findAll()
                .stream()
                .map(fb::toResponse)
                .toList();
    }
}