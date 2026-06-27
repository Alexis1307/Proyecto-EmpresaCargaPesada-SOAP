package cibertec.pe.pedido.dto;

import cibertec.pe.pedido.EstadoEnvio;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoDetails {
    //Pedido
    private Long pedidoId;
    private LocalDateTime fechaPedido;
    private String origen;
    private String destino;
    private String descripcionCarga;
    private Double pesoCarga;
    private EstadoEnvio estado;

    //Cliente
    private Long clienteId;
    private String rudDniCliente;
    private String nombreRazonSocialCliente;
    private String direccionCliente;
    private String telefonoCliente;

    //Conductor
    private Long conductorId;
    private String nombreConductor;
    private String dniConductor;
    private String licenciaConductor;
    private String telefonoConductor;

    //Vehiculo
    private Long vehiculoId;
    private String placaVehiculo;
    private String marcaVehiculo;
    private Double capacidadCargaVehiculo;
    private String tipoVehiculo;
}
