package cibertec.pe.pedido;

import cibertec.pe.pedido.dto.PedidoDetails;
import cibertec.pe.pedido.dto.PedidoResponse;
import org.springframework.stereotype.Component;

@Component
public class PedidoBussiness {

    public PedidoResponse convertirListado(Pedido pedido){

        PedidoResponse pedidoResponse = new PedidoResponse();

        pedidoResponse.setId(pedido.getId());
        pedidoResponse.setNombreORazonSocialCliente(pedido.getCliente().getNombreRazonSocial());
        pedidoResponse.setOrigen(pedido.getOrigen());
        pedidoResponse.setDestino(pedido.getDestino());
        pedidoResponse.setEstado(pedido.getEstado());
        return pedidoResponse;
    }

    public PedidoDetails convertirRespuesta(Pedido p){
        PedidoDetails dto = new PedidoDetails();

        dto.setPedidoId(p.getId());
        dto.setFechaPedido(p.getFechaPedido());
        dto.setOrigen(p.getOrigen());
        dto.setDestino(p.getDestino());
        dto.setDescripcionCarga(p.getDescripcionCarga());
        dto.setPesoCarga(p.getPesoCarga());
        dto.setEstado(p.getEstado());

        dto.setClienteId(p.getCliente().getId());
        dto.setRudDniCliente(p.getCliente().getRudDni());
        dto.setNombreRazonSocialCliente(p.getCliente().getNombreRazonSocial());
        dto.setTelefonoCliente(p.getCliente().getTelefono());
        dto.setDireccionCliente(p.getCliente().getDireccion());

        dto.setConductorId(p.getConductor().getId());
        dto.setNombreConductor(p.getConductor().getNombre());
        dto.setDniConductor(p.getConductor().getDni());
        dto.setLicenciaConductor(p.getConductor().getLicencia());
        dto.setTelefonoConductor(p.getConductor().getTelefono());

        dto.setVehiculoId(p.getVehiculo().getId());
        dto.setPlacaVehiculo(p.getVehiculo().getPlaca());
        dto.setMarcaVehiculo(p.getVehiculo().getMarca());
        dto.setTipoVehiculo(p.getVehiculo().getTipoVehiculo().toString());
        dto.setCapacidadCargaVehiculo(p.getVehiculo().getCapacidadCargaKg());

        return dto;
    }

    public PedidoDetails convertirDetalle(Pedido pedido) {

        PedidoDetails details = new PedidoDetails();

        details.setPedidoId(pedido.getId());
        details.setFechaPedido(pedido.getFechaPedido());
        details.setOrigen(pedido.getOrigen());
        details.setDestino(pedido.getDestino());
        details.setDescripcionCarga(pedido.getDescripcionCarga());
        details.setPesoCarga(pedido.getPesoCarga());
        details.setEstado(pedido.getEstado());

        if (pedido.getCliente() != null) {
            details.setClienteId(pedido.getCliente().getId());
            details.setRudDniCliente(pedido.getCliente().getRudDni());
            details.setNombreRazonSocialCliente(pedido.getCliente().getNombreRazonSocial());
            details.setDireccionCliente(pedido.getCliente().getDireccion());
            details.setTelefonoCliente(pedido.getCliente().getTelefono());
        }

        if (pedido.getConductor() != null) {
            details.setConductorId(pedido.getConductor().getId());
            details.setNombreConductor(pedido.getConductor().getNombre());
            details.setDniConductor(pedido.getConductor().getDni());
            details.setLicenciaConductor(pedido.getConductor().getLicencia());
            details.setTelefonoConductor(pedido.getConductor().getTelefono());
        }

        if (pedido.getVehiculo() != null) {
            details.setVehiculoId(pedido.getVehiculo().getId());
            details.setPlacaVehiculo(pedido.getVehiculo().getPlaca());
            details.setMarcaVehiculo(pedido.getVehiculo().getMarca());
            details.setCapacidadCargaVehiculo(pedido.getVehiculo().getCapacidadCargaKg());
            details.setTipoVehiculo(pedido.getVehiculo().getTipoVehiculo().toString());
        }

        return details;
    }

    public Double calcularCostoEnvio(Double peso) {

        if (peso <= 500) {
            return 150.0;
        } else if (peso <= 1000) {
            return 300.0;
        } else if (peso <= 3000) {
            return 600.0;
        } else {
            return 1000.0;
        }
    }
}
