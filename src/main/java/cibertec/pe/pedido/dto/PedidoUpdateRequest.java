package cibertec.pe.pedido.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoUpdateRequest {
    private Long clienteId;
    private String origen;
    private String destino;
    private String descripcionCarga;
    private Double pesoCarga;
}
