package cibertec.pe.pedido;

import cibertec.pe.pedido.dto.PedidoDetails;
import cibertec.pe.pedido.dto.PedidoRequest;
import cibertec.pe.pedido.dto.PedidoResponse;
import cibertec.pe.pedido.dto.PedidoUpdateRequest;
import jakarta.jws.WebService;

import java.util.List;

@WebService
public interface IPedidoService {
    List<PedidoResponse> getAllPedido();
    List<PedidoResponse> getAllPedidoPorEstado(EstadoEnvio estado);
    List<PedidoResponse> getAllPedidosMenosCancelado();
    PedidoDetails findPedido(Long id);
    PedidoResponse createPedido(PedidoRequest request);
    String updatePedido(Long id, PedidoUpdateRequest request);
    String updateStateEnRuta(Long id);
    String updateStateEntregado(Long id);
    String cancelarPedido(Long id);
    String asignarManual(Long pedidoId, Long conductorId, Long vehiculoId);
    String asignarPedidoPendiente(Long pedidoId, Long conductorId, Long vehiculoId);
}
