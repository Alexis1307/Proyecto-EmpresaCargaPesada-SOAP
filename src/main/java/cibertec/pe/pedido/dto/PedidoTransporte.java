package cibertec.pe.pedido.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PedidoTransporte {
    private Long conductorId;
    private Long vehiculoId;
}
