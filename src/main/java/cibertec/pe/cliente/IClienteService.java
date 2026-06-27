package cibertec.pe.cliente;

import jakarta.jws.WebService;

import java.util.List;

@WebService
public interface IClienteService {
    List<Cliente> getAllClientes();
    List<Cliente> getClientesPorEstado(EstadoCliente estado);
    Cliente findCliente(Long id);
    Cliente createCliente(Cliente cliente);
    Cliente updateCliente(Long id, Cliente cliente);
    Cliente deleteCliente(Long id);
}
