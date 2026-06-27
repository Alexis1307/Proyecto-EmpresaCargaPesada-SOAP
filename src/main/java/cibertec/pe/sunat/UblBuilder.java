package cibertec.pe.sunat;

import cibertec.pe.cliente.Cliente;
import cibertec.pe.empresa.Empresa;
import cibertec.pe.factura.Factura;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Construye el XML UBL 2.1 de una Factura electrónica según el estándar de SUNAT Perú.
 *
 * Adaptado al contexto de TRANSPORTE DE CARGA PESADA:
 * - El servicio facturado es el "SERVICIO DE TRANSPORTE DE CARGA" del Pedido.
 * - Los montos se toman de la Factura (subtotal, igv, total).
 * - El cliente se obtiene desde factura.getPedido().getCliente().
 * - La descripción incluye origen, destino y peso de carga del Pedido.
 */
@Service
public class UblBuilder {

    private static final String XMLNS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String XMLNS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String XMLNS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String XMLNS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String XMLNS_SAC = "urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1";
    private static final String XMLNS_DS = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Construye el Document XML UBL 2.1 listo para ser firmado.
     *
     * @param empresa La empresa emisora (extraída de la BD o application.properties).
     * @param factura La factura con montos calculados y su Pedido asociado.
     * @return Document DOM con la estructura completa de la factura electrónica.
     */
    public Document construirFacturaXml(Empresa empresa, Factura factura) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Elemento raíz <Invoice>
        Element invoice = doc.createElementNS(XMLNS_INVOICE, "Invoice");
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", XMLNS_CAC);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", XMLNS_CBC);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", XMLNS_EXT);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:sac", XMLNS_SAC);
        invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ds", XMLNS_DS);
        doc.appendChild(invoice);

        // Bloque obligatorio para la firma digital (se llenará en FirmaDigitalService)
        Element ublExtensions = doc.createElementNS(XMLNS_EXT, "ext:UBLExtensions");
        Element ublExtension = doc.createElementNS(XMLNS_EXT, "ext:UBLExtension");
        Element extensionContent = doc.createElementNS(XMLNS_EXT, "ext:ExtensionContent");
        ublExtension.appendChild(extensionContent);
        ublExtensions.appendChild(ublExtension);
        invoice.appendChild(ublExtensions);

        // Versiones y tipo de proceso
        append(doc, invoice, XMLNS_CBC, "cbc:UBLVersionID", "2.1");
        append(doc, invoice, XMLNS_CBC, "cbc:CustomizationID", "2.0");
        append(doc, invoice, XMLNS_CBC, "cbc:ProfileID", "0101");

        // Número de la factura (Serie-Correlativo, ej: F001-00000001)
        append(doc, invoice, XMLNS_CBC, "cbc:ID", factura.getNumeroFactura());

        // Fecha de emisión
        String fecha = factura.getFechaEmision().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        append(doc, invoice, XMLNS_CBC, "cbc:IssueDate", fecha);
        append(doc, invoice, XMLNS_CBC, "cbc:IssueTime", "00:00:00");

        // Tipo de comprobante: "01" = Factura (catálogo 01 SUNAT)
        Element tipo = append(doc, invoice, XMLNS_CBC, "cbc:InvoiceTypeCode", "01");
        tipo.setAttribute("listAgencyName", "PE:SUNAT");
        tipo.setAttribute("listName", "Tipo de Documento");
        tipo.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01");

        // Moneda: Soles peruanos
        Element moneda = append(doc, invoice, XMLNS_CBC, "cbc:DocumentCurrencyCode", "PEN");
        moneda.setAttribute("listID", "ISO 4217 Alpha");
        moneda.setAttribute("listName", "Currency");
        moneda.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");

        // Datos de la empresa emisora (tu empresa de transporte)
        buildSupplierParty(doc, invoice, empresa);

        // Datos del cliente del pedido
        buildCustomerParty(doc, invoice, factura);

        // Totales de IGV
        buildTaxTotal(doc, invoice, factura);

        // Totales monetarios de la factura
        buildLegalMonetaryTotal(doc, invoice, factura);

        // Línea del servicio de transporte facturado
        buildInvoiceLine(doc, invoice, factura);

        return doc;
    }

    /**
     * Datos del emisor (tu empresa de transporte de carga).
     */
    private void buildSupplierParty(Document doc, Element invoice, Empresa empresa) {
        Element accountingSupplierParty = doc.createElementNS(XMLNS_CAC, "cac:AccountingSupplierParty");
        Element party = doc.createElementNS(XMLNS_CAC, "cac:Party");

        // RUC de la empresa emisora
        Element partyIdentification = doc.createElementNS(XMLNS_CAC, "cac:PartyIdentification");
        Element id = append(doc, partyIdentification, XMLNS_CBC, "cbc:ID", empresa.getRuc());
        id.setAttribute("schemeID", "6");           // 6 = RUC en catálogo SUNAT
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        id.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
        party.appendChild(partyIdentification);

        // Razón social y dirección de la empresa
        Element partyLegalEntity = doc.createElementNS(XMLNS_CAC, "cac:PartyLegalEntity");
        append(doc, partyLegalEntity, XMLNS_CBC, "cbc:RegistrationName", empresa.getRazonSocial());

        Element registrationAddress = doc.createElementNS(XMLNS_CAC, "cac:RegistrationAddress");

        // Ubigeo: usar codigoEstablecimiento si existe, o "0000" por defecto
        String ubigeo = (empresa.getCodigoEstablecimiento() != null && !empresa.getCodigoEstablecimiento().isBlank())
            ? empresa.getCodigoEstablecimiento() : "0000";
        Element ubigeoEl = append(doc, registrationAddress, XMLNS_CBC, "cbc:ID", ubigeo);
        ubigeoEl.setAttribute("schemeAgencyName", "PE:INEI");
        ubigeoEl.setAttribute("schemeName", "Ubigeos");
        ubigeoEl.setAttribute("schemeID", "PE");

        append(doc, registrationAddress, XMLNS_CBC, "cbc:AddressTypeCode", "0000");

        if (empresa.getDistrito() != null && !empresa.getDistrito().isBlank()) {
            append(doc, registrationAddress, XMLNS_CBC, "cbc:District", empresa.getDistrito());
        }
        if (empresa.getProvincia() != null && !empresa.getProvincia().isBlank()) {
            append(doc, registrationAddress, XMLNS_CBC, "cbc:CityName", empresa.getProvincia());
        }
        if (empresa.getDepartamento() != null && !empresa.getDepartamento().isBlank()) {
            append(doc, registrationAddress, XMLNS_CBC, "cbc:CountrySubentity", empresa.getDepartamento());
        }

        Element country = doc.createElementNS(XMLNS_CAC, "cac:Country");
        Element countryCode = append(doc, country, XMLNS_CBC, "cbc:IdentificationCode", "PE");
        countryCode.setAttribute("listID", "ISO 3166-1");
        countryCode.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");
        countryCode.setAttribute("listName", "Country");
        registrationAddress.appendChild(country);

        partyLegalEntity.appendChild(registrationAddress);
        party.appendChild(partyLegalEntity);
        accountingSupplierParty.appendChild(party);
        invoice.appendChild(accountingSupplierParty);
    }

    /**
     * Datos del receptor (cliente del pedido de transporte).
     * ADAPTACIÓN: Se obtiene del cliente asociado al pedido de la factura.
     */
    private void buildCustomerParty(Document doc, Element invoice, Factura factura) {
        Element accountingCustomerParty = doc.createElementNS(XMLNS_CAC, "cac:AccountingCustomerParty");
        Element party = doc.createElementNS(XMLNS_CAC, "cac:Party");

        // Valores por defecto (para datos incompletos en pruebas Beta)
        String docCliente = "20000000001";
        String nombreCliente = "CLIENTE GENERAL";

        // Obtener datos reales del cliente asociado al pedido
        if (factura.getPedido() != null && factura.getPedido().getCliente() != null) {
            Cliente cliente = factura.getPedido().getCliente();
            if (cliente.getRudDni() != null && !cliente.getRudDni().isBlank()) {
                docCliente = cliente.getRudDni();
            }
            if (cliente.getNombreRazonSocial() != null && !cliente.getNombreRazonSocial().isBlank()) {
                nombreCliente = cliente.getNombreRazonSocial();
            }
        }

        // Detectar si es RUC (11 dígitos) o DNI (8 dígitos) para el schemeID correcto
        // schemeID "6" = RUC, schemeID "1" = DNI, según catálogo 06 de SUNAT
        String schemeId = (docCliente.length() == 11) ? "6" : "1";

        Element partyIdentification = doc.createElementNS(XMLNS_CAC, "cac:PartyIdentification");
        Element id = append(doc, partyIdentification, XMLNS_CBC, "cbc:ID", docCliente);
        id.setAttribute("schemeID", schemeId);
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        id.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
        party.appendChild(partyIdentification);

        Element partyLegalEntity = doc.createElementNS(XMLNS_CAC, "cac:PartyLegalEntity");
        append(doc, partyLegalEntity, XMLNS_CBC, "cbc:RegistrationName", nombreCliente);
        party.appendChild(partyLegalEntity);

        accountingCustomerParty.appendChild(party);
        invoice.appendChild(accountingCustomerParty);
    }

    /**
     * Total de IGV de la factura.
     */
    private void buildTaxTotal(Document doc, Element invoice, Factura factura) {
        Element taxTotal = doc.createElementNS(XMLNS_CAC, "cac:TaxTotal");
        append(doc, taxTotal, XMLNS_CBC, "cbc:TaxAmount", format(factura.getIgv()))
            .setAttribute("currencyID", "PEN");

        Element taxSubtotal = doc.createElementNS(XMLNS_CAC, "cac:TaxSubtotal");
        append(doc, taxSubtotal, XMLNS_CBC, "cbc:TaxableAmount", format(factura.getSubtotal()))
            .setAttribute("currencyID", "PEN");
        append(doc, taxSubtotal, XMLNS_CBC, "cbc:TaxAmount", format(factura.getIgv()))
            .setAttribute("currencyID", "PEN");

        Element taxCategory = doc.createElementNS(XMLNS_CAC, "cac:TaxCategory");
        append(doc, taxCategory, XMLNS_CBC, "cbc:ID", "S");
        append(doc, taxCategory, XMLNS_CBC, "cbc:Percent", "18.00");

        Element taxScheme = doc.createElementNS(XMLNS_CAC, "cac:TaxScheme");
        Element idScheme = append(doc, taxScheme, XMLNS_CBC, "cbc:ID", "1000");
        idScheme.setAttribute("schemeName", "Codigo de tributos");
        idScheme.setAttribute("schemeAgencyName", "PE:SUNAT");
        idScheme.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo05");
        append(doc, taxScheme, XMLNS_CBC, "cbc:Name", "IGV");
        append(doc, taxScheme, XMLNS_CBC, "cbc:TaxTypeCode", "VAT");

        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);
        taxTotal.appendChild(taxSubtotal);
        invoice.appendChild(taxTotal);
    }

    /**
     * Totales monetarios de la factura (subtotal, total con IGV, monto a pagar).
     */
    private void buildLegalMonetaryTotal(Document doc, Element invoice, Factura factura) {
        Element total = doc.createElementNS(XMLNS_CAC, "cac:LegalMonetaryTotal");
        append(doc, total, XMLNS_CBC, "cbc:LineExtensionAmount", format(factura.getSubtotal()))
            .setAttribute("currencyID", "PEN");
        append(doc, total, XMLNS_CBC, "cbc:TaxInclusiveAmount", format(factura.getTotal()))
            .setAttribute("currencyID", "PEN");
        append(doc, total, XMLNS_CBC, "cbc:PayableAmount", format(factura.getTotal()))
            .setAttribute("currencyID", "PEN");
        invoice.appendChild(total);
    }

    /**
     * Línea de detalle: el servicio de transporte de carga del pedido.
     * ADAPTACIÓN: La descripción incluye origen, destino y peso del pedido.
     */
    private void buildInvoiceLine(Document doc, Element invoice, Factura factura) {
        Element line = doc.createElementNS(XMLNS_CAC, "cac:InvoiceLine");
        append(doc, line, XMLNS_CBC, "cbc:ID", "1");

        // Unidad de servicio: ZZ = Unidad mutuamente definida (para servicios)
        Element qty = append(doc, line, XMLNS_CBC, "cbc:InvoicedQuantity", "1");
        qty.setAttribute("unitCode", "ZZ");
        qty.setAttribute("unitCodeListID", "UN/ECE rec 20");
        qty.setAttribute("unitCodeListAgencyName", "United Nations Economic Commission for Europe");

        append(doc, line, XMLNS_CBC, "cbc:LineExtensionAmount", format(factura.getSubtotal()))
            .setAttribute("currencyID", "PEN");

        // Precio de venta con IGV incluido
        Element pricingReference = doc.createElementNS(XMLNS_CAC, "cac:PricingReference");
        Element altPrice = doc.createElementNS(XMLNS_CAC, "cac:AlternativeConditionPrice");
        append(doc, altPrice, XMLNS_CBC, "cbc:PriceAmount", format(factura.getTotal()))
            .setAttribute("currencyID", "PEN");
        Element priceTypeCode = append(doc, altPrice, XMLNS_CBC, "cbc:PriceTypeCode", "01");
        priceTypeCode.setAttribute("listName", "Tipo de Precio");
        priceTypeCode.setAttribute("listAgencyName", "PE:SUNAT");
        priceTypeCode.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo16");
        pricingReference.appendChild(altPrice);
        line.appendChild(pricingReference);

        // IGV de la línea
        Element taxTotal = doc.createElementNS(XMLNS_CAC, "cac:TaxTotal");
        append(doc, taxTotal, XMLNS_CBC, "cbc:TaxAmount", format(factura.getIgv()))
            .setAttribute("currencyID", "PEN");

        Element taxSubtotal = doc.createElementNS(XMLNS_CAC, "cac:TaxSubtotal");
        append(doc, taxSubtotal, XMLNS_CBC, "cbc:TaxableAmount", format(factura.getSubtotal()))
            .setAttribute("currencyID", "PEN");
        append(doc, taxSubtotal, XMLNS_CBC, "cbc:TaxAmount", format(factura.getIgv()))
            .setAttribute("currencyID", "PEN");

        Element taxCategory = doc.createElementNS(XMLNS_CAC, "cac:TaxCategory");
        append(doc, taxCategory, XMLNS_CBC, "cbc:Percent", "18.00");
        Element ex = append(doc, taxCategory, XMLNS_CBC, "cbc:TaxExemptionReasonCode", "10");
        ex.setAttribute("listName", "Afectacion del IGV");
        ex.setAttribute("listAgencyName", "PE:SUNAT");
        ex.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo07");

        Element taxScheme = doc.createElementNS(XMLNS_CAC, "cac:TaxScheme");
        Element idScheme = append(doc, taxScheme, XMLNS_CBC, "cbc:ID", "1000");
        idScheme.setAttribute("schemeName", "Codigo de tributos");
        idScheme.setAttribute("schemeAgencyName", "PE:SUNAT");
        idScheme.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo05");
        append(doc, taxScheme, XMLNS_CBC, "cbc:Name", "IGV");
        append(doc, taxScheme, XMLNS_CBC, "cbc:TaxTypeCode", "VAT");

        taxCategory.appendChild(taxScheme);
        taxSubtotal.appendChild(taxCategory);
        taxTotal.appendChild(taxSubtotal);
        line.appendChild(taxTotal);

        // ADAPTACIÓN: Descripción del servicio de transporte de carga
        Element item = doc.createElementNS(XMLNS_CAC, "cac:Item");
        String descripcion = buildDescripcionServicio(factura);
        append(doc, item, XMLNS_CBC, "cbc:Description", descripcion);
        line.appendChild(item);

        // Precio unitario (sin IGV)
        Element price = doc.createElementNS(XMLNS_CAC, "cac:Price");
        append(doc, price, XMLNS_CBC, "cbc:PriceAmount", format(factura.getSubtotal()))
            .setAttribute("currencyID", "PEN");
        line.appendChild(price);

        invoice.appendChild(line);
    }

    /**
     * Construye la descripción del servicio usando los datos del pedido de transporte.
     */
    private String buildDescripcionServicio(Factura factura) {
        if (factura.getPedido() == null) {
            return "SERVICIO DE TRANSPORTE DE CARGA";
        }

        StringBuilder desc = new StringBuilder("SERVICIO DE TRANSPORTE DE CARGA PESADA");

        if (factura.getPedido().getOrigen() != null) {
            desc.append(" - ORIGEN: ").append(factura.getPedido().getOrigen().toUpperCase());
        }
        if (factura.getPedido().getDestino() != null) {
            desc.append(" - DESTINO: ").append(factura.getPedido().getDestino().toUpperCase());
        }
        if (factura.getPedido().getPesoCarga() != null) {
            desc.append(" - PESO: ").append(factura.getPedido().getPesoCarga()).append(" KG");
        }

        return desc.toString();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Element append(Document doc, Element parent, String ns, String tag, String value) {
        Element e = doc.createElementNS(ns, tag);
        if (value != null && !value.isBlank()) {
            e.setTextContent(value);
        }
        parent.appendChild(e);
        return e;
    }

    private String format(Double value) {
        return value == null ? "0.00" : String.format(Locale.US, "%.2f", value);
    }
}
