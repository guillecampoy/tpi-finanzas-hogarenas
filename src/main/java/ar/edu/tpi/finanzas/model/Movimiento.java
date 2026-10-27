package ar.edu.tpi.finanzas.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Documento de la colección "movimientos".
 * Los links (cuentaId, creadoPorId) se guardan como ObjectId en Mongo y se exponen como texto en la API.
 * La categoría es un subdocumento embebido (id + nombre histórico).
 */
@Document("movimientos")
@JsonInclude(JsonInclude.Include.NON_NULL) // deletedAt solo aparece si el movimiento fue dado de baja
public class Movimiento {

    @Id
    private String id;

    private String descripcion;

    // Sin targetType, Spring guardaría el BigDecimal como texto. Así queda como Decimal128.
    @Field(targetType = FieldType.DECIMAL128)
    @JsonFormat(shape = JsonFormat.Shape.STRING) // en la API el monto viaja como texto
    private BigDecimal monto;

    private String tipo; // INGRESO | EGRESO

    // Instant evita problemas de zona horaria (en la base es una fecha ISODate)
    private Instant fecha;

    @Field(targetType = FieldType.OBJECT_ID)
    private String cuentaId; // link a cuentas

    @Field(targetType = FieldType.OBJECT_ID)
    private String creadoPorId; // link a usuarios

    private Categoria categoria; // embedding: id + nombre histórico

    private boolean activo = true;

    private Instant deletedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }

    public String getCuentaId() { return cuentaId; }
    public void setCuentaId(String cuentaId) { this.cuentaId = cuentaId; }

    public String getCreadoPorId() { return creadoPorId; }
    public void setCreadoPorId(String creadoPorId) { this.creadoPorId = creadoPorId; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    /** Subdocumento embebido: copia del id y del nombre de la categoría al momento de crear el movimiento. */
    public static class Categoria {

        // La propiedad en Java se llama categoriaId (si se llamara "id", Spring la guardaría como "_id").
        // En la base y en la API sigue llamándose "id".
        @Field(name = "id", targetType = FieldType.OBJECT_ID)
        @JsonProperty("id")
        private String categoriaId;

        private String nombre;

        public String getCategoriaId() { return categoriaId; }
        public void setCategoriaId(String categoriaId) { this.categoriaId = categoriaId; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }
}
