package com.estudo.votacao.candidato;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.estudo.votacao.comum.PaginaResponse;
import com.estudo.votacao.eleicao.Cargo;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidatos")
public class CandidatoController {

    private final CandidatoService service;

    public CandidatoController(CandidatoService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<CandidatoResponse> listar(
            @RequestParam(required = false) Long eleicaoId,
            @RequestParam(required = false) Cargo cargo,
            @PageableDefault(size = 20, sort = {"cargo", "numero"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(eleicaoId, cargo, pageable);
    }

    @GetMapping("/{id}")
    public CandidatoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<CandidatoResponse> criar(@Valid @RequestBody CandidatoRequest request) {
        CandidatoResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public CandidatoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CandidatoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
