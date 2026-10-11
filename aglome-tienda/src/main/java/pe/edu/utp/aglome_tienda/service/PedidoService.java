package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.aglome_tienda.dto.DetallePedidoDTO;
import pe.edu.utp.aglome_tienda.dto.PedidoDTO;
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

    @Transactional
    public PedidoDTO confirmarCompra(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Carrito carrito = carritoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("No tienes un carrito activo"));

        List<CarritoItem> items = carritoItemRepository.findByCarritoId(carrito.getId());

        if (items.isEmpty()) {
            throw new RuntimeException("Tu carrito está vacío, no se puede confirmar la compra");
        }

        // valida stock de todo ANTES de descontar nada (evita descuentos parciales si algo falla)
        for (CarritoItem item : items) {
            if (item.getVariante().getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + item.getVariante().getSku());
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

            variante.setStock(variante.getStock() - item.getCantidad());
            varianteRepository.save(variante);

            total = total.add(subtotal);
        }

        pedidoGuardado.setTotal(total);
        pedidoRepository.save(pedidoGuardado);

        carritoService.vaciarCarrito(carrito.getId());

        return mapearADto(pedidoGuardado);
    }

    public List<PedidoDTO> misPedidos(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return pedidoRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId())
                .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public List<PedidoDTO> misPedidosPorEstado(String email, EstadoPedido estado) {
    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return pedidoRepository.buscarPorUsuarioYEstado(usuario.getId(), estado)
            .stream().map(this::mapearADto).collect(Collectors.toList());
    }

    public PedidoDTO obtenerPorId(String email, UUID pedidoId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("Este pedido no te pertenece");
        }

        return mapearADto(pedido);
    }

    // ── para el panel admin: ver todos los pedidos ──────────────────
    public List<PedidoDTO> listarTodosAdmin() {
        return pedidoRepository.findAll().stream().map(this::mapearADto).collect(Collectors.toList());
    }

    @Transactional
    public PedidoDTO cambiarEstado(UUID pedidoId, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
        throw new RuntimeException("El pedido ya está cancelado");
        }

        // Si se cancela, el stock de cada producto vuelve al inventario
        if (nuevoEstado == EstadoPedido.CANCELADO) {
            for (DetallePedido d : detallePedidoRepository.findByPedidoId(pedidoId)) {
                Variante v = d.getVariante();
                v.setStock(v.getStock() + d.getCantidad());
                varianteRepository.save(v);
            }
        }

        pedido.setEstado(nuevoEstado);
        return mapearADto(pedidoRepository.save(pedido));
    }

    // ── helper privado ───────────────────────────────────────────────
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

        return new PedidoDTO(pedido.getId(), pedido.getFecha(), pedido.getEstado().name(), pedido.getTotal(), detalles);
    }
}