package cibertec.pe.vehiculo;

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
@Table(name = "tbl_vehiculo")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Vehiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    private String placa;
    @NotBlank(message = "La marca es obligatoria")
    private String marca;
    @NotBlank(message = "La capacidad de carga es obligatoria")
    private Double capacidadCargaKg;

    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipoVehiculo;

    @Enumerated(EnumType.STRING)
    private EstadoVehiculo estado;

    @XmlTransient
    @OneToMany(mappedBy = "vehiculo", fetch = FetchType.EAGER)
    private List<Pedido> pedidos = new ArrayList<>();
}
