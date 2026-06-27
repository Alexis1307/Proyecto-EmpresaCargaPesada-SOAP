package cibertec.pe.cliente;

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
@Table(name = "tbl_cliente")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "EL RUC o DNI son obligatorios")
    private String rudDni;
    @NotBlank(message = "EL nombre o Razon Social son obligatorios")
    private String nombreRazonSocial;
    @NotBlank(message = "La direccion es obligatoria")
    private String direccion;
    @NotBlank(message = "EL telefono es obligatorio")
    private String telefono;

    @Enumerated(EnumType.STRING)
    private EstadoCliente estado;

    @XmlTransient
    @OneToMany(mappedBy = "cliente", fetch = FetchType.EAGER)
    private List<Pedido> pedidos = new ArrayList<>();
}
