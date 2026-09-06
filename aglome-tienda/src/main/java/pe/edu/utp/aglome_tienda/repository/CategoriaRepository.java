package pe.edu.utp.aglome_tienda.repository;

import pe.edu.utp.aglome_tienda.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}