package cibertec.pe.empresa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_empresa")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ruc;
    private String razonSocial;
    private String direccion;
    private String telefono;
    private String correo;
    private String ubigeo;
    private String departamento;
    private String provincia;
    private String distrito;
    private String codigoEstablecimiento;
}
