package cibertec.pe.factura;

import cibertec.pe.sunat.DocumentoTributario;
import cibertec.pe.pedido.Pedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_factura")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroFactura;
    private LocalDateTime fechaEmision;
    private Double subtotal;
    private Double igv;
    private Double total;

    private String codigoRespuestaSunat;

    @Column(columnDefinition = "TEXT")
    private String mensajeRespuestaSunat;

    private String hash;

    @Enumerated(EnumType.STRING)
    private EstadoFactura estado;

    @OneToOne
    @JoinColumn(name = "pedido_id", unique = true)
    private Pedido pedido;

    @OneToOne(mappedBy = "factura", cascade = CascadeType.ALL)
    private DocumentoTributario documentoTributario;
}