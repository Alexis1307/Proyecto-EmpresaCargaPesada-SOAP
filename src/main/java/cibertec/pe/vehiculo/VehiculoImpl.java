package cibertec.pe.vehiculo;

import jakarta.jws.WebService;
import org.springframework.stereotype.Component;

import java.util.List;

@WebService
@Component
public class VehiculoImpl implements IVehiculoService{

    private IVehiculoRepository repo;

    public VehiculoImpl(IVehiculoRepository repo){
        this.repo = repo;
    }

    @Override
    public List<Vehiculo> getAllVehiculo() {
        return repo.findAll();
    }

    @Override
    public List<Vehiculo> getVehiculosPorEstado(EstadoVehiculo estado) {
        return repo.findByEstado(estado);
    }

    @Override
    public Vehiculo findVehiculo(Long id) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        return repo.findById(id).orElseThrow(()->
                new RuntimeException("Vehiculo no encontrado"));
    }

    @Override
    public Vehiculo createVehiculo(Vehiculo vehiculo) {
        if (vehiculo == null) throw new RuntimeException("Los datos no pueden estar vacios");
        vehiculo.setEstado(EstadoVehiculo.ACTIVO);
        return repo.save(vehiculo);
    }

    @Override
    public Vehiculo updateVehiculo(Long id, Vehiculo vehiculo) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        if (vehiculo == null) throw new RuntimeException("Los datos no pueden estar vacios");

        Vehiculo v = repo.findById(id).orElseThrow(()->
                new RuntimeException("Vehiculo no encontrado"));

        v.setPlaca(vehiculo.getPlaca());
        v.setMarca(vehiculo.getMarca());
        v.setTipoVehiculo(vehiculo.getTipoVehiculo());
        v.setCapacidadCargaKg(vehiculo.getCapacidadCargaKg());

        return repo.save(v);
    }

    @Override
    public Vehiculo deleteVehiculo(Long id) {
        Vehiculo v = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));

        v.setEstado(EstadoVehiculo.INACTIVO);
        return repo.save(v);
    }
}
