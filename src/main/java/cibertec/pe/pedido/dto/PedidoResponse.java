package cibertec.pe.pedido.dto;

import cibertec.pe.pedido.EstadoEnvio;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PedidoResponse {
    private Long id;
    private String nombreORazonSocialCliente;
    private boolean asignado;
    private boolean facturable;
    private String origen;
    private String destino;
    private EstadoEnvio estado;
}

