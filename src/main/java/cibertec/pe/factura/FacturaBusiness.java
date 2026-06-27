package cibertec.pe.factura;

import cibertec.pe.factura.dto.FacturaDetails;
import cibertec.pe.factura.dto.FacturaResponse;
import org.springframework.stereotype.Component;

@Component
public class FacturaBusiness {

    public FacturaResponse toResponse(Factura f) {

        FacturaResponse dto = new FacturaResponse();

        dto.setId(f.getId());
        dto.setNumeroFactura(f.getNumeroFactura());
        dto.setFechaEmision(f.getFechaEmision());

        dto.setSubtotal(f.getSubtotal());
        dto.setIgv(f.getIgv());
        dto.setTotal(f.getTotal());

        dto.setPedidoId(f.getPedido().getId());
        dto.setClienteNombre(f.getPedido().getCliente().getNombreRazonSocial());

        dto.setEstadoFactura(f.getEstado());

        return dto;
    }

    public FacturaDetails toDetails(Factura f) {

        FacturaDetails dto = new FacturaDetails();

        dto.setId(f.getId());
        dto.setNumeroFactura(f.getNumeroFactura());
        dto.setFechaEmision(f.getFechaEmision());

        dto.setSubtotal(f.getSubtotal());
        dto.setIgv(f.getIgv());
        dto.setTotal(f.getTotal());

        dto.setPedidoId(f.getPedido().getId());
        dto.setOrigen(f.getPedido().getOrigen());
        dto.setDestino(f.getPedido().getDestino());
        dto.setPesoCarga(f.getPedido().getPesoCarga());
        dto.setEstadoEnvio(f.getPedido().getEstado().toString());

        dto.setClienteNombre(f.getPedido().getCliente().getNombreRazonSocial());
        dto.setClienteDocumento(f.getPedido().getCliente().getRudDni());

        return dto;
    }
}
