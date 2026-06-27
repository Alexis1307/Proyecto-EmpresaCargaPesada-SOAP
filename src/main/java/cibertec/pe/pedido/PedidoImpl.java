package cibertec.pe.pedido;

import cibertec.pe.conductor.Conductor;
import cibertec.pe.conductor.EstadoConductor;
import cibertec.pe.conductor.IConductorRepository;
import cibertec.pe.pedido.dto.PedidoDetails;
import cibertec.pe.pedido.dto.PedidoRequest;
import cibertec.pe.pedido.dto.PedidoResponse;
import cibertec.pe.pedido.dto.PedidoUpdateRequest;
import cibertec.pe.cliente.Cliente;
import cibertec.pe.cliente.IClienteRepository;
import cibertec.pe.vehiculo.EstadoVehiculo;
import cibertec.pe.vehiculo.IVehiculoRepository;
import cibertec.pe.vehiculo.Vehiculo;
import jakarta.jws.WebService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@WebService
@Component
public class PedidoImpl implements IPedidoService {

    private final IPedidoRepository repo;
    private final IClienteRepository clienteRepo;
    private final IVehiculoRepository vehiculoRepo;
    private final IConductorRepository conductorRepo;
    private final PedidoBussiness pb;

    public PedidoImpl(IPedidoRepository repo,
                      PedidoBussiness pb,
                      IClienteRepository clienteRepo,
                      IVehiculoRepository vehiculoRepo,
                      IConductorRepository conductorRepo) {

        this.repo = repo;
        this.pb = pb;
        this.clienteRepo = clienteRepo;
        this.vehiculoRepo = vehiculoRepo;
        this.conductorRepo = conductorRepo;
    }

    @Override
    public List<PedidoResponse> getAllPedido() {
        return repo.findAll().stream().map(pb::convertirListado).toList();
    }

    @Override
    public List<PedidoResponse> getAllPedidoPorEstado(EstadoEnvio estado) {
        return repo.findByEstado(estado).stream().map(pb::convertirListado).toList();
    }

    @Override
    public List<PedidoResponse> getAllPedidosMenosCancelado() {
        return repo.findAll().stream()
                .filter(p -> p.getEstado() != EstadoEnvio.CANCELADO)
                .map(pb::convertirListado)
                .toList();
    }

    @Override
    public PedidoDetails findPedido(Long id) {
        Pedido pedido = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        return pb.convertirRespuesta(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse createPedido(PedidoRequest request) {

        if (request == null) throw new RuntimeException("Los datos no pueden estar vacios");

        Pedido p = new Pedido();

        p.setFechaPedido(LocalDateTime.now());
        p.setOrigen(request.getOrigen());
        p.setDestino(request.getDestino());
        p.setDescripcionCarga(request.getDescripcionCarga());
        p.setPesoCarga(request.getPesoCarga());
        p.setCostoEnvio(pb.calcularCostoEnvio(request.getPesoCarga()));

        Cliente c = clienteRepo.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        p.setCliente(c);

        asignarRecursos(p);

        Pedido guardado = repo.save(p);
        return pb.convertirListado(guardado);
    }

    @Override
    public String updatePedido(Long id, PedidoUpdateRequest request) {

        Pedido p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Cliente cliente = clienteRepo.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        p.setCliente(cliente);
        p.setOrigen(request.getOrigen());
        p.setDestino(request.getDestino());
        p.setDescripcionCarga(request.getDescripcionCarga());
        p.setPesoCarga(request.getPesoCarga());
        p.setCostoEnvio(pb.calcularCostoEnvio(request.getPesoCarga()));

        repo.save(p);
        return "Pedido actualizado";
    }

    @Override
    public String updateStateEnRuta(Long id) {
        Pedido p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        p.setEstado(EstadoEnvio.EN_RUTA);
        repo.save(p);
        return "Estado cambiado a En Ruta";
    }

    @Override
    public String updateStateEntregado(Long id) {
        Pedido p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        p.setEstado(EstadoEnvio.ENTREGADO);
        repo.save(p);
        return "Estado cambiado a Entregado";
    }

    @Override
    public String cancelarPedido(Long id) {
        Pedido p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        p.setEstado(EstadoEnvio.CANCELADO);
        repo.save(p);
        return "Pedido Cancelado correctamente";
    }

    @Override
    public String asignarManual(Long pedidoId, Long conductorId, Long vehiculoId) {

        Pedido p = repo.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        Conductor conductor = conductorRepo.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

        Vehiculo vehiculo = vehiculoRepo.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));

        p.setConductor(conductor);
        p.setVehiculo(vehiculo);
        p.setEstado(EstadoEnvio.EN_RUTA);

        repo.save(p);

        return "Pedido asignado manualmente";
    }

    private void asignarRecursos(Pedido p) {

        List<Vehiculo> vehiculos = vehiculoRepo.findByEstado(EstadoVehiculo.ACTIVO);
        List<Conductor> conductores = conductorRepo.findByEstado(EstadoConductor.ACTIVO);

        if (vehiculos.isEmpty() || conductores.isEmpty()) {
            p.setEstado(EstadoEnvio.PENDIENTE_ASIGNACION);
            return;
        }

        Vehiculo vehiculoDisponible = vehiculos.stream()
                .filter(v -> v.getCapacidadCargaKg() >= p.getPesoCarga())
                .findFirst()
                .orElse(null);

        if (vehiculoDisponible == null) {
            p.setEstado(EstadoEnvio.PENDIENTE_ASIGNACION);
            return;
        }

        Conductor conductorDisponible = conductores.get(0);

        p.setVehiculo(vehiculoDisponible);
        p.setConductor(conductorDisponible);
        p.setEstado(EstadoEnvio.EN_RUTA);
    }

    @Override
    public String asignarPedidoPendiente(Long pedidoId, Long conductorId, Long vehiculoId) {

        Pedido p = repo.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (p.getEstado() != EstadoEnvio.PENDIENTE_ASIGNACION) {
            throw new RuntimeException("El pedido no está pendiente de asignación");
        }

        Conductor c = conductorRepo.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

        Vehiculo v = vehiculoRepo.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));

        if (v.getCapacidadCargaKg() < p.getPesoCarga()) {
            throw new RuntimeException("El vehiculo no soporta la carga");
        }

        p.setConductor(c);
        p.setVehiculo(v);
        p.setEstado(EstadoEnvio.EN_RUTA);

        repo.save(p);

        return "Pedido asignado correctamente";
    }
}