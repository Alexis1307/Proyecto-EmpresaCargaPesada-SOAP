package cibertec.pe.factura.dto;

import cibertec.pe.factura.EstadoFactura;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FacturaDetails {

    private Long id;
    private String numeroFactura;
    private LocalDateTime fechaEmision;

    private Double subtotal;
    private Double igv;
    private Double total;

    private Long pedidoId;
    private String origen;
    private String destino;
    private Double pesoCarga;
    private String estadoEnvio;

    private String clienteNombre;
    private String clienteDocumento;

    private EstadoFactura estadoFactura;
    private String codigoRespuestaSunat;
    private String mensajeRespuestaSunat;
}
