package cibertec.pe;

import cibertec.pe.cliente.ClienteImpl;
import cibertec.pe.cliente.IClienteService;
import cibertec.pe.conductor.ConductorImpl;
import cibertec.pe.conductor.IConductorService;
import cibertec.pe.empresa.EmpresaImpl;
import cibertec.pe.empresa.IEmpresaService;
import cibertec.pe.pedido.IPedidoService;
import cibertec.pe.pedido.PedidoImpl;
import cibertec.pe.vehiculo.IVehiculoService;
import cibertec.pe.vehiculo.VehiculoImpl;
import jakarta.xml.ws.Endpoint;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ProyectoApplication {

	public static void main(String[] args) {

		ConfigurableApplicationContext context =
				SpringApplication.run(ProyectoApplication.class, args);

		IClienteService cliente = context.getBean(ClienteImpl.class);

		IConductorService conductor = context.getBean(ConductorImpl.class);

		IVehiculoService vehiculo = context.getBean(VehiculoImpl.class);

		IEmpresaService empresa = context.getBean(EmpresaImpl.class);

		IPedidoService pedido = context.getBean(PedidoImpl.class);

		Endpoint.publish("http://localhost:8085/ws/cliente", unwrapProxy(cliente));
		Endpoint.publish("http://localhost:8085/ws/conductor", unwrapProxy(conductor));
		Endpoint.publish("http://localhost:8085/ws/vehiculo", unwrapProxy(vehiculo));
		Endpoint.publish("http://localhost:8085/ws/empresa", unwrapProxy(empresa));
		Endpoint.publish("http://localhost:8085/ws/pedido", unwrapProxy(pedido));

		System.out.println("Todos los servicios publicados");
	}

	private static Object unwrapProxy(Object bean) {
		if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
			try {
				return ((Advised) bean).getTargetSource().getTarget();
			} catch (Exception e) {
				System.err.println("Error al remover el proxy de Spring: " + e.getMessage());
			}
		}
		return bean;
	}

}
