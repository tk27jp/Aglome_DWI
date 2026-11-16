package pe.edu.utp.aglome_tienda.model;

import java.util.List;

public enum EstadoPedido {
    PENDIENTE,
    CONFIRMADO,
    EN_PREPARACION,
    ENVIADO,
    ENTREGADO,
    CANCELADO;

    /** Estados a los que se puede pasar desde este. */
    public List<EstadoPedido> siguientes() {
        return switch (this) {
            case PENDIENTE -> List.of(CONFIRMADO, CANCELADO);
            case CONFIRMADO -> List.of(EN_PREPARACION, CANCELADO);
            case EN_PREPARACION -> List.of(ENVIADO, CANCELADO);
            case ENVIADO -> List.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> List.of();
        };
    }

    public boolean puedePasarA(EstadoPedido nuevo) {
        return siguientes().contains(nuevo);
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    /** El cliente solo puede cancelar mientras la tienda no empezó a prepararlo. */
    public boolean cancelablePorCliente() {
        return this == PENDIENTE || this == CONFIRMADO;
    }
}
