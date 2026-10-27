package ar.edu.tpi.finanzas.api;

import ar.edu.tpi.finanzas.model.Movimiento;
import ar.edu.tpi.finanzas.repository.MovimientoRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.result.UpdateResult;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CRUD de movimientos.
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

    // CREATE: acepta fecha como yyyy-MM-dd y monto como decimal en texto.
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody MovimientoRequest request) {
        String error = validar(request, true);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("error", error));

        Movimiento movimiento = new Movimiento();
        movimiento.setDescripcion(request.descripcion.trim());
        movimiento.setMonto(new BigDecimal(request.monto));
        movimiento.setTipo(request.tipo.trim().toUpperCase());
        movimiento.setFecha(parseFecha(request.fecha));
        movimiento.setCuentaId(request.cuentaId);
        movimiento.setCreadoPorId(request.creadoPorId);

        String referenciaError = validarReferencias(request);
        if (referenciaError != null) return ResponseEntity.badRequest().body(Map.of("error", referenciaError));
        movimiento.setCategoria(cargarCategoria(request.categoriaId));

        Movimiento guardado = repository.save(movimiento);
        return ResponseEntity.created(java.net.URI.create("/api/movimientos/" + guardado.getId())).body(guardado);
    }

    // UPDATE parcial: solo acepta cambios en campos informados; no permite modificar inactivos.
    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody MovimientoRequest request) {
        if (!ObjectId.isValid(id)) return ResponseEntity.badRequest().body(Map.of("error", "id inválido"));
        if (repository.findByIdAndActivoTrue(id).isEmpty()) return ResponseEntity.notFound().build();

        String error = validar(request, false);
        if (error != null) return ResponseEntity.badRequest().body(Map.of("error", error));
        String referenciaError = validarReferencias(request);
        if (referenciaError != null) return ResponseEntity.badRequest().body(Map.of("error", referenciaError));

        Query query = new Query(Criteria.where("_id").is(new ObjectId(id)).and("activo").is(true));
        Update update = new Update();
        if (request.descripcion != null) update.set("descripcion", request.descripcion.trim());
        if (request.monto != null) update.set("monto", new BigDecimal(request.monto));
        if (request.tipo != null) update.set("tipo", request.tipo.trim().toUpperCase());
        if (request.fecha != null) update.set("fecha", parseFecha(request.fecha));
        if (request.cuentaId != null) update.set("cuentaId", new ObjectId(request.cuentaId));
        if (request.creadoPorId != null) update.set("creadoPorId", new ObjectId(request.creadoPorId));
        if (request.categoriaId != null) update.set("categoria", cargarCategoria(request.categoriaId));
        if (update.getUpdateObject().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "debe informar al menos un campo para actualizar"));

        UpdateResult resultado = mongoTemplate.updateFirst(query, update, Movimiento.class);
        if (resultado.getMatchedCount() == 0) return ResponseEntity.notFound().build();
        return repository.findByIdAndActivoTrue(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    private String validar(MovimientoRequest r, boolean crear) {
        if (r == null) return "el cuerpo de la solicitud es obligatorio";
        if (crear && (r.descripcion == null || r.monto == null || r.tipo == null || r.fecha == null
                || r.cuentaId == null || r.creadoPorId == null || r.categoriaId == null)) {
            return "descripcion, monto, tipo, fecha, cuentaId, creadoPorId y categoriaId son obligatorios";
        }
        if (r.descripcion != null && r.descripcion.trim().isEmpty()) return "descripcion no puede estar vacía";
        if (r.monto != null) {
            try { if (new BigDecimal(r.monto).signum() <= 0) return "monto debe ser mayor a cero"; }
            catch (NumberFormatException e) { return "monto debe ser un decimal válido"; }
        }
        if (r.tipo != null && !List.of("INGRESO", "EGRESO").contains(r.tipo.trim().toUpperCase())) return "tipo debe ser INGRESO o EGRESO";
        if (r.fecha != null) {
            try { LocalDate.parse(r.fecha); }
            catch (DateTimeParseException e) { return "fecha debe tener formato yyyy-MM-dd y ser válida"; }
        }
        if (r.cuentaId != null && !ObjectId.isValid(r.cuentaId)) return "cuentaId debe ser un ObjectId hexadecimal de 24 caracteres";
        if (r.creadoPorId != null && !ObjectId.isValid(r.creadoPorId)) return "creadoPorId debe ser un ObjectId hexadecimal de 24 caracteres";
        if (r.categoriaId != null && !ObjectId.isValid(r.categoriaId)) return "categoriaId debe ser un ObjectId hexadecimal de 24 caracteres";
        return null;
    }

    private String validarReferencias(MovimientoRequest r) {
        if (r.cuentaId != null && !existeActivo("cuentas", r.cuentaId)) return "cuentaId no existe o está inactiva";
        if (r.creadoPorId != null && !existeActivo("usuarios", r.creadoPorId)) return "creadoPorId no existe o está inactivo";
        if (r.categoriaId != null && cargarCategoria(r.categoriaId) == null) return "categoriaId no existe o está inactiva";
        return null;
    }

    private boolean existeActivo(String coleccion, String id) {
        return mongoTemplate.exists(new Query(Criteria.where("_id").is(new ObjectId(id)).and("activo").is(true)), org.bson.Document.class, coleccion);
    }

    private Movimiento.Categoria cargarCategoria(String id) {
        org.bson.Document doc = mongoTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(id)).and("activo").is(true)), org.bson.Document.class, "categorias");
        if (doc == null || !(doc.get("nombre") instanceof String nombre) || nombre.isBlank()) return null;
        Movimiento.Categoria categoria = new Movimiento.Categoria();
        categoria.setCategoriaId(id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private Instant parseFecha(String fecha) {
        return LocalDate.parse(fecha).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static class MovimientoRequest {
        @Schema(description = "Descripción del movimiento", example = "Reparación del aire acondicionado")
        public String descripcion;
        @Schema(description = "Importe decimal positivo, enviado como texto", example = "10000.00")
        public String monto;
        @Schema(description = "Tipo de movimiento", allowableValues = {"INGRESO", "EGRESO"}, example = "EGRESO")
        public String tipo;
        @Schema(description = "Fecha del movimiento en formato yyyy-MM-dd", example = "2026-10-27")
        public String fecha;
        @Schema(description = "ObjectId hexadecimal de 24 caracteres de una cuenta existente y activa", example = "507f1f77bcf86cd799439011", pattern = "^[a-fA-F0-9]{24}$")
        public String cuentaId;
        @Schema(description = "ObjectId hexadecimal de 24 caracteres de un usuario existente y activo", example = "507f191e810c19729de860ea", pattern = "^[a-fA-F0-9]{24}$")
        @JsonProperty("creadoPorId") public String creadoPorId;
        @Schema(description = "ObjectId hexadecimal de 24 caracteres de una categoría existente y activa", example = "507f1f77bcf86cd799439012", pattern = "^[a-fA-F0-9]{24}$")
        public String categoriaId;
    }
}
