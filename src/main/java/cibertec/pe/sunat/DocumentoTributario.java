package cibertec.pe.sunat;

import cibertec.pe.factura.Factura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DocumentoTributario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String xmlGenerado;

    private String hashFirma;
    private LocalDateTime fechaEnvio;
    private String estadoSunat;
    private String codigoRespuesta;

    @Lob
    private String xmlFirmado;
    @Lob
    private byte[] cdrZip;

    @OneToOne
    private Factura factura;
}
