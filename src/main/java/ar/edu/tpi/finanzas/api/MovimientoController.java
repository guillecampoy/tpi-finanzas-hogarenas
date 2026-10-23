package ar.edu.tpi.finanzas.api;

import ar.edu.tpi.finanzas.model.Movimiento;
import ar.edu.tpi.finanzas.repository.MovimientoRepository;
import com.mongodb.client.result.UpdateResult;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * CRUD de movimientos: parte de READ y DELETE lógico.
 * (CREATE y UPDATE se agregan en esta misma clase o en otra, según se acuerde con Guille.)
 */
@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MovimientoController(MovimientoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    // READ (lista): solo los movimientos activos
    @GetMapping
    public List<Movimiento> listar() {
        return repository.findByActivoTrue();
    }

    // READ (uno): 200 si existe y está activo, 404 si no existe o está dado de baja, 400 si el id es inválido
    @GetMapping("/{id}")
    public ResponseEntity<Movimiento> obtener(@PathVariable String id) {
        if (!ObjectId.isValid(id)) {
            return ResponseEntity.badRequest().build();
        }
        return repository.findByIdAndActivoTrue(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE lógico: no borra, hace un update de activo=false y deletedAt=ahora (como el $set del shell)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String id) {
        if (!ObjectId.isValid(id)) {
            return ResponseEntity.badRequest().build();
        }
        Query query = new Query(Criteria.where("_id").is(new ObjectId(id)).and("activo").is(true));
        Update update = new Update().set("activo", false).set("deletedAt", Instant.now());
        UpdateResult resultado = mongoTemplate.updateFirst(query, update, Movimiento.class);

        if (resultado.getMatchedCount() == 0) {
            return ResponseEntity.notFound().build(); // no existía o ya estaba dado de baja
        }
        return ResponseEntity.noContent().build(); // 204
    }
}
