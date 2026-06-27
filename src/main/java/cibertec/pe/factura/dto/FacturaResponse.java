package cibertec.pe.factura.dto;

import cibertec.pe.factura.EstadoFactura;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class FacturaResponse {
    private Long id;
    private String numeroFactura;
    private LocalDateTime fechaEmision;

    private Double subtotal;
    private Double igv;
    private Double total;

    private Long pedidoId;
    private String clienteNombre;
    private EstadoFactura estadoFactura;
}
