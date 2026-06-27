package cibertec.pe.factura;

import cibertec.pe.factura.dto.FacturaRequest;
import cibertec.pe.factura.dto.FacturaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
public class FacturaTestController {

    private final IFacturaService facturaService;

    // Spring Boot inyectará automáticamente tu componente FacturaImpl
    public FacturaTestController(IFacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @PostMapping("/generar")
    public ResponseEntity<FacturaResponse> probarFacturacion(@RequestBody FacturaRequest request) {
        try {
            FacturaResponse response = facturaService.generarFactura(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Si algo falla (firme digital, SOAP de SUNAT, etc.), verás el error detallado aquí
            throw new RuntimeException("Error al procesar la integración con SUNAT: " + e.getMessage(), e);
        }
    }
}
