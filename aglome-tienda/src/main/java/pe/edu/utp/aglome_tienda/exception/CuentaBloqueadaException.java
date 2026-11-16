package pe.edu.utp.aglome_tienda.exception;

/** La cuenta está bloqueada temporalmente por demasiados intentos fallidos de contraseña. Responde 423. */
public class CuentaBloqueadaException extends RuntimeException {
    public CuentaBloqueadaException(String mensaje) {
        super(mensaje);
    }
}
