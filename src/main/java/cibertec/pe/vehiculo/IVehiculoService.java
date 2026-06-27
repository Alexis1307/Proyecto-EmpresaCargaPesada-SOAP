package cibertec.pe.vehiculo;

import jakarta.jws.WebService;

import java.util.List;

@WebService
public interface IVehiculoService {
    List<Vehiculo> getAllVehiculo();
    List<Vehiculo> getVehiculosPorEstado(EstadoVehiculo estado);
    Vehiculo findVehiculo(Long id);
    Vehiculo createVehiculo(Vehiculo vehiculo);
    Vehiculo updateVehiculo(Long id, Vehiculo vehiculo);
    Vehiculo deleteVehiculo(Long id);
}
