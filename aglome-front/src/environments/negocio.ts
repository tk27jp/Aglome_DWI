// Datos de contacto de Aglome que se muestran en el pie, la portada y el botón de WhatsApp.
// Lo que dejes vacío ('') simplemente no se muestra.

export interface Sede {
  nombre: string;
  direccion: string;
  /** Una línea por tramo de horario. */
  horarios: { dias: string; horas: string }[];
  foto: string;
}

export const negocio = {
  nombre: 'Aglome Perú',
  razonSocial: 'AGLOME PERÚ S.R.L.',
  lema: 'Madera, melamina y ferretería',
  ciudad: 'Chimbote, Perú',
  /** Número con código de país y sin signos (51 = Perú). */
  whatsapp: '51913974157',
  telefono: '913 974 157',
  correo: 'Aglome.peru@hotmail.com',
  /** Desde cuándo opera la empresa. */
  desde: 2019,
  fundacion: '2 de octubre de 2019',
  mision: 'Proveer materiales de calidad y servicios especializados para proyectos de construcción y remodelación, atendiendo las necesidades específicas de nuestros clientes con excelencia.',
  vision: 'Ser la empresa líder en provisión de materiales para proyectos en la región Áncash, reconocida por nuestra calidad, servicio y compromiso con el desarrollo de nuestra comunidad.',
  sedes: [
    {
      nombre: 'Miramar Alto',
      direccion: 'Jr. Libertad Nro. 538 - Miramar Alto',
      horarios: [
        { dias: 'Lunes a viernes', horas: '8:00 a. m. – 1:00 p. m. / 2:30 p. m. – 7:00 p. m.' },
        { dias: 'Sábado', horas: '9:00 a. m. – 1:00 p. m.' },
      ],
      foto: 'sedes/miramar-alto.jpg',
    },
    {
      nombre: 'El Progreso',
      direccion: 'Jr. Independencia Nro. 331 - El Progreso',
      horarios: [
        { dias: 'Lunes a viernes', horas: '8:00 a. m. – 1:00 p. m. / 2:30 p. m. – 6:30 p. m.' },
        { dias: 'Sábado', horas: '9:00 a. m. – 1:00 p. m.' },
      ],
      foto: 'sedes/el-progreso.jpg',
    },
    {
      nombre: 'Nuevo Chimbote',
      direccion: 'Calle 4 MZ.G LT. 10 - Zona Centro Sur Bellavista',
      horarios: [{ dias: 'Lunes a sábado', horas: '8:00 a. m. – 1:00 p. m. / 3:00 p. m. – 6:00 p. m.' }],
      foto: 'sedes/nuevo-chimbote.jpg',
    },
  ] as Sede[],
  /** Enlace "insertar mapa" de Google Maps (src del iframe). Opcional: cada sede ya tiene su botón "Cómo llegar". */
  mapaUrl: '',
};

/** Abre Google Maps buscando la dirección de la sede (no necesita clave). */
export function enlaceMapa(sede: Sede): string {
  return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(`${sede.direccion}, Chimbote, Áncash, Perú`);
}
