package cibertec.pe.factura;

import cibertec.pe.empresa.Empresa;
import cibertec.pe.factura.dto.FacturaDetails;
import cibertec.pe.factura.dto.FacturaRequest;
import cibertec.pe.factura.dto.FacturaResponse;
import cibertec.pe.pedido.EstadoEnvio;
import cibertec.pe.pedido.IPedidoRepository;
import cibertec.pe.pedido.Pedido;

import cibertec.pe.sunat.*;
import cibertec.pe.sunat.DocumentoTributario;
import jakarta.jws.WebService;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@WebService
@Component
public class FacturaImpl implements IFacturaService {

    private final IFacturaRepository facturaRepo;
    private final IPedidoRepository pedidoRepo;
    private final FacturaBusiness fb;

    // Inyección de la tubería de integración con SUNAT
    private final UblBuilder ublBuilder;
    private final FirmaDigitalService firmaDigitalService;
    private final ZipService zipService;
    private final SunatSoapService sunatSoapService;
    private final CdrService cdrService;
    private final IDocumentoTributario docTributarioRepo; // Tu repositorio para DocumentoTributario

    public FacturaImpl(IFacturaRepository facturaRepo,
                       IPedidoRepository pedidoRepo,
                       FacturaBusiness fb,
                       UblBuilder ublBuilder,
                       FirmaDigitalService firmaDigitalService,
                       ZipService zipService,
                       SunatSoapService sunatSoapService,
                       CdrService cdrService,
                       IDocumentoTributario docTributarioRepo) {
        this.facturaRepo = facturaRepo;
        this.pedidoRepo = pedidoRepo;
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

        // 1. Validaciones del Pedido
        Pedido pedido = pedidoRepo.findById(request.getPedidoId())
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != EstadoEnvio.ENTREGADO) {
            throw new RuntimeException("Solo se puede facturar pedidos ENTREGADOS");
        }

        facturaRepo.findByPedidoId(pedido.getId()).ifPresent(f -> {
            throw new RuntimeException("Este pedido ya tiene factura");
        });

        // 2. Instanciar y Calcular montos de la Factura
        Factura f = new Factura();
        f.setPedido(pedido);
        f.setFechaEmision(LocalDateTime.now());
        f.setEstado(EstadoFactura.EN_PROCESO);

        double subtotal = pedido.getPesoCarga() * 10;
        double igv = subtotal * 0.18;
        double total = subtotal + igv;

        f.setSubtotal(subtotal);
        f.setIgv(igv);
        f.setTotal(total);

        // Ajuste Obligatorio SUNAT: Serie de 4 caracteres y correlativo numérico de 8 dígitos
        // Usamos el ID del pedido rellenado con ceros a la izquierda para simular el correlativo único
        String correlativo = String.format("%08d", pedido.getId());
        f.setNumeroFactura("F001-" + correlativo);

        // Guardado inicial para obtener ID de la factura
        Factura guardada = facturaRepo.save(f);

        // 3. Simulación u obtención de datos de Emisor (Tu Empresa)
        // Lo ideal es jalarlo de la BD, aquí inicializamos uno con tus campos para la prueba
        Empresa miEmpresa = new Empresa();
        miEmpresa.setRuc("20123456789"); // Pon un RUC Beta válido para tus pruebas
        miEmpresa.setRazonSocial("TRANSPORTE S.A.C.");
        miEmpresa.setDireccion("Av. Larco 123");
        miEmpresa.setDepartamento("LA LIBERTAD");
        miEmpresa.setProvincia("TRUJILLO");
        miEmpresa.setDistrito("TRUJILLO");
        miEmpresa.setCodigoEstablecimiento("0000");

        // 4. FLUJO DE ORQUESTRACIÓN SOAP - SUNAT
        try {
            // A. Construir el XML Estructurado (DOM Document)
            Document docXml = ublBuilder.construirFacturaXml(miEmpresa, guardada);

            // Convertir el Document DOM a String puro para guardarlo y procesarlo
            StringWriter writer = new StringWriter();
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(docXml), new StreamResult(writer));
            String xmlStringPuro = writer.toString();

            // B. Firmar Digitalmente el XML
            // Ajustar ruta de tu llave privada .pfx y clave correspondientes
            String rutaCert = "src/main/resources/certificado.pfx";
            String claveCert = "123456";

            byte[] xmlFirmadoBytes = firmaDigitalService.firmarDocumentoXml(xmlStringPuro, rutaCert, claveCert);
            String xmlFirmadoString = new String(xmlFirmadoBytes, java.nio.charset.StandardCharsets.UTF_8);

            // C. Comprimir el archivo firmado a formato ZIP en memoria
            byte[] zipEnvioBytes = zipService.comprimirXmlEnMemoria(xmlFirmadoBytes, miEmpresa, guardada);

            // D. Enviar el ZIP mediante SOAP a los servidores de SUNAT
            String nombreZip = miEmpresa.getRuc() + "-01-" + guardada.getNumeroFactura() + ".zip";
            byte[] cdrZipBytes = sunatSoapService.sendBill(nombreZip, zipEnvioBytes);

            // E. Descomprimir y Analizar la Constancia de Recepción (CDR)
            Map<String, String> respuestaSunat = cdrService.procesarCdr(cdrZipBytes);
            String codigoCdr = respuestaSunat.get("codigo");
            String mensajeCdr = respuestaSunat.get("mensaje");

            // 5. Actualizar el estado final de la Factura según SUNAT
            guardada.setCodigoRespuestaSunat(codigoCdr);
            guardada.setMensajeRespuestaSunat(mensajeCdr);

            if ("0".equals(codigoCdr)) {
                guardada.setEstado(EstadoFactura.ACEPTADA); // Cambia según tu Enum de estados reales
            } else {
                guardada.setEstado(EstadoFactura.RECHAZADA);
            }
            facturaRepo.save(guardada);

            // 6. Almacenar el Historial en la Entidad DocumentoTributario
           DocumentoTributario docTributario = new DocumentoTributario();
            docTributario.setFactura(guardada);
            docTributario.setXmlGenerado(xmlStringPuro);
            docTributario.setXmlFirmado(xmlFirmadoString);
            docTributario.setCdrZip(cdrZipBytes);
            docTributario.setCodigoRespuesta(codigoCdr);
            docTributario.setEstadoSunat(guardada.getEstado().name());
            docTributario.setFechaEnvio(LocalDateTime.now());
            docTributario.setHashFirma(guardada.getHash()); // Si extraes el DigestValue puedes mapearlo aquí

            docTributarioRepo.save(docTributario);

        } catch (Exception e) {
            // Manejo de contingencia por si se cae el servidor de SUNAT u ocurre un error de red
            guardada.setEstado(EstadoFactura.RECHAZADA);
            guardada.setMensajeRespuestaSunat("Error en comunicación SOAP: " + e.getMessage());
            facturaRepo.save(guardada);
            throw new RuntimeException("La factura se guardó pero falló la comunicación con SUNAT: " + e.getMessage(), e);
        }

        return fb.toResponse(guardada);
    }

    @Override
    public FacturaDetails getFacturaPorPedido(Long pedidoId) {
        Factura f = facturaRepo.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RuntimeException("No existe factura para este pedido"));
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