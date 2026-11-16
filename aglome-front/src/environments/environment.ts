// La API se llama con ruta relativa: el front se sirve desde el mismo servidor Spring Boot (puerto 8080).
// Con ng serve, proxy.conf.json reenvía /api al back.
export const environment = {
  apiUrl: '/api',
};
