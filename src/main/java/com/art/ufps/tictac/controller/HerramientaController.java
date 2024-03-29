package com.art.ufps.tictac.controller;

import com.art.ufps.tictac.dto.MensajeValidacion;
import com.art.ufps.tictac.dto.ProcesoJsonDto;
import com.art.ufps.tictac.dto.RequestBodyWraper;
import com.art.ufps.tictac.entity.*;
import com.art.ufps.tictac.excepciones.MensajeDto;
import com.art.ufps.tictac.repository.*;
import com.art.ufps.tictac.service.LiderLineaInterface;
import com.art.ufps.tictac.service.TemaInterface;
import com.art.ufps.tictac.service.implement.HerramientaService;
import com.art.ufps.tictac.service.implement.LiderLineaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/herramienta")
@CrossOrigin()
public class HerramientaController {

    private final HerramientaService herramientaService;

    private final HerramientaRepository herramientaRepository;

    private final TemaRepository temaRepository;

    private final TemaInterface temaInterface;

    private final LiderLineaInterface liderLineaInterface;

    private final MomentoRepository momentoRepository;

    private final ProcesoRepository procesoRepository;

    private final RecursoRepository recursoRepository;

    private final RecursoProcesoRepository recursoProcesoRepository;

    private final LineaTransversalRepository lineaTransversalRepository;


    @Autowired
    public HerramientaController(HerramientaService herramientaService, HerramientaRepository herramientaRepository, TemaRepository temaRepository, TemaInterface temaInterface, LiderLineaInterface liderLineaInterface, MomentoRepository momentoRepository, ProcesoRepository procesoRepository, RecursoRepository recursoRepository, RecursoProcesoRepository recursoProcesoRepository, LineaTransversalRepository lineaTransversalRepository) {
        this.herramientaService = herramientaService;
        this.herramientaRepository = herramientaRepository;
        this.temaRepository = temaRepository;
        this.temaInterface = temaInterface;
        this.liderLineaInterface = liderLineaInterface;
        this.momentoRepository = momentoRepository;
        this.procesoRepository = procesoRepository;
        this.recursoRepository = recursoRepository;
        this.recursoProcesoRepository = recursoProcesoRepository;
        this.lineaTransversalRepository = lineaTransversalRepository;
    }

    @GetMapping("/list/{ruta}")
    public ResponseEntity<Optional<List<Herramienta>>> list(@PathVariable("ruta")String ruta, @RequestParam("eje") String eje) {

        Optional<List<Tema>> temas = temaInterface.getByEje(Integer.parseInt(eje));
        Optional<List<Herramienta>> herramientas = Optional.empty();

        if (temas.isPresent()) {
            List<Tema> temaList = temas.get();
            List<Herramienta> herramientaList = new ArrayList<>();

            for (Tema tema : temaList) {
                Optional<Herramienta> herramienta = herramientaService.getByIdTema(tema.getIdTema());
                if (herramienta.isPresent() && ruta.equals("home") && herramienta.get().getVisibilidad() == 1 && herramienta.get().getEstado().equals("1")) {
                    herramientaList.add(herramienta.get());
                }
                else if (herramienta.isPresent() && ruta.equals("institucion") && herramienta.get().getEstado().equals("1")){
                    herramientaList.add(herramienta.get());
                }
            }
            herramientas = Optional.of(herramientaList);
        }

        return new ResponseEntity<>(herramientas, HttpStatus.OK);
    }

    @GetMapping("/listPendientes/{ruta}")
    public ResponseEntity<Optional<List<Herramienta>>> listPendientes(@PathVariable("ruta")String ruta, @RequestParam("documento") String documento) {

        LiderLinea liderLinea = liderLineaInterface.getByIdDocente(documento);
        int idLinea = liderLinea.getIdLinea();

        Optional<List<Tema>> temas = temaInterface.getByEje(idLinea);
        Optional<List<Herramienta>> herramientas = Optional.empty();

        if (temas.isPresent()) {
            List<Tema> temaList = temas.get();
            List<Herramienta> herramientaList = new ArrayList<>();

            for (Tema tema : temaList) {
                Optional<Herramienta> herramienta = herramientaService.getByIdTema(tema.getIdTema());
                if (herramienta.isPresent() && ruta.equals("institucion") && herramienta.get().getEstado().equals("0")){
                    herramientaList.add(herramienta.get());
                }
            }
            herramientas = Optional.of(herramientaList);
        }
        return new ResponseEntity<>(herramientas, HttpStatus.OK);
    }

    @PutMapping("/validar/{id}")
    public ResponseEntity<?> update(@PathVariable("id") int id, @RequestBody MensajeValidacion mensajeValidacion) {
        Optional<Herramienta> herramientaOptional = herramientaService.getById(id);

        if (herramientaOptional.isPresent()) {
            Herramienta herramienta = herramientaOptional.get();
            // Actualizar el mensaje de la herramienta con el valor recibido en el cuerpo de la solicitud
            herramienta.setComentarios(mensajeValidacion.getMensaje());
            herramienta.setEstado(mensajeValidacion.getEstado());
            herramientaService.save(herramienta);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/getbyid/{id}")
    public ResponseEntity<Herramienta> getById(@PathVariable("id") int id) {
        if (!herramientaService.existById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Herramienta herramienta = herramientaService.getById(id).get();
        return new ResponseEntity<>(herramienta, HttpStatus.OK);
    }

    @GetMapping("/getbyname/{nombre}")
    public ResponseEntity<Herramienta> getByNombre(@PathVariable("nombre") String nombre) {
        if (!herramientaService.existByNombre(nombre)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Herramienta herramienta = herramientaService.getByNombre(nombre).get();
        return new ResponseEntity<>(herramienta, HttpStatus.OK);

    }

    @PostMapping("/create")
    public ResponseEntity<MensajeDto> create(@RequestBody RequestBodyWraper requestBodyWraper) {
        try {
            Tema temaDto = requestBodyWraper.getTema();
            Herramienta herramientaDto = requestBodyWraper.getHerramienta();
            Momento momentoDto1 = requestBodyWraper.getMomento1();
            Momento momentoDto2 = requestBodyWraper.getMomento2();
            Momento momentoDto3 = requestBodyWraper.getMomento3();
            List<ProcesoJsonDto> procesosDto = requestBodyWraper.getListaprocesos();
            Recurso recursoDto = requestBodyWraper.getRecurso();

            insertarEnCascada(temaDto, herramientaDto, momentoDto1, momentoDto2, momentoDto3, procesosDto);

            return new ResponseEntity<>(new MensajeDto("Exito", true), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(new MensajeDto("Error", false),HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable("id") int id, @RequestBody Herramienta herramientaDto) {
        if (!herramientaService.existById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Herramienta herramienta = herramientaService.getById(id).get();
        herramienta.setIdTema(herramientaDto.getIdTema());
        herramienta.setDocenteAutor(herramientaDto.getDocenteAutor());
        herramienta.setNombreHerramienta(herramientaDto.getNombreHerramienta());
        herramienta.setObjetivos(herramientaDto.getObjetivos());
        herramienta.setVisibilidad(herramientaDto.getVisibilidad());
        herramienta.setComentarios(herramientaDto.getComentarios());
        herramienta.setEstado(herramientaDto.getEstado());
        herramienta.setRecomendacion(herramientaDto.getRecomendacion());
        herramienta.setFechaAprobacion(herramientaDto.getFechaAprobacion());
        herramienta.setFechaCreacion(herramientaDto.getFechaCreacion());

        herramientaService.save(herramienta);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<MensajeDto> delete(@PathVariable("id") int id) {
        if (!herramientaService.existById(id)) {
            return new ResponseEntity<>(new MensajeDto("Error",false),HttpStatus.NOT_FOUND);
        }
        herramientaService.delete(id);
        return new ResponseEntity<>(new MensajeDto("Exito",false),HttpStatus.OK);
    }


    @Transactional
    public void insertarEnCascada(Tema tema, Herramienta herramienta, Momento momento1, Momento momento2, Momento momento3, List<ProcesoJsonDto> procesosDto) {
        try {
            // 1. Se inserta en tema
            temaRepository.save(tema);

            // 2. Se inserta en herramienta que necesita a tema
            herramienta.setIdTema(tema.getIdTema());
            Date fechaActual = new Date(System.currentTimeMillis());
            herramienta.setFechaCreacion(fechaActual);
            herramientaService.save(herramienta);

            // 3. Se inserta en momento que necesita a herramienta
            momento1.setIdHerramienta(herramienta.getIdHerramienta());
            momentoRepository.save(momento1);

            momento2.setIdHerramienta(herramienta.getIdHerramienta());
            momentoRepository.save(momento2);

            momento3.setIdHerramienta(herramienta.getIdHerramienta());
            momentoRepository.save(momento3);

            //4. Se inserta en proceso que necesita a momento
            //procesos.setIdMomento(momento.getIdMomento());
            //procesoRepository.save(proceso);
            for (ProcesoJsonDto procesoJsonDto: procesosDto) {
                Proceso proceso = new Proceso();
                proceso.setTiempo(Time.valueOf(procesoJsonDto.getTiempo()));

                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode json = objectMapper.createObjectNode();
                json.put("descripcion", procesoJsonDto.getProceso());
                json.put("recurso", procesoJsonDto.getRecurso());
                proceso.setDescripcion(json.toString());
                proceso.setIdMomento(momento2.getIdMomento());
                procesoRepository.save(proceso);
            }
            // 5. Se inserta en recurso
            //recursoRepository.save(recurso);

            // 6. Se inserta en recurso_proceso que necesita a recurso y proceso
            //RecursoProceso recursoProceso = new RecursoProceso();
            //recursoProceso.setIdRecurso(recurso.getIdRecurso());
            //recursoProceso.setIdProceso(proceso.getIdProceso());

            //recursoProcesoRepository.save(recursoProceso);

        } catch (Exception e) {
            System.out.println(e);
        }

    }

}
