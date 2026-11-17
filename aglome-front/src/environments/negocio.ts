

export interface Sede {
  nombre: string;
  direccion: string;
  telefono?: string;
  horarios: { dias: string; horas: string }[];
  foto: string;
  enlace?: string;
  incrustar?: string;
}

const buscarEnMapa = (consulta: string) => 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(consulta);

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
      direccion: 'Jr. Libertad Nro. 548 - Miramar Alto',
      telefono: '923 197 032',
      horarios: [
        { dias: 'Lunes a viernes', horas: '8:30 a. m. – 1:00 p. m. / 2:30 p. m. – 7:00 p. m.' },
        { dias: 'Sábado', horas: '9:00 a. m. – 1:00 p. m.' },
      ],
      foto: 'sedes/miramar-alto.jpg',
      enlace: buscarEnMapa('Aglome Peru - Libertad, Chimbote'),
      incrustar: '',
    },
    {
      nombre: 'El Progreso',
      direccion: 'Jr. Independencia Nro. 331 - El Progreso',
      telefono: '908 969 088',
      horarios: [
        { dias: 'Lunes a viernes', horas: '8:30 a. m. – 1:00 p. m. / 2:30 p. m. – 6:30 p. m.' },
        { dias: 'Sábado', horas: '9:00 a. m. – 1:00 p. m.' },
      ],
      foto: 'sedes/el-progreso.jpg',
      enlace: buscarEnMapa('Aglome Peru - El Progreso, Chimbote'),
      incrustar: '',
    },
    {
      nombre: 'Nuevo Chimbote',
      direccion: 'Mz. G Lote 10 - Zona Centro Sur B, Bellavista',
      telefono: '993 028 488',
      horarios: [{ dias: 'Lunes a sábado', horas: '8:00 a. m. – 1:00 p. m. / 3:00 p. m. – 6:00 p. m.' }],
      foto: 'sedes/nuevo-chimbote.jpg',
      enlace: buscarEnMapa('Aglome Peru - Bellavista, Chimbote'),
      incrustar: '',
    },
  ] as Sede[],
  mapaUrl: '',
};

export function enlaceMapa(sede: Sede): string {
  if (sede.enlace) return sede.enlace;
  return 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(`${sede.direccion}, Chimbote, Áncash, Perú`);
}