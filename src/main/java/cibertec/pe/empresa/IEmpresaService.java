package cibertec.pe.empresa;

import jakarta.jws.WebService;

@WebService
public interface IEmpresaService {
    Empresa getEmpresa(Long id);
    Empresa createEmpresa(Empresa empresa);
    Empresa updateEmpresa(Long id, Empresa empresa);
}
