package cibertec.pe.conductor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IConductorRepository extends JpaRepository<Conductor, Long> {
    List<Conductor> findByEstado(EstadoConductor estado);
}
