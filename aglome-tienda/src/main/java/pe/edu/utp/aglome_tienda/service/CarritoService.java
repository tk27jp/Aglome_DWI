package pe.edu.utp.aglome_tienda.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import pe.edu.utp.aglome_tienda.dto.AgregarAlCarritoRequest;
import pe.edu.utp.aglome_tienda.dto.CarritoDTO;
import pe.edu.utp.aglome_tienda.dto.CarritoItemDTO;
import pe.edu.utp.aglome_tienda.model.*;
import pe.edu.utp.aglome_tienda.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository carritoItemRepository;
    private final UsuarioRepository usuarioRepository;
    private final VarianteRepository varianteRepository;

    // ── obtiene el carrito del usuario, o se lo crea si es la primera vez ──
    public Carrito obtenerOCrearCarrito(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return carritoRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    Carrito nuevo = new Carrito();
                    nuevo.setUsuario(usuario);
                    return carritoRepository.save(nuevo);
                });
    }

    public CarritoDTO verCarrito(String email) {
        Carrito carrito = obtenerOCrearCarrito(email);
        return mapearADto(carrito);
    }

    public CarritoDTO agregarItem(String email, AgregarAlCarritoRequest request) {
        Carrito carrito = obtenerOCrearCarrito(email);

        Variante variante = varianteRepository.findById(request.getVarianteId())
                .orElseThrow(() -> new RuntimeException("Variante no encontrada"));

        if (variante.getStock() < request.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + variante.getStock());
        }

        // si ya existe ese producto en el carrito, suma la cantidad en vez de duplicar la fila
        CarritoItem item = carritoItemRepository.findByCarritoIdAndVarianteId(carrito.getId(), variante.getId())
                .orElseGet(() -> {
                    CarritoItem nuevo = new CarritoItem();
                    nuevo.setCarrito(carrito);
                    nuevo.setVariante(variante);
                    nuevo.setCantidad(0);
                    return nuevo;
                });

        item.setCantidad(item.getCantidad() + request.getCantidad());
        carritoItemRepository.save(item);

        return mapearADto(carrito);
    }

    public CarritoDTO actualizarCantidad(String email, UUID itemId, Integer cantidad) {
        Carrito carrito = obtenerOCrearCarrito(email);

        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado en el carrito"));

        if (!item.getCarrito().getId().equals(carrito.getId())) {
            throw new RuntimeException("Este item no pertenece a tu carrito");
        }

        if (cantidad <= 0) {
            carritoItemRepository.delete(item);
        } else {
            if (item.getVariante().getStock() < cantidad) {
                throw new RuntimeException("Stock insuficiente. Disponible: " + item.getVariante().getStock());
            }
            item.setCantidad(cantidad);
            carritoItemRepository.save(item);
        }

        return mapearADto(carrito);
    }

    public CarritoDTO eliminarItem(String email, UUID itemId) {
        Carrito carrito = obtenerOCrearCarrito(email);

        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado en el carrito"));

        if (!item.getCarrito().getId().equals(carrito.getId())) {
            throw new RuntimeException("Este item no pertenece a tu carrito");
        }

        carritoItemRepository.delete(item);
        return mapearADto(carrito);
    }

    public void vaciarCarrito(UUID carritoId) {
        carritoItemRepository.deleteByCarritoId(carritoId);
    }

    // ── helper privado ──────────────────────────────────────────────
    private CarritoDTO mapearADto(Carrito carrito) {
        List<CarritoItem> items = carritoItemRepository.findByCarritoId(carrito.getId());

        List<CarritoItemDTO> itemsDto = items.stream().map(i -> {
            BigDecimal subtotal = i.getVariante().getPrecioVenta().multiply(BigDecimal.valueOf(i.getCantidad()));
            return new CarritoItemDTO(
                    i.getId(),
                    i.getVariante().getId(),
                    i.getVariante().getProductoBase().getNombre(),
                    i.getVariante().getSku(),
                    i.getVariante().getPrecioVenta(),
                    i.getCantidad(),
                    subtotal
            );
        }).collect(Collectors.toList());

        BigDecimal total = itemsDto.stream()
                .map(CarritoItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarritoDTO(carrito.getId(), itemsDto, total);
    }
}