package cibertec.pe.conductor;

import jakarta.jws.WebService;
import org.springframework.stereotype.Component;

import java.util.List;

@WebService
@Component
public class ConductorImpl implements IConductorService{

    private IConductorRepository repo;

    public ConductorImpl(IConductorRepository repo){
        this.repo = repo;
    }

    @Override
    public List<Conductor> getAllConductor() {
        return repo.findAll();
    }

    @Override
    public List<Conductor> getConductoresPorEstado(EstadoConductor estado) {
        return repo.findByEstado(estado);
    }

    @Override
    public Conductor findConductor(Long id) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        return repo.findById(id).orElseThrow(()->
                new RuntimeException("Conductor no encontrado"));
    }

    @Override
    public Conductor createConductor(Conductor conductor) {
        if (conductor == null) throw new RuntimeException("Los datos no pueden estar vacios");
        conductor.setEstado(EstadoConductor.ACTIVO);
        return repo.save(conductor);
    }

    @Override
    public Conductor updateConductor(Long id, Conductor conductor) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        if (conductor == null) throw new RuntimeException("Los datos no pueden estar vacios");

        Conductor c = repo.findById(id).orElseThrow(()->
                new RuntimeException("Conductor no encontrado"));

        c.setNombre(conductor.getNombre());
        c.setDni(conductor.getDni());
        c.setLicencia(conductor.getLicencia());
        c.setTelefono(conductor.getTelefono());

        return repo.save(c);
    }

    @Override
    public Conductor deleteConductor(Long id) {
        Conductor c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

        c.setEstado(EstadoConductor.INACTIVO);
        return repo.save(c);
    }
}
