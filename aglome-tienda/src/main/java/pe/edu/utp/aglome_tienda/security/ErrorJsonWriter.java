package pe.edu.utp.aglome_tienda.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Escribe los errores 401 y 403 con el mismo formato JSON que ApiError.
 * Estos errores ocurren en la cadena de filtros de seguridad, antes de llegar a los
 * controladores, por eso el GlobalExceptionHandler no los puede atrapar.
 */
final class ErrorJsonWriter {

    private ErrorJsonWriter() {
    }

    static void escribir(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String mensaje) throws IOException {
        String json = "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escapar(status.getReasonPhrase()) + "\","
                + "\"mensaje\":\"" + escapar(mensaje) + "\","
                + "\"path\":\"" + escapar(request.getRequestURI()) + "\","
                + "\"errores\":[]"
                + "}";

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }

    private static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
