package cibertec.pe.sunat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDocumentoTributario extends JpaRepository<DocumentoTributario, Long> {
}
