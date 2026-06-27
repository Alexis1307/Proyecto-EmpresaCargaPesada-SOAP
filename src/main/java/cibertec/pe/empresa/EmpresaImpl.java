package cibertec.pe.empresa;

import jakarta.jws.WebService;
import org.springframework.stereotype.Component;


@WebService
@Component
public class EmpresaImpl implements IEmpresaService{

    private IEmpresaRepository repo;

    public EmpresaImpl(IEmpresaRepository repo){
        this.repo = repo;
    }

    @Override
    public Empresa getEmpresa(Long id) {
        if (id <= 0) throw new RuntimeException("El id debe ser mayor a 0");
        return repo.findById(id).orElseThrow(()->
                new RuntimeException("Empresa no encontrada"));
    }

    @Override
    public Empresa createEmpresa(Empresa empresa) {
        if (empresa == null) throw new RuntimeException("Los datos no pueden estar vacios");
        return repo.save(empresa);
    }

    @Override
    public Empresa updateEmpresa(Long id, Empresa empresa) {

        if (id <= 0)
            throw new RuntimeException("El id debe ser mayor a 0");

        if (empresa == null)
            throw new RuntimeException("Los datos no pueden estar vacíos");

        Empresa e = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        e.setRuc(empresa.getRuc());
        e.setRazonSocial(empresa.getRazonSocial());
        e.setDireccion(empresa.getDireccion());
        e.setTelefono(empresa.getTelefono());

        e.setCorreo(empresa.getCorreo());
        e.setUbigeo(empresa.getUbigeo());
        e.setDepartamento(empresa.getDepartamento());
        e.setProvincia(empresa.getProvincia());
        e.setDistrito(empresa.getDistrito());
        e.setCodigoEstablecimiento(empresa.getCodigoEstablecimiento());

        return repo.save(e);
    }
}
