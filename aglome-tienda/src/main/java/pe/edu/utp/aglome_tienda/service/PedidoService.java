package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.aglome_tienda.dto.DetallePedidoDTO;
import pe.edu.utp.aglome_tienda.dto.PedidoDTO;
import pe.edu.utp.aglome_tienda.exception.ConflictoException;
import pe.edu.utp.aglome_tienda.exception.RecursoNoEncontradoException;
import pe.edu.utp.aglome_tienda.exception.ReglaNegocioException;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository carritoItemRepository;
    private final VarianteRepository varianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;
    private final MovimientoStockService movimientoStockService;

    @Transactional
    public PedidoDTO confirmarCompra(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Carrito carrito = carritoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ReglaNegocioException("No tienes un carrito activo"));

        List<CarritoItem> items = carritoItemRepository.findByCarritoId(carrito.getId());

        if (items.isEmpty()) {
            throw new ReglaNegocioException("Tu carrito está vacío, no se puede confirmar la compra");
        }

        // valida stock de todo ANTES de descontar nada (evita descuentos parciales si algo falla)
        for (CarritoItem item : items) {
            Variante v = item.getVariante();
            if (Boolean.TRUE.equals(v.getEliminado())
                    || Boolean.TRUE.equals(v.getProductoBase().getEliminado())) {
                throw new ConflictoException("El producto ya no está disponible: " + v.getSku());
            }
            if (item.getVariante().getStock() < item.getCantidad()) {
                throw new ConflictoException("Stock insuficiente para: " + item.getVariante().getSku());
            }
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido.setTotal(BigDecimal.ZERO); // se calcula abajo
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        BigDecimal total = BigDecimal.ZERO;

        for (CarritoItem item : items) {
            Variante variante = item.getVariante();
            BigDecimal subtotal = variante.getPrecioVenta().multiply(BigDecimal.valueOf(item.getCantidad()));

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedidoGuardado);
            detalle.setVariante(variante);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(variante.getPrecioVenta()); // "foto" del precio actual
            detalle.setSubtotal(subtotal);
            detallePedidoRepository.save(detalle);

            int stockAnterior = variante.getStock();
            variante.setStock(stockAnterior - item.getCantidad());
            varianteRepository.save(variante);
            movimientoStockService.registrar(variante, TipoMovimiento.VENTA,
                    stockAnterior, variante.getStock(), "Compra confirmada", pedidoGuardado.getId());

            total = total.add(subtotal);
        }

        pedidoGuardado.setTotal(total);
        pedidoRepository.save(pedidoGuardado);

        carritoService.vaciarCarrito(carrito.getId());

        return mapearADto(pedidoGuardado);
    }

    public List<PedidoDTO> misPedidos(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId())
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public List<PedidoDTO> misPedidosPorEstado(String email, EstadoPedido estado) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return pedidoRepository.buscarPorUsuarioYEstado(usuario.getId(), estado)
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public PedidoDTO obtenerPorId(String email, UUID pedidoId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));

        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Este pedido no te pertenece");
        }

        return mapearADto(pedido);
    }

    /** El cliente cancela su propio pedido, solo mientras la tienda no empezó a prepararlo. */
    @Transactional
    public PedidoDTO cancelarMiPedido(String email, UUID pedidoId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));

        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Este pedido no te pertenece");
        }
        if (!pedido.getEstado().cancelablePorCliente()) {
            throw new ConflictoException("Este pedido ya no se puede cancelar porque está "
                    + pedido.getEstado() + ". Comunícate con la tienda");
        }

        return mapearADto(aplicarCambioDeEstado(pedido, EstadoPedido.CANCELADO));
    }

    // ── para el panel admin ──────────────────────────────────────────
    /** Todos los pedidos, los más recientes primero; con estado solo los de ese estado. */
    public List<PedidoDTO> listarTodosAdmin(EstadoPedido estado) {
        List<Pedido> pedidos = estado == null
                ? pedidoRepository.findAllByOrderByFechaDesc()
                : pedidoRepository.findByEstadoOrderByFechaDesc(estado);
        return pedidos.stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public PedidoDTO obtenerPorIdAdmin(UUID pedidoId) {
        return mapearADto(pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado")));
    }

    @Transactional
    public PedidoDTO cambiarEstado(UUID pedidoId, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));

        EstadoPedido actual = pedido.getEstado();
        if (!actual.puedePasarA(nuevoEstado)) {
            throw new ConflictoException(mensajeTransicionInvalida(actual, nuevoEstado));
        }

        return mapearADto(aplicarCambioDeEstado(pedido, nuevoEstado));
    }

    // ── helpers privados ─────────────────────────────────────────────
    private Pedido aplicarCambioDeEstado(Pedido pedido, EstadoPedido nuevoEstado) {
        EstadoPedido anterior = pedido.getEstado();

        // Si se cancela, el stock vuelve al inventario. Un pedido PENDIENTE nunca descontó stock.
        if (nuevoEstado == EstadoPedido.CANCELADO && anterior != EstadoPedido.PENDIENTE) {
            for (DetallePedido d : detallePedidoRepository.findByPedidoId(pedido.getId())) {
                Variante v = d.getVariante();
                int stockAnterior = v.getStock();
                v.setStock(stockAnterior + d.getCantidad());
                varianteRepository.save(v);
                movimientoStockService.registrar(v, TipoMovimiento.CANCELACION,
                        stockAnterior, v.getStock(), "Pedido cancelado", pedido.getId());
            }
        }

        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    private String mensajeTransicionInvalida(EstadoPedido actual, EstadoPedido nuevo) {
        if (actual == EstadoPedido.CANCELADO) {
            return "El pedido ya está cancelado";
        }
        if (actual.esFinal()) {
            return "El pedido ya está " + actual + " y no admite más cambios";
        }
        String permitidos = actual.siguientes().stream().map(Enum::name).collect(Collectors.joining(", "));
        return "No se puede pasar un pedido de " + actual + " a " + nuevo
                + ". Estados permitidos: " + permitidos;
    }

    private PedidoDTO mapearADto(Pedido pedido) {
        List<DetallePedidoDTO> detalles = detallePedidoRepository.findByPedidoId(pedido.getId())
                .stream()
                .map(d -> new DetallePedidoDTO(
                        d.getVariante().getProductoBase().getNombre(),
                        d.getVariante().getSku(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()
                ))
                .collect(Collectors.toList());

        List<String> siguientes = pedido.getEstado().siguientes().stream()
                .map(Enum::name).collect(Collectors.toList());

        return new PedidoDTO(pedido.getId(), pedido.getFecha(), pedido.getEstado().name(),
                pedido.getTotal(), detalles, siguientes);
    }
}