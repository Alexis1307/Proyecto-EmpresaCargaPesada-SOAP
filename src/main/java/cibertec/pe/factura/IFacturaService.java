package cibertec.pe.factura;

import cibertec.pe.factura.dto.FacturaDetails;
import cibertec.pe.factura.dto.FacturaRequest;
import cibertec.pe.factura.dto.FacturaResponse;
import jakarta.jws.WebService;

import java.util.List;

@WebService
public interface IFacturaService {
    FacturaResponse generarFactura(FacturaRequest request);
    FacturaDetails getFacturaPorPedido(Long pedidoId);
    List<FacturaResponse> getAllFacturas();
}
