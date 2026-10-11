package com.estudo.votacao.boletim;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/boletins")
public class BoletimController {

    private final BoletimService service;
    private final ReceptorBoletimService receptor;

    public BoletimController(BoletimService service, ReceptorBoletimService receptor) {
        this.service = service;
        this.receptor = receptor;
    }

    /** 201 na primeira recepção; 200 quando o mesmo BU é retransmitido. */
    @PostMapping("/assinados")
    public ResponseEntity<BoletimResponse> receberAssinado(@Valid @RequestBody BoletimAssinadoRequest envelope) {
        ReceptorBoletimService.Recebimento recebimento = receptor.receber(envelope);
        if (!recebimento.novo()) {
            return ResponseEntity.ok(recebimento.boletim());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/boletins/{id}")
                .buildAndExpand(recebimento.boletim().id())
                .toUri();
        return ResponseEntity.created(location).body(recebimento.boletim());
    }

    @PostMapping
    public ResponseEntity<BoletimResponse> registrar(@Valid @RequestBody RegistrarBoletimRequest request) {
        BoletimResponse registrado = service.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(registrado.id())
                .toUri();
        return ResponseEntity.created(location).body(registrado);
    }

    @GetMapping("/{id}")
    public BoletimResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
