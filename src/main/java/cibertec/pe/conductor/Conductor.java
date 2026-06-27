package cibertec.pe.conductor;

import cibertec.pe.pedido.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.xml.bind.annotation.XmlTransient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_conductor")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Conductor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "EL nombre es obligatorio")
    private String nombre;
    @NotBlank(message = "EL DNI es obligatorio")
    private String dni;
    @NotBlank(message = "La licencia es obligatoria")
    private String licencia;
    @NotBlank(message = "EL telefono es obligatorio")
    private String telefono;

    @Enumerated(EnumType.STRING)
    private EstadoConductor estado;

    @XmlTransient
    @OneToMany(mappedBy = "conductor", fetch = FetchType.EAGER)
    private List<Pedido> pedidos = new ArrayList<>();
}
