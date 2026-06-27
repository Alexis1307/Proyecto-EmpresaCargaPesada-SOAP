package cibertec.pe.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PedidoRequest {
    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;
    @NotBlank(message = "El origen es obligatorio")
    private String origen;
    @NotBlank(message = "El destino es obligatorio")
    private String destino;
    @NotBlank(message = "La descripcion de la carga obligatoria")
    private String descripcionCarga;
    @NotNull(message = "El peso es obligatorio")
    private Double pesoCarga;
}
