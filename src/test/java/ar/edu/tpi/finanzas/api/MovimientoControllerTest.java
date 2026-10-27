package ar.edu.tpi.finanzas.api;

import ar.edu.tpi.finanzas.model.Movimiento;
import ar.edu.tpi.finanzas.repository.MovimientoRepository;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovimientoControllerTest {

    private MovimientoRepository repository;
    private MongoTemplate mongoTemplate;
    private MovimientoController controller;

    @BeforeEach
    void setUp() {
        repository = mock(MovimientoRepository.class);
        mongoTemplate = mock(MongoTemplate.class);
        controller = new MovimientoController(repository, mongoTemplate);
    }

    @Test
    void crearRechazaMontoNoPositivoSinConsultarLaBase() {
        MovimientoController.MovimientoRequest request = solicitudValida();
        request.monto = "0";

        ResponseEntity<?> respuesta = controller.crear(request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        verifyNoInteractions(repository, mongoTemplate);
    }

    @Test
    void crearRechazaIdentificadorInvalidoSinConsultarLaBase() {
        MovimientoController.MovimientoRequest request = solicitudValida();
        request.cuentaId = "no-es-object-id";

        ResponseEntity<?> respuesta = controller.crear(request);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("cuentaId debe ser un ObjectId hexadecimal de 24 caracteres",
                ((java.util.Map<?, ?>) respuesta.getBody()).get("error"));
        verifyNoInteractions(repository, mongoTemplate);
    }

    @Test
    void crearGuardaMovimientoConCategoriaHistoricaYResponde201() {
        MovimientoController.MovimientoRequest request = solicitudValida();
        when(mongoTemplate.exists(any(Query.class), eq(Document.class), eq("cuentas"))).thenReturn(true);
        when(mongoTemplate.exists(any(Query.class), eq(Document.class), eq("usuarios"))).thenReturn(true);
        when(mongoTemplate.findOne(any(Query.class), eq(Document.class), eq("categorias")))
                .thenReturn(new Document("nombre", "Alimentación"));
        when(repository.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            movimiento.setId(new ObjectId().toHexString());
            return movimiento;
        });

        ResponseEntity<?> respuesta = controller.crear(request);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        Movimiento guardado = (Movimiento) respuesta.getBody();
        assertNotNull(guardado);
        assertEquals("48500.00", guardado.getMonto().toPlainString());
        assertEquals("Alimentación", guardado.getCategoria().getNombre());
        assertTrue(guardado.isActivo());
    }

    @Test
    void actualizarRechazaIdInvalido() {
        ResponseEntity<?> respuesta = controller.actualizar("invalido", new MovimientoController.MovimientoRequest());

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        verifyNoInteractions(repository, mongoTemplate);
    }

    @Test
    void actualizarParcialmenteMovimientoActivo() {
        String id = new ObjectId().toHexString();
        Movimiento existente = new Movimiento();
        existente.setId(id);
        existente.setActivo(true);
        Movimiento actualizado = new Movimiento();
        actualizado.setId(id);
        actualizado.setDescripcion("Alquiler actualizado");
        actualizado.setActivo(true);
        when(repository.findByIdAndActivoTrue(id)).thenReturn(Optional.of(existente), Optional.of(actualizado));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Movimiento.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        MovimientoController.MovimientoRequest request = new MovimientoController.MovimientoRequest();
        request.descripcion = "Alquiler actualizado";

        ResponseEntity<?> respuesta = controller.actualizar(id, request);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(actualizado, respuesta.getBody());
        verify(mongoTemplate).updateFirst(any(Query.class), argThat(update ->
                "Alquiler actualizado".equals(update.getUpdateObject().get("$set", Document.class).getString("descripcion"))), eq(Movimiento.class));
    }

    @Test
    void actualizarNoEncuentraMovimientoInactivo() {
        String id = new ObjectId().toHexString();
        when(repository.findByIdAndActivoTrue(id)).thenReturn(Optional.empty());

        ResponseEntity<?> respuesta = controller.actualizar(id, new MovimientoController.MovimientoRequest());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Movimiento.class));
    }

    private MovimientoController.MovimientoRequest solicitudValida() {
        MovimientoController.MovimientoRequest request = new MovimientoController.MovimientoRequest();
        request.descripcion = "Compra en supermercado";
        request.monto = "48500.00";
        request.tipo = "EGRESO";
        request.fecha = "2026-10-18";
        request.cuentaId = new ObjectId().toHexString();
        request.creadoPorId = new ObjectId().toHexString();
        request.categoriaId = new ObjectId().toHexString();
        return request;
    }
}
