package ar.edu.tpi.finanzas.repository;

import ar.edu.tpi.finanzas.model.Movimiento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la colección "movimientos".
 * Spring genera la implementación a partir del nombre de cada método.
 */
public interface MovimientoRepository extends MongoRepository<Movimiento, String> {

    // Equivale a db.movimientos.find({ activo: true })
    List<Movimiento> findByActivoTrue();

    // Equivale a db.movimientos.findOne({ _id: id, activo: true })
    Optional<Movimiento> findByIdAndActivoTrue(String id);
}
