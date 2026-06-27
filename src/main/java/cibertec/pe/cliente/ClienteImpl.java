package cibertec.pe.cliente;

import jakarta.jws.WebService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@WebService
@Component
public class ClienteImpl implements IClienteService{

    private IClienteRepository repo;

    public ClienteImpl(IClienteRepository repo){
        this.repo = repo;
    }

    @Override
    public List<Cliente> getAllClientes() {
        return repo.findAll();
    }

    @Override
    public List<Cliente> getClientesPorEstado(EstadoCliente estado) {
        return repo.findByEstado(estado);
    }

    @Override
    public Cliente findCliente(Long id) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        return repo.findById(id).orElseThrow(()->
                new RuntimeException("Cliente no encontrado"));
    }

    @Override
    public Cliente createCliente(Cliente cliente) {
        cliente.setEstado(EstadoCliente.ACTIVO);
        return repo.save(cliente);
    }

    @Override
    public Cliente updateCliente(Long id, Cliente cliente) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        if (cliente == null) throw new RuntimeException("Los datos no pueden estar vacios");

        Cliente c = repo.findById(id).orElseThrow(()->
                new RuntimeException("Cliente no encontrado"));

        c.setRudDni(cliente.getRudDni());
        c.setNombreRazonSocial(cliente.getNombreRazonSocial());
        c.setDireccion(cliente.getDireccion());
        c.setTelefono(cliente.getTelefono());

        return repo.save(c);
    }

    @Override
    public Cliente deleteCliente(Long id) {
        Cliente c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        c.setEstado(EstadoCliente.INACTIVO);
        return repo.save(c);
    }
}
