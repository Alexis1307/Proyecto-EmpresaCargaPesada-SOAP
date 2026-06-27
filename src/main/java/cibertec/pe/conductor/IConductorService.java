package cibertec.pe.conductor;

import jakarta.jws.WebService;

import java.util.List;

@WebService
public interface IConductorService {
    List<Conductor> getAllConductor();
    List<Conductor> getConductoresPorEstado(EstadoConductor estado);
    Conductor findConductor(Long id);
    Conductor createConductor(Conductor conductor);
    Conductor updateConductor(Long id, Conductor conductor);
    Conductor deleteConductor(Long id);
}
